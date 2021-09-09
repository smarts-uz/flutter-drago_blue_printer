package com.renseki.lib.android_bluetooth_printer

import androidx.annotation.NonNull
import com.dantsu.escposprinter.EscPosPrinter
import com.dantsu.escposprinter.connection.bluetooth.BluetoothPrintersConnections
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result

class AndroidBluetoothPrinterPlugin : FlutterPlugin, MethodCallHandler {
    private lateinit var channel: MethodChannel

    override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        channel = MethodChannel(flutterPluginBinding.binaryMessenger, "android_bluetooth_printer")
        channel.setMethodCallHandler(this)
    }

    override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
        if (call.method == "print") {
            val text = (call.arguments as? Map<*, *>)?.get("text") as? String

            if (text == null || text.isBlank()) {
                result.error(
                    "400",
                    "Please supply this print method with a text to print",
                    null,
                )
            } else {
                print(text)
                result.success("printed")
            }
        } else {
            result.notImplemented()
        }
    }

    private fun print(text: String) {
        val printer = EscPosPrinter(
            BluetoothPrintersConnections.selectFirstPaired(),
            203,
            58f,
            32,
        )

        printer.printFormattedText(text)

        printer.disconnectPrinter()
    }

    override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
    }
}
