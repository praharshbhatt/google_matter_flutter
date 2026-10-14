# google_matter_flutter

[![pub package](https://img.shields.io/pub/v/google_matter_flutter.svg)](https://pub.dev/packages/google_matter_flutter)
[![License: MIT](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
![Platform: Android](https://img.shields.io/badge/platform-Android-green.svg)

A Flutter plugin that brings Google's Matter SDK to Android. Commission Matter devices, discover what they
support, and control them through a capability-based Dart API.

<p align="center">
  <img src="https://raw.githubusercontent.com/praharshbhatt/google_matter_flutter/main/assets/example_app.png" alt="Example app showing a commissioned Matter device with discovered capabilities" width="320">
</p>

> **Note**
> The plugin depends on Google's `com.google.matter:matter-android-demo-sdk` Maven artifact, which Gradle resolves
> automatically. Google provides this SDK for development and example purposes only, with no warranty, so it should
> not be used in production apps.

## Features

- **Commissioning** through Google Play services.
- **Discovery** of a commissioned device's endpoints, device types and clusters (`discoverEndpoints()`).
  Capabilities come from the Matter spec rather than from device names.
- **Control and read** common clusters:

  | Capability | Examples |
  |---|---|
  | On/Off | lights, plugs, switches |
  | Level Control | dimming |
  | Color Control | hue/saturation, color temperature |
  | Door Lock | lock, unlock, state |
  | Window Covering | blinds, shades |
  | Thermostat | heating and cooling setpoints |
  | Sensors (read-only) | temperature, humidity, illuminance, pressure, flow, occupancy, boolean state |

- **Legacy API** (`getLastStatus()` / `listenToEvents()`) is retained for backward compatibility and is deprecated
  in favor of the capability API.

## Platform support

| Platform | Supported |
|---|---|
| Android | Yes, `minSdkVersion 27` (Android 8.1+) |
| iOS | No (wraps Google's Android-only Matter SDK) |

## Limitations

- Reads are polling-based; attribute subscriptions and event reporting are not implemented.
- Groups, scenes, OTA, and specialized clusters (media, appliances, energy management) are not wired up. Fans,
  robotic vacuums and smoke/CO alarms are commissioned but expose no capabilities yet.
- To add a cluster, see `ClustersHelper.kt`; the native SDK already includes the cluster classes.

The plugin is under active development and APIs may change. Contributions are welcome.

## Testing

Dart unit tests: `flutter test`. The plugin has also been exercised on a physical Android device against Google's
Matter Virtual Device across 23 device types.

### Testing without hardware (Matter Virtual Device)

[Matter Virtual Device (MVD)](https://developers.home.google.com/matter/tools/virtual-device) is Google's desktop
app that emulates Matter devices.

1. Install and start MVD, choose a **Device Type**, and click **Create device**.
2. Run the example app on a physical Android phone (`cd example && flutter run`).
3. Tap **+**, choose **Set up without QR code**, and enter the manual pairing code shown by MVD. With MVD's default
   settings the passcode is `20202021` and the discriminator is `3840`.
4. Tap **Discover capabilities** to see the clusters the virtual device exposes, then use the action buttons.
   MVD's log shows each command it receives.

Troubleshooting:
- The phone and computer must be on the same LAN and reach each other over **IPv6**. An active VPN on the phone
  commonly causes "PASE connection failed", so disable it while commissioning.
- Make sure your firewall allows UDP 5540 (Matter) and 5353 (mDNS).
- MVD runs one device at a time and uses a different endpoint layout per type (for example the OnOff Light is on
  endpoint 13), so rely on discovery rather than assuming endpoint 1.

## Getting started

1. Add `google_matter_flutter` as a [dependency in your pubspec.yaml file](https://flutter.dev/docs/development/platform-integration/platform-channels).
2. Add the following to your `AndroidManifest.xml`:

Inside the `<activity>` tag:
```xml
<intent-filter>
    <action android:name="android.intent.action.MAIN" />
    <category android:name="android.intent.category.LAUNCHER" />
</intent-filter>
<intent-filter>
    <action android:name="com.google.android.gms.home.matter.ACTION_COMMISSION_DEVICE" />
    <category android:name="android.intent.category.DEFAULT" />
</intent-filter>
```

Inside the `<application>` tag:
```xml
<service
    android:name="com.google.google_matter_flutter.google_matter_flutter.AppCommissioningService"
    android:exported="true" />
```

## Usage example
```dart
final GoogleMatterFlutter _googleMatterFlutterPlugin = GoogleMatterFlutter();

// Commission a Matter device
final MatterDevice? device = await _googleMatterFlutterPlugin.commissionDevice();
if (device == null) return;

// Discover what the device actually supports, per the Matter spec
await device.discoverEndpoints();

if (device.hasCapability(MatterCapability.onOff)) {
    final isOn = await device.getOnOff();
    await device.toggleOnOff();
}

if (device.hasCapability(MatterCapability.levelControl)) {
    await device.setLevel(128); // 0-254
}

if (device.hasCapability(MatterCapability.doorLock)) {
    await device.lockDoor();
    final lockState = await device.getLockState();
}

if (device.hasCapability(MatterCapability.temperatureMeasurement)) {
    final tempCelsiusX100 = await device.getTemperatureMeasurement();
}
```