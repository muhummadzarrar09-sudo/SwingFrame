$ErrorActionPreference = "Stop"
$ProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$Apk = Join-Path $ProjectRoot "artifacts\SwingFrame.apk"

if (-not $env:ANDROID_SDK_ROOT) {
    if ($env:ANDROID_HOME) {
        $env:ANDROID_SDK_ROOT = $env:ANDROID_HOME
    } else {
        $env:ANDROID_SDK_ROOT = Join-Path $env:LOCALAPPDATA "Android\Sdk"
    }
}
$Adb = Join-Path $env:ANDROID_SDK_ROOT "platform-tools\adb.exe"

if (-not (Test-Path $Adb)) {
    throw "adb.exe not found. Install Android SDK Platform-Tools from Android Studio's SDK Manager."
}
if (-not (Test-Path $Apk)) {
    throw "APK not found. Run .\swingframe-build.ps1 from the project root first."
}

Write-Host "Connected devices:" -ForegroundColor Cyan
& $Adb devices
Write-Host "`nInstalling SwingFrame..." -ForegroundColor Green
& $Adb install -r $Apk
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
Write-Host "`nInstalled. Open SwingFrame from the phone's app drawer." -ForegroundColor Green
