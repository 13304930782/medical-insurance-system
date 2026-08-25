param(
    [string]$Password = "changeit-local-only",
    [string]$Output = "config/localhost.p12"
)

$ErrorActionPreference = "Stop"
$projectRoot = Split-Path -Parent $PSScriptRoot
$target = [System.IO.Path]::GetFullPath((Join-Path $projectRoot $Output))
$configRoot = [System.IO.Path]::GetFullPath((Join-Path $projectRoot "config"))
if (-not $target.StartsWith($configRoot,[System.StringComparison]::OrdinalIgnoreCase)) {
    throw "证书只能生成到项目 config 目录内"
}
if (Test-Path -LiteralPath $target) {
    throw "目标文件已存在：$target"
}
$javaHome = if ($env:JAVA_HOME) { $env:JAVA_HOME } else { "C:\Program Files\JetBrains\IntelliJ IDEA 2026.2.1\jbr" }
$keytool = Join-Path $javaHome "bin/keytool.exe"
if (-not (Test-Path -LiteralPath $keytool)) { throw "未找到 keytool：$keytool" }
New-Item -ItemType Directory -Path (Split-Path -Parent $target) -Force | Out-Null
& $keytool -genkeypair -alias medical-localhost -keyalg RSA -keysize 2048 -storetype PKCS12 -keystore $target -validity 825 -storepass $Password -keypass $Password -dname "CN=localhost, OU=Development, O=Medical Insurance Demo, L=Local, ST=Local, C=CN" -ext "SAN=dns:localhost,ip:127.0.0.1"
if ($LASTEXITCODE -ne 0) { throw "本地 HTTPS 证书生成失败" }
Write-Host "已生成：$target"
Write-Host "启动变量：SSL_ENABLED=true;SERVER_PORT=8443;SSL_KEY_STORE=$target;SSL_KEY_STORE_PASSWORD=$Password;SESSION_COOKIE_SECURE=true"
