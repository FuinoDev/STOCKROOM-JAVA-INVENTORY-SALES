param([string]$Destination)
$ErrorActionPreference = 'Stop'
$projectRoot = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$configPath = Join-Path $projectRoot 'config\local.properties'
$settings = @{}
if (Test-Path -LiteralPath $configPath) {
    foreach ($line in [System.IO.File]::ReadAllLines($configPath)) {
        if ($line -match '^\s*([^#!][^=]*)=(.*)$') { $settings[$matches[1].Trim()] = $matches[2].Trim() }
    }
}
$dbUrl = if ($env:DB_URL) { $env:DB_URL } else { $settings['db.url'] }
$dbUser = if ($env:DB_USERNAME) { $env:DB_USERNAME } else { $settings['db.username'] }
$dbSecret = if ($env:DB_PASSWORD) { $env:DB_PASSWORD } else { $settings['db.password'] }
if ($dbUrl -notmatch '^jdbc:postgresql://(?<server>[A-Za-z0-9.-]+):(?<port>\d+)/(?<database>[A-Za-z0-9_]+)$') {
    throw 'Backup expects a simple PostgreSQL JDBC URL with an explicit host, port and database. For custom JDBC parameters use pg_dump directly.'
}
$dbServer = $matches['server']
$dbPort = $matches['port']
$dbName = $matches['database']
$pgCommand = Get-Command psql.exe -ErrorAction SilentlyContinue
if (!$pgCommand) { throw 'Add PostgreSQL bin to PATH before running the backup.' }
if (!$Destination) { $Destination = Join-Path $projectRoot ('.local\backups\stockroom-' + (Get-Date -Format 'yyyyMMdd-HHmmss') + '.dump') }
$backupPath = [System.IO.Path]::GetFullPath($Destination)
if (Test-Path -LiteralPath $backupPath) { throw 'The backup destination already exists. Choose a new filename.' }
[System.IO.Directory]::CreateDirectory((Split-Path $backupPath)) | Out-Null
$previousPassword = $env:PGPASSWORD
try {
    $env:PGPASSWORD = $dbSecret
    & (Join-Path (Split-Path $pgCommand.Source) 'pg_dump.exe') -h $dbServer -p $dbPort -U $dbUser -d $dbName -w --format=custom --no-owner --no-privileges --file=$backupPath
    if ($LASTEXITCODE -ne 0) { throw 'Backup failed. Do not rely on the output file.' }
} finally { $env:PGPASSWORD = $previousPassword }
Write-Output "Backup saved: $backupPath"
