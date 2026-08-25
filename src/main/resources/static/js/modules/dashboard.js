/** 个人与单位报销 Dashboard */

const dashboardState = {
  mode: "person",
  selectedCompany: null,
  selectedPerson: null,
  selectedVisit: null,
  visits: [],
  prescriptions: [],
  settlements: [],
  previewedVisit: null,
  initialized: false,
};

function dashboardYear() {
  const value = Number($("dashboardYear").value);
  return value >= 2000 && value <= 2100 ? value : new Date().getFullYear();
}

async function loadDashboard() {
  if (!$("dashboardYear").value)
    $("dashboardYear").value = new Date().getFullYear();
  await loadDashboardSummary();
  if (!dashboardState.initialized) {
    dashboardState.initialized = true;
    await searchDashboardSubjects();
  } else if (dashboardState.selectedPerson) {
    await refreshDashboardPerson();
  }
}

async function loadDashboardSummary() {
  const data = await api(
    `/api/reimbursements/dashboard/summary?year=${dashboardYear()}`,
  );
  $("dashboardPeopleCount").textContent = Number(
    data.peopleCount || 0,
  ).toLocaleString();
  $("dashboardCompanyCount").textContent = Number(
    data.companyCount || 0,
  ).toLocaleString();
  $("dashboardPendingCount").textContent = Number(
    data.pendingVisitCount || 0,
  ).toLocaleString();
  $("dashboardSettledCount").textContent = Number(
    data.settledVisitCount || 0,
  ).toLocaleString();
  $("dashboardFundPaid").textContent =
    `${Number(data.fundPaid || 0).toFixed(2)} 元`;
  $("dashboardSettledLabel").textContent = `${data.year} 年正式结算`;
  $("dashboardFundLabel").textContent = `${data.year} 年基金支付`;
  renderCompanyTypePie(data.companyTypeDistribution || []);
}

function renderCompanyTypePie(rows) {
  const colors = [
    "#11856e",
    "#3e8ec9",
    "#e4a23a",
    "#8a67c7",
    "#d76570",
    "#55a9a0",
    "#78899a",
    "#b67b45",
  ];
  const total = rows.reduce(
    (sum, row) => sum + Number(row.companyCount || 0),
    0,
  );
  if (!total) {
    $("companyTypePie").style.background = "#dfe7ec";
    $("companyTypeLegend").innerHTML =
      '<div class="dashboard-empty">暂无单位类型数据</div>';
    return;
  }
  let start = 0;
  const stops = rows.map((row, index) => {
    const end = start + (Number(row.companyCount || 0) / total) * 100;
    const value = `${colors[index % colors.length]} ${start.toFixed(4)}% ${end.toFixed(4)}%`;
    start = end;
    return value;
  });
  $("companyTypePie").style.background = `conic-gradient(${stops.join(",")})`;
  $("companyTypeLegend").innerHTML = rows
    .map((row, index) => {
      const count = Number(row.companyCount || 0);
      const percent = (count / total) * 100;
      return `<div><i style="background:${colors[index % colors.length]}"></i><span>${escapeHtml(row.companyType)}：${count.toLocaleString()} 家（${percent.toFixed(1)}%）</span></div>`;
    })
    .join("");
}

function setDashboardMode(mode) {
  dashboardState.mode = mode;
  dashboardState.selectedCompany = null;
  clearDashboardPerson();
  document
    .querySelectorAll("[data-dashboard-mode]")
    .forEach((button) =>
      button.classList.toggle("active", button.dataset.dashboardMode === mode),
    );
  $("dashboardKeyword").value = "";
  $("dashboardKeyword").placeholder =
    mode === "company"
      ? "输入单位编号或单位名称"
      : "输入个人编号、姓名、证件号或社保卡号";
  $("dashboardSubjectTitle").textContent =
    mode === "company" ? "选择参保单位" : "选择参保人员";
  $("dashboardCompanySummary").classList.add("hidden");
  searchDashboardSubjects();
}

function clearDashboardPerson() {
  dashboardState.selectedPerson = null;
  dashboardState.selectedVisit = null;
  dashboardState.visits = [];
  dashboardState.prescriptions = [];
  dashboardState.settlements = [];
  dashboardState.previewedVisit = null;
  reimbursementState.selectedVisit = null;
  $("dashboardNoSelection").classList.remove("hidden");
  $("dashboardWorkspace").classList.add("hidden");
}

async function searchDashboardSubjects() {
  const keyword = encodeURIComponent($("dashboardKeyword").value.trim());
  resetTablePager("dashboardSubject");
  try {
    if (dashboardState.mode === "company") {
      dashboardState.selectedCompany = null;
      clearDashboardPerson();
      $("dashboardCompanySummary").classList.add("hidden");
      $("dashboardSubjectTitle").textContent = "选择参保单位";
      const data = await api(
        `/api/companies?keyword=${keyword}&page=1&size=50`,
      );
      renderDashboardCompanies(data.items || []);
    } else {
      const data = await api(`/api/people?keyword=${keyword}&page=1&size=50`);
      renderDashboardPeople(data.items || []);
    }
  } catch (error) {
    alert(error.message);
  }
}

function renderDashboardCompanies(rows) {
  $("dashboardSubjectList").innerHTML = rows.length
    ? rows
        .map(
          (item) => `
    <div class="dashboard-subject">
      <div><strong>${escapeHtml(item.companyName || item.companyId)}</strong><small>${escapeHtml(item.companyId)} · ${escapeHtml(item.companyType || "未维护单位类型")}</small></div>
      <button class="action-btn dashboard-company-select" data-key="${treatmentKey(item)}">选择单位</button>
    </div>`,
        )
        .join("")
    : '<div class="dashboard-empty">没有找到参保单位</div>';
  paginateClientCollection(
    "dashboardSubject",
    "dashboardSubjectList",
    ":scope > .dashboard-subject",
  );
}

function renderDashboardPeople(rows) {
  $("dashboardSubjectList").innerHTML = rows.length
    ? rows
        .map(
          (item) => `
    <div class="dashboard-subject ${dashboardState.selectedPerson && dashboardState.selectedPerson.peopleId === item.peopleId ? "active" : ""}">
      <div><strong>${escapeHtml(item.name || item.peopleId)}</strong><small>${escapeHtml(item.peopleId)} · ${escapeHtml(item.companyName || item.companyId || "无单位")}</small></div>
      <button class="action-btn dashboard-person-select" data-key="${treatmentKey(item)}">进入办理</button>
    </div>`,
        )
        .join("")
    : '<div class="dashboard-empty">没有找到参保人员</div>';
  paginateClientCollection(
    "dashboardSubject",
    "dashboardSubjectList",
    ":scope > .dashboard-subject",
  );
}

async function selectDashboardCompany(company) {
  dashboardState.selectedCompany = company;
  clearDashboardPerson();
  $("dashboardSubjectTitle").textContent = "选择单位参保人员";
  $("dashboardCompanySummary").innerHTML =
    `当前单位：<strong>${escapeHtml(company.companyName)}</strong>（${escapeHtml(company.companyId)}）　<button class="action-btn" id="dashboardBackCompanies">返回单位列表</button>`;
  $("dashboardCompanySummary").classList.remove("hidden");
  const data = await api(
    `/api/people?keyword=${encodeURIComponent(company.companyId)}&page=1&size=100`,
  );
  const people = (data.items || []).filter(
    (item) => item.companyId === company.companyId,
  );
  resetTablePager("dashboardSubject");
  renderDashboardPeople(people);
  $("dashboardBackCompanies").onclick = searchDashboardSubjects;
}

async function selectDashboardPerson(person) {
  ["dashboardVisit", "dashboardPrescription", "dashboardSettlement"].forEach(
    resetTablePager,
  );
  dashboardState.selectedPerson = await api(
    `/api/people/${encodeURIComponent(person.peopleId)}`,
  );
  dashboardState.selectedVisit = null;
  dashboardState.previewedVisit = null;
  $("dashboardNoSelection").classList.add("hidden");
  $("dashboardWorkspace").classList.remove("hidden");
  await refreshDashboardPerson();
}

async function refreshDashboardPerson(preferredVisitNumber) {
  if (!dashboardState.selectedPerson) return;
  const personId = dashboardState.selectedPerson.peopleId;
  const year = dashboardYear();
  const [visits, settlements] = await Promise.all([
    api(`/api/reimbursements/visits?keyword=${encodeURIComponent(personId)}`),
    api(
      `/api/reimbursements/settlements?keyword=${encodeURIComponent(personId)}&year=${year}`,
    ),
  ]);
  dashboardState.visits = (visits || []).filter(
    (item) => item.personId === personId,
  );
  dashboardState.settlements = (settlements || []).filter(
    (item) => item.personId === personId,
  );
  const selectedNumber =
    preferredVisitNumber ||
    (dashboardState.selectedVisit &&
      dashboardState.selectedVisit.hospitalizationNumber);
  dashboardState.selectedVisit =
    dashboardState.visits.find(
      (item) => item.hospitalizationNumber === selectedNumber,
    ) || null;
  renderDashboardProfile();
  renderDashboardVisits();
  renderDashboardSettlements();
  if (dashboardState.selectedVisit) await loadDashboardPrescriptions();
  else {
    dashboardState.prescriptions = [];
    $("dashboardPrescriptionArea").classList.add("hidden");
    renderDashboardFlow();
  }
}

function renderDashboardProfile() {
  const person = dashboardState.selectedPerson;
  const net = dashboardState.settlements.reduce(
    (result, row) => {
      result.total += Number(row.totalFee || 0);
      result.fund += Number(row.fundFee || 0);
      result.personal += Number(row.personalFee || 0);
      return result;
    },
    { total: 0, fund: 0, personal: 0 },
  );
  const cells = [
    ["办理人员", `${person.name || ""}（${person.peopleId}）`],
    [
      "参保单位",
      `${person.companyName || "未关联单位"}${person.companyId ? `（${person.companyId}）` : ""}`,
    ],
    ["医疗人员类别", person.medicalPersonnel || "未维护"],
    ["登记定点机构", person.medinsName || person.medinsId || "未维护"],
    [`${dashboardYear()} 年医疗费用`, `${net.total.toFixed(2)} 元`],
    [`${dashboardYear()} 年基金支付`, `${net.fund.toFixed(2)} 元`],
    [`${dashboardYear()} 年个人费用`, `${net.personal.toFixed(2)} 元`],
    [
      "未结算就诊",
      `${dashboardState.visits.filter((item) => item.settlementFlag !== "已结算" && item.settlementFlag !== "SETTLED").length} 次`,
    ],
  ];
  $("dashboardProfile").innerHTML = cells
    .map(
      (row) =>
        `<div><span>${escapeHtml(row[0])}</span><strong>${escapeHtml(row[1])}</strong></div>`,
    )
    .join("");
}

function renderDashboardFlow() {
  const selected = !!dashboardState.selectedPerson;
  const hasVisit = !!dashboardState.selectedVisit;
  const hasPrescription = dashboardState.prescriptions.length > 0;
  const previewed =
    hasVisit &&
    dashboardState.previewedVisit ===
      dashboardState.selectedVisit.hospitalizationNumber;
  const settled =
    hasVisit &&
    ["已结算", "SETTLED"].includes(dashboardState.selectedVisit.settlementFlag);
  const done = [
    selected,
    hasVisit,
    hasPrescription,
    previewed || settled,
    settled,
  ];
  const labels = ["选择人员", "就诊登记", "处方录入", "预结算", "结算打印"];
  const firstIncomplete = done.findIndex((value) => !value);
  $("dashboardFlow").innerHTML = labels
    .map(
      (label, index) =>
        `<div class="flow-step ${done[index] ? "done" : firstIncomplete === index ? "active" : ""}"><b>${done[index] ? "✓" : index + 1}</b>${label}</div>`,
    )
    .join("");
}

function renderDashboardVisits() {
  $("dashboardVisitBody").innerHTML = dashboardState.visits.length
    ? dashboardState.visits
        .map((item) => {
          const settled = ["已结算", "SETTLED"].includes(item.settlementFlag);
          const active =
            dashboardState.selectedVisit &&
            dashboardState.selectedVisit.hospitalizationNumber ===
              item.hospitalizationNumber;
          return `<tr${active ? ' style="background:#edf9f5"' : ""}><td>${escapeHtml(item.hospitalizationNumber)}</td><td>${escapeHtml(item.medicalCategory)}</td><td>${escapeHtml(item.institutionName || item.designatedNumber)}</td><td>${escapeHtml(item.diseaseName || item.diseaseCode)}</td><td>${escapeHtml(toDateDisplay(item.admissionDate))}</td><td><span class="tag">${escapeHtml(item.settlementFlag)}</span></td><td><button class="action-btn dashboard-visit-select" data-key="${treatmentKey(item)}">${settled ? "查看结算" : "继续办理"}</button>${settled ? "" : `<button class="action-btn dashboard-visit-edit" data-key="${treatmentKey(item)}">编辑</button>`}</td></tr>`;
        })
        .join("")
    : '<tr><td colspan="7" class="empty">该人员暂无就诊资料，请点击“新增就诊资料”</td></tr>';
  paginateClientTable("dashboardVisit", "dashboardVisitBody");
}

async function selectDashboardVisit(visit) {
  dashboardState.selectedVisit = visit;
  dashboardState.previewedVisit = null;
  reimbursementState.selectedVisit = visit;
  renderDashboardVisits();
  await loadDashboardPrescriptions();
  if (["已结算", "SETTLED"].includes(visit.settlementFlag))
    await openDashboardVisitSettlement();
}

async function loadDashboardPrescriptions() {
  const visit = dashboardState.selectedVisit;
  if (
    !visit ||
    !dashboardState.selectedPerson ||
    $("dashboardPage").classList.contains("hidden")
  )
    return;
  reimbursementState.selectedVisit = visit;
  dashboardState.prescriptions = await api(
    `/api/reimbursements/visits/${encodeURIComponent(visit.hospitalizationNumber)}/prescriptions`,
  );
  $("dashboardPrescriptionArea").classList.remove("hidden");
  $("dashboardPrescriptionBody").innerHTML = dashboardState.prescriptions.length
    ? dashboardState.prescriptions
        .map(
          (item) => `
    <tr><td>${item.sourceType === "MANUAL" ? "手工录入" : "目录选择"}</td><td>${escapeHtml(item.chargeableItemsCategory)}</td><td>${escapeHtml(item.projectName)}（${escapeHtml(item.projectCoding)}）</td><td>${Number(item.unitPrice).toFixed(2)}</td><td>${Number(item.quantity)}</td><td>${Number(item.amount).toFixed(2)}</td><td>${["已结算", "SETTLED"].includes(visit.settlementFlag) ? "-" : `<button class="action-btn dashboard-prescription-edit" data-key="${treatmentKey(item)}">编辑</button><button class="action-btn delete dashboard-prescription-delete" data-key="${treatmentKey(item)}">删除</button>`}</td></tr>`,
        )
        .join("")
    : '<tr><td colspan="7" class="empty">暂无处方明细</td></tr>';
  paginateClientTable("dashboardPrescription", "dashboardPrescriptionBody");
  const settled = ["已结算", "SETTLED"].includes(visit.settlementFlag);
  $("dashboardAddPrescriptionButton").classList.toggle("hidden", settled);
  $("dashboardPreviewButton").classList.toggle("hidden", settled);
  $("dashboardSettleButton").classList.toggle("hidden", settled);
  $("dashboardSettleButton").disabled =
    !dashboardState.prescriptions.length ||
    dashboardState.previewedVisit !== visit.hospitalizationNumber;
  $("dashboardViewSettlementButton").classList.toggle("hidden", !settled);
  renderDashboardFlow();
}

function renderDashboardSettlements() {
  $("dashboardSettlementBody").innerHTML = dashboardState.settlements.length
    ? dashboardState.settlements
        .map(
          (row) => `
    <tr><td>${escapeHtml(row.settlementNo)}</td><td>${escapeHtml(row.hospitalizationNumber)}</td><td>${Number(row.totalFee || 0).toFixed(2)}</td><td>${Number(row.fundFee || 0).toFixed(2)}</td><td>${Number(row.personalFee || 0).toFixed(2)}</td><td><span class="tag">${escapeHtml(row.settlementStatus)}</span></td><td><button class="action-btn dashboard-settlement-detail" data-id="${row.settlementId}">详情/打印</button></td></tr>`,
        )
        .join("")
    : '<tr><td colspan="7" class="empty">该年度暂无结算记录</td></tr>';
  paginateClientTable("dashboardSettlement", "dashboardSettlementBody");
}

async function previewDashboardSettlement(confirmable) {
  const visit = dashboardState.selectedVisit;
  if (!visit) return;
  try {
    const result = await api(
      `/api/reimbursements/visits/${encodeURIComponent(visit.hospitalizationNumber)}/preview`,
      { method: "POST" },
    );
    dashboardState.previewedVisit = visit.hospitalizationNumber;
    $("dashboardSettleButton").disabled = false;
    renderDashboardFlow();
    openSettlement(result, confirmable);
  } catch (error) {
    alert(error.message);
  }
}

async function openDashboardVisitSettlement() {
  const visit = dashboardState.selectedVisit;
  if (!visit) return;
  const row = dashboardState.settlements.find(
    (item) =>
      item.hospitalizationNumber === visit.hospitalizationNumber &&
      Number(item.transactionType) === 1,
  );
  if (!row) return alert("未找到该就诊的正式结算记录");
  openSettlement(
    await api(`/api/reimbursements/settlements/${row.settlementId}`),
    false,
  );
}

async function dashboardVisitSaved(number) {
  if (
    !dashboardState.selectedPerson ||
    $("dashboardPage").classList.contains("hidden")
  )
    return;
  dashboardState.previewedVisit = null;
  await refreshDashboardPerson(number);
  await loadDashboardSummary();
}

async function dashboardSettlementChanged(number) {
  if (
    !dashboardState.selectedPerson ||
    $("dashboardPage").classList.contains("hidden")
  )
    return;
  dashboardState.previewedVisit = null;
  await refreshDashboardPerson(number);
  await loadDashboardSummary();
}

document
  .querySelectorAll("[data-dashboard-mode]")
  .forEach(
    (button) =>
      (button.onclick = () => setDashboardMode(button.dataset.dashboardMode)),
  );
$("dashboardSearchButton").onclick = searchDashboardSubjects;
$("dashboardResetButton").onclick = () => {
  $("dashboardKeyword").value = "";
  searchDashboardSubjects();
};
$("dashboardKeyword").addEventListener("keydown", (event) => {
  if (event.key === "Enter") searchDashboardSubjects();
});
$("refreshDashboardButton").onclick = async () => {
  await loadDashboardSummary();
  if (dashboardState.selectedPerson) await refreshDashboardPerson();
};
$("dashboardYear").onchange = $("refreshDashboardButton").onclick;
$("dashboardSubjectList").addEventListener("click", (event) => {
  const button = event.target.closest("button[data-key]");
  if (!button) return;
  const item = decodeTreatmentKey(button.dataset.key);
  if (button.classList.contains("dashboard-company-select"))
    selectDashboardCompany(item).catch((error) => alert(error.message));
  if (button.classList.contains("dashboard-person-select"))
    selectDashboardPerson(item).catch((error) => alert(error.message));
});
$("dashboardNewVisitButton").onclick = () =>
  openVisit(
    null,
    dashboardState.selectedPerson && dashboardState.selectedPerson.peopleId,
  );
$("dashboardOpenCenterButton").onclick = () => {
  $("visitKeyword").value = dashboardState.selectedPerson.peopleId;
  showPage("reimbursement");
  loadVisits();
};
$("dashboardOpenQueryButton").onclick = () => {
  $("settlementKeyword").value = dashboardState.selectedPerson.peopleId;
  $("settlementYear").value = dashboardYear();
  showPage("settlementQuery");
  loadSettlements();
};
$("dashboardVisitBody").addEventListener("click", (event) => {
  const button = event.target.closest("button[data-key]");
  if (!button) return;
  const visit = decodeTreatmentKey(button.dataset.key);
  if (button.classList.contains("dashboard-visit-select"))
    selectDashboardVisit(visit).catch((error) => alert(error.message));
  if (button.classList.contains("dashboard-visit-edit")) openVisit(visit);
});
$("dashboardAddPrescriptionButton").onclick = () => {
  reimbursementState.selectedVisit = dashboardState.selectedVisit;
  openPrescription(null);
};
$("dashboardPreviewButton").onclick = () => previewDashboardSettlement(false);
$("dashboardSettleButton").onclick = () => previewDashboardSettlement(true);
$("dashboardViewSettlementButton").onclick = () =>
  openDashboardVisitSettlement().catch((error) => alert(error.message));
$("dashboardPrescriptionBody").addEventListener("click", async (event) => {
  const button = event.target.closest("button[data-key]");
  if (!button) return;
  const item = decodeTreatmentKey(button.dataset.key);
  reimbursementState.selectedVisit = dashboardState.selectedVisit;
  if (button.classList.contains("dashboard-prescription-edit"))
    return openPrescription(item);
  if (
    button.classList.contains("dashboard-prescription-delete") &&
    confirm("确定删除该处方明细吗？")
  ) {
    try {
      await api(
        `/api/reimbursements/visits/${encodeURIComponent(dashboardState.selectedVisit.hospitalizationNumber)}/prescriptions`,
        { method: "DELETE", body: JSON.stringify(item) },
      );
      toast("删除成功");
      dashboardState.previewedVisit = null;
      await loadDashboardPrescriptions();
    } catch (error) {
      alert(error.message);
    }
  }
});
$("dashboardSettlementBody").addEventListener("click", async (event) => {
  const button = event.target.closest("button[data-id]");
  if (!button) return;
  try {
    openSettlement(
      await api(`/api/reimbursements/settlements/${button.dataset.id}`),
      false,
    );
  } catch (error) {
    alert(error.message);
  }
});
