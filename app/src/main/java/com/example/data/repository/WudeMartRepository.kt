package com.example.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.example.data.local.SessionAndCartStore
import com.example.data.model.AuthRequest
import com.example.data.model.CartEntryStorage
import com.example.data.model.CartItem
import com.example.data.model.CreateOrderItemRequest
import com.example.data.model.CreateOrderRequest
import com.example.data.model.CustomerSession
import com.example.data.model.DeliveryFeeQuote
import com.example.data.model.DeliveryProfileStorage
import com.example.data.model.DeliveryQuoteDto
import com.example.data.model.DeliveryQuoteRequest
import com.example.data.model.DeliveryTrackingDto
import com.example.data.model.DeliveryTrackingInfo
import com.example.data.model.FulfillmentChoiceRequest
import com.example.data.model.FulfillmentMethod
import com.example.data.model.GeoPoint
import com.example.data.model.MessageTone
import com.example.data.model.Order
import com.example.data.model.OrderDto
import com.example.data.model.OrderItem
import com.example.data.model.OrderStatus
import com.example.data.model.PaymentDto
import com.example.data.model.PaymentInstruction
import com.example.data.model.PaymentInstructionDto
import com.example.data.model.PaymentMethodsEnvelopeDto
import com.example.data.model.PaymentRecord
import com.example.data.model.Product
import com.example.data.model.ProductDto
import com.example.data.model.StoreConfigDto
import com.example.data.model.StorePickupConfig
import com.example.data.model.TransactionMessage
import com.example.data.model.TransactionMessageDto
import com.example.data.model.TransactionMessagesEnvelopeDto
import com.example.data.remote.NetworkModule
import com.example.data.remote.NetworkMonitor
import com.example.data.remote.WudeMartApi
import com.example.ui.i18n.AppLanguage
import com.example.ui.theme.ThemePreference
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

sealed class ApiResult<out T> {
  data class Success<T>(val data: T, val fromCache: Boolean = false) : ApiResult<T>()
  data class Error(val message: String, val isAuthError: Boolean = false) : ApiResult<Nothing>()
}

class WudeMartRepository(
  private val context: Context,
  private val api: WudeMartApi,
  private val store: SessionAndCartStore,
  networkMonitor: NetworkMonitor,
) {
  val isOnline: Flow<Boolean> = networkMonitor.isOnline
  val sessionFlow: Flow<CustomerSession?> = store.sessionFlow
  val deliveryProfileFlow: Flow<DeliveryProfileStorage> = store.deliveryProfileFlow
  val themePreferenceFlow: Flow<ThemePreference> = store.themePreferenceFlow
  val languagePreferenceFlow: Flow<AppLanguage> = store.languagePreferenceFlow

  val cartFlow: Flow<List<CartItem>> =
    store.cartFlow.map { entries ->
      entries.map { entry ->
        CartItem(
          product =
            Product(
              id = entry.productId,
              name = entry.name,
              description = entry.description,
              price = entry.price,
              imageUrl = entry.imageUrl,
              available = true,
              createdAt = "",
              updatedAt = "",
              currency = "ETB",
              categoryLabel = inferCategory(entry.name, entry.description),
            ),
          quantity = entry.quantity,
        )
      }
    }

  fun wishlistFlow(accountKey: String): Flow<Set<Long>> = store.wishlistFlow(accountKey)

  suspend fun toggleWishlist(accountKey: String, productId: Long): Boolean =
    store.toggleWishlist(accountKey, productId)

  suspend fun fetchProducts(): ApiResult<List<Product>> = withContext(Dispatchers.IO) {
    try {
      val dtos = api.getProducts()
      store.saveCachedProducts(dtos)
      ApiResult.Success(dtos.map { it.toDomain() }, fromCache = false)
    } catch (e: Exception) {
      val cached = store.getCachedProducts()
      if (cached.isNotEmpty()) {
        ApiResult.Success(cached.map { it.toDomain() }, fromCache = true)
      } else {
        ApiResult.Error(friendlyError(e, "Could not load Wude Mart catalog."))
      }
    }
  }

  suspend fun fetchProductDetail(productId: Long): ApiResult<Product> = withContext(Dispatchers.IO) {
    try {
      val dto = api.getProductById(productId)
      ApiResult.Success(dto.toDomain())
    } catch (e: Exception) {
      val cached = store.getCachedProducts().firstOrNull { it.id == productId }
      if (cached != null) {
        ApiResult.Success(cached.toDomain(), fromCache = true)
      } else {
        ApiResult.Error(friendlyError(e, "Could not load product details."))
      }
    }
  }

  suspend fun login(email: String, password: String): ApiResult<CustomerSession> =
    withContext(Dispatchers.IO) {
      try {
        val resp = api.login(AuthRequest(email.trim(), password))
        if (resp.isSuccessful && resp.body() != null) {
          val auth = resp.body()!!
          val initialSession =
            CustomerSession(
              token = auth.accessToken,
              email = auth.email,
              role = auth.role ?: "customer",
            )
          store.saveSession(initialSession)
          val enriched = fetchAndSaveProfileInternal(initialSession)
          ApiResult.Success(enriched)
        } else {
          val err = resp.errorBody()?.string()?.takeIf { it.isNotBlank() }
          ApiResult.Error(err ?: "Invalid email or password.")
        }
      } catch (e: Exception) {
        ApiResult.Error(friendlyError(e, "Unable to sign in right now."))
      }
    }

  suspend fun register(email: String, password: String): ApiResult<CustomerSession> =
    withContext(Dispatchers.IO) {
      try {
        val resp = api.register(AuthRequest(email.trim(), password))
        if (resp.isSuccessful && resp.body() != null) {
          val auth = resp.body()!!
          val initialSession =
            CustomerSession(
              token = auth.accessToken,
              email = auth.email,
              role = auth.role ?: "customer",
            )
          store.saveSession(initialSession)
          val enriched = fetchAndSaveProfileInternal(initialSession)
          ApiResult.Success(enriched)
        } else {
          val err = resp.errorBody()?.string()?.takeIf { it.isNotBlank() }
          ApiResult.Error(err ?: "Could not create your account.")
        }
      } catch (e: Exception) {
        ApiResult.Error(friendlyError(e, "Unable to register right now."))
      }
    }

  suspend fun refreshProfile(): ApiResult<CustomerSession> = withContext(Dispatchers.IO) {
    val current = store.sessionFlow.first()
      ?: return@withContext ApiResult.Error("Not signed in.", isAuthError = true)
    try {
      val resp = api.getMe()
      when {
        resp.isSuccessful && resp.body() != null -> {
          val me = resp.body()!!
          val updated =
            current.copy(
              userId = me.id,
              email = me.email,
              role = me.role,
              createdAt = me.createdAt,
            )
          store.saveSession(updated)
          ApiResult.Success(updated)
        }
        resp.code() == 401 -> {
          store.clearSession()
          ApiResult.Error("Your session expired. Please sign in again.", isAuthError = true)
        }
        else -> ApiResult.Error("Failed to refresh account details.")
      }
    } catch (e: Exception) {
      ApiResult.Error(friendlyError(e, "Unable to reach Wude Mart servers."))
    }
  }

  private suspend fun fetchAndSaveProfileInternal(base: CustomerSession): CustomerSession {
    return runCatching {
        val resp = api.getMe()
        if (resp.isSuccessful && resp.body() != null) {
          val me = resp.body()!!
          val enriched =
            base.copy(
              userId = me.id,
              email = me.email,
              role = me.role,
              createdAt = me.createdAt,
            )
          store.saveSession(enriched)
          enriched
        } else {
          base
        }
      }
      .getOrDefault(base)
  }

  suspend fun logout() {
    store.clearSession()
  }

  suspend fun addToCart(product: Product, quantity: Int = 1) {
    val current = store.cartFlow.first().toMutableList()
    val index = current.indexOfFirst { it.productId == product.id }
    if (index >= 0) {
      val existing = current[index]
      current[index] = existing.copy(quantity = (existing.quantity + quantity).coerceAtMost(99))
    } else {
      current.add(
        CartEntryStorage(
          productId = product.id,
          name = product.name,
          description = product.description,
          price = product.price,
          imageUrl = product.imageUrl,
          quantity = quantity.coerceIn(1, 99),
        )
      )
    }
    store.saveCart(current)
  }

  suspend fun updateCartQuantity(productId: Long, quantity: Int) {
    val current = store.cartFlow.first().toMutableList()
    if (quantity <= 0) {
      current.removeAll { it.productId == productId }
    } else {
      val index = current.indexOfFirst { it.productId == productId }
      if (index >= 0) {
        current[index] = current[index].copy(quantity = quantity.coerceAtMost(99))
      }
    }
    store.saveCart(current)
  }

  suspend fun removeFromCart(productId: Long) {
    val current = store.cartFlow.first().filterNot { it.productId == productId }
    store.saveCart(current)
  }

  suspend fun clearCart() {
    store.saveCart(emptyList())
  }

  suspend fun saveDeliveryProfile(profile: DeliveryProfileStorage) {
    store.saveDeliveryProfile(profile)
  }

  suspend fun saveThemePreference(pref: ThemePreference) {
    store.saveThemePreference(pref)
  }

  suspend fun saveLanguagePreference(language: AppLanguage) {
    store.saveLanguagePreference(language)
  }

  suspend fun placeOrder(
    cartItems: List<CartItem>,
    delivery: DeliveryProfileStorage,
    extraNote: String,
  ): ApiResult<Order> = withContext(Dispatchers.IO) {
    if (cartItems.isEmpty()) {
      return@withContext ApiResult.Error("Your cart is empty.")
    }
    try {
      store.saveDeliveryProfile(delivery)
      val formattedNote = buildString {
        append("Recipient: ${delivery.fullName.trim()}")
        append(" | Phone: ${delivery.phone.trim()}")
        val locParts =
          listOf(delivery.subCity.trim(), delivery.city.trim(), delivery.addressLine.trim())
            .filter { it.isNotBlank() }
        if (locParts.isNotEmpty()) {
          append(" | Delivery: ${locParts.joinToString(", ")}")
        }
        if (extraNote.isNotBlank()) {
          append(" | Note: ${extraNote.trim()}")
        }
      }

      val request =
        CreateOrderRequest(
          items =
            cartItems.map {
              CreateOrderItemRequest(productId = it.product.id, quantity = it.quantity)
            },
          customerNote = formattedNote,
        )

      val resp = api.createOrder(request)
      when {
        resp.isSuccessful && resp.body() != null -> {
          val order = resp.body()!!.toDomain()
          store.saveCart(emptyList())
          ApiResult.Success(order)
        }
        resp.code() == 401 -> {
          store.clearSession()
          ApiResult.Error("Please sign in to place an order.", isAuthError = true)
        }
        else -> {
          val msg = resp.errorBody()?.string()?.takeIf { it.isNotBlank() }
          ApiResult.Error(msg ?: "Could not place your order (${resp.code()}).")
        }
      }
    } catch (e: Exception) {
      ApiResult.Error(friendlyError(e, "Network error while placing order."))
    }
  }

  suspend fun fetchOrders(): ApiResult<List<Order>> = withContext(Dispatchers.IO) {
    try {
      val resp = api.getOrders()
      when {
        resp.isSuccessful && resp.body() != null -> {
          val orders = resp.body()!!.map { it.toDomain() }.sortedByDescending { it.id }
          ApiResult.Success(orders)
        }
        resp.code() == 401 -> {
          store.clearSession()
          ApiResult.Error("Session expired. Sign in to view orders.", isAuthError = true)
        }
        else -> ApiResult.Error("Could not load orders (${resp.code()}).")
      }
    } catch (e: Exception) {
      ApiResult.Error(friendlyError(e, "Unable to fetch your order history."))
    }
  }

  suspend fun fetchOrderDetail(orderId: Long): ApiResult<Order> = withContext(Dispatchers.IO) {
    try {
      val resp = api.getOrderById(orderId)
      when {
        resp.isSuccessful && resp.body() != null -> {
          val baseOrder = resp.body()!!.toDomain()
          val extraMessages = fetchOrderMessagesRawInternal(orderId)
          val tracking = baseOrder.deliveryTracking ?: fetchOrderTrackingInternal(orderId)
          val mergedMessages = mergeTransactionMessages(extraMessages, baseOrder.messages)
          ApiResult.Success(
            baseOrder.copy(
              messages = mergedMessages,
              deliveryTracking = tracking,
              expectedDeliveryDate =
                baseOrder.expectedDeliveryDate ?: tracking?.expectedDeliveryDate,
              expectedDeliveryWindow =
                baseOrder.expectedDeliveryWindow ?: tracking?.expectedDeliveryWindow,
            )
          )
        }
        resp.code() == 401 -> {
          store.clearSession()
          ApiResult.Error("Session expired. Sign in again.", isAuthError = true)
        }
        else -> ApiResult.Error("Could not load order #$orderId.")
      }
    } catch (e: Exception) {
      ApiResult.Error(friendlyError(e, "Unable to refresh order #$orderId."))
    }
  }

  /**
   * Fetches active payment methods directly from GET /api/payment-methods.
   */
  suspend fun fetchBackendPaymentInstructions(orderFromApi: Order? = null): ApiResult<List<PaymentInstruction>> =
    withContext(Dispatchers.IO) {
      try {
        val resp = api.getPaymentMethodsRaw()
        if (resp.isSuccessful) {
          val raw = resp.body()?.string()?.trim().orEmpty()
          val parsed = parsePaymentMethodsJson(raw, NetworkModule.moshi)
          if (parsed.isNotEmpty()) {
            return@withContext ApiResult.Success(parsed)
          }
        }
        val embedded = orderFromApi?.paymentInstructions.orEmpty()
        if (embedded.isNotEmpty()) {
          return@withContext ApiResult.Success(embedded)
        }
        if (!resp.isSuccessful) {
          return@withContext ApiResult.Error("Could not load payment methods (${resp.code()}).")
        }
        ApiResult.Success(emptyList())
      } catch (e: Exception) {
        val embedded = orderFromApi?.paymentInstructions.orEmpty()
        if (embedded.isNotEmpty()) {
          ApiResult.Success(embedded)
        } else {
          ApiResult.Error(friendlyError(e, "Could not load payment methods."))
        }
      }
    }

  suspend fun fetchStorePickupConfig(): StorePickupConfig? = withContext(Dispatchers.IO) {
    val adapter = NetworkModule.moshi.adapter(StoreConfigDto::class.java)
    runCatching {
        val resp = api.getStoreConfigRaw()
        if (!resp.isSuccessful) return@runCatching null
        val raw = resp.body()?.string()?.trim().orEmpty()
        if (raw.isBlank()) return@runCatching null
        adapter.fromJson(raw)?.toDomainOrNull()
      }
      .getOrNull()
  }

  suspend fun fetchDeliveryQuote(
    orderId: Long,
    subtotal: Double,
    city: String,
    subCity: String,
    addressLine: String,
    lat: Double?,
    lng: Double?,
  ): ApiResult<DeliveryFeeQuote> = withContext(Dispatchers.IO) {
    val cleanCity = city.trim()
    val cleanSubCity = subCity.trim()
    val cleanAddress = addressLine.trim()
    val combinedAddress =
      listOf(cleanSubCity, cleanCity, cleanAddress)
        .filter { it.isNotBlank() }
        .joinToString(", ")
    val req =
      DeliveryQuoteRequest(
        orderId = orderId,
        city = cleanCity,
        subCity = cleanSubCity.takeIf { it.isNotBlank() },
        address = combinedAddress.ifBlank { cleanAddress },
        deliveryCity = cleanCity,
        deliveryAddress = combinedAddress.ifBlank { cleanAddress },
        subtotal = subtotal,
        latitude = lat,
        longitude = lng,
      )
    try {
      var resp = api.getDeliveryQuote(req)
      if (resp.code() == 404 || resp.code() == 405) {
        resp = api.getDeliveryPricingQuote(req)
      }
      when {
        resp.isSuccessful -> {
          val raw = resp.body()?.string()?.trim().orEmpty()
          val quote = parseDeliveryQuoteJson(raw, NetworkModule.moshi)
          if (quote != null) {
            ApiResult.Success(quote)
          } else {
            ApiResult.Error("Server did not return a valid delivery fee quote.")
          }
        }
        resp.code() == 401 -> {
          store.clearSession()
          ApiResult.Error("Session expired. Please sign in again.", isAuthError = true)
        }
        resp.code() == 404 || resp.code() == 405 || resp.code() == 501 -> {
          ApiResult.Error(
            "Delivery pricing endpoint is not available on the server right now (HTTP ${resp.code()})."
          )
        }
        else -> {
          val errRaw = resp.errorBody()?.string()?.trim().orEmpty()
          val errMsg =
            runCatching {
                NetworkModule.moshi.adapter(DeliveryQuoteDto::class.java).fromJson(errRaw)?.let {
                  it.error?.takeIf { s -> s.isNotBlank() }
                    ?: it.message?.takeIf { s -> s.isNotBlank() }
                }
              }
              .getOrNull()
          ApiResult.Error(
            errMsg
              ?: errRaw.takeIf { it.isNotBlank() && !it.startsWith("<") }
              ?: "Unable to calculate delivery fee (${resp.code()})."
          )
        }
      }
    } catch (e: Exception) {
      ApiResult.Error(friendlyError(e, "Failed to retrieve delivery fee quote."))
    }
  }

  suspend fun submitFulfillmentChoice(
    orderId: Long,
    method: FulfillmentMethod,
    deliveryProfile: DeliveryProfileStorage?,
    note: String,
    quotedDeliveryFee: Double,
    deliveryZoneId: Long? = null,
  ): ApiResult<Order> = withContext(Dispatchers.IO) {
    try {
      val formattedAddress =
        if (method == FulfillmentMethod.DELIVERY && deliveryProfile != null) {
          listOf(
              deliveryProfile.subCity.trim(),
              deliveryProfile.city.trim(),
              deliveryProfile.addressLine.trim(),
            )
            .filter { it.isNotBlank() }
            .joinToString(", ")
        } else {
          null
        }
      val phoneValue = deliveryProfile?.phone?.trim()?.takeIf { it.isNotBlank() }
      val req =
        FulfillmentChoiceRequest(
          fulfillmentMethod = method.apiValue,
          deliveryType = method.apiValue,
          deliveryCity =
            if (method == FulfillmentMethod.DELIVERY) {
              deliveryProfile?.city?.trim()?.takeIf { it.isNotBlank() }
            } else null,
          deliverySubCity =
            if (method == FulfillmentMethod.DELIVERY) {
              deliveryProfile?.subCity?.trim()?.takeIf { it.isNotBlank() }
            } else null,
          deliveryAddress = formattedAddress?.takeIf { it.isNotBlank() },
          deliveryZoneId = if (method == FulfillmentMethod.DELIVERY) deliveryZoneId else null,
          deliveryFee = if (method == FulfillmentMethod.STORE_PICKUP) 0.0 else quotedDeliveryFee,
          customerName = deliveryProfile?.fullName?.trim()?.takeIf { it.isNotBlank() },
          customerPhone = phoneValue,
          phone = phoneValue,
          note = note.trim().takeIf { it.isNotBlank() },
          latitude = if (method == FulfillmentMethod.DELIVERY) deliveryProfile?.latitude else null,
          longitude = if (method == FulfillmentMethod.DELIVERY) deliveryProfile?.longitude else null,
        )
      val resp = api.submitFulfillmentChoice(orderId, req)
      when {
        resp.isSuccessful && resp.body() != null -> ApiResult.Success(resp.body()!!.toDomain())
        resp.code() == 401 -> {
          store.clearSession()
          ApiResult.Error("Session expired. Please sign in again.", isAuthError = true)
        }
        resp.code() == 404 || resp.code() == 405 || resp.code() == 501 -> {
          ApiResult.Error(
            "Fulfillment confirmation endpoint (/api/orders/$orderId/fulfillment) is not enabled on the server yet."
          )
        }
        else -> {
          val msg = resp.errorBody()?.string()?.takeIf { it.isNotBlank() }
          ApiResult.Error(msg ?: "Could not save your fulfillment choice (${resp.code()}).")
        }
      }
    } catch (e: Exception) {
      ApiResult.Error(friendlyError(e, "Failed to submit fulfillment choice."))
    }
  }

  suspend fun fetchOrderTracking(orderId: Long): ApiResult<DeliveryTrackingInfo?> =
    withContext(Dispatchers.IO) {
      try {
        val resp = api.getOrderTrackingRaw(orderId)
        when {
          resp.isSuccessful -> {
            val raw = resp.body()?.string()?.trim().orEmpty()
            val adapter = NetworkModule.moshi.adapter(DeliveryTrackingDto::class.java)
            val parsed = if (raw.isBlank()) null else adapter.fromJson(raw)?.toDomain()
            ApiResult.Success(parsed)
          }
          resp.code() == 404 -> ApiResult.Success(null)
          resp.code() == 401 -> {
            store.clearSession()
            ApiResult.Error("Session expired. Sign in again.", isAuthError = true)
          }
          else -> ApiResult.Error("Tracking unavailable (${resp.code()}).")
        }
      } catch (e: Exception) {
        ApiResult.Error(friendlyError(e, "Unable to fetch delivery tracking."))
      }
    }

  private suspend fun fetchOrderTrackingInternal(orderId: Long): DeliveryTrackingInfo? {
    return (fetchOrderTracking(orderId) as? ApiResult.Success)?.data
  }

  private suspend fun fetchOrderMessagesRawInternal(orderId: Long): List<TransactionMessage> {
    val listType =
      Types.newParameterizedType(List::class.java, TransactionMessageDto::class.java)
    val listAdapter = NetworkModule.moshi.adapter<List<TransactionMessageDto>>(listType)
    val envelopeAdapter = NetworkModule.moshi.adapter(TransactionMessagesEnvelopeDto::class.java)

    return runCatching {
        val resp = api.getOrderMessagesRaw(orderId)
        if (!resp.isSuccessful) return@runCatching emptyList()
        val raw = resp.body()?.string()?.trim().orEmpty()
        when {
          raw.startsWith("[") -> {
            listAdapter.fromJson(raw)
              ?.mapIndexedNotNull { idx, dto -> dto.toDomainOrNull(idx) }
              .orEmpty()
          }
          raw.startsWith("{") -> {
            val env = envelopeAdapter.fromJson(raw)
            (env?.messages ?: env?.data).orEmpty()
              .mapIndexedNotNull { idx, dto -> dto.toDomainOrNull(idx) }
          }
          else -> emptyList()
        }
      }
      .getOrDefault(emptyList())
  }

  suspend fun uploadPaymentScreenshot(
    orderId: Long,
    imageUri: Uri,
    selectedPaymentMethod: String?,
  ): ApiResult<PaymentRecord> = withContext(Dispatchers.IO) {
    try {
      val imageBytes =
        readAndOptimizeImageBytes(imageUri)
          ?: return@withContext ApiResult.Error("Could not read the selected image file.")

      val fileName = "wude_proof_${orderId}_${System.currentTimeMillis()}.jpg"
      val fileBody = imageBytes.toRequestBody("image/jpeg".toMediaTypeOrNull())
      val proofPart = MultipartBody.Part.createFormData("proof", fileName, fileBody)

      val methodPart =
        selectedPaymentMethod
          ?.trim()
          ?.takeIf { it.isNotBlank() }
          ?.toRequestBody("text/plain".toMediaTypeOrNull())

      val resp =
        api.uploadPaymentProof(
          orderId = orderId,
          proof = proofPart,
          paymentMethod = methodPart,
        )

      when {
        resp.isSuccessful && resp.body() != null -> ApiResult.Success(resp.body()!!.toDomain())
        resp.code() == 401 -> {
          store.clearSession()
          ApiResult.Error("Session expired. Sign in to upload payment proof.", isAuthError = true)
        }
        else -> {
          val msg = resp.errorBody()?.string()?.takeIf { it.isNotBlank() }
          ApiResult.Error(msg ?: "Upload failed (${resp.code()}). Please try again.")
        }
      }
    } catch (e: Exception) {
      ApiResult.Error(friendlyError(e, "Failed to upload payment screenshot."))
    }
  }

  private fun readAndOptimizeImageBytes(uri: Uri): ByteArray? {
    val rawBytes =
      context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
    if (rawBytes.size <= 1_400_000) return rawBytes

    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, bounds)
    var sampleSize = 1
    while (bounds.outWidth / sampleSize > 1600 || bounds.outHeight / sampleSize > 1600) {
      sampleSize *= 2
    }
    val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
    val bitmap = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, decodeOpts)
      ?: return rawBytes
    val out = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
    bitmap.recycle()
    return out.toByteArray()
  }

  private fun friendlyError(e: Exception, fallback: String): String {
    return when (e) {
      is IOException -> "No internet connection. Please check your mobile data or Wi-Fi."
      else -> e.localizedMessage?.takeIf { it.isNotBlank() } ?: fallback
    }
  }

  companion object {
    fun parsePaymentMethodsJson(raw: String, moshi: Moshi): List<PaymentInstruction> {
      val trimmed = raw.trim()
      if (trimmed.isBlank()) return emptyList()
      val listType =
        Types.newParameterizedType(List::class.java, PaymentInstructionDto::class.java)
      val listAdapter = moshi.adapter<List<PaymentInstructionDto>>(listType)
      val envelopeAdapter = moshi.adapter(PaymentMethodsEnvelopeDto::class.java)

      return when {
        trimmed.startsWith("[") -> {
          listAdapter.fromJson(trimmed)
            ?.filter { it.isCurrentlyActive() }
            ?.mapIndexedNotNull { idx, dto -> dto.toDomainOrNull(idx) }
            .orEmpty()
        }
        trimmed.startsWith("{") -> {
          val env = envelopeAdapter.fromJson(trimmed)
          val sharedInstructions =
            listOfNotNull(env?.adminInstructions, env?.instructions)
              .firstOrNull { it.isNotBlank() }
              .orEmpty()
          val list =
            env?.paymentMethods
              ?: env?.methods
              ?: env?.data
              ?: emptyList()
          list
            .filter { it.isCurrentlyActive() }
            .mapIndexedNotNull { idx, dto ->
              dto.toDomainOrNull(idx, fallbackInstructions = sharedInstructions)
            }
        }
        else -> emptyList()
      }
    }

    fun parseDeliveryQuoteJson(raw: String, moshi: Moshi): DeliveryFeeQuote? {
      val trimmed = raw.trim()
      if (trimmed.isBlank() || !trimmed.startsWith("{")) return null
      val adapter = moshi.adapter(DeliveryQuoteDto::class.java)
      val dto = runCatching { adapter.fromJson(trimmed) }.getOrNull() ?: return null
      val fee = dto.deliveryFee ?: dto.fee ?: dto.price ?: return null
      if (fee.isNaN() || fee.isInfinite() || fee < 0.0) return null
      return DeliveryFeeQuote(
        deliveryFee = fee,
        subtotal = dto.subtotal?.takeIf { !it.isNaN() && !it.isInfinite() && it >= 0.0 },
        total = dto.total?.takeIf { !it.isNaN() && !it.isInfinite() && it >= 0.0 },
        zoneId = dto.deliveryZoneId ?: dto.zoneId,
        zoneName = (dto.deliveryZoneName ?: dto.zoneName).orEmpty().trim(),
        estimatedDelivery = dto.estimatedDelivery?.takeIf { it.isNotBlank() },
      )
    }
  }
}

private fun Any?.asFlexibleBoolean(defaultValue: Boolean): Boolean {
  return when (this) {
    null -> defaultValue
    is Boolean -> this
    is Number -> this.toInt() != 0
    is String -> {
      val norm = this.trim().lowercase(Locale.ROOT)
      norm == "true" || norm == "1" || norm == "yes" || norm == "active"
    }
    else -> defaultValue
  }
}

private fun ProductDto.toDomain(): Product =
  Product(
    id = id,
    name = name,
    description = description.orEmpty().ifBlank { "Quality product curated on Wude Mart." },
    price = price,
    imageUrl = imageUrl.orEmpty().trim(),
    available = available != 0,
    createdAt = createdAt.orEmpty(),
    updatedAt = updatedAt.orEmpty(),
    currency = currency ?: "ETB",
    categoryLabel = category?.takeIf { it.isNotBlank() } ?: inferCategory(name, description.orEmpty()),
  )

private fun inferCategory(name: String, description: String): String {
  val text = "$name $description".lowercase(Locale.ROOT)
  return when {
    listOf("honey", "coffee", "tea", "spice", "food", "organic", "teff", "oil", "grain")
      .any { text.contains(it) } -> "Groceries & Pantry"
    listOf("dress", "shirt", "shoe", "bag", "scarf", "fashion", "jacket", "cloth")
      .any { text.contains(it) } -> "Fashion & Style"
    listOf("phone", "laptop", "earbud", "charger", "tech", "watch", "electronic")
      .any { text.contains(it) } -> "Electronics"
    listOf("home", "kitchen", "pot", "basket", "decor", "bed", "lamp")
      .any { text.contains(it) } -> "Home & Living"
    listOf("cream", "soap", "perfume", "beauty", "skin", "hair")
      .any { text.contains(it) } -> "Beauty & Care"
    else -> "Marketplace"
  }
}

private fun PaymentInstructionDto.isCurrentlyActive(): Boolean {
  return when {
    isActive != null -> isActive.asFlexibleBoolean(true)
    active != null -> active.asFlexibleBoolean(true)
    else -> true
  }
}

private fun PaymentInstructionDto.toDomainOrNull(
  index: Int,
  fallbackInstructions: String = "",
): PaymentInstruction? {
  val providerText =
    listOfNotNull(providerName, provider, name, title, type, code)
      .firstOrNull { it.isNotBlank() }
      ?: return null
  val holderText =
    listOfNotNull(accountName, accountHolder, holderName).firstOrNull { it.isNotBlank() }.orEmpty()
  val numberText =
    listOfNotNull(accountNumber, merchantNumber, phone).firstOrNull { it.isNotBlank() }.orEmpty()
  val instrText =
    listOfNotNull(adminInstructions, instructions, description, note, fallbackInstructions)
      .firstOrNull { it.isNotBlank() }
      .orEmpty()
  val lower = "${providerText.lowercase()} ${type.orEmpty().lowercase()} ${code.orEmpty().lowercase()}"
  return PaymentInstruction(
    id = id?.toString() ?: "${providerText.lowercase().replace(" ", "_")}_$index",
    providerName = providerText,
    accountName = holderText,
    accountNumber = numberText,
    instructions = instrText,
    isTelebirr = lower.contains("telebirr"),
  )
}

private fun TransactionMessageDto.toDomainOrNull(index: Int): TransactionMessage? {
  val text = listOfNotNull(message, body, content).firstOrNull { it.isNotBlank() } ?: return null
  val senderText =
    when {
      !sender.isNullOrBlank() -> sender
      senderRole?.lowercase(Locale.ROOT) == "admin" -> "Wude Mart Admin"
      !senderRole.isNullOrBlank() -> senderRole.replaceFirstChar { it.uppercase() }
      else -> "Wude Mart Admin"
    }
  val combinedType = "${status.orEmpty().lowercase()} ${messageType.orEmpty().lowercase()}"
  val tone =
    when {
      combinedType.contains("approv") || combinedType.contains("paid") || combinedType.contains("verif") ->
        MessageTone.APPROVED
      combinedType.contains("reject") || combinedType.contains("fail") || combinedType.contains("cancel") ->
        MessageTone.REJECTED
      combinedType.contains("review") || combinedType.contains("pending") || combinedType.contains("submit") ->
        MessageTone.PENDING
      else -> MessageTone.INFO
    }
  val resolvedTitle =
    title?.takeIf { it.isNotBlank() }
      ?: when {
        combinedType.contains("reject") -> "Payment Rejected"
        combinedType.contains("approv") || combinedType.contains("verif") -> "Payment Approved"
        combinedType.contains("delivery_date") || combinedType.contains("schedule") ->
          "Delivery Schedule Updated"
        combinedType.contains("dispatch") || combinedType.contains("ship") -> "Order Dispatched"
        else -> "Order Update"
      }
  return TransactionMessage(
    id = id?.toString() ?: "api_msg_$index",
    senderLabel = senderText,
    title = resolvedTitle,
    body = text,
    timestamp = createdAt.orEmpty(),
    tone = tone,
  )
}

private fun StoreConfigDto.toDomainOrNull(): StorePickupConfig? {
  val addr = storeAddress?.trim().orEmpty()
  val instr = pickupInstructions?.trim().orEmpty()
  val hours = openingHours?.trim().orEmpty()
  val name = storeName?.trim().orEmpty()
  if (addr.isBlank() && instr.isBlank() && hours.isBlank() && name.isBlank()) return null
  return StorePickupConfig(
    storeName = name.ifBlank { "Wude Mart Store" },
    storeAddress = addr,
    pickupInstructions = instr,
    openingHours = hours,
    deliveryFee = deliveryFee,
    pickupEnabled = pickupEnabled.asFlexibleBoolean(true),
  )
}

private fun DeliveryTrackingDto.toDomain(): DeliveryTrackingInfo {
  val lat = currentLat ?: latitude
  val lng = currentLng ?: longitude
  val curPoint = if (lat != null && lng != null) GeoPoint(lat, lng) else null
  val destPoint =
    if (destinationLat != null && destinationLng != null) GeoPoint(destinationLat, destinationLng)
    else null
  val routePoints =
    route.orEmpty().mapNotNull { pt ->
      val pLat = pt.lat ?: pt.latitude
      val pLng = pt.lng ?: pt.lon ?: pt.longitude
      if (pLat != null && pLng != null) GeoPoint(pLat, pLng) else null
    }
  return DeliveryTrackingInfo(
    status = status.orEmpty(),
    sharingEnabled = sharingEnabled.asFlexibleBoolean(curPoint != null),
    driverName = driverName ?: courierName.orEmpty(),
    driverPhone = driverPhone ?: courierPhone.orEmpty(),
    currentLocation = curPoint,
    destinationLocation = destPoint,
    destinationAddress = destinationAddress.orEmpty(),
    lastUpdatedAt = lastUpdatedAt ?: updatedAt.orEmpty(),
    etaText = etaText ?: estimatedArrival.orEmpty(),
    expectedDeliveryDate = expectedDeliveryDate?.takeIf { it.isNotBlank() },
    expectedDeliveryWindow = expectedDeliveryWindow?.takeIf { it.isNotBlank() },
    route = routePoints,
  )
}

private fun PaymentDto.toDomain(): PaymentRecord =
  PaymentRecord(
    id = id,
    orderId = orderId,
    proofFilename = proofFilename.orEmpty(),
    status = status,
    adminNote = adminNote.orEmpty(),
    createdAt = createdAt.orEmpty(),
    reviewedAt = reviewedAt,
    currency = currency ?: "ETB",
    paymentMethod = paymentMethod.orEmpty(),
  )

internal fun OrderDto.toDomain(): Order {
  val domainStatus = OrderStatus.fromApi(status)
  val domainPayments = payments.map { it.toDomain() }
  val combinedInstructions =
    ((paymentMethods.orEmpty()) + (paymentInstructions.orEmpty()))
      .filter { it.isCurrentlyActive() }
      .mapIndexedNotNull { idx, dto -> dto.toDomainOrNull(idx) }

  val apiMessages =
    ((messages.orEmpty()) + (transactionMessages.orEmpty()))
      .mapIndexedNotNull { idx, dto -> dto.toDomainOrNull(idx) }

  val synthesizedMessages = buildList {
    if (!adminNote.isNullOrBlank()) {
      add(
        TransactionMessage(
          id = "order_admin_$id",
          senderLabel = "Wude Mart Admin",
          title = "Administrator Update",
          body = adminNote,
          timestamp = updatedAt.orEmpty(),
          tone =
            when (domainStatus) {
              OrderStatus.PAID, OrderStatus.CONFIRMED, OrderStatus.DELIVERED -> MessageTone.APPROVED
              OrderStatus.REJECTED, OrderStatus.CANCELLED -> MessageTone.REJECTED
              else -> MessageTone.INFO
            },
        )
      )
    }

    domainPayments.forEach { p ->
      val pLower = p.status.lowercase()
      val tone =
        when {
          pLower.contains("approv") || pLower.contains("verif") || pLower.contains("paid") ->
            MessageTone.APPROVED
          pLower.contains("reject") || pLower.contains("fail") -> MessageTone.REJECTED
          else -> MessageTone.PENDING
        }
      val title =
        when (tone) {
          MessageTone.APPROVED -> "Payment #${p.id} Approved"
          MessageTone.REJECTED -> "Payment #${p.id} Rejected"
          else -> "Payment #${p.id} Submitted for Verification"
        }
      val body =
        when {
          p.adminNote.isNotBlank() -> p.adminNote
          tone == MessageTone.APPROVED ->
            "Your payment proof has been verified and approved by an administrator."
          tone == MessageTone.REJECTED ->
            "Your payment proof could not be verified. Please upload a clear payment screenshot."
          else ->
            "Receipt received (${p.proofFilename.ifBlank { "screenshot" }}). Awaiting administrator approval."
        }
      add(
        TransactionMessage(
          id = "payment_${p.id}_${p.status}",
          senderLabel = if (p.adminNote.isNotBlank() || tone != MessageTone.PENDING) "Wude Mart Admin" else "Payment System",
          title = title,
          body = body,
          timestamp = p.reviewedAt ?: p.createdAt,
          tone = tone,
        )
      )
    }
  }

  val explicitMethod = FulfillmentMethod.fromApi(fulfillmentMethod)
  val confirmedFlag = fulfillmentConfirmed.asFlexibleBoolean(false)
  val deliveryTypeMethod = FulfillmentMethod.fromApi(deliveryType)
  val resolvedMethod =
    when {
      explicitMethod != null -> explicitMethod
      confirmedFlag -> deliveryTypeMethod
      deliveryTypeMethod == FulfillmentMethod.STORE_PICKUP -> FulfillmentMethod.STORE_PICKUP
      deliveryTypeMethod == FulfillmentMethod.DELIVERY &&
        (deliveryZoneId != null ||
          !deliveryAddress.isNullOrBlank() ||
          !deliveryCity.isNullOrBlank()) -> FulfillmentMethod.DELIVERY
      else -> null
    }

  val computedItemsSubtotal =
    if (items.isNotEmpty()) items.sumOf { it.unitPrice * it.quantity } else total
  val resolvedSubtotal = subtotal?.takeIf { it > 0.0 } ?: computedItemsSubtotal

  return Order(
    id = id,
    status = domainStatus,
    rawStatus = status,
    total = total,
    subtotal = resolvedSubtotal,
    customerNote = customerNote.orEmpty(),
    createdAt = createdAt.orEmpty(),
    updatedAt = updatedAt.orEmpty(),
    currency = currency ?: "ETB",
    items =
      items.map {
        OrderItem(
          id = it.id,
          productId = it.productId,
          productName = it.productName,
          unitPrice = it.unitPrice,
          quantity = it.quantity,
          currency = it.currency ?: "ETB",
          imageUrl = it.imageUrl.orEmpty(),
        )
      },
    payments = domainPayments,
    paymentInstructions = combinedInstructions,
    messages = mergeTransactionMessages(apiMessages, synthesizedMessages),
    fulfillmentMethod = resolvedMethod,
    fulfillmentConfirmed = fulfillmentConfirmed.asFlexibleBoolean(resolvedMethod != null),
    deliveryFee = deliveryFee,
    deliveryZoneId = deliveryZoneId,
    deliveryZoneName = deliveryZoneName.orEmpty(),
    deliveryAddress = deliveryAddress.orEmpty(),
    deliveryCity = deliveryCity.orEmpty(),
    pickupInfo = pickupConfig?.toDomainOrNull(),
    expectedDeliveryDate = expectedDeliveryDate?.takeIf { it.isNotBlank() },
    expectedDeliveryWindow =
      (expectedDeliveryWindow ?: deliveryTimeWindow)?.takeIf { it.isNotBlank() },
    expectedPickupDate = expectedPickupDate?.takeIf { it.isNotBlank() },
    expectedPickupWindow = expectedPickupWindow?.takeIf { it.isNotBlank() },
    deliveryTracking = deliveryTracking?.toDomain(),
  )
}

private fun mergeTransactionMessages(
  primary: List<TransactionMessage>,
  secondary: List<TransactionMessage>,
): List<TransactionMessage> {
  val seen = HashSet<String>()
  return (primary + secondary).filter { msg ->
    val key = "${msg.tone}|${msg.body.trim().lowercase(Locale.ROOT)}"
    seen.add(key)
  }
}
