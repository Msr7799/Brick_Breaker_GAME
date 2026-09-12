$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest
Set-Location $PSScriptRoot

function Invoke-GradleStep {
    param([string]$Label, [string[]]$Tasks)
    Write-Host ""
    Write-Host $Label -ForegroundColor MAROON
    & .\gradlew.bat @Tasks
    if ($LASTEXITCODE -ne 0) {
        Write-Host "FAILED: $Label" -ForegroundColor Red
        exit $LASTEXITCODE
    }
}

Invoke-GradleStep "[1/3] Compile + QA APK" @('clean', 'assembleQa')
Invoke-GradleStep "[2/3] QA lint" @('lintQa')
Invoke-GradleStep "[3/3] Install QA on connected Android device" @('installQa')

$adb = Get-Command adb -ErrorAction SilentlyContinue
if ($adb) {
    & adb shell am force-stop com.example.brick_breaker_ball.qa | Out-Null
    & adb shell monkey -p com.example.brick_breaker_ball.qa -c android.intent.category.LAUNCHER 1 | Out-Null
}

Write-Host ""
Write-Host "QA BUILD INSTALLED" -ForegroundColor Green
Write-Host "- Level Editor is visible from the main menu."
Write-Host "- Google rewarded ads use Google's official TEST ad unit."
Write-Host "- Release still refuses Google sample ad IDs."
