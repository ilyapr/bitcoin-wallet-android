package com.bitzhash.wallet.bitcoin.ui

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.drawable.AnimatedVectorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Browser
import android.text.method.LinkMovementMethod
import android.text.util.Linkify
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.bitzhash.wallet.bitcoin.R
import com.bitzhash.wallet.bitcoin.core.WalletViewModel
import com.bitzhash.wallet.bitcoin.formatBTC
import com.bitzhash.wallet.bitcoin.formatNumber
import com.bitzhash.wallet.bitcoin.formatSignedNumber
import io.horizontalsystems.bitcoincore.BitcoinCore
import io.horizontalsystems.bitcoincore.models.BalanceInfo
import io.horizontalsystems.bitcoincore.models.BlockInfo
import io.horizontalsystems.bitcoincore.models.TransactionInfo
import kotlinx.coroutines.launch
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern

class BalanceFragment : Fragment() {

    data class Item(var transactionInfo: TransactionInfo, var currentBlockHeight: Int)

    class TransactionsAdapter(private var items: List<Item>) : RecyclerView.Adapter<TransactionsAdapter.ViewHolder>() {

        class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

            companion object {
                private val format = DateTimeFormat.forPattern("d.MM.yyyy HH:mm:ss")
                private const val displayedConfsThreshold = 6
                private const val explorerMainNetURL = "https://live.blockcypher.com/btc"
                private const val explorerTestNetURL = "https://live.blockcypher.com/btc-testnet"
                private const val BE_TX_MAINNET_URL = "$explorerMainNetURL/tx/"
                private const val BE_TX_TESTNET_URL = "$explorerTestNetURL/tx/"
                private const val BE_ADDR_MAINNET_URL = "$explorerMainNetURL/address/"
                private const val BE_ADDR_TESTNET_URL = "$explorerTestNetURL/address/"

                fun txURL(tx: String) = (if (WalletViewModel.isMainnet()) BE_TX_MAINNET_URL
                    else BE_TX_TESTNET_URL) + tx

                fun toAddressURLPrefix() = if (WalletViewModel.isMainnet()) BE_ADDR_MAINNET_URL
                    else BE_ADDR_TESTNET_URL
            }

            private val layout = itemView.findViewById<ConstraintLayout>(R.id.layout)
            private val time = itemView.findViewById<TextView>(R.id.tx_time)
            private val block = itemView.findViewById<TextView>(R.id.tx_block)
            private val amount = itemView.findViewById<TextView>(R.id.tx_amount)
            private val image = itemView.findViewById<ImageView>(R.id.tx_image)
            private val status = itemView.findViewById<TextView>(R.id.tx_status)

            fun bind(item: Item) {

                fun amount() = if (item.transactionInfo.amount == 0L)
                        itemView.context.getString(R.string.tx_self_transfer)
                    else itemView.context.getString(
                        R.string.transaction_amount,
                        formatSignedNumber(WalletViewModel.satoshiToBTC(item.transactionInfo.amount).toDouble()))

                fun time() = DateTime(item.transactionInfo.timestamp * 1000).toString(format)

                fun openBrowser(uri: String) {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
                    intent.putExtra(Browser.EXTRA_APPLICATION_ID, itemView.context.packageName)
                    itemView.context.startActivity(intent)
                }

                fun confs() = item.currentBlockHeight + 1 - item.transactionInfo.blockHeight!!

                layout.setOnClickListener {

                    val dialogInfo = Dialog(itemView.context)
                    dialogInfo.setContentView(R.layout.dialog_transaction_info)

                    val amount = dialogInfo.findViewById<TextView>(R.id.amount)
                    val txFee = dialogInfo.findViewById<TextView>(R.id.txFee)
                    val addressFrom = dialogInfo.findViewById<TextView>(R.id.addressFrom)
                    val addressTo = dialogInfo.findViewById<TextView>(R.id.addressTo)
                    val time = dialogInfo.findViewById<TextView>(R.id.time)
                    val confs = dialogInfo.findViewById<TextView>(R.id.confs)
                    val height = dialogInfo.findViewById<TextView>(R.id.height)
                    val tx = dialogInfo.findViewById<TextView>(R.id.tx)

                    val NA = itemView.context.getString(R.string.label_tx_not_available)

                    amount.text = amount()
                    time.text = time()
                    txFee.text = if (item.transactionInfo.fee != null) itemView.context.formatBTC(item.transactionInfo.fee!!) else NA

                    addressFrom.text = item.transactionInfo.from.joinToString(separator = ";\n") { it.address }
                    addressTo.text = item.transactionInfo.to.joinToString(separator = ";\n") { it.address }

                    addressFrom.movementMethod = LinkMovementMethod.getInstance()
                    addressTo.movementMethod = LinkMovementMethod.getInstance()

                    val pattern = Pattern.compile("[a-zA-Z0-9]+")
                    Linkify.addLinks(addressFrom, pattern, toAddressURLPrefix())
                    Linkify.addLinks(addressTo, pattern, toAddressURLPrefix())

                    tx.text = item.transactionInfo.transactionHash
                    height.text = item.transactionInfo.blockHeight?.toString() ?: NA

                    tx.paintFlags = Paint.UNDERLINE_TEXT_FLAG
                    tx.setOnClickListener {
                        openBrowser(txURL(item.transactionInfo.transactionHash))
                    }

                    if (item.transactionInfo.blockHeight == null) {
                        confs.text = itemView.context.getString(R.string.label_tx_status_pending)
                    } else {
                        confs.text = confs().toString()
                    }

                    dialogInfo.show()
                }

                time.text = time()
                block.text = itemView.context.getString(R.string.tx_block,
                    item.transactionInfo.blockHeight?.toString() ?: itemView.context.getString(R.string.block_not_mempool))

                amount.text = amount()

                amount.setTextColor(ContextCompat.getColor(itemView.context,
                    when {
                        item.transactionInfo.amount > 0 -> R.color.transactionPlus
                        item.transactionInfo.amount < 0 -> R.color.transactionMinus
                        else -> R.color.transactionZero
                    }
                ))

                fun confsText(): String {
                    val confs = confs()
                    return if (confs <= displayedConfsThreshold) itemView.context.getString(R.string.tx_status_confs, confs)
                    else itemView.context.getString(R.string.tx_status_confs_threshold)
                }

                when {

                    item.transactionInfo.blockHeight == null -> {
                        image.setBackgroundResource(R.drawable.baseline_arrow_right_alt_24)
                        status.text = itemView.context.getString(R.string.tx_status_pending)
                        //status.setTextColor(ContextCompat.getColor(itemView.context, R.color.transactionStatusPending))
                    }

                    confs() >= WalletViewModel.CONFIRMATIONS -> {
                        image.setBackgroundResource(R.drawable.baseline_check_circle_outline_24)
                        status.text = confsText()
                        //status.setTextColor(ContextCompat.getColor(itemView.context, R.color.transactionStatusConfirmed))
                    }

                    else -> {
                        image.setBackgroundResource(R.drawable.baseline_schedule_24)

                        // this if is just in case, if we change WalletViewModel.CONFIRMATIONS > 1
                        if (confs() == 0) {
                            status.text = itemView.context.getString(R.string.tx_status_unconfirmed)
                        } else {
                            status.text = confsText()
                        }

                        //status.setTextColor(ContextCompat.getColor(itemView.context, R.color.transactionStatusUnconfirmed))
                    }
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val layoutInflater = LayoutInflater.from(parent.context)
            return ViewHolder(layoutInflater.inflate(R.layout.item_transaction, parent, false))
        }

        override fun getItemCount(): Int = items.size

        override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(items[holder.adapterPosition])

        fun update(items: MutableList<Item>) {
            this.items = items
            notifyDataSetChanged()
        }

        fun updateStatus(blockInfo: BlockInfo) = items.forEach {
            it.currentBlockHeight = blockInfo.height
            notifyDataSetChanged()
        }
    }

    companion object {
        const val CHANNEL_ID = "bitcoin_wallet"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val viewModel = ViewModelProvider(activity!!).get(WalletViewModel::class.java)
        val root = inflater.inflate(R.layout.fragment_balance, container, false)

        val balanceView = root.findViewById<TextView>(R.id.balance)
        val refresh = root.findViewById<AppCompatImageView>(R.id.refresh)
        val balanceCurrency = root.findViewById<TextView>(R.id.balanceCurrency)
        val recyclerView = root.findViewById<RecyclerView>(R.id.transactions)

        fun getTransactions(): MutableList<Item> = viewModel.transactions.value!!
            .map { Item(it, viewModel.lastBlock.value!!.height) }
            .toMutableList()

        val adapter = TransactionsAdapter(getTransactions())

        recyclerView.layoutManager = LinearLayoutManager(activity)
        recyclerView.adapter = adapter

        val networkInfo = root.findViewById<TextView>(R.id.networkInfo)

        viewModel.lastBlock.observe(viewLifecycleOwner, Observer { block ->
            networkInfo.text = getString(
                R.string.network_info, viewModel.networkName,
                block?.height?.toString() ?: getString(R.string.block_not_available)
            )
        })

        val statusView = root.findViewById<TextView>(R.id.status)

        viewModel.state.observe(viewLifecycleOwner, Observer { state ->

            when (state) {

                is BitcoinCore.KitState.Synced -> {
                    statusView.text = getString(R.string.status_synced)
                    statusView.setTextColor(ContextCompat.getColor(context!!, R.color.statusSynced))
                }

                is BitcoinCore.KitState.Syncing -> {
                    statusView.text = getString(R.string.status_syncing, (state.progress * 100).toInt().toString())
                    statusView.setTextColor(ContextCompat.getColor(context!!, R.color.statusSyncing))
                }

                is BitcoinCore.KitState.NotSynced -> {
                    statusView.text = getString(R.string.status_not_synced)
                    statusView.setTextColor(ContextCompat.getColor(context!!, R.color.statusNotSynced))
                }
            }
        })

        fun amount(info: BalanceInfo) = info.spendable + info.unspendable
        fun refreshCurrencyAmount(amount: Long) {
            viewModel.viewModelScope.launch {
                try {
                    activity!!.priceFormatted(
                        balanceCurrency,
                        amount,
                        R.string.home_balance_currency
                    )
                } catch (e: Exception) {
                    Log.e(BalanceFragment::class.java.simpleName, e.toString())
                    balanceCurrency.text = getString(R.string.currency_data_unavailable)
                }
            }
        }

        refresh.setOnClickListener {

            refresh.clearAnimation()

            val drawable = ContextCompat.getDrawable(activity!!, R.drawable.refresh_amimation) as AnimatedVectorDrawable
            refresh.setImageDrawable(drawable)
            drawable.start()

            if (viewModel.balance.value != null) {
                refreshCurrencyAmount(amount(viewModel.balance.value!!))
            } else {
                balanceCurrency.text = getString(R.string.currency_data_unavailable)
            }
        }

        viewModel.balance.observe(viewLifecycleOwner, Observer {
            val amount = amount(it)
            val balance = WalletViewModel.satoshiToBTC(amount)
            balanceView.text = getString(R.string.home_balance, formatNumber(balance))
            refreshCurrencyAmount(amount)
        })

        fun id() = SimpleDateFormat("ddHHmmss", Locale.US).format(Date()).toInt()
        fun sendNotification(title: String, content: String) {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

                val channel = NotificationChannel(CHANNEL_ID, getString(R.string.notification_channel_name), NotificationManager.IMPORTANCE_DEFAULT)
                    .apply {
                        description = getString(R.string.notification_channel_description)
                        enableVibration(true)
                        enableLights(true)
                        setLightColor(R.color.colorAccent)
                }

                val notificationManager: NotificationManager = activity!!.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.createNotificationChannel(channel)
            }

            val notificationIntent = Intent(context!!, MainActivity::class.java)
            notificationIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            val pendingIntent = PendingIntent.getActivity(context!!, 0, notificationIntent, 0)

            val notification = NotificationCompat.Builder(context!!, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setContentTitle(title)
                .setContentText(content)
                .setWhen(Timestamp(System.currentTimeMillis()).time)
                .setDefaults(Notification.DEFAULT_LIGHTS or Notification.DEFAULT_VIBRATE)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setLights(ContextCompat.getColor(context!!, R.color.colorAccent), 200, 200)
                .setContentIntent(pendingIntent)
                .build()

            notification.flags = Notification.FLAG_AUTO_CANCEL

            with (NotificationManagerCompat.from(context!!)) {
                notify(id(), notification)
            }
        }

        viewModel.incomingTransactions.observe(viewLifecycleOwner, Observer {

            if (it.size == 1) {

                val amount = it[0].amount

                // notify only about incoming transactions
                if (amount > 0L) {
                    sendNotification(
                        getString(R.string.notification_tx_received_title),
                        getString(R.string.notification_tx_received_content, formatSignedNumber(WalletViewModel.satoshiToBTC(amount))))
                }
            } /*
            TODO
            else {
                val amount = it.filter { tx -> tx.amount >= 0 }.map { tx -> tx.amount }.sum()

                // notify only about incoming transactions
                if (amount > 0L) {

                }
            }*/
        })
        viewModel.transactions.observe(viewLifecycleOwner, Observer { adapter.update(getTransactions()) })
        viewModel.lastBlock.observe(viewLifecycleOwner, Observer {
            if (it != null) {
                adapter.updateStatus(it)
            }
        })

        val button = root.findViewById<FloatingActionButton>(R.id.scan)

        button.setOnClickListener {
            activity!!.startActivityForResult(Intent(context, ScannerActivity::class.java),
                MainActivity.REQUEST_CODE_SCAN
            )
        }

        return root
    }
}