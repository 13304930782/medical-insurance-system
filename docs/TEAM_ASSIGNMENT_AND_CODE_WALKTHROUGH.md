# 医疗保险报销系统小组分工与代码讲解手册

> 本文是老师深入追问时使用的详细备查资料。正常课程答辩优先使用 `TEAM_BASIC_PRESENTATION_GUIDE.md`，只讲基础增删改查、对应文件和页面演示；复杂校验、事务与测试可说明为在 AI 辅助下完成并经过运行验证。

## 1. 分组结论

当前分组可以直接使用：


| 成员  | 负责功能                              | 负责重点                                      |
| --- | --------------------------------- | ----------------------------------------- |
| A   | 1.1 药品、1.2 诊疗项目、1.3 服务设施          | 三类医保目录 CRUD、有效期/类别/审批标志校验、批量导入导出          |
| B   | 1.4 病种、1.5 定点医疗机构、1.6 待遇参数        | 病种和机构 CRUD、原表与扩展表协同、封顶线/起付线/分段比例及区间校验     |
| C   | 2.1 机构审批、2.2 特检特治审批、5.1 单位、5.2 个人 | 单位与人员 CRUD、审批有效期、人员/单位/机构/目录项目关联校验        |
| D   | 3.1 中心报销、3.2 取消报销、4.1 费用查询        | 就诊和处方、预结算、正式结算、负交易冲正、年度累计、查询打印与 Dashboard |


这不是把文件平均分成四份，而是按业务依赖分组。A 的三个模块结构相似，因此增加通用批量数据能力；B 负责计算前置参数；C 负责参保主体和审批；D 负责把前三人的数据串成完整报销流程。

## 2. 全组统一讲解的分层关系

项目采用 Spring Boot 单体分层架构。任何一个 CRUD 都按同一条链路运行：

```text
index.html 页面表格/表单
        ↓ 用户点击，modules/*.js 组装 JSON
Controller（接收 HTTP 请求、返回统一 JSON）
        ↓ 调用
Service（业务校验、事务、操作日志）
        ↓ 调用
Mapper/DAO（MyBatis SQL）
        ↓
MySQL 原始表或 ext_ 扩展表
        ↑
Form/Model（前后端字段的数据载体）
```

答辩时可以用“新增药品”举例：

1. `index.html` 的 `medicineForm` 收集字段。
2. `modules/catalog.js` 监听表单提交，向 `POST /api/medicines` 发送 JSON。
3. `MedicineController` 接收 `MedicineForm`，调用 `MedicineService.create`。
4. `MedicineService` 校验编码、名称、默认状态和审批标志，并开启事务。
5. `MedicineMapper` 执行 `INSERT INTO t_medicine`。
6. `SystemMapper` 写入“谁在什么时间新增了什么药品”的操作日志。
7. Controller 返回成功，前端关闭弹窗并刷新列表。

所有成员都应能讲清这条链路，之后再换成自己模块的类名和数据表名。

## 3. A：药品、诊疗项目、服务设施与批量数据



### 3.1 A 负责的后端文件



#### 药品信息维护

- `controller/MedicineController.java`：`/api/medicines` 的查询、详情、新增、修改、删除接口。
- `service/impl/MedicineService.java`：分页、编码重复校验、默认值、有效期格式、特检标志与审批标志联动、操作日志。
- `dao/MedicineMapper.java`：对原表 `t_medicine` 执行 SQL。
- `model/MedicineForm.java`：药品表单字段载体。
- `exception/MedicineBusinessException.java`：药品业务错误。



#### 诊疗项目信息维护

- `controller/DiagnosisController.java`
- `service/impl/DiagnosisService.java`
- `dao/DiagnosisMapper.java`
- `model/DiagnosisForm.java`
- `exception/DiagnosisBusinessException.java`
- 对应原表：`t_diagnosis_project`。



#### 服务设施项目维护

- `controller/FacilityController.java`
- `service/impl/FacilityService.java`
- `dao/FacilityMapper.java`
- `model/FacilityForm.java`
- `exception/FacilityBusinessException.java`
- 对应原表：`t_service_facilities`。



#### 批量导入导出公共能力（A 作为主要讲解人）

- `controller/BulkDataController.java`：模板下载、XLS/XLSX 导入、导出、批量删除入口。
- `service/impl/BulkDataService.java`：读取 Excel、匹配表头、校验、事务写入、生成结果文件。
- `service/BulkModuleRegistry.java`：登记允许批量处理的模块、表名和是否允许删除。
- `util/ExcelCellValues.java`：统一读取 Excel 单元格值。
- `model/BulkDeleteRequest.java`
- `exception/BulkBusinessException.java`
- `test/.../bulk/BulkDataIntegrationTest.java`

其他成员仍负责确认自己业务表的模板字段和校验规则；A 负责讲通用批量机制，不代表 A 独自承担所有业务数据正确性。

### 3.2 A 负责的前端区域

- `templates/index.html`
  - `medicinePage`、`medicineForm`
  - `diagnosisPage`、`diagnosisForm`
  - `facilityPage`、`facilityForm`
  - `bulkPage`
- `static/js/modules/catalog.js`
  - `loadMedicines`、`editMedicine`、`deleteMedicine` 及 `medicineForm` 提交事件。
  - `loadDiagnoses`、`editDiagnosis`、`deleteDiagnosis` 及 `diagnosisForm` 提交事件。
  - `loadFacilities`、`editFacility`、`deleteFacility` 及 `facilityForm` 提交事件。
  - `loadBulkModules`、模板下载、导入、导出和批量删除事件。
- `static/css/app.css` 中上述页面共用样式，只在确有需要时修改。



### 3.3 A 的设计逻辑

三类目录使用相同 CRUD 模式，但字段和规则不同：

- 药品：维护收费类别、甲乙丙等级、最高限价、医院等级、有效期、审批标志、特检标志。
- 诊疗项目：维护收费类别、甲乙丙等级、最高限价、医院等级和审批标志；特殊检查/治疗类别自动要求审批。
- 服务设施：维护设施编码、名称、收费类别和有效期，不能出现药品类别与设施目录错误联动。
- 后端是最终校验者；即使绕过前端，也必须拒绝重复主键和错误数据。
- 每次新增、修改、删除都通过 `SystemMapper` 写业务操作日志。



### 3.4 A 的答辩演示顺序

1. 任选一个药品关键字查询，说明分页查询。
2. 新增一条药品，演示特检标志与审批标志联动。
3. 修改最高限价或有效期，再删除测试数据。
4. 切换到诊疗和设施页，说明三类目录复用同一分层结构，但对应不同原表。
5. 下载 Excel 模板，说明 `.xls`、`.xlsx`、仅校验、UPSERT 和安全批量删除。

一句话总结：A 负责“报销时每条费用从哪里来，以及目录项目是否合法、有效、属于哪一类”。

## 4. B：病种、定点医疗机构与待遇计算参数



### 4.1 B 负责的后端文件



#### 病种信息维护

- `controller/DiseaseController.java`
- `service/impl/DiseaseService.java`
- `dao/DiseaseMapper.java`
- `model/DiseaseForm.java`
- `exception/DiseaseBusinessException.java`
- 对应原表：`t_disease_`，末尾下划线必须保留。



#### 定点医疗机构维护

- `controller/InstitutionController.java`
- `service/impl/InstitutionService.java`
- `dao/InstitutionMapper.java`
- `model/InstitutionForm.java`
- `exception/InstitutionBusinessException.java`
- 原表：`t_medical_insititution`，历史拼写 `insititution` 不得改动。
- 扩展表：`ext_medical_institution_profile`，保存机构类别、联系方式、地址等原表没有的字段。



#### 医疗待遇计算参数

- `controller/TreatmentParameterController.java`：三组 REST 接口。
- `service/impl/TreatmentParameterService.java`：字典校验、非负校验、比例校验、分段区间冲突校验和日志。
- `dao/TreatmentParameterMapper.java`
- `model/CappingLineForm.java`
- `model/MinimumPaymentStandardForm.java`
- `model/SegmentRatioForm.java`
- `exception/TreatmentParameterBusinessException.java`
- 三张原表：
  - `t_capping_line`
  - `t_minimum_payment_standard`
  - `t_individual_segement_self_funded_ratio`



#### B 需要理解的数据字典文件

- `controller/DictionaryController.java`
- `dao/DictionaryMapper.java`
- `resources/mapper/DictionaryMapper.xml`

待遇参数中的医疗类别、医疗人员类别、医院等级不能随便输入，`TreatmentParameterService` 会调用 `DictionaryMapper.contains` 做后端校验。

### 4.2 B 负责的前端区域

- `templates/index.html`
  - `diseasePage`、`diseaseForm`
  - `institutionPage`、`institutionForm`
  - `treatmentPage`
  - `cappingForm`、`minimumForm`、`segmentForm`
- `static/js/modules/master-data.js`
  - 病种的 `loadDiseases`、编辑、删除、提交。
  - 机构的 `loadInstitutions`、编辑、删除、提交。
- `static/js/modules/treatment-approval.js`
  - 参数的 `loadTreatmentParameters`、三个页签、三个表单及删除逻辑。
- `static/js/core/navigation.js`
  - `loadDictionaryOptions` 中与病种、机构和待遇参数相关的字典。



### 4.3 B 的设计逻辑

- 病种为就诊登记提供疾病编码和名称，报销时必须验证病种存在。
- 医疗机构采用“原表 + 扩展表”：事务内同时维护两张表，既不修改文档原表结构，又能补充业务字段。
- 封顶线按医疗人员类别维护。
- 起付标准按“医疗类别 + 人员类别 + 医院等级”唯一确定。
- 分段比例按同一组合维护多个金额区间；下限必须小于上限，比例在数据库保存为 `0～1`，页面显示为百分比。
- `overlappingSegments` 防止同一条件下金额区间交叉，否则报销计算会产生重复或空档。



### 4.4 B 的答辩演示顺序

1. 新增病种，说明表名 `t_disease_` 来自原始文档且不能改。
2. 新增机构，说明原表只保存文档字段，扩展表补充详细资料，两张表在同一事务写入。
3. 展示三类待遇参数页签。
4. 故意录入上限小于下限或重叠区间，展示后端拒绝。
5. 说明这些参数会被 D 的 `SettlementService` 实时读取，不是写死在 Java 中。

一句话总结：B 负责“患者在哪里看什么病，以及报销计算使用哪套起付线、比例和封顶线”。

## 5. C：单位、个人与两类审批



### 5.1 C 负责的后端文件



#### 单位基本信息维护

- `controller/CompanyController.java`
- `service/impl/CompanyService.java`
- `dao/CompanyMapper.java`
- `resources/mapper/CompanyMapper.xml`：本项目中单位 CRUD 的 SQL 主要放在 XML Mapper 中。
- `model/CompanyForm.java`
- `exception/CompanyBusinessException.java`
- 对应原表：`company`。



#### 个人基本信息维护

- `controller/PersonController.java`
- `service/impl/PersonService.java`
- `dao/PersonMapper.java`
- `model/PersonForm.java`
- `exception/PersonBusinessException.java`
- 对应原表：`people`。



#### 人员就诊机构审批与特检特治审批

- `controller/ApprovalController.java`：一个 Controller 提供两组审批接口。
- `service/impl/ApprovalService.java`：人员、机构、目录项目、有效期和审批状态校验。
- `dao/ApprovalMapper.java`
- `model/InstitutionApprovalForm.java`
- `model/SpecialApprovalForm.java`
- `exception/ApprovalBusinessException.java`
- 原表：
  - `t_application_info`：人员就诊机构审批。
  - `t_special_approval`：特检特治审批，原字段 `drug_Code` 保持不变。
- 扩展表：`ext_special_approval_item`，用于在不修改原表的前提下支持诊疗项目审批。



### 5.2 C 负责的前端区域

- `templates/index.html`
  - `companyPage`、`companyForm`
  - `personPage`、`personForm`
  - `institutionApprovalPage`、`institutionApprovalForm`
  - `specialApprovalPage`、`specialApprovalForm`
- `static/js/modules/master-data.js`
  - `loadCompanies`、单位编辑/删除/提交。
  - `loadPeople`、个人编辑/删除/提交。
- `static/js/modules/treatment-approval.js`
  - `loadInstitutionApprovals` 及机构审批表单事件。
  - `loadSpecialApprovals`、`loadSpecialProjectOptions` 及特检特治表单事件。
- `static/js/core/navigation.js`
  - `loadReferenceOptions` 中单位、个人和机构候选项。



### 5.3 C 的设计逻辑

- 个人记录通过 `company_id` 关联单位；保存个人时先验证单位存在。
- 身份证件号和社保卡号做唯一校验；未填写社保卡号时后端自动生成。
- 个人可关联登记定点机构，保存时验证机构存在。
- 机构审批先验证人员与机构均存在，并校验结束日期不能早于开始日期。
- 特检特治审批先选项目类别，再只查询“确实需要审批”的药品或诊疗项目。
- 选择诊疗项目时，原表的 `drug_Code` 不乱改，而是在 `ext_special_approval_item` 保存统一的项目类型和项目编码。
- D 的结算服务会按人员、项目、就诊日期查询是否存在“有效且通过”的审批。



### 5.4 C 的答辩演示顺序

1. 先新增单位，再新增个人，说明个人依赖单位和定点机构。
2. 尝试录入重复证件号，展示唯一校验。
3. 给该人员新增就诊机构审批，展示人员、单位和机构概要。
4. 新增特检特治审批，切换“药品/诊疗项目”，展示候选项目自动过滤。
5. 说明审批记录本身是 CRUD，但后续是否允许报销还要同时满足状态、日期和项目匹配。

一句话总结：C 负责“谁参保、属于哪个单位，以及此人是否获得了跨机构或特殊项目的报销资格”。

## 6. D：中心报销、取消报销、费用查询与 Dashboard



### 6.1 D 负责的后端文件



#### 就诊和处方录入

- `controller/ReimbursementController.java`
- `service/impl/ReimbursementService.java`
- `dao/ReimbursementMapper.java`
- `model/VisitForm.java`
- `model/PrescriptionForm.java`
- 原表：
  - `t_personnel_visits_info`
  - `t_prescription_details`
- 扩展表：`ext_prescription_item`，记录目录来源和目录类别。
- `ext_business_sequence`：并发安全地生成 `MZ########` 门诊号。



#### 预结算、正式结算、取消和综合查询

- `controller/SettlementController.java`
- `service/impl/SettlementService.java`
- `dao/SettlementMapper.java`
- `model/SettlementRecord.java`
- `exception/ReimbursementBusinessException.java`
- 数据表：
  - `ext_reimbursement_settlement`：正交易和负交易主记录。
  - `ext_settlement_fee_breakdown`：各类自费和基金费用构成。
  - `ext_settlement_item_result`：逐处方项目计算结果。
  - `personal_annual_expenses`：个人年度累计。



#### Dashboard

- `controller/ReimbursementDashboardController.java`
- `service/impl/ReimbursementDashboardService.java`
- `dao/ReimbursementDashboardMapper.java`
- `static/js/modules/dashboard.js`



#### 数据库迁移和测试

- `resources/db/migration/V1__extension_baseline.sql`：报销扩展表和门诊序列等基础结构。
- `resources/db/migration/V2__settlement_consistency.sql`：结算一致性约束。
- `test/.../reimbursement/ReimbursementApiIntegrationTest.java`
- `test/.../reimbursement/SettlementApiIntegrationTest.java`
- `test/.../reimbursement/ReimbursementDashboardIntegrationTest.java`



### 6.2 D 负责的前端区域

- `templates/index.html`
  - `dashboardPage`
  - `reimbursementPage`、`visitForm`、`prescriptionForm`
  - `settlementQueryPage`、`settlementModal`
- `static/js/modules/reimbursement.js`
  - `loadVisits`、`openVisit`、门诊号沿用与新增逻辑。
  - `loadPrescriptions`、目录类别/收费类别/项目编码联动。
  - `previewSettlement`、`openSettlement`、正式结算、取消和打印。
  - `loadSettlements` 和综合查询。
- `static/js/modules/dashboard.js`：个人/单位对象选择、流程状态、饼图、就诊处方和结算快捷办理。



### 6.3 D 的设计逻辑



#### 就诊与处方

- 门诊先查询该人员是否有未结算旧门诊；用户确认后可沿用，否则数据库序列生成新门诊号。
- 就诊保存时验证人员、机构、病种、医疗类别、医院等级和日期。
- 机构等级从机构资料反向确定，避免手工填写不一致。
- 处方目录类别支持药品、诊疗项目、服务设施；收费类别和项目编码必须双向联动。
- 后端根据目录真实记录再次校验，金额由后端按“单价 × 数量”重算。
- 手工项目或目录外项目全额自费。



#### 待遇计算顺序

`SettlementService.calculate` 是全系统最核心的方法，讲解顺序为：

1. 读取就诊、人员、单位、病种和机构。
2. 验证机构有效期及人员就诊机构审批。
3. 逐条验证处方项目是否存在、是否有效、医院等级是否允许、是否缺少特检审批。
4. 应用最高限价和甲乙丙类规则，得到可纳入医保费用。
5. 读取 B 维护的起付标准。
6. 按 B 维护的实际金额区间逐段计算基金比例。
7. 读取个人年度累计和封顶线，限制本次基金支付。
8. 返回总费用、各类自费、基金支付、个人费用和逐项说明。



#### 预结算、正式结算与取消

- 预结算只调用计算方法并展示，不写账。
- 正式结算在一个事务中锁定就诊和年度累计，写结算主表、明细表、年度累计、就诊状态和日志，防止重复结算。
- 取消报销不删除原记录，而是新增金额取反的负交易，恢复年度累计，并把原结算标为已取消。
- 同一正交易只能取消一次。



### 6.4 D 的答辩演示顺序

1. 在 Dashboard 选择单位和人员，展示单位类型饼图和人员流程。
2. 新建门诊，说明旧门诊确认和自动生成门诊号。
3. 录入药品/诊疗/设施处方，展示类别联动及后端金额重算。
4. 预结算，逐项解释最高限价、甲乙丙类、审批、起付线、分段比例和封顶线。
5. 正式结算，说明年度累计只在此时更新。
6. 综合查询并打印结算单。
7. 取消报销，展示原正交易保留、新增负交易、年度累计恢复。

一句话总结：D 负责“把 A、B、C 维护的数据组合起来完成真正的医保结算，并保证账务可查、可追溯、可冲正”。

## 7. 四个人文件之间的业务关系

```text
A：药品 / 诊疗 / 设施目录 ───────────────┐
                                          │ 提供处方项目、等级、限价、有效期
B：病种 / 机构 / 待遇参数 ───────────────┤
                                          │ 提供病种机构资格、起付线、比例、封顶线
C：单位 / 人员 / 审批 ──────────────────┤
                                          │ 提供参保主体和审批资格
                                          ▼
D：就诊 → 处方 → 预结算 → 正式结算 → 查询/取消
                                          │
                                          ▼
                  年度累计 + 正负交易 + 项目计算明细 + 操作日志
```

依赖关系不是四套互不相关的 CRUD：

- C 的个人必须引用 C 的单位，并可引用 B 的机构。
- C 的特检审批引用 A 的药品或诊疗项目。
- D 的就诊引用 C 的个人、B 的机构和病种。
- D 的处方引用 A 的三类目录。
- D 的结算读取 C 的审批和 B 的三类待遇参数。
- 所有 Service 最后调用共享的 `SystemMapper` 写操作日志。



## 8. 共享文件如何认领，避免互相覆盖

以下物理文件是共享的，不能简单说“整个文件归某个人”：

- `templates/index.html`：每人只负责自己模块的 `<section>` 和表单弹窗。
- `static/js/core/*.js`：全组共享的状态、HTTP、分页、字典和页面导航。
- `static/js/modules/*.js`：每人只负责自己业务模块文件中的加载、编辑、删除和提交函数。
- `static/js/app.js`：只负责启动应用，不放业务功能。
- `static/css/app.css`：全组共享，修改公共类前先说明影响范围。
- `dao/SystemMapper.java`：业务日志公共入口。
- `service/impl/AuthService.java`：获取当前登录用户的公共能力。
- `resources/application.yml`、`pom.xml`、安全配置和主启动类：属于全组运行基础，不算某一个业务模块的个人成果。

推荐协作规则：

1. 一个人一次只提交自己模块文件和共享文件中的自己区段。
2. 提交说明写成 `A: 完成药品新增校验`、`C: 完成特检审批联动`。
3. D 做最终全流程联调，但不能在不通知负责人的情况下改 A/B/C 的业务规则。
4. 每人准备至少一条成功用例和一条失败校验用例。



## 9. 每个人统一使用的答辩话术模板

每人介绍自己模块时按下面六句话组织，老师最容易听懂：

1. “我负责的是……，它在完整报销流程中解决……问题。”
2. “页面入口位于 `index.html` 的……区域，交互逻辑在 `modules` 下对应业务脚本的……函数。”
3. “前端调用……接口，由……Controller 接收。”
4. “核心规则写在……Service，包括……校验和事务。”
5. “……Mapper 使用 MyBatis 操作……表，原表字段没有被修改；新增数据放在……扩展表。”
6. “我用……成功场景和……失败场景验证，操作完成后还会写入日志。”

不要按文件逐行念代码。先说业务目标，再沿“页面 → Controller → Service → Mapper → 数据表”走一遍，最后展示校验和测试。

## 10. 推荐的全组答辩顺序

1. A：基础目录和批量数据，说明费用项目来源。
2. B：病种、机构和待遇参数，说明计算依据。
3. C：单位、人员和审批，说明参保主体与资格。
4. D：完整跑通 Dashboard、就诊、处方、结算、查询与取消。

这个顺序与系统真实依赖一致，最后由 D 演示时能自然调用前三个人维护的数据，形成完整闭环。
