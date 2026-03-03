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

$runner = 'mvnw.cmd'
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
  Check-ValidValue -ParameterName 'runner' -Value $runner -ValidValues $validRunners

  $repoRoot = (git rev-parse --show-toplevel 2>$null)
  if (-not $repoRoot) {
    throw 'Could not determine git repository root.'
  }
  $repoRoot = $repoRoot.Trim()

  Push-Location $repoRoot
  try {
    $runner = Resolve-Runner -SelectedRunner $runner
    $mavenArgs = @(
      'package',
      'javadoc:javadoc',
      'javadoc:aggregate',
      '-Pdistro,distro-wildfly,distro-webjar,javadocs',
      '-pl',
      '!distro/wildfly/modules,!distro/wildfly/assembly,!distro/wildfly/webapp,!distro/wildfly/distro,!engine-rest/engine-rest-openapi,!engine-rest/docs',
      '-DskipTests=true',
      '-Dskip.frontend.build=true'
    )
    Invoke-Maven -Runner $runner -Arguments $mavenArgs

    $targetRoot = Join-Path (Get-Location).Path 'target'
    $tempDir = Join-Path $targetRoot 'apidocs'
    $targetDir = Join-Path $targetRoot 'javadoc'
    $targetZipFile = Join-Path $targetRoot 'javadoc.zip'

    $apiDocBaseDir = Get-ChildItem -Path $targetRoot -Recurse -Directory -Filter 'apidocs' |
      Sort-Object FullName |
      Select-Object -First 1

    if (-not $apiDocBaseDir) {
      throw 'Could not find generated apidocs directory under target/.'
    }

    if (Test-Path -LiteralPath $tempDir) {
      Remove-Item -LiteralPath $tempDir -Recurse -Force
    }
    if ($apiDocBaseDir.FullName -ne $tempDir) {
      Move-Item -LiteralPath $apiDocBaseDir.FullName -Destination $targetRoot
    }

    if (Test-Path -LiteralPath $targetDir) {
      Remove-Item -LiteralPath $targetDir -Recurse -Force
    }
    Move-Item -LiteralPath $tempDir -Destination $targetDir

    if (Test-Path -LiteralPath $targetZipFile) {
      Remove-Item -LiteralPath $targetZipFile -Force
    }
    Compress-Archive -Path (Join-Path $targetDir '*') -DestinationPath $targetZipFile -Force
    Write-Host "INFO: Javadocs generated at: $targetDir"
    Write-Host "INFO: Javadocs zipped to: $targetZipFile"
  } finally {
    Pop-Location
  }
} catch {
  [Console]::Error.WriteLine("ERROR: $($_.Exception.Message)")
  exit 1
}

exit 0
