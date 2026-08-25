/** 待遇参数、人员就诊机构审批和特检特治审批 */

function setTreatmentTab(type) {
  treatmentState.type = type;
  treatmentState.editing = null;
  resetTablePager(type);
  document
    .querySelectorAll("[data-treatment-tab]")
    .forEach((button) =>
      button.classList.toggle("active", button.dataset.treatmentTab === type),
    );
  ["capping", "minimum", "segment"].forEach((item) =>
    $(item + "Table").classList.toggle("hidden", item !== type),
  );
  loadTreatmentParameters();
}
document
  .querySelectorAll("[data-treatment-tab]")
  .forEach(
    (button) =>
      (button.onclick = () => setTreatmentTab(button.dataset.treatmentTab)),
  );
async function loadTreatmentParameters() {
  const keyword = encodeURIComponent($("treatmentKeyword").value.trim()),
    type = treatmentState.type;
  if (type === "capping") {
    const rows = await api(
      `/api/treatment-parameters/capping-lines?keyword=${keyword}`,
    );
    $("cappingBody").innerHTML = rows.length
      ? rows
          .map(
            (item) =>
              `<tr><td>${escapeHtml(item.medicalPersonnelCategory)}</td><td>${Number(item.cappingLineFee).toFixed(2)}</td><td><button class="action-btn treatment-edit" data-key="${treatmentKey(item)}">编辑</button><button class="action-btn delete treatment-delete" data-key="${treatmentKey(item)}">删除</button></td></tr>`,
          )
          .join("")
      : '<tr><td colspan="3" class="empty">暂无封顶线参数</td></tr>';
  } else if (type === "minimum") {
    const rows = await api(
      `/api/treatment-parameters/minimum-payment-standards?keyword=${keyword}`,
    );
    $("minimumBody").innerHTML = rows.length
      ? rows
          .map(
            (item) =>
              `<tr><td>${escapeHtml(item.medicalCategory)}</td><td>${escapeHtml(item.medicalPersonnelCategory)}</td><td>${escapeHtml(item.hospitalLevel)}</td><td>${Number(item.minimumPaymentStandard).toFixed(2)}</td><td><button class="action-btn treatment-edit" data-key="${treatmentKey(item)}">编辑</button><button class="action-btn delete treatment-delete" data-key="${treatmentKey(item)}">删除</button></td></tr>`,
          )
          .join("")
      : '<tr><td colspan="5" class="empty">暂无起付标准参数</td></tr>';
  } else {
    const rows = await api(
      `/api/treatment-parameters/segment-ratios?keyword=${keyword}`,
    );
    $("segmentBody").innerHTML = rows.length
      ? rows
          .map(
            (item) =>
              `<tr><td>${escapeHtml(item.medicalCategory)}</td><td>${escapeHtml(item.medicalPersonnelCategory)}</td><td>${escapeHtml(item.hospitalLevel)}</td><td>${Number(item.minimumAmount).toFixed(2)}</td><td>${Number(item.maximumAmount).toFixed(2)}</td><td>${(Number(item.reimbursementProportion) * 100).toFixed(2)}%</td><td><button class="action-btn treatment-edit" data-key="${treatmentKey(item)}">编辑</button><button class="action-btn delete treatment-delete" data-key="${treatmentKey(item)}">删除</button></td></tr>`,
          )
          .join("")
      : '<tr><td colspan="7" class="empty">暂无分段比例参数</td></tr>';
  }
  paginateClientTable(type, `${type}Body`);
}
$("searchTreatmentButton").onclick = () => {
  resetTablePager(treatmentState.type);
  loadTreatmentParameters();
};
$("resetTreatmentButton").onclick = () => {
  $("treatmentKeyword").value = "";
  resetTablePager(treatmentState.type);
  loadTreatmentParameters();
};
$("treatmentKeyword").addEventListener("keydown", (event) => {
  if (event.key === "Enter") $("searchTreatmentButton").click();
});
function setKeyDisabled(form, disabled) {
  [
    "medicalCategory",
    "medicalPersonnelCategory",
    "hospitalLevel",
    "minimumAmount",
    "maximumAmount",
  ].forEach((name) => {
    if (form.elements[name]) form.elements[name].disabled = disabled;
  });
}
$("addTreatmentButton").onclick = () => {
  treatmentState.editing = null;
  if (treatmentState.type === "capping") {
    const form = $("cappingForm");
    form.reset();
    setKeyDisabled(form, false);
    $("cappingModalTitle").textContent = "新增基金封顶线";
    $("cappingModal").classList.remove("hidden");
  } else if (treatmentState.type === "minimum") {
    const form = $("minimumForm");
    form.reset();
    setKeyDisabled(form, false);
    $("minimumModalTitle").textContent = "新增起付标准";
    $("minimumModal").classList.remove("hidden");
  } else {
    const form = $("segmentForm");
    form.reset();
    setKeyDisabled(form, false);
    $("segmentModalTitle").textContent = "新增个人分段自费比例";
    $("segmentModal").classList.remove("hidden");
  }
};
function closeTreatmentModal(type) {
  $(type + "Modal").classList.add("hidden");
  treatmentState.editing = null;
}
$("closeCappingModal").onclick = $("cancelCappingModal").onclick = () =>
  closeTreatmentModal("capping");
$("closeMinimumModal").onclick = $("cancelMinimumModal").onclick = () =>
  closeTreatmentModal("minimum");
$("closeSegmentModal").onclick = $("cancelSegmentModal").onclick = () =>
  closeTreatmentModal("segment");
function editTreatment(item) {
  treatmentState.editing = item;
  if (treatmentState.type === "capping") {
    const form = $("cappingForm");
    form.medicalPersonnelCategory.value = item.medicalPersonnelCategory;
    form.cappingLineFee.value = item.cappingLineFee;
    setKeyDisabled(form, true);
    $("cappingModalTitle").textContent = "编辑基金封顶线";
    $("cappingModal").classList.remove("hidden");
  } else if (treatmentState.type === "minimum") {
    const form = $("minimumForm");
    Object.keys(item).forEach((key) => {
      if (form.elements[key]) form.elements[key].value = item[key];
    });
    setKeyDisabled(form, true);
    $("minimumModalTitle").textContent = "编辑起付标准";
    $("minimumModal").classList.remove("hidden");
  } else {
    const form = $("segmentForm");
    Object.keys(item).forEach((key) => {
      if (form.elements[key]) form.elements[key].value = item[key];
    });
    form.reimbursementPercent.value =
      Number(item.reimbursementProportion) * 100;
    setKeyDisabled(form, true);
    $("segmentModalTitle").textContent = "编辑个人分段自费比例";
    $("segmentModal").classList.remove("hidden");
  }
}
async function deleteTreatment(item) {
  if (!confirm("确定删除该待遇参数吗？")) return;
  if (treatmentState.type === "capping") {
    await api(
      `/api/treatment-parameters/capping-lines/${encodeURIComponent(item.medicalPersonnelCategory)}`,
      { method: "DELETE" },
    );
  } else if (treatmentState.type === "minimum") {
    const q = new URLSearchParams({
      medicalCategory: item.medicalCategory,
      medicalPersonnelCategory: item.medicalPersonnelCategory,
      hospitalLevel: item.hospitalLevel,
    });
    await api(`/api/treatment-parameters/minimum-payment-standards?${q}`, {
      method: "DELETE",
    });
  } else {
    const q = new URLSearchParams({
      medicalCategory: item.medicalCategory,
      medicalPersonnelCategory: item.medicalPersonnelCategory,
      hospitalLevel: item.hospitalLevel,
      minimumAmount: item.minimumAmount,
      maximumAmount: item.maximumAmount,
    });
    await api(`/api/treatment-parameters/segment-ratios?${q}`, {
      method: "DELETE",
    });
  }
  toast("删除成功");
  loadTreatmentParameters();
}
["cappingBody", "minimumBody", "segmentBody"].forEach((id) =>
  $(id).addEventListener("click", (event) => {
    const button = event.target.closest("button[data-key]");
    if (!button) return;
    const item = decodeTreatmentKey(button.dataset.key);
    if (button.classList.contains("treatment-edit")) editTreatment(item);
    if (button.classList.contains("treatment-delete"))
      deleteTreatment(item).catch((error) => alert(error.message));
  }),
);
$("cappingForm").addEventListener("submit", async (event) => {
  event.preventDefault();
  const values = Object.fromEntries(new FormData(event.target));
  if (treatmentState.editing)
    values.medicalPersonnelCategory =
      treatmentState.editing.medicalPersonnelCategory;
  values.cappingLineFee = Number(values.cappingLineFee);
  try {
    await api(
      treatmentState.editing
        ? `/api/treatment-parameters/capping-lines/${encodeURIComponent(values.medicalPersonnelCategory)}`
        : "/api/treatment-parameters/capping-lines",
      {
        method: treatmentState.editing ? "PUT" : "POST",
        body: JSON.stringify(values),
      },
    );
    closeTreatmentModal("capping");
    toast("保存成功");
    loadTreatmentParameters();
  } catch (error) {
    alert(error.message);
  }
});
$("minimumForm").addEventListener("submit", async (event) => {
  event.preventDefault();
  const values = Object.fromEntries(new FormData(event.target));
  if (treatmentState.editing)
    Object.assign(values, {
      medicalCategory: treatmentState.editing.medicalCategory,
      medicalPersonnelCategory: treatmentState.editing.medicalPersonnelCategory,
      hospitalLevel: treatmentState.editing.hospitalLevel,
    });
  values.minimumPaymentStandard = Number(values.minimumPaymentStandard);
  try {
    await api("/api/treatment-parameters/minimum-payment-standards", {
      method: treatmentState.editing ? "PUT" : "POST",
      body: JSON.stringify(values),
    });
    closeTreatmentModal("minimum");
    toast("保存成功");
    loadTreatmentParameters();
  } catch (error) {
    alert(error.message);
  }
});
$("segmentForm").addEventListener("submit", async (event) => {
  event.preventDefault();
  const values = Object.fromEntries(new FormData(event.target));
  if (treatmentState.editing)
    Object.assign(values, {
      medicalCategory: treatmentState.editing.medicalCategory,
      medicalPersonnelCategory: treatmentState.editing.medicalPersonnelCategory,
      hospitalLevel: treatmentState.editing.hospitalLevel,
      minimumAmount: treatmentState.editing.minimumAmount,
      maximumAmount: treatmentState.editing.maximumAmount,
    });
  values.minimumAmount = Number(values.minimumAmount);
  values.maximumAmount = Number(values.maximumAmount);
  values.reimbursementProportion = Number(values.reimbursementPercent) / 100;
  delete values.reimbursementPercent;
  try {
    await api("/api/treatment-parameters/segment-ratios", {
      method: treatmentState.editing ? "PUT" : "POST",
      body: JSON.stringify(values),
    });
    closeTreatmentModal("segment");
    toast("保存成功");
    loadTreatmentParameters();
  } catch (error) {
    alert(error.message);
  }
});

async function loadInstitutionApprovals() {
  const keyword = encodeURIComponent(
    $("institutionApprovalKeyword").value.trim(),
  );
  const rows = await api(`/api/approvals/institutions?keyword=${keyword}`);
  $("institutionApprovalBody").innerHTML = rows.length
    ? rows
        .map(
          (item) =>
            `<tr><td>${escapeHtml(item.approvalNumber)}</td><td>${escapeHtml(item.personName || "")}（${escapeHtml(item.personId)}）</td><td>${escapeHtml(item.companyName)}</td><td>${escapeHtml(item.medicalInstitutionName || item.medicalInstitutionCode)}</td><td>${escapeHtml(item.startDate || "不限")} 至 ${escapeHtml(item.terminationDate || "不限")}</td><td><span class="tag">${escapeHtml(item.approvalFlag)}</span></td><td>${escapeHtml(item.approver)}</td><td><button class="action-btn institution-approval-edit" data-key="${treatmentKey(item)}">编辑</button><button class="action-btn delete institution-approval-delete" data-key="${treatmentKey(item)}">删除</button></td></tr>`,
        )
        .join("")
    : '<tr><td colspan="8" class="empty">暂无人员就诊机构审批记录</td></tr>';
  paginateClientTable("institutionApproval", "institutionApprovalBody");
}
$("searchInstitutionApprovalButton").onclick = () => {
  resetTablePager("institutionApproval");
  loadInstitutionApprovals();
};
$("resetInstitutionApprovalButton").onclick = () => {
  $("institutionApprovalKeyword").value = "";
  resetTablePager("institutionApproval");
  loadInstitutionApprovals();
};
$("institutionApprovalKeyword").addEventListener("keydown", (event) => {
  if (event.key === "Enter") $("searchInstitutionApprovalButton").click();
});
function openInstitutionApproval(item) {
  approvalState.institutionEditing = item || null;
  const form = $("institutionApprovalForm");
  form.reset();
  form.approvalCategory.value = "人员就诊机构审批";
  form.approvalFlag.value = "审批通过";
  if (item)
    Object.keys(item).forEach((key) => {
      if (form.elements[key])
        form.elements[key].value =
          key === "approvalDate" ? toDateInput(item[key]) : (item[key] ?? "");
    });
  form.approvalNumber.disabled = !!item;
  $("institutionApprovalModalTitle").textContent = item
    ? "编辑人员就诊机构审批"
    : "新增人员就诊机构审批";
  $("institutionApprovalModal").classList.remove("hidden");
}
function closeInstitutionApproval() {
  approvalState.institutionEditing = null;
  $("institutionApprovalModal").classList.add("hidden");
}
$("addInstitutionApprovalButton").onclick = () => openInstitutionApproval(null);
$("closeInstitutionApprovalModal").onclick = $(
  "cancelInstitutionApprovalModal",
).onclick = closeInstitutionApproval;
$("institutionApprovalBody").addEventListener("click", (event) => {
  const button = event.target.closest("button[data-key]");
  if (!button) return;
  const item = decodeTreatmentKey(button.dataset.key);
  if (button.classList.contains("institution-approval-edit"))
    openInstitutionApproval(item);
  if (
    button.classList.contains("institution-approval-delete") &&
    confirm("确定删除该审批记录吗？")
  )
    api(
      `/api/approvals/institutions/${encodeURIComponent(item.approvalNumber)}`,
      { method: "DELETE" },
    )
      .then(() => {
        toast("删除成功");
        loadInstitutionApprovals();
      })
      .catch((error) => alert(error.message));
});
$("institutionApprovalForm").addEventListener("submit", async (event) => {
  event.preventDefault();
  const values = Object.fromEntries(new FormData(event.target));
  if (approvalState.institutionEditing)
    values.approvalNumber = approvalState.institutionEditing.approvalNumber;
  try {
    await api(
      approvalState.institutionEditing
        ? `/api/approvals/institutions/${encodeURIComponent(values.approvalNumber)}`
        : "/api/approvals/institutions",
      {
        method: approvalState.institutionEditing ? "PUT" : "POST",
        body: JSON.stringify(values),
      },
    );
    closeInstitutionApproval();
    toast("保存成功");
    loadInstitutionApprovals();
  } catch (error) {
    alert(error.message);
  }
});

async function loadSpecialApprovals() {
  const keyword = encodeURIComponent($("specialApprovalKeyword").value.trim());
  const rows = await api(`/api/approvals/special?keyword=${keyword}`);
  $("specialApprovalBody").innerHTML = rows.length
    ? rows
        .map(
          (item) =>
            `<tr><td>${escapeHtml(item.approvalNumber)}</td><td>${escapeHtml(item.personName || "")}（${escapeHtml(item.personId)}）</td><td>${escapeHtml(item.companyName)}</td><td>${item.itemType === "DIAGNOSIS" ? "诊疗项目" : "药品"}</td><td>${escapeHtml(item.projectName || item.projectCode)}（${escapeHtml(item.projectCode)}）</td><td>${escapeHtml(item.startDate || "不限")} 至 ${escapeHtml(item.terminationDate || "不限")}</td><td><span class="tag">${escapeHtml(item.approvalFlag)}</span></td><td>${escapeHtml(item.approver)}</td><td><button class="action-btn special-approval-edit" data-key="${treatmentKey(item)}">编辑</button><button class="action-btn delete special-approval-delete" data-key="${treatmentKey(item)}">删除</button></td></tr>`,
        )
        .join("")
    : '<tr><td colspan="9" class="empty">暂无特检特治审批记录</td></tr>';
  paginateClientTable("specialApproval", "specialApprovalBody");
}
$("searchSpecialApprovalButton").onclick = () => {
  resetTablePager("specialApproval");
  loadSpecialApprovals();
};
$("resetSpecialApprovalButton").onclick = () => {
  $("specialApprovalKeyword").value = "";
  resetTablePager("specialApproval");
  loadSpecialApprovals();
};
$("specialApprovalKeyword").addEventListener("keydown", (event) => {
  if (event.key === "Enter") $("searchSpecialApprovalButton").click();
});
async function loadSpecialProjectOptions(type, preserveCode = false) {
  const form = $("specialApprovalForm");
  const previous = preserveCode ? form.projectCode.value : "";
  approvalState.specialProjects = await api(
    `/api/approvals/special/projects?itemType=${encodeURIComponent(type)}`,
  );
  $("specialProjectOptions").innerHTML = approvalState.specialProjects
    .map(
      (item) =>
        `<option value="${escapeHtml(item.projectCode)}" label="${escapeHtml(catalogOptionLabel(item))}"></option>`,
    )
    .join("");
  form.projectCode.value = previous;
  form.projectCode.setCustomValidity(
    previous &&
      !approvalState.specialProjects.some(
        (item) => item.projectCode === previous,
      )
      ? "所选项目不属于当前类别或不需要审批"
      : "",
  );
  refreshClearButtons(form);
}
$("specialApprovalForm").elements.itemType.onchange = async (event) => {
  const form = $("specialApprovalForm");
  form.projectCode.value = "";
  form.projectCode.setCustomValidity("");
  await loadSpecialProjectOptions(event.target.value);
  form.projectCode.focus();
};
$("specialApprovalForm").elements.projectCode.onchange = (event) => {
  const item = approvalState.specialProjects.find(
    (row) => row.projectCode === event.target.value,
  );
  event.target.setCustomValidity(
    event.target.value && !item ? "请选择当前类别中确实需要审批的项目" : "",
  );
  refreshClearButtons($("specialApprovalForm"));
};
async function openSpecialApproval(item) {
  approvalState.specialEditing = item || null;
  const form = $("specialApprovalForm");
  form.reset();
  form.approvalCategory.value = "特检特治审批";
  form.approvalFlag.value = "审批通过";
  if (item)
    Object.keys(item).forEach((key) => {
      if (form.elements[key])
        form.elements[key].value =
          key === "approvalDate" ? toDateInput(item[key]) : (item[key] ?? "");
    });
  form.approvalNumber.disabled = !!item;
  await loadSpecialProjectOptions(form.itemType.value, !!item);
  refreshClearButtons(form);
  $("specialApprovalModalTitle").textContent = item
    ? "编辑特检特治审批"
    : "新增特检特治审批";
  $("specialApprovalModal").classList.remove("hidden");
}
function closeSpecialApproval() {
  approvalState.specialEditing = null;
  $("specialApprovalModal").classList.add("hidden");
}
$("addSpecialApprovalButton").onclick = () => openSpecialApproval(null);
$("closeSpecialApprovalModal").onclick = $(
  "cancelSpecialApprovalModal",
).onclick = closeSpecialApproval;
$("specialApprovalBody").addEventListener("click", (event) => {
  const button = event.target.closest("button[data-key]");
  if (!button) return;
  const item = decodeTreatmentKey(button.dataset.key);
  if (button.classList.contains("special-approval-edit"))
    openSpecialApproval(item);
  if (
    button.classList.contains("special-approval-delete") &&
    confirm("确定删除该审批记录吗？")
  )
    api(`/api/approvals/special/${encodeURIComponent(item.approvalNumber)}`, {
      method: "DELETE",
    })
      .then(() => {
        toast("删除成功");
        loadSpecialApprovals();
      })
      .catch((error) => alert(error.message));
});
$("specialApprovalForm").addEventListener("submit", async (event) => {
  event.preventDefault();
  const values = Object.fromEntries(new FormData(event.target));
  if (approvalState.specialEditing)
    values.approvalNumber = approvalState.specialEditing.approvalNumber;
  try {
    await api(
      approvalState.specialEditing
        ? `/api/approvals/special/${encodeURIComponent(values.approvalNumber)}`
        : "/api/approvals/special",
      {
        method: approvalState.specialEditing ? "PUT" : "POST",
        body: JSON.stringify(values),
      },
    );
    closeSpecialApproval();
    toast("保存成功");
    loadSpecialApprovals();
  } catch (error) {
    alert(error.message);
  }
});
