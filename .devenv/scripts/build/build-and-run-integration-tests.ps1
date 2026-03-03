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

$executeBuild = $true
$executeTest = $true
$testSuite = 'engine'
$database = 'h2'
$distro = 'tomcat'
$runner = 'mvnw.cmd'

$validTestSuites = @('engine', 'webapps', 'db-rolling-update')
$validDistros = @('operaton', 'tomcat', 'wildfly')
$validDatabases = @('h2', 'postgresql', 'postgresql-xa', 'mysql', 'mariadb', 'oracle', 'db2', 'sqlserver')
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

foreach ($arg in $args) {
  if ($arg -match '^--testsuite=(.+)$') {
    $testSuite = $Matches[1]
    continue
  }
  if ($arg -match '^--distro=(.+)$') {
    $distro = $Matches[1]
    continue
  }
  if ($arg -match '^--db=(.+)$') {
    $database = $Matches[1]
    continue
  }
  if ($arg -eq '--no-build') {
    $executeBuild = $false
    continue
  }
  if ($arg -eq '--no-test') {
    $executeTest = $false
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

try {
  Check-ValidValue -ParameterName 'testsuite' -Value $testSuite -ValidValues $validTestSuites
  Check-ValidValue -ParameterName 'distro' -Value $distro -ValidValues $validDistros
  Check-ValidValue -ParameterName 'db' -Value $database -ValidValues $validDatabases
  Check-ValidValue -ParameterName 'runner' -Value $runner -ValidValues $validRunners

  $repoRoot = (git rev-parse --show-toplevel 2>$null)
  if (-not $repoRoot) {
    throw 'Could not determine git repository root.'
  }
  $repoRoot = $repoRoot.Trim()

  Push-Location $repoRoot
  try {
    $runner = Resolve-Runner -SelectedRunner $runner

    if ($executeBuild) {
      $profiles = @('distro', 'distro-webjar', 'h2-in-memory')
      $buildArgs = @('-DskipTests')

      switch ($distro) {
        'operaton' {
          $profiles += @('distro-run', 'integration-test-operaton-run')
        }
        'tomcat' {
          $profiles += @('tomcat', 'distro-tomcat')
          if ($testSuite -eq 'engine') {
            $buildArgs += '-Dskip.frontend.build=true'
          }
        }
        'wildfly' {
          $profiles += @('wildfly', 'distro-wildfly')
          if ($testSuite -eq 'engine') {
            $buildArgs += '-Dskip.frontend.build=true'
          }
        }
      }

      Write-Host "INFO: Building $testSuite integration tests for distro $distro with $database database using profiles: [$($profiles -join ' ')]"
      $buildArgs += @("-P$($profiles -join ',')", 'clean', 'install')
      Invoke-Maven -Runner $runner -Arguments $buildArgs
    } else {
      Write-Host 'INFO: Skipping build'
    }

    if ($executeTest) {
      $profiles = @()
      $testArgs = @('clean', 'verify')

      switch ($testSuite) {
        'engine' {
          $profiles += 'engine-integration'
        }
        'webapps' {
          $profiles += 'webapps-integration'
        }
      }

      switch ($distro) {
        'operaton' {
          $profiles += 'integration-test-operaton-run'
          $testArgs = @('-f', 'distro/run/qa') + $testArgs
        }
        'tomcat' {
          $profiles += 'tomcat'
          $testArgs = @('-f', 'qa') + $testArgs
        }
        'wildfly' {
          $profiles += 'wildfly'
          $testArgs = @('-f', 'qa') + $testArgs
        }
      }

      $profiles += $database
      Write-Host "INFO: Running $testSuite integration tests for distro $distro with $database database using profiles: [$($profiles -join ' ')]"
      $testArgs = @("-P$($profiles -join ',')") + $testArgs
      Invoke-Maven -Runner $runner -Arguments $testArgs
      Write-Host 'INFO: Integration tests completed successfully'
    } else {
      Write-Host 'INFO: Skipping tests'
    }
  } finally {
    Pop-Location
  }
} catch {
  [Console]::Error.WriteLine("ERROR: $($_.Exception.Message)")
  exit 1
}

exit 0
