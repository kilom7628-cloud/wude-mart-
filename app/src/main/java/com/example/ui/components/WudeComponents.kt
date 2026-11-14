package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.ImageLoader
import coil.compose.SubcomposeAsyncImage
import com.example.data.model.EtbFormatter
import com.example.data.model.OrderStatus
import com.example.data.model.Product
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.LocalWudeStrings
import com.example.ui.theme.OutfitFontFamily
import com.example.ui.theme.StatusDanger
import com.example.ui.theme.StatusDangerBg
import com.example.ui.theme.StatusInfo
import com.example.ui.theme.StatusInfoBg
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusSuccessBg
import com.example.ui.theme.StatusWarning
import com.example.ui.theme.StatusWarningBg
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun EtbPriceText(
  amount: Double,
  modifier: Modifier = Modifier,
  color: Color = MaterialTheme.colorScheme.primary,
  fontSizeSp: Int = 17,
) {
  Text(
    text = EtbFormatter.format(amount),
    modifier = modifier,
    color = color,
    fontFamily = OutfitFontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = fontSizeSp.sp,
    maxLines = 1,
    overflow = TextOverflow.Ellipsis,
  )
}

/**
 * Monochrome Black & White Hybrid Glass card:
 * subtle translucent surface, thin luminous silver/white border, and soft elevation shadow.
 */
@Composable
fun GlassCard(
  modifier: Modifier = Modifier,
  shape: Shape = RoundedCornerShape(20.dp),
  elevation: Dp = 4.dp,
  tintColor: Color? = null,
  content: @Composable ColumnScope.() -> Unit,
) {
  val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.35f
  val resolvedTint =
    tintColor
      ?: if (isDark) {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.84f)
      } else {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.88f)
      }
  val borderBrush =
    Brush.linearGradient(
      colors =
        if (isDark) {
          listOf(
            Color.White.copy(alpha = 0.20f),
            Color.White.copy(alpha = 0.06f),
          )
        } else {
          listOf(
            Color.White.copy(alpha = 0.85f),
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.85f),
          )
        }
    )
  Surface(
    modifier =
      modifier.shadow(
        elevation = elevation,
        shape = shape,
        ambientColor = Color.Black.copy(alpha = if (isDark) 0.40f else 0.08f),
        spotColor = Color.Black.copy(alpha = if (isDark) 0.45f else 0.10f),
      ),
    shape = shape,
    color = resolvedTint,
    border = BorderStroke(1.dp, borderBrush),
  ) {
    Column(modifier = Modifier.animateContentSize(), content = content)
  }
}

@Composable
fun Modifier.pressBounce(enabled: Boolean = true, onClick: () -> Unit): Modifier {
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()
  val scale by
    animateFloatAsState(
      targetValue = if (isPressed && enabled) 0.965f else 1f,
      animationSpec = spring(dampingRatio = 0.62f, stiffness = 520f),
      label = "press_bounce_scale",
    )
  return this.graphicsLayer {
      scaleX = scale
      scaleY = scale
    }
    .clickable(
      interactionSource = interactionSource,
      indication = androidx.compose.material3.ripple(),
      enabled = enabled,
      onClick = onClick,
    )
}

@Composable
fun WishlistHeartButton(
  isWishlisted: Boolean,
  onToggle: () -> Unit,
  modifier: Modifier = Modifier,
  testTag: String = "wishlist_heart_button",
) {
  val scaleAnim = remember { Animatable(1f) }
  LaunchedEffect(isWishlisted) {
    if (isWishlisted) {
      scaleAnim.animateTo(1.32f, spring(dampingRatio = 0.45f, stiffness = 600f))
      scaleAnim.animateTo(1f, spring(dampingRatio = 0.6f, stiffness = 450f))
    } else {
      scaleAnim.animateTo(0.88f, tween(80))
      scaleAnim.animateTo(1f, spring(dampingRatio = 0.65f, stiffness = 450f))
    }
  }
  val heartTint by
    animateColorAsState(
      targetValue =
        if (isWishlisted) Color(0xFFD92B4B) else MaterialTheme.colorScheme.onSurface,
      animationSpec = tween(180),
      label = "heart_tint",
    )

  Surface(
    shape = CircleShape,
    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.84f),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
    modifier = modifier.size(40.dp),
  ) {
    IconButton(
      onClick = onToggle,
      modifier =
        Modifier.fillMaxSize()
          .minimumInteractiveComponentSize()
          .testTag(testTag),
    ) {
      Icon(
        imageVector = if (isWishlisted) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
        contentDescription = if (isWishlisted) "Remove from wishlist" else "Save to wishlist",
        tint = heartTint,
        modifier =
          Modifier.size(20.dp).graphicsLayer {
            scaleX = scaleAnim.value
            scaleY = scaleAnim.value
          },
      )
    }
  }
}

@Composable
fun ProductThumbnail(
  imageUrl: String,
  productName: String,
  imageLoader: ImageLoader,
  modifier: Modifier = Modifier,
) {
  if (imageUrl.isBlank()) {
    ProductImagePlaceholder(productName = productName, modifier = modifier)
  } else {
    SubcomposeAsyncImage(
      model = imageUrl,
      imageLoader = imageLoader,
      contentDescription = productName,
      contentScale = ContentScale.Crop,
      modifier = modifier,
      loading = {
        Box(
          modifier =
            Modifier.fillMaxSize()
              .background(MaterialTheme.colorScheme.surfaceVariant),
          contentAlignment = Alignment.Center,
        ) {
          CircularProgressIndicator(
            modifier = Modifier.size(22.dp),
            strokeWidth = 2.dp,
            color = MaterialTheme.colorScheme.primary,
          )
        }
      },
      error = {
        ProductImagePlaceholder(productName = productName, modifier = Modifier.fillMaxSize())
      },
    )
  }
}

@Composable
fun ProductImagePlaceholder(
  productName: String,
  modifier: Modifier = Modifier,
) {
  Box(
    modifier =
      modifier.background(
        Brush.linearGradient(
          colors =
            listOf(
              MaterialTheme.colorScheme.surfaceVariant,
              MaterialTheme.colorScheme.surfaceContainerHigh,
            )
        )
      ),
    contentAlignment = Alignment.Center,
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
      modifier = Modifier.padding(12.dp),
    ) {
      Icon(
        imageVector = Icons.Filled.ShoppingBag,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(34.dp),
      )
      Spacer(modifier = Modifier.height(6.dp))
      Text(
        text = productName.take(18),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@Composable
fun WudeProductCard(
  product: Product,
  isWishlisted: Boolean,
  imageLoader: ImageLoader,
  onClick: () -> Unit,
  onAddToCart: () -> Unit,
  onToggleWishlist: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val strings = LocalWudeStrings.current
  var justAdded by remember { mutableStateOf(false) }
  val scope = rememberCoroutineScope()

  Card(
    modifier =
      modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(18.dp))
        .testTag("product_card_${product.id}")
        .pressBounce(onClick = onClick),
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f)),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
  ) {
    Column {
      Box(
        modifier =
          Modifier.fillMaxWidth()
            .aspectRatio(1.05f)
            .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
      ) {
        ProductThumbnail(
          imageUrl = product.imageUrl,
          productName = product.name,
          imageLoader = imageLoader,
          modifier = Modifier.fillMaxSize(),
        )
        Surface(
          modifier = Modifier.align(Alignment.TopStart).padding(10.dp),
          shape = RoundedCornerShape(50),
          color =
            if (product.available) MaterialTheme.colorScheme.surface.copy(alpha = 0.86f)
            else StatusDangerBg,
          border = BorderStroke(0.7.dp, Color.White.copy(alpha = 0.55f)),
        ) {
          Text(
            text = if (product.available) product.categoryLabel else strings.outOfStock,
            style = MaterialTheme.typography.labelSmall,
            color =
              if (product.available) MaterialTheme.colorScheme.onSurface
              else StatusDanger,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }

        WishlistHeartButton(
          isWishlisted = isWishlisted,
          onToggle = onToggleWishlist,
          modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
          testTag = "wishlist_toggle_${product.id}",
        )
      }

      Column(modifier = Modifier.padding(12.dp)) {
        Text(
          text = product.name,
          style = MaterialTheme.typography.titleSmall,
          color = MaterialTheme.colorScheme.onSurface,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
          minLines = 2,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = product.description,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          EtbPriceText(
            amount = product.price,
            fontSizeSp = 17,
            modifier = Modifier.weight(1f),
          )

          val buttonColor by
            animateColorAsState(
              targetValue =
                if (justAdded) StatusSuccess else MaterialTheme.colorScheme.primary,
              animationSpec = tween(180),
              label = "add_cart_btn_color",
            )
          FilledTonalIconButton(
            onClick = {
              onAddToCart()
              scope.launch {
                justAdded = true
                delay(700L)
                justAdded = false
              }
            },
            enabled = product.available,
            modifier =
              Modifier.minimumInteractiveComponentSize()
                .testTag("add_to_cart_${product.id}"),
            colors =
              IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = buttonColor,
                contentColor = MaterialTheme.colorScheme.onPrimary,
              ),
          ) {
            AnimatedContent(
              targetState = justAdded,
              transitionSpec = {
                (scaleIn(spring(dampingRatio = 0.6f)) + fadeIn()) togetherWith
                  (scaleOut() + fadeOut())
              },
              label = "add_icon_swap",
            ) { added ->
              Icon(
                imageVector = if (added) Icons.Filled.Check else Icons.Filled.Add,
                contentDescription = "${strings.addToCart}: ${product.name}",
              )
            }
          }
        }
      }
    }
  }
}

@Composable
fun ExpectedScheduleCard(
  isPickup: Boolean,
  expectedDate: String?,
  expectedWindow: String?,
  scheduleUpdatedAlert: Boolean = false,
  modifier: Modifier = Modifier,
) {
  val strings = LocalWudeStrings.current
  val title = if (isPickup) strings.expectedPickupDateTitle else strings.expectedDeliveryDateTitle

  GlassCard(
    shape = RoundedCornerShape(20.dp),
    modifier = modifier.fillMaxWidth().testTag("expected_schedule_card"),
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Filled.CalendarMonth,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
          )
        }
        if (scheduleUpdatedAlert) {
          Surface(shape = RoundedCornerShape(50), color = StatusInfoBg) {
            Text(
              text = "Updated",
              style = MaterialTheme.typography.labelSmall,
              color = StatusInfo,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
            )
          }
        }
      }

      if (!expectedDate.isNullOrBlank()) {
        Text(
          text = expectedDate,
          style = MaterialTheme.typography.headlineSmall,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface,
        )
        if (!expectedWindow.isNullOrBlank()) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Filled.Schedule,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "${strings.timeWindowLabel}: $expectedWindow",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      } else {
        Text(
          text = strings.deliveryDateWillBeConfirmed,
          style = MaterialTheme.typography.bodyLarge,
          fontWeight = FontWeight.SemiBold,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      if (scheduleUpdatedAlert) {
        Text(
          text = strings.scheduleUpdatedNotice,
          style = MaterialTheme.typography.bodySmall,
          color = StatusInfo,
        )
      }
    }
  }
}

@Composable
fun CatalogLoadingSkeleton(modifier: Modifier = Modifier) {
  val transition = rememberInfiniteTransition(label = "shimmer")
  val shift by
    transition.animateFloat(
      initialValue = 0f,
      targetValue = 900f,
      animationSpec =
        infiniteRepeatable(
          animation = tween(durationMillis = 1200, easing = LinearEasing),
          repeatMode = RepeatMode.Restart,
        ),
      label = "shimmer_shift",
    )

  val shimmerBrush =
    Brush.linearGradient(
      colors =
        listOf(
          MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
          MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
          MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        ),
      start = Offset(shift - 300f, 0f),
      end = Offset(shift, 300f),
    )

  Column(
    modifier = modifier.fillMaxSize().padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    Box(
      modifier =
        Modifier.fillMaxWidth()
          .height(180.dp)
          .clip(RoundedCornerShape(22.dp))
          .background(shimmerBrush)
    )
    repeat(2) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        repeat(2) {
          Box(
            modifier =
              Modifier.weight(1f)
                .height(220.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(shimmerBrush)
          )
        }
      }
    }
  }
}

@Composable
fun OfflineBanner(
  isOnline: Boolean,
  fromCache: Boolean = false,
  onRetry: (() -> Unit)? = null,
) {
  val strings = LocalWudeStrings.current
  AnimatedVisibility(
    visible = !isOnline || fromCache,
    enter = fadeIn(),
    exit = fadeOut(),
  ) {
    Surface(
      modifier = Modifier.fillMaxWidth(),
      color = StatusWarningBg.copy(alpha = 0.94f),
      contentColor = Color(0xFF4A2C00),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.weight(1f),
        ) {
          Icon(
            imageVector = Icons.Filled.CloudOff,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = if (!isOnline) strings.offlineModeMsg else strings.cachedItemsMsg,
            style = MaterialTheme.typography.labelMedium,
          )
        }
        if (onRetry != null) {
          Text(
            text = strings.retry,
            style = MaterialTheme.typography.labelLarge,
            color = Color(0xFF141519),
            modifier =
              Modifier.clickable(onClick = onRetry)
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .testTag("offline_retry_button"),
          )
        }
      }
    }
  }
}

@Composable
fun OrderStatusPill(
  status: OrderStatus,
  language: AppLanguage = AppLanguage.ENGLISH,
  modifier: Modifier = Modifier,
) {
  val (bg, fg) =
    when (status) {
      OrderStatus.PENDING_PAYMENT -> StatusWarningBg to StatusWarning
      OrderStatus.PAYMENT_REVIEW -> StatusInfoBg to StatusInfo
      OrderStatus.PAID,
      OrderStatus.CONFIRMED,
      OrderStatus.PROCESSING,
      OrderStatus.SHIPPED,
      OrderStatus.DELIVERED -> StatusSuccessBg to StatusSuccess
      OrderStatus.CANCELLED,
      OrderStatus.REJECTED -> StatusDangerBg to StatusDanger
    }

  Surface(
    modifier = modifier,
    shape = RoundedCornerShape(50),
    color = bg,
  ) {
    Text(
      text = status.labelFor(language),
      color = fg,
      style = MaterialTheme.typography.labelMedium,
      fontWeight = FontWeight.SemiBold,
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
    )
  }
}

@Composable
fun OrderStatusStepper(
  status: OrderStatus,
  language: AppLanguage = AppLanguage.ENGLISH,
  modifier: Modifier = Modifier,
) {
  if (status == OrderStatus.CANCELLED || status == OrderStatus.REJECTED) {
    Surface(
      modifier = modifier.fillMaxWidth(),
      shape = RoundedCornerShape(14.dp),
      color = StatusDangerBg,
    ) {
      Row(
        modifier = Modifier.padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Icon(
          imageVector = Icons.Outlined.ErrorOutline,
          contentDescription = null,
          tint = StatusDanger,
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
          text =
            if (status == OrderStatus.REJECTED) {
              if (language == AppLanguage.AMHARIC) {
                "የክፍያ ማረጋገጫ አልተሳካም። ከታች ያለውን የአስተዳዳሪ መልዕክት አይተው አዲስ ፎቶ ይላኩ።"
              } else {
                "Payment verification failed. Please review administrator feedback below and submit a valid screenshot."
              }
            } else {
              if (language == AppLanguage.AMHARIC) "ይህ ትዕዛዝ ተሰርዟል።" else "This order was cancelled."
            },
          style = MaterialTheme.typography.bodyMedium,
          color = StatusDanger,
        )
      }
    }
    return
  }

  val steps =
    if (language == AppLanguage.AMHARIC) {
      listOf(
        1 to "ትዕዛዝ\nተላከ",
        2 to "ክፍያ\nበማጣራት",
        3 to "ተረጋግጦ\nተከፈለ",
        5 to "በመንገድ\nላይ",
        6 to "ደረሰ",
      )
    } else {
      listOf(
        1 to "Order\nPlaced",
        2 to "Payment\nReview",
        3 to "Verified\n& Paid",
        5 to "Out for\nDelivery",
        6 to "Delivered",
      )
    }

  Row(
    modifier = modifier.fillMaxWidth(),
    verticalAlignment = Alignment.Top,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    steps.forEachIndexed { idx, (stepNumber, label) ->
      val reached = status.stepIndex >= stepNumber
      val isCurrent =
        when (stepNumber) {
          1 -> status == OrderStatus.PENDING_PAYMENT
          2 -> status == OrderStatus.PAYMENT_REVIEW
          3 -> status == OrderStatus.PAID || status == OrderStatus.CONFIRMED || status == OrderStatus.PROCESSING
          5 -> status == OrderStatus.SHIPPED
          6 -> status == OrderStatus.DELIVERED
          else -> false
        }

      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.weight(1f),
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Box(
            modifier =
              Modifier.weight(1f)
                .height(3.dp)
                .background(
                  if (idx == 0) Color.Transparent
                  else if (reached) MaterialTheme.colorScheme.primary
                  else MaterialTheme.colorScheme.outlineVariant
                )
          )
          Box(
            modifier =
              Modifier.size(if (isCurrent) 28.dp else 24.dp)
                .clip(CircleShape)
                .background(
                  when {
                    reached -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.surfaceVariant
                  }
                ),
            contentAlignment = Alignment.Center,
          ) {
            if (reached) {
              Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(14.dp),
              )
            } else {
              Text(
                text = "${idx + 1}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
          Box(
            modifier =
              Modifier.weight(1f)
                .height(3.dp)
                .background(
                  if (idx == steps.lastIndex) Color.Transparent
                  else if (status.stepIndex > stepNumber) MaterialTheme.colorScheme.primary
                  else MaterialTheme.colorScheme.outlineVariant
                )
          )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
          text = label,
          style = MaterialTheme.typography.labelSmall,
          textAlign = TextAlign.Center,
          fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
          color =
            if (reached) MaterialTheme.colorScheme.onSurface
            else MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}

@Composable
fun LoadingStateView(
  message: String = "Loading Wude Mart…",
  modifier: Modifier = Modifier,
) {
  Box(
    modifier = modifier.fillMaxSize().padding(24.dp),
    contentAlignment = Alignment.Center,
  ) {
    GlassCard(
      shape = RoundedCornerShape(22.dp),
      modifier = Modifier.padding(12.dp),
    ) {
      Column(
        modifier = Modifier.padding(horizontal = 28.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(14.dp))
        Text(
          text = message,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}

@Composable
fun ErrorStateView(
  message: String,
  onRetry: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val strings = LocalWudeStrings.current
  Box(
    modifier = modifier.fillMaxSize().padding(24.dp),
    contentAlignment = Alignment.Center,
  ) {
    GlassCard(shape = RoundedCornerShape(22.dp)) {
      Column(
        modifier = Modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
      ) {
        Surface(
          shape = CircleShape,
          color = StatusDangerBg,
          modifier = Modifier.size(64.dp),
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = Icons.Outlined.ErrorOutline,
              contentDescription = null,
              tint = StatusDanger,
              modifier = Modifier.size(32.dp),
            )
          }
        }
        Spacer(modifier = Modifier.height(14.dp))
        Text(
          text = strings.somethingWentWrong,
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
          text = message,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(18.dp))
        Button(
          onClick = onRetry,
          modifier = Modifier.minimumInteractiveComponentSize().testTag("retry_button"),
          contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
        ) {
          Icon(imageVector = Icons.Filled.Refresh, contentDescription = null)
          Spacer(modifier = Modifier.width(8.dp))
          Text(strings.tryAgain)
        }
      }
    }
  }
}

@Composable
fun EmptyArtworkState(
  drawableResId: Int?,
  title: String,
  subtitle: String,
  actionLabel: String? = null,
  onAction: (() -> Unit)? = null,
  testTag: String = "empty_state_action",
  modifier: Modifier = Modifier,
) {
  Box(
    modifier = modifier.fillMaxSize().padding(24.dp),
    contentAlignment = Alignment.Center,
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier = Modifier.fillMaxWidth(),
    ) {
      if (drawableResId != null) {
        Image(
          painter = painterResource(id = drawableResId),
          contentDescription = title,
          contentScale = ContentScale.Crop,
          modifier =
            Modifier.size(width = 210.dp, height = 158.dp)
              .clip(RoundedCornerShape(20.dp)),
        )
      } else {
        Surface(
          shape = CircleShape,
          color = MaterialTheme.colorScheme.primaryContainer,
          modifier = Modifier.size(72.dp),
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = Icons.Filled.Inventory2,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(34.dp),
            )
          }
        }
      }
      Spacer(modifier = Modifier.height(18.dp))
      Text(
        text = title,
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.onBackground,
        textAlign = TextAlign.Center,
      )
      Spacer(modifier = Modifier.height(8.dp))
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
      )
      if (actionLabel != null && onAction != null) {
        Spacer(modifier = Modifier.height(20.dp))
        OutlinedButton(
          onClick = onAction,
          modifier = Modifier.minimumInteractiveComponentSize().testTag(testTag),
          border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
          colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
        ) {
          Text(actionLabel)
        }
      }
    }
  }
}
