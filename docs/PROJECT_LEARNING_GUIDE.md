# 医疗保险报销系统——项目学习与部署手册

## 1. 项目目标与文档约束

本项目把医疗保险中心的基础资料、待遇参数、审批、就诊处方、费用计算、正式结算、取消报销和综合查询连成一条可运行的业务链。实现时以以下资料为依据：

- `01.项目需求/项目功能结构-医疗保险中心.xls`：决定模块范围和主要业务步骤。
- `01.项目需求/需求规约-医疗保险中心0.3.pdf`：决定报销办理、校验和待遇计算顺序。
- `02.数据库设计/数据库一览表.DOC`：决定原始表名、字段名和原始拼写。
- `02.数据库设计/参数.xls`：提供页面选项及数据字典。
- `02.数据库设计/数据.xls`：提供药品、诊疗项目和服务设施原始目录。

固定规则是：原表名、原字段名、大小写和历史拼写不改；新增账号、安全、日志、工作流和计算明细放入 `ext_` 或 `sys_` 扩展表。`DatabaseDesignContractIntegrationTest` 会自动检查原始表契约。

## 2. 技术架构

```text
浏览器
  ├─ Thymeleaf：首屏 HTML
  ├─ app.css：全部静态样式
  ├─ js/core：公共状态、HTTP/分页工具和页面导航
  ├─ js/modules：认证、目录、资料、参数审批、报销、批量、日志、AI、Dashboard
  └─ app.js：只负责启动应用
             │ HTTPS / JSON / Session Cookie
Spring Boot 4
  ├─ Controller：接口和 HTTP 状态
  ├─ Service：业务校验、权限后的业务流程、事务
  ├─ MyBatis Mapper：SQL 和数据库行锁
  ├─ Spring Security：会话认证和角色授权
  ├─ Audit：请求日志与业务日志
  ├─ Flyway：扩展表版本迁移
  └─ Apache POI：XLS/XLSX 导入、模板、导出
             │
MySQL 8
  ├─ 文档原始业务表（保持不变）
  ├─ ext_* 扩展业务表
  └─ sys_* 账号安全表
```

采用单体分层架构，适合课程项目：部署简单，又能清楚展示页面层、接口层、业务层和数据层的职责。每个业务包通常包含 Controller、Service、Mapper、表单对象和异常类型，可以从任意一个模块按这个顺序阅读。

## 3. 目录阅读顺序

1. `pom.xml`：确认 Maven 依赖和 Java 版本。
2. `src/main/resources/application.yml`：查看数据库、邮箱、HTTPS、AI 和会话配置。
3. `src/main/java/com/medical/insurance/config`：理解权限、会话和统一请求日志。
4. `src/main/java/com/medical/insurance/auth`：学习注册审批、RSA 挑战、BCrypt、锁定和重置密码。
5. `src/main/java/com/medical/insurance/reimbursement`：学习本项目最重要的就诊、处方和结算事务。
6. `src/main/java/com/medical/insurance/bulk`：学习通用 XLS/XLSX 数据交换。
7. `src/main/java/com/medical/insurance/ai`：学习有证据约束的模型调用。
8. `src/main/resources/templates/index.html`、`static/css`、`static/js/core` 与 `static/js/modules`：理解前端分层和前后端交互。
9. `src/test/java`：用自动化测试反向理解业务规则。

## 4. 完整业务流程

### 4.1 基础准备

管理员维护药品、诊疗项目、服务设施、病种、定点机构、参保单位、参保人员和三类待遇计算参数。页面中的类别、人员类别、医院等级等必须来自数据字典，后端再次校验，不能只依赖前端下拉框。

### 4.2 审批

审批员处理人员就诊机构审批和特检特治审批。系统核对人员、机构、项目、有效期、审批状态和项目是否确实需要审批。不需要审批的药品或项目不能误录入特检审批。

### 4.3 就诊登记

报销员可从 Dashboard 选择个人，也可先选择单位再选择该单位人员。门诊登记时系统查询该人员是否有未结算门诊：有则弹窗询问是否沿用；不沿用或不存在时，数据库序列表生成下一个 `MZ########` 号码。号码生成在数据库中完成，可避免两个人同时操作生成重复号码。

### 4.4 处方明细

目录类别、收费项目类别和项目编码是联动关系。先选类别会过滤项目；直接选项目会反向填充类别。后端仍会核对编码实际所属目录，防止绕过页面提交错误类别。目录外手工项目标记为全额自费；总金额由后端重新按单价乘数量计算。

### 4.5 待遇计算

预结算与正式结算共用同一计算服务，顺序如下：

1. 人员、病种、定点机构和机构等级校验。
2. 就诊机构审批和特检特治审批校验。
3. 每条处方应用有效期、最高限价、甲乙丙类和目录外自费规则。
4. 汇总可纳入基本医疗保险的费用。
5. 扣除起付标准。
6. 按数据库中的真实区间逐段计算个人自付与基金支付。
7. 根据个人年度累计应用封顶线。

预结算只返回计算结果，不写年度累计。正式结算通过事务同时写入结算主记录、逐项目结果、费用构成、年度累计、就诊结算标志和日志。对就诊及年度累计使用数据库锁和唯一约束，防止重复点击造成重复结算。

### 4.6 取消与查询

取消报销不删除原记录，而是新增一条关联原结算的负交易，并在同一事务恢复年度累计。系统禁止重复取消。综合查询可以继续查看原结算、负交易、处方计算明细和费用构成，并支持打印。

## 5. 账号、安全和权限

- 密码传输：浏览器获取两分钟、一次性的 RSA-OAEP-256 挑战，提交加密密码，不把明文写进请求正文。
- 密码存储：数据库只保存 BCrypt 哈希，无法反解原密码。
- 注册：新用户固定为报销经办候选账号，管理员审核后才能登录；注册接口不能创建管理员。
- 防爆破：连续登录失败 5 次锁定 15 分钟；重置验证码 10 分钟有效，连续错误 5 次失效，同一账号和邮箱 60 秒内只发送一封。验证码事务提交后再由受控线程池异步发送邮件，避免页面等待SMTP连接。
- 会话版本：修改密码、重置密码或管理员改变账号状态后，旧会话立即失效。
- `ADMIN`：基础资料、参数、账号审核、批量数据、日志和 AI 知识库。
- `APPROVER`：审批及只读业务目录。
- `REIMBURSEMENT`：Dashboard、就诊处方、结算、取消和综合查询。

应用层 RSA 不能替代 HTTPS，因为页面、Cookie 和普通业务数据仍需传输层保护。正式部署必须开启 HTTPS、Secure Cookie，并更换默认密码和密钥。

## 6. 批量导入、导出与删除

系统原生支持 `.xls` 与 `.xlsx`，因此不需要退回 JSON。推荐流程：

1. 管理员选择模块并下载模板。
2. 保留第一行字段名，按行填写数据。
3. 先用 `VALIDATE_ONLY` 检查，不写数据库。
4. 修正错误后使用 `UPSERT`，或需要严格新增时使用 `INSERT_ONLY`。
5. 在任务结果中核对成功、失败数量和逐行错误。

系统还能识别原 `数据.xls` 以及由它另存的 `.xlsx` 中“药品、诊疗项目、服务设施”中文表头。删除必须明确提交主键列表和 `DELETE N`，结算、年度累计、就诊及处方等关键账务数据不允许走通用删除，避免绕过业务事务。

## 7. 日志与留痕

登录、退出、失败登录、账号审批及所有业务请求都会记录账号、角色、方法、路径、结果、状态码、耗时、业务编号、客户端地址和时间。日志页面可筛选并导出 XLSX。密码、验证码、身份证号和手机号等敏感内容不会原样写入日志。

## 8. AI 系统与政策问答

localhost 能直接调用 DeepSeek API，不需要为了 API 调用而先上线。安全流程如下：

1. 程序启动时把项目完整使用指南写入 `SYSTEM_DOCUMENT`，覆盖系统页面、角色、配置、审批、报销、导入和日志等功能。
2. 后台定时任务从国家医疗保障局“政策文件”和“政策解读”栏目发现最新正文并增量同步；政策问题本地证据不足时，再使用国家医保局公开站内搜索按问题抓取相关正文。
3. 外部同步只接受 `https://www.nhsa.gov.cn`，拒绝自定义端口、重定向和超大文件；问题中的身份证号、手机号等内容不会拼入官网搜索词。
4. 用户问题先经过系统问题与政策问题分流，再过滤无关证据。地方门诊报销流程缺少参保地时先追问省市和医保类型。
5. 提示词要求模型只能使用提供的证据，系统操作引用系统指南，政策结论引用官方政策并标注 `[资料N]`；未引用资料的回答不展示。

启用变量：

```text
AI_ENABLED=true
DEEPSEEK_API_KEY=<自己的 API Key>
DEEPSEEK_BASE_URL=https://api.deepseek.com
DEEPSEEK_MODEL=deepseek-v4-flash
AI_AUTO_SYNC_ENABLED=true
```

系统启动约 15 秒后自动同步，之后默认每 24 小时执行一次；管理员可从右下角浮动聊天窗查看状态并立即同步。模型不能绕过证据自行上网，也不能用训练记忆补写政策。系统指南全部开放给问答；数据库密码、API Key、密码哈希和越权业务数据不会进入提示词，AI 也不会直接执行新增、删除、审批或结算。

## 9. 本地运行与 HTTPS

### 9.1 IDEA / HTTP

用 IDEA 打开包含 `pom.xml` 的目录，等待 Maven 同步，运行 `MedicalInsuranceApplication`，访问 `http://127.0.0.1:8080/`。如果提示 8080 已被占用，说明已有一个实例正在运行：停止旧实例，不要重复启动；或者设置 `SERVER_PORT=8081`。

### 9.2 localhost HTTPS

执行：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\create-local-https.ps1
```

运行配置加入：

```text
SSL_ENABLED=true
SERVER_PORT=8443
SSL_KEY_STORE=C:\绝对路径\medical-insurance-system\config\localhost.p12
SSL_KEY_STORE_PASSWORD=changeit-local-only
SESSION_COOKIE_SECURE=true
```

访问 `https://localhost:8443/`。自签名证书只是本机开发证书；浏览器首次提示不受信任属于正常现象。公开上线时用域名、反向代理和可信 CA 证书，并把 `SERVER_ADDRESS` 改成部署所需监听地址。

### 9.3 上线前必须覆盖的变量

```text
DB_USERNAME
DB_PASSWORD
INITIAL_ADMIN_PASSWORD
INITIAL_ROLE_PASSWORD
PASSWORD_RESET_SECRET
EXPOSE_RESET_CODE=false
RESET_MAIL_ENABLED=true
SMTP_HOST / SMTP_PORT / SMTP_USERNAME / SMTP_PASSWORD
SSL_ENABLED=true
SESSION_COOKIE_SECURE=true
DEEPSEEK_API_KEY（启用 AI 时）
```

## 10. Maven 验证

在项目目录执行：

```powershell
mvn test
mvn package
```

当前全量结果为 16 个测试类、26 个测试，0 失败、0 错误、0 跳过。自动化覆盖原表契约、源工作簿读取、认证安全、角色权限、基础资料、审批、参数区间、门诊号、处方联动、待遇计算边界、重复结算、取消冲正、Dashboard、批量 XLS/XLSX、日志、AI 系统指南检索和无证据拒答。

## 11. 学习成果和功能亮点

- 学会用 Maven 管理 Spring Web、Thymeleaf、MyBatis、MySQL Driver 等依赖。
- 学会在不破坏历史数据库的条件下，用扩展表和 Flyway 演进系统。
- 学会把前端校验与后端权威校验结合，解决类别联动和越权提交问题。
- 学会用事务、行锁、唯一约束和负交易保证账务一致性。
- 学会实现 RSA 传输、BCrypt 存储、登录锁定、角色权限和会话失效。
- 学会兼容 XLS/XLSX、校验预览、逐行错误和安全批量删除。
- 学会把静态 HTML、CSS、JavaScript 拆分，并启用响应压缩。
- 学会构建可解释的 Dashboard，以及区分系统资料与官方政策证据的 AI 问答。
- 学会用集成测试验证真实 MySQL 表结构和完整业务边界，而不只测试页面是否能打开。

## 12. 组内分工建议

- 初学者 A：阅读页面和基础资料 CRUD，负责录入与浏览器验收。
- 初学者 B：阅读数据字典、批量导入和 POI，负责准备测试数据。
- 后端成员：重点阅读审批、SettlementService、事务与并发测试。
- 安全成员：重点阅读 auth、SecurityConfig、SessionAuthenticationFilter 和审计日志。
- 文档成员：根据 `TEST_RECORD.md` 的验收路径保存截图，最终再补充课程报告，不直接复制代码注释代替设计说明。
