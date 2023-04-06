import 'package:google_matter_flutter/models/matter_device.dart';
import 'package:plugin_platform_interface/plugin_platform_interface.dart';

import 'google_matter_flutter_method_channel.dart';

abstract class GoogleMatterFlutterPlatform extends PlatformInterface {
  /// Constructs a GoogleMatterFlutterPlatform.
  GoogleMatterFlutterPlatform() : super(token: _token);

  static final Object _token = Object();

  static GoogleMatterFlutterPlatform _instance = MethodChannelGoogleMatterFlutter();

  /// The default instance of [GoogleMatterFlutterPlatform] to use.
  ///
  /// Defaults to [MethodChannelGoogleMatterFlutter].
  static GoogleMatterFlutterPlatform get instance => _instance;

  /// Platform-specific implementations should set this with their own
  /// platform-specific class that extends [GoogleMatterFlutterPlatform] when
  /// they register themselves.
  static set instance(GoogleMatterFlutterPlatform instance) {
    PlatformInterface.verifyToken(instance, _token);
    _instance = instance;
  }

  Future<MatterDevice?> commissionDevice() {
    throw UnimplementedError('commissionDevice() has not been implemented.');
  }

  Future<bool?> getDeviceData(int deviceId, String deviceName) {
    throw UnimplementedError('getDeviceData() has not been implemented.');
  }
}
