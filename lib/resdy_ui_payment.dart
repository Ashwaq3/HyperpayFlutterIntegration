import 'package:cloud_functions/cloud_functions.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

class ReadyUIPayment extends StatefulWidget {
  const ReadyUIPayment({Key? key}) : super(key: key);

  @override
  ReadyUIPaymentState createState() => ReadyUIPaymentState();
}

String _checkOutId = '';
String _resultText = '';

class ReadyUIPaymentState extends State<ReadyUIPayment> {
  static const platform = MethodChannel('Hyperpay.demo.fultter/channel');

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
        home: Scaffold(
            appBar: AppBar(title: const Text('READY UI')),
            body: Padding(
                padding: const EdgeInsets.all(10.0),
                child: Center(
                    child: SingleChildScrollView(
                        child: Column(
                            mainAxisAlignment: MainAxisAlignment.center,
                            children: [
                      ElevatedButton(
                          child: const Text('Credit Card'),
                          onPressed: () async {
                            _checkOutPage("credit");
                          }),
                      const SizedBox(height: 35),
                      Text(_resultText,
                          style: const TextStyle(
                              color: Colors.green, fontSize: 20))
                    ]))))));
  }

  Future<void> _checkOutPage(String type) async {
    var response = await FirebaseFunctions.instance
        .httpsCallable("checkoutId")
        .call('tes');

    _checkOutId = '${response.data["id"]}';
    String transactionStatus;
    try {
      final String result = await platform.invokeMethod('gethyperpayresponse', {
        "type": "ReadyUI",
        "mode": "TEST",
        "checkoutid": _checkOutId,
        "brand": type,
      });
      transactionStatus = result;
    } on PlatformException catch (e) {
      transactionStatus = "${e.message}";
    }

    if (transactionStatus == "success" || transactionStatus == "SYNC") {
      getPaymentStatus();
    } else {
      setState(() {
        _resultText = transactionStatus;
      });
    }
  }

  Future<void> getPaymentStatus() async {
    var response = await FirebaseFunctions.instance
        .httpsCallable("paymentStatus")
        .call(_checkOutId);

    setState(() {
      _resultText = response.data["result"].toString();
    });
  }
}
