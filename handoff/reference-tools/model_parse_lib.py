"""模型骨架解析库（**括号配对 + 受限 Java 子集解释器**）。

为什么不用正则
==============
`addBox(...)` 有第 7 个可选参数 `CubeDeformation`，而且调用里**嵌套着子部件的
`addOrReplaceChild`**。历史上有三轮审计都是栽在"用正则从 `addOrReplaceChild(`
往后找第一个 `PartPose.xxx(...)`"上：父部件会拿到**子部件**的位姿，于是头被算到
离躯干 28 像素的地方，"头浮空"的结论全是假的（见 render_models5.py 的注释）。

本库的做法
==========
把 `createBodyLayer()` 的**整个方法体**当成一小段 Java 来**解释执行**：

    * 词法/语法只覆盖模型代码里真正出现的东西（声明、赋值、for、if、三元、算术、
      字符串拼接、链式调用、数组下标、`.length`、`new`、`Mth.PI`、`(float)` 转换）；
    * `addOrReplaceChild` / `getChild` / `addBox` / `texOffs` / `mirror` /
      `PartPose.*` / `MeshDefinition` 都实现为内置对象；
    * `for (int i = ...)` 会**真的循环**，所以 `segment_0..8` 这种循环生成的
      部件也一个不少（这是正则永远做不到的）。

输出的每个部件都带**完整世界变换**（沿父链累积 offset + zRot/yRot/xRot），
因此可以精确算出"最低点 y"——注意 MC 模型空间 **+y 朝下**，
所以"脚底"是 y 的**最大值**，原版基准是 `脚底 y = 24`（`LivingEntityRenderer`
在画之前会 `translate(0, -1.501, 0)`，1.501 格 = 24.016 像素）。
"""
import math
import re

# ----------------------------------------------------------------------
# 词法
# ----------------------------------------------------------------------
_TOKEN_RE = re.compile(r"""
    (?P<ws>\s+)
  | (?P<str>"(?:[^"\\]|\\.)*")
  | (?P<num>(?:\d+\.\d*|\.\d+|\d+)(?:[eE][-+]?\d+)?[FfDdLl]?)
  | (?P<id>[A-Za-z_$][A-Za-z0-9_$]*)
  | (?P<op>==|!=|<=|>=|&&|\|\||\+=|-=|\*=|/=|[-+*/%<>=!?:.,;()\[\]{}])
""", re.X)

_CASTS = {"float", "double", "int", "long", "short", "byte", "char",
          "Float", "Double", "Integer", "Long"}


class ParseError(Exception):
    pass


def strip_comments(src):
    out = []
    i, n = 0, len(src)
    while i < n:
        c = src[i]
        if c == '"':
            j = i + 1
            while j < n:
                if src[j] == "\\":
                    j += 2
                    continue
                if src[j] == '"':
                    break
                j += 1
            out.append(src[i:j + 1])
            i = j + 1
            continue
        if c == "/" and i + 1 < n and src[i + 1] == "/":
            j = src.find("\n", i)
            i = n if j < 0 else j
            continue
        if c == "/" and i + 1 < n and src[i + 1] == "*":
            j = src.find("*/", i + 2)
            i = n if j < 0 else j + 2
            continue
        out.append(c)
        i += 1
    return "".join(out)


def tokenize(src):
    toks, i = [], 0
    while i < len(src):
        m = _TOKEN_RE.match(src, i)
        if not m:
            raise ParseError("无法识别的字符 %r @%d" % (src[i], i))
        i = m.end()
        if m.lastgroup != "ws":
            toks.append((m.lastgroup, m.group()))
    toks.append(("eof", ""))
    return toks


# ----------------------------------------------------------------------
# 语法（只覆盖模型代码用得到的子集）
# ----------------------------------------------------------------------
class Parser:
    def __init__(self, toks):
        self.t = toks
        self.i = 0

    def peek(self, k=0):
        return self.t[min(self.i + k, len(self.t) - 1)]

    def at(self, val):
        return self.t[self.i][1] == val

    def take(self):
        v = self.t[self.i]
        self.i += 1
        return v

    def expect(self, val):
        if not self.at(val):
            raise ParseError("期望 %r，实际 %r" % (val, self.peek()))
        return self.take()

    # ---- 语句 ----
    def block(self):
        self.expect("{")
        stmts = []
        while not self.at("}"):
            stmts.append(self.statement())
        self.expect("}")
        return ("block", stmts)

    def statement(self):
        if self.at("{"):
            return self.block()
        if self.at("for"):
            return self.for_stmt()
        if self.at("if"):
            return self.if_stmt()
        if self.at("return"):
            self.take()
            e = None if self.at(";") else self.expr()
            self.expect(";")
            return ("return", e)
        # 声明 或 表达式语句：先按表达式解析，再判断是不是 `Type name = init`
        save = self.i
        try:
            e = self.expr()
            if self.at(";"):
                self.take()
                return ("expr", e)
            raise ParseError("表达式后没有分号")
        except ParseError:
            self.i = save
        # 形如 `PartDefinition head = ...` / `float f = ...` / `int[] A = {...}`
        typ = self.take()[1]
        if self.at("["):
            self.take()
            self.expect("]")
            typ += "[]"
        if self.peek()[0] != "id":
            raise ParseError("声明缺少变量名: %r" % (self.peek(),))
        name = self.take()[1]
        init = None
        if self.at("="):
            self.take()
            if self.at("{"):
                self.take()
                vals = []
                if not self.at("}"):
                    vals.append(self.expr())
                    while self.at(","):
                        self.take()
                        vals.append(self.expr())
                self.expect("}")
                init = ("array", vals)
            else:
                init = self.expr()
        self.expect(";")
        return ("decl", typ, name, init)

    def for_stmt(self):
        self.expect("for")
        self.expect("(")
        init = self.statement_no_semi()
        self.expect(";")
        cond = self.expr()
        self.expect(";")
        upd = self.statement_no_semi()
        self.expect(")")
        body = self.statement()
        return ("for", init, cond, upd, body)

    def statement_no_semi(self):
        """for 的 init/update 段：可能是声明，也可能是赋值/自增表达式。"""
        if self.peek()[0] == "id" and self.peek()[1] in _CASTS:
            typ = self.take()[1]
            name = self.take()[1]
            init = None
            if self.at("="):
                self.take()
                init = self.expr()
            return ("decl", typ, name, init)
        return ("expr", self.expr())

    def if_stmt(self):
        self.expect("if")
        self.expect("(")
        cond = self.expr()
        self.expect(")")
        then = self.statement()
        els = None
        if self.at("else"):
            self.take()
            els = self.statement()
        return ("if", cond, then, els)

    # ---- 表达式 ----
    def expr(self):
        return self.assignment()

    def assignment(self):
        left = self.ternary()
        if self.peek()[1] in ("=", "+=", "-=", "*=", "/="):
            op = self.take()[1]
            right = self.assignment()
            return ("assign", left, op, right)
        return left

    def ternary(self):
        c = self.binary(0)
        if self.at("?"):
            self.take()
            a = self.assignment()
            self.expect(":")
            b = self.assignment()
            return ("ternary", c, a, b)
        return c

    _LEVELS = [("||",), ("&&",), ("==", "!="), ("<", ">", "<=", ">="),
               ("+", "-"), ("*", "/", "%")]

    def binary(self, lvl):
        if lvl >= len(self._LEVELS):
            return self.unary()
        ops = self._LEVELS[lvl]
        left = self.binary(lvl + 1)
        while self.peek()[1] in ops:
            op = self.take()[1]
            right = self.binary(lvl + 1)
            left = ("bin", op, left, right)
        return left

    def unary(self):
        if self.at("-"):
            self.take()
            return ("un", "-", self.unary())
        if self.at("!"):
            self.take()
            return ("un", "!", self.unary())
        return self.postfix()

    def postfix(self):
        e = self.primary()
        while True:
            if self.at("."):
                self.take()
                name = self.take()[1]
                if self.at("("):
                    args = self.args()
                    e = ("call", ("member", e, name), args)
                else:
                    e = ("member", e, name)
            elif self.at("["):
                self.take()
                idx = self.expr()
                self.expect("]")
                e = ("index", e, idx)
            elif self.at("+") and self.peek(1)[1] == "+":
                self.take()
                self.take()
                e = ("inc", e, 1.0)
            elif self.at("-") and self.peek(1)[1] == "-":
                self.take()
                self.take()
                e = ("inc", e, -1.0)
            else:
                return e

    def args(self):
        self.expect("(")
        out = []
        if not self.at(")"):
            out.append(self.expr())
            while self.at(","):
                self.take()
                out.append(self.expr())
        self.expect(")")
        return out

    def primary(self):
        kind, val = self.take()
        if kind == "num":
            return ("num", float(re.sub(r"[FfDdLl]$", "", val)))
        if kind == "str":
            return ("str", val[1:-1])
        if val == "(":
            # 可能是强制类型转换 `(float) x`
            if self.peek()[0] == "id" and self.peek()[1] in _CASTS and self.peek(1)[1] == ")":
                self.take()
                self.take()
                return self.unary()
            e = self.expr()
            self.expect(")")
            return e
        if val == "new":
            typ = self.take()[1]
            args = self.args()
            return ("new", typ, args)
        if kind == "id":
            if val in ("true", "false"):
                return ("bool", val == "true")
            if val in ("null",):
                return ("null",)
            return ("id", val)
        raise ParseError("看不懂的表达式起点 %r" % (val,))


def parse_expression(text):
    p = Parser(tokenize(text))
    e = p.expr()
    return e


# ----------------------------------------------------------------------
# 运行时对象
# ----------------------------------------------------------------------
class Part:
    __slots__ = ("name", "parent", "children", "boxes", "pose", "order")

    def __init__(self, name, parent, pose=None):
        self.name = name
        self.parent = parent                # Part 或 None（根）
        self.children = []
        self.boxes = []                     # [(x,y,z,sx,sy,sz,inflate)]
        if isinstance(pose, PartPose):
            pose = (pose.x, pose.y, pose.z, pose.rx, pose.ry, pose.rz)
        self.pose = pose or (0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
        self.order = -1
        if parent is not None:
            parent.children.append(self)


class CubeList:
    def __init__(self):
        self.boxes = []
        self.mirror = False
        self.tex = None


class PartPose:
    def __init__(self, x, y, z, rx=0.0, ry=0.0, rz=0.0):
        self.x, self.y, self.z = x, y, z
        self.rx, self.ry, self.rz = rx, ry, rz


class Mesh:
    def __init__(self):
        self.root = Part("root", None)


class Builtin:
    """占位对象：CubeDeformation.NONE 之类，只为了不被当成未定义变量。"""

    def __init__(self, tag, value=None):
        self.tag = tag
        self.value = value

    def __repr__(self):
        return "<%s>" % self.tag


IDENT = lambda v: v  # noqa: E731


def _vanilla_humanoid_mesh():
    """原版 `HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F)` 的等价物。

    `CultivationNpcModel.createBodyLayer()` 直接调了它，源码里没有这些数字，
    所以必须在这里照抄一份（数值来自 1.20.1 反编译的 HumanoidModel.createMesh）。
    脚底 = 12 + 12 = 24 ✓
    """
    mesh = Mesh()
    r = mesh.root
    def box(tex, x, y, z, sx, sy, sz):
        cl = CubeList()
        cl.tex = tex
        cl.boxes.append((x, y, z, sx, sy, sz, 0.0))
        return cl
    def add(parent, name, cl, pose):
        p = Part(name, parent, pose)
        p.boxes = list(cl.boxes)
        return p
    add(r, "head", box((0, 0), -4, -8, -4, 8, 8, 8), PartPose(0, 0, 0))
    add(r, "hat", box((32, 0), -4, -8, -4, 8, 8, 8), PartPose(0, 0, 0))
    add(r, "body", box((16, 16), -4, 0, -2, 8, 12, 4), PartPose(0, 0, 0))
    add(r, "right_arm", box((40, 16), -3, -2, -2, 4, 12, 4), PartPose(-5, 2, 0))
    add(r, "left_arm", box((40, 16), -1, -2, -2, 4, 12, 4), PartPose(5, 2, 0))
    add(r, "right_leg", box((0, 16), -2, 0, -2, 4, 12, 4), PartPose(-1.9, 12, 0))
    add(r, "left_leg", box((0, 16), -2, 0, -2, 4, 12, 4), PartPose(1.9, 12, 0))
    return mesh


class Interp:
    """解释执行一段受限的 Java 方法体，收集部件树。"""

    def __init__(self, env=None):
        self.env = dict(env or {})
        self.parts = []
        self._counter = 0

    # ---- 语句 ----
    def run(self, node):
        kind = node[0]
        if kind == "block":
            for s in node[1]:
                r = self.run(s)
                if r == "RETURN":
                    return r
            return None
        if kind == "expr":
            self.eval(node[1])
            return None
        if kind == "decl":
            _typ, name, init = node[1], node[2], node[3]
            self.env[name] = self.eval(init) if init is not None else None
            return None
        if kind == "return":
            if node[1] is not None:
                self.eval(node[1])
            return "RETURN"
        if kind == "if":
            if self.truth(self.eval(node[1])):
                return self.run(node[2])
            if node[3] is not None:
                return self.run(node[3])
            return None
        if kind == "for":
            self.run(node[1])
            guard = 0
            while self.truth(self.eval(node[2])):
                r = self.run(node[4])
                if r == "RETURN":
                    return r
                self.run(node[3])
                guard += 1
                if guard > 4096:
                    raise ParseError("for 循环跑了 4096 次还没停，疑似死循环")
            return None
        raise ParseError("未知语句 %r" % (kind,))

    # ---- 表达式 ----
    def eval(self, node):
        if node is None:
            return None
        kind = node[0]
        if kind == "num":
            return node[1]
        if kind == "str":
            return node[1]
        if kind == "bool":
            return node[1]
        if kind == "null":
            return None
        if kind == "id":
            name = node[1]
            if name not in self.env:
                raise ParseError("未定义变量: %s" % name)
            return self.env[name]
        if kind == "un":
            v = self.eval(node[2])
            return -v if node[1] == "-" else (not self.truth(v))
        if kind == "bin":
            op = node[1]
            if op == "&&":
                return self.truth(self.eval(node[2])) and self.truth(self.eval(node[3]))
            if op == "||":
                return self.truth(self.eval(node[2])) or self.truth(self.eval(node[3]))
            a, b = self.eval(node[2]), self.eval(node[3])
            if op == "+":
                if isinstance(a, str) or isinstance(b, str):
                    return _jstr(a) + _jstr(b)
                return a + b
            if op == "-":
                return a - b
            if op == "*":
                return a * b
            if op == "/":
                return a / b
            if op == "%":
                return a % b
            if op == "==":
                return a == b
            if op == "!=":
                return a != b
            if op == "<":
                return a < b
            if op == ">":
                return a > b
            if op == "<=":
                return a <= b
            if op == ">=":
                return a >= b
            raise ParseError("未知运算符 %s" % op)
        if kind == "ternary":
            return self.eval(node[2]) if self.truth(self.eval(node[1])) else self.eval(node[3])
        if kind == "assign":
            target, op, value = node[1], node[2], node[3]
            v = self.eval(value)
            if op != "=":
                cur = self.eval(target)
                v = {"+=": cur + v, "-=": cur - v, "*=": cur * v, "/=": cur / v}[op]
            self.store(target, v)
            return v
        if kind == "new":
            typ, args = node[1], [self.eval(a) for a in node[2]]
            if typ == "MeshDefinition":
                return Mesh()
            if typ == "CubeDeformation":
                return Builtin("cubedef", args[1] if len(args) > 1 else 0.0)
            if typ == "PartPose":
                return Builtin("partpose")
            raise ParseError("不认识的 new %s" % typ)
        if kind == "index":
            obj = self.eval(node[1])
            idx = int(self.eval(node[2]))
            return obj[idx]
        if kind == "array":
            return [self.eval(x) for x in node[1]]
        if kind == "inc":
            cur = self.eval(node[1])
            self.store(node[1], cur + node[2])
            return cur
        if kind == "member":
            obj, name = self.eval(node[1]), node[2]
            return self.member(obj, name)
        if kind == "call":
            callee = node[1]
            args = [self.eval(a) for a in node[2]]
            if callee[0] == "member":
                obj = self.eval(callee[1])
                return self.call(obj, callee[2], args)
            if callee[0] == "id":
                return self.static_call(callee[1], args)
            raise ParseError("看不懂的调用 %r" % (callee,))
        raise ParseError("未知表达式 %r" % (kind,))

    def store(self, target, value):
        if target[0] == "id":
            self.env[target[1]] = value
            return
        if target[0] == "member":
            obj = self.eval(target[1])
            if isinstance(obj, Builtin) and obj.tag == "partpose":
                setattr(obj, target[2], value)
                return
            raise ParseError("不能给 %r 赋值" % (target,))
        raise ParseError("不能给 %r 赋值" % (target,))

    # ---- 成员/方法 ----
    def member(self, obj, name):
        if isinstance(obj, Builtin):
            if obj.tag == "class":
                return self.static_call(obj.value + "." + name, [])
            if obj.tag == "partpose":
                return getattr(obj, name)
            return obj
        if isinstance(obj, Mesh) and name == "root":
            return obj.root
        if isinstance(obj, Part) and name == "_name":
            return obj.name
        if isinstance(obj, str) and name == "length":
            return len(obj)
        if isinstance(obj, list) and name == "length":
            return len(obj)
        raise ParseError("不认识的成员 %r.%s" % (obj, name))

    def call(self, obj, name, args):
        if isinstance(obj, Mesh):
            if name == "getRoot":
                return obj.root
        if isinstance(obj, Builtin) and obj.tag == "class":
            return self.static_call(obj.value + "." + name, args)
        if isinstance(obj, CubeList):
            if name == "texOffs":
                obj.tex = (args[0], args[1])
                return obj
            if name == "mirror":
                obj.mirror = True
                return obj
            if name == "addBox":
                nums = [float(a) for a in args[:6]]
                extra = args[6] if len(args) > 6 else None
                inflate = 0.0
                if isinstance(extra, Builtin) and extra.tag == "cubedef":
                    inflate = float(extra.value or 0.0)
                elif isinstance(extra, (int, float)):
                    inflate = float(extra)
                obj.boxes.append(tuple(nums) + (inflate,))
                return obj
        if isinstance(obj, Part):
            if name == "addOrReplaceChild":
                pname, cubes, pose = args[0], args[1], args[2]
                for c in list(obj.children):
                    if c.name == pname:
                        obj.children.remove(c)
                        if c in self.parts:
                            self.parts.remove(c)
                pose = pose if isinstance(pose, PartPose) else PartPose(0, 0, 0)
                p = Part(pname, obj, (pose.x, pose.y, pose.z, pose.rx, pose.ry, pose.rz))
                p.boxes = list(cubes.boxes) if isinstance(cubes, CubeList) else []
                self._counter += 1
                p.order = self._counter
                self.parts.append(p)
                return p
            if name == "getChild":
                for c in obj.children:
                    if c.name == args[0]:
                        return c
                raise ParseError("getChild 找不到 %s" % args[0])
        raise ParseError("不认识的调用 %r.%s" % (obj, name))

    def static_call(self, path, args):
        if path == "CubeListBuilder.create":
            return CubeList()
        if path == "PartPose.ZERO":
            return PartPose(0, 0, 0)
        if path == "PartPose.offset":
            return PartPose(*[float(a) for a in args[:3]])
        if path == "PartPose.offsetAndRotation":
            vals = [float(a) for a in args[:6]]
            while len(vals) < 6:
                vals.append(0.0)
            return PartPose(*vals)
        if path == "PartPose.rotation":
            vals = [float(a) for a in args[:3]]
            return PartPose(0, 0, 0, *vals)
        if path == "CubeDeformation.NONE":
            return Builtin("cubedef", 0.0)
        if path == "HumanoidModel.createMesh":
            mesh = _vanilla_humanoid_mesh()
            self._register_tree(mesh.root)
            return mesh
        if path == "LayerDefinition.create":
            return Builtin("layerdef", args)
        if path in ("Mth.PI", "Math.PI"):
            return math.pi
        raise ParseError("不认识的静态调用 %s" % path)

    def _register_tree(self, part):
        """把某个内置 Mesh 里已经建好的部件（原版人形骨架）登记进收集列表。"""
        for c in part.children:
            self._counter += 1
            c.order = self._counter
            self.parts.append(c)
            self._register_tree(c)

    @staticmethod
    def truth(v):
        if isinstance(v, bool):
            return v
        if v is None:
            return False
        if isinstance(v, (int, float)):
            return v != 0
        return True


def _jstr(v):
    if isinstance(v, float) and v.is_integer():
        return str(int(v))
    return str(v)


# ----------------------------------------------------------------------
# 变换：把部件树变成"每个盒子的世界坐标范围"
# ----------------------------------------------------------------------
def _rot_x(a):
    c, s = math.cos(a), math.sin(a)
    return [[1, 0, 0], [0, c, -s], [0, s, c]]


def _rot_y(a):
    c, s = math.cos(a), math.sin(a)
    return [[c, 0, s], [0, 1, 0], [-s, 0, c]]


def _rot_z(a):
    c, s = math.cos(a), math.sin(a)
    return [[c, -s, 0], [s, c, 0], [0, 0, 1]]


def _mm(a, b):
    return [[sum(a[i][k] * b[k][j] for k in range(3)) for j in range(3)]
            for i in range(3)]


def _mv(m, v):
    return tuple(sum(m[i][k] * v[k] for k in range(3)) for i in range(3))


def part_transforms(parts):
    """返回 {部件名: (R, t)}——把部件空间的点变到模型空间（像素）。

    MC 的 `ModelPart.translateAndRotate` 顺序：先平移 offset，
    再绕 Z、绕 Y、绕 X 旋转（`T * Rz * Ry * Rx`）。
    """
    by_name = {}
    for p in parts:
        by_name.setdefault(p.name, p)
    out = {"root": ([[1, 0, 0], [0, 1, 0], [0, 0, 1]], (0.0, 0.0, 0.0))}
    resolved = set()

    def resolve(p, depth=0):
        if p.name in resolved or depth > 64:
            return out.get(p.name, ([[1, 0, 0], [0, 1, 0], [0, 0, 1]], (0.0,) * 3))
        rx, ry, rz = p.pose[3], p.pose[4], p.pose[5]
        local_r = _mm(_mm(_rot_z(rz), _rot_y(ry)), _rot_x(rx))
        local_t = (p.pose[0], p.pose[1], p.pose[2])
        if p.parent is None:
            base_r, base_t = [[1, 0, 0], [0, 1, 0], [0, 0, 1]], (0.0, 0.0, 0.0)
        else:
            base_r, base_t = resolve(p.parent, depth + 1)
        rot = _mm(base_r, local_r)
        off = _mv(base_r, local_t)
        t = (base_t[0] + off[0], base_t[1] + off[1], base_t[2] + off[2])
        out[p.name] = (rot, t)
        resolved.add(p.name)
        return out[p.name]

    for p in parts:
        resolve(p)
    return out


def box_world_aabb(rot, t, box):
    """一个盒子在模型空间的 AABB（像素）。box 允许 7 元（多一个 inflate）。"""
    bx, by, bz, sx, sy, sz = box[:6]
    inflate = box[6] if len(box) > 6 else 0.0
    xs = (bx - inflate, bx + sx + inflate)
    ys = (by - inflate, by + sy + inflate)
    zs = (bz - inflate, bz + sz + inflate)
    pts = [(-1, -1, -1), (-1, -1, 1), (-1, 1, -1), (-1, 1, 1),
           (1, -1, -1), (1, -1, 1), (1, 1, -1), (1, 1, 1)]
    lo = [1e9] * 3
    hi = [-1e9] * 3
    for i, j, k in pts:
        v = (xs[0] if i < 0 else xs[1],
             ys[0] if j < 0 else ys[1],
             zs[0] if k < 0 else zs[1])
        w = _mv(rot, v)
        for d in range(3):
            c = w[d] + t[d]
            lo[d] = min(lo[d], c)
            hi[d] = max(hi[d], c)
    return lo, hi


# ----------------------------------------------------------------------
# 从 Java 文件里取方法体 / 常量
# ----------------------------------------------------------------------
def extract_body(src, method_re):
    """按括号配对取出匹配方法的 `{...}` 方法体（不含花括号本身）。"""
    m = re.search(method_re, src)
    if not m:
        return None
    i = src.find("{", m.end() - 1)
    if i < 0:
        return None
    depth = 0
    j = i
    while j < len(src):
        if src[j] == "{":
            depth += 1
        elif src[j] == "}":
            depth -= 1
            if depth == 0:
                return src[i + 1:j]
        j += 1
    return None


def class_constants(src):
    """类里的 `static final int X = 9;` / `static final int[] F = {1,3};` /
    `static final PartPose P = PartPose.offset(...);`。"""
    env = {}
    for m in re.finditer(r'static\s+final\s+int(?:\[\])?\s+(\w+)\s*=\s*([^;]+);', src):
        name, expr = m.group(1), m.group(2).strip()
        if expr.startswith("{"):
            vals = [float(v) for v in re.findall(r'-?\d+(?:\.\d+)?', expr)]
            env[name] = [int(v) for v in vals]
        else:
            try:
                env[name] = int(float(expr.rstrip("Ff")))
            except ValueError:
                pass
    return env


def pose_constants(src, env):
    """求值 `static final PartPose` 常量（渲染器里当挂点用，可能写在方法之后）。"""
    out = {}
    it = Interp(dict(env))
    for m in re.finditer(r'static\s+final\s+PartPose\s+(\w+)\s*=\s*([^;]+);', src):
        try:
            out[m.group(1)] = it.eval(parse_expression(m.group(2)))
        except ParseError:
            pass
    return out


def parse_model_file(path):
    """解析一个含 `createBodyLayer()` 的 Java 文件，返回 (parts, 纹理尺寸)。"""
    src = strip_comments(open(path, encoding="utf-8").read())
    body = extract_body(src, r'createBodyLayer\s*\(\s*\)\s*\{')
    if body is None:
        raise ParseError("找不到 createBodyLayer(): %s" % path)
    env = class_constants(src)
    env["Mth"] = Builtin("class", "Mth")
    env["Math"] = Builtin("class", "Math")
    env["CubeDeformation"] = Builtin("class", "CubeDeformation")
    # 项目里的模型常写 private static final CubeDeformation NONE = ...，
    # 然后把 NONE 当第 7 个参数传给 addBox —— 解析器要把这个裸名字也认出来，
    # 否则会抛"未定义变量: NONE"（新增骨架时必踩）。
    env["NONE"] = Builtin("CubeDeformation", "NONE")
    env["DEFORM"] = Builtin("CubeDeformation", "NONE")
    env["NONE_D"] = Builtin("CubeDeformation", "NONE")
    env["CubeListBuilder"] = Builtin("class", "CubeListBuilder")
    env["PartPose"] = Builtin("class", "PartPose")
    env["HumanoidModel"] = Builtin("class", "HumanoidModel")
    env["LayerDefinition"] = Builtin("class", "LayerDefinition")
    env.update(pose_constants(src, env))
    it = Interp(env)
    p = Parser(tokenize(body))
    while not p.at("eof"):
        if it.run(p.statement()) == "RETURN":
            break
    tex = (64, 64)
    m = re.search(r'LayerDefinition\.create\(\s*mesh\s*,\s*(\d+)\s*,\s*(\d+)', body)
    if m:
        tex = (int(m.group(1)), int(m.group(2)))
    return it.parts, tex


# 地面接触组：这些部件才是"脚"。注意 +y 朝下，所以取的是**最大** y。
GROUND_KEYS = ("leg", "foot", "hoof", "talon", "claw", "paw", "thigh", "shin")


def part_stats(parts):
    """返回逐部件与整体的 y 范围（像素，+y 朝下 => 最大 y = 最低点）。"""
    tr = part_transforms(parts)
    rows = []
    for p in parts:
        if not p.boxes:
            continue
        rot, t = tr[p.name]
        lo_all, hi_all = None, None
        for b in p.boxes:
            lo, hi = box_world_aabb(rot, t, b)
            if lo_all is None:
                lo_all, hi_all = list(lo), list(hi)
            else:
                lo_all = [min(lo_all[d], lo[d]) for d in range(3)]
                hi_all = [max(hi_all[d], hi[d]) for d in range(3)]
        rows.append({"name": p.name, "lo": lo_all, "hi": hi_all,
                     "boxes": p.boxes, "rot": rot, "t": t,
                     "is_ground": any(k in p.name.lower() for k in GROUND_KEYS)})
    return rows


def summary(parts):
    rows = part_stats(parts)
    if not rows:
        return None
    foot_all = max(r["hi"][1] for r in rows)
    ground = [r for r in rows if r["is_ground"]]
    foot_leg = max((r["hi"][1] for r in ground), default=None)
    top = min(r["lo"][1] for r in rows)
    return {"rows": rows, "foot_all": foot_all, "foot_leg": foot_leg,
            "top": top, "height_px": foot_all - top}
