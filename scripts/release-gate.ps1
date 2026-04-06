$ErrorActionPreference = "Stop"

Set-Location (Join-Path $PSScriptRoot "..")

Write-Host "[release-gate] Running version/artifact consistency checks ..."
.\scripts\check-version-artifacts.ps1

Write-Host "[release-gate] Running test suite ..."
mvn clean test

Write-Host "[release-gate] Building EXE package ..."
.\scripts\package-exe.ps1

Write-Host "[release-gate] SUCCESS: All checks passed."
