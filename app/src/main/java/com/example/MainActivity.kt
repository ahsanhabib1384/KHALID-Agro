package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.core.AppLanguage
import com.example.core.LanguageManager
import com.example.core.authStateFlow
import com.example.core.configureAppCheck
import com.example.core.tr
import com.example.data.AiCropDoctorRepository
import com.example.data.MarketplaceRepository
import com.example.data.MarketplaceSeedAndAdminRepository
import com.example.model.OrderStatus
import com.example.model.UiState
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.KhalidAgroTopBar
import com.example.ui.screens.*
import com.example.ui.theme.KhalidAgroTheme
import com.example.ui.theme.PrimaryDeepGreen
import com.example.ui.theme.WarmYellow
import com.example.ui.viewmodel.CropDoctorViewModel
import com.example.ui.viewmodel.MarketplaceViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

private object Routes {
    const val HOME = "home"
    const val SHOP = "shop"
    const val LIVESTOCK = "livestock"
    const val BAZAR_PRICES = "bazar_prices"
    const val ACCOUNT = "account"
    const val CART = "cart"
    const val CROP_DOCTOR = "crop_doctor"
    const val FARMER_DASHBOARD = "farmer_dashboard"
    const val DELIVERY_DASHBOARD = "delivery_dashboard"
    const val ADMIN_CONSOLE = "admin_console"
    const val ABOUT = "about"
    const val PRODUCT_DETAIL = "product_detail/{productId}"
    const val LIVESTOCK_DETAIL = "livestock_detail/{livestockId}"
    const val NEGOTIATION = "negotiation/{negId}"

    fun productDetail(id: String) = "product_detail/$id"
    fun livestockDetail(id: String) = "livestock_detail/$id"
    fun negotiation(id: String) = "negotiation/$id"
}

private data class NavTabItem(
    val route: String,
    val labelBn: String,
    val labelEn: String,
    val icon: ImageVector,
    val testTag: String
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        configureAppCheck(this, intent)

        setContent {
            KhalidAgroTheme {
                KhalidAgroApp()
            }
        }
    }
}

@Composable
fun KhalidAgroApp() {
    val context = LocalContext.current
    val currentUser by Firebase.auth.authStateFlow().collectAsStateWithLifecycle(
        initialValue = Firebase.auth.currentUser
    )

    val user = currentUser
    if (user == null) {
        AuthScreen(onAuthSuccess = {})
        return
    }

    key(user.uid) {
        val marketplaceViewModel: MarketplaceViewModel = viewModel(
            key = "marketplace_${user.uid}",
            factory = viewModelFactory {
                initializer {
                    val appContext = context.applicationContext
                    val db = FirebaseFirestore.getInstance(
                        appContext.getString(R.string.firestore_database_id)
                    )
                    MarketplaceViewModel(
                        repo = MarketplaceRepository(db, Firebase.auth),
                        adminRepo = MarketplaceSeedAndAdminRepository(db, Firebase.auth),
                        languageManager = LanguageManager(appContext),
                        currentUserId = user.uid
                    )
                }
            }
        )

        val cropDoctorViewModel: CropDoctorViewModel = viewModel(
            key = "crop_doctor_${user.uid}",
            factory = viewModelFactory {
                initializer {
                    val appContext = context.applicationContext
                    val db = FirebaseFirestore.getInstance(
                        appContext.getString(R.string.firestore_database_id)
                    )
                    CropDoctorViewModel(
                        aiRepo = AiCropDoctorRepository(db, Firebase.auth),
                        currentUserId = user.uid
                    )
                }
            }
        )

        AuthenticatedKhalidAgroShell(
            marketplaceViewModel = marketplaceViewModel,
            cropDoctorViewModel = cropDoctorViewModel
        )
    }
}

@Composable
private fun AuthenticatedKhalidAgroShell(
    marketplaceViewModel: MarketplaceViewModel,
    cropDoctorViewModel: CropDoctorViewModel
) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }

    val lang by marketplaceViewModel.language.collectAsStateWithLifecycle()
    val searchQuery by marketplaceViewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCategory by marketplaceViewModel.selectedCategory.collectAsStateWithLifecycle()
    val organicOnly by marketplaceViewModel.organicOnly.collectAsStateWithLifecycle()
    val selectedLivestockCategory by marketplaceViewModel.selectedLivestockCategory.collectAsStateWithLifecycle()
    val statusBanner by marketplaceViewModel.statusBanner.collectAsStateWithLifecycle()
    val isBusy by marketplaceViewModel.busy.collectAsStateWithLifecycle()

    val userAccountState by marketplaceViewModel.userAccount.collectAsStateWithLifecycle()
    val productsState by marketplaceViewModel.productsState.collectAsStateWithLifecycle()
    val myProductsState by marketplaceViewModel.myProductsState.collectAsStateWithLifecycle()
    val livestockState by marketplaceViewModel.livestockState.collectAsStateWithLifecycle()
    val myLivestockState by marketplaceViewModel.myLivestockState.collectAsStateWithLifecycle()
    val marketPricesState by marketplaceViewModel.marketPricesState.collectAsStateWithLifecycle()
    val verifiedFarmersState by marketplaceViewModel.verifiedFarmersState.collectAsStateWithLifecycle()
    val cartState by marketplaceViewModel.cartState.collectAsStateWithLifecycle()
    val favoritesState by marketplaceViewModel.favoritesState.collectAsStateWithLifecycle()
    val buyerOrdersState by marketplaceViewModel.buyerOrdersState.collectAsStateWithLifecycle()
    val farmerOrdersState by marketplaceViewModel.farmerOrdersState.collectAsStateWithLifecycle()
    val deliveryOrdersState by marketplaceViewModel.deliveryOrdersState.collectAsStateWithLifecycle()
    val buyerNegotiationsState by marketplaceViewModel.buyerNegotiationsState.collectAsStateWithLifecycle()
    val farmerNegotiationsState by marketplaceViewModel.farmerNegotiationsState.collectAsStateWithLifecycle()
    val reviewsState by marketplaceViewModel.reviewsState.collectAsStateWithLifecycle()
    val settings by marketplaceViewModel.settingsState.collectAsStateWithLifecycle()

    val adminFarmersState by marketplaceViewModel.adminFarmersState.collectAsStateWithLifecycle()
    val adminOrdersState by marketplaceViewModel.adminOrdersState.collectAsStateWithLifecycle()

    val diagnosisState by cropDoctorViewModel.diagnosisState.collectAsStateWithLifecycle()
    val krishiStreamState by cropDoctorViewModel.krishiStreamState.collectAsStateWithLifecycle()
    val diagnosisHistoryState by cropDoctorViewModel.historyState.collectAsStateWithLifecycle()

    var wholesaleMode by remember { mutableStateOf(false) }

    LaunchedEffect(statusBanner) {
        val msg = statusBanner
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            marketplaceViewModel.clearBanner()
        }
    }

    val cartItems = (cartState as? UiState.Success)?.data.orEmpty()
    val cartCount = cartItems.size

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentRoute = currentDestination?.route

    val primaryTabs = remember {
        listOf(
            NavTabItem(Routes.HOME, "হোম", "Home", Icons.Filled.Home, "nav_home"),
            NavTabItem(Routes.SHOP, "বাজার", "Shop", Icons.Filled.Storefront, "nav_shop"),
            NavTabItem(Routes.LIVESTOCK, "পশুহাট", "Livestock", Icons.Filled.Pets, "nav_livestock"),
            NavTabItem(Routes.BAZAR_PRICES, "দর ও AI", "Bazar & AI", Icons.AutoMirrored.Filled.TrendingUp, "nav_bazar"),
            NavTabItem(Routes.ACCOUNT, "অ্যাকাউন্ট", "Account", Icons.Filled.Person, "nav_account")
        )
    }

    val routesWithMainTopBar = remember {
        setOf(
            Routes.HOME,
            Routes.SHOP,
            Routes.LIVESTOCK,
            Routes.BAZAR_PRICES,
            Routes.ACCOUNT,
            Routes.CART,
            Routes.CROP_DOCTOR
        )
    }
    val showMainTopBar = currentRoute in routesWithMainTopBar
    val showPrimaryNav = currentRoute in primaryTabs.map { it.route }

    fun navigateTopTab(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Row(modifier = Modifier.fillMaxSize()) {
            if (useRail && showPrimaryNav) {
                NavigationRail(
                    containerColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.statusBarsPadding().navigationBarsPadding()
                ) {
                    Spacer(Modifier.height(12.dp))
                    primaryTabs.forEach { tab ->
                        val selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true
                        NavigationRailItem(
                            selected = selected,
                            onClick = {
                                if (tab.route == Routes.SHOP) wholesaleMode = false
                                navigateTopTab(tab.route)
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.labelEn) },
                            label = {
                                Text(
                                    text = if (lang == AppLanguage.BN) tab.labelBn else tab.labelEn,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            modifier = Modifier.testTag(tab.testTag)
                        )
                    }
                }
            }

            Scaffold(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                snackbarHost = { SnackbarHost(snackbarHostState) },
                topBar = {
                    if (showMainTopBar) {
                        KhalidAgroTopBar(
                            lang = lang,
                            cartCount = cartCount,
                            onToggleLanguage = { marketplaceViewModel.toggleLanguage() },
                            onCartClick = { navController.navigate(Routes.CART) { launchSingleTop = true } },
                            onAccountClick = { navigateTopTab(Routes.ACCOUNT) },
                            onLogoClick = { navigateTopTab(Routes.HOME) }
                        )
                    }
                },
                bottomBar = {
                    if (!useRail && showPrimaryNav) {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = 4.dp
                        ) {
                            primaryTabs.forEach { tab ->
                                val selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = {
                                        if (tab.route == Routes.SHOP) wholesaleMode = false
                                        navigateTopTab(tab.route)
                                    },
                                    icon = { Icon(tab.icon, contentDescription = tab.labelEn) },
                                    label = {
                                        Text(
                                            text = if (lang == AppLanguage.BN) tab.labelBn else tab.labelEn,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = PrimaryDeepGreen,
                                        selectedTextColor = PrimaryDeepGreen,
                                        indicatorColor = WarmYellow.copy(alpha = 0.45f)
                                    ),
                                    modifier = Modifier.testTag(tab.testTag)
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                NavHost(
                    navController = navController,
                    startDestination = Routes.HOME,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    composable(Routes.HOME) {
                        HomeScreen(
                            lang = lang,
                            searchQuery = searchQuery,
                            onSearchChange = { marketplaceViewModel.setSearchQuery(it) },
                            productsState = productsState,
                            marketPricesState = marketPricesState,
                            farmersState = verifiedFarmersState,
                            reviewsState = reviewsState,
                            onOpenShop = { cat, wholesale ->
                                marketplaceViewModel.setCategory(cat)
                                wholesaleMode = wholesale
                                navigateTopTab(Routes.SHOP)
                            },
                            onOpenLivestock = { cat ->
                                marketplaceViewModel.setLivestockCategory(cat)
                                navigateTopTab(Routes.LIVESTOCK)
                            },
                            onOpenProductDetail = { id ->
                                navController.navigate(Routes.productDetail(id))
                            },
                            onQuickAddToCart = { product ->
                                marketplaceViewModel.addProductToCart(product, product.minOrderQty)
                            },
                            onOpenMarketPrices = { navigateTopTab(Routes.BAZAR_PRICES) },
                            onOpenCropDoctor = { navController.navigate(Routes.CROP_DOCTOR) },
                            onOpenFarmerJoin = { navigateTopTab(Routes.ACCOUNT) },
                            onOpenAbout = { navController.navigate(Routes.ABOUT) }
                        )
                    }

                    composable(Routes.SHOP) {
                        ShopScreen(
                            lang = lang,
                            productsState = productsState,
                            searchQuery = searchQuery,
                            selectedCategory = selectedCategory,
                            organicOnly = organicOnly,
                            wholesaleMode = wholesaleMode,
                            onSearchChange = { marketplaceViewModel.setSearchQuery(it) },
                            onCategoryChange = { marketplaceViewModel.setCategory(it) },
                            onOrganicToggle = { marketplaceViewModel.setOrganicOnly(it) },
                            onProductClick = { id -> navController.navigate(Routes.productDetail(id)) },
                            onQuickAdd = { product, qty -> marketplaceViewModel.addProductToCart(product, qty) }
                        )
                    }

                    composable(Routes.LIVESTOCK) {
                        LivestockScreen(
                            lang = lang,
                            livestockState = livestockState,
                            selectedCategory = selectedLivestockCategory,
                            onCategoryChange = { marketplaceViewModel.setLivestockCategory(it) },
                            onAnimalClick = { id -> navController.navigate(Routes.livestockDetail(id)) }
                        )
                    }

                    composable(Routes.BAZAR_PRICES) {
                        MarketPricesAndCalendarScreen(
                            lang = lang,
                            marketPricesState = marketPricesState,
                            settings = settings,
                            onOpenCropDoctor = { navController.navigate(Routes.CROP_DOCTOR) }
                        )
                    }

                    composable(Routes.CROP_DOCTOR) {
                        CropDoctorScreen(
                            lang = lang,
                            diagnosisState = diagnosisState,
                            krishiStreamState = krishiStreamState,
                            historyState = diagnosisHistoryState,
                            onAnalyzeCrop = { crop, sym, bmp ->
                                cropDoctorViewModel.analyzeCrop(crop, sym, bmp, lang)
                            },
                            onAskKrishiHelp = { q ->
                                cropDoctorViewModel.askKrishiHelp(q, lang)
                            }
                        )
                    }

                    composable(Routes.CART) {
                        CartCheckoutScreen(
                            lang = lang,
                            cartState = cartState,
                            userAccount = (userAccountState as? UiState.Success)?.data,
                            settings = settings,
                            isBusy = isBusy,
                            onUpdateQuantity = { item, newQty ->
                                marketplaceViewModel.updateCartQuantity(item, newQty)
                            },
                            onRemoveItem = { id -> marketplaceViewModel.removeFromCart(id) },
                            onPlaceOrder = { name, phone, addr, dist, pm, notes ->
                                marketplaceViewModel.placeOrder(name, phone, addr, dist, pm, notes) {
                                    navigateTopTab(Routes.ACCOUNT)
                                }
                            },
                            onBrowseShop = { navigateTopTab(Routes.SHOP) }
                        )
                    }

                    composable(Routes.ACCOUNT) {
                        AccountScreen(
                            lang = lang,
                            isAdminUser = marketplaceViewModel.isAdminUser,
                            userAccountState = userAccountState,
                            buyerOrdersState = buyerOrdersState,
                            favoritesState = favoritesState,
                            buyerNegotiationsState = buyerNegotiationsState,
                            onUpdateProfile = { name, phone, div, dist, addr, role, fName, fLoc ->
                                marketplaceViewModel.updateRoleAndProfile(
                                    name, phone, div, dist, addr, role, fName, fLoc
                                )
                            },
                            onCancelOrder = { orderId ->
                                marketplaceViewModel.updateOrderStatus(orderId, OrderStatus.CANCELLED)
                            },
                            onOpenNegotiation = { neg ->
                                navController.navigate(Routes.negotiation(neg.id))
                            },
                            onOpenFarmerDashboard = { navController.navigate(Routes.FARMER_DASHBOARD) },
                            onOpenDeliveryDashboard = { navController.navigate(Routes.DELIVERY_DASHBOARD) },
                            onOpenAdminConsole = { navController.navigate(Routes.ADMIN_CONSOLE) },
                            onOpenAbout = { navController.navigate(Routes.ABOUT) },
                            onSignedOut = {}
                        )
                    }

                    composable(
                        route = Routes.PRODUCT_DETAIL,
                        arguments = listOf(navArgument("productId") { type = NavType.StringType })
                    ) { backStack ->
                        val productId = backStack.arguments?.getString("productId").orEmpty()
                        val allProducts = (productsState as? UiState.Success)?.data.orEmpty()
                        val product = allProducts.firstOrNull { it.id == productId }
                        val reviews = (reviewsState as? UiState.Success)?.data.orEmpty()
                        val favorites = (favoritesState as? UiState.Success)?.data.orEmpty()

                        if (product == null) {
                            NotFoundBackScreen(lang = lang, onBack = { navController.popBackStack() })
                        } else {
                            ProductDetailScreen(
                                product = product,
                                allProducts = allProducts,
                                reviews = reviews,
                                favorites = favorites,
                                lang = lang,
                                onBack = { navController.popBackStack() },
                                onAddToCart = { p, q -> marketplaceViewModel.addProductToCart(p, q) },
                                onBuyNow = { p, q ->
                                    marketplaceViewModel.addProductToCart(p, q)
                                    navController.navigate(Routes.CART)
                                },
                                onToggleFavorite = { isFav ->
                                    marketplaceViewModel.toggleFavorite(
                                        targetId = product.id,
                                        targetType = "PRODUCT",
                                        titleEn = product.nameEn,
                                        titleBn = product.nameBn,
                                        subtitle = "${product.farmerName} • ${product.farmLocation}",
                                        currentlyFav = isFav
                                    )
                                },
                                onStartNegotiation = { qtyMon, offeredPrice, msg ->
                                    marketplaceViewModel.startNegotiation(
                                        product = product,
                                        requestedQtyMon = qtyMon,
                                        offeredPrice = offeredPrice,
                                        message = msg
                                    ) {
                                        navigateTopTab(Routes.ACCOUNT)
                                    }
                                },
                                onSubmitReview = { rating, comment ->
                                    marketplaceViewModel.submitReview(product.id, rating, comment)
                                },
                                onOpenRelated = { relatedId ->
                                    navController.navigate(Routes.productDetail(relatedId))
                                }
                            )
                        }
                    }

                    composable(
                        route = Routes.LIVESTOCK_DETAIL,
                        arguments = listOf(navArgument("livestockId") { type = NavType.StringType })
                    ) { backStack ->
                        val livestockId = backStack.arguments?.getString("livestockId").orEmpty()
                        val allLivestock = (livestockState as? UiState.Success)?.data.orEmpty()
                        val animal = allLivestock.firstOrNull { it.id == livestockId }

                        if (animal == null) {
                            NotFoundBackScreen(lang = lang, onBack = { navController.popBackStack() })
                        } else {
                            LivestockDetailScreen(
                                animal = animal,
                                lang = lang,
                                onBack = { navController.popBackStack() },
                                onAddToCart = { marketplaceViewModel.addLivestockToCart(it) },
                                onBuyNow = {
                                    marketplaceViewModel.addLivestockToCart(it)
                                    navController.navigate(Routes.CART)
                                }
                            )
                        }
                    }

                    composable(Routes.FARMER_DASHBOARD) {
                        FarmerDashboardScreen(
                            lang = lang,
                            userAccount = (userAccountState as? UiState.Success)?.data,
                            myProductsState = myProductsState,
                            myLivestockState = myLivestockState,
                            farmerOrdersState = farmerOrdersState,
                            farmerNegotiationsState = farmerNegotiationsState,
                            onBack = { navController.popBackStack() },
                            onSaveProduct = { product ->
                                marketplaceViewModel.saveProduct(product) {}
                            },
                            onDeleteProduct = { id -> marketplaceViewModel.deleteProduct(id) },
                            onSaveLivestock = { animal ->
                                marketplaceViewModel.saveLivestock(animal) {}
                            },
                            onUpdateOrderStatus = { orderId, status, payStatus ->
                                marketplaceViewModel.updateOrderStatus(orderId, status, payStatus)
                            },
                            onOpenNegotiation = { neg ->
                                navController.navigate(Routes.negotiation(neg.id))
                            }
                        )
                    }

                    composable(Routes.DELIVERY_DASHBOARD) {
                        DeliveryDashboardScreen(
                            lang = lang,
                            deliveryOrdersState = deliveryOrdersState,
                            onBack = { navController.popBackStack() },
                            onUpdateDelivery = { orderId, status, note ->
                                marketplaceViewModel.updateOrderStatus(
                                    orderId = orderId,
                                    status = status,
                                    confirmationNote = note
                                )
                            }
                        )
                    }

                    composable(Routes.ADMIN_CONSOLE) {
                        AdminConsoleScreen(
                            lang = lang,
                            adminFarmersState = adminFarmersState,
                            adminOrdersState = adminOrdersState,
                            marketPricesState = marketPricesState,
                            settings = settings,
                            onBack = { navController.popBackStack() },
                            onVerifyFarmer = { farmerId, ownerUid, verified ->
                                marketplaceViewModel.adminVerifyFarmer(farmerId, ownerUid, verified)
                            },
                            onAssignDeliveryAndStatus = { orderId, status, assignee ->
                                marketplaceViewModel.updateOrderStatus(
                                    orderId = orderId,
                                    status = status,
                                    deliveryAssigneeId = assignee
                                )
                            },
                            onUpdateMarketPrice = { mp ->
                                marketplaceViewModel.adminUpdateMarketPrice(mp)
                            },
                            onUpdateSettings = { updated ->
                                marketplaceViewModel.adminUpdateSettings(updated)
                            }
                        )
                    }

                    composable(
                        route = Routes.NEGOTIATION,
                        arguments = listOf(navArgument("negId") { type = NavType.StringType })
                    ) { backStack ->
                        val negId = backStack.arguments?.getString("negId").orEmpty()
                        val buyerNegs = (buyerNegotiationsState as? UiState.Success)?.data.orEmpty()
                        val farmerNegs = (farmerNegotiationsState as? UiState.Success)?.data.orEmpty()
                        val neg = (buyerNegs + farmerNegs).firstOrNull { it.id == negId }

                        if (neg == null) {
                            NotFoundBackScreen(lang = lang, onBack = { navController.popBackStack() })
                        } else {
                            val messages by remember(neg.id) {
                                marketplaceViewModel.observeMessages(neg)
                            }.collectAsStateWithLifecycle(initialValue = emptyList())

                            NegotiationChatScreen(
                                negotiation = neg,
                                messages = messages,
                                currentUserId = marketplaceViewModel.currentUserId,
                                lang = lang,
                                onBack = { navController.popBackStack() },
                                onSendMessage = { text, price, status ->
                                    marketplaceViewModel.sendNegotiationMessage(neg, text, price, status)
                                }
                            )
                        }
                    }

                    composable(Routes.ABOUT) {
                        AboutContactScreen(
                            lang = lang,
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NotFoundBackScreen(lang: AppLanguage, onBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        EmptyStateCard(
            title = tr(lang, "তথ্য পাওয়া যায়নি", "Item Unavailable"),
            subtitle = tr(lang, "অনুগ্রহ করে পূর্বের পেজে ফিরে যান।", "Please go back and select an active item.")
        )
        Spacer(Modifier.height(14.dp))
        Button(onClick = onBack, modifier = Modifier.defaultMinSize(minHeight = 48.dp)) {
            Text(tr(lang, "ফিরে যান", "Go Back"))
        }
    }
}
