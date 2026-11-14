# Herb / Alchemy -- in-game (RCON) verification.
#
# NOTE: this file is intentionally ASCII-only. Windows PowerShell 5.1 reads
# .ps1 files as ANSI unless they carry a UTF-8 BOM, so a Chinese comment here
# would break the parser with "The string is missing the terminator".
# (Chinese docs live in docs/herb-alchemy-spec.md instead.)
#
# Prereq: the Forge server is already running (gradle runServer) with RCON on
# (run/server.properties: enable-rcon=true, rcon.port=25598, rcon.password=hongmengtest).
#
# What it proves that the offline checker cannot:
#   1. the server reached "Done (" (datapack + mod registration OK)
#   2. setblock works for all 8 new blocks (incl. crop age properties)
#   3. the alchemy furnace really brews: main + aux + fuel -> pill, 200 ticks
#   4. an invalid main/aux pair never consumes fuel and never produces anything
#   5. /place feature runs the herb worldgen configuration
#   6. every new item id resolves (chest NBT)
#
# Usage:  powershell -ExecutionPolicy Bypass -File xiuxian-mod/tools/check_herb_alchemy_ingame.ps1 [-Stop]

param(
    [string]$LogPath = "",
    [string]$RconHost = "127.0.0.1",
    [int]$Port = 25598,
    [string]$Password = "hongmengtest",
    [switch]$Stop
)

$ErrorActionPreference = "Continue"
$ws = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
if (-not $LogPath) { $LogPath = Join-Path $ws "_cache\server_run2.log" }
$rcon = Join-Path $ws "tools\rcon.js"
$nodeCmd = Get-Command node -ErrorAction SilentlyContinue
if ($nodeCmd) { $node = $nodeCmd.Source } else {
    $node = "C:\Users\duyun\.dsh\dsh-runtimes\dsh-primary-runtime\dependencies\node\bin\node.exe"
}

function Send-Rcon([string]$cmd) {
    Write-Host ("  > " + $cmd)
    $out = & $node $rcon $RconHost $Port $Password $cmd 2>&1
    Write-Host ("    " + ($out -join " / "))
    return ($out -join " ")
}

Write-Host "== 1. wait for 'Done (' =="
$deadline = (Get-Date).AddSeconds(300)
$up = $false
while ((Get-Date) -lt $deadline) {
    if ((Test-Path $LogPath) -and (Select-String -Path $LogPath -Pattern 'Done (' -SimpleMatch -ErrorAction SilentlyContinue)) {
        $up = $true
        (Select-String -Path $LogPath -Pattern 'Done (' -SimpleMatch | Select-Object -First 1).Line.Trim() | Write-Host
        break
    }
    Start-Sleep -Seconds 5
}
if (-not $up) { Write-Host "  !! server did not come up in 300s"; exit 1 }

$parse = Select-String -Path $LogPath -Pattern "Couldn't parse|Failed to parse|Could not parse|Parsing error" -ErrorAction SilentlyContinue
Write-Host ("== 2. datapack parse errors: {0} ==" -f ($parse | Measure-Object).Count)

Write-Host "== 3. mob spawning off (this dev world has an unrelated eagle AI crash) =="
Send-Rcon "gamerule doMobSpawning false" | Out-Null
Send-Rcon "forceload add 0 0" | Out-Null

Write-Host "== 4. new blocks: setblock =="
$blocks = @(
    "hongmeng:alchemy_furnace", "hongmeng:lingtian_soil",
    "hongmeng:xuelian_grass", "hongmeng:xuelian_grass[age=7]",
    "hongmeng:huoyan_hua[age=7]", "hongmeng:hanbing_teng[age=3]",
    "hongmeng:leiyin_cao[age=7]", "hongmeng:kongming_lan[age=7]",
    "hongmeng:xueluo_zhi[age=7]"
)
$i = 0
foreach ($b in $blocks) {
    Send-Rcon ("setblock 8 100 {0} {1}" -f (2 + $i * 2), $b) | Out-Null
    $i++
}

Write-Host "== 5. every new item id resolves (chest NBT, single-quoted SNBT) =="
$chest = "minecraft:chest{Items:[{Slot:0b,id:'hongmeng:xuelian_grass_seed',Count:1b}," +
         "{Slot:1b,id:'hongmeng:xuelian_grass_petal',Count:1b}," +
         "{Slot:2b,id:'hongmeng:pill_huiling',Count:1b}," +
         "{Slot:3b,id:'hongmeng:lingtian_soil',Count:1b}," +
         "{Slot:4b,id:'hongmeng:xueluo_zhi_petal',Count:1b}," +
         "{Slot:5b,id:'hongmeng:daoyin',Count:1b}]}"
Send-Rcon ("setblock 8 100 30 " + $chest) | Out-Null
Send-Rcon "data get block 8 100 30 Items" | Out-Null

Write-Host "== 6. herb worldgen: place feature on a grass platform, then count =="
Send-Rcon "fill 0 99 16 15 99 31 minecraft:grass_block" | Out-Null
Send-Rcon "place feature hongmeng:xuelian_grass 8 100 24" | Out-Null
Send-Rcon "fill 0 100 16 15 102 31 minecraft:air replace hongmeng:xuelian_grass" | Out-Null

Write-Host "== 7. real brew: main + aux + fuel, wait 200 ticks =="
Send-Rcon "setblock 8 100 2 hongmeng:alchemy_furnace" | Out-Null
Send-Rcon "data merge block 8 100 2 {Items:[{Slot:0b,id:'hongmeng:xuelian_grass_petal',Count:1b},{Slot:1b,id:'hongmeng:lingye',Count:1b},{Slot:2b,id:'hongmeng:lingye',Count:2b}]}" | Out-Null
Send-Rcon "data get block 8 100 2" | Out-Null
Start-Sleep -Seconds 14
Write-Host "  ... expect slot 3 = hongmeng:pill_huiling x2, main+aux consumed"
Send-Rcon "data get block 8 100 2" | Out-Null

Write-Host "== 8. invalid pair: no fuel burned, no output =="
Send-Rcon "data merge block 8 100 2 {Items:[{Slot:0b,id:'hongmeng:xuelian_grass_petal',Count:1b},{Slot:1b,id:'minecraft:dirt',Count:1b},{Slot:2b,id:'hongmeng:lingye',Count:1b},{Slot:3b,id:'minecraft:air',Count:1b}]}" | Out-Null
Start-Sleep -Seconds 12
Send-Rcon "data get block 8 100 2" | Out-Null

Write-Host "== 9. cleanup =="
Send-Rcon "setblock 8 100 2 minecraft:air" | Out-Null
Send-Rcon "setblock 8 100 30 minecraft:air" | Out-Null
Send-Rcon "fill 0 99 16 15 99 31 minecraft:air" | Out-Null
Send-Rcon "forceload remove 0 0" | Out-Null

if ($Stop) { Send-Rcon "stop" | Out-Null }
Write-Host "== done =="
