# -*- coding: utf-8 -*-
"""交接包自检：禁改文件 / GeckoLib / 源码完整性。

用法（在交接包根目录）：
    python verify_handoff.py

PASS = 可以交接；FAIL = 禁止交付。
"""
from __future__ import annotations

import hashlib
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent
MOD = ROOT / "xiuxian-mod"

EXPECTED_GECKOLIB_SHA256 = (
    "c04abba4fd354dd8c0e8a732f1f05f4dca893d2e1d962b2ab128bbd411478747"
)

errors: list[str] = []
warnings: list[str] = []


def sha256(path: Path) -> str:
    h = hashlib.sha256()
    with path.open("rb") as f:
        for chunk in iter(lambda: f.read(1024 * 1024), b""):
            h.update(chunk)
    return h.hexdigest()


def fail(msg: str) -> None:
    errors.append(msg)


def warn(msg: str) -> None:
    warnings.append(msg)


def main() -> int:
    print("=== 鸿蒙修仙 3D 模型交接包自检 ===")
    print("根目录:", ROOT)

    # 1) 禁改文件
    forbidden_manifest = ROOT / "FORBIDDEN.sha256"
    if not forbidden_manifest.exists():
        fail("缺少 FORBIDDEN.sha256")
    else:
        for raw in forbidden_manifest.read_text(encoding="utf-8").splitlines():
            raw = raw.strip()
            if not raw or raw.startswith("#"):
                continue
            parts = raw.split()
            if len(parts) != 2:
                fail("FORBIDDEN.sha256 行格式错误: " + raw)
                continue
            expected, rel = parts
            path = ROOT / rel
            if not path.exists():
                fail("禁改文件不存在: " + rel)
                continue
            actual = sha256(path)
            if actual != expected:
                fail(f"禁改文件被修改: {rel}\n  期望 {expected}\n  实际 {actual}")
            else:
                print(f"  [OK] 禁改文件未变: {rel}")

    # 2) GeckoLib 依赖
    build_gradle = MOD / "build.gradle"
    if not build_gradle.exists():
        fail("缺少 xiuxian-mod/build.gradle")
    else:
        text = build_gradle.read_text(encoding="utf-8", errors="replace")
        if "geckolib-forge-1.20.1-4.8.4.jar" not in text:
            fail("build.gradle 未声明 geckolib-forge-1.20.1-4.8.4.jar")
        else:
            print("  [OK] build.gradle 已声明 GeckoLib 4.8.4")

    geckolib_jar = MOD / "libs" / "geckolib-forge-1.20.1-4.8.4.jar"
    if not geckolib_jar.exists():
        fail("缺少 libs/geckolib-forge-1.20.1-4.8.4.jar")
    else:
        actual = sha256(geckolib_jar)
        if actual != EXPECTED_GECKOLIB_SHA256:
            fail(f"GeckoLib jar 哈希不符:\n  期望 {EXPECTED_GECKOLIB_SHA256}\n  实际 {actual}")
        else:
            print("  [OK] GeckoLib jar 哈希一致")

    # 3) 关键源码目录
    entity_dir = MOD / "src/main/java/com/hongmeng/xiuxian/entity"
    client_dir = MOD / "src/main/java/com/hongmeng/xiuxian/client"
    if not entity_dir.is_dir():
        fail("缺少 entity 源码目录")
    else:
        n = len(list(entity_dir.glob("*.java")))
        print(f"  [OK] entity/*.java = {n}")
        if n < 50:
            warn("entity 类数量偏少，确认是否完整")

    if not client_dir.is_dir():
        fail("缺少 client 源码目录")
    else:
        models = len(list(client_dir.glob("*Model.java")))
        renderers = len(list(client_dir.glob("*Renderer.java")))
        print(f"  [OK] client Model = {models}, Renderer = {renderers}")
        if models < 30:
            fail("client Model 类数量异常少，源码可能不完整")
        if renderers < 40:
            fail("client Renderer 类数量异常少，源码可能不完整")

    # 4) 资源目录
    assets = MOD / "src/main/resources/assets/hongmeng"
    for rel, minimum in [
        ("geo", 100),
        ("textures/entity", 50),
    ]:
        path = assets / rel
        if not path.is_dir():
            fail("缺少资源目录: " + rel)
            continue
        n = len([p for p in path.iterdir() if p.is_file()])
        print(f"  [OK] assets/{rel} 文件数 = {n}")
        if n < minimum:
            fail(f"assets/{rel} 文件数异常少（< {minimum}）")

    # 5) 只读状态（Windows 可选检查；不阻断）
    for rel in [
        "xiuxian-mod/src/main/java/com/hongmeng/xiuxian/difficulty/BossLadder.java",
        "xiuxian-mod/src/main/java/com/hongmeng/xiuxian/entity/BossAttackKit.java",
    ]:
        p = ROOT / rel
        if p.exists() and not (p.stat().st_mode & 0o200):
            print(f"  [OK] 只读属性: {rel}")

    print()
    if warnings:
        print("WARNINGS:")
        for w in warnings:
            print("  -", w)
    if errors:
        print("FAIL:")
        for e in errors:
            print("  -", e)
        print("\n结论：FAIL，禁止交付。")
        return 1
    print("结论：PASS，可以交接。")
    return 0


if __name__ == "__main__":
    sys.exit(main())
