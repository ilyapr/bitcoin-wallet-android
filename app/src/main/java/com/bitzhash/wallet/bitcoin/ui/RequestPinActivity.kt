package com.bitzhash.wallet.bitcoin.ui

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.*
import com.bitzhash.wallet.bitcoin.R
import com.bitzhash.wallet.bitcoin.getStoredPin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class RequestPinActivity : AppCompatActivity(), PinCodeFragment.OnPinEnteredListener {

    companion object {
        private const val TRIES = 3
        private const val DELAY: Long = 2000 //2 seconds
    }

    class RequestPinModel : ViewModel() {
        val title = MutableLiveData<String>()
        val pinTry = MutableLiveData<Int>()

        init {
            clear()
        }

        fun clear() {
            pinTry.value = 1
            title.value = null
        }

        fun inc() {
            pinTry.value = pinTry.value?.plus(1)
        }
    }

    private lateinit var model: RequestPinModel
    private lateinit var keyboard: PinCodeFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pin)

        setupBars()

        model = ViewModelProvider(this)[RequestPinModel::class.java]
        val title = findViewById<TextView>(R.id.pinTitle)

        model.title.observe(this, Observer {
            title.text = it
        })

        if (model.title.value == null) {
            model.title.value = getString(R.string.pin_title_enter)
        }

        keyboard = PinCodeFragment()

        val ft: FragmentTransaction = supportFragmentManager.beginTransaction()
        ft.replace(R.id.pinKeyboard, keyboard)
        ft.commit()
    }

    override fun onPinEnteredListener(pin: String) {

        if (pin == getStoredPin()) {

            setResult(RESULT_OK, Intent())
            model.clear()
            finish()

        } else {

            model.inc()

            if (model.pinTry.value == TRIES) {

                setResult(RESULT_CANCELED, Intent())
                model.clear()
                finish()

            } else {

                model.title.value = getString(R.string.pin_title_auth_error)
                model.viewModelScope.launch {
                    delay(DELAY)
                    model.title.value = getString(R.string.pin_title_enter)
                    keyboard.clear()
                    keyboard.unlock()
                }
            }
        }
    }
}
