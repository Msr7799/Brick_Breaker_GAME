$ErrorActionPreference = "Stop"

Write-Host "==============================================" -ForegroundColor MAROON
Write-Host " BrickBreakerBall - QA Ads Build + ADB Install" -ForegroundColor MAROON
Write-Host "==============================================" -ForegroundColor MAROON
Write-Host ""

$ProjectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $ProjectRoot

function Find-Adb {
    $cmd = Get-Command adb -ErrorAction SilentlyContinue
    if ($cmd) { return $cmd.Source }

    $candidates = @(
        "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe",
        "$env:ANDROID_HOME\platform-tools\adb.exe",
        "$env:ANDROID_SDK_ROOT\platform-tools\adb.exe"
    )

    foreach ($candidate in $candidates) {
        if ($candidate -and (Test-Path $candidate)) {
            return $candidate
        }
    }

    throw "ADB not found. Install Android SDK Platform-Tools or add adb.exe to PATH."
}

$adb = Find-Adb

Write-Host "[1/5] Checking ADB device..." -ForegroundColor Yellow
& $adb start-server | Out-Null
$devices = & $adb devices
$deviceLines = $devices | Select-String "`tdevice$"

if (-not $deviceLines) {
    Write-Host ""
    Write-Host "No authorized Android device found." -ForegroundColor Red
    Write-Host "Reconnect USB, enable USB debugging, accept the authorization prompt, then run again."
    & $adb devices
    exit 1
}

& $adb devices

Write-Host ""
Write-Host "[2/5] Building QA APK with Google rewarded TEST ads..." -ForegroundColor Yellow
& ".\gradlew.bat" --stop | Out-Null
& ".\gradlew.bat" assembleQa
if ($LASTEXITCODE -ne 0) {
    throw "assembleQa failed."
}

$apk = Join-Path $ProjectRoot "app\build\outputs\apk\qa\app-qa.apk"
if (-not (Test-Path $apk)) {
    $apk = Get-ChildItem -Path (Join-Path $ProjectRoot "app\build\outputs\apk\qa") -Filter "*.apk" -Recurse |
        Sort-Object LastWriteTime -Descending |
        Select-Object -First 1 -ExpandProperty FullName
}

if (-not $apk -or -not (Test-Path $apk)) {
    throw "QA APK was not found after a successful build."
}

$apkSizeMb = [math]::Round((Get-Item $apk).Length / 1MB, 1)
Write-Host "APK: $apk ($apkSizeMb MB)" -ForegroundColor Green

Write-Host ""
Write-Host "[3/5] Checking phone storage / package manager..." -ForegroundColor Yellow
& $adb shell df -h /data 2>$null
& $adb shell pm list packages 1>$null
if ($LASTEXITCODE -ne 0) {
    Write-Host "Package manager did not respond cleanly. Restarting ADB..." -ForegroundColor DarkYellow
    & $adb kill-server | Out-Null
    Start-Sleep -Seconds 2
    & $adb start-server | Out-Null
    Start-Sleep -Seconds 2
    & $adb wait-for-device
}

Write-Host ""
Write-Host "[4/5] Installing QA APK directly with ADB..." -ForegroundColor Yellow

function Install-Apk {
    param([string]$ApkPath)
    $output = & $adb install -r -t $ApkPath 2>&1
    $code = $LASTEXITCODE
    $output | ForEach-Object { Write-Host $_ }
    return @{ Code = $code; Output = ($output -join "`n") }
}

$result = Install-Apk $apk

if ($result.Code -ne 0) {
    Write-Host ""
    Write-Host "First install attempt failed. Performing ADB recovery..." -ForegroundColor DarkYellow
    & $adb kill-server | Out-Null
    Start-Sleep -Seconds 3
    & $adb start-server | Out-Null
    & $adb wait-for-device
    Start-Sleep -Seconds 2

    # Clear stale Package Installer sessions if Android exposes them.
    & $adb shell pm install-abandon 2>$null | Out-Null

    $result = Install-Apk $apk
}

if ($result.Code -ne 0) {
    Write-Host ""
    Write-Host "Direct ADB install still failed." -ForegroundColor Red
    Write-Host ""
    Write-Host "Run these diagnostics and send me the output:" -ForegroundColor Yellow
    Write-Host "  adb devices -l"
    Write-Host "  adb shell df -h /data"
    Write-Host "  adb shell dumpsys package | Select-String -Pattern `"installer|staging`""
    Write-Host "  adb install -r -t `"$apk`""
    exit 1
}

Write-Host ""
Write-Host "[5/5] Launching app..." -ForegroundColor Yellow

# Try to resolve the installed QA package automatically.
$packageCandidates = & $adb shell pm list packages | ForEach-Object {
    ($_ -replace "^package:", "").Trim()
} | Where-Object {
    $_ -match "brick.*breaker|brick_breaker_ball"
}

$packageName = $packageCandidates | Select-Object -First 1

if ($packageName) {
    Write-Host "Installed package: $packageName" -ForegroundColor Green
    & $adb shell monkey -p $packageName -c android.intent.category.LAUNCHER 1 | Out-Null
} else {
    Write-Host "APK installed, but package name could not be auto-detected." -ForegroundColor DarkYellow
    Write-Host "Launch BrickBreakerBall manually from the phone."
}

Write-Host ""
Write-Host "==============================================" -ForegroundColor Green
Write-Host " QA INSTALL SUCCESSFUL" -ForegroundColor Green
Write-Host "==============================================" -ForegroundColor Green
Write-Host ""
Write-Host "Test path:" -ForegroundColor MAROON
Write-Host "  SHOP -> FREE -> WATCH AD"
Write-Host ""
Write-Host "QA should use Google's official Rewarded TEST creative."
Write-Host "Reward must be granted only after the earned-reward callback."
