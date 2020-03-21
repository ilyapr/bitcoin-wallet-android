package com.bitzhash.wallet.bitcoin

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.bitzhash.wallet.bitcoin.core.AddressType
import java.lang.Exception

fun Context.setStoredWords(words: String) = getSharedPreferences().edit().putString("words", words).apply()
fun Context.getStoredWords(): String? = getSharedPreferences().getString("words", "")

fun Context.setStoredAddressType(type: AddressType) = getSharedPreferences().edit().putString("address_type", type.type()).apply()
fun Context.getStoredAddressType() = AddressType.valueOf(getSharedPreferences().getString("address_type", "")
    ?: throw Exception("null address type"))

fun Context.setStoredPin(pin: String) = getSharedPreferences().edit().putString("pin", pin).apply()
fun Context.getStoredPin(): String? = getSharedPreferences().getString("pin", "")

private fun Context.getSharedPreferences() = EncryptedSharedPreferences.create(
    "secret_shared_prefs",
    MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC),
    this,
    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
)
