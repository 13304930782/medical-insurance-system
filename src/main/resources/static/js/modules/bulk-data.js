/** 批量模板、XLS/XLSX 导入导出和安全删除 */

async function loadBulkModules() {
  if (!bulkState.modules.length) {
    bulkState.modules = await api("/api/bulk/modules");
    $("bulkModuleSelect").innerHTML = bulkState.modules
      .map(
        (item) =>
          `<option value="${escapeHtml(item.code)}">${escapeHtml(item.label)}（${escapeHtml(item.table)}）</option>`,
      )
      .join("");
  }
  renderBulkModule();
}
function selectedBulkModule() {
  return bulkState.modules.find(
    (item) => item.code === $("bulkModuleSelect").value,
  );
}
function renderBulkModule() {
  const item = selectedBulkModule();
  if (!item) return;
  $("bulkModuleInfo").textContent =
    `原表：${item.table}｜主键顺序：${item.primaryKeys.join(" | ")}｜${item.importable ? "允许导入" : "只读导出"}｜${item.deletable ? "允许安全批量删除" : "删除须走对应业务页面"}`;
  $("bulkImportButton").disabled = !item.importable;
  $("bulkDeletePanel").classList.toggle("hidden", !item.deletable);
  $("bulkDeleteKeyLabel").textContent = `主键：${item.primaryKeys.join(" | ")}`;
  $("bulkDeleteKeys").value = "";
  $("bulkResult").textContent = "";
}
async function downloadBulkFile(path, filename) {
  const response = await fetch(path);
  if (response.status === 401) {
    showLogin();
    throw new Error("请先登录");
  }
  if (!response.ok) {
    let result = {};
    try {
      result = await response.json();
    } catch (e) {}
    throw new Error(result.message || "下载失败");
  }
  const url = URL.createObjectURL(await response.blob());
  const link = document.createElement("a");
  link.href = url;
  link.download = filename;
  document.body.appendChild(link);
  link.click();
  link.remove();
  URL.revokeObjectURL(url);
}
$("bulkModuleSelect").onchange = renderBulkModule;
$("bulkTemplateButton").onclick = () => {
  const item = selectedBulkModule();
  downloadBulkFile(
    `/api/bulk/${encodeURIComponent(item.code)}/template.xlsx`,
    `${item.code}-template.xlsx`,
  ).catch((error) => alert(error.message));
};
$("bulkExportButton").onclick = () => {
  const item = selectedBulkModule();
  downloadBulkFile(
    `/api/bulk/${encodeURIComponent(item.code)}/export.xlsx`,
    `${item.code}-export.xlsx`,
  ).catch((error) => alert(error.message));
};
$("bulkImportButton").onclick = async () => {
  const item = selectedBulkModule(),
    file = $("bulkFile").files[0];
  if (!file) return alert("请先选择.xls或.xlsx文件");
  if (
    $("bulkMode").value !== "VALIDATE_ONLY" &&
    !confirm(
      `确认以“${$("bulkMode").selectedOptions[0].textContent}”模式导入 ${item.label} 吗？`,
    )
  )
    return;
  const data = new FormData();
  data.append("file", file);
  try {
    $("bulkResult").textContent = "正在处理，请稍候……";
    const response = await fetch(
      `/api/bulk/${encodeURIComponent(item.code)}/import?mode=${encodeURIComponent($("bulkMode").value)}`,
      { method: "POST", body: data },
    );
    const result = await response.json();
    if (!response.ok) throw new Error(result.message || "导入失败");
    const value = result.data;
    $("bulkResult").textContent =
      `任务 ${value.jobNo}：共 ${value.totalRows} 行，成功 ${value.successRows} 行，失败 ${value.failureRows} 行${value.errors.length ? `。首个错误：第 ${value.errors[0].rowNumber} 行，${value.errors[0].message}` : ""}`;
    toast("批量任务已完成");
  } catch (error) {
    $("bulkResult").textContent = error.message;
    alert(error.message);
  }
};
$("bulkDeleteButton").onclick = async () => {
  const item = selectedBulkModule(),
    lines = $("bulkDeleteKeys")
      .value.split(/\r?\n/)
      .map((value) => value.trim())
      .filter(Boolean);
  if (!lines.length) return alert("请填写要删除的主键");
  const keys = [];
  for (const line of lines) {
    const parts = line.split("|").map((value) => value.trim());
    if (parts.length !== item.primaryKeys.length)
      return alert(`主键列数不正确：${line}`);
    keys.push(
      Object.fromEntries(
        item.primaryKeys.map((key, index) => [key, parts[index]]),
      ),
    );
  }
  const expected = `DELETE ${keys.length}`,
    confirmation = prompt(
      `即将删除 ${keys.length} 行 ${item.label} 数据。\n请输入 ${expected} 继续：`,
    );
  if (confirmation === null) return;
  try {
    const result = await api(`/api/bulk/${encodeURIComponent(item.code)}`, {
      method: "DELETE",
      body: JSON.stringify({ keys, confirmation }),
    });
    toast(`已删除 ${result.deletedRows} 行`);
    $("bulkDeleteKeys").value = "";
  } catch (error) {
    alert(error.message);
  }
};
