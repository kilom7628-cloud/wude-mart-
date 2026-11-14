package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import com.example.R
import com.example.data.model.Product
import com.example.ui.components.CatalogLoadingSkeleton
import com.example.ui.components.EmptyArtworkState
import com.example.ui.components.ErrorStateView
import com.example.ui.components.EtbPriceText
import com.example.ui.components.GlassCard
import com.example.ui.components.OfflineBanner
import com.example.ui.components.ProductThumbnail
import com.example.ui.components.WudeProductCard
import com.example.ui.components.pressBounce
import com.example.ui.i18n.LocalWudeStrings
import com.example.ui.theme.EthiopicFontFamily
import com.example.ui.theme.GraphiteCharcoal
import com.example.ui.theme.PureBlack
import com.example.ui.viewmodel.CatalogUiState

@Composable
fun HomeScreen(
  catalogState: CatalogUiState,
  products: List<Product>,
  categories: List<String>,
  selectedCategory: String,
  wishlistIds: Set<Long>,
  isOnline: Boolean,
  imageLoader: ImageLoader,
  onSelectCategory: (String) -> Unit,
  onSearchClick: () -> Unit,
  onWishlistClick: () -> Unit,
  onProductClick: (Long) -> Unit,
  onAddToCart: (Product) -> Unit,
  onToggleWishlist: (Product) -> Unit,
  onRefresh: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val strings = LocalWudeStrings.current

  Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
    // Top Brand Header with preserved Wude Mart logo
    Surface(
      color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
      tonalElevation = 1.dp,
    ) {
      Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
              painter = painterResource(id = R.drawable.img_wude_icon),
              contentDescription = strings.appName,
              contentScale = ContentScale.Crop,
              modifier = Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)),
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                  text = "Wude Mart",
                  style = MaterialTheme.typography.titleLarge,
                  color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = "ውዴ ማርት",
                  fontFamily = EthiopicFontFamily,
                  style = MaterialTheme.typography.labelMedium,
                  color = MaterialTheme.colorScheme.primary,
                )
              }
              Text(
                text = strings.brandSubtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }

          Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
              onClick = onWishlistClick,
              modifier = Modifier.minimumInteractiveComponentSize().testTag("home_wishlist_button"),
            ) {
              BadgedBox(
                badge = {
                  if (wishlistIds.isNotEmpty()) {
                    Badge { Text("${wishlistIds.size}") }
                  }
                }
              ) {
                Icon(
                  imageVector =
                    if (wishlistIds.isNotEmpty()) Icons.Filled.Favorite
                    else Icons.Outlined.FavoriteBorder,
                  contentDescription = strings.navWishlist,
                  tint = MaterialTheme.colorScheme.primary,
                )
              }
            }

            IconButton(
              onClick = onRefresh,
              modifier = Modifier.minimumInteractiveComponentSize().testTag("home_refresh_button"),
            ) {
              Icon(
                imageVector = Icons.Filled.Refresh,
                contentDescription = strings.refresh,
                tint = MaterialTheme.colorScheme.primary,
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Search Trigger Pill
        Surface(
          modifier =
            Modifier.fillMaxWidth()
              .clip(RoundedCornerShape(14.dp))
              .pressBounce(onClick = onSearchClick)
              .testTag("home_search_bar"),
          shape = RoundedCornerShape(14.dp),
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(
              imageVector = Icons.Filled.Search,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
              text = strings.searchHint,
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }
    }

    OfflineBanner(
      isOnline = isOnline,
      fromCache = (catalogState as? CatalogUiState.Success)?.fromCache == true,
      onRetry = onRefresh,
    )

    when (catalogState) {
      is CatalogUiState.Loading -> {
        CatalogLoadingSkeleton()
      }
      is CatalogUiState.Error -> {
        ErrorStateView(message = catalogState.message, onRetry = onRefresh)
      }
      is CatalogUiState.Success -> {
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
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
          ) {
            // Editorial Hero Banner
            item {
              HomeHeroBanner(
                onExploreClick = onSearchClick,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
              )
            }

            // Ethiopian Market Trust Strip
            item {
              LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                item {
                  TrustBadgeChip(
                    icon = Icons.Outlined.AutoAwesome,
                    title = strings.trustBirrTitle,
                    subtitle = strings.trustBirrSub,
                  )
                }
                item {
                  TrustBadgeChip(
                    icon = Icons.Filled.VerifiedUser,
                    title = strings.trustVerifiedTitle,
                    subtitle = strings.trustVerifiedSub,
                  )
                }
                item {
                  TrustBadgeChip(
                    icon = Icons.Filled.LocalShipping,
                    title = strings.trustDeliveryTitle,
                    subtitle = strings.trustDeliverySub,
                  )
                }
              }
            }

            // Discovery Categories Strip
            if (categories.size > 1) {
              item {
                Column {
                  Text(
                    text = strings.browseCollections,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                  )
                  LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                  ) {
                    items(categories) { cat ->
                      val selected = cat == selectedCategory
                      val displayCat = if (cat == "All") strings.allCategory else cat
                      FilterChip(
                        selected = selected,
                        onClick = { onSelectCategory(cat) },
                        label = { Text(displayCat) },
                        modifier = Modifier.testTag("home_category_${cat.lowercase().replace(" ", "_")}"),
                        colors =
                          FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                          ),
                      )
                    }
                  }
                }
              }
            }

            // Featured Highlights Carousel
            val featured = products.take(6)
            if (featured.isNotEmpty() && selectedCategory == "All") {
              item {
                Column {
                  Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    Text(
                      text = strings.featuredPicks,
                      style = MaterialTheme.typography.titleLarge,
                      color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                      text = strings.seeAll,
                      style = MaterialTheme.typography.labelLarge,
                      color = MaterialTheme.colorScheme.primary,
                      modifier =
                        Modifier.clickable(onClick = onSearchClick)
                          .padding(6.dp)
                          .testTag("home_see_all"),
                    )
                  }
                  Spacer(modifier = Modifier.height(8.dp))
                  LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                  ) {
                    items(featured, key = { "feat_${it.id}" }) { product ->
                      FeaturedStripCard(
                        product = product,
                        imageLoader = imageLoader,
                        onClick = { onProductClick(product.id) },
                      )
                    }
                  }
                }
              }
            }

            // All Marketplace Products Grid Header
            item {
              Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Text(
                  text =
                    if (selectedCategory == "All") strings.allMarketplaceItems
                    else selectedCategory,
                  style = MaterialTheme.typography.titleLarge,
                  color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                  text = "${products.size} ${strings.itemsSuffix}",
                  style = MaterialTheme.typography.labelMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }

            if (products.isEmpty()) {
              item {
                EmptyArtworkState(
                  drawableResId = null,
                  title = strings.noProductsAvailable,
                  subtitle = strings.noProductsSubtitle,
                  actionLabel = strings.showAllProducts,
                  onAction = { onSelectCategory("All") },
                  modifier = Modifier.height(280.dp),
                )
              }
            } else {
              items(rows) { rowItems ->
                Row(
                  modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                  horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                  rowItems.forEach { product ->
                    WudeProductCard(
                      product = product,
                      isWishlisted = wishlistIds.contains(product.id),
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
}

@Composable
private fun HomeHeroBanner(
  onExploreClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val strings = LocalWudeStrings.current
  Box(
    modifier =
      modifier
        .fillMaxWidth()
        .height(186.dp)
        .clip(RoundedCornerShape(22.dp))
        .pressBounce(onClick = onExploreClick)
        .testTag("home_hero_banner"),
  ) {
    Image(
      painter = painterResource(id = R.drawable.img_hero_banner),
      contentDescription = strings.appName,
      contentScale = ContentScale.Crop,
      modifier = Modifier.fillMaxSize(),
    )
    Box(
      modifier =
        Modifier.fillMaxSize()
          .background(
            Brush.horizontalGradient(
              colors =
                listOf(
                  PureBlack.copy(alpha = 0.90f),
                  GraphiteCharcoal.copy(alpha = 0.72f),
                  Color.Transparent,
                )
            )
          )
    )
    Column(
      modifier = Modifier.fillMaxSize().padding(18.dp),
      verticalArrangement = Arrangement.SpaceBetween,
    ) {
      Surface(
        shape = RoundedCornerShape(50),
        color = Color.White.copy(alpha = 0.16f),
        border = BorderStroke(0.8.dp, Color.White.copy(alpha = 0.45f)),
      ) {
        Text(
          text = strings.heroBadge,
          style = MaterialTheme.typography.labelSmall,
          fontWeight = FontWeight.Bold,
          color = Color.White,
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
      }
      Column(modifier = Modifier.fillMaxWidth(0.75f)) {
        Text(
          text = strings.heroHeadline,
          style = MaterialTheme.typography.headlineSmall,
          color = Color.White,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(
          onClick = onExploreClick,
          shape = RoundedCornerShape(50),
          colors =
            ButtonDefaults.buttonColors(
              containerColor = Color.White.copy(alpha = 0.96f),
              contentColor = PureBlack,
            ),
          contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
          modifier = Modifier.minimumInteractiveComponentSize().testTag("hero_explore_button"),
        ) {
          Text(
            text = strings.shopNow,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
          )
        }
      }
    }
  }
}

@Composable
private fun TrustBadgeChip(
  icon: ImageVector,
  title: String,
  subtitle: String,
) {
  GlassCard(shape = RoundedCornerShape(16.dp), elevation = 2.dp) {
    Row(
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.size(34.dp),
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp),
          )
        }
      }
      Spacer(modifier = Modifier.width(10.dp))
      Column {
        Text(
          text = title,
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
          text = subtitle,
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}

@Composable
private fun FeaturedStripCard(
  product: Product,
  imageLoader: ImageLoader,
  onClick: () -> Unit,
) {
  GlassCard(
    modifier =
      Modifier.width(172.dp)
        .clip(RoundedCornerShape(18.dp))
        .pressBounce(onClick = onClick)
        .testTag("featured_card_${product.id}"),
    shape = RoundedCornerShape(18.dp),
    elevation = 3.dp,
  ) {
    ProductThumbnail(
      imageUrl = product.imageUrl,
      productName = product.name,
      imageLoader = imageLoader,
      modifier = Modifier.fillMaxWidth().height(112.dp),
    )
    Column(modifier = Modifier.padding(10.dp)) {
      Text(
        text = product.name,
        style = MaterialTheme.typography.titleSmall,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Spacer(modifier = Modifier.height(4.dp))
      EtbPriceText(
        amount = product.price,
        fontSizeSp = 15,
      )
    }
  }
}
