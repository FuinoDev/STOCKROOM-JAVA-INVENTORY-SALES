param([switch]$Build)
$ErrorActionPreference = 'Stop'
$projectRoot = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
Set-Location -LiteralPath $projectRoot
if (Test-Path -LiteralPath (Join-Path $projectRoot '.local\postgres\PG_VERSION')) {
    & (Join-Path $PSScriptRoot 'setup-local.ps1')
} elseif (!(Test-Path -LiteralPath (Join-Path $projectRoot 'config\local.properties')) -and !$env:DB_URL) {
    & (Join-Path $PSScriptRoot 'setup-local.ps1')
}
$jar = Join-Path $projectRoot 'target\stockroom.jar'
$pendingJar = Join-Path $projectRoot 'target\stockroom.pending.jar'
if (Test-Path -LiteralPath $pendingJar) {
    $currentJar = Get-Item -LiteralPath $jar -ErrorAction SilentlyContinue
    $pendingFile = Get-Item -LiteralPath $pendingJar
    if (!$currentJar -or $pendingFile.LastWriteTimeUtc -gt $currentJar.LastWriteTimeUtc) {
        try {
            if ($currentJar) { [System.IO.File]::Replace($pendingJar,$jar,$null) }
            else { [System.IO.File]::Move($pendingJar,$jar) }
        } catch {
            throw 'Close all Stockroom windows, then double-click Start Stockroom again to apply the prepared update. Your saved data is unchanged.'
        }
    }
}

if ($Build -or !(Test-Path -LiteralPath $jar)) {
    & mvn "-Dmaven.repo.local=$projectRoot\.m2" -B -ntp package
    if ($LASTEXITCODE -ne 0) { throw 'The build failed. Read the message above.' }
}
$javaCommand = Get-Command javaw.exe -ErrorAction SilentlyContinue
if (!$javaCommand) { throw 'Install a JDK 17 or newer and add its bin directory to PATH.' }
Start-Process -FilePath $javaCommand.Source -ArgumentList @('--enable-native-access=ALL-UNNAMED','-jar',"`"$jar`"") -WorkingDirectory $projectRoot -WindowStyle Hidden
