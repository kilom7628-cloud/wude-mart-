package com.example.data.model

import com.example.ui.i18n.AppLanguage
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

object EtbFormatter {
  private val numberFormat: DecimalFormat
    get() = DecimalFormat("#,##0.00", DecimalFormatSymbols(Locale.US))

  /**
   * Single consistent Ethiopian Birr (ETB) format used across the entire Wude Mart app.
   */
  fun format(amount: Double): String {
    val safeAmount = if (amount.isNaN() || amount.isInfinite()) 0.0 else amount
    return "${numberFormat.format(safeAmount)} ብር"
  }
}

object EthiopianPhoneValidator {
  private val regex = Regex("""^(\+251|251|0)?[79]\d{8}$""")

  fun normalize(raw: String): String {
    val cleaned = raw.replace(" ", "").replace("-", "").trim()
    return when {
      cleaned.startsWith("+251") -> cleaned
      cleaned.startsWith("251") && cleaned.length == 12 -> "+$cleaned"
      cleaned.startsWith("0") && cleaned.length == 10 -> "+251${cleaned.substring(1)}"
      cleaned.length == 9 && (cleaned.startsWith("9") || cleaned.startsWith("7")) -> "+251$cleaned"
      else -> cleaned
    }
  }

  fun isValid(raw: String): Boolean {
    val cleaned = raw.replace(" ", "").replace("-", "").trim()
    return regex.matches(cleaned)
  }
}

data class Product(
  val id: Long,
  val name: String,
  val description: String,
  val price: Double,
  val imageUrl: String,
  val available: Boolean,
  val createdAt: String,
  val updatedAt: String,
  val currency: String,
  val categoryLabel: String,
)

data class CartItem(
  val product: Product,
  val quantity: Int,
) {
  val lineTotal: Double
    get() = product.price * quantity
}

enum class FulfillmentMethod(val apiValue: String) {
  DELIVERY("delivery"),
  STORE_PICKUP("pickup");

  companion object {
    fun fromApi(value: String?): FulfillmentMethod? {
      val norm = value?.lowercase(Locale.ROOT)?.trim().orEmpty()
      return when {
        norm.contains("pickup") || norm.contains("collect") -> STORE_PICKUP
        norm.contains("deliver") -> DELIVERY
        else -> null
      }
    }
  }
}

data class DeliveryFeeQuote(
  val deliveryFee: Double,
  val subtotal: Double? = null,
  val total: Double? = null,
  val zoneId: Long? = null,
  val zoneName: String = "",
  val estimatedDelivery: String? = null,
)

data class StorePickupConfig(
  val storeName: String,
  val storeAddress: String,
  val pickupInstructions: String,
  val openingHours: String,
  val deliveryFee: Double?,
  val pickupEnabled: Boolean = true,
)

data class GeoPoint(
  val lat: Double,
  val lng: Double,
)

data class DeliveryTrackingInfo(
  val status: String,
  val sharingEnabled: Boolean,
  val driverName: String,
  val driverPhone: String,
  val currentLocation: GeoPoint?,
  val destinationLocation: GeoPoint?,
  val destinationAddress: String,
  val lastUpdatedAt: String,
  val etaText: String,
  val expectedDeliveryDate: String?,
  val expectedDeliveryWindow: String?,
  val route: List<GeoPoint>,
)

enum class OrderStatus(
  val apiValue: String,
  val displayLabel: String,
  val amharicLabel: String,
  val stepIndex: Int,
) {
  PENDING_PAYMENT("pending_payment", "Awaiting Payment", "ክፍያ በመጠባበቅ ላይ", 1),
  PAYMENT_REVIEW("payment_review", "Payment Under Review", "ክፍያ በማጣራት ላይ", 2),
  PAID("paid", "Payment Verified", "ክፍያ ተረጋግጧል", 3),
  CONFIRMED("confirmed", "Order Confirmed", "ትዕዛዝ ተረጋግጧል", 3),
  PROCESSING("processing", "Preparing Order", "ትዕዛዝ በዝግጅት ላይ", 4),
  SHIPPED("shipped", "Out for Delivery", "ለመድረስ በመንገድ ላይ", 5),
  DELIVERED("delivered", "Delivered", "ደርሷል", 6),
  CANCELLED("cancelled", "Cancelled", "ተሰርዟል", -1),
  REJECTED("rejected", "Payment Rejected", "ክፍያ ውድቅ ተደርጓል", -2);

  fun labelFor(language: AppLanguage): String =
    if (language == AppLanguage.AMHARIC) amharicLabel else displayLabel

  companion object {
    fun fromApi(value: String): OrderStatus {
      val normalized = value.lowercase(Locale.ROOT).trim()
      return entries.firstOrNull { it.apiValue == normalized }
        ?: when {
          normalized.contains("review") -> PAYMENT_REVIEW
          normalized.contains("pending") -> PENDING_PAYMENT
          normalized.contains("confirm") -> CONFIRMED
          normalized.contains("paid") || normalized.contains("approv") -> PAID
          normalized.contains("process") || normalized.contains("pack") -> PROCESSING
          normalized.contains("ship") || normalized.contains("transit") || normalized.contains("delivering") || normalized.contains("dispatch") -> SHIPPED
          normalized.contains("deliver") || normalized.contains("complet") -> DELIVERED
          normalized.contains("reject") -> REJECTED
          normalized.contains("cancel") -> CANCELLED
          else -> PENDING_PAYMENT
        }
    }
  }
}

data class OrderItem(
  val id: Long,
  val productId: Long,
  val productName: String,
  val unitPrice: Double,
  val quantity: Int,
  val currency: String,
  val imageUrl: String,
) {
  val lineTotal: Double
    get() = unitPrice * quantity
}

data class PaymentRecord(
  val id: Long,
  val orderId: Long,
  val proofFilename: String,
  val status: String,
  val adminNote: String,
  val createdAt: String,
  val reviewedAt: String?,
  val currency: String,
  val paymentMethod: String = "",
)

data class PaymentInstruction(
  val id: String,
  val providerName: String,
  val accountName: String,
  val accountNumber: String,
  val instructions: String,
  val isTelebirr: Boolean = false,
)

enum class MessageTone {
  INFO,
  PENDING,
  APPROVED,
  REJECTED,
}

data class TransactionMessage(
  val id: String,
  val senderLabel: String,
  val title: String,
  val body: String,
  val timestamp: String,
  val tone: MessageTone,
)

data class Order(
  val id: Long,
  val status: OrderStatus,
  val rawStatus: String,
  val total: Double,
  val subtotal: Double = total,
  val customerNote: String,
  val createdAt: String,
  val updatedAt: String,
  val currency: String,
  val items: List<OrderItem>,
  val payments: List<PaymentRecord>,
  val paymentInstructions: List<PaymentInstruction>,
  val messages: List<TransactionMessage>,
  val fulfillmentMethod: FulfillmentMethod? = null,
  val fulfillmentConfirmed: Boolean = false,
  val deliveryFee: Double? = null,
  val deliveryZoneId: Long? = null,
  val deliveryZoneName: String = "",
  val deliveryAddress: String = "",
  val deliveryCity: String = "",
  val pickupInfo: StorePickupConfig? = null,
  val expectedDeliveryDate: String? = null,
  val expectedDeliveryWindow: String? = null,
  val expectedPickupDate: String? = null,
  val expectedPickupWindow: String? = null,
  val deliveryTracking: DeliveryTrackingInfo? = null,
) {
  val effectiveProductSubtotal: Double
    get() =
      when {
        subtotal > 0.0 -> subtotal
        items.isNotEmpty() -> items.sumOf { it.lineTotal }
        deliveryFee != null -> (total - deliveryFee).coerceAtLeast(0.0)
        else -> total
      }

  val isPaymentVerified: Boolean
    get() =
      status == OrderStatus.PAID ||
        status == OrderStatus.CONFIRMED ||
        status == OrderStatus.PROCESSING ||
        status == OrderStatus.SHIPPED ||
        status == OrderStatus.DELIVERED ||
        payments.any {
          val s = it.status.lowercase(Locale.ROOT)
          s == "approved" || s == "verified" || s == "paid"
        }

  val latestRejectedPayment: PaymentRecord?
    get() {
      val last = payments.maxByOrNull { it.id } ?: return null
      val s = last.status.lowercase(Locale.ROOT)
      return if (s == "rejected" || s == "failed") last else null
    }

  val isDispatchedOrTrackable: Boolean
    get() =
      status == OrderStatus.SHIPPED ||
        (deliveryTracking != null &&
          (deliveryTracking.driverName.isNotBlank() ||
            deliveryTracking.currentLocation != null ||
            deliveryTracking.sharingEnabled))
}

data class CustomerSession(
  val token: String,
  val email: String,
  val role: String,
  val userId: Long? = null,
  val createdAt: String? = null,
) {
  val isSignedIn: Boolean
    get() = token.isNotBlank()
}

enum class ProductSortOption(val labelEn: String, val labelAm: String) {
  FEATURED("Featured", "ተመራጭ"),
  PRICE_LOW_HIGH("Price: Low to High", "ዋጋ: ከዝቅተኛ ወደ ከፍተኛ"),
  PRICE_HIGH_LOW("Price: High to Low", "ዋጋ: ከከፍተኛ ወደ ዝቅተኛ"),
  NEWEST("Newest Arrivals", "አዲስ የገቡ");

  fun labelFor(language: AppLanguage): String =
    if (language == AppLanguage.AMHARIC) labelAm else labelEn
}

val ETHIOPIAN_CITIES =
  listOf(
    "Addis Ababa",
    "Adama",
    "Bahir Dar",
    "Hawassa",
    "Dire Dawa",
    "Mekelle",
    "Gondar",
    "Jimma",
    "Dessie",
    "Bishoftu",
  )

val ADDIS_SUB_CITIES =
  listOf(
    "Bole",
    "Yeka",
    "Kirkos",
    "Arada",
    "Lideta",
    "Nifas Silk-Lafto",
    "Akaky Kaliti",
    "Kolfe Keranio",
    "Gullele",
    "Addis Ketema",
    "Lemi Kura",
  )
