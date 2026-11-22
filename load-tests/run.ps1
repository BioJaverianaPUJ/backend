param(
    [ValidateSet("mobile", "admin", "observations", "observation-smoke", "combined", "combined-http")]
    [string]$Scenario = "combined"
)

$ErrorActionPreference = "Stop"
$root = $PSScriptRoot
$envFile = Join-Path $root ".env"

if (-not (Test-Path $envFile)) {
    throw "Copy load-tests/.env.example to load-tests/.env and configure test-only values first."
}

foreach ($line in Get-Content $envFile) {
    $trimmed = $line.Trim()
    if (-not $trimmed -or $trimmed.StartsWith("#")) {
        continue
    }

    $parts = $trimmed -split "=", 2
    if ($parts.Count -ne 2) {
        throw "Invalid environment line in load-tests/.env"
    }

    $name = $parts[0].Trim()
    $value = $parts[1].Trim()
    if (($value.StartsWith('"') -and $value.EndsWith('"')) -or
        ($value.StartsWith("'") -and $value.EndsWith("'"))) {
        $value = $value.Substring(1, $value.Length - 2)
    }
        if ($name -in @("MOBILE_TOKEN", "ADMIN_TOKEN")) {
        $existingToken = [Environment]::GetEnvironmentVariable($name, "Process")

        if (-not [string]::IsNullOrWhiteSpace($existingToken)) {
            continue
        }
    }

    if (-not [string]::IsNullOrWhiteSpace($value)) {
        [Environment]::SetEnvironmentVariable($name, $value, "Process")
    }
}

$k6 = Get-Command k6 -ErrorAction SilentlyContinue
if (-not $k6) {
    throw "k6 is not installed or is not available on PATH."
}

if (-not $env:API_BASE_URL) {
    throw "API_BASE_URL is required."
}
$apiUri = [Uri]$env:API_BASE_URL
if (-not $apiUri.IsLoopback) {
    throw "Refusing to run against a non-loopback API target."
}

$scriptPath = Join-Path $root ("k6\" + $Scenario + ".js")
$resultsPath = Join-Path $root "results"
New-Item -ItemType Directory -Force -Path $resultsPath | Out-Null
$stamp = Get-Date -Format "yyyyMMdd-HHmmss"
$summaryPath = Join-Path $resultsPath ("{0}-{1}.json" -f $Scenario, $stamp)
$metricsPath = Join-Path $resultsPath ("{0}-{1}-metrics.ndjson" -f $Scenario, $stamp)

& $k6.Source run --include-system-env-vars --summary-export $summaryPath --out ("json=" + $metricsPath) $scriptPath
$k6ExitCode = $LASTEXITCODE

if ($Scenario -eq "observations" -or $Scenario -eq "combined") {
    $LASTEXITCODE = 0
    & (Join-Path $root "summarize-rabbit.ps1") -MetricsPath $metricsPath
    if ($LASTEXITCODE -ne 0) {
        throw "RabbitMQ sample analysis failed. See $metricsPath"
    }
}

if ($k6ExitCode -ne 0) {
    throw "k6 run failed with exit code $k6ExitCode. See $summaryPath and $metricsPath"
}

Write-Host "k6 summary: $summaryPath"
Write-Host "k6 raw metrics: $metricsPath"
