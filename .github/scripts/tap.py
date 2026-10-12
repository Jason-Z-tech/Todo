"""Tippt auf das erste UI-Element mit genau diesem Text (oder dieser Beschreibung)."""
import re
import subprocess
import sys
import time
import xml.etree.ElementTree as ET

target = sys.argv[1]
for _ in range(6):
    subprocess.run(["adb", "shell", "uiautomator", "dump", "/sdcard/ui.xml"], capture_output=True)
    raw = subprocess.run(["adb", "exec-out", "cat", "/sdcard/ui.xml"], capture_output=True).stdout
    try:
        root = ET.fromstring(raw)
    except ET.ParseError:
        time.sleep(1)
        continue
    for node in root.iter("node"):
        if target in (node.get("text"), node.get("content-desc")):
            x1, y1, x2, y2 = map(int, re.findall(r"\d+", node.get("bounds")))
            subprocess.run(["adb", "shell", "input", "tap", str((x1 + x2) // 2), str((y1 + y2) // 2)])
            print(f"tapped {target!r}")
            sys.exit(0)
    time.sleep(1)
print(f"not found: {target!r}")
sys.exit(1)
