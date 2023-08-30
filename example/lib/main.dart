import 'dart:async';
import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:google_matter_flutter/google_matter_flutter.dart';
import 'package:shared_preferences/shared_preferences.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatefulWidget {
  const MyApp({Key? key}) : super(key: key);

  @override
  State<MyApp> createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  final GoogleMatterFlutter _googleMatterFlutterPlugin = GoogleMatterFlutter();
  bool isCommissioningInProgress = false;

  // Loads the commissioned Matter devices
  Future<List<MatterDevice>> loadMatterDevices() async {
    final prefs = await SharedPreferences.getInstance();
    final List matterDevicesList =
        jsonDecode(prefs.getString("matterDevices") ?? "[]");

    return matterDevicesList
        .map((e) => MatterDevice.fromJson(e as Map))
        .toList();
  }

  // Save this commissioned matter device
  Future addMatterDevice(MatterDevice matterDevice) async {
    final prefs = await SharedPreferences.getInstance();
    final List<MatterDevice> matterDevices = await loadMatterDevices();
    matterDevices.add(matterDevice);
    final List matterDevicesList =
        matterDevices.map((e) => e.toJson()).toList();
    prefs.setString("matterDevices", jsonEncode(matterDevicesList));
  }

  // Remove this commissioned matter device
  Future removeMatterDevice(MatterDevice matterDevice) async {
    final prefs = await SharedPreferences.getInstance();
    final List<MatterDevice> matterDevices = await loadMatterDevices();
    matterDevices
        .removeWhere((device) => device.deviceId == matterDevice.deviceId);
    final List matterDevicesList =
        matterDevices.map((e) => e.toJson()).toList();
    prefs.setString("matterDevices", jsonEncode(matterDevicesList));
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      theme: ThemeData(
        useMaterial3: true,
        colorScheme: ColorScheme.fromSwatch(
          brightness: Brightness.dark,
          primarySwatch: Colors.deepPurple,
        ),
      ),
      darkTheme: ThemeData(
        useMaterial3: true,
        colorScheme: ColorScheme.fromSwatch(
          brightness: Brightness.dark,
          primarySwatch: Colors.deepPurple,
        ),
      ),
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Google Matter example app'),
        ),
        body: FutureBuilder<List<MatterDevice>>(
          future: loadMatterDevices(),
          builder: (BuildContext context,
              AsyncSnapshot<List<MatterDevice>> snapshot) {
            if (snapshot.data == null) {
              return const Center(child: CircularProgressIndicator());
            }
            return ListView.builder(
              itemCount: snapshot.data!.length,
              itemBuilder: (BuildContext context, int index) {
                return Card(
                  margin:
                      const EdgeInsets.symmetric(horizontal: 12, vertical: 12),
                  child: Padding(
                    padding: const EdgeInsets.symmetric(
                      horizontal: 12,
                      vertical: 8,
                    ),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Row(
                          mainAxisAlignment: MainAxisAlignment.spaceBetween,
                          children: [
                            Expanded(
                              child: Text(
                                "Device Name: ${snapshot.data![index].deviceName}",
                                style: Theme.of(context).textTheme.titleLarge,
                                maxLines: 3,
                              ),
                            ),
                            IconButton(
                              onPressed: () async {
                                // Delete this device
                                await removeMatterDevice(snapshot.data![index]);
                                setState(() {});
                              },
                              icon: const Icon(Icons.delete),
                            ),
                            IconButton(
                              onPressed: () async {
                                // Delete this device
                                TextEditingController pinCodeController =
                                    TextEditingController();
                                await showDialog(
                                    context: context,
                                    builder: (context) => AlertDialog(
                                          content: Column(
                                            children: [
                                              Text("PASSCODE"),
                                              TextField(
                                                controller: pinCodeController,
                                              )
                                            ],
                                          ),
                                          actions: [
                                            TextButton(
                                                onPressed: () {
                                                  Navigator.of(context).pop();
                                                },
                                                child: Text("Cancel")),
                                            TextButton(
                                                onPressed: () {
                                                  Navigator.of(context).pop();
                                                },
                                                child: Text("SHARE")),
                                          ],
                                        ));
                                try {
                                  await shareMatterDevice(snapshot.data![index],
                                      int.parse(pinCodeController.text));
                                } catch (_) {}
                                setState(() {});
                              },
                              icon: const Icon(Icons.share),
                            )
                          ],
                        ),
                        const SizedBox(height: 4),
                        Text(
                          "Device Id: ${snapshot.data![index].deviceId}",
                          style: Theme.of(context).textTheme.bodyLarge,
                        ),
                        Text(
                          "Vendor Id: ${snapshot.data![index].vendorId}",
                          style: Theme.of(context).textTheme.bodyLarge,
                        ),
                        Text(
                          "Device Type: ${snapshot.data![index].deviceType}",
                          style: Theme.of(context).textTheme.bodyLarge,
                        ),
                        if (!isCommissioningInProgress)
                          StreamBuilder<bool?>(
                              stream: snapshot.data![index].listenToEvents(),
                              builder: (context, snapshot) {
                                return Text(
                                  "Value: ${snapshot.data ?? "Offline"}",
                                  style: Theme.of(context).textTheme.bodyLarge,
                                );
                              }),
                      ],
                    ),
                  ),
                );
              },
            );
          },
        ),
        floatingActionButton: FloatingActionButton.large(
          child: const Icon(Icons.add),
          onPressed: () async {
            try {
              setState(() {
                isCommissioningInProgress = true;
              });

              final MatterDevice? device =
                  await _googleMatterFlutterPlugin.commissionDevice();

              if (device != null) {
                await addMatterDevice(device);
                setState(() {
                  isCommissioningInProgress = false;
                });
              }
            } catch (ex) {
              setState(() {
                isCommissioningInProgress = false;
              });

              print('Failed to get Commissioning: $ex');
            }
          },
        ),
      ),
    );
  }

  shareMatterDevice(MatterDevice matterDevice, int passCode) async {
    await _googleMatterFlutterPlugin.shareDevice(matterDevice.deviceId,
        matterDevice.vendorId, matterDevice.describeContents, passCode);
  }
}
