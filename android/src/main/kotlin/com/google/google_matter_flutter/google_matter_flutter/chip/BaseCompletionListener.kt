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

import chip.devicecontroller.ChipDeviceController

/**
 * ChipDeviceController uses a CompletionListener for callbacks. This is a "base" default
 * implementation for that CompletionListener.
 */
abstract class BaseCompletionListener : ChipDeviceController.CompletionListener {
    override fun onConnectDeviceComplete() {
        print("BaseCompletionListener onConnectDeviceComplete()")
    }

    override fun onStatusUpdate(status: Int) {
        print("BaseCompletionListener onStatusUpdate(): status [${status}]")
    }

    override fun onPairingComplete(code: Int) {
        print("BaseCompletionListener onPairingComplete(): code [${code}]")
    }

    override fun onPairingDeleted(code: Int) {
        print("BaseCompletionListener onPairingDeleted(): code [${code}]")
    }

    override fun onCommissioningComplete(nodeId: Long, errorCode: Int) {
        print(
            "BaseCompletionListener onCommissioningComplete(): nodeId [${nodeId}] errorCode [${errorCode}]"
        )
    }

    override fun onNotifyChipConnectionClosed() {
        print("BaseCompletionListener onNotifyChipConnectionClosed()")
    }

    override fun onCloseBleComplete() {
        print("BaseCompletionListener onCloseBleComplete()")
    }

    override fun onError(error: Throwable) {
        print("BaseCompletionListener onError(): $error")
    }

    override fun onOpCSRGenerationComplete(csr: ByteArray) {
        print("BaseCompletionListener onOpCSRGenerationComplete() csr [${csr}]")
    }

    override fun onReadCommissioningInfo(
        vendorId: Int,
        productId: Int,
        wifiEndpointId: Int,
        threadEndpointId: Int
    ) {
        print(
            "onReadCommissioningInfo: vendorId [${vendorId}]  productId [${productId}]  wifiEndpointId [${wifiEndpointId}] threadEndpointId [${threadEndpointId}]"
        )
    }

    override fun onCommissioningStatusUpdate(nodeId: Long, stage: String?, errorCode: Int) {
        print(
            "onCommissioningStatusUpdate nodeId [${nodeId}]  stage [${stage}]  errorCode [${errorCode}]"
        )
    }
}
