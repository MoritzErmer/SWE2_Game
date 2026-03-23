param(
    [string]$AppVersion,
    [string]$Vendor = "DHBW Stuttgart",
    [int]$PackageTimeoutSeconds = 300
)

$ErrorActionPreference = "Stop"

function Get-VersionFromPom {
    param([string]$PomPath)

    if (-not (Test-Path $PomPath)) {
        throw "pom.xml not found at '$PomPath'."
    }

    [xml]$pomXml = Get-Content $PomPath
    $ns = New-Object System.Xml.XmlNamespaceManager($pomXml.NameTable)
    $ns.AddNamespace("m", "http://maven.apache.org/POM/4.0.0")
    $versionNode = $pomXml.SelectSingleNode("/m:project/m:version", $ns)

    if ($null -eq $versionNode -or [string]::IsNullOrWhiteSpace($versionNode.InnerText)) {
        throw "Could not resolve version from pom.xml."
    }

    return $versionNode.InnerText.Trim()
}

function Assert-Command {
    param(
        [string]$CommandName,
        [string]$HelpText
    )

    if (-not (Get-Command $CommandName -ErrorAction SilentlyContinue)) {
        throw "$CommandName not found. $HelpText"
    }
}

Assert-Command -CommandName "mvn" -HelpText "Install Maven and ensure it is in PATH."
Assert-Command -CommandName "jpackage" -HelpText "Use JDK 17+ and ensure jpackage is in PATH."

$pomPath = Join-Path $PSScriptRoot "..\pom.xml"
if ([string]::IsNullOrWhiteSpace($AppVersion)) {
    $AppVersion = Get-VersionFromPom -PomPath $pomPath
}

Write-Host "[package-exe] Building project with version $AppVersion ..."
mvn clean package -DskipTests

$jarName = "swe2-game-$AppVersion.jar"
$jarPath = Join-Path $PSScriptRoot "..\target\$jarName"
if (-not (Test-Path $jarPath)) {
    throw "Expected JAR not found: $jarPath"
}

$outDir = Join-Path $PSScriptRoot "..\dist"
if (Test-Path $outDir) {
    Remove-Item -Recurse -Force $outDir
}
New-Item -ItemType Directory -Path $outDir | Out-Null

$jpackageArgs = @(
    "--type", "exe",
    "--name", "SWE2-Game",
    "--input", (Join-Path $PSScriptRoot "..\target"),
    "--main-jar", $jarName,
    "--main-class", "game.Main",
    "--dest", $outDir,
    "--vendor", $Vendor,
    "--app-version", $AppVersion,
    "--win-shortcut",
    "--win-menu"
)

Write-Host "[package-exe] Running jpackage (timeout: $PackageTimeoutSeconds sec) ..."
$proc = Start-Process -FilePath "jpackage" -ArgumentList $jpackageArgs -PassThru -NoNewWindow

if (-not $proc.WaitForExit($PackageTimeoutSeconds * 1000)) {
    try {
        $proc.Kill()
    } catch {
        Write-Warning "Failed to kill timed-out jpackage process."
    }
    throw "jpackage timed out after $PackageTimeoutSeconds seconds."
}

if ($proc.ExitCode -ne 0) {
    throw "jpackage failed with exit code $($proc.ExitCode)."
}

$exeFile = Get-ChildItem -Path $outDir -Filter "*.exe" -ErrorAction SilentlyContinue | Select-Object -First 1
if ($null -eq $exeFile) {
    throw "Packaging finished but no .exe artifact was found in '$outDir'."
}

Write-Host "[package-exe] EXE created successfully: $($exeFile.FullName)"
