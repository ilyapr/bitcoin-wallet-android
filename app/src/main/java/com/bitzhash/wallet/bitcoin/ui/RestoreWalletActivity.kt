package com.bitzhash.wallet.bitcoin.ui

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.bitzhash.wallet.bitcoin.R
import com.bitzhash.wallet.bitcoin.core.AddressType
import com.bitzhash.wallet.bitcoin.setStoredAddressType
import com.bitzhash.wallet.bitcoin.setStoredWords
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import io.horizontalsystems.hdwalletkit.Mnemonic

class RestoreWalletActivity : AppCompatActivity() {

    companion object {
        const val RESTORE_REWRITE = "RESTORE_REWRITE"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_restore_wallet)

        setupBars()

        val progress = findViewById<ProgressBar>(R.id.progress)
        val radioGroup = findViewById<RadioGroup>(R.id.radioGroup)
        var addressType: AddressType = AddressType.P2SHSegwit

        progress.visibility = View.GONE

        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            addressType = when (checkedId) {
                R.id.radioLegacy -> AddressType.Legacy
                R.id.radioSegwitCompatible -> AddressType.P2SHSegwit
                R.id.radioSegwit -> AddressType.NativeSegwit
                else -> throw Exception("Undefined priority")
            }
        }

        val words = findViewById<TextInputEditText>(R.id.words)
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

        findViewById<MaterialButton>(R.id.paste).setOnClickListener {
            val paste = clipboard.primaryClip?.getItemAt(0)?.text.toString()
            words.setText(paste)
        }

        val done = findViewById<MaterialButton>(R.id.done)

        done.setOnClickListener {

            if (words.text.isNullOrBlank()) {
                showErrorToast(getString(R.string.recover_wallet_empty_seed))
            } else {

                val text = words.text?.trim()

                fun save() {
                    progress.visibility = View.VISIBLE
                    setStoredAddressType(addressType)
                    setStoredWords(text.toString())
                    setResult(RESULT_OK, Intent())
                    finish()
                }

                try {
                    Mnemonic().validate(text!!.split(" "))

                    if (intent.getBooleanExtra("RESTORE_REWRITE", false)) {

                        showSimpleDialog(getString(R.string.recover_warning), getString(R.string.recover_warning_ok)) {
                            save()
                        }

                    } else {
                        save()
                    }

                } catch (e: Exception) {
                    showErrorToast(getString(R.string.recover_wallet_invalid_seed))
                }
            }
        }
    }
}
