package com.google.google_matter_flutter.google_matter_flutter

import android.content.Intent
import android.os.Build
import androidx.annotation.NonNull
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
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
    private lateinit var methodCallResult: Result

    override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        channel = MethodChannel(flutterPluginBinding.binaryMessenger, "google_matter_flutter")
        channel.setMethodCallHandler(this)
    }

    @RequiresApi(Build.VERSION_CODES.O_MR1)
    override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
        methodCallResult = result
        when (call.method) {
            "commissionDevice" -> {
                if (!this::googleMatter.isInitialized) {
                    googleMatter = GoogleMatter(activity!!, result)
                }
                googleMatter.commissionDevice()
            }
            "getDeviceData" -> {
                val args = call.arguments as List<*>
                val deviceId: Long = args[0] as Long
                val deviceName: String = args[1] as String


                if (!this::googleMatter.isInitialized) {
                    googleMatter = GoogleMatter(activity!!, result)
                }
                val deviceData = googleMatter.getDeviceData(activity!!, deviceId, deviceName)

                result.success(deviceData)
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
        googleMatter = GoogleMatter(activity!!, methodCallResult)
    }

    override fun onDetachedFromActivityForConfigChanges() {
        activity = null;
    }

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
        activity = binding.activity
        binding.addActivityResultListener(this)
    }

    override fun onDetachedFromActivity() {
        activity = null;
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
        // Send the result status
        googleMatter.onActivityResult(requestCode, resultCode, data)

        return resultCode == AppCompatActivity.RESULT_OK
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null);
    }

}
