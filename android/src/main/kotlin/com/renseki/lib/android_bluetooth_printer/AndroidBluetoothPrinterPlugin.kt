package com.renseki.lib.android_bluetooth_printer

import android.content.Context
import com.dantsu.escposprinter.EscPosPrinter
import com.dantsu.escposprinter.connection.bluetooth.BluetoothConnection
import com.dantsu.escposprinter.connection.bluetooth.BluetoothPrintersConnections
import com.dantsu.escposprinter.connection.tcp.TcpConnection
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result


class AndroidBluetoothPrinterPlugin : FlutterPlugin, MethodCallHandler {
    private lateinit var channel: MethodChannel
    private lateinit var context: Context

    override fun onAttachedToEngine(flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        channel = MethodChannel(flutterPluginBinding.binaryMessenger, "android_bluetooth_printer")
        channel.setMethodCallHandler(this)
        context = flutterPluginBinding.applicationContext
    }

    override fun onMethodCall(call: MethodCall, result: Result) {
        if (call.method == "print") {
            val text = (call.arguments as? Map<*, *>)?.get("text") as? String
            val width = (call.arguments as? Map<*, *>)?.get("width") as? Int
            val ipAddress = (call.arguments as? Map<*, *>)?.get("ip_address") as? String

            if (text.isNullOrBlank()) {
                result.error(
                    "400",
                    "Please supply this print method with a text to print",
                    null,
                )
            } else {
                print(text, ipAddress, width, {
                    result.success("printed")
                }) { e ->
                    result.error(
                        "500",
                        e.message ?: e.toString(),
                        null,
                    )
                }
            }
        } else {
            result.notImplemented()
        }
    }

    private fun getPrinter(printerAddress: String?): BluetoothConnection {
        val bluetoothPrinters = BluetoothPrintersConnections().list

        if (bluetoothPrinters.isNullOrEmpty()) {
            throw Exception("No bluetooth printers found")
        }

        if (printerAddress != null) {
            val lastPrinter = bluetoothPrinters.firstOrNull { it.device.address == printerAddress }
            if (lastPrinter != null) try {
                return lastPrinter.connect()
            } catch (ignore: Exception) {
                ignore.printStackTrace()
            }
        }

        for (printer in bluetoothPrinters) {
            try {
                printer.connect()
                context.setLastPrinterAddress(printer.device.address)
                return printer
            } catch (ignore: Exception) {
                continue
            }
        }

        throw Exception("No bluetooth printers can be connected to")
    }

    private fun Context.setLastPrinterAddress(address: String) {
        getSharedPreferences("android_bluetooth_print_settings", Context.MODE_PRIVATE)
            .edit()
            .putString("LAST_PRINTER_ADDRESS", address)
            .apply()
    }

    private fun Context.getLastPrinterAddress(): String? {
        return getSharedPreferences(
            "android_bluetooth_print_settings",
            Context.MODE_PRIVATE
        ).getString(
            "LAST_PRINTER_ADDRESS",
            null
        )
    }

    private fun print(
        text: String,
        ipAddress: String?,
        width: Int?,
        onSuccess: () -> Unit,
        onError: (Throwable) -> Unit,
    ) {
        val widthInMM = when (width) {
            32 -> 58f
            33 -> 58f
            42 -> 80f
            else -> 58f
        }

        val maxChars = width ?: 32

        object : Thread() {
            override fun run() {
                val printer = if (ipAddress.isNullOrBlank()) {
                    EscPosPrinter(
                        getPrinter(context.getLastPrinterAddress()),
                        203,
                        widthInMM,
                        maxChars,
                    )
                } else {
                    EscPosPrinter(
                        TcpConnection(
                            ipAddress.split(":").first(),
                            ipAddress.split(":").last().toInt(),
                            1000,
                        ),
                        203,
                        widthInMM,
                        maxChars,
                    )
                }

                printer.printFormattedTextAndCut(text)

                onSuccess()
            }
        }.also {
            it.uncaughtExceptionHandler = Thread.UncaughtExceptionHandler { _, ex -> onError(ex) }
            it.start()
        }
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
    }
}
