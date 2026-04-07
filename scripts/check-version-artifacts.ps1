$ErrorActionPreference = "Stop"

$root = Join-Path $PSScriptRoot ".."
$pomPath = Join-Path $root "pom.xml"
$runScriptPath = Join-Path $root "scripts\run.ps1"
$packageScriptPath = Join-Path $root "scripts\package-exe.ps1"

foreach ($path in @($pomPath, $runScriptPath, $packageScriptPath)) {
    if (-not (Test-Path $path)) {
        throw "Missing required file: $path"
    }
}

[xml]$pomXml = Get-Content $pomPath
$ns = New-Object System.Xml.XmlNamespaceManager($pomXml.NameTable)
$ns.AddNamespace("m", "http://maven.apache.org/POM/4.0.0")
$versionNode = $pomXml.SelectSingleNode("/m:project/m:version", $ns)
if ($null -eq $versionNode -or [string]::IsNullOrWhiteSpace($versionNode.InnerText)) {
    throw "Could not read project version from pom.xml"
}
$pomVersion = $versionNode.InnerText.Trim()

$runScript = Get-Content $runScriptPath -Raw
$packageScript = Get-Content $packageScriptPath -Raw

# The run/package scripts must use version resolution from pom.xml and not hardcoded JAR versions.
if ($runScript -notmatch "Get-VersionFromPom") {
    throw "run.ps1 must resolve version from pom.xml."
}
if ($packageScript -notmatch "Get-VersionFromPom") {
    throw "package-exe.ps1 must resolve version from pom.xml."
}
if ($runScript -match "swe2-game-\d+\.\d+\.\d+\.jar") {
    throw "run.ps1 contains hardcoded versioned JAR name."
}
if ($packageScript -match 'AppVersion\s*=\s*"\d+\.\d+\.\d+"') {
    throw "package-exe.ps1 contains hardcoded AppVersion default."
}

Write-Host "[check-version-artifacts] OK: version and artifact references are consistent."
Write-Host "[check-version-artifacts] POM version: $pomVersion"
