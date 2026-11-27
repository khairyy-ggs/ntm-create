$ErrorActionPreference = 'Stop'
$projectRoot = $PSScriptRoot
$authoredFile = Join-Path $projectRoot 'gradle\dejson-authored.txt'
$recipePaths = @(
    'data/hbm/recipe/shaft_cable.json',
    'data/hbm/recipe/machine_shaft_turbine.json',
    'data/hbm/recipe/shaft_output.json',
    'data/hbm/recipe/machine_vertical_heat_exchanger.json'
)

if (Test-Path $authoredFile) {
    $existing = @(Get-Content $authoredFile)
    $missing = @($recipePaths | Where-Object { $_ -notin $existing })
    if ($missing.Count -gt 0) {
        Copy-Item $authoredFile "$authoredFile.bak" -Force
        Add-Content -Path $authoredFile -Value $missing
        Write-Host "Added $($missing.Count) recipe paths to gradle\dejson-authored.txt (backup: gradle\dejson-authored.txt.bak)"
    } else {
        Write-Host 'Recipe paths already exist in gradle\dejson-authored.txt'
    }
} else {
    Write-Host 'gradle\dejson-authored.txt not found; no authored-path list was changed.'
}

Write-Host ''
Write-Host 'Patch files are in place. Build from this project root with:'
Write-Host '.\gradlew.bat --configure-on-demand :1.21.1-neoforge:build'
