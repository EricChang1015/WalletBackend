#!/bin/bash

# 設置基本URL
BASE_URL="http://localhost:8080"

# 設置測試用戶信息
USERNAME="testuser"
EMAIL="test@example.com"
PASSWORD="password123"

# 顏色設置
GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# 函數：檢查HTTP狀態碼
check_status() {
    if [ $1 -eq $2 ]; then
        echo -e "${GREEN}成功: $3${NC}"
    else
        echo -e "${RED}失敗: $3 (狀態碼: $1, 預期: $2)${NC}"
        return 1
    fi
}

# 1. 嘗試登錄
echo "嘗試登錄..."
LOGIN_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST $BASE_URL/auth/login \
    -H "Content-Type: application/json" \
    -d "{\"loginType\":\"email\",\"email\":\"$EMAIL\",\"password\":\"$PASSWORD\"}")

HTTP_STATUS=$(echo "$LOGIN_RESPONSE" | tail -n1)
RESPONSE_BODY=$(echo "$LOGIN_RESPONSE" | sed '$d')

if ! check_status $HTTP_STATUS 200 "登錄"; then
    echo "登錄失敗，嘗試創建用戶..."

    # 創建用戶
    CREATE_USER_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST $BASE_URL/v1/users \
        -H "Content-Type: application/json" \
        -d "{\"username\":\"$USERNAME\",\"email\":\"$EMAIL\",\"password\":\"$PASSWORD\"}")

    HTTP_STATUS=$(echo "$CREATE_USER_RESPONSE" | tail -n1)
    RESPONSE_BODY=$(echo "$CREATE_USER_RESPONSE" | sed '$d')

    check_status $HTTP_STATUS 201 "創建用戶"
    echo "$RESPONSE_BODY"

    # 再次嘗試登錄
    echo "再次嘗試登錄..."
    LOGIN_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST $BASE_URL/auth/login \
        -H "Content-Type: application/json" \
        -d "{\"loginType\":\"email\",\"email\":\"$EMAIL\",\"password\":\"$PASSWORD\"}")

    HTTP_STATUS=$(echo "$LOGIN_RESPONSE" | tail -n1)
    RESPONSE_BODY=$(echo "$LOGIN_RESPONSE" | sed '$d')

    check_status $HTTP_STATUS 200 "登錄"
fi

echo "$RESPONSE_BODY"

# 提取 token
TOKEN=$(echo "$RESPONSE_BODY" | grep -o '"token":"[^"]*' | cut -d'"' -f4)

if [ -z "$TOKEN" ]; then
    echo -e "${RED}錯誤: 無法獲取 token${NC}"
    exit 1
fi

# 使用 token 進行身份驗證請求
echo "訪問受保護的資源..."
PROTECTED_RESPONSE=$(curl -s -w "\n%{http_code}" -X GET $BASE_URL/protected \
    -H "Authorization: Bearer $TOKEN")

HTTP_STATUS=$(echo "$PROTECTED_RESPONSE" | tail -n1)
RESPONSE_BODY=$(echo "$PROTECTED_RESPONSE" | sed '$d')

check_status $HTTP_STATUS 200 "訪問受保護的資源"
echo "$RESPONSE_BODY"

# 登出
echo "登出..."
LOGOUT_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST $BASE_URL/auth/logout \
    -H "Authorization: Bearer $TOKEN")

HTTP_STATUS=$(echo "$LOGOUT_RESPONSE" | tail -n1)
RESPONSE_BODY=$(echo "$LOGOUT_RESPONSE" | sed '$d')

check_status $HTTP_STATUS 200 "登出"
echo "$RESPONSE_BODY"

echo "測試完成"