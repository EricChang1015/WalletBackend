package com.aspectgaming.wallet

import com.aspectgaming.wallet.model.Account
import com.aspectgaming.wallet.model.User
import com.aspectgaming.wallet.repository.AccountRepository
import com.aspectgaming.wallet.service.AccountService
import com.aspectgaming.wallet.service.AuthService
import io.vertx.core.AbstractVerticle
import io.vertx.core.Promise
import io.vertx.core.json.Json
import io.vertx.ext.auth.KeyStoreOptions
import io.vertx.ext.auth.jwt.JWTAuth
import io.vertx.ext.auth.jwt.JWTAuthOptions
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.kotlin.core.json.json
import io.vertx.kotlin.core.json.obj

class WalletApplication : AbstractVerticle() {

    private lateinit var authService: AuthService

    override fun start(startPromise: Promise<Void>) {
        val router = Router.router(vertx)
        router.route().handler(BodyHandler.create())
        println(" 設置 JWT 認證")
        // 設置 JWT 認證
        val jwtAuthOptions = JWTAuthOptions()
            .setKeyStore(
                KeyStoreOptions()
                    .setType("pkcs12")
                    .setPath("keystore.p12")
                    .setPassword("secret")
            )

        val jwtAuth = JWTAuth.create(vertx, jwtAuthOptions)

        authService = AuthService(vertx, jwtAuth)
        println(" 配置路由")

        // 配置路由
        configureRoutes(router)

        // 啟動HTTP服務器
        println(" 啟動HTTP服務器")
        vertx.createHttpServer()
            .requestHandler(router)
            .listen(8080) { result ->
                if (result.succeeded()) {
                    println("Server started on port 8080")
                    startPromise.complete()
                } else {
                    println("Failed to start server on port 8080 - ${result.cause()}")
                    startPromise.fail(result.cause())
                }
            }
    }

    private fun configureRoutes(router: Router) {
        router.post("/users").handler(::createUser)
        router.post("/accounts").handler(::createAccount)

        // 新增的認證路由
        router.post("/auth/login").handler(::login)
        router.post("/auth/logout").handler(::logout)
        router.post("/auth/refresh").handler(::refreshToken)

        // 保護的路由示例
        router.get("/protected").handler(authService::checkToken).handler { ctx ->
            ctx.response().end("這是一個受保護的資源")
        }
    }

    private fun createUser(context: RoutingContext) {
        val accountRepository = AccountRepository(vertx)
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
            val userInfo = jsonBody.mapTo(User::class.java)
            accountRepository.newUser(userInfo) { ar ->
                if (ar.succeeded()) {
                    context.response()
                        .setStatusCode(201)
                        .end(Json.encode(ar.result()))
                } else {
                    val errorMessage = ar.cause()?.message ?: "Unknown error"
                    context.response()
                        .setStatusCode(500)
                        .end(Json.encode(mapOf("error" to "Failed to create user: $errorMessage")))
                }
            }
        } catch (e: Exception) {
            context.response()
                .setStatusCode(400)
                .end(Json.encode(mapOf("error" to "Invalid user data: ${e.message}")))
        }
    }

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
                    val errorMessage = ar.cause()?.message ?: "Unknown error"
                    context.response()
                        .setStatusCode(500)
                        .end(Json.encode(mapOf("error" to "Failed to create account: $errorMessage")))
                }
            }
        } catch (e: Exception) {
            context.response()
                .setStatusCode(400)
                .end(Json.encode(mapOf("error" to "Invalid account data: ${e.message}")))
        }
    }

    private fun login(context: RoutingContext) {
        val body = context.bodyAsJson
        println("Raw body: ${body}")

        val loginType = body.getString("loginType")
        val identifier = body.getString(if (loginType == "email") "email" else "username")
        val password = body.getString("password")

        authService.login(loginType, identifier, password) { ar ->
            if (ar.succeeded()) {
                context.response()
                    .setStatusCode(200)
                    .putHeader("Content-Type", "application/json")
                    .end(json {
                        obj(
                            "token" to ar.result(),
                            "tokenType" to "Bearer",
                            "expiresIn" to 3600
                        )
                    }.encode())
            } else {
                context.response()
                    .setStatusCode(401)
                    .putHeader("Content-Type", "application/json")
                    .end(json {
                        obj("error" to obj(
                            "code" to "UNAUTHORIZED",
                            "message" to ar.cause().message
                        ))
                    }.encode())
            }
        }
    }

    private fun logout(context: RoutingContext) {
        val token = context.request().getHeader("Authorization")?.removePrefix("Bearer ")
        if (token != null) {
            authService.logout(token) { ar ->
                if (ar.succeeded()) {
                    context.response().setStatusCode(200).end()
                } else {
                    context.response().setStatusCode(500).end()
                }
            }
        } else {
            context.response().setStatusCode(400).end()
        }
    }

    private fun refreshToken(context: RoutingContext) {
        val token = context.request().getHeader("Authorization")?.removePrefix("Bearer ")
        if (token != null) {
            authService.refreshToken(token) { ar ->
                if (ar.succeeded()) {
                    context.response()
                        .setStatusCode(200)
                        .putHeader("Content-Type", "application/json")
                        .end(json {
                            obj(
                                "token" to ar.result(),
                                "tokenType" to "Bearer",
                                "expiresIn" to 3600
                            )
                        }.encode())
                } else {
                    context.response()
                        .setStatusCode(401)
                        .putHeader("Content-Type", "application/json")
                        .end(json {
                            obj("error" to obj(
                                "code" to "UNAUTHORIZED",
                                "message" to ar.cause().message
                            ))
                        }.encode())
                }
            }
        } else {
            context.response().setStatusCode(400).end()
        }
    }
}