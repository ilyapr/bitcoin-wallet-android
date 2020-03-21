package com.bitzhash.wallet.bitcoin.ui

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Gravity
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.lifecycle.viewModelScope
import com.bitzhash.wallet.bitcoin.R
import com.bitzhash.wallet.bitcoin.core.FeePriority
import com.bitzhash.wallet.bitcoin.core.WalletViewModel
import com.bitzhash.wallet.bitcoin.formatBTC
import com.bitzhash.wallet.bitcoin.formatCurrency
import com.bitzhash.wallet.bitcoin.formatNumber
import com.bitzhash.wallet.bitcoin.network.ApiService
import com.bitzhash.wallet.bitcoin.ui.MainActivity.Companion.REQUEST_CODE_SEND
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

open class EditableTextWatcher : TextWatcher {
    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
    override fun afterTextChanged(s: Editable) {}
}

fun AppCompatActivity.setupBars() {
    window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
    window.addFlags(android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
    window.statusBarColor = androidx.core.content.ContextCompat.getColor(this, R.color.statusBar)

    val toolbar = findViewById<Toolbar>(R.id.toolbar)
    setSupportActionBar(toolbar)
}

fun Context.setUpFeeChoice(owner: LifecycleOwner, viewModel: WalletViewModel, fee: TextView, feeCurrency: TextView, radioGroup: RadioGroup) {

    viewModel.feeLiveData.observe(owner, Observer {
        if (it != null) {
            fee.text = formatBTC(it)
            viewModel.viewModelScope.launch {
                try {
                    priceFormatted(feeCurrency, it, R.string.label_tx_fee_amount_currency)
                } catch (e: Exception) {
                    Log.e("setUpFeeChoice", e.toString())
                    feeCurrency.text = getString(R.string.currency_data_unavailable)
                }
            }
        } else {
            fee.text = getString(R.string.label_tx_fee_amount_btc_no_fee)
            feeCurrency.text = getString(R.string.label_tx_fee_amount_currency_no_fee)
        }
    })

    try {
        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            viewModel.feePriority = when (checkedId) {
                R.id.radioLow -> FeePriority.Low
                R.id.radioMedium -> FeePriority.Medium
                R.id.radioHigh -> FeePriority.High
                else -> throw Exception("Undefined priority")
            }
        }
    } catch (e: Exception) {
    }
}

fun Activity.setUp(dialog: Dialog, viewModel: WalletViewModel, amountBTC: Double?) {

    val title = dialog.findViewById<TextView>(R.id.title)
    val sendAddress = dialog.findViewById<TextView>(R.id.sendAddress)

    title.text = getString(R.string.dialog_send, formatNumber(amountBTC ?: WalletViewModel.satoshiToBTC(viewModel.amount!!).toDouble()))
    sendAddress.text = viewModel.address

    val send = dialog.findViewById<MaterialButton>(R.id.send)
    send.setOnClickListener {
        dialog.dismiss()
        startActivityForResult(Intent(this, RequestPinActivity::class.java), REQUEST_CODE_SEND)
    }
}

suspend fun Context.price(view: TextView, amount: Long) {
    val ticker = ApiService.bitstampApi().ticker()
    view.setText(formatCurrency(WalletViewModel.satsToAmountCurrency(amount, ticker.price)))
}

suspend fun Context.priceFormatted(view: TextView, amount: Long, stringRes: Int) {
    val ticker = ApiService.bitstampApi().ticker()
    view.setText(getString(stringRes, formatCurrency(WalletViewModel.satsToAmountCurrency(amount, ticker.price))))
}

fun Context.refreshCurrencyAmount(scope: CoroutineScope, view: TextView, amount: Long?) {
    if (amount != null) {
        scope.launch {
            try {
                price(view, amount)
            } catch (e: Exception) {
                Log.e("refreshCurrencyAmount", e.toString())
            }
        }
    }
}

fun Context.showToast(text: String) {
    Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
}

fun Context.showErrorToast(text: String) {
    val toast = Toast.makeText(this, text, Toast.LENGTH_LONG)
    toast.setGravity(Gravity.CENTER, 0, 0)
    toast.show()
}

fun Context.showSimpleDialog(text: String, button: String? = null, callback: () -> Unit) {

    val dialog = Dialog(this)
    dialog.setContentView(R.layout.dialog_simple)
    dialog.show()

    val title = dialog.findViewById<TextView>(R.id.title)
    val close = dialog.findViewById<MaterialButton>(R.id.close)

    title.text = text

    if (button != null) {
        close.text = button
    }

    close.setOnClickListener {
        dialog.dismiss()
        callback()
    }
}