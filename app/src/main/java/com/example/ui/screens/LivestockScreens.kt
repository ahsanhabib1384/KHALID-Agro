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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.AppLanguage
import com.example.core.WeightUtils
import com.example.core.tr
import com.example.model.Livestock
import com.example.model.LivestockCategory
import com.example.model.UiState
import com.example.ui.components.*
import com.example.ui.theme.*

@Composable
fun LivestockScreen(
    lang: AppLanguage,
    livestockState: UiState<List<Livestock>>,
    selectedCategory: LivestockCategory?,
    onCategoryChange: (LivestockCategory?) -> Unit,
    onAnimalClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
            Text(
                text = tr(lang, "খালিদ অ্যাগ্রো গবাদিপশু হাট", "KHALID AGRO Livestock Market"),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = PrimaryDeepGreen
            )
            Text(
                text = tr(lang, "টিকা সম্পন্ন সুস্থ গরু, ছাগল, ভেড়া, হাঁস ও মুরগি সরাসরি খামার থেকে", "Vaccinated cattle, goats, sheep & poultry direct from farm"),
                style = MaterialTheme.typography.bodySmall,
                color = TextMutedGreen
            )
            Spacer(Modifier.height(10.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = selectedCategory == null,
                        onClick = { onCategoryChange(null) },
                        label = { Text(tr(lang, "সব গবাদিপশু", "All Livestock")) }
                    )
                }
                items(LivestockCategory.entries) { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { onCategoryChange(cat) },
                        label = { Text(if (lang == AppLanguage.BN) cat.bn else cat.en) }
                    )
                }
            }
        }

        when (livestockState) {
            is UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            is UiState.Error -> ErrorRetryCard(livestockState.message, Modifier.padding(16.dp))
            is UiState.Success -> {
                val filtered = remember(livestockState.data, selectedCategory) {
                    livestockState.data.filter { selectedCategory == null || it.categoryEnum() == selectedCategory }
                }
                if (filtered.isEmpty()) {
                    EmptyStateCard(
                        title = tr(lang, "কোনো গবাদিপশু পাওয়া যায়নি", "No livestock listings yet"),
                        subtitle = tr(lang, "অন্য ক্যাটাগরি নির্বাচন করুন বা শীঘ্রই আবার দেখুন।", "Try another category or check back soon."),
                        modifier = Modifier.padding(16.dp)
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filtered, key = { it.id }) { animal ->
                            LivestockCard(
                                animal = animal,
                                lang = lang,
                                onClick = { onAnimalClick(animal.id) },
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
fun LivestockDetailScreen(
    animal: Livestock,
    lang: AppLanguage,
    onBack: () -> Unit,
    onAddToCart: (Livestock) -> Unit,
    onBuyNow: (Livestock) -> Unit
) {
    val context = LocalContext.current
    val title = if (lang == AppLanguage.BN) animal.animalNameBn else animal.animalNameEn
    val desc = if (lang == AppLanguage.BN) animal.descriptionBn else animal.descriptionEn

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                ListingImage(
                    imageKey = animal.imageKey,
                    imageUri = animal.imageUri,
                    contentDescription = title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(230.dp)
                        .clip(RoundedCornerShape(20.dp))
                )
            }

            item {
                Column {
                    Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                    Spacer(Modifier.height(4.dp))
                    Text("${animal.farmerName} ✓ • ${animal.farmLocation}", color = SecondaryGreen, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = WeightUtils.formatTaka(animal.price),
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = PrimaryDeepGreen
                        )
                        Surface(color = LightGreen, shape = RoundedCornerShape(10.dp)) {
                            Text(
                                text = "${tr(lang, "পাইকারি:", "Wholesale:")} ${WeightUtils.formatTaka(animal.wholesalePrice)}",
                                style = MaterialTheme.typography.labelLarge,
                                color = PrimaryDeepGreen,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                    border = BorderStroke(1.dp, OutlineGreen)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(tr(lang, "স্বাস্থ্য ও প্রজাতির বিবরণ", "Animal Specifications & Health"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        SpecRow(tr(lang, "জাত (Breed)", "Breed"), animal.breed)
                        SpecRow(tr(lang, "লিঙ্গ", "Gender"), if (lang == AppLanguage.BN) animal.genderEnum().bn else animal.genderEnum().en)
                        SpecRow(tr(lang, "বয়স", "Age"), "${animal.ageMonths} ${tr(lang, "মাস", "months")}")
                        SpecRow(tr(lang, "জীবিত ওজন", "Live Weight"), "${WeightUtils.trimDecimal(animal.liveWeightKg)} KG (${animal.estimatedWeightDate})")
                        SpecRow(tr(lang, "টিকা ইতিহাস", "Vaccination"), animal.vaccinationHistory)
                        SpecRow(tr(lang, "স্বাস্থ্য অবস্থা", "Health Info"), animal.healthInfo)
                        if (animal.videoNote.isNotBlank()) {
                            SpecRow(tr(lang, "ভিডিও পরিদর্শন", "Video Inspection"), animal.videoNote)
                        }
                        SpecRow(tr(lang, "ডেলিভারি", "Delivery"), animal.estimatedDelivery)
                    }
                }
            }

            if (desc.isNotBlank()) {
                item {
                    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = SurfaceWhite)) {
                        Text(desc, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(14.dp))
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = { onAddToCart(animal) },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f).height(52.dp).testTag("livestock_add_cart")
                        ) {
                            Text(tr(lang, "কার্টে যোগ", "Add to Cart"), fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = { onBuyNow(animal) },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f).height(52.dp).testTag("livestock_buy_now")
                        ) {
                            Text(tr(lang, "বুকিং / কিনুন", "Buy Now"), fontWeight = FontWeight.Bold)
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FilledTonalButton(
                            onClick = { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:+8801700000000"))) },
                            modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp)
                        ) {
                            Icon(Icons.Filled.Phone, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(tr(lang, "খামারে কল করুন", "Call Farm"))
                        }
                        FilledTonalButton(
                            onClick = {
                                val msg = Uri.encode("Hello, I want live video & booking details for ${animal.animalNameEn} on KHALID AGRO.")
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/8801700000000?text=$msg")))
                            },
                            modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp)
                        ) {
                            Text("WhatsApp Video")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SpecRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = TextMutedGreen, modifier = Modifier.weight(0.42f))
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(0.58f))
    }
}
