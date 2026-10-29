# ELM327 Bluetooth Emulator

Android App die einen echten ELM327 Bluetooth OBD-II Adapter emuliert. Andere Android Apps (z.B. Torque, DashCommand, OBD Car Doctor) können sich per Bluetooth mit dieser App verbinden und empfangen simulierte Fahrzeugdaten.

## Features

- Bluetooth RFCOMM Server (SPP Profil)
- ELM327 v1.5 Protokoll Parser mit ELM-konformer Ausgabe (CR-Terminierung, `>`-Prompt, Echo, `ATL0/1`, `ATS0/1`, `ATH0/1`)
- AT-Befehle: `ATZ`, `ATD`, `ATE0/E1`, `ATL0/L1`, `ATH0/H1`, `ATS0/S1`, `ATSP0-9`/`ATSPA0-9`, `ATSPAA/B/C`, `ATST<hex>`, `ATI`, `ATRV`, `ATDP`, `ATDPN`, `ATWS`, `ATFE`, `AT@1/2/3`, `ATCAF`, `ATSH`, `ATCRA`, `ATPP`, `ATMA` u.a.
- OBD-II Mode 01: 56 PIDs in standardkonformer Skalierung (RPM, Geschwindigkeit, Kühlmittel-, Ansaug-, Umgebungs- und Öltemperatur, Drosselklappe, Tankfüllstand, MAF, MAP, Zündzeitpunkt, Lambdasonden, Fuel Trims, Katalysatortemperaturen, Modulspannung, absolute Last, Laufzeiten, Distanzen, Tankdruck, Verdunstungsdruck, Luftdruck, Kraftstofftyp)
- OBD-II Mode 02 (Freeze Frame inkl. Frame-Nummer), Mode 03/07/0A (DTC), Mode 09 (VIN, Calibration ID, ECU-Name)
- Korrekte PID-Unterstützungs-Bitmap: nur tatsächlich implementierte PIDs werden beworben
- Simulierte Fahrzeugdaten mit realistischer Variierung
- Foreground Service für dauerhaften Betrieb
- Live-Log zur Fehleranalyse
- Eingebaute Bluetooth-Selbsttests (Adapter, Parser, RFCOMM)

## Installation

1. APK aus dem [letzten Release](https://github.com/hendr15k/elm327-emulator/releases/latest) herunterladen
2. Auf dem Android-Gerät installieren (Installation aus unbekannten Quellen zulassen)
3. App öffnen und Berechtigungen erteilen

Ab v1.2.0 sind alle APKs mit einem festen Release-Schlüssel signiert. Frühere
Versionen entstanden mit wechselnden Debug-Schlüsseln; ein Update über eine ältere
Installation hinweg verlangt deshalb einmalig eine Deinstallation der alten App.

## Verwendung

1. ELM327 Emulator App öffnen
2. **"Start Server"** antippen
3. In der OBD-App (z.B. Torque) nach Bluetooth-Geräten suchen
4. Das Gerät erscheint als **"ELM327"**
5. Verbinden - die OBD-App empfängt nun simulierte Fahrzeugdaten

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

Für signierte Builds (Debug und Release nutzen denselben Schlüssel) liegen die
Zugangsdaten außerhalb des Repositories; `tools/gradle-signed.sh` lädt sie und
reicht alle Gradle-Argumente durch:

```bash
tools/gradle-signed.sh assembleRelease
```

Erwartete Variablen: `ELM327_KEYSTORE`, `ELM327_KEYSTORE_PASSWORD`,
`ELM327_KEY_ALIAS`, `ELM327_KEY_PASSWORD`. Ohne diese Variablen baut Gradle
weiterhin mit dem Standard-Debug-Schlüssel des Rechners.

## Tests

```bash
./gradlew testDebugUnitTest
```

Die Unit-Tests prüfen die OBD-Encodierung (jedes Payload-Byte ist ein Hex-Paar),
die AT-Kommando-Antworten, die PID-Bitmap und die Kommando-Zerlegung des
Bluetooth-Streams.

## Lizenz

MIT
