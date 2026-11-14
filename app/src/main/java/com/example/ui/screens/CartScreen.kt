package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ShoppingCartCheckout
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import com.example.R
import com.example.data.model.CartItem
import com.example.data.model.EtbFormatter
import com.example.ui.components.EmptyArtworkState
import com.example.ui.components.EtbPriceText
import com.example.ui.components.GlassCard
import com.example.ui.components.ProductThumbnail
import com.example.ui.i18n.LocalWudeStrings
import com.example.ui.theme.StatusDanger

@Composable
fun CartScreen(
  cartItems: List<CartItem>,
  isSignedIn: Boolean,
  imageLoader: ImageLoader,
  onBrowseProducts: () -> Unit,
  onProductClick: (Long) -> Unit,
  onUpdateQuantity: (Long, Int) -> Unit,
  onRemoveItem: (Long) -> Unit,
  onClearCart: () -> Unit,
  onProceedToCheckout: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val strings = LocalWudeStrings.current
  var showClearCartConfirm by remember { mutableStateOf(false) }

  if (showClearCartConfirm) {
    AlertDialog(
      onDismissRequest = { showClearCartConfirm = false },
      title = { Text(strings.clearCartTitle) },
      text = { Text(strings.clearCartConfirm) },
      confirmButton = {
        Button(
          onClick = {
            showClearCartConfirm = false
            onClearCart()
          },
          colors = ButtonDefaults.buttonColors(containerColor = StatusDanger),
          modifier = Modifier.testTag("confirm_clear_cart_button"),
        ) {
          Text(strings.clearAll)
        }
      },
      dismissButton = {
        TextButton(
          onClick = { showClearCartConfirm = false },
          modifier = Modifier.testTag("cancel_clear_cart_button"),
        ) {
          Text(strings.keepItems)
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
            text = strings.shoppingCart,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
          )
          Text(
            text = "${cartItems.sumOf { it.quantity }} ${strings.itemsSuffix} • ብር (ETB)",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        if (cartItems.isNotEmpty()) {
          IconButton(
            onClick = { showClearCartConfirm = true },
            modifier = Modifier.minimumInteractiveComponentSize().testTag("clear_cart_button"),
          ) {
            Icon(
              imageVector = Icons.Filled.DeleteSweep,
              contentDescription = strings.clearAll,
              tint = MaterialTheme.colorScheme.error,
            )
          }
        }
      }
    }

    if (cartItems.isEmpty()) {
      EmptyArtworkState(
        drawableResId = R.drawable.img_empty_cart,
        title = strings.cartEmptyTitle,
        subtitle = strings.cartEmptySub,
        actionLabel = strings.startShopping,
        onAction = onBrowseProducts,
        testTag = "empty_cart_shop_button",
      )
    } else {
      val subtotal = cartItems.sumOf { it.lineTotal }
      Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(modifier = Modifier.widthIn(max = 680.dp).fillMaxSize()) {
          LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            items(cartItems, key = { it.product.id }) { item ->
              CartItemCard(
                item = item,
                imageLoader = imageLoader,
                onClick = { onProductClick(item.product.id) },
                onIncrement = { onUpdateQuantity(item.product.id, item.quantity + 1) },
                onDecrement = { onUpdateQuantity(item.product.id, item.quantity - 1) },
                onRemove = { onRemoveItem(item.product.id) },
              )
            }
          }

          Surface(
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            tonalElevation = 4.dp,
            shadowElevation = 8.dp,
          ) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Text(
                  text = strings.itemsSubtotal,
                  style = MaterialTheme.typography.bodyLarge,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                EtbPriceText(
                  amount = subtotal,
                  fontSizeSp = 21,
                )
              }

              Spacer(modifier = Modifier.height(8.dp))
              HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
              Spacer(modifier = Modifier.height(10.dp))

              Button(
                onClick = onProceedToCheckout,
                modifier =
                  Modifier.fillMaxWidth()
                    .minimumInteractiveComponentSize()
                    .testTag("proceed_to_checkout_button"),
                shape = RoundedCornerShape(14.dp),
                contentPadding = PaddingValues(vertical = 14.dp),
              ) {
                Icon(
                  imageVector = Icons.Filled.ShoppingCartCheckout,
                  contentDescription = null,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text =
                    if (isSignedIn) strings.proceedToCheckout
                    else strings.signInToCheckout,
                  style = MaterialTheme.typography.titleSmall,
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
private fun CartItemCard(
  item: CartItem,
  imageLoader: ImageLoader,
  onClick: () -> Unit,
  onIncrement: () -> Unit,
  onDecrement: () -> Unit,
  onRemove: () -> Unit,
) {
  val strings = LocalWudeStrings.current
  GlassCard(
    modifier = Modifier.fillMaxWidth().testTag("cart_item_${item.product.id}"),
    shape = RoundedCornerShape(18.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(12.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      ProductThumbnail(
        imageUrl = item.product.imageUrl,
        productName = item.product.name,
        imageLoader = imageLoader,
        modifier =
          Modifier.size(84.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
      )

      Spacer(modifier = Modifier.width(12.dp))

      Column(modifier = Modifier.weight(1f)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.Top,
        ) {
          Text(
            text = item.product.name,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).clickable(onClick = onClick),
          )
          IconButton(
            onClick = onRemove,
            modifier =
              Modifier.minimumInteractiveComponentSize()
                .testTag("remove_cart_${item.product.id}"),
          ) {
            Icon(
              imageVector = Icons.Filled.DeleteOutline,
              contentDescription = "Remove ${item.product.name}",
              tint = MaterialTheme.colorScheme.error,
            )
          }
        }

        Text(
          text = "${strings.unitLabel}: ${EtbFormatter.format(item.product.price)}",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              IconButton(
                onClick = onDecrement,
                modifier =
                  Modifier.minimumInteractiveComponentSize()
                    .testTag("cart_minus_${item.product.id}"),
              ) {
                Icon(Icons.Filled.Remove, contentDescription = "Decrease quantity")
              }
              Text(
                text = "${item.quantity}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 4.dp),
              )
              IconButton(
                onClick = onIncrement,
                modifier =
                  Modifier.minimumInteractiveComponentSize()
                    .testTag("cart_plus_${item.product.id}"),
              ) {
                Icon(Icons.Filled.Add, contentDescription = "Increase quantity")
              }
            }
          }

          EtbPriceText(
            amount = item.lineTotal,
            fontSizeSp = 16,
          )
        }
      }
    }
  }
}
