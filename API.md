# 接口说明

所有业务接口都要求先建立会话。无会话返回 401，无角色权限返回 403，业务校验失败返回 400。密码类请求先调用 `GET /api/auth/encryption-challenge`，使用返回的 RSA-OAEP-256 公钥加密 `challengeId + 换行 + 密码内容`，再提交 `challengeId` 和 Base64 格式的 `encryptedPassword`；运行环境默认拒绝明文密码。

## 登录与基础资料

- `GET /api/auth/encryption-challenge`：取得两分钟有效、只能使用一次的密码加密挑战。
- `POST /api/auth/register`：注册普通报销经办账号，参数为账号、姓名、邮箱和加密密码。
- `POST /api/auth/login`、`GET /api/auth/me`、`POST /api/auth/logout`。
- `POST /api/auth/password-reset/request`：账号与邮箱匹配时生成十分钟重置验证码。
- `POST /api/auth/password-reset/confirm`：验证码与加密的新密码重置账号密码。
- `POST /api/auth/change-password`：登录后修改密码；密文内容为 `当前密码 + NUL + 新密码`，成功后会话失效。
- `GET /api/admin/accounts?status=PENDING`：管理员查询待审核账号。
- `POST /api/admin/accounts/{userId}/approve`：管理员批准账号并指定 `APPROVER` 或 `REIMBURSEMENT` 角色。
- `POST /api/admin/accounts/{userId}/reject`：管理员拒绝账号。
- `/api/medicines`：药品 CRUD
- `/api/diagnoses`：诊疗项目 CRUD
- `/api/facilities`：服务设施 CRUD
- `/api/diseases`：病种 CRUD
- `/api/institutions`：定点医疗机构 CRUD
- `/api/companies`：单位 CRUD
- `/api/people`：个人 CRUD
- `GET /api/dictionaries?category=...`：参数字典

## 医疗待遇参数

- `/api/treatment-parameters/capping-lines`：封顶线 CRUD
- `/api/treatment-parameters/minimum-payment-standards`：起付标准 CRUD
- `/api/treatment-parameters/segment-ratios`：分段比例 CRUD

## 审批

- `/api/approvals/institutions`：人员就诊机构审批 CRUD
- `/api/approvals/special`：特检特治审批 CRUD

## 就诊与处方

- `GET /api/reimbursements/dashboard/summary?year=`：Dashboard 汇总，包括参保人员、单位、待结算就诊、年度正式结算次数和年度基金支付。
- `GET/POST /api/reimbursements/visits`
- `GET /api/reimbursements/visits/outpatient-candidate?personId=`：查询可沿用的未结算门诊记录。
- `GET/PUT/DELETE /api/reimbursements/visits/{住院号}`
- `GET/POST/PUT/DELETE /api/reimbursements/visits/{住院号}/prescriptions`

## 结算、取消与查询

- `POST /api/reimbursements/visits/{住院号}/preview`：预结算，不写累计。
- `POST /api/reimbursements/visits/{住院号}/settle`：正式结算。
- `GET /api/reimbursements/settlements?keyword=&year=`：综合查询。
- `GET /api/reimbursements/settlements/{结算ID}`：结算、就诊、处方计算明细和年度累计。
- `POST /api/reimbursements/settlements/{结算ID}/cancel`：取消，JSON 为 `{ "reason": "原因" }`。

## 批量数据与日志

- `GET /api/bulk/modules`：可用模块及其导入、删除权限。
- `GET /api/bulk/{module}/template.xlsx`：下载空白 XLSX 模板。
- `GET /api/bulk/{module}/export.xlsx`：导出模块全部数据。
- `POST /api/bulk/{module}/import?mode=VALIDATE_ONLY|UPSERT|INSERT_ONLY`：上传 `.xls` 或 `.xlsx`。
- `DELETE /api/bulk/{module}`：按主键删除，JSON 中包含 `keys` 和精确确认文本 `DELETE N`。
- `GET /api/audit-logs`：按账号、操作、结果和时间筛选统一审计日志。
- `GET /api/audit-logs/export.xlsx`：按相同条件导出日志。

## AI 系统与政策问答

- `POST /api/ai/chat`：系统操作问题检索内置完整使用指南，政策问题检索国家医保局官方资料；回答必须包含 `[资料N]` 引用，来源类型为 `SYSTEM_DOCUMENT` 或 `OFFICIAL_WEB`。
- `GET /api/ai/knowledge/documents`：管理员查询知识文档。
- `POST /api/ai/knowledge/fetch`：管理员导入 HTTPS 政府官网页面，JSON 为 `{ "url": "https://...gov.cn/..." }`。
- `GET /api/ai/knowledge/sync-status`：管理员查看国家医保局自动同步状态及知识文档数量。
- `POST /api/ai/knowledge/sync`：管理员立即从配置的国家医保局政策栏目执行一次增量同步。
- `DELETE /api/ai/knowledge/documents/{id}`：管理员删除文档及其分块。

批量管理、日志、账号审核和 AI 知识库接口仅限 `ADMIN`；AI 提问允许所有已登录角色。

## 角色

- `ADMIN`：基础资料与参数管理，同时作为超级管理员保留全部权限。
- `APPROVER`：审批接口。
- `REIMBURSEMENT`：Dashboard、就诊、处方、结算、取消和综合查询接口。
- 审批员和报销员可以读取目录、人员、单位和机构资料，用于业务选择，但不能修改这些基础资料。
