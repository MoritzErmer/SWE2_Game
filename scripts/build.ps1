param(
    [switch]$SkipTests
)

$ErrorActionPreference = "Stop"

$testArg = ""
if ($SkipTests) {
    $testArg = "-DskipTests"
}

mvn clean package $testArg
