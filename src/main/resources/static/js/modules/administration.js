/** 账号审核和业务操作日志 */

async function loadSystemPage() {
  await Promise.all([loadAccounts(), loadAuditLogs()]);
}
async function loadAccounts() {
  const statusLabels = {
      PENDING: "待审核",
      APPROVED: "已通过",
      REJECTED: "已拒绝",
      DELETED: "已删除",
    },
    roleLabels = {
      ADMIN: "管理员",
      APPROVER: "审批员",
      REIMBURSEMENT: "报销经办员",
    };
  const rows = await api(
    `/api/admin/accounts?status=${encodeURIComponent($("accountStatusFilter").value)}`,
  );
  $("accountBody").innerHTML = rows.length
    ? rows
        .map((row) => {
          let actions = "-";
          if (row.accountStatus === "DELETED")
            actions = `<button class="action-btn account-restore" data-id="${row.userId}" data-name="${escapeHtml(row.username)}">恢复账号</button>`;
          else if (row.roleCode !== "ADMIN") {
            const review =
              row.accountStatus === "PENDING"
                ? `<button class="action-btn account-approve" data-id="${row.userId}" data-role="APPROVER">通过为审批员</button><button class="action-btn account-approve" data-id="${row.userId}" data-role="REIMBURSEMENT">通过为经办员</button><button class="action-btn delete account-reject" data-id="${row.userId}">拒绝</button>`
                : "";
            actions = `${review}<button class="action-btn delete account-delete" data-id="${row.userId}" data-name="${escapeHtml(row.username)}">删除账号</button>`;
          }
          return `<tr><td>${escapeHtml(row.username)}</td><td>${escapeHtml(row.realName)}</td><td>${escapeHtml(row.email || "")}</td><td><span class="tag">${escapeHtml(statusLabels[row.accountStatus] || row.accountStatus)}</span></td><td>${escapeHtml(roleLabels[row.roleCode] || row.roleCode)}</td><td>${escapeHtml(toDateDisplay(row.registeredAt))}</td><td>${actions}</td></tr>`;
        })
        .join("")
    : '<tr><td colspan="7" class="empty">暂无账号</td></tr>';
  paginateClientTable("account", "accountBody");
}
$("accountStatusFilter").onchange = () => {
  resetTablePager("account");
  loadAccounts();
};
$("accountBody").addEventListener("click", async (event) => {
  const button = event.target.closest("button[data-id]");
  if (!button) return;
  try {
    if (button.classList.contains("account-approve")) {
      if (!confirm(`确认审核通过并分配角色 ${button.dataset.role} 吗？`))
        return;
      await api(`/api/admin/accounts/${button.dataset.id}/approve`, {
        method: "POST",
        body: JSON.stringify({ roleCode: button.dataset.role }),
      });
    } else if (button.classList.contains("account-reject")) {
      if (!confirm("确认拒绝该注册申请吗？")) return;
      await api(`/api/admin/accounts/${button.dataset.id}/reject`, {
        method: "POST",
      });
    } else if (button.classList.contains("account-delete")) {
      if (
        !confirm(
          `确认删除账号“${button.dataset.name}”吗？该账号将立即无法登录，但历史业务记录会保留。`,
        )
      )
        return;
      await api(`/api/admin/accounts/${button.dataset.id}`, {
        method: "DELETE",
      });
    } else if (button.classList.contains("account-restore")) {
      if (!confirm(`确认恢复账号“${button.dataset.name}”吗？`)) return;
      await api(`/api/admin/accounts/${button.dataset.id}/restore`, {
        method: "POST",
      });
    }
    toast("账号操作已完成");
    await Promise.all([loadAccounts(), loadAuditLogs()]);
  } catch (error) {
    alert(error.message);
  }
});
async function loadAuditLogs() {
  const keyword = encodeURIComponent($("auditKeyword").value.trim()),
    result = encodeURIComponent($("auditResult").value);
  const data = await api(
    `/api/audit-logs?keyword=${keyword}&result=${result}&page=${auditState.page}&size=${auditState.size}`,
  );
  $("auditBody").innerHTML = data.items.length
    ? data.items
        .map((row) => {
          const operator = row.realName
            ? `${row.realName}（${row.username || "无账号"}）`
            : row.username || "系统";
          const resultLabel =
            row.operationResult === "SUCCESS" ? "成功" : "失败";
          return `<tr><td>${escapeHtml(toDateDisplay(row.createdAt))}</td><td>${escapeHtml(operator)}</td><td>${escapeHtml(row.operationModule)}</td><td><strong>${escapeHtml(row.operationLabel || row.operationType)}</strong></td><td>${escapeHtml(row.businessNo || "-")}</td><td style="white-space:normal">${escapeHtml(row.operationContent || "-")}</td><td><span class="tag">${escapeHtml(resultLabel)}</span></td><td>${escapeHtml(row.ipAddress || "-")}</td></tr>`;
        })
        .join("")
    : '<tr><td colspan="8" class="empty">暂无业务操作日志</td></tr>';
  updateServerPager("audit", "auditBody", auditState, data, loadAuditLogs);
}
$("auditSearchButton").onclick = () => {
  auditState.page = 1;
  loadAuditLogs();
};
$("auditExportButton").onclick = () => {
  const keyword = encodeURIComponent($("auditKeyword").value.trim()),
    result = encodeURIComponent($("auditResult").value);
  downloadBulkFile(
    `/api/audit-logs/export.xlsx?keyword=${keyword}&result=${result}`,
    "系统操作日志.xlsx",
  ).catch((error) => alert(error.message));
};
