package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import com.example.data.model.Product
import com.example.data.model.ProductSortOption
import com.example.ui.components.CatalogLoadingSkeleton
import com.example.ui.components.EmptyArtworkState
import com.example.ui.components.ErrorStateView
import com.example.ui.components.OfflineBanner
import com.example.ui.components.WudeProductCard
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.LocalWudeStrings
import com.example.ui.viewmodel.CatalogUiState

@Composable
fun ExploreScreen(
  catalogState: CatalogUiState,
  products: List<Product>,
  searchQuery: String,
  categories: List<String>,
  selectedCategory: String,
  sortOption: ProductSortOption,
  onlyAvailable: Boolean,
  wishlistIds: Set<Long>,
  language: AppLanguage,
  isOnline: Boolean,
  imageLoader: ImageLoader,
  onSearchChange: (String) -> Unit,
  onSelectCategory: (String) -> Unit,
  onSelectSort: (ProductSortOption) -> Unit,
  onToggleAvailable: (Boolean) -> Unit,
  onWishlistClick: () -> Unit,
  onProductClick: (Long) -> Unit,
  onAddToCart: (Product) -> Unit,
  onToggleWishlist: (Product) -> Unit,
  onRefresh: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val strings = LocalWudeStrings.current

  Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
    Surface(
      color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
      tonalElevation = 1.dp,
    ) {
      Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = strings.navExplore,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
          )
          IconButton(
            onClick = onWishlistClick,
            modifier = Modifier.minimumInteractiveComponentSize().testTag("explore_wishlist_button"),
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
        }
        Spacer(modifier = Modifier.height(10.dp))
        OutlinedTextField(
          value = searchQuery,
          onValueChange = onSearchChange,
          modifier = Modifier.fillMaxWidth().testTag("explore_search_input"),
          placeholder = { Text(strings.searchPlaceholder) },
          leadingIcon = {
            Icon(imageVector = Icons.Filled.Search, contentDescription = null)
          },
          trailingIcon = {
            if (searchQuery.isNotEmpty()) {
              IconButton(
                onClick = { onSearchChange("") },
                modifier = Modifier.minimumInteractiveComponentSize().testTag("clear_search_button"),
              ) {
                Icon(imageVector = Icons.Filled.Close, contentDescription = "Clear search")
              }
            }
          },
          singleLine = true,
          shape = RoundedCornerShape(16.dp),
        )

        Spacer(modifier = Modifier.height(10.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          items(categories) { category ->
            val displayLabel = if (category == "All") strings.allCategory else category
            FilterChip(
              selected = category == selectedCategory,
              onClick = { onSelectCategory(category) },
              label = { Text(displayLabel) },
              modifier = Modifier.testTag("explore_cat_${category.lowercase().replace(" ", "_")}"),
              colors =
                FilterChipDefaults.filterChipColors(
                  selectedContainerColor = MaterialTheme.colorScheme.primary,
                  selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
          }
        }

        Spacer(modifier = Modifier.height(6.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          item {
            FilterChip(
              selected = onlyAvailable,
              onClick = { onToggleAvailable(!onlyAvailable) },
              label = { Text(strings.inStockOnly) },
              modifier = Modifier.testTag("filter_in_stock"),
            )
          }
          items(ProductSortOption.entries) { option ->
            FilterChip(
              selected = sortOption == option,
              onClick = { onSelectSort(option) },
              label = { Text(option.labelFor(language)) },
              modifier = Modifier.testTag("sort_${option.name.lowercase()}"),
              colors =
                FilterChipDefaults.filterChipColors(
                  selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                  selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                ),
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
      is CatalogUiState.Loading -> CatalogLoadingSkeleton()
      is CatalogUiState.Error -> ErrorStateView(catalogState.message, onRefresh)
      is CatalogUiState.Success -> {
        if (products.isEmpty()) {
          EmptyArtworkState(
            drawableResId = null,
            title = strings.noMatchingItems,
            subtitle = strings.noMatchingSubtitle,
            actionLabel = strings.resetFilters,
            onAction = {
              onSearchChange("")
              onSelectCategory("All")
              onToggleAvailable(false)
            },
            testTag = "reset_explore_filters",
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
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Text(
                    text = "${products.size} ${strings.productsFoundSuffix}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
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
