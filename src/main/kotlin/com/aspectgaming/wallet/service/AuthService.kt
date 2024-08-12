package com.aspectgaming.wallet.service

import com.aspectgaming.wallet.model.User
import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.jwt.JWTAuth
import io.vertx.ext.auth.JWTOptions
import io.vertx.ext.web.RoutingContext
import io.vertx.kotlin.core.json.json
import io.vertx.kotlin.core.json.obj

class AuthService(private val vertx: Vertx, private val jwtAuth: JWTAuth) {
    fun login(loginType: String, identifier: String, password: String, handler: Handler<AsyncResult<String>>) {
        // 在實際應用中，這裡應該查詢數據庫來驗證用戶
        // 這裡為了示例，我們使用硬編碼的用戶
        val user = User("1", "testuser", "password123", "test@example.com")
        println("Login request for $identifier with password $password using $loginType")
        println("user: $user")

        if ((loginType == "email" && identifier == user.email) || (loginType == "username" && identifier == user.username)) {
            if (password == user.password) {
                // 生成 JWT token
                val token = jwtAuth.generateToken(
                    json {
                        obj(
                            "sub" to user.userId,
                            "username" to user.username,
                            "email" to user.email
                        )
                    },
                    JWTOptions().setExpiresInMinutes(60)
                )
                println("Login successful")
                handler.handle(Future.succeededFuture(token))
            } else {
                println("Invalid password")
                handler.handle(Future.failedFuture("Invalid password"))
            }
        } else {
            println("User not found")
            handler.handle(Future.failedFuture("User not found"))
        }
    }

    fun logout(token: String, handler: Handler<AsyncResult<Void>>) {
        // 在實際應用中，你可能需要將 token 加入黑名單或在 Redis 中標記為已失效
        // 這裡為了示例，我們只是簡單地返回成功
        handler.handle(Future.succeededFuture())
    }

    fun refreshToken(oldToken: String, handler: Handler<AsyncResult<String>>) {
        jwtAuth.authenticate(json { obj("token" to oldToken) }) { ar ->
            if (ar.succeeded()) {
                val user = ar.result()
                val newToken = jwtAuth.generateToken(
                    json {
                        obj(
                            "sub" to user.principal().getString("sub"),
                            "username" to user.principal().getString("username"),
                            "email" to user.principal().getString("email")
                        )
                    },
                    JWTOptions().setExpiresInMinutes(60)
                )
                handler.handle(Future.succeededFuture(newToken))
            } else {
                handler.handle(Future.failedFuture(ar.cause()))
            }
        }
    }

    fun validateToken(token: String): Future<Boolean> {
        return Future.future { promise ->
            jwtAuth.authenticate(JsonObject().put("token", token)) { res ->
                if (res.succeeded()) {
                    promise.complete(true)
                } else {
                    promise.complete(false)
                }
            }
        }
    }

    // 這個方法可以用作路由處理器
    fun checkToken(context: RoutingContext) {
        val token = context.request().getHeader("Authorization")?.removePrefix("Bearer ")
        if (token == null) {
            context.fail(401)
            return
        }

        validateToken(token).onComplete { ar ->
            if (ar.succeeded() && ar.result()) {
                context.next()
            } else {
                context.fail(401)
            }
        }
    }
}