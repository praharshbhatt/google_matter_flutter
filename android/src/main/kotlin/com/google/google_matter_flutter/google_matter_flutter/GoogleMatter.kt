package com.google.google_matter_flutter.google_matter_flutter

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.home.matter.Matter
import com.google.android.gms.home.matter.commissioning.CommissioningRequest
import com.google.android.gms.home.matter.commissioning.CommissioningResult
import com.google.android.gms.home.matter.commissioning.CommissioningWindow
import com.google.android.gms.home.matter.commissioning.ShareDeviceRequest
import com.google.android.gms.home.matter.common.DeviceDescriptor
import com.google.android.gms.home.matter.common.Discriminator
import com.google.google_matter_flutter.google_matter_flutter.chip.ChipClient
import com.google.google_matter_flutter.google_matter_flutter.chip.ClustersHelper
import com.google.google_matter_flutter.google_matter_flutter.chip.MatterConstants
import com.google.google_matter_flutter.google_matter_flutter.chip.MatterLog
import io.flutter.plugin.common.MethodChannel
import kotlinx.coroutines.runBlocking

class GoogleMatter(
    private val activity: android.app.Activity,
    var methodCallResult: MethodChannel.Result? = null
) {
    private companion object {
        const val COMMISSION_REQUEST_CODE = 1001
    }

    /**
     * Opens a new commissioning window on an already-commissioned device and shares it with
     * another ecosystem/app (e.g. a secondary smart home app) via Google's ShareDevice API.
     * Contributed by Marco Lettieri (microbees.com).
     */
    @RequiresApi(Build.VERSION_CODES.O_MR1)
    fun shareDevice(productId: Int, vendorId: Int, discriminator: Int, passCode: Long) {
        MatterLog.d("Starting ShareDeviceRequest...")
        val shareDeviceRequest = ShareDeviceRequest.builder()
            .setDeviceDescriptor(
                DeviceDescriptor.builder().setProductId(productId).setVendorId(vendorId).build()
            )
            .setCommissioningWindow(
                CommissioningWindow.builder()
                    .setDiscriminator(Discriminator.forLongValue(discriminator))
                    .setPasscode(passCode)
                    .setWindowOpenMillis(SystemClock.elapsedRealtime())
                    .setDurationSeconds(180L)
                    .build()
            )
            .build()
        Matter.getCommissioningClient(activity)
            .shareDevice(shareDeviceRequest)
            .addOnSuccessListener { methodCallResult?.success(true) }
            .addOnFailureListener { error ->
                methodCallResult?.error("${error.message}", "${error.message}", error)
            }
    }

    @RequiresApi(Build.VERSION_CODES.O_MR1)
    fun commissionDevice() {
        // Register the commissioningLauncher here
        MatterLog.d("Starting CommissioningRequest...")
        val request: CommissioningRequest = CommissioningRequest.builder().setCommissioningService(
            ComponentName(
                activity,
                AppCommissioningService::class.java
            )
        ).build()
        Matter.getCommissioningClient(activity)
            .commissionDevice(request)
            .addOnSuccessListener { intentBuilder ->

                // Ref: https://stackoverflow.com/questions/14131171/calling-startintentsenderforresult-from-fragment-android-billing-v3
                activity.startIntentSenderForResult(
                    intentBuilder,
                    COMMISSION_REQUEST_CODE, Intent(), Integer.valueOf(0), Integer.valueOf(0),
                    Integer.valueOf(0),
                )

                // commissioningLauncher.launch(IntentSenderRequest.Builder(result).build())
            }.addOnFailureListener { error ->
                replyError("${error.message}", "${error.message}", error)
            }
    }

    // Replies at most once per commissioning request, so a late/duplicate result can't crash
    private fun replyError(code: String, message: String?, details: Any?) {
        methodCallResult?.error(code, message, details)
        methodCallResult = null
    }

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
        // Ignore results of unrelated activities, and results nobody is waiting for
        if (requestCode != COMMISSION_REQUEST_CODE || methodCallResult == null) return false

        // Send the status
        when (resultCode) {
            AppCompatActivity.RESULT_OK -> {
                // Proceed to get this device's details
                if (data == null) {
                    replyError("1", "Timed Out", "No commissioning data returned")
                    return false
                } else {
                    // Get the Device Data and send it back
                    val result: CommissioningResult =
                        CommissioningResult.fromIntentSenderResult(
                            resultCode,
                            data
                        )
                    MatterLog.d("Commissioned Device data: $result")

                    val commissionedDevice: HashMap<String, Any> = HashMap()
                    if (result.token == null) {
                        replyError("2", "token", "Failed to get token")
                        return false
                    } else {
                        commissionedDevice["deviceId"] = result.token?.toLong()!!
                    }
                    commissionedDevice["deviceName"] = result.deviceName
                    commissionedDevice["deviceType"] =
                        result.commissionedDeviceDescriptor.deviceType
                    commissionedDevice["vendorId"] = result.commissionedDeviceDescriptor.vendorId
                    commissionedDevice["describeContents"] =
                        result.commissionedDeviceDescriptor.describeContents()
                    methodCallResult?.success(commissionedDevice)
                    methodCallResult = null
                }
            }
            else -> {
                replyError("0", "User Cancelled", "User Cancelled")
            }
        }

        return resultCode == AppCompatActivity.RESULT_OK
    }

    // Queries [ClustersHelper] to get the device's current brodcasted data
    fun getDeviceData(
        context: Context,
        deviceId: Long,
        deviceName: String
    ): Boolean {
        val clustersHelper: ClustersHelper = ClustersHelper(ChipClient.getInstance(context))
        var isOn: Boolean? = null
        runBlocking {

            if (deviceName.lowercase()
                    .contains("occupancy") || deviceName.lowercase()
                    .contains("pir")
            ) {
                // If occupancy sensor, get the bitmap value
                val deviceStateBitmapCluster: Int? =
                    clustersHelper.getDeviceStateBitmapCluster(deviceId, 1)
                if (deviceStateBitmapCluster != null) {
                    isOn = deviceStateBitmapCluster > 0
                }
            } else if (deviceName.lowercase()
                    .contains("door") || deviceName.lowercase().contains("contact")
            ) {
                // For Door contact
                isOn =
                    clustersHelper.getDeviceStateBooleanCluster(
                        deviceId,
                        1
                    )
            } else {
                // If any other device, get the boolean value
                isOn =
                    clustersHelper.getDeviceStateOnOffCluster(deviceId, 1)
            }
            if (isOn == null) {
                MatterLog.d("[device ping] failed")
                isOn = false
            } else {
                MatterLog.d("[device ping] success [${isOn}]")
            }


        }
        return isOn == true

    }

    /**
     * Discovers the endpoints, device types and clusters supported by [deviceId], so the Dart side
     * can decide which capabilities/UI to expose for it, instead of relying on name-sniffing
     * heuristics.
     */
    fun discoverDevice(context: Context, deviceId: Long): List<Map<String, Any?>> {
        val clustersHelper = ClustersHelper(ChipClient.getInstance(context))
        var result: List<Map<String, Any?>> = emptyList()
        runBlocking {
            val matterInfo = clustersHelper.fetchDeviceMatterInfo(deviceId, 0)
            result = matterInfo.map { info ->
                mapOf(
                    "endpoint" to info.endpoint,
                    "deviceTypes" to info.types,
                    "deviceTypeNames" to
                        info.types.map { MatterConstants.DeviceTypesMap[it] ?: "Unknown (0x${it.toString(16)})" },
                    "serverClusters" to info.serverClusters,
                    "clusterNames" to
                        info.serverClusters.mapNotNull { cluster ->
                            (cluster as? Long)?.let { MatterConstants.ClustersMap[it] }
                        }
                )
            }
        }
        return result
    }

    /**
     * Generic dispatcher for cluster commands/reads coming from the Dart side. [action] identifies
     * the cluster operation (see the plugin's Dart-side `MatterAction` enum for the full list), and
     * [args] carries its positional parameters.
     */
    fun performClusterAction(
        context: Context,
        action: String,
        deviceId: Long,
        endpoint: Int,
        args: List<Any?>
    ): Any? {
        val clustersHelper = ClustersHelper(ChipClient.getInstance(context))
        var result: Any? = null
        runBlocking {
            result =
                when (action) {
                    "getOnOff" -> clustersHelper.getDeviceStateOnOffCluster(deviceId, endpoint)
                    "toggleOnOff" -> {
                        clustersHelper.toggleDeviceStateOnOffCluster(deviceId, endpoint)
                        null
                    }
                    "getLevel" -> clustersHelper.getLevelControlCurrentLevel(deviceId, endpoint)
                    "setLevel" -> {
                        clustersHelper.setLevelControlLevel(deviceId, endpoint, (args[0] as Number).toInt())
                        null
                    }
                    "setHueAndSaturation" -> {
                        clustersHelper.setColorControlHueAndSaturation(
                            deviceId, endpoint, (args[0] as Number).toInt(), (args[1] as Number).toInt()
                        )
                        null
                    }
                    "setColorTemperature" -> {
                        clustersHelper.setColorControlTemperature(
                            deviceId, endpoint, (args[0] as Number).toInt()
                        )
                        null
                    }
                    "lockDoor" -> {
                        clustersHelper.lockDoor(deviceId, endpoint)
                        null
                    }
                    "unlockDoor" -> {
                        clustersHelper.unlockDoor(deviceId, endpoint)
                        null
                    }
                    "getLockState" -> clustersHelper.getDoorLockState(deviceId, endpoint)
                    "openWindowCovering" -> {
                        clustersHelper.openWindowCovering(deviceId, endpoint)
                        null
                    }
                    "closeWindowCovering" -> {
                        clustersHelper.closeWindowCovering(deviceId, endpoint)
                        null
                    }
                    "setWindowCoveringLiftPosition" -> {
                        clustersHelper.setWindowCoveringLiftPosition(
                            deviceId, endpoint, (args[0] as Number).toInt()
                        )
                        null
                    }
                    "getThermostatLocalTemperature" ->
                        clustersHelper.getThermostatLocalTemperature(deviceId, endpoint)
                    "setThermostatHeatingSetpoint" -> {
                        clustersHelper.setThermostatHeatingSetpoint(
                            deviceId, endpoint, (args[0] as Number).toInt()
                        )
                        null
                    }
                    "setThermostatCoolingSetpoint" -> {
                        clustersHelper.setThermostatCoolingSetpoint(
                            deviceId, endpoint, (args[0] as Number).toInt()
                        )
                        null
                    }
                    "getTemperatureMeasurement" ->
                        clustersHelper.getTemperatureMeasurement(deviceId, endpoint)
                    "getRelativeHumidityMeasurement" ->
                        clustersHelper.getRelativeHumidityMeasurement(deviceId, endpoint)
                    "getIlluminanceMeasurement" ->
                        clustersHelper.getIlluminanceMeasurement(deviceId, endpoint)
                    "getPressureMeasurement" -> clustersHelper.getPressureMeasurement(deviceId, endpoint)
                    "getFlowMeasurement" -> clustersHelper.getFlowMeasurement(deviceId, endpoint)
                    "getOccupancy" -> clustersHelper.getDeviceStateBitmapCluster(deviceId, endpoint)
                    "getContactState" -> clustersHelper.getDeviceStateBooleanCluster(deviceId, endpoint)
                    else -> throw IllegalArgumentException("Unknown cluster action [$action]")
                }
        }
        return result
    }

}