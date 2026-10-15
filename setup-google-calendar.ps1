param([string]$OAuthJson)
$ErrorActionPreference = 'Stop'
$configDir = Join-Path $PSScriptRoot 'backend/config'
$configPath = Join-Path $configDir 'google-calendar.properties'
New-Item -ItemType Directory -Path $configDir -Force | Out-Null
$settings = @{}
if (Test-Path -LiteralPath $configPath) {
    foreach ($line in [System.IO.File]::ReadAllLines($configPath)) {
        if ($line -match '^([^#=]+)=(.*)$') { $settings[$matches[1]] = $matches[2] }
    }
}
if ($OAuthJson) {
    $oauth = Get-Content -LiteralPath $OAuthJson -Raw | ConvertFrom-Json
    if (-not $oauth.web.client_id -or -not $oauth.web.client_secret) {
        throw 'Use the downloaded JSON for an OAuth client of type Web application.'
    }
    $redirect = 'http://127.0.0.1:5173/api/google-calendar/callback'
    if ($oauth.web.redirect_uris -notcontains $redirect) {
        throw "Add this Authorized redirect URI in Google Cloud and download JSON again: $redirect"
    }
    foreach ($value in @($oauth.web.client_id, $oauth.web.client_secret)) {
        if ($value -match '[\r\n\\]') { throw 'Unexpected format of OAuth credentials.' }
    }
    $settings['google.calendar.client-id'] = $oauth.web.client_id
    $settings['google.calendar.client-secret'] = $oauth.web.client_secret
}
if (-not $settings['google.calendar.encryption-key']) {
    $bytes = New-Object byte[] 32
    $random = [System.Security.Cryptography.RandomNumberGenerator]::Create()
    try { $random.GetBytes($bytes) } finally { $random.Dispose() }
    $settings['google.calendar.encryption-key'] = [Convert]::ToBase64String($bytes)
}
$settings['google.calendar.redirect-uri'] = 'http://127.0.0.1:5173/api/google-calendar/callback'
$settings['google.calendar.frontend-url'] = 'http://127.0.0.1:5173/'
$lines = @('# Local Google Calendar configuration. Do not commit or share this file.')
$lines += $settings.Keys | Sort-Object | ForEach-Object { "$_=$($settings[$_])" }
[System.IO.File]::WriteAllLines($configPath, [string[]]$lines, [System.Text.Encoding]::ASCII)
if ($settings['google.calendar.client-id'] -and $settings['google.calendar.client-secret']) {
    Write-Host 'Google Calendar configuration saved. Restart the backend, then connect in StudyPlanner.'
} else {
    Write-Host 'Local encryption key prepared. Run this script with -OAuthJson and the downloaded Google client JSON.'
}
