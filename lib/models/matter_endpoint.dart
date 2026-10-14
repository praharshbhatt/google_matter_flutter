import 'package:google_matter_flutter/models/matter_clusters.dart';
import 'package:google_matter_flutter/models/matter_device_types.dart';

/// Describes one Matter endpoint discovered on a commissioned device: which device type(s) it
/// declares, and which server clusters (capabilities) it exposes.
class MatterEndpoint {
  final int endpoint;
  final List<int> deviceTypeIds;
  final List<String> deviceTypeNames;
  final List<int> clusterIds;

  const MatterEndpoint({
    required this.endpoint,
    required this.deviceTypeIds,
    required this.deviceTypeNames,
    required this.clusterIds,
  });

  factory MatterEndpoint.fromMap(Map<dynamic, dynamic> map) {
    return MatterEndpoint(
      endpoint: map["endpoint"] as int,
      deviceTypeIds: (map["deviceTypes"] as List? ?? [])
          .map((e) => (e as num).toInt())
          .toList(),
      deviceTypeNames: (map["deviceTypeNames"] as List? ?? [])
          .map((e) => e.toString())
          .toList(),
      clusterIds: (map["serverClusters"] as List? ?? [])
          .map((e) => (e as num).toInt())
          .toList(),
    );
  }

  /// Derives the set of user-facing [MatterCapability]s this endpoint supports, based on its
  /// advertised cluster IDs. This is the mechanism that replaces the old device-name-string
  /// sniffing (`deviceName.contains("occupancy")`, etc.) with a spec-driven approach that works
  /// for any Matter device, not just the one this plugin originally shipped with.
  Set<MatterCapability> get capabilities {
    final caps = <MatterCapability>{};
    for (final clusterId in clusterIds) {
      switch (clusterId) {
        case MatterClusterIds.onOff:
          caps.add(MatterCapability.onOff);
          break;
        case MatterClusterIds.levelControl:
          caps.add(MatterCapability.levelControl);
          break;
        case MatterClusterIds.colorControl:
          caps.add(MatterCapability.colorControl);
          break;
        case MatterClusterIds.doorLock:
          caps.add(MatterCapability.doorLock);
          break;
        case MatterClusterIds.windowCovering:
          caps.add(MatterCapability.windowCovering);
          break;
        case MatterClusterIds.thermostat:
          caps.add(MatterCapability.thermostat);
          break;
        case MatterClusterIds.temperatureMeasurement:
          caps.add(MatterCapability.temperatureMeasurement);
          break;
        case MatterClusterIds.relativeHumidityMeasurement:
          caps.add(MatterCapability.humidityMeasurement);
          break;
        case MatterClusterIds.illuminanceMeasurement:
          caps.add(MatterCapability.illuminanceMeasurement);
          break;
        case MatterClusterIds.pressureMeasurement:
          caps.add(MatterCapability.pressureMeasurement);
          break;
        case MatterClusterIds.flowMeasurement:
          caps.add(MatterCapability.flowMeasurement);
          break;
        case MatterClusterIds.occupancySensing:
          caps.add(MatterCapability.occupancySensing);
          break;
        case MatterClusterIds.booleanState:
          caps.add(MatterCapability.booleanState);
          break;
      }
    }
    return caps;
  }
}
