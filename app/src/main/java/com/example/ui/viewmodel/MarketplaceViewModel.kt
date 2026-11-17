package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.AppLanguage
import com.example.core.LanguageManager
import com.example.core.WeightUtils
import com.example.data.MarketplaceRepository
import com.example.data.MarketplaceSeedAndAdminRepository
import com.example.model.*
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MarketplaceViewModel(
    private val repo: MarketplaceRepository,
    private val adminRepo: MarketplaceSeedAndAdminRepository,
    private val languageManager: LanguageManager,
    val currentUserId: String
) : ViewModel() {

    companion object {
        private const val TAG = "MarketplaceVM"
        private const val STOP_TIMEOUT_MS = 5000L
    }

    val isAdminUser: Boolean = repo.isBootstrappedAdminUser()

    val language: StateFlow<AppLanguage> = languageManager.languageFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), AppLanguage.BN)

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow<ProductCategory?>(null)
    val selectedCategory: StateFlow<ProductCategory?> = _selectedCategory.asStateFlow()

    private val _organicOnly = MutableStateFlow(false)
    val organicOnly: StateFlow<Boolean> = _organicOnly.asStateFlow()

    private val _selectedFreshness = MutableStateFlow<HarvestFreshness?>(null)
    val selectedFreshness: StateFlow<HarvestFreshness?> = _selectedFreshness.asStateFlow()

    private val _selectedLivestockCategory = MutableStateFlow<LivestockCategory?>(null)
    val selectedLivestockCategory: StateFlow<LivestockCategory?> = _selectedLivestockCategory.asStateFlow()

    private val _statusBanner = MutableStateFlow<String?>(null)
    val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    init {
        viewModelScope.launch {
            adminRepo.ensureUserProfileAndBootstrap()
                .onFailure { err ->
                    Log.w(TAG, "Profile bootstrap warning", err)
                }
        }
    }

    private fun <T> Flow<T>.toUiState(label: String): StateFlow<UiState<T>> =
        map<T, UiState<T>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing $label", rootError)
                emit(UiState.Error(rootError.message ?: "Failed to load $label"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
                initialValue = UiState.Loading
            )

    val userAccount: StateFlow<UiState<UserAccount?>> =
        repo.observeUserAccount(currentUserId).toUiState("account")

    val productsState: StateFlow<UiState<List<Product>>> =
        repo.observeActiveProducts(currentUserId).toUiState("products")

    val myProductsState: StateFlow<UiState<List<Product>>> =
        repo.observeMyProducts(currentUserId).toUiState("my products")

    val livestockState: StateFlow<UiState<List<Livestock>>> =
        repo.observeActiveLivestock(currentUserId).toUiState("livestock")

    val myLivestockState: StateFlow<UiState<List<Livestock>>> =
        repo.observeMyLivestock(currentUserId).toUiState("my livestock")

    val marketPricesState: StateFlow<UiState<List<MarketPrice>>> =
        repo.observeMarketPrices(currentUserId).toUiState("market prices")

    val verifiedFarmersState: StateFlow<UiState<List<FarmerProfile>>> =
        repo.observeVerifiedFarmers(currentUserId).toUiState("farmers")

    val cartState: StateFlow<UiState<List<CartItem>>> =
        repo.observeCart(currentUserId).toUiState("cart")

    val favoritesState: StateFlow<UiState<List<FavoriteItem>>> =
        repo.observeFavorites(currentUserId).toUiState("favorites")

    val buyerOrdersState: StateFlow<UiState<List<Order>>> =
        repo.observeBuyerOrders(currentUserId).toUiState("my orders")

    val farmerOrdersState: StateFlow<UiState<List<Order>>> =
        repo.observeFarmerOrders(currentUserId).toUiState("incoming orders")

    val deliveryOrdersState: StateFlow<UiState<List<Order>>> =
        repo.observeDeliveryOrders(currentUserId).toUiState("assigned deliveries")

    val buyerNegotiationsState: StateFlow<UiState<List<Negotiation>>> =
        repo.observeNegotiations(currentUserId, isFarmerSide = false).toUiState("buyer negotiations")

    val farmerNegotiationsState: StateFlow<UiState<List<Negotiation>>> =
        repo.observeNegotiations(currentUserId, isFarmerSide = true).toUiState("farmer negotiations")

    val reviewsState: StateFlow<UiState<List<Review>>> =
        repo.observeReviews(currentUserId).toUiState("reviews")

    val settingsState: StateFlow<MarketplaceSettings> =
        repo.observeSettings(currentUserId)
            .catch { emit(MarketplaceSettings()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), MarketplaceSettings())

    val adminUsersState: StateFlow<UiState<List<UserAccount>>> =
        if (isAdminUser) adminRepo.observeAllUsersAdmin(currentUserId).toUiState("admin users")
        else MutableStateFlow(UiState.Success(emptyList()))

    val adminFarmersState: StateFlow<UiState<List<FarmerProfile>>> =
        if (isAdminUser) adminRepo.observeAllFarmersAdmin(currentUserId).toUiState("admin farmers")
        else MutableStateFlow(UiState.Success(emptyList()))

    val adminOrdersState: StateFlow<UiState<List<Order>>> =
        if (isAdminUser) adminRepo.observeAllOrdersAdmin(currentUserId).toUiState("admin orders")
        else MutableStateFlow(UiState.Success(emptyList()))

    fun observeMessages(negotiation: Negotiation): Flow<List<NegotiationMessage>> =
        repo.observeNegotiationMessages(
            negotiationId = negotiation.id,
            uid = currentUserId,
            isFarmerSide = (negotiation.farmerId == currentUserId)
        ).catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            emit(emptyList())
        }

    fun toggleLanguage() {
        viewModelScope.launch {
            val next = if (language.value == AppLanguage.BN) AppLanguage.EN else AppLanguage.BN
            languageManager.setLanguage(next)
        }
    }

    fun setSearchQuery(query: String) { _searchQuery.value = query }
    fun setCategory(category: ProductCategory?) { _selectedCategory.value = category }
    fun setOrganicOnly(only: Boolean) { _organicOnly.value = only }
    fun setFreshness(freshness: HarvestFreshness?) { _selectedFreshness.value = freshness }
    fun setLivestockCategory(category: LivestockCategory?) { _selectedLivestockCategory.value = category }
    fun clearBanner() { _statusBanner.value = null }

    fun addProductToCart(product: Product, quantity: Double) {
        viewModelScope.launch {
            _busy.value = true
            val item = CartItem(
                productId = product.id,
                listingType = "PRODUCT",
                farmerId = product.farmerId,
                farmerName = product.farmerName,
                titleEn = product.nameEn,
                titleBn = product.nameBn,
                unit = product.unit,
                quantity = quantity,
                unitPrice = product.pricePerUnit,
                wholesalePrice = product.wholesalePricePerUnit,
                wholesaleMinMon = product.wholesaleMinMon,
                imageKey = product.imageKey
            )
            repo.addToCart(item)
                .onSuccess {
                    _statusBanner.value = if (language.value == AppLanguage.BN) {
                        "${product.nameBn} কার্টে যোগ করা হয়েছে"
                    } else {
                        "${product.nameEn} added to cart"
                    }
                }
                .onFailure { _statusBanner.value = it.localizedMessage ?: "Failed to add to cart" }
            _busy.value = false
        }
    }

    fun addLivestockToCart(animal: Livestock, quantity: Double = 1.0) {
        viewModelScope.launch {
            _busy.value = true
            val item = CartItem(
                productId = animal.id,
                listingType = "LIVESTOCK",
                farmerId = animal.farmerId,
                farmerName = animal.farmerName,
                titleEn = animal.animalNameEn,
                titleBn = animal.animalNameBn,
                unit = ProductUnit.PIECE.name,
                quantity = quantity,
                unitPrice = animal.price,
                wholesalePrice = animal.wholesalePrice,
                wholesaleMinMon = 5.0,
                imageKey = animal.imageKey
            )
            repo.addToCart(item)
                .onSuccess {
                    _statusBanner.value = if (language.value == AppLanguage.BN) {
                        "${animal.animalNameBn} কার্টে যোগ হয়েছে"
                    } else {
                        "${animal.animalNameEn} added to cart"
                    }
                }
                .onFailure { _statusBanner.value = it.localizedMessage ?: "Failed to add livestock" }
            _busy.value = false
        }
    }

    fun updateCartQuantity(item: CartItem, newQuantity: Double) {
        viewModelScope.launch {
            val price = if (WeightUtils.isWholesaleEligible(newQuantity, item.unit, item.wholesaleMinMon)) {
                item.wholesalePrice
            } else item.unitPrice
            repo.updateCartQuantity(item.productId, newQuantity, price)
                .onFailure { _statusBanner.value = it.localizedMessage ?: "Cart update failed" }
        }
    }

    fun removeFromCart(productId: String) {
        viewModelScope.launch {
            repo.removeFromCart(productId)
                .onFailure { _statusBanner.value = it.localizedMessage ?: "Remove failed" }
        }
    }

    fun toggleFavorite(
        targetId: String,
        targetType: String,
        titleEn: String,
        titleBn: String,
        subtitle: String,
        currentlyFav: Boolean
    ) {
        viewModelScope.launch {
            repo.toggleFavorite(
                FavoriteItem(
                    targetId = targetId,
                    targetType = targetType,
                    titleEn = titleEn,
                    titleBn = titleBn,
                    subtitle = subtitle
                ),
                currentlyFav
            ).onFailure { _statusBanner.value = it.localizedMessage ?: "Favorite update failed" }
        }
    }

    fun placeOrder(
        buyerName: String,
        buyerPhone: String,
        deliveryAddress: String,
        district: String,
        paymentMethod: PaymentMethod,
        notes: String,
        onOrderPlaced: (String) -> Unit
    ) {
        val currentCart = (cartState.value as? UiState.Success)?.data.orEmpty()
        if (currentCart.isEmpty()) {
            _statusBanner.value = "Cart is empty"
            return
        }
        viewModelScope.launch {
            _busy.value = true
            repo.placeOrderAtomic(
                cartItems = currentCart,
                buyerName = buyerName,
                buyerPhone = buyerPhone,
                deliveryAddress = deliveryAddress,
                district = district,
                paymentMethod = paymentMethod,
                notes = notes,
                settings = settingsState.value
            ).onSuccess { orderId ->
                _statusBanner.value = if (language.value == AppLanguage.BN) {
                    "অর্ডার সফল হয়েছে (#${orderId.takeLast(6)})"
                } else {
                    "Order confirmed (#${orderId.takeLast(6)})"
                }
                onOrderPlaced(orderId)
            }.onFailure {
                _statusBanner.value = it.localizedMessage ?: "Order failed"
            }
            _busy.value = false
        }
    }

    fun startNegotiation(
        product: Product,
        requestedQtyMon: Double,
        offeredPrice: Double,
        message: String,
        onCreated: () -> Unit
    ) {
        val buyerName = (userAccount.value as? UiState.Success)?.data?.displayName ?: "Buyer"
        viewModelScope.launch {
            _busy.value = true
            repo.startNegotiation(product, buyerName, requestedQtyMon, offeredPrice, message)
                .onSuccess {
                    _statusBanner.value = if (language.value == AppLanguage.BN) "দাম প্রস্তাব পাঠানো হয়েছে" else "Price offer sent to farmer"
                    onCreated()
                }
                .onFailure { _statusBanner.value = it.localizedMessage ?: "Failed to start negotiation" }
            _busy.value = false
        }
    }

    fun sendNegotiationMessage(
        negotiation: Negotiation,
        text: String,
        offeredPrice: Double? = null,
        nextStatus: NegotiationStatus? = null
    ) {
        val senderName = (userAccount.value as? UiState.Success)?.data?.displayName ?: "User"
        viewModelScope.launch {
            repo.sendNegotiationMessage(negotiation, senderName, text, offeredPrice, nextStatus)
                .onFailure { _statusBanner.value = it.localizedMessage ?: "Message failed" }
        }
    }

    fun submitReview(productId: String, rating: Long, comment: String) {
        val buyerName = (userAccount.value as? UiState.Success)?.data?.displayName ?: "Verified Buyer"
        viewModelScope.launch {
            _busy.value = true
            repo.submitReview(productId, buyerName, rating, comment)
                .onSuccess {
                    _statusBanner.value = if (language.value == AppLanguage.BN) "রিভিউ যুক্ত হয়েছে" else "Review submitted"
                }
                .onFailure { _statusBanner.value = it.localizedMessage ?: "Review failed" }
            _busy.value = false
        }
    }

    fun updateRoleAndProfile(
        displayName: String,
        phone: String,
        division: String,
        district: String,
        address: String,
        role: UserRole,
        farmName: String,
        farmLocation: String
    ) {
        viewModelScope.launch {
            _busy.value = true
            adminRepo.updateMyRoleAndProfile(displayName, phone, division, district, address, role, farmName, farmLocation)
                .onSuccess {
                    _statusBanner.value = if (language.value == AppLanguage.BN) "প্রোফাইল আপডেট হয়েছে" else "Profile updated"
                }
                .onFailure { _statusBanner.value = it.localizedMessage ?: "Profile update failed" }
            _busy.value = false
        }
    }

    fun saveProduct(product: Product, onSaved: () -> Unit) {
        viewModelScope.launch {
            _busy.value = true
            adminRepo.saveProduct(product)
                .onSuccess {
                    _statusBanner.value = if (language.value == AppLanguage.BN) "পণ্য সংরক্ষণ হয়েছে" else "Product saved"
                    onSaved()
                }
                .onFailure { _statusBanner.value = it.localizedMessage ?: "Save failed" }
            _busy.value = false
        }
    }

    fun deleteProduct(productId: String) {
        viewModelScope.launch {
            adminRepo.deleteProduct(productId)
                .onSuccess { _statusBanner.value = "Listing deleted" }
                .onFailure { _statusBanner.value = it.localizedMessage ?: "Delete failed" }
        }
    }

    fun saveLivestock(animal: Livestock, onSaved: () -> Unit) {
        viewModelScope.launch {
            _busy.value = true
            adminRepo.saveLivestock(animal)
                .onSuccess {
                    _statusBanner.value = if (language.value == AppLanguage.BN) "গবাদিপশু যুক্ত হয়েছে" else "Livestock saved"
                    onSaved()
                }
                .onFailure { _statusBanner.value = it.localizedMessage ?: "Save failed" }
            _busy.value = false
        }
    }

    fun updateOrderStatus(
        orderId: String,
        status: OrderStatus,
        paymentStatus: PaymentStatus? = null,
        deliveryAssigneeId: String? = null,
        confirmationNote: String? = null
    ) {
        viewModelScope.launch {
            adminRepo.updateOrderStatus(orderId, status, paymentStatus, deliveryAssigneeId, confirmationNote)
                .onSuccess {
                    _statusBanner.value = if (language.value == AppLanguage.BN) "অর্ডার স্ট্যাটাস আপডেট হয়েছে" else "Order status updated"
                }
                .onFailure { _statusBanner.value = it.localizedMessage ?: "Update failed" }
        }
    }

    fun adminVerifyFarmer(farmerId: String, ownerUid: String, verified: Boolean) {
        viewModelScope.launch {
            adminRepo.adminVerifyFarmer(farmerId, ownerUid, verified)
                .onSuccess { _statusBanner.value = if (verified) "Farmer verified" else "Verification revoked" }
                .onFailure { _statusBanner.value = it.localizedMessage ?: "Admin action failed" }
        }
    }

    fun adminUpdateMarketPrice(price: MarketPrice) {
        viewModelScope.launch {
            adminRepo.adminUpdateMarketPrice(price)
                .onSuccess { _statusBanner.value = if (language.value == AppLanguage.BN) "বাজার দর আপডেট হয়েছে" else "Market price updated" }
                .onFailure { _statusBanner.value = it.localizedMessage ?: "Market price update failed" }
        }
    }

    fun adminUpdateSettings(settings: MarketplaceSettings) {
        viewModelScope.launch {
            adminRepo.adminUpdateSettings(settings)
                .onSuccess { _statusBanner.value = "Settings saved" }
                .onFailure { _statusBanner.value = it.localizedMessage ?: "Settings update failed" }
        }
    }
}
