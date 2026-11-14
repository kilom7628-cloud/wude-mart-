package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import coil.ImageLoader
import com.example.data.local.SessionAndCartStore
import com.example.data.location.LocationHelper
import com.example.data.model.CartItem
import com.example.data.model.CustomerSession
import com.example.data.model.DeliveryProfileStorage
import com.example.data.model.DeliveryTrackingInfo
import com.example.data.model.EthiopianPhoneValidator
import com.example.data.model.FulfillmentMethod
import com.example.data.model.Order
import com.example.data.model.OrderStatus
import com.example.data.model.PaymentInstruction
import com.example.data.model.Product
import com.example.data.model.ProductSortOption
import com.example.data.model.StorePickupConfig
import com.example.data.remote.NetworkModule
import com.example.data.remote.NetworkMonitor
import com.example.data.repository.ApiResult
import com.example.data.repository.WudeMartRepository
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.stringsFor
import com.example.ui.theme.ThemePreference
import java.util.Locale
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

sealed interface CatalogUiState {
  data object Loading : CatalogUiState
  data class Success(val products: List<Product>, val fromCache: Boolean = false) : CatalogUiState
  data class Error(val message: String) : CatalogUiState
}

sealed interface OrdersUiState {
  data object SignedOut : OrdersUiState
  data object Loading : OrdersUiState
  data class Success(val orders: List<Order>) : OrdersUiState
  data class Error(val message: String) : OrdersUiState
}

data class ProductDetailUiState(
  val isLoading: Boolean = true,
  val product: Product? = null,
  val error: String? = null,
  val selectedQuantity: Int = 1,
)

data class OrderDetailUiState(
  val isLoading: Boolean = true,
  val isRefreshing: Boolean = false,
  val order: Order? = null,
  val paymentInstructions: List<PaymentInstruction> = emptyList(),
  val paymentMethodsError: String? = null,
  val selectedPaymentMethodId: String? = null,
  val instructionsLoaded: Boolean = false,
  val isUploadingProof: Boolean = false,
  val awaitingApprovalState: Boolean = false,
  val uploadSuccessMessage: String? = null,
  val lastSubmittedPaymentId: Long? = null,
  val storePickupConfig: StorePickupConfig? = null,
  val isLoadingDeliveryQuote: Boolean = false,
  val quotedDeliveryFee: Double? = null,
  val quotedSubtotal: Double? = null,
  val quotedTotal: Double? = null,
  val quotedZoneId: Long? = null,
  val quotedZoneName: String = "",
  val quotedEstimatedDelivery: String? = null,
  val deliveryQuoteError: String? = null,
  val lastQuotedAddressKey: String? = null,
  val isSubmittingFulfillment: Boolean = false,
  val fulfillmentError: String? = null,
  val scheduleUpdatedAlert: Boolean = false,
  val error: String? = null,
)

data class DeliveryTrackingUiState(
  val isLoading: Boolean = true,
  val isRefreshing: Boolean = false,
  val order: Order? = null,
  val tracking: DeliveryTrackingInfo? = null,
  val error: String? = null,
)

data class AuthFormState(
  val isRegisterMode: Boolean = false,
  val isSubmitting: Boolean = false,
  val errorMessage: String? = null,
)

data class CheckoutUiState(
  val isPlacingOrder: Boolean = false,
  val isDetectingLocation: Boolean = false,
  val locationStatusMessage: String? = null,
  val errorMessage: String? = null,
)

sealed interface UiEvent {
  data class Snackbar(val message: String) : UiEvent
  data class OrderPlaced(val orderId: Long) : UiEvent
  data object AuthSuccess : UiEvent
}

@OptIn(ExperimentalCoroutinesApi::class)
class WudeMartViewModel(application: Application) : AndroidViewModel(application) {
  private val store = SessionAndCartStore(application, NetworkModule.moshi)
  private val api = NetworkModule.createApi { store.getAccessToken() }
  private val networkMonitor = NetworkMonitor(application)
  private val repository = WudeMartRepository(application, api, store, networkMonitor)
  private val locationHelper = LocationHelper(application)

  val imageLoader: ImageLoader = NetworkModule.createImageLoader(application)

  private val _events = MutableSharedFlow<UiEvent>()
  val events: SharedFlow<UiEvent> = _events.asSharedFlow()

  val isOnline: StateFlow<Boolean> =
    repository.isOnline.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

  val session: StateFlow<CustomerSession?> =
    repository.sessionFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

  val cartItems: StateFlow<List<CartItem>> =
    repository.cartFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val deliveryProfile: StateFlow<DeliveryProfileStorage> =
    repository.deliveryProfileFlow.stateIn(
      viewModelScope,
      SharingStarted.WhileSubscribed(5000),
      DeliveryProfileStorage(),
    )

  val themePreference: StateFlow<ThemePreference> =
    repository.themePreferenceFlow.stateIn(
      viewModelScope,
      SharingStarted.WhileSubscribed(5000),
      ThemePreference.SYSTEM,
    )

  val language: StateFlow<AppLanguage> =
    repository.languagePreferenceFlow.stateIn(
      viewModelScope,
      SharingStarted.WhileSubscribed(5000),
      AppLanguage.ENGLISH,
    )

  val wishlistProductIds: StateFlow<Set<Long>> =
    session
      .flatMapLatest { current ->
        val key = current?.email?.lowercase(Locale.ROOT)?.takeIf { it.isNotBlank() } ?: "guest"
        repository.wishlistFlow(key)
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

  private val _catalogState = MutableStateFlow<CatalogUiState>(CatalogUiState.Loading)
  val catalogState: StateFlow<CatalogUiState> = _catalogState.asStateFlow()

  val wishlistProducts: StateFlow<List<Product>> =
    combine(_catalogState, wishlistProductIds) { state, ids ->
        val all = (state as? CatalogUiState.Success)?.products.orEmpty()
        all.filter { ids.contains(it.id) }
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  private val _searchQuery = MutableStateFlow("")
  val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

  private val _selectedCategory = MutableStateFlow("All")
  val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

  private val _sortOption = MutableStateFlow(ProductSortOption.FEATURED)
  val sortOption: StateFlow<ProductSortOption> = _sortOption.asStateFlow()

  private val _onlyAvailable = MutableStateFlow(true)
  val onlyAvailable: StateFlow<Boolean> = _onlyAvailable.asStateFlow()

  val availableCategories: StateFlow<List<String>> =
    catalogState
      .combine(_onlyAvailable) { state, _ ->
        when (state) {
          is CatalogUiState.Success -> {
            val cats = state.products.map { it.categoryLabel }.distinct().sorted()
            listOf("All") + cats
          }
          else -> listOf("All")
        }
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf("All"))

  val filteredProducts: StateFlow<List<Product>> =
    combine(
        _catalogState,
        _searchQuery,
        _selectedCategory,
        _sortOption,
        _onlyAvailable,
      ) { state, query, category, sort, availableOnly ->
        val items = (state as? CatalogUiState.Success)?.products.orEmpty()
        val q = query.trim().lowercase(Locale.ROOT)
        val filtered =
          items.filter { product ->
            val matchesAvail = !availableOnly || product.available
            val matchesCat = category == "All" || product.categoryLabel == category
            val matchesSearch =
              q.isBlank() ||
                product.name.lowercase(Locale.ROOT).contains(q) ||
                product.description.lowercase(Locale.ROOT).contains(q) ||
                product.categoryLabel.lowercase(Locale.ROOT).contains(q)
            matchesAvail && matchesCat && matchesSearch
          }
        when (sort) {
          ProductSortOption.FEATURED -> filtered
          ProductSortOption.PRICE_LOW_HIGH -> filtered.sortedBy { it.price }
          ProductSortOption.PRICE_HIGH_LOW -> filtered.sortedByDescending { it.price }
          ProductSortOption.NEWEST -> filtered.sortedByDescending { it.createdAt }
        }
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  private val _productDetailState = MutableStateFlow(ProductDetailUiState())
  val productDetailState: StateFlow<ProductDetailUiState> = _productDetailState.asStateFlow()

  private val _ordersState = MutableStateFlow<OrdersUiState>(OrdersUiState.Loading)
  val ordersState: StateFlow<OrdersUiState> = _ordersState.asStateFlow()

  private val _orderDetailState = MutableStateFlow(OrderDetailUiState())
  val orderDetailState: StateFlow<OrderDetailUiState> = _orderDetailState.asStateFlow()

  private val _trackingState = MutableStateFlow(DeliveryTrackingUiState())
  val trackingState: StateFlow<DeliveryTrackingUiState> = _trackingState.asStateFlow()

  private val _authFormState = MutableStateFlow(AuthFormState())
  val authFormState: StateFlow<AuthFormState> = _authFormState.asStateFlow()

  private val _checkoutState = MutableStateFlow(CheckoutUiState())
  val checkoutState: StateFlow<CheckoutUiState> = _checkoutState.asStateFlow()

  private var orderPollingJob: Job? = null
  private var trackingPollingJob: Job? = null
  private var deliveryQuoteJob: Job? = null

  init {
    refreshCatalog()
    viewModelScope.launch {
      session.collect { current ->
        if (current?.isSignedIn == true) {
          refreshOrders()
        } else {
          _ordersState.value = OrdersUiState.SignedOut
        }
      }
    }
  }

  private fun currentStrings() = stringsFor(language.value)

  fun refreshCatalog() {
    viewModelScope.launch {
      if (_catalogState.value !is CatalogUiState.Success) {
        _catalogState.value = CatalogUiState.Loading
      }
      when (val res = repository.fetchProducts()) {
        is ApiResult.Success -> {
          _catalogState.value = CatalogUiState.Success(res.data, fromCache = res.fromCache)
        }
        is ApiResult.Error -> {
          _catalogState.value = CatalogUiState.Error(res.message)
        }
      }
    }
  }

  fun updateSearchQuery(query: String) {
    _searchQuery.value = query
  }

  fun selectCategory(category: String) {
    _selectedCategory.value = category
  }

  fun selectSortOption(option: ProductSortOption) {
    _sortOption.value = option
  }

  fun setOnlyAvailable(only: Boolean) {
    _onlyAvailable.value = only
  }

  fun toggleWishlist(product: Product) {
    val accountKey =
      session.value?.email?.lowercase(Locale.ROOT)?.takeIf { it.isNotBlank() } ?: "guest"
    viewModelScope.launch {
      val added = repository.toggleWishlist(accountKey, product.id)
      val s = currentStrings()
      _events.emit(
        UiEvent.Snackbar(
          if (added) "${s.savedToWishlist}: ${product.name}"
          else "${s.removedFromWishlist}: ${product.name}"
        )
      )
    }
  }

  fun loadProductDetail(productId: Long) {
    viewModelScope.launch {
      val existing =
        (_catalogState.value as? CatalogUiState.Success)?.products?.firstOrNull { it.id == productId }
      _productDetailState.value =
        ProductDetailUiState(
          isLoading = existing == null,
          product = existing,
          error = null,
          selectedQuantity = 1,
        )
      when (val res = repository.fetchProductDetail(productId)) {
        is ApiResult.Success -> {
          _productDetailState.update {
            it.copy(isLoading = false, product = res.data, error = null)
          }
        }
        is ApiResult.Error -> {
          if (existing == null) {
            _productDetailState.update {
              it.copy(isLoading = false, error = res.message)
            }
          }
        }
      }
    }
  }

  fun setDetailQuantity(quantity: Int) {
    _productDetailState.update { it.copy(selectedQuantity = quantity.coerceIn(1, 99)) }
  }

  fun addToCart(product: Product, quantity: Int = 1) {
    viewModelScope.launch {
      repository.addToCart(product, quantity)
      _events.emit(UiEvent.Snackbar("${currentStrings().addedToCart}: ${product.name}"))
    }
  }

  fun updateCartQuantity(productId: Long, quantity: Int) {
    viewModelScope.launch { repository.updateCartQuantity(productId, quantity) }
  }

  fun removeFromCart(productId: Long) {
    viewModelScope.launch { repository.removeFromCart(productId) }
  }

  fun clearCart() {
    viewModelScope.launch {
      repository.clearCart()
      _events.emit(UiEvent.Snackbar("Cart cleared"))
    }
  }

  fun setAuthMode(register: Boolean) {
    _authFormState.update { it.copy(isRegisterMode = register, errorMessage = null) }
  }

  fun clearAuthError() {
    _authFormState.update { it.copy(errorMessage = null) }
  }

  fun submitAuth(email: String, password: String) {
    val cleanEmail = email.trim()
    if (cleanEmail.isBlank() || !cleanEmail.contains("@")) {
      _authFormState.update { it.copy(errorMessage = "Please enter a valid email address.") }
      return
    }
    if (password.length < 8) {
      _authFormState.update {
        it.copy(errorMessage = "Password must be at least 8 characters.")
      }
      return
    }
    viewModelScope.launch {
      _authFormState.update { it.copy(isSubmitting = true, errorMessage = null) }
      val result =
        if (_authFormState.value.isRegisterMode) {
          repository.register(cleanEmail, password)
        } else {
          repository.login(cleanEmail, password)
        }
      when (result) {
        is ApiResult.Success -> {
          _authFormState.update { it.copy(isSubmitting = false, errorMessage = null) }
          _events.emit(UiEvent.AuthSuccess)
          _events.emit(UiEvent.Snackbar("Welcome, ${result.data.email}"))
          refreshOrders()
        }
        is ApiResult.Error -> {
          _authFormState.update { it.copy(isSubmitting = false, errorMessage = result.message) }
        }
      }
    }
  }

  fun signOut() {
    viewModelScope.launch {
      repository.logout()
      _ordersState.value = OrdersUiState.SignedOut
      _events.emit(UiEvent.Snackbar("Signed out of Wude Mart"))
    }
  }

  fun refreshAccountProfile() {
    viewModelScope.launch {
      when (val res = repository.refreshProfile()) {
        is ApiResult.Success -> _events.emit(UiEvent.Snackbar("Account refreshed"))
        is ApiResult.Error -> _events.emit(UiEvent.Snackbar(res.message))
      }
    }
  }

  fun saveDeliveryProfile(profile: DeliveryProfileStorage) {
    viewModelScope.launch {
      repository.saveDeliveryProfile(profile)
      _events.emit(UiEvent.Snackbar("Delivery details saved"))
    }
  }

  fun setThemePreference(pref: ThemePreference) {
    viewModelScope.launch { repository.saveThemePreference(pref) }
  }

  fun setLanguage(lang: AppLanguage) {
    viewModelScope.launch { repository.saveLanguagePreference(lang) }
  }

  fun detectOneTimeDeliveryAddress(
    onResolved: (city: String, subCity: String, addressLine: String, lat: Double, lng: Double) -> Unit,
  ) {
    if (_checkoutState.value.isDetectingLocation) return
    viewModelScope.launch {
      _checkoutState.update { it.copy(isDetectingLocation = true, locationStatusMessage = null) }
      val resolved = locationHelper.fetchOneTimeDeliveryAddress()
      if (resolved != null) {
        val current = deliveryProfile.value
        repository.saveDeliveryProfile(
          current.copy(
            city = resolved.city.ifBlank { current.city },
            subCity = resolved.subCity.ifBlank { current.subCity },
            addressLine = resolved.addressLine.ifBlank { current.addressLine },
            latitude = resolved.latitude,
            longitude = resolved.longitude,
          )
        )
        onResolved(
          resolved.city,
          resolved.subCity,
          resolved.addressLine,
          resolved.latitude,
          resolved.longitude,
        )
        _checkoutState.update {
          it.copy(
            isDetectingLocation = false,
            locationStatusMessage = currentStrings().locationFilledSuccess,
          )
        }
      } else {
        _checkoutState.update {
          it.copy(
            isDetectingLocation = false,
            locationStatusMessage = currentStrings().locationUnavailableNotice,
          )
        }
      }
    }
  }

  fun notifyLocationPermissionDenied() {
    _checkoutState.update {
      it.copy(
        isDetectingLocation = false,
        locationStatusMessage = currentStrings().locationDeniedNotice,
      )
    }
  }

  fun clearLocationStatusMessage() {
    _checkoutState.update { it.copy(locationStatusMessage = null) }
  }

  fun placeOrder(
    fullName: String,
    phone: String,
    city: String,
    subCity: String,
    addressLine: String,
    extraNote: String,
    latitude: Double? = deliveryProfile.value.latitude,
    longitude: Double? = deliveryProfile.value.longitude,
  ) {
    if (fullName.trim().length < 2) {
      _checkoutState.update { it.copy(errorMessage = "Please enter the recipient's full name.") }
      return
    }
    if (!EthiopianPhoneValidator.isValid(phone)) {
      _checkoutState.update {
        it.copy(
          errorMessage = "Enter a valid Ethiopian phone number (e.g. +251911223344 or 0911223344)."
        )
      }
      return
    }
    if (addressLine.trim().length < 4) {
      _checkoutState.update {
        it.copy(errorMessage = "Please provide a delivery street, building, or landmark.")
      }
      return
    }
    val items = cartItems.value
    if (items.isEmpty()) {
      _checkoutState.update { it.copy(errorMessage = "Your cart is empty.") }
      return
    }

    val normalizedPhone = EthiopianPhoneValidator.normalize(phone)
    val delivery =
      DeliveryProfileStorage(
        fullName = fullName.trim(),
        phone = normalizedPhone,
        city = city.trim(),
        subCity = subCity.trim(),
        addressLine = addressLine.trim(),
        latitude = latitude,
        longitude = longitude,
      )

    viewModelScope.launch {
      _checkoutState.update { it.copy(isPlacingOrder = true, errorMessage = null) }
      when (val res = repository.placeOrder(items, delivery, extraNote)) {
        is ApiResult.Success -> {
          _checkoutState.update { it.copy(isPlacingOrder = false, errorMessage = null) }
          refreshOrders()
          _events.emit(UiEvent.OrderPlaced(res.data.id))
        }
        is ApiResult.Error -> {
          _checkoutState.update { it.copy(isPlacingOrder = false, errorMessage = res.message) }
        }
      }
    }
  }

  fun clearCheckoutError() {
    _checkoutState.update { it.copy(errorMessage = null) }
  }

  fun refreshOrders() {
    val currentSession = session.value
    if (currentSession == null || !currentSession.isSignedIn) {
      _ordersState.value = OrdersUiState.SignedOut
      return
    }
    viewModelScope.launch {
      if (_ordersState.value !is OrdersUiState.Success) {
        _ordersState.value = OrdersUiState.Loading
      }
      when (val res = repository.fetchOrders()) {
        is ApiResult.Success -> _ordersState.value = OrdersUiState.Success(res.data)
        is ApiResult.Error -> {
          _ordersState.value =
            if (res.isAuthError) OrdersUiState.SignedOut else OrdersUiState.Error(res.message)
        }
      }
    }
  }

  fun selectPaymentMethod(methodId: String) {
    _orderDetailState.update { it.copy(selectedPaymentMethodId = methodId) }
  }

  fun dismissWaitingApprovalView() {
    _orderDetailState.update { it.copy(awaitingApprovalState = false) }
  }

  fun loadOrderDetail(orderId: Long, startPolling: Boolean = true) {
    orderPollingJob?.cancel()
    deliveryQuoteJob?.cancel()
    viewModelScope.launch {
      _orderDetailState.value = OrderDetailUiState(isLoading = true)
      fetchOrderDetailInternal(orderId)
    }
    if (startPolling) {
      orderPollingJob =
        viewModelScope.launch {
          while (isActive) {
            val currentOrder = _orderDetailState.value.order
            val interval =
              if (currentOrder?.status == OrderStatus.PAYMENT_REVIEW) 10_000L else 20_000L
            delay(interval)
            if (
              currentOrder != null &&
                currentOrder.status != OrderStatus.DELIVERED &&
                currentOrder.status != OrderStatus.CANCELLED
            ) {
              fetchOrderDetailInternal(orderId, silent = true)
            }
          }
        }
    }
  }

  fun stopOrderDetailPolling() {
    orderPollingJob?.cancel()
    orderPollingJob = null
  }

  fun refreshOrderDetail(orderId: Long) {
    viewModelScope.launch {
      _orderDetailState.update { it.copy(isRefreshing = true, error = null) }
      fetchOrderDetailInternal(orderId)
    }
  }

  private suspend fun fetchOrderDetailInternal(orderId: Long, silent: Boolean = false) {
    when (val res = repository.fetchOrderDetail(orderId)) {
      is ApiResult.Success -> {
        val order = res.data
        val methodsRes = repository.fetchBackendPaymentInstructions(order)
        val instructions = (methodsRes as? ApiResult.Success)?.data.orEmpty()
        val methodsError = (methodsRes as? ApiResult.Error)?.message
        val storeConfig = order.pickupInfo ?: repository.fetchStorePickupConfig()

        val prevOrder = _orderDetailState.value.order
        val dateChanged =
          prevOrder != null &&
            ((prevOrder.expectedDeliveryDate != null &&
              order.expectedDeliveryDate != null &&
              prevOrder.expectedDeliveryDate != order.expectedDeliveryDate) ||
              (prevOrder.expectedPickupDate != null &&
                order.expectedPickupDate != null &&
                prevOrder.expectedPickupDate != order.expectedPickupDate))
        if (dateChanged) {
          _events.emit(UiEvent.Snackbar(currentStrings().scheduleUpdatedNotice))
        }

        _orderDetailState.update { prev ->
          val nextSelected =
            prev.selectedPaymentMethodId?.takeIf { id -> instructions.any { it.id == id } }
              ?: instructions.firstOrNull()?.id
          val stillInReview = order.status == OrderStatus.PAYMENT_REVIEW
          val keepWaitingScreen =
            when {
              order.isPaymentVerified ||
                order.status == OrderStatus.REJECTED ||
                order.latestRejectedPayment != null ||
                order.status == OrderStatus.CANCELLED -> false
              prev.awaitingApprovalState && stillInReview -> true
              else -> false
            }
          prev.copy(
            isLoading = false,
            isRefreshing = false,
            order = order,
            paymentInstructions = instructions,
            paymentMethodsError = methodsError,
            selectedPaymentMethodId = nextSelected,
            instructionsLoaded = true,
            awaitingApprovalState = keepWaitingScreen,
            storePickupConfig = storeConfig,
            scheduleUpdatedAlert = prev.scheduleUpdatedAlert || dateChanged,
            error = null,
          )
        }
      }
      is ApiResult.Error -> {
        if (!silent) {
          _orderDetailState.update {
            it.copy(isLoading = false, isRefreshing = false, error = res.message)
          }
        } else {
          _orderDetailState.update { it.copy(isRefreshing = false) }
        }
      }
    }
  }

  fun clearDeliveryQuote() {
    deliveryQuoteJob?.cancel()
    _orderDetailState.update {
      it.copy(
        isLoadingDeliveryQuote = false,
        quotedDeliveryFee = null,
        quotedSubtotal = null,
        quotedTotal = null,
        quotedZoneId = null,
        quotedZoneName = "",
        quotedEstimatedDelivery = null,
        deliveryQuoteError = null,
        lastQuotedAddressKey = null,
      )
    }
  }

  fun requestDeliveryQuote(
    orderId: Long,
    subtotal: Double,
    city: String,
    subCity: String,
    addressLine: String,
    lat: Double?,
    lng: Double?,
    force: Boolean = false,
  ) {
    val cleanCity = city.trim()
    val cleanSubCity = subCity.trim()
    val cleanAddress = addressLine.trim()
    if (cleanCity.isBlank() || cleanAddress.length < 3) {
      clearDeliveryQuote()
      return
    }
    val addressKey = "$orderId|$cleanCity|$cleanSubCity|$cleanAddress|${lat ?: ""}|${lng ?: ""}"
    val current = _orderDetailState.value
    if (
      !force &&
        addressKey == current.lastQuotedAddressKey &&
        (current.isLoadingDeliveryQuote ||
          (current.quotedDeliveryFee != null && current.deliveryQuoteError == null))
    ) {
      return
    }
    deliveryQuoteJob?.cancel()
    deliveryQuoteJob =
      viewModelScope.launch {
        _orderDetailState.update {
          it.copy(
            isLoadingDeliveryQuote = true,
            quotedDeliveryFee = null,
            quotedSubtotal = null,
            quotedTotal = null,
            quotedZoneId = null,
            quotedZoneName = "",
            quotedEstimatedDelivery = null,
            deliveryQuoteError = null,
            fulfillmentError = null,
            lastQuotedAddressKey = addressKey,
          )
        }
        when (
          val res =
            repository.fetchDeliveryQuote(
              orderId = orderId,
              subtotal = subtotal,
              city = cleanCity,
              subCity = cleanSubCity,
              addressLine = cleanAddress,
              lat = lat,
              lng = lng,
            )
        ) {
          is ApiResult.Success -> {
            if (!isActive || _orderDetailState.value.lastQuotedAddressKey != addressKey) return@launch
            _orderDetailState.update {
              it.copy(
                isLoadingDeliveryQuote = false,
                quotedDeliveryFee = res.data.deliveryFee,
                quotedSubtotal = res.data.subtotal,
                quotedTotal = res.data.total,
                quotedZoneId = res.data.zoneId,
                quotedZoneName = res.data.zoneName,
                quotedEstimatedDelivery = res.data.estimatedDelivery,
                deliveryQuoteError = null,
              )
            }
          }
          is ApiResult.Error -> {
            if (!isActive || _orderDetailState.value.lastQuotedAddressKey != addressKey) return@launch
            _orderDetailState.update {
              it.copy(
                isLoadingDeliveryQuote = false,
                quotedDeliveryFee = null,
                quotedSubtotal = null,
                quotedTotal = null,
                quotedZoneId = null,
                quotedZoneName = "",
                quotedEstimatedDelivery = null,
                deliveryQuoteError = res.message,
              )
            }
          }
        }
      }
  }

  fun submitFulfillmentChoice(
    orderId: Long,
    method: FulfillmentMethod,
    updatedProfile: DeliveryProfileStorage?,
    note: String,
  ) {
    val currentState = _orderDetailState.value
    if (currentState.isSubmittingFulfillment) return

    val authoritativeFee: Double
    val zoneId: Long?
    if (method == FulfillmentMethod.DELIVERY) {
      val fee = currentState.quotedDeliveryFee
      if (currentState.isLoadingDeliveryQuote || fee == null) {
        _orderDetailState.update {
          it.copy(fulfillmentError = currentStrings().deliveryFeeRequiredError)
        }
        return
      }
      authoritativeFee = fee
      zoneId = currentState.quotedZoneId
    } else {
      authoritativeFee = 0.0
      zoneId = null
    }

    viewModelScope.launch {
      _orderDetailState.update { it.copy(isSubmittingFulfillment = true, fulfillmentError = null) }
      if (updatedProfile != null) {
        repository.saveDeliveryProfile(updatedProfile)
      }
      when (
        val res =
          repository.submitFulfillmentChoice(
            orderId = orderId,
            method = method,
            deliveryProfile = updatedProfile,
            note = note,
            quotedDeliveryFee = authoritativeFee,
            deliveryZoneId = zoneId,
          )
      ) {
        is ApiResult.Success -> {
          _orderDetailState.update {
            it.copy(
              isSubmittingFulfillment = false,
              order = res.data,
              fulfillmentError = null,
            )
          }
          _events.emit(UiEvent.Snackbar(currentStrings().fulfillmentConfirmedLabel))
          refreshOrders()
        }
        is ApiResult.Error -> {
          _orderDetailState.update {
            it.copy(isSubmittingFulfillment = false, fulfillmentError = res.message)
          }
        }
      }
    }
  }

  fun uploadPaymentScreenshot(
    orderId: Long,
    imageUri: Uri,
    onSuccessClearUri: () -> Unit,
  ) {
    if (_orderDetailState.value.isUploadingProof) return
    val state = _orderDetailState.value
    val selectedMethodName =
      state.paymentInstructions
        .firstOrNull { it.id == state.selectedPaymentMethodId }
        ?.providerName

    viewModelScope.launch {
      _orderDetailState.update {
        it.copy(isUploadingProof = true, uploadSuccessMessage = null, error = null)
      }
      when (
        val res =
          repository.uploadPaymentScreenshot(
            orderId = orderId,
            imageUri = imageUri,
            selectedPaymentMethod = selectedMethodName,
          )
      ) {
        is ApiResult.Success -> {
          onSuccessClearUri()
          _orderDetailState.update {
            it.copy(
              isUploadingProof = false,
              awaitingApprovalState = true,
              lastSubmittedPaymentId = res.data.id,
              uploadSuccessMessage = currentStrings().paymentSubmittedSuccess,
              error = null,
            )
          }
          _events.emit(UiEvent.Snackbar(currentStrings().paymentSubmittedSuccess))
          fetchOrderDetailInternal(orderId)
          refreshOrders()
        }
        is ApiResult.Error -> {
          _orderDetailState.update {
            it.copy(isUploadingProof = false, error = res.message)
          }
        }
      }
    }
  }

  fun loadDeliveryTracking(orderId: Long) {
    trackingPollingJob?.cancel()
    viewModelScope.launch {
      _trackingState.value = DeliveryTrackingUiState(isLoading = true)
      fetchTrackingInternal(orderId)
    }
    trackingPollingJob =
      viewModelScope.launch {
        while (isActive) {
          delay(10_000L)
          val st = _trackingState.value.order?.status
          if (st != OrderStatus.DELIVERED && st != OrderStatus.CANCELLED) {
            fetchTrackingInternal(orderId, silent = true)
          }
        }
      }
  }

  fun refreshDeliveryTracking(orderId: Long) {
    viewModelScope.launch {
      _trackingState.update { it.copy(isRefreshing = true, error = null) }
      fetchTrackingInternal(orderId)
    }
  }

  fun stopDeliveryTrackingPolling() {
    trackingPollingJob?.cancel()
    trackingPollingJob = null
  }

  private suspend fun fetchTrackingInternal(orderId: Long, silent: Boolean = false) {
    val orderRes = repository.fetchOrderDetail(orderId)
    val trackRes = repository.fetchOrderTracking(orderId)
    val order = (orderRes as? ApiResult.Success)?.data ?: _trackingState.value.order
    val tracking = (trackRes as? ApiResult.Success)?.data ?: order?.deliveryTracking
    val err =
      when {
        orderRes is ApiResult.Error && !silent -> orderRes.message
        trackRes is ApiResult.Error && !silent -> trackRes.message
        else -> null
      }
    _trackingState.update {
      it.copy(
        isLoading = false,
        isRefreshing = false,
        order = order,
        tracking = tracking,
        error = err,
      )
    }
  }

  override fun onCleared() {
    super.onCleared()
    orderPollingJob?.cancel()
    trackingPollingJob?.cancel()
    deliveryQuoteJob?.cancel()
  }
}
