/** 应用启动入口：所有业务代码位于 core/ 和 modules/ 目录。 */

(async function bootstrap() {
  try {
    const sessionUser = await api("/api/auth/me");
    showApp(sessionUser);
  } catch (error) {
    showLogin();
  }
})();
