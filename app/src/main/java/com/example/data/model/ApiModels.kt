package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ProductDto(
  val id: Long,
  val name: String,
  val description: String? = null,
  val price: Double,
  @Json(name = "image_url") val imageUrl: String? = null,
  val available: Int = 1,
  @Json(name = "created_at") val createdAt: String? = null,
  @Json(name = "updated_at") val updatedAt: String? = null,
  val currency: String? = "ETB",
  val category: String? = null,
)

@JsonClass(generateAdapter = true)
data class AuthRequest(
  val email: String,
  val password: String,
)

@JsonClass(generateAdapter = true)
data class AuthResponse(
  @Json(name = "access_token") val accessToken: String,
  @Json(name = "token_type") val tokenType: String? = "bearer",
  val role: String? = "customer",
  val email: String,
)

@JsonClass(generateAdapter = true)
data class UserProfileDto(
  val id: Long,
  val email: String,
  val role: String = "customer",
  @Json(name = "created_at") val createdAt: String? = null,
)

@JsonClass(generateAdapter = true)
data class CreateOrderItemRequest(
  @Json(name = "product_id") val productId: Long,
  val quantity: Int,
)

@JsonClass(generateAdapter = true)
data class CreateOrderRequest(
  val items: List<CreateOrderItemRequest>,
  @Json(name = "customer_note") val customerNote: String = "",
)

@JsonClass(generateAdapter = true)
data class FulfillmentChoiceRequest(
  @Json(name = "fulfillment_method") val fulfillmentMethod: String,
  @Json(name = "delivery_type") val deliveryType: String = fulfillmentMethod,
  @Json(name = "delivery_city") val deliveryCity: String? = null,
  @Json(name = "delivery_sub_city") val deliverySubCity: String? = null,
  @Json(name = "delivery_address") val deliveryAddress: String? = null,
  @Json(name = "delivery_zone_id") val deliveryZoneId: Long? = null,
  @Json(name = "delivery_fee") val deliveryFee: Double? = null,
  @Json(name = "customer_name") val customerName: String? = null,
  @Json(name = "customer_phone") val customerPhone: String? = null,
  val phone: String? = null,
  val note: String? = null,
  val latitude: Double? = null,
  val longitude: Double? = null,
)

@JsonClass(generateAdapter = true)
data class DeliveryQuoteRequest(
  @Json(name = "order_id") val orderId: Long? = null,
  val city: String,
  @Json(name = "sub_city") val subCity: String? = null,
  val address: String,
  @Json(name = "delivery_city") val deliveryCity: String = city,
  @Json(name = "delivery_address") val deliveryAddress: String = address,
  val subtotal: Double? = null,
  val latitude: Double? = null,
  val longitude: Double? = null,
)

@JsonClass(generateAdapter = true)
data class DeliveryQuoteDto(
  @Json(name = "delivery_fee") val deliveryFee: Double? = null,
  val fee: Double? = null,
  val price: Double? = null,
  val subtotal: Double? = null,
  val total: Double? = null,
  val currency: String? = "ETB",
  @Json(name = "delivery_zone_id") val deliveryZoneId: Long? = null,
  @Json(name = "zone_id") val zoneId: Long? = null,
  @Json(name = "delivery_zone_name") val deliveryZoneName: String? = null,
  @Json(name = "zone_name") val zoneName: String? = null,
  @Json(name = "estimated_delivery") val estimatedDelivery: String? = null,
  val message: String? = null,
  val error: String? = null,
)

@JsonClass(generateAdapter = true)
data class OrderItemDto(
  val id: Long,
  @Json(name = "order_id") val orderId: Long? = null,
  @Json(name = "product_id") val productId: Long,
  @Json(name = "product_name") val productName: String,
  @Json(name = "unit_price") val unitPrice: Double,
  val quantity: Int,
  val currency: String? = "ETB",
  @Json(name = "image_url") val imageUrl: String? = null,
)

@JsonClass(generateAdapter = true)
data class PaymentDto(
  val id: Long,
  @Json(name = "order_id") val orderId: Long,
  @Json(name = "proof_filename") val proofFilename: String? = null,
  val status: String,
  @Json(name = "admin_note") val adminNote: String? = null,
  @Json(name = "reviewed_by") val reviewedBy: Long? = null,
  @Json(name = "created_at") val createdAt: String? = null,
  @Json(name = "reviewed_at") val reviewedAt: String? = null,
  val currency: String? = "ETB",
  @Json(name = "payment_method") val paymentMethod: String? = null,
)

/**
 * Matches actual GET /api/payment-methods JSON schema:
 * [{"id":1,"provider_name":"Telebirr","account_number":"0913766553","account_name":"Wude sharifo","instructions":"...","is_active":1}]
 * Uses Any? for active/is_active to support both SQLite integer (1/0) and boolean (true/false).
 */
@JsonClass(generateAdapter = true)
data class PaymentInstructionDto(
  val id: Long? = null,
  @Json(name = "provider_name") val providerName: String? = null,
  val type: String? = null,
  val code: String? = null,
  val provider: String? = null,
  val name: String? = null,
  val title: String? = null,
  @Json(name = "account_name") val accountName: String? = null,
  @Json(name = "account_holder") val accountHolder: String? = null,
  @Json(name = "holder_name") val holderName: String? = null,
  @Json(name = "account_number") val accountNumber: String? = null,
  @Json(name = "merchant_number") val merchantNumber: String? = null,
  val phone: String? = null,
  val instructions: String? = null,
  @Json(name = "admin_instructions") val adminInstructions: String? = null,
  val description: String? = null,
  val note: String? = null,
  val active: Any? = null,
  @Json(name = "is_active") val isActive: Any? = null,
)

@JsonClass(generateAdapter = true)
data class PaymentMethodsEnvelopeDto(
  @Json(name = "payment_methods") val paymentMethods: List<PaymentInstructionDto>? = null,
  val methods: List<PaymentInstructionDto>? = null,
  val data: List<PaymentInstructionDto>? = null,
  val instructions: String? = null,
  @Json(name = "admin_instructions") val adminInstructions: String? = null,
)

@JsonClass(generateAdapter = true)
data class TransactionMessageDto(
  val id: Long? = null,
  @Json(name = "order_id") val orderId: Long? = null,
  val sender: String? = null,
  @Json(name = "sender_role") val senderRole: String? = null,
  @Json(name = "sender_email") val senderEmail: String? = null,
  @Json(name = "message_type") val messageType: String? = null,
  val title: String? = null,
  val message: String? = null,
  val body: String? = null,
  val content: String? = null,
  val status: String? = null,
  @Json(name = "created_at") val createdAt: String? = null,
)

@JsonClass(generateAdapter = true)
data class TransactionMessagesEnvelopeDto(
  val messages: List<TransactionMessageDto>? = null,
  val data: List<TransactionMessageDto>? = null,
)

@JsonClass(generateAdapter = true)
data class StoreConfigDto(
  @Json(name = "store_name") val storeName: String? = null,
  @Json(name = "store_address") val storeAddress: String? = null,
  @Json(name = "pickup_instructions") val pickupInstructions: String? = null,
  @Json(name = "opening_hours") val openingHours: String? = null,
  @Json(name = "delivery_fee") val deliveryFee: Double? = null,
  @Json(name = "pickup_enabled") val pickupEnabled: Any? = null,
)

@JsonClass(generateAdapter = true)
data class RoutePointDto(
  val lat: Double? = null,
  val latitude: Double? = null,
  val lng: Double? = null,
  val lon: Double? = null,
  val longitude: Double? = null,
)

@JsonClass(generateAdapter = true)
data class DeliveryTrackingDto(
  val status: String? = null,
  @Json(name = "sharing_enabled") val sharingEnabled: Any? = null,
  @Json(name = "driver_name") val driverName: String? = null,
  @Json(name = "courier_name") val courierName: String? = null,
  @Json(name = "driver_phone") val driverPhone: String? = null,
  @Json(name = "courier_phone") val courierPhone: String? = null,
  @Json(name = "current_lat") val currentLat: Double? = null,
  @Json(name = "current_lng") val currentLng: Double? = null,
  val latitude: Double? = null,
  val longitude: Double? = null,
  @Json(name = "destination_lat") val destinationLat: Double? = null,
  @Json(name = "destination_lng") val destinationLng: Double? = null,
  @Json(name = "destination_address") val destinationAddress: String? = null,
  @Json(name = "last_updated_at") val lastUpdatedAt: String? = null,
  @Json(name = "updated_at") val updatedAt: String? = null,
  @Json(name = "eta_text") val etaText: String? = null,
  @Json(name = "estimated_arrival") val estimatedArrival: String? = null,
  @Json(name = "expected_delivery_date") val expectedDeliveryDate: String? = null,
  @Json(name = "expected_delivery_window") val expectedDeliveryWindow: String? = null,
  val route: List<RoutePointDto>? = null,
)

@JsonClass(generateAdapter = true)
data class OrderDto(
  val id: Long,
  @Json(name = "customer_id") val customerId: Long? = null,
  val status: String,
  val total: Double,
  val subtotal: Double? = null,
  @Json(name = "customer_note") val customerNote: String? = null,
  @Json(name = "created_at") val createdAt: String? = null,
  @Json(name = "updated_at") val updatedAt: String? = null,
  val currency: String? = "ETB",
  val items: List<OrderItemDto> = emptyList(),
  val payments: List<PaymentDto> = emptyList(),
  @Json(name = "payment_instructions") val paymentInstructions: List<PaymentInstructionDto>? = null,
  @Json(name = "payment_methods") val paymentMethods: List<PaymentInstructionDto>? = null,
  val messages: List<TransactionMessageDto>? = null,
  @Json(name = "transaction_messages") val transactionMessages: List<TransactionMessageDto>? = null,
  @Json(name = "admin_note") val adminNote: String? = null,
  @Json(name = "fulfillment_method") val fulfillmentMethod: String? = null,
  @Json(name = "delivery_type") val deliveryType: String? = null,
  @Json(name = "fulfillment_confirmed") val fulfillmentConfirmed: Any? = null,
  @Json(name = "delivery_fee") val deliveryFee: Double? = null,
  @Json(name = "delivery_zone_id") val deliveryZoneId: Long? = null,
  @Json(name = "delivery_zone_name") val deliveryZoneName: String? = null,
  @Json(name = "delivery_address") val deliveryAddress: String? = null,
  @Json(name = "delivery_city") val deliveryCity: String? = null,
  @Json(name = "customer_phone") val customerPhone: String? = null,
  @Json(name = "customer_name") val customerName: String? = null,
  @Json(name = "pickup_config") val pickupConfig: StoreConfigDto? = null,
  @Json(name = "expected_delivery_date") val expectedDeliveryDate: String? = null,
  @Json(name = "expected_delivery_window") val expectedDeliveryWindow: String? = null,
  @Json(name = "delivery_time_window") val deliveryTimeWindow: String? = null,
  @Json(name = "expected_pickup_date") val expectedPickupDate: String? = null,
  @Json(name = "expected_pickup_window") val expectedPickupWindow: String? = null,
  @Json(name = "delivery_tracking") val deliveryTracking: DeliveryTrackingDto? = null,
)

@JsonClass(generateAdapter = true)
data class CartEntryStorage(
  val productId: Long,
  val name: String,
  val description: String,
  val price: Double,
  val imageUrl: String,
  val quantity: Int,
)

@JsonClass(generateAdapter = true)
data class DeliveryProfileStorage(
  val fullName: String = "",
  val phone: String = "",
  val city: String = "Addis Ababa",
  val subCity: String = "Bole",
  val addressLine: String = "",
  val latitude: Double? = null,
  val longitude: Double? = null,
)
