<#
 Copyright 2026 the Operaton contributors.

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at:

     https://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
#>

$ErrorActionPreference = 'Stop'

$comparisonVersion = ''
$runner = 'mvnw.cmd'

$validRunners = @('mvn', 'mvnw.cmd', 'mvnd')

function Show-Help {
  Write-Host 'Usage: check-api-compatibility.ps1 [--comparison-version <version>] [--runner=<runner>] [--help]'
  Write-Host ''
  Write-Host 'Options:'
  Write-Host '  --comparison-version <version>   Set the clirr comparison version (overrides default)'
  Write-Host '  --runner=<runner>                Use mvn, mvnd or mvnw (default: mvnw)'
  Write-Host '  --help                           Show this help message and exit'
}

function Check-ValidValue {
  param(
    [Parameter(Mandatory = $true)]
    [string]$ParameterName,
    [Parameter(Mandatory = $true)]
    [string]$Value,
    [Parameter(Mandatory = $true)]
    [string[]]$ValidValues
  )

  if ($ValidValues -contains $Value) {
    return
  }

  throw "Argument '$ParameterName' must be one of: [$($ValidValues -join ' ')], but was '$Value'"
}

function Resolve-Runner {
  param(
    [Parameter(Mandatory = $true)]
    [string]$SelectedRunner
  )

  if ($SelectedRunner -eq 'mvnw.cmd') {
    if (Test-Path -LiteralPath '.\mvnw.cmd') {
      return '.\mvnw.cmd'
    }
  } elseif (Get-Command $SelectedRunner -ErrorAction SilentlyContinue) {
    return $SelectedRunner
  }

  Write-Host "WARNING: Runner '$SelectedRunner' not found. Falling back to 'mvnw.cmd'."
  if (Test-Path -LiteralPath '.\mvnw.cmd') {
    return '.\mvnw.cmd'
  }

  throw "Runner 'mvnw.cmd' not found in repository root."
}

function Invoke-Maven {
  param(
    [Parameter(Mandatory = $true)]
    [string]$Runner,
    [Parameter(Mandatory = $true)]
    [string[]]$Arguments
  )

  Write-Host "$Runner $($Arguments -join ' ')"
  & $Runner @Arguments

  if ($LASTEXITCODE -ne 0) {
    throw "Command failed with exit code $LASTEXITCODE"
  }
}

function Normalize-RelativePath {
  param(
    [Parameter(Mandatory = $true)]
    [string]$BasePath,
    [Parameter(Mandatory = $true)]
    [string]$TargetPath
  )

  return [System.IO.Path]::GetRelativePath($BasePath, $TargetPath).Replace('\', '/')
}

$showHelp = $false
for ($i = 0; $i -lt $args.Count; $i++) {
  $arg = $args[$i]

  if ($arg -eq '--comparison-version') {
    if ($i + 1 -ge $args.Count) {
      throw "Missing value for '$arg'"
    }
    $i++
    $comparisonVersion = $args[$i]
    continue
  }
  if ($arg -match '^--comparison-version=(.+)$') {
    $comparisonVersion = $Matches[1]
    continue
  }
  if ($arg -eq '--help') {
    $showHelp = $true
    continue
  }
  if ($arg -match '^--runner=(.+)$') {
    switch ($Matches[1]) {
      'mvn' { $runner = 'mvn' }
      'mvnd' { $runner = 'mvnd' }
      'mvnw' { $runner = 'mvnw.cmd' }
      'mvnw.cmd' { $runner = 'mvnw.cmd' }
    }
  }
}

if ($showHelp) {
  Show-Help
  exit 0
}

try {
  Check-ValidValue -ParameterName 'runner' -Value $runner -ValidValues $validRunners

  $repoRoot = (git rev-parse --show-toplevel 2>$null)
  if (-not $repoRoot) {
    throw 'Could not determine git repository root.'
  }
  $repoRoot = $repoRoot.Trim()

  Push-Location $repoRoot
  try {
    $runner = Resolve-Runner -SelectedRunner $runner
    $modules = @(
      'model-api/bpmn-model',
      'model-api/cmmn-model',
      'model-api/dmn-model',
      'model-api/xml-model',
      'engine',
      'engine-dmn/engine',
      'engine-dmn/feel-api',
      'engine-dmn/feel-juel',
      'connect/http-client',
      'connect/soap-http-client',
      'spin/core',
      'spin/dataformat-json-jackson',
      'spin/dataformat-xml-dom'
    ) -join ','

    $mavenArgs = @(
      '-am',
      '-Pcheck-api-compatibility',
      '-DskipTests',
      '-Dskip.frontend.build=true',
      '-pl',
      $modules
    )
    if ($comparisonVersion) {
      $mavenArgs += "-Dclirr.comparisonVersion=$comparisonVersion"
    }

    Write-Host 'INFO: Checking API compatibility with previous release...'
    Invoke-Maven -Runner $runner -Arguments $mavenArgs

    $clirrDir = Join-Path (Get-Location).Path 'target/reports/clirr'
    if (-not (Test-Path -LiteralPath $clirrDir)) {
      throw "Clirr report directory '$clirrDir' does not exist."
    }

    Get-ChildItem -Path $clirrDir -Recurse -File |
      Where-Object { $_.Length -eq 0 } |
      ForEach-Object {
        Remove-Item -LiteralPath $_.FullName -Force
        Write-Host "[INFO] Removed empty clirr report: $($_.FullName)"
      }

    $mdFile = Join-Path $clirrDir 'clirr.md'
    Set-Content -Path $mdFile -Value '## all'

    $allDir = Join-Path $clirrDir 'all'
    if (Test-Path -LiteralPath $allDir) {
      Get-ChildItem -Path $allDir -Recurse -File -Filter '*.txt' |
        Where-Object { $_.Length -gt 0 } |
        Sort-Object FullName |
        ForEach-Object {
          $relative = Normalize-RelativePath -BasePath $clirrDir -TargetPath $_.FullName
          $display = $relative -replace '^all/', ''
          Add-Content -Path $mdFile -Value "- [$display]($relative)"
        }
    }

    Add-Content -Path $mdFile -Value ''
    Add-Content -Path $mdFile -Value '## restrictive'

    $restrictiveDir = Join-Path $clirrDir 'restrictive'
    if (Test-Path -LiteralPath $restrictiveDir) {
      Get-ChildItem -Path $restrictiveDir -Recurse -File -Filter '*.txt' |
        Where-Object { $_.Length -gt 0 } |
        Sort-Object FullName |
        ForEach-Object {
          $relative = Normalize-RelativePath -BasePath $clirrDir -TargetPath $_.FullName
          $display = $relative -replace '^restrictive/', ''
          Add-Content -Path $mdFile -Value "- [$display]($relative)"
        }
    }

    $targetZipFile = Join-Path (Get-Location).Path 'target/clirr-reports.zip'
    if (Test-Path -LiteralPath $targetZipFile) {
      Remove-Item -LiteralPath $targetZipFile -Force
    }
    Compress-Archive -Path (Join-Path $clirrDir '*') -DestinationPath $targetZipFile -Force
    Write-Host "INFO: Clirr reports zipped to: $targetZipFile"
    Write-Host 'INFO: Done!'
  } finally {
    Pop-Location
  }
} catch {
  [Console]::Error.WriteLine("ERROR: $($_.Exception.Message)")
  exit 1
}

exit 0
