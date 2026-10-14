/// Human-readable names for common Matter device type IDs, mirroring the native
/// `MatterConstants.DeviceTypesMap` in the Android plugin (kept in sync manually since Dart and
/// Kotlin can't easily share a single source of truth here).
class MatterDeviceTypeIds {
  MatterDeviceTypeIds._();

  static const int rootNode = 0x0016;
  static const int aggregator = 0x000E;
  static const int bridgedNode = 0x0013;

  static const int onOffLight = 0x0100;
  static const int dimmableLight = 0x0101;
  static const int colorTemperatureLight = 0x010C;
  static const int extendedColorLight = 0x010D;
  static const int onOffLightSwitch = 0x0103;
  static const int dimmerSwitch = 0x0104;
  static const int colorDimmerSwitch = 0x0105;
  static const int genericSwitch = 0x000F;

  static const int onOffPlugInUnit = 0x010A;
  static const int dimmablePlugInUnit = 0x010B;

  static const int contactSensor = 0x0015;
  static const int lightSensor = 0x0106;
  static const int occupancySensor = 0x0107;
  static const int temperatureSensor = 0x0302;
  static const int pressureSensor = 0x0305;
  static const int flowSensor = 0x0306;
  static const int humiditySensor = 0x0307;
  static const int onOffSensor = 0x0850;
  static const int waterLeakDetector = 0x0043;
  static const int smokeCoAlarm = 0x0076;
  static const int airQualitySensor = 0x002C;

  static const int doorLock = 0x000A;
  static const int doorLockController = 0x000B;
  static const int windowCovering = 0x0202;
  static const int windowCoveringController = 0x0203;

  static const int thermostat = 0x0301;
  static const int fan = 0x002B;
  static const int roomAirConditioner = 0x0072;
  static const int waterValve = 0x0042;
  static const int heatPump = 0x0309;
  static const int waterHeater = 0x050F;

  static const int pump = 0x0303;
  static const int pumpController = 0x0304;
}

/// The set of user-facing capabilities this plugin can expose for a device, derived from which
/// clusters its endpoints advertise. A single [MatterDevice] can have more than one capability
/// (for example, an extended-color light has [onOff], [levelControl] AND [colorControl]).
enum MatterCapability {
  /// Simple on/off toggle (lights, plugs, switches). Cluster 0x0006.
  onOff,

  /// Brightness/dimming level, 0-254. Cluster 0x0008.
  levelControl,

  /// Hue/saturation or color-temperature control. Cluster 0x0300.
  colorControl,

  /// Lock/unlock and lock-state reporting. Cluster 0x0101.
  doorLock,

  /// Open/close and lift-position control for blinds/shades/curtains. Cluster 0x0102.
  windowCovering,

  /// Heating/cooling setpoints and local temperature. Cluster 0x0201.
  thermostat,

  /// Ambient temperature reading. Cluster 0x0402.
  temperatureMeasurement,

  /// Relative humidity reading. Cluster 0x0405.
  humidityMeasurement,

  /// Ambient light level reading. Cluster 0x0400.
  illuminanceMeasurement,

  /// Barometric pressure reading. Cluster 0x0403.
  pressureMeasurement,

  /// Fluid/air flow reading. Cluster 0x0404.
  flowMeasurement,

  /// PIR/ultrasonic occupancy reading. Cluster 0x0406.
  occupancySensing,

  /// Generic boolean sensor state (contact, water leak, etc). Cluster 0x0045.
  booleanState,
}
