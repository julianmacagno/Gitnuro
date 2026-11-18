$ErrorActionPreference = "Stop"

$repo = if ($env:GITIA_REPO) { $env:GITIA_REPO } else { "julianmacagno/Gitnuro" }
$url = "https://github.com/$repo/releases/latest/download/GitIA-universal.jar"

$javaOutput = cmd /c "java -version 2>&1"
if ($LASTEXITCODE -ne 0 -or $javaOutput[0] -notmatch '"(\d+)' -or [int]$Matches[1] -lt 21) {
    throw "Java 21 or newer is required"
}

$jar = Join-Path $env:TEMP "GitIA.jar"
Invoke-WebRequest -Uri $url -OutFile $jar
& java -jar $jar --install
Remove-Item $jar
