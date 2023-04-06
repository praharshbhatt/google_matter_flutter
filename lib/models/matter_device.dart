import 'package:google_matter_flutter/google_matter_flutter.dart';

class MatterDevice {
  late String deviceName;
  late int deviceId, deviceType, vendorId, describeContents;

  MatterDevice(
    this.deviceId,
    this.deviceName,
    this.deviceType,
    this.vendorId,
    this.describeContents,
  );

  MatterDevice.fromJson(Map json) {
    deviceId = json["deviceId"];
    deviceName = json["deviceName"];
    deviceType = json["deviceType"];
    vendorId = json["vendorId"];
    describeContents = json["describeContents"];
  }

  Map<String, dynamic> toJson() {
    return {
      "deviceId": deviceId,
      "deviceName": deviceName,
      "deviceType": deviceType,
      "vendorId": vendorId,
      "describeContents": describeContents,
    };
  }

  // Gets the last status of this device.
  Future<bool?> getLastStatus() async {
    final GoogleMatterFlutter googleMatterFlutterPlugin = GoogleMatterFlutter();

    return await googleMatterFlutterPlugin.getDeviceData(
      deviceId,
      deviceName,
    );
  }

  // Listens to the the data for this Matter device
  Stream<bool?> listenToEvents() {
    final GoogleMatterFlutter googleMatterFlutterPlugin = GoogleMatterFlutter();

    return Stream.periodic(const Duration(seconds: 10), (count) async {
      return await googleMatterFlutterPlugin.getDeviceData(
        deviceId,
        deviceName,
      );
    }).asyncMap((event) async => event);
  }
}
