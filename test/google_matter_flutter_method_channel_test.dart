import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:google_matter_flutter/google_matter_flutter_method_channel.dart';

void main() {
  MethodChannelGoogleMatterFlutter platform = MethodChannelGoogleMatterFlutter();
  const MethodChannel channel = MethodChannel('google_matter_flutter');

  TestWidgetsFlutterBinding.ensureInitialized();

  setUp(() {
    channel.setMockMethodCallHandler((MethodCall methodCall) async {
      return '42';
    });
  });

  tearDown(() {
    channel.setMockMethodCallHandler(null);
  });

  test('getPlatformVersion', () async {
    expect(await platform.getPlatformVersion(), '42');
  });
}
