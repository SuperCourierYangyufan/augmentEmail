# API认证系统使用说明

## 概述

本系统为所有API接口添加了认证机制，通过`authKey`参数进行访问控制和使用次数限制。

## 功能特性

### 1. 认证密钥管理
- 支持认证密钥的创建、查询、状态管理
- 支持使用次数限制和过期时间控制
- 自动记录使用情况和IP地址

### 2. 操作日志记录
- 记录所有邮件相关操作
- 包含操作类型、结果、响应时间等详细信息
- 支持按认证密钥查询日志

### 3. 数据库表结构

#### auth_keys 表
```sql
- id: 主键ID
- auth_key: 认证密钥（唯一）
- max_count: 最大使用次数
- used_count: 已使用次数
- status: 状态（1-有效，0-无效）
- description: 密钥描述
- create_time: 创建时间
- expire_time: 过期时间
- last_used_time: 最后使用时间
- last_used_ip: 最后使用IP
```

#### email_logs 表
```sql
- id: 主键ID
- auth_key: 使用的认证密钥
- operation_type: 操作类型（GENERATE_EMAIL, GET_VERIFICATION）
- email_address: 邮箱地址
- result: 操作结果（SUCCESS, FAILED）
- error_message: 错误信息
- request_ip: 请求IP
- user_agent: 用户代理
- create_time: 创建时间
- response_time: 响应时间(毫秒)
```

## API接口说明

### 1. 生成临时邮箱
```
GET /temp-email?authKey={认证密钥}
```

**参数：**
- `authKey`: 必需，认证密钥

**响应：**
- 成功：返回生成的临时邮箱地址
- 失败：返回错误信息

**示例：**
```bash
curl "http://localhost:8081/temp-email?authKey=test_key_123456"
```

### 2. 获取验证码
```
GET /verification-code?authKey={认证密钥}&emailAddress={邮箱地址}
```

**参数：**
- `authKey`: 必需，认证密钥
- `emailAddress`: 必需，目标邮箱地址

**响应：**
- 成功：返回验证码
- 失败：返回错误信息

**示例：**
```bash
curl "http://localhost:8081/verification-code?authKey=test_key_123456&emailAddress=test@example.com"
```

## 管理接口

### 1. 查看认证密钥信息
```
GET /admin/auth-key/{authKey}
```

### 2. 查看认证密钥使用日志
```
GET /admin/auth-key/{authKey}/logs?limit=50
```

### 3. 获取所有认证密钥
```
GET /admin/auth-keys
```

### 4. 创建新认证密钥
```
POST /admin/auth-key
```
**参数：**
- `authKey`: 认证密钥
- `maxCount`: 最大使用次数（默认100）
- `description`: 描述（可选）
- `expireTime`: 过期时间（可选，格式：2025-12-31T23:59:59）

### 5. 更新认证密钥状态
```
PUT /admin/auth-key/{authKey}/status?status={状态}
```

### 6. 获取系统统计信息
```
GET /admin/stats
```

## 预置测试数据

系统启动时会自动创建以下测试认证密钥：

1. **test_key_123456**
   - 最大使用次数：1000
   - 描述：测试密钥
   - 过期时间：2025-12-31 23:59:59

2. **demo_key_789012**
   - 最大使用次数：500
   - 描述：演示密钥
   - 过期时间：2025-12-31 23:59:59

3. **admin_key_345678**
   - 最大使用次数：10000
   - 描述：管理员密钥
   - 过期时间：2026-12-31 23:59:59

## 错误处理

### 常见错误码
- `400 Bad Request`: 认证失败或参数错误
- `404 Not Found`: 未找到验证码或资源
- `500 Internal Server Error`: 服务器内部错误

### 认证失败原因
1. 认证密钥不存在
2. 认证密钥已过期
3. 使用次数已达上限
4. 认证密钥已被禁用

## 数据库配置

系统使用MySQL 5.7数据库，配置信息：
- 服务器地址: `yangyufan.top:3306`
- 数据库名: `augment`
- 用户名: `root`
- 密码: `Yang199691`
- 字符集: `utf8mb4`
- 连接URL: `jdbc:mysql://yangyufan.top:3306/augment?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true`

### 数据库初始化

1. 首先在MySQL服务器上执行 `database_init.sql` 脚本创建数据库和表结构
2. 或者让Spring Boot自动创建（已配置 `ddl-auto: update`）

## 日志级别

系统配置了详细的日志记录：
- 应用日志级别：DEBUG
- SQL日志：显示执行的SQL语句
- 认证操作：记录所有认证和使用情况

## 安全建议

1. 在生产环境中使用更安全的数据库（如MySQL、PostgreSQL）
2. 定期轮换认证密钥
3. 监控异常使用模式
4. 设置合理的使用次数限制
5. 定期清理过期的日志数据

## 扩展功能

系统设计支持以下扩展：
1. 添加更多的操作类型
2. 实现认证密钥的自动过期清理
3. 添加使用频率限制（如每分钟最大请求数）
4. 实现认证密钥的分组管理
5. 添加邮件通知功能
