import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';
import 'package:google_matter_flutter/models/matter_device.dart';

import 'google_matter_flutter_platform_interface.dart';

/// An implementation of [GoogleMatterFlutterPlatform] that uses method channels.
class MethodChannelGoogleMatterFlutter extends GoogleMatterFlutterPlatform {
  /// The method channel used to interact with the native platform.
  @visibleForTesting
  final methodChannel = const MethodChannel('google_matter_flutter');

  @override
  Future<MatterDevice?> commissionDevice() async {
    final device = await methodChannel.invokeMethod<Map>('commissionDevice');
    return device != null ? MatterDevice.fromJson(device) : null;
  }

  @override
  Future<bool?> getDeviceData(int deviceId, String deviceName) async {
    final deviceData = await methodChannel.invokeMethod<bool>('getDeviceData', [
      deviceId,
      deviceName,
    ]);
    return deviceData;
  }

  Future<bool?> shareDevice(
      int productId, int vendorId, int discriminator, int passCode) async {
    final succeed = await methodChannel.invokeMethod<bool>(
        'shareDevice', [productId, vendorId, discriminator, passCode]);
    return succeed;
  }
}
