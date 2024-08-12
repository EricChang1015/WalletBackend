package com.aspectgaming.wallet

import io.vertx.core.Vertx
// 主函數
fun main() {
    val vertx = Vertx.vertx()
    println("Starting Wallet Application")
    vertx.deployVerticle(WalletApplication())
}