#!/usr/bin/env bash
# 本机 Maven 启动器：系统 Maven 安装不完整（缺 m2.conf），且 sh 版 mvn 依赖 dirname
# 在 Git Bash 下会失败，这里直接用 java -classpath 启动 classworlds。
# 用法：./mvn.sh clean package
# JDK 解析：外部 JAVA_HOME 优先，但必须真实可执行；否则回退本机 JDK 8
#（环境里的 JAVA_HOME 可能指向已卸载的旧版本，不能盲信）
# pwd -W 是 Git Bash 特有（返回 Windows 风格路径），Linux CI 下回退普通 pwd
BASE=$(cd "${BASH_SOURCE[0]%/*}" && { pwd -W 2>/dev/null || pwd; })
MH="$BASE/.tools/apache-maven-3.9.9"
JH="${JAVA_HOME:-}"
JH="${JH//\\//}"  # Windows 反斜杠路径转正斜杠
if [ -z "$JH" ] || [ ! -x "$JH/bin/java" ]; then
  JH="C:/Program Files/Eclipse Adoptium/jdk-8.0.482.8-hotspot"
fi
export JAVA_HOME="$JH"
exec "$JH/bin/java" -classpath "$MH/boot/plexus-classworlds-2.8.0.jar" \
  "-Dclassworlds.conf=$MH/bin/m2.conf" \
  "-Dmaven.home=$MH" \
  "-Dmaven.multiModuleProjectDirectory=$BASE" \
  org.codehaus.plexus.classworlds.launcher.Launcher "$@"
