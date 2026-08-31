$ErrorActionPreference = 'Stop'
$projectRoot = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$dataPath = Join-Path $projectRoot '.local\postgres'
if (!(Test-Path -LiteralPath (Join-Path $dataPath 'PG_VERSION'))) { Write-Output 'No project database to stop.'; exit 0 }
$pgCommand = Get-Command psql.exe -ErrorAction SilentlyContinue
if (!$pgCommand) { throw 'Add PostgreSQL bin to PATH before running this script.' }
& (Join-Path (Split-Path $pgCommand.Source) 'pg_ctl.exe') -D $dataPath -m fast -w stop
if ($LASTEXITCODE -ne 0) { throw 'Could not stop the project database.' }
