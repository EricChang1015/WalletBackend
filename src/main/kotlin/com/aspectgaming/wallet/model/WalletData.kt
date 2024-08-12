package com.aspectgaming.wallet.model
import java.math.BigDecimal

data class User(
    val userId: String = "",
    val username: String = "",
    val password: String = "",
    val email: String = ""
)

// 數據模型
data class Account(
    val accountId: String = "",
    val userId: String = "",
    val currency: String = "",
    val balance: BigDecimal = BigDecimal.ZERO
)
