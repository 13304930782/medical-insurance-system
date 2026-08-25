# 医疗保险报销系统

系统已实现报销办理 Dashboard、基础资料、待遇参数、待遇审批、就诊与处方、预结算、正式结算、取消报销、综合查询和打印，并补充账号审批、批量数据、统一审计日志、官方资料约束的 AI 问答和本地 HTTPS。原始15张表及字段严格保留《数据库一览表.DOC》的名称和拼写；新增业务内容只使用 `ext_` 扩展表。

## 技术栈

- Maven
- Java 17（兼容更高版本 JDK）
- Spring Boot 4.0.8、Spring Web、Spring Security、Spring Mail
- Thymeleaf
- MyBatis Framework 4.0
- MySQL Driver / MySQL 8
- Apache POI、Flyway

## 代码目录（分层结构）

项目采用便于课堂讲解和小组协作的 MVC 分层目录：

```text
src/main/java/com/medical/insurance
├─ controller/       Web 页面和 REST 接口
├─ dao/              MyBatis Mapper 数据访问接口
├─ model/            表单、请求对象和业务模型
├─ service/          服务层公共组件
│  └─ impl/          具体业务服务实现
├─ exception/        业务异常
├─ config/           安全、异步、启动初始化配置
└─ util/             通用工具

src/main/resources
├─ mapper/           MyBatis XML（适合展示动态 SQL）
├─ templates/
│  └─ fragments/     登录、菜单和 AI 助手等 Thymeleaf 公共片段
├─ static/css/       页面样式
└─ static/js/
   ├─ core/          全局状态、HTTP/分页工具和页面导航
   ├─ modules/       按认证、目录、资料、审批、报销等业务拆分的脚本
   └─ app.js         应用启动入口（不再承载业务代码）
```

Controller 只处理参数和 HTTP 响应，业务校验及事务放在 Service，数据库读写统一通过 DAO。前端同样按层拆分：`core` 提供公共能力，`modules` 按业务组织，`app.js` 最后启动应用。新模块推荐使用 Mapper XML；已有复杂结算 SQL仍可保留 MyBatis 注解，两种写法由同一个 MyBatis 框架管理。

## IDEA运行

1. 用 IDEA 打开本目录（包含 `pom.xml` 的 `medical-insurance-system` 目录）。
2. 等待 Maven 同步完成。
3. 打开 `MedicalInsuranceApplication` 运行配置，按需设置：
   - `DB_USERNAME=root`
   - `DB_PASSWORD=123456`
   - `DATA_IMPORT_ENABLED=false`
4. 运行 `MedicalInsuranceApplication`。
5. 浏览器访问 `http://127.0.0.1:8080/`。

项目默认只监听本机 `127.0.0.1`，本机访问不需要放开 Windows 防火墙端口。

## 初始账号

- `admin / 123456`，找回邮箱 `admin@medical.local`：超级管理员。
- `approver / 123456`，找回邮箱 `approver@medical.local`：待遇审批员。
- `reimbursement / 123456`，找回邮箱 `reimbursement@medical.local`：报销经办员。

可分别通过 `INITIAL_ADMIN_PASSWORD` 和 `INITIAL_ROLE_PASSWORD` 修改首次初始化密码。已有账号不会被启动程序覆盖。

新注册账号需要管理员在“系统管理 → 账号审核与操作日志”中审核后才能登录。

## 账号安全

- 登录、注册、重置密码和修改密码均先获取一次性 RSA-OAEP-256 挑战，浏览器请求中不发送明文密码；挑战两分钟过期且只能使用一次。
- 密码只以 BCrypt 哈希保存在 `sys_user.password_hash`，注册密码须为8至64位可见英文字符，并同时包含大小写字母、数字和特殊字符。
- 新注册账号固定为 `REIMBURSEMENT`，不能通过注册接口创建管理员或审批员。
- 连续登录失败5次会临时锁定15分钟；找回密码验证码10分钟有效，连续错误5次后失效，同一账号和邮箱60秒内只发送一封。验证码记录提交后由后台线程发送邮件，页面无需等待SMTP投递完成。
- 本地演示默认通过页面显示重置验证码。部署时设置 `EXPOSE_RESET_CODE=false`，接入邮件发送，并把 `PASSWORD_RESET_SECRET` 设置为独立随机密钥。
- RSA应用层加密用于避免密码出现在请求正文中；正式网络部署仍必须使用 HTTPS，并设置 `SESSION_COOKIE_SECURE=true`，以保护页面、会话Cookie和全部业务数据。

## 已实现业务

- 报销办理 Dashboard：支持“个人报销”和“单位报销”两个入口。个人入口按编号、姓名、证件号或社保卡号选择人员；单位入口先选择参保单位，再选择该单位人员。
- Dashboard 使用饼图显示不同单位类型的数量与占比。
- Dashboard 内可连续完成就诊登记、处方录入、预结算、正式结算、年度累计查看和结算单打印，并显示五步办理进度。
- 门诊登记会提示是否沿用该人员未结算的历史门诊号；选择新建时由数据库并发安全地生成递增门诊号，用户无需手输。
- Dashboard 展示参保人数、单位数、待结算就诊、选定年度正式结算次数和基金支付金额。
- 药品、诊疗项目、服务设施、病种、定点机构、单位、个人维护。
- 封顶线、起付标准、个人分段报销比例维护和区间冲突校验。
- 人员就诊机构审批、药品及诊疗项目特检特治审批。
- 就诊资料及多条处方维护；目录外手工项目自动标记全额自费；金额由后端按单价乘数量重算。
- 计算顺序：资格与审批、项目最高限价、甲乙丙类、医院等级、起付标准、实际区间分段比例、年度封顶线。
- 预结算不落库、不更新年度累计。
- 正式结算在一个事务内写入结算、项目计算明细、费用构成、年度累计、就诊标志和操作日志。
- 取消报销保留原记录，生成负交易并恢复年度累计，禁止重复取消。
- 按个人ID、姓名、住院号、结算号和年度综合查询，并可打印费用清单。
- 管理员可下载 XLSX 模板、导入 XLS/XLSX、导出 XLSX，并通过精确确认短语安全删除选定数据；导入任务和逐行错误可追踪。
- 登录及所有业务请求统一留痕，管理员可筛选查看并导出 XLSX。
- 右下角浮动 AI 同时回答本系统页面、角色、配置和完整业务流程，并从国家医疗保障局自动同步或按问题检索政策；系统资料与官方政策分开标识，无相关证据时不展示无关来源。

## 数据库脚本

- `../数据库/01_create_database.sql`：完整建库建表。
- `../数据库/02_add_medical_institution_profile.sql`：定点机构扩展资料。
- `../数据库/03_add_workflow_extensions.sql`：审批、处方来源、结算及明细扩展表。
- `../数据库/04_seed_bulk_test_data.sql`：生成带 `TST-` 标识的批量联调数据，可重复执行。
- `../数据库/05_clear_bulk_test_data.sql`：只清除上述 `TST-` 测试数据，不处理 Excel 原始数据和手工业务数据。
- `../数据库/10_add_authentication_extensions.sql`：注册邮箱、登录锁定和密码重置验证码扩展表。

程序启动时也会用 `CREATE TABLE IF NOT EXISTS` 检查工作流扩展表，不修改任何原始表字段。

批量测试数据包含 300 个单位、5000 名人员、40 家医院、120 个病种、880 个测试目录项目、3000 次就诊、9000 条处方及 500 笔历史正式结算。人员覆盖 40 个国籍和源数据字典中的全部 7 种身份证件类型，证件号均为虚构测试编号。

在 Navicat 中需要重建或清空测试数据时，打开对应 SQL 文件后选择 `medical_insurance_reimbursement` 数据库并“运行全部”即可。清空脚本允许重复执行。

## 文档

- [接口说明](API.md)
- [测试记录](TEST_RECORD.md)
- [项目学习与部署手册](docs/PROJECT_LEARNING_GUIDE.md)

## 批量数据

管理员进入“批量数据”页面后先下载当前模块模板。模板第一行就是数据库实际字段名，可直接保存为 `.xlsx` 后导入；旧版 `.xls` 也支持。导入模式包括仅校验、按主键新增或更新、仅新增。结算与年度累计属于账务数据，只允许导出，不开放通用导入和删除。

## 本地 HTTPS

localhost 可以使用 HTTPS。先执行：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\create-local-https.ps1
```

再设置 `SSL_ENABLED=true`、`SERVER_PORT=8443`、`SSL_KEY_STORE=<项目绝对路径>\config\localhost.p12`、`SSL_KEY_STORE_PASSWORD=changeit-local-only`、`SESSION_COOKIE_SECURE=true` 后启动，访问 `https://localhost:8443/`。自签名证书会触发浏览器信任提示，仅适合本机开发；正式上线请换成可信 CA 证书。

## DeepSeek 问答

localhost 可以直接调用 DeepSeek API，无需先把网站上线。在 IDEA 的 `MedicalInsuranceApplication` 运行配置中设置以下环境变量后重启：

```text
AI_ENABLED=true
DEEPSEEK_API_KEY=<自己的 DeepSeek API Key>
DEEPSEEK_MODEL=deepseek-v4-flash
AI_AUTO_SYNC_ENABLED=true
```

登录后点击右下角黑色问答图标即可使用。程序启动时会把完整系统使用指南自动更新到本地知识库，因此可以直接询问菜单操作、角色权限、导入、审批、预结算、正式结算、取消和日志等系统问题。政策问题会先查本地官方证据，不足时再调用国家医保局站内搜索并增量抓取相关正文；后台仍按每日计划同步政策栏目。外部抓取只允许 `https://www.nhsa.gov.cn`，不跟随重定向，不接受自定义端口。系统资料与官方政策分别标识，找不到相关证据时不会展示无关链接。

## Excel重新导入

仅在需要重新导入时把 `DATA_IMPORT_ENABLED` 设为 `true`。导入采用主键更新，可重复执行且不会修改源 Excel；完成后恢复为 `false`。
