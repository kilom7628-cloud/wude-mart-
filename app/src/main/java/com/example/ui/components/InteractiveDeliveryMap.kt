package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeliveryDining
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.AsyncImage
import com.example.data.model.GeoPoint
import com.example.ui.i18n.LocalWudeStrings
import com.example.ui.theme.StatusSuccess
import kotlin.math.PI
import kotlin.math.asinh
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.tan

private const val DEFAULT_ADDIS_LAT = 9.0192
private const val DEFAULT_ADDIS_LNG = 38.7525
private const val TILE_PX = 256.0

/**
 * Interactive pan-and-zoom Mercator map for live delivery tracking.
 * Renders real OpenStreetMap tiles (with subtle monochrome filter) + graceful vector grid fallback,
 * animated courier marker, customer destination marker, and backend-provided route polyline.
 */
@Composable
fun InteractiveDeliveryMap(
  courierLocation: GeoPoint?,
  destinationLocation: GeoPoint?,
  route: List<GeoPoint>,
  imageLoader: ImageLoader,
  modifier: Modifier = Modifier,
) {
  val strings = LocalWudeStrings.current
  val initialCenter = courierLocation ?: destinationLocation ?: GeoPoint(DEFAULT_ADDIS_LAT, DEFAULT_ADDIS_LNG)

  var zoomLevel by remember { mutableFloatStateOf(14f) }
  var centerLat by remember(initialCenter.lat, initialCenter.lng) {
    mutableStateOf(initialCenter.lat)
  }
  var centerLng by remember(initialCenter.lat, initialCenter.lng) {
    mutableStateOf(initialCenter.lng)
  }

  val animCourierLat by
    animateFloatAsState(
      targetValue = (courierLocation?.lat ?: initialCenter.lat).toFloat(),
      animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
      label = "courier_lat",
    )
  val animCourierLng by
    animateFloatAsState(
      targetValue = (courierLocation?.lng ?: initialCenter.lng).toFloat(),
      animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
      label = "courier_lng",
    )

  val monochromeFilter =
    remember {
      ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0.15f) })
    }

  BoxWithConstraints(
    modifier =
      modifier
        .fillMaxWidth()
        .height(290.dp)
        .clip(RoundedCornerShape(22.dp))
        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
        .testTag("interactive_delivery_map")
        .pointerInput(Unit) {
          detectTransformGestures { _, pan, zoom, _ ->
            val newZoom = (zoomLevel + kotlin.math.ln(zoom.toDouble()).toFloat() * 1.4f).coerceIn(11f, 17f)
            zoomLevel = newZoom
            val worldScale = TILE_PX * 2.0.pow(newZoom.toDouble())
            val curWorld = latLngToWorld(centerLat, centerLng, worldScale)
            val nextWorld = Offset(curWorld.x - pan.x, curWorld.y - pan.y)
            val nextGeo = worldToLatLng(nextWorld.x.toDouble(), nextWorld.y.toDouble(), worldScale)
            centerLat = nextGeo.lat
            centerLng = nextGeo.lng
          }
        }
  ) {
    val density = LocalDensity.current
    val widthPx = with(density) { maxWidth.toPx() }
    val heightPx = with(density) { maxHeight.toPx() }
    val intZoom = zoomLevel.roundToInt().coerceIn(11, 17)
    val tileScale = TILE_PX * 2.0.pow(intZoom.toDouble())
    val centerWorld = latLngToWorld(centerLat, centerLng, tileScale)

    // Render surrounding OSM map tiles cleanly
    val centerTileX = floor(centerWorld.x / TILE_PX).toInt()
    val centerTileY = floor(centerWorld.y / TILE_PX).toInt()
    val maxTiles = (1 shl intZoom)

    for (dx in -1..1) {
      for (dy in -1..1) {
        val tx = (centerTileX + dx).mod(maxTiles)
        val ty = (centerTileY + dy).coerceIn(0, maxTiles - 1)
        val screenX = (tx * TILE_PX - centerWorld.x + widthPx / 2.0).toFloat()
        val screenY = (ty * TILE_PX - centerWorld.y + heightPx / 2.0).toFloat()
        val tileSizeDp = with(density) { TILE_PX.toFloat().toDp() }

        AsyncImage(
          model = "https://tile.openstreetmap.org/$intZoom/$tx/$ty.png",
          imageLoader = imageLoader,
          contentDescription = null,
          contentScale = ContentScale.FillBounds,
          colorFilter = monochromeFilter,
          modifier =
            Modifier.offset { IntOffset(screenX.roundToInt(), screenY.roundToInt()) }
              .size(tileSizeDp),
        )
      }
    }

    // Subtle monochrome grid + route + markers overlay
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.16f)
    val routeColor = MaterialTheme.colorScheme.primary
    Canvas(modifier = Modifier.fillMaxSize()) {
      val step = 48.dp.toPx()
      var gx = 0f
      while (gx < size.width) {
        drawLine(gridColor, Offset(gx, 0f), Offset(gx, size.height), strokeWidth = 1f)
        gx += step
      }
      var gy = 0f
      while (gy < size.height) {
        drawLine(gridColor, Offset(0f, gy), Offset(size.width, gy), strokeWidth = 1f)
        gy += step
      }

      fun geoToScreen(pt: GeoPoint): Offset {
        val world = latLngToWorld(pt.lat, pt.lng, tileScale)
        return Offset(
          x = (world.x - centerWorld.x + size.width / 2.0).toFloat(),
          y = (world.y - centerWorld.y + size.height / 2.0).toFloat(),
        )
      }

      if (route.size >= 2) {
        val path = Path()
        route.forEachIndexed { idx, pt ->
          val p = geoToScreen(pt)
          if (idx == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
        }
        drawPath(
          path = path,
          color = routeColor,
          style =
            Stroke(
              width = 4.dp.toPx(),
              cap = StrokeCap.Round,
              pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f, 10f), 0f),
            ),
        )
      } else if (courierLocation != null && destinationLocation != null) {
        val p1 = geoToScreen(GeoPoint(animCourierLat.toDouble(), animCourierLng.toDouble()))
        val p2 = geoToScreen(destinationLocation)
        drawLine(
          color = routeColor.copy(alpha = 0.7f),
          start = p1,
          end = p2,
          strokeWidth = 3.dp.toPx(),
          pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 10f), 0f),
        )
      }
    }

    // Destination marker
    if (destinationLocation != null) {
      val destWorld = latLngToWorld(destinationLocation.lat, destinationLocation.lng, tileScale)
      val destX = (destWorld.x - centerWorld.x + widthPx / 2.0).toFloat()
      val destY = (destWorld.y - centerWorld.y + heightPx / 2.0).toFloat()
      Box(
        modifier =
          Modifier.offset {
            IntOffset((destX - 18.dp.toPx()).roundToInt(), (destY - 36.dp.toPx()).roundToInt())
          }
      ) {
        Surface(
          shape = CircleShape,
          color = MaterialTheme.colorScheme.primary,
          border = BorderStroke(2.dp, Color.White),
          shadowElevation = 4.dp,
          modifier = Modifier.size(36.dp),
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = Icons.Filled.LocationOn,
              contentDescription = strings.destinationLabel,
              tint = MaterialTheme.colorScheme.onPrimary,
              modifier = Modifier.size(20.dp),
            )
          }
        }
      }
    }

    // Animated Delivery Person marker
    if (courierLocation != null) {
      val cWorld = latLngToWorld(animCourierLat.toDouble(), animCourierLng.toDouble(), tileScale)
      val cX = (cWorld.x - centerWorld.x + widthPx / 2.0).toFloat()
      val cY = (cWorld.y - centerWorld.y + heightPx / 2.0).toFloat()
      Box(
        modifier =
          Modifier.offset {
            IntOffset((cX - 20.dp.toPx()).roundToInt(), (cY - 20.dp.toPx()).roundToInt())
          }
      ) {
        Surface(
          shape = CircleShape,
          color = StatusSuccess,
          border = BorderStroke(2.5.dp, Color.White),
          shadowElevation = 6.dp,
          modifier = Modifier.size(40.dp),
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = Icons.Filled.DeliveryDining,
              contentDescription = strings.driverLabel,
              tint = Color.White,
              modifier = Modifier.size(22.dp),
            )
          }
        }
      }
    }

    // Top-left Glass Legend
    Surface(
      shape = RoundedCornerShape(14.dp),
      color = MaterialTheme.colorScheme.surface.copy(alpha = 0.86f),
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
      modifier = Modifier.align(Alignment.TopStart).padding(10.dp),
    ) {
      Row(
        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(StatusSuccess))
          Spacer(modifier = Modifier.width(5.dp))
          Text(
            text = strings.driverLabel,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
          )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier =
              Modifier.size(8.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
          )
          Spacer(modifier = Modifier.width(5.dp))
          Text(
            text = strings.destinationLabel,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
          )
        }
      }
    }

    // Bottom-right Zoom & Recenter Controls
    Column(
      modifier = Modifier.align(Alignment.BottomEnd).padding(10.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
      ) {
        Column {
          IconButton(
            onClick = { zoomLevel = (zoomLevel + 1f).coerceAtMost(17f) },
            modifier = Modifier.size(40.dp).minimumInteractiveComponentSize().testTag("map_zoom_in"),
          ) {
            Icon(Icons.Filled.Add, contentDescription = "Zoom in", modifier = Modifier.size(18.dp))
          }
          IconButton(
            onClick = { zoomLevel = (zoomLevel - 1f).coerceAtLeast(11f) },
            modifier = Modifier.size(40.dp).minimumInteractiveComponentSize().testTag("map_zoom_out"),
          ) {
            Icon(Icons.Filled.Remove, contentDescription = "Zoom out", modifier = Modifier.size(18.dp))
          }
        }
      }
      Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
      ) {
        IconButton(
          onClick = {
            val target = courierLocation ?: destinationLocation ?: GeoPoint(DEFAULT_ADDIS_LAT, DEFAULT_ADDIS_LNG)
            centerLat = target.lat
            centerLng = target.lng
            zoomLevel = 14f
          },
          modifier = Modifier.size(40.dp).minimumInteractiveComponentSize().testTag("map_recenter"),
        ) {
          Icon(
            Icons.Filled.MyLocation,
            contentDescription = strings.recenterMap,
            modifier = Modifier.size(18.dp),
          )
        }
      }
    }
  }
}

private fun latLngToWorld(lat: Double, lng: Double, worldScale: Double): Offset {
  val clampedLat = lat.coerceIn(-85.0511, 85.0511)
  val x = (lng + 180.0) / 360.0 * worldScale
  val sinLat = kotlin.math.sin(Math.toRadians(clampedLat))
  val y = (0.5 - kotlin.math.ln((1.0 + sinLat) / (1.0 - sinLat)) / (4.0 * PI)) * worldScale
  return Offset(x.toFloat(), y.toFloat())
}

private fun worldToLatLng(x: Double, y: Double, worldScale: Double): GeoPoint {
  val lng = x / worldScale * 360.0 - 180.0
  val n = PI - 2.0 * PI * y / worldScale
  val lat = Math.toDegrees(kotlin.math.atan(kotlin.math.sinh(n)))
  return GeoPoint(lat.coerceIn(-85.0511, 85.0511), lng.coerceIn(-180.0, 180.0))
}
