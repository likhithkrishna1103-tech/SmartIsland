<#
Smart Island (2026)
© Animesh Gupta — github.com/agupta07505
Licensed under the GNU GPL v3License
Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
#>

param(
    [Parameter(Mandatory = $true)]
    [string]$KeystorePath,

    [string]$OutputPath
)

$resolvedKeystore = Resolve-Path -LiteralPath $KeystorePath

if (-not $OutputPath) {
    $fileName = [System.IO.Path]::GetFileNameWithoutExtension($resolvedKeystore.Path)
    $OutputPath = Join-Path -Path (Get-Location) -ChildPath "$($fileName)_base64.txt"
}

$base64 = [Convert]::ToBase64String([System.IO.File]::ReadAllBytes($resolvedKeystore.Path))
Set-Content -LiteralPath $OutputPath -Value $base64 -Encoding ascii -NoNewline

Write-Host "Wrote base64 keystore to: $OutputPath"
Write-Host "Copy this file's content into the GitHub secret: ANDROID_KEYSTORE_BASE64"
