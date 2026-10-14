import 'package:google_matter_flutter/models/matter_device.dart';
import 'package:google_matter_flutter/models/matter_endpoint.dart';
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

  /// Queries the device for its endpoints, device types and supported clusters, so capabilities
  /// can be derived from the Matter spec rather than guessed from the device's name.
  Future<List<MatterEndpoint>> discoverDevice(int deviceId) {
    throw UnimplementedError('discoverDevice() has not been implemented.');
  }

  /// Generic dispatcher for cluster reads/commands. See [MatterAction] for the supported actions.
  Future<T?> performClusterAction<T>(
    String action,
    int deviceId,
    int endpoint, [
    List<dynamic> args = const [],
  ]) {
    throw UnimplementedError('performClusterAction() has not been implemented.');
  }

  /// Opens a new commissioning window and shares the device with another ecosystem/app via
  /// Google's ShareDevice API.
  Future<bool?> shareDevice(int productId, int vendorId, int discriminator, int passCode) {
    throw UnimplementedError('shareDevice() has not been implemented.');
  }
}

