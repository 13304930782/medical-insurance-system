/** 药品、诊疗项目和服务设施目录维护 */

async function loadMedicines() {
  const keyword = encodeURIComponent($("medicineKeyword").value.trim());
  const data = await api(
    `/api/medicines?keyword=${keyword}&page=${medicineState.page}&size=${medicineState.size}`,
  );
  $("medicineBody").innerHTML = data.items.length
    ? data.items
        .map(
          (item) =>
            `<tr><td>${escapeHtml(item.medId)}</td><td>${escapeHtml(item.medName)}</td><td>${escapeHtml(item.medExpType)}</td><td>${escapeHtml(item.medExpLevel)}</td><td>${Number(item.medMaxPrize || 0).toFixed(2)}</td><td>${escapeHtml(item.medSize)}</td><td>${escapeHtml(item.medMeasurement)}</td><td><span class="tag">${escapeHtml(item.medValid || "")}</span></td><td><button class="action-btn medicine-edit" data-id="${escapeHtml(item.medId)}">编辑</button><button class="action-btn delete medicine-delete" data-id="${escapeHtml(item.medId)}" data-name="${escapeHtml(item.medName)}">删除</button></td></tr>`,
        )
        .join("")
    : '<tr><td colspan="9" class="empty">没有找到药品数据</td></tr>';
  updateServerPager(
    "medicine",
    "medicineBody",
    medicineState,
    data,
    loadMedicines,
  );
}
$("searchMedicineButton").onclick = () => {
  medicineState.page = 1;
  loadMedicines();
};
$("resetMedicineButton").onclick = () => {
  $("medicineKeyword").value = "";
  medicineState.page = 1;
  loadMedicines();
};
$("medicineKeyword").addEventListener("keydown", (event) => {
  if (event.key === "Enter") $("searchMedicineButton").click();
});
$("medicinePrevButton").onclick = () => {
  if (medicineState.page > 1) {
    medicineState.page--;
    loadMedicines();
  }
};
$("medicineNextButton").onclick = () => {
  if (medicineState.page < medicineState.totalPages) {
    medicineState.page++;
    loadMedicines();
  }
};
function openMedicineModal() {
  $("medicineModal").classList.remove("hidden");
}
function closeMedicineModal() {
  $("medicineModal").classList.add("hidden");
  medicineState.editingId = null;
}
$("closeMedicineModal").onclick = $("cancelMedicineModal").onclick =
  closeMedicineModal;
$("addMedicineButton").onclick = () => {
  medicineState.editingId = null;
  $("medicineModalTitle").textContent = "新增药品";
  const form = $("medicineForm");
  form.reset();
  form.medId.disabled = false;
  form.medMaxPrize.value = "0";
  form.medApprovalmark.value = "不需要审批";
  form.medHosLevel.value = "所有医院";
  form.medValid.value = "有效";
  openMedicineModal();
};
async function editMedicine(id) {
  const item = await api(`/api/medicines/${encodeURIComponent(id)}`);
  medicineState.editingId = id;
  $("medicineModalTitle").textContent = "编辑药品";
  const form = $("medicineForm");
  Object.keys(item).forEach((key) => {
    if (form.elements[key]) form.elements[key].value = item[key] ?? "";
  });
  form.medStarttime.value = toDateInput(item.medStarttime);
  form.medEndtime.value = toDateInput(item.medEndtime);
  form.medId.disabled = true;
  openMedicineModal();
}
function medicineSpecialRequiresApproval(value) {
  return ["是", "特检特治", "需要审批", "1"].includes(
    String(value || "").trim(),
  );
}
$("medicineForm").elements.medSpecialmark.addEventListener(
  "change",
  (event) => {
    if (medicineSpecialRequiresApproval(event.target.value))
      $("medicineForm").elements.medApprovalmark.value = "需要审批";
  },
);
$("medicineForm").elements.medApprovalmark.addEventListener(
  "change",
  (event) => {
    const form = $("medicineForm");
    if (
      event.target.value === "不需要审批" &&
      medicineSpecialRequiresApproval(form.medSpecialmark.value)
    )
      form.medSpecialmark.value = "否";
  },
);
async function deleteMedicine(id, name) {
  if (!confirm(`确定删除“${name}”吗？`)) return;
  await api(`/api/medicines/${encodeURIComponent(id)}`, { method: "DELETE" });
  toast("删除成功");
  loadMedicines();
}
$("medicineBody").addEventListener("click", (event) => {
  const button = event.target.closest("button[data-id]");
  if (!button) return;
  if (button.classList.contains("medicine-edit"))
    editMedicine(button.dataset.id);
  if (button.classList.contains("medicine-delete"))
    deleteMedicine(button.dataset.id, button.dataset.name);
});
$("medicineForm").addEventListener("submit", async (event) => {
  event.preventDefault();
  const values = Object.fromEntries(new FormData(event.target));
  values.medId = medicineState.editingId || values.medId;
  values.medMaxPrize = Number(values.medMaxPrize || 0);
  const url = medicineState.editingId
    ? `/api/medicines/${encodeURIComponent(medicineState.editingId)}`
    : "/api/medicines";
  try {
    await api(url, {
      method: medicineState.editingId ? "PUT" : "POST",
      body: JSON.stringify(values),
    });
    closeMedicineModal();
    toast("保存成功");
    loadMedicines();
  } catch (error) {
    alert(error.message);
  }
});

async function loadDiagnoses() {
  const keyword = encodeURIComponent($("diagnosisKeyword").value.trim());
  const data = await api(
    `/api/diagnoses?keyword=${keyword}&page=${diagnosisState.page}&size=${diagnosisState.size}`,
  );
  $("diagnosisBody").innerHTML = data.items.length
    ? data.items
        .map(
          (item) =>
            `<tr><td>${escapeHtml(item.diaId)}</td><td>${escapeHtml(item.diaName)}</td><td>${escapeHtml(item.diaExpType)}</td><td>${escapeHtml(item.diaExpLevel)}</td><td>${Number(item.diaMaxPrize || 0).toFixed(2)}</td><td>${escapeHtml(item.diaHosLevel)}</td><td>${escapeHtml(item.diaApprovalmark)}</td><td><span class="tag">${escapeHtml(item.diaValid || "")}</span></td><td><button class="action-btn diagnosis-edit" data-id="${escapeHtml(item.diaId)}">编辑</button><button class="action-btn delete diagnosis-delete" data-id="${escapeHtml(item.diaId)}" data-name="${escapeHtml(item.diaName)}">删除</button></td></tr>`,
        )
        .join("")
    : '<tr><td colspan="9" class="empty">没有找到诊疗项目数据</td></tr>';
  updateServerPager(
    "diagnosis",
    "diagnosisBody",
    diagnosisState,
    data,
    loadDiagnoses,
  );
}
$("searchDiagnosisButton").onclick = () => {
  diagnosisState.page = 1;
  loadDiagnoses();
};
$("resetDiagnosisButton").onclick = () => {
  $("diagnosisKeyword").value = "";
  diagnosisState.page = 1;
  loadDiagnoses();
};
$("diagnosisKeyword").addEventListener("keydown", (event) => {
  if (event.key === "Enter") $("searchDiagnosisButton").click();
});
$("diagnosisPrevButton").onclick = () => {
  if (diagnosisState.page > 1) {
    diagnosisState.page--;
    loadDiagnoses();
  }
};
$("diagnosisNextButton").onclick = () => {
  if (diagnosisState.page < diagnosisState.totalPages) {
    diagnosisState.page++;
    loadDiagnoses();
  }
};
function openDiagnosisModal() {
  $("diagnosisModal").classList.remove("hidden");
}
function closeDiagnosisModal() {
  $("diagnosisModal").classList.add("hidden");
  diagnosisState.editingId = null;
}
$("closeDiagnosisModal").onclick = $("cancelDiagnosisModal").onclick =
  closeDiagnosisModal;
$("addDiagnosisButton").onclick = () => {
  diagnosisState.editingId = null;
  $("diagnosisModalTitle").textContent = "新增诊疗项目";
  const form = $("diagnosisForm");
  form.reset();
  form.diaId.disabled = false;
  form.diaMaxPrize.value = "0";
  form.diaApprovalmark.value = "不需要审批";
  form.diaHosLevel.value = "所有医院";
  form.diaValid.value = "有效";
  openDiagnosisModal();
};
async function editDiagnosis(id) {
  const item = await api(`/api/diagnoses/${encodeURIComponent(id)}`);
  diagnosisState.editingId = id;
  $("diagnosisModalTitle").textContent = "编辑诊疗项目";
  const form = $("diagnosisForm");
  Object.keys(item).forEach((key) => {
    if (form.elements[key]) form.elements[key].value = item[key] ?? "";
  });
  form.diaStarttime.value = toDateInput(item.diaStarttime);
  form.diaEndtime.value = toDateInput(item.diaEndtime);
  form.diaId.disabled = true;
  openDiagnosisModal();
}
function diagnosisTypeRequiresApproval(value) {
  return ["特殊检查费", "特殊治疗费", "特检费", "特治费"].includes(
    String(value || "").trim(),
  );
}
$("diagnosisForm").elements.diaExpType.addEventListener("change", (event) => {
  if (diagnosisTypeRequiresApproval(event.target.value))
    $("diagnosisForm").elements.diaApprovalmark.value = "需要审批";
});
async function deleteDiagnosis(id, name) {
  if (!confirm(`确定删除“${name}”吗？`)) return;
  await api(`/api/diagnoses/${encodeURIComponent(id)}`, { method: "DELETE" });
  toast("删除成功");
  loadDiagnoses();
}
$("diagnosisBody").addEventListener("click", (event) => {
  const button = event.target.closest("button[data-id]");
  if (!button) return;
  if (button.classList.contains("diagnosis-edit"))
    editDiagnosis(button.dataset.id);
  if (button.classList.contains("diagnosis-delete"))
    deleteDiagnosis(button.dataset.id, button.dataset.name);
});
$("diagnosisForm").addEventListener("submit", async (event) => {
  event.preventDefault();
  const values = Object.fromEntries(new FormData(event.target));
  values.diaId = diagnosisState.editingId || values.diaId;
  values.diaMaxPrize = Number(values.diaMaxPrize || 0);
  const url = diagnosisState.editingId
    ? `/api/diagnoses/${encodeURIComponent(diagnosisState.editingId)}`
    : "/api/diagnoses";
  try {
    await api(url, {
      method: diagnosisState.editingId ? "PUT" : "POST",
      body: JSON.stringify(values),
    });
    closeDiagnosisModal();
    toast("保存成功");
    loadDiagnoses();
  } catch (error) {
    alert(error.message);
  }
});

async function loadFacilities() {
  const keyword = encodeURIComponent($("facilityKeyword").value.trim());
  const data = await api(
    `/api/facilities?keyword=${keyword}&page=${facilityState.page}&size=${facilityState.size}`,
  );
  $("facilityBody").innerHTML = data.items.length
    ? data.items
        .map(
          (item) =>
            `<tr><td>${escapeHtml(item.serId)}</td><td>${escapeHtml(item.serName)}</td><td>${escapeHtml(item.serExpType)}</td><td>${escapeHtml(toDateDisplay(item.serStarttime))}</td><td>${escapeHtml(toDateDisplay(item.serEndtime))}</td><td><span class="tag">${escapeHtml(item.serValid || "")}</span></td><td><button class="action-btn facility-edit" data-id="${escapeHtml(item.serId)}">编辑</button><button class="action-btn delete facility-delete" data-id="${escapeHtml(item.serId)}" data-name="${escapeHtml(item.serName)}">删除</button></td></tr>`,
        )
        .join("")
    : '<tr><td colspan="7" class="empty">没有找到服务设施数据</td></tr>';
  updateServerPager(
    "facility",
    "facilityBody",
    facilityState,
    data,
    loadFacilities,
  );
}
$("searchFacilityButton").onclick = () => {
  facilityState.page = 1;
  loadFacilities();
};
$("resetFacilityButton").onclick = () => {
  $("facilityKeyword").value = "";
  facilityState.page = 1;
  loadFacilities();
};
$("facilityKeyword").addEventListener("keydown", (event) => {
  if (event.key === "Enter") $("searchFacilityButton").click();
});
$("facilityPrevButton").onclick = () => {
  if (facilityState.page > 1) {
    facilityState.page--;
    loadFacilities();
  }
};
$("facilityNextButton").onclick = () => {
  if (facilityState.page < facilityState.totalPages) {
    facilityState.page++;
    loadFacilities();
  }
};
function openFacilityModal() {
  $("facilityModal").classList.remove("hidden");
}
function closeFacilityModal() {
  $("facilityModal").classList.add("hidden");
  facilityState.editingId = null;
}
$("closeFacilityModal").onclick = $("cancelFacilityModal").onclick =
  closeFacilityModal;
$("addFacilityButton").onclick = () => {
  facilityState.editingId = null;
  $("facilityModalTitle").textContent = "新增服务设施";
  const form = $("facilityForm");
  form.reset();
  form.serId.disabled = false;
  form.serValid.value = "有效";
  openFacilityModal();
};
async function editFacility(id) {
  const item = await api(`/api/facilities/${encodeURIComponent(id)}`);
  facilityState.editingId = id;
  $("facilityModalTitle").textContent = "编辑服务设施";
  const form = $("facilityForm");
  Object.keys(item).forEach((key) => {
    if (form.elements[key]) form.elements[key].value = item[key] ?? "";
  });
  form.serStarttime.value = toDateInput(item.serStarttime);
  form.serEndtime.value = toDateInput(item.serEndtime);
  form.serId.disabled = true;
  openFacilityModal();
}
async function deleteFacility(id, name) {
  if (!confirm(`确定删除“${name}”吗？`)) return;
  await api(`/api/facilities/${encodeURIComponent(id)}`, { method: "DELETE" });
  toast("删除成功");
  loadFacilities();
}
$("facilityBody").addEventListener("click", (event) => {
  const button = event.target.closest("button[data-id]");
  if (!button) return;
  if (button.classList.contains("facility-edit"))
    editFacility(button.dataset.id);
  if (button.classList.contains("facility-delete"))
    deleteFacility(button.dataset.id, button.dataset.name);
});
$("facilityForm").addEventListener("submit", async (event) => {
  event.preventDefault();
  const values = Object.fromEntries(new FormData(event.target));
  values.serId = facilityState.editingId || values.serId;
  const url = facilityState.editingId
    ? `/api/facilities/${encodeURIComponent(facilityState.editingId)}`
    : "/api/facilities";
  try {
    await api(url, {
      method: facilityState.editingId ? "PUT" : "POST",
      body: JSON.stringify(values),
    });
    closeFacilityModal();
    toast("保存成功");
    loadFacilities();
  } catch (error) {
    alert(error.message);
  }
});
