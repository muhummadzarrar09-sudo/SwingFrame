param(
    [switch]$Clean
)

$ErrorActionPreference = "Stop"
$ProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
Set-Location $ProjectRoot

function Test-JavaHome([string]$HomePath) {
    return $HomePath -and (Test-Path (Join-Path $HomePath "bin\java.exe"))
}

# Discover Android Studio's bundled JDK or a separately installed JDK 17.
if (-not (Test-JavaHome $env:JAVA_HOME)) {
    $JdkCandidates = @(
        (Join-Path $env:ProgramFiles "Android\Android Studio\jbr"),
        (Join-Path $env:LOCALAPPDATA "Programs\Android Studio\jbr")
    )

    $SearchRoots = @(
        (Join-Path $env:ProgramFiles "Eclipse Adoptium"),
        (Join-Path $env:ProgramFiles "Microsoft"),
        (Join-Path $env:ProgramFiles "Java")
    )
    foreach ($SearchRoot in $SearchRoots) {
        if (Test-Path $SearchRoot) {
            $JdkCandidates += Get-ChildItem $SearchRoot -Directory -ErrorAction SilentlyContinue |
                Where-Object { $_.Name -match "jdk-?(17|18|19|20|21|22|23|24|25)" } |
                Sort-Object LastWriteTime -Descending |
                ForEach-Object { $_.FullName }
        }
    }

    $DetectedJdk = $JdkCandidates |
        Where-Object { Test-JavaHome $_ } |
        Select-Object -First 1

    if ($DetectedJdk) {
        $env:JAVA_HOME = $DetectedJdk
    } else {
        throw @"
JDK 17+ was not found.

Fastest fix:
  winget install --exact --id EclipseAdoptium.Temurin.17.JDK

Then close and reopen PowerShell, return to this folder, and run:
  .\scripts\build-windows.ps1

Android Studio's bundled JDK is also supported.
"@
    }
}

$JavaExe = Join-Path $env:JAVA_HOME "bin\java.exe"
$PreviousErrorActionPreference = $ErrorActionPreference
try {
    # `java -version` writes normal version information to stderr. Windows PowerShell 5
    # wraps that output as NativeCommandError when ErrorActionPreference is Stop.
    $ErrorActionPreference = "Continue"
    $JavaVersionText = (& $JavaExe -version 2>&1 | Out-String)
} finally {
    $ErrorActionPreference = $PreviousErrorActionPreference
}
if ($JavaVersionText -notmatch 'version "(?<major>\d+)') {
    throw "Could not determine the Java version at $JavaExe"
}
if ([int]$Matches.major -lt 17) {
    throw "Java $($Matches.major) is too old. SwingFrame requires JDK 17 or newer. Current JAVA_HOME: $env:JAVA_HOME"
}
$env:Path = "$(Join-Path $env:JAVA_HOME 'bin');$env:Path"

if (-not $env:ANDROID_SDK_ROOT) {
    if ($env:ANDROID_HOME) {
        $env:ANDROID_SDK_ROOT = $env:ANDROID_HOME
    } else {
        $env:ANDROID_SDK_ROOT = Join-Path $env:LOCALAPPDATA "Android\Sdk"
    }
}
$env:ANDROID_HOME = $env:ANDROID_SDK_ROOT
$env:Path = "$(Join-Path $env:ANDROID_SDK_ROOT 'platform-tools');$env:Path"

if (-not (Test-Path (Join-Path $env:ANDROID_SDK_ROOT "platforms\android-36\android.jar"))) {
    throw "Android SDK Platform 36 is missing. In Android Studio open Tools > SDK Manager and install Android SDK Platform 36 and Build-Tools 36.0.0."
}

Write-Host "`nUsing Java:" -ForegroundColor Cyan
Write-Host $JavaVersionText.Trim()
Write-Host "JAVA_HOME: $env:JAVA_HOME"
Write-Host "`nUsing Android SDK: $env:ANDROID_SDK_ROOT" -ForegroundColor Cyan
Write-Host "`nChecking Gradle wrapper..." -ForegroundColor Cyan
& .\gradlew.bat --version
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

$Tasks = @("testDebugUnitTest", "assembleDebug", "--stacktrace")
if ($Clean) { $Tasks = @("clean") + $Tasks }

Write-Host "`nBuilding SwingFrame APK..." -ForegroundColor Green
& .\gradlew.bat @Tasks
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

$BuiltApk = Join-Path $ProjectRoot "app\build\outputs\apk\debug\app-debug.apk"
$ArtifactDir = Join-Path $ProjectRoot "artifacts"
$FinalApk = Join-Path $ArtifactDir "SwingFrame.apk"
New-Item -ItemType Directory -Force -Path $ArtifactDir | Out-Null
Copy-Item -Force $BuiltApk $FinalApk

Write-Host "`nAPK ready:" -ForegroundColor Green
Write-Host $FinalApk
Write-Host "`nSHA-256:" -ForegroundColor Cyan
Get-FileHash -Algorithm SHA256 $FinalApk | Format-List
