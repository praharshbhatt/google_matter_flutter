# google_matter_flutter
<?code-excerpt path-base="excerpts/packages/google_matter_flutter_example"?>

[![pub package](https://img.shields.io/pub/v/google_matter_flutter.svg)](https://pub.dev/packages/google_matter_flutter)

Flutter plugin for getting implementing Google's Matter SDKs on Android
Supports only Android for now.


![Google Matter Flutter mobile app demo](https://github.com/praharshbhatt/google_matter_flutter/blob/main/assets/Google%20Matter%20Flutter%20mobile%20app%20demo.gif?raw=true)

## Available features:
* Commission a Matter device.
* Get device events (Boolean only).


## Unavailable features:
* Get events other than Boolean.
* Have ability to send commands to the device.
* Set the device type during registration.


Please note that this plugin is still under development, and many APIs are not be available yet.
The library is also unstable and crashed when unhandled devices are commissioned.
If you can help with any of the above, please feel free to contribute to the project.


## Usage

1. To use this plugin, add `google_matter_flutter` as a [dependency in your pubspec.yaml file](https://flutter.dev/docs/development/platform-integration/platform-channels).
2. Then, download the sample-app-for-matter-android-codelab.zip from [here](https://github.com/google-home/sample-app-for-matter-android/archive/refs/heads/codelab.zip).
3. Once downloaded, extract this zip, and navigate to sample-app-for-matter-android-codelab/app.
4. You will find a folter "third_party" here. Copy and paste this folder in the android directory of your project.
5. Add the following in your AndroidManifest.xml:

Inside Activity tag:
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

Inside Application tag:
```xml
<service
    android:name="com.google.google_matter_flutter.google_matter_flutter.AppCommissioningService"
    android:exported="true" />
```

## Example
```dart
final GoogleMatterFlutter _googleMatterFlutterPlugin = GoogleMatterFlutter();

// Commission a Matter device
final MatterDevice? device = await _googleMatterFlutterPlugin.commissionDevice();

// Get device events
if (device != null) {
    device.listenToEvents().listen((event) {
        print(event);
    });
}
```