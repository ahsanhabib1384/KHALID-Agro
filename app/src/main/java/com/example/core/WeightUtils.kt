package com.example.core

import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToLong

object WeightUtils {
    const val KG_PER_MON = 40.0
    const val DEFAULT_WHOLESALE_MIN_MON = 5.0

    fun monToKg(mon: Double): Double = mon * KG_PER_MON

    fun kgToMon(kg: Double): Double = kg / KG_PER_MON

    fun isWholesaleEligible(
        quantity: Double,
        unit: String,
        wholesaleMinMon: Double = DEFAULT_WHOLESALE_MIN_MON
    ): Boolean {
        val monEquivalent = when (unit.uppercase()) {
            "MON" -> quantity
            "KG" -> kgToMon(quantity)
            else -> if (quantity >= 20.0) wholesaleMinMon else 0.0
        }
        return monEquivalent >= wholesaleMinMon
    }

    fun quantityInKg(quantity: Double, unit: String): Double = when (unit.uppercase()) {
        "MON" -> monToKg(quantity)
        "KG" -> quantity
        else -> quantity
    }

    fun formatTaka(amount: Double): String {
        val formatter = NumberFormat.getNumberInstance(Locale("en", "IN"))
        formatter.maximumFractionDigits = 0
        return "৳${formatter.format(amount.roundToLong())}"
    }

    fun formatWeightSummary(quantity: Double, unit: String, lang: AppLanguage): String {
        return when (unit.uppercase()) {
            "MON" -> {
                val kg = monToKg(quantity).roundToLong()
                if (lang == AppLanguage.BN) "${trimDecimal(quantity)} মণ ($kg কেজি)"
                else "${trimDecimal(quantity)} Mon ($kg KG)"
            }
            "KG" -> {
                val mon = kgToMon(quantity)
                if (lang == AppLanguage.BN) "${trimDecimal(quantity)} কেজি (${trimDecimal(mon)} মণ)"
                else "${trimDecimal(quantity)} KG (${trimDecimal(mon)} Mon)"
            }
            else -> "${trimDecimal(quantity)} $unit"
        }
    }

    fun trimDecimal(value: Double): String {
        return if (value % 1.0 == 0.0) value.toLong().toString()
        else String.format(Locale.US, "%.1f", value)
    }
}
