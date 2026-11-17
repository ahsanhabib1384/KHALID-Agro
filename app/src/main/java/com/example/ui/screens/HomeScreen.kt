package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.core.AppLanguage
import com.example.core.tr
import com.example.model.*
import com.example.ui.components.*
import com.example.ui.theme.*

private data class HomeCategoryTile(
    val titleBn: String,
    val titleEn: String,
    val icon: ImageVector,
    val imageRes: Int,
    val productCategory: ProductCategory? = null,
    val livestockCategory: LivestockCategory? = null
)

@Composable
fun HomeScreen(
    lang: AppLanguage,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    productsState: UiState<List<Product>>,
    marketPricesState: UiState<List<MarketPrice>>,
    farmersState: UiState<List<FarmerProfile>>,
    reviewsState: UiState<List<Review>>,
    onOpenShop: (ProductCategory?, Boolean) -> Unit,
    onOpenLivestock: (LivestockCategory?) -> Unit,
    onOpenProductDetail: (String) -> Unit,
    onQuickAddToCart: (Product) -> Unit,
    onOpenMarketPrices: () -> Unit,
    onOpenCropDoctor: () -> Unit,
    onOpenFarmerJoin: () -> Unit,
    onOpenAbout: () -> Unit
) {
    val context = LocalContext.current
    var selectedFreshnessTab by remember { mutableStateOf(HarvestFreshness.TODAY) }

    val categories = remember {
        listOf(
            HomeCategoryTile("টাটকা ফসল ও সবজি", "Fresh Crops & Veg", Icons.Filled.Eco, R.drawable.img_crops_harvest, ProductCategory.FRESH_CROPS),
            HomeCategoryTile("চাল ও ধান", "Rice & Paddy", Icons.Filled.Grain, R.drawable.img_hero_farm, ProductCategory.RICE_PADDY),
            HomeCategoryTile("ভুট্টা", "Maize", Icons.Filled.Grass, R.drawable.img_crops_harvest, ProductCategory.MAIZE),
            HomeCategoryTile("আলু ও পেঁয়াজ", "Potato & Onion", Icons.Filled.Spa, R.drawable.img_crops_harvest, ProductCategory.POTATO_ONION),
            HomeCategoryTile("বীজ", "Seeds", Icons.Filled.Yard, R.drawable.img_hero_farm, ProductCategory.SEEDS),
            HomeCategoryTile("সার ও জৈব সার", "Fertilizers", Icons.Filled.Science, R.drawable.img_crops_harvest, ProductCategory.FERTILIZERS),
            HomeCategoryTile("কৃষি যন্ত্রপাতি", "Agri Tools", Icons.Filled.Build, R.drawable.img_hero_farm, ProductCategory.TOOLS),
            HomeCategoryTile("গরু", "Cattle", Icons.Filled.Pets, R.drawable.img_livestock_cattle, livestockCategory = LivestockCategory.COW),
            HomeCategoryTile("ছাগল ও ভেড়া", "Goats & Sheep", Icons.Filled.Pets, R.drawable.img_livestock_cattle, livestockCategory = LivestockCategory.GOAT),
            HomeCategoryTile("হাঁস ও মুরগি", "Poultry & Ducks", Icons.Filled.Egg, R.drawable.img_livestock_cattle, livestockCategory = LivestockCategory.CHICKEN)
        )
    }

    val allProducts = (productsState as? UiState.Success)?.data.orEmpty()
    val filteredProducts = remember(allProducts, searchQuery) {
        if (searchQuery.isBlank()) allProducts
        else allProducts.filter {
            it.nameEn.contains(searchQuery, true) ||
                it.nameBn.contains(searchQuery, true) ||
                it.farmLocation.contains(searchQuery, true)
        }
    }

    val deals = remember(filteredProducts) {
        filteredProducts.filter { it.discountPct > 0.0 || it.dealBadge.isNotBlank() }
    }
    val bestSellers = remember(filteredProducts) {
        filteredProducts.sortedByDescending { it.salesCount }.take(6)
    }
    val freshlyHarvested = remember(filteredProducts, selectedFreshnessTab) {
        filteredProducts.filter { it.freshnessEnum() == selectedFreshnessTab }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // A. HERO SECTION
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .clip(RoundedCornerShape(24.dp))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_hero_farm),
                    contentDescription = "Khalid Agro Hero Fields",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(340.dp)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(340.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    PrimaryDeepGreen.copy(alpha = 0.78f),
                                    PrimaryDeepGreen.copy(alpha = 0.92f)
                                )
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = WarmYellow,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "KHALID AGRO • CHUA-BD",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = TextDarkGreen,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = tr(lang, "১ মণ = ৪০ কেজি", "1 Mon = 40 KG"),
                            style = MaterialTheme.typography.labelSmall,
                            color = LightGreen
                        )
                    }

                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = tr(
                            lang,
                            "মধ্যস্বত্বভোগী ছাড়াই কিনুন সরাসরি কৃষকের কাছ থেকে",
                            "Buy Directly From Farmers Without Middlemen"
                        ),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = tr(
                            lang,
                            "টাটকা ফসল, মানসম্মত বীজ ও সুস্থ গবাদিপশু – সব এক জায়গায়।",
                            "Fresh crops, quality seeds & healthy livestock – all in one place."
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = LightGreen
                    )

                    Spacer(Modifier.height(14.dp))
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearchChange,
                        placeholder = {
                            Text(
                                tr(lang, "ধান, ভুট্টা, পেঁয়াজ, বীজ বা গরু খুঁজুন...", "Search paddy, maize, onion, seeds, cattle..."),
                                style = MaterialTheme.typography.bodySmall
                            )
                        },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = PrimaryDeepGreen) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedBorderColor = WarmYellow,
                            unfocusedBorderColor = Color.Transparent
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("home_search_input")
                    )

                    Spacer(Modifier.height(14.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = { onOpenShop(null, false) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = WarmYellow,
                                contentColor = TextDarkGreen
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 48.dp)
                                .testTag("hero_shop_now_button")
                        ) {
                            Text(
                                tr(lang, "এখনই বাজার করুন", "Shop Now"),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                        FilledTonalButton(
                            onClick = onOpenFarmerJoin,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 48.dp)
                                .testTag("hero_join_farmer_button")
                        ) {
                            Text(
                                tr(lang, "কৃষক হিসেবে যোগ দিন", "Join as Farmer"),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { onOpenShop(null, true) },
                        border = BorderStroke(1.dp, LightGreen),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 46.dp)
                            .testTag("hero_wholesale_button")
                    ) {
                        Icon(Icons.Filled.Inventory2, contentDescription = null, tint = WarmYellow, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            tr(lang, "পাইকারি অর্ডার করুন (৫+ মণ বিশেষ মূল্য)", "Wholesale Order (5+ Mon Special Rate)"),
                            color = Color.White,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }

        // B. LIVE BAZAR DOR TICKER
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                SectionHeader(
                    title = tr(lang, "লাইভ বাজার দর (চুয়াডাঙ্গা)", "Live Bazar Dor (Verified)"),
                    actionText = tr(lang, "সব দেখুন", "View All"),
                    onAction = onOpenMarketPrices
                )
                Spacer(Modifier.height(8.dp))
                when (marketPricesState) {
                    is UiState.Loading -> LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp))
                    is UiState.Error -> ErrorRetryCard(marketPricesState.message, Modifier.padding(horizontal = 16.dp))
                    is UiState.Success -> {
                        if (marketPricesState.data.isEmpty()) {
                            EmptyStateCard(
                                title = tr(lang, "বাজার দর হালনাগাদ হচ্ছে", "Bazar Rates Pending Verification"),
                                subtitle = tr(lang, "শুধুমাত্র অ্যাডমিন যাচাইকৃত বাজার দর দেখানো হয়।", "Only verified admin-entered market rates appear here."),
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        } else {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(marketPricesState.data, key = { it.id }) { mp ->
                                    BazarDorChip(price = mp, lang = lang, onClick = onOpenMarketPrices)
                                }
                            }
                        }
                    }
                }
            }
        }

        // AI CROP DOCTOR QUICK BANNER
        item {
            Card(
                onClick = onOpenCropDoctor,
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = LightGreen),
                border = BorderStroke(1.dp, SecondaryGreen.copy(alpha = 0.4f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .testTag("home_crop_doctor_banner")
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = PrimaryDeepGreen,
                        shape = CircleShape,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.LocalHospital, contentDescription = null, tint = WarmYellow)
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = tr(lang, "এআই ফসল ডাক্তার ও কৃষি হেল্প", "AI Crop Doctor & Krishi Help"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryDeepGreen
                        )
                        Text(
                            text = tr(
                                lang,
                                "পাতার ছবি বা লক্ষণ দিয়ে তাৎক্ষণিক রোগ নির্ণয় ও চিকিৎসা জানুন",
                                "Upload leaf photo or symptoms for instant crop diagnosis & care"
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextDarkGreen
                        )
                    }
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = PrimaryDeepGreen)
                }
            }
        }

        // C. PRODUCT CATEGORIES (10 Categories)
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                SectionHeader(
                    title = tr(lang, "পণ্যের ক্যাটাগরি", "Product Categories"),
                    actionText = tr(lang, "শপ", "Shop"),
                    onAction = { onOpenShop(null, false) }
                )
                Spacer(Modifier.height(8.dp))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(categories) { tile ->
                        Card(
                            onClick = {
                                if (tile.livestockCategory != null) onOpenLivestock(tile.livestockCategory)
                                else onOpenShop(tile.productCategory, false)
                            },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                            border = BorderStroke(1.dp, OutlineGreen.copy(alpha = 0.7f)),
                            modifier = Modifier.width(130.dp)
                        ) {
                            Column {
                                Box(modifier = Modifier.fillMaxWidth().height(76.dp)) {
                                    Image(
                                        painter = painterResource(id = tile.imageRes),
                                        contentDescription = tile.titleEn,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    Surface(
                                        color = PrimaryDeepGreen.copy(alpha = 0.85f),
                                        shape = CircleShape,
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .padding(6.dp)
                                            .size(28.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(tile.icon, contentDescription = null, tint = WarmYellow, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                                Text(
                                    text = if (lang == AppLanguage.BN) tile.titleBn else tile.titleEn,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // D. TODAY'S DEALS
        if (deals.isNotEmpty()) {
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    SectionHeader(
                        title = tr(lang, "আজকের বিশেষ অফার", "Today's Deals"),
                        actionText = tr(lang, "সব অফার", "All Deals"),
                        onAction = { onOpenShop(null, false) }
                    )
                    Spacer(Modifier.height(8.dp))
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(deals, key = { "deal_${it.id}" }) { product ->
                            ProductCard(
                                product = product,
                                lang = lang,
                                onClick = { onOpenProductDetail(product.id) },
                                onQuickAdd = { onQuickAddToCart(product) },
                                modifier = Modifier.width(236.dp)
                            )
                        }
                    }
                }
            }
        }

        // E. BEST SELLERS
        if (bestSellers.isNotEmpty()) {
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    SectionHeader(
                        title = tr(lang, "সবচেয়ে বেশি বিক্রিত পণ্য", "Best Sellers"),
                        actionText = tr(lang, "আরও দেখুন", "See More"),
                        onAction = { onOpenShop(null, false) }
                    )
                    Spacer(Modifier.height(8.dp))
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(bestSellers, key = { "best_${it.id}" }) { product ->
                            ProductCard(
                                product = product,
                                lang = lang,
                                onClick = { onOpenProductDetail(product.id) },
                                onQuickAdd = { onQuickAddToCart(product) },
                                modifier = Modifier.width(236.dp)
                            )
                        }
                    }
                }
            }
        }

        // F. FRESHLY HARVESTED (Today / Yesterday / 2 Days Ago)
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                SectionHeader(
                    title = tr(lang, "সদ্য তোলা টাটকা ফসল", "Freshly Harvested"),
                    actionText = tr(lang, "শপ", "Shop"),
                    onAction = { onOpenShop(null, false) }
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    listOf(
                        HarvestFreshness.TODAY,
                        HarvestFreshness.YESTERDAY,
                        HarvestFreshness.TWO_DAYS_AGO
                    ).forEach { freshness ->
                        FilterChip(
                            selected = selectedFreshnessTab == freshness,
                            onClick = { selectedFreshnessTab = freshness },
                            label = {
                                Text(
                                    if (lang == AppLanguage.BN) freshness.bn else freshness.en,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                if (freshlyHarvested.isEmpty()) {
                    EmptyStateCard(
                        title = tr(lang, "এই সময়ের ফসল শীঘ্রই আসছে", "No items for this harvest window yet"),
                        subtitle = tr(lang, "অন্যান্য টাটকা ফসল দেখতে সব পণ্য দেখুন।", "Try another freshness tab or browse the full shop."),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                } else {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(freshlyHarvested, key = { "fresh_${it.id}" }) { product ->
                            ProductCard(
                                product = product,
                                lang = lang,
                                onClick = { onOpenProductDetail(product.id) },
                                onQuickAdd = { onQuickAddToCart(product) },
                                modifier = Modifier.width(236.dp)
                            )
                        }
                    }
                }
            }
        }

        // G. FEATURED FARMER (Ahsan Habib Nasim, Chuadanga)
        item {
            val farmers = (farmersState as? UiState.Success)?.data.orEmpty()
            val featured = farmers.firstOrNull()
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = PrimaryDeepGreen),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.img_farmer_nasim),
                            contentDescription = "Ahsan Habib Nasim",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(68.dp)
                                .clip(CircleShape)
                        )
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = featured?.name ?: "Ahsan Habib Nasim",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(Modifier.width(6.dp))
                                Icon(Icons.Filled.Verified, contentDescription = "Verified", tint = WarmYellow, modifier = Modifier.size(20.dp))
                            }
                            Text(
                                text = featured?.farmName ?: "Khalid Agro — Chuadanga Flagship Farm",
                                style = MaterialTheme.typography.labelLarge,
                                color = WarmYellow
                            )
                            Text(
                                text = featured?.location ?: "Chuadanga, Khulna Division, Bangladesh",
                                style = MaterialTheme.typography.bodySmall,
                                color = LightGreen
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = if (lang == AppLanguage.BN) {
                            featured?.bioBn ?: "চুয়াডাঙ্গা থেকে সরাসরি মাঠের টাটকা ফসল, উন্নত বীজ ও সুস্থ গবাদিপশু।"
                        } else {
                            featured?.bioEn ?: "Direct verified farm produce, certified seeds, and healthy livestock from Chuadanga."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.92f)
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = {
                                val phone = featured?.contactPhone?.ifBlank { "+8801700000000" } ?: "+8801700000000"
                                context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")))
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = WarmYellow, contentColor = TextDarkGreen),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp)
                        ) {
                            Icon(Icons.Filled.Phone, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(tr(lang, "সরাসরি কল", "Call Farm"), fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = {
                                val phone = (featured?.whatsappNumber ?: "+8801700000000").replace("+", "")
                                val url = "https://wa.me/$phone"
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                            },
                            border = BorderStroke(1.dp, LightGreen),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp)
                        ) {
                            Text("WhatsApp", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // H. WHY BUY DIRECT?
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text(
                    text = tr(lang, "কেন খালিদ অ্যাগ্রো থেকে সরাসরি কিনবেন?", "Why Buy Direct From KHALID AGRO?"),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(10.dp))
                val reasons = listOf(
                    tr(lang, "কোনো মধ্যস্বত্বভোগী বা আড়তদারের বাড়তি খরচ নেই", "No unnecessary middlemen"),
                    tr(lang, "ন্যায্য দাম ও কৃষকের সরাসরি লাভ", "Fair prices for farmers & buyers"),
                    tr(lang, "মাঠ থেকে তোলা শতভাগ টাটকা কৃষিপণ্য", "Freshly harvested agricultural products"),
                    tr(lang, "কৃষকের সাথে সরাসরি দরদাম ও কথা বলার সুযোগ", "Direct farmer communication & negotiation"),
                    tr(lang, "স্বচ্ছ ওজন (১ মণ = ৪০ কেজি) ও গ্রেড তথ্য", "Transparent weight (1 Mon = 40 KG) & grade info")
                )
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                    border = BorderStroke(1.dp, OutlineGreen)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        reasons.forEach { reason ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = SecondaryGreen, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(10.dp))
                                Text(reason, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
        }

        // I. TESTIMONIALS (Clearly labeled sample reviews vs real buyer reviews)
        item {
            val reviews = (reviewsState as? UiState.Success)?.data.orEmpty()
            if (reviews.isNotEmpty()) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    SectionHeader(
                        title = tr(lang, "ক্রেতাদের মতামত", "Customer Reviews"),
                        actionText = "",
                        onAction = {}
                    )
                    Spacer(Modifier.height(8.dp))
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(reviews.take(6), key = { it.id }) { rev ->
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                                border = BorderStroke(1.dp, OutlineGreen),
                                modifier = Modifier.width(260.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(rev.buyerName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                        if (rev.sampleReview) {
                                            Surface(color = WarningAmber.copy(alpha = 0.2f), shape = RoundedCornerShape(6.dp)) {
                                                Text(
                                                    text = tr(lang, "নমুনা রিভিউ", "Sample Review"),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = WarmYellowDark,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Row {
                                        repeat(rev.rating.toInt().coerceIn(1, 5)) {
                                            Icon(Icons.Filled.Star, contentDescription = null, tint = WarmYellowDark, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                    Spacer(Modifier.height(6.dp))
                                    Text(rev.comment, style = MaterialTheme.typography.bodySmall, maxLines = 3, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                }
            }
        }

        // J. FOOTER
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceVariantCream),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.img_khalid_agro_logo),
                            contentDescription = "KHALID AGRO",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(44.dp).clip(CircleShape)
                        )
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("KHALID AGRO", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = PrimaryDeepGreen)
                            Text("From Our Fields to Your Home – কৃষকের মাঠ থেকে সরাসরি আপনার ঘরে", style = MaterialTheme.typography.bodySmall, color = TextMutedGreen)
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = tr(lang, "ঠিকানা: চুয়াডাঙ্গা সদর, খুলনা বিভাগ, বাংলাদেশ", "Location: Chuadanga, Khulna Division, Bangladesh"),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onOpenAbout, modifier = Modifier.weight(1f).defaultMinSize(minHeight = 46.dp)) {
                            Text(tr(lang, "আমাদের সম্পর্কে", "About & Contact"))
                        }
                        Button(onClick = onOpenFarmerJoin, modifier = Modifier.weight(1f).defaultMinSize(minHeight = 46.dp)) {
                            Text(tr(lang, "বিক্রেতা নিবন্ধন", "Seller Signup"))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, actionText: String, onAction: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        if (actionText.isNotBlank()) {
            TextButton(onClick = onAction) {
                Text(actionText, fontWeight = FontWeight.Bold, color = SecondaryGreen)
            }
        }
    }
}
