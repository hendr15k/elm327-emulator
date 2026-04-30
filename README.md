# ELM327 Bluetooth Emulator

Android App die einen echten ELM327 Bluetooth OBD-II Adapter emuliert. Andere Android Apps (z.B. Torque, DashCommand, OBD Car Doctor) können sich per Bluetooth mit dieser App verbinden und empfangen simulierte Fahrzeugdaten.

## Features

- Bluetooth RFCOMM Server (SPP Profil)
- ELM327 v1.5 Protokoll Parser
- AT-Befehle: `ATZ`, `ATE0/E1`, `ATL0/L1`, `ATH0/H1`, `ATSP0-9`, `ATI`, `ATRV`, `ATDP`, `ATD`, `ATWS`, etc.
- OBD-II Mode 01 PIDs: RPM, Geschwindigkeit, Kühlmitteltemperatur, Drosselklappe, Tankfüllstand, MAF, Lufteinlasstemperatur, Motorlaufzeit, etc.
- OBD-II Mode 03/07: DTC Codes
- Simulierte Fahrzeugdaten mit realistischer Variierung
- Foreground Service für dauerhaften Betrieb
- Live-Log zur Fehleranalyse

## Installation

1. APK aus dem [letzten Release](https://github.com/hendr15k/elm327-emulator/releases/latest) herunterladen
2. Auf dem Android-Gerät installieren (Installation aus unbekannten Quellen zulassen)
3. App öffnen und Berechtigungen erteilen

## Verwendung

1. ELM327 Emulator App öffnen
2. **"Start Server"** antippen
3. In der OBD-App (z.B. Torque) nach Bluetooth-Geräten suchen
4. Das Gerät erscheint als **"ELM327"**
5. Verbinden - die OBD-App empfängt nun simulierte Fahr zeugdaten

## Voraussetzungen

- Android 8.0 (API 26) oder höher
- Bluetooth-Unterstützung
- Berechtigungen: Bluetooth Connect, Bluetooth Advertise, Benachrichtigungen

## Technologie

- **Sprache:** Kotlin
- **Min SDK:** 26 (Android 8.0)
- **Target SDK:** 34 (Android 14)
- **Architektur:** MVVM
- **Build:** Gradle 8.7 + AGP 8.3.2

## Projekt bauen

```bash
git clone https://github.com/hendr15k/elm327-emulator.git
cd elm327-emulator
./gradlew assembleDebug
```

Die APK findet sich dann unter `app/build/outputs/apk/debug/app-debug.apk`.

## Lizenz

MIT
