## 0.2.0

* Added `GoogleMatterFlutter.shareDevice()`, allowing an already-commissioned device to be shared
  with another ecosystem/app by opening a new commissioning window via Google's `ShareDevice` API
  (contributed by Marco Lettieri, microbees.com; re-integrated on top of the singleton `ChipClient`
  and nullable `methodCallResult` introduced below).
* **BREAKING:** Removed the direct `shared_preferences` dependency from this plugin's own
  `pubspec.yaml`. If your app relied on getting `shared_preferences` transitively through this
  plugin without declaring it yourself, add it directly to your own `pubspec.yaml`.
* Removed the unused `hilt-android` dependency and its inert `@Inject`/`@Singleton`/
  `@AndroidEntryPoint`/`@ApplicationContext` annotations (the Hilt Gradle plugin/annotation
  processor was never actually applied, so they did nothing but add ~2MB of dead weight and
  mislead readers into thinking a real DI graph was wired up).
* Fixed `ChipClient` (and transitively `AppCommissioningService` and `GoogleMatter`'s
  `getDeviceData()`/`discoverDevice()`/`performClusterAction()`) to share one process-wide
  `ChipDeviceController` instance via a manual singleton (`ChipClient.getInstance()`) instead of
  constructing a brand new one - reloading the native CHIP JNI library and recreating the BLE
  manager/mDNS resolver/browser - on every single call. This was a latent performance/resource
  leak that got worse the more a device's state was polled.
* Replaced deprecated `String.toLowerCase()` calls with `lowercase()` in `GoogleMatter.kt`.
* Cleaned up the bundled example app: converted `Key? key` constructor to a super parameter, and
  documented/silenced the intentional `listenToEvents()` deprecation warning instead of leaving
  an unexplained analyzer hit.
* Clarified the package `description` in `pubspec.yaml` for the pub.dev listing.
* Replaced every raw `print()`/`println()` call on the native Android side with a small
  `MatterLog` helper that only logs (via `android.util.Log`, tagged `GoogleMatterFlutter`) when
  this module is built with `BuildConfig.DEBUG == true`, so apps depending on this plugin no
  longer get unconditional stdout/logcat spam in release builds.
* Removed a leftover raw `print()` from the example app in favor of `debugPrint()`.
* Massively expanded device/cluster support, replacing the original single-device,
  name-sniffing-based integration with a spec-driven capability model:
  * Added `MatterDevice.discoverEndpoints()`, backed by a new native `discoverDevice()` method
    channel call, which queries the device's actual endpoints, device types and server clusters
    (via the existing `ClustersHelper.fetchDeviceMatterInfo`) instead of guessing behavior from
    the device's name string.
  * Added `MatterEndpoint` and `MatterCapability` (on/off, level control, color control, door
    lock, window covering, thermostat, and five read-only sensor types) to represent what a
    device supports, derived directly from its advertised cluster IDs per the Matter Application
    Cluster spec.
  * Added native cluster helpers (`ClustersHelper.kt`) and a generic `performClusterAction()`
    method channel dispatch (`GoogleMatter.kt` / `GoogleMatterFlutterPlugin.kt`) covering:
    `LevelControlCluster` (get/set brightness), `ColorControlCluster` (hue/saturation, color
    temperature), `DoorLockCluster` (lock/unlock/state), `WindowCoveringCluster` (open/close/lift
    position), `ThermostatCluster` (local temperature, heating/cooling setpoints), and read-only
    `TemperatureMeasurementCluster`, `RelativeHumidityMeasurementCluster`,
    `IlluminanceMeasurementCluster`, `PressureMeasurementCluster`, `FlowMeasurementCluster`.
  * Added corresponding typed Dart methods on `MatterDevice` (`getOnOff`/`toggleOnOff`,
    `getLevel`/`setLevel`, `setHueAndSaturation`/`setColorTemperature`,
    `lockDoor`/`unlockDoor`/`getLockState`, `openWindowCovering`/`closeWindowCovering`/
    `setWindowCoveringLiftPosition`, thermostat setpoint getters/setters, and sensor getters).
  * Expanded `MatterConstants.DeviceTypesMap` (native) with ~50 common Matter device type IDs
    (lights, plugs, sensors, locks, window coverings, thermostats, appliances, etc), sourced from
    the upstream `connectedhomeip` Matter Device Library spec.
* Fixed `GoogleMatterFlutter.commissionDevice()`/`getDeviceData()` to go through
  `GoogleMatterFlutterPlatform.instance` (the standard plugin-platform-interface pattern) instead
  of hardcoding `MethodChannelGoogleMatterFlutter()` directly — this was a latent bug that broke
  test/platform mocking for those two methods.
* The original `getLastStatus()`/`listenToEvents()` boolean-polling API is preserved and marked
  `@Deprecated` (not removed) for backwards compatibility with existing integrations.
* Removed a leftover reference to an internal/unrelated project name from the workspace root
  README; genericized the wording instead.
* Added unit tests for capability derivation, endpoint-based action dispatch and method channel
  argument marshalling.

## 0.1.0

* **BREAKING:** `minSdkVersion` raised from `16` to `27` (Android 8.1+), required by the newer
  Matter/Home Mobile SDK APIs. Apps targeting older devices must bump their own `minSdkVersion`
  before taking this dependency.
* **BREAKING:** Android toolchain raised to AGP `8.2.0`, Kotlin `1.9.20`, Java/Kotlin bytecode
  target `17`, and `compileSdk 34` (from AGP `7.1.2`, Kotlin `1.6.10`, Java `1.8`, `compileSdk
  31`). Consuming apps need a compatible Gradle/AGP/Kotlin setup.
* Replaced the manually-bundled `third_party/connectedhomeip` native binaries
  (`CHIPController.jar`, `SetupPayloadParser.jar`, per-ABI `.so` libs) with Google's officially
  published `com.google.matter:matter-android-demo-sdk:1.0` Maven artifact, following the
  [2024 Google Home Sample App for Matter (sample-apps-for-matter-android, v2.0.0)](https://github.com/google-home/sample-apps-for-matter-android).
  Consumers no longer need to manually download/copy any native libraries.
* Updated `ClustersHelper` for renamed/changed CHIP APIs in the newer Matter SDK:
  `ChipClusters.BasicCluster` -> `ChipClusters.BasicInformationCluster`, and
  `ChipStructs.DescriptorClusterDeviceTypeStruct.type` -> `.deviceType`.
* Added error handling (`CommissioningService.CommissioningError`) to
  `AppCommissioningService` when PASE/commissioning fails, instead of silently swallowing it.
* Bumped `play-services-base` 18.1.0 -> 18.2.0, `hilt-android` 2.41 -> 2.48.1,
  `shared_preferences` ^2.0.17 -> ^2.3.2, `plugin_platform_interface` ^2.1.3 -> ^2.1.7.
* Removed unused `androidx.datastore`/`protobuf-javalite` dependencies (dead weight carried
  over from the original codelab, never actually used by this plugin).
* Added AGP-8-required `namespace` to `build.gradle` and removed the now-deprecated `package`
  attribute from `AndroidManifest.xml`.
* Verified the plugin's bundled `example/` app and the standalone `google_matter_flutter_example`
  app both build a debug APK successfully end-to-end with this new stack.

## 0.0.1

* Initial release.
* Integrate google Matter lib following the [codelab from Google](https://developers.home.google.com/codelabs/matter-sample-app?hl=en#0)
