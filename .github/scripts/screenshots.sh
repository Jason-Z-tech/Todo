#!/usr/bin/env bash
# Startet die App im Emulator, spielt einen kurzen Ablauf durch und macht Screenshots.
set -x
OUT=screenshots
mkdir -p "$OUT"

shot() { sleep "${2:-2}"; adb exec-out screencap -p > "$OUT/$1.png"; }
tap() { python3 .github/scripts/tap.py "$1"; }

# Feste Uhrzeit, damit der Ablauf reproduzierbar ist: 12.10.2026, 08:00
adb root
sleep 3
adb shell settings put global auto_time 0
adb shell settings put system time_12_24 24
adb shell date 101208002026.00

adb install -r Tagwerk.apk
adb shell pm grant app.tagwerk android.permission.POST_NOTIFICATIONS
adb shell am start -W -n app.tagwerk/.MainActivity
shot 1-tag 5

tap "Alle"
shot 2-alle

tap "Neue Aufgabe"
sleep 2
adb shell input text "Zahnarzt%sanrufen"
tap "18:00"
tap "Gesundheit"
shot 3-neue-aufgabe
adb shell input keyevent KEYCODE_BACK   # Tastatur schliessen
sleep 1
adb shell input swipe 540 1900 540 700 400
sleep 1
tap "Speichern"
shot 4-gespeichert

tap "Tag"
sleep 1
tap "Abhaken"
shot 5-tag-nachher

adb shell dumpsys alarm | grep -B1 -A4 "app.tagwerk" > "$OUT/alarms.txt"

# Erinnerung auslösen: Uhr kurz vor 18:00 stellen
adb shell date 101217592026.55
sleep 15
adb shell cmd statusbar expand-notifications
shot 6-erinnerung 3
adb shell cmd statusbar collapse

# Dunkler Modus
adb shell cmd uimode night yes
adb shell am force-stop app.tagwerk
adb shell am start -W -n app.tagwerk/.MainActivity
shot 7-dunkel 4

adb logcat -d -b crash > "$OUT/crash.txt"
adb shell dumpsys notification --noredact | grep -A2 "app.tagwerk" | head -40 > "$OUT/notifications.txt"
ls -la "$OUT"
