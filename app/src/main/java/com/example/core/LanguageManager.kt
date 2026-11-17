package com.example.core

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "khalid_agro_prefs")

enum class AppLanguage {
    BN, EN
}

class LanguageManager(private val context: Context) {
    private val langKey = stringPreferencesKey("app_language")

    val languageFlow: Flow<AppLanguage> = context.dataStore.data.map { prefs ->
        when (prefs[langKey]) {
            AppLanguage.EN.name -> AppLanguage.EN
            else -> AppLanguage.BN
        }
    }

    suspend fun setLanguage(lang: AppLanguage) {
        context.dataStore.edit { prefs ->
            prefs[langKey] = lang.name
        }
    }
}

fun tr(lang: AppLanguage, bn: String, en: String): String =
    if (lang == AppLanguage.BN) bn else en
