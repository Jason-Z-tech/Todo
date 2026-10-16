# Tagwerk – To-do-Liste für Android

Eine ruhige To-do-App in Pastelltönen – mit Abhaken, Uhrzeiten und Erinnerungen.

## Vorschau

<p>
  <img src="https://github.com/Jason-Z-tech/Todo/releases/latest/download/1-tag.png" width="200" alt="Tagesansicht" />
  <img src="https://github.com/Jason-Z-tech/Todo/releases/latest/download/9-kalender-monat.png" width="200" alt="Kalender" />
  <img src="https://github.com/Jason-Z-tech/Todo/releases/latest/download/12-checkliste.png" width="200" alt="Checkliste" />
  <img src="https://github.com/Jason-Z-tech/Todo/releases/latest/download/13-einstellungen.png" width="200" alt="Einstellungen" />
  <img src="https://github.com/Jason-Z-tech/Todo/releases/latest/download/3b-neue-aufgabe-unten.png" width="200" alt="Neue Aufgabe" />
  <img src="https://github.com/Jason-Z-tech/Todo/releases/latest/download/5-erinnerung.png" width="200" alt="Erinnerung" />
  <img src="https://github.com/Jason-Z-tech/Todo/releases/latest/download/14-thema-flieder.png" width="200" alt="Farbthema Flieder" />
  <img src="https://github.com/Jason-Z-tech/Todo/releases/latest/download/8-dunkel-alle.png" width="200" alt="Dunkler Modus" />
</p>

Die Bilder entstehen bei jedem Build automatisch im Android-Emulator.

## Installieren

1. Auf dem Handy diesen Link **in Chrome** öffnen (nicht im Browser einer Chat-App):
   **https://github.com/Jason-Z-tech/Todo/releases/latest/download/Tagwerk.apk**
2. Fragt Chrome „Datei kann schädlich sein“, auf **Trotzdem herunterladen** tippen.
3. Die Datei öffnen und „Installation aus dieser Quelle zulassen“ für Chrome erlauben.
4. Falls Play Protect fragt: **Details → Trotzdem installieren**.
5. Beim ersten Start Benachrichtigungen erlauben, damit die Erinnerungen ankommen.

Updates installiert man genauso – sie werden über die bestehende App installiert,
Aufgaben bleiben erhalten.

Jeder Push baut die App neu, testet sie im Emulator und veröffentlicht sie danach als
neue Version unter *Releases*. Der Link oben zeigt immer auf die neueste Version.

**Signatur:** Alle Builds werden mit dem Schlüssel in `signing/tagwerk.jks` signiert,
damit Updates zur installierten App passen. Für eine private App ist das so in Ordnung;
wer die App weitergibt, sollte den Schlüssel stattdessen als GitHub-Secret hinterlegen.

## Funktionen

- **Abhaken** mit kleiner Animation – erledigte Aufgaben wandern in „Erledigt“
- **Datum & Uhrzeit** mit Schnellauswahl (Heute, Morgen, 09:00, 12:00, 18:00 …)
- **Erinnerungen** zur Uhrzeit oder 5 Min / 15 Min / 30 Min / 1 Std / 1 Tag vorher –
  direkt aus der Benachrichtigung „Erledigt“ oder „10 Min später“
- **Wiederholen**: täglich, werktags, wöchentlich, an **bestimmten Wochentagen** (z. B. Mo + Do),
  monatlich, jährlich – abgehakte Tage merkt sich die App, spätere Termine stehen schon im Kalender
- **Listen** in Pastellfarben – frei **benennbar**, Farbe wählbar, neue Listen anlegen oder löschen
- **Teilschritte** innerhalb einer Aufgabe
- **Wichtig**-Stern, Notizen
- **Aufgaben**: Ansichten **Tag** (mit Wochenleiste), **Alle** (Überfällig, Heute, Morgen, Diese Woche …) und **Erledigt**
- **Kalender** mit Umschalter **Tag** (Zeitleiste), **Woche** und **Monat**
- **Checkliste**: nur die To-dos eines Tages zum Ankreuzen, nach Uhrzeit sortiert – ohne Uhrzeit ganz unten
- **Farbthemen**: fertige Pastelltöne oder eine **eigene Farbe** per Farbcode (z. B. `#F4B6A0`) oder Reglern
- Begrüßung mit eigenem Namen – standardmäßig „Hallo Queen“
- Wischen zum Löschen mit „Rückgängig“
- Heller und dunkler Modus, Erinnerungen überleben Neustarts

## Technik

Kotlin, Jetpack Compose, Material 3 (eigenes Pastell-Design), AlarmManager für pünktliche
Erinnerungen, Daten lokal als JSON. minSdk 26 (Android 8.0).

```
./gradlew assembleDebug testDebugUnitTest
```

Schriften: [Nunito](https://github.com/googlefonts/nunito) und
[Fraunces](https://github.com/undercasetype/Fraunces), beide unter der SIL Open Font License.
