$ErrorActionPreference='Stop'
$projectRoot=(Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$candidates=@()
if($env:GRADLE_HOME){$candidates+=Join-Path $env:GRADLE_HOME 'bin\gradle.bat'}
$command=Get-Command gradle.bat,gradle -ErrorAction SilentlyContinue|Select-Object -First 1
if($command){$candidates+=$command.Source}
$candidates+=Join-Path $env:TEMP 'gradle-9.5.1\gradle-9.5.1\bin\gradle.bat'
$gradle=$candidates|Where-Object{$_ -and (Test-Path -LiteralPath $_ -PathType Leaf)}|Select-Object -First 1
if(-not $gradle){throw 'Gradle 9.5.1 is unavailable. Set GRADLE_HOME or install the project Gradle runtime before deployment.'}
$javaHomeCandidate=$env:JAVA_HOME
if(-not $javaHomeCandidate -or -not (Test-Path -LiteralPath (Join-Path $javaHomeCandidate 'bin\java.exe'))){
    $javaHomeCandidate=Join-Path $env:TEMP 'temurin-jdk-25\jdk-25.0.3+9'
}
if(-not (Test-Path -LiteralPath (Join-Path $javaHomeCandidate 'bin\java.exe'))){throw 'Java 25 is unavailable. Set JAVA_HOME to a Java 25 JDK before deployment.'}
$env:JAVA_HOME=$javaHomeCandidate
$env:Path=(Join-Path $javaHomeCandidate 'bin')+';'+$env:Path
Push-Location $projectRoot
try{
    & $gradle buildDeployment
    if($LASTEXITCODE-ne 0){exit $LASTEXITCODE}
}finally{Pop-Location}
