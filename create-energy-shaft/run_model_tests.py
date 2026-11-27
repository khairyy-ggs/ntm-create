#!/usr/bin/env python3
"""Runs the pure-Java model tests with the JDK single-file launcher (no javac, no Gradle needed)."""
import os, re, subprocess, sys, tempfile
root = os.path.dirname(os.path.abspath(__file__))
mdir = os.path.join(root, "src/main/java/com/hbm/tileentity/machine")
test = open(os.path.join(root, "ShaftTurbineModelTest.java")).read()
test = re.sub(r"^import .*\n", "", test, flags=re.M)
test = re.sub(r"\bInputs\b", "ShaftTurbineModel.Inputs", test)
test = re.sub(r"\bOutputs\b", "ShaftTurbineModel.Outputs", test)
test = test.replace("ShaftTurbineModel.ShaftTurbineModel.", "ShaftTurbineModel.").replace("public class ShaftTurbineModelTest", "public class Combined")
parts = ["import java.util.*;\n", test]
for name in ("ShaftTurbineModel", "HeatExchangerModel"):
    src = open(os.path.join(mdir, name + ".java")).read()
    src = re.sub(r"^package .*\n", "", src, flags=re.M).replace("public final class " + name, "final class " + name)
    parts.append(src)
with tempfile.TemporaryDirectory() as d:
    f = os.path.join(d, "Combined.java")
    open(f, "w").write("\n".join(parts))
    sys.exit(subprocess.call(["java", f]))
