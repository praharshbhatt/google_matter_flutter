import 'package:flutter_test/flutter_test.dart';
import 'package:google_matter_flutter/google_matter_flutter.dart';
import 'package:google_matter_flutter/google_matter_flutter_platform_interface.dart';
import 'package:google_matter_flutter/google_matter_flutter_method_channel.dart';
import 'package:plugin_platform_interface/plugin_platform_interface.dart';

class MockGoogleMatterFlutterPlatform
    with MockPlatformInterfaceMixin
    implements GoogleMatterFlutterPlatform {

  @override
  Future<String?> getPlatformVersion() => Future.value('42');
}

void main() {
  final GoogleMatterFlutterPlatform initialPlatform = GoogleMatterFlutterPlatform.instance;

  test('$MethodChannelGoogleMatterFlutter is the default instance', () {
    expect(initialPlatform, isInstanceOf<MethodChannelGoogleMatterFlutter>());
  });

  test('getPlatformVersion', () async {
    GoogleMatterFlutter googleMatterFlutterPlugin = GoogleMatterFlutter();
    MockGoogleMatterFlutterPlatform fakePlatform = MockGoogleMatterFlutterPlatform();
    GoogleMatterFlutterPlatform.instance = fakePlatform;

    expect(await googleMatterFlutterPlugin.getPlatformVersion(), '42');
  });
}
