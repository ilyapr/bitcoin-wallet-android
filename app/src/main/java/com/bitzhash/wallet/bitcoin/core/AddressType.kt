package com.bitzhash.wallet.bitcoin.core

import io.horizontalsystems.bitcoincore.core.Bip

sealed class AddressType(val bip: Bip) {

    object Legacy : AddressType(Bip.BIP44)
    object P2SHSegwit : AddressType(Bip.BIP49)
    object NativeSegwit : AddressType(Bip.BIP84)

    fun type() = bip.name

    companion object {
        fun valueOf(type: String) = when (Bip.valueOf(type)) {
            Bip.BIP44 -> Legacy
            Bip.BIP49 -> P2SHSegwit
            Bip.BIP84 -> NativeSegwit
        }
    }
}
