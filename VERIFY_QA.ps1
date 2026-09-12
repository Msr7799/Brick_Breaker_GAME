$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest
Set-Location $PSScriptRoot
function Step([string]$Label,[string[]]$Tasks) {
  Write-Host ""; Write-Host $Label -ForegroundColor MAROON
  & .\gradlew.bat @Tasks
  if ($LASTEXITCODE -ne 0) { throw "Failed: $Label" }
}
Step '[1/4] Unit tests' @('test')
Step '[2/4] QA lint' @('lintQa')
Step '[3/4] QA APK' @('assembleQa')
Step '[4/4] Debug APK' @('assembleDebug')
Write-Host ''; Write-Host 'QA BUILD VERIFIED' -ForegroundColor Green
