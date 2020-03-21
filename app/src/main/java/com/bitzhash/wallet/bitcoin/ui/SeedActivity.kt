package com.bitzhash.wallet.bitcoin.ui

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import com.bitzhash.wallet.bitcoin.R
import com.bitzhash.wallet.bitcoin.core.AddressType
import com.bitzhash.wallet.bitcoin.getStoredAddressType
import com.bitzhash.wallet.bitcoin.setStoredWords
import com.google.android.material.button.MaterialButton
import io.horizontalsystems.hdwalletkit.Mnemonic
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper

class SeedActivity : AppCompatActivity() {

    companion object {

        fun setCopySeedUI(activity: Activity, words: String) {
            val clipboard = activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val copyAddress = activity.findViewById<MaterialButton>(R.id.copy)

            copyAddress.setOnClickListener {
                clipboard.setPrimaryClip(ClipData.newPlainText("seed", words))
                activity.showToast(activity.getString(R.string.copied))
            }
        }

        fun addressTypeText(context: Context, type: AddressType) = when (type) {
            is AddressType.Legacy -> context.getString(R.string.address_type_legacy)
            is AddressType.P2SHSegwit -> context.getString(R.string.address_type_segwit_compatible)
            is AddressType.NativeSegwit -> context.getString(R.string.address_native_segwit)
        }
    }

    override fun attachBaseContext(newBase: Context) = super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_seed)

        setupBars()

        val words = Mnemonic().generate(Mnemonic.Strength.High).joinToString(separator = " ")
        val wordsView = findViewById<TextView>(R.id.words)
        val addressType = findViewById<TextView>(R.id.addressType)
        addressType.text = addressTypeText(this, getStoredAddressType())
        wordsView.text = words

        val done = findViewById<MaterialButton>(R.id.done)

        done.setOnClickListener {

            setStoredWords(words)

            val data = Intent(this@SeedActivity, PinCodeActivity::class.java)
            data.putExtra(PinCodeActivity.EXTRA_CREATE_PIN, true)
            startActivity(data)
            finish()
        }

        setCopySeedUI(this, words)
    }
}
