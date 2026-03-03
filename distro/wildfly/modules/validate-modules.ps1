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

# Directory where module.xml files are expected (relative to this Maven module).
$modulesDir = Join-Path $PSScriptRoot 'target/modules'
$validationFailed = $false

Write-Host "Starting Wildfly module.xml validation in $modulesDir..."

if (-not (Test-Path -LiteralPath $modulesDir -PathType Container)) {
  Write-Host "WARNING: Module directory '$modulesDir' not found. Skipping validation."
  exit 0
}

$moduleFiles = Get-ChildItem -Path $modulesDir -Filter 'module.xml' -Recurse -File
if (-not $moduleFiles) {
  Write-Host "No module.xml files found under '$modulesDir'. Skipping resource existence check."
  exit 0
}

foreach ($moduleFile in $moduleFiles) {
  Write-Host "  Processing $($moduleFile.FullName)"
  $moduleDir = $moduleFile.DirectoryName

  try {
    [xml]$moduleXml = Get-Content -LiteralPath $moduleFile.FullName -Raw
  } catch {
    Write-Host "    ERROR: Failed to parse XML file '$($moduleFile.FullName)': $($_.Exception.Message)"
    $validationFailed = $true
    continue
  }

  $resourceNodes = $moduleXml.SelectNodes("//*[local-name()='module']/*[local-name()='resources']/*[local-name()='resource-root']")
  if (-not $resourceNodes -or $resourceNodes.Count -eq 0) {
    Write-Host "    No <resource-root> paths found in $($moduleFile.FullName). Skipping resource existence check."
    continue
  }

  foreach ($resourceNode in $resourceNodes) {
    $relativePath = ''
    if ($resourceNode.Attributes -and $resourceNode.Attributes['path']) {
      $relativePath = $resourceNode.Attributes['path'].Value.Trim()
    }

    if ([string]::IsNullOrWhiteSpace($relativePath)) {
      continue
    }

    $absoluteResourcePath = Join-Path $moduleDir $relativePath
    if (-not (Test-Path -LiteralPath $absoluteResourcePath -PathType Leaf)) {
      Write-Host "    ERROR: Resource not found for $($moduleFile.FullName):"
      Write-Host "      Referenced path: '$relativePath'"
      Write-Host "      Expected absolute path: '$absoluteResourcePath'"
      $validationFailed = $true
    } else {
      Write-Host "    Resource found: '$relativePath' (at '$absoluteResourcePath')"
    }
  }
}

if ($validationFailed) {
  Write-Host '----------------------------------------------------------------------'
  Write-Host 'Wildfly module.xml validation FAILED: One or more referenced resources were not found.'
  Write-Host '----------------------------------------------------------------------'
  exit 1
}

Write-Host '----------------------------------------------------------------------'
Write-Host 'Wildfly module.xml validation PASSED: All referenced resources found.'
Write-Host '----------------------------------------------------------------------'
exit 0
