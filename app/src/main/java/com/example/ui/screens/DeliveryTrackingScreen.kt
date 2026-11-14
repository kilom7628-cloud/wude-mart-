package com.example.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.DeliveryDining
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import com.example.data.model.FulfillmentMethod
import com.example.ui.components.ErrorStateView
import com.example.ui.components.ExpectedScheduleCard
import com.example.ui.components.GlassCard
import com.example.ui.components.InteractiveDeliveryMap
import com.example.ui.components.LoadingStateView
import com.example.ui.i18n.LocalWudeStrings
import com.example.ui.theme.StatusInfo
import com.example.ui.theme.StatusInfoBg
import com.example.ui.theme.StatusWarning
import com.example.ui.theme.StatusWarningBg
import com.example.ui.viewmodel.DeliveryTrackingUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeliveryTrackingScreen(
  orderId: Long,
  uiState: DeliveryTrackingUiState,
  imageLoader: ImageLoader,
  onBack: () -> Unit,
  onRefresh: () -> Unit,
) {
  val strings = LocalWudeStrings.current
  val context = LocalContext.current

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("${strings.deliveryTrackingTitle} • #$orderId") },
        navigationIcon = {
          IconButton(
            onClick = onBack,
            modifier = Modifier.minimumInteractiveComponentSize().testTag("tracking_back_button"),
          ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.back)
          }
        },
        actions = {
          IconButton(
            onClick = onRefresh,
            enabled = !uiState.isRefreshing,
            modifier = Modifier.minimumInteractiveComponentSize().testTag("tracking_refresh_button"),
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
          message = "${strings.deliveryTrackingTitle}…",
          modifier = Modifier.padding(innerPadding),
        )
      }
      uiState.error != null && uiState.order == null && uiState.tracking == null -> {
        ErrorStateView(
          message = uiState.error,
          onRetry = onRefresh,
          modifier = Modifier.padding(innerPadding),
        )
      }
      else -> {
        val order = uiState.order
        val tracking = uiState.tracking ?: order?.deliveryTracking
        val isPickup = order?.fulfillmentMethod == FulfillmentMethod.STORE_PICKUP

        Box(
          modifier =
            Modifier.fillMaxSize()
              .background(MaterialTheme.colorScheme.background)
              .padding(innerPadding),
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
            ExpectedScheduleCard(
              isPickup = isPickup,
              expectedDate =
                if (isPickup) order?.expectedPickupDate
                else tracking?.expectedDeliveryDate ?: order?.expectedDeliveryDate,
              expectedWindow =
                if (isPickup) order?.expectedPickupWindow
                else tracking?.expectedDeliveryWindow ?: order?.expectedDeliveryWindow,
            )

            InteractiveDeliveryMap(
              courierLocation = tracking?.currentLocation,
              destinationLocation = tracking?.destinationLocation,
              route = tracking?.route.orEmpty(),
              imageLoader = imageLoader,
            )

            if (tracking == null || !tracking.sharingEnabled || tracking.currentLocation == null) {
              Surface(
                shape = RoundedCornerShape(16.dp),
                color = StatusInfoBg,
                border = BorderStroke(1.dp, StatusInfo.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth().testTag("tracking_unavailable_notice"),
              ) {
                Row(
                  modifier = Modifier.padding(14.dp),
                  verticalAlignment = Alignment.Top,
                ) {
                  Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = null,
                    tint = StatusInfo,
                  )
                  Spacer(modifier = Modifier.width(10.dp))
                  Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                      text = strings.locationSharingUnavailable,
                      style = MaterialTheme.typography.titleSmall,
                      color = StatusInfo,
                      fontWeight = FontWeight.SemiBold,
                    )
                    if (!uiState.error.isNullOrBlank()) {
                      Text(
                        text = uiState.error,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                      )
                    }
                  }
                }
              }
            } else if (tracking.status.contains("stale", ignoreCase = true)) {
              Surface(
                shape = RoundedCornerShape(14.dp),
                color = StatusWarningBg,
                border = BorderStroke(1.dp, StatusWarning.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth(),
              ) {
                Row(
                  modifier = Modifier.padding(12.dp),
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Icon(
                    imageVector = Icons.Filled.Schedule,
                    contentDescription = null,
                    tint = StatusWarning,
                  )
                  Spacer(modifier = Modifier.width(8.dp))
                  Text(
                    text = strings.locationStaleNotice,
                    style = MaterialTheme.typography.bodySmall,
                    color = StatusWarning,
                  )
                }
              }
            }

            // Courier Info & ETA Card
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
                  Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Surface(
                      shape = CircleShape,
                      color = MaterialTheme.colorScheme.primaryContainer,
                      modifier = Modifier.size(44.dp),
                    ) {
                      Box(contentAlignment = Alignment.Center) {
                        Icon(
                          imageVector = Icons.Filled.DeliveryDining,
                          contentDescription = null,
                          tint = MaterialTheme.colorScheme.primary,
                        )
                      }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                      Text(
                        text = strings.courierLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                      )
                      Text(
                        text = tracking?.driverName?.ifBlank { null } ?: "—",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                      )
                    }
                  }

                  val driverPhone = tracking?.driverPhone.orEmpty()
                  if (driverPhone.isNotBlank()) {
                    Button(
                      onClick = {
                        val dialIntent =
                          Intent(Intent.ACTION_DIAL, Uri.parse("tel:$driverPhone"))
                        context.startActivity(dialIntent)
                      },
                      shape = RoundedCornerShape(12.dp),
                      contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                      modifier =
                        Modifier.minimumInteractiveComponentSize()
                          .testTag("call_courier_button"),
                    ) {
                      Icon(
                        imageVector = Icons.Filled.Call,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                      )
                      Spacer(modifier = Modifier.width(6.dp))
                      Text(strings.callCourier)
                    }
                  }
                }

                if (!tracking?.etaText.isNullOrBlank()) {
                  HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                  ) {
                    Text(
                      text = strings.etaLabel,
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                      text = tracking?.etaText.orEmpty(),
                      style = MaterialTheme.typography.titleSmall,
                      fontWeight = FontWeight.Bold,
                      color = MaterialTheme.colorScheme.primary,
                    )
                  }
                }

                if (!tracking?.lastUpdatedAt.isNullOrBlank()) {
                  HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                  ) {
                    Text(
                      text = strings.lastUpdatedLabel,
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                      text = tracking?.lastUpdatedAt.orEmpty().take(19).replace("T", " "),
                      style = MaterialTheme.typography.bodySmall,
                      fontWeight = FontWeight.Medium,
                    )
                  }
                }

                val destAddress =
                  tracking?.destinationAddress?.ifBlank { null }
                    ?: order?.deliveryAddress?.ifBlank { null }
                    ?: order?.customerNote.orEmpty()
                if (destAddress.isNotBlank()) {
                  HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                  Row(verticalAlignment = Alignment.Top) {
                    Icon(
                      imageVector = Icons.Filled.LocationOn,
                      contentDescription = null,
                      tint = MaterialTheme.colorScheme.primary,
                      modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                      Text(
                        text = strings.destinationLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                      )
                      Text(
                        text = destAddress,
                        style = MaterialTheme.typography.bodySmall,
                      )
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}
