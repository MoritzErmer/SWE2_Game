$ErrorActionPreference = "Stop"

$root = Join-Path $PSScriptRoot ".."
$taskFile = Join-Path $root "Aufgabenstellung.md"
$typstFile = Join-Path $root "docs\typst\main.typ"
$traceFile = Join-Path $root "REQUIREMENTS_TRACEABILITY.md"

if (-not (Test-Path $taskFile)) { throw "Missing file: $taskFile" }
if (-not (Test-Path $typstFile)) { throw "Missing file: $typstFile" }
if (-not (Test-Path $traceFile)) { throw "Missing file: $traceFile" }

$task = Get-Content $taskFile -Raw
$typst = Get-Content $typstFile -Raw
$trace = Get-Content $traceFile -Raw

$requiredTypstHeadings = @(
    "== 1 Projektidee",
    "== 2 Spielregeln und Gameplay",
    "== 3 Multithreading-Charakter",
    "== 4 Anforderungen (Requirements)",
    "== 5 Architektur und Design Patterns",
    "== 6 Verlaesslichkeit",
    "== 7 Testkonzept und Nachweisstruktur",
    "== 8 Build, Standalone und EXE-Bereitstellung"
)

foreach ($heading in $requiredTypstHeadings) {
    if ($typst -notmatch [Regex]::Escape($heading)) {
        throw "Missing required Typst section heading: $heading"
    }
}

# Ensure all four reliability questions are represented in documentation.
$reliabilitySections = @(
    "=== 6.1 Frage 1",
    "=== 6.2 Frage 2",
    "=== 6.3 Frage 3",
    "=== 6.4 Frage 4"
)
foreach ($section in $reliabilitySections) {
    if ($typst -notmatch [Regex]::Escape($section)) {
        throw "Missing reliability subsection in docs: $section"
    }
}

# Ensure requirements F-01..F-10 are present.
for ($i = 1; $i -le 10; $i++) {
    $id = "F-{0:D2}" -f $i
    if ($typst -notmatch [Regex]::Escape($id)) {
        throw "Missing requirement ID in docs: $id"
    }
    if ($trace -notmatch [Regex]::Escape($id)) {
        throw "Missing requirement ID in traceability matrix: $id"
    }
}

# Ensure traceability references tasks + docs sections.
if ($trace -notmatch "Aufgabenstellung") {
    throw "Traceability file must reference Aufgabenstellung source."
}
if ($trace -notmatch "docs/typst/main.typ") {
    throw "Traceability file must reference Typst documentation."
}

Write-Host "[check-doc-consistency] OK: Aufgabenstellung, Typst and traceability are consistent."
