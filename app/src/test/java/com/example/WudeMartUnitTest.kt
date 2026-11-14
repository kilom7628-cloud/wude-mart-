package com.example

import com.example.data.model.EtbFormatter
import com.example.data.model.EthiopianPhoneValidator
import com.example.data.model.FulfillmentMethod
import com.example.data.model.MessageTone
import com.example.data.model.Order
import com.example.data.model.OrderDto
import com.example.data.model.OrderItem
import com.example.data.model.OrderStatus
import com.example.data.model.PaymentInstruction
import com.example.data.model.PaymentRecord
import com.example.data.model.TransactionMessage
import com.example.data.remote.NetworkModule
import com.example.data.repository.WudeMartRepository
import com.example.data.repository.toDomain
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.stringsFor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WudeMartUnitTest {

  @Test
  fun `etb formatter formats consistent birr price without foreign currency`() {
    val a = EtbFormatter.format(1250.0)
    val b = EtbFormatter.format(2500.5)
    val safeZero = EtbFormatter.format(Double.NaN)

    assertEquals("1,250.00 ብር", a)
    assertEquals("2,500.50 ብር", b)
    assertEquals("0.00 ብር", safeZero)
    assertFalse(a.contains("$") || a.contains("USD"))
    assertFalse(b.contains("$") || b.contains("USD"))
  }

  @Test
  fun `ethiopian phone validator accepts valid formats and normalizes`() {
    assertTrue(EthiopianPhoneValidator.isValid("+251911223344"))
    assertTrue(EthiopianPhoneValidator.isValid("0911223344"))
    assertTrue(EthiopianPhoneValidator.isValid("0711223344"))
    assertFalse(EthiopianPhoneValidator.isValid("12345"))
    assertEquals("+251911223344", EthiopianPhoneValidator.normalize("0911223344"))
    assertEquals("+251711223344", EthiopianPhoneValidator.normalize("0711223344"))
  }

  @Test
  fun `order status maps backend statuses accurately`() {
    assertEquals(OrderStatus.PENDING_PAYMENT, OrderStatus.fromApi("pending_payment"))
    assertEquals(OrderStatus.PAYMENT_REVIEW, OrderStatus.fromApi("payment_review"))
    assertEquals(OrderStatus.PAID, OrderStatus.fromApi("paid"))
    assertEquals(OrderStatus.PAID, OrderStatus.fromApi("approved"))
    assertEquals(OrderStatus.DELIVERED, OrderStatus.fromApi("delivered"))
    assertEquals(OrderStatus.REJECTED, OrderStatus.fromApi("rejected"))
    assertEquals(OrderStatus.CANCELLED, OrderStatus.fromApi("cancelled"))
  }

  @Test
  fun `parses real production payment methods json with provider_name and numeric is_active`() {
    val livePayload =
      """
      [
        {
          "id": 1,
          "provider_name": "Telebirr",
          "account_number": "0913766553",
          "account_name": "Wude sharifo",
          "instructions": "Transfer to Telebirr phone number or merchant pay.",
          "is_active": 1
        },
        {
          "id": 2,
          "provider_name": "Inactive Bank",
          "account_number": "1000999999",
          "account_name": "Disabled",
          "instructions": "Do not use",
          "is_active": 0
        }
      ]
      """.trimIndent()

    val parsed = WudeMartRepository.parsePaymentMethodsJson(livePayload, NetworkModule.moshi)

    assertEquals(1, parsed.size)
    val telebirr = parsed.first()
    assertEquals("Telebirr", telebirr.providerName)
    assertEquals("0913766553", telebirr.accountNumber)
    assertEquals("Wude sharifo", telebirr.accountName)
    assertTrue(telebirr.isTelebirr)
    assertTrue(telebirr.instructions.contains("Telebirr"))
  }

  @Test
  fun `amharic localization and order status labels are populated`() {
    val am = stringsFor(AppLanguage.AMHARIC)
    val en = stringsFor(AppLanguage.ENGLISH)

    assertTrue(am.navShop.isNotBlank())
    assertTrue(am.wishlistTitle.isNotBlank())
    assertTrue(am.deliveryDateWillBeConfirmed.isNotBlank())
    assertEquals("Your payment has been verified!", en.paymentVerifiedTitle)
    assertEquals("ክፍያዎ ተረጋግጧል!", am.paymentVerifiedTitle)
    assertEquals("ክፍያ ተረጋግጧል", OrderStatus.PAID.labelFor(AppLanguage.AMHARIC))
  }

  @Test
  fun `order verification, rejection detection, and fulfillment mapping work accurately`() {
    assertEquals(FulfillmentMethod.DELIVERY, FulfillmentMethod.fromApi("delivery"))
    assertEquals(FulfillmentMethod.STORE_PICKUP, FulfillmentMethod.fromApi("pickup"))
    assertEquals(FulfillmentMethod.STORE_PICKUP, FulfillmentMethod.fromApi("store_pickup"))
    assertNull(FulfillmentMethod.fromApi(null))

    val rejectedPendingOrder =
      Order(
        id = 3,
        status = OrderStatus.PENDING_PAYMENT,
        rawStatus = "pending_payment",
        total = 350.0,
        customerNote = "",
        createdAt = "",
        updatedAt = "",
        currency = "ETB",
        items = emptyList(),
        payments =
          listOf(
            PaymentRecord(
              id = 12,
              orderId = 3,
              proofFilename = "receipt.jpg",
              status = "rejected",
              adminNote = "Blurry screenshot",
              createdAt = "2026-01-01T10:00:00Z",
              reviewedAt = null,
              currency = "ETB",
            )
          ),
        paymentInstructions = emptyList(),
        messages = emptyList(),
      )
    assertFalse(rejectedPendingOrder.isPaymentVerified)
    assertNotNull(rejectedPendingOrder.latestRejectedPayment)
    assertFalse(rejectedPendingOrder.isDispatchedOrTrackable)

    val paidOrder = rejectedPendingOrder.copy(status = OrderStatus.PAID, payments = emptyList())
    assertTrue(paidOrder.isPaymentVerified)

    val shippedOrder = paidOrder.copy(status = OrderStatus.SHIPPED)
    assertTrue(shippedOrder.isDispatchedOrTrackable)
  }

  @Test
  fun `payment instruction and transaction message preserve backend fields`() {
    val telebirr =
      PaymentInstruction(
        id = "telebirr",
        providerName = "Telebirr",
        accountName = "Wude Mart Trading",
        accountNumber = "0911000000",
        instructions = "Transfer exact ETB total and upload screenshot.",
        isTelebirr = true,
      )
    val message =
      TransactionMessage(
        id = "pay_10",
        senderLabel = "Wude Mart Admin",
        title = "Payment Approved",
        body = "Receipt verified.",
        timestamp = "2026-01-01T10:00:00Z",
        tone = MessageTone.APPROVED,
      )

    assertTrue(telebirr.isTelebirr)
    assertEquals("Wude Mart Trading", telebirr.accountName)
    assertEquals(MessageTone.APPROVED, message.tone)
  }

  @Test
  fun `parses backend delivery quote json and rejects invalid payloads`() {
    val validJson =
      """
      {
        "delivery_fee": 150.0,
        "subtotal": 1200.0,
        "total": 1350.0,
        "delivery_zone_id": 7,
        "delivery_zone_name": "Bole - Addis Ababa",
        "estimated_delivery": "Same day"
      }
      """.trimIndent()
    val quote = WudeMartRepository.parseDeliveryQuoteJson(validJson, NetworkModule.moshi)
    assertNotNull(quote)
    assertEquals(150.0, quote!!.deliveryFee, 0.001)
    assertEquals(1200.0, quote.subtotal!!, 0.001)
    assertEquals(1350.0, quote.total!!, 0.001)
    assertEquals(7L, quote.zoneId)
    assertEquals("Bole - Addis Ababa", quote.zoneName)
    assertEquals("Same day", quote.estimatedDelivery)

    val altJson = """{"fee": 0.0, "zone_id": 2, "zone_name": "Central"}"""
    val zeroQuote = WudeMartRepository.parseDeliveryQuoteJson(altJson, NetworkModule.moshi)
    assertNotNull(zeroQuote)
    assertEquals(0.0, zeroQuote!!.deliveryFee, 0.001)
    assertEquals(2L, zeroQuote.zoneId)
    assertEquals("Central", zeroQuote.zoneName)

    assertNull(WudeMartRepository.parseDeliveryQuoteJson("""{"error": "Zone not covered"}""", NetworkModule.moshi))
    assertNull(WudeMartRepository.parseDeliveryQuoteJson("""{"delivery_fee": -50}""", NetworkModule.moshi))
    assertNull(WudeMartRepository.parseDeliveryQuoteJson("", NetworkModule.moshi))
  }

  @Test
  fun `order dto default delivery_type without address or confirmation leaves fulfillmentMethod null`() {
    val liveStylePaidOrderDto =
      OrderDto(
        id = 4L,
        status = "paid",
        total = 1250.0,
        subtotal = 1250.0,
        deliveryType = "delivery",
        deliveryFee = 0.0,
        deliveryZoneId = null,
        deliveryZoneName = "",
        deliveryAddress = "",
        deliveryCity = "",
      )
    val unconfirmedDomain = liveStylePaidOrderDto.toDomain()
    assertTrue(unconfirmedDomain.isPaymentVerified)
    assertNull(unconfirmedDomain.fulfillmentMethod)
    assertEquals(1250.0, unconfirmedDomain.effectiveProductSubtotal, 0.001)

    val confirmedDeliveryDto =
      liveStylePaidOrderDto.copy(
        deliveryZoneId = 3L,
        deliveryZoneName = "Bole",
        deliveryAddress = "Bole Medhanialem",
        deliveryCity = "Addis Ababa",
        deliveryFee = 150.0,
        total = 1400.0,
      )
    val confirmedDelivery = confirmedDeliveryDto.toDomain()
    assertEquals(FulfillmentMethod.DELIVERY, confirmedDelivery.fulfillmentMethod)
    assertEquals(150.0, confirmedDelivery.deliveryFee!!, 0.001)
    assertEquals(1250.0, confirmedDelivery.effectiveProductSubtotal, 0.001)

    val pickupDto = liveStylePaidOrderDto.copy(deliveryType = "pickup", deliveryFee = 0.0)
    val confirmedPickup = pickupDto.toDomain()
    assertEquals(FulfillmentMethod.STORE_PICKUP, confirmedPickup.fulfillmentMethod)
    assertEquals("0.00 ብር", EtbFormatter.format(0.0))
  }

  @Test
  fun `effective product subtotal derives accurately from items or total minus fee`() {
    val itemOrder =
      Order(
        id = 9L,
        status = OrderStatus.PAID,
        rawStatus = "paid",
        total = 1400.0,
        subtotal = 0.0,
        customerNote = "",
        createdAt = "",
        updatedAt = "",
        currency = "ETB",
        items =
          listOf(
            OrderItem(
              id = 1L,
              productId = 10L,
              productName = "Wild Honey",
              unitPrice = 600.0,
              quantity = 2,
              currency = "ETB",
              imageUrl = "",
            )
          ),
        payments = emptyList(),
        paymentInstructions = emptyList(),
        messages = emptyList(),
        deliveryFee = 200.0,
      )
    assertEquals(1200.0, itemOrder.effectiveProductSubtotal, 0.001)
  }
}
