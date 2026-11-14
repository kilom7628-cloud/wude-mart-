package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import com.example.data.model.Product
import com.example.ui.components.EmptyArtworkState
import com.example.ui.components.WudeProductCard
import com.example.ui.i18n.LocalWudeStrings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WishlistScreen(
  products: List<Product>,
  imageLoader: ImageLoader,
  onBack: () -> Unit,
  onBrowseProducts: () -> Unit,
  onProductClick: (Long) -> Unit,
  onAddToCart: (Product) -> Unit,
  onToggleWishlist: (Product) -> Unit,
) {
  val strings = LocalWudeStrings.current

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text(strings.wishlistTitle)
            Text(
              text = "${products.size} ${strings.itemsSuffix}",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        },
        navigationIcon = {
          IconButton(
            onClick = onBack,
            modifier = Modifier.minimumInteractiveComponentSize().testTag("wishlist_back_button"),
          ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.back)
          }
        },
        colors =
          TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
          ),
      )
    }
  ) { innerPadding ->
    Column(
      modifier =
        Modifier.fillMaxSize()
          .background(MaterialTheme.colorScheme.background)
          .padding(innerPadding)
    ) {
      if (products.isEmpty()) {
        EmptyArtworkState(
          drawableResId = null,
          title = strings.wishlistEmptyTitle,
          subtitle = strings.wishlistEmptySub,
          actionLabel = strings.exploreMarketplace,
          onAction = onBrowseProducts,
          testTag = "wishlist_empty_explore",
        )
      } else {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
          val columns =
            when {
              maxWidth >= 840.dp -> 4
              maxWidth >= 600.dp -> 3
              else -> 2
            }
          val rows = products.chunked(columns)

          LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            item {
              Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth(),
              ) {
                Row(
                  modifier = Modifier.padding(12.dp),
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                  )
                  Spacer(modifier = Modifier.width(8.dp))
                  Text(
                    text = strings.wishlistSyncNote,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }
            }

            items(rows) { rowItems ->
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
              ) {
                rowItems.forEach { product ->
                  WudeProductCard(
                    product = product,
                    isWishlisted = true,
                    imageLoader = imageLoader,
                    onClick = { onProductClick(product.id) },
                    onAddToCart = { onAddToCart(product) },
                    onToggleWishlist = { onToggleWishlist(product) },
                    modifier = Modifier.weight(1f),
                  )
                }
                repeat(columns - rowItems.size) {
                  Spacer(modifier = Modifier.weight(1f))
                }
              }
            }
          }
        }
      }
    }
  }
}
