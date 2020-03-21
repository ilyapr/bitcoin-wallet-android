package com.bitzhash.wallet.bitcoin.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import com.bitzhash.wallet.bitcoin.R
import com.bitzhash.wallet.bitcoin.core.WalletViewModel
import android.content.ClipData
import android.content.Context.CLIPBOARD_SERVICE
import android.content.ClipboardManager
import android.text.Editable
import android.util.Log
import android.widget.*
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.bitzhash.wallet.bitcoin.formatNumber
import com.bitzhash.wallet.bitcoin.network.ApiService
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch
import net.glxn.qrgen.android.QRCode

class ReceiveFragment : Fragment() {

    private lateinit var viewModel: WalletViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        viewModel = ViewModelProvider(activity!!).get(WalletViewModel::class.java)
        val root = inflater.inflate(R.layout.fragment_receive, container, false)
        val address = root.findViewById<TextView>(R.id.address)
        val code = root.findViewById<ImageView>(R.id.addressQR)

        fun generateQR(text: String) = code.setImageBitmap(QRCode.from(text).bitmap())

        viewModel.receiveAddressLiveData.observe(viewLifecycleOwner, Observer {
            address.text = it
            generateQR("bitcoin:${it}")
        })

        viewModel.generateReceiveAddress()

        val receiveAmount = root.findViewById<TextInputEditText>(R.id.amountBTC)
        val amountCurrency = root.findViewById<TextInputEditText>(R.id.amountCurrency)

        fun refreshQR() {
            val amount = try { receiveAmount.text.toString().toDouble() } catch (e: Exception) { 0.0 }

            if (amount > 0) {
                generateQR("bitcoin:${viewModel.receiveAddressLiveData.value}?amount=${amount}")
            } else {
                generateQR("bitcoin:${viewModel.receiveAddressLiveData.value}")
            }
        }

        receiveAmount.addTextChangedListener(
            object : EditableTextWatcher() {
                override fun afterTextChanged(s: Editable) {
                    if (receiveAmount.hasFocus()) {
                        try {
                            val amount = if (s.isBlank()) 0 else WalletViewModel.BTCtoSatoshi(s.toString().toDouble())
                            activity!!.refreshCurrencyAmount(viewModel.viewModelScope, amountCurrency, amount)
                            refreshQR()
                        } catch (e: NumberFormatException) {
                        }
                    }
                }
            })

        amountCurrency.addTextChangedListener(
            object : EditableTextWatcher() {
                override fun afterTextChanged(s: Editable) {
                    if (amountCurrency.hasFocus()) {
                        try {
                            suspend fun price() {
                                val ticker = ApiService.bitstampApi().ticker()

                                if (s.isNotBlank()) {
                                    receiveAmount.setText(
                                        formatNumber(
                                            WalletViewModel.amountCurrencyToBTC(
                                                s.toString().toDouble(), ticker.price)))
                                    refreshQR()
                                }
                            }

                            viewModel.viewModelScope.launch {
                                try {
                                    price()
                                } catch (e: Exception) {
                                    Log.e(ReceiveFragment::class.java.simpleName, e.toString())
                                }
                            }
                        } catch (e: NumberFormatException) {
                        }
                    }
                }
            })

        val currencyInputLayout = root.findViewById<TextInputLayout>(R.id.amountCurrencyInputLayout)
        currencyInputLayout.setEndIconOnClickListener {
            receiveAmount.setText("")
            amountCurrency.setText("")
            refreshQR()
        }

        val clipboard = activity?.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        val copyAddress = root.findViewById<MaterialButton>(R.id.copyAddress)

        copyAddress.setOnClickListener {
            clipboard.setPrimaryClip(ClipData.newPlainText("address", address.text))
            activity!!.showToast(getString(R.string.copied))
        }

        return root
    }
}