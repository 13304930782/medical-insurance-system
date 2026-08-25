/** 帮助中心、AI 对话、政策同步和可拖动入口 */

function helpChunkHtml(value) {
  return String(value || "")
    .split(/\r?\n/)
    .map((line) => line.trim())
    .filter(Boolean)
    .map((line) => {
      const heading = line.match(/^(#{1,3})\s+(.+)$/);
      if (heading) {
        const level = Math.min(4, heading[1].length + 1);
        return `<h${level}>${escapeHtml(heading[2])}</h${level}>`;
      }
      if (/^[-*]\s+/.test(line))
        return `<p class="help-list-item">${escapeHtml(line.replace(/^[-*]\s+/, ""))}</p>`;
      return `<p>${escapeHtml(line)}</p>`;
    })
    .join("");
}
function helpChunkTitle(chunk, index) {
  const match = String(chunk.chunkContent || "").match(/^(?:#{1,3})\s+(.+)$/m);
  return match ? match[1].trim() : `资料段落 ${index + 1}`;
}
async function loadHelpDocument(documentId, chunkId) {
  $("helpDocumentBody").innerHTML =
    '<div class="help-loading">正在加载帮助资料……</div>';
  $("helpTableOfContents").innerHTML =
    '<div class="help-loading">正在加载目录……</div>';
  try {
    const data = await api(
      documentId
        ? `/api/ai/help/documents/${encodeURIComponent(documentId)}`
        : "/api/ai/help",
    );
    $("helpDocumentTitle").textContent = data.title;
    $("helpDocumentType").textContent =
      data.sourceType === "SYSTEM_DOCUMENT" ? "系统使用指南" : "医保官方资料";
    $("helpDocumentMeta").textContent =
      `来源：${data.publisher || "医疗保险报销系统"}${data.fetchedAt ? `｜更新时间：${toDateDisplay(data.fetchedAt)}` : ""}`;
    const official = /^https:\/\//i.test(data.sourceUrl || "");
    $("helpOfficialLink").classList.toggle("hidden", !official);
    if (official) $("helpOfficialLink").href = data.sourceUrl;
    const chunks = data.chunks || [];
    $("helpDocumentBody").innerHTML =
      chunks
        .map(
          (chunk, index) =>
            `<section id="helpChunk-${chunk.chunkId}" class="help-chunk" data-chunk-id="${chunk.chunkId}">${helpChunkHtml(chunk.chunkContent)}</section>`,
        )
        .join("") || '<div class="empty">该资料暂无正文</div>';
    $("helpTableOfContents").innerHTML = chunks
      .map(
        (chunk, index) =>
          `<button type="button" data-help-chunk="${chunk.chunkId}">${escapeHtml(helpChunkTitle(chunk, index))}</button>`,
      )
      .join("");
    $("helpTableOfContents")
      .querySelectorAll("[data-help-chunk]")
      .forEach(
        (button) =>
          (button.onclick = () => scrollToHelpChunk(button.dataset.helpChunk)),
      );
    if (chunkId) requestAnimationFrame(() => scrollToHelpChunk(chunkId));
  } catch (error) {
    $("helpDocumentBody").innerHTML =
      `<div class="empty">${escapeHtml(error.message)}</div>`;
    $("helpTableOfContents").innerHTML =
      '<div class="empty">资料加载失败</div>';
  }
}
function scrollToHelpChunk(chunkId) {
  const target = $(`helpChunk-${chunkId}`);
  if (!target) return;
  target.scrollIntoView({ behavior: "smooth", block: "start" });
  target.classList.add("help-highlight");
  setTimeout(() => target.classList.remove("help-highlight"), 2200);
  $("helpTableOfContents")
    .querySelectorAll("[data-help-chunk]")
    .forEach((button) =>
      button.classList.toggle(
        "active",
        button.dataset.helpChunk === String(chunkId),
      ),
    );
}
function openHelp(documentId = null, chunkId = null) {
  if (currentPageName !== "help") {
    helpReturnPage = currentPageName;
    helpReturnScrollTop = document.querySelector("#appView main").scrollTop;
  }
  showPage("help");
  document.querySelector("#appView main").scrollTop = 0;
  $("aiChatWidget").classList.add("hidden");
  $("aiChatLauncher").setAttribute("aria-expanded", "false");
  loadHelpDocument(documentId, chunkId);
}
$("helpBackButton").onclick = () => {
  showPage(helpReturnPage || "dashboard");
  requestAnimationFrame(
    () =>
      (document.querySelector("#appView main").scrollTop = helpReturnScrollTop),
  );
};
function renderAiAnswer(bubble, text, citations = []) {
  bubble.textContent = "";
  const byNumber = new Map(
    citations.map((item) => [Number(item.number), item]),
  );
  const pattern = /\[资料(\d+)\]/g;
  let cursor = 0,
    match;
  while ((match = pattern.exec(text)) !== null) {
    if (match.index > cursor)
      bubble.append(document.createTextNode(text.slice(cursor, match.index)));
    const citation = byNumber.get(Number(match[1]));
    if (citation) {
      const button = document.createElement("button");
      button.type = "button";
      button.className = "ai-citation-link";
      button.textContent = match[0];
      button.title = "在帮助中心查看此资料";
      button.onclick = () => openHelp(citation.documentId, citation.chunkId);
      bubble.appendChild(button);
    } else bubble.append(document.createTextNode(match[0]));
    cursor = pattern.lastIndex;
  }
  if (cursor < text.length)
    bubble.append(document.createTextNode(text.slice(cursor)));
}
function appendAiMessage(side, text, sources = []) {
  const row = document.createElement("div");
  row.className = `ai-message ${side}`;
  const avatar = document.createElement("span");
  avatar.className = "ai-avatar";
  avatar.textContent = side === "user" ? "我" : "医";
  const content = document.createElement("div");
  content.className = "ai-message-content";
  const bubble = document.createElement("div");
  bubble.className = "ai-bubble";
  bubble.textContent = text;
  content.appendChild(bubble);
  if (sources.length) {
    const sourceBox = document.createElement("div");
    sourceBox.className = "ai-message-sources";
    sourceBox.append("依据资料：");
    sources.forEach((source) => {
      const button = document.createElement("button");
      button.type = "button";
      button.className = "ai-source-link";
      button.textContent = source.title;
      button.onclick = () => openHelp(source.documentId);
      sourceBox.appendChild(button);
    });
    content.appendChild(sourceBox);
  }
  row.append(avatar, content);
  $("aiChatMessages").appendChild(row);
  $("aiChatMessages").scrollTop = $("aiChatMessages").scrollHeight;
  return bubble;
}
function renderAiSyncStatus(status) {
  const count = Number(status.documentCount || 0);
  const state = status.running
    ? "正在从国家医保局同步"
    : status.result || "尚未执行";
  const time = status.lastCompletedAt
    ? `，上次完成 ${toDateDisplay(status.lastCompletedAt)}`
    : "";
  $("aiSyncStatus").textContent = `官方资料 ${count} 篇；${state}${time}`;
  $("aiSyncButton").disabled = Boolean(status.running);
}
async function loadAiSyncStatus() {
  if (currentRole !== "ADMIN") return;
  $("aiSyncPanel").classList.remove("hidden");
  try {
    renderAiSyncStatus(await api("/api/ai/knowledge/sync-status"));
  } catch (error) {
    $("aiSyncStatus").textContent = `同步状态读取失败：${error.message}`;
  }
}
const aiLauncherPositionKey = "medical-insurance-ai-launcher-position";
let suppressAiLauncherClick = false;
function clampAiLauncher(left, top) {
  const launcher = $("aiChatLauncher"),
    gap = 8;
  return {
    left: Math.min(
      Math.max(gap, left),
      Math.max(gap, window.innerWidth - launcher.offsetWidth - gap),
    ),
    top: Math.min(
      Math.max(gap, top),
      Math.max(gap, window.innerHeight - launcher.offsetHeight - gap),
    ),
  };
}
function setAiLauncherPosition(left, top, save = false) {
  const launcher = $("aiChatLauncher"),
    position = clampAiLauncher(left, top);
  launcher.style.left = `${position.left}px`;
  launcher.style.top = `${position.top}px`;
  launcher.style.right = "auto";
  launcher.style.bottom = "auto";
  if (save)
    try {
      localStorage.setItem(aiLauncherPositionKey, JSON.stringify(position));
    } catch (error) {}
  if (!$("aiChatWidget").classList.contains("hidden")) positionAiWidget();
}
function positionAiWidget() {
  const launcher = $("aiChatLauncher"),
    widget = $("aiChatWidget"),
    gap = 10,
    rect = launcher.getBoundingClientRect(),
    width = widget.offsetWidth || Math.min(400, window.innerWidth - 28),
    height = widget.offsetHeight || Math.min(620, window.innerHeight - 125);
  let left = rect.right - width,
    top = rect.top - height - 12;
  if (top < gap) top = rect.bottom + 12;
  left = Math.min(
    Math.max(gap, left),
    Math.max(gap, window.innerWidth - width - gap),
  );
  top = Math.min(
    Math.max(gap, top),
    Math.max(gap, window.innerHeight - height - gap),
  );
  widget.style.left = `${left}px`;
  widget.style.top = `${top}px`;
  widget.style.right = "auto";
  widget.style.bottom = "auto";
}
function restoreAiLauncherPosition() {
  try {
    const saved = JSON.parse(
      localStorage.getItem(aiLauncherPositionKey) || "null",
    );
    if (saved && Number.isFinite(saved.left) && Number.isFinite(saved.top))
      setAiLauncherPosition(saved.left, saved.top);
  } catch (error) {}
}
function initAiLauncherDrag() {
  const launcher = $("aiChatLauncher");
  restoreAiLauncherPosition();
  launcher.addEventListener("pointerdown", (event) => {
    if (event.button !== 0) return;
    const startRect = launcher.getBoundingClientRect(),
      startX = event.clientX,
      startY = event.clientY;
    let dragged = false;
    launcher.setPointerCapture(event.pointerId);
    launcher.classList.add("dragging");
    const move = (moveEvent) => {
      const dx = moveEvent.clientX - startX,
        dy = moveEvent.clientY - startY;
      if (!dragged && Math.hypot(dx, dy) < 5) return;
      dragged = true;
      moveEvent.preventDefault();
      setAiLauncherPosition(startRect.left + dx, startRect.top + dy);
    };
    const finish = (finishEvent) => {
      launcher.removeEventListener("pointermove", move);
      launcher.removeEventListener("pointerup", finish);
      launcher.removeEventListener("pointercancel", finish);
      launcher.classList.remove("dragging");
      if (launcher.hasPointerCapture(finishEvent.pointerId))
        launcher.releasePointerCapture(finishEvent.pointerId);
      if (dragged) {
        suppressAiLauncherClick = true;
        const rect = launcher.getBoundingClientRect();
        setAiLauncherPosition(rect.left, rect.top, true);
      }
    };
    launcher.addEventListener("pointermove", move);
    launcher.addEventListener("pointerup", finish);
    launcher.addEventListener("pointercancel", finish);
  });
  window.addEventListener("resize", () => {
    const rect = launcher.getBoundingClientRect();
    setAiLauncherPosition(rect.left, rect.top);
    if (!$("aiChatWidget").classList.contains("hidden")) positionAiWidget();
  });
}
function openAiChat() {
  const opening = $("aiChatWidget").classList.contains("hidden");
  $("aiChatWidget").classList.toggle("hidden", !opening);
  $("aiChatLauncher").setAttribute("aria-expanded", String(opening));
  if (opening) {
    positionAiWidget();
    $("aiChatInput").focus();
    loadAiSyncStatus();
  }
}
initAiLauncherDrag();
$("aiChatLauncher").onclick = () => {
  if (suppressAiLauncherClick) {
    suppressAiLauncherClick = false;
    return;
  }
  openAiChat();
};
$("aiChatClose").onclick = () => {
  $("aiChatWidget").classList.add("hidden");
  $("aiChatLauncher").setAttribute("aria-expanded", "false");
};
async function sendAiQuestion() {
  const question = $("aiChatInput").value.trim();
  if (!question) return;
  appendAiMessage("user", question);
  $("aiChatInput").value = "";
  $("aiChatSend").disabled = true;
  const bubble = appendAiMessage(
    "assistant",
    "正在结合当前页面和系统资料分析……",
  );
  try {
    const result = await api("/api/ai/chat", {
      method: "POST",
      body: JSON.stringify({ question, page: currentPageName }),
    });
    renderAiAnswer(bubble, result.answer, result.citations || []);
    const content = bubble.parentElement;
    if (result.sources?.length) {
      const sourceBox = document.createElement("div");
      sourceBox.className = "ai-message-sources";
      sourceBox.append("查看完整资料：");
      result.sources.forEach((source) => {
        const button = document.createElement("button");
        button.type = "button";
        button.className = "ai-source-link";
        button.textContent = source.title;
        button.onclick = () => openHelp(source.documentId);
        sourceBox.appendChild(button);
      });
      content.appendChild(sourceBox);
    }
  } catch (error) {
    bubble.textContent = `暂时无法回答：${error.message}`;
  } finally {
    $("aiChatSend").disabled = false;
    $("aiChatInput").focus();
    $("aiChatMessages").scrollTop = $("aiChatMessages").scrollHeight;
  }
}
$("aiChatSend").onclick = sendAiQuestion;
$("aiChatInput").addEventListener("keydown", (event) => {
  if (event.key === "Enter" && !event.shiftKey) {
    event.preventDefault();
    sendAiQuestion();
  }
});
$("aiSyncButton").onclick = async () => {
  if (!confirm("立即从国家医疗保障局官网同步最新政策资料吗？")) return;
  $("aiSyncButton").disabled = true;
  $("aiSyncStatus").textContent =
    "正在从国家医保局读取政策，首次同步可能需要几十秒……";
  try {
    const status = await api("/api/ai/knowledge/sync", { method: "POST" });
    renderAiSyncStatus(status);
    toast(`官方资料同步完成，新增 ${status.lastImported || 0} 篇`);
  } catch (error) {
    $("aiSyncStatus").textContent = `同步失败：${error.message}`;
  } finally {
    $("aiSyncButton").disabled = false;
  }
};
