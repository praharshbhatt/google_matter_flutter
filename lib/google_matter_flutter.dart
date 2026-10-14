import 'package:google_matter_flutter/models/matter_device.dart';
export 'package:google_matter_flutter/models/matter_device.dart';
export 'package:google_matter_flutter/models/matter_endpoint.dart';
export 'package:google_matter_flutter/models/matter_device_types.dart';
export 'package:google_matter_flutter/models/matter_clusters.dart';
export 'package:google_matter_flutter/models/matter_action.dart';
import 'package:google_matter_flutter/models/matter_endpoint.dart';
import 'google_matter_flutter_platform_interface.dart';

class GoogleMatterFlutter {
  Future<MatterDevice?> commissionDevice() {
    return GoogleMatterFlutterPlatform.instance.commissionDevice();
  }

  Future<bool?> getDeviceData(int deviceId, String deviceName) {
    return GoogleMatterFlutterPlatform.instance.getDeviceData(deviceId, deviceName);
  }

  /// Discovers the endpoints, device types and clusters supported by [deviceId]. Use this right
  /// after commissioning to figure out what capabilities/UI to show for the device, instead of
  /// relying on its name.
  Future<List<MatterEndpoint>> discoverDevice(int deviceId) {
    return GoogleMatterFlutterPlatform.instance.discoverDevice(deviceId);
  }

  /// Opens a new commissioning window on an already-commissioned device and shares it with
  /// another ecosystem/app (e.g. a secondary smart home app), using Google's ShareDevice API.
  /// Contributed by Marco Lettieri (microbees.com).
  Future<bool?> shareDevice(int productId, int vendorId, int discriminator, int passCode) {
    return GoogleMatterFlutterPlatform.instance.shareDevice(
      productId,
      vendorId,
      discriminator,
      passCode,
    );
  }
}
