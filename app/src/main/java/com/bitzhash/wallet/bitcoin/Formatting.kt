package com.bitzhash.wallet.bitcoin

import android.content.Context
import com.bitzhash.wallet.bitcoin.core.WalletViewModel
import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*

internal fun formatNumber(number: BigDecimal) = formatNumber(number.toDouble())
internal fun formatNumber(number: Double) =
    if (number.toString().contains(".")) {
        val df = DecimalFormat("0", DecimalFormatSymbols.getInstance(Locale.ENGLISH))
        df.maximumFractionDigits = 340
        df.format(number)
    } else String.format("%d", number.toLong())
internal fun formatSignedNumber(number: BigDecimal) = formatSignedNumber(number.toDouble())
internal fun formatSignedNumber(number: Double) = if (number > 0) "+${formatNumber(number)}" else formatNumber(number)
internal fun Context.formatCurrency(number: Double) = getString(R.string.format_currency, number)
internal fun Context.formatBTC(sats: Long) = getString(R.string.amount_BTC, formatNumber(WalletViewModel.satoshiToBTC(sats).toDouble()))
