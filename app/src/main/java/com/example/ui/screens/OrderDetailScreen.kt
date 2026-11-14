package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import coil.ImageLoader
import coil.compose.AsyncImage
import com.example.data.model.DeliveryProfileStorage
import com.example.data.model.EtbFormatter
import com.example.data.model.FulfillmentMethod
import com.example.data.model.MessageTone
import com.example.data.model.Order
import com.example.data.model.OrderStatus
import com.example.data.model.PaymentInstruction
import com.example.data.model.PaymentRecord
import com.example.data.model.StorePickupConfig
import com.example.data.model.TransactionMessage
import com.example.ui.components.ErrorStateView
import com.example.ui.components.EtbPriceText
import com.example.ui.components.ExpectedScheduleCard
import com.example.ui.components.GlassCard
import com.example.ui.components.LoadingStateView
import com.example.ui.components.OrderStatusPill
import com.example.ui.components.OrderStatusStepper
import com.example.ui.components.ProductThumbnail
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.LocalWudeStrings
import com.example.ui.theme.StatusDanger
import com.example.ui.theme.StatusDangerBg
import com.example.ui.theme.StatusInfo
import com.example.ui.theme.StatusInfoBg
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusSuccessBg
import com.example.ui.theme.StatusWarning
import com.example.ui.theme.StatusWarningBg
import com.example.ui.viewmodel.OrderDetailUiState
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailScreen(
  orderId: Long,
  uiState: OrderDetailUiState,
  language: AppLanguage,
  savedDelivery: DeliveryProfileStorage,
  isDetectingLocation: Boolean,
  locationStatusMessage: String?,
  imageLoader: ImageLoader,
  onBack: () -> Unit,
  onRefresh: () -> Unit,
  onSelectPaymentMethod: (String) -> Unit,
  onDismissWaitingView: () -> Unit,
  onUploadProof: (uri: Uri, onSuccessClearUri: () -> Unit) -> Unit,
  onDetectLocation: (onResolved: (String, String, String, Double, Double) -> Unit) -> Unit,
  onLocationDenied: () -> Unit,
  onRequestDeliveryQuote: (city: String, subCity: String, addressLine: String, lat: Double?, lng: Double?, force: Boolean) -> Unit,
  onClearDeliveryQuote: () -> Unit,
  onSubmitFulfillment: (FulfillmentMethod, DeliveryProfileStorage?, String) -> Unit,
  onTrackDelivery: () -> Unit,
  onCopiedMessage: (String) -> Unit,
) {
  val strings = LocalWudeStrings.current
  var selectedUriString by rememberSaveable(orderId) { mutableStateOf<String?>(null) }

  val photoPickerLauncher =
    rememberLauncherForActivityResult(
      contract = ActivityResultContracts.PickVisualMedia(),
      onResult = { uri ->
        if (uri != null) {
          selectedUriString = uri.toString()
        }
      },
    )

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("${strings.orderLabel} #$orderId") },
        navigationIcon = {
          IconButton(
            onClick = onBack,
            modifier = Modifier.minimumInteractiveComponentSize().testTag("order_detail_back"),
          ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.back)
          }
        },
        actions = {
          IconButton(
            onClick = onRefresh,
            enabled = !uiState.isRefreshing,
            modifier = Modifier.minimumInteractiveComponentSize().testTag("order_detail_refresh"),
          ) {
            if (uiState.isRefreshing) {
              CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
              Icon(Icons.Filled.Refresh, contentDescription = strings.refresh)
            }
          }
        },
        colors =
          TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
          ),
      )
    }
  ) { innerPadding ->
    when {
      uiState.isLoading && uiState.order == null -> {
        LoadingStateView(
          message = "${strings.orderLabel} #$orderId…",
          modifier = Modifier.padding(innerPadding),
        )
      }
      uiState.error != null && uiState.order == null -> {
        ErrorStateView(
          message = uiState.error,
          onRetry = onRefresh,
          modifier = Modifier.padding(innerPadding),
        )
      }
      uiState.order != null -> {
        val order = uiState.order
        val showWaitingScreen =
          uiState.awaitingApprovalState &&
            (order.status == OrderStatus.PAYMENT_REVIEW || order.status == OrderStatus.PENDING_PAYMENT) &&
            order.latestRejectedPayment == null

        AnimatedContent(
          targetState = showWaitingScreen,
          transitionSpec = {
            fadeIn(animationSpec = tween(260)) togetherWith fadeOut(animationSpec = tween(220))
          },
          label = "order_waiting_transition",
          modifier =
            Modifier.fillMaxSize()
              .background(MaterialTheme.colorScheme.background)
              .padding(innerPadding),
        ) { isWaiting ->
          if (isWaiting) {
            PaymentWaitingApprovalView(
              order = order,
              lastPaymentId = uiState.lastSubmittedPaymentId ?: order.payments.lastOrNull()?.id,
              isRefreshing = uiState.isRefreshing,
              onRefreshStatus = onRefresh,
              onReturnToOrderTracking = onBack,
              onViewOrderDetails = onDismissWaitingView,
            )
          } else {
            Box(
              modifier = Modifier.fillMaxSize(),
              contentAlignment = Alignment.TopCenter,
            ) {
              Column(
                modifier =
                  Modifier.widthIn(max = 720.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
              ) {
                if (uiState.error != null) {
                  Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = StatusDangerBg,
                    border = BorderStroke(1.dp, StatusDanger.copy(alpha = 0.35f)),
                  ) {
                    Row(
                      modifier = Modifier.fillMaxWidth().padding(12.dp),
                      verticalAlignment = Alignment.CenterVertically,
                    ) {
                      Icon(
                        imageVector = Icons.Filled.ErrorOutline,
                        contentDescription = null,
                        tint = StatusDanger,
                      )
                      Spacer(modifier = Modifier.width(10.dp))
                      Text(
                        text = uiState.error,
                        style = MaterialTheme.typography.bodySmall,
                        color = StatusDanger,
                      )
                    }
                  }
                }

                // Live Order Status & Approval/Rejection Callout Banner
                OrderStatusFeedbackBanner(order = order, language = language)

                // Live Order Status Stepper & Amount
                OrderLiveTrackingCard(
                  order = order,
                  language = language,
                  onTrackDelivery = onTrackDelivery,
                )

                // Expected Delivery / Pickup Date Card
                if (order.status != OrderStatus.CANCELLED) {
                  val isPickup = order.fulfillmentMethod == FulfillmentMethod.STORE_PICKUP
                  ExpectedScheduleCard(
                    isPickup = isPickup,
                    expectedDate =
                      if (isPickup) order.expectedPickupDate ?: order.expectedDeliveryDate
                      else order.expectedDeliveryDate,
                    expectedWindow =
                      if (isPickup) order.expectedPickupWindow ?: order.expectedDeliveryWindow
                      else order.expectedDeliveryWindow,
                    scheduleUpdatedAlert = uiState.scheduleUpdatedAlert,
                  )
                }

                // Post-Payment Fulfillment Choice (ONLY after backend verifies payment)
                if (order.isPaymentVerified) {
                  if (order.fulfillmentMethod == null) {
                    PostPaymentFulfillmentChoiceCard(
                      order = order,
                      uiState = uiState,
                      savedDelivery = savedDelivery,
                      storeConfig = order.pickupInfo ?: uiState.storePickupConfig,
                      isDetectingLocation = isDetectingLocation,
                      locationStatusMessage = locationStatusMessage,
                      isSubmitting = uiState.isSubmittingFulfillment,
                      errorMessage = uiState.fulfillmentError,
                      onDetectLocation = onDetectLocation,
                      onLocationDenied = onLocationDenied,
                      onRequestDeliveryQuote = onRequestDeliveryQuote,
                      onClearDeliveryQuote = onClearDeliveryQuote,
                      onConfirmChoice = onSubmitFulfillment,
                    )
                  } else {
                    ConfirmedFulfillmentCard(
                      order = order,
                      storeConfig = order.pickupInfo ?: uiState.storePickupConfig,
                    )
                  }
                }

                // Transaction & Admin Messages
                TransactionMessagesCard(messages = order.messages)

                // Active Payment Methods & Instructions from Backend
                if (
                  !order.isPaymentVerified &&
                    (order.status == OrderStatus.PENDING_PAYMENT ||
                      order.status == OrderStatus.PAYMENT_REVIEW ||
                      order.status == OrderStatus.REJECTED)
                ) {
                  BackendPaymentMethodsCard(
                    order = order,
                    instructions = uiState.paymentInstructions,
                    methodsError = uiState.paymentMethodsError,
                    selectedMethodId = uiState.selectedPaymentMethodId,
                    isRefreshing = uiState.isRefreshing,
                    onSelectMethod = onSelectPaymentMethod,
                    onRefresh = onRefresh,
                    onCopied = onCopiedMessage,
                  )

                  // Payment Screenshot Upload (no transaction reference field)
                  PaymentUploadCard(
                    order = order,
                    selectedUri = selectedUriString?.let { Uri.parse(it) },
                    selectedMethod =
                      uiState.paymentInstructions.firstOrNull {
                        it.id == uiState.selectedPaymentMethodId
                      },
                    isUploading = uiState.isUploadingProof,
                    hasUploadError = uiState.error != null,
                    imageLoader = imageLoader,
                    onPickImage = {
                      photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                      )
                    },
                    onSubmit = {
                      val uri = selectedUriString?.let { Uri.parse(it) }
                      if (uri != null) {
                        onUploadProof(uri) {
                          selectedUriString = null
                        }
                      }
                    },
                  )
                }

                // Submitted Payment Records
                if (order.payments.isNotEmpty()) {
                  SubmittedPaymentsCard(payments = order.payments)
                }

                // Items & Delivery Note Breakdown
                OrderItemsBreakdownCard(
                  order = order,
                  imageLoader = imageLoader,
                )
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun PostPaymentFulfillmentChoiceCard(
  order: Order,
  uiState: OrderDetailUiState,
  savedDelivery: DeliveryProfileStorage,
  storeConfig: StorePickupConfig?,
  isDetectingLocation: Boolean,
  locationStatusMessage: String?,
  isSubmitting: Boolean,
  errorMessage: String?,
  onDetectLocation: (onResolved: (String, String, String, Double, Double) -> Unit) -> Unit,
  onLocationDenied: () -> Unit,
  onRequestDeliveryQuote: (city: String, subCity: String, addressLine: String, lat: Double?, lng: Double?, force: Boolean) -> Unit,
  onClearDeliveryQuote: () -> Unit,
  onConfirmChoice: (FulfillmentMethod, DeliveryProfileStorage?, String) -> Unit,
) {
  val strings = LocalWudeStrings.current
  val context = LocalContext.current

  var selectedMethod by rememberSaveable(order.id) { mutableStateOf(FulfillmentMethod.DELIVERY) }
  var city by rememberSaveable(order.id, savedDelivery.city) {
    mutableStateOf(savedDelivery.city.ifBlank { order.deliveryCity.ifBlank { "Addis Ababa" } })
  }
  var subCity by rememberSaveable(order.id, savedDelivery.subCity) {
    mutableStateOf(savedDelivery.subCity.ifBlank { "Bole" })
  }
  var addressLine by rememberSaveable(order.id, savedDelivery.addressLine) {
    mutableStateOf(savedDelivery.addressLine.ifBlank { order.deliveryAddress })
  }
  var deliveryNote by rememberSaveable(order.id) { mutableStateOf("") }
  var detectedLat by rememberSaveable(order.id) { mutableStateOf(savedDelivery.latitude) }
  var detectedLng by rememberSaveable(order.id) { mutableStateOf(savedDelivery.longitude) }
  var showLocationRationale by remember { mutableStateOf(false) }

  LaunchedEffect(selectedMethod, city, subCity, addressLine, detectedLat, detectedLng) {
    if (selectedMethod == FulfillmentMethod.DELIVERY) {
      if (city.trim().isNotBlank() && addressLine.trim().length >= 3) {
        delay(450)
        onRequestDeliveryQuote(
          city.trim(),
          subCity.trim(),
          addressLine.trim(),
          detectedLat,
          detectedLng,
          false,
        )
      } else {
        onClearDeliveryQuote()
      }
    } else {
      onClearDeliveryQuote()
    }
  }

  fun triggerSingleShotLocation() {
    onDetectLocation { resolvedCity, resolvedSubCity, resolvedAddress, lat, lng ->
      if (resolvedCity.isNotBlank()) city = resolvedCity
      if (resolvedSubCity.isNotBlank()) subCity = resolvedSubCity
      if (resolvedAddress.isNotBlank()) addressLine = resolvedAddress
      detectedLat = lat
      detectedLng = lng
    }
  }

  val locationPermissionLauncher =
    rememberLauncherForActivityResult(
      contract = ActivityResultContracts.RequestMultiplePermissions(),
      onResult = { grants ->
        val granted =
          grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
          triggerSingleShotLocation()
        } else {
          onLocationDenied()
        }
      },
    )

  if (showLocationRationale) {
    AlertDialog(
      onDismissRequest = { showLocationRationale = false },
      title = { Text(strings.locationRationaleTitle) },
      text = { Text(strings.locationRationaleBody) },
      confirmButton = {
        Button(
          onClick = {
            showLocationRationale = false
            locationPermissionLauncher.launch(
              arrayOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
              )
            )
          },
        ) {
          Text(strings.allowLocation)
        }
      },
      dismissButton = {
        TextButton(onClick = { showLocationRationale = false }) {
          Text(strings.enterManually)
        }
      },
    )
  }

  GlassCard(
    shape = RoundedCornerShape(20.dp),
    modifier = Modifier.fillMaxWidth().testTag("post_payment_fulfillment_card"),
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Surface(
        shape = RoundedCornerShape(50),
        color = StatusSuccessBg,
      ) {
        Text(
          text = strings.paymentVerifiedTitle,
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.Bold,
          color = StatusSuccess,
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
        )
      }

      Text(
        text = strings.howReceiveOrder,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
      )

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        val deliverySelected = selectedMethod == FulfillmentMethod.DELIVERY
        Surface(
          shape = RoundedCornerShape(16.dp),
          color =
            if (deliverySelected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
          border =
            BorderStroke(
              width = if (deliverySelected) 1.5.dp else 1.dp,
              color =
                if (deliverySelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outlineVariant,
            ),
          modifier =
            Modifier.weight(1f)
              .clip(RoundedCornerShape(16.dp))
              .clickable { selectedMethod = FulfillmentMethod.DELIVERY }
              .testTag("fulfillment_option_delivery"),
        ) {
          Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Icon(
              imageVector = Icons.Filled.LocalShipping,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
            )
            Text(
              text = strings.fulfillmentDeliveryTitle,
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
            )
            Text(
              text = strings.fulfillmentDeliverySub,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }

        val pickupSelected = selectedMethod == FulfillmentMethod.STORE_PICKUP
        Surface(
          shape = RoundedCornerShape(16.dp),
          color =
            if (pickupSelected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
          border =
            BorderStroke(
              width = if (pickupSelected) 1.5.dp else 1.dp,
              color =
                if (pickupSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outlineVariant,
            ),
          modifier =
            Modifier.weight(1f)
              .clip(RoundedCornerShape(16.dp))
              .clickable { selectedMethod = FulfillmentMethod.STORE_PICKUP }
              .testTag("fulfillment_option_pickup"),
        ) {
          Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Icon(
              imageVector = Icons.Filled.Storefront,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
            )
            Text(
              text = strings.fulfillmentPickupTitle,
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
            )
            Text(
              text = strings.fulfillmentPickupSub,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }

      if (selectedMethod == FulfillmentMethod.DELIVERY) {
        OutlinedButton(
          onClick = {
            val hasFine =
              ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION,
              ) == PackageManager.PERMISSION_GRANTED
            val hasCoarse =
              ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION,
              ) == PackageManager.PERMISSION_GRANTED
            if (hasFine || hasCoarse) {
              triggerSingleShotLocation()
            } else {
              showLocationRationale = true
            }
          },
          enabled = !isDetectingLocation,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize(),
        ) {
          if (isDetectingLocation) {
            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
            Spacer(modifier = Modifier.width(8.dp))
            Text(strings.detectingLocation)
          } else {
            Icon(Icons.Filled.MyLocation, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(strings.useCurrentLocation)
          }
        }

        if (locationStatusMessage != null) {
          Text(
            text = locationStatusMessage,
            style = MaterialTheme.typography.bodySmall,
            color = StatusInfo,
          )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = city,
            onValueChange = { city = it },
            label = { Text(strings.cityRegion) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.weight(1f).testTag("fulfillment_city_input"),
          )
          OutlinedTextField(
            value = subCity,
            onValueChange = { subCity = it },
            label = { Text(strings.addisSubCity) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.weight(1f).testTag("fulfillment_subcity_input"),
          )
        }

        OutlinedTextField(
          value = addressLine,
          onValueChange = { addressLine = it },
          label = { Text(strings.streetLandmark) },
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth().testTag("fulfillment_address_input"),
        )

        OutlinedTextField(
          value = deliveryNote,
          onValueChange = { deliveryNote = it },
          label = { Text(strings.deliveryNoteOptional) },
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth().testTag("fulfillment_note_input"),
        )
      } else {
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
          modifier = Modifier.fillMaxWidth().testTag("store_pickup_info_card"),
        ) {
          Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Text(
              text = strings.noDeliveryFeePickup,
              style = MaterialTheme.typography.labelLarge,
              color = StatusSuccess,
              fontWeight = FontWeight.SemiBold,
            )
            if (storeConfig != null && (storeConfig.storeName.isNotBlank() || storeConfig.storeAddress.isNotBlank() || storeConfig.pickupInstructions.isNotBlank() || storeConfig.openingHours.isNotBlank())) {
              if (storeConfig.storeName.isNotBlank()) {
                Text(
                  text = storeConfig.storeName,
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.Bold,
                )
              }
              if (storeConfig.storeAddress.isNotBlank()) {
                Text(
                  text = "${strings.storeAddressLabel}: ${storeConfig.storeAddress}",
                  style = MaterialTheme.typography.bodyMedium,
                  fontWeight = FontWeight.SemiBold,
                )
              }
              if (storeConfig.openingHours.isNotBlank()) {
                Text(
                  text = "${strings.openingHoursLabel}: ${storeConfig.openingHours}",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
              if (storeConfig.pickupInstructions.isNotBlank()) {
                Text(
                  text = "${strings.pickupInstructionsLabel}: ${storeConfig.pickupInstructions}",
                  style = MaterialTheme.typography.bodySmall,
                )
              }
            } else {
              Text(
                text = strings.pickupConfigPending,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
      }

      // Complete ETB Price Breakdown (Authoritative backend pricing for Delivery, ETB 0.00 for Store Pickup)
      val productSubtotal =
        if (selectedMethod == FulfillmentMethod.DELIVERY && uiState.quotedSubtotal != null) {
          uiState.quotedSubtotal
        } else {
          order.effectiveProductSubtotal
        }
      val hasAddressForQuote = city.trim().isNotBlank() && addressLine.trim().length >= 3
      val deliveryFeeForDisplay: Double? =
        if (selectedMethod == FulfillmentMethod.STORE_PICKUP) {
          0.0
        } else {
          uiState.quotedDeliveryFee
        }
      val finalTotalForDisplay: Double? =
        if (selectedMethod == FulfillmentMethod.STORE_PICKUP) {
          productSubtotal
        } else if (!uiState.isLoadingDeliveryQuote && uiState.quotedDeliveryFee != null) {
          uiState.quotedTotal ?: (productSubtotal + uiState.quotedDeliveryFee)
        } else {
          null
        }

      Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.52f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth().testTag("fulfillment_price_breakdown_card"),
      ) {
        Column(
          modifier = Modifier.padding(14.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = strings.priceBreakdownTitle,
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
            )
            if (selectedMethod == FulfillmentMethod.DELIVERY && hasAddressForQuote) {
              TextButton(
                onClick = {
                  onRequestDeliveryQuote(
                    city.trim(),
                    subCity.trim(),
                    addressLine.trim(),
                    detectedLat,
                    detectedLng,
                    true,
                  )
                },
                enabled = !uiState.isLoadingDeliveryQuote,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                modifier =
                  Modifier.minimumInteractiveComponentSize()
                    .testTag("refresh_delivery_quote_button"),
              ) {
                Icon(
                  imageVector = Icons.Filled.Refresh,
                  contentDescription = strings.refreshQuote,
                  modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(strings.refreshQuote, style = MaterialTheme.typography.labelMedium)
              }
            }
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = strings.productSubtotalLabel,
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
              text = EtbFormatter.format(productSubtotal),
              style = MaterialTheme.typography.bodyMedium,
              fontWeight = FontWeight.SemiBold,
              modifier = Modifier.testTag("fulfillment_subtotal_value"),
            )
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = strings.deliveryFeeLabel,
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            when {
              selectedMethod == FulfillmentMethod.STORE_PICKUP -> {
                Text(
                  text = EtbFormatter.format(0.0),
                  style = MaterialTheme.typography.bodyMedium,
                  fontWeight = FontWeight.Bold,
                  color = StatusSuccess,
                  modifier = Modifier.testTag("fulfillment_delivery_fee_value"),
                )
              }
              uiState.isLoadingDeliveryQuote -> {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(6.dp),
                  modifier = Modifier.testTag("fulfillment_delivery_fee_loading"),
                ) {
                  CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                  Text(
                    text = strings.calculatingDeliveryFee,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                  )
                }
              }
              deliveryFeeForDisplay != null -> {
                Text(
                  text = EtbFormatter.format(deliveryFeeForDisplay),
                  style = MaterialTheme.typography.bodyMedium,
                  fontWeight = FontWeight.Bold,
                  modifier = Modifier.testTag("fulfillment_delivery_fee_value"),
                )
              }
              else -> {
                Text(
                  text = "—",
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.testTag("fulfillment_delivery_fee_unavailable"),
                )
              }
            }
          }

          if (
            selectedMethod == FulfillmentMethod.DELIVERY &&
              !uiState.isLoadingDeliveryQuote &&
              uiState.quotedZoneName.isNotBlank()
          ) {
            Text(
              text = "${strings.deliveryZoneLabel}: ${uiState.quotedZoneName}",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }

          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = strings.finalTotalLabel,
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
            )
            if (finalTotalForDisplay != null) {
              EtbPriceText(
                amount = finalTotalForDisplay,
                fontSizeSp = 17,
                modifier = Modifier.testTag("fulfillment_final_total_value"),
              )
            } else {
              Text(
                text = "—",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag("fulfillment_final_total_pending"),
              )
            }
          }
        }
      }

      if (selectedMethod == FulfillmentMethod.DELIVERY) {
        if (!hasAddressForQuote) {
          Text(
            text = strings.enterAddressForQuote,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        } else if (!uiState.isLoadingDeliveryQuote && uiState.deliveryQuoteError != null) {
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = StatusDangerBg,
            border = BorderStroke(1.dp, StatusDanger.copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth().testTag("delivery_quote_error_banner"),
          ) {
            Column(
              modifier = Modifier.padding(12.dp),
              verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  imageVector = Icons.Filled.ErrorOutline,
                  contentDescription = null,
                  tint = StatusDanger,
                  modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = uiState.deliveryQuoteError,
                  style = MaterialTheme.typography.bodySmall,
                  color = StatusDanger,
                )
              }
              OutlinedButton(
                onClick = {
                  onRequestDeliveryQuote(
                    city.trim(),
                    subCity.trim(),
                    addressLine.trim(),
                    detectedLat,
                    detectedLng,
                    true,
                  )
                },
                shape = RoundedCornerShape(10.dp),
                modifier =
                  Modifier.minimumInteractiveComponentSize()
                    .testTag("retry_delivery_quote_button"),
              ) {
                Icon(
                  imageVector = Icons.Filled.Refresh,
                  contentDescription = null,
                  modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(strings.retry)
              }
            }
          }
        }
      }

      if (errorMessage != null) {
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = StatusDangerBg,
        ) {
          Text(
            text = errorMessage,
            style = MaterialTheme.typography.bodySmall,
            color = StatusDanger,
            modifier = Modifier.fillMaxWidth().padding(10.dp),
          )
        }
      }

      val canConfirmDelivery =
        hasAddressForQuote &&
          !uiState.isLoadingDeliveryQuote &&
          uiState.quotedDeliveryFee != null &&
          uiState.deliveryQuoteError == null
      val canConfirm =
        !isSubmitting && (selectedMethod == FulfillmentMethod.STORE_PICKUP || canConfirmDelivery)

      Button(
        onClick = {
          val updatedDelivery =
            if (selectedMethod == FulfillmentMethod.DELIVERY) {
              savedDelivery.copy(
                city = city.trim(),
                subCity = subCity.trim(),
                addressLine = addressLine.trim(),
                latitude = detectedLat,
                longitude = detectedLng,
              )
            } else {
              null
            }
          onConfirmChoice(selectedMethod, updatedDelivery, deliveryNote.trim())
        },
        enabled = canConfirm,
        shape = RoundedCornerShape(14.dp),
        contentPadding = PaddingValues(vertical = 14.dp),
        modifier =
          Modifier.fillMaxWidth()
            .minimumInteractiveComponentSize()
            .testTag("confirm_fulfillment_button"),
      ) {
        if (isSubmitting) {
          CircularProgressIndicator(
            modifier = Modifier.size(18.dp),
            strokeWidth = 2.dp,
            color = MaterialTheme.colorScheme.onPrimary,
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(strings.submittingChoice)
        } else {
          Text(strings.confirmFulfillmentChoice)
        }
      }
    }
  }
}

@Composable
private fun ConfirmedFulfillmentCard(
  order: Order,
  storeConfig: StorePickupConfig?,
) {
  val strings = LocalWudeStrings.current
  val isPickup = order.fulfillmentMethod == FulfillmentMethod.STORE_PICKUP
  val appliedDeliveryFee =
    if (isPickup) 0.0 else (order.deliveryFee ?: storeConfig?.deliveryFee)
  GlassCard(shape = RoundedCornerShape(20.dp)) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = if (isPickup) Icons.Filled.Storefront else Icons.Filled.LocalShipping,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text =
            "${strings.fulfillmentConfirmedLabel}: ${
              if (isPickup) strings.fulfillmentPickupTitle else strings.fulfillmentDeliveryTitle
            }",
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold,
        )
      }
      if (isPickup) {
        Text(
          text = strings.noDeliveryFeePickup,
          style = MaterialTheme.typography.bodySmall,
          color = StatusSuccess,
        )
        val addr = storeConfig?.storeAddress.orEmpty()
        if (addr.isNotBlank()) {
          Text("${strings.storeAddressLabel}: $addr", style = MaterialTheme.typography.bodySmall)
        }
        val hours = storeConfig?.openingHours.orEmpty()
        if (hours.isNotBlank()) {
          Text(
            text = "${strings.openingHoursLabel}: $hours",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      } else {
        if (order.deliveryAddress.isNotBlank()) {
          Text(order.deliveryAddress, style = MaterialTheme.typography.bodySmall)
        }
        if (order.deliveryZoneName.isNotBlank()) {
          Text(
            text = "${strings.deliveryZoneLabel}: ${order.deliveryZoneName}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Text(
          text = strings.productSubtotalLabel,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
          text = EtbFormatter.format(order.effectiveProductSubtotal),
          style = MaterialTheme.typography.bodySmall,
          fontWeight = FontWeight.SemiBold,
        )
      }
      if (appliedDeliveryFee != null) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Text(
            text = strings.deliveryFeeLabel,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Text(
            text = EtbFormatter.format(appliedDeliveryFee),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = if (isPickup) StatusSuccess else MaterialTheme.colorScheme.onSurface,
          )
        }
      }
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Text(
          text = strings.finalTotalLabel,
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold,
        )
        EtbPriceText(amount = order.total, fontSizeSp = 15)
      }
    }
  }
}

@Composable
private fun PaymentWaitingApprovalView(
  order: Order,
  lastPaymentId: Long?,
  isRefreshing: Boolean,
  onRefreshStatus: () -> Unit,
  onReturnToOrderTracking: () -> Unit,
  onViewOrderDetails: () -> Unit,
) {
  val strings = LocalWudeStrings.current
  val infiniteTransition = rememberInfiniteTransition(label = "payment_waiting")
  val pulseScale by
    infiniteTransition.animateFloat(
      initialValue = 0.94f,
      targetValue = 1.08f,
      animationSpec =
        infiniteRepeatable(
          animation = tween(1400, easing = FastOutSlowInEasing),
          repeatMode = RepeatMode.Reverse,
        ),
      label = "pulse_scale",
    )
  val ringRotation by
    infiniteTransition.animateFloat(
      initialValue = 0f,
      targetValue = 360f,
      animationSpec =
        infiniteRepeatable(
          animation = tween(4200, easing = LinearEasing),
          repeatMode = RepeatMode.Restart,
        ),
      label = "ring_rotation",
    )

  Box(
    modifier =
      Modifier.fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(20.dp)
        .testTag("payment_waiting_approval_view"),
    contentAlignment = Alignment.Center,
  ) {
    GlassCard(
      shape = RoundedCornerShape(26.dp),
      elevation = 6.dp,
      modifier = Modifier.widthIn(max = 540.dp).fillMaxWidth(),
    ) {
      Column(
        modifier = Modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
      ) {
        Box(
          modifier = Modifier.size(116.dp),
          contentAlignment = Alignment.Center,
        ) {
          Box(
            modifier =
              Modifier.size(112.dp)
                .scale(pulseScale)
                .clip(CircleShape)
                .background(
                  Brush.radialGradient(
                    colors =
                      listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                        Color.Transparent,
                      )
                  )
                )
          )
          Box(
            modifier =
              Modifier.size(92.dp)
                .graphicsLayer { rotationZ = ringRotation }
                .border(
                  width = 3.dp,
                  brush =
                    Brush.sweepGradient(
                      colors =
                        listOf(
                          MaterialTheme.colorScheme.primary,
                          MaterialTheme.colorScheme.secondary,
                          MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        )
                    ),
                  shape = CircleShape,
                )
          )
          Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(72.dp),
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = Icons.Filled.HourglassTop,
                contentDescription = strings.waitingVerification,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(34.dp),
              )
            }
          }
        }

        Surface(
          shape = RoundedCornerShape(50),
          color = StatusInfoBg,
        ) {
          Text(
            text = strings.notYetApprovedBadge,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = StatusInfo,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
          )
        }

        Text(
          text = strings.paymentSubmittedSuccess,
          style = MaterialTheme.typography.headlineSmall,
          fontWeight = FontWeight.Bold,
          textAlign = TextAlign.Center,
          color = MaterialTheme.colorScheme.onSurface,
        )

        Text(
          text = strings.waitingVerification,
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.primary,
          fontWeight = FontWeight.SemiBold,
          textAlign = TextAlign.Center,
        )

        Text(
          text = strings.waitingVerificationExplain,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          textAlign = TextAlign.Center,
        )

        Surface(
          shape = RoundedCornerShape(16.dp),
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Text(
                text = strings.orderReference,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              Text(
                text = "${strings.orderLabel} #${order.id}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
              )
            }
            if (lastPaymentId != null) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                Text(
                  text = strings.proofSubmission,
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                  text = "#$lastPaymentId",
                  style = MaterialTheme.typography.bodySmall,
                  fontWeight = FontWeight.SemiBold,
                )
              }
            }
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Text(
                text = strings.orderAmountEtb,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              EtbPriceText(amount = order.total, fontSizeSp = 16)
            }
          }
        }

        Button(
          onClick = onReturnToOrderTracking,
          shape = RoundedCornerShape(14.dp),
          contentPadding = PaddingValues(vertical = 14.dp),
          modifier =
            Modifier.fillMaxWidth()
              .minimumInteractiveComponentSize()
              .testTag("return_to_order_tracking_button"),
        ) {
          Text(strings.returnToOrderTracking)
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          OutlinedButton(
            onClick = onRefreshStatus,
            enabled = !isRefreshing,
            shape = RoundedCornerShape(14.dp),
            modifier =
              Modifier.weight(1f)
                .minimumInteractiveComponentSize()
                .testTag("waiting_check_status_button"),
          ) {
            if (isRefreshing) {
              CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
            } else {
              Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(strings.checkStatus)
          }

          TextButton(
            onClick = onViewOrderDetails,
            modifier =
              Modifier.weight(1f)
                .minimumInteractiveComponentSize()
                .testTag("view_order_details_from_waiting_button"),
          ) {
            Text(strings.orderAndMessages)
          }
        }
      }
    }
  }
}

@Composable
private fun OrderStatusFeedbackBanner(order: Order, language: AppLanguage) {
  val strings = LocalWudeStrings.current
  val rejectedPayment = if (!order.isPaymentVerified) order.latestRejectedPayment else null

  when {
    order.status == OrderStatus.REJECTED || rejectedPayment != null -> {
      Surface(
        shape = RoundedCornerShape(16.dp),
        color = StatusDangerBg,
        border = BorderStroke(1.dp, StatusDanger.copy(alpha = 0.35f)),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(14.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Icon(Icons.Filled.ErrorOutline, contentDescription = null, tint = StatusDanger)
          Spacer(modifier = Modifier.width(10.dp))
          Column {
            Text(
              text = strings.paymentNotApprovedBanner,
              style = MaterialTheme.typography.titleSmall,
              color = StatusDanger,
              fontWeight = FontWeight.Bold,
            )
            Text(
              text =
                rejectedPayment?.adminNote?.ifBlank { null }
                  ?: order.messages.firstOrNull { it.tone == MessageTone.REJECTED }?.body
                  ?: strings.paymentNotApprovedBanner,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurface,
            )
          }
        }
      }
    }
    order.isPaymentVerified -> {
      Surface(
        shape = RoundedCornerShape(16.dp),
        color = StatusSuccessBg,
        border = BorderStroke(1.dp, StatusSuccess.copy(alpha = 0.35f)),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(14.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = StatusSuccess)
          Spacer(modifier = Modifier.width(10.dp))
          Column {
            Text(
              text = "${strings.paymentApprovedBanner} • ${order.status.labelFor(language)}",
              style = MaterialTheme.typography.titleSmall,
              color = StatusSuccess,
              fontWeight = FontWeight.Bold,
            )
            Text(
              text = strings.paymentApprovedSub,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurface,
            )
          }
        }
      }
    }
    order.status == OrderStatus.PAYMENT_REVIEW -> {
      Surface(
        shape = RoundedCornerShape(16.dp),
        color = StatusInfoBg,
        border = BorderStroke(1.dp, StatusInfo.copy(alpha = 0.35f)),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(14.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Icon(Icons.Filled.HourglassTop, contentDescription = null, tint = StatusInfo)
          Spacer(modifier = Modifier.width(10.dp))
          Column {
            Text(
              text = strings.waitingVerification,
              style = MaterialTheme.typography.titleSmall,
              color = StatusInfo,
              fontWeight = FontWeight.Bold,
            )
            Text(
              text = strings.notYetApprovedBadge,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurface,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun OrderLiveTrackingCard(
  order: Order,
  language: AppLanguage,
  onTrackDelivery: () -> Unit,
) {
  val strings = LocalWudeStrings.current
  GlassCard(shape = RoundedCornerShape(20.dp)) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column {
          Text(
            strings.liveStatus,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Text(
            text = "${strings.orderLabel} #${order.id}",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
          )
        }
        val effectiveStatus =
          if (!order.isPaymentVerified && order.latestRejectedPayment != null && order.status == OrderStatus.PENDING_PAYMENT) {
            OrderStatus.REJECTED
          } else {
            order.status
          }
        OrderStatusPill(status = effectiveStatus, language = language)
      }

      OrderStatusStepper(status = order.status, language = language)

      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(strings.orderAmountEtb, style = MaterialTheme.typography.bodyMedium)
        EtbPriceText(amount = order.total, fontSizeSp = 20)
      }

      if (order.isDispatchedOrTrackable) {
        Button(
          onClick = onTrackDelivery,
          shape = RoundedCornerShape(14.dp),
          contentPadding = PaddingValues(vertical = 12.dp),
          modifier =
            Modifier.fillMaxWidth()
              .minimumInteractiveComponentSize()
              .testTag("track_delivery_button"),
        ) {
          Icon(Icons.Filled.LocalShipping, contentDescription = null)
          Spacer(modifier = Modifier.width(8.dp))
          Text(strings.trackDeliveryAction)
        }
      }
    }
  }
}

@Composable
private fun TransactionMessagesCard(messages: List<TransactionMessage>) {
  val strings = LocalWudeStrings.current
  GlassCard(
    shape = RoundedCornerShape(20.dp),
    modifier = Modifier.testTag("transaction_messages_card"),
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Filled.Forum,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = strings.transactionMessagesTitle,
            style = MaterialTheme.typography.titleMedium,
          )
        }
        if (messages.isNotEmpty()) {
          Surface(
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.primaryContainer,
          ) {
            Text(
              text = "${messages.size}",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onPrimaryContainer,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            )
          }
        }
      }

      if (messages.isEmpty()) {
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(
              imageVector = Icons.Filled.MarkEmailRead,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
              text = strings.noMessagesYet,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      } else {
        messages.forEach { msg ->
          val (bg, fg) =
            when (msg.tone) {
              MessageTone.APPROVED -> StatusSuccessBg to StatusSuccess
              MessageTone.REJECTED -> StatusDangerBg to StatusDanger
              MessageTone.PENDING -> StatusInfoBg to StatusInfo
              MessageTone.INFO ->
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f) to
                  MaterialTheme.colorScheme.primary
            }
          Surface(
            shape = RoundedCornerShape(14.dp),
            color = bg.copy(alpha = 0.85f),
            border = BorderStroke(1.dp, fg.copy(alpha = 0.25f)),
            modifier = Modifier.fillMaxWidth().testTag("tx_message_${msg.id}"),
          ) {
            Column(
              modifier = Modifier.padding(12.dp),
              verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Text(
                  text = msg.senderLabel,
                  style = MaterialTheme.typography.labelMedium,
                  fontWeight = FontWeight.Bold,
                  color = fg,
                )
                if (msg.timestamp.isNotBlank()) {
                  Text(
                    text = msg.timestamp.take(16).replace("T", " "),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }
              Text(
                text = msg.title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
              )
              Text(
                text = msg.body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun BackendPaymentMethodsCard(
  order: Order,
  instructions: List<PaymentInstruction>,
  methodsError: String?,
  selectedMethodId: String?,
  isRefreshing: Boolean,
  onSelectMethod: (String) -> Unit,
  onRefresh: () -> Unit,
  onCopied: (String) -> Unit,
) {
  val strings = LocalWudeStrings.current
  val clipboard = LocalClipboardManager.current
  val activeMethod =
    instructions.firstOrNull { it.id == selectedMethodId } ?: instructions.firstOrNull()

  GlassCard(shape = RoundedCornerShape(20.dp)) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Filled.AccountBalance,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = strings.paymentMethodsTitle,
            style = MaterialTheme.typography.titleMedium,
          )
        }
        Text(
          text = EtbFormatter.format(order.total),
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.primary,
          fontWeight = FontWeight.Bold,
        )
      }

      if (instructions.isNotEmpty()) {
        Text(
          text = strings.selectPaymentMethodHint,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          items(instructions, key = { it.id }) { method ->
            val selected = method.id == activeMethod?.id
            Surface(
              shape = RoundedCornerShape(14.dp),
              color =
                if (selected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
              border =
                BorderStroke(
                  width = if (selected) 1.5.dp else 1.dp,
                  color =
                    if (selected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outlineVariant,
                ),
              modifier =
                Modifier.clip(RoundedCornerShape(14.dp))
                  .minimumInteractiveComponentSize()
                  .clickable { onSelectMethod(method.id) }
                  .testTag("payment_method_chip_${method.id}"),
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Icon(
                  imageVector =
                    if (method.isTelebirr) Icons.Filled.PhoneAndroid
                    else Icons.Filled.AccountBalanceWallet,
                  contentDescription = null,
                  tint =
                    if (selected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = method.providerName,
                  style = MaterialTheme.typography.labelLarge,
                  fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                  color =
                    if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurface,
                )
              }
            }
          }
        }

        if (activeMethod != null) {
          Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth().testTag("selected_payment_method_details"),
          ) {
            Column(
              modifier = Modifier.fillMaxWidth().padding(14.dp),
              verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Text(
                  text = activeMethod.providerName,
                  style = MaterialTheme.typography.titleMedium,
                  color = MaterialTheme.colorScheme.primary,
                  fontWeight = FontWeight.Bold,
                )
              }

              if (activeMethod.accountName.isNotBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                  )
                  Spacer(modifier = Modifier.width(8.dp))
                  Column {
                    Text(
                      text = strings.accountHolder,
                      style = MaterialTheme.typography.labelSmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                      text = activeMethod.accountName,
                      style = MaterialTheme.typography.bodyMedium,
                      fontWeight = FontWeight.SemiBold,
                    )
                  }
                }
              }

              if (activeMethod.accountNumber.isNotBlank()) {
                Surface(
                  shape = RoundedCornerShape(12.dp),
                  color = MaterialTheme.colorScheme.surface,
                  border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                  Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    Column(modifier = Modifier.weight(1f)) {
                      Text(
                        text =
                          if (activeMethod.isTelebirr) strings.telebirrNumber
                          else strings.accountWalletNumber,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                      )
                      Text(
                        text = activeMethod.accountNumber,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                      )
                    }
                    IconButton(
                      onClick = {
                        clipboard.setText(AnnotatedString(activeMethod.accountNumber))
                        onCopied("${strings.copiedPrefix} ${activeMethod.accountNumber}")
                      },
                      modifier =
                        Modifier.minimumInteractiveComponentSize()
                          .testTag("copy_payment_account_button"),
                    ) {
                      Icon(
                        imageVector = Icons.Filled.ContentCopy,
                        contentDescription = strings.copy,
                        tint = MaterialTheme.colorScheme.primary,
                      )
                    }
                  }
                }
              }

              if (activeMethod.instructions.isNotBlank()) {
                Surface(
                  shape = RoundedCornerShape(12.dp),
                  color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                ) {
                  Column(modifier = Modifier.fillMaxWidth().padding(10.dp)) {
                    Text(
                      text = strings.adminInstructions,
                      style = MaterialTheme.typography.labelMedium,
                      color = MaterialTheme.colorScheme.primary,
                      fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                      text = activeMethod.instructions,
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurface,
                    )
                  }
                }
              }
            }
          }
        }
      } else {
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = if (methodsError != null) StatusDangerBg else StatusInfoBg.copy(alpha = 0.7f),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.Top,
          ) {
            Icon(
              imageVector = if (methodsError != null) Icons.Filled.ErrorOutline else Icons.Outlined.Info,
              contentDescription = null,
              tint = if (methodsError != null) StatusDanger else StatusInfo,
              modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = strings.paymentMethodsTitle,
                style = MaterialTheme.typography.titleSmall,
                color = if (methodsError != null) StatusDanger else StatusInfo,
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = methodsError ?: strings.noPaymentMethodsPublished,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
              )
              Spacer(modifier = Modifier.height(8.dp))
              OutlinedButton(
                onClick = onRefresh,
                enabled = !isRefreshing,
                modifier =
                  Modifier.minimumInteractiveComponentSize()
                    .testTag("refresh_payment_instructions"),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
              ) {
                Text(strings.refreshPaymentMethods)
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun PaymentUploadCard(
  order: Order,
  selectedUri: Uri?,
  selectedMethod: PaymentInstruction?,
  isUploading: Boolean,
  hasUploadError: Boolean,
  imageLoader: ImageLoader,
  onPickImage: () -> Unit,
  onSubmit: () -> Unit,
) {
  val strings = LocalWudeStrings.current
  GlassCard(shape = RoundedCornerShape(20.dp)) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = Icons.Filled.VerifiedUser,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text =
            if (order.payments.isEmpty()) strings.uploadScreenshotTitle
            else strings.uploadNewScreenshotTitle,
          style = MaterialTheme.typography.titleMedium,
        )
      }

      if (selectedMethod != null) {
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        ) {
          Text(
            text = "${selectedMethod.providerName} • ${selectedMethod.accountNumber}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
          )
        }
      }

      if (selectedUri != null) {
        AsyncImage(
          model = selectedUri,
          imageLoader = imageLoader,
          contentDescription = strings.chooseScreenshot,
          contentScale = ContentScale.Crop,
          modifier =
            Modifier.fillMaxWidth()
              .height(210.dp)
              .clip(RoundedCornerShape(16.dp))
              .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp)),
        )
      }

      OutlinedButton(
        onClick = onPickImage,
        enabled = !isUploading,
        shape = RoundedCornerShape(12.dp),
        modifier =
          Modifier.fillMaxWidth()
            .minimumInteractiveComponentSize()
            .testTag("pick_screenshot_button"),
      ) {
        Icon(Icons.Filled.Image, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text(if (selectedUri == null) strings.chooseScreenshot else strings.changeScreenshot)
      }

      Button(
        onClick = onSubmit,
        enabled = selectedUri != null && !isUploading,
        shape = RoundedCornerShape(14.dp),
        contentPadding = PaddingValues(vertical = 14.dp),
        modifier =
          Modifier.fillMaxWidth()
            .minimumInteractiveComponentSize()
            .testTag("submit_payment_proof_button"),
      ) {
        if (isUploading) {
          CircularProgressIndicator(
            modifier = Modifier.size(18.dp),
            strokeWidth = 2.dp,
            color = MaterialTheme.colorScheme.onPrimary,
          )
          Spacer(modifier = Modifier.width(10.dp))
          Text(strings.uploadingScreenshot)
        } else {
          Icon(Icons.Filled.UploadFile, contentDescription = null)
          Spacer(modifier = Modifier.width(8.dp))
          Text(if (hasUploadError) strings.retryUpload else strings.submitPaymentProof)
        }
      }
    }
  }
}

@Composable
private fun SubmittedPaymentsCard(payments: List<PaymentRecord>) {
  val strings = LocalWudeStrings.current
  GlassCard(shape = RoundedCornerShape(20.dp)) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Text(
        text = strings.paymentSubmissions,
        style = MaterialTheme.typography.titleMedium,
      )
      payments.forEach { payment ->
        val statusLower = payment.status.lowercase()
        val (bg, fg) =
          when (statusLower) {
            "approved", "verified", "paid" -> StatusSuccessBg to StatusSuccess
            "rejected", "failed" -> StatusDangerBg to StatusDanger
            else -> StatusWarningBg to StatusWarning
          }
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ) {
          Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text = "#${payment.id}",
                style = MaterialTheme.typography.titleSmall,
              )
              Surface(shape = RoundedCornerShape(50), color = bg) {
                Text(
                  text = payment.status.replaceFirstChar { it.uppercase() },
                  color = fg,
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.SemiBold,
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                )
              }
            }
            if (payment.paymentMethod.isNotBlank()) {
              Text(
                text = payment.paymentMethod,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
              )
            }
            if (payment.proofFilename.isNotBlank()) {
              Text(
                text = payment.proofFilename,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            if (payment.adminNote.isNotBlank()) {
              Text(
                text = payment.adminNote,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
              )
            }
            if (payment.createdAt.isNotBlank()) {
              Text(
                text = payment.createdAt.take(19).replace("T", " "),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun OrderItemsBreakdownCard(
  order: Order,
  imageLoader: ImageLoader,
) {
  val strings = LocalWudeStrings.current
  GlassCard(shape = RoundedCornerShape(20.dp)) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Text(
        text = strings.itemsInOrder,
        style = MaterialTheme.typography.titleMedium,
      )
      order.items.forEach { item ->
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          ProductThumbnail(
            imageUrl = item.imageUrl,
            productName = item.productName,
            imageLoader = imageLoader,
            modifier = Modifier.size(54.dp).clip(RoundedCornerShape(10.dp)),
          )
          Spacer(modifier = Modifier.width(12.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = item.productName,
              style = MaterialTheme.typography.titleSmall,
            )
            Text(
              text = "${item.quantity} × ${EtbFormatter.format(item.unitPrice)}",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          Text(
            text = EtbFormatter.format(item.lineTotal),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
          )
        }
      }
      if (order.customerNote.isNotBlank()) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Text(
          text = strings.deliveryCustomerNote,
          style = MaterialTheme.typography.labelLarge,
        )
        Text(
          text = order.customerNote,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}
