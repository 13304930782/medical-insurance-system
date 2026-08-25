/** 就诊、处方、预结算、正式结算、取消和费用查询 */

async function loadVisits() {
  const keyword = encodeURIComponent($("visitKeyword").value.trim());
  const rows = await api(`/api/reimbursements/visits?keyword=${keyword}`);
  $("visitBody").innerHTML = rows.length
    ? rows
        .map(
          (item) =>
            `<tr><td>${escapeHtml(item.hospitalizationNumber)}</td><td>${escapeHtml(item.personName || "")}（${escapeHtml(item.personId)}）</td><td>${escapeHtml(item.companyName)}</td><td>${escapeHtml(item.medicalCategory)}</td><td>${escapeHtml(item.institutionName || item.designatedNumber)}</td><td>${escapeHtml(item.diseaseName || item.diseaseCode)}</td><td>${escapeHtml(toDateDisplay(item.admissionDate))}</td><td><span class="tag">${escapeHtml(item.settlementFlag)}</span></td><td><button class="action-btn visit-detail" data-key="${treatmentKey(item)}">处方明细</button><button class="action-btn visit-edit" data-key="${treatmentKey(item)}">编辑</button><button class="action-btn delete visit-delete" data-key="${treatmentKey(item)}">删除</button></td></tr>`,
        )
        .join("")
    : '<tr><td colspan="9" class="empty">暂无就诊资料</td></tr>';
  paginateClientTable("visit", "visitBody");
  if (reimbursementState.selectedVisit) {
    const found = rows.find(
      (item) =>
        item.hospitalizationNumber ===
        reimbursementState.selectedVisit.hospitalizationNumber,
    );
    if (found) {
      reimbursementState.selectedVisit = found;
      loadPrescriptions();
    } else {
      $("prescriptionPanel").classList.add("hidden");
      reimbursementState.selectedVisit = null;
    }
  }
}
$("searchVisitButton").onclick = () => {
  resetTablePager("visit");
  loadVisits();
};
$("resetVisitButton").onclick = () => {
  $("visitKeyword").value = "";
  resetTablePager("visit");
  loadVisits();
};
$("visitKeyword").addEventListener("keydown", (event) => {
  if (event.key === "Enter") $("searchVisitButton").click();
});
function syncVisitInstitution() {
  const form = $("visitForm");
  const institution = referenceState.institutions.find(
    (item) => item.institutionId === form.designatedNumber.value,
  );
  if (institution && institution.hospitalLevel)
    form.hospitalGrade.value = institution.hospitalLevel;
}
function syncVisitDisease() {
  const form = $("visitForm");
  const disease = referenceState.diseases.find(
    (item) => item.diseaseId === form.diseaseCode.value,
  );
  if (disease) {
    form.admissionCode.value = disease.diseaseId;
    form.diagnosedName.value = disease.diseaseName || "";
  }
}
$("visitForm").elements.designatedNumber.addEventListener(
  "change",
  syncVisitInstitution,
);
$("visitForm").elements.diseaseCode.addEventListener(
  "change",
  syncVisitDisease,
);
async function usePreviousOutpatient(personId) {
  const result = await api(
    `/api/reimbursements/visits/outpatient-candidate?personId=${encodeURIComponent(personId)}`,
  );
  if (!result.hasCandidate) return false;
  const item = result.candidate;
  if (
    !confirm(
      `发现该人员未结算的历史门诊号 ${item.hospitalizationNumber}（${item.medicalCategory}）。\n\n是否沿用并继续办理？`,
    )
  )
    return false;
  closeVisit();
  if (!$("dashboardPage").classList.contains("hidden"))
    await selectDashboardVisit(item);
  else {
    reimbursementState.selectedVisit = item;
    $("prescriptionPanel").classList.remove("hidden");
    await loadPrescriptions();
  }
  return true;
}
async function openVisit(item, prefillPersonId) {
  reimbursementState.visitEditing = item || null;
  const form = $("visitForm");
  form.reset();
  if (item)
    Object.keys(item).forEach((key) => {
      if (form.elements[key])
        form.elements[key].value = ["admissionDate", "dischargeDate"].includes(
          key,
        )
          ? toDateInput(item[key])
          : (item[key] ?? "");
    });
  if (!item && prefillPersonId) form.personId.value = prefillPersonId;
  form.hospitalizationNumber.disabled = !!item;
  form.hospitalizationNumber.readOnly =
    !item && String(form.medicalCategory.value || "").includes("门诊");
  $("visitModalTitle").textContent = item ? "编辑就诊资料" : "新增就诊资料";
  $("visitModal").classList.remove("hidden");
  if (!item && prefillPersonId)
    try {
      await usePreviousOutpatient(prefillPersonId);
    } catch (error) {
      alert(error.message);
    }
}
function closeVisit() {
  reimbursementState.visitEditing = null;
  $("visitModal").classList.add("hidden");
}
$("addVisitButton").onclick = () => openVisit(null);
$("closeVisitModal").onclick = $("cancelVisitModal").onclick = closeVisit;
$("visitForm").personId.addEventListener("change", async (event) => {
  if (reimbursementState.visitEditing || !event.target.value.trim()) return;
  try {
    await usePreviousOutpatient(event.target.value.trim());
  } catch (error) {
    alert(error.message);
  }
});
$("visitForm").medicalCategory.addEventListener("change", (event) => {
  if (reimbursementState.visitEditing) return;
  const outpatient = event.target.value.includes("门诊");
  const number = $("visitForm").hospitalizationNumber;
  number.readOnly = outpatient;
  if (outpatient) number.value = "";
  number.placeholder = outpatient ? "保存时自动生成" : "请输入住院号";
});
$("visitBody").addEventListener("click", (event) => {
  const button = event.target.closest("button[data-key]");
  if (!button) return;
  const item = decodeTreatmentKey(button.dataset.key);
  if (button.classList.contains("visit-detail")) {
    reimbursementState.selectedVisit = item;
    $("prescriptionPanel").classList.remove("hidden");
    loadPrescriptions();
  }
  if (button.classList.contains("visit-edit")) openVisit(item);
  if (
    button.classList.contains("visit-delete") &&
    confirm("确定删除该就诊资料及全部处方明细吗？")
  )
    api(
      `/api/reimbursements/visits/${encodeURIComponent(item.hospitalizationNumber)}`,
      { method: "DELETE" },
    )
      .then(() => {
        toast("删除成功");
        loadVisits();
      })
      .catch((error) => alert(error.message));
});
$("visitForm").addEventListener("submit", async (event) => {
  event.preventDefault();
  const values = Object.fromEntries(new FormData(event.target));
  const editing = !!reimbursementState.visitEditing;
  if (editing)
    values.hospitalizationNumber =
      reimbursementState.visitEditing.hospitalizationNumber;
  if (!editing && String(values.medicalCategory || "").includes("门诊"))
    values.hospitalizationNumber = "";
  try {
    const saved = await api(
      editing
        ? `/api/reimbursements/visits/${encodeURIComponent(values.hospitalizationNumber)}`
        : "/api/reimbursements/visits",
      { method: editing ? "PUT" : "POST", body: JSON.stringify(values) },
    );
    const savedNumber = editing
      ? values.hospitalizationNumber
      : saved.hospitalizationNumber;
    closeVisit();
    toast(`保存成功${editing ? "" : `，门诊号/住院号：${savedNumber}`}`);
    loadVisits();
    if (typeof dashboardVisitSaved === "function")
      dashboardVisitSaved(savedNumber);
  } catch (error) {
    alert(error.message);
  }
});
async function loadPrescriptions() {
  const visit = reimbursementState.selectedVisit;
  if (!visit) return;
  $("selectedVisitInfo").textContent =
    `${visit.personName || visit.personId}｜${visit.hospitalizationNumber}｜${visit.medicalCategory}`;
  const rows = await api(
    `/api/reimbursements/visits/${encodeURIComponent(visit.hospitalizationNumber)}/prescriptions`,
  );
  $("prescriptionBody").innerHTML = rows.length
    ? rows
        .map(
          (item) =>
            `<tr><td>${item.sourceType === "MANUAL" ? "手工录入" : "目录选择"}</td><td>${escapeHtml(item.catalogType)}</td><td>${escapeHtml(item.chargeableItemsCategory)}</td><td>${escapeHtml(item.projectCoding)}</td><td>${escapeHtml(item.projectName)}</td><td>${Number(item.unitPrice).toFixed(2)}</td><td>${Number(item.quantity)}</td><td>${Number(item.amount).toFixed(2)}</td><td><button class="action-btn prescription-edit" data-key="${treatmentKey(item)}">编辑</button><button class="action-btn delete prescription-delete" data-key="${treatmentKey(item)}">删除</button></td></tr>`,
        )
        .join("")
    : '<tr><td colspan="9" class="empty">暂无处方明细</td></tr>';
  paginateClientTable("prescription", "prescriptionBody");
  $("addPrescriptionButton").disabled = visit.settlementFlag === "已结算";
  $("settlementActions").innerHTML =
    visit.settlementFlag === "已结算"
      ? '<button class="secondary-btn" id="viewVisitSettlementButton">查看结算记录</button>'
      : '<button class="secondary-btn" id="previewSettlementButton">预结算</button><button class="primary-btn" id="settleVisitButton">正式结算</button>';
  if ($("previewSettlementButton"))
    $("previewSettlementButton").onclick = () =>
      previewSettlement(visit.hospitalizationNumber, false);
  if ($("settleVisitButton"))
    $("settleVisitButton").onclick = () =>
      previewSettlement(visit.hospitalizationNumber, true);
  if ($("viewVisitSettlementButton"))
    $("viewVisitSettlementButton").onclick = async () => {
      const rows = await api(
        `/api/reimbursements/settlements?keyword=${encodeURIComponent(visit.hospitalizationNumber)}`,
      );
      if (rows.length)
        openSettlement(
          await api(`/api/reimbursements/settlements/${rows[0].settlementId}`),
          false,
        );
      else alert("未找到结算记录");
    };
}
function renderPrescriptionCategoryOptions() {
  const form = $("prescriptionForm");
  let categories;
  if (form.sourceType.value === "MANUAL") {
    categories = Array.from($("expenseTypeOptions").options).map(
      (option) => option.value,
    );
  } else {
    categories = (reimbursementState.catalogItems || []).map(
      catalogItemCategory,
    );
  }
  categories = [...new Set(categories.filter(Boolean))].sort((left, right) =>
    left.localeCompare(right, "zh-CN"),
  );
  $("prescriptionExpenseTypeOptions").innerHTML = categories
    .map((category) => `<option value="${escapeHtml(category)}"></option>`)
    .join("");
}
function renderCatalogProjectOptions() {
  const form = $("prescriptionForm");
  const category = form.chargeableItemsCategory.value.trim();
  const rows = (reimbursementState.catalogItems || []).filter(
    (item) => !category || catalogItemCategory(item) === category,
  );
  $("catalogProjectOptions").innerHTML = rows
    .map(
      (item) =>
        `<option value="${escapeHtml(catalogItemCode(item))}" label="${escapeHtml(catalogOptionLabel(item))}"></option>`,
    )
    .join("");
}
function clearPrescriptionProject() {
  const form = $("prescriptionForm");
  form.projectCoding.value = "";
  form.projectName.value = "";
  form.projectCoding.setCustomValidity("");
  refreshClearButtons(form);
}
function syncPrescriptionProject() {
  const form = $("prescriptionForm");
  const item = (reimbursementState.catalogItems || []).find(
    (row) => catalogItemCode(row) === form.projectCoding.value,
  );
  if (item) {
    form.projectName.value = catalogItemName(item);
    form.chargeableItemsCategory.value = catalogItemCategory(item);
    form.projectCoding.setCustomValidity("");
    renderCatalogProjectOptions();
  } else if (form.projectCoding.value && form.sourceType.value === "CATALOG") {
    form.projectCoding.setCustomValidity(
      "请选择与目录类别、收费项目类别匹配的项目",
    );
  } else {
    form.projectCoding.setCustomValidity("");
  }
  refreshClearButtons(form);
}
async function loadCatalogOptions(type) {
  let data;
  if (type === "DIAGNOSIS") data = await api("/api/diagnoses?page=1&size=5000");
  else if (type === "FACILITY")
    data = await api("/api/facilities?page=1&size=5000");
  else data = await api("/api/medicines?page=1&size=5000");
  reimbursementState.catalogItems = data.items || [];
  renderPrescriptionCategoryOptions();
  renderCatalogProjectOptions();
}
function syncPrescriptionSource() {
  const form = $("prescriptionForm");
  const manual = form.sourceType.value === "MANUAL";
  if (manual) {
    form.catalogType.value = "MANUAL";
    form.projectCoding.required = false;
    form.projectName.readOnly = false;
    reimbursementState.catalogItems = [];
    $("catalogProjectOptions").innerHTML = "";
    renderPrescriptionCategoryOptions();
  } else {
    if (form.catalogType.value === "MANUAL")
      form.catalogType.value = "MEDICINE";
    form.projectCoding.required = true;
    form.projectName.readOnly = true;
    if (!reimbursementState.catalogItems.length)
      loadCatalogOptions(form.catalogType.value);
    else renderPrescriptionCategoryOptions();
  }
  refreshClearButtons(form);
}
$("prescriptionForm").elements.sourceType.onchange = () => {
  const form = $("prescriptionForm");
  form.chargeableItemsCategory.value = "";
  clearPrescriptionProject();
  syncPrescriptionSource();
};
$("prescriptionForm").elements.catalogType.onchange = async (event) => {
  const form = $("prescriptionForm");
  form.chargeableItemsCategory.value = "";
  clearPrescriptionProject();
  reimbursementState.catalogItems = [];
  $("prescriptionExpenseTypeOptions").innerHTML = "";
  if (event.target.value !== "MANUAL")
    await loadCatalogOptions(event.target.value);
};
$("prescriptionForm").elements.chargeableItemsCategory.addEventListener(
  "input",
  () => {
    const form = $("prescriptionForm");
    renderCatalogProjectOptions();
    const selected = (reimbursementState.catalogItems || []).find(
      (row) => catalogItemCode(row) === form.projectCoding.value,
    );
    if (
      selected &&
      catalogItemCategory(selected) !==
        form.chargeableItemsCategory.value.trim()
    )
      clearPrescriptionProject();
  },
);
$("prescriptionForm").elements.projectCoding.addEventListener(
  "input",
  (event) => {
    if (!event.target.value)
      $("prescriptionForm").elements.projectName.value = "";
    event.target.setCustomValidity("");
    refreshClearButtons($("prescriptionForm"));
  },
);
$("prescriptionForm").elements.projectCoding.onchange = syncPrescriptionProject;
async function openPrescription(item) {
  reimbursementState.prescriptionEditing = item || null;
  const form = $("prescriptionForm");
  form.reset();
  form.sourceType.value = item ? item.sourceType : "CATALOG";
  form.catalogType.value = item ? item.catalogType : "MEDICINE";
  if (item)
    Object.keys(item).forEach((key) => {
      if (form.elements[key]) form.elements[key].value = item[key] ?? "";
    });
  form.sourceType.disabled = !!item;
  form.catalogType.disabled = !!item;
  form.chargeableItemsCategory.disabled = !!item;
  form.projectCoding.disabled = !!item;
  reimbursementState.catalogItems = [];
  if (form.sourceType.value === "CATALOG")
    await loadCatalogOptions(form.catalogType.value);
  syncPrescriptionSource();
  refreshClearButtons(form);
  $("prescriptionModalTitle").textContent = item
    ? "编辑处方明细"
    : "新增处方明细";
  $("prescriptionModal").classList.remove("hidden");
}
function closePrescription() {
  reimbursementState.prescriptionEditing = null;
  $("prescriptionModal").classList.add("hidden");
}
$("addPrescriptionButton").onclick = () => openPrescription(null);
$("closePrescriptionModal").onclick = $("cancelPrescriptionModal").onclick =
  closePrescription;
$("prescriptionBody").addEventListener("click", (event) => {
  const button = event.target.closest("button[data-key]");
  if (!button) return;
  const item = decodeTreatmentKey(button.dataset.key);
  if (button.classList.contains("prescription-edit")) openPrescription(item);
  if (
    button.classList.contains("prescription-delete") &&
    confirm("确定删除该处方明细吗？")
  )
    api(
      `/api/reimbursements/visits/${encodeURIComponent(reimbursementState.selectedVisit.hospitalizationNumber)}/prescriptions`,
      { method: "DELETE", body: JSON.stringify(item) },
    )
      .then(() => {
        toast("删除成功");
        loadPrescriptions();
      })
      .catch((error) => alert(error.message));
});
$("prescriptionForm").addEventListener("submit", async (event) => {
  event.preventDefault();
  const values = Object.fromEntries(new FormData(event.target));
  if (reimbursementState.prescriptionEditing)
    Object.assign(values, {
      sourceType: reimbursementState.prescriptionEditing.sourceType,
      catalogType: reimbursementState.prescriptionEditing.catalogType,
      chargeableItemsCategory:
        reimbursementState.prescriptionEditing.chargeableItemsCategory,
      projectCoding: reimbursementState.prescriptionEditing.projectCoding,
    });
  values.unitPrice = Number(values.unitPrice);
  values.quantity = Number(values.quantity);
  try {
    await api(
      `/api/reimbursements/visits/${encodeURIComponent(reimbursementState.selectedVisit.hospitalizationNumber)}/prescriptions`,
      {
        method: reimbursementState.prescriptionEditing ? "PUT" : "POST",
        body: JSON.stringify(values),
      },
    );
    closePrescription();
    toast("保存成功");
    loadPrescriptions();
    if (typeof loadDashboardPrescriptions === "function")
      loadDashboardPrescriptions();
  } catch (error) {
    alert(error.message);
  }
});

const feeLabels = {
  OVER_LIMIT_SELF: "超过最高限价自费",
  CLASS_SELF: "乙/丙类自费",
  HOSPITAL_SELF: "医院等级限制自费",
  SPECIAL_SELF: "无有效审批自费",
  NON_CATALOG_SELF: "目录外项目自费",
  INVALID_ITEM_SELF: "无效目录项目自费",
  DEDUCTIBLE_SELF: "起付标准个人负担",
  SEGMENT_SELF: "分段个人负担",
  CAP_SELF: "超过年度封顶线自费",
};
async function previewSettlement(number, confirmable) {
  try {
    openSettlement(
      await api(
        `/api/reimbursements/visits/${encodeURIComponent(number)}/preview`,
        { method: "POST" },
      ),
      confirmable,
    );
  } catch (error) {
    alert(error.message);
  }
}
function openSettlement(data, confirmable) {
  reimbursementState.settlement = data;
  const status = data.settlementStatus || "PREVIEW";
  $("settlementModalTitle").textContent =
    status === "PREVIEW" ? "医疗费用预结算清单" : "医疗费用结算清单";
  $("settlementIdentity").textContent =
    `人员：${data.personName || data.personId || ""}（${data.personId || ""}）｜单位：${data.companyName || data.visit?.companyName || ""}｜住院号：${data.hospitalizationNumber || ""}｜医疗机构：${data.institutionName || ""}｜年度：${data.year || ""}`;
  const cards = Array.of(
    Array.of("医疗总费用", data.totalFee),
    Array.of("符合医保费用", data.eligibleFee),
    Array.of("起付标准自付", data.deductibleSelfFee),
    Array.of("分段个人自付", data.segmentSelfFee),
    Array.of("基金支付", data.fundFee),
    Array.of("个人费用合计", data.personalFee),
    Array.of(
      "结算前年度基金",
      data.annualFundBefore ?? data.annualExpense?.medicareExpenses,
    ),
    Array.of(
      "结算后年度基金",
      data.annualFundAfter ?? data.annualExpense?.medicareExpenses,
    ),
  );
  $("settlementSummary").innerHTML = cards
    .map(
      (row) =>
        `<div class="summary-card"><span>${row[0]}</span><strong>${Number(row[1] || 0).toFixed(2)} 元</strong></div>`,
    )
    .join("");
  $("settlementBreakdownBody").innerHTML = (data.breakdowns || [])
    .map(
      (row) =>
        `<tr><td>${escapeHtml(feeLabels[row.feeType] || row.feeType)}</td><td>${Number(row.amount || 0).toFixed(2)}</td></tr>`,
    )
    .join("");
  $("settlementItemBody").innerHTML = (data.items || [])
    .map(
      (row) =>
        `<tr><td>${escapeHtml(row.chargeableItemsCategory)}</td><td>${escapeHtml(row.projectCoding)}</td><td>${escapeHtml(row.projectName)}</td><td>${Number(row.totalFee || 0).toFixed(2)}</td><td>${Number(row.eligibleFee || 0).toFixed(2)}</td><td>${Number(row.selfFee || 0).toFixed(2)}</td><td style="white-space:normal">${escapeHtml(row.calculationNote)}</td></tr>`,
    )
    .join("");
  resetTablePager("settlementBreakdown");
  resetTablePager("settlementItem");
  paginateClientTable("settlementBreakdown", "settlementBreakdownBody");
  paginateClientTable("settlementItem", "settlementItemBody");
  $("confirmSettlementButton").classList.toggle(
    "hidden",
    !confirmable || status !== "PREVIEW",
  );
  $("cancelSettlementButton").classList.toggle(
    "hidden",
    !(status === "SETTLED" && Number(data.transactionType) === 1),
  );
  $("settlementModal").classList.remove("hidden");
}
function closeSettlement() {
  $("settlementModal").classList.add("hidden");
  reimbursementState.settlement = null;
}
$("closeSettlementModal").onclick = closeSettlement;
$("printSettlementButton").onclick = () => window.print();
$("confirmSettlementButton").onclick = async () => {
  const data = reimbursementState.settlement;
  if (!data || !confirm("确认执行正式结算并更新年度累计吗？")) return;
  try {
    openSettlement(
      await api(
        `/api/reimbursements/visits/${encodeURIComponent(data.hospitalizationNumber)}/settle`,
        { method: "POST" },
      ),
      false,
    );
    toast("正式结算成功");
    loadVisits();
    if (typeof dashboardSettlementChanged === "function")
      dashboardSettlementChanged(data.hospitalizationNumber);
  } catch (error) {
    alert(error.message);
  }
};
$("cancelSettlementButton").onclick = async () => {
  const data = reimbursementState.settlement;
  const reason = prompt("请输入取消报销原因");
  if (reason === null) return;
  try {
    openSettlement(
      await api(`/api/reimbursements/settlements/${data.settlementId}/cancel`, {
        method: "POST",
        body: JSON.stringify({ reason }),
      }),
      false,
    );
    toast("取消报销成功");
    loadSettlements();
    loadVisits();
    if (typeof dashboardSettlementChanged === "function")
      dashboardSettlementChanged(data.hospitalizationNumber);
  } catch (error) {
    alert(error.message);
  }
};
async function loadSettlements() {
  const keyword = encodeURIComponent($("settlementKeyword").value.trim());
  const year = $("settlementYear").value;
  const rows = await api(
    `/api/reimbursements/settlements?keyword=${keyword}${year ? `&year=${encodeURIComponent(year)}` : ""}`,
  );
  $("settlementBody").innerHTML = rows.length
    ? rows
        .map(
          (row) =>
            `<tr><td>${escapeHtml(row.settlementNo)}</td><td>${Number(row.transactionType) === 1 ? "正交易" : "负交易"}</td><td>${escapeHtml(row.personName)}（${escapeHtml(row.personId)}）</td><td>${escapeHtml(row.hospitalizationNumber)}</td><td>${escapeHtml(String(row.settledAt || "").slice(0, 4))} / ${escapeHtml(row.medicalCategory)}</td><td>${Number(row.totalFee || 0).toFixed(2)}</td><td>${Number(row.fundFee || 0).toFixed(2)}</td><td>${Number(row.personalFee || 0).toFixed(2)}</td><td><span class="tag">${escapeHtml(row.settlementStatus)}</span></td><td>${escapeHtml(toDateDisplay(row.settledAt))}</td><td><button class="action-btn settlement-detail" data-id="${row.settlementId}">详情/打印</button>${row.settlementStatus === "SETTLED" && Number(row.transactionType) === 1 ? `<button class="action-btn delete settlement-cancel" data-id="${row.settlementId}">取消报销</button>` : ""}</td></tr>`,
        )
        .join("")
    : '<tr><td colspan="11" class="empty">暂无结算记录</td></tr>';
  paginateClientTable("settlement", "settlementBody");
}
$("searchSettlementButton").onclick = () => {
  resetTablePager("settlement");
  loadSettlements();
};
$("resetSettlementButton").onclick = () => {
  $("settlementKeyword").value = "";
  $("settlementYear").value = "";
  resetTablePager("settlement");
  loadSettlements();
};
$("settlementBody").addEventListener("click", async (event) => {
  const button = event.target.closest("button[data-id]");
  if (!button) return;
  try {
    openSettlement(
      await api(`/api/reimbursements/settlements/${button.dataset.id}`),
      false,
    );
    if (button.classList.contains("settlement-cancel"))
      $("cancelSettlementButton").click();
  } catch (error) {
    alert(error.message);
  }
});
