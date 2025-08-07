#!/bin/bash

# Augment服务安装脚本
# 作者: Augment Team
# 版本: 1.0
# 描述: 用于在CentOS上安装和配置Augment服务

# =============================================================================
# 配置参数
# =============================================================================

APP_NAME="augment"
APP_VERSION="0.0.1-SNAPSHOT"
JAR_NAME="${APP_NAME}-${APP_VERSION}.jar"
APP_HOME="/data/augment"
SERVICE_USER="augment"
SERVICE_SCRIPT="augment-service.sh"

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

# 检查是否为root用户
check_root() {
    if [ "$EUID" -ne 0 ]; then
        print_message "red" "请使用root用户运行此脚本"
        exit 1
    fi
}

# 检查操作系统
check_os() {
    if [ ! -f /etc/redhat-release ]; then
        print_message "red" "此脚本仅支持CentOS/RHEL系统"
        exit 1
    fi
    
    local os_version=$(cat /etc/redhat-release)
    print_message "green" "检测到操作系统: $os_version"
}

# 检查Java环境
check_java() {
    if ! command -v java &> /dev/null; then
        print_message "yellow" "Java未安装，正在安装OpenJDK 11..."
        yum install -y java-11-openjdk java-11-openjdk-devel
        
        if [ $? -eq 0 ]; then
            print_message "green" "Java安装成功"
        else
            print_message "red" "Java安装失败"
            exit 1
        fi
    else
        local java_version=$(java -version 2>&1 | head -n 1)
        print_message "green" "Java已安装: $java_version"
    fi
}

# 创建服务用户
create_user() {
    if ! id "$SERVICE_USER" &>/dev/null; then
        print_message "yellow" "创建服务用户: $SERVICE_USER"
        useradd -r -s /bin/false -d "$APP_HOME" "$SERVICE_USER"
        
        if [ $? -eq 0 ]; then
            print_message "green" "用户创建成功"
        else
            print_message "red" "用户创建失败"
            exit 1
        fi
    else
        print_message "green" "服务用户已存在: $SERVICE_USER"
    fi
}

# 创建目录结构
create_directories() {
    print_message "yellow" "创建目录结构..."
    
    # 创建应用目录
    mkdir -p "$APP_HOME"
    mkdir -p "$APP_HOME/logs"
    mkdir -p "$APP_HOME/config"
    mkdir -p "$APP_HOME/backup"
    
    # 设置目录权限
    chown -R "$SERVICE_USER:$SERVICE_USER" "$APP_HOME"
    chmod 755 "$APP_HOME"
    chmod 755 "$APP_HOME/logs"
    chmod 755 "$APP_HOME/config"
    chmod 755 "$APP_HOME/backup"
    
    print_message "green" "目录结构创建完成"
}

# 复制文件
copy_files() {
    print_message "yellow" "复制应用文件..."
    
    # 检查JAR文件是否存在
    if [ ! -f "$JAR_NAME" ]; then
        print_message "red" "JAR文件不存在: $JAR_NAME"
        print_message "yellow" "请确保在包含JAR文件的目录中运行此脚本"
        exit 1
    fi
    
    # 复制JAR文件
    cp "$JAR_NAME" "$APP_HOME/"
    chown "$SERVICE_USER:$SERVICE_USER" "$APP_HOME/$JAR_NAME"
    chmod 644 "$APP_HOME/$JAR_NAME"
    
    # 复制启动脚本
    if [ -f "$SERVICE_SCRIPT" ]; then
        cp "$SERVICE_SCRIPT" "$APP_HOME/"
        chown "$SERVICE_USER:$SERVICE_USER" "$APP_HOME/$SERVICE_SCRIPT"
        chmod 755 "$APP_HOME/$SERVICE_SCRIPT"
        
        # 创建系统级别的启动脚本链接
        ln -sf "$APP_HOME/$SERVICE_SCRIPT" "/usr/local/bin/augment"
        
        print_message "green" "文件复制完成"
    else
        print_message "red" "启动脚本不存在: $SERVICE_SCRIPT"
        exit 1
    fi
}

# 创建systemd服务文件
create_systemd_service() {
    print_message "yellow" "创建systemd服务文件..."
    
    cat > /etc/systemd/system/augment.service << EOF
[Unit]
Description=Augment Application Service
After=network.target

[Service]
Type=forking
User=$SERVICE_USER
Group=$SERVICE_USER
WorkingDirectory=$APP_HOME
ExecStart=$APP_HOME/$SERVICE_SCRIPT start
ExecStop=$APP_HOME/$SERVICE_SCRIPT stop
ExecReload=$APP_HOME/$SERVICE_SCRIPT restart
PIDFile=$APP_HOME/augment.pid
Restart=on-failure
RestartSec=10

[Install]
WantedBy=multi-user.target
EOF

    # 重新加载systemd配置
    systemctl daemon-reload
    
    # 启用服务
    systemctl enable augment.service
    
    print_message "green" "systemd服务配置完成"
}

# 配置防火墙
configure_firewall() {
    print_message "yellow" "配置防火墙..."
    
    # 检查防火墙状态
    if systemctl is-active --quiet firewalld; then
        print_message "green" "防火墙正在运行，添加端口规则..."
        
        # 添加常用端口（根据实际应用需要调整）
        firewall-cmd --permanent --add-port=8081/tcp
        firewall-cmd --permanent --add-port=8443/tcp
        firewall-cmd --reload
        
        print_message "green" "防火墙配置完成"
    else
        print_message "yellow" "防火墙未运行，跳过配置"
    fi
}

# 创建日志轮转配置
create_logrotate() {
    print_message "yellow" "配置日志轮转..."
    
    cat > /etc/logrotate.d/augment << EOF
$APP_HOME/logs/*.log {
    daily
    missingok
    rotate 30
    compress
    delaycompress
    notifempty
    create 644 $SERVICE_USER $SERVICE_USER
    postrotate
        /usr/local/bin/augment restart > /dev/null 2>&1 || true
    endscript
}
EOF

    print_message "green" "日志轮转配置完成"
}

# 显示安装信息
show_info() {
    print_message "green" "============================================"
    print_message "green" "Augment服务安装完成！"
    print_message "green" "============================================"
    echo ""
    echo "安装信息:"
    echo "  应用目录: $APP_HOME"
    echo "  服务用户: $SERVICE_USER"
    echo "  JAR文件: $APP_HOME/$JAR_NAME"
    echo "  日志目录: $APP_HOME/logs"
    echo ""
    echo "服务管理命令:"
    echo "  启动服务: systemctl start augment"
    echo "  停止服务: systemctl stop augment"
    echo "  重启服务: systemctl restart augment"
    echo "  查看状态: systemctl status augment"
    echo "  查看日志: journalctl -u augment -f"
    echo ""
    echo "应用管理命令:"
    echo "  启动应用: augment start"
    echo "  停止应用: augment stop"
    echo "  重启应用: augment restart"
    echo "  查看状态: augment status"
    echo "  查看日志: augment logs"
    echo ""
    echo "配置文件位置:"
    echo "  应用配置: $APP_HOME/config/"
    echo "  日志配置: /etc/logrotate.d/augment"
    echo "  服务配置: /etc/systemd/system/augment.service"
    echo ""
    print_message "yellow" "建议在启动服务前检查配置文件是否正确"
}

# =============================================================================
# 主程序
# =============================================================================

main() {
    print_message "green" "开始安装Augment服务..."
    
    # 检查环境
    check_root
    check_os
    check_java
    
    # 安装步骤
    create_user
    create_directories
    copy_files
    create_systemd_service
    configure_firewall
    create_logrotate
    
    # 显示安装信息
    show_info
    
    print_message "green" "安装完成！"
}

# 运行主程序
main "$@"
