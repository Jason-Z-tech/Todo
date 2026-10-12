# Tagwerk – To-do-Liste für Android

Eine ruhige To-do-App in Pastelltönen – mit Abhaken, Uhrzeiten und Erinnerungen.

## Vorschau

<p>
  <img src="https://github.com/Jason-Z-tech/Todo/releases/download/latest/1-tag.png" width="200" alt="Tagesansicht" />
  <img src="https://github.com/Jason-Z-tech/Todo/releases/download/latest/3b-neue-aufgabe-unten.png" width="200" alt="Neue Aufgabe" />
  <img src="https://github.com/Jason-Z-tech/Todo/releases/download/latest/5-erinnerung.png" width="200" alt="Erinnerung" />
  <img src="https://github.com/Jason-Z-tech/Todo/releases/download/latest/8-dunkel-alle.png" width="200" alt="Dunkler Modus" />
</p>

Die Bilder entstehen bei jedem Build automatisch im Android-Emulator.

## Installieren

1. Auf dem Handy die neueste APK herunterladen:
   **[Tagwerk.apk](../../releases/download/latest/Tagwerk.apk)** (Seite: *Releases → latest*)
2. Öffnen und „Installation aus unbekannten Quellen“ für den Browser bzw. Dateimanager erlauben.
3. Beim ersten Start Benachrichtigungen erlauben, damit die Erinnerungen ankommen.

Jeder Push baut die App automatisch neu (GitHub Actions → *APK bauen*).

## Funktionen

- **Abhaken** mit kleiner Animation – erledigte Aufgaben wandern in „Erledigt“
- **Datum & Uhrzeit** mit Schnellauswahl (Heute, Morgen, 09:00, 12:00, 18:00 …)
- **Erinnerungen** zur Uhrzeit oder 5 Min / 15 Min / 30 Min / 1 Std / 1 Tag vorher –
  direkt aus der Benachrichtigung „Erledigt“ oder „10 Min später“
- **Wiederholen**: täglich, werktags, wöchentlich, monatlich, jährlich
- **Listen** in Pastellfarben: Persönlich, Arbeit, Einkauf, Gesundheit, Zuhause, Ideen
- **Teilschritte** innerhalb einer Aufgabe
- **Wichtig**-Stern, Notizen
- Ansichten **Tag** (mit Wochenleiste), **Alle** (Überfällig, Heute, Morgen, Diese Woche …) und **Erledigt**
- Wischen zum Löschen mit „Rückgängig“
- Heller und dunkler Modus, Erinnerungen überleben Neustarts

## Technik

Kotlin, Jetpack Compose, Material 3 (eigenes Pastell-Design), AlarmManager für pünktliche
Erinnerungen, Daten lokal als JSON. minSdk 26 (Android 8.0).

```
./gradlew assembleDebug
```

Schriften: [Nunito](https://github.com/googlefonts/nunito) und
[Fraunces](https://github.com/undercasetype/Fraunces), beide unter der SIL Open Font License.
