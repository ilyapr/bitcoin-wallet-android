package com.bitzhash.wallet.bitcoin.ui

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.bitzhash.wallet.bitcoin.R
import com.bitzhash.wallet.bitcoin.ui.RestoreWalletActivity.Companion.RESTORE_REWRITE
import com.google.android.material.button.MaterialButton

class ChoiceCreateRestoreActivity : AppCompatActivity() {

    companion object {
        const val REQUEST_CODE_RESTORE_WALLET = 1
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE_RESTORE_WALLET && resultCode == Activity.RESULT_OK) {
            startActivity(PinCodeActivity.intentCreate(this))
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_choice_create_restore)

        setupBars()

        findViewById<MaterialButton>(R.id.create).setOnClickListener {
            startActivity(Intent(this, AddressTypeActivity::class.java))
            finish()
        }

        findViewById<MaterialButton>(R.id.restore).setOnClickListener {
            val intent = Intent(this, RestoreWalletActivity::class.java)
            intent.putExtra(RESTORE_REWRITE, false)
            startActivityForResult(intent, REQUEST_CODE_RESTORE_WALLET)
        }
    }
}
