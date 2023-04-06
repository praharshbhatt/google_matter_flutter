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

package com.google.google_matter_flutter.google_matter_flutter

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.google.android.gms.home.matter.commissioning.CommissioningCompleteMetadata
import com.google.android.gms.home.matter.commissioning.CommissioningRequestMetadata
import com.google.android.gms.home.matter.commissioning.CommissioningService
import com.google.google_matter_flutter.google_matter_flutter.chip.ChipClient
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * The CommissioningService that's responsible for commissioning the device on the app's custom
 * fabric. AppCommissioningService is specified when building the
 * [com.google.android.gms.home.matter.commissioning.CommissioningRequest] in
 * [../screens.home.HomeViewModel].
 */
@AndroidEntryPoint
class AppCommissioningService : Service(), CommissioningService.Callback {
  private val chipClient: ChipClient = ChipClient(this)

  private val serviceJob = Job()
  private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

  private lateinit var commissioningServiceDelegate: CommissioningService

  override fun onCreate() {
    super.onCreate()
    print("onCreate()")
    commissioningServiceDelegate = CommissioningService.Builder(this).setCallback(this).build()
  }

  override fun onBind(intent: Intent): IBinder {
    print("onBind(): intent [${intent}]")
    return commissioningServiceDelegate.asBinder()
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    print("onStartCommand(): intent [${intent}] flags [${flags}] startId [${startId}]")
    return super.onStartCommand(intent, flags, startId)
  }

  override fun onDestroy() {
    super.onDestroy()
    print("onDestroy()")
    serviceJob.cancel()
  }

  override fun onCommissioningRequested(metadata: CommissioningRequestMetadata) {
    print(
      "*** onCommissioningRequested ***:\n" +
              "\tdeviceDescriptor: " +
              "deviceType [${metadata.deviceDescriptor.deviceType}] " +
              "vendorId [${metadata.deviceDescriptor.vendorId}] " +
              "productId [${metadata.deviceDescriptor.productId}]\n" +
              "\tnetworkLocation: " +
              "IP address toString() [${metadata.networkLocation.ipAddress}] " +
              "IP address hostAddress [${metadata.networkLocation.ipAddress.hostAddress}] " +
              "port [${metadata.networkLocation.port}]\n" +
              "\tpassCode [${metadata.passcode}]")

    // Perform commissioning on custom fabric
    // Generate a device ID
    val deviceId: Long = System.currentTimeMillis()
    serviceScope.launch {
      print(
        "Commissioning: App fabric -> ChipClient.establishPaseConnection(): deviceId [${deviceId}]")
      chipClient.awaitEstablishPaseConnection(
        deviceId,
        metadata.networkLocation.ipAddress.hostAddress!!,
        metadata.networkLocation.port,
        metadata.passcode)
      print("Commissioning: App fabric -> ChipClient.commissionDevice(): deviceId [${deviceId}]")
      chipClient.awaitCommissionDevice(deviceId, null)

      print("Commissioning: Calling commissioningServiceDelegate.sendCommissioningComplete()")
      commissioningServiceDelegate
        .sendCommissioningComplete(
          CommissioningCompleteMetadata.builder().setToken(deviceId.toString()).build())
        .addOnSuccessListener {
          print(
            "Commissioning: OnSuccess for commissioningServiceDelegate.sendCommissioningComplete()")
        }
        .addOnFailureListener { ex ->
          print("Commissioning: Failed to send commissioning complete.:  $ex")
        }
    }
  }
}
