$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$h2Candidates = @(
    (Join-Path $root ".m2\repository\com\h2database\h2\2.4.240\h2-2.4.240.jar"),
    (Join-Path $root ".m2-repo\com\h2database\h2\2.4.240\h2-2.4.240.jar"),
    (Join-Path $env:USERPROFILE ".m2\repository\com\h2database\h2\2.4.240\h2-2.4.240.jar")
)
$buildDir = Join-Path $root "tools\.build"
$sourceFile = Join-Path $root "tools\ExportDatabaseJson.java"
$outputFile = Join-Path $root "data\quanlyluong-export.json"
$jdbcUrl = "jdbc:h2:file:./data/quanlyluong;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"

$h2Jar = $h2Candidates | Where-Object { Test-Path $_ } | Select-Object -First 1
if (!$h2Jar) {
    throw "Không tìm thấy H2 jar ở các vị trí đã kiểm tra."
}

New-Item -ItemType Directory -Force -Path $buildDir | Out-Null

Push-Location $root
try {
    & javac -cp $h2Jar -d $buildDir $sourceFile
    & java -cp "$buildDir;$h2Jar" ExportDatabaseJson $jdbcUrl $outputFile
} finally {
    Pop-Location
}
