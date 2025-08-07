#!/bin/bash

# Augment服务启动脚本
# 作者: Augment Team
# 版本: 1.0
# 描述: 用于在CentOS上启动、停止、重启Augment服务

# =============================================================================
# 配置参数
# =============================================================================

# 应用配置
APP_NAME="augment"
APP_VERSION="0.0.1-SNAPSHOT"
JAR_NAME="${APP_NAME}-${APP_VERSION}.jar"

# 目录配置
APP_HOME="/data/augment"
JAR_PATH="${APP_HOME}/${JAR_NAME}"
LOG_DIR="${APP_HOME}/logs"
PID_FILE="${APP_HOME}/${APP_NAME}.pid"

# JVM配置
JAVA_OPTS="-Xms512m -Xmx2g -XX:+UseG1GC -XX:+UseStringDeduplication"
JAVA_OPTS="${JAVA_OPTS} -XX:+PrintGCDetails -XX:+PrintGCTimeStamps"
JAVA_OPTS="${JAVA_OPTS} -Xloggc:${LOG_DIR}/gc.log"
JAVA_OPTS="${JAVA_OPTS} -XX:+HeapDumpOnOutOfMemoryError"
JAVA_OPTS="${JAVA_OPTS} -XX:HeapDumpPath=${LOG_DIR}/"

# 应用配置
APP_OPTS="--spring.profiles.active=prod"
APP_OPTS="${APP_OPTS} --logging.file.path=${LOG_DIR}"
APP_OPTS="${APP_OPTS} --logging.level.root=INFO"

# 日志配置
STDOUT_LOG="${LOG_DIR}/application.log"
ERROR_LOG="${LOG_DIR}/error.log"

# =============================================================================
# 工具函数
# =============================================================================

# 打印带颜色的消息
print_message() {
    local color=$1
    local message=$2
    case $color in
        "green")  echo -e "\033[32m[INFO] $message\033[0m" ;;
        "yellow") echo -e "\033[33m[WARN] $message\033[0m" ;;
        "red")    echo -e "\033[31m[ERROR] $message\033[0m" ;;
        *)        echo "[INFO] $message" ;;
    esac
}

# 检查Java环境
check_java() {
    if [ -z "$JAVA_HOME" ]; then
        JAVA_CMD="java"
    else
        JAVA_CMD="$JAVA_HOME/bin/java"
    fi
    
    if ! command -v $JAVA_CMD &> /dev/null; then
        print_message "red" "Java未安装或未配置JAVA_HOME环境变量"
        exit 1
    fi
    
    local java_version=$($JAVA_CMD -version 2>&1 | head -n 1 | cut -d'"' -f2)
    print_message "green" "使用Java版本: $java_version"
}

# 检查必要的目录和文件
check_environment() {
    # 检查应用目录
    if [ ! -d "$APP_HOME" ]; then
        print_message "yellow" "创建应用目录: $APP_HOME"
        mkdir -p "$APP_HOME"
    fi
    
    # 检查日志目录
    if [ ! -d "$LOG_DIR" ]; then
        print_message "yellow" "创建日志目录: $LOG_DIR"
        mkdir -p "$LOG_DIR"
    fi
    
    # 检查JAR文件
    if [ ! -f "$JAR_PATH" ]; then
        print_message "red" "JAR文件不存在: $JAR_PATH"
        exit 1
    fi
}

# 获取进程ID
get_pid() {
    if [ -f "$PID_FILE" ]; then
        cat "$PID_FILE"
    fi
}

# 检查进程是否运行
is_running() {
    local pid=$(get_pid)
    if [ -n "$pid" ] && kill -0 "$pid" 2>/dev/null; then
        return 0
    else
        return 1
    fi
}

# =============================================================================
# 服务操作函数
# =============================================================================

# 启动服务
start() {
    print_message "green" "正在启动 $APP_NAME 服务..."
    
    if is_running; then
        local pid=$(get_pid)
        print_message "yellow" "服务已经在运行中 (PID: $pid)"
        return 1
    fi
    
    check_java
    check_environment
    
    # 启动应用
    nohup $JAVA_CMD $JAVA_OPTS -jar "$JAR_PATH" $APP_OPTS \
        > "$STDOUT_LOG" 2> "$ERROR_LOG" &
    
    local pid=$!
    echo $pid > "$PID_FILE"
    
    # 等待启动
    sleep 3
    
    if is_running; then
        print_message "green" "服务启动成功 (PID: $pid)"
        print_message "green" "日志文件: $STDOUT_LOG"
        print_message "green" "错误日志: $ERROR_LOG"
        return 0
    else
        print_message "red" "服务启动失败"
        rm -f "$PID_FILE"
        return 1
    fi
}

# 停止服务
stop() {
    print_message "yellow" "正在停止 $APP_NAME 服务..."
    
    if ! is_running; then
        print_message "yellow" "服务未运行"
        rm -f "$PID_FILE"
        return 1
    fi
    
    local pid=$(get_pid)
    
    # 优雅停止
    kill "$pid"
    
    # 等待进程结束
    local count=0
    while is_running && [ $count -lt 30 ]; do
        sleep 1
        count=$((count + 1))
    done
    
    if is_running; then
        print_message "yellow" "优雅停止超时，强制终止进程"
        kill -9 "$pid"
        sleep 2
    fi
    
    if ! is_running; then
        print_message "green" "服务停止成功"
        rm -f "$PID_FILE"
        return 0
    else
        print_message "red" "服务停止失败"
        return 1
    fi
}

# 重启服务
restart() {
    print_message "green" "正在重启 $APP_NAME 服务..."
    stop
    sleep 2
    start
}

# 查看服务状态
status() {
    if is_running; then
        local pid=$(get_pid)
        print_message "green" "服务正在运行 (PID: $pid)"
        
        # 显示内存使用情况
        local memory_info=$(ps -p $pid -o pid,ppid,pcpu,pmem,vsz,rss,comm --no-headers)
        echo "进程信息: $memory_info"
        
        # 显示端口监听情况
        local ports=$(netstat -tlnp 2>/dev/null | grep $pid | awk '{print $4}' | cut -d: -f2)
        if [ -n "$ports" ]; then
            echo "监听端口: $ports"
        fi
        
        return 0
    else
        print_message "red" "服务未运行"
        return 1
    fi
}

# 查看日志
logs() {
    local lines=${1:-50}
    
    if [ -f "$STDOUT_LOG" ]; then
        print_message "green" "显示最近 $lines 行应用日志:"
        tail -n $lines "$STDOUT_LOG"
    else
        print_message "yellow" "日志文件不存在: $STDOUT_LOG"
    fi
}

# 查看错误日志
error_logs() {
    local lines=${1:-50}
    
    if [ -f "$ERROR_LOG" ]; then
        print_message "green" "显示最近 $lines 行错误日志:"
        tail -n $lines "$ERROR_LOG"
    else
        print_message "yellow" "错误日志文件不存在: $ERROR_LOG"
    fi
}

# =============================================================================
# 主程序
# =============================================================================

# 显示使用帮助
usage() {
    echo "用法: $0 {start|stop|restart|status|logs|error-logs}"
    echo ""
    echo "命令说明:"
    echo "  start       启动服务"
    echo "  stop        停止服务"
    echo "  restart     重启服务"
    echo "  status      查看服务状态"
    echo "  logs [n]    查看应用日志 (默认显示最近50行)"
    echo "  error-logs [n] 查看错误日志 (默认显示最近50行)"
    echo ""
    echo "配置信息:"
    echo "  应用目录: $APP_HOME"
    echo "  JAR文件: $JAR_PATH"
    echo "  日志目录: $LOG_DIR"
    echo "  内存配置: 2GB"
}

# 主程序入口
case "$1" in
    start)
        start
        ;;
    stop)
        stop
        ;;
    restart)
        restart
        ;;
    status)
        status
        ;;
    logs)
        logs $2
        ;;
    error-logs)
        error_logs $2
        ;;
    *)
        usage
        exit 1
        ;;
esac

exit $?
