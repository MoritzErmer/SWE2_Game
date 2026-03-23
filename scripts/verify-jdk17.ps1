$ErrorActionPreference = "Stop"

function Assert-Command {
    param(
        [string]$CommandName,
        [string]$Hint
    )

    if (-not (Get-Command $CommandName -ErrorAction SilentlyContinue)) {
        throw "$CommandName not found. $Hint"
    }
}

Assert-Command -CommandName "java" -Hint "Install JDK 17+ and add it to PATH."
Assert-Command -CommandName "javac" -Hint "Install JDK 17+ and add it to PATH."
Assert-Command -CommandName "jpackage" -Hint "Use a JDK distribution that includes jpackage."
Assert-Command -CommandName "mvn" -Hint "Install Maven and add it to PATH."

$javaVersionOutput = & java -version 2>&1
$versionLine = $javaVersionOutput | Select-Object -First 1

if ($versionLine -notmatch '"(\d+)') {
    throw "Could not parse Java version from output: $versionLine"
}

$major = [int]$Matches[1]
if ($major -lt 17) {
    throw "Java $major detected. JDK 17 or newer is required."
}

Write-Host "[verify-jdk17] Java line: $versionLine"
Write-Host "[verify-jdk17] Environment OK (Java $major, javac, jpackage, mvn found)."
