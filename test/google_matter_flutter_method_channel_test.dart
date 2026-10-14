import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:google_matter_flutter/google_matter_flutter_method_channel.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  final platform = MethodChannelGoogleMatterFlutter();
  const channel = MethodChannel('google_matter_flutter');

  final log = <MethodCall>[];

  setUp(() {
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(channel, (methodCall) async {
      log.add(methodCall);
      switch (methodCall.method) {
        case 'getDeviceData':
          return true;
        case 'discoverDevice':
          return [
            {
              'endpoint': 1,
              'deviceTypes': [256],
              'deviceTypeNames': ['On/Off Light'],
              'serverClusters': [6],
            }
          ];
        case 'performClusterAction':
          return true;
        case 'shareDevice':
          return true;
        case 'commissionDevice':
          return {
            'deviceId': 1,
            'deviceName': 'Test',
            'deviceType': 256,
            'vendorId': 1,
            'describeContents': 0,
          };
        default:
          return null;
      }
    });
  });

  tearDown(() {
    log.clear();
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(channel, null);
  });

  test('getDeviceData invokes the method channel with the right args', () async {
    final result = await platform.getDeviceData(123, 'Light');
    expect(result, isTrue);
    expect(log, hasLength(1));
    expect(log.single.method, 'getDeviceData');
    expect(log.single.arguments, [123, 'Light']);
  });

  test('discoverDevice parses the returned endpoint maps', () async {
    final endpoints = await platform.discoverDevice(123);
    expect(endpoints, hasLength(1));
    expect(endpoints.first.endpoint, 1);
    expect(endpoints.first.clusterIds, [6]);
  });

  test('performClusterAction forwards action/deviceId/endpoint/args', () async {
    final result = await platform.performClusterAction<bool>('getOnOff', 123, 1, []);
    expect(result, isTrue);
    expect(log.single.arguments, ['getOnOff', 123, 1, []]);
  });

  test('commissionDevice parses the returned device map', () async {
    final device = await platform.commissionDevice();
    expect(device?.deviceId, 1);
    expect(device?.deviceName, 'Test');
  });

  test('shareDevice forwards productId/vendorId/discriminator/passCode', () async {
    final result = await platform.shareDevice(1, 2, 3, 20250101);
    expect(result, isTrue);
    expect(log.single.method, 'shareDevice');
    expect(log.single.arguments, [1, 2, 3, 20250101]);
  });
}
