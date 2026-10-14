/*
 * Copyright 2022 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.google_matter_flutter.google_matter_flutter.chip

import chip.devicecontroller.ChipClusters
import chip.devicecontroller.ChipStructs
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * Encapsulates the information of interest when querying a Matter device just after it has been
 * commissioned.
 */
data class DeviceMatterInfo(
    val endpoint: Int,
    val types: List<Long>,
    val serverClusters: List<Any>,
    val clientClusters: List<Any>
)

/** Facilitates access to Clusters functionality, delegating to the shared [ChipClient]. */
class ClustersHelper(private val chipClient: ChipClient) {

    // -----------------------------------------------------------------------------------------------
    // Convenience functions

    /** Fetches MatterDeviceInfo for each endpoint supported by the device. */
    suspend fun fetchDeviceMatterInfo(nodeId: Long, endpoint: Int): List<DeviceMatterInfo> {
        MatterLog.d("fetchDeviceMatterInfo(): nodeId [${nodeId}]")
        val matterDeviceInfoList = arrayListOf<DeviceMatterInfo>()
        val connectedDevicePtr =
            try {
                chipClient.getConnectedDevicePointer(nodeId)
            } catch (e: IllegalStateException) {
                MatterLog.d("Can't get connectedDevicePointer.")
                return emptyList()
            }

        val partsListAttribute =
            readDescriptorClusterPartsListAttribute(connectedDevicePtr, endpoint)
        MatterLog.d("partsListAttribute [${partsListAttribute}]")

        // For each part (endpoint)
        partsListAttribute?.forEach { part ->
            MatterLog.d("part [$part] is [${part.javaClass}]")
            val endpointInt =
                when (part) {
                    is Int -> part.toInt()
                    else -> return@forEach
                }
            MatterLog.d("Processing part [$part]")

            // DeviceListAttribute
            val deviceListAttribute =
                readDescriptorClusterDeviceListAttribute(connectedDevicePtr, endpointInt)
            val types = arrayListOf<Long>()
            deviceListAttribute.forEach { types.add(it.deviceType) }

            // ServerListAttribute
            val serverListAttribute =
                readDescriptorClusterServerListAttribute(connectedDevicePtr, endpointInt)
            val serverClusters = arrayListOf<Any>()
            serverListAttribute.forEach { serverClusters.add(it) }

            // ClientListAttribute
            val clientListAttribute =
                readDescriptorClusterClientListAttribute(connectedDevicePtr, endpointInt)
            val clientClusters = arrayListOf<Any>()
            clientListAttribute.forEach { clientClusters.add(it) }

            // Build the DeviceMatterInfo
            val deviceMatterInfo =
                DeviceMatterInfo(endpointInt, types, serverClusters, clientClusters)
            matterDeviceInfoList.add(deviceMatterInfo)
        }
        return matterDeviceInfoList
    }

    // -----------------------------------------------------------------------------------------------
    // DescriptorCluster functions

    /**
     * PartsListAttribute. These are the endpoints supported.
     *
     * ```
     * For example, on endpoint 0:
     *     sendReadPartsListAttribute part: [1]
     *     sendReadPartsListAttribute part: [2]
     * ```
     */
    suspend fun readDescriptorClusterPartsListAttribute(
        devicePtr: Long,
        endpoint: Int
    ): List<Any>? {
        return suspendCoroutine { continuation ->
            getDescriptorClusterForDevice(devicePtr, endpoint)
                .readPartsListAttribute(
                    object : ChipClusters.DescriptorCluster.PartsListAttributeCallback {
                        override fun onSuccess(values: MutableList<Int>?) {
                            continuation.resume(values)
                        }

                        override fun onError(ex: Exception) {
                            continuation.resumeWithException(ex)
                        }
                    })
        }
    }

    /**
     * DeviceListAttribute
     *
     * ```
     * For example, on endpoint 0:
     *   device: [long type: 22, int revision: 1] -> maps to Root node (0x0016) (utility device type)
     * on endpoint 1:
     *   device: [long type: 256, int revision: 1] -> maps to On/Off Light (0x0100)
     * ```
     */
    suspend fun readDescriptorClusterDeviceListAttribute(
        devicePtr: Long,
        endpoint: Int
    ): List<ChipStructs.DescriptorClusterDeviceTypeStruct> {
        return suspendCoroutine { continuation ->
            getDescriptorClusterForDevice(devicePtr, endpoint)
                .readDeviceTypeListAttribute(
                    object : ChipClusters.DescriptorCluster.DeviceTypeListAttributeCallback {
                        override fun onSuccess(
                            values: List<ChipStructs.DescriptorClusterDeviceTypeStruct>
                        ) {
                            continuation.resume(values)
                        }

                        override fun onError(ex: Exception) {
                            continuation.resumeWithException(ex)
                        }
                    })
        }
    }

    /**
     * ServerListAttribute See
     * https://github.com/project-chip/connectedhomeip/blob/master/zzz_generated/app-common/app-common/zap-generated/ids/Clusters.h
     *
     * ```
     * For example: on endpoint 0
     *     sendReadServerListAttribute: [3]
     *     sendReadServerListAttribute: [4]
     *     sendReadServerListAttribute: [29]
     *     ... and more ...
     * on endpoint 1:
     *     sendReadServerListAttribute: [3]
     *     sendReadServerListAttribute: [4]
     *     sendReadServerListAttribute: [5]
     *     sendReadServerListAttribute: [6]
     *     sendReadServerListAttribute: [7]
     *     ... and more ...
     * on endpoint 2:
     *     sendReadServerListAttribute: [4]
     *     sendReadServerListAttribute: [6]
     *     sendReadServerListAttribute: [29]
     *     sendReadServerListAttribute: [1030]
     *
     * Some mappings:
     *     namespace Groups = 0x00000004 (4)
     *     namespace OnOff = 0x00000006 (6)
     *     namespace Descriptor = 0x0000001D (29)
     *     namespace OccupancySensing = 0x00000406 (1030)
     * ```
     */
    suspend fun readDescriptorClusterServerListAttribute(
        devicePtr: Long,
        endpoint: Int
    ): List<Long> {
        return suspendCoroutine { continuation ->
            getDescriptorClusterForDevice(devicePtr, endpoint)
                .readServerListAttribute(
                    object : ChipClusters.DescriptorCluster.ServerListAttributeCallback {
                        override fun onSuccess(values: MutableList<Long>) {
                            continuation.resume(values)
                        }

                        override fun onError(ex: Exception) {
                            continuation.resumeWithException(ex)
                        }
                    })
        }
    }

    /** ClientListAttribute */
    suspend fun readDescriptorClusterClientListAttribute(
        devicePtr: Long,
        endpoint: Int
    ): List<Long> {
        return suspendCoroutine { continuation ->
            getDescriptorClusterForDevice(devicePtr, endpoint)
                .readClientListAttribute(
                    object : ChipClusters.DescriptorCluster.ClientListAttributeCallback {
                        override fun onSuccess(values: MutableList<Long>) {
                            continuation.resume(values)
                        }

                        override fun onError(ex: Exception) {
                            continuation.resumeWithException(ex)
                        }
                    })
        }
    }

    private fun getDescriptorClusterForDevice(
        devicePtr: Long,
        endpoint: Int
    ): ChipClusters.DescriptorCluster {
        return ChipClusters.DescriptorCluster(devicePtr, endpoint)
    }

    // -----------------------------------------------------------------------------------------------
    // ApplicationCluster functions

    suspend fun readApplicationBasicClusterAttributeList(
        deviceId: Long,
        endpoint: Int
    ): List<Long> {
        val connectedDevicePtr =
            try {
                chipClient.getConnectedDevicePointer(deviceId)
            } catch (e: IllegalStateException) {
                MatterLog.d("Can't get connectedDevicePointer.")
                return emptyList()
            }
        return suspendCoroutine { continuation ->
            getApplicationBasicClusterForDevice(connectedDevicePtr, endpoint)
                .readAttributeListAttribute(
                    object : ChipClusters.ApplicationBasicCluster.AttributeListAttributeCallback {
                        override fun onSuccess(value: MutableList<Long>) {
                            continuation.resume(value)
                        }

                        override fun onError(ex: Exception) {
                            continuation.resumeWithException(ex)
                        }
                    })
        }
    }

    private fun getApplicationBasicClusterForDevice(
        devicePtr: Long,
        endpoint: Int
    ): ChipClusters.ApplicationBasicCluster {
        return ChipClusters.ApplicationBasicCluster(devicePtr, endpoint)
    }

    // -----------------------------------------------------------------------------------------------
    // BasicCluster functions

    suspend fun readBasicClusterVendorIDAttribute(deviceId: Long, endpoint: Int): Int? {
        val connectedDevicePtr =
            try {
                chipClient.getConnectedDevicePointer(deviceId)
            } catch (e: IllegalStateException) {
                MatterLog.d("Can't get connectedDevicePointer.")
                return null
            }
        return suspendCoroutine { continuation ->
            getBasicClusterForDevice(connectedDevicePtr, endpoint)
                .readVendorIDAttribute(
                    object : ChipClusters.BasicInformationCluster.VendorIDAttributeCallback {
                        override fun onSuccess(value: Int?) {
                            continuation.resume(value)
                        }

                        override fun onError(ex: Exception) {
                            continuation.resumeWithException(ex)
                        }
                    })
        }
    }

    suspend fun readBasicClusterAttributeList(deviceId: Long, endpoint: Int): List<Long> {
        val connectedDevicePtr =
            try {
                chipClient.getConnectedDevicePointer(deviceId)
            } catch (e: IllegalStateException) {
                MatterLog.d("Can't get connectedDevicePointer.")
                return emptyList()
            }

        return suspendCoroutine { continuation ->
            getBasicClusterForDevice(connectedDevicePtr, endpoint)
                .readAttributeListAttribute(
                    object : ChipClusters.BasicInformationCluster.AttributeListAttributeCallback {
                        override fun onSuccess(values: MutableList<Long>) {
                            continuation.resume(values)
                        }

                        override fun onError(ex: Exception) {
                            continuation.resumeWithException(ex)
                        }
                    })
        }
    }

    private fun getBasicClusterForDevice(
        devicePtr: Long,
        endpoint: Int
    ): ChipClusters.BasicInformationCluster {
        return ChipClusters.BasicInformationCluster(devicePtr, endpoint)
    }

    // -----------------------------------------------------------------------------------------------
    // OnOffCluster functions

    // CODELAB FEATURED BEGIN
    suspend fun toggleDeviceStateOnOffCluster(deviceId: Long, endpoint: Int) {
        MatterLog.d("toggleDeviceStateOnOffCluster())")
        val connectedDevicePtr =
            try {
                chipClient.getConnectedDevicePointer(deviceId)
            } catch (e: IllegalStateException) {
                MatterLog.d("Can't get connectedDevicePointer.")
                return
            }
        return suspendCoroutine { continuation ->
            getOnOffClusterForDevice(connectedDevicePtr, endpoint)
                .toggle(
                    object : ChipClusters.DefaultClusterCallback {
                        override fun onSuccess() {
                            continuation.resume(Unit)
                        }

                        override fun onError(ex: Exception) {
                            MatterLog.d("readOnOffAttribute command failure: $ex")
                            continuation.resumeWithException(ex)
                        }
                    })
        }
    }

    suspend fun getDeviceStateOnOffCluster(deviceId: Long, endpoint: Int): Boolean? {
        MatterLog.d("getDeviceStateOnOffCluster())")
        val connectedDevicePtr =
            try {
                chipClient.getConnectedDevicePointer(deviceId)
            } catch (e: IllegalStateException) {
                MatterLog.d("Can't get connectedDevicePointer.")
                return null
            }
        return suspendCoroutine { continuation ->
            getOnOffClusterForDevice(connectedDevicePtr, endpoint)
                .readOnOffAttribute(
                    object : ChipClusters.BooleanAttributeCallback {
                        override fun onSuccess(value: Boolean) {
                            continuation.resume(value)
                        }

                        override fun onError(ex: Exception) {
                            MatterLog.d("readOnOffAttribute command failure: $ex")
                            continuation.resumeWithException(ex)
                        }
                    }
                )
        }
    }

    // For occupancy sensor
    suspend fun getDeviceStateBitmapCluster(deviceId: Long, endpoint: Int): Int? {
        MatterLog.d("getDeviceStateOnOffCluster())")
        val connectedDevicePtr =
            try {
                chipClient.getConnectedDevicePointer(deviceId)
            } catch (e: IllegalStateException) {
                MatterLog.d("Can't get connectedDevicePointer.")
                return null
            }
        return suspendCoroutine { continuation ->
            ChipClusters.OccupancySensingCluster(connectedDevicePtr, endpoint)
                .readOccupancyAttribute(
                    object : ChipClusters.IntegerAttributeCallback {
                        override fun onSuccess(value: Int) {
                            continuation.resume(value)
                            MatterLog.d("PIR value: $value")
                        }

                        override fun onError(ex: Exception) {
                            MatterLog.d("readOnOffAttribute command failure: $ex")
                            continuation.resumeWithException(ex)
                        }
                    }
                )
        }
    }

    // For door contact
    suspend fun getDeviceStateBooleanCluster(deviceId: Long, endpoint: Int): Boolean? {
        MatterLog.d("getDeviceStateOnOffCluster())")
        val connectedDevicePtr =
            try {
                chipClient.getConnectedDevicePointer(deviceId)
            } catch (e: IllegalStateException) {
                MatterLog.d("Can't get connectedDevicePointer.")
                return null
            }
        return suspendCoroutine { continuation ->
            ChipClusters.BooleanStateCluster(connectedDevicePtr, endpoint)
                .readStateValueAttribute(
                    object : ChipClusters.BooleanAttributeCallback {
                        override fun onSuccess(value: Boolean) {
                            continuation.resume(value)
                        }

                        override fun onError(ex: Exception) {
                            MatterLog.d("readOnOffAttribute command failure: $ex")
                            continuation.resumeWithException(ex)
                        }
                    }
                )
        }
    }

    private fun getOnOffClusterForDevice(
        devicePtr: Long,
        endpoint: Int
    ): ChipClusters.OnOffCluster {
        return ChipClusters.OnOffCluster(devicePtr, endpoint)
    }

    // -----------------------------------------------------------------------------------------------
    // LevelControlCluster functions (dimmable lights/plugs)

    suspend fun getLevelControlCurrentLevel(deviceId: Long, endpoint: Int): Int? {
        val connectedDevicePtr = getConnectedDevicePtrOrNull(deviceId) ?: return null
        return suspendCoroutine { continuation ->
            ChipClusters.LevelControlCluster(connectedDevicePtr, endpoint)
                .readCurrentLevelAttribute(
                    object : ChipClusters.LevelControlCluster.CurrentLevelAttributeCallback {
                        override fun onSuccess(value: Int?) {
                            continuation.resume(value)
                        }

                        override fun onError(ex: Exception) {
                            continuation.resumeWithException(ex)
                        }
                    })
        }
    }

    /** [level] is 0-254 per the Matter Level Control cluster spec. */
    suspend fun setLevelControlLevel(
        deviceId: Long,
        endpoint: Int,
        level: Int,
        transitionTimeDs: Int = 0
    ) {
        val connectedDevicePtr = getConnectedDevicePtrOrNull(deviceId) ?: return
        return suspendCoroutine { continuation ->
            ChipClusters.LevelControlCluster(connectedDevicePtr, endpoint)
                .moveToLevel(
                    object : ChipClusters.DefaultClusterCallback {
                        override fun onSuccess() {
                            continuation.resume(Unit)
                        }

                        override fun onError(ex: Exception) {
                            continuation.resumeWithException(ex)
                        }
                    },
                    level,
                    transitionTimeDs,
                    0,
                    0
                )
        }
    }

    // -----------------------------------------------------------------------------------------------
    // ColorControlCluster functions (color/color-temperature lights)

    suspend fun setColorControlHueAndSaturation(
        deviceId: Long,
        endpoint: Int,
        hue: Int,
        saturation: Int,
        transitionTimeDs: Int = 0
    ) {
        val connectedDevicePtr = getConnectedDevicePtrOrNull(deviceId) ?: return
        return suspendCoroutine { continuation ->
            ChipClusters.ColorControlCluster(connectedDevicePtr, endpoint)
                .moveToHueAndSaturation(
                    object : ChipClusters.DefaultClusterCallback {
                        override fun onSuccess() {
                            continuation.resume(Unit)
                        }

                        override fun onError(ex: Exception) {
                            continuation.resumeWithException(ex)
                        }
                    },
                    hue,
                    saturation,
                    transitionTimeDs,
                    0,
                    0
                )
        }
    }

    /** [colorTemperatureMireds] is the mired color temperature (roughly 1,000,000 / kelvin). */
    suspend fun setColorControlTemperature(
        deviceId: Long,
        endpoint: Int,
        colorTemperatureMireds: Int,
        transitionTimeDs: Int = 0
    ) {
        val connectedDevicePtr = getConnectedDevicePtrOrNull(deviceId) ?: return
        return suspendCoroutine { continuation ->
            ChipClusters.ColorControlCluster(connectedDevicePtr, endpoint)
                .moveToColorTemperature(
                    object : ChipClusters.DefaultClusterCallback {
                        override fun onSuccess() {
                            continuation.resume(Unit)
                        }

                        override fun onError(ex: Exception) {
                            continuation.resumeWithException(ex)
                        }
                    },
                    colorTemperatureMireds,
                    transitionTimeDs,
                    0,
                    0
                )
        }
    }

    // -----------------------------------------------------------------------------------------------
    // DoorLockCluster functions

    suspend fun lockDoor(deviceId: Long, endpoint: Int, pinCode: ByteArray? = null) {
        val connectedDevicePtr = getConnectedDevicePtrOrNull(deviceId) ?: return
        return suspendCoroutine { continuation ->
            ChipClusters.DoorLockCluster(connectedDevicePtr, endpoint)
                .lockDoor(
                    object : ChipClusters.DefaultClusterCallback {
                        override fun onSuccess() {
                            continuation.resume(Unit)
                        }

                        override fun onError(ex: Exception) {
                            continuation.resumeWithException(ex)
                        }
                    },
                    java.util.Optional.ofNullable(pinCode),
                    0
                )
        }
    }

    suspend fun unlockDoor(deviceId: Long, endpoint: Int, pinCode: ByteArray? = null) {
        val connectedDevicePtr = getConnectedDevicePtrOrNull(deviceId) ?: return
        return suspendCoroutine { continuation ->
            ChipClusters.DoorLockCluster(connectedDevicePtr, endpoint)
                .unlockDoor(
                    object : ChipClusters.DefaultClusterCallback {
                        override fun onSuccess() {
                            continuation.resume(Unit)
                        }

                        override fun onError(ex: Exception) {
                            continuation.resumeWithException(ex)
                        }
                    },
                    java.util.Optional.ofNullable(pinCode),
                    0
                )
        }
    }

    /** Returns the DoorLock cluster's LockState attribute (1 = Locked, 2 = Unlocked, etc). */
    suspend fun getDoorLockState(deviceId: Long, endpoint: Int): Int? {
        val connectedDevicePtr = getConnectedDevicePtrOrNull(deviceId) ?: return null
        return suspendCoroutine { continuation ->
            ChipClusters.DoorLockCluster(connectedDevicePtr, endpoint)
                .readLockStateAttribute(
                    object : ChipClusters.DoorLockCluster.LockStateAttributeCallback {
                        override fun onSuccess(value: Int?) {
                            continuation.resume(value)
                        }

                        override fun onError(ex: Exception) {
                            continuation.resumeWithException(ex)
                        }
                    })
        }
    }

    // -----------------------------------------------------------------------------------------------
    // WindowCoveringCluster functions

    /** Fully opens the window covering (0% lift position). */
    suspend fun openWindowCovering(deviceId: Long, endpoint: Int) {
        val connectedDevicePtr = getConnectedDevicePtrOrNull(deviceId) ?: return
        return suspendCoroutine { continuation ->
            ChipClusters.WindowCoveringCluster(connectedDevicePtr, endpoint)
                .upOrOpen(
                    object : ChipClusters.DefaultClusterCallback {
                        override fun onSuccess() {
                            continuation.resume(Unit)
                        }

                        override fun onError(ex: Exception) {
                            continuation.resumeWithException(ex)
                        }
                    })
        }
    }

    /** Fully closes the window covering (100% lift position). */
    suspend fun closeWindowCovering(deviceId: Long, endpoint: Int) {
        val connectedDevicePtr = getConnectedDevicePtrOrNull(deviceId) ?: return
        return suspendCoroutine { continuation ->
            ChipClusters.WindowCoveringCluster(connectedDevicePtr, endpoint)
                .downOrClose(
                    object : ChipClusters.DefaultClusterCallback {
                        override fun onSuccess() {
                            continuation.resume(Unit)
                        }

                        override fun onError(ex: Exception) {
                            continuation.resumeWithException(ex)
                        }
                    })
        }
    }

    /** [liftPercent100ths] is 0-10000 (0 = fully open, 10000 = fully closed). */
    suspend fun setWindowCoveringLiftPosition(deviceId: Long, endpoint: Int, liftPercent100ths: Int) {
        val connectedDevicePtr = getConnectedDevicePtrOrNull(deviceId) ?: return
        return suspendCoroutine { continuation ->
            ChipClusters.WindowCoveringCluster(connectedDevicePtr, endpoint)
                .goToLiftPercentage(
                    object : ChipClusters.DefaultClusterCallback {
                        override fun onSuccess() {
                            continuation.resume(Unit)
                        }

                        override fun onError(ex: Exception) {
                            continuation.resumeWithException(ex)
                        }
                    },
                    liftPercent100ths
                )
        }
    }

    // -----------------------------------------------------------------------------------------------
    // ThermostatCluster functions

    suspend fun getThermostatLocalTemperature(deviceId: Long, endpoint: Int): Int? {
        val connectedDevicePtr = getConnectedDevicePtrOrNull(deviceId) ?: return null
        return suspendCoroutine { continuation ->
            ChipClusters.ThermostatCluster(connectedDevicePtr, endpoint)
                .readLocalTemperatureAttribute(
                    object : ChipClusters.ThermostatCluster.LocalTemperatureAttributeCallback {
                        override fun onSuccess(value: Int?) {
                            continuation.resume(value)
                        }

                        override fun onError(ex: Exception) {
                            continuation.resumeWithException(ex)
                        }
                    })
        }
    }

    /** [heatingSetpointCelsiusX100] is in hundredths of a degree Celsius, e.g. 2100 = 21.00C. */
    suspend fun setThermostatHeatingSetpoint(
        deviceId: Long,
        endpoint: Int,
        heatingSetpointCelsiusX100: Int
    ) {
        val connectedDevicePtr = getConnectedDevicePtrOrNull(deviceId) ?: return
        return suspendCoroutine { continuation ->
            ChipClusters.ThermostatCluster(connectedDevicePtr, endpoint)
                .writeOccupiedHeatingSetpointAttribute(
                    object : ChipClusters.DefaultClusterCallback {
                        override fun onSuccess() {
                            continuation.resume(Unit)
                        }

                        override fun onError(ex: Exception) {
                            continuation.resumeWithException(ex)
                        }
                    },
                    heatingSetpointCelsiusX100
                )
        }
    }

    /** [coolingSetpointCelsiusX100] is in hundredths of a degree Celsius, e.g. 2400 = 24.00C. */
    suspend fun setThermostatCoolingSetpoint(
        deviceId: Long,
        endpoint: Int,
        coolingSetpointCelsiusX100: Int
    ) {
        val connectedDevicePtr = getConnectedDevicePtrOrNull(deviceId) ?: return
        return suspendCoroutine { continuation ->
            ChipClusters.ThermostatCluster(connectedDevicePtr, endpoint)
                .writeOccupiedCoolingSetpointAttribute(
                    object : ChipClusters.DefaultClusterCallback {
                        override fun onSuccess() {
                            continuation.resume(Unit)
                        }

                        override fun onError(ex: Exception) {
                            continuation.resumeWithException(ex)
                        }
                    },
                    coolingSetpointCelsiusX100
                )
        }
    }

    // -----------------------------------------------------------------------------------------------
    // Sensor cluster functions (read-only measurements)

    /** TemperatureMeasurement cluster; result is hundredths of a degree Celsius. */
    suspend fun getTemperatureMeasurement(deviceId: Long, endpoint: Int): Int? {
        val connectedDevicePtr = getConnectedDevicePtrOrNull(deviceId) ?: return null
        return suspendCoroutine { continuation ->
            ChipClusters.TemperatureMeasurementCluster(connectedDevicePtr, endpoint)
                .readMeasuredValueAttribute(
                    object :
                        ChipClusters.TemperatureMeasurementCluster.MeasuredValueAttributeCallback {
                        override fun onSuccess(value: Int?) {
                            continuation.resume(value)
                        }

                        override fun onError(ex: Exception) {
                            continuation.resumeWithException(ex)
                        }
                    })
        }
    }

    /** RelativeHumidityMeasurement cluster; result is hundredths of a percent (0-10000). */
    suspend fun getRelativeHumidityMeasurement(deviceId: Long, endpoint: Int): Int? {
        val connectedDevicePtr = getConnectedDevicePtrOrNull(deviceId) ?: return null
        return suspendCoroutine { continuation ->
            ChipClusters.RelativeHumidityMeasurementCluster(connectedDevicePtr, endpoint)
                .readMeasuredValueAttribute(
                    object :
                        ChipClusters.RelativeHumidityMeasurementCluster.MeasuredValueAttributeCallback {
                        override fun onSuccess(value: Int?) {
                            continuation.resume(value)
                        }

                        override fun onError(ex: Exception) {
                            continuation.resumeWithException(ex)
                        }
                    })
        }
    }

    /** IlluminanceMeasurement cluster; result is in Lux (encoded per spec, 10000*log10(lux)+1). */
    suspend fun getIlluminanceMeasurement(deviceId: Long, endpoint: Int): Int? {
        val connectedDevicePtr = getConnectedDevicePtrOrNull(deviceId) ?: return null
        return suspendCoroutine { continuation ->
            ChipClusters.IlluminanceMeasurementCluster(connectedDevicePtr, endpoint)
                .readMeasuredValueAttribute(
                    object :
                        ChipClusters.IlluminanceMeasurementCluster.MeasuredValueAttributeCallback {
                        override fun onSuccess(value: Int?) {
                            continuation.resume(value)
                        }

                        override fun onError(ex: Exception) {
                            continuation.resumeWithException(ex)
                        }
                    })
        }
    }

    /** PressureMeasurement cluster; result is in kPa. */
    suspend fun getPressureMeasurement(deviceId: Long, endpoint: Int): Int? {
        val connectedDevicePtr = getConnectedDevicePtrOrNull(deviceId) ?: return null
        return suspendCoroutine { continuation ->
            ChipClusters.PressureMeasurementCluster(connectedDevicePtr, endpoint)
                .readMeasuredValueAttribute(
                    object : ChipClusters.PressureMeasurementCluster.MeasuredValueAttributeCallback {
                        override fun onSuccess(value: Int?) {
                            continuation.resume(value)
                        }

                        override fun onError(ex: Exception) {
                            continuation.resumeWithException(ex)
                        }
                    })
        }
    }

    /** FlowMeasurement cluster; result is in tenths of m/h. */
    suspend fun getFlowMeasurement(deviceId: Long, endpoint: Int): Int? {
        val connectedDevicePtr = getConnectedDevicePtrOrNull(deviceId) ?: return null
        return suspendCoroutine { continuation ->
            ChipClusters.FlowMeasurementCluster(connectedDevicePtr, endpoint)
                .readMeasuredValueAttribute(
                    object : ChipClusters.FlowMeasurementCluster.MeasuredValueAttributeCallback {
                        override fun onSuccess(value: Int?) {
                            continuation.resume(value)
                        }

                        override fun onError(ex: Exception) {
                            continuation.resumeWithException(ex)
                        }
                    })
        }
    }

    private suspend fun getConnectedDevicePtrOrNull(deviceId: Long): Long? {
        return try {
            chipClient.getConnectedDevicePointer(deviceId)
        } catch (e: IllegalStateException) {
            MatterLog.d("Can't get connectedDevicePointer.")
            null
        }
    }

    // -----------------------------------------------------------------------------------------------
    // Administrator Commissioning Cluster (11.19)

    suspend fun openCommissioningWindowAdministratorCommissioningCluster(
        deviceId: Long,
        endpoint: Int,
        timeoutSeconds: Int,
        pakeVerifier: ByteArray,
        discriminator: Int,
        iterations: Long,
        salt: ByteArray,
        timedInvokeTimeoutMs: Int
    ) {
        MatterLog.d("openCommissioningWindowAdministratorCommissioningCluster())")
        val connectedDevicePtr =
            try {
                chipClient.getConnectedDevicePointer(deviceId)
            } catch (e: IllegalStateException) {
                MatterLog.d("Can't get connectedDevicePointer.: $e")
                return
            }

        /*
        ChipClusters.DefaultClusterCallback var1, Integer var2, byte[] var3, Integer var4, Long var5, byte[] var6, int var7
         */
        return suspendCoroutine { continuation ->
            getAdministratorCommissioningClusterForDevice(connectedDevicePtr, endpoint)
                .openCommissioningWindow(
                    object : ChipClusters.DefaultClusterCallback {
                        override fun onSuccess() {
                            continuation.resume(Unit)
                        }

                        override fun onError(ex: java.lang.Exception?) {
                            MatterLog.d("getAdministratorCommissioningClusterForDevice.openCommissioningWindow command failure: $ex")
                            continuation.resumeWithException(ex!!)
                        }
                    },
                    timeoutSeconds,
                    pakeVerifier,
                    discriminator,
                    iterations,
                    salt,
                    timedInvokeTimeoutMs
                )
        }
    }

    private fun getAdministratorCommissioningClusterForDevice(
        devicePtr: Long,
        endpoint: Int
    ): ChipClusters.AdministratorCommissioningCluster {
        return ChipClusters.AdministratorCommissioningCluster(devicePtr, endpoint)
    }
}
