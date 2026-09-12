#!/usr/bin/env bash
# 一键生产打包：web/admin 构建产物拷入 server 静态目录 → 单体 bootJar
# 产物：server/build/libs/*.jar，启动后 http://<host>:8080/ （员工端）与 /admin/ （中台）
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

# ── 可移植的工具链解析 ──（优先级：环境变量 > ~/tools/env.sh > 常见默认位置）
if [ -f "$HOME/tools/env.sh" ]; then
  # shellcheck disable=SC1091
  source "$HOME/tools/env.sh"
fi
if [ -z "${JAVA_HOME:-}" ] || [ ! -x "$JAVA_HOME/bin/java" ]; then
  for d in "$HOME"/.jdks/*/ "$HOME"/tools/jdk*/; do
    if [ -x "${d}bin/java" ]; then export JAVA_HOME="${d%/}"; break; fi
  done
fi
if [ -d "$HOME/zcode-tools/node/bin" ]; then
  export PATH="$HOME/zcode-tools/node/bin:$PATH"
fi
if [ -n "${JAVA_HOME:-}" ]; then
  export PATH="$JAVA_HOME/bin:$PATH"
fi
command -v java >/dev/null 2>&1 || { echo "❌ 未找到 java，请设置 JAVA_HOME（JDK 17+）"; exit 1; }
command -v node >/dev/null 2>&1 || { echo "❌ 未找到 node，请安装 Node.js 20+ 或将其加入 PATH"; exit 1; }

echo "▶ 构建 web（员工端）"
( cd "$ROOT/web" && npm install --no-audit --no-fund && npm run build )

echo "▶ 构建 admin（中台）"
( cd "$ROOT/admin" && npm install --no-audit --no-fund && npm run build )

echo "▶ 拷入 server 静态目录"
STATIC="$ROOT/server/src/main/resources/static"
rm -rf "$STATIC"
mkdir -p "$STATIC/admin"
cp -r "$ROOT/web/dist/." "$STATIC/"
cp -r "$ROOT/admin/dist/." "$STATIC/admin/"

echo "▶ 打包 server bootJar"
( cd "$ROOT/server" && ./gradlew bootJar )

JAR=$(ls -t "$ROOT/server/build/libs/"*.jar | head -1)
echo
echo "✅ 打包完成: $JAR"
echo "   启动: LUNCH_DATA_DIR=/var/lib/lunch-order java -jar $JAR"
echo "   访问: http://<host>:8080/  与  http://<host>:8080/admin/"
