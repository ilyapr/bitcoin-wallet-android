package com.bitzhash.wallet.bitcoin.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.*
import com.bitzhash.wallet.bitcoin.R
import com.bitzhash.wallet.bitcoin.setStoredPin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class PinCodeActivity : AppCompatActivity(), PinCodeFragment.OnPinEnteredListener {

    companion object {
        const val EXTRA_CREATE_PIN = "CREATE_PIN"
        private const val DELAY: Long = 2000 //2 seconds

        fun intentCreate(context: Context): Intent {
            val intent = Intent(context, PinCodeActivity::class.java)
            intent.putExtra(EXTRA_CREATE_PIN, true)
            return intent
        }
    }

    class PinModel : ViewModel() {
        val title = MutableLiveData<String>()
        var pin = MutableLiveData<String>()
    }

    private lateinit var model: PinModel

    private var createPin = false
    private lateinit var keyboard: PinCodeFragment
    private lateinit var title: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pin)

        setupBars()

        createPin = intent.extras?.getBoolean(EXTRA_CREATE_PIN) ?: false
        model = ViewModelProvider(this)[PinModel::class.java]
        title = findViewById(R.id.pinTitle)

        model.title.observe(this, Observer {
            title.text = it
        })

        if (model.title.value == null) {
            model.title.value = getString(R.string.pin_title_new)
        }

        keyboard = PinCodeFragment()

        val ft: FragmentTransaction = supportFragmentManager.beginTransaction()
        ft.replace(R.id.pinKeyboard, keyboard)
        ft.commit()
    }

    override fun onPinEnteredListener(pin: String) {

        when (model.pin.value) {
            null -> {
                model.pin.value = pin
                model.title.value = getString(R.string.pin_title_confirm)
                keyboard.unlock()
            }
            pin -> {

                setStoredPin(pin)

                if (createPin) {
                    startActivity(Intent(this, MainActivity::class.java))
                } else {
                    setResult(RESULT_OK, Intent())
                }

                finish()
            }
            else -> {
                model.title.value = getString(R.string.pin_title_incorrect)
                model.viewModelScope.launch {
                    delay(DELAY)
                    title.text = getString(R.string.pin_title_new)
                    model.pin.value = null
                    keyboard.clear()
                    keyboard.unlock()
                }
            }
        }
    }
}
