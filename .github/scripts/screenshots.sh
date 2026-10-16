#!/usr/bin/env bash
# Startet die App im Emulator, spielt einen kurzen Ablauf durch und macht Screenshots.
set -x
OUT=screenshots
mkdir -p "$OUT"

# System-Dialoge des Emulators („… reagiert nicht“) wegklicken
calm() { python3 .github/scripts/tap.py "Wait" --quiet || true; }
shot() { sleep "${2:-2}"; calm; adb exec-out screencap -p > "$OUT/$1.png"; }
tap() { calm; python3 .github/scripts/tap.py "$1"; }

# Feste Uhrzeit, damit der Ablauf reproduzierbar ist: 12.10.2026, 08:00
adb root
sleep 3
adb shell settings put global auto_time 0
adb shell settings put system time_12_24 24
adb shell date 101208002026.00
sleep 20   # Launcher nach dem Zeitsprung zur Ruhe kommen lassen
calm

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
shot 3-neue-aufgabe
adb shell input keyevent KEYCODE_BACK   # Tastatur schliessen
sleep 1
adb shell input swipe 540 1900 540 900 400
sleep 1
tap "Gesundheit"
shot 3b-neue-aufgabe-unten 1
tap "Speichern"
shot 4-gespeichert

echo "--- geplante Alarme ---"
adb shell dumpsys alarm | grep -B1 -A4 "app.tagwerk" | tee "$OUT/alarms.txt"

# Erinnerung auslösen: Uhr kurz vor 18:00 stellen
adb shell date 101217592026.50
sleep 20
adb shell cmd statusbar expand-notifications
shot 5-erinnerung 3
echo "--- Benachrichtigungen ---"
adb shell dumpsys notification --noredact | grep -A3 "pkg=app.tagwerk" | head -40 | tee "$OUT/notifications.txt"
adb shell cmd statusbar collapse
sleep 1

tap "Tag"
sleep 1
tap "Abhaken"
shot 6-abgehakt 2

# Neue Bereiche: Kalender (Monat/Woche/Tag), Checkliste, Einstellungen
tap "Kalender"
shot 9-kalender-monat
tap "Woche"
shot 10-kalender-woche
tap "Tag"
shot 11-kalender-tag
tap "Checkliste"
shot 12-checkliste
tap "Einstellungen"
shot 13-einstellungen
tap "Flieder"
shot 14-thema-flieder
tap "Pfirsich"
sleep 1
tap "Aufgaben"
sleep 1

# Dunkler Modus
adb shell cmd uimode night yes
adb shell am force-stop app.tagwerk
adb shell am start -W -n app.tagwerk/.MainActivity
shot 7-dunkel 4
tap "Alle"
shot 8-dunkel-alle
tap "Kalender"
shot 15-dunkel-kalender

adb logcat -d -b crash > "$OUT/crash.txt"
echo "--- Abstürze ---"
cat "$OUT/crash.txt"
ls -la "$OUT"

# Der Test soll nur grün sein, wenn die App nicht abstürzt und die Erinnerung ankommt.
# (Das Crash-Log enthält alle Apps des Emulators – nur Abstürze von Tagwerk zählen.)
if grep -qE "Process: app\.tagwerk|>>> app\.tagwerk" "$OUT/crash.txt"; then echo "FEHLER: Tagwerk ist abgestürzt"; exit 1; fi
grep -q "pkg=app.tagwerk" "$OUT/notifications.txt" || { echo "FEHLER: keine Erinnerung erschienen"; exit 1; }
echo "Emulator-Test bestanden"
