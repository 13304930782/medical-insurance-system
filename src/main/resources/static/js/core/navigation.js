/** 登录视图、角色菜单、公共字典和页面导航 */

function showLogin() {
  $("appView").classList.add("hidden");
  $("loginView").classList.remove("hidden");
  if ($("aiChatWidget")) {
    $("aiChatWidget").classList.add("hidden");
    $("aiChatLauncher").setAttribute("aria-expanded", "false");
  }
}
function showApp(user) {
  currentRole = user.roleCode;
  $("loginView").classList.add("hidden");
  $("appView").classList.remove("hidden");
  $("currentUser").textContent =
    `${user.realName}（${user.username} / ${user.roleCode}）`;
  applyRole(user.roleCode);
  loadDictionaryOptions();
  loadReferenceOptions();
  showPage(user.roleCode === "APPROVER" ? "institutionApproval" : "dashboard");
}
function applyRole(role) {
  const base = [
      "medicine",
      "diagnosis",
      "facility",
      "disease",
      "institution",
      "treatment",
      "company",
      "person",
      "bulk",
      "system",
    ],
    approval = ["institutionApproval", "specialApproval"],
    reimbursement = ["dashboard", "reimbursement", "settlementQuery"];
  base
    .concat(approval, reimbursement)
    .forEach((name) =>
      $(`${name}Menu`).classList.toggle(
        "hidden",
        role !== "ADMIN" &&
          !(
            (role === "APPROVER" && approval.includes(name)) ||
            (role === "REIMBURSEMENT" && reimbursement.includes(name))
          ),
      ),
    );
}
async function loadDictionaryOptions() {
  const mappings = [
    { category: "收费类别", id: "expenseTypeOptions" },
    { category: "医疗类别", id: "medicalCategoryOptions" },
    { category: "审批类别", id: "approvalCategoryOptions" },
    { category: "审批标志", id: "approvalFlagOptions" },
    { category: "是否需要审批标志", id: "approvalMarkOptions" },
    { category: "医院等级", id: "hospitalLevelOptions" },
    { category: "特检特制标志", id: "specialMarkOptions" },
    { category: "疾病种类", id: "diseaseTypeOptions" },
    { category: "病种报销标志", id: "diseaseReimbursementOptions" },
    { category: "医疗机构类别", id: "institutionTypeOptions" },
    { category: "单位类型", id: "companyTypeOptions" },
    { category: "证件类型", id: "idTypeOptions" },
    { category: "性别", id: "sexOptions" },
    { category: "民族", id: "nationalityOptions" },
    { category: "离退休状态", id: "retirementOptions" },
    { category: "户口性质", id: "residenceTypeOptions" },
    { category: "文化程度", id: "educationOptions" },
    { category: "政治面貌", id: "politicalStatusOptions" },
    { category: "个人身份", id: "identityOptions" },
    { category: "用工形式", id: "employmentOptions" },
    { category: "专业技术职务", id: "technicalPositionOptions" },
    { category: "国家职业资格等级（工人技术等级）", id: "workerLevelOptions" },
    { category: "婚姻状况", id: "marriageOptions" },
    { category: "行政职务", id: "administrativePositionOptions" },
    { category: "医疗人员类别", id: "medicalPersonnelOptions" },
    { category: "健康状况", id: "healthOptions" },
    { category: "劳模标志", id: "modelWorkerOptions" },
    { category: "干部标志", id: "cadreOptions" },
    { category: "公务员标志", id: "civilServantOptions" },
    { category: "在编标志", id: "authorizedStrengthOptions" },
    { category: "居民类别", id: "residentTypeOptions" },
    { category: "灵活就业标志", id: "flexibleEmploymentOptions" },
    { category: "农民工标志", id: "migrantWorkerOptions" },
    { category: "雇主标志", id: "employerOptions" },
    { category: "军转人员标志", id: "militaryPersonnelOptions" },
  ];
  await Promise.all(
    mappings.map(async ({ category, id }) => {
      const rows = await api(
        `/api/dictionaries?category=${encodeURIComponent(category)}`,
      );
      $(id).innerHTML = rows
        .map((item) => `<option value="${escapeHtml(item.label)}"></option>`)
        .join("");
    }),
  );
}
async function loadReferenceOptions() {
  const results = await Promise.all([
    api("/api/companies?page=1&size=5000"),
    api("/api/institutions?page=1&size=5000"),
    api("/api/people?page=1&size=5000"),
    api("/api/diseases?page=1&size=5000"),
  ]);
  [
    referenceState.companies,
    referenceState.institutions,
    referenceState.people,
    referenceState.diseases,
  ] = results.map((result) => result.items || []);
  $("companyOptions").innerHTML = referenceState.companies
    .map(
      (item) =>
        `<option value="${escapeHtml(item.companyId)}" label="${escapeHtml(item.companyName)}"></option>`,
    )
    .join("");
  $("medinsOptions").innerHTML = referenceState.institutions
    .map(
      (item) =>
        `<option value="${escapeHtml(item.institutionId)}" label="${escapeHtml(item.institutionName)}"></option>`,
    )
    .join("");
  $("peopleOptions").innerHTML = referenceState.people
    .map(
      (item) =>
        `<option value="${escapeHtml(item.peopleId)}" label="${escapeHtml(item.name)}"></option>`,
    )
    .join("");
  $("diseaseOptions").innerHTML = referenceState.diseases
    .map(
      (item) =>
        `<option value="${escapeHtml(item.diseaseId)}" label="${escapeHtml(item.diseaseName)}"></option>`,
    )
    .join("");
}
function showPage(name) {
  currentPageName = name;
  [
    "dashboard",
    "medicine",
    "diagnosis",
    "facility",
    "disease",
    "institution",
    "company",
    "person",
    "treatment",
    "institutionApproval",
    "specialApproval",
    "reimbursement",
    "settlementQuery",
    "bulk",
    "system",
    "help",
  ].forEach((item) => {
    $(`${item}Page`).classList.toggle("hidden", item !== name);
    $(`${item}Menu`).classList.toggle("active", item === name);
  });
  if (name === "dashboard") loadDashboard();
  if (name === "medicine") loadMedicines();
  if (name === "diagnosis") loadDiagnoses();
  if (name === "facility") loadFacilities();
  if (name === "disease") loadDiseases();
  if (name === "institution") loadInstitutions();
  if (name === "company") loadCompanies();
  if (name === "person") loadPeople();
  if (name === "treatment") loadTreatmentParameters();
  if (name === "institutionApproval") loadInstitutionApprovals();
  if (name === "specialApproval") loadSpecialApprovals();
  if (name === "reimbursement") loadVisits();
  if (name === "settlementQuery") loadSettlements();
  if (name === "bulk") loadBulkModules();
  if (name === "system") loadSystemPage();
}
$("dashboardMenu").onclick = () => showPage("dashboard");
$("medicineMenu").onclick = () => showPage("medicine");
$("diagnosisMenu").onclick = () => showPage("diagnosis");
$("facilityMenu").onclick = () => showPage("facility");
$("diseaseMenu").onclick = () => showPage("disease");
$("institutionMenu").onclick = () => showPage("institution");
$("companyMenu").onclick = () => showPage("company");
$("personMenu").onclick = () => showPage("person");
$("treatmentMenu").onclick = () => showPage("treatment");
$("institutionApprovalMenu").onclick = () => showPage("institutionApproval");
$("specialApprovalMenu").onclick = () => showPage("specialApproval");
$("reimbursementMenu").onclick = () => showPage("reimbursement");
$("settlementQueryMenu").onclick = () => showPage("settlementQuery");
$("bulkMenu").onclick = () => showPage("bulk");
$("systemMenu").onclick = () => showPage("system");
$("helpMenu").onclick = () => openHelp();
