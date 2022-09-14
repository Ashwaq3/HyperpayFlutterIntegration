package com.example.hyperpay_payment;

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.oppwa.mobile.connect.checkout.dialog.CheckoutActivity
import com.oppwa.mobile.connect.checkout.meta.CheckoutSettings
import com.oppwa.mobile.connect.exception.PaymentError
import com.oppwa.mobile.connect.exception.PaymentException
import com.oppwa.mobile.connect.payment.BrandsValidation
import com.oppwa.mobile.connect.payment.CheckoutInfo
import com.oppwa.mobile.connect.payment.ImagesRequest
import com.oppwa.mobile.connect.payment.PaymentParams
import com.oppwa.mobile.connect.payment.card.CardPaymentParams
import com.oppwa.mobile.connect.provider.Connect
import com.oppwa.mobile.connect.provider.ITransactionListener
import com.oppwa.mobile.connect.provider.Transaction
import com.oppwa.mobile.connect.provider.TransactionType
import com.oppwa.mobile.connect.service.ConnectService
import com.oppwa.mobile.connect.service.IProviderBinder
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

class MainActivity : FlutterActivity(),
    ITransactionListener, MethodChannel.Result {
    private val CHANNEL = "Hyperpay.demo.fultter/channel"
    private var checkoutid: String? = ""
    private var Result: MethodChannel.Result? = null
    private var type: String? = ""
    private var number: String? = null
    private var holder: String? = null
    private var cvv: String? = null
    private var year: String? = null
    private var month: String? = null
    private var brand: String? = null
    private var binder: IProviderBinder? = null
    private var mode: String? = ""
    private val handler = Handler(Looper.getMainLooper())

    fun check(ccNumber: String?): Boolean {
        var sum = 0
        var alternate = false
        for (i in ccNumber!!.length - 1 downTo 0) {
            var n = ccNumber.substring(i, i + 1).toInt()
            if (alternate) {
                n *= 2
                if (n > 9) {
                    n = n % 10 + 1
                }
            }
            sum += n
            alternate = !alternate
        }
        return sum % 10 == 0
    }

    override fun success(result: Any?) {
        handler.post { Result!!.success(result) }
    }

    override fun error(
        errorCode: String, errorMessage: String?, errorDetails: Any?
    ) {
        handler.post { Result!!.error(errorCode, errorMessage, errorDetails) }
    }

    override fun notImplemented() {
        handler.post { Result!!.notImplemented() }
    }

    var ptMadaVExp: String? = ""
    var ptMadaMExp: String? = ""
    var brands: String? = ""
    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        MethodChannel(
            flutterEngine.dartExecutor.binaryMessenger,
            CHANNEL
        ).setMethodCallHandler { call, result ->
            Result = result
            if (call.method == "gethyperpayresponse") {
                type = call.argument("type")
                mode = call.argument("mode")
                checkoutid = call.argument("checkoutid")
                if (type == "ReadyUI") {
                    openCheckoutUI(checkoutid)
                } else {
                    brands = call.argument("brand")
                    number = call.argument("card_number")
                    holder = call.argument("holder_name")
                    year = call.argument("year")
                    month = call.argument("month")
                    cvv = call.argument("cvv")
                    ptMadaVExp = call.argument("MadaRegexV")
                    ptMadaMExp = call.argument("MadaRegexM")
                    openCustomUI(checkoutid)
                }
            } else {
                error("1", "Method name is not found", "")
            }
        }

    }

    private fun openCheckoutUI(checkoutId: String?) {
        val paymentBrands: MutableSet<String> = LinkedHashSet()
        if (brands == "mada") {
            paymentBrands.add("MADA")
        } else {
            paymentBrands.add("VISA")
            paymentBrands.add("MASTER")
        }
        var checkoutSettings = CheckoutSettings(
            checkoutId!!, paymentBrands,
            Connect.ProviderMode.TEST
        ).setShopperResultUrl("com.example.hyperpaypayment://result")
        if (mode == "LIVE") {
            checkoutSettings = CheckoutSettings(
                checkoutId, paymentBrands,
                Connect.ProviderMode.LIVE
            ).setShopperResultUrl("com.example.hyperpaypayment://result")
        }
        val componentName = ComponentName(
            packageName, CheckoutBroadcastReceiver::class.java.getName()
        )


        /* Set up the Intent and start the checkout activity. */
        val intent = checkoutSettings.createCheckoutActivityIntent(this, componentName)
        startActivityForResult(intent, CheckoutActivity.REQUEST_CODE_CHECKOUT)
    }

    private fun openCustomUI(checkoutid: String?) {
        Toast.makeText(baseContext, "Waiting..", Toast.LENGTH_LONG).show()

            val result = check(number)
            if (!result) {
                Toast.makeText(baseContext, "Card Number is Invalid", Toast.LENGTH_LONG).show()
            } else if (!CardPaymentParams.isNumberValid(number)) {
                Toast.makeText(baseContext, "Card Number is Invalid", Toast.LENGTH_LONG).show()
            } else if (!CardPaymentParams.isHolderValid(holder)) {
                Toast.makeText(baseContext, "Card Holder is Invalid", Toast.LENGTH_LONG).show()
            } else if (!CardPaymentParams.isExpiryYearValid(year)) {
                Toast.makeText(baseContext, "Expiry Year is Invalid", Toast.LENGTH_LONG).show()
            } else if (!CardPaymentParams.isExpiryMonthValid(month)) {
                Toast.makeText(baseContext, "Expiry Month is Invalid", Toast.LENGTH_LONG).show()
            } else if (!CardPaymentParams.isCvvValid(cvv)) {
                Toast.makeText(baseContext, "CVV is Invalid", Toast.LENGTH_LONG).show()
            } else {
                val firstnumber = number!![0].toString()

                // To add MADA
                if (brands == "mada") {
                    val bin = number!!.substring(0, 6)
                    if (bin.matches(ptMadaVExp!!.toRegex()) || bin.matches(ptMadaMExp!!.toRegex())) {
                        brand = "MADA"
                    } else {
                        Toast.makeText(
                            this@MainActivity,
                            "This card is not Mada card",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } else {
                    if (firstnumber == "4") {
                        brand = "VISA"
                    } else if (firstnumber == "5") {
                        brand = "MASTER"
                    }
                }
                try {
                    val paymentParams: PaymentParams = CardPaymentParams(
                        checkoutid,
                        brand,
                        number!!,
                        holder,
                        month,
                        year,
                        cvv
                    )
                    paymentParams.shopperResultUrl = "com.example.hyperpaypayment://result"
                    val transaction = Transaction(paymentParams)
                    binder!!.submitTransaction(transaction)
                } catch (e: PaymentException) {
                    e.printStackTrace()
                }
            }

    }

    private val serviceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            binder = service as IProviderBinder
            binder!!.addTransactionListener(this@MainActivity)

            /* we have a connection to the service */try {
                if (mode == "LIVE") {
                    binder!!.initializeProvider(Connect.ProviderMode.LIVE)
                } else {
                    binder!!.initializeProvider(Connect.ProviderMode.TEST)
                }
            } catch (ee: PaymentException) {
            }
        }

        override fun onServiceDisconnected(name: ComponentName) {
            binder = null
        }
    }

    override fun onStart() {
        super.onStart()
        val intent = Intent(this, ConnectService::class.java)
        startService(intent)
        bindService(intent, serviceConnection, BIND_AUTO_CREATE)
    }

    override fun brandsValidationRequestSucceeded(brandsValidation: BrandsValidation) {}
    override fun brandsValidationRequestFailed(paymentError: PaymentError) {}
    override fun imagesRequestSucceeded(imagesRequest: ImagesRequest) {}
    override fun imagesRequestFailed() {}
    override fun paymentConfigRequestSucceeded(checkoutInfo: CheckoutInfo) {}
    override fun paymentConfigRequestFailed(paymentError: PaymentError) {}
    override fun transactionCompleted(transaction: Transaction) {
        if (transaction == null) {
            return
        }
        if (transaction.transactionType == TransactionType.SYNC) {
            success("SYNC")
        } else {
            /* wait for the callback in the s */
            val uri = Uri.parse(transaction.redirectUrl)
            val intent2 = Intent(Intent.ACTION_VIEW, uri)
            startActivity(intent2)
        }
    }

    override fun transactionFailed(transaction: Transaction, paymentError: PaymentError) {}
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        when (resultCode) {
            CheckoutActivity.RESULT_OK -> {
                /* transaction completed */
                val transaction =
                    data.getParcelableExtra<Transaction>(CheckoutActivity.CHECKOUT_RESULT_TRANSACTION)


                /* resource path if needed */
                val resourcePath =
                    data.getStringExtra(CheckoutActivity.CHECKOUT_RESULT_RESOURCE_PATH)
                if (transaction!!.transactionType == TransactionType.SYNC) {
                    /* check the result of synchronous transaction */
                    success("SYNC")
                } else {
                    /* wait for the asynchronous transaction callback in the onNewIntent() */
                }
            }
            CheckoutActivity.RESULT_CANCELED -> {
                /* shopper canceled the checkout process */Toast.makeText(
                    baseContext, "canceled", Toast.LENGTH_LONG
                ).show()
                error("2", "Canceled", "")
            }
            CheckoutActivity.RESULT_ERROR -> {
                /* error occurred */
                val error =
                    data.getParcelableExtra<PaymentError>(CheckoutActivity.CHECKOUT_RESULT_ERROR)
                Toast.makeText(baseContext, "error", Toast.LENGTH_LONG).show()
                Log.e("errorrr", error!!.errorInfo.toString())
                Log.e("errorrr2", error.errorCode.toString())
                Log.e("errorrr3", error.errorMessage)
                Log.e("errorrr4", error.describeContents().toString())
                error("3", "Checkout Result Error", "")
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.scheme == "com.example.hyperpaypayment") {
            success("success")
        }
    }
}