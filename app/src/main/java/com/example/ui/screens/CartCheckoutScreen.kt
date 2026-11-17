package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.AppLanguage
import com.example.core.WeightUtils
import com.example.core.tr
import com.example.model.*
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.ErrorRetryCard
import com.example.ui.components.ListingImage
import com.example.ui.theme.*

@Composable
fun CartCheckoutScreen(
    lang: AppLanguage,
    cartState: UiState<List<CartItem>>,
    userAccount: UserAccount?,
    settings: MarketplaceSettings,
    isBusy: Boolean,
    onUpdateQuantity: (CartItem, Double) -> Unit,
    onRemoveItem: (String) -> Unit,
    onPlaceOrder: (String, String, String, String, PaymentMethod, String) -> Unit,
    onBrowseShop: () -> Unit
) {
    var buyerName by remember(userAccount) { mutableStateOf(userAccount?.displayName.orEmpty()) }
    var buyerPhone by remember(userAccount) { mutableStateOf(userAccount?.phone.orEmpty()) }
    var district by remember(userAccount) { mutableStateOf(userAccount?.district?.ifBlank { "Chuadanga" } ?: "Chuadanga") }
    var address by remember(userAccount) { mutableStateOf(userAccount?.address.orEmpty()) }
    var notes by remember { mutableStateOf("") }
    var selectedPayment by remember { mutableStateOf(PaymentMethod.CASH_ON_DELIVERY) }

    when (cartState) {
        is UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        is UiState.Error -> ErrorRetryCard(cartState.message, Modifier.padding(16.dp))
        is UiState.Success -> {
            val items = cartState.data
            if (items.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    EmptyStateCard(
                        title = tr(lang, "আপনার কার্ট খালি", "Your Cart is Empty"),
                        subtitle = tr(lang, "সরাসরি কৃষকের কাছ থেকে টাটকা ফসল বা গবাদিপশু যোগ করুন।", "Add fresh crops, seeds, or livestock directly from farms.")
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = onBrowseShop, modifier = Modifier.defaultMinSize(minHeight = 48.dp)) {
                        Text(tr(lang, "বাজারে যান", "Browse Marketplace"))
                    }
                }
            } else {
                val totalWeightKg = items.sumOf { WeightUtils.quantityInKg(it.quantity, it.unit) }
                val subtotal = items.sumOf { item ->
                    val wholesale = WeightUtils.isWholesaleEligible(item.quantity, item.unit, item.wholesaleMinMon)
                    val rate = if (wholesale && item.wholesalePrice > 0) item.wholesalePrice else item.unitPrice
                    rate * item.quantity
                }
                val delivery = if (subtotal >= settings.freeDeliveryMinAmount) 0.0 else settings.baseDeliveryCharge
                val total = subtotal + delivery

                LazyColumn(
                    modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text(
                            text = tr(lang, "কার্ট ও চেকআউট", "Cart & Checkout"),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryDeepGreen
                        )
                    }

                    items(items, key = { it.productId }) { item ->
                        val wholesale = WeightUtils.isWholesaleEligible(item.quantity, item.unit, item.wholesaleMinMon)
                        val activeRate = if (wholesale && item.wholesalePrice > 0) item.wholesalePrice else item.unitPrice
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                            border = BorderStroke(1.dp, OutlineGreen)
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                ListingImage(
                                    imageKey = item.imageKey,
                                    imageUri = "",
                                    contentDescription = item.titleEn,
                                    modifier = Modifier.size(64.dp).clip(RoundedCornerShape(12.dp))
                                )
                                Spacer(Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (lang == AppLanguage.BN) item.titleBn else item.titleEn,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${WeightUtils.formatTaka(activeRate)} / ${item.unit} • ${WeightUtils.formatWeightSummary(item.quantity, item.unit, lang)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextMutedGreen
                                    )
                                    if (wholesale) {
                                        Text(
                                            text = tr(lang, "পাইকারি দর প্রয়োগ হয়েছে", "Wholesale rate active"),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = SecondaryGreen,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = { onUpdateQuantity(item, item.quantity - 1.0) },
                                            modifier = Modifier.size(40.dp)
                                        ) { Icon(Icons.Filled.RemoveCircleOutline, contentDescription = "Decrease") }
                                        Text(WeightUtils.trimDecimal(item.quantity), fontWeight = FontWeight.Bold)
                                        IconButton(
                                            onClick = { onUpdateQuantity(item, item.quantity + 1.0) },
                                            modifier = Modifier.size(40.dp)
                                        ) { Icon(Icons.Filled.AddCircleOutline, contentDescription = "Increase") }
                                        Spacer(Modifier.weight(1f))
                                        IconButton(onClick = { onRemoveItem(item.productId) }, modifier = Modifier.size(40.dp)) {
                                            Icon(Icons.Filled.DeleteOutline, contentDescription = "Remove", tint = ErrorRed)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Order Summary Card
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = LightGreen)
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(tr(lang, "মোট হিসাব (১ মণ = ৪০ কেজি)", "Order Summary (1 Mon = 40 KG)"), fontWeight = FontWeight.Bold, color = PrimaryDeepGreen)
                                SummaryLine(tr(lang, "মোট ওজন", "Total Weight"), "${WeightUtils.trimDecimal(totalWeightKg)} KG (${WeightUtils.trimDecimal(WeightUtils.kgToMon(totalWeightKg))} Mon)")
                                SummaryLine(tr(lang, "পণ্যের মূল্য", "Subtotal"), WeightUtils.formatTaka(subtotal))
                                SummaryLine(tr(lang, "ডেলিভারি চার্জ", "Delivery Charge"), if (delivery == 0.0) tr(lang, "ফ্রি", "FREE") else WeightUtils.formatTaka(delivery))
                                HorizontalDivider(color = PrimaryDeepGreen.copy(alpha = 0.25f))
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text(tr(lang, "সর্বমোট", "Total Payable"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = PrimaryDeepGreen)
                                    Text(WeightUtils.formatTaka(total), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = PrimaryDeepGreen)
                                }
                            }
                        }
                    }

                    // Delivery & Payment Form
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                            border = BorderStroke(1.dp, OutlineGreen)
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(tr(lang, "ডেলিভারি ও পেমেন্ট তথ্য", "Delivery & Payment Details"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                OutlinedTextField(
                                    value = buyerName,
                                    onValueChange = { buyerName = it },
                                    label = { Text(tr(lang, "ক্রেতার নাম", "Full Name")) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = buyerPhone,
                                    onValueChange = { buyerPhone = it },
                                    label = { Text(tr(lang, "মোবাইল নম্বর", "Phone Number (e.g. 017...)")) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth().testTag("checkout_phone_input")
                                )
                                OutlinedTextField(
                                    value = district,
                                    onValueChange = { district = it },
                                    label = { Text(tr(lang, "জেলা", "District")) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = address,
                                    onValueChange = { address = it },
                                    label = { Text(tr(lang, "বিস্তারিত ঠিকানা", "Full Delivery Address")) },
                                    modifier = Modifier.fillMaxWidth().testTag("checkout_address_input")
                                )
                                OutlinedTextField(
                                    value = notes,
                                    onValueChange = { notes = it },
                                    label = { Text(tr(lang, "অতিরিক্ত নির্দেশনা (ঐচ্ছিক)", "Order Notes (optional)")) },
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Text(tr(lang, "পেমেন্ট মাধ্যম", "Payment Method"), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                                PaymentMethod.entries.forEach { pm ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        RadioButton(
                                            selected = selectedPayment == pm,
                                            onClick = { selectedPayment = pm }
                                        )
                                        Text(if (lang == AppLanguage.BN) pm.bn else pm.en, style = MaterialTheme.typography.bodyMedium)
                                    }
                                }

                                Button(
                                    onClick = {
                                        onPlaceOrder(
                                            buyerName.ifBlank { "Khalid Agro Buyer" },
                                            buyerPhone,
                                            address,
                                            district,
                                            selectedPayment,
                                            notes
                                        )
                                    },
                                    enabled = !isBusy && buyerPhone.length >= 6 && address.length >= 3,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth().height(54.dp).testTag("place_order_button")
                                ) {
                                    if (isBusy) {
                                        CircularProgressIndicator(Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
                                    } else {
                                        Text(
                                            text = "${tr(lang, "অর্ডার নিশ্চিত করুন", "Confirm Order")} (${WeightUtils.formatTaka(total)})",
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
    }
}

@Composable
private fun SummaryLine(label: String, value: String) {
    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.bodySmall)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
    }
}
