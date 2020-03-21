package com.bitzhash.wallet.bitcoin.ui

import android.app.Dialog
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.android.material.textfield.TextInputLayout
import com.bitzhash.wallet.bitcoin.*
import com.bitzhash.wallet.bitcoin.core.WalletViewModel
import com.bitzhash.wallet.bitcoin.formatNumber
import com.bitzhash.wallet.bitcoin.network.ApiService
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class SendFragment : Fragment() {

    private lateinit var viewModel: WalletViewModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        arguments?.let {
            val args = SendFragmentArgs.fromBundle(it)

            if (args.address != "0") {
                viewModel.addressLiveData.postValue(args.address)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        viewModel = ViewModelProvider(activity!!).get(WalletViewModel::class.java)
        val root = inflater.inflate(R.layout.fragment_send, container, false)

        val clipboard = activity?.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val sendAddress = root.findViewById<TextInputEditText>(R.id.sendAddress)
        val sendAddressInputLayout = root.findViewById<TextInputLayout>(R.id.sendAddressInputLayout)
        val sendAmount = root.findViewById<TextInputEditText>(R.id.amountBTC)
        val sendAmountInputLayout = root.findViewById<TextInputLayout>(R.id.amountBTCInputLayout)
        val currencyInputLayout = root.findViewById<TextInputLayout>(R.id.amountCurrencyInputLayout)
        val amountCurrency = root.findViewById<TextInputEditText>(R.id.amountCurrency)

        sendAddressInputLayout.isErrorEnabled = true
        sendAmountInputLayout.isErrorEnabled = true

        root.findViewById<MaterialButton>(R.id.paste).setOnClickListener {
            val paste = clipboard.primaryClip?.getItemAt(0)?.text.toString()
            sendAddress.setText(paste)
            viewModel.address = paste
            viewModel.addressLiveData.value = viewModel.address
        }

        viewModel.addressLiveData.observe(viewLifecycleOwner, Observer {
            sendAddress.setText(it ?: "")
        })

        viewModel.amountLiveData.observe(viewLifecycleOwner, Observer {
            sendAmount.setText(
                if (viewModel.amountLiveData.value != null)
                    formatNumber(WalletViewModel.satoshiToBTC(viewModel.amountLiveData.value!!).toDouble())
                else "")

            viewModel.amount = viewModel.amountLiveData.value
        })

        // reset after sending
        viewModel.amountCurrencyLiveData.observe(viewLifecycleOwner, Observer {
            if (viewModel.amountCurrencyLiveData.value == null || viewModel.amountCurrencyLiveData.value!! == 0.0) {
                amountCurrency.setText("")
            }
        })

        viewModel.errorAddressLiveData.observe(viewLifecycleOwner, Observer {
            if (sendAddressInputLayout.error != it) {
                sendAddressInputLayout.error = it
            }
        })

        viewModel.errorAmountLiveData.observe(viewLifecycleOwner, Observer {
            if (sendAmountInputLayout.error != it) {
                sendAmountInputLayout.error = it
            }
        })

        viewModel.errorLiveData.observe(viewLifecycleOwner, Observer {
            if (it != null) {
                activity!!.showErrorToast(it)
                viewModel.errorLiveData.value = null
            }
        })

        root.findViewById<Button>(R.id.max).setOnClickListener {
            viewModel.setMaxAmount()
            activity!!.refreshCurrencyAmount(viewModel.viewModelScope, amountCurrency, viewModel.amount)
        }

        currencyInputLayout.setEndIconOnClickListener {
            sendAmount.setText("")
            amountCurrency.setText("")
        }

        sendAddress.addTextChangedListener(
            object : EditableTextWatcher() {
                override fun afterTextChanged(s: Editable) {
                    viewModel.address = sendAddress.text.toString()
                }
            })

        sendAmount.addTextChangedListener(
            object : EditableTextWatcher() {
                override fun afterTextChanged(s: Editable) {
                    if (sendAmount.hasFocus()) {
                        try {
                            viewModel.amount = if (s.isBlank()) 0 else WalletViewModel.BTCtoSatoshi(s.toString().toDouble())
                            activity!!.refreshCurrencyAmount(viewModel.viewModelScope, amountCurrency, viewModel.amount!!)
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
                                    viewModel.amount = WalletViewModel.amountCurrencyToSatoshi(s.toString().toDouble(), ticker.price)
                                    viewModel.amountLiveData.value = viewModel.amount
                                }
                            }

                            viewModel.viewModelScope.launch {

                                try {
                                    price()
                                } catch (e: Exception) {
                                    Log.e(SendFragment::class.java.simpleName, e.toString())
                                }
                            }
                        } catch (e: NumberFormatException) {
                        }
                    }
                }
            })

        context!!.setUpFeeChoice(
            viewLifecycleOwner,
            viewModel,
            root.findViewById(R.id.fee),
            root.findViewById(R.id.feeCurrency),
            root.findViewById(R.id.radioGroup))

        root.findViewById<MaterialButton>(R.id.send).setOnClickListener {

            if (viewModel.address == null || viewModel.amount == null) {
                activity!!.showErrorToast(getString(R.string.error_fill_all))
            } else {

                try {
                    viewModel.validateAndGetFee()
                    showSendDialog(viewModel)
                } catch (e: Exception) {
                }
            }
        }

        return root
    }

    private fun showSendDialog(viewModel: WalletViewModel) {

        val dialog = Dialog(this@SendFragment.context!!)
        dialog.setContentView(R.layout.dialog_send)

        activity!!.setUp(dialog, viewModel, null)

        dialog.show()
    }
}