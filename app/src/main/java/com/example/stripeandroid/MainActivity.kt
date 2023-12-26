import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.annotation.Nullable
import androidx.appcompat.app.AppCompatActivity
import com.example.stripeandroid.R
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.stripe.android.ApiResultCallback
import com.stripe.android.PaymentIntentResult
import com.stripe.android.Stripe
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.StripeIntent
import com.stripe.android.view.CardInputWidget
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import java.lang.ref.WeakReference
import java.lang.reflect.Type
import java.util.Objects
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.collections.Map
import kotlin.collections.MutableList
import kotlin.collections.MutableMap
import kotlin.collections.set


class MainActivity : AppCompatActivity() {
    var amountText: EditText? = null
    var cardInputWidget: CardInputWidget? = null
    var payButton: Button? = null

    // we need paymentIntentClientSecret to start transaction
    private var paymentIntentClientSecret: String? = null

    //declare stripe
    private var stripe: Stripe? = null
    var amountDouble: Double? = null
    private var httpClient: OkHttpClient? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        amountText = findViewById(R.id.amount_id);
        cardInputWidget = findViewById(R.id.cardInputWidget);
        payButton = findViewById(R.id.payButton);


        progressDialog = ProgressDialog(this)
        progressDialog!!.setTitle("Transaction in progress")
        progressDialog!!.setCancelable(false)
        httpClient = OkHttpClient()

        //Initialize
        stripe = Stripe(
            applicationContext,
            Objects.requireNonNull("pk_test_51ISLoeDrYpYnN0xnqW7bZ0tJKmtxUEdYOhD8AXoO10S9aMSXZ8Hk6e7EXJvKpn476isXZXgdG5R5TAj7aVXceJZo00bIx1MjgM")
        )
        payButton!!.setOnClickListener(View.OnClickListener { //get Amount
            amountDouble = amountText!!.text.toString() as Double
            //call checkout to get paymentIntentClientSecret key
            progressDialog!!.show()
            startCheckout()
        })
    }

    private fun startCheckout() {
        run {

            // Create a PaymentIntent by calling the server's endpoint.
            val mediaType: MediaType = "application/json; charset=utf-8".toMediaTypeOrNull()!!
            //        String json = "{"
//                + "\"currency\":\"usd\","
//                + "\"items\":["
//                + "{\"id\":\"photo_subscription\"}"
//                + "]"
//                + "}";
            val amount = amountDouble!! * 100
            val payMap: MutableMap<String, Any> =
                HashMap()
            val itemMap: MutableMap<String, Any> =
                HashMap()
            val itemList: MutableList<Map<String, Any>> =
                ArrayList()
            payMap["currency"] = "INR"
            itemMap["id"] = "photo_subscription"
            itemMap["amount"] = amount
            itemList.add(itemMap)
            payMap["items"] = itemList
            val json = Gson().toJson(payMap)
            val body: RequestBody = json.toRequestBody(mediaType)
            val request: Request = Request.Builder()
                .url(BACKEND_URL + "create-payment-intent")
                .post(body)
                .build()
            httpClient!!.newCall(request)
                .enqueue(PayCallback(this))
        }
    }

    private class PayCallback internal constructor(activity: MainActivity) : Callback {
        private val activityRef: WeakReference<MainActivity>

        init {
            activityRef = WeakReference(activity)
        }

        override fun onFailure(call: Call, e: IOException) {
            progressDialog!!.dismiss()
            val activity = activityRef.get() ?: return
            activity.runOnUiThread {
                Toast.makeText(
                    activity, "Error: $e", Toast.LENGTH_LONG
                ).show()
            }
        }

        @Throws(IOException::class)
        override fun onResponse(call: Call, response: Response) {
            val activity = activityRef.get() ?: return
            if (!response.isSuccessful) {
                activity.runOnUiThread {
                    Toast.makeText(
                        activity, "Error: $response", Toast.LENGTH_LONG
                    ).show()
                }
            } else {
                activity.onPaymentSuccess(response)
            }
        }
    }

    @Throws(IOException::class)
    private fun onPaymentSuccess(response: Response) {
        val gson = Gson()
        val type: Type = object : TypeToken<Map<String?, String?>?>() {}.type
        val responseMap = gson.fromJson<Map<String, String>>(
            Objects.requireNonNull(response.body)!!.string(),
            type
        )
        paymentIntentClientSecret = responseMap["clientSecret"]

        //once you get the payment client secret start transaction
        //get card detail
        val params = cardInputWidget!!.paymentMethodCreateParams
        if (params != null) {
            //now use paymentIntentClientSecret to start transaction
            val confirmParams = ConfirmPaymentIntentParams.createWithPaymentMethodCreateParams(
                params,
                paymentIntentClientSecret!!
            )
            //start payment
            stripe!!.confirmPayment(this@MainActivity, confirmParams)
        }
        Log.i("TAG", "onPaymentSuccess: $paymentIntentClientSecret")
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, @Nullable data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        // Handle the result of stripe.confirmPayment
        stripe!!.onPaymentResult(requestCode, data, PaymentResultCallback(this))
    }

    private class PaymentResultCallback internal constructor(activity: MainActivity) :
        ApiResultCallback<PaymentIntentResult?> {
        private val activityRef: WeakReference<MainActivity>

        init {
            activityRef = WeakReference(activity)
        }

        //If Payment is successful

        //If Payment is not successful
        override fun onError(e: Exception) {
            progressDialog!!.dismiss()
            val activity = activityRef.get() ?: return
            // Payment request failed – allow retrying using the same payment method
            activity.displayAlert("Error", e.toString())
        }

        override fun onSuccess(result: PaymentIntentResult?) {
            progressDialog!!.dismiss()
            val activity = activityRef.get() ?: return
            val (_, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, status, _, lastPaymentError) = result!!.intent
            if (status == StripeIntent.Status.Succeeded) {
                // Payment completed successfully
                val gson = GsonBuilder().setPrettyPrinting().create()
                val toast = Toast.makeText(activity, "Ordered Successful", Toast.LENGTH_SHORT)
                toast.setGravity(Gravity.CENTER, 0, 0)
                toast.show()
            } else if (status == StripeIntent.Status.RequiresPaymentMethod) {
                // Payment failed – allow retrying using a different payment method
                activity.displayAlert(
                    "Payment failed",
                    Objects.requireNonNull<PaymentIntent.Error?>(lastPaymentError).message
                )
            }        }
    }

    private fun displayAlert(
        title: String,
        @Nullable message: String?
    ) {
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
        builder.setPositiveButton("Ok", null)
        builder.create().show()
    }

    companion object {
        // 10.0.2.2 is the Android emulator's alias to localhost
        // 192.168.1.6 If you are testing in real device with usb connected to same network then use your IP address
        private const val BACKEND_URL =
            "http://192.168.1.6:4242/" //4242 is port mentioned in server i.e index.js
        var progressDialog: ProgressDialog? = null
    }
}