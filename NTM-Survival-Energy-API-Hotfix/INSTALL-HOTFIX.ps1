$ErrorActionPreference = 'Stop'
$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$payloadRoot = Join-Path $PSScriptRoot 'payload'
$backupRoot = Join-Path $projectRoot ('.ntm-renewable-api-backup-' + (Get-Date -Format 'yyyyMMdd-HHmmss'))

$relativeFiles = @(
    'src/main/java/com/hbm/tileentity/machine/RenewablePowerModel.java',
    'src/main/java/com/hbm/tileentity/machine/TileEntityRenewableGenerator.java',
    'src/main/java/com/hbm/tileentity/machine/TileEntityMachineSolarPanel.java',
    'src/main/java/com/hbm/tileentity/machine/TileEntityMachineWindTurbine.java'
)

if (-not (Test-Path (Join-Path $projectRoot 'gradlew.bat'))) {
    throw "Jalankan installer dari root project yang berisi gradlew.bat. Folder saat ini: $projectRoot"
}

foreach ($relative in $relativeFiles) {
    $source = Join-Path $payloadRoot $relative
    $target = Join-Path $projectRoot $relative
    if (-not (Test-Path $source)) { throw "File patch tidak ditemukan: $source" }
    if (-not (Test-Path $target)) { throw "Source project tidak ditemukan: $target" }
}

New-Item -ItemType Directory -Path $backupRoot -Force | Out-Null
foreach ($relative in $relativeFiles) {
    $source = Join-Path $payloadRoot $relative
    $target = Join-Path $projectRoot $relative
    $backup = Join-Path $backupRoot $relative
    New-Item -ItemType Directory -Path (Split-Path $backup) -Force | Out-Null
    Copy-Item -LiteralPath $target -Destination $backup -Force
    Copy-Item -LiteralPath $source -Destination $target -Force
    Write-Host "Updated: $relative" -ForegroundColor Green
}

Write-Host ''
Write-Host 'HOTFIX APPLIED.' -ForegroundColor Green
Write-Host "Backup: $backupRoot"
Write-Host 'Tidak ada file registry, resep, model, atau tekstur yang diubah.'
