# 临时邮箱-查看邮件内容 技术设计（分页修订）

## 总览
实现“查看邮件内容”能力，仅展示最新一封邮件的三项核心内容：发送人、发送时间、完整邮件内容；不新增鉴权，沿用既有全局机制；UI 风格与现有页面一致并支持刷新。

## 后端设计
### 接口定义
- Method: GET
- Path: `/api/temp-email/emails`
- Query: `emailAddress`（必填）、`page`（默认1）、`size`（默认5）
- Auth: 依赖既有全局鉴权/会话拦截，不在接口内重复实现。
- Status：
  - 200：成功，`{ success: true, data: Page<EmailContentDto>, message }`
  - 4xx/5xx：异常按现有风格返回

### 返回结构
```
{
  success: true,
  data: {
    content: [ { sender, sentTime, content }, ... ],
    totalElements, totalPages, number, size, first, last
  },
  message: '查询成功'
}
```

### 实现方案
- 在 `EmailService` 中新增分页方法：`Map<String, Object> listEmailContents(String emailAddress, int pageIndex, int size)`：
  - 复用 IMAP 连接与 `ensureConnection()`；
  - 打开 INBOX，使用 `RecipientStringTerm(Message.RecipientType.TO, emailAddress)` 查询；
  - 将返回的 `Message[]` 根据 `getSentDate()` 进行降序排序（最新在前）；
  - 依据 `pageIndex/size` 进行内存分页，截取当前页消息；
  - 对页内每封邮件抽取 `sender/sentTime/content`（使用 `getPreferredContent` 优先 HTML，回退纯文本）；
  - 组装分页结构：`content/totalElements/totalPages/number/size/first/last`；
  - 关闭 Folder。
- DTO 复用：`EmailContentDto { sender, sentTime, content }`。
- 在 `TempEmailController` 新增 GET `/emails` 映射，接受 `emailAddress/page/size`，内部转为 `pageIndex=page-1` 调用服务。

### 错误与安全
- 不返回 `authKey` 等敏感信息。
- HTML 渲染交由前端进行基本安全处理（移除脚本或采用安全容器），本接口不修改 HTML 内容。

## 前端设计
### 入口按钮
- 在每个邮箱卡片动作区新增按钮：`查看邮件`，点击触发 `showEmailContent(email.emailAddress)`。

### 模态框结构
- 复用现有 `.modal` 风格，新增：
  - `#emailContentModal` 容器
  - 标题区：展示“邮件内容”，提供“刷新”与“关闭”按钮
  - 内容区：
    - `#mailBody`：分页列表容器（每条: 发送人/发送时间/完整内容）
    - `#mailPagination`：分页控件容器

### 交互逻辑
- `showEmailContent(addr)`：打开模态 -> loading -> `fetch('/api/temp-email/emails?emailAddress=...&page=1&size=...')` -> `renderEmailList(pageData)`。
- `refreshEmailContent()`：基于当前邮箱地址与当前页再次请求；
- `changeMailPage(p)`：切换页码，重新拉取并渲染；
- `hideEmailContentModal()`：关闭模态；
- 401/会话失效：沿用 `handleUnauthorizedResponse`。

### 样式与可用性
- 继承现有圆角、阴影与配色；
- `#mailBody` 区域设置可滚动且适配移动端；
- HTML 渲染使用安全容器，避免脚本执行；
- 分页控件沿用页面现有 `.pagination` 样式，保持一致。

## 可扩展点
- 后续可支持分页浏览历史邮件、附件名展示与下载、邮件搜索等。

## 兼容性与回退
- 未找到邮件：模态内显示空态提示与刷新按钮，不影响主列表操作；
- 接口异常：toast 通知 + 模态保留以便重试。
