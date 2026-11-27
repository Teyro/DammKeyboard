#!/bin/bash
# Bildschirmfotos im Emulator: Tastatur einschalten, Einstellungen öffnen, ins Textfeld tippen, Buchstaben/Zahlen/Zahlenblock
set -x
mkdir -p bilder
P=de.oejendorferdamm.dammkeyboard
adb install -r app/build/outputs/apk/debug/app-debug.apk
# direkt nach der Installation kennt Android die Tastatur noch nicht – kurz warten
for i in $(seq 1 15); do adb shell ime list -a -s | grep -q dammkeyboard && break; sleep 2; done
adb shell ime list -a -s
adb shell ime enable $P/.DammKeyboardDienst
adb shell ime set $P/.DammKeyboardDienst
foto() { sleep "${2:-3}"; adb exec-out screencap -p > "bilder/$1.png"; }
# Querformat wie auf dem Board (Schwebemodus über die Einstellung „an“)
for modus in aus an; do
  adb shell am force-stop $P
  adb shell "run-as $P sh -c 'mkdir -p shared_prefs; echo \"<?xml version=\\\"1.0\\\" encoding=\\\"utf-8\\\"?><map><string name=\\\"schweben\\\">$modus</string></map>\" > shared_prefs/einstellungen.xml'"
  # Zwangs-Stopp setzt auch die Tastatur zurück (gleiches Paket) – wieder auswählen
  adb shell ime enable $P/.DammKeyboardDienst; adb shell ime set $P/.DammKeyboardDienst
  adb shell am start -W -n $P/.EinstellungenActivity >/dev/null; sleep 3
  # erstes Textfeld antippen (unten in der Liste – Bildschirmmitte unten, dann scrollen)
  adb shell input swipe 540 1800 540 300 300; sleep 1
  adb shell uiautomator dump /sdcard/ui.xml >/dev/null; adb pull /sdcard/ui.xml ui.xml >/dev/null
  XY=$(grep -o 'text="Hier tippen …"[^>]*bounds="\[[0-9]*,[0-9]*\]\[[0-9]*,[0-9]*\]' ui.xml | grep -o '\[[0-9]*,[0-9]*\]' | head -1 | tr -d '[]' | tr ',' ' ')
  adb shell input tap $XY; foto "buchstaben_$modus" 3
  adb shell input text "Hallo"; foto "getippt_$modus" 2
  adb shell settings get secure default_input_method
done
adb logcat -d > logcat.txt
