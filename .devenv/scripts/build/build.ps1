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

$buildProfile = 'normal'
$skipTests = $false
$reportPlugins = $false
$runner = 'mvnw.cmd'
$mavenArgs = @()

$validBuildProfiles = @('fast', 'normal', 'max')
$validRunners = @('mvn', 'mvnw.cmd', 'mvnd')

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

foreach ($arg in $args) {
  if ($arg -match '^--profile=(.+)$') {
    $buildProfile = $Matches[1]
    continue
  }
  if ($arg -eq '--skip-tests') {
    $skipTests = $true
    continue
  }
  if ($arg -eq '--reports') {
    $reportPlugins = $true
    continue
  }
  if ($arg -match '^--runner=(.+)$') {
    switch ($Matches[1]) {
      'mvn' { $runner = 'mvn' }
      'mvnd' { $runner = 'mvnd' }
      'mvnw' { $runner = 'mvnw.cmd' }
      'mvnw.cmd' { $runner = 'mvnw.cmd' }
    }
    continue
  }

  $mavenArgs += $arg
}

try {
  Check-ValidValue -ParameterName 'profile' -Value $buildProfile -ValidValues $validBuildProfiles
  Check-ValidValue -ParameterName 'runner' -Value $runner -ValidValues $validRunners

  $repoRoot = (git rev-parse --show-toplevel 2>$null)
  if (-not $repoRoot) {
    throw 'Could not determine git repository root.'
  }
  $repoRoot = $repoRoot.Trim()

  Push-Location $repoRoot
  try {
    $runner = Resolve-Runner -SelectedRunner $runner
    $profiles = @()

    $mavenArgs += @('clean', 'install')

    if ($reportPlugins) {
      $projectRoot = (Get-Location).Path
      $mavenArgs += @('versions:dependency-updates-aggregate-report')
      $mavenArgs += @('versions:plugin-updates-aggregate-report')
      $mavenArgs += @('-Dsave=true', '-Ddisplay=false', 'io.github.orhankupusoglu:sloc-maven-plugin:sloc')
      $mavenArgs += @(
        '-Dbuildplan.appendOutput=true',
        "-Dbuildplan.outputFile=$projectRoot/target/reports/buildplan.txt",
        'fr.jcgay.maven.plugins:buildplan-maven-plugin:list'
      )
    }

    if ($skipTests) {
      $mavenArgs += '-DskipTests'
    }

    switch ($buildProfile) {
      'fast' {
        $profiles += @('distro', 'h2-in-memory')
      }
      'normal' {
        $profiles += @(
          'distro',
          'distro-webjar',
          'distro-run',
          'distro-tomcat',
          'h2-in-memory',
          'check-api-compatibility'
        )
      }
      'max' {
        $profiles += @(
          'distro',
          'distro-run',
          'distro-tomcat',
          'distro-wildfly',
          'distro-webjar',
          'distro-starter',
          'h2-in-memory',
          'check-api-compatibility',
          'quarkus-tests'
        )
      }
    }

    $arguments = @("-P$($profiles -join ',')") + $mavenArgs
    Write-Host "INFO: Running build with profile '$buildProfile' and profiles: [$($profiles -join ' ')]"
    Invoke-Maven -Runner $runner -Arguments $arguments
  } finally {
    Pop-Location
  }
} catch {
  [Console]::Error.WriteLine("ERROR: $($_.Exception.Message)")
  exit 1
}

exit 0
