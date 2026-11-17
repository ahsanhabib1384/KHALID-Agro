package com.example.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@Keep
@IgnoreExtraProperties
data class UserAccount(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val email: String = "",
    val phone: String = "",
    val division: String = "Khulna",
    val district: String = "Chuadanga",
    val address: String = "",
    val role: String = UserRole.BUYER.name,
    val farmName: String = "",
    val farmLocation: String = "",
    val verifiedFarmer: Boolean = false,
    val totalPurchasesCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun roleEnum(): UserRole = safeEnum(role, UserRole.BUYER)

    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "displayName" to displayName.ifBlank { "Khalid Agro User" },
        "email" to email.ifBlank { null },
        "phone" to phone.ifBlank { null },
        "division" to division.ifBlank { null },
        "district" to district.ifBlank { null },
        "address" to address.ifBlank { null },
        "role" to role,
        "farmName" to farmName.ifBlank { null },
        "farmLocation" to farmLocation.ifBlank { null },
        "verifiedFarmer" to verifiedFarmer,
        "totalPurchasesCount" to totalPurchasesCount,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    ).filterValues { it != null } as Map<String, Any>
}

@Keep
@IgnoreExtraProperties
data class FarmerProfile(
    @DocumentId val id: String = "",
    val ownerUid: String = "",
    val name: String = "",
    val farmName: String = "",
    val location: String = "",
    val division: String = "",
    val bioEn: String = "",
    val bioBn: String = "",
    val specialty: String = "",
    val verified: Boolean = false,
    val rating: Double = 5.0,
    val totalSales: Long = 0L,
    val contactPhone: String = "",
    val whatsappNumber: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "ownerUid" to ownerUid,
        "name" to name,
        "farmName" to farmName,
        "location" to location,
        "division" to division,
        "bioEn" to bioEn.ifBlank { null },
        "bioBn" to bioBn.ifBlank { null },
        "specialty" to specialty.ifBlank { null },
        "verified" to verified,
        "rating" to rating,
        "totalSales" to totalSales,
        "contactPhone" to contactPhone.ifBlank { null },
        "whatsappNumber" to whatsappNumber.ifBlank { null },
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    ).filterValues { it != null } as Map<String, Any>
}

@Keep
@IgnoreExtraProperties
data class Product(
    @DocumentId val id: String = "",
    val farmerId: String = "",
    val farmerName: String = "",
    val farmLocation: String = "",
    val nameEn: String = "",
    val nameBn: String = "",
    val category: String = ProductCategory.FRESH_CROPS.name,
    val descriptionEn: String = "",
    val descriptionBn: String = "",
    val imageKey: String = "crops_harvest",
    val imageUri: String = "",
    val unit: String = ProductUnit.MON.name,
    val grade: String = ProductGrade.A_GRADE.name,
    val organic: Boolean = false,
    val pricePerUnit: Double = 0.0,
    val pricePerMon: Double = 0.0,
    val wholesalePricePerUnit: Double = 0.0,
    val minOrderQty: Double = 1.0,
    val wholesaleMinMon: Double = 5.0,
    val wholesaleDiscountPct: Double = 8.0,
    val stockQty: Double = 0.0,
    val harvestDateLabel: String = "Today",
    val harvestFreshness: String = HarvestFreshness.TODAY.name,
    val estimatedDelivery: String = "24-48 hours",
    val preOrderAvailable: Boolean = false,
    val dealBadge: String = "",
    val discountPct: Double = 0.0,
    val salesCount: Long = 0L,
    val status: String = ListingStatus.ACTIVE.name,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun categoryEnum(): ProductCategory = safeEnum(category, ProductCategory.FRESH_CROPS)
    fun unitEnum(): ProductUnit = safeEnum(unit, ProductUnit.KG)
    fun gradeEnum(): ProductGrade = safeEnum(grade, ProductGrade.A_GRADE)
    fun freshnessEnum(): HarvestFreshness = safeEnum(harvestFreshness, HarvestFreshness.TODAY)
    fun statusEnum(): ListingStatus = safeEnum(status, ListingStatus.ACTIVE)

    fun toCreateMap(): Map<String, Any> = mapOf(
        "farmerId" to farmerId,
        "farmerName" to farmerName,
        "farmLocation" to farmLocation,
        "nameEn" to nameEn,
        "nameBn" to nameBn,
        "category" to category,
        "descriptionEn" to descriptionEn.ifBlank { null },
        "descriptionBn" to descriptionBn.ifBlank { null },
        "imageKey" to imageKey.ifBlank { null },
        "imageUri" to imageUri.ifBlank { null },
        "unit" to unit,
        "grade" to grade,
        "organic" to organic,
        "pricePerUnit" to pricePerUnit,
        "pricePerMon" to pricePerMon,
        "wholesalePricePerUnit" to wholesalePricePerUnit,
        "minOrderQty" to minOrderQty,
        "wholesaleMinMon" to wholesaleMinMon,
        "wholesaleDiscountPct" to wholesaleDiscountPct,
        "stockQty" to stockQty,
        "harvestDateLabel" to harvestDateLabel.ifBlank { null },
        "harvestFreshness" to harvestFreshness,
        "estimatedDelivery" to estimatedDelivery.ifBlank { null },
        "preOrderAvailable" to preOrderAvailable,
        "dealBadge" to dealBadge.ifBlank { null },
        "discountPct" to discountPct,
        "salesCount" to salesCount,
        "status" to status,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    ).filterValues { it != null } as Map<String, Any>
}

@Keep
@IgnoreExtraProperties
data class Livestock(
    @DocumentId val id: String = "",
    val farmerId: String = "",
    val farmerName: String = "",
    val farmLocation: String = "",
    val animalNameEn: String = "",
    val animalNameBn: String = "",
    val category: String = LivestockCategory.COW.name,
    val breed: String = "",
    val gender: String = AnimalGender.MALE.name,
    val ageMonths: Long = 12L,
    val liveWeightKg: Double = 0.0,
    val estimatedWeightDate: String = "",
    val price: Double = 0.0,
    val wholesalePrice: Double = 0.0,
    val vaccinationHistory: String = "",
    val healthInfo: String = "",
    val descriptionEn: String = "",
    val descriptionBn: String = "",
    val imageKey: String = "livestock_cattle",
    val imageUri: String = "",
    val videoNote: String = "",
    val deliveryAvailable: Boolean = true,
    val estimatedDelivery: String = "2-3 days",
    val status: String = ListingStatus.ACTIVE.name,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun categoryEnum(): LivestockCategory = safeEnum(category, LivestockCategory.COW)
    fun genderEnum(): AnimalGender = safeEnum(gender, AnimalGender.MALE)
    fun statusEnum(): ListingStatus = safeEnum(status, ListingStatus.ACTIVE)

    fun toCreateMap(): Map<String, Any> = mapOf(
        "farmerId" to farmerId,
        "farmerName" to farmerName,
        "farmLocation" to farmLocation,
        "animalNameEn" to animalNameEn,
        "animalNameBn" to animalNameBn,
        "category" to category,
        "breed" to breed,
        "gender" to gender,
        "ageMonths" to ageMonths,
        "liveWeightKg" to liveWeightKg,
        "estimatedWeightDate" to estimatedWeightDate.ifBlank { null },
        "price" to price,
        "wholesalePrice" to wholesalePrice,
        "vaccinationHistory" to vaccinationHistory.ifBlank { null },
        "healthInfo" to healthInfo.ifBlank { null },
        "descriptionEn" to descriptionEn.ifBlank { null },
        "descriptionBn" to descriptionBn.ifBlank { null },
        "imageKey" to imageKey.ifBlank { null },
        "imageUri" to imageUri.ifBlank { null },
        "videoNote" to videoNote.ifBlank { null },
        "deliveryAvailable" to deliveryAvailable,
        "estimatedDelivery" to estimatedDelivery.ifBlank { null },
        "status" to status,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    ).filterValues { it != null } as Map<String, Any>
}

@Keep
@IgnoreExtraProperties
data class MarketPrice(
    @DocumentId val id: String = "",
    val productNameEn: String = "",
    val productNameBn: String = "",
    val category: String = "",
    val currentPrice: Double = 0.0,
    val previousPrice: Double = 0.0,
    val unit: String = ProductUnit.KG.name,
    val movement: String = PriceMovement.STABLE.name,
    val marketLocation: String = "Chuadanga Sadar Bazar",
    val verifiedBy: String = "Ahsan Habib Nasim",
    val active: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun movementEnum(): PriceMovement = safeEnum(movement, PriceMovement.STABLE)

    fun toCreateMap(): Map<String, Any> = mapOf(
        "productNameEn" to productNameEn,
        "productNameBn" to productNameBn,
        "category" to category,
        "currentPrice" to currentPrice,
        "previousPrice" to previousPrice,
        "unit" to unit,
        "movement" to movement,
        "marketLocation" to marketLocation,
        "verifiedBy" to verifiedBy,
        "active" to active,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@Keep
@IgnoreExtraProperties
data class OrderItemLine(
    val productId: String = "",
    val listingType: String = "PRODUCT",
    val title: String = "",
    val qty: Double = 1.0,
    val unit: String = "KG",
    val price: Double = 0.0
) {
    fun toMap(): Map<String, Any> = mapOf(
        "productId" to productId,
        "listingType" to listingType,
        "title" to title,
        "qty" to qty,
        "unit" to unit,
        "price" to price
    )
}

@Keep
@IgnoreExtraProperties
data class Order(
    @DocumentId val id: String = "",
    val buyerId: String = "",
    val buyerName: String = "",
    val buyerPhone: String = "",
    val deliveryAddress: String = "",
    val district: String = "",
    val farmerId: String = "",
    val farmerName: String = "",
    val deliveryAssigneeId: String = "",
    val itemsSummary: String = "",
    val items: List<OrderItemLine> = emptyList(),
    val totalWeightKg: Double = 0.0,
    val subtotal: Double = 0.0,
    val deliveryCharge: Double = 0.0,
    val discountAmount: Double = 0.0,
    val totalAmount: Double = 0.0,
    val wholesaleOrder: Boolean = false,
    val paymentMethod: String = PaymentMethod.CASH_ON_DELIVERY.name,
    val paymentStatus: String = PaymentStatus.PENDING.name,
    val orderStatus: String = OrderStatus.PENDING.name,
    val notes: String = "",
    val deliveryConfirmationNote: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun orderStatusEnum(): OrderStatus = safeEnum(orderStatus, OrderStatus.PENDING)
    fun paymentMethodEnum(): PaymentMethod = safeEnum(paymentMethod, PaymentMethod.CASH_ON_DELIVERY)
    fun paymentStatusEnum(): PaymentStatus = safeEnum(paymentStatus, PaymentStatus.PENDING)

    fun toCreateMap(): Map<String, Any> = mapOf(
        "buyerId" to buyerId,
        "buyerName" to buyerName,
        "buyerPhone" to buyerPhone,
        "deliveryAddress" to deliveryAddress,
        "district" to district,
        "farmerId" to farmerId,
        "farmerName" to farmerName,
        "deliveryAssigneeId" to deliveryAssigneeId.ifBlank { null },
        "itemsSummary" to itemsSummary,
        "items" to items.map { it.toMap() },
        "totalWeightKg" to totalWeightKg,
        "subtotal" to subtotal,
        "deliveryCharge" to deliveryCharge,
        "discountAmount" to discountAmount,
        "totalAmount" to totalAmount,
        "wholesaleOrder" to wholesaleOrder,
        "paymentMethod" to paymentMethod,
        "paymentStatus" to paymentStatus,
        "orderStatus" to orderStatus,
        "notes" to notes.ifBlank { null },
        "deliveryConfirmationNote" to deliveryConfirmationNote.ifBlank { null },
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    ).filterValues { it != null } as Map<String, Any>
}

@Keep
@IgnoreExtraProperties
data class Negotiation(
    @DocumentId val id: String = "",
    val buyerId: String = "",
    val buyerName: String = "",
    val farmerId: String = "",
    val farmerName: String = "",
    val productId: String = "",
    val productTitle: String = "",
    val requestedQtyMon: Double = 1.0,
    val listedPricePerUnit: Double = 0.0,
    val offeredPricePerUnit: Double = 0.0,
    val counterPricePerUnit: Double = 0.0,
    val status: String = NegotiationStatus.OPEN.name,
    val lastMessage: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun statusEnum(): NegotiationStatus = safeEnum(status, NegotiationStatus.OPEN)

    fun toCreateMap(): Map<String, Any> = mapOf(
        "buyerId" to buyerId,
        "buyerName" to buyerName,
        "farmerId" to farmerId,
        "farmerName" to farmerName,
        "productId" to productId,
        "productTitle" to productTitle,
        "requestedQtyMon" to requestedQtyMon,
        "listedPricePerUnit" to listedPricePerUnit,
        "offeredPricePerUnit" to offeredPricePerUnit,
        "counterPricePerUnit" to counterPricePerUnit,
        "status" to status,
        "lastMessage" to lastMessage,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@Keep
@IgnoreExtraProperties
data class NegotiationMessage(
    @DocumentId val id: String = "",
    val buyerId: String = "",
    val farmerId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val text: String = "",
    val offeredPrice: Double? = null,
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "buyerId" to buyerId,
        "farmerId" to farmerId,
        "senderId" to senderId,
        "senderName" to senderName,
        "text" to text,
        "offeredPrice" to offeredPrice,
        "createdAt" to FieldValue.serverTimestamp()
    ).filterValues { it != null } as Map<String, Any>
}

@Keep
@IgnoreExtraProperties
data class Review(
    @DocumentId val id: String = "",
    val productId: String = "",
    val buyerId: String = "",
    val buyerName: String = "",
    val rating: Long = 5L,
    val comment: String = "",
    val sampleReview: Boolean = false,
    val verifiedPurchase: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "productId" to productId,
        "buyerId" to buyerId,
        "buyerName" to buyerName,
        "rating" to rating,
        "comment" to comment,
        "sampleReview" to sampleReview,
        "verifiedPurchase" to verifiedPurchase,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@Keep
@IgnoreExtraProperties
data class MarketplaceSettings(
    @DocumentId val id: String = "",
    val commissionPct: Double = 0.0,
    val baseDeliveryCharge: Double = 120.0,
    val freeDeliveryMinAmount: Double = 5000.0,
    val wholesaleMinMon: Double = 5.0,
    val seasonalNoticeBn: String = "আমন ধান কাটা ও শীতকালীন সবজি বীজ বপনের মৌসুম চলছে – চুয়াডাঙ্গা থেকে সরাসরি সংগ্রহ।",
    val seasonalNoticeEn: String = "Aman paddy harvest & Rabi winter vegetable planting season active – direct from Chuadanga.",
    val publicReadable: Boolean = true,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "commissionPct" to commissionPct,
        "baseDeliveryCharge" to baseDeliveryCharge,
        "freeDeliveryMinAmount" to freeDeliveryMinAmount,
        "wholesaleMinMon" to wholesaleMinMon,
        "seasonalNoticeBn" to seasonalNoticeBn.ifBlank { null },
        "seasonalNoticeEn" to seasonalNoticeEn.ifBlank { null },
        "publicReadable" to publicReadable,
        "updatedAt" to FieldValue.serverTimestamp()
    ).filterValues { it != null } as Map<String, Any>
}

@Keep
@IgnoreExtraProperties
data class CartItem(
    @DocumentId val id: String = "",
    val productId: String = "",
    val listingType: String = "PRODUCT",
    val farmerId: String = "",
    val farmerName: String = "",
    val titleEn: String = "",
    val titleBn: String = "",
    val unit: String = ProductUnit.KG.name,
    val quantity: Double = 1.0,
    val unitPrice: Double = 0.0,
    val wholesalePrice: Double = 0.0,
    val wholesaleMinMon: Double = 5.0,
    val imageKey: String = "crops_harvest",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "productId" to productId,
        "listingType" to listingType,
        "farmerId" to farmerId,
        "farmerName" to farmerName,
        "titleEn" to titleEn,
        "titleBn" to titleBn,
        "unit" to unit,
        "quantity" to quantity,
        "unitPrice" to unitPrice,
        "wholesalePrice" to wholesalePrice,
        "wholesaleMinMon" to wholesaleMinMon,
        "imageKey" to imageKey.ifBlank { null },
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    ).filterValues { it != null } as Map<String, Any>
}

@Keep
@IgnoreExtraProperties
data class FavoriteItem(
    @DocumentId val id: String = "",
    val targetId: String = "",
    val targetType: String = "PRODUCT",
    val titleEn: String = "",
    val titleBn: String = "",
    val subtitle: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "targetId" to targetId,
        "targetType" to targetType,
        "titleEn" to titleEn,
        "titleBn" to titleBn,
        "subtitle" to subtitle.ifBlank { null },
        "createdAt" to FieldValue.serverTimestamp()
    ).filterValues { it != null } as Map<String, Any>
}

@Keep
@IgnoreExtraProperties
data class CropDiagnosis(
    @DocumentId val id: String = "",
    val cropName: String = "",
    val symptoms: String = "",
    val diagnosis: String = "",
    val treatment: String = "",
    val severity: String = "MODERATE",
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "cropName" to cropName,
        "symptoms" to symptoms,
        "diagnosis" to diagnosis,
        "treatment" to treatment,
        "severity" to severity,
        "createdAt" to FieldValue.serverTimestamp()
    )
}
