
import 'dart:async';

import 'package:flutter/services.dart';

class AndroidBluetoothPrinter {
  static const MethodChannel _channel =
      const MethodChannel('android_bluetooth_printer');

  static Future<String?> print(String text) async {
    final String? version = await _channel.invokeMethod('print', {
      "text": text,
    });
    return version;
  }
}
