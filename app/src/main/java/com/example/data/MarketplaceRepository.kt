package com.example.data

import android.content.Context
import com.example.R
import com.example.core.OperationType
import com.example.core.WeightUtils
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

class MarketplaceRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    companion object {
        const val SUPER_ADMIN_EMAIL = "ahsanhabib5832@gmail.com"
    }

    private fun requireUserId(): String =
        auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")

    fun isBootstrappedAdminUser(): Boolean {
        val u = auth.currentUser ?: return false
        return u.isEmailVerified && u.email.equals(SUPER_ADMIN_EMAIL, ignoreCase = true)
    }

    private fun <T> safeCollectionFlow(
        path: String,
        uid: String,
        block: () -> Flow<List<T>>
    ): Flow<List<T>> = flow {
        emitAll(
            block().catch { error ->
                val fsErr = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (fsErr == null && error is CancellationException) throw error
                val ex = fsErr ?: (error as? Exception)
                if (ex != null && auth.currentUser?.uid == uid) {
                    handleFirestoreError(ex, OperationType.LIST, path)
                }
                throw error
            }
        )
    }

    fun observeUserAccount(uid: String): Flow<UserAccount?> = flow {
        val docRef = db.collection("users").document(uid)
        emitAll(
            docRef.snapshots().map { snap ->
                if (snap.exists()) {
                    snap.toObject(UserAccount::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                } else null
            }.catch { error ->
                val fsErr = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (fsErr == null && error is CancellationException) throw error
                if (auth.currentUser?.uid == uid) {
                    handleFirestoreError(fsErr ?: error, OperationType.GET, docRef.path)
                }
                throw error
            }
        )
    }

    fun observeActiveProducts(uid: String): Flow<List<Product>> =
        safeCollectionFlow("products", uid) {
            db.collection("products")
                .whereEqualTo("status", ListingStatus.ACTIVE.name)
                .snapshots()
                .map { snap ->
                    snap.toObjects(Product::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.salesCount }
                }
        }

    fun observeMyProducts(uid: String): Flow<List<Product>> =
        safeCollectionFlow("products?farmerId=$uid", uid) {
            db.collection("products")
                .whereEqualTo("farmerId", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(Product::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.updatedAt ?: Timestamp(0, 0) }
                }
        }

    fun observeActiveLivestock(uid: String): Flow<List<Livestock>> =
        safeCollectionFlow("livestock", uid) {
            db.collection("livestock")
                .whereEqualTo("status", ListingStatus.ACTIVE.name)
                .snapshots()
                .map { snap ->
                    snap.toObjects(Livestock::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
        }

    fun observeMyLivestock(uid: String): Flow<List<Livestock>> =
        safeCollectionFlow("livestock?farmerId=$uid", uid) {
            db.collection("livestock")
                .whereEqualTo("farmerId", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(Livestock::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.updatedAt ?: Timestamp(0, 0) }
                }
        }

    fun observeMarketPrices(uid: String): Flow<List<MarketPrice>> =
        safeCollectionFlow("market_prices", uid) {
            db.collection("market_prices")
                .whereEqualTo("active", true)
                .snapshots()
                .map { snap ->
                    snap.toObjects(MarketPrice::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.productNameEn }
                }
        }

    fun observeVerifiedFarmers(uid: String): Flow<List<FarmerProfile>> =
        safeCollectionFlow("farmers", uid) {
            db.collection("farmers")
                .whereEqualTo("verified", true)
                .snapshots()
                .map { snap ->
                    snap.toObjects(FarmerProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.totalSales }
                }
        }

    fun observeCart(uid: String): Flow<List<CartItem>> =
        safeCollectionFlow("users/$uid/cart", uid) {
            db.collection("users").document(uid).collection("cart")
                .snapshots()
                .map { snap ->
                    snap.toObjects(CartItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
        }

    fun observeFavorites(uid: String): Flow<List<FavoriteItem>> =
        safeCollectionFlow("users/$uid/favorites", uid) {
            db.collection("users").document(uid).collection("favorites")
                .snapshots()
                .map { snap ->
                    snap.toObjects(FavoriteItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
        }

    fun observeBuyerOrders(uid: String): Flow<List<Order>> =
        safeCollectionFlow("orders?buyerId=$uid", uid) {
            db.collection("orders")
                .whereEqualTo("buyerId", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(Order::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
        }

    fun observeFarmerOrders(uid: String): Flow<List<Order>> =
        safeCollectionFlow("orders?farmerId=$uid", uid) {
            db.collection("orders")
                .whereEqualTo("farmerId", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(Order::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
        }

    fun observeDeliveryOrders(uid: String): Flow<List<Order>> =
        safeCollectionFlow("orders?deliveryAssigneeId=$uid", uid) {
            db.collection("orders")
                .whereEqualTo("deliveryAssigneeId", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(Order::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
        }

    fun observeNegotiations(uid: String, isFarmerSide: Boolean): Flow<List<Negotiation>> =
        safeCollectionFlow("negotiations", uid) {
            val field = if (isFarmerSide) "farmerId" else "buyerId"
            db.collection("negotiations")
                .whereEqualTo(field, uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(Negotiation::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.updatedAt ?: Timestamp(0, 0) }
                }
        }

    fun observeNegotiationMessages(
        negotiationId: String,
        uid: String,
        isFarmerSide: Boolean
    ): Flow<List<NegotiationMessage>> =
        safeCollectionFlow("negotiations/$negotiationId/messages", uid) {
            val field = if (isFarmerSide) "farmerId" else "buyerId"
            db.collection("negotiations").document(negotiationId).collection("messages")
                .whereEqualTo(field, uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(NegotiationMessage::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.createdAt ?: Timestamp(0, 0) }
                }
        }

    fun observeReviews(uid: String): Flow<List<Review>> =
        safeCollectionFlow("reviews", uid) {
            db.collection("reviews")
                .whereGreaterThanOrEqualTo("rating", 1)
                .snapshots()
                .map { snap ->
                    snap.toObjects(Review::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
        }

    fun observeSettings(uid: String): Flow<MarketplaceSettings> = flow {
        val docRef = db.collection("settings").document("marketplace_config")
        emitAll(
            docRef.snapshots().map { snap ->
                snap.toObject(MarketplaceSettings::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    ?: MarketplaceSettings()
            }.catch { error ->
                val fsErr = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (fsErr == null && error is CancellationException) throw error
                if (auth.currentUser?.uid == uid) {
                    handleFirestoreError(fsErr ?: error, OperationType.GET, docRef.path)
                }
                emit(MarketplaceSettings())
            }
        )
    }

    suspend fun addToCart(item: CartItem): Result<Unit> {
        val docPath = "users/${auth.currentUser?.uid ?: "unauth"}/cart/${item.productId}"
        return try {
            val uid = requireUserId()
            require(item.quantity > 0) { "Quantity must be greater than zero." }
            val ref = db.collection("users").document(uid).collection("cart").document(item.productId)
            val existing = ref.get().await()
            if (existing.exists()) {
                val prevQty = existing.getDouble("quantity") ?: 0.0
                val newQty = prevQty + item.quantity
                val effectivePrice = if (WeightUtils.isWholesaleEligible(newQty, item.unit, item.wholesaleMinMon)) {
                    item.wholesalePrice
                } else item.unitPrice
                ref.update(
                    mapOf(
                        "quantity" to newQty,
                        "unitPrice" to effectivePrice,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                val effectivePrice = if (WeightUtils.isWholesaleEligible(item.quantity, item.unit, item.wholesaleMinMon)) {
                    item.wholesalePrice
                } else item.unitPrice
                ref.set(item.copy(unitPrice = effectivePrice).toCreateMap()).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateCartQuantity(productId: String, quantity: Double, unitPrice: Double): Result<Unit> {
        val docPath = "users/${auth.currentUser?.uid ?: "unauth"}/cart/$productId"
        return try {
            val uid = requireUserId()
            val ref = db.collection("users").document(uid).collection("cart").document(productId)
            if (quantity <= 0) {
                ref.delete().await()
            } else {
                ref.update(
                    mapOf(
                        "quantity" to quantity,
                        "unitPrice" to unitPrice,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun removeFromCart(productId: String): Result<Unit> {
        val docPath = "users/${auth.currentUser?.uid ?: "unauth"}/cart/$productId"
        return try {
            val uid = requireUserId()
            db.collection("users").document(uid).collection("cart").document(productId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun toggleFavorite(fav: FavoriteItem, isCurrentlyFav: Boolean): Result<Unit> {
        val docPath = "users/${auth.currentUser?.uid ?: "unauth"}/favorites/${fav.targetId}"
        return try {
            val uid = requireUserId()
            val ref = db.collection("users").document(uid).collection("favorites").document(fav.targetId)
            if (isCurrentlyFav) {
                ref.delete().await()
            } else {
                ref.set(fav.toCreateMap()).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun placeOrderAtomic(
        cartItems: List<CartItem>,
        buyerName: String,
        buyerPhone: String,
        deliveryAddress: String,
        district: String,
        paymentMethod: PaymentMethod,
        notes: String,
        settings: MarketplaceSettings
    ): Result<String> {
        val orderId = "ord_${UUID.randomUUID().toString().take(12)}"
        val docPath = "orders/$orderId"
        return try {
            val uid = requireUserId()
            require(cartItems.isNotEmpty()) { "Cart is empty." }
            require(buyerPhone.length >= 6) { "Valid phone number required." }
            require(deliveryAddress.length >= 3) { "Delivery address required." }

            val firstFarmerId = cartItems.first().farmerId
            val firstFarmerName = cartItems.first().farmerName

            var totalWeightKg = 0.0
            var subtotal = 0.0
            var discountTotal = 0.0
            var isWholesale = false

            val orderLines = cartItems.map { item ->
                val weightKg = WeightUtils.quantityInKg(item.quantity, item.unit)
                totalWeightKg += weightKg
                val wholesaleHit = WeightUtils.isWholesaleEligible(item.quantity, item.unit, item.wholesaleMinMon)
                if (wholesaleHit) isWholesale = true
                val activePrice = if (wholesaleHit && item.wholesalePrice > 0) item.wholesalePrice else item.unitPrice
                val regularCost = item.unitPrice * item.quantity
                val actualCost = activePrice * item.quantity
                if (regularCost > actualCost) discountTotal += (regularCost - actualCost)
                subtotal += actualCost
                OrderItemLine(
                    productId = item.productId,
                    listingType = item.listingType,
                    title = item.titleEn,
                    qty = item.quantity,
                    unit = item.unit,
                    price = activePrice
                )
            }

            val deliveryCharge = if (subtotal >= settings.freeDeliveryMinAmount) 0.0 else settings.baseDeliveryCharge
            val totalAmount = subtotal + deliveryCharge

            db.runTransaction { tx ->
                // Check & decrement stock atomically to prevent overselling
                val updates = mutableListOf<Triple<com.google.firebase.firestore.DocumentReference, Map<String, Any>, Boolean>>()
                for (item in cartItems) {
                    if (item.listingType == "PRODUCT") {
                        val pRef = db.collection("products").document(item.productId)
                        val snap = tx.get(pRef)
                        if (snap.exists()) {
                            val currentStock = snap.getDouble("stockQty") ?: 0.0
                            val currentSales = snap.getLong("salesCount") ?: 0L
                            if (currentStock < item.quantity) {
                                throw IllegalStateException("Insufficient stock for ${item.titleEn}")
                            }
                            val nextStock = currentStock - item.quantity
                            val nextStatus = if (nextStock <= 0.0) ListingStatus.SOLD_OUT.name else ListingStatus.ACTIVE.name
                            updates.add(
                                Triple(
                                    pRef,
                                    mapOf(
                                        "stockQty" to nextStock,
                                        "salesCount" to (currentSales + 1L),
                                        "status" to nextStatus,
                                        "updatedAt" to FieldValue.serverTimestamp()
                                    ),
                                    true
                                )
                            )
                        }
                    } else {
                        val lRef = db.collection("livestock").document(item.productId)
                        val snap = tx.get(lRef)
                        if (snap.exists()) {
                            val status = snap.getString("status") ?: ListingStatus.ACTIVE.name
                            if (status != ListingStatus.ACTIVE.name) {
                                throw IllegalStateException("${item.titleEn} is no longer available.")
                            }
                            updates.add(
                                Triple(
                                    lRef,
                                    mapOf(
                                        "status" to ListingStatus.SOLD_OUT.name,
                                        "updatedAt" to FieldValue.serverTimestamp()
                                    ),
                                    true
                                )
                            )
                        }
                    }
                }

                for ((ref, payload, _) in updates) {
                    tx.update(ref, payload)
                }

                val summary = orderLines.joinToString(", ") { "${WeightUtils.trimDecimal(it.qty)} ${it.unit} ${it.title}" }
                    .take(450)
                val order = Order(
                    buyerId = uid,
                    buyerName = buyerName.ifBlank { "Khalid Agro Buyer" },
                    buyerPhone = buyerPhone,
                    deliveryAddress = deliveryAddress,
                    district = district,
                    farmerId = firstFarmerId,
                    farmerName = firstFarmerName,
                    itemsSummary = summary,
                    items = orderLines,
                    totalWeightKg = totalWeightKg,
                    subtotal = subtotal,
                    deliveryCharge = deliveryCharge,
                    discountAmount = discountTotal,
                    totalAmount = totalAmount,
                    wholesaleOrder = isWholesale,
                    paymentMethod = paymentMethod.name,
                    paymentStatus = if (paymentMethod == PaymentMethod.CASH_ON_DELIVERY) PaymentStatus.PENDING.name else PaymentStatus.RESERVED.name,
                    orderStatus = OrderStatus.PENDING.name,
                    notes = notes
                )
                tx.set(db.collection("orders").document(orderId), order.toCreateMap())
            }.await()

            // Clear user's cart after confirmed transaction
            val cartSnap = db.collection("users").document(uid).collection("cart").get().await()
            val batch = db.batch()
            cartSnap.documents.forEach { batch.delete(it.reference) }
            batch.commit().await()

            Result.success(orderId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun startNegotiation(
        product: Product,
        buyerName: String,
        requestedQtyMon: Double,
        offeredPrice: Double,
        message: String
    ): Result<String> {
        val negId = "neg_${UUID.randomUUID().toString().take(12)}"
        return try {
            val uid = requireUserId()
            val neg = Negotiation(
                buyerId = uid,
                buyerName = buyerName,
                farmerId = product.farmerId,
                farmerName = product.farmerName,
                productId = product.id,
                productTitle = "${product.nameBn} (${product.nameEn})",
                requestedQtyMon = requestedQtyMon,
                listedPricePerUnit = product.pricePerUnit,
                offeredPricePerUnit = offeredPrice,
                counterPricePerUnit = offeredPrice,
                status = NegotiationStatus.OPEN.name,
                lastMessage = message
            )
            db.collection("negotiations").document(negId).set(neg.toCreateMap()).await()
            val msgId = "msg_${UUID.randomUUID().toString().take(12)}"
            val msg = NegotiationMessage(
                buyerId = uid,
                farmerId = product.farmerId,
                senderId = uid,
                senderName = buyerName,
                text = message,
                offeredPrice = offeredPrice
            )
            db.collection("negotiations").document(negId)
                .collection("messages").document(msgId)
                .set(msg.toCreateMap()).await()
            Result.success(negId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "negotiations/$negId")
            Result.failure(e)
        }
    }

    suspend fun sendNegotiationMessage(
        negotiation: Negotiation,
        senderName: String,
        text: String,
        offeredPrice: Double?,
        nextStatus: NegotiationStatus?
    ): Result<Unit> {
        val msgId = "msg_${UUID.randomUUID().toString().take(12)}"
        return try {
            val uid = requireUserId()
            val msg = NegotiationMessage(
                buyerId = negotiation.buyerId,
                farmerId = negotiation.farmerId,
                senderId = uid,
                senderName = senderName,
                text = text,
                offeredPrice = offeredPrice
            )
            db.collection("negotiations").document(negotiation.id)
                .collection("messages").document(msgId)
                .set(msg.toCreateMap()).await()

            val updates = mutableMapOf<String, Any>(
                "lastMessage" to text.take(450),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (offeredPrice != null) {
                if (uid == negotiation.farmerId) updates["counterPricePerUnit"] = offeredPrice
                else updates["offeredPricePerUnit"] = offeredPrice
            }
            if (nextStatus != null) {
                updates["status"] = nextStatus.name
            }
            db.collection("negotiations").document(negotiation.id).update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "negotiations/${negotiation.id}")
            Result.failure(e)
        }
    }

    suspend fun submitReview(productId: String, buyerName: String, rating: Long, comment: String): Result<Unit> {
        val reviewId = "rev_${UUID.randomUUID().toString().take(12)}"
        return try {
            val uid = requireUserId()
            require(rating in 1L..5L) { "Rating must be between 1 and 5." }
            require(comment.isNotBlank()) { "Review comment cannot be empty." }
            val review = Review(
                productId = productId,
                buyerId = uid,
                buyerName = buyerName,
                rating = rating,
                comment = comment.take(750),
                sampleReview = false,
                verifiedPurchase = true
            )
            db.collection("reviews").document(reviewId).set(review.toCreateMap()).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "reviews/$reviewId")
            Result.failure(e)
        }
    }
}
