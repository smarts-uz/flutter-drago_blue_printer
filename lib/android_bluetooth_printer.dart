import 'dart:async';

import 'package:flutter/services.dart';

class AndroidBluetoothPrinter {
  static const MethodChannel _channel =
      const MethodChannel('android_bluetooth_printer');

  /// Print to the first connected bluetooth printer
  static Future<String?> print(
    String text, {
    int? width,
    String? ipAddress,
  }) async {
    final String? version = await _channel.invokeMethod('print', {
      "text": text,
      if (ipAddress != null) "ip_address": ipAddress,
      if (width != null) "width": width,
    });
    return version;
  }
}
