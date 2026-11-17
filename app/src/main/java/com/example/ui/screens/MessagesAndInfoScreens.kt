package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.core.AppLanguage
import com.example.core.WeightUtils
import com.example.core.tr
import com.example.model.Negotiation
import com.example.model.NegotiationMessage
import com.example.model.NegotiationStatus
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NegotiationChatScreen(
    negotiation: Negotiation,
    messages: List<NegotiationMessage>,
    currentUserId: String,
    lang: AppLanguage,
    onBack: () -> Unit,
    onSendMessage: (String, Double?, NegotiationStatus?) -> Unit
) {
    var msgText by remember { mutableStateOf("") }
    var offerPriceText by remember { mutableStateOf("") }
    val isFarmer = negotiation.farmerId == currentUserId

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(negotiation.productTitle, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                        Text(
                            text = "${tr(lang, "অবস্থা:", "Status:")} ${negotiation.status} • ${WeightUtils.formatTaka(negotiation.counterPricePerUnit)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = SecondaryGreen
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).background(BgCream)
        ) {
            if (isFarmer && negotiation.statusEnum() != NegotiationStatus.ACCEPTED) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { onSendMessage(tr(lang, "প্রস্তাব গ্রহণ করা হয়েছে!", "Offer Accepted!"), negotiation.offeredPricePerUnit, NegotiationStatus.ACCEPTED) },
                        modifier = Modifier.weight(1f)
                    ) { Text(tr(lang, "দাম মেনে নিন", "Accept Offer")) }
                    OutlinedButton(
                        onClick = { onSendMessage(tr(lang, "প্রস্তাবটি গ্রহণযোগ্য নয়।", "Offer Declined."), null, NegotiationStatus.REJECTED) },
                        modifier = Modifier.weight(1f)
                    ) { Text(tr(lang, "প্রত্যাখ্যান", "Reject")) }
                }
            }

            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages, key = { it.id }) { m ->
                    val mine = m.senderId == currentUserId
                    Column(
                        horizontalAlignment = if (mine) Alignment.End else Alignment.Start,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Surface(
                            color = if (mine) PrimaryDeepGreen else SurfaceWhite,
                            shape = RoundedCornerShape(14.dp),
                            border = if (mine) null else BorderStroke(1.dp, OutlineGreen)
                        ) {
                            Column(modifier = Modifier.padding(12.dp).widthIn(max = 280.dp)) {
                                Text(m.senderName, style = MaterialTheme.typography.labelSmall, color = if (mine) WarmYellow else SecondaryGreen, fontWeight = FontWeight.Bold)
                                Text(m.text, style = MaterialTheme.typography.bodyMedium, color = if (mine) Color.White else TextDarkGreen)
                                if (m.offeredPrice != null) {
                                    Text(
                                        text = "${tr(lang, "দর প্রস্তাব:", "Offer:")} ${WeightUtils.formatTaka(m.offeredPrice)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (mine) LightGreen else PrimaryDeepGreen
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Surface(tonalElevation = 3.dp, color = SurfaceWhite) {
                Column(modifier = Modifier.padding(10.dp).navigationBarsPadding(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = offerPriceText,
                            onValueChange = { offerPriceText = it },
                            placeholder = { Text(tr(lang, "দর ৳", "Price ৳")) },
                            singleLine = true,
                            modifier = Modifier.width(105.dp)
                        )
                        OutlinedTextField(
                            value = msgText,
                            onValueChange = { msgText = it },
                            placeholder = { Text(tr(lang, "বার্তা লিখুন...", "Type message...")) },
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = {
                                if (msgText.isNotBlank()) {
                                    val price = offerPriceText.toDoubleOrNull()
                                    val nextStatus = if (price != null && isFarmer) NegotiationStatus.COUNTERED else null
                                    onSendMessage(msgText, price, nextStatus)
                                    msgText = ""
                                    offerPriceText = ""
                                }
                            },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = PrimaryDeepGreen)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutContactScreen(
    lang: AppLanguage,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(tr(lang, "খালিদ অ্যাগ্রো সম্পর্কে", "About KHALID AGRO")) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).background(BgCream),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Image(
                        painter = painterResource(id = R.drawable.img_khalid_agro_logo),
                        contentDescription = "KHALID AGRO",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(110.dp).clip(CircleShape)
                    )
                    Spacer(Modifier.height(10.dp))
                    Text("KHALID AGRO", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, color = PrimaryDeepGreen)
                    Text("From Our Fields to Your Home – কৃষকের মাঠ থেকে সরাসরি আপনার ঘরে", style = MaterialTheme.typography.bodyMedium, color = SecondaryGreen)
                }
            }
            item {
                Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = SurfaceWhite)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Ahsan Habib Nasim", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.width(6.dp))
                            Icon(Icons.Filled.Verified, contentDescription = null, tint = SecondaryGreen, modifier = Modifier.size(18.dp))
                        }
                        Text(tr(lang, "প্রতিষ্ঠাতা ও প্রধান কৃষক • চুয়াডাঙ্গা, খুলনা বিভাগ, বাংলাদেশ", "Founder & Lead Farmer • Chuadanga, Khulna Division, Bangladesh"), style = MaterialTheme.typography.bodySmall, color = TextMutedGreen)
                        Text(
                            text = tr(
                                lang,
                                "খালিদ অ্যাগ্রো বাংলাদেশের কৃষকদের জন্য একটি সরাসরি ডিজিটাল মার্কেটপ্লেস যেখানে কোনো মধ্যস্বত্বভোগী ছাড়াই ক্রেতা, পাইকার এবং কৃষকরা সরাসরি যুক্ত হতে পারেন।",
                                "KHALID AGRO is Bangladesh's direct farmer-to-buyer digital agriculture marketplace connecting Chuadanga and nationwide farmers directly with consumers, wholesalers (Paikars), and livestock buyers."
                            ),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:+8801700000000"))) },
                                modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp)
                            ) {
                                Icon(Icons.Filled.Phone, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(tr(lang, "কল করুন", "Call"))
                            }
                            OutlinedButton(
                                onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/8801700000000"))) },
                                modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp)
                            ) {
                                Text("WhatsApp")
                            }
                        }
                    }
                }
            }
        }
    }
}
