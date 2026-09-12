$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $root
Write-Host "[1/3] Building QA with Google's official Rewarded test IDs..."
& .\gradlew.bat clean assembleQa
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
Write-Host "[2/3] Installing QA build on the connected Android device..."
& .\gradlew.bat installQa
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
Write-Host "[3/3] Launching QA app..."
& adb shell monkey -p com.example.brick_breaker_ball.qa -c android.intent.category.LAUNCHER 1 | Out-Null
Write-Host "Open SHOP -> FREE DAILY DROP -> WATCH AD +1. A Google TEST rewarded creative should appear."
