import 'package:flutter_test/flutter_test.dart';
import 'package:google_matter_flutter/google_matter_flutter.dart';
import 'package:google_matter_flutter/google_matter_flutter_platform_interface.dart';
import 'package:plugin_platform_interface/plugin_platform_interface.dart';

class _Platform
    with MockPlatformInterfaceMixin
    implements GoogleMatterFlutterPlatform {
  List<MatterEndpoint> endpoints = [];
  Object? error;
  final calls = <String>[];

  @override
  Future<MatterDevice?> commissionDevice() async => null;

  @override
  Future<bool?> getDeviceData(int deviceId, String deviceName) async => null;

  @override
  Future<List<MatterEndpoint>> discoverDevice(int deviceId) async {
    if (error != null) throw error!;
    return endpoints;
  }

  @override
  Future<T?> performClusterAction<T>(String action, int deviceId, int endpoint,
      [List<dynamic> args = const []]) async {
    if (error != null) throw error!;
    calls.add('$action@$endpoint');
    return null;
  }

  @override
  Future<bool?> shareDevice(
      int productId, int vendorId, int discriminator, int passCode) async {
    if (error != null) throw error!;
    return true;
  }
}

class _Spec {
  final List<int> clusters;
  final Set<MatterCapability> expected;
  const _Spec(this.clusters, this.expected);
}

MatterEndpoint _ep(int id, List<int> clusters, [List<int> types = const []]) =>
    MatterEndpoint(
        endpoint: id,
        deviceTypeIds: types,
        deviceTypeNames: const [],
        clusterIds: clusters);

void main() {
  // device name -> (clusters, expected capabilities)
  final matrix = <String, _Spec>{
    'on/off light': _Spec([MatterClusterIds.onOff], {MatterCapability.onOff}),
    'dimmable light': _Spec(
        [MatterClusterIds.onOff, MatterClusterIds.levelControl],
        {MatterCapability.onOff, MatterCapability.levelControl}),
    'extended color light': _Spec([
      MatterClusterIds.onOff,
      MatterClusterIds.levelControl,
      MatterClusterIds.colorControl
    ], {
      MatterCapability.onOff,
      MatterCapability.levelControl,
      MatterCapability.colorControl
    }),
    'door lock':
        _Spec([MatterClusterIds.doorLock], {MatterCapability.doorLock}),
    'window covering': _Spec(
        [MatterClusterIds.windowCovering], {MatterCapability.windowCovering}),
    'thermostat':
        _Spec([MatterClusterIds.thermostat], {MatterCapability.thermostat}),
    'temperature sensor': _Spec([MatterClusterIds.temperatureMeasurement],
        {MatterCapability.temperatureMeasurement}),
    'humidity sensor': _Spec([MatterClusterIds.relativeHumidityMeasurement],
        {MatterCapability.humidityMeasurement}),
    'light sensor': _Spec([MatterClusterIds.illuminanceMeasurement],
        {MatterCapability.illuminanceMeasurement}),
    'pressure sensor': _Spec([MatterClusterIds.pressureMeasurement],
        {MatterCapability.pressureMeasurement}),
    'flow sensor': _Spec(
        [MatterClusterIds.flowMeasurement], {MatterCapability.flowMeasurement}),
    'occupancy sensor': _Spec([MatterClusterIds.occupancySensing],
        {MatterCapability.occupancySensing}),
    'contact sensor':
        _Spec([MatterClusterIds.booleanState], {MatterCapability.booleanState}),
    'fan (unsupported cluster)':
        _Spec([MatterClusterIds.fanControl], <MatterCapability>{}),
    'root-only / empty': _Spec(<int>[], <MatterCapability>{}),
    'duplicate clusters': _Spec(
        [MatterClusterIds.onOff, MatterClusterIds.onOff],
        {MatterCapability.onOff}),
  };

  late _Platform platform;
  setUp(() {
    platform = _Platform();
    GoogleMatterFlutterPlatform.instance = platform;
  });

  matrix.forEach((name, spec) {
    test('capabilities for $name', () {
      expect(_ep(1, spec.clusters).capabilities, spec.expected);
    });
  });

  test('device without discovery has no capabilities', () {
    expect(MatterDevice(1, 'x', 0, 0, 0).capabilities, isEmpty);
  });

  test('root endpoint (0) is never used as an action target', () async {
    platform.endpoints = [
      _ep(0, [MatterClusterIds.onOff]),
      _ep(3, [MatterClusterIds.onOff])
    ];
    final d = MatterDevice(1, 'x', 0, 0, 0);
    await d.discoverEndpoints();
    await d.toggleOnOff();
    expect(platform.calls, ['toggleOnOff@3']);
  });

  test('multi-endpoint bridge routes each capability to its endpoint',
      () async {
    platform.endpoints = [
      _ep(0, []),
      _ep(1, [MatterClusterIds.onOff]),
      _ep(2, [MatterClusterIds.doorLock]),
      _ep(3, [MatterClusterIds.windowCovering]),
    ];
    final d = MatterDevice(1, 'x', 0, 0, 0);
    await d.discoverEndpoints();
    await d.lockDoor();
    await d.closeWindowCovering();
    await d.toggleOnOff();
    expect(platform.calls,
        ['lockDoor@2', 'closeWindowCovering@3', 'toggleOnOff@1']);
  });

  test('platform errors propagate to the caller', () async {
    platform.error = Exception('offline');
    final d = MatterDevice(1, 'x', 0, 0, 0);
    expect(d.discoverEndpoints(), throwsException);
    expect(d.getOnOff(), throwsException);
  });

  test('MatterDevice json round trip', () {
    final d = MatterDevice(42, 'lamp', 256, 4996, 0);
    final r = MatterDevice.fromJson(d.toJson());
    expect([
      r.deviceId,
      r.deviceName,
      r.deviceType,
      r.vendorId,
      r.describeContents
    ], [
      42,
      'lamp',
      256,
      4996,
      0
    ]);
  });

  test('fromJson with missing fields throws instead of silently succeeding',
      () {
    expect(() => MatterDevice.fromJson({'deviceId': 1}), throwsA(anything));
  });

  test('endpoint parsing tolerates missing lists', () {
    final e = MatterEndpoint.fromMap({'endpoint': 1});
    expect(e.clusterIds, isEmpty);
    expect(e.capabilities, isEmpty);
  });

  test('every MatterAction has a unique wire value', () {
    final v = MatterAction.values.map((a) => a.wireValue).toList();
    expect(v.toSet().length, v.length);
  });
}
