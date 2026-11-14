package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.data.model.ADDIS_SUB_CITIES
import com.example.data.model.CartItem
import com.example.data.model.DeliveryProfileStorage
import com.example.data.model.ETHIOPIAN_CITIES
import com.example.data.model.EtbFormatter
import com.example.ui.components.EtbPriceText
import com.example.ui.components.GlassCard
import com.example.ui.i18n.LocalWudeStrings
import com.example.ui.theme.StatusDanger
import com.example.ui.theme.StatusDangerBg
import com.example.ui.theme.StatusInfo
import com.example.ui.theme.StatusInfoBg
import com.example.ui.viewmodel.CheckoutUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreen(
  cartItems: List<CartItem>,
  savedDelivery: DeliveryProfileStorage,
  checkoutState: CheckoutUiState,
  onBack: () -> Unit,
  onClearError: () -> Unit,
  onClearLocationStatus: () -> Unit,
  onDetectLocation: (onResolved: (String, String, String, Double, Double) -> Unit) -> Unit,
  onLocationDenied: () -> Unit,
  onPlaceOrder: (
    fullName: String,
    phone: String,
    city: String,
    subCity: String,
    addressLine: String,
    extraNote: String,
    latitude: Double?,
    longitude: Double?,
  ) -> Unit,
) {
  val strings = LocalWudeStrings.current
  val context = LocalContext.current

  var fullName by rememberSaveable(savedDelivery.fullName) { mutableStateOf(savedDelivery.fullName) }
  var phone by rememberSaveable(savedDelivery.phone) { mutableStateOf(savedDelivery.phone) }
  var city by rememberSaveable(savedDelivery.city) {
    mutableStateOf(savedDelivery.city.ifBlank { "Addis Ababa" })
  }
  var subCity by rememberSaveable(savedDelivery.subCity) {
    mutableStateOf(savedDelivery.subCity.ifBlank { "Bole" })
  }
  var addressLine by rememberSaveable(savedDelivery.addressLine) {
    mutableStateOf(savedDelivery.addressLine)
  }
  var extraNote by rememberSaveable { mutableStateOf("") }
  var detectedLat by rememberSaveable(savedDelivery.latitude) { mutableStateOf(savedDelivery.latitude) }
  var detectedLng by rememberSaveable(savedDelivery.longitude) { mutableStateOf(savedDelivery.longitude) }
  var showLocationRationale by remember { mutableStateOf(false) }

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
          modifier = Modifier.testTag("location_rationale_allow"),
        ) {
          Text(strings.allowLocation)
        }
      },
      dismissButton = {
        TextButton(
          onClick = { showLocationRationale = false },
          modifier = Modifier.testTag("location_rationale_manual"),
        ) {
          Text(strings.enterManually)
        }
      },
    )
  }

  val totalAmount = cartItems.sumOf { it.lineTotal }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text(strings.ethiopianCheckout) },
        navigationIcon = {
          IconButton(
            onClick = onBack,
            modifier = Modifier.minimumInteractiveComponentSize().testTag("checkout_back_button"),
          ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.back)
          }
        },
        colors =
          TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
          ),
      )
    },
    bottomBar = {
      Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        tonalElevation = 4.dp,
        shadowElevation = 8.dp,
      ) {
        Column(
          modifier =
            Modifier.fillMaxWidth()
              .navigationBarsPadding()
              .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(strings.orderTotal, style = MaterialTheme.typography.titleMedium)
            EtbPriceText(
              amount = totalAmount,
              fontSizeSp = 22,
            )
          }
          Spacer(modifier = Modifier.height(10.dp))
          Button(
            onClick = {
              onPlaceOrder(
                fullName,
                phone,
                city,
                subCity,
                addressLine,
                extraNote,
                detectedLat,
                detectedLng,
              )
            },
            enabled = !checkoutState.isPlacingOrder && cartItems.isNotEmpty(),
            shape = RoundedCornerShape(14.dp),
            contentPadding = PaddingValues(vertical = 14.dp),
            modifier =
              Modifier.fillMaxWidth()
                .minimumInteractiveComponentSize()
                .testTag("place_order_button"),
          ) {
            if (checkoutState.isPlacingOrder) {
              CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary,
              )
              Spacer(modifier = Modifier.width(10.dp))
              Text(strings.placingOrder)
            } else {
              Text(
                text = "${strings.placeOrder} • ${EtbFormatter.format(totalAmount)}",
                style = MaterialTheme.typography.titleSmall,
              )
            }
          }
        }
      }
    },
  ) { innerPadding ->
    Box(
      modifier =
        Modifier.fillMaxSize()
          .background(MaterialTheme.colorScheme.background)
          .padding(innerPadding),
      contentAlignment = Alignment.TopCenter,
    ) {
      Column(
        modifier =
          Modifier.widthIn(max = 680.dp)
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
      ) {
        if (checkoutState.errorMessage != null) {
          Surface(
            shape = RoundedCornerShape(14.dp),
            color = StatusDangerBg,
          ) {
            Text(
              text = checkoutState.errorMessage,
              style = MaterialTheme.typography.bodyMedium,
              color = StatusDanger,
              modifier = Modifier.fillMaxWidth().padding(14.dp),
            )
          }
        }

        // Delivery Contact & Ethiopian Location Card
        GlassCard(shape = RoundedCornerShape(20.dp)) {
          Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Text(
              text = strings.deliveryDetailsEthiopia,
              style = MaterialTheme.typography.titleMedium,
            )

            OutlinedButton(
              onClick = {
                onClearLocationStatus()
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
              enabled = !checkoutState.isDetectingLocation,
              shape = RoundedCornerShape(12.dp),
              modifier =
                Modifier.fillMaxWidth()
                  .minimumInteractiveComponentSize()
                  .testTag("checkout_use_location_button"),
            ) {
              if (checkoutState.isDetectingLocation) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(strings.detectingLocation)
              } else {
                Icon(Icons.Filled.MyLocation, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(strings.useCurrentLocation)
              }
            }

            if (checkoutState.locationStatusMessage != null) {
              Surface(
                shape = RoundedCornerShape(12.dp),
                color = StatusInfoBg,
              ) {
                Text(
                  text = checkoutState.locationStatusMessage,
                  style = MaterialTheme.typography.bodySmall,
                  color = StatusInfo,
                  modifier = Modifier.fillMaxWidth().padding(10.dp),
                )
              }
            }

            OutlinedTextField(
              value = fullName,
              onValueChange = {
                fullName = it
                onClearError()
              },
              label = { Text(strings.recipientFullName) },
              placeholder = { Text("Abebe Bikila") },
              leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
              singleLine = true,
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier.fillMaxWidth().testTag("checkout_name_input"),
            )

            OutlinedTextField(
              value = phone,
              onValueChange = {
                phone = it
                onClearError()
              },
              label = { Text(strings.ethiopianPhone) },
              placeholder = { Text("+251 911 223344 / 0911223344") },
              leadingIcon = { Icon(Icons.Filled.Phone, contentDescription = null) },
              supportingText = { Text(strings.phoneHelper) },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
              singleLine = true,
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier.fillMaxWidth().testTag("checkout_phone_input"),
            )

            Text(
              text = strings.cityRegion,
              style = MaterialTheme.typography.labelLarge,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              items(ETHIOPIAN_CITIES) { c ->
                FilterChip(
                  selected = city == c,
                  onClick = {
                    city = c
                    if (c != "Addis Ababa") subCity = ""
                  },
                  label = { Text(c) },
                  modifier = Modifier.testTag("city_chip_${c.lowercase().replace(" ", "_")}"),
                )
              }
            }

            if (city == "Addis Ababa") {
              Text(
                text = strings.addisSubCity,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(ADDIS_SUB_CITIES) { sc ->
                  FilterChip(
                    selected = subCity == sc,
                    onClick = { subCity = sc },
                    label = { Text(sc) },
                    modifier = Modifier.testTag("subcity_chip_${sc.lowercase().replace(" ", "_")}"),
                  )
                }
              }
            }

            OutlinedTextField(
              value = addressLine,
              onValueChange = {
                addressLine = it
                onClearError()
              },
              label = { Text(strings.streetLandmark) },
              placeholder = { Text("Bole Medhanialem, near Edna Mall") },
              leadingIcon = { Icon(Icons.Filled.LocationOn, contentDescription = null) },
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier.fillMaxWidth().testTag("checkout_address_input"),
            )

            OutlinedTextField(
              value = extraNote,
              onValueChange = { extraNote = it },
              label = { Text(strings.deliveryNoteOptional) },
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier.fillMaxWidth().testTag("checkout_note_input"),
            )
          }
        }

        // Order Summary Card
        GlassCard(shape = RoundedCornerShape(20.dp)) {
          Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Text(
              text = "${strings.orderSummary} (${cartItems.sumOf { it.quantity }} ${strings.itemsSuffix})",
              style = MaterialTheme.typography.titleMedium,
            )
            cartItems.forEach { item ->
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                Text(
                  text = "${item.quantity}× ${item.product.name}",
                  style = MaterialTheme.typography.bodyMedium,
                  modifier = Modifier.weight(1f),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = EtbFormatter.format(item.lineTotal),
                  style = MaterialTheme.typography.bodyMedium,
                  fontWeight = FontWeight.SemiBold,
                )
              }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Text(
                text = strings.totalDueEtb,
                style = MaterialTheme.typography.titleSmall,
              )
              Text(
                text = EtbFormatter.format(totalAmount),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
              )
            }
            Text(
              text = strings.checkoutPaymentHint,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }
    }
  }
}
