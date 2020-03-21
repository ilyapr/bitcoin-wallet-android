package com.bitzhash.wallet.bitcoin.ui

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.lifecycle.*
import com.bitzhash.wallet.bitcoin.R
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class PinCodeFragment : Fragment() {

    companion object {
        private const val PIN_LENGTH = 6
        private const val DELAY = 100L
    }

    interface OnPinEnteredListener {
        fun onPinEnteredListener(pin: String)
    }

    class PinCodeViewModel : ViewModel() {

        val pin = MutableLiveData<String>()

        init {
            pin.value = ""
        }

        fun add(num: Int) {
            pin.value += num.toString()
        }

        fun del() {
            if (!pin.value.isNullOrEmpty()) {
                pin.value = pin.value!!.dropLast(1)
            }
        }
    }

    private lateinit var model: PinCodeViewModel
    private var listener: OnPinEnteredListener? = null
    private lateinit var bullets: List<ImageView>
    private lateinit var buttons: List<MaterialButton>
    private lateinit var backspace: MaterialButton

    fun lock() = lock(false)
    fun unlock() = lock(true)

    private fun lock(enabled: Boolean) {
        buttons.forEach { it.isEnabled = enabled }
        backspace.isEnabled = enabled
    }

    fun clear() {
        model.pin.value = ""
        bullets.forEach { it.setImageResource(R.drawable.pin_bullet) }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        model = activity?.run {
            ViewModelProvider(this)[PinCodeViewModel::class.java]
        } ?: throw Exception("Invalid Activity")

        val view = inflater.inflate(R.layout.fragment_pin_code, container, false)

        bullets = listOf(
            R.id.bullet_1,
            R.id.bullet_2,
            R.id.bullet_3,
            R.id.bullet_4,
            R.id.bullet_5,
            R.id.bullet_6
        ).map { view.findViewById<ImageView>(it) }

        buttons = listOf(
            R.id.keyboard_0,
            R.id.keyboard_1,
            R.id.keyboard_2,
            R.id.keyboard_3,
            R.id.keyboard_4,
            R.id.keyboard_5,
            R.id.keyboard_6,
            R.id.keyboard_7,
            R.id.keyboard_8,
            R.id.keyboard_9
        ).map { view.findViewById<MaterialButton>(it) }

        buttons.forEachIndexed { index, element ->
            element.setOnClickListener {
                model.add(index)

                if (model.pin.value!!.length == PIN_LENGTH) {
                    lock()

                    model.viewModelScope.launch {
                        delay(DELAY)
                        listener?.onPinEnteredListener(model.pin.value!!)
                        clear()
                    }
                }
            }
        }

        model.pin.observe(viewLifecycleOwner, Observer {
            for (i in 0 until PIN_LENGTH) {
                bullets[i].setImageResource(if (i < it.length) R.drawable.pin_bullet_filled else R.drawable.pin_bullet)
            }
        })

        backspace = view.findViewById(R.id.keyboard_backspace)
        backspace.setOnClickListener {
            model.del()
        }

        return view
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        if (context is OnPinEnteredListener) {
            listener = context
        } else {
            throw RuntimeException(context.toString() + " must implement " + OnPinEnteredListener::class.java.simpleName)
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }
}
