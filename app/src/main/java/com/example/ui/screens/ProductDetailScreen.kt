package com.example.ui.screens

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import com.example.data.model.Product
import com.example.ui.components.ErrorStateView
import com.example.ui.components.EtbPriceText
import com.example.ui.components.GlassCard
import com.example.ui.components.LoadingStateView
import com.example.ui.components.ProductThumbnail
import com.example.ui.components.WishlistHeartButton
import com.example.ui.i18n.LocalWudeStrings
import com.example.ui.theme.StatusDanger
import com.example.ui.theme.StatusDangerBg
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusSuccessBg
import com.example.ui.viewmodel.ProductDetailUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailScreen(
  uiState: ProductDetailUiState,
  isWishlisted: Boolean,
  imageLoader: ImageLoader,
  onBack: () -> Unit,
  onQuantityChange: (Int) -> Unit,
  onAddToCart: (Product, Int) -> Unit,
  onBuyNow: (Product, Int) -> Unit,
  onToggleWishlist: (Product) -> Unit,
  onRetry: () -> Unit,
) {
  val strings = LocalWudeStrings.current

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text(strings.productDetails) },
        navigationIcon = {
          IconButton(
            onClick = onBack,
            modifier = Modifier.minimumInteractiveComponentSize().testTag("detail_back_button"),
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = strings.back,
            )
          }
        },
        actions = {
          val product = uiState.product
          if (product != null) {
            WishlistHeartButton(
              isWishlisted = isWishlisted,
              onToggle = { onToggleWishlist(product) },
              modifier = Modifier.padding(end = 8.dp),
              testTag = "detail_wishlist_button",
            )
          }
        },
        colors =
          TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
          ),
      )
    },
    bottomBar = {
      val product = uiState.product
      if (product != null) {
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
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                  shape = RoundedCornerShape(12.dp),
                  color = MaterialTheme.colorScheme.surfaceVariant,
                ) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                      onClick = { onQuantityChange(uiState.selectedQuantity - 1) },
                      enabled = uiState.selectedQuantity > 1,
                      modifier = Modifier.minimumInteractiveComponentSize().testTag("detail_qty_minus"),
                    ) {
                      Icon(Icons.Filled.Remove, contentDescription = "Decrease quantity")
                    }
                    Text(
                      text = "${uiState.selectedQuantity}",
                      style = MaterialTheme.typography.titleMedium,
                      modifier = Modifier.padding(horizontal = 8.dp),
                    )
                    IconButton(
                      onClick = { onQuantityChange(uiState.selectedQuantity + 1) },
                      modifier = Modifier.minimumInteractiveComponentSize().testTag("detail_qty_plus"),
                    ) {
                      Icon(Icons.Filled.Add, contentDescription = "Increase quantity")
                    }
                  }
                }
              }

              Column(horizontalAlignment = Alignment.End) {
                Text(
                  text = strings.subtotal,
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                EtbPriceText(
                  amount = product.price * uiState.selectedQuantity,
                  fontSizeSp = 19,
                )
              }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              OutlinedButton(
                onClick = { onAddToCart(product, uiState.selectedQuantity) },
                enabled = product.available,
                modifier =
                  Modifier.weight(1f)
                    .minimumInteractiveComponentSize()
                    .testTag("detail_add_to_cart"),
                shape = RoundedCornerShape(14.dp),
                contentPadding = PaddingValues(vertical = 14.dp),
              ) {
                Icon(
                  imageVector = Icons.Filled.ShoppingCart,
                  contentDescription = null,
                  modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(strings.addToCart)
              }

              Button(
                onClick = { onBuyNow(product, uiState.selectedQuantity) },
                enabled = product.available,
                modifier =
                  Modifier.weight(1f)
                    .minimumInteractiveComponentSize()
                    .testTag("detail_buy_now"),
                shape = RoundedCornerShape(14.dp),
                colors =
                  ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                  ),
                contentPadding = PaddingValues(vertical = 14.dp),
              ) {
                Text(if (product.available) strings.buyNow else strings.unavailable)
              }
            }
          }
        }
      }
    },
  ) { innerPadding ->
    when {
      uiState.isLoading && uiState.product == null -> {
        LoadingStateView(
          message = "${strings.productDetails}…",
          modifier = Modifier.padding(innerPadding),
        )
      }
      uiState.error != null && uiState.product == null -> {
        ErrorStateView(
          message = uiState.error,
          onRetry = onRetry,
          modifier = Modifier.padding(innerPadding),
        )
      }
      uiState.product != null -> {
        val product = uiState.product
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
            ProductThumbnail(
              imageUrl = product.imageUrl,
              productName = product.name,
              imageLoader = imageLoader,
              modifier =
                Modifier.fillMaxWidth()
                  .height(300.dp)
                  .clip(RoundedCornerShape(22.dp)),
            )

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.primaryContainer,
              ) {
                Text(
                  text = product.categoryLabel,
                  style = MaterialTheme.typography.labelMedium,
                  color = MaterialTheme.colorScheme.onPrimaryContainer,
                  modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                )
              }

              Surface(
                shape = RoundedCornerShape(50),
                color = if (product.available) StatusSuccessBg else StatusDangerBg,
              ) {
                Text(
                  text = if (product.available) strings.inStock else strings.outOfStock,
                  style = MaterialTheme.typography.labelMedium,
                  fontWeight = FontWeight.SemiBold,
                  color = if (product.available) StatusSuccess else StatusDanger,
                  modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                )
              }
            }

            Column {
              Text(
                text = product.name,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
              )
              Spacer(modifier = Modifier.height(8.dp))
              EtbPriceText(
                amount = product.price,
                fontSizeSp = 26,
              )
            }

            GlassCard(shape = RoundedCornerShape(20.dp)) {
              Column(modifier = Modifier.padding(16.dp)) {
                Text(
                  text = strings.productDescription,
                  style = MaterialTheme.typography.titleMedium,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                  text = product.description,
                  style = MaterialTheme.typography.bodyLarge,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }

            GlassCard(
              shape = RoundedCornerShape(20.dp),
              tintColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
            ) {
              Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(
                    imageVector = Icons.Filled.LocalShipping,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                  )
                  Spacer(modifier = Modifier.width(12.dp))
                  Column {
                    Text(
                      text = strings.deliveryReadyTitle,
                      style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                      text = strings.deliveryReadySub,
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                  }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(
                    imageVector = Icons.Filled.VerifiedUser,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                  )
                  Spacer(modifier = Modifier.width(12.dp))
                  Column {
                    Text(
                      text = strings.proofVerifyTitle,
                      style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                      text = strings.proofVerifySub,
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                  }
                }
              }
            }

            Spacer(modifier = Modifier.height(12.dp))
          }
        }
      }
    }
  }
}
