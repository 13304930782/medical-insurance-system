# 医疗保险报销系统四人基础答辩讲稿

## 一、统一介绍

本项目使用 IDEA 开发，采用 Maven 管理项目，主要技术包括 Spring Boot、Spring Web、Thymeleaf、MyBatis 和 MySQL。

系统主要完成医疗保险中心相关数据的查询、新增、修改和删除，并在这些基础数据之上实现审批、报销和费用查询。

项目开发过程中使用了 AI 辅助编写部分代码、完善业务校验和生成测试。小组主要完成了需求整理、数据库导入、功能划分、页面操作、基础增删改查、运行测试和整体联调。

## 二、只需要掌握的文件关系

每个人只需记住下面这条基础关系：

```text
index.html：显示页面和表单
     ↓
modules/*.js：按功能处理按钮点击并请求后端
     ↓
Controller：接收查询、新增、修改、删除请求
     ↓
Service：处理基本业务
     ↓
Mapper：使用 MyBatis 操作 MySQL 数据表
```

答辩时不需要展开复杂校验、事务、并发和计算公式。老师如果追问，可以回答：

> 复杂业务校验和测试代码是在 AI 辅助下完成的，我们通过 Maven 测试和实际页面操作进行了验证。我们的主要学习重点是掌握 Spring Boot 分层结构、MyBatis 数据库操作和基础增删改查流程。

## 三、A 的讲解内容

### 负责项目

- 1.1 药品信息维护
- 1.2 诊疗项目信息维护
- 1.3 服务设施项目维护

### 主要功能

- 根据编码或名称查询数据。
- 新增药品、诊疗项目和服务设施。
- 修改已有数据。
- 删除不需要的数据。
- 使用 Excel 批量导入和导出目录数据。

### 对应代码文件

药品：

- `MedicineController.java`
- `MedicineService.java`
- `MedicineMapper.java`
- `MedicineForm.java`

诊疗项目：

- `DiagnosisController.java`
- `DiagnosisService.java`
- `DiagnosisMapper.java`
- `DiagnosisForm.java`

服务设施：

- `FacilityController.java`
- `FacilityService.java`
- `FacilityMapper.java`
- `FacilityForm.java`

页面功能位于：

- `index.html` 中的药品、诊疗项目和服务设施区域。
- `modules/catalog.js` 中的药品、诊疗项目和服务设施函数。

对应数据表：

- `t_medicine`
- `t_diagnosis_project`
- `t_service_facilities`

### A 可以直接这样讲

> 我负责药品、诊疗项目和服务设施三个基础目录模块。这三个模块结构比较相似，都实现了查询、新增、修改和删除。前端在 index.html 中显示数据，catalog.js 负责页面操作，Controller 接收请求，Service 处理功能，Mapper 操作 MySQL 中对应的三张数据表。此外，目录数据还支持 Excel 批量导入和导出。

### A 的现场演示

1. 查询一个药品。
2. 新增一条测试药品。
3. 修改测试药品名称。
4. 删除测试药品。
5. 简单展示诊疗项目、服务设施和 Excel 导入页面。

## 四、B 的讲解内容

### 负责项目

- 1.4 病种信息维护
- 1.5 定点医疗机构信息维护
- 1.6 医疗待遇计算参数维护

### 主要功能

- 病种信息的查询、新增、修改和删除。
- 定点医疗机构信息的查询、新增、修改和删除。
- 封顶线、起付标准和分段比例的查询、新增、修改和删除。

### 对应代码文件

病种：

- `DiseaseController.java`
- `DiseaseService.java`
- `DiseaseMapper.java`
- `DiseaseForm.java`

定点医疗机构：

- `InstitutionController.java`
- `InstitutionService.java`
- `InstitutionMapper.java`
- `InstitutionForm.java`

待遇参数：

- `TreatmentParameterController.java`
- `TreatmentParameterService.java`
- `TreatmentParameterMapper.java`
- `CappingLineForm.java`
- `MinimumPaymentStandardForm.java`
- `SegmentRatioForm.java`

页面功能位于：

- `index.html` 中的病种、医疗机构和待遇参数区域。
- `modules/master-data.js` 中的病种、机构函数。
- `modules/treatment-approval.js` 中的待遇参数函数。

对应数据表：

- `t_disease_`
- `t_medical_insititution`
- `ext_medical_institution_profile`
- `t_capping_line`
- `t_minimum_payment_standard`
- `t_individual_segement_self_funded_ratio`

### B 可以直接这样讲

> 我负责病种、定点医疗机构和医疗待遇参数维护。病种和机构模块完成基础资料的增删改查。待遇参数分为封顶线、起付标准和分段比例三个页面，这些参数会在后面的报销功能中使用。代码同样采用 Controller、Service、Mapper 的分层方式，通过 MyBatis 操作 MySQL 数据表。

### B 的现场演示

1. 查询并新增一个病种。
2. 查询并修改一个医疗机构。
3. 打开待遇参数页面，分别展示三个页签。
4. 新增或修改一条封顶线参数。

## 五、C 的讲解内容

### 负责项目

- 2.1 人员就诊机构审批
- 2.2 特检特治审批
- 5.1 单位基本信息维护
- 5.2 个人基本信息维护

### 主要功能

- 单位信息的查询、新增、修改和删除。
- 个人信息的查询、新增、修改和删除。
- 人员就诊机构审批记录的查询、新增、修改和删除。
- 特检特治审批记录的查询、新增、修改和删除。

### 对应代码文件

单位：

- `CompanyController.java`
- `CompanyService.java`
- `CompanyMapper.java`
- `CompanyMapper.xml`
- `CompanyForm.java`

个人：

- `PersonController.java`
- `PersonService.java`
- `PersonMapper.java`
- `PersonForm.java`

审批：

- `ApprovalController.java`
- `ApprovalService.java`
- `ApprovalMapper.java`
- `InstitutionApprovalForm.java`
- `SpecialApprovalForm.java`

页面功能位于：

- `index.html` 中的单位、个人、机构审批和特检特治审批区域。
- `modules/master-data.js` 中的单位和个人函数。
- `modules/treatment-approval.js` 中的两类审批函数。

对应数据表：

- `company`
- `people`
- `t_application_info`
- `t_special_approval`
- `ext_special_approval_item`

### C 可以直接这样讲

> 我负责单位、个人和两类审批功能。单位和个人模块主要维护参保单位与参保人员资料。审批模块用于记录某个人的就诊机构审批和特检特治审批。所有页面都支持查询、新增、修改和删除，后端通过 Controller、Service 和 Mapper 操作对应的数据表。

### C 的现场演示

1. 查询一个单位并查看单位信息。
2. 查询该单位下的个人。
3. 新增一条人员就诊机构审批。
4. 新增一条特检特治审批。
5. 修改或删除刚才的测试审批。

## 六、D 的讲解内容

### 负责项目

- 3.1 中心报销
- 3.2 取消报销
- 4.1 医疗人员费用查询

### 主要功能

- 选择单位或个人办理报销。
- 新增和修改就诊资料。
- 新增、修改和删除处方明细。
- 执行预结算和正式结算。
- 查询结算记录和费用明细。
- 取消已经完成的报销。
- 打印结算清单。

### 对应代码文件

就诊和处方：

- `ReimbursementController.java`
- `ReimbursementService.java`
- `ReimbursementMapper.java`
- `VisitForm.java`
- `PrescriptionForm.java`

结算、取消和查询：

- `SettlementController.java`
- `SettlementService.java`
- `SettlementMapper.java`
- `SettlementRecord.java`

Dashboard：

- `ReimbursementDashboardController.java`
- `ReimbursementDashboardService.java`
- `ReimbursementDashboardMapper.java`
- `modules/dashboard.js`

页面功能位于：

- `index.html` 中的 Dashboard、中心报销和综合查询区域。
- `modules/reimbursement.js` 中的就诊、处方和结算函数。
- `modules/dashboard.js` 中的 Dashboard 功能。

主要数据表：

- `t_personnel_visits_info`
- `t_prescription_details`
- `personal_annual_expenses`
- `ext_reimbursement_settlement`
- `ext_settlement_fee_breakdown`
- `ext_settlement_item_result`

### D 可以直接这样讲

> 我负责中心报销、取消报销和费用查询。用户可以在 Dashboard 中选择个人或单位人员，录入就诊资料和处方，然后进行预结算和正式结算。完成结算后可以在综合查询中查看费用明细、打印清单，也可以取消报销。计算和校验部分是在 AI 辅助下完成的，我主要负责页面流程、基础数据操作和完整流程测试。

### D 的现场演示

1. 在 Dashboard 选择一个人员。
2. 打开已有就诊记录和处方。
3. 点击预结算，展示费用结果。
4. 查询一条已经正式结算的记录。
5. 打开详情并展示打印按钮。
6. 如果测试数据允许，再展示取消报销。

## 七、四个人如何说明模块关系

只需要讲清下面四句话：

1. A 维护药品、诊疗项目和服务设施，为处方录入提供项目数据。
2. B 维护病种、医疗机构和待遇参数，为就诊和报销提供基础数据。
3. C 维护单位、个人和审批，为报销提供人员资料和审批记录。
4. D 使用 A、B、C 维护的数据，完成就诊、处方、报销、取消和查询。

可以用下面的简图：

```text
A 基础项目目录 ─┐
B 病种机构参数 ─┼→ D 中心报销、取消报销、费用查询
C 单位人员审批 ─┘
```

## 八、老师追问时的简短回答

### 为什么要分 Controller、Service、Mapper？

> 为了把页面请求、业务处理和数据库操作分开，代码更清楚，也方便修改和维护。

### MyBatis 有什么作用？

> MyBatis 用来执行 SQL，把 Java 中的数据和 MySQL 数据表连接起来。

### Maven 有什么作用？

> Maven 用来管理 Spring Boot、MyBatis、MySQL Driver 等依赖，也可以统一运行和测试项目。

### Thymeleaf 有什么作用？

> Thymeleaf 用于生成和组织系统的 HTML 页面，本项目再配合 JavaScript 调用后端接口。

### AI 在项目中做了什么？

> AI 辅助生成和整理了部分代码、复杂业务校验及测试，我们根据需求文档进行了功能选择、数据库导入、页面操作、运行验证和调整。通过这个过程，我们主要学习了 Maven 项目结构、Spring Boot 分层、MyBatis 和基础增删改查。

### 原始数据库为什么还有扩展表？

> 原始表名和字段来自老师提供的数据库文档，我们没有修改。原表没有但系统需要的内容，单独放在 `ext_` 扩展表中。

## 九、推荐答辩时间

- 项目统一介绍：1 分钟。
- A：2 分钟。
- B：2 分钟。
- C：2 分钟。
- D：3 分钟并完成整体演示。
- 老师提问：根据上面的简短回答作答。

整个讲解重点放在页面能完成什么、数据保存到哪张表，以及 Controller、Service、Mapper 的基础关系，不主动展开复杂算法。
