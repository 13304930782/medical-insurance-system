/** 登录、注册、密码重置、修改密码和退出 */

function showAuthPanel(name) {
  document
    .querySelectorAll("[data-auth-form]")
    .forEach((panel) =>
      panel.classList.toggle("hidden", panel.dataset.authForm !== name),
    );
  document
    .querySelectorAll("[data-auth-panel]")
    .forEach((button) =>
      button.classList.toggle("active", button.dataset.authPanel === name),
    );
  document.querySelectorAll(".auth-message").forEach((message) => {
    message.textContent = "";
    message.classList.remove("success");
  });
}
document
  .querySelectorAll("[data-auth-panel]")
  .forEach(
    (button) =>
      (button.onclick = () => showAuthPanel(button.dataset.authPanel)),
  );
function base64ToBytes(value) {
  const binary = atob(value);
  return Uint8Array.from(binary, (char) => char.charCodeAt(0));
}
function bytesToBase64(bytes) {
  let binary = "";
  bytes.forEach((value) => (binary += String.fromCharCode(value)));
  return btoa(binary);
}
async function encryptPasswordPayload(secret) {
  if (!window.crypto?.subtle)
    throw new Error("当前浏览器不支持安全密码加密，请使用新版浏览器");
  const challenge = await api("/api/auth/encryption-challenge");
  const key = await crypto.subtle.importKey(
    "spki",
    base64ToBytes(challenge.publicKey),
    { name: "RSA-OAEP", hash: "SHA-256" },
    false,
    ["encrypt"],
  );
  const plaintext = new TextEncoder().encode(
    `${challenge.challengeId}\n${secret}`,
  );
  const encrypted = await crypto.subtle.encrypt(
    { name: "RSA-OAEP" },
    key,
    plaintext,
  );
  return {
    challengeId: challenge.challengeId,
    encryptedPassword: bytesToBase64(new Uint8Array(encrypted)),
  };
}
$("loginForm").addEventListener("submit", async (event) => {
  event.preventDefault();
  const message = $("loginMessage");
  message.textContent = "";
  const values = Object.fromEntries(new FormData(event.target));
  try {
    const encrypted = await encryptPasswordPayload(values.password);
    await api("/api/auth/login", {
      method: "POST",
      body: JSON.stringify({ username: values.username, ...encrypted }),
    });
    await new Promise((resolve) => setTimeout(resolve, 80));
    const sessionUser = await api("/api/auth/me");
    showApp(sessionUser);
    event.target.password.value = "";
  } catch (error) {
    message.textContent = error.message;
  }
});
$("registerForm").addEventListener("submit", async (event) => {
  event.preventDefault();
  const message = $("registerMessage");
  message.textContent = "";
  const values = Object.fromEntries(new FormData(event.target));
  if (values.password !== values.confirmPassword) {
    message.textContent = "两次输入的密码不一致";
    return;
  }
  try {
    const encrypted = await encryptPasswordPayload(values.password);
    const result = await api("/api/auth/register", {
      method: "POST",
      body: JSON.stringify({
        username: values.username,
        realName: values.realName,
        email: values.email,
        ...encrypted,
      }),
    });
    $("loginForm").username.value = values.username;
    event.target.reset();
    showAuthPanel("login");
    $("loginMessage").textContent =
      result.message || "注册申请已提交，请等待管理员审核";
    $("loginMessage").classList.add("success");
  } catch (error) {
    message.textContent = error.message;
  }
});
let forgotCooldownTimer = null;
function setForgotRequestLoading(loading) {
  const button = $("forgotRequestButton");
  button.disabled = loading;
  button.classList.toggle("is-loading", loading);
  button.setAttribute("aria-busy", String(loading));
  button.textContent = loading ? "正在发送..." : "获取重置验证码";
}
function startForgotRequestCooldown(seconds = 60) {
  const button = $("forgotRequestButton");
  if (forgotCooldownTimer) clearInterval(forgotCooldownTimer);
  let remaining = Math.max(1, Number(seconds) || 60);
  button.classList.remove("is-loading");
  button.setAttribute("aria-busy", "false");
  button.disabled = true;
  const update = () => {
    button.textContent = `${remaining}秒后可重新发送`;
    remaining--;
    if (remaining < 0) {
      clearInterval(forgotCooldownTimer);
      forgotCooldownTimer = null;
      button.disabled = false;
      button.textContent = "重新获取验证码";
    }
  };
  update();
  forgotCooldownTimer = setInterval(update, 1000);
}
$("forgotRequestForm").addEventListener("submit", async (event) => {
  event.preventDefault();
  const button = $("forgotRequestButton");
  if (button.disabled) return;
  const message = $("forgotMessage");
  message.textContent = "";
  message.classList.remove("success");
  setForgotRequestLoading(true);
  try {
    const values = Object.fromEntries(new FormData(event.target));
    const result = await api("/api/auth/password-reset/request", {
      method: "POST",
      body: JSON.stringify(values),
    });
    const reset = $("resetPasswordForm");
    reset.username.value = values.username;
    reset.email.value = values.email;
    reset.classList.remove("hidden");
    if (result.developmentResetCode) {
      reset.code.value = result.developmentResetCode;
      message.textContent = `测试环境验证码：${result.developmentResetCode}（已自动填写）`;
    } else message.textContent = result.message;
    message.classList.add("success");
    startForgotRequestCooldown(result.retryAfterSeconds || 60);
  } catch (error) {
    message.textContent = error.message;
    setForgotRequestLoading(false);
  }
});
$("resetPasswordForm").addEventListener("submit", async (event) => {
  event.preventDefault();
  const message = $("forgotMessage");
  message.classList.remove("success");
  message.textContent = "";
  const values = Object.fromEntries(new FormData(event.target));
  if (values.password !== values.confirmPassword) {
    message.textContent = "两次输入的新密码不一致";
    return;
  }
  try {
    const encrypted = await encryptPasswordPayload(values.password);
    await api("/api/auth/password-reset/confirm", {
      method: "POST",
      body: JSON.stringify({
        username: values.username,
        email: values.email,
        code: values.code,
        ...encrypted,
      }),
    });
    $("loginForm").username.value = values.username;
    event.target.reset();
    $("forgotRequestForm").reset();
    showAuthPanel("login");
    $("loginMessage").textContent = "密码已重置，请使用新密码登录";
    $("loginMessage").classList.add("success");
  } catch (error) {
    message.textContent = error.message;
  }
});
document.querySelectorAll("[data-submit-form]").forEach((button) =>
  button.addEventListener("click", () => {
    const form = $(button.dataset.submitForm);
    if (form && form.reportValidity()) form.requestSubmit();
  }),
);
$("changePasswordButton").onclick = () => {
  $("changePasswordForm").reset();
  $("changePasswordMessage").textContent = "";
  $("changePasswordModal").classList.remove("hidden");
};
$("closeChangePasswordModal").onclick = $("cancelChangePasswordModal").onclick =
  () => $("changePasswordModal").classList.add("hidden");
$("changePasswordForm").addEventListener("submit", async (event) => {
  event.preventDefault();
  const message = $("changePasswordMessage");
  message.textContent = "";
  const values = Object.fromEntries(new FormData(event.target));
  if (values.newPassword !== values.confirmPassword) {
    message.textContent = "两次输入的新密码不一致";
    return;
  }
  try {
    const encrypted = await encryptPasswordPayload(
      `${values.currentPassword}\0${values.newPassword}`,
    );
    await api("/api/auth/change-password", {
      method: "POST",
      body: JSON.stringify(encrypted),
    });
    $("changePasswordModal").classList.add("hidden");
    showLogin();
    showAuthPanel("login");
    $("loginMessage").textContent = "密码修改成功，请重新登录";
    $("loginMessage").classList.add("success");
  } catch (error) {
    message.textContent = error.message;
  }
});
$("logoutButton").onclick = async () => {
  await api("/api/auth/logout", { method: "POST" });
  showLogin();
  showAuthPanel("login");
};
