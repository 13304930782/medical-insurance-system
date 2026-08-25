/** 病种、定点医疗机构、单位和个人资料维护 */

async function loadDiseases() {
  const keyword = encodeURIComponent($("diseaseKeyword").value.trim());
  const data = await api(
    `/api/diseases?keyword=${keyword}&page=${diseaseState.page}&size=${diseaseState.size}`,
  );
  $("diseaseBody").innerHTML = data.items.length
    ? data.items
        .map(
          (item) =>
            `<tr><td>${escapeHtml(item.diseaseId)}</td><td>${escapeHtml(item.diseaseName)}</td><td>${escapeHtml(item.diseaseType)}</td><td>${escapeHtml(item.diseaseReimbursementStandards)}</td><td>${escapeHtml(item.notes)}</td><td><button class="action-btn disease-edit" data-id="${escapeHtml(item.diseaseId)}">编辑</button><button class="action-btn delete disease-delete" data-id="${escapeHtml(item.diseaseId)}" data-name="${escapeHtml(item.diseaseName)}">删除</button></td></tr>`,
        )
        .join("")
    : '<tr><td colspan="6" class="empty">暂无病种数据，可点击右上角新增</td></tr>';
  updateServerPager("disease", "diseaseBody", diseaseState, data, loadDiseases);
}
$("searchDiseaseButton").onclick = () => {
  diseaseState.page = 1;
  loadDiseases();
};
$("resetDiseaseButton").onclick = () => {
  $("diseaseKeyword").value = "";
  diseaseState.page = 1;
  loadDiseases();
};
$("diseaseKeyword").addEventListener("keydown", (event) => {
  if (event.key === "Enter") $("searchDiseaseButton").click();
});
$("diseasePrevButton").onclick = () => {
  if (diseaseState.page > 1) {
    diseaseState.page--;
    loadDiseases();
  }
};
$("diseaseNextButton").onclick = () => {
  if (diseaseState.page < diseaseState.totalPages) {
    diseaseState.page++;
    loadDiseases();
  }
};
function openDiseaseModal() {
  $("diseaseModal").classList.remove("hidden");
}
function closeDiseaseModal() {
  $("diseaseModal").classList.add("hidden");
  diseaseState.editingId = null;
}
$("closeDiseaseModal").onclick = $("cancelDiseaseModal").onclick =
  closeDiseaseModal;
$("addDiseaseButton").onclick = () => {
  diseaseState.editingId = null;
  $("diseaseModalTitle").textContent = "新增病种";
  const form = $("diseaseForm");
  form.reset();
  form.diseaseId.disabled = false;
  openDiseaseModal();
};
async function editDisease(id) {
  const item = await api(`/api/diseases/${encodeURIComponent(id)}`);
  diseaseState.editingId = id;
  $("diseaseModalTitle").textContent = "编辑病种";
  const form = $("diseaseForm");
  Object.keys(item).forEach((key) => {
    if (form.elements[key]) form.elements[key].value = item[key] ?? "";
  });
  form.diseaseId.disabled = true;
  openDiseaseModal();
}
async function deleteDisease(id, name) {
  if (!confirm(`确定删除“${name}”吗？`)) return;
  await api(`/api/diseases/${encodeURIComponent(id)}`, { method: "DELETE" });
  toast("删除成功");
  loadDiseases();
}
$("diseaseBody").addEventListener("click", (event) => {
  const button = event.target.closest("button[data-id]");
  if (!button) return;
  if (button.classList.contains("disease-edit")) editDisease(button.dataset.id);
  if (button.classList.contains("disease-delete"))
    deleteDisease(button.dataset.id, button.dataset.name);
});
$("diseaseForm").addEventListener("submit", async (event) => {
  event.preventDefault();
  const values = Object.fromEntries(new FormData(event.target));
  values.diseaseId = diseaseState.editingId || values.diseaseId;
  const url = diseaseState.editingId
    ? `/api/diseases/${encodeURIComponent(diseaseState.editingId)}`
    : "/api/diseases";
  try {
    await api(url, {
      method: diseaseState.editingId ? "PUT" : "POST",
      body: JSON.stringify(values),
    });
    closeDiseaseModal();
    toast("保存成功");
    loadDiseases();
  } catch (error) {
    alert(error.message);
  }
});

async function loadInstitutions() {
  const keyword = encodeURIComponent($("institutionKeyword").value.trim());
  const data = await api(
    `/api/institutions?keyword=${keyword}&page=${institutionState.page}&size=${institutionState.size}`,
  );
  $("institutionBody").innerHTML = data.items.length
    ? data.items
        .map(
          (item) =>
            `<tr><td>${escapeHtml(item.institutionId)}</td><td>${escapeHtml(item.institutionName)}</td><td>${escapeHtml(item.hospitalLevel)}</td><td>${escapeHtml(item.institutionType)}</td><td>${escapeHtml(item.contactName)}</td><td>${escapeHtml(item.contactPhone)}</td><td>${escapeHtml(item.address)}</td><td><button class="action-btn institution-edit" data-id="${escapeHtml(item.institutionId)}">编辑</button><button class="action-btn delete institution-delete" data-id="${escapeHtml(item.institutionId)}" data-name="${escapeHtml(item.institutionName)}">删除</button></td></tr>`,
        )
        .join("")
    : '<tr><td colspan="8" class="empty">暂无定点医疗机构数据，可点击右上角新增</td></tr>';
  updateServerPager(
    "institution",
    "institutionBody",
    institutionState,
    data,
    loadInstitutions,
  );
}
$("searchInstitutionButton").onclick = () => {
  institutionState.page = 1;
  loadInstitutions();
};
$("resetInstitutionButton").onclick = () => {
  $("institutionKeyword").value = "";
  institutionState.page = 1;
  loadInstitutions();
};
$("institutionKeyword").addEventListener("keydown", (event) => {
  if (event.key === "Enter") $("searchInstitutionButton").click();
});
$("institutionPrevButton").onclick = () => {
  if (institutionState.page > 1) {
    institutionState.page--;
    loadInstitutions();
  }
};
$("institutionNextButton").onclick = () => {
  if (institutionState.page < institutionState.totalPages) {
    institutionState.page++;
    loadInstitutions();
  }
};
function openInstitutionModal() {
  $("institutionModal").classList.remove("hidden");
}
function closeInstitutionModal() {
  $("institutionModal").classList.add("hidden");
  institutionState.editingId = null;
}
$("closeInstitutionModal").onclick = $("cancelInstitutionModal").onclick =
  closeInstitutionModal;
$("addInstitutionButton").onclick = () => {
  institutionState.editingId = null;
  $("institutionModalTitle").textContent = "新增定点医疗机构";
  const form = $("institutionForm");
  form.reset();
  form.institutionId.disabled = false;
  openInstitutionModal();
};
async function editInstitution(id) {
  const item = await api(`/api/institutions/${encodeURIComponent(id)}`);
  institutionState.editingId = id;
  $("institutionModalTitle").textContent = "编辑定点医疗机构";
  const form = $("institutionForm");
  Object.keys(item).forEach((key) => {
    if (form.elements[key]) form.elements[key].value = item[key] ?? "";
  });
  form.institutionId.disabled = true;
  openInstitutionModal();
}
async function deleteInstitution(id, name) {
  if (!confirm(`确定删除“${name}”吗？`)) return;
  await api(`/api/institutions/${encodeURIComponent(id)}`, {
    method: "DELETE",
  });
  toast("删除成功");
  loadInstitutions();
}
$("institutionBody").addEventListener("click", (event) => {
  const button = event.target.closest("button[data-id]");
  if (!button) return;
  if (button.classList.contains("institution-edit"))
    editInstitution(button.dataset.id);
  if (button.classList.contains("institution-delete"))
    deleteInstitution(button.dataset.id, button.dataset.name);
});
$("institutionForm").addEventListener("submit", async (event) => {
  event.preventDefault();
  const values = Object.fromEntries(new FormData(event.target));
  values.institutionId = institutionState.editingId || values.institutionId;
  const url = institutionState.editingId
    ? `/api/institutions/${encodeURIComponent(institutionState.editingId)}`
    : "/api/institutions";
  try {
    await api(url, {
      method: institutionState.editingId ? "PUT" : "POST",
      body: JSON.stringify(values),
    });
    closeInstitutionModal();
    toast("保存成功");
    loadInstitutions();
  } catch (error) {
    alert(error.message);
  }
});

async function loadCompanies() {
  const keyword = encodeURIComponent($("companyKeyword").value.trim());
  const data = await api(
    `/api/companies?keyword=${keyword}&page=${companyState.page}&size=${companyState.size}`,
  );
  $("companyBody").innerHTML = data.items.length
    ? data.items
        .map(
          (item) =>
            `<tr><td>${escapeHtml(item.companyId)}</td><td>${escapeHtml(item.companyName)}</td><td>${escapeHtml(item.companyType)}</td><td>${escapeHtml(item.address)}</td><td>${escapeHtml(item.postcode)}</td><td>${escapeHtml(item.phoneNumber)}</td><td><button class="action-btn company-edit" data-id="${escapeHtml(item.companyId)}">编辑</button><button class="action-btn delete company-delete" data-id="${escapeHtml(item.companyId)}" data-name="${escapeHtml(item.companyName)}">删除</button></td></tr>`,
        )
        .join("")
    : '<tr><td colspan="7" class="empty">暂无单位数据，可点击右上角新增</td></tr>';
  updateServerPager(
    "company",
    "companyBody",
    companyState,
    data,
    loadCompanies,
  );
}
$("searchCompanyButton").onclick = () => {
  companyState.page = 1;
  loadCompanies();
};
$("resetCompanyButton").onclick = () => {
  $("companyKeyword").value = "";
  companyState.page = 1;
  loadCompanies();
};
$("companyKeyword").addEventListener("keydown", (event) => {
  if (event.key === "Enter") $("searchCompanyButton").click();
});
$("companyPrevButton").onclick = () => {
  if (companyState.page > 1) {
    companyState.page--;
    loadCompanies();
  }
};
$("companyNextButton").onclick = () => {
  if (companyState.page < companyState.totalPages) {
    companyState.page++;
    loadCompanies();
  }
};
function openCompanyModal() {
  $("companyModal").classList.remove("hidden");
}
function closeCompanyModal() {
  $("companyModal").classList.add("hidden");
  companyState.editingId = null;
}
$("closeCompanyModal").onclick = $("cancelCompanyModal").onclick =
  closeCompanyModal;
$("addCompanyButton").onclick = () => {
  companyState.editingId = null;
  $("companyModalTitle").textContent = "新增单位";
  const form = $("companyForm");
  form.reset();
  form.companyId.disabled = false;
  openCompanyModal();
};
async function editCompany(id) {
  const item = await api(`/api/companies/${encodeURIComponent(id)}`);
  companyState.editingId = id;
  $("companyModalTitle").textContent = "编辑单位";
  const form = $("companyForm");
  Object.keys(item).forEach((key) => {
    if (form.elements[key]) form.elements[key].value = item[key] ?? "";
  });
  form.companyId.disabled = true;
  openCompanyModal();
}
async function deleteCompany(id, name) {
  if (!confirm(`确定删除“${name}”吗？`)) return;
  await api(`/api/companies/${encodeURIComponent(id)}`, { method: "DELETE" });
  toast("删除成功");
  loadCompanies();
}
$("companyBody").addEventListener("click", (event) => {
  const button = event.target.closest("button[data-id]");
  if (!button) return;
  if (button.classList.contains("company-edit")) editCompany(button.dataset.id);
  if (button.classList.contains("company-delete"))
    deleteCompany(button.dataset.id, button.dataset.name);
});
$("companyForm").addEventListener("submit", async (event) => {
  event.preventDefault();
  const values = Object.fromEntries(new FormData(event.target));
  values.companyId = companyState.editingId || values.companyId;
  const url = companyState.editingId
    ? `/api/companies/${encodeURIComponent(companyState.editingId)}`
    : "/api/companies";
  try {
    await api(url, {
      method: companyState.editingId ? "PUT" : "POST",
      body: JSON.stringify(values),
    });
    closeCompanyModal();
    toast("保存成功");
    loadCompanies();
  } catch (error) {
    alert(error.message);
  }
});

async function loadPeople() {
  const keyword = encodeURIComponent($("personKeyword").value.trim());
  const data = await api(
    `/api/people?keyword=${keyword}&page=${personState.page}&size=${personState.size}`,
  );
  $("personBody").innerHTML = data.items.length
    ? data.items
        .map(
          (item) =>
            `<tr><td>${escapeHtml(item.peopleId)}</td><td>${escapeHtml(item.name)}</td><td>${escapeHtml(item.sex)}</td><td>${escapeHtml(item.idType)}</td><td>${escapeHtml(item.id)}</td><td>${escapeHtml(item.companyName || item.companyId)}</td><td>${escapeHtml(item.medicalPersonnel)}</td><td>${escapeHtml(item.socialSecurityId)}</td><td>${escapeHtml(item.medinsName || item.medinsId)}</td><td><button class="action-btn person-edit" data-id="${escapeHtml(item.peopleId)}">编辑</button><button class="action-btn delete person-delete" data-id="${escapeHtml(item.peopleId)}" data-name="${escapeHtml(item.name)}">删除</button></td></tr>`,
        )
        .join("")
    : '<tr><td colspan="10" class="empty">暂无个人数据，可点击右上角新增</td></tr>';
  updateServerPager("person", "personBody", personState, data, loadPeople);
}
$("searchPersonButton").onclick = () => {
  personState.page = 1;
  loadPeople();
};
$("resetPersonButton").onclick = () => {
  $("personKeyword").value = "";
  personState.page = 1;
  loadPeople();
};
$("personKeyword").addEventListener("keydown", (event) => {
  if (event.key === "Enter") $("searchPersonButton").click();
});
$("personPrevButton").onclick = () => {
  if (personState.page > 1) {
    personState.page--;
    loadPeople();
  }
};
$("personNextButton").onclick = () => {
  if (personState.page < personState.totalPages) {
    personState.page++;
    loadPeople();
  }
};
function openPersonModal() {
  $("personModal").classList.remove("hidden");
}
function closePersonModal() {
  $("personModal").classList.add("hidden");
  personState.editingId = null;
}
$("closePersonModal").onclick = $("cancelPersonModal").onclick =
  closePersonModal;
$("addPersonButton").onclick = () => {
  personState.editingId = null;
  $("personModalTitle").textContent = "新增个人";
  const form = $("personForm");
  form.reset();
  form.peopleId.disabled = false;
  openPersonModal();
};
async function editPerson(id) {
  const item = await api(`/api/people/${encodeURIComponent(id)}`);
  personState.editingId = id;
  $("personModalTitle").textContent = "编辑个人";
  const form = $("personForm");
  Object.keys(item).forEach((key) => {
    if (form.elements[key]) form.elements[key].value = item[key] ?? "";
  });
  form.peopleId.disabled = true;
  openPersonModal();
}
async function deletePerson(id, name) {
  if (!confirm(`确定删除“${name}”吗？`)) return;
  await api(`/api/people/${encodeURIComponent(id)}`, { method: "DELETE" });
  toast("删除成功");
  loadPeople();
}
$("personBody").addEventListener("click", (event) => {
  const button = event.target.closest("button[data-id]");
  if (!button) return;
  if (button.classList.contains("person-edit")) editPerson(button.dataset.id);
  if (button.classList.contains("person-delete"))
    deletePerson(button.dataset.id, button.dataset.name);
});
$("personForm").addEventListener("submit", async (event) => {
  event.preventDefault();
  const values = Object.fromEntries(new FormData(event.target));
  values.peopleId = personState.editingId || values.peopleId;
  const url = personState.editingId
    ? `/api/people/${encodeURIComponent(personState.editingId)}`
    : "/api/people";
  try {
    await api(url, {
      method: personState.editingId ? "PUT" : "POST",
      body: JSON.stringify(values),
    });
    closePersonModal();
    toast("保存成功");
    loadPeople();
  } catch (error) {
    alert(error.message);
  }
});
