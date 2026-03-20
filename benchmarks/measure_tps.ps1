<#
Usage: .\measure_tps.ps1 -LogFile "path\to\logs\latest.log" -Lines 2000

This script scans recent server log lines for common TPS or ms/t patterns and computes a simple average TPS.
#>
param(
    [string]$LogFile = "logs\latest.log",
    [int]$Lines = 2000
)

if (-not (Test-Path $LogFile)) {
    Write-Error "Log file not found: $LogFile"
    exit 1
}

$lines = Get-Content -Path $LogFile -Tail $Lines -Encoding UTF8
$values = @()

foreach ($l in $lines) {
    # Pattern: TPS: 19.8 or tps=19.8
    if ($l -match '(?i)tps[:=]?\s*([0-9]+(?:\.[0-9]+)?)') {
        $values += [double]$Matches[1]
        continue
    }
    # Pattern: 1.23 ms/t  -> convert to TPS = 1000 / ms
    if ($l -match '([0-9]+(?:\.[0-9]+)?)\s*ms/t') {
        $ms = [double]$Matches[1]
        if ($ms -gt 0) { $values += (1000.0 / $ms) }
        continue
    }
}

if ($values.Count -eq 0) {
    Write-Host "No TPS or ms/t patterns found in the last $Lines lines. Adjust log verbosity or patterns."
    exit 0
}

$avg = ($values | Measure-Object -Average).Average
$min = ($values | Measure-Object -Minimum).Minimum
$max = ($values | Measure-Object -Maximum).Maximum

Write-Host "Samples: $($values.Count)  AvgTPS: $([math]::Round($avg,2))  Min: $([math]::Round($min,2))  Max: $([math]::Round($max,2))"
# Optionally output CSV
$csvLine = "$(Get-Date -Format o),$([math]::Round($avg,2)),$([math]::Round($min,2)),$([math]::Round($max,2)),$($values.Count)"
$outCsv = Join-Path -Path (Split-Path $LogFile -Parent) -ChildPath "benchmark_results.csv"
Add-Content -Path $outCsv -Value $csvLine
Write-Host "Appended results to $outCsv"