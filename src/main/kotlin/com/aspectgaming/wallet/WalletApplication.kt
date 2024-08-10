package com.aspectgaming.wallet

import com.aspectgaming.wallet.model.Account
import com.aspectgaming.wallet.service.AccountService
import io.vertx.core.AbstractVerticle
import io.vertx.core.Promise
import io.vertx.core.json.Json
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.BodyHandler

class WalletApplication : AbstractVerticle() {

    override fun start(startPromise: Promise<Void>) {
        val router = Router.router(vertx)
        router.route().handler(BodyHandler.create())
        // 配置路由
        configureRoutes(router)

        // 啟動HTTP服務器
        vertx.createHttpServer()
            .requestHandler(router)
            .listen(8080) { result ->
                if (result.succeeded()) {
                    println("Server started on port 8080")
                    startPromise.complete()
                } else {
                    startPromise.fail(result.cause())
                }
            }
    }

    private fun configureRoutes(router: Router) {
        router.post("/v1/accounts").handler(::createAccount)
//        router.delete("/v1/accounts/:accountId").handler(::removeAccount)
//        router.get("/v1/accounts/:accountId/balance").handler(::getBalance)
//        router.post("/v1/accounts/:accountId/deposits").handler(::deposit)
//        router.post("/v1/accounts/:accountId/withdrawals").handler(::withdraw)
    }

    // 路由處理函數
    private fun createAccount(context: RoutingContext) {
        val accountService = AccountService(vertx)
        val body = context.body()
        println("Raw body: ${body.asString()}")
        if (body == null) {
            context.response()
                .setStatusCode(400)
                .end(Json.encode(mapOf("error" to "Request body is missing")))
            return
        }

        val jsonBody = body.asJsonObject()
        if (jsonBody == null) {
            context.response()
                .setStatusCode(400)
                .end(Json.encode(mapOf("error" to "Invalid JSON in request body")))
            return
        }

        try {
            val account = jsonBody.mapTo(Account::class.java)
            accountService.createAccount(account) { ar ->
                if (ar.succeeded()) {
                    context.response()
                        .setStatusCode(201)
                        .end(Json.encode(ar.result()))
                } else {
                    context.response()
                        .setStatusCode(500)
                        .end(Json.encode(mapOf("error" to "Failed to create account")))
                }
            }
        } catch (e: Exception) {
            context.response()
                .setStatusCode(400)
                .end(Json.encode(mapOf("error" to "Invalid account data: ${e.message}")))
        }
    }
    // 實現其他路由處理函數...
}
