param(
    [string]$AppVersion,
    [string]$Vendor = "DHBW Stuttgart"
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

function Assert-WixInstalled {
    $inPath = Get-Command "candle.exe" -ErrorAction SilentlyContinue
    $inDefaultDir = Get-ChildItem "C:\Program Files (x86)\WiX Toolset*\bin\candle.exe" `
                        -ErrorAction SilentlyContinue | Select-Object -First 1
    if (-not $inPath -and -not $inDefaultDir) {
        throw "WiX Toolset 3.x nicht gefunden. jpackage benoetigt WiX fuer den Typ 'exe'.`nDownload: https://wixtoolset.org/releases/ — danach PATH aktualisieren."
    }
}

Assert-Command -CommandName "mvn"      -HelpText "Install Maven and ensure it is in PATH."
Assert-Command -CommandName "jpackage" -HelpText "Use JDK 17+ and ensure jpackage is in PATH."
Assert-Command -CommandName "jdeps"    -HelpText "Use JDK 17+ and ensure jdeps is in PATH."
Assert-Command -CommandName "jlink"    -HelpText "Use JDK 17+ and ensure jlink is in PATH."
Assert-WixInstalled

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

# --- jdeps: erforderliche Java-Module ermitteln ---
Write-Host "[package-exe] Analysiere Java-Module mit jdeps ..."
$jdepsOutput = & jdeps --print-module-deps --ignore-missing-deps $jarPath 2>$null
# jdeps gibt mehrzeilige Ausgabe; die letzte nicht-leere Zeile enthaelt die Modulliste
$jdepsModules = ($jdepsOutput | Where-Object { $_ -match '\S' } | Select-Object -Last 1).Trim()
if ([string]::IsNullOrWhiteSpace($jdepsModules)) {
    Write-Warning "[package-exe] jdeps lieferte kein Ergebnis — nur Baseline-Module werden verwendet."
}

# Mandatory baseline: always required for this Swing application.
# jdeps with --ignore-missing-deps can miss runtime modules like java.logging
# (used via java.util.logging.Logger in Main.java).
$baseline = @("java.base", "java.desktop", "java.logging", "java.management")
$detected = if (-not [string]::IsNullOrWhiteSpace($jdepsModules)) { $jdepsModules -split "," } else { @() }
$modules = ($baseline + $detected | Sort-Object -Unique) -join ","
Write-Host "[package-exe] Module (baseline + jdeps): $modules"

# --- jlink: minimale JRE erstellen ---
$runtimeDir = Join-Path $outDir "runtime"
Write-Host "[package-exe] Erstelle minimale JRE mit jlink ..."
& jlink `
    --no-header-files `
    --no-man-pages `
    --compress=2 `
    --strip-debug `
    --add-modules $modules `
    --output $runtimeDir
if ($LASTEXITCODE -ne 0) {
    throw "jlink fehlgeschlagen (Exit-Code $LASTEXITCODE)."
}
$runtimeSizeMB = "{0:N1}" -f ((Get-ChildItem $runtimeDir -Recurse | Measure-Object Length -Sum).Sum / 1MB)
Write-Host "[package-exe] Minimale JRE erstellt: $runtimeDir ($runtimeSizeMB MB)"

# --- jpackage: Windows-EXE-Installer mit gebundeler JRE ---
# & operator + splatting (@array) passes each element as a separate argument.
# PowerShell automatically quotes values that contain spaces — no manual escaping needed.
$inputDir = Join-Path $PSScriptRoot "..\target"
$jpackageArgs = @(
    "--type",           "exe",
    "--name",           "FactoryGame",
    "--input",          $inputDir,
    "--main-jar",       $jarName,
    "--main-class",     "game.Main",
    "--dest",           $outDir,
    "--vendor",         $Vendor,
    "--app-version",    $AppVersion,
    "--runtime-image",  $runtimeDir,
    "--win-shortcut",
    "--win-menu",
    "--win-dir-chooser"
)

# Direct invocation via & operator: output (including jpackage error messages) streams
# natively to the console / GitHub Actions log. Timeout is enforced at the job level
# (timeout-minutes: 20 in release-gate.yml).
Write-Host "[package-exe] Running jpackage ..."
& jpackage @jpackageArgs
if ($LASTEXITCODE -ne 0) {
    throw "jpackage failed with exit code $LASTEXITCODE."
}

$exeFile = Get-ChildItem -Path $outDir -Filter "*.exe" -ErrorAction SilentlyContinue | Select-Object -First 1
if ($null -eq $exeFile) {
    throw "Packaging finished but no .exe artifact was found in '$outDir'."
}

# Rename FactoryGame-X.Y.Z.exe → FactoryGame_X.Y.Z.exe (underscore convention)
$renamedName = $exeFile.Name -replace "^FactoryGame-", "FactoryGame_"
if ($renamedName -ne $exeFile.Name) {
    $renamedPath = Join-Path $outDir $renamedName
    Rename-Item -Path $exeFile.FullName -NewName $renamedName
    Write-Host "[package-exe] EXE created successfully: $renamedPath"
} else {
    Write-Host "[package-exe] EXE created successfully: $($exeFile.FullName)"
}
