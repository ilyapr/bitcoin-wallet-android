package com.bitzhash.wallet.bitcoin.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import com.bitzhash.wallet.bitcoin.R
import com.bitzhash.wallet.bitcoin.getStoredAddressType
import com.bitzhash.wallet.bitcoin.getStoredWords
import com.google.android.material.button.MaterialButton

class BackupWalletActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_seed)

        setupBars()

        val wordsView = findViewById<TextView>(R.id.words)

        wordsView.text = getStoredWords()
        val addressType = findViewById<TextView>(R.id.addressType)
        addressType.text = SeedActivity.addressTypeText(this, getStoredAddressType())
        val done = findViewById<Button>(R.id.done)

        done.setOnClickListener { finish() }

        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val copyAddress = findViewById<MaterialButton>(R.id.copy)

        copyAddress.setOnClickListener {
            clipboard.setPrimaryClip(ClipData.newPlainText("seed", wordsView.text))
            showToast(getString(R.string.copied))
        }

        SeedActivity.setCopySeedUI(this, wordsView.text.toString())
    }
}
