#!/bin/bash

# 設定輸出文件名稱
output_folder="build"
output_file="${output_folder}/all_files_content.md"

# 清空輸出文件（如果已存在）
mkdir -p ${output_folder}
> "$output_file"

function outputContent() {
	file=$1
    echo "## File: $file" >> "$output_file"
    echo '```' >> "$output_file"
    
    # 輸出檔案內容
    cat "$file" >> "$output_file"
    
    echo '```' >> "$output_file"
    echo "" >> "$output_file"
}

find src -type f | while read -r file; do
	outputContent $file
done

outputContent db/init.sql
outputContent docker-compose.yml
outputContent settings.gradle.kts
outputContent build.gradle.kts

echo "檔案內容已合併到 $output_file"
