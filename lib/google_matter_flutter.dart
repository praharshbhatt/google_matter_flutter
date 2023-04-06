import 'package:google_matter_flutter/models/matter_device.dart';
export 'package:google_matter_flutter/models/matter_device.dart';
import 'google_matter_flutter_method_channel.dart';

class GoogleMatterFlutter {
  Future<MatterDevice?> commissionDevice() {
    return MethodChannelGoogleMatterFlutter().commissionDevice();
  }

  Future<bool?> getDeviceData(int deviceId, String deviceName) {
    return MethodChannelGoogleMatterFlutter().getDeviceData(deviceId, deviceName);
  }
}
