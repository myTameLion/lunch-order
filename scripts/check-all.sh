#!/usr/bin/env bash
# 一键运行全部模块单元测试（server / web / admin / android）
set -uo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

# ── 可移植的工具链解析 ──
# 优先级：已设置的环境变量 > ~/tools/env.sh（可选的本机工具链约定）> 常见默认位置
if [ -f "$HOME/tools/env.sh" ]; then
  # shellcheck disable=SC1091
  source "$HOME/tools/env.sh"
fi
if [ -z "${JAVA_HOME:-}" ] || [ ! -x "$JAVA_HOME/bin/java" ]; then
  for d in "$HOME"/.jdks/*/ "$HOME"/tools/jdk*/; do
    if [ -x "${d}bin/java" ]; then export JAVA_HOME="${d%/}"; break; fi
  done
fi
export ANDROID_HOME="${ANDROID_HOME:-$HOME/tools/android-sdk}"
if [ -d "$HOME/zcode-tools/node/bin" ]; then
  export PATH="$HOME/zcode-tools/node/bin:$PATH"
fi
if [ -n "${JAVA_HOME:-}" ]; then
  export PATH="$JAVA_HOME/bin:$PATH"
fi
command -v java >/dev/null 2>&1 || { echo "❌ 未找到 java，请设置 JAVA_HOME（JDK 17+）"; exit 1; }
command -v node >/dev/null 2>&1 || { echo "❌ 未找到 node，请安装 Node.js 20+ 或将其加入 PATH"; exit 1; }

FAIL=0
step() { echo; echo "════════════ $1 ════════════"; }

step "server · Gradle test"
( cd "$ROOT/server" && ./gradlew test ) || FAIL=1

step "web · Vitest"
( cd "$ROOT/web" && npm install --no-audit --no-fund >/dev/null 2>&1; npm test ) || FAIL=1

step "admin · Vitest"
( cd "$ROOT/admin" && npm install --no-audit --no-fund >/dev/null 2>&1; npm test ) || FAIL=1

step "android · Gradle testDebugUnitTest"
( cd "$ROOT/android" && ./gradlew :app:testDebugUnitTest ) || FAIL=1

echo
if [ "$FAIL" -eq 0 ]; then
  echo "✅ 全部模块单测通过"
else
  echo "❌ 存在失败模块，请查看上方日志"
fi
exit $FAIL
