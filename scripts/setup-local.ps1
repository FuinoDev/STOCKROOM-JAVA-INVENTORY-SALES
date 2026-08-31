param([int]$Port = 55432)
$ErrorActionPreference = 'Stop'
$projectRoot = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$localRoot = Join-Path $projectRoot '.local'
$dataPath = Join-Path $localRoot 'postgres'
$configPath = Join-Path $projectRoot 'config\local.properties'
$pgCommand = Get-Command psql.exe -ErrorAction SilentlyContinue
if ($pgCommand) { $pgBin = Split-Path $pgCommand.Source } else {
    $pgInstall = Get-ChildItem -LiteralPath 'C:\Program Files\PostgreSQL' -Directory -ErrorAction SilentlyContinue | Sort-Object Name -Descending | Select-Object -First 1
    if (!$pgInstall) { throw 'Install PostgreSQL 14 or newer, then run this script again.' }
    $pgBin = Join-Path $pgInstall.FullName 'bin'
}
[System.IO.Directory]::CreateDirectory($localRoot) | Out-Null
function New-LocalSecret {
    $secretBytes = New-Object byte[] 32
    $generator = [System.Security.Cryptography.RandomNumberGenerator]::Create()
    $generator.GetBytes($secretBytes)
    $generator.Dispose()
    return [Convert]::ToBase64String($secretBytes)
}
$adminSecretPath = Join-Path $localRoot 'db-admin.secret'
$appSecretPath = Join-Path $localRoot 'db-app.secret'
if (!(Test-Path -LiteralPath $adminSecretPath)) {
    if (Test-Path -LiteralPath (Join-Path $dataPath 'PG_VERSION')) { throw 'Database exists but its local administrator secret is missing. Restore .local/db-admin.secret from backup.' }
    [System.IO.File]::WriteAllText($adminSecretPath,(New-LocalSecret))
}
if (!(Test-Path -LiteralPath $appSecretPath)) { [System.IO.File]::WriteAllText($appSecretPath,(New-LocalSecret)) }
if (!(Test-Path -LiteralPath (Join-Path $dataPath 'PG_VERSION'))) {
    & (Join-Path $pgBin 'initdb.exe') -D $dataPath -U postgres --pwfile=$adminSecretPath --auth=scram-sha-256 --encoding=UTF8 --locale=C
    if ($LASTEXITCODE -ne 0) { throw 'Could not initialize the project database.' }
}
& (Join-Path $pgBin 'pg_ctl.exe') -D $dataPath status | Out-Null
if ($LASTEXITCODE -ne 0) {
    $serverOptions = "-h 127.0.0.1 -p $Port"
    & (Join-Path $pgBin 'pg_ctl.exe') -D $dataPath -l (Join-Path $localRoot 'postgres.log') -o $serverOptions -w start
    if ($LASTEXITCODE -ne 0) { throw 'Could not start the project database. Check .local/postgres.log and whether the port is in use.' }
}
$previousPassword = $env:PGPASSWORD
try {
    $env:PGPASSWORD = [System.IO.File]::ReadAllText($adminSecretPath).Trim()
    $psqlPath = Join-Path $pgBin 'psql.exe'
    $connectionArgs = @('-h','127.0.0.1','-p',"$Port",'-U','postgres','-d','postgres','-v','ON_ERROR_STOP=1','-w')
    $roleExists = & $psqlPath @connectionArgs -tAc "SELECT 1 FROM pg_roles WHERE rolname='inventory_app'"
    if ($LASTEXITCODE -ne 0) { throw 'Could not authenticate to the isolated database.' }
    if ("$roleExists".Trim() -ne '1') {
        $appPassword = [System.IO.File]::ReadAllText($appSecretPath).Trim()
        $roleSql = "CREATE ROLE inventory_app LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE PASSWORD '$appPassword';"
        # Send the generated SQL over standard input; passwords are never process arguments.
        $roleSql | & $psqlPath @connectionArgs
        if ($LASTEXITCODE -ne 0) { throw 'Could not create the application database account.' }
    }
    foreach ($dbName in @('inventory_sales','inventory_sales_test')) {
        $exists = & $psqlPath @connectionArgs -tAc "SELECT 1 FROM pg_database WHERE datname='$dbName'"
        if ($LASTEXITCODE -ne 0) { throw 'Could not check database status.' }
        if ("$exists".Trim() -ne '1') {
            & $psqlPath @connectionArgs -c "CREATE DATABASE $dbName OWNER inventory_app;"
            if ($LASTEXITCODE -ne 0) { throw "Could not create $dbName." }
        }
    }
} finally { $env:PGPASSWORD = $previousPassword }
if (!(Test-Path -LiteralPath $configPath)) {
    [System.IO.Directory]::CreateDirectory((Split-Path $configPath)) | Out-Null
    $appPassword = [System.IO.File]::ReadAllText($appSecretPath).Trim()
    $configText = "db.url=jdbc:postgresql://127.0.0.1:$Port/inventory_sales`ndb.username=inventory_app`ndb.password=$appPassword`nbusiness.name=Stockroom`nbusiness.timezone=Asia/Manila`n"
    [System.IO.File]::WriteAllText($configPath,$configText,[System.Text.UTF8Encoding]::new($false))
}
Write-Output 'Local PostgreSQL is ready. Application data and test data use separate databases.'
