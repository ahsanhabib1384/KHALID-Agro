package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.core.AppLanguage
import com.example.core.WeightUtils
import com.example.core.signOut
import com.example.core.tr
import com.example.model.*
import com.example.ui.components.EmptyStateCard
import com.example.ui.theme.*

@Composable
fun AccountScreen(
    lang: AppLanguage,
    isAdminUser: Boolean,
    userAccountState: UiState<UserAccount?>,
    buyerOrdersState: UiState<List<Order>>,
    favoritesState: UiState<List<FavoriteItem>>,
    buyerNegotiationsState: UiState<List<Negotiation>>,
    onUpdateProfile: (String, String, String, String, String, UserRole, String, String) -> Unit,
    onCancelOrder: (String) -> Unit,
    onOpenNegotiation: (Negotiation) -> Unit,
    onOpenFarmerDashboard: () -> Unit,
    onOpenDeliveryDashboard: () -> Unit,
    onOpenAdminConsole: () -> Unit,
    onOpenAbout: () -> Unit,
    onSignedOut: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    val account = (userAccountState as? UiState.Success)?.data
    var selectedTab by remember { mutableIntStateOf(0) }

    var displayName by remember(account) { mutableStateOf(account?.displayName.orEmpty()) }
    var phone by remember(account) { mutableStateOf(account?.phone.orEmpty()) }
    var division by remember(account) { mutableStateOf(account?.division?.ifBlank { "Khulna" } ?: "Khulna") }
    var district by remember(account) { mutableStateOf(account?.district?.ifBlank { "Chuadanga" } ?: "Chuadanga") }
    var address by remember(account) { mutableStateOf(account?.address.orEmpty()) }
    var farmName by remember(account) { mutableStateOf(account?.farmName.orEmpty()) }
    var farmLocation by remember(account) { mutableStateOf(account?.farmLocation.orEmpty()) }
    var selectedRole by remember(account) { mutableStateOf(account?.roleEnum() ?: UserRole.BUYER) }

    val orders = (buyerOrdersState as? UiState.Success)?.data.orEmpty()
    val favorites = (favoritesState as? UiState.Success)?.data.orEmpty()
    val negotiations = (buyerNegotiationsState as? UiState.Success)?.data.orEmpty()

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = PrimaryDeepGreen)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = account?.displayName?.ifBlank { "Khalid Agro Member" } ?: "Khalid Agro Member",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Text(
                                text = "${account?.email.orEmpty()} • ${if (lang == AppLanguage.BN) selectedRole.bn else selectedRole.en}",
                                style = MaterialTheme.typography.bodySmall,
                                color = LightGreen
                            )
                        }
                        IconButton(
                            onClick = { signOut(credentialManager, scope, onSignedOut) },
                            modifier = Modifier.testTag("sign_out_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Sign Out", tint = WarmYellow)
                        }
                    }

                    // Role Workspace Shortcuts
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        if (selectedRole == UserRole.FARMER || isAdminUser) {
                            FilledTonalButton(
                                onClick = onOpenFarmerDashboard,
                                modifier = Modifier.weight(1f).testTag("open_farmer_dashboard_btn")
                            ) {
                                Text(tr(lang, "কৃষক প্যানেল", "Farmer Panel"), style = MaterialTheme.typography.labelMedium)
                            }
                        }
                        if (selectedRole == UserRole.DELIVERY || isAdminUser) {
                            FilledTonalButton(
                                onClick = onOpenDeliveryDashboard,
                                modifier = Modifier.weight(1f).testTag("open_delivery_dashboard_btn")
                            ) {
                                Text(tr(lang, "ডেলিভারি", "Delivery"), style = MaterialTheme.typography.labelMedium)
                            }
                        }
                        if (isAdminUser) {
                            Button(
                                onClick = onOpenAdminConsole,
                                colors = ButtonDefaults.buttonColors(containerColor = WarmYellow, contentColor = TextDarkGreen),
                                modifier = Modifier.weight(1f).testTag("open_admin_console_btn")
                            ) {
                                Text(tr(lang, "অ্যাডমিন", "Admin"), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        item {
            ScrollableTabRow(selectedTabIndex = selectedTab, edgePadding = 0.dp) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text(tr(lang, "অর্ডার (${orders.size})", "Orders (${orders.size})")) })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text(tr(lang, "প্রিয় (${favorites.size})", "Favorites (${favorites.size})")) })
                Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text(tr(lang, "দরদাম (${negotiations.size})", "Chats (${negotiations.size})")) })
                Tab(selected = selectedTab == 3, onClick = { selectedTab = 3 }, text = { Text(tr(lang, "প্রোফাইল ও রোল", "Profile & Role")) })
            }
        }

        when (selectedTab) {
            0 -> {
                if (orders.isEmpty()) {
                    item {
                        EmptyStateCard(
                            title = tr(lang, "এখনও কোনো অর্ডার নেই", "No orders placed yet"),
                            subtitle = tr(lang, "আপনার অর্ডারগুলো এখানে রিয়েল-টাইমে ট্র্যাক করতে পারবেন।", "Track your farm-to-home orders here.")
                        )
                    }
                } else {
                    items(orders, key = { it.id }) { order ->
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                            border = BorderStroke(1.dp, OutlineGreen)
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text("#${order.id.takeLast(6).uppercase()}", fontWeight = FontWeight.Bold, color = PrimaryDeepGreen)
                                    Surface(color = LightGreen, shape = RoundedCornerShape(8.dp)) {
                                        Text(
                                            text = if (lang == AppLanguage.BN) order.orderStatusEnum().bn else order.orderStatusEnum().en,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = PrimaryDeepGreen,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                                Text(order.itemsSummary, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Text("${tr(lang, "মোট ওজন:", "Weight:")} ${WeightUtils.trimDecimal(order.totalWeightKg)} KG • ${WeightUtils.formatTaka(order.totalAmount)}", style = MaterialTheme.typography.bodySmall)
                                Text("${tr(lang, "কৃষক:", "Farmer:")} ${order.farmerName} • ${order.district}", style = MaterialTheme.typography.bodySmall, color = TextMutedGreen)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (order.orderStatusEnum() == OrderStatus.PENDING) {
                                        OutlinedButton(onClick = { onCancelOrder(order.id) }) {
                                            Text(tr(lang, "বাতিল", "Cancel"), color = ErrorRed)
                                        }
                                    }
                                    FilledTonalButton(onClick = {
                                        val invoiceText = buildString {
                                            appendLine("KHALID AGRO INVOICE #${order.id.takeLast(6).uppercase()}")
                                            appendLine("Buyer: ${order.buyerName} (${order.buyerPhone})")
                                            appendLine("Farmer: ${order.farmerName}")
                                            appendLine("Items: ${order.itemsSummary}")
                                            appendLine("Weight: ${order.totalWeightKg} KG")
                                            appendLine("Total: ${WeightUtils.formatTaka(order.totalAmount)}")
                                            appendLine("Payment: ${order.paymentMethod} (${order.paymentStatus})")
                                        }
                                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_TEXT, invoiceText)
                                        }
                                        context.startActivity(Intent.createChooser(sendIntent, "Share Invoice"))
                                    }) {
                                        Icon(Icons.Filled.ReceiptLong, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text(tr(lang, "ইনভয়েস", "Invoice"))
                                    }
                                }
                            }
                        }
                    }
                }
            }
            1 -> {
                if (favorites.isEmpty()) {
                    item {
                        EmptyStateCard(
                            title = tr(lang, "পছন্দের তালিকা খালি", "No favorites saved"),
                            subtitle = tr(lang, "পণ্যের হার্ট আইকনে ট্যাপ করে প্রিয় তালিকা তৈরি করুন।", "Tap the heart icon on products to save them here.")
                        )
                    }
                } else {
                    items(favorites, key = { it.targetId }) { fav ->
                        Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = SurfaceWhite)) {
                            Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Favorite, contentDescription = null, tint = ErrorRed)
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text(if (lang == AppLanguage.BN) fav.titleBn else fav.titleEn, fontWeight = FontWeight.Bold)
                                    Text(fav.subtitle, style = MaterialTheme.typography.bodySmall, color = TextMutedGreen)
                                }
                            }
                        }
                    }
                }
            }
            2 -> {
                if (negotiations.isEmpty()) {
                    item {
                        EmptyStateCard(
                            title = tr(lang, "কোনো দরদাম আলোচনা নেই", "No active negotiations"),
                            subtitle = tr(lang, "পণ্য পেজ থেকে 'দাম আলোচনা' বাটনে ক্লিক করে কৃষকের সাথে কথা বলুন।", "Tap Negotiate on any product to start direct pricing chat.")
                        )
                    }
                } else {
                    items(negotiations, key = { it.id }) { neg ->
                        Card(
                            onClick = { onOpenNegotiation(neg) },
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                            border = BorderStroke(1.dp, OutlineGreen)
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text(neg.productTitle, fontWeight = FontWeight.Bold)
                                    Text(neg.status, style = MaterialTheme.typography.labelSmall, color = SecondaryGreen, fontWeight = FontWeight.Bold)
                                }
                                Text("${neg.farmerName} • ${neg.lastMessage}", style = MaterialTheme.typography.bodySmall, color = TextMutedGreen)
                            }
                        }
                    }
                }
            }
            else -> {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                        border = BorderStroke(1.dp, OutlineGreen)
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(tr(lang, "অ্যাকাউন্ট তথ্য ও ভূমিকা নির্বাচন", "Profile & Role Setup"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            OutlinedTextField(value = displayName, onValueChange = { displayName = it }, label = { Text(tr(lang, "নাম", "Name")) }, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text(tr(lang, "ফোন নম্বর", "Phone")) }, modifier = Modifier.fillMaxWidth())
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(value = division, onValueChange = { division = it }, label = { Text(tr(lang, "বিভাগ", "Division")) }, modifier = Modifier.weight(1f))
                                OutlinedTextField(value = district, onValueChange = { district = it }, label = { Text(tr(lang, "জেলা", "District")) }, modifier = Modifier.weight(1f))
                            }
                            OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text(tr(lang, "ঠিকানা", "Address")) }, modifier = Modifier.fillMaxWidth())

                            Text(tr(lang, "অ্যাকাউন্ট রোল", "Select Role"), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                            val availableRoles = if (isAdminUser) UserRole.entries else listOf(UserRole.BUYER, UserRole.WHOLESALER, UserRole.FARMER, UserRole.DELIVERY)
                            availableRoles.forEach { r ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(selected = selectedRole == r, onClick = { selectedRole = r })
                                    Text(if (lang == AppLanguage.BN) r.bn else r.en)
                                }
                            }

                            if (selectedRole == UserRole.FARMER || selectedRole == UserRole.SUPER_ADMIN) {
                                OutlinedTextField(value = farmName, onValueChange = { farmName = it }, label = { Text(tr(lang, "খামারের নাম", "Farm Name")) }, modifier = Modifier.fillMaxWidth())
                                OutlinedTextField(value = farmLocation, onValueChange = { farmLocation = it }, label = { Text(tr(lang, "খামারের অবস্থান", "Farm Location")) }, modifier = Modifier.fillMaxWidth())
                            }

                            Button(
                                onClick = { onUpdateProfile(displayName, phone, division, district, address, selectedRole, farmName, farmLocation) },
                                modifier = Modifier.fillMaxWidth().height(50.dp).testTag("save_profile_button")
                            ) {
                                Text(tr(lang, "প্রোফাইল সংরক্ষণ করুন", "Save Profile & Role"), fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(onClick = onOpenAbout, modifier = Modifier.fillMaxWidth()) {
                                Text(tr(lang, "খালিদ অ্যাগ্রো সম্পর্কে", "About & Contact KHALID AGRO"))
                            }
                        }
                    }
                }
            }
        }
    }
}
