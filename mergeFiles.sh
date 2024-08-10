#!/bin/bash

# 設定輸出文件名稱
output_file="all_files_content.md"

# 清空輸出文件（如果已存在）
> "$output_file"

# 遍歷 src 目錄下的所有檔案
find src -type f | while read -r file; do
    # 輸出檔案位置與名稱
    echo "# File: $file" >> "$output_file"
    echo '```' >> "$output_file"
    
    # 輸出檔案內容
    cat "$file" >> "$output_file"
    
    echo '```' >> "$output_file"
    echo "" >> "$output_file"
done

echo "檔案內容已合併到 $output_file"