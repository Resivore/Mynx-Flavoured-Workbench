[CmdletBinding()]
param(
    [string]$JavaHome,
    [string]$GradlePath,
    [string]$ReferenceWorkbenchRoot
)

$ErrorActionPreference = 'Stop'
$arguments = @{}
if (-not [string]::IsNullOrWhiteSpace($JavaHome)) { $arguments.JavaHome = $JavaHome }
if (-not [string]::IsNullOrWhiteSpace($GradlePath)) { $arguments.GradlePath = $GradlePath }
if (-not [string]::IsNullOrWhiteSpace($ReferenceWorkbenchRoot)) { $arguments.ReferenceWorkbenchRoot = $ReferenceWorkbenchRoot }

& (Join-Path $PSScriptRoot 'Build-Canary1.ps1') @arguments
if (-not $?) { throw 'Canary 4 build failed.' }
