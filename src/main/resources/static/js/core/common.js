/** 通用 DOM、HTTP、分页、日期和编码工具 */

const escapeHtml = (value) =>
  String(value ?? "").replace(
    /[&<>'"]/g,
    (char) =>
      ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", "'": "&#39;", '"': "&quot;" })[
        char
      ],
  );
async function api(url, options = {}) {
  const response = await fetch(url, {
    credentials: "include",
    headers: { "Content-Type": "application/json", ...(options.headers || {}) },
    ...options,
  });
  let result = {};
  try {
    result = await response.json();
  } catch (e) {}
  if (response.status === 401) {
    showLogin();
    throw new Error(result.message || "请先登录");
  }
  if (!response.ok) throw new Error(result.message || "操作失败");
  return result.data;
}
function toast(message) {
  $("toast").textContent = message;
  $("toast").classList.remove("hidden");
  setTimeout(() => $("toast").classList.add("hidden"), 2200);
}
function ensureTablePager(prefix, targetId) {
  let state = tablePagerStates.get(prefix);
  if (!state) {
    state = {
      page: 1,
      size: 20,
      totalPages: 1,
      total: 0,
      onPage: null,
      items: [],
    };
    tablePagerStates.set(prefix, state);
  }
  const target = $(targetId),
    existingInfo = $(`${prefix}PageInfo`);
  let pager = $(`${prefix}Pagination`) || existingInfo?.closest(".pagination");
  if (pager && !pager.id) pager.id = `${prefix}Pagination`;
  if (!pager) {
    pager = document.createElement("div");
    pager.id = `${prefix}Pagination`;
    pager.className = "pagination";
    if (existingInfo) {
      existingInfo.parentNode.insertBefore(pager, existingInfo);
      existingInfo.classList.remove("help-text");
      pager.appendChild(existingInfo);
    } else {
      const info = document.createElement("span");
      info.id = `${prefix}PageInfo`;
      pager.appendChild(info);
      if (target?.tagName === "TBODY")
        target.closest(".table-wrap")?.appendChild(pager);
      else target?.insertAdjacentElement("afterend", pager);
    }
  }
  let previous = $(`${prefix}PrevButton`),
    next = $(`${prefix}NextButton`);
  if (!previous) {
    previous = document.createElement("button");
    previous.id = `${prefix}PrevButton`;
    previous.type = "button";
    previous.textContent = "上一页";
    pager.appendChild(previous);
  }
  if (!$(`${prefix}PageInput`)) {
    const jump = document.createElement("span");
    jump.className = "page-jump";
    jump.innerHTML = `到 <input id="${prefix}PageInput" type="number" min="1" inputmode="numeric" aria-label="输入要跳转的页码"> 页 <button id="${prefix}JumpButton" type="button">跳转</button>`;
    next ? pager.insertBefore(jump, next) : pager.appendChild(jump);
  }
  if (!next) {
    next = document.createElement("button");
    next.id = `${prefix}NextButton`;
    next.type = "button";
    next.textContent = "下一页";
    pager.appendChild(next);
  }
  if (!pager.dataset.bound) {
    pager.dataset.bound = "true";
    previous.onclick = () => requestTablePage(prefix, state.page - 1);
    next.onclick = () => requestTablePage(prefix, state.page + 1);
    $(`${prefix}JumpButton`).onclick = () =>
      requestTablePage(prefix, $(`${prefix}PageInput`).value, true);
    $(`${prefix}PageInput`).addEventListener("keydown", (event) => {
      if (event.key === "Enter") {
        event.preventDefault();
        requestTablePage(prefix, event.currentTarget.value, true);
      }
    });
  }
  return state;
}
function requestTablePage(prefix, value, fromInput = false) {
  const state = tablePagerStates.get(prefix);
  if (!state) return;
  const parsed = Number.parseInt(value, 10);
  if (!Number.isFinite(parsed)) {
    if (fromInput) toast("请输入正确的页码");
    return;
  }
  const target = Math.max(1, Math.min(parsed, Math.max(state.totalPages, 1)));
  if (fromInput && target !== parsed)
    toast(`页码范围为 1～${Math.max(state.totalPages, 1)}`);
  $(`${prefix}PageInput`).value = target;
  if (target === state.page) return;
  state.page = target;
  state.onPage?.(target);
}
function resetTablePager(prefix) {
  const state = tablePagerStates.get(prefix);
  if (state) state.page = 1;
}
function updateTablePager(
  prefix,
  targetId,
  { page, totalPages, total, onPage },
) {
  const state = ensureTablePager(prefix, targetId);
  state.page = Math.max(1, Number(page) || 1);
  state.totalPages = Math.max(1, Number(totalPages) || 1);
  state.total = Math.max(0, Number(total) || 0);
  state.onPage = onPage;
  $(`${prefix}PageInfo`).textContent =
    `共 ${state.total} 条，第 ${state.page} / ${state.totalPages} 页`;
  $(`${prefix}PageInput`).value = state.page;
  $(`${prefix}PageInput`).max = state.totalPages;
  $(`${prefix}PrevButton`).disabled = state.page <= 1;
  $(`${prefix}NextButton`).disabled = state.page >= state.totalPages;
  $(`${prefix}JumpButton`).disabled = state.totalPages <= 1;
  return state;
}
function updateServerPager(prefix, targetId, state, data, loader) {
  state.page = Number(data.page) || state.page;
  const responseSize = Number(data.size) || state.size;
  state.totalPages =
    Number(data.totalPages) ||
    Math.ceil(Number(data.total || 0) / responseSize);
  updateTablePager(prefix, targetId, {
    page: state.page,
    totalPages: Math.max(state.totalPages, 1),
    total: data.total,
    onPage: (page) => {
      state.page = page;
      loader();
    },
  });
}
function paginateClientCollection(prefix, targetId, selector, reset = false) {
  const target = $(targetId);
  if (!target) return;
  const pagerState = ensureTablePager(prefix, targetId);
  if (reset) pagerState.page = 1;
  const items = Array.from(target.querySelectorAll(selector));
  pagerState.items = items;
  pagerState.total = items.length;
  pagerState.totalPages = Math.max(
    1,
    Math.ceil(items.length / pagerState.size),
  );
  pagerState.page = Math.min(
    Math.max(1, pagerState.page),
    pagerState.totalPages,
  );
  const render = () => {
    const start = (pagerState.page - 1) * pagerState.size;
    pagerState.items.forEach((item, index) =>
      item.classList.toggle(
        "client-page-hidden",
        index < start || index >= start + pagerState.size,
      ),
    );
    updateTablePager(prefix, targetId, {
      page: pagerState.page,
      totalPages: pagerState.totalPages,
      total: pagerState.total,
      onPage: (page) => {
        pagerState.page = page;
        render();
      },
    });
  };
  render();
}
function paginateClientTable(prefix, bodyId, reset = false) {
  paginateClientCollection(
    prefix,
    bodyId,
    ":scope > tr:not(:has(.empty))",
    reset,
  );
}
function refreshClearButtons(form) {
  (form || document)
    .querySelectorAll(".clear-field-button")
    .forEach((button) => {
      const input = $(button.dataset.clearForm).elements[
        button.dataset.clearName
      ];
      button.disabled = !input || input.disabled || !input.value;
    });
}
document.querySelectorAll(".clear-field-button").forEach((button) => {
  const input = $(button.dataset.clearForm).elements[button.dataset.clearName];
  button.onclick = () => {
    input.value = "";
    input.setCustomValidity("");
    input.dispatchEvent(new Event("input", { bubbles: true }));
    input.dispatchEvent(new Event("change", { bubbles: true }));
    input.focus();
    refreshClearButtons($(button.dataset.clearForm));
  };
  input.addEventListener("input", () =>
    refreshClearButtons($(button.dataset.clearForm)),
  );
});
function toDateInput(value) {
  return value ? String(value).replace(" ", "T").slice(0, 16) : "";
}
function toDateDisplay(value) {
  return value ? String(value).replace("T", " ").slice(0, 19) : "";
}
function treatmentKey(item) {
  return encodeURIComponent(JSON.stringify(item));
}
function decodeTreatmentKey(value) {
  return JSON.parse(decodeURIComponent(value));
}
function catalogItemCode(item) {
  return item.projectCode || item.medId || item.diaId || item.serId || "";
}
function catalogItemName(item) {
  return item.projectName || item.medName || item.diaName || item.serName || "";
}
function catalogItemCategory(item) {
  return (
    item.chargeableItemsCategory ||
    item.medExpType ||
    item.diaExpType ||
    item.serExpType ||
    ""
  );
}
function catalogItemLevel(item) {
  return item.expenseLevel || item.medExpLevel || item.diaExpLevel || "";
}
function catalogBaseCoverage(level) {
  const value = String(level || "");
  if (value.includes("丙")) return 0;
  if (value.includes("乙")) return 90;
  return 100;
}
function catalogOptionLabel(item) {
  const name = catalogItemName(item),
    category = catalogItemCategory(item),
    level = catalogItemLevel(item);
  const parts = [name];
  if (category) parts.push(category);
  parts.push(level || "未标等级（按甲类）");
  parts.push(`基础纳入比例 ${catalogBaseCoverage(level)}%`);
  return parts.filter(Boolean).join("｜");
}
