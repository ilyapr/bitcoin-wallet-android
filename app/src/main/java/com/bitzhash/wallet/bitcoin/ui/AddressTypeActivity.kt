package com.bitzhash.wallet.bitcoin.ui

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.RadioGroup
import com.bitzhash.wallet.bitcoin.R
import com.bitzhash.wallet.bitcoin.core.AddressType
import com.bitzhash.wallet.bitcoin.setStoredAddressType
import com.google.android.material.button.MaterialButton
import java.lang.Exception

class AddressTypeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_address_type)

        setupBars()

        val radioGroup = findViewById<RadioGroup>(R.id.radioGroup)
        var addressType: AddressType = AddressType.P2SHSegwit

        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            addressType = when (checkedId) {
                R.id.radioLegacy -> AddressType.Legacy
                R.id.radioSegwitCompatible -> AddressType.P2SHSegwit
                R.id.radioSegwit -> AddressType.NativeSegwit
                else -> throw Exception("Undefined priority")
            }
        }

        val done = findViewById<MaterialButton>(R.id.done)
        done.setOnClickListener {
            setStoredAddressType(addressType)
            startActivity(Intent(this, SeedActivity::class.java))
            finish()
        }
    }
}
