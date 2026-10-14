import 'dart:async';
import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:google_matter_flutter/google_matter_flutter.dart';
import 'package:shared_preferences/shared_preferences.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatefulWidget {
  const MyApp({super.key});

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
                          // Demonstrates the legacy polling stream still works for anyone
                          // migrating gradually; new code should prefer the capability-specific
                          // getters shown in _CapabilitiesSection below.
                          StreamBuilder<bool?>(
                              // ignore: deprecated_member_use
                              stream: snapshot.data![index].listenToEvents(),
                              builder: (context, snapshot) {
                                return Text(
                                  "Value: ${snapshot.data ?? "Offline"}",
                                  style: Theme.of(context).textTheme.bodyLarge,
                                );
                              }),
                        const SizedBox(height: 8),
                        _CapabilitiesSection(device: snapshot.data![index]),
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

              debugPrint('Failed to get Commissioning: $ex');
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

/// Demonstrates the spec-driven capability discovery API: queries the device for its supported
/// clusters, then shows relevant quick-actions for whichever capabilities it reports.
class _CapabilitiesSection extends StatefulWidget {
  final MatterDevice device;

  const _CapabilitiesSection({required this.device});

  @override
  State<_CapabilitiesSection> createState() => _CapabilitiesSectionState();
}

class _CapabilitiesSectionState extends State<_CapabilitiesSection> {
  bool _isLoading = false;
  Set<MatterCapability>? _capabilities;

  Future<void> _discover() async {
    setState(() => _isLoading = true);
    try {
      await widget.device.discoverEndpoints();
      setState(() => _capabilities = widget.device.capabilities);
    } catch (ex) {
      // Discovery can fail if the device is offline or unreachable; surface it via a snackbar
      // rather than crashing the whole list.
      if (mounted) {
        ScaffoldMessenger.of(context)
            .showSnackBar(SnackBar(content: Text('Discovery failed: $ex')));
      }
    } finally {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final capabilities = _capabilities;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          children: [
            TextButton.icon(
              onPressed: _isLoading ? null : _discover,
              icon: _isLoading
                  ? const SizedBox(
                      width: 16,
                      height: 16,
                      child: CircularProgressIndicator(strokeWidth: 2),
                    )
                  : const Icon(Icons.search),
              label: const Text('Discover capabilities'),
            ),
          ],
        ),
        if (capabilities != null)
          Wrap(
            spacing: 6,
            runSpacing: 6,
            children: [
              for (final capability in capabilities)
                Chip(label: Text(capability.name)),
              if (capabilities.isEmpty) const Chip(label: Text('none found')),
            ],
          ),
        if (capabilities != null) ...[
          const SizedBox(height: 4),
          Wrap(
            spacing: 8,
            children: [
              if (capabilities.contains(MatterCapability.onOff))
                ElevatedButton(
                  onPressed: () => widget.device.toggleOnOff(),
                  child: const Text('Toggle on/off'),
                ),
              if (capabilities.contains(MatterCapability.doorLock)) ...[
                ElevatedButton(
                  onPressed: () => widget.device.lockDoor(),
                  child: const Text('Lock'),
                ),
                ElevatedButton(
                  onPressed: () => widget.device.unlockDoor(),
                  child: const Text('Unlock'),
                ),
              ],
              if (capabilities.contains(MatterCapability.windowCovering)) ...[
                ElevatedButton(
                  onPressed: () => widget.device.openWindowCovering(),
                  child: const Text('Open'),
                ),
                ElevatedButton(
                  onPressed: () => widget.device.closeWindowCovering(),
                  child: const Text('Close'),
                ),
              ],
            ],
          ),
        ],
      ],
    );
  }
}
