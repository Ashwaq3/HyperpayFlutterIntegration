import 'package:firebase_core/firebase_core.dart';
import 'package:flutter/material.dart';
import 'package:hyperpay_payment/resdy_ui_payment.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await Firebase.initializeApp();

  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return const MaterialApp(
      debugShowCheckedModeBanner: false,
      home: ReadyUIPayment(),
    );
  }
}
