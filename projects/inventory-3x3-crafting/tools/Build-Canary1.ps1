[CmdletBinding()]
param(
    [string]$JavaHome,
    [string]$GradlePath,
    [string]$ReferenceWorkbenchRoot
)

$ErrorActionPreference = 'Stop'
$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$projectCache = Join-Path $env:TEMP 'inventory-3x3-crafting-gradle-project-cache'
if ([string]::IsNullOrWhiteSpace($ReferenceWorkbenchRoot)) {
    $ReferenceWorkbenchRoot = (Resolve-Path (Join-Path $projectRoot '..\..')).Path
}

if ([string]::IsNullOrWhiteSpace($JavaHome)) {
    $javaCandidates = @(Get-ChildItem -LiteralPath $env:TEMP -Directory -Filter 'temurin-jdk25*' -ErrorAction SilentlyContinue |
        ForEach-Object { Get-ChildItem -LiteralPath $_.FullName -Directory -ErrorAction SilentlyContinue } |
        Where-Object { Test-Path -LiteralPath (Join-Path $_.FullName 'bin\javac.exe') } |
        Sort-Object FullName -Descending)
    if ($javaCandidates.Count -eq 0) { throw 'A complete Java 25 JDK with javac was not found.' }
    $JavaHome = $javaCandidates[0].FullName
}
if (-not (Test-Path -LiteralPath (Join-Path $JavaHome 'bin\javac.exe') -PathType Leaf)) {
    throw "JAVA_HOME is not a complete JDK: $JavaHome"
}
$javaVersion = & (Join-Path $JavaHome 'bin\java.exe') --version
if ($LASTEXITCODE -ne 0) { throw 'Unable to query the selected Java runtime.' }
if (($javaVersion -join "`n") -notmatch '^(?:openjdk|java) 25\.') {
    throw "Java 25 is required: $($javaVersion -join ' ')"
}

if ([string]::IsNullOrWhiteSpace($GradlePath)) {
    $gradleCandidates = @(Get-ChildItem -LiteralPath $env:TEMP -Directory -Filter 'gradle-*' -ErrorAction SilentlyContinue |
        ForEach-Object { Get-ChildItem -LiteralPath $_.FullName -Filter gradle.bat -File -Recurse -ErrorAction SilentlyContinue } |
        Sort-Object FullName -Descending)
    if ($gradleCandidates.Count -eq 0) { throw 'A Gradle distribution was not found.' }
    $GradlePath = $gradleCandidates[0].FullName
}
if (-not (Test-Path -LiteralPath $GradlePath -PathType Leaf)) { throw "Gradle is missing: $GradlePath" }

& (Join-Path $PSScriptRoot 'Verify-UpstreamContract.ps1') -WorkbenchRoot $ReferenceWorkbenchRoot
if (-not $?) { throw 'Upstream contract verification failed.' }

$previousJavaHome = $env:JAVA_HOME
$previousPath = $env:Path
try {
    $env:JAVA_HOME = $JavaHome
    $env:Path = "$(Join-Path $JavaHome 'bin');$previousPath"
    & $GradlePath -p $projectRoot --project-cache-dir $projectCache clean test build --no-daemon "-Pworkbench_root=$ReferenceWorkbenchRoot"
    if ($LASTEXITCODE -ne 0) { throw "Gradle build failed with exit code $LASTEXITCODE" }
} finally {
    $env:JAVA_HOME = $previousJavaHome
    $env:Path = $previousPath
}
