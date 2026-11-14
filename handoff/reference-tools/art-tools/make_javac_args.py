# -*- coding: utf-8 -*-
"""把一组源文件 + classpath 写成 javac 参数文件，方便绕过 Windows 命令行长度限制。"""
import os
import sys

WS = r"C:\Users\duyun\Documents\deepseek-harness\default-workspace"


def main():
    out = sys.argv[1]
    srcs = sys.argv[2:]
    cp = os.path.join(WS, "xiuxian-mod", "build", "classes", "java", "main") + ";" + \
        ";".join(open(os.path.join(WS, "_cache", "cp_full.txt"), encoding="utf-8").read().splitlines())
    target = os.path.join(WS, "_cache", out + ".args")
    lines = ["-encoding", "UTF-8", "-nowarn", "-cp", cp, "-d",
             os.path.join(WS, "_cache", out)]
    lines += srcs
    with open(target, "w", encoding="utf-8", newline="\n") as f:
        f.write("\n".join(lines) + "\n")
    print(target)


if __name__ == "__main__":
    main()
