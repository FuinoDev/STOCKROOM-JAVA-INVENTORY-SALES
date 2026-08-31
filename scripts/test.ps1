param([string]$Tests, [string]$BuildDirectory)
$ErrorActionPreference = 'Stop'
$projectRoot = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
Set-Location -LiteralPath $projectRoot
& (Join-Path $PSScriptRoot 'setup-local.ps1')
$oldUrl = $env:TEST_DB_URL
$oldUser = $env:TEST_DB_USERNAME
$oldPassword = $env:TEST_DB_PASSWORD
try {
    $env:TEST_DB_URL = 'jdbc:postgresql://127.0.0.1:55432/inventory_sales_test'
    $env:TEST_DB_USERNAME = 'inventory_app'
    $env:TEST_DB_PASSWORD = [System.IO.File]::ReadAllText((Join-Path $projectRoot '.local\db-app.secret')).Trim()
    $mavenArguments = @("-Dmaven.repo.local=$projectRoot\.m2", "-B", "-ntp", "verify")
    if ($Tests) { $mavenArguments += "-Dtest=$Tests" }
    if ($BuildDirectory) { $mavenArguments += "-Dstockroom.build.directory=$BuildDirectory" }
    & mvn @mavenArguments
    if ($LASTEXITCODE -ne 0) { throw 'Tests failed.' }
} finally {
    $env:TEST_DB_URL = $oldUrl
    $env:TEST_DB_USERNAME = $oldUser
    $env:TEST_DB_PASSWORD = $oldPassword
}
