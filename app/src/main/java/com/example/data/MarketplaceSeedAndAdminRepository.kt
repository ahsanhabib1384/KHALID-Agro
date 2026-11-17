package com.example.data

import android.content.Context
import com.example.R
import com.example.core.OperationType
import com.example.core.handleFirestoreError
import com.example.model.*
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class MarketplaceSeedAndAdminRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String =
        auth.currentUser?.uid ?: throw IllegalStateException("Authentication required.")

    fun isBootstrappedAdmin(): Boolean {
        val u = auth.currentUser ?: return false
        return u.isEmailVerified && u.email.equals(MarketplaceRepository.SUPER_ADMIN_EMAIL, ignoreCase = true)
    }

    suspend fun ensureUserProfileAndBootstrap(): Result<UserAccount> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val docRef = db.collection("users").document(uid)
        return try {
            val snap = docRef.get().await()
            val user = auth.currentUser!!
            val isAdmin = isBootstrappedAdmin()
            val account = if (snap.exists()) {
                val existing = snap.toObject(UserAccount::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    ?: UserAccount(userId = uid)
                if (isAdmin && existing.role != UserRole.SUPER_ADMIN.name) {
                    docRef.update(
                        mapOf(
                            "role" to UserRole.SUPER_ADMIN.name,
                            "verifiedFarmer" to true,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                    existing.copy(role = UserRole.SUPER_ADMIN.name, verifiedFarmer = true)
                } else {
                    existing
                }
            } else {
                val newAccount = UserAccount(
                    userId = uid,
                    displayName = user.displayName ?: if (isAdmin) "Ahsan Habib Nasim" else "Khalid Agro Member",
                    email = user.email.orEmpty(),
                    division = "Khulna",
                    district = "Chuadanga",
                    role = if (isAdmin) UserRole.SUPER_ADMIN.name else UserRole.BUYER.name,
                    farmName = if (isAdmin) "Khalid Agro Chuadanga Farm" else "",
                    farmLocation = if (isAdmin) "Chuadanga, Khulna Division" else "",
                    verifiedFarmer = isAdmin
                )
                docRef.set(newAccount.toCreateMap()).await()
                newAccount
            }

            if (isAdmin) {
                seedInitialMarketplaceIfEmpty(uid)
            }
            Result.success(account)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    private suspend fun seedInitialMarketplaceIfEmpty(adminUid: String) {
        val existingProducts = db.collection("products").whereEqualTo("status", "ACTIVE").limit(1).get().await()
        if (!existingProducts.isEmpty) return

        val batch = db.batch()

        // 1. Verified Farm Profile: Ahsan Habib Nasim (Chuadanga)
        val nasimFarm = FarmerProfile(
            ownerUid = adminUid,
            name = "Ahsan Habib Nasim",
            farmName = "Khalid Agro — Chuadanga Flagship Farm",
            location = "Chuadanga Sadar, Chuadanga",
            division = "Khulna Division",
            bioEn = "Direct farmer-to-buyer harvest from Chuadanga. High-grade maize, Miniket paddy, winter vegetables, certified seeds, and healthy Deshi livestock.",
            bioBn = "চুয়াডাঙ্গা থেকে মধ্যস্বত্বভোগী ছাড়া সরাসরি মাঠের টাটকা ফসল, উন্নত বীজ ও সুস্থ গবাদিপশু সরবরাহকারী ভেরিফাইড খামার।",
            specialty = "Maize, Miniket Dhan, Seasonal Vegetables & Deshi Cattle",
            verified = true,
            rating = 4.9,
            totalSales = 148L,
            contactPhone = "+8801700000000",
            whatsappNumber = "+8801700000000"
        )
        batch.set(db.collection("farmers").document("farmer_ahsan_nasim"), nasimFarm.toCreateMap())

        // 2. Initial Marketplace Settings (0% commission default)
        batch.set(
            db.collection("settings").document("marketplace_config"),
            MarketplaceSettings().toCreateMap()
        )

        // 3. Initial Verified Market Prices (Bazar Dor)
        val prices = listOf(
            "mp_potato" to MarketPrice(
                productNameEn = "Potato (Lal Alu)", productNameBn = "লাল আলু",
                category = "POTATO_ONION", currentPrice = 48.0, previousPrice = 52.0,
                unit = "KG", movement = PriceMovement.DOWN.name,
                marketLocation = "Chuadanga Sadar Bazar", verifiedBy = "Ahsan Habib Nasim", active = true
            ),
            "mp_onion" to MarketPrice(
                productNameEn = "Deshi Onion (Peyaj)", productNameBn = "দেশি পেঁয়াজ",
                category = "POTATO_ONION", currentPrice = 95.0, previousPrice = 90.0,
                unit = "KG", movement = PriceMovement.UP.name,
                marketLocation = "Chuadanga Sadar Bazar", verifiedBy = "Ahsan Habib Nasim", active = true
            ),
            "mp_maize" to MarketPrice(
                productNameEn = "Golden Maize (Bhutta)", productNameBn = "ভুট্টা",
                category = "MAIZE", currentPrice = 1320.0, previousPrice = 1320.0,
                unit = "MON", movement = PriceMovement.STABLE.name,
                marketLocation = "Chuadanga Agro Hub", verifiedBy = "Ahsan Habib Nasim", active = true
            ),
            "mp_paddy" to MarketPrice(
                productNameEn = "Miniket Paddy (Dhan)", productNameBn = "মিনিকেট ধান",
                category = "RICE_PADDY", currentPrice = 1480.0, previousPrice = 1440.0,
                unit = "MON", movement = PriceMovement.UP.name,
                marketLocation = "Chuadanga Rice Bazar", verifiedBy = "Ahsan Habib Nasim", active = true
            ),
            "mp_beans" to MarketPrice(
                productNameEn = "Country Beans (Sim)", productNameBn = "শিম",
                category = "FRESH_CROPS", currentPrice = 60.0, previousPrice = 65.0,
                unit = "KG", movement = PriceMovement.DOWN.name,
                marketLocation = "Chuadanga Morning Haat", verifiedBy = "Ahsan Habib Nasim", active = true
            )
        )
        prices.forEach { (id, mp) -> batch.set(db.collection("market_prices").document(id), mp.toCreateMap()) }

        // 4. Products (Crops, Paddy, Maize, Potato/Onion, Seeds, Fertilizers, Tools)
        val products = listOf(
            "prod_maize_chuadanga" to Product(
                farmerId = adminUid, farmerName = "Ahsan Habib Nasim", farmLocation = "Chuadanga, Khulna",
                nameEn = "Chuadanga Golden Maize (Bhutta)", nameBn = "চুয়াডাঙ্গার সোনালী ভুট্টা",
                category = ProductCategory.MAIZE.name,
                descriptionEn = "Sun-dried A-Grade Chuadanga hybrid maize, ideal for poultry feed and flour mills. 1 Mon = 40 KG.",
                descriptionBn = "চুয়াডাঙ্গার রোদে শুকানো এ-গ্রেড ভুট্টা। পোল্ট্রি ফিড ও আটা কলের জন্য সেরা। ১ মণ = ৪০ কেজি।",
                imageKey = "crops_harvest", unit = ProductUnit.MON.name, grade = ProductGrade.A_GRADE.name,
                organic = false, pricePerUnit = 1320.0, pricePerMon = 1320.0, wholesalePricePerUnit = 1240.0,
                minOrderQty = 1.0, wholesaleMinMon = 5.0, wholesaleDiscountPct = 6.0, stockQty = 180.0,
                harvestDateLabel = "Today", harvestFreshness = HarvestFreshness.TODAY.name,
                estimatedDelivery = "24-48 hours nationwide", preOrderAvailable = false,
                dealBadge = "Today's Deal", discountPct = 5.0, salesCount = 42L, status = ListingStatus.ACTIVE.name
            ),
            "prod_miniket_paddy" to Product(
                farmerId = adminUid, farmerName = "Ahsan Habib Nasim", farmLocation = "Chuadanga, Khulna",
                nameEn = "Aromatic Miniket Paddy (Dhan)", nameBn = "মিনিকেট ধান",
                category = ProductCategory.RICE_PADDY.name,
                descriptionEn = "Clean, stone-free freshly harvested Miniket paddy directly from our Chuadanga fields.",
                descriptionBn = "নিজস্ব খামারের পরিষ্কার ও পাথরমুক্ত মিনিকেট ধান। ১ মণ = ৪০ কেজি।",
                imageKey = "hero_farm", unit = ProductUnit.MON.name, grade = ProductGrade.A_GRADE.name,
                organic = true, pricePerUnit = 1480.0, pricePerMon = 1480.0, wholesalePricePerUnit = 1390.0,
                minOrderQty = 1.0, wholesaleMinMon = 5.0, wholesaleDiscountPct = 6.0, stockQty = 240.0,
                harvestDateLabel = "Yesterday", harvestFreshness = HarvestFreshness.YESTERDAY.name,
                estimatedDelivery = "2-3 days", preOrderAvailable = false,
                dealBadge = "Best Seller", discountPct = 0.0, salesCount = 67L, status = ListingStatus.ACTIVE.name
            ),
            "prod_deshi_onion" to Product(
                farmerId = adminUid, farmerName = "Ahsan Habib Nasim", farmLocation = "Chuadanga, Khulna",
                nameEn = "Deshi Red Onion (Peyaj)", nameBn = "দেশি লাল পেঁয়াজ",
                category = ProductCategory.POTATO_ONION.name,
                descriptionEn = "Firm, pungent Deshi red onions sorted by size and cured for long shelf life.",
                descriptionBn = "বাছাইকৃত ঝাঁঝালো দেশি লাল পেঁয়াজ। সরাসরি কৃষকের গুদাম থেকে।",
                imageKey = "crops_harvest", unit = ProductUnit.KG.name, grade = ProductGrade.A_GRADE.name,
                organic = true, pricePerUnit = 92.0, pricePerMon = 3680.0, wholesalePricePerUnit = 84.0,
                minOrderQty = 5.0, wholesaleMinMon = 5.0, wholesaleDiscountPct = 9.0, stockQty = 1200.0,
                harvestDateLabel = "2 Days Ago", harvestFreshness = HarvestFreshness.TWO_DAYS_AGO.name,
                estimatedDelivery = "24-48 hours", preOrderAvailable = false,
                dealBadge = "Wholesale Special", discountPct = 8.0, salesCount = 54L, status = ListingStatus.ACTIVE.name
            ),
            "prod_country_beans" to Product(
                farmerId = adminUid, farmerName = "Ahsan Habib Nasim", farmLocation = "Chuadanga, Khulna",
                nameEn = "Fresh Country Beans (Deshi Sim)", nameBn = "টাটকা দেশি শিম",
                category = ProductCategory.FRESH_CROPS.name,
                descriptionEn = "Hand-picked tender winter country beans harvested at dawn without chemical sprays.",
                descriptionBn = "সকালবেলা মাঠ থেকে তোলা কচি দেশি শিম, সম্পূর্ণ বিষমুক্ত।",
                imageKey = "crops_harvest", unit = ProductUnit.KG.name, grade = ProductGrade.A_GRADE.name,
                organic = true, pricePerUnit = 58.0, pricePerMon = 2320.0, wholesalePricePerUnit = 50.0,
                minOrderQty = 2.0, wholesaleMinMon = 5.0, wholesaleDiscountPct = 14.0, stockQty = 450.0,
                harvestDateLabel = "Today", harvestFreshness = HarvestFreshness.TODAY.name,
                estimatedDelivery = "Same/Next Day", preOrderAvailable = false,
                dealBadge = "Harvested Today", discountPct = 10.0, salesCount = 31L, status = ListingStatus.ACTIVE.name
            ),
            "prod_hybrid_maize_seed" to Product(
                farmerId = adminUid, farmerName = "Ahsan Habib Nasim", farmLocation = "Chuadanga, Khulna",
                nameEn = "Certified Hybrid Maize Seeds (5 KG)", nameBn = "উচ্চফলনশীল হাইব্রিড ভুট্টা বীজ",
                category = ProductCategory.SEEDS.name,
                descriptionEn = "High-germination (>94%) hybrid maize seed packets tested in Chuadanga alluvial soil.",
                descriptionBn = "৯৪%+ অঙ্কুরোদগম ক্ষমতা সম্পন্ন পরীক্ষিত হাইব্রিড ভুট্টা বীজ।",
                imageKey = "hero_farm", unit = ProductUnit.PACKET.name, grade = ProductGrade.A_GRADE.name,
                organic = false, pricePerUnit = 1650.0, pricePerMon = 1650.0, wholesalePricePerUnit = 1520.0,
                minOrderQty = 1.0, wholesaleMinMon = 5.0, wholesaleDiscountPct = 8.0, stockQty = 90.0,
                harvestDateLabel = "Certified Batch", harvestFreshness = HarvestFreshness.STANDARD.name,
                estimatedDelivery = "48 hours", preOrderAvailable = true,
                dealBadge = "High Yield", discountPct = 0.0, salesCount = 29L, status = ListingStatus.ACTIVE.name
            ),
            "prod_vermicompost" to Product(
                farmerId = adminUid, farmerName = "Ahsan Habib Nasim", farmLocation = "Chuadanga, Khulna",
                nameEn = "Organic Vermicompost Fertilizer (40 KG Bag)", nameBn = "কেঁচো কম্পোস্ট জৈব সার (৪০ কেজি)",
                category = ProductCategory.FERTILIZERS.name,
                descriptionEn = "Nutrient-rich farm-produced vermicompost to restore soil fertility naturally.",
                descriptionBn = "খামারে উৎপাদিত প্রাকৃতিক কেঁচো কম্পোস্ট সার, মাটির উর্বরতা বাড়াতে কার্যকর।",
                imageKey = "crops_harvest", unit = ProductUnit.BAG.name, grade = ProductGrade.A_GRADE.name,
                organic = true, pricePerUnit = 680.0, pricePerMon = 680.0, wholesalePricePerUnit = 600.0,
                minOrderQty = 1.0, wholesaleMinMon = 5.0, wholesaleDiscountPct = 12.0, stockQty = 140.0,
                harvestDateLabel = "Fresh Batch", harvestFreshness = HarvestFreshness.YESTERDAY.name,
                estimatedDelivery = "2-3 days", preOrderAvailable = false,
                dealBadge = "100% Organic", discountPct = 0.0, salesCount = 24L, status = ListingStatus.ACTIVE.name
            ),
            "prod_knapsack_sprayer" to Product(
                farmerId = adminUid, farmerName = "Ahsan Habib Nasim", farmLocation = "Chuadanga, Khulna",
                nameEn = "16L Heavy-Duty Farm Sprayer", nameBn = "১৬ লিটার কৃষি স্প্রে মেশিন",
                category = ProductCategory.TOOLS.name,
                descriptionEn = "Durable dual-nozzle agricultural knapsack sprayer for field crops and orchards.",
                descriptionBn = "টেকসই ১৬ লিটার ক্ষমতাসম্পন্ন কৃষি স্প্রে মেশিন।",
                imageKey = "hero_farm", unit = ProductUnit.PIECE.name, grade = ProductGrade.STANDARD.name,
                organic = false, pricePerUnit = 2450.0, pricePerMon = 2450.0, wholesalePricePerUnit = 2200.0,
                minOrderQty = 1.0, wholesaleMinMon = 5.0, wholesaleDiscountPct = 10.0, stockQty = 25.0,
                harvestDateLabel = "In Stock", harvestFreshness = HarvestFreshness.STANDARD.name,
                estimatedDelivery = "2-4 days", preOrderAvailable = false,
                dealBadge = "", discountPct = 0.0, salesCount = 15L, status = ListingStatus.ACTIVE.name
            )
        )
        products.forEach { (id, prod) -> batch.set(db.collection("products").document(id), prod.toCreateMap()) }

        // 5. Livestock Marketplace
        val livestockList = listOf(
            "live_shahiwal_bull" to Livestock(
                farmerId = adminUid, farmerName = "Ahsan Habib Nasim", farmLocation = "Chuadanga, Khulna",
                animalNameEn = "Shahiwal Cross Bull #KA-101", animalNameBn = "শাহীওয়াল ক্রস ষাঁড় #KA-101",
                category = LivestockCategory.COW.name, breed = "Shahiwal Cross", gender = AnimalGender.MALE.name,
                ageMonths = 24L, liveWeightKg = 345.0, estimatedWeightDate = "2026-11-15",
                price = 128000.0, wholesalePrice = 122000.0,
                vaccinationHistory = "FMD, Anthrax, Black Quarter (Up to date)",
                healthInfo = "Vet inspected, natural grass & silage diet, active appetite.",
                descriptionEn = "Healthy farm-raised bull raised on natural green fodder in Chuadanga.",
                descriptionBn = "চুয়াডাঙ্গার খামারে প্রাকৃতিক ঘাস ও ভুট্টা সাইলেজ খাইয়ে পালিত সুস্থ ষাঁড়।",
                imageKey = "livestock_cattle", videoNote = "Live video inspection available via WhatsApp",
                deliveryAvailable = true, estimatedDelivery = "2-3 days via livestock carrier",
                status = ListingStatus.ACTIVE.name
            ),
            "live_bengal_goat" to Livestock(
                farmerId = adminUid, farmerName = "Ahsan Habib Nasim", farmLocation = "Chuadanga, Khulna",
                animalNameEn = "Black Bengal Goat Pair #KA-204", animalNameBn = "ব্ল্যাক বেঙ্গল ছাগল #KA-204",
                category = LivestockCategory.GOAT.name, breed = "Black Bengal", gender = AnimalGender.MALE.name,
                ageMonths = 14L, liveWeightKg = 24.0, estimatedWeightDate = "2026-11-14",
                price = 18500.0, wholesalePrice = 17200.0,
                vaccinationHistory = "PPR & Goat Pox vaccinated",
                healthInfo = "Dewormed, healthy coat, free-range farm raised.",
                descriptionEn = "Authentic Chuadanga Black Bengal goat known for superior meat quality.",
                descriptionBn = "চুয়াডাঙ্গার বিখ্যাত ব্ল্যাক বেঙ্গল জাতের সুস্থ ছাগল।",
                imageKey = "livestock_cattle", videoNote = "Video inspection via WhatsApp call",
                deliveryAvailable = true, estimatedDelivery = "24-48 hours",
                status = ListingStatus.ACTIVE.name
            ),
            "live_sonali_poultry" to Livestock(
                farmerId = adminUid, farmerName = "Ahsan Habib Nasim", farmLocation = "Chuadanga, Khulna",
                animalNameEn = "Deshi Sonali Chicken Lot (20 Birds)", animalNameBn = "দেশি সোনালী মুরগি (২০টি লট)",
                category = LivestockCategory.CHICKEN.name, breed = "Sonali Cross", gender = AnimalGender.FEMALE.name,
                ageMonths = 4L, liveWeightKg = 26.0, estimatedWeightDate = "2026-11-16",
                price = 8400.0, wholesalePrice = 7900.0,
                vaccinationHistory = "Newcastle & Gumboro completed",
                healthInfo = "Antibiotic-free maize-fed poultry batch.",
                descriptionEn = "Farm-fresh maize-fed Sonali chickens ready for households or restaurants.",
                descriptionBn = "ভুট্টা ও প্রাকৃতিক খাদ্যে পালিত ২০টি সুস্থ সোনালী মুরগির লট।",
                imageKey = "livestock_cattle", videoNote = "Batch video available on request",
                deliveryAvailable = true, estimatedDelivery = "24 hours",
                status = ListingStatus.ACTIVE.name
            )
        )
        livestockList.forEach { (id, animal) -> batch.set(db.collection("livestock").document(id), animal.toCreateMap()) }

        // 6. Clearly labeled Sample Reviews (never presented as fabricated authentic reviews)
        val sampleReviews = listOf(
            "rev_sample_1" to Review(
                productId = "prod_maize_chuadanga",
                buyerId = adminUid,
                buyerName = "Sample Wholesale Buyer (Paikar)",
                rating = 5L,
                comment = "[Sample Development Review] Moisture level was perfect and 5+ Mon wholesale pricing applied smoothly.",
                sampleReview = true,
                verifiedPurchase = false
            ),
            "rev_sample_2" to Review(
                productId = "prod_miniket_paddy",
                buyerId = adminUid,
                buyerName = "Sample Dhaka Household Buyer",
                rating = 5L,
                comment = "[Sample Development Review] Freshly harvested aroma and accurate 40 KG per Mon weight.",
                sampleReview = true,
                verifiedPurchase = false
            )
        )
        sampleReviews.forEach { (id, rev) -> batch.set(db.collection("reviews").document(id), rev.toCreateMap()) }

        batch.commit().await()
    }

    suspend fun updateMyRoleAndProfile(
        displayName: String,
        phone: String,
        division: String,
        district: String,
        address: String,
        role: UserRole,
        farmName: String,
        farmLocation: String
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val docPath = "users/$uid"
        return try {
            val safeRole = if (role == UserRole.SUPER_ADMIN && !isBootstrappedAdmin()) UserRole.BUYER else role
            db.collection("users").document(uid).update(
                mapOf(
                    "displayName" to displayName.ifBlank { "Khalid Agro User" },
                    "phone" to phone,
                    "division" to division,
                    "district" to district,
                    "address" to address,
                    "role" to safeRole.name,
                    "farmName" to farmName,
                    "farmLocation" to farmLocation,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            // If user opted in as Farmer, create their unverified storefront card if not present
            if (safeRole == UserRole.FARMER && farmName.isNotBlank()) {
                val fRef = db.collection("farmers").document("farmer_$uid")
                val fSnap = fRef.get().await()
                if (!fSnap.exists()) {
                    val fp = FarmerProfile(
                        ownerUid = uid,
                        name = displayName,
                        farmName = farmName,
                        location = farmLocation.ifBlank { "$district, $division" },
                        division = division,
                        bioEn = "Direct farmer listing on Khalid Agro.",
                        bioBn = "খালিদ অ্যাগ্রোর সরাসরি কৃষক প্রোফাইল।",
                        specialty = "Fresh Agricultural Produce",
                        verified = isBootstrappedAdmin(),
                        contactPhone = phone,
                        whatsappNumber = phone
                    )
                    fRef.set(fp.toCreateMap()).await()
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun saveProduct(product: Product): Result<String> {
        val productId = product.id.ifBlank { "prod_${UUID.randomUUID().toString().take(12)}" }
        val docPath = "products/$productId"
        return try {
            val uid = requireUserId()
            require(product.nameEn.isNotBlank() && product.nameBn.isNotBlank()) { "Product name in English and Bengali required." }
            val ref = db.collection("products").document(productId)
            if (product.id.isBlank()) {
                ref.set(product.copy(farmerId = uid).toCreateMap()).await()
            } else {
                ref.update(
                    mapOf(
                        "nameEn" to product.nameEn,
                        "nameBn" to product.nameBn,
                        "category" to product.category,
                        "descriptionEn" to product.descriptionEn,
                        "descriptionBn" to product.descriptionBn,
                        "unit" to product.unit,
                        "grade" to product.grade,
                        "organic" to product.organic,
                        "pricePerUnit" to product.pricePerUnit,
                        "pricePerMon" to product.pricePerMon,
                        "wholesalePricePerUnit" to product.wholesalePricePerUnit,
                        "stockQty" to product.stockQty,
                        "status" to if (product.stockQty <= 0.0) ListingStatus.SOLD_OUT.name else ListingStatus.ACTIVE.name,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(productId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteProduct(productId: String): Result<Unit> {
        return try {
            requireUserId()
            db.collection("products").document(productId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "products/$productId")
            Result.failure(e)
        }
    }

    suspend fun saveLivestock(animal: Livestock): Result<String> {
        val id = animal.id.ifBlank { "live_${UUID.randomUUID().toString().take(12)}" }
        return try {
            val uid = requireUserId()
            val ref = db.collection("livestock").document(id)
            if (animal.id.isBlank()) {
                ref.set(animal.copy(farmerId = uid).toCreateMap()).await()
            } else {
                ref.update(
                    mapOf(
                        "animalNameEn" to animal.animalNameEn,
                        "animalNameBn" to animal.animalNameBn,
                        "price" to animal.price,
                        "liveWeightKg" to animal.liveWeightKg,
                        "healthInfo" to animal.healthInfo,
                        "vaccinationHistory" to animal.vaccinationHistory,
                        "status" to animal.status,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "livestock/$id")
            Result.failure(e)
        }
    }

    suspend fun updateOrderStatus(
        orderId: String,
        newStatus: OrderStatus,
        paymentStatus: PaymentStatus? = null,
        deliveryAssigneeId: String? = null,
        confirmationNote: String? = null
    ): Result<Unit> {
        return try {
            requireUserId()
            val updates = mutableMapOf<String, Any>(
                "orderStatus" to newStatus.name,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (paymentStatus != null) updates["paymentStatus"] = paymentStatus.name
            if (!deliveryAssigneeId.isNullOrBlank()) updates["deliveryAssigneeId"] = deliveryAssigneeId
            if (!confirmationNote.isNullOrBlank()) updates["deliveryConfirmationNote"] = confirmationNote
            db.collection("orders").document(orderId).update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "orders/$orderId")
            Result.failure(e)
        }
    }

    // --- ADMIN STREAMS & ACTIONS ---
    fun observeAllUsersAdmin(uid: String): Flow<List<UserAccount>> = flow {
        emitAll(
            db.collection("users").snapshots().map { snap ->
                snap.toObjects(UserAccount::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }.catch { error ->
                val fsErr = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (fsErr == null && error is CancellationException) throw error
                if (auth.currentUser?.uid == uid) {
                    handleFirestoreError(fsErr ?: error, OperationType.LIST, "users")
                }
                throw error
            }
        )
    }

    fun observeAllFarmersAdmin(uid: String): Flow<List<FarmerProfile>> = flow {
        emitAll(
            db.collection("farmers").snapshots().map { snap ->
                snap.toObjects(FarmerProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }.catch { error ->
                val fsErr = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (fsErr == null && error is CancellationException) throw error
                if (auth.currentUser?.uid == uid) {
                    handleFirestoreError(fsErr ?: error, OperationType.LIST, "farmers")
                }
                throw error
            }
        )
    }

    fun observeAllOrdersAdmin(uid: String): Flow<List<Order>> = flow {
        emitAll(
            db.collection("orders").snapshots().map { snap ->
                snap.toObjects(Order::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }.catch { error ->
                val fsErr = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (fsErr == null && error is CancellationException) throw error
                if (auth.currentUser?.uid == uid) {
                    handleFirestoreError(fsErr ?: error, OperationType.LIST, "orders")
                }
                throw error
            }
        )
    }

    suspend fun adminVerifyFarmer(farmerDocId: String, ownerUid: String, verified: Boolean): Result<Unit> {
        return try {
            requireUserId()
            db.collection("farmers").document(farmerDocId).update(
                mapOf("verified" to verified, "updatedAt" to FieldValue.serverTimestamp())
            ).await()
            if (ownerUid.isNotBlank()) {
                val uRef = db.collection("users").document(ownerUid)
                if (uRef.get().await().exists()) {
                    uRef.update(
                        mapOf("verifiedFarmer" to verified, "updatedAt" to FieldValue.serverTimestamp())
                    ).await()
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "farmers/$farmerDocId")
            Result.failure(e)
        }
    }

    suspend fun adminUpdateMarketPrice(price: MarketPrice): Result<Unit> {
        val priceId = price.id.ifBlank { "mp_${UUID.randomUUID().toString().take(8)}" }
        return try {
            requireUserId()
            val ref = db.collection("market_prices").document(priceId)
            if (price.id.isBlank()) {
                ref.set(price.toCreateMap()).await()
            } else {
                ref.update(
                    mapOf(
                        "currentPrice" to price.currentPrice,
                        "previousPrice" to price.previousPrice,
                        "movement" to price.movement,
                        "marketLocation" to price.marketLocation,
                        "verifiedBy" to price.verifiedBy,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "market_prices/$priceId")
            Result.failure(e)
        }
    }

    suspend fun adminUpdateSettings(settings: MarketplaceSettings): Result<Unit> {
        return try {
            requireUserId()
            db.collection("settings").document("marketplace_config")
                .set(settings.toCreateMap()).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "settings/marketplace_config")
            Result.failure(e)
        }
    }
}
