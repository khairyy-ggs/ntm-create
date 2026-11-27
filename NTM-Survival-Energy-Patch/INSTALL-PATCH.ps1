$ErrorActionPreference = 'Stop'

$projectRoot = (Get-Location).Path
$patchRoot = $PSScriptRoot
$payloadRoot = Join-Path $patchRoot 'payload'

if (!(Test-Path (Join-Path $projectRoot 'gradlew.bat'))) {
    throw 'Jalankan script dari root project yang berisi gradlew.bat.'
}
if (!(Test-Path (Join-Path $projectRoot 'src\main\java'))) {
    throw 'Folder src\main\java tidak ditemukan di project ini.'
}

$backupRoot = Join-Path $projectRoot '.ntm-survival-expansion-backup'
New-Item -ItemType Directory -Path $backupRoot -Force | Out-Null

function Backup-ProjectFile([string]$relativePath) {
    $source = Join-Path $projectRoot $relativePath
    if (Test-Path $source) {
        $backup = Join-Path $backupRoot $relativePath
        New-Item -ItemType Directory -Path (Split-Path $backup) -Force | Out-Null
        if (!(Test-Path $backup)) { Copy-Item $source $backup -Force }
    }
}

function Copy-PayloadTree([string]$relativePath) {
    $source = Join-Path $payloadRoot $relativePath
    if (Test-Path $source) {
        Get-ChildItem $source -Recurse -File | ForEach-Object {
            $rel = $_.FullName.Substring($payloadRoot.Length + 1)
            $target = Join-Path $projectRoot $rel
            New-Item -ItemType Directory -Path (Split-Path $target) -Force | Out-Null
            if (Test-Path $target) { Backup-ProjectFile $rel }
            Copy-Item $_.FullName $target -Force
        }
    }
}

function Add-TextBefore([string]$filePath, [string]$anchor, [string]$insert, [string]$marker) {
    $full = Join-Path $projectRoot $filePath
    $text = Get-Content $full -Raw
    if ($text.Contains($marker)) { return }
    if (!$text.Contains($anchor)) { throw "Anchor tidak ditemukan di $filePath : $anchor" }
    Backup-ProjectFile $filePath
    $text = $text.Replace($anchor, $insert + "`r`n" + $anchor)
    [System.IO.File]::WriteAllText($full, $text, [System.Text.UTF8Encoding]::new($false))
}

function Add-TextAfter([string]$filePath, [string]$anchor, [string]$insert, [string]$marker) {
    $full = Join-Path $projectRoot $filePath
    $text = Get-Content $full -Raw
    if ($text.Contains($marker)) { return }
    if (!$text.Contains($anchor)) { throw "Anchor tidak ditemukan di $filePath : $anchor" }
    Backup-ProjectFile $filePath
    $text = $text.Replace($anchor, $anchor + "`r`n" + $insert)
    [System.IO.File]::WriteAllText($full, $text, [System.Text.UTF8Encoding]::new($false))
}

Write-Host '[1/7] Menyalin source Java dan resource ...' -ForegroundColor Cyan
Copy-PayloadTree 'src\main\java'
Copy-PayloadTree 'src\main\resources'

$machineBlocksPath = 'src\main\java\com\hbm\blocks\machine\ModMachineBlocks.java'
$blockEntitiesPath = 'src\main\java\com\hbm\tileentity\ModBlockEntities.java'
$turbineModelPath = 'src\main\java\com\hbm\tileentity\machine\ShaftTurbineModel.java'
$authoredPath = 'gradle\dejson-authored.txt'

foreach ($required in @($machineBlocksPath, $blockEntitiesPath, $turbineModelPath, $authoredPath)) {
    if (!(Test-Path (Join-Path $projectRoot $required))) { throw "File project wajib tidak ditemukan: $required" }
}

Write-Host '[2/7] Mendaftarkan Solar Panel dan Wind Turbine ...' -ForegroundColor Cyan
$blockInsert = @'
    // BEGIN NTM SURVIVAL ENERGY EXPANSION - renewable source blocks
    public static final RegistryObject<MachineSolarPanel> machine_solar_panel = REG.<MachineSolarPanel>block(
            "machine_solar_panel", MachineSolarPanel::new,
            p -> p.requiresCorrectToolForDrops().strength(3.5F, 5.0F).sound(SoundType.METAL).noOcclusion())
            .model(d -> d.models(Models.parented("minecraft:block/air", Slots.of("particle", "hbm:blocks/machine_solar_panel_side")))
                    .states(Variants.uniform("facing", "north", "east", "south", "west")).blockItemDirect())
            .loot(Loot.self()).tag(BlockTag.PICKAXE);

    public static final RegistryObject<MachineWindTurbine> machine_wind_turbine = REG.<MachineWindTurbine>block(
            "machine_wind_turbine", MachineWindTurbine::new,
            p -> p.requiresCorrectToolForDrops().strength(4.0F, 6.0F).sound(SoundType.METAL).noOcclusion())
            .model(d -> d.models(Models.parented("minecraft:block/air", Slots.of("particle", "hbm:blocks/machine_wind_turbine_side")))
                    .states(Variants.uniform("facing", "north", "east", "south", "west")).blockItemDirect())
            .loot(Loot.self()).tag(BlockTag.PICKAXE);
    // END NTM SURVIVAL ENERGY EXPANSION - renewable source blocks
'@
Add-TextBefore $machineBlocksPath '    public static final RegistryObject<MachineVerticalHeatExchanger> machine_vertical_heat_exchanger =' $blockInsert 'BEGIN NTM SURVIVAL ENERGY EXPANSION - renewable source blocks'

$tabInsert = @'
        // BEGIN NTM SURVIVAL ENERGY EXPANSION - creative tab items
        tabItem(machine_solar_panel);
        tabItem(machine_wind_turbine);
        // END NTM SURVIVAL ENERGY EXPANSION - creative tab items
'@
Add-TextAfter $machineBlocksPath '        tabItem(machine_shaft_turbine);' $tabInsert 'BEGIN NTM SURVIVAL ENERGY EXPANSION - creative tab items'

Write-Host '[3/7] Mendaftarkan Block Entity renewable sources ...' -ForegroundColor Cyan
$beImports = @'
import com.hbm.tileentity.machine.TileEntityMachineSolarPanel;
import com.hbm.tileentity.machine.TileEntityMachineWindTurbine;
'@
Add-TextAfter $blockEntitiesPath 'import com.hbm.tileentity.machine.TileEntityMachineShaftTurbine;' $beImports 'TileEntityMachineSolarPanel;'

$beInsert = @'
    // BEGIN NTM SURVIVAL ENERGY EXPANSION - renewable source block entities
    public static final RegistryObject<BlockEntityType<TileEntityMachineSolarPanel>> MACHINE_SOLAR_PANEL =
            REG.blockEntity("machine_solar_panel", () -> BlockEntities.serverTicking(
                    TileEntityMachineSolarPanel::new, TileEntityMachineSolarPanel::serverTick,
                    ModMachineBlocks.machine_solar_panel.get()));

    public static final RegistryObject<BlockEntityType<TileEntityMachineWindTurbine>> MACHINE_WIND_TURBINE =
            REG.blockEntity("machine_wind_turbine", () -> BlockEntities.serverTicking(
                    TileEntityMachineWindTurbine::new, TileEntityMachineWindTurbine::serverTick,
                    ModMachineBlocks.machine_wind_turbine.get()));
    // END NTM SURVIVAL ENERGY EXPANSION - renewable source block entities
'@
Add-TextBefore $blockEntitiesPath '    public static final RegistryObject<BlockEntityType<TileEntityMachineVerticalHeatExchanger>> MACHINE_VERTICAL_HEAT_EXCHANGER =' $beInsert 'BEGIN NTM SURVIVAL ENERGY EXPANSION - renewable source block entities'

Write-Host '[4/7] Menyeimbangkan Compact Shaft Turbine menjadi 200k SU ...' -ForegroundColor Cyan
$turbineFull = Join-Path $projectRoot $turbineModelPath
$turbineText = Get-Content $turbineFull -Raw

# Be tolerant of previous balance edits: match declarations by constant name,
# not by the exact old numeric literal or access modifier.
$balance = @(
    @{ Name = 'MAX_SU';             Value = '200_000L' },
    @{ Name = 'MAX_POWER';          Value = '5_000_000L' },
    @{ Name = 'FULL_OUTPUT_POWER';  Value = '1_000_000L' },
    @{ Name = 'HE_PER_SU';          Value = '0.005D' },
    @{ Name = 'START_POWER';        Value = '25_000L' },
    @{ Name = 'SU_STEP';            Value = '2_000' }
)
$updatedTurbineText = $turbineText
foreach ($entry in $balance) {
    $pattern = '(?m)^([ \t]*(?:(?:public|protected|private)[ \t]+)?static[ \t]+final[ \t]+(?:long|int|double)[ \t]+' + [regex]::Escape($entry.Name) + '[ \t]*=[ \t]*)[^;\r\n]*([ \t]*;[ \t]*(?://[^\r\n]*)?\r?)$'
    $foundMatches = [regex]::Matches($updatedTurbineText, $pattern)
    if ($foundMatches.Count -ne 1) {
        throw "Deklarasi $($entry.Name) harus ditemukan tepat satu kali di ShaftTurbineModel.java, ditemukan $($foundMatches.Count). Tidak ada perubahan balance yang disimpan."
    }
    $foundMatch = $foundMatches[0]
    $newLine = $foundMatch.Groups[1].Value + $entry.Value + $foundMatch.Groups[2].Value
    $updatedTurbineText = $updatedTurbineText.Remove($foundMatch.Index, $foundMatch.Length).Insert($foundMatch.Index, $newLine)
}

# Update the explanatory comment when present; it is not required for operation.
$updatedTurbineText = $updatedTurbineText -replace 'so a full turbine draws 1M HE/t\.', 'so the compact turbine draws about 1,000 HE/t at full output.'
if ($updatedTurbineText -ne $turbineText) {
    Backup-ProjectFile $turbineModelPath
    [System.IO.File]::WriteAllText($turbineFull, $updatedTurbineText, [System.Text.UTF8Encoding]::new($false))
}
Write-Host '[5/7] Mendaftarkan file JSON buatan tangan ke Dejson ...' -ForegroundColor Cyan
$authoredFull = Join-Path $projectRoot $authoredPath
$authored = Get-Content $authoredFull -Raw
$paths = @(
    'assets/hbm/blockstates/machine_solar_panel.json',
    'assets/hbm/blockstates/machine_wind_turbine.json',
    'assets/hbm/models/block/machine_solar_panel.json',
    'assets/hbm/models/block/machine_wind_turbine.json',
    'assets/hbm/models/item/machine_solar_panel.json',
    'assets/hbm/models/item/machine_wind_turbine.json',
    'assets/hbm/items/machine_solar_panel.json',
    'assets/hbm/items/machine_wind_turbine.json',
    'data/hbm/recipe/machine_solar_panel.json',
    'data/hbm/recipe/machine_wind_turbine.json',
    'data/hbm/loot_table/blocks/machine_solar_panel.json',
    'data/hbm/loot_table/blocks/machine_wind_turbine.json'
)
$added = @()
foreach ($path in $paths) {
    if (!$authored.Contains($path)) { $authored = $authored.TrimEnd() + "`r`n" + $path; $added += $path }
}
if ($added.Count -gt 0) {
    Backup-ProjectFile $authoredPath
    [System.IO.File]::WriteAllText($authoredFull, $authored.TrimEnd() + "`r`n", [System.Text.UTF8Encoding]::new($false))
}

Write-Host '[6/7] Menambahkan nama blok ke en_us.json ...' -ForegroundColor Cyan
$langPath = 'src\main\resources\assets\hbm\lang\en_us.json'
$langFull = Join-Path $projectRoot $langPath
if (!(Test-Path $langFull)) { throw 'assets/hbm/lang/en_us.json tidak ditemukan.' }
$langText = Get-Content $langFull -Raw
$langEntries = [ordered]@{
    'block.hbm.machine_solar_panel' = 'Photovoltaic Solar Panel'
    'block.hbm.machine_wind_turbine' = 'Industrial Wind Turbine'
}
$missingEntries = @()
foreach ($key in $langEntries.Keys) {
    if ($langText -notmatch ('"' + [regex]::Escape($key) + '"\s*:')) {
        $value = $langEntries[$key] | ConvertTo-Json -Compress
        $missingEntries += ('  "' + $key + '": ' + $value)
    }
}
if ($missingEntries.Count -gt 0) {
    Backup-ProjectFile $langPath
    $close = $langText.LastIndexOf('}')
    if ($close -lt 0) { throw 'Format en_us.json tidak valid: penutup object tidak ditemukan.' }
    $before = $langText.Substring(0, $close).TrimEnd()
    if ($before.EndsWith('{')) { $join = "`r`n" } else { $join = ",`r`n" }
    $newText = $before + $join + ($missingEntries -join ",`r`n") + "`r`n}`r`n"
    $null = $newText | ConvertFrom-Json
    [System.IO.File]::WriteAllText($langFull, $newText, [System.Text.UTF8Encoding]::new($false))
}

Write-Host '[7/7] Selesai. Backup sumber lama disimpan di .ntm-survival-expansion-backup' -ForegroundColor Green
Write-Host 'File baru: Solar Panel + Wind Turbine; model, tekstur, recipe, loot table; tier profile.' -ForegroundColor Green
Write-Host 'Jalankan build setelah memastikan output patch ini tidak memiliki konflik:' -ForegroundColor Yellow
Write-Host '.\gradlew.bat --configure-on-demand :1.21.1-neoforge:build' -ForegroundColor Yellow
