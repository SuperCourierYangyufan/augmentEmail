# 临时邮箱-查看邮件内容 任务拆解（分页修订）

- [x] 后端/DTO：新增邮件内容传输对象 EmailContentDto
  - Files: `src/main/java/org/my/augment/service/dto/EmailContentDto.java`（或 controller.dto 包，按项目习惯）
  - Links: requirements.md#数据模型/实体调整, design.md#实现方案
  - Prompt:
    Implement the task for spec view-temp-email-details, first run spec-workflow-guide to get the workflow guide then implement the task:
    Role: Java DTO/建模工程师
    Task: 定义不可持久化的 DTO：`sender`、`sentTime(yyyy-MM-dd HH:mm:ss)`、`content`；提供构造与访问器；不添加持久化注解。
    Restrictions: 不引入 Lombok 之外的新依赖；命名与包结构遵循现有风格。
    _Leverage: 现有时间格式化实践
    _Requirements: 字段与需求一致
    Success: 源码编译通过。

- [ ] 后端/服务：EmailService 增加分页查询 listEmailContents(email,pageIndex,size)
  - Files: `src/main/java/org/my/augment/service/EmailService.java`
  - Links: requirements.md#接口约定, design.md#实现方案
  - Prompt:
    Implement the task for spec view-temp-email-details, first run spec-workflow-guide to get the workflow guide then implement the task:
    Role: 邮件/IMAP 开发工程师
    Task: 在 EmailService 中实现 `Map<String,Object> listEmailContents(String emailAddress,int pageIndex,int size)`：搜索收件人为该地址的邮件，按发送时间倒序排序，分页切片，页内为每封邮件生成 EmailContentDto（sender/sentTime/content）。
    Restrictions: 关闭 Folder；不影响既有方法；排空与边界处理；时间格式 `yyyy-MM-dd HH:mm:ss`。
    _Leverage: ensureConnection(), getPreferredContent(), getTextFromMultipart()
    _Requirements: 返回分页结构字段齐全
    Success: 可被控制器调用并通过编译。

- [ ] 后端/接口：TempEmailController 新增 GET /api/temp-email/emails
  - Files: `src/main/java/org/my/augment/controller/TempEmailController.java`
  - Links: requirements.md#接口约定, design.md#接口定义
  - Prompt:
    Implement the task for spec view-temp-email-details, first run spec-workflow-guide to get the workflow guide then implement the task:
    Role: Spring Boot 后端工程师
    Task: 新增 `/api/temp-email/emails` 接口，入参 `emailAddress/page/size`，将 `page` 转换为 `pageIndex=page-1`，调用 `emailService.listEmailContents` 并返回 `{ success, data(分页), message }`。
    Restrictions: 不新增鉴权逻辑；异常按项目统一风格记录日志并返回 message。
    _Leverage: 现有日志记录与响应结构
    _Requirements: 返回分页字段齐全
    Success: 接口可被前端调用并返回约定分页结构。

- [ ] 前端/HTML：升级“查看邮件”模态（列表+分页）
  - Files: `src/main/resources/static/temp-email-manager.html`
  - Links: requirements.md#目标, design.md#前端设计
  - Prompt:
    Implement the task for spec view-temp-email-details, first run spec-workflow-guide to get the workflow guide then implement the task:
    Role: 原生 HTML/CSS 开发
    Task: 保持卡片“查看邮件”按钮不变；将 `#emailContentModal` 的内容区调整为 `#mailBody`（列表容器）+ `#mailPagination`（分页容器）。
    Restrictions: 不引入第三方框架；保证移动端适配；保持页面风格一致。
    _Leverage: 现有 `.modal` 与 `.pagination` 样式
    _Requirements: 容器 id 清晰、可滚动区域合理
    Success: DOM 结构清晰，为 JS 绑定预留 id。

- [ ] 前端/JS：分页拉取/渲染/刷新邮件列表
  - Files: `src/main/resources/static/temp-email-manager.js`
  - Links: requirements.md#主要场景与验收标准, design.md#交互逻辑
  - Prompt:
    Implement the task for spec view-temp-email-details, first run spec-workflow-guide to get the workflow guide then implement the task:
    Role: 原生 JavaScript 前端工程师
    Task: 实现 `fetchEmailPage(addr,page)`, `renderEmailList(pageData)`, `renderMailPagination()`, `changeMailPage(p)`；调整 `showEmailContent/refreshEmailContent` 使用分页；`#mailBody` 渲染多个邮件条目，每条含发送人/发送时间/完整内容（HTML 需净化）。
    Restrictions: 不引入新依赖；与现有通知/loading 机制一致；避免全局命名冲突。
    _Leverage: `handleUnauthorizedResponse`, `showNotification`, `showError`, `sanitizeHtml` 现有方法
    _Requirements: 刷新不关闭模态，分页交互友好
    Success: 页面不报错，能正常分页查看与刷新邮件内容。

- [ ] 验证与构建：最小验证路径（分页）
  - Files: N/A
  - Links: requirements.md#主要场景与验收标准
  - Prompt:
    Implement the task for spec view-temp-email-details, first run spec-workflow-guide to get the workflow guide then implement the task:
    Role: QA/构建工程
    Task: `mvn clean compile`；登录后在列表中点击“查看邮件”，分页浏览发送人/发送时间/正文；测试页码切换、刷新、401/空态。
    Restrictions: 不写 UI 自动化
    _Leverage: 现有登录与数据
    _Requirements: 性能与可用性
    Success: 编译通过，核心路径手测可用。
