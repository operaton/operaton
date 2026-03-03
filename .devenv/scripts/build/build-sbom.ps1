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

function Invoke-ExternalCommand {
  param(
    [Parameter(Mandatory = $true)]
    [string]$Command,
    [Parameter(Mandatory = $true)]
    [string[]]$Arguments
  )

  Write-Host "$Command $($Arguments -join ' ')"
  & $Command @Arguments

  if ($LASTEXITCODE -ne 0) {
    throw "Command '$Command' failed with exit code $LASTEXITCODE"
  }
}

function Resolve-CycloneDx {
  if (Get-Command cyclonedx -ErrorAction SilentlyContinue) {
    Write-Host 'CycloneDX CLI already available.'
    return 'cyclonedx'
  }

  if ($env:CI) {
    if (Test-Path -LiteralPath '/home/linuxbrew/.linuxbrew/bin/brew') {
      Write-Host 'Installing CycloneDX via Homebrew for CI environment...'
      Invoke-ExternalCommand -Command '/home/linuxbrew/.linuxbrew/bin/brew' -Arguments @(
        'install',
        'cyclonedx/cyclonedx/cyclonedx-cli'
      )
    } elseif (Get-Command brew -ErrorAction SilentlyContinue) {
      Write-Host 'Installing CycloneDX via Homebrew for CI environment...'
      Invoke-ExternalCommand -Command 'brew' -Arguments @('install', 'cyclonedx/cyclonedx/cyclonedx-cli')
    } else {
      throw 'Homebrew not found in CI environment. Install CycloneDX CLI manually.'
    }
  } elseif (Get-Command brew -ErrorAction SilentlyContinue) {
    Write-Host 'CycloneDX CLI not found; installing via Homebrew...'
    Invoke-ExternalCommand -Command 'brew' -Arguments @('install', 'cyclonedx/cyclonedx/cyclonedx-cli')
  } else {
    throw 'Homebrew not found. Install Homebrew or install CycloneDX CLI manually.'
  }

  if (Get-Command cyclonedx -ErrorAction SilentlyContinue) {
    Write-Host 'CycloneDX CLI installation verified.'
    return 'cyclonedx'
  }
  if (Test-Path -LiteralPath '/home/linuxbrew/.linuxbrew/bin/cyclonedx') {
    Write-Host 'CycloneDX CLI installation verified.'
    return '/home/linuxbrew/.linuxbrew/bin/cyclonedx'
  }

  throw 'CycloneDX CLI installation failed.'
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
    $cycloneDx = Resolve-CycloneDx
    $distros = @('run', 'tomcat', 'wildfly')

    Write-Host 'Generating SBOM files for Operaton modules...'
    foreach ($distro in $distros) {
      Write-Host "INFO: Generating SBOM for distro: $distro"
      $mavenArgs = @(
        'org.cyclonedx:cyclonedx-maven-plugin:makeAggregateBom',
        "-Pdistro-serverless,distro-$distro",
        '-Ddeploy.skip=false',
        '-DoutputDirectory=target/sbom',
        "-DoutputName=operaton-modules-$distro",
        '-DoutputFormat=json',
        '-DprojectType=application',
        '-DskipAttach=true',
        '-DskipNotDeployed=true'
      )
      Invoke-Maven -Runner $runner -Arguments $mavenArgs

      $sourceBom = Join-Path 'target/sbom' "operaton-modules-$distro.json"
      if (-not (Test-Path -LiteralPath $sourceBom)) {
        throw "SBOM file '$sourceBom' not found. Maven plugin may have failed."
      }
      $targetBom = Join-Path 'target/sbom' "operaton-modules-$distro.cyclonedx-json.sbom"
      Move-Item -LiteralPath $sourceBom -Destination $targetBom -Force
    }

    Write-Host 'Generating CycloneDX SBOM for Node.js frontend module...'
    New-Item -ItemType Directory -Path 'target/sbom' -Force | Out-Null
    Invoke-ExternalCommand -Command 'docker' -Arguments @(
      'run',
      '--rm',
      '-v',
      "${repoRoot}:/repo",
      'aquasec/trivy:latest',
      'fs',
      '--scanners',
      'vuln',
      '--format',
      'cyclonedx',
      '--output',
      '/repo/target/sbom/operaton-webapps.cyclonedx-json.sbom',
      '/repo/webapps/frontend'
    )

    Write-Host 'Merging CycloneDX SBOMs for each distribution...'
    foreach ($distro in $distros) {
      Invoke-ExternalCommand -Command $cycloneDx -Arguments @(
        'merge',
        '--input-files',
        "target/sbom/operaton-modules-$distro.cyclonedx-json.sbom",
        'target/sbom/operaton-webapps.cyclonedx-json.sbom',
        '--output-file',
        "distro/$distro/assembly/resources/sbom.cdx.json",
        '--input-format',
        'json'
      )
    }

    Write-Host 'INFO: SBOM files generated in target/sbom/'
  } finally {
    Pop-Location
  }
} catch {
  [Console]::Error.WriteLine("ERROR: $($_.Exception.Message)")
  exit 1
}

exit 0
