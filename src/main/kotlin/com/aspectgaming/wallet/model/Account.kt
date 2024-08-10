package com.aspectgaming.wallet.model
import java.math.BigDecimal

// 數據模型
data class Account(
    val id: String = "",
    val userId: String = "",
    val currency: String = "",
    val balance: BigDecimal = BigDecimal.ZERO
)
