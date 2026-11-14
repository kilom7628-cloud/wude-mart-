package com.example.ui.screens

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.Order
import com.example.data.model.OrderStatus
import com.example.ui.components.EmptyArtworkState
import com.example.ui.components.ErrorStateView
import com.example.ui.components.EtbPriceText
import com.example.ui.components.GlassCard
import com.example.ui.components.LoadingStateView
import com.example.ui.components.OfflineBanner
import com.example.ui.components.OrderStatusPill
import com.example.ui.components.pressBounce
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.LocalWudeStrings
import com.example.ui.viewmodel.OrdersUiState

@Composable
fun OrdersScreen(
  ordersState: OrdersUiState,
  language: AppLanguage,
  isOnline: Boolean,
  onSignInClick: () -> Unit,
  onBrowseClick: () -> Unit,
  onOrderClick: (Long) -> Unit,
  onRefresh: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val strings = LocalWudeStrings.current
  var selectedFilter by rememberSaveable { mutableStateOf("all") }
  val filterOptions =
    listOf(
      "all" to strings.filterAll,
      "pending" to strings.filterPending,
      "review" to strings.filterInReview,
      "completed" to strings.filterCompleted,
    )

  Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
    Surface(
      color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
      tonalElevation = 1.dp,
    ) {
      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column {
          Text(
            text = strings.myOrders,
            style = MaterialTheme.typography.headlineSmall,
          )
          Text(
            text = strings.ordersSubtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        IconButton(
          onClick = onRefresh,
          modifier = Modifier.minimumInteractiveComponentSize().testTag("orders_refresh_button"),
        ) {
          Icon(Icons.Filled.Refresh, contentDescription = strings.refresh)
        }
      }
    }

    OfflineBanner(isOnline = isOnline, onRetry = onRefresh)

    when (ordersState) {
      is OrdersUiState.SignedOut -> {
        EmptyArtworkState(
          drawableResId = R.drawable.img_empty_orders,
          title = strings.signInOrdersTitle,
          subtitle = strings.signInOrdersSub,
          actionLabel = strings.signInOrRegister,
          onAction = onSignInClick,
          testTag = "orders_sign_in_button",
        )
      }
      is OrdersUiState.Loading -> {
        LoadingStateView("${strings.myOrders}…")
      }
      is OrdersUiState.Error -> {
        ErrorStateView(message = ordersState.message, onRetry = onRefresh)
      }
      is OrdersUiState.Success -> {
        if (ordersState.orders.isEmpty()) {
          EmptyArtworkState(
            drawableResId = R.drawable.img_empty_orders,
            title = strings.noOrdersTitle,
            subtitle = strings.noOrdersSub,
            actionLabel = strings.exploreMarketplace,
            onAction = onBrowseClick,
            testTag = "orders_explore_button",
          )
        } else {
          val filtered =
            ordersState.orders.filter { order ->
              when (selectedFilter) {
                "pending" ->
                  order.status == OrderStatus.PENDING_PAYMENT ||
                    order.status == OrderStatus.REJECTED ||
                    order.latestRejectedPayment != null
                "review" -> order.status == OrderStatus.PAYMENT_REVIEW
                "completed" ->
                  order.status in
                    listOf(
                      OrderStatus.PAID,
                      OrderStatus.CONFIRMED,
                      OrderStatus.PROCESSING,
                      OrderStatus.SHIPPED,
                      OrderStatus.DELIVERED,
                    )
                else -> true
              }
            }

          Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Column(modifier = Modifier.widthIn(max = 720.dp).fillMaxSize()) {
              LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                items(filterOptions) { (key, label) ->
                  FilterChip(
                    selected = selectedFilter == key,
                    onClick = { selectedFilter = key },
                    label = { Text(label) },
                    modifier = Modifier.testTag("orders_filter_$key"),
                  )
                }
              }

              if (filtered.isEmpty()) {
                EmptyArtworkState(
                  drawableResId = null,
                  title = strings.noOrdersTitle,
                  subtitle = strings.noMatchingSubtitle,
                  actionLabel = strings.filterAll,
                  onAction = { selectedFilter = "all" },
                )
              } else {
                LazyColumn(
                  modifier = Modifier.fillMaxSize(),
                  contentPadding = PaddingValues(16.dp),
                  verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                  items(filtered, key = { it.id }) { order ->
                    OrderHistoryCard(
                      order = order,
                      language = language,
                      onClick = { onOrderClick(order.id) },
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

@Composable
private fun OrderHistoryCard(
  order: Order,
  language: AppLanguage,
  onClick: () -> Unit,
) {
  val strings = LocalWudeStrings.current
  GlassCard(
    modifier =
      Modifier.fillMaxWidth()
        .clip(RoundedCornerShape(18.dp))
        .testTag("order_card_${order.id}")
        .pressBounce(onClick = onClick),
    shape = RoundedCornerShape(18.dp),
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Filled.ReceiptLong,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
          )
          Spacer(modifier = Modifier.width(8.dp))
          Column {
            Text(
              text = "${strings.orderLabel} #${order.id}",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
            )
            Text(
              text = order.createdAt.take(10).ifBlank { strings.orderLabel },
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }

        val effectiveStatus =
          if (!order.isPaymentVerified && order.latestRejectedPayment != null && order.status == OrderStatus.PENDING_PAYMENT) {
            OrderStatus.REJECTED
          } else {
            order.status
          }
        OrderStatusPill(status = effectiveStatus, language = language)
      }

      Spacer(modifier = Modifier.height(10.dp))
      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
      Spacer(modifier = Modifier.height(10.dp))

      val summaryNames =
        order.items.joinToString(", ") { "${it.quantity}× ${it.productName}" }
          .ifBlank { strings.appName }
      Text(
        text = summaryNames,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
      )

      Spacer(modifier = Modifier.height(10.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column {
          Text(
            text = strings.totalPaidDue,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          EtbPriceText(
            amount = order.total,
            fontSizeSp = 18,
          )
        }

        if (order.status == OrderStatus.PENDING_PAYMENT || order.status == OrderStatus.REJECTED) {
          Button(
            onClick = onClick,
            shape = RoundedCornerShape(50),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            modifier = Modifier.minimumInteractiveComponentSize(),
          ) {
            Text(strings.payAndUpload)
          }
        } else {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = strings.trackOrder,
              style = MaterialTheme.typography.labelLarge,
              color = MaterialTheme.colorScheme.primary,
            )
            Icon(
              imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
            )
          }
        }
      }
    }
  }
}
