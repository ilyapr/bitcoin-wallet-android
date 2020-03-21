package com.bitzhash.wallet.bitcoin.ui

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.bitzhash.wallet.bitcoin.R
import com.bitzhash.wallet.bitcoin.getStoredAddressType
import com.bitzhash.wallet.bitcoin.getStoredPin
import com.bitzhash.wallet.bitcoin.getStoredWords
import kotlinx.coroutines.*
import java.lang.Exception

class SplashActivity : AppCompatActivity() {

    companion object {
        private const val DELAY: Long = 1500
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        setupBars()

        val scope = CoroutineScope(Dispatchers.Main)
        scope.launch {

            delay(DELAY)

            val type = try { getStoredAddressType() } catch (e: Exception) { null }
            val words = getStoredWords()
            val pin = getStoredPin()

            if (type != null && words != null && words.isNotEmpty() && pin != null && pin.isNotEmpty()) {
                startActivity(Intent(this@SplashActivity, MainActivity::class.java))
            } else {
                startActivity(Intent(this@SplashActivity, ChoiceCreateRestoreActivity::class.java))
            }

            finish()
        }
    }
}
