<#
.SYNOPSIS
    MintHive 本地一键启动脚本（后端 + 用户端，可选管理端）

.DESCRIPTION
    自动完成以下步骤：
      1. 检查 backend/.env、MySQL(3306)、Redis(6379 或 16379)
      2. jar 缺失或源码有更新时自动 mvn package
      3. 启动后端（注入 Redis 端口、CORS 白名单、服务端口）
      4. 启动用户端 Vite（避开 Windows 保留端口段，默认 4400）
      5. 轮询 /actuator/health，就绪后打印访问地址

.PARAMETER Stop
    停止本脚本启动的进程（按端口查找并结束）

.PARAMETER WithAdmin
    同时启动管理端（默认 4401）

.PARAMETER BackendPort
    后端端口，默认 8080

.PARAMETER UserPort
    用户端端口，默认 4400（5173/5174 在部分 Windows 上被保留端口占用）

.PARAMETER AdminPort
    管理端端口，默认 4401

.PARAMETER SkipBuild
    跳过 mvn package，直接使用现有 jar

.EXAMPLE
    .\start-dev.ps1
    .\start-dev.ps1 -WithAdmin
    .\start-dev.ps1 -Stop
#>
param(
    [switch]$Stop,
    [switch]$WithAdmin,
    [int]$BackendPort = 8080,
    [int]$UserPort = 4400,
    [int]$AdminPort = 4401,
    [switch]$SkipBuild
)

$ErrorActionPreference = 'Stop'

$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$backendDir = Join-Path $root 'backend'
$userDir = Join-Path $root 'frontend-user'
$adminDir = Join-Path $root 'frontend-admin'
$jarPath = Join-Path $backendDir 'target\minthive-backend.jar'
$managementPort = 9090

# ---------- 通用工具 ----------

<#
.SYNOPSIS
    检测本机端口是否已监听
.PARAMETER Port
    端口号
.OUTPUTS
    System.Boolean
#>
function Test-Port {
    param([int]$Port)
    $client = New-Object Net.Sockets.TcpClient
    try {
        $client.Connect('127.0.0.1', $Port)
        return $true
    } catch {
        return $false
    } finally {
        $client.Dispose()
    }
}

<#
.SYNOPSIS
    获取监听指定端口的进程 ID
.PARAMETER Port
    端口号
.OUTPUTS
    System.Int32 或 $null
#>
function Get-PortOwner {
    param([int]$Port)
    $conn = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($conn) { return $conn.OwningProcess }
    return $null
}

<#
.SYNOPSIS
    结束占用指定端口的进程
.PARAMETER Port
    端口号
.PARAMETER Name
    日志中显示的服务名
#>
function Stop-ByPort {
    param([int]$Port, [string]$Name)
    $procId = Get-PortOwner $Port
    if ($procId) {
        Stop-Process -Id $procId -Force -ErrorAction SilentlyContinue
        Write-Host ("[stop] {0} (port {1}, pid {2})" -f $Name, $Port, $procId) -ForegroundColor Yellow
    } else {
        Write-Host ("[skip] {0} (port {1}) not running" -f $Name, $Port) -ForegroundColor DarkGray
    }
}

# ---------- 停止模式 ----------

if ($Stop) {
    Stop-ByPort $BackendPort 'backend-api'
    Stop-ByPort $managementPort 'backend-management'
    Stop-ByPort $UserPort 'frontend-user'
    Stop-ByPort $AdminPort 'frontend-admin'
    Write-Host '[done] services stopped' -ForegroundColor Green
    exit 0
}

# ---------- 前置检查 ----------

Write-Host '=== MintHive dev launcher ===' -ForegroundColor Cyan

if (-not (Test-Path (Join-Path $backendDir '.env'))) {
    Write-Host '[error] backend/.env not found. Copy backend/.env.example to backend/.env and fill in JWT_DEV_SECRET / AI keys.' -ForegroundColor Red
    exit 1
}

if (-not (Test-Port 3306)) {
    Write-Host '[error] MySQL 3306 is not reachable. Start your local MySQL first.' -ForegroundColor Red
    exit 1
}

# Redis 可能是本机服务(6379)或 Docker 映射(16379)
$redisPort = 0
if (Test-Port 6379) {
    $redisPort = 6379
} elseif (Test-Port 16379) {
    $redisPort = 16379
} else {
    Write-Host '[error] Redis not reachable on 6379 or 16379. Try: docker start minthive-redis' -ForegroundColor Red
    exit 1
}

foreach ($port in @($BackendPort, $managementPort, $UserPort)) {
    if (Test-Port $port) {
        Write-Host ("[error] port {0} is already in use. Run: .\start-dev.ps1 -Stop" -f $port) -ForegroundColor Red
        exit 1
    }
}

# ---------- 构建后端 ----------

$needBuild = -not (Test-Path $jarPath)
if (-not $needBuild -and -not $SkipBuild) {
    $jarTime = (Get-Item $jarPath).LastWriteTime
    $newer = Get-ChildItem -Path (Join-Path $backendDir 'src\main') -Recurse -Include *.java,*.yml,*.xml -File |
        Where-Object { $_.LastWriteTime -gt $jarTime } | Select-Object -First 1
    if ($newer) {
        Write-Host ("[build] source changed: {0}" -f $newer.Name) -ForegroundColor Yellow
        $needBuild = $true
    }
}

if ($needBuild -and -not $SkipBuild) {
    Write-Host '[build] mvn -DskipTests package ...' -ForegroundColor Yellow
    Push-Location $backendDir
    try {
        & mvn -q -DskipTests package
        if ($LASTEXITCODE -ne 0) { throw 'maven build failed' }
    } finally {
        Pop-Location
    }
}

# ---------- 启动后端 ----------

$corsOrigins = (@(
    "http://127.0.0.1:$UserPort",
    "http://localhost:$UserPort",
    "http://127.0.0.1:$AdminPort",
    "http://localhost:$AdminPort",
    "http://127.0.0.1:5173",
    "http://localhost:5173"
) -join ',')

$backendCmd = "`$env:SPRING_DATA_REDIS_PORT='$redisPort'; " +
    "`$env:SERVER_PORT='$BackendPort'; " +
    "`$env:CORS_ALLOWED_ORIGINS='$corsOrigins'; " +
    "java -jar target\minthive-backend.jar"

Write-Host ("[start] backend on {0} (management {1}, redis {2})" -f $BackendPort, $managementPort, $redisPort) -ForegroundColor Green
Start-Process -FilePath 'powershell' -WorkingDirectory $backendDir `
    -ArgumentList '-NoExit', '-NoProfile', '-Command', $backendCmd | Out-Null

# ---------- 启动前端 ----------

<#
.SYNOPSIS
    启动一个 Vite 开发服务器窗口
.PARAMETER Dir
    前端项目目录
.PARAMETER Port
    监听端口
.PARAMETER Name
    日志中显示的名称
#>
function Start-Vite {
    param([string]$Dir, [int]$Port, [string]$Name)
    if (-not (Test-Path (Join-Path $Dir 'node_modules'))) {
        Write-Host ("[install] npm install in {0} ..." -f $Name) -ForegroundColor Yellow
        Push-Location $Dir
        try {
            & npm install
            if ($LASTEXITCODE -ne 0) { throw "npm install failed in $Dir" }
        } finally {
            Pop-Location
        }
    }
    Write-Host ("[start] {0} on {1}" -f $Name, $Port) -ForegroundColor Green
    $cmd = "`$env:PROXY_TARGET='http://127.0.0.1:$BackendPort'; npm run dev -- --host 127.0.0.1 --port $Port"
    Start-Process -FilePath 'powershell' -WorkingDirectory $Dir `
        -ArgumentList '-NoExit', '-NoProfile', '-Command', $cmd | Out-Null
}

Start-Vite $userDir $UserPort 'frontend-user'
if ($WithAdmin) {
    Start-Vite $adminDir $AdminPort 'frontend-admin'
}

# ---------- 等待后端就绪 ----------

Write-Host '[wait] backend health check ...' -ForegroundColor Yellow
$healthy = $false
for ($i = 0; $i -lt 40; $i++) {
    Start-Sleep -Seconds 3
    try {
        $resp = Invoke-WebRequest -Uri ("http://127.0.0.1:{0}/actuator/health" -f $managementPort) -TimeoutSec 3 -UseBasicParsing
        if ($resp.StatusCode -eq 200) { $healthy = $true; break }
    } catch {
        # 启动中，继续等待
    }
}

if ($healthy) {
    Write-Host '[done] backend is UP' -ForegroundColor Green
} else {
    Write-Host '[warn] backend did not become healthy in 120s, check the backend window for errors' -ForegroundColor Yellow
}

Write-Host ''
Write-Host '  API      : ' -NoNewline; Write-Host ("http://127.0.0.1:{0}" -f $BackendPort) -ForegroundColor Cyan
Write-Host '  Swagger  : ' -NoNewline; Write-Host ("http://127.0.0.1:{0}/doc.html" -f $BackendPort) -ForegroundColor Cyan
Write-Host '  Metrics  : ' -NoNewline; Write-Host ("http://127.0.0.1:{0}/actuator/health" -f $managementPort) -ForegroundColor Cyan
Write-Host '  用户端    : ' -NoNewline; Write-Host ("http://127.0.0.1:{0}" -f $UserPort) -ForegroundColor Cyan
if ($WithAdmin) {
    Write-Host '  管理端    : ' -NoNewline; Write-Host ("http://127.0.0.1:{0}" -f $AdminPort) -ForegroundColor Cyan
}
Write-Host ''
Write-Host '  停止服务  : .\start-dev.ps1 -Stop' -ForegroundColor DarkGray
