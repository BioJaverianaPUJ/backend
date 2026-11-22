param(
    [Parameter(Mandatory = $true)]
    [string]$MetricsPath
)

$ErrorActionPreference = "Stop"
if (-not (Test-Path $MetricsPath)) {
    throw "Metrics file not found: $MetricsPath"
}

$samples = New-Object System.Collections.Generic.List[object]
foreach ($line in [System.IO.File]::ReadLines((Resolve-Path $MetricsPath))) {
    $event = ConvertFrom-Json -InputObject $line
    if ($event.type -ne "Point" -or
        $event.metric -notin @("rabbit_messages_ready", "rabbit_messages_unacked", "rabbit_messages_total")) {
        continue
    }

    $samples.Add([pscustomobject]@{
        Metric = $event.metric
        Phase = $event.data.tags.phase
        Value = [double]$event.data.value
        Time = [datetime]$event.data.time
    })
}

if ($samples.Count -eq 0) {
    throw "No RabbitMQ metric samples found in $MetricsPath"
}

$rows = New-Object System.Collections.Generic.List[object]
foreach ($metric in @("rabbit_messages_ready", "rabbit_messages_unacked", "rabbit_messages_total")) {
    $before = @($samples | Where-Object { $_.Metric -eq $metric -and $_.Phase -eq "before" } | Sort-Object Time)
    $during = @($samples | Where-Object { $_.Metric -eq $metric -and $_.Phase -eq "during" } | Sort-Object Time)
    $after = @($samples | Where-Object { $_.Metric -eq $metric -and $_.Phase -eq "after" } | Sort-Object Time)
    if ($before.Count -eq 0 -or $during.Count -eq 0 -or $after.Count -eq 0) {
        throw "Missing before/during/after samples for $metric"
    }

    $baseline = $before[0].Value
    $final = $after[$after.Count - 1].Value
    $maxDuring = ($during | Measure-Object -Property Value -Maximum).Maximum
    $rows.Add([pscustomobject]@{
        Metric = $metric
        Baseline = $baseline
        DuringSamples = $during.Count
        MaxDuring = $maxDuring
        Final = $final
        IncreaseObservedDuring = ($maxDuring -gt $baseline)
        FinalAboveBaseline = ($final -gt $baseline)
    })
}

$rows | Format-Table -AutoSize
$ready = $rows | Where-Object { $_.Metric -eq "rabbit_messages_ready" }
if ($ready.IncreaseObservedDuring -or $ready.FinalAboveBaseline) {
    Write-Warning "RabbitMQ ready backlog increased above baseline in recorded samples. Review the time series; RNF-07 is not demonstrated."
} else {
    Write-Output "No increase in ready backlog was observed in the sampled intervals; this alone does not establish RNF-07 compliance."
}

