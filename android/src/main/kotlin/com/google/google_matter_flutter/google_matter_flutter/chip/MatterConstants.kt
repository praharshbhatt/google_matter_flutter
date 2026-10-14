package com.google.google_matter_flutter.google_matter_flutter.chip

/**
 * Matter device type IDs and cluster IDs, per the Matter Device Library / Application Cluster
 * specifications. Used to identify what kind of endpoint we're talking to and what
 * clusters/capabilities it's likely to expose, without having to hardcode per-device logic.
 */
object MatterConstants {
  /**
   * Device type IDs as defined by the Matter Device Library specification
   * (see connectedhomeip's `matter-devices.xml`). Not exhaustive of every device type in the
   * spec, but covers the common consumer device categories this plugin aims to support.
   */
  val DeviceTypesMap =
      mapOf<Long, String>(
          0x0016L to "Root Node",
          0x000EL to "Aggregator",
          0x0013L to "Bridged Node",
          0x0011L to "Power Source",
          // Lighting & switches
          0x0100L to "On/Off Light",
          0x0101L to "Dimmable Light",
          0x010CL to "Color Temperature Light",
          0x010DL to "Extended Color Light",
          0x0103L to "On/Off Light Switch",
          0x0104L to "Dimmer Switch",
          0x0105L to "Color Dimmer Switch",
          0x000FL to "Generic Switch",
          // Plugs / outlets
          0x010AL to "On/Off Plug-in Unit",
          0x010BL to "Dimmable Plug-in Unit",
          // Sensors
          0x0015L to "Contact Sensor",
          0x0106L to "Light Sensor",
          0x0107L to "Occupancy Sensor",
          0x0302L to "Temperature Sensor",
          0x0305L to "Pressure Sensor",
          0x0306L to "Flow Sensor",
          0x0307L to "Humidity Sensor",
          0x0850L to "On/Off Sensor",
          0x0041L to "Water Freeze Detector",
          0x0043L to "Water Leak Detector",
          0x0044L to "Rain Sensor",
          0x002CL to "Air Quality Sensor",
          0x0076L to "Smoke/CO Alarm",
          // Closures / access
          0x000AL to "Door Lock",
          0x000BL to "Door Lock Controller",
          0x0202L to "Window Covering",
          0x0203L to "Window Covering Controller",
          0x0230L to "Closure",
          0x0231L to "Closure Panel",
          // Climate & HVAC
          0x0301L to "Thermostat",
          0x030AL to "Thermostat Controller",
          0x002BL to "Fan",
          0x0072L to "Room Air Conditioner",
          0x0042L to "Water Valve",
          0x0309L to "Heat Pump",
          0x050FL to "Water Heater",
          // Energy
          0x0303L to "Pump",
          0x0304L to "Pump Controller",
          0x050CL to "Energy EVSE",
          0x050DL to "Device Energy Management",
          0x0017L to "Solar Power",
          0x0018L to "Battery Storage",
          0x0514L to "Electrical Meter",
          // Appliances
          0x002DL to "Air Purifier",
          0x0075L to "Dishwasher",
          0x007BL to "Oven",
          0x0079L to "Microwave Oven",
          0x0070L to "Refrigerator",
          0x0073L to "Laundry Washer",
          0x007CL to "Laundry Dryer",
          0x007AL to "Extractor Hood",
          0x0074L to "Robotic Vacuum Cleaner",
          0x0071L to "Temperature Controlled Cabinet",
          0x0078L to "Cooktop",
          0x0077L to "Cook Surface",
          // Media
          0x0023L to "Casting Video Player",
          0x0028L to "Basic Video Player",
          0x0022L to "Speaker",
          0x0148L to "Doorbell",
          0x0142L to "Camera",
      )

  val ClustersMap =
      mapOf<Long, String>(
          3L to "Identify",
          4L to "Groups",
          5L to "Scenes",
          6L to "On/Off",
          8L to "Level Control",
          29L to "Descriptor",
          30L to "Binding",
          31L to "Access Control",
          40L to "Basic Information",
          41L to "OTA Software Update Provider",
          42L to "OTA Software Update Requestor",
          43L to "Localization Configuration",
          44L to "Time Format Localization",
          44L to "Time Format Localization",
          44L to "Unit Localization",
          48L to "General Commissioning",
          49L to "Network Commissioning",
          50L to "Diagnostics Logs",
          51L to "General Diagnostics",
          52L to "Software Diagnostics",
          53L to "Thread Network Diagnostics",
          54L to "Wi-Fi Network Diagnostics",
          55L to "Ethernet Network Diagnostics",
          56L to "Time Synchronization",
          59L to "Switch",
          60L to "Administrator Commissioning",
          62L to "Node Operational Credentials",
          63L to "Group Key Management",
          64L to "Fixed label",
          65L to "User label",
          1030L to "Occupancy Sensing",
      )
}
