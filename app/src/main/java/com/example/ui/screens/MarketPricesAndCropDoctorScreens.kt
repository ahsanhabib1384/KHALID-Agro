package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingFlat
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.AppLanguage
import com.example.core.WeightUtils
import com.example.core.tr
import com.example.model.*
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.ErrorRetryCard
import com.example.ui.theme.*
import com.example.ui.viewmodel.CropDoctorAiState
import com.example.ui.viewmodel.KrishiStreamState

private data class SeasonalCropGuide(
    val seasonBn: String,
    val seasonEn: String,
    val months: String,
    val cropsBn: String,
    val cropsEn: String
)

@Composable
fun MarketPricesAndCalendarScreen(
    lang: AppLanguage,
    marketPricesState: UiState<List<MarketPrice>>,
    settings: MarketplaceSettings,
    onOpenCropDoctor: () -> Unit
) {
    val seasons = remember {
        listOf(
            SeasonalCropGuide(
                "রবি মৌসুম (শীতকালীন)", "Rabi Season (Winter)",
                "Nov – Mar",
                "ভুট্টা, আলু, পেঁয়াজ, দেশি শিম, গম, সরিষা ও শীতকালীন সবজি",
                "Maize, Potato, Onion, Country Beans, Wheat, Mustard & Winter Vegetables"
            ),
            SeasonalCropGuide(
                "খরিফ-১ মৌসুম (গ্রীষ্ম)", "Kharif-1 (Summer)",
                "Mar – Jul",
                "আউশ ধান, পাট, গ্রীষ্মকালীন ভুট্টা, পেঁপে ও পটল",
                "Aus Paddy, Jute, Summer Maize, Papaya & Pointed Gourd (Potol)"
            ),
            SeasonalCropGuide(
                "খরিফ-২ মৌসুম (বর্ষা)", "Kharif-2 (Monsoon)",
                "Jul – Nov",
                "আমন ধান, আগাম শীতকালীন শিম ও রোপা ফসল",
                "Aman Paddy, Early Winter Beans & Transplanted Crops"
            )
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column {
                Text(
                    text = tr(lang, "আজকের যাচাইকৃত বাজার দর", "Verified Live Bazar Dor"),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryDeepGreen
                )
                Text(
                    text = tr(lang, "অ্যাডমিন যাচাইকৃত বাজার মূল্য (চুয়াডাঙ্গা ও প্রধান মোকাম)", "Admin-verified market rates (Chuadanga & regional hubs)"),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMutedGreen
                )
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = LightGreen),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.CalendarMonth, contentDescription = null, tint = PrimaryDeepGreen)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = if (lang == AppLanguage.BN) settings.seasonalNoticeBn else settings.seasonalNoticeEn,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = PrimaryDeepGreen
                    )
                }
            }
        }

        when (marketPricesState) {
            is UiState.Loading -> item { LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) }
            is UiState.Error -> item { ErrorRetryCard(marketPricesState.message) }
            is UiState.Success -> {
                if (marketPricesState.data.isEmpty()) {
                    item {
                        EmptyStateCard(
                            title = tr(lang, "কোনো বাজার দর যুক্ত নেই", "No verified market prices yet"),
                            subtitle = tr(lang, "অ্যাডমিন যাচাইকৃত দর এখানে প্রদর্শিত হবে।", "Admin-entered verified rates appear here.")
                        )
                    }
                } else {
                    items(marketPricesState.data, key = { it.id }) { mp ->
                        val (icon, color) = when (mp.movementEnum()) {
                            PriceMovement.UP -> Icons.AutoMirrored.Filled.TrendingUp to ErrorRed
                            PriceMovement.DOWN -> Icons.AutoMirrored.Filled.TrendingDown to SecondaryGreen
                            PriceMovement.STABLE -> Icons.AutoMirrored.Filled.TrendingFlat to TextMutedGreen
                        }
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                            border = BorderStroke(1.dp, OutlineGreen)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (lang == AppLanguage.BN) mp.productNameBn else mp.productNameEn,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${mp.marketLocation} • ${tr(lang, "যাচাইকারী:", "Verified by:")} ${mp.verifiedBy}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextMutedGreen
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "${WeightUtils.formatTaka(mp.currentPrice)} / ${mp.unit}",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = PrimaryDeepGreen
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(icon, contentDescription = mp.movement, tint = color, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            text = if (lang == AppLanguage.BN) mp.movementEnum().bn else mp.movementEnum().en,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = color,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Seasonal Crop Calendar
        item {
            Spacer(Modifier.height(4.dp))
            Text(
                text = tr(lang, "বাংলাদেশ মৌসুমি ফসল পঞ্জিকা", "Bangladesh Seasonal Crop Calendar"),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        items(seasons) { s ->
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                border = BorderStroke(1.dp, OutlineGreen)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text(if (lang == AppLanguage.BN) s.seasonBn else s.seasonEn, fontWeight = FontWeight.Bold, color = PrimaryDeepGreen)
                        Surface(color = WarmYellow.copy(alpha = 0.3f), shape = RoundedCornerShape(8.dp)) {
                            Text(s.months, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                        }
                    }
                    Text(if (lang == AppLanguage.BN) s.cropsBn else s.cropsEn, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        item {
            Button(
                onClick = onOpenCropDoctor,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().height(52.dp).testTag("open_crop_doctor_from_prices")
            ) {
                Icon(Icons.Filled.LocalHospital, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(tr(lang, "এআই ফসল ডাক্তারের পরামর্শ নিন", "Open AI Crop Doctor & Krishi Help"), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun CropDoctorScreen(
    lang: AppLanguage,
    diagnosisState: CropDoctorAiState,
    krishiStreamState: KrishiStreamState,
    historyState: UiState<List<CropDiagnosis>>,
    onAnalyzeCrop: (String, String, Bitmap?) -> Unit,
    onAskKrishiHelp: (String) -> Unit
) {
    val context = LocalContext.current
    var cropName by remember { mutableStateOf("") }
    var symptoms by remember { mutableStateOf("") }
    var selectedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var krishiQuestion by remember { mutableStateOf("") }
    var activeTab by remember { mutableIntStateOf(0) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                val bmp = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri))
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                }
                // Scale down softly to keep payload small (< 20MB limit)
                val maxDim = 1024
                val ratio = minOf(maxDim.toFloat() / bmp.width, maxDim.toFloat() / bmp.height, 1f)
                selectedBitmap = if (ratio < 1f) {
                    Bitmap.createScaledBitmap(bmp, (bmp.width * ratio).toInt(), (bmp.height * ratio).toInt(), true)
                } else bmp
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column {
                Text(
                    text = tr(lang, "এআই ফসল ডাক্তার ও কৃষি হেল্প", "AI Crop Doctor & Krishi Help"),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryDeepGreen
                )
                Text(
                    text = tr(lang, "ফসলের ছবি ও লক্ষণ বিশ্লেষণ এবং কৃষি পরামর্শ", "Crop disease diagnosis & live agricultural advice"),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMutedGreen
                )
                Spacer(Modifier.height(10.dp))
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = activeTab == 0,
                        onClick = { activeTab = 0 },
                        shape = SegmentedButtonDefaults.itemShape(0, 2)
                    ) {
                        Text(tr(lang, "ফসল রোগ নির্ণয়", "Crop Doctor"))
                    }
                    SegmentedButton(
                        selected = activeTab == 1,
                        onClick = { activeTab = 1 },
                        shape = SegmentedButtonDefaults.itemShape(1, 2)
                    ) {
                        Text(tr(lang, "কৃষি হেল্প চ্যাট", "Krishi Help"))
                    }
                }
            }
        }

        if (activeTab == 0) {
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                    border = BorderStroke(1.dp, OutlineGreen)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = cropName,
                            onValueChange = { cropName = it },
                            label = { Text(tr(lang, "ফসলের নাম (যেমন: ধান, ভুট্টা, শিম)", "Crop Name (e.g., Paddy, Maize, Beans)")) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("crop_doctor_crop_input")
                        )
                        OutlinedTextField(
                            value = symptoms,
                            onValueChange = { symptoms = it },
                            label = { Text(tr(lang, "রোগের লক্ষণ লিখুন", "Describe Symptoms (spots, yellowing, pests)")) },
                            minLines = 3,
                            modifier = Modifier.fillMaxWidth().testTag("crop_doctor_symptoms_input")
                        )

                        if (selectedBitmap != null) {
                            Image(
                                bitmap = selectedBitmap!!.asImageBitmap(),
                                contentDescription = "Selected crop photo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxWidth().height(170.dp).clip(RoundedCornerShape(12.dp))
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton(
                                onClick = {
                                    photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                },
                                modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp).testTag("crop_doctor_pick_photo")
                            ) {
                                Icon(Icons.Filled.AddAPhoto, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(tr(lang, "পাতার ছবি", "Pick Photo"))
                            }
                            Button(
                                onClick = { onAnalyzeCrop(cropName, symptoms, selectedBitmap) },
                                enabled = diagnosisState !is CropDoctorAiState.Loading,
                                modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp).testTag("crop_doctor_analyze_button")
                            ) {
                                if (diagnosisState is CropDoctorAiState.Loading) {
                                    CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                                } else {
                                    Text(tr(lang, "পরীক্ষা করুন", "Diagnose"), fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            item {
                when (diagnosisState) {
                    is CropDoctorAiState.Idle -> Unit
                    is CropDoctorAiState.Loading -> {
                        Card(colors = CardDefaults.cardColors(containerColor = LightGreen)) {
                            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(Modifier.size(22.dp))
                                Spacer(Modifier.width(12.dp))
                                Text(tr(lang, "ফসলের অবস্থা বিশ্লেষণ করা হচ্ছে...", "Analyzing crop symptoms..."))
                            }
                        }
                    }
                    is CropDoctorAiState.Error -> ErrorRetryCard(diagnosisState.message)
                    is CropDoctorAiState.Success -> {
                        val res = diagnosisState.result
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                            border = BorderStroke(1.5.dp, SecondaryGreen)
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text(tr(lang, "সম্ভাব্য রোগ নির্ণয়", "Diagnosis"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = PrimaryDeepGreen)
                                    Surface(color = WarmYellow, shape = RoundedCornerShape(8.dp)) {
                                        Text(res.severity, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                                    }
                                }
                                Text(res.diagnosis, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                                HorizontalDivider()
                                Text(tr(lang, "প্রস্তাবিত চিকিৎসা", "Treatment Steps"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = SecondaryGreen)
                                Text(res.treatmentSteps, style = MaterialTheme.typography.bodyMedium)
                                if (res.preventiveAdvice.isNotBlank()) {
                                    Text(tr(lang, "প্রতিরোধমূলক পরামর্শ", "Preventive Care"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Text(res.preventiveAdvice, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }

            // Saved history from Firestore
            if (historyState is UiState.Success && historyState.data.isNotEmpty()) {
                item {
                    Text(tr(lang, "পূর্ববর্তী ফসল পরীক্ষার তালিকা", "Saved Diagnosis History"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                items(historyState.data, key = { it.id }) { diag ->
                    Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = SurfaceWhite)) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text(diag.cropName, fontWeight = FontWeight.Bold, color = PrimaryDeepGreen)
                                Text(diag.severity, style = MaterialTheme.typography.labelSmall, color = WarningAmber, fontWeight = FontWeight.Bold)
                            }
                            Text(diag.diagnosis, style = MaterialTheme.typography.bodyMedium)
                            Text(diag.treatment, style = MaterialTheme.typography.bodySmall, color = TextMutedGreen)
                        }
                    }
                }
            }
        } else {
            // Krishi Help streaming Q&A
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                    border = BorderStroke(1.dp, OutlineGreen)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = krishiQuestion,
                            onValueChange = { krishiQuestion = it },
                            label = { Text(tr(lang, "কৃষি বা গবাদিপশু বিষয়ক প্রশ্ন লিখুন", "Ask any farming or livestock question")) },
                            minLines = 2,
                            modifier = Modifier.fillMaxWidth().testTag("krishi_help_input")
                        )
                        Button(
                            onClick = { onAskKrishiHelp(krishiQuestion) },
                            enabled = krishiQuestion.isNotBlank() && krishiStreamState !is KrishiStreamState.Loading,
                            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("krishi_help_send")
                        ) {
                            Text(tr(lang, "পরামর্শ নিন", "Ask Krishi Advisor"), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            item {
                when (krishiStreamState) {
                    is KrishiStreamState.Idle -> Unit
                    is KrishiStreamState.Loading -> LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    is KrishiStreamState.Streaming -> {
                        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = LightGreen)) {
                            Text(krishiStreamState.text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(14.dp))
                        }
                    }
                    is KrishiStreamState.Success -> {
                        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = SurfaceWhite), border = BorderStroke(1.dp, SecondaryGreen)) {
                            Text(krishiStreamState.text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(14.dp))
                        }
                    }
                    is KrishiStreamState.Error -> ErrorRetryCard(krishiStreamState.message)
                }
            }
        }
    }
}
