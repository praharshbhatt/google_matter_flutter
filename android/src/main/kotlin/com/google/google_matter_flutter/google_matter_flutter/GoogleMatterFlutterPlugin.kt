package com.google.google_matter_flutter.google_matter_flutter

import android.content.Intent
import android.os.Build
import androidx.annotation.NonNull
import androidx.annotation.RequiresApi
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry


/** GoogleMatterFlutterPlugin */
class GoogleMatterFlutterPlugin : FlutterPlugin, MethodCallHandler, ActivityAware,
    PluginRegistry.ActivityResultListener {
    /// The MethodChannel that will the communication between Flutter and native Android
    ///
    /// This local reference serves to register the plugin with the Flutter Engine and unregister it
    /// when the Flutter Engine is detached from the Activity
    private lateinit var channel: MethodChannel

    // Declare Google Matter class
    private lateinit var googleMatter: GoogleMatter
    var activity: android.app.Activity? = null

    override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        channel = MethodChannel(flutterPluginBinding.binaryMessenger, "google_matter_flutter")
        channel.setMethodCallHandler(this)
    }

    @RequiresApi(Build.VERSION_CODES.O_MR1)
    override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
        when (call.method) {
            "commissionDevice" -> {
                val currentActivity = activity
                if (currentActivity == null) {
                    result.error("commissionDevice", "No foreground activity", null)
                    return
                }
                if (!this::googleMatter.isInitialized) {
                    googleMatter = GoogleMatter(currentActivity)
                }
                googleMatter.methodCallResult = result
                googleMatter.commissionDevice()
            }
            "shareDevice" -> {
                val currentActivity = activity
                if (currentActivity == null) {
                    result.error("shareDevice", "No foreground activity", null)
                    return
                }
                if (!this::googleMatter.isInitialized) {
                    googleMatter = GoogleMatter(currentActivity)
                }
                val args = call.arguments as List<*>
                val productId: Int = args[0] as Int
                val vendorId: Int = args[1] as Int
                val discriminator: Int = args[2] as Int
                // Matter passcodes are small (<=8 digits), so the standard codec may encode them
                // as Int32 rather than Int64; use Number to accept either.
                val passCode: Long = (args[3] as Number).toLong()
                googleMatter.methodCallResult = result
                googleMatter.shareDevice(productId, vendorId, discriminator, passCode)
            }
            "getDeviceData" -> {
                val args = call.arguments as List<*>
                val deviceId: Long = args[0] as Long
                val deviceName: String = args[1] as String


                if (!this::googleMatter.isInitialized) {
                    googleMatter = GoogleMatter(activity!!)
                }
                // Device offline / unsupported endpoint throws; don't let it escape to the main looper
                try {
                    result.success(googleMatter.getDeviceData(activity!!, deviceId, deviceName))
                } catch (e: Exception) {
                    result.error("getDeviceData", e.message, null)
                }
            }
            "discoverDevice" -> {
                val args = call.arguments as List<*>
                val deviceId: Long = args[0] as Long

                if (!this::googleMatter.isInitialized) {
                    googleMatter = GoogleMatter(activity!!)
                }
                try {
                    val endpoints = googleMatter.discoverDevice(activity!!, deviceId)
                    result.success(endpoints)
                } catch (e: Exception) {
                    result.error("discoverDevice", e.message, null)
                }
            }
            "performClusterAction" -> {
                val args = call.arguments as List<*>
                val action: String = args[0] as String
                val deviceId: Long = args[1] as Long
                val endpoint: Int = (args[2] as Number).toInt()
                @Suppress("UNCHECKED_CAST")
                val actionArgs: List<Any?> = args.getOrNull(3) as? List<Any?> ?: emptyList()

                if (!this::googleMatter.isInitialized) {
                    googleMatter = GoogleMatter(activity!!)
                }
                try {
                    val actionResult =
                        googleMatter.performClusterAction(
                            activity!!, action, deviceId, endpoint, actionArgs
                        )
                    result.success(actionResult)
                } catch (e: Exception) {
                    result.error("performClusterAction", e.message, null)
                }
            }
            else -> {
                result.notImplemented()
            }
        }
    }

    // https://stackoverflow.com/questions/71876510/how-to-listen-a-startactivityforresult-call-when-using-flutterplugin
    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        activity = binding.activity
        binding.addActivityResultListener(this)

        // Initialize Google Matter class with this activity
        val pending = if (this::googleMatter.isInitialized) googleMatter.methodCallResult else null
        googleMatter = GoogleMatter(binding.activity, pending)
    }

    override fun onDetachedFromActivityForConfigChanges() {
        activity = null;
    }

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
        activity = binding.activity
        binding.addActivityResultListener(this)
        // The activity instance changes on recreation (e.g. fold/unfold), so don't keep the old one
        val pending = if (this::googleMatter.isInitialized) googleMatter.methodCallResult else null
        googleMatter = GoogleMatter(binding.activity, pending)
    }

    override fun onDetachedFromActivity() {
        activity = null;
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
        // Send the result status
        if (!this::googleMatter.isInitialized) return false
        return googleMatter.onActivityResult(requestCode, resultCode, data)
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null);
    }

}
