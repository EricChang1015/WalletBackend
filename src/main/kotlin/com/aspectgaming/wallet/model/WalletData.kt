package com.aspectgaming.wallet.model
import java.math.BigDecimal


data class AuthRequest(
    val loginType: String = "",
    val identifier: String = "",
    val password: String = ""
)

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
