package com.bitzhash.wallet.bitcoin.core

sealed class FeePriority(val feeRate: Int) {
    object Low : FeePriority(5)
    object Medium : FeePriority(10)
    object High : FeePriority(15)
}
