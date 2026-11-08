Get-Content .env | ForEach-Object {
    if ($_ -and $_ -notmatch '^#') {
        $name, $value = $_.Split('=', 2)
        [System.Environment]::SetEnvironmentVariable($name.Trim(), $value.Trim())
    }
}