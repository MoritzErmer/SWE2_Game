$ErrorActionPreference = "Stop"

function Get-VersionFromPom {
	param([string]$PomPath)

	[xml]$pomXml = Get-Content $PomPath
	$ns = New-Object System.Xml.XmlNamespaceManager($pomXml.NameTable)
	$ns.AddNamespace("m", "http://maven.apache.org/POM/4.0.0")
	$versionNode = $pomXml.SelectSingleNode("/m:project/m:version", $ns)

	if ($null -eq $versionNode -or [string]::IsNullOrWhiteSpace($versionNode.InnerText)) {
		throw "Could not resolve project version from pom.xml"
	}

	return $versionNode.InnerText.Trim()
}

Set-Location (Join-Path $PSScriptRoot "..")
if (-not (Test-Path "pom.xml")) {
	throw "pom.xml not found in project root."
}

$version = Get-VersionFromPom -PomPath "pom.xml"

& mvn clean package "-Dmaven.test.skip=true"

$jarPath = "target/swe2-game-$version.jar"
if (-not (Test-Path $jarPath)) {
	throw "Expected JAR not found: $jarPath"
}

java -jar $jarPath
