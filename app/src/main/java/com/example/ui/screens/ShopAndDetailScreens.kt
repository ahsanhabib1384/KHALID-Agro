package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.AppLanguage
import com.example.core.WeightUtils
import com.example.core.tr
import com.example.model.*
import com.example.ui.components.*
import com.example.ui.theme.*

@Composable
fun ShopScreen(
    lang: AppLanguage,
    productsState: UiState<List<Product>>,
    searchQuery: String,
    selectedCategory: ProductCategory?,
    organicOnly: Boolean,
    wholesaleMode: Boolean,
    onSearchChange: (String) -> Unit,
    onCategoryChange: (ProductCategory?) -> Unit,
    onOrganicToggle: (Boolean) -> Unit,
    onProductClick: (String) -> Unit,
    onQuickAdd: (Product, Double) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (wholesaleMode) tr(lang, "পাইকারি বাজার (৫+ মণ)", "Wholesale Market (5+ Mon)")
                    else tr(lang, "কৃষি পণ্যের বাজার", "Khalid Agro Shop"),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                FilterChip(
                    selected = organicOnly,
                    onClick = { onOrganicToggle(!organicOnly) },
                    label = { Text(tr(lang, "অর্গানিক", "Organic")) },
                    leadingIcon = { Icon(Icons.Filled.Eco, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
            }

            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                placeholder = { Text(tr(lang, "পণ্য বা এলাকা খুঁজুন...", "Search crops, seeds, tools...")) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().testTag("shop_search_field")
            )
            Spacer(Modifier.height(8.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = selectedCategory == null,
                        onClick = { onCategoryChange(null) },
                        label = { Text(tr(lang, "সব পণ্য", "All")) }
                    )
                }
                items(ProductCategory.entries) { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { onCategoryChange(cat) },
                        label = { Text(if (lang == AppLanguage.BN) cat.bn else cat.en) }
                    )
                }
            }
        }

        when (productsState) {
            is UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            is UiState.Error -> ErrorRetryCard(productsState.message, Modifier.padding(16.dp))
            is UiState.Success -> {
                val filtered = remember(productsState.data, searchQuery, selectedCategory, organicOnly) {
                    productsState.data.filter { p ->
                        val catOk = selectedCategory == null || p.categoryEnum() == selectedCategory
                        val orgOk = !organicOnly || p.organic
                        val queryOk = searchQuery.isBlank() ||
                            p.nameEn.contains(searchQuery, true) ||
                            p.nameBn.contains(searchQuery, true) ||
                            p.farmLocation.contains(searchQuery, true)
                        catOk && orgOk && queryOk && p.stockQty > 0
                    }
                }

                if (filtered.isEmpty()) {
                    EmptyStateCard(
                        title = tr(lang, "কোনো পণ্য পাওয়া যায়নি", "No matching products found"),
                        subtitle = tr(lang, "ফিল্টার পরিবর্তন করে আবার চেষ্টা করুন।", "Try clearing filters or searching another crop."),
                        modifier = Modifier.padding(16.dp)
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filtered, key = { it.id }) { product ->
                            val qtyToAdd = if (wholesaleMode) product.wholesaleMinMon else product.minOrderQty
                            ProductCard(
                                product = product,
                                lang = lang,
                                onClick = { onProductClick(product.id) },
                                onQuickAdd = { onQuickAdd(product, qtyToAdd) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailScreen(
    product: Product,
    allProducts: List<Product>,
    reviews: List<Review>,
    favorites: List<FavoriteItem>,
    lang: AppLanguage,
    onBack: () -> Unit,
    onAddToCart: (Product, Double) -> Unit,
    onBuyNow: (Product, Double) -> Unit,
    onToggleFavorite: (Boolean) -> Unit,
    onStartNegotiation: (Double, Double, String) -> Unit,
    onSubmitReview: (Long, String) -> Unit,
    onOpenRelated: (String) -> Unit
) {
    val context = LocalContext.current
    var quantity by remember(product.id) { mutableStateOf(product.minOrderQty.coerceAtLeast(1.0)) }
    var showNegDialog by remember { mutableStateOf(false) }
    var reviewRating by remember { mutableStateOf(5L) }
    var reviewComment by remember { mutableStateOf("") }

    val isFav = favorites.any { it.targetId == product.id }
    val wholesaleApplied = WeightUtils.isWholesaleEligible(quantity, product.unit, product.wholesaleMinMon)
    val activeUnitPrice = if (wholesaleApplied) product.wholesalePricePerUnit else product.pricePerUnit
    val lineTotal = activeUnitPrice * quantity
    val unitLabel = if (lang == AppLanguage.BN) product.unitEnum().bn else product.unitEnum().en
    val title = if (lang == AppLanguage.BN) product.nameBn else product.nameEn
    val description = if (lang == AppLanguage.BN) product.descriptionBn else product.descriptionEn

    val productReviews = remember(reviews, product.id) { reviews.filter { it.productId == product.id } }
    val related = remember(allProducts, product.id) {
        allProducts.filter { it.id != product.id && it.category == product.category }.take(4)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("detail_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { onToggleFavorite(isFav) }, modifier = Modifier.testTag("detail_favorite_button")) {
                        Icon(
                            imageVector = if (isFav) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (isFav) ErrorRed else PrimaryDeepGreen
                        )
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                ListingImage(
                    imageKey = product.imageKey,
                    imageUri = product.imageUri,
                    contentDescription = title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .clip(RoundedCornerShape(20.dp))
                )
            }

            item {
                Column {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AssistChip(onClick = {}, label = { Text(if (lang == AppLanguage.BN) product.gradeEnum().bn else product.gradeEnum().en) })
                        AssistChip(onClick = {}, label = { Text(if (lang == AppLanguage.BN) product.freshnessEnum().bn else product.freshnessEnum().en) })
                        if (product.organic) {
                            AssistChip(onClick = {}, label = { Text(tr(lang, "অর্গানিক", "Organic")) })
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "${product.farmerName} ✓ • ${product.farmLocation}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SecondaryGreen,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(10.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            Text(
                                text = "${WeightUtils.formatTaka(activeUnitPrice)} / $unitLabel",
                                style = MaterialTheme.typography.displaySmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = PrimaryDeepGreen
                            )
                            Text(
                                text = "${tr(lang, "১ মণ (৪০ কেজি) মূল্য:", "Price per Mon (40 KG):")} ${WeightUtils.formatTaka(product.pricePerMon)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMutedGreen
                            )
                        }
                        Surface(
                            color = LightGreen,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.End) {
                                Text(tr(lang, "পাইকারি মূল্য (${WeightUtils.trimDecimal(product.wholesaleMinMon)}+ মণ)", "Wholesale (${WeightUtils.trimDecimal(product.wholesaleMinMon)}+ Mon)"), style = MaterialTheme.typography.labelSmall, color = PrimaryDeepGreen)
                                Text(WeightUtils.formatTaka(product.wholesalePricePerUnit), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = PrimaryDeepGreen)
                            }
                        }
                    }
                }
            }

            // Weight & Quantity Converter Card (1 Mon = 40 KG, 2 Mon = 80 KG, 5 Mon = 200 KG)
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                    border = BorderStroke(1.dp, OutlineGreen)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = tr(lang, "পরিমাণ ও ওজন ক্যালকুলেটর (১ মণ = ৪০ কেজি)", "Quantity & Weight Converter (1 Mon = 40 KG)"),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                FilledTonalIconButton(
                                    onClick = { if (quantity > 1.0) quantity -= 1.0 },
                                    modifier = Modifier.size(48.dp)
                                ) { Icon(Icons.Filled.Remove, contentDescription = "Decrease") }
                                Text(
                                    text = "${WeightUtils.trimDecimal(quantity)} $unitLabel",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 14.dp)
                                )
                                FilledTonalIconButton(
                                    onClick = { if (quantity < product.stockQty) quantity += 1.0 },
                                    modifier = Modifier.size(48.dp)
                                ) { Icon(Icons.Filled.Add, contentDescription = "Increase") }
                            }
                            Text(
                                text = WeightUtils.formatTaka(lineTotal),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = PrimaryDeepGreen
                            )
                        }

                        // Quick presets: 1 Mon (40 KG), 2 Mon (80 KG), 5 Mon (200 KG)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(1.0 to 40, 2.0 to 80, 5.0 to 200).forEach { (mon, kg) ->
                                val presetQty = if (product.unit == ProductUnit.KG.name) kg.toDouble() else mon
                                OutlinedButton(
                                    onClick = { quantity = presetQty.coerceAtMost(product.stockQty) },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("${mon.toInt()} Mon\n(${kg} KG)", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }

                        Text(
                            text = "${tr(lang, "মোট ওজন:", "Weight:")} ${WeightUtils.formatWeightSummary(quantity, product.unit, lang)} • ${tr(lang, "মজুদ:", "Stock:")} ${WeightUtils.trimDecimal(product.stockQty)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMutedGreen
                        )

                        if (wholesaleApplied) {
                            Surface(color = WarmYellow.copy(alpha = 0.25f), shape = RoundedCornerShape(10.dp)) {
                                Text(
                                    text = tr(lang, "পাইকারি ছাড় স্বয়ংক্রিয়ভাবে প্রয়োগ হয়েছে!", "Wholesale 5+ Mon discount applied automatically!"),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryDeepGreen,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Action Buttons
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = { onAddToCart(product, quantity) },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f).height(52.dp).testTag("detail_add_cart")
                        ) {
                            Icon(Icons.Filled.AddShoppingCart, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text(tr(lang, "কার্টে যোগ", "Add to Cart"), fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = { onBuyNow(product, quantity) },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f).height(52.dp).testTag("detail_buy_now")
                        ) {
                            Text(tr(lang, "এখনই কিনুন", "Buy Now"), fontWeight = FontWeight.Bold)
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalButton(
                            onClick = { showNegDialog = true },
                            modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp).testTag("detail_negotiate")
                        ) {
                            Icon(Icons.Filled.Handshake, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(tr(lang, "দাম আলোচনা", "Negotiate"), style = MaterialTheme.typography.labelMedium)
                        }
                        FilledTonalButton(
                            onClick = {
                                val msg = Uri.encode("Hello, I want to order ${WeightUtils.trimDecimal(quantity)} $unitLabel of ${product.nameEn} from KHALID AGRO.")
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/8801700000000?text=$msg")))
                            },
                            modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp)
                        ) {
                            Text("WhatsApp", style = MaterialTheme.typography.labelMedium)
                        }
                        FilledTonalButton(
                            onClick = { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:+8801700000000"))) },
                            modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp)
                        ) {
                            Icon(Icons.Filled.Phone, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(tr(lang, "কল", "Call"), style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }

            // Description & Delivery Info
            item {
                Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = SurfaceWhite)) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(tr(lang, "পণ্যের বিবরণ ও ডেলিভারি", "Description & Delivery"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(description, style = MaterialTheme.typography.bodyMedium)
                        HorizontalDivider(Modifier.padding(vertical = 4.dp))
                        Text("${tr(lang, "ফসল তোলার তারিখ:", "Harvest:")} ${product.harvestDateLabel}", style = MaterialTheme.typography.bodySmall)
                        Text("${tr(lang, "আনুমানিক ডেলিভারি:", "Estimated Delivery:")} ${product.estimatedDelivery}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            // Reviews Section + Submit Review
            item {
                Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = SurfaceWhite)) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(tr(lang, "ক্রেতাদের রিভিউ", "Customer Reviews (${productReviews.size})"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        productReviews.forEach { rev ->
                            Column(modifier = Modifier.fillMaxWidth().background(BgCream, RoundedCornerShape(10.dp)).padding(10.dp)) {
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text(rev.buyerName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                                    if (rev.sampleReview) {
                                        Text(tr(lang, "নমুনা রিভিউ", "Sample Review"), style = MaterialTheme.typography.labelSmall, color = WarmYellowDark)
                                    }
                                }
                                Text("★".repeat(rev.rating.toInt().coerceIn(1, 5)), color = WarmYellowDark)
                                Text(rev.comment, style = MaterialTheme.typography.bodySmall)
                            }
                        }

                        OutlinedTextField(
                            value = reviewComment,
                            onValueChange = { reviewComment = it },
                            label = { Text(tr(lang, "আপনার মতামত লিখুন", "Write a review")) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Row {
                                (1L..5L).forEach { star ->
                                    IconButton(onClick = { reviewRating = star }) {
                                        Icon(
                                            Icons.Filled.Star,
                                            contentDescription = "$star stars",
                                            tint = if (star <= reviewRating) WarmYellowDark else OutlineGreen
                                        )
                                    }
                                }
                            }
                            Button(
                                onClick = {
                                    if (reviewComment.isNotBlank()) {
                                        onSubmitReview(reviewRating, reviewComment)
                                        reviewComment = ""
                                    }
                                },
                                enabled = reviewComment.isNotBlank()
                            ) {
                                Text(tr(lang, "রিভিউ দিন", "Submit"))
                            }
                        }
                    }
                }
            }

            // Related Products
            if (related.isNotEmpty()) {
                item {
                    Text(tr(lang, "একই ধরনের আরও পণ্য", "Related Products"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(related, key = { it.id }) { rel ->
                            ProductCard(
                                product = rel,
                                lang = lang,
                                onClick = { onOpenRelated(rel.id) },
                                onQuickAdd = { onAddToCart(rel, rel.minOrderQty) },
                                modifier = Modifier.width(220.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showNegDialog) {
        var offerPrice by remember { mutableStateOf(product.wholesalePricePerUnit.toInt().toString()) }
        var offerQtyMon by remember { mutableStateOf("5") }
        var offerMsg by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showNegDialog = false },
            title = { Text(tr(lang, "কৃষকের সাথে দাম আলোচনা", "Negotiate Price With Farmer")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = offerQtyMon,
                        onValueChange = { offerQtyMon = it },
                        label = { Text(tr(lang, "পরিমাণ (মণ)", "Quantity (Mon)")) },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = offerPrice,
                        onValueChange = { offerPrice = it },
                        label = { Text(tr(lang, "প্রস্তাবিত মূল্য (৳ / ইউনিট)", "Offered Price (৳ / unit)")) },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = offerMsg,
                        onValueChange = { offerMsg = it },
                        label = { Text(tr(lang, "বার্তা", "Message to farmer")) }
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val qty = offerQtyMon.toDoubleOrNull() ?: 1.0
                    val price = offerPrice.toDoubleOrNull() ?: product.wholesalePricePerUnit
                    onStartNegotiation(qty, price, offerMsg.ifBlank { "Interested in bulk order at ৳$price" })
                    showNegDialog = false
                }) {
                    Text(tr(lang, "প্রস্তাব পাঠান", "Send Offer"))
                }
            },
            dismissButton = {
                TextButton(onClick = { showNegDialog = false }) { Text(tr(lang, "বাতিল", "Cancel")) }
            }
        )
    }
}
