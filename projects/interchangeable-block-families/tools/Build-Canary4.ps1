[CmdletBinding()]
param(
    [string]$JavaHome,
    [string]$GradlePath,
    [string]$CnmReferenceJar,
    [string]$QsnReferenceJar,
    [string]$QsnCompatReferenceJar
)

$ErrorActionPreference = 'Stop'
$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$repositoryRoot = (Resolve-Path (Join-Path $projectRoot '..\..')).Path

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
if ($LASTEXITCODE -ne 0 -or ($javaVersion -join "`n") -notmatch '^(?:openjdk|java) 25\.') {
    throw "Java 25 is required: $($javaVersion -join ' ')"
}

if ([string]::IsNullOrWhiteSpace($GradlePath)) {
    $GradlePath = Join-Path $repositoryRoot 'projects\custom-portals\gradlew.bat'
}
if (-not (Test-Path -LiteralPath $GradlePath -PathType Leaf)) {
    throw "The Workbench Gradle wrapper is missing: $GradlePath"
}

$previousJavaHome = $env:JAVA_HOME
$previousPath = $env:Path
try {
    $env:JAVA_HOME = $JavaHome
    $env:Path = "$(Join-Path $JavaHome 'bin');$previousPath"
    $gradleArguments = @('-p', $projectRoot)
    if (-not [string]::IsNullOrWhiteSpace($CnmReferenceJar)) {
        $gradleArguments += "-Pcnm_reference_jar=$CnmReferenceJar"
    }
    if (-not [string]::IsNullOrWhiteSpace($QsnReferenceJar)) {
        $gradleArguments += "-Pqsn_reference_jar=$QsnReferenceJar"
    }
    if (-not [string]::IsNullOrWhiteSpace($QsnCompatReferenceJar)) {
        $gradleArguments += "-Pqsn_compat_reference_jar=$QsnCompatReferenceJar"
    }
    $gradleArguments += @('clean', 'test', 'runGameTest', 'build', '--no-daemon')
    & $GradlePath @gradleArguments
    if ($LASTEXITCODE -ne 0) { throw "Gradle build failed with exit code $LASTEXITCODE" }
}
finally {
    $env:JAVA_HOME = $previousJavaHome
    $env:Path = $previousPath
}
