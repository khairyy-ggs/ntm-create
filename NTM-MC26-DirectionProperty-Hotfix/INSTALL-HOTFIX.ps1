$ErrorActionPreference = 'Stop'
$projectRoot = (Get-Location).Path
$patchRoot = $PSScriptRoot
if (!(Test-Path (Join-Path $projectRoot 'gradlew.bat'))) {
    throw 'Jalankan dari root project yang berisi gradlew.bat.'
}
$files = @(
    'src\main\java\com\hbm\blocks\machine\MachineSolarPanel.java',
    'src\main\java\com\hbm\blocks\machine\MachineWindTurbine.java'
)
$backupRoot = Join-Path $projectRoot '.ntm-directionproperty-backup'
foreach ($rel in $files) {
    $source = Join-Path $patchRoot $rel
    $target = Join-Path $projectRoot $rel
    if (!(Test-Path $source)) { throw "File hotfix tidak ditemukan: $source" }
    if (Test-Path $target) {
        $backup = Join-Path $backupRoot $rel
        New-Item -ItemType Directory -Force -Path (Split-Path $backup) | Out-Null
        if (!(Test-Path $backup)) { Copy-Item $target $backup -Force }
    }
    New-Item -ItemType Directory -Force -Path (Split-Path $target) | Out-Null
    Copy-Item $source $target -Force
}
Write-Host 'Hotfix DirectionProperty selesai dipasang.' -ForegroundColor Green
Write-Host "Backup file lama: $backupRoot" -ForegroundColor Yellow
