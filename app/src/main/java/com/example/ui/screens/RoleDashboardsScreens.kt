package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.AppLanguage
import com.example.core.WeightUtils
import com.example.core.tr
import com.example.model.*
import com.example.ui.components.EmptyStateCard
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FarmerDashboardScreen(
    lang: AppLanguage,
    userAccount: UserAccount?,
    myProductsState: UiState<List<Product>>,
    myLivestockState: UiState<List<Livestock>>,
    farmerOrdersState: UiState<List<Order>>,
    farmerNegotiationsState: UiState<List<Negotiation>>,
    onBack: () -> Unit,
    onSaveProduct: (Product) -> Unit,
    onDeleteProduct: (String) -> Unit,
    onSaveLivestock: (Livestock) -> Unit,
    onUpdateOrderStatus: (String, OrderStatus, PaymentStatus?) -> Unit,
    onOpenNegotiation: (Negotiation) -> Unit
) {
    var tab by remember { mutableIntStateOf(0) }
    var showAddProduct by remember { mutableStateOf(false) }
    var showAddLivestock by remember { mutableStateOf(false) }

    val products = (myProductsState as? UiState.Success)?.data.orEmpty()
    val livestock = (myLivestockState as? UiState.Success)?.data.orEmpty()
    val orders = (farmerOrdersState as? UiState.Success)?.data.orEmpty()
    val negotiations = (farmerNegotiationsState as? UiState.Success)?.data.orEmpty()
    val totalEarnings = orders.filter { it.orderStatusEnum() != OrderStatus.CANCELLED }.sumOf { it.subtotal }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(tr(lang, "কৃষক / বিক্রেতা ড্যাশবোর্ড", "Farmer Seller Dashboard")) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { if (tab == 1) showAddLivestock = true else showAddProduct = true },
                containerColor = PrimaryDeepGreen,
                contentColor = WarmYellow
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text(if (tab == 1) tr(lang, "পশু যোগ", "Add Livestock") else tr(lang, "নতুন পণ্য", "Add Product"))
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).background(BgCream)) {
            Card(
                colors = CardDefaults.cardColors(containerColor = LightGreen),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(tr(lang, "মোট আয় (০% কমিশন)", "Total Earnings (0% Fee)"), style = MaterialTheme.typography.labelMedium, color = PrimaryDeepGreen)
                        Text(WeightUtils.formatTaka(totalEarnings), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, color = PrimaryDeepGreen)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(tr(lang, "পণ্য: ${products.size} • অর্ডার: ${orders.size}", "Products: ${products.size} • Orders: ${orders.size}"), style = MaterialTheme.typography.labelMedium)
                        Text(
                            text = if (userAccount?.verifiedFarmer == true) tr(lang, "ভেরিফাইড কৃষক ✓", "Verified Farmer ✓") else tr(lang, "অনুমোদন অপেক্ষমান", "Pending Verification"),
                            style = MaterialTheme.typography.labelSmall,
                            color = SecondaryGreen,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            ScrollableTabRow(selectedTabIndex = tab, edgePadding = 12.dp) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text(tr(lang, "আমার ফসল (${products.size})", "Products (${products.size})")) })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text(tr(lang, "গবাদিপশু (${livestock.size})", "Livestock (${livestock.size})")) })
                Tab(selected = tab == 2, onClick = { tab = 2 }, text = { Text(tr(lang, "অর্ডার (${orders.size})", "Orders (${orders.size})")) })
                Tab(selected = tab == 3, onClick = { tab = 3 }, text = { Text(tr(lang, "দরদাম (${negotiations.size})", "Offers (${negotiations.size})")) })
            }

            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                when (tab) {
                    0 -> {
                        if (products.isEmpty()) item { EmptyStateCard(tr(lang, "কোনো পণ্য যুক্ত নেই", "No products listed"), tr(lang, "+ বাটনে ট্যাপ করে প্রথম পণ্য যুক্ত করুন", "Tap + to list your harvest.")) }
                        items(products, key = { it.id }) { p ->
                            Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = SurfaceWhite), border = BorderStroke(1.dp, OutlineGreen)) {
                                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("${p.nameBn} (${p.nameEn})", fontWeight = FontWeight.Bold)
                                        Text("${WeightUtils.formatTaka(p.pricePerUnit)} / ${p.unit} • Stock: ${WeightUtils.trimDecimal(p.stockQty)} • ${p.status}", style = MaterialTheme.typography.bodySmall, color = TextMutedGreen)
                                    }
                                    IconButton(onClick = { onDeleteProduct(p.id) }) {
                                        Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete", tint = ErrorRed)
                                    }
                                }
                            }
                        }
                    }
                    1 -> {
                        if (livestock.isEmpty()) item { EmptyStateCard(tr(lang, "কোনো গবাদিপশু নেই", "No livestock listed"), tr(lang, "+ বাটনে ট্যাপ করে যুক্ত করুন", "Tap + to add cattle, goat, or poultry.")) }
                        items(livestock, key = { it.id }) { a ->
                            Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = SurfaceWhite)) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("${a.animalNameBn} (${a.breed})", fontWeight = FontWeight.Bold)
                                    Text("${WeightUtils.formatTaka(a.price)} • ${WeightUtils.trimDecimal(a.liveWeightKg)} KG • ${a.status}", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                    2 -> {
                        if (orders.isEmpty()) item { EmptyStateCard(tr(lang, "কোনো অর্ডার আসেনি", "No incoming orders yet"), tr(lang, "ক্রেতাদের অর্ডার এখানে দেখা যাবে", "Incoming buyer orders will appear here.")) }
                        items(orders, key = { it.id }) { o ->
                            Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = SurfaceWhite), border = BorderStroke(1.dp, OutlineGreen)) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("#${o.id.takeLast(6).uppercase()} • ${o.buyerName} (${o.buyerPhone})", fontWeight = FontWeight.Bold)
                                    Text(o.itemsSummary, style = MaterialTheme.typography.bodySmall)
                                    Text("${WeightUtils.formatTaka(o.totalAmount)} • ${o.orderStatus}", color = PrimaryDeepGreen, fontWeight = FontWeight.Bold)
                                    if (o.orderStatusEnum() != OrderStatus.DELIVERED && o.orderStatusEnum() != OrderStatus.CANCELLED) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            OutlinedButton(onClick = { onUpdateOrderStatus(o.id, OrderStatus.CONFIRMED, null) }) { Text("Confirm") }
                                            OutlinedButton(onClick = { onUpdateOrderStatus(o.id, OrderStatus.PACKED, null) }) { Text("Packed") }
                                            Button(onClick = { onUpdateOrderStatus(o.id, OrderStatus.OUT_FOR_DELIVERY, PaymentStatus.PAID) }) { Text("Dispatch") }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    else -> {
                        if (negotiations.isEmpty()) item { EmptyStateCard(tr(lang, "কোনো দাম প্রস্তাব নেই", "No price offers yet"), tr(lang, "পাইকার বা ক্রেতাদের প্রস্তাব এখানে দেখা যাবে", "Buyer price offers appear here.")) }
                        items(negotiations, key = { it.id }) { neg ->
                            Card(onClick = { onOpenNegotiation(neg) }, shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = SurfaceWhite)) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("${neg.productTitle} — ${neg.buyerName}", fontWeight = FontWeight.Bold)
                                    Text("Offer: ${WeightUtils.formatTaka(neg.offeredPricePerUnit)} (${neg.requestedQtyMon} Mon) • ${neg.status}", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddProduct) {
        AddProductDialog(
            farmerName = userAccount?.displayName ?: "Farmer",
            farmLocation = userAccount?.farmLocation?.ifBlank { "Chuadanga, Khulna" } ?: "Chuadanga, Khulna",
            onDismiss = { showAddProduct = false },
            onSave = {
                onSaveProduct(it)
                showAddProduct = false
            }
        )
    }

    if (showAddLivestock) {
        AddLivestockDialog(
            farmerName = userAccount?.displayName ?: "Farmer",
            farmLocation = userAccount?.farmLocation?.ifBlank { "Chuadanga, Khulna" } ?: "Chuadanga, Khulna",
            onDismiss = { showAddLivestock = false },
            onSave = {
                onSaveLivestock(it)
                showAddLivestock = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeliveryDashboardScreen(
    lang: AppLanguage,
    deliveryOrdersState: UiState<List<Order>>,
    onBack: () -> Unit,
    onUpdateDelivery: (String, OrderStatus, String) -> Unit
) {
    val context = LocalContext.current
    val orders = (deliveryOrdersState as? UiState.Success)?.data.orEmpty()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(tr(lang, "ডেলিভারি ড্যাশবোর্ড", "Delivery Partner Dashboard")) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).background(BgCream),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (orders.isEmpty()) {
                item {
                    EmptyStateCard(
                        title = tr(lang, "কোনো ডেলিভারি নির্ধারিত নেই", "No assigned deliveries"),
                        subtitle = tr(lang, "সুপার অ্যাডমিন বা কৃষক আপনার আইডিতে অর্ডার অ্যাসাইন করলে এখানে দেখা যাবে।", "Assigned orders from Khalid Agro appear here.")
                    )
                }
            } else {
                items(orders, key = { it.id }) { o ->
                    var note by remember { mutableStateOf("") }
                    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = SurfaceWhite), border = BorderStroke(1.dp, OutlineGreen)) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("#${o.id.takeLast(6).uppercase()} • ${o.buyerName}", fontWeight = FontWeight.Bold)
                            Text("${o.deliveryAddress}, ${o.district}", style = MaterialTheme.typography.bodySmall)
                            Text("${o.itemsSummary} (${WeightUtils.formatTaka(o.totalAmount)})", style = MaterialTheme.typography.bodySmall)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${o.buyerPhone}"))) }) {
                                    Icon(Icons.Filled.Phone, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(tr(lang, "কল", "Call Buyer"))
                                }
                                if (o.orderStatusEnum() != OrderStatus.DELIVERED) {
                                    Button(onClick = { onUpdateDelivery(o.id, OrderStatus.DELIVERED, note.ifBlank { "Delivered safely" }) }) {
                                        Text(tr(lang, "ডেলিভারি সম্পন্ন", "Mark Delivered"))
                                    }
                                }
                            }
                            if (o.orderStatusEnum() != OrderStatus.DELIVERED) {
                                OutlinedTextField(
                                    value = note,
                                    onValueChange = { note = it },
                                    label = { Text(tr(lang, "ডেলিভারি কনফার্মেশন নোট", "Delivery confirmation note")) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminConsoleScreen(
    lang: AppLanguage,
    adminFarmersState: UiState<List<FarmerProfile>>,
    adminOrdersState: UiState<List<Order>>,
    marketPricesState: UiState<List<MarketPrice>>,
    settings: MarketplaceSettings,
    onBack: () -> Unit,
    onVerifyFarmer: (String, String, Boolean) -> Unit,
    onAssignDeliveryAndStatus: (String, OrderStatus, String?) -> Unit,
    onUpdateMarketPrice: (MarketPrice) -> Unit,
    onUpdateSettings: (MarketplaceSettings) -> Unit
) {
    var tab by remember { mutableIntStateOf(0) }
    val farmers = (adminFarmersState as? UiState.Success)?.data.orEmpty()
    val orders = (adminOrdersState as? UiState.Success)?.data.orEmpty()
    val prices = (marketPricesState as? UiState.Success)?.data.orEmpty()

    var commissionInput by remember(settings) { mutableStateOf(settings.commissionPct.toString()) }
    var deliveryFeeInput by remember(settings) { mutableStateOf(settings.baseDeliveryCharge.toInt().toString()) }
    var freeMinInput by remember(settings) { mutableStateOf(settings.freeDeliveryMinAmount.toInt().toString()) }
    var showAddPriceDialog by remember { mutableStateOf(false) }

    val totalGmv = orders.filter { it.orderStatusEnum() != OrderStatus.CANCELLED }.sumOf { it.totalAmount }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(tr(lang, "সুপার অ্যাডমিন কনসোল", "Super Admin Console")) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).background(BgCream)) {
            Card(colors = CardDefaults.cardColors(containerColor = PrimaryDeepGreen), modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("GMV / Total Sales", style = MaterialTheme.typography.labelSmall, color = LightGreen)
                        Text(WeightUtils.formatTaka(totalGmv), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = WarmYellow)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Orders: ${orders.size}", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                        Text("Commission: ${settings.commissionPct}%", color = LightGreen, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            ScrollableTabRow(selectedTabIndex = tab, edgePadding = 12.dp) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Farmers (${farmers.size})") })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Orders (${orders.size})") })
                Tab(selected = tab == 2, onClick = { tab = 2 }, text = { Text("Bazar Dor (${prices.size})") })
                Tab(selected = tab == 3, onClick = { tab = 3 }, text = { Text("Settings") })
            }

            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) {
                when (tab) {
                    0 -> {
                        items(farmers, key = { it.id }) { f ->
                            Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = SurfaceWhite)) {
                                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("${f.name} — ${f.farmName}", fontWeight = FontWeight.Bold)
                                        Text("${f.location} • Verified: ${f.verified}", style = MaterialTheme.typography.bodySmall)
                                    }
                                    Button(onClick = { onVerifyFarmer(f.id, f.ownerUid, !f.verified) }) {
                                        Text(if (f.verified) "Revoke" else "Approve")
                                    }
                                }
                            }
                        }
                    }
                    1 -> {
                        items(orders, key = { it.id }) { o ->
                            var assignee by remember { mutableStateOf(o.deliveryAssigneeId) }
                            Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = SurfaceWhite)) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("#${o.id.takeLast(6).uppercase()} • ${o.buyerName} -> ${o.farmerName}", fontWeight = FontWeight.Bold)
                                    Text("${o.itemsSummary} • ${WeightUtils.formatTaka(o.totalAmount)} • ${o.orderStatus}", style = MaterialTheme.typography.bodySmall)
                                    OutlinedTextField(
                                        value = assignee,
                                        onValueChange = { assignee = it },
                                        label = { Text("Delivery Assignee UID") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(onClick = { onAssignDeliveryAndStatus(o.id, OrderStatus.OUT_FOR_DELIVERY, assignee.ifBlank { null }) }) {
                                            Text("Assign & Dispatch")
                                        }
                                        OutlinedButton(onClick = { onAssignDeliveryAndStatus(o.id, OrderStatus.DELIVERED, null) }) {
                                            Text("Mark Delivered")
                                        }
                                    }
                                }
                            }
                        }
                    }
                    2 -> {
                        item {
                            Button(onClick = { showAddPriceDialog = true }, modifier = Modifier.fillMaxWidth()) {
                                Text(tr(lang, "নতুন বাজার দর যোগ করুন", "Add Verified Bazar Dor"))
                            }
                        }
                        items(prices, key = { it.id }) { mp ->
                            var newPrice by remember(mp.currentPrice) { mutableStateOf(mp.currentPrice.toInt().toString()) }
                            Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = SurfaceWhite)) {
                                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("${mp.productNameBn} (${mp.productNameEn})", fontWeight = FontWeight.Bold)
                                        Text("Unit: ${mp.unit} • ${mp.marketLocation}", style = MaterialTheme.typography.bodySmall)
                                    }
                                    OutlinedTextField(value = newPrice, onValueChange = { newPrice = it }, singleLine = true, modifier = Modifier.width(96.dp))
                                    Button(onClick = {
                                        val p = newPrice.toDoubleOrNull() ?: mp.currentPrice
                                        val mv = when {
                                            p > mp.currentPrice -> PriceMovement.UP.name
                                            p < mp.currentPrice -> PriceMovement.DOWN.name
                                            else -> PriceMovement.STABLE.name
                                        }
                                        onUpdateMarketPrice(mp.copy(previousPrice = mp.currentPrice, currentPrice = p, movement = mv))
                                    }) { Text("Save") }
                                }
                            }
                        }
                    }
                    else -> {
                        item {
                            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = SurfaceWhite)) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    OutlinedTextField(value = commissionInput, onValueChange = { commissionInput = it }, label = { Text("Commission % (Default 0%)") }, modifier = Modifier.fillMaxWidth())
                                    OutlinedTextField(value = deliveryFeeInput, onValueChange = { deliveryFeeInput = it }, label = { Text("Base Delivery Charge (৳)") }, modifier = Modifier.fillMaxWidth())
                                    OutlinedTextField(value = freeMinInput, onValueChange = { freeMinInput = it }, label = { Text("Free Delivery Threshold (৳)") }, modifier = Modifier.fillMaxWidth())
                                    Button(
                                        onClick = {
                                            onUpdateSettings(
                                                settings.copy(
                                                    commissionPct = commissionInput.toDoubleOrNull() ?: 0.0,
                                                    baseDeliveryCharge = deliveryFeeInput.toDoubleOrNull() ?: 120.0,
                                                    freeDeliveryMinAmount = freeMinInput.toDoubleOrNull() ?: 5000.0
                                                )
                                            )
                                        },
                                        modifier = Modifier.fillMaxWidth().testTag("admin_save_settings_btn")
                                    ) { Text("Save Marketplace Settings") }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddPriceDialog) {
        var nameEn by remember { mutableStateOf("") }
        var nameBn by remember { mutableStateOf("") }
        var priceText by remember { mutableStateOf("") }
        var unit by remember { mutableStateOf("KG") }
        AlertDialog(
            onDismissRequest = { showAddPriceDialog = false },
            title = { Text("Add Verified Bazar Price") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = nameEn, onValueChange = { nameEn = it }, label = { Text("English Name") })
                    OutlinedTextField(value = nameBn, onValueChange = { nameBn = it }, label = { Text("Bengali Name") })
                    OutlinedTextField(value = priceText, onValueChange = { priceText = it }, label = { Text("Price (৳)") })
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = unit == "KG", onClick = { unit = "KG" }, label = { Text("KG") })
                        FilterChip(selected = unit == "MON", onClick = { unit = "MON" }, label = { Text("MON") })
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val p = priceText.toDoubleOrNull() ?: 0.0
                    if (nameEn.isNotBlank() && nameBn.isNotBlank() && p > 0) {
                        onUpdateMarketPrice(
                            MarketPrice(
                                productNameEn = nameEn,
                                productNameBn = nameBn,
                                category = "FRESH_CROPS",
                                currentPrice = p,
                                previousPrice = p,
                                unit = unit,
                                movement = PriceMovement.STABLE.name,
                                marketLocation = "Chuadanga Sadar Bazar",
                                verifiedBy = "Ahsan Habib Nasim",
                                active = true
                            )
                        )
                        showAddPriceDialog = false
                    }
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showAddPriceDialog = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun AddProductDialog(
    farmerName: String,
    farmLocation: String,
    onDismiss: () -> Unit,
    onSave: (Product) -> Unit
) {
    var nameEn by remember { mutableStateOf("") }
    var nameBn by remember { mutableStateOf("") }
    var priceText by remember { mutableStateOf("") }
    var wholesaleText by remember { mutableStateOf("") }
    var stockText by remember { mutableStateOf("20") }
    var unit by remember { mutableStateOf(ProductUnit.MON) }
    var category by remember { mutableStateOf(ProductCategory.FRESH_CROPS) }
    var organic by remember { mutableStateOf(true) }
    var imageUri by remember { mutableStateOf("") }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        if (uri != null) imageUri = uri.toString()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Farm Product") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.heightIn(max = 400.dp)) {
                item {
                    OutlinedTextField(value = nameEn, onValueChange = { nameEn = it }, label = { Text("English Name") })
                    OutlinedTextField(value = nameBn, onValueChange = { nameBn = it }, label = { Text("Bengali Name") })
                    OutlinedTextField(value = priceText, onValueChange = { priceText = it }, label = { Text("Price per Unit (৳)") })
                    OutlinedTextField(value = wholesaleText, onValueChange = { wholesaleText = it }, label = { Text("Wholesale Price (৳)") })
                    OutlinedTextField(value = stockText, onValueChange = { stockText = it }, label = { Text("Available Stock Qty") })
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = organic, onCheckedChange = { organic = it })
                        Text("Organic Produce")
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(ProductUnit.MON, ProductUnit.KG, ProductUnit.BAG).forEach { u ->
                            FilterChip(selected = unit == u, onClick = { unit = u }, label = { Text(u.name) })
                        }
                    }
                    OutlinedButton(onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
                        Text(if (imageUri.isBlank()) "Attach Photo (Optional)" else "Photo Attached ✓")
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val p = priceText.toDoubleOrNull() ?: 0.0
                val w = wholesaleText.toDoubleOrNull() ?: (p * 0.92)
                val s = stockText.toDoubleOrNull() ?: 10.0
                if (nameEn.isNotBlank() && nameBn.isNotBlank() && p > 0) {
                    val monPrice = if (unit == ProductUnit.KG) p * 40.0 else p
                    onSave(
                        Product(
                            farmerName = farmerName,
                            farmLocation = farmLocation,
                            nameEn = nameEn,
                            nameBn = nameBn,
                            category = category.name,
                            descriptionEn = "Freshly listed by $farmerName from $farmLocation.",
                            descriptionBn = "$farmLocation থেকে সরাসরি কৃষকের পণ্য।",
                            imageKey = "crops_harvest",
                            imageUri = imageUri,
                            unit = unit.name,
                            organic = organic,
                            pricePerUnit = p,
                            pricePerMon = monPrice,
                            wholesalePricePerUnit = w,
                            stockQty = s,
                            harvestFreshness = HarvestFreshness.TODAY.name
                        )
                    )
                }
            }) { Text("Publish") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun AddLivestockDialog(
    farmerName: String,
    farmLocation: String,
    onDismiss: () -> Unit,
    onSave: (Livestock) -> Unit
) {
    var nameEn by remember { mutableStateOf("") }
    var nameBn by remember { mutableStateOf("") }
    var breed by remember { mutableStateOf("Deshi") }
    var weightText by remember { mutableStateOf("180") }
    var priceText by remember { mutableStateOf("") }
    var vax by remember { mutableStateOf("FMD & Anthrax Vaccinated") }
    var category by remember { mutableStateOf(LivestockCategory.COW) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Livestock Listing") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = nameEn, onValueChange = { nameEn = it }, label = { Text("Animal Title (EN)") })
                OutlinedTextField(value = nameBn, onValueChange = { nameBn = it }, label = { Text("Animal Title (BN)") })
                OutlinedTextField(value = breed, onValueChange = { breed = it }, label = { Text("Breed") })
                OutlinedTextField(value = weightText, onValueChange = { weightText = it }, label = { Text("Live Weight (KG)") })
                OutlinedTextField(value = priceText, onValueChange = { priceText = it }, label = { Text("Price (৳)") })
                OutlinedTextField(value = vax, onValueChange = { vax = it }, label = { Text("Vaccination History") })
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(LivestockCategory.COW, LivestockCategory.GOAT, LivestockCategory.CHICKEN).forEach { c ->
                        FilterChip(selected = category == c, onClick = { category = c }, label = { Text(c.en) })
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val p = priceText.toDoubleOrNull() ?: 0.0
                val w = weightText.toDoubleOrNull() ?: 50.0
                if (nameEn.isNotBlank() && nameBn.isNotBlank() && p > 0) {
                    onSave(
                        Livestock(
                            farmerName = farmerName,
                            farmLocation = farmLocation,
                            animalNameEn = nameEn,
                            animalNameBn = nameBn,
                            category = category.name,
                            breed = breed,
                            liveWeightKg = w,
                            estimatedWeightDate = "Today",
                            price = p,
                            wholesalePrice = p * 0.95,
                            vaccinationHistory = vax,
                            healthInfo = "Healthy farm-raised animal",
                            imageKey = "livestock_cattle"
                        )
                    )
                }
            }) { Text("Publish") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
