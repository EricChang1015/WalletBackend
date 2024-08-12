錢包 API v1
===

[TOC]

## 簡介
這套 API 設計用於支持中心化交易所的核心錢包操作，包括帳戶管理、資金存取、餘額查詢等關鍵功能。我們的 API 遵循 RESTful 設計原則，提供了直觀且易於使用的接口。

## 主要功能：
1. 用戶管理：創建、查詢和更新用戶信息。
2. 帳戶管理：創建、刪除和更新用戶帳戶。
3. 黑名單管理：實施風險控制，管理受限制的帳戶。
4. 餘額操作：查詢帳戶餘額，支持多種加密貨幣。
5. 存款功能：處理入金操作，生成充值地址。
6. 提款功能：安全地處理出金請求。
7. 交易歷史：檢索詳細的交易記錄。
8. 內部轉帳：在系統內部進行資金轉移。

## API 特點：
* RESTful 架構：遵循標準 HTTP 方法和狀態碼。
* 版本控制：通過 URL 路徑實現 API 版本管理。
* 安全性：實施嚴格的身份驗證和授權機制。
* 錯誤處理：提供詳細的錯誤碼和描述信息。
* 多幣種支持：靈活處理各種加密貨幣。

## 基本URL結構
```
https://api.example.com/v1
```

## 認證
所有請求除了`/auth/login` 與`/users`,
其餘請求, 請求頭必須包含Authorization
- **請求頭:**
  ```
  Authorization: Bearer <token>
  ```

### 1. 用戶登錄
- **POST** `/auth/login`
- **狀態碼:** 200 (OK), 401 (Unauthorized)
- **請求體:**
  ```json
  {
    "loginType": "email" | "username",
    "email": "string",
    "username": "string",
    "password": "string"
  }
  ```
  注意：根據 loginType 的不同，只需提供相應的字段：
  - email + password
  - username + password

- **響應體:**
  ```json
  {
    "userId": "string",
    "token": "string",
    "tokenType": "Bearer",
    "expiresIn": 3600
  }
  ```

### 2. 用戶登出
- **POST** `/auth/logout`
- **狀態碼:** 200 (OK), 401 (Unauthorized)

### 3. 刷新訪問令牌
- **POST** `/auth/refresh`
- **狀態碼:** 200 (OK), 401 (Unauthorized)
- **響應體:**
  ```json
  {
    "token": "string",
    "tokenType": "Bearer",
    "expiresIn": 3600
  }
  ```

## 用戶管理
### 1. 創建用戶
- **POST** `/users`
- **狀態碼:** 201 (Created), 400 (Bad Request), 409 (Conflict)
- **請求體:**
  ```json
  {
    "username": "string",
    "email": "string",
    "password": "string"
  }
  ```
- **響應體:**
  ```json
  {
    "userId": "string",
    "username": "string",
    "email": "string",
    "email_verified": "boolean",
    "status": "string",
    "createdAt": "timestamp"
  }
  ```
* status: active, inactive, banned

### 2. 獲取用戶信息
- **GET** `/users/{userId}`
- **狀態碼:** 200 (OK), 404 (Not Found)
- **響應體:** 同創建用戶

### 3. 更新用戶信息
- **PUT** `/users/{userId}`
- **狀態碼:** 200 (OK), 400 (Bad Request), 404 (Not Found)
- **請求體:**
  ```json
  {
    "username": "string",
    "email": "string",
    "password": "string"
  }
  ```


## 帳戶管理

### 1. 新增帳號
- **POST** `/accounts`
- **狀態碼:** 201 (Created), 400 (Bad Request), 409 (Conflict)
- **請求體:**
  ```json
  {
    "userId": "string",
    "currency": "string"
  }
  ```
- **響應體:**
  ```json
  {
    "accountId": "string",
    "userId": "string",
    "currency": "string",
    "balance": "0",
    "availableBalance": "0",
    "frozenBalance": "0",
    "createdAt": "timestamp"
  }
  ```

### 2. 移除帳號
- **DELETE** `/accounts/{accountId}`
- **狀態碼:** 200 (OK), 404 (Not Found)

### 3. 獲取帳號信息
- **GET** `/accounts/{accountId}`
- **狀態碼:** 200 (OK), 404 (Not Found)
- **響應體:** 同新增帳號

## 黑名單功能

### 1. 添加到黑名單
- **POST** `/blacklist`
- **狀態碼:** 201 (Created), 400 (Bad Request), 409 (Conflict)
- **請求體：**
  ```json
  {
    "userId": "string",
    "reason": "string"
  }
  ```
- **響應體:**
  ```json
  {
    "id": "string",
    "userId": "string"
  }
  ```

### 2. 從黑名單移除
- **DELETE** `/blacklist/{userId}`
- **狀態碼:** 200 (OK), 404 (Not Found)

### 3. 檢查是否在黑名單
- **GET** `/blacklist/{userId}`
- **狀態碼:** 200 (OK), 404 (Not Found)

## 餘額查詢

### 查詢餘額
- **GET** `/accounts/{accountId}/balance`
- **狀態碼:** 200 (OK), 404 (Not Found)
- **響應體:**
  ```json
  {
    "currency": "string",
    "balance": "string",
    "availableBalance": "string",
    "frozenBalance": "string"
  }
  ```

## 存款功能

### 1. 存款
- **POST** `/accounts/{accountId}/deposits`
- **狀態碼:** 201 (Created), 400 (Bad Request), 404 (Not Found)
- **請求體:**
  ```json
  {
    "amount": "string",
    "currency": "string",
    "clientTransactionId": "string"
  }
  ```
- **響應體:** 同查詢餘額

## 提款功能

### 提款
- **POST** `/accounts/{accountId}/withdrawals`
- **狀態碼:** 201 (Created), 400 (Bad Request), 404 (Not Found), 422 (Unprocessable Entity)
- **請求體:**
  ```json
  {
    "amount": "string",
    "currency": "string",
    "address": "string",
    "clientTransactionId": "string"
  }
  ```
- **響應體:** 同查詢餘額

## 內部轉帳

### 內部轉帳
- **POST** `/transfers`
- **狀態碼:** 201 (Created), 400 (Bad Request), 404 (Not Found), 422 (Unprocessable Entity)
- **請求體:**
  ```json
  {
    "fromAccountId": "string",
    "toAccountId": "string",
    "amount": "string",
    "currency": "string"
  }
  ```
- **響應體:** 同查詢餘額

## 錯誤響應格式

所有的錯誤響應都應該遵循以下格式：

```json
{
  "error": {
    "code": "ERROR_CODE",
    "message": "錯誤描述信息"
  }
}
```

## 錯誤碼列表

- USER_NOT_FOUND: 找不到指定的用戶
- ACCOUNT_NOT_FOUND: 找不到指定的帳戶
- INVALID_AMOUNT: 無效的金額
- INSUFFICIENT_FUNDS: 餘額不足
- ACCOUNT_BLOCKED: 帳戶已被凍結
- INVALID_ADDRESS: 無效的地址
- TRANSACTION_FAILED: 交易失敗
- BLACKLISTED: 帳戶在黑名單中
- INVALID_USER_ID: 提供的用戶ID格式不正確或無效
- INVALID_ACCOUNT_ID: 提供的帳號ID格式不正確或無效
- DUPLICATE_USER: 該用戶已經存在，無法重複創建
- DUPLICATE_ACCOUNT: 該帳號已經存在，無法重複創建
- ADDRESS_GENERATION_FAILED: 充值地址生成失敗
- TRANSFER_NOT_ALLOWED: 禁止進行該轉帳操作
- INSUFFICIENT_PERMISSIONS: 用戶無權執行該操作
- DUPLICATE_TRANSACTION: 重複的交易ID
