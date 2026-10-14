import 'package:flutter_test/flutter_test.dart';
import 'package:google_matter_flutter/google_matter_flutter.dart';
import 'package:google_matter_flutter/google_matter_flutter_platform_interface.dart';
import 'package:plugin_platform_interface/plugin_platform_interface.dart';

class MockGoogleMatterFlutterPlatform
    with MockPlatformInterfaceMixin
    implements GoogleMatterFlutterPlatform {
  MatterDevice? commissionedDevice;
  List<MatterEndpoint> endpointsToReturn = [];
  final calls = <String>[];

  @override
  Future<MatterDevice?> commissionDevice() async => commissionedDevice;

  @override
  Future<bool?> getDeviceData(int deviceId, String deviceName) async => true;

  @override
  Future<List<MatterEndpoint>> discoverDevice(int deviceId) async => endpointsToReturn;

  @override
  Future<T?> performClusterAction<T>(
    String action,
    int deviceId,
    int endpoint, [
    List<dynamic> args = const [],
  ]) async {
    calls.add('$action@$endpoint(${args.join(",")})');
    return null;
  }

  @override
  Future<bool?> shareDevice(
      int productId, int vendorId, int discriminator, int passCode) async {
    calls.add('shareDevice($productId,$vendorId,$discriminator,$passCode)');
    return true;
  }
}

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();
  final GoogleMatterFlutterPlatform initialPlatform = GoogleMatterFlutterPlatform.instance;

  test('MethodChannelGoogleMatterFlutter is the default instance', () {
    expect(initialPlatform, isInstanceOf<GoogleMatterFlutterPlatform>());
  });

  group('GoogleMatterFlutter', () {
    late MockGoogleMatterFlutterPlatform fakePlatform;

    setUp(() {
      fakePlatform = MockGoogleMatterFlutterPlatform();
      GoogleMatterFlutterPlatform.instance = fakePlatform;
    });

    test('commissionDevice returns what the platform provides', () async {
      fakePlatform.commissionedDevice = MatterDevice(123, 'Light', 256, 1, 0);
      final device = await GoogleMatterFlutter().commissionDevice();
      expect(device?.deviceId, 123);
    });

    test('discoverDevice forwards to the platform', () async {
      fakePlatform.endpointsToReturn = [
        const MatterEndpoint(
          endpoint: 1,
          deviceTypeIds: [0x0100],
          deviceTypeNames: ['On/Off Light'],
          clusterIds: [0x0006],
        ),
      ];
      final endpoints = await GoogleMatterFlutter().discoverDevice(123);
      expect(endpoints, hasLength(1));
      expect(endpoints.first.capabilities, contains(MatterCapability.onOff));
    });

    test('shareDevice forwards to the platform', () async {
      final result = await GoogleMatterFlutter().shareDevice(1, 2, 3, 456);
      expect(result, isTrue);
      expect(fakePlatform.calls.single, 'shareDevice(1,2,3,456)');
    });
  });

  group('MatterDevice capability dispatch', () {
    late MockGoogleMatterFlutterPlatform fakePlatform;
    late MatterDevice device;

    setUp(() {
      fakePlatform = MockGoogleMatterFlutterPlatform();
      GoogleMatterFlutterPlatform.instance = fakePlatform;
      device = MatterDevice(42, 'Test Device', 0x0100, 1, 0);
    });

    test('picks the endpoint that actually reports the capability', () async {
      await device.discoverEndpoints(); // no-op with empty endpoints by default
      fakePlatform.endpointsToReturn = [
        const MatterEndpoint(
          endpoint: 0,
          deviceTypeIds: [0x0016],
          deviceTypeNames: ['Root Node'],
          clusterIds: [],
        ),
        const MatterEndpoint(
          endpoint: 2,
          deviceTypeIds: [0x000A],
          deviceTypeNames: ['Door Lock'],
          clusterIds: [0x0101],
        ),
      ];
      await device.discoverEndpoints();
      expect(device.hasCapability(MatterCapability.doorLock), isTrue);

      await device.lockDoor();
      expect(fakePlatform.calls, contains('lockDoor@2()'));
    });

    test('falls back to endpoint 1 when capability was never discovered', () async {
      await device.toggleOnOff();
      expect(fakePlatform.calls, contains('toggleOnOff@1()'));
    });

    test('setLevel passes the level through as an argument', () async {
      await device.setLevel(128);
      expect(fakePlatform.calls, contains('setLevel@1(128)'));
    });
  });

  group('MatterEndpoint.capabilities', () {
    test('derives capabilities from cluster IDs, not the device name', () {
      const endpoint = MatterEndpoint(
        endpoint: 1,
        deviceTypeIds: [0x010D], // Extended Color Light
        deviceTypeNames: ['Extended Color Light'],
        clusterIds: [0x0006, 0x0008, 0x0300], // OnOff, LevelControl, ColorControl
      );

      expect(
        endpoint.capabilities,
        {
          MatterCapability.onOff,
          MatterCapability.levelControl,
          MatterCapability.colorControl,
        },
      );
    });

    test('unknown cluster IDs are ignored rather than throwing', () {
      const endpoint = MatterEndpoint(
        endpoint: 1,
        deviceTypeIds: [0x0016],
        deviceTypeNames: ['Root Node'],
        clusterIds: [0x0003, 0x001D], // Identify, Descriptor - no capability mapping
      );

      expect(endpoint.capabilities, isEmpty);
    });
  });
}
