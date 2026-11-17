package com.example.model

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

inline fun <reified T : Enum<T>> safeEnum(raw: String?, default: T): T =
    enumValues<T>().firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: default

enum class UserRole(val bn: String, val en: String) {
    BUYER("ক্রেতা", "Buyer"),
    WHOLESALER("পাইকার / পাইকারি ক্রেতা", "Wholesale Buyer (Paikar)"),
    FARMER("কৃষক / বিক্রেতা", "Farmer / Seller"),
    DELIVERY("ডেলিভারি পার্টনার", "Delivery Personnel"),
    SUPER_ADMIN("সুপার অ্যাডমিন", "Super Admin")
}

enum class ProductCategory(val bn: String, val en: String) {
    FRESH_CROPS("টাটকা ফসল ও সবজি", "Fresh Crops & Vegetables"),
    RICE_PADDY("চাল ও ধান", "Rice & Paddy"),
    MAIZE("ভুট্টা", "Maize"),
    POTATO_ONION("আলু ও পেঁয়াজ", "Potato & Onion"),
    SEEDS("বীজ", "Seeds"),
    FERTILIZERS("সার ও জৈব সার", "Fertilizers"),
    TOOLS("কৃষি যন্ত্রপাতি", "Agricultural Tools")
}

enum class ProductUnit(val bn: String, val en: String) {
    KG("কেজি", "KG"),
    MON("মণ (৪০ কেজি)", "Mon (40 KG)"),
    PIECE("পিস", "Piece"),
    PACKET("প্যাকেট", "Packet"),
    BAG("বস্তা", "Bag")
}

enum class ProductGrade(val bn: String, val en: String) {
    A_GRADE("এ গ্রেড", "A Grade"),
    B_GRADE("বি গ্রেড", "B Grade"),
    STANDARD("স্ট্যান্ডার্ড", "Standard")
}

enum class HarvestFreshness(val bn: String, val en: String) {
    TODAY("আজকের তোলা", "Harvested Today"),
    YESTERDAY("গতকালের তোলা", "Harvested Yesterday"),
    TWO_DAYS_AGO("২ দিন আগের তোলা", "Harvested 2 Days Ago"),
    STANDARD("নিয়মিত মজুদ", "Standard Stock")
}

enum class ListingStatus(val bn: String, val en: String) {
    ACTIVE("সক্রিয়", "Active"),
    SOLD_OUT("স্টক শেষ", "Sold Out"),
    DRAFT("খসড়া", "Draft")
}

enum class LivestockCategory(val bn: String, val en: String) {
    COW("গরু", "Cow"),
    GOAT("ছাগল", "Goat"),
    SHEEP("ভেড়া", "Sheep"),
    CHICKEN("মুরগি", "Chicken"),
    DUCK("হাঁস", "Duck")
}

enum class AnimalGender(val bn: String, val en: String) {
    MALE("ষাঁড় / পাঁঠা / মোরগ", "Male"),
    FEMALE("গাভী / ছাগী / মুরগি", "Female")
}

enum class PriceMovement(val bn: String, val en: String) {
    UP("বেড়েছে", "Up"),
    DOWN("কমেছে", "Down"),
    STABLE("স্থিতিশীল", "Stable")
}

enum class OrderStatus(val bn: String, val en: String) {
    PENDING("অপেক্ষমান", "Pending"),
    CONFIRMED("নিশ্চিত", "Confirmed"),
    PACKED("প্যাকিং সম্পন্ন", "Packed"),
    OUT_FOR_DELIVERY("ডেলিভারির পথে", "Out for Delivery"),
    DELIVERED("ডেলিভারি সম্পন্ন", "Delivered"),
    CANCELLED("বাতিল", "Cancelled")
}

enum class PaymentMethod(val bn: String, val en: String) {
    CASH_ON_DELIVERY("ক্যাশ অন ডেলিভারি", "Cash on Delivery"),
    BKASH_MANUAL("বিকাশ (ম্যানুয়াল)", "bKash (Manual)"),
    NAGAD_MANUAL("নগদ (ম্যানুয়াল)", "Nagad (Manual)"),
    BANK_TRANSFER("ব্যাংক ট্রান্সফার", "Bank Transfer")
}

enum class PaymentStatus(val bn: String, val en: String) {
    PENDING("অপেক্ষমান", "Pending"),
    PAID("পরিশোধিত", "Paid"),
    RESERVED("সংরক্ষিত", "Reserved")
}

enum class NegotiationStatus(val bn: String, val en: String) {
    OPEN("চলমান", "Open"),
    COUNTERED("পাল্টা প্রস্তাব", "Countered"),
    ACCEPTED("গৃহীত", "Accepted"),
    REJECTED("প্রত্যাখ্যাত", "Rejected")
}
