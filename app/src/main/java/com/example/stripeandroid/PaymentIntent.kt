data class PaymentIntentResponse(
    val id: String,
    val objec: String,
    val amount: Int,
    val amount_capturable: Int,
    val amount_details: AmountDetails,
    val amount_received: Int,
    val application: Any?,
    val application_fee_amount: Any?,
    val automatic_payment_methods: Any?,
    val canceled_at: Any?,
    val cancellation_reason: Any?,
    val capture_method: String,
    val client_secret: String,
    val confirmation_method: String,
    val created: Long,
    val currency: String,
    val customer: Any?,
    val description: String,
    val invoice: Any?,
    val last_payment_error: Any?,
    val latest_charge: Any?,
    val livemode: Boolean,
    val metadata: Map<String, Any>,
    val next_action: Any?,
    val on_behalf_of: Any?,
    val payment_method: Any?,
    val payment_method_options: PaymentMethodOptions,
    val payment_method_types: List<String>,
    val processing: Any?,
    val receipt_email: Any?,
    val redaction: Any?,
    val review: Any?,
    val setup_future_usage: Any?,
    val shipping: Any?,
    val statement_descriptor: Any?,
    val statement_descriptor_suffix: Any?,
    val status: String,
    val transfer_data: Any?,
    val transfer_group: Any?
)

data class AmountDetails(
    val tip: Map<String, Any>
)

data class PaymentMethodOptions(
    val card: CardOptions
)

data class CardOptions(
    val installments: Any?,
    val mandate_options: Any?,
    val network: Any?,
    val request_three_d_secure: String
)
