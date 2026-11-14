package com.example.ui.navigation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.i18n.LocalWudeStrings
import com.example.ui.i18n.stringsFor
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.CartScreen
import com.example.ui.screens.CheckoutScreen
import com.example.ui.screens.DeliveryTrackingScreen
import com.example.ui.screens.ExploreScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.OrderDetailScreen
import com.example.ui.screens.OrdersScreen
import com.example.ui.screens.ProductDetailScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.WishlistScreen
import com.example.ui.viewmodel.UiEvent
import com.example.ui.viewmodel.WudeMartViewModel
import kotlinx.coroutines.launch

object WudeRoutes {
  const val HOME = "home"
  const val EXPLORE = "explore"
  const val CART = "cart"
  const val ORDERS = "orders"
  const val PROFILE = "profile"
  const val WISHLIST = "wishlist"
  const val PRODUCT_DETAIL = "product/{productId}"
  const val AUTH = "auth"
  const val CHECKOUT = "checkout"
  const val ORDER_DETAIL = "order/{orderId}"
  const val DELIVERY_TRACKING = "tracking/{orderId}"

  fun productDetail(productId: Long) = "product/$productId"
  fun orderDetail(orderId: Long) = "order/$orderId"
  fun deliveryTracking(orderId: Long) = "tracking/$orderId"
}

private data class TopLevelDestination(
  val route: String,
  val label: String,
  val selectedIcon: ImageVector,
  val unselectedIcon: ImageVector,
  val testTag: String,
)

@Composable
fun WudeMartAppShell(viewModel: WudeMartViewModel) {
  val navController = rememberNavController()
  val snackbarHostState = remember { SnackbarHostState() }
  val scope = rememberCoroutineScope()

  val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()
  val session by viewModel.session.collectAsStateWithLifecycle()
  val cartItems by viewModel.cartItems.collectAsStateWithLifecycle()
  val catalogState by viewModel.catalogState.collectAsStateWithLifecycle()
  val filteredProducts by viewModel.filteredProducts.collectAsStateWithLifecycle()
  val categories by viewModel.availableCategories.collectAsStateWithLifecycle()
  val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
  val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
  val sortOption by viewModel.sortOption.collectAsStateWithLifecycle()
  val onlyAvailable by viewModel.onlyAvailable.collectAsStateWithLifecycle()
  val wishlistProductIds by viewModel.wishlistProductIds.collectAsStateWithLifecycle()
  val wishlistProducts by viewModel.wishlistProducts.collectAsStateWithLifecycle()
  val productDetailState by viewModel.productDetailState.collectAsStateWithLifecycle()
  val ordersState by viewModel.ordersState.collectAsStateWithLifecycle()
  val orderDetailState by viewModel.orderDetailState.collectAsStateWithLifecycle()
  val trackingState by viewModel.trackingState.collectAsStateWithLifecycle()
  val authFormState by viewModel.authFormState.collectAsStateWithLifecycle()
  val checkoutState by viewModel.checkoutState.collectAsStateWithLifecycle()
  val deliveryProfile by viewModel.deliveryProfile.collectAsStateWithLifecycle()
  val themePreference by viewModel.themePreference.collectAsStateWithLifecycle()
  val language by viewModel.language.collectAsStateWithLifecycle()

  val strings = remember(language) { stringsFor(language) }

  var pendingCheckoutAfterAuth by rememberSaveable { mutableStateOf(false) }

  val totalCartBadge = cartItems.sumOf { it.quantity }
  val badgeScale = remember { Animatable(1f) }
  var prevBadgeCount by remember { mutableIntStateOf(totalCartBadge) }
  LaunchedEffect(totalCartBadge) {
    if (totalCartBadge > prevBadgeCount) {
      badgeScale.animateTo(1.28f, spring(dampingRatio = 0.45f, stiffness = 550f))
      badgeScale.animateTo(1f, spring(dampingRatio = 0.65f, stiffness = 400f))
    }
    prevBadgeCount = totalCartBadge
  }

  LaunchedEffect(Unit) {
    viewModel.events.collect { event ->
      when (event) {
        is UiEvent.Snackbar -> {
          snackbarHostState.showSnackbar(event.message)
        }
        is UiEvent.OrderPlaced -> {
          navController.navigate(WudeRoutes.orderDetail(event.orderId)) {
            popUpTo(WudeRoutes.CART) { inclusive = false }
          }
        }
        is UiEvent.AuthSuccess -> {
          if (pendingCheckoutAfterAuth && cartItems.isNotEmpty()) {
            pendingCheckoutAfterAuth = false
            navController.navigate(WudeRoutes.CHECKOUT) {
              popUpTo(WudeRoutes.AUTH) { inclusive = true }
            }
          } else {
            navController.popBackStack()
          }
        }
      }
    }
  }

  val topLevelDestinations =
    remember(strings) {
      listOf(
        TopLevelDestination(
          route = WudeRoutes.HOME,
          label = strings.navShop,
          selectedIcon = Icons.Filled.Home,
          unselectedIcon = Icons.Outlined.Home,
          testTag = "nav_home",
        ),
        TopLevelDestination(
          route = WudeRoutes.EXPLORE,
          label = strings.navExplore,
          selectedIcon = Icons.Filled.Search,
          unselectedIcon = Icons.Outlined.Search,
          testTag = "nav_explore",
        ),
        TopLevelDestination(
          route = WudeRoutes.CART,
          label = strings.navCart,
          selectedIcon = Icons.Filled.ShoppingCart,
          unselectedIcon = Icons.Outlined.ShoppingCart,
          testTag = "nav_cart",
        ),
        TopLevelDestination(
          route = WudeRoutes.ORDERS,
          label = strings.navOrders,
          selectedIcon = Icons.Filled.ReceiptLong,
          unselectedIcon = Icons.Outlined.ReceiptLong,
          testTag = "nav_orders",
        ),
        TopLevelDestination(
          route = WudeRoutes.PROFILE,
          label = strings.navAccount,
          selectedIcon = Icons.Filled.Person,
          unselectedIcon = Icons.Outlined.Person,
          testTag = "nav_profile",
        ),
      )
    }

  val navBackStackEntry by navController.currentBackStackEntryAsState()
  val currentDestination = navBackStackEntry?.destination
  val showTopLevelNav =
    topLevelDestinations.any { dest ->
      currentDestination?.hierarchy?.any { it.route == dest.route } == true
    }

  fun navigateToTab(route: String) {
    navController.navigate(route) {
      popUpTo(navController.graph.findStartDestination().id) {
        saveState = true
      }
      launchSingleTop = true
      restoreState = true
    }
  }

  CompositionLocalProvider(LocalWudeStrings provides strings) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
      val useNavigationRail = maxWidth >= 600.dp

      Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
          if (showTopLevelNav && !useNavigationRail) {
            NavigationBar(
              containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
              tonalElevation = 3.dp,
            ) {
              topLevelDestinations.forEach { dest ->
                val selected =
                  currentDestination?.hierarchy?.any { it.route == dest.route } == true
                val iconScale by
                  animateFloatAsState(
                    targetValue = if (selected) 1.12f else 1.0f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                    label = "nav_bar_icon_scale_${dest.route}",
                  )
                NavigationBarItem(
                  selected = selected,
                  onClick = { navigateToTab(dest.route) },
                  modifier = Modifier.testTag(dest.testTag),
                  icon = {
                    if (dest.route == WudeRoutes.CART && totalCartBadge > 0) {
                      BadgedBox(
                        badge = {
                          Badge(
                            modifier =
                              Modifier.graphicsLayer {
                                scaleX = badgeScale.value
                                scaleY = badgeScale.value
                              }
                          ) {
                            Text(if (totalCartBadge > 99) "99+" else "$totalCartBadge")
                          }
                        }
                      ) {
                        Icon(
                          imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                          contentDescription = dest.label,
                          modifier =
                            Modifier.graphicsLayer {
                              scaleX = iconScale
                              scaleY = iconScale
                            },
                        )
                      }
                    } else {
                      Icon(
                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                        contentDescription = dest.label,
                        modifier =
                          Modifier.graphicsLayer {
                            scaleX = iconScale
                            scaleY = iconScale
                          },
                      )
                    }
                  },
                  label = { Text(dest.label) },
                )
              }
            }
          }
        },
      ) { innerPadding ->
        Row(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
          if (showTopLevelNav && useNavigationRail) {
            NavigationRail(
              containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
              modifier = Modifier.statusBarsPadding(),
            ) {
              Spacer(modifier = Modifier.weight(0.2f))
              topLevelDestinations.forEach { dest ->
                val selected =
                  currentDestination?.hierarchy?.any { it.route == dest.route } == true
                val iconScale by
                  animateFloatAsState(
                    targetValue = if (selected) 1.12f else 1.0f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                    label = "nav_rail_icon_scale_${dest.route}",
                  )
                NavigationRailItem(
                  selected = selected,
                  onClick = { navigateToTab(dest.route) },
                  modifier = Modifier.testTag(dest.testTag),
                  icon = {
                    if (dest.route == WudeRoutes.CART && totalCartBadge > 0) {
                      BadgedBox(
                        badge = {
                          Badge(
                            modifier =
                              Modifier.graphicsLayer {
                                scaleX = badgeScale.value
                                scaleY = badgeScale.value
                              }
                          ) {
                            Text(if (totalCartBadge > 99) "99+" else "$totalCartBadge")
                          }
                        }
                      ) {
                        Icon(
                          imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                          contentDescription = dest.label,
                          modifier =
                            Modifier.graphicsLayer {
                              scaleX = iconScale
                              scaleY = iconScale
                            },
                        )
                      }
                    } else {
                      Icon(
                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                        contentDescription = dest.label,
                        modifier =
                          Modifier.graphicsLayer {
                            scaleX = iconScale
                            scaleY = iconScale
                          },
                      )
                    }
                  },
                  label = { Text(dest.label) },
                )
              }
              Spacer(modifier = Modifier.weight(1f))
            }
          }

          NavHost(
            navController = navController,
            startDestination = WudeRoutes.HOME,
            modifier = Modifier.weight(1f).fillMaxSize(),
            enterTransition = {
              fadeIn(animationSpec = tween(220)) +
                slideInHorizontally(animationSpec = tween(240)) { it / 12 }
            },
            exitTransition = {
              fadeOut(animationSpec = tween(180))
            },
            popEnterTransition = {
              fadeIn(animationSpec = tween(200))
            },
            popExitTransition = {
              fadeOut(animationSpec = tween(180)) +
                slideOutHorizontally(animationSpec = tween(220)) { it / 12 }
            },
          ) {
            composable(WudeRoutes.HOME) {
              HomeScreen(
                catalogState = catalogState,
                products = filteredProducts,
                categories = categories,
                selectedCategory = selectedCategory,
                wishlistIds = wishlistProductIds,
                isOnline = isOnline,
                imageLoader = viewModel.imageLoader,
                onSelectCategory = viewModel::selectCategory,
                onSearchClick = { navigateToTab(WudeRoutes.EXPLORE) },
                onWishlistClick = { navController.navigate(WudeRoutes.WISHLIST) },
                onProductClick = { id -> navController.navigate(WudeRoutes.productDetail(id)) },
                onAddToCart = { product -> viewModel.addToCart(product, 1) },
                onToggleWishlist = viewModel::toggleWishlist,
                onRefresh = viewModel::refreshCatalog,
              )
            }

            composable(WudeRoutes.EXPLORE) {
              ExploreScreen(
                catalogState = catalogState,
                products = filteredProducts,
                searchQuery = searchQuery,
                categories = categories,
                selectedCategory = selectedCategory,
                sortOption = sortOption,
                onlyAvailable = onlyAvailable,
                wishlistIds = wishlistProductIds,
                language = language,
                isOnline = isOnline,
                imageLoader = viewModel.imageLoader,
                onSearchChange = viewModel::updateSearchQuery,
                onSelectCategory = viewModel::selectCategory,
                onSelectSort = viewModel::selectSortOption,
                onToggleAvailable = viewModel::setOnlyAvailable,
                onWishlistClick = { navController.navigate(WudeRoutes.WISHLIST) },
                onProductClick = { id -> navController.navigate(WudeRoutes.productDetail(id)) },
                onAddToCart = { product -> viewModel.addToCart(product, 1) },
                onToggleWishlist = viewModel::toggleWishlist,
                onRefresh = viewModel::refreshCatalog,
              )
            }

            composable(WudeRoutes.WISHLIST) {
              WishlistScreen(
                products = wishlistProducts,
                imageLoader = viewModel.imageLoader,
                onBack = { navController.popBackStack() },
                onBrowseProducts = { navigateToTab(WudeRoutes.EXPLORE) },
                onProductClick = { id -> navController.navigate(WudeRoutes.productDetail(id)) },
                onAddToCart = { product -> viewModel.addToCart(product, 1) },
                onToggleWishlist = viewModel::toggleWishlist,
              )
            }

            composable(WudeRoutes.CART) {
              CartScreen(
                cartItems = cartItems,
                isSignedIn = session?.isSignedIn == true,
                imageLoader = viewModel.imageLoader,
                onBrowseProducts = { navigateToTab(WudeRoutes.HOME) },
                onProductClick = { id -> navController.navigate(WudeRoutes.productDetail(id)) },
                onUpdateQuantity = viewModel::updateCartQuantity,
                onRemoveItem = viewModel::removeFromCart,
                onClearCart = viewModel::clearCart,
                onProceedToCheckout = {
                  if (session?.isSignedIn == true) {
                    navController.navigate(WudeRoutes.CHECKOUT)
                  } else {
                    pendingCheckoutAfterAuth = true
                    navController.navigate(WudeRoutes.AUTH)
                  }
                },
              )
            }

            composable(WudeRoutes.ORDERS) {
              OrdersScreen(
                ordersState = ordersState,
                language = language,
                isOnline = isOnline,
                onSignInClick = {
                  pendingCheckoutAfterAuth = false
                  navController.navigate(WudeRoutes.AUTH)
                },
                onBrowseClick = { navigateToTab(WudeRoutes.HOME) },
                onOrderClick = { orderId ->
                  navController.navigate(WudeRoutes.orderDetail(orderId))
                },
                onRefresh = viewModel::refreshOrders,
              )
            }

            composable(WudeRoutes.PROFILE) {
              ProfileScreen(
                session = session,
                deliveryProfile = deliveryProfile,
                themePreference = themePreference,
                language = language,
                wishlistCount = wishlistProductIds.size,
                isDetectingLocation = checkoutState.isDetectingLocation,
                locationStatusMessage = checkoutState.locationStatusMessage,
                onSignInClick = {
                  pendingCheckoutAfterAuth = false
                  navController.navigate(WudeRoutes.AUTH)
                },
                onRefreshProfile = viewModel::refreshAccountProfile,
                onSignOut = viewModel::signOut,
                onSaveDelivery = viewModel::saveDeliveryProfile,
                onThemeChange = viewModel::setThemePreference,
                onLanguageChange = viewModel::setLanguage,
                onOpenWishlist = { navController.navigate(WudeRoutes.WISHLIST) },
                onDetectLocation = viewModel::detectOneTimeDeliveryAddress,
                onLocationDenied = viewModel::notifyLocationPermissionDenied,
                onClearLocationStatus = viewModel::clearLocationStatusMessage,
              )
            }

            composable(
              route = WudeRoutes.PRODUCT_DETAIL,
              arguments = listOf(navArgument("productId") { type = NavType.LongType }),
            ) { entry ->
              val productId = entry.arguments?.getLong("productId") ?: 0L
              LaunchedEffect(productId) { viewModel.loadProductDetail(productId) }
              ProductDetailScreen(
                uiState = productDetailState,
                isWishlisted = wishlistProductIds.contains(productId),
                imageLoader = viewModel.imageLoader,
                onBack = { navController.popBackStack() },
                onQuantityChange = viewModel::setDetailQuantity,
                onAddToCart = { product, qty -> viewModel.addToCart(product, qty) },
                onBuyNow = { product, qty ->
                  viewModel.addToCart(product, qty)
                  if (session?.isSignedIn == true) {
                    navController.navigate(WudeRoutes.CHECKOUT)
                  } else {
                    pendingCheckoutAfterAuth = true
                    navController.navigate(WudeRoutes.AUTH)
                  }
                },
                onToggleWishlist = viewModel::toggleWishlist,
                onRetry = { viewModel.loadProductDetail(productId) },
              )
            }

            composable(WudeRoutes.AUTH) {
              AuthScreen(
                formState = authFormState,
                onBack = {
                  pendingCheckoutAfterAuth = false
                  navController.popBackStack()
                },
                onSetMode = viewModel::setAuthMode,
                onSubmit = viewModel::submitAuth,
                onClearError = viewModel::clearAuthError,
              )
            }

            composable(WudeRoutes.CHECKOUT) {
              CheckoutScreen(
                cartItems = cartItems,
                savedDelivery = deliveryProfile,
                checkoutState = checkoutState,
                onBack = { navController.popBackStack() },
                onClearError = viewModel::clearCheckoutError,
                onClearLocationStatus = viewModel::clearLocationStatusMessage,
                onDetectLocation = viewModel::detectOneTimeDeliveryAddress,
                onLocationDenied = viewModel::notifyLocationPermissionDenied,
                onPlaceOrder = viewModel::placeOrder,
              )
            }

            composable(
              route = WudeRoutes.ORDER_DETAIL,
              arguments = listOf(navArgument("orderId") { type = NavType.LongType }),
            ) { entry ->
              val orderId = entry.arguments?.getLong("orderId") ?: 0L
              LaunchedEffect(orderId) { viewModel.loadOrderDetail(orderId) }
              DisposableEffect(orderId) {
                onDispose { viewModel.stopOrderDetailPolling() }
              }
              OrderDetailScreen(
                orderId = orderId,
                uiState = orderDetailState,
                language = language,
                savedDelivery = deliveryProfile,
                isDetectingLocation = checkoutState.isDetectingLocation,
                locationStatusMessage = checkoutState.locationStatusMessage,
                imageLoader = viewModel.imageLoader,
                onBack = { navController.popBackStack() },
                onRefresh = { viewModel.refreshOrderDetail(orderId) },
                onSelectPaymentMethod = viewModel::selectPaymentMethod,
                onDismissWaitingView = viewModel::dismissWaitingApprovalView,
                onUploadProof = { uri, onSuccessClearUri ->
                  viewModel.uploadPaymentScreenshot(orderId, uri, onSuccessClearUri)
                },
                onDetectLocation = viewModel::detectOneTimeDeliveryAddress,
                onLocationDenied = viewModel::notifyLocationPermissionDenied,
                onRequestDeliveryQuote = { city, subCity, addressLine, lat, lng, force ->
                  viewModel.requestDeliveryQuote(
                    orderId = orderId,
                    subtotal = orderDetailState.order?.effectiveProductSubtotal ?: 0.0,
                    city = city,
                    subCity = subCity,
                    addressLine = addressLine,
                    lat = lat,
                    lng = lng,
                    force = force,
                  )
                },
                onClearDeliveryQuote = viewModel::clearDeliveryQuote,
                onSubmitFulfillment = { method, profile, note ->
                  viewModel.submitFulfillmentChoice(orderId, method, profile, note)
                },
                onTrackDelivery = {
                  navController.navigate(WudeRoutes.deliveryTracking(orderId))
                },
                onCopiedMessage = { msg ->
                  scope.launch { snackbarHostState.showSnackbar(msg) }
                },
              )
            }

            composable(
              route = WudeRoutes.DELIVERY_TRACKING,
              arguments = listOf(navArgument("orderId") { type = NavType.LongType }),
            ) { entry ->
              val orderId = entry.arguments?.getLong("orderId") ?: 0L
              LaunchedEffect(orderId) { viewModel.loadDeliveryTracking(orderId) }
              DisposableEffect(orderId) {
                onDispose { viewModel.stopDeliveryTrackingPolling() }
              }
              DeliveryTrackingScreen(
                orderId = orderId,
                uiState = trackingState,
                imageLoader = viewModel.imageLoader,
                onBack = { navController.popBackStack() },
                onRefresh = { viewModel.refreshDeliveryTracking(orderId) },
              )
            }
          }
        }
      }
    }
  }
}
