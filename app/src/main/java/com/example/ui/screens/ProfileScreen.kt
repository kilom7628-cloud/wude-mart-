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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.data.model.CustomerSession
import com.example.data.model.DeliveryProfileStorage
import com.example.ui.components.GlassCard
import com.example.ui.components.pressBounce
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.LocalWudeStrings
import com.example.ui.theme.StatusDanger
import com.example.ui.theme.StatusInfo
import com.example.ui.theme.StatusInfoBg
import com.example.ui.theme.ThemePreference

@Composable
fun ProfileScreen(
  session: CustomerSession?,
  deliveryProfile: DeliveryProfileStorage,
  themePreference: ThemePreference,
  language: AppLanguage,
  wishlistCount: Int,
  isDetectingLocation: Boolean,
  locationStatusMessage: String?,
  onSignInClick: () -> Unit,
  onRefreshProfile: () -> Unit,
  onSignOut: () -> Unit,
  onSaveDelivery: (DeliveryProfileStorage) -> Unit,
  onThemeChange: (ThemePreference) -> Unit,
  onLanguageChange: (AppLanguage) -> Unit,
  onOpenWishlist: () -> Unit,
  onDetectLocation: (onResolved: (String, String, String, Double, Double) -> Unit) -> Unit,
  onLocationDenied: () -> Unit,
  onClearLocationStatus: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val strings = LocalWudeStrings.current
  val context = LocalContext.current

  var fullName by rememberSaveable(deliveryProfile.fullName) { mutableStateOf(deliveryProfile.fullName) }
  var phone by rememberSaveable(deliveryProfile.phone) { mutableStateOf(deliveryProfile.phone) }
  var city by rememberSaveable(deliveryProfile.city) {
    mutableStateOf(deliveryProfile.city.ifBlank { "Addis Ababa" })
  }
  var subCity by rememberSaveable(deliveryProfile.subCity) {
    mutableStateOf(deliveryProfile.subCity.ifBlank { "Bole" })
  }
  var addressLine by rememberSaveable(deliveryProfile.addressLine) {
    mutableStateOf(deliveryProfile.addressLine)
  }
  var detectedLat by rememberSaveable(deliveryProfile.latitude) { mutableStateOf(deliveryProfile.latitude) }
  var detectedLng by rememberSaveable(deliveryProfile.longitude) { mutableStateOf(deliveryProfile.longitude) }
  var showSignOutConfirm by remember { mutableStateOf(false) }
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

  if (showSignOutConfirm) {
    AlertDialog(
      onDismissRequest = { showSignOutConfirm = false },
      title = { Text(strings.signOutConfirmTitle) },
      text = { Text(strings.signOutConfirmBody) },
      confirmButton = {
        Button(
          onClick = {
            showSignOutConfirm = false
            onSignOut()
          },
          colors = ButtonDefaults.buttonColors(containerColor = StatusDanger),
          modifier = Modifier.testTag("confirm_sign_out_button"),
        ) {
          Text(strings.signOut)
        }
      },
      dismissButton = {
        TextButton(
          onClick = { showSignOutConfirm = false },
          modifier = Modifier.testTag("cancel_sign_out_button"),
        ) {
          Text(strings.staySignedIn)
        }
      },
    )
  }

  Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
    Surface(
      color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
      tonalElevation = 1.dp,
    ) {
      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column {
          Text(
            text = strings.accountAndPreferences,
            style = MaterialTheme.typography.headlineSmall,
          )
          Text(
            text = strings.profileSubtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        if (session?.isSignedIn == true) {
          IconButton(
            onClick = onRefreshProfile,
            modifier = Modifier.minimumInteractiveComponentSize().testTag("profile_refresh_button"),
          ) {
            Icon(Icons.Filled.Refresh, contentDescription = strings.refresh)
          }
        }
      }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
      Column(
        modifier =
          Modifier.widthIn(max = 680.dp)
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
      ) {
        // Customer Account Card
        GlassCard(shape = RoundedCornerShape(20.dp)) {
          Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(52.dp),
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Icon(
                    imageVector = Icons.Filled.AccountCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp),
                  )
                }
              }
              Spacer(modifier = Modifier.width(12.dp))
              Column(modifier = Modifier.weight(1f)) {
                if (session?.isSignedIn == true) {
                  Text(
                    text = session.email,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                  )
                  Text(
                    text =
                      buildString {
                        append(strings.navAccount)
                        if (session.userId != null) append(" • #${session.userId}")
                      },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                  if (!session.createdAt.isNullOrBlank()) {
                    Text(
                      text = session.createdAt.take(10),
                      style = MaterialTheme.typography.labelSmall,
                      color = MaterialTheme.colorScheme.primary,
                    )
                  }
                } else {
                  Text(
                    text = strings.shoppingAsGuest,
                    style = MaterialTheme.typography.titleMedium,
                  )
                  Text(
                    text = strings.guestSubtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }
            }

            if (session?.isSignedIn == true) {
              OutlinedButton(
                onClick = { showSignOutConfirm = true },
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, StatusDanger.copy(alpha = 0.5f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusDanger),
                modifier =
                  Modifier.fillMaxWidth()
                    .minimumInteractiveComponentSize()
                    .testTag("profile_sign_out_button"),
              ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(strings.signOut)
              }
            } else {
              Button(
                onClick = onSignInClick,
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(vertical = 12.dp),
                modifier =
                  Modifier.fillMaxWidth()
                    .minimumInteractiveComponentSize()
                    .testTag("profile_sign_in_button"),
              ) {
                Text(strings.signInCreateAccount)
              }
            }
          }
        }

        // Wishlist Shortcut Card
        GlassCard(
          shape = RoundedCornerShape(20.dp),
          modifier =
            Modifier.fillMaxWidth()
              .clip(RoundedCornerShape(20.dp))
              .pressBounce(onClick = onOpenWishlist)
              .testTag("profile_wishlist_button"),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
              Icon(
                imageVector = Icons.Filled.Favorite,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
              )
              Spacer(modifier = Modifier.width(12.dp))
              Column {
                Text(strings.wishlistTitle, style = MaterialTheme.typography.titleMedium)
                Text(
                  text = strings.wishlistSubtitle,
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
              if (wishlistCount > 0) {
                Badge { Text("$wishlistCount") }
                Spacer(modifier = Modifier.width(8.dp))
              }
              Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
              )
            }
          }
        }

        // Language Selector (English / አማርኛ)
        GlassCard(shape = RoundedCornerShape(20.dp)) {
          Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Filled.Language,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(strings.languageTitle, style = MaterialTheme.typography.titleMedium)
            }

            Text(
              text = strings.languageSubtitle,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
              AppLanguage.entries.forEachIndexed { index, lang ->
                SegmentedButton(
                  selected = language == lang,
                  onClick = { onLanguageChange(lang) },
                  shape = SegmentedButtonDefaults.itemShape(index, AppLanguage.entries.size),
                  modifier = Modifier.testTag("language_${lang.code}"),
                ) {
                  Text(lang.displayName)
                }
              }
            }
          }
        }

        // Saved Ethiopian Delivery Address
        GlassCard(shape = RoundedCornerShape(20.dp)) {
          Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Filled.LocalShipping,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(strings.savedDeliveryInfo, style = MaterialTheme.typography.titleMedium)
            }

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
              enabled = !isDetectingLocation,
              shape = RoundedCornerShape(12.dp),
              modifier =
                Modifier.fillMaxWidth()
                  .minimumInteractiveComponentSize()
                  .testTag("profile_use_location_button"),
            ) {
              if (isDetectingLocation) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(strings.detectingLocation)
              } else {
                Icon(Icons.Filled.MyLocation, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(strings.useCurrentLocation)
              }
            }

            if (locationStatusMessage != null) {
              Surface(
                shape = RoundedCornerShape(12.dp),
                color = StatusInfoBg,
              ) {
                Text(
                  text = locationStatusMessage,
                  style = MaterialTheme.typography.bodySmall,
                  color = StatusInfo,
                  modifier = Modifier.fillMaxWidth().padding(10.dp),
                )
              }
            }

            OutlinedTextField(
              value = fullName,
              onValueChange = { fullName = it },
              label = { Text(strings.recipientFullName) },
              singleLine = true,
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.fillMaxWidth().testTag("profile_delivery_name"),
            )

            OutlinedTextField(
              value = phone,
              onValueChange = { phone = it },
              label = { Text(strings.ethiopianPhone) },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
              singleLine = true,
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.fillMaxWidth().testTag("profile_delivery_phone"),
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
              OutlinedTextField(
                value = city,
                onValueChange = { city = it },
                label = { Text(strings.cityRegion) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f),
              )
              OutlinedTextField(
                value = subCity,
                onValueChange = { subCity = it },
                label = { Text(strings.addisSubCity) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f),
              )
            }

            OutlinedTextField(
              value = addressLine,
              onValueChange = { addressLine = it },
              label = { Text(strings.streetLandmark) },
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.fillMaxWidth().testTag("profile_delivery_address"),
            )

            Button(
              onClick = {
                onSaveDelivery(
                  DeliveryProfileStorage(
                    fullName = fullName.trim(),
                    phone = phone.trim(),
                    city = city.trim(),
                    subCity = subCity.trim(),
                    addressLine = addressLine.trim(),
                    latitude = detectedLat,
                    longitude = detectedLng,
                  )
                )
              },
              shape = RoundedCornerShape(12.dp),
              modifier =
                Modifier.fillMaxWidth()
                  .minimumInteractiveComponentSize()
                  .testTag("profile_save_delivery_button"),
            ) {
              Text(strings.saveDeliveryDetails)
            }
          }
        }

        // Appearance Settings (consistent ETB ብር format enforced app-wide)
        GlassCard(shape = RoundedCornerShape(20.dp)) {
          Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Filled.BrightnessMedium,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(strings.appearance, style = MaterialTheme.typography.titleMedium)
            }

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
              ThemePreference.entries.forEachIndexed { index, pref ->
                val label =
                  when (pref) {
                    ThemePreference.SYSTEM -> strings.themeSystem
                    ThemePreference.LIGHT -> strings.themeLight
                    ThemePreference.DARK -> strings.themeDark
                  }
                SegmentedButton(
                  selected = themePreference == pref,
                  onClick = { onThemeChange(pref) },
                  shape = SegmentedButtonDefaults.itemShape(index, ThemePreference.entries.size),
                  modifier = Modifier.testTag("theme_${pref.name.lowercase()}"),
                ) {
                  Text(label)
                }
              }
            }
          }
        }
      }
    }
  }
}
