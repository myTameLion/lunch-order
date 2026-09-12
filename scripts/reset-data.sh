#!/usr/bin/env bash
# 重置数据目录（删除后下次启动自动重新种子初始化）
# 用法: ./reset-data.sh [数据目录]   默认: <repo>/server/data
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DATA_DIR="${1:-$ROOT/server/data}"

if [ ! -d "$DATA_DIR" ]; then
  echo "目录不存在: $DATA_DIR（无需重置）"
  exit 0
fi
echo "将删除数据目录: $DATA_DIR"
read -r -p "确认? [y/N] " ans
if [ "${ans:-N}" = "y" ] || [ "${ans:-N}" = "Y" ]; then
  rm -rf "$DATA_DIR"
  echo "✅ 已删除。下次启动服务端将自动重新种子初始化（admin/admin123 与 5 个演示账号）。"
else
  echo "已取消"
fi
