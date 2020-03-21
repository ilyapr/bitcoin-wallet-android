package com.bitzhash.wallet.bitcoin.ui

import android.app.Activity
import android.app.Dialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.bitzhash.wallet.bitcoin.R
import com.bitzhash.wallet.bitcoin.core.WalletViewModel
import io.horizontalsystems.bitcoincore.exceptions.AddressFormatException
import net.danlew.android.joda.JodaTimeAndroid
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper

class MainActivity : AppCompatActivity() {

    companion object {
        const val REQUEST_CODE_SCAN = 1
        const val REQUEST_CODE_SEND = 2
        const val REQUEST_CODE_CHANGE_PIN_AUTHORIZE = 3
        const val REQUEST_CODE_CHANGE_PIN = 4
        const val REQUEST_CODE_BACKUP_WALLET = 5
        const val REQUEST_CODE_RESTORE_WALLET = 6
        const val REQUEST_CODE_RESTORE_WALLET_COMPLETE = 7
    }

    private lateinit var navController: NavController

    override fun attachBaseContext(newBase: Context) = super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase))

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?)  {

        super.onActivityResult(requestCode, resultCode, data)

        fun showPinCanceled() = showToast(getString(R.string.pin_title_abort))

        when (requestCode) {

            REQUEST_CODE_SCAN ->
                if (resultCode == Activity.RESULT_OK)
                    processPayment(data!!.getStringExtra(ScannerActivity.EXTRA_RESULT))

            REQUEST_CODE_SEND ->
                if (resultCode == Activity.RESULT_OK) {
                    try {
                        val viewModel = ViewModelProvider(this).get(WalletViewModel::class.java)
                        viewModel.send(this)

                        showSimpleDialog(getString(R.string.dialog_transaction_sent)) {}

                    } catch (ignored: Exception) {
                        showErrorToast(getString(R.string.dialog_transaction_failed))
                    }
                }

            REQUEST_CODE_CHANGE_PIN_AUTHORIZE ->
                if (resultCode == Activity.RESULT_OK)
                    startActivityForResult(Intent(this, PinCodeActivity::class.java), REQUEST_CODE_CHANGE_PIN)

            REQUEST_CODE_CHANGE_PIN -> when (resultCode) {
                Activity.RESULT_OK -> showSimpleDialog(getString(R.string.dialog_pin_code_changed)) {}
                Activity.RESULT_CANCELED -> showPinCanceled()
            }

            REQUEST_CODE_BACKUP_WALLET -> when (resultCode) {
                Activity.RESULT_OK -> startActivity(Intent(this, BackupWalletActivity::class.java))
                Activity.RESULT_CANCELED -> showPinCanceled()
            }

            REQUEST_CODE_RESTORE_WALLET -> when (resultCode) {

                Activity.RESULT_OK -> {
                    val intent = Intent(this, RestoreWalletActivity::class.java)
                    intent.putExtra(RestoreWalletActivity.RESTORE_REWRITE, true)
                    startActivityForResult(intent, REQUEST_CODE_RESTORE_WALLET_COMPLETE)
                }
                Activity.RESULT_CANCELED -> showPinCanceled()
            }

            REQUEST_CODE_RESTORE_WALLET_COMPLETE -> if (resultCode == Activity.RESULT_OK) {
                ViewModelProvider(this).get(WalletViewModel::class.java).restart()
            }
        }
    }

    private fun processPayment(uri: String?) {

        if (uri.isNullOrBlank()) {
            showErrorToast(getString(R.string.qr_error))
            return
        }

        val viewModel = ViewModelProvider(this).get(WalletViewModel::class.java)
        val paymentData = viewModel.parsePaymentAddress(uri)

        if (paymentData.amount == null) {

            try {
                viewModel.validateAndGetFee(paymentData.address)
            } catch (e: AddressFormatException) {
                showErrorToast(getString(R.string.address_error))
                return
            } catch (e: Exception) {
                // no amount
            }

            val action = BalanceFragmentDirections.sendFromScanner(paymentData.address)
            navController.navigate(action)

        } else {

            val amount = paymentData.amount!!
            val sats = WalletViewModel.BTCtoSatoshi(amount)

            try {
                viewModel.validateAndGetFee(paymentData.address, sats)
            } catch (e: Exception) {
                showErrorToast(viewModel.message(e, this))
                return
            }

            viewModel.addressLiveData.postValue(paymentData.address)
            viewModel.amountLiveData.postValue(sats)

            viewModel.address = paymentData.address
            viewModel.amount = sats

            showScannerSendDialog(viewModel, amount)
        }
    }

    private fun showScannerSendDialog(viewModel: WalletViewModel, amountBTC: Double?) {

        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_scanner_send)

        setUp(dialog, viewModel, amountBTC)
        setUpFeeChoice(
            this as LifecycleOwner,
            viewModel,
            dialog.findViewById(R.id.fee),
            dialog.findViewById(R.id.feeCurrency),
            dialog.findViewById(R.id.radioGroup))

        if (!viewModel.errorLiveData.hasActiveObservers()) {

            viewModel.errorLiveData.observe(this as LifecycleOwner, Observer {
                if (it != null) {
                    showErrorToast(it)
                    viewModel.errorLiveData.value = null
                }
            })
        }

        dialog.show()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.toolbar_main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {

        R.id.menu_main_about -> {
            val dialog = Dialog(this)
            dialog.setContentView(R.layout.dialog_about)
            val address = dialog.findViewById<TextView>(R.id.address)
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

            address.setOnClickListener {
                clipboard.setPrimaryClip(ClipData.newPlainText("address", address.text))
                showToast(getString(R.string.copied))
            }
            dialog.show()
            true
        }

        R.id.menu_change_pin -> {
            startActivityForResult(Intent(this, RequestPinActivity::class.java), REQUEST_CODE_CHANGE_PIN_AUTHORIZE)
            true
        }

        R.id.menu_backup_wallet -> {
            startActivityForResult(Intent(this, RequestPinActivity::class.java), REQUEST_CODE_BACKUP_WALLET)
            true
        }

        R.id.menu_restore_wallet -> {
            startActivityForResult(Intent(this, RequestPinActivity::class.java), REQUEST_CODE_RESTORE_WALLET)
            true
        }

        else -> super.onOptionsItemSelected(item)
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setupBars()

        val navView = findViewById<BottomNavigationView>(R.id.nav_view)

        navController = findNavController(R.id.nav_host_fragment)
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_home,
                R.id.navigation_receive,
                R.id.navigation_send
            )
        )

        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        JodaTimeAndroid.init(this)
    }
}
