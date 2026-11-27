# DammKeyboard

Eigene, schlanke Bildschirmtastatur der Schule am Öjendorfer Damm – Optik wie die CTOUCH-Tastatur der Boards.

- **Klein und schnell**: eine einzige gezeichnete Ansicht, keine Bibliotheken (Release-APK ≈ 35 KB), ab Android 8
- **Datenschutz**: keine Internet-Berechtigung, keine Wortvorschläge, kein Lernen – nichts wird gespeichert oder gesendet
- **Keine Tastentöne**; leichte Vibration abschaltbar (Standard: aus)
- **Schwebend auf großen Bildschirmen** (Boards, Tablets): Griffe links und rechts zum Verschieben, ▾ klappt ein, die App darunter wird nicht gestaucht – auf Handys unten wie gewohnt
- QWERTZ mit Ziffern-Hinweisen, `?123`, weitere Zeichen, Zahlenblock für Zahl- und Telefonfelder, Emoji
- Lange drücken: Umlaute und ß, Akzente, Ziffern der oberen Reihe; Löschen wiederholt beim Halten
- Über die Leertaste wischen bewegt den Cursor; automatische Großschreibung am Satzanfang; ⇧ zweimal = feststellen
- 🌐 wechselt die Tastatur (Auswahl des Systems)

## Installieren

APK aus dem [Release](https://github.com/Teyro/DammKeyboard/releases) oder über das Damm-F-Droid-Verzeichnis.
App öffnen → „DammKeyboard in den Einstellungen aktivieren“ → „Tastatur auswählen“.

## Bauen

```
./gradlew testDebugUnitTest assembleDebug
```

Releases entstehen über einen Tag `vJJ.MM.TT` (Datumsversion wie die anderen Damm-Apps), signiert in CI.

## Lizenz

GNU GPL v3 oder neuer, siehe [LICENSE](LICENSE).
