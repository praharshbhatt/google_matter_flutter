package com.google.google_matter_flutter.google_matter_flutter

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.home.matter.Matter
import com.google.android.gms.home.matter.commissioning.CommissioningRequest
import com.google.android.gms.home.matter.commissioning.CommissioningResult
import com.google.android.gms.home.matter.commissioning.CommissioningWindow
import com.google.android.gms.home.matter.commissioning.ShareDeviceRequest
import com.google.android.gms.home.matter.common.DeviceDescriptor
import com.google.android.gms.home.matter.common.Discriminator
import com.google.android.gms.home.matter.common.NetworkLocation
import com.google.google_matter_flutter.google_matter_flutter.chip.ChipClient
import com.google.google_matter_flutter.google_matter_flutter.chip.ClustersHelper
import dagger.hilt.android.qualifiers.ApplicationContext
import io.flutter.plugin.common.MethodChannel
import kotlinx.coroutines.runBlocking

class GoogleMatter(
    private val activity: android.app.Activity,
    private val methodCallResult: MethodChannel.Result
) {
    @RequiresApi(Build.VERSION_CODES.O_MR1)
    fun shareDevice( @ApplicationContext context: Context,
                            productId: Int,
                     setVendorId: Int,
                     discriminator : Int ,
                    passCode : Long ) {
        val shareDeviceRequest =
            ShareDeviceRequest.builder()
                .setDeviceDescriptor(DeviceDescriptor.builder().setProductId(productId).setVendorId(setVendorId).build())
                .setCommissioningWindow(
                    CommissioningWindow.builder()
                        .setDiscriminator(Discriminator.forLongValue(discriminator))
                        .setPasscode(passCode)
                        .setWindowOpenMillis(SystemClock.elapsedRealtime())
                        .setDurationSeconds(180L)
                        .build())
                .build()
        Matter.getCommissioningClient(activity)
            .shareDevice(shareDeviceRequest)
            .addOnSuccessListener { result ->
                methodCallResult.success(true)
            }
            .addOnFailureListener { error ->
                methodCallResult.error("${error.message}", "${error.message}", error)
            }




    }
    @RequiresApi(Build.VERSION_CODES.O_MR1)
    fun commissionDevice() {
        // Register the commissioningLauncher here
        print("Starting CommissioningRequest...")
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
                    1001, Intent(), Integer.valueOf(0), Integer.valueOf(0),
                    Integer.valueOf(0),
                )

                // commissioningLauncher.launch(IntentSenderRequest.Builder(result).build())
            }.addOnFailureListener { error ->
                methodCallResult.error("${error.message}", "${error.message}", error)
            }
    }

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
        Log.d("MATTER ","ACTIVITY RESULT "+requestCode+" "+resultCode)
        // Send the status
        when (resultCode) {
            null -> {
                methodCallResult.error("1", "Timed Out", "Timed Out")
            }
            AppCompatActivity.RESULT_OK -> {
                // Proceed to get this device's details
                if (data != null) {
                    // Get the Device Data and send it back
                    val result: CommissioningResult =
                        CommissioningResult.fromIntentSenderResult(
                            resultCode,
                            data
                        )
                    print("Commissioned Device data: $result")

                    val commissionedDevice: HashMap<String, Any> = HashMap()
                    if (result.token == null) {
                        methodCallResult.error("2", "token", "Failed to get token")
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
                    methodCallResult.success(commissionedDevice)
                }
            }
            else -> {
                methodCallResult.error("0", "User Cancelled", "User Cancelled")
            }
        }

        return resultCode == AppCompatActivity.RESULT_OK
    }

    // Queries [ClustersHelper] to get the device's current brodcasted data
    fun getDeviceData(
        @ApplicationContext context: Context,
        deviceId: Long,
        deviceName: String
    ): Boolean {
        val clustersHelper: ClustersHelper = ClustersHelper(ChipClient(context))
        var isOn: Boolean? = null
        runBlocking {

            if (deviceName.toLowerCase()
                    .contains("occupancy") || deviceName.toLowerCase()
                    .contains("pir")
            ) {
                // If occupancy sensor, get the bitmap value
                val deviceStateBitmapCluster: Int? =
                    clustersHelper.getDeviceStateBitmapCluster(deviceId, 1)
                if (deviceStateBitmapCluster != null) {
                    isOn = deviceStateBitmapCluster > 0
                }
            } else if (deviceName.toLowerCase()
                    .contains("door") || deviceName.toLowerCase().contains("contact")
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
                print("[device ping] failed")
                isOn = false
            } else {
                print("[device ping] success [${isOn}]")
            }


        }
        return isOn == true

    }

}