import 'package:google_matter_flutter/google_matter_flutter.dart';
import 'package:google_matter_flutter/google_matter_flutter_platform_interface.dart';

/// Represents a commissioned Matter device and provides typed, capability-aware accessors for
/// interacting with it, instead of the original single-device, name-sniffing-based API.
///
/// After commissioning, call [discoverEndpoints] once to populate [endpoints]/[capabilities] from
/// the device's actual advertised clusters (per the Matter spec), then use the capability methods
/// (e.g. [getOnOff], [setLevel], [lockDoor]) that make sense for what it reports supporting.
class MatterDevice {
  late String deviceName;
  late int deviceId, deviceType, vendorId, describeContents;

  /// Endpoints discovered via [discoverEndpoints]. Empty until that's been called at least once.
  List<MatterEndpoint> endpoints = [];

  MatterDevice(
    this.deviceId,
    this.deviceName,
    this.deviceType,
    this.vendorId,
    this.describeContents,
  );

  MatterDevice.fromJson(Map json) {
    deviceId = json["deviceId"];
    deviceName = json["deviceName"];
    deviceType = json["deviceType"];
    vendorId = json["vendorId"];
    describeContents = json["describeContents"];
  }

  Map<String, dynamic> toJson() {
    return {
      "deviceId": deviceId,
      "deviceName": deviceName,
      "deviceType": deviceType,
      "vendorId": vendorId,
      "describeContents": describeContents,
    };
  }

  /// Queries the device for its endpoints/device types/clusters and caches the result in
  /// [endpoints]. Call this once after commissioning (or whenever you want to re-check what the
  /// device currently advertises).
  Future<List<MatterEndpoint>> discoverEndpoints() async {
    endpoints = await GoogleMatterFlutterPlatform.instance.discoverDevice(deviceId);
    return endpoints;
  }

  /// The union of [MatterCapability]s across every discovered endpoint. Empty until
  /// [discoverEndpoints] has been called.
  Set<MatterCapability> get capabilities =>
      endpoints.expand((e) => e.capabilities).toSet();

  bool hasCapability(MatterCapability capability) => capabilities.contains(capability);

  /// The first endpoint (other than the root node, endpoint 0) that reports [capability], or null
  /// if none do. Most simple devices only have one non-root endpoint, but bridges/composed
  /// devices can have several.
  int? _endpointFor(MatterCapability capability) {
    for (final e in endpoints) {
      if (e.endpoint != 0 && e.capabilities.contains(capability)) return e.endpoint;
    }
    return null;
  }

  Future<T?> _performAction<T>(
    MatterAction action,
    MatterCapability capability, [
    List<dynamic> args = const [],
  ]) {
    final endpoint = _endpointFor(capability) ?? 1;
    return GoogleMatterFlutterPlatform.instance
        .performClusterAction<T>(action.wireValue, deviceId, endpoint, args);
  }

  // ---------------------------------------------------------------------------------------------
  // OnOff (lights, plugs, switches)

  Future<bool?> getOnOff() => _performAction<bool>(MatterAction.getOnOff, MatterCapability.onOff);

  Future<void> toggleOnOff() =>
      _performAction<void>(MatterAction.toggleOnOff, MatterCapability.onOff);

  // ---------------------------------------------------------------------------------------------
  // LevelControl (dimmable lights/plugs)

  /// Current brightness level, 0-254.
  Future<int?> getLevel() =>
      _performAction<int>(MatterAction.getLevel, MatterCapability.levelControl);

  /// Sets brightness level, 0-254.
  Future<void> setLevel(int level) =>
      _performAction<void>(MatterAction.setLevel, MatterCapability.levelControl, [level]);

  // ---------------------------------------------------------------------------------------------
  // ColorControl (color/color-temperature lights)

  Future<void> setHueAndSaturation(int hue, int saturation) => _performAction<void>(
        MatterAction.setHueAndSaturation,
        MatterCapability.colorControl,
        [hue, saturation],
      );

  /// [colorTemperatureMireds] is the mired color temperature (roughly 1,000,000 / kelvin).
  Future<void> setColorTemperature(int colorTemperatureMireds) => _performAction<void>(
        MatterAction.setColorTemperature,
        MatterCapability.colorControl,
        [colorTemperatureMireds],
      );

  // ---------------------------------------------------------------------------------------------
  // DoorLock

  Future<void> lockDoor() =>
      _performAction<void>(MatterAction.lockDoor, MatterCapability.doorLock);

  Future<void> unlockDoor() =>
      _performAction<void>(MatterAction.unlockDoor, MatterCapability.doorLock);

  /// The DoorLock cluster's LockState attribute (1 = Locked, 2 = Unlocked, 3 = NotFullyLocked).
  Future<int?> getLockState() =>
      _performAction<int>(MatterAction.getLockState, MatterCapability.doorLock);

  // ---------------------------------------------------------------------------------------------
  // WindowCovering (blinds/shades/curtains)

  Future<void> openWindowCovering() =>
      _performAction<void>(MatterAction.openWindowCovering, MatterCapability.windowCovering);

  Future<void> closeWindowCovering() =>
      _performAction<void>(MatterAction.closeWindowCovering, MatterCapability.windowCovering);

  /// [liftPercent100ths] is 0-10000 (0 = fully open, 10000 = fully closed).
  Future<void> setWindowCoveringLiftPosition(int liftPercent100ths) => _performAction<void>(
        MatterAction.setWindowCoveringLiftPosition,
        MatterCapability.windowCovering,
        [liftPercent100ths],
      );

  // ---------------------------------------------------------------------------------------------
  // Thermostat

  /// Local temperature in hundredths of a degree Celsius, e.g. 2100 = 21.00C.
  Future<int?> getThermostatLocalTemperature() => _performAction<int>(
        MatterAction.getThermostatLocalTemperature,
        MatterCapability.thermostat,
      );

  /// [heatingSetpointCelsiusX100] is in hundredths of a degree Celsius, e.g. 2100 = 21.00C.
  Future<void> setThermostatHeatingSetpoint(int heatingSetpointCelsiusX100) => _performAction<void>(
        MatterAction.setThermostatHeatingSetpoint,
        MatterCapability.thermostat,
        [heatingSetpointCelsiusX100],
      );

  /// [coolingSetpointCelsiusX100] is in hundredths of a degree Celsius, e.g. 2400 = 24.00C.
  Future<void> setThermostatCoolingSetpoint(int coolingSetpointCelsiusX100) => _performAction<void>(
        MatterAction.setThermostatCoolingSetpoint,
        MatterCapability.thermostat,
        [coolingSetpointCelsiusX100],
      );

  // ---------------------------------------------------------------------------------------------
  // Sensors (read-only)

  /// Hundredths of a degree Celsius.
  Future<int?> getTemperatureMeasurement() => _performAction<int>(
        MatterAction.getTemperatureMeasurement,
        MatterCapability.temperatureMeasurement,
      );

  /// Hundredths of a percent, 0-10000.
  Future<int?> getRelativeHumidityMeasurement() => _performAction<int>(
        MatterAction.getRelativeHumidityMeasurement,
        MatterCapability.humidityMeasurement,
      );

  /// Encoded per spec: 10000*log10(lux)+1.
  Future<int?> getIlluminanceMeasurement() => _performAction<int>(
        MatterAction.getIlluminanceMeasurement,
        MatterCapability.illuminanceMeasurement,
      );

  /// kPa.
  Future<int?> getPressureMeasurement() => _performAction<int>(
        MatterAction.getPressureMeasurement,
        MatterCapability.pressureMeasurement,
      );

  /// Tenths of m/h.
  Future<int?> getFlowMeasurement() =>
      _performAction<int>(MatterAction.getFlowMeasurement, MatterCapability.flowMeasurement);

  /// Occupancy bitmap (bit 0: occupied).
  Future<int?> getOccupancy() =>
      _performAction<int>(MatterAction.getOccupancy, MatterCapability.occupancySensing);

  /// Generic boolean sensor state (e.g. contact sensors, water leak detectors).
  Future<bool?> getContactState() =>
      _performAction<bool>(MatterAction.getContactState, MatterCapability.booleanState);

  // ---------------------------------------------------------------------------------------------
  // Deprecated legacy API (kept for backwards compatibility with the original single-device
  // integration this plugin was built for). Prefer [discoverEndpoints] + the capability-specific
  // methods above for anything new.

  /// Gets the last status of this device using the original name-based heuristic
  /// (occupancy/PIR -> bitmap cluster, door/contact -> boolean cluster, else -> on/off cluster).
  @Deprecated('Use discoverEndpoints() + the capability-specific getters instead.')
  Future<bool?> getLastStatus() async {
    final GoogleMatterFlutter googleMatterFlutterPlugin = GoogleMatterFlutter();

    return await googleMatterFlutterPlugin.getDeviceData(
      deviceId,
      deviceName,
    );
  }

  /// Listens to the the data for this Matter device using the original name-based heuristic.
  @Deprecated(
      'Poll the capability-specific getters (e.g. getOnOff()) on your own schedule instead.')
  Stream<bool?> listenToEvents() {
    final GoogleMatterFlutter googleMatterFlutterPlugin = GoogleMatterFlutter();

    return Stream.periodic(const Duration(seconds: 10), (count) async {
      return await googleMatterFlutterPlugin.getDeviceData(
        deviceId,
        deviceName,
      );
    }).asyncMap((event) async => event);
  }
}
