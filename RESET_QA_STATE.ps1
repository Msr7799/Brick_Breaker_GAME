$ErrorActionPreference = "Stop"
Write-Host "This clears ONLY the QA package data (com.example.brick_breaker_ball.qa)."
& adb shell pm clear com.example.brick_breaker_ball.qa
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
Write-Host "QA state cleared. Starter ball + starter paddle family will be the only cosmetics owned on first launch."
