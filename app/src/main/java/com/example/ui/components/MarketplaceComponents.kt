package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingFlat
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.core.AppLanguage
import com.example.core.WeightUtils
import com.example.core.tr
import com.example.model.*
import com.example.ui.theme.*

@Composable
fun KhalidAgroTopBar(
    lang: AppLanguage,
    cartCount: Int,
    onToggleLanguage: () -> Unit,
    onCartClick: () -> Unit,
    onAccountClick: () -> Unit,
    onLogoClick: () -> Unit = {}
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onLogoClick)
                    .testTag("brand_header")
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_khalid_agro_logo),
                    contentDescription = "KHALID AGRO Logo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                )
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        text = "KHALID AGRO",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryDeepGreen,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = tr(lang, "সরাসরি কৃষকের মাঠ থেকে", "Direct From Our Fields"),
                        style = MaterialTheme.typography.labelSmall,
                        color = SecondaryGreen,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Language toggle pill
            FilledTonalButton(
                onClick = onToggleLanguage,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                modifier = Modifier
                    .defaultMinSize(minWidth = 48.dp, minHeight = 40.dp)
                    .testTag("language_toggle_button")
            ) {
                Text(
                    text = if (lang == AppLanguage.BN) "EN" else "বাং",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.width(6.dp))

            // Cart button with badge
            IconButton(
                onClick = onCartClick,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("top_cart_button")
            ) {
                BadgedBox(
                    badge = {
                        if (cartCount > 0) {
                            Badge(containerColor = WarmYellow, contentColor = TextDarkGreen) {
                                Text(cartCount.toString(), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Filled.ShoppingCart,
                        contentDescription = tr(lang, "কার্ট", "Cart"),
                        tint = PrimaryDeepGreen
                    )
                }
            }

            // Account icon
            IconButton(
                onClick = onAccountClick,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("top_account_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.AccountCircle,
                    contentDescription = tr(lang, "অ্যাকাউন্ট", "Account"),
                    tint = PrimaryDeepGreen
                )
            }
        }
    }
}

@Composable
fun ListingImage(
    imageKey: String,
    imageUri: String,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    if (imageUri.isNotBlank()) {
        AsyncImage(
            model = imageUri,
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            error = painterResource(id = drawableForKey(imageKey)),
            placeholder = painterResource(id = drawableForKey(imageKey)),
            modifier = modifier
        )
    } else {
        Image(
            painter = painterResource(id = drawableForKey(imageKey)),
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
    }
}

fun drawableForKey(key: String): Int = when (key) {
    "hero_farm" -> R.drawable.img_hero_farm
    "livestock_cattle" -> R.drawable.img_livestock_cattle
    "farmer_nasim" -> R.drawable.img_farmer_nasim
    "logo" -> R.drawable.img_khalid_agro_logo
    else -> R.drawable.img_crops_harvest
}

@Composable
fun ProductCard(
    product: Product,
    lang: AppLanguage,
    onClick: () -> Unit,
    onQuickAdd: () -> Unit,
    modifier: Modifier = Modifier
) {
    val title = if (lang == AppLanguage.BN) product.nameBn else product.nameEn
    val unitLabel = if (lang == AppLanguage.BN) product.unitEnum().bn else product.unitEnum().en

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, OutlineGreen.copy(alpha = 0.55f)),
        modifier = modifier.testTag("product_card_${product.id}")
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(132.dp)
            ) {
                ListingImage(
                    imageKey = product.imageKey,
                    imageUri = product.imageUri,
                    contentDescription = title,
                    modifier = Modifier.fillMaxSize()
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (product.organic) {
                        Surface(
                            color = SecondaryGreen,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = tr(lang, "অর্গানিক", "Organic"),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                    } else {
                        Spacer(Modifier.width(1.dp))
                    }
                    if (product.dealBadge.isNotBlank()) {
                        Surface(
                            color = WarmYellow,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = product.dealBadge,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextDarkGreen,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }

            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "${product.farmerName} • ${product.farmLocation}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Text(
                            text = WeightUtils.formatTaka(product.pricePerUnit),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = PrimaryDeepGreen
                        )
                        Text(
                            text = "/ $unitLabel",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Surface(
                        color = LightGreen,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "${tr(lang, "পাইকারি", "Wholesale")} ${WeightUtils.formatTaka(product.wholesalePricePerUnit)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = PrimaryDeepGreen,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "${tr(lang, "মজুদ:", "Stock:")} ${WeightUtils.trimDecimal(product.stockQty)} $unitLabel",
                        style = MaterialTheme.typography.labelSmall,
                        color = SecondaryGreen,
                        fontWeight = FontWeight.Medium
                    )
                    Button(
                        onClick = onQuickAdd,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .defaultMinSize(minHeight = 40.dp)
                            .testTag("add_cart_${product.id}")
                    ) {
                        Icon(
                            Icons.Filled.AddShoppingCart,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(tr(lang, "যোগ করুন", "Add"), style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

@Composable
fun LivestockCard(
    animal: Livestock,
    lang: AppLanguage,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val title = if (lang == AppLanguage.BN) animal.animalNameBn else animal.animalNameEn
    val catLabel = if (lang == AppLanguage.BN) animal.categoryEnum().bn else animal.categoryEnum().en

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, WarmYellow.copy(alpha = 0.7f)),
        modifier = modifier.testTag("livestock_card_${animal.id}")
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(145.dp)
            ) {
                ListingImage(
                    imageKey = animal.imageKey,
                    imageUri = animal.imageUri,
                    contentDescription = title,
                    modifier = Modifier.fillMaxSize()
                )
                Surface(
                    color = PrimaryDeepGreen.copy(alpha = 0.9f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                ) {
                    Text(
                        text = "$catLabel • ${animal.breed}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Surface(
                    color = WarmYellow,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                ) {
                    Text(
                        text = "${WeightUtils.trimDecimal(animal.liveWeightKg)} KG",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextDarkGreen,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${tr(lang, "বয়স:", "Age:")} ${animal.ageMonths} ${tr(lang, "মাস", "mo")} • ${animal.farmLocation}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = WeightUtils.formatTaka(animal.price),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = PrimaryDeepGreen
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.Verified,
                            contentDescription = null,
                            tint = SecondaryGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = tr(lang, "টিকা সম্পন্ন", "Vaccinated"),
                            style = MaterialTheme.typography.labelSmall,
                            color = SecondaryGreen,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BazarDorChip(price: MarketPrice, lang: AppLanguage, onClick: () -> Unit) {
    val name = if (lang == AppLanguage.BN) price.productNameBn else price.productNameEn
    val (moveIcon, moveColor) = when (price.movementEnum()) {
        PriceMovement.UP -> Icons.AutoMirrored.Filled.TrendingUp to ErrorRed
        PriceMovement.DOWN -> Icons.AutoMirrored.Filled.TrendingDown to SecondaryGreen
        PriceMovement.STABLE -> Icons.AutoMirrored.Filled.TrendingFlat to TextMutedGreen
    }
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, OutlineGreen),
        modifier = Modifier.widthIn(min = 148.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(Modifier.width(6.dp))
                Icon(
                    imageVector = moveIcon,
                    contentDescription = price.movement,
                    tint = moveColor,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = "${WeightUtils.formatTaka(price.currentPrice)} / ${price.unit}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = PrimaryDeepGreen
            )
            Text(
                text = price.marketLocation,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun EmptyStateCard(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Filled.Agriculture,
                contentDescription = null,
                tint = SecondaryGreen,
                modifier = Modifier.size(44.dp)
            )
            Spacer(Modifier.height(10.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ErrorRetryCard(message: String, modifier: Modifier = Modifier) {
    Card(
        colors = CardDefaults.cardColors(containerColor = ErrorRed.copy(alpha = 0.08f)),
        border = BorderStroke(1.dp, ErrorRed.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.ErrorOutline, contentDescription = null, tint = ErrorRed)
            Spacer(Modifier.width(10.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = ErrorRed
            )
        }
    }
}
