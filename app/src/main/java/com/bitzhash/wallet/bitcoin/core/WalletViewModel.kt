package com.bitzhash.wallet.bitcoin.core

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.bitzhash.wallet.bitcoin.*
import io.horizontalsystems.bitcoincore.BitcoinCore
import io.horizontalsystems.bitcoincore.exceptions.AddressFormatException
import io.horizontalsystems.bitcoincore.managers.SendValueErrors
import io.horizontalsystems.bitcoincore.models.BalanceInfo
import io.horizontalsystems.bitcoincore.models.BlockInfo
import io.horizontalsystems.bitcoincore.models.TransactionInfo
import io.horizontalsystems.bitcoinkit.BitcoinKit
import io.reactivex.disposables.CompositeDisposable
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.roundToLong

class WalletViewModel : ViewModel(), BitcoinKit.Listener {

    private lateinit var bitcoinKit: BitcoinKit

    companion object {
        const val BTC = 100_000_000
        const val WALLET = "MyWallet"
        const val CONFIRMATIONS = 1

        val networkType: BitcoinKit.NetworkType
            get() = if (BuildConfig.MAIN_NET == true) BitcoinKit.NetworkType.MainNet else BitcoinKit.NetworkType.TestNet

        fun isMainnet() = networkType == BitcoinKit.NetworkType.MainNet
        fun satoshiToBTC(satoshi: Long): BigDecimal = BigDecimal.ONE.divide(BigDecimal(BTC)).multiply(BigDecimal(satoshi))
        fun BTCtoSatoshi(amountBtc: Double): Long = (amountBtc * BTC).roundToLong()
        fun amountCurrencyToBTC(amount: Double, price: Double) =
            amount.toBigDecimal().divide(
                price.toBigDecimal(),
            5,
                RoundingMode.HALF_UP
            ).toDouble()
        fun amountCurrencyToSatoshi(amount: Double, price: Double) = BTCtoSatoshi(amountCurrencyToBTC(amount, price))
        fun satsToAmountCurrency(satoshi: Long, price: Double) = satoshiToBTC(satoshi).multiply(price.toBigDecimal()).toDouble()
    }

    val networkName: String
    val balance = MutableLiveData<BalanceInfo>()
    val state = MutableLiveData<BitcoinCore.KitState>()
    val lastBlock = MutableLiveData<BlockInfo>()
    val transactions = MutableLiveData<List<TransactionInfo>>()
    val incomingTransactions = MutableLiveData<List<TransactionInfo>>()

    val receiveAddressLiveData = MutableLiveData<String>()
    val feeLiveData = MutableLiveData<Long>()

    val addressLiveData = MutableLiveData<String>()
    val amountLiveData = MutableLiveData<Long>()
    val amountCurrencyLiveData = MutableLiveData<Double>()

    val errorLiveData = MutableLiveData<String>()
    val errorAddressLiveData = MutableLiveData<String>()
    val errorAmountLiveData = MutableLiveData<String>()

    var address: String? = null
        set(value) {
            field = value
            updateFee()
        }

    var amount: Long? = null
        set(value) {
            field = value
            updateFee()
        }

    var feePriority: FeePriority = FeePriority.Medium
        set(value) {
            field = value
            updateFee()
        }

    private val disposables = CompositeDisposable()

    fun generateReceiveAddress() {
        receiveAddressLiveData.value = bitcoinKit.receiveAddress()
    }

    fun parsePaymentAddress(uri: String) = bitcoinKit.parsePaymentAddress(uri)

    fun send(context: Context) {

        when {

            address.isNullOrBlank() -> {
                errorLiveData.value = context.getString(R.string.error_no_address)
                throw Exception("Send address cannot be blank")
            }

            amount == null -> {
                errorLiveData.value = context.getString(R.string.error_no_amount)
                throw Exception("Send amount cannot be blank")
            }

            else -> try {

                bitcoinKit.send(
                    address!!,
                    amount!!,
                    feeRate = feePriority.feeRate,
                    pluginData = emptyMap()
                )

                address = null
                amount = null

                addressLiveData.value = null
                amountLiveData.value = null
                amountCurrencyLiveData.value = null
                feeLiveData.value = null

                errorLiveData.value = null
                errorAddressLiveData.value = null
                errorAmountLiveData.value = null

            } catch (e: Exception) {

                processException(e)
                throw e
            }
        }
    }

    fun setMaxAmount() {
        try {
            amountLiveData.value = bitcoinKit.maximumSpendableValue(address, feePriority.feeRate, emptyMap())
        } catch (e: Exception) {
            if (address != null) {
                processException(e)
            }
        }
    }

    fun validateAndGetFee() = validateAndGetFee(address, amount)

    fun validateAndGetFee(address: String?, amount: Long? = null) =
        bitcoinKit.fee(amount ?: 0, address, feeRate = feePriority.feeRate, pluginData = emptyMap())

    private fun updateFee() {

        try {
            if (!address.isNullOrBlank() || (amount != null && amount != 0L)) {

                errorAddressLiveData.value = null
                errorAmountLiveData.value = null
                errorLiveData.value = null

                feeLiveData.value = validateAndGetFee()
            }
        } catch (e: Exception) {
            processException(e)
        }
    }

    private fun processException(e: Exception) {

        val context = BitcoinWalletApplication.instance.applicationContext

        when (e) {
            is SendValueErrors.InsufficientUnspentOutputs,
            is SendValueErrors.EmptyOutputs,
            is SendValueErrors.NoSingleOutput -> errorAmountLiveData.value = context.getString(R.string.error_send_empty_output)

            is SendValueErrors.Dust -> errorAmountLiveData.value = amountError(context)
            is AddressFormatException -> errorAddressLiveData.value = context.getString(R.string.error_invalid_address)
            else -> errorLiveData.value = e.message ?: e.javaClass.simpleName
        }
    }

    fun message(e: Exception, context: Context) = when (e) {
        is SendValueErrors.InsufficientUnspentOutputs,
        is SendValueErrors.EmptyOutputs,
        is SendValueErrors.NoSingleOutput -> context.getString(R.string.error_send_empty_output)

        is SendValueErrors.Dust -> amountError(context)
        is AddressFormatException -> context.getString(R.string.error_invalid_address)
        else -> e.message ?: e.javaClass.simpleName
    }

    private fun amountError(context: Context) = context.getString(
        if (amount == null || amount == 0L) R.string.error_no_amount else R.string.error_dust)

    init {
        init()
        networkName = bitcoinKit.networkName
    }

    private fun init() {

        val context = BitcoinWalletApplication.instance.applicationContext

        bitcoinKit = BitcoinKit(
            context,
            (context.getStoredWords() as String).split(" "),
            WALLET,
            networkType,
            bip = context.getStoredAddressType().bip,
            confirmationsThreshold = CONFIRMATIONS
        )

        bitcoinKit.listener = this
        balance.value = bitcoinKit.balance

        bitcoinKit.transactions().subscribe { txList: List<TransactionInfo> ->
            transactions.value = txList
        }.let {
            disposables.add(it)
        }

        lastBlock.value = bitcoinKit.lastBlockInfo
        state.value = BitcoinCore.KitState.NotSynced

        bitcoinKit.start()
    }

    fun restart() {
        bitcoinKit.stop()
        init()
    }

    override fun onTransactionsUpdate(inserted: List<TransactionInfo>, updated: List<TransactionInfo>) {

        incomingTransactions.postValue(inserted)

        bitcoinKit.transactions().subscribe { txList: List<TransactionInfo> ->
            transactions.postValue(txList)
        }.let {
            disposables.add(it)
        }
    }

    override fun onTransactionsDelete(hashes: List<String>) {}
    override fun onBalanceUpdate(balance: BalanceInfo) = this.balance.postValue(balance)
    override fun onLastBlockInfoUpdate(blockInfo: BlockInfo) = this.lastBlock.postValue(blockInfo)
    override fun onKitStateUpdate(state: BitcoinCore.KitState) = this.state.postValue(state)
}