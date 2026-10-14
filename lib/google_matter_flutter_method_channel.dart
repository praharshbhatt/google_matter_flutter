import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';
import 'package:google_matter_flutter/models/matter_device.dart';
import 'package:google_matter_flutter/models/matter_endpoint.dart';

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

  @override
  Future<List<MatterEndpoint>> discoverDevice(int deviceId) async {
    final endpoints = await methodChannel.invokeMethod<List>('discoverDevice', [deviceId]);
    if (endpoints == null) return [];
    return endpoints
        .cast<Map>()
        .map((endpointMap) => MatterEndpoint.fromMap(endpointMap))
        .toList();
  }

  @override
  Future<T?> performClusterAction<T>(
    String action,
    int deviceId,
    int endpoint, [
    List<dynamic> args = const [],
  ]) async {
    return await methodChannel.invokeMethod<T>('performClusterAction', [
      action,
      deviceId,
      endpoint,
      args,
    ]);
  }

  @override
  Future<bool?> shareDevice(int productId, int vendorId, int discriminator, int passCode) async {
    return await methodChannel.invokeMethod<bool>('shareDevice', [
      productId,
      vendorId,
      discriminator,
      passCode,
    ]);
  }
}

