$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest
Set-Location $PSScriptRoot

function Invoke-GradleStep {
    param(
        [Parameter(Mandatory = $true)][string]$Label,
        [Parameter(Mandatory = $true)][string[]]$Tasks
    )

    Write-Host "" 
    Write-Host $Label -ForegroundColor MAROON
    & .\gradlew.bat @Tasks
    if ($LASTEXITCODE -ne 0) {
        Write-Host "" 
        Write-Host "BUILD FAILED at: $Label" -ForegroundColor Red
        exit $LASTEXITCODE
    }
}

Invoke-GradleStep "[1/5] Cleaning" @('clean')
Invoke-GradleStep "[2/5] Unit tests" @('test')
Invoke-GradleStep "[3/5] Android lint" @('lintRelease')
Invoke-GradleStep "[4/5] Release APK" @('assembleRelease')
Invoke-GradleStep "[5/5] Release AAB" @('bundleRelease')

$apk = Get-ChildItem -Path .\app\build\outputs\apk\release\*.apk -ErrorAction SilentlyContinue | Select-Object -First 1
$aab = Get-ChildItem -Path .\app\build\outputs\bundle\release\*.aab -ErrorAction SilentlyContinue | Select-Object -First 1

if (-not $apk) { throw 'Release APK was not produced.' }
if (-not $aab) { throw 'Release AAB was not produced.' }

Write-Host ""
Write-Host "BUILD VERIFIED" -ForegroundColor Green
Write-Host "APK: $($apk.FullName)"
Write-Host "AAB: $($aab.FullName)"
