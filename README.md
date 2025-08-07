# Augment服务部署指南

## 概述

本文档描述如何在CentOS系统上部署和管理Augment服务。

## 系统要求

- **操作系统**: CentOS 7/8 或 RHEL 7/8
- **Java版本**: OpenJDK 11 或更高版本
- **内存**: 最少4GB（应用配置2GB堆内存）
- **磁盘空间**: 最少10GB可用空间
- **网络**: 确保8081端口可访问（可根据需要调整）

## 文件说明

- `augment-0.0.1-SNAPSHOT.jar` - 应用程序JAR包
- `augment-service.sh` - 服务启动脚本
- `install.sh` - 自动安装脚本
- `README.md` - 本说明文档

## 快速安装

### 1. 准备文件

将以下文件上传到服务器的同一目录：
```bash
augment-0.0.1-SNAPSHOT.jar
augment-service.sh
install.sh
```

### 2. 执行安装

```bash
# 给脚本执行权限
chmod +x install.sh augment-service.sh

# 以root用户运行安装脚本
sudo ./install.sh
```

### 3. 启动服务

```bash
# 使用systemd启动
sudo systemctl start augment

# 或使用应用脚本启动
sudo -u augment /data/augment/augment-service.sh start
```

## 服务管理

### systemd命令

```bash
# 启动服务
sudo systemctl start augment

# 停止服务
sudo systemctl stop augment

# 重启服务
sudo systemctl restart augment

# 查看服务状态
sudo systemctl status augment

# 查看服务日志
sudo journalctl -u augment -f

# 开机自启动
sudo systemctl enable augment
```

### 应用脚本命令

```bash
# 切换到augment用户
sudo su - augment

# 或直接使用命令
sudo -u augment augment start    # 启动
sudo -u augment augment stop     # 停止
sudo -u augment augment restart  # 重启
sudo -u augment augment status   # 状态
sudo -u augment augment logs     # 查看日志
sudo -u augment augment error-logs # 查看错误日志
```

## 目录结构

```
/data/augment/
├── augment-0.0.1-SNAPSHOT.jar    # 应用JAR包
├── augment-service.sh             # 启动脚本
├── augment.pid                    # 进程ID文件
├── logs/                          # 日志目录
│   ├── application.log            # 应用日志
│   ├── error.log                  # 错误日志
│   └── gc.log                     # GC日志
├── config/                        # 配置目录
└── backup/                        # 备份目录
```

## 配置说明

### JVM配置

- **堆内存**: 2GB (-Xmx2g)
- **初始堆内存**: 512MB (-Xms512m)
- **垃圾收集器**: G1GC
- **GC日志**: 启用并输出到logs/gc.log
- **内存溢出转储**: 启用并输出到logs/目录

### 应用配置

- **运行环境**: production (spring.profiles.active=prod)
- **日志级别**: INFO
- **日志文件**: logs/application.log

### 端口配置

默认端口为8081，可以通过修改应用配置文件调整。

## 日志管理

### 日志文件

- `application.log` - 应用运行日志
- `error.log` - 错误日志
- `gc.log` - JVM垃圾收集日志

### 日志轮转

系统已配置logrotate，日志文件会：
- 每天轮转一次
- 保留30天的历史日志
- 自动压缩旧日志文件

### 查看日志

```bash
# 查看应用日志（最近50行）
augment logs

# 查看应用日志（最近100行）
augment logs 100

# 查看错误日志
augment error-logs

# 实时查看日志
tail -f /data/augment/logs/application.log
```

## 故障排除

### 常见问题

1. **服务启动失败**
   ```bash
   # 检查Java环境
   java -version
   
   # 检查JAR文件权限
   ls -la /data/augment/augment-0.0.1-SNAPSHOT.jar
   
   # 查看错误日志
   augment error-logs
   ```

2. **端口被占用**
   ```bash
   # 检查端口占用
   netstat -tlnp | grep 8081

   # 或使用ss命令
   ss -tlnp | grep 8081
   ```

3. **内存不足**
   ```bash
   # 检查系统内存
   free -h
   
   # 检查进程内存使用
   augment status
   ```

### 性能监控

```bash
# 查看进程状态
augment status

# 查看系统资源使用
top -p $(cat /data/augment/augment.pid)

# 查看JVM信息
jstat -gc $(cat /data/augment/augment.pid)
```

## 安全建议

1. **用户权限**: 应用运行在专用的augment用户下，避免使用root权限
2. **文件权限**: 确保配置文件和日志文件权限正确设置
3. **防火墙**: 只开放必要的端口
4. **日志监控**: 定期检查错误日志，及时发现问题

## 备份与恢复

### 备份

```bash
# 创建备份目录
mkdir -p /backup/augment/$(date +%Y%m%d)

# 备份应用文件
cp /data/augment/augment-0.0.1-SNAPSHOT.jar /backup/augment/$(date +%Y%m%d)/

# 备份配置文件
cp -r /data/augment/config /backup/augment/$(date +%Y%m%d)/

# 备份日志文件（可选）
cp -r /data/augment/logs /backup/augment/$(date +%Y%m%d)/
```

### 恢复

```bash
# 停止服务
sudo systemctl stop augment

# 恢复文件
cp /backup/augment/20231225/augment-0.0.1-SNAPSHOT.jar /data/augment/
cp -r /backup/augment/20231225/config /data/augment/

# 设置权限
chown -R augment:augment /data/augment

# 启动服务
sudo systemctl start augment
```

## 更新升级

```bash
# 停止服务
sudo systemctl stop augment

# 备份当前版本
cp /data/augment/augment-0.0.1-SNAPSHOT.jar /data/augment/backup/

# 替换新版本
cp augment-0.0.2-SNAPSHOT.jar /data/augment/
chown augment:augment /data/augment/augment-0.0.2-SNAPSHOT.jar

# 更新启动脚本中的JAR文件名（如果版本号变化）
# 编辑 /data/augment/augment-service.sh

# 启动服务
sudo systemctl start augment

# 验证服务状态
augment status
```

## 联系支持

如遇到问题，请提供以下信息：
- 操作系统版本
- Java版本
- 错误日志内容
- 服务状态信息
