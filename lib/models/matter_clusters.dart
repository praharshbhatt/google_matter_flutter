/// Well-known Matter cluster IDs (see the Matter Application Cluster spec), used to determine
/// which capabilities/actions a given endpoint supports based on its advertised server cluster
/// list (see [MatterEndpoint.clusterIds]).
///
/// This is not an exhaustive list of every cluster in the spec — just the ones this plugin knows
/// how to act on today. Extend this (and the corresponding native `ClustersHelper`/`GoogleMatter`
/// Kotlin code) as more clusters get supported.
class MatterClusterIds {
  MatterClusterIds._();

  static const int identify = 0x0003;
  static const int groups = 0x0004;
  static const int scenes = 0x0005;
  static const int onOff = 0x0006;
  static const int levelControl = 0x0008;
  static const int descriptor = 0x001D;
  static const int basicInformation = 0x0028;
  static const int colorControl = 0x0300;
  static const int doorLock = 0x0101;
  static const int windowCovering = 0x0102;
  static const int thermostat = 0x0201;
  static const int fanControl = 0x0202;
  static const int temperatureMeasurement = 0x0402;
  static const int pressureMeasurement = 0x0403;
  static const int flowMeasurement = 0x0404;
  static const int relativeHumidityMeasurement = 0x0405;
  static const int occupancySensing = 0x0406;
  static const int illuminanceMeasurement = 0x0400;
  static const int booleanState = 0x0045;
  static const int administratorCommissioning = 0x003C;
}
