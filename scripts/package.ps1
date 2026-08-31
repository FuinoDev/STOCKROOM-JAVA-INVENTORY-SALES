param([switch]$SkipBuild)
$ErrorActionPreference = 'Stop'
$projectRoot = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
Set-Location -LiteralPath $projectRoot
if (!$SkipBuild) {
    & mvn "-Dmaven.repo.local=$projectRoot\.m2" -B -ntp package
    if ($LASTEXITCODE -ne 0) { throw 'Build failed; no release was created.' }
}
$jarPath = Join-Path $projectRoot 'target\stockroom.jar'
if (!(Test-Path -LiteralPath $jarPath)) { throw 'Build the application first.' }
$stagingRoot = Join-Path $projectRoot ('.local\release-' + [Guid]::NewGuid().ToString('N') + '\Stockroom')
$distRoot = Join-Path $projectRoot 'dist'
[System.IO.Directory]::CreateDirectory($stagingRoot) | Out-Null
[System.IO.Directory]::CreateDirectory((Join-Path $stagingRoot 'target')) | Out-Null
[System.IO.Directory]::CreateDirectory((Join-Path $stagingRoot 'config')) | Out-Null
[System.IO.Directory]::CreateDirectory($distRoot) | Out-Null
Copy-Item -LiteralPath $jarPath -Destination (Join-Path $stagingRoot 'target\stockroom.jar')
foreach ($file in @('README.md','Start Stockroom.cmd','THIRD-PARTY-NOTICES.md')) { Copy-Item -LiteralPath (Join-Path $projectRoot $file) -Destination $stagingRoot }
Copy-Item -LiteralPath (Join-Path $projectRoot 'config\application.example.properties') -Destination (Join-Path $stagingRoot 'config')
Copy-Item -LiteralPath (Join-Path $projectRoot 'scripts') -Destination $stagingRoot -Recurse
Copy-Item -LiteralPath (Join-Path $projectRoot 'docs') -Destination $stagingRoot -Recurse
$archivePath = Join-Path $distRoot 'Stockroom-1.0.0.zip'
Compress-Archive -LiteralPath $stagingRoot -DestinationPath $archivePath -Force
Get-FileHash -LiteralPath $archivePath -Algorithm SHA256 | Select-Object Hash,Path
Write-Output 'The release excludes passwords, database data, test fixtures, and dependency caches.'
