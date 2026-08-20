<#
    swingframe-build.ps1 - build SwingFrame, install it, and watch the logs.

    Usage:
        .\swingframe-build.ps1                 build + install + open logcat
        .\swingframe-build.ps1 -NoInstall      build only, then reveal the APK
        .\swingframe-build.ps1 -Clean          wipe build dirs first
        .\swingframe-build.ps1 -Release        build an unsigned release APK
        .\swingframe-build.ps1 -NoLogcat       skip the log tail
        .\swingframe-build.ps1 -Offline        build without hitting the network

    NOTE: this file is deliberately pure ASCII.
#>
param(
    [switch]$Clean,
    [switch]$NoInstall,
    [switch]$NoLogcat,
    [switch]$Release,
    [switch]$Offline
)

$ErrorActionPreference = "Stop"

$AppId   = "app.swingframe"
$Variant = if ($Release) { "release" } else { "debug" }
$Task    = if ($Release) { "assembleRelease" } else { "assembleDebug" }

function Step { param($m) Write-Host "`n[ >> ] $m" -ForegroundColor Cyan }
function Ok   { param($m) Write-Host "[ OK ] $m" -ForegroundColor Green }
function Warn { param($m) Write-Host "[ ~~ ] $m" -ForegroundColor Yellow }
function Info { param($m) Write-Host "       $m" -ForegroundColor DarkGray }
function Die  {
    param($m, $hint)
    Write-Host "`n[ !! ] $m" -ForegroundColor Red
    if ($hint) { Write-Host "       $hint" -ForegroundColor Yellow }
    exit 1
}

# Structural validation (ZIP magic bytes) for a downloaded wrapper JAR. This is a sanity
# check only - see the checksum warning next to the download for the remaining gap.
function Test-ZipSignature {
    param([string]$Path)
    try {
        $fs = [System.IO.File]::OpenRead($Path)
        $sig = New-Object byte[] 4
        $null = $fs.Read($sig, 0, 4)
        $fs.Close()
        return ($sig[0] -eq 0x50 -and $sig[1] -eq 0x4B -and $sig[2] -eq 0x03 -and $sig[3] -eq 0x04)
    } catch {
        return $false
    }
}

$sw = [System.Diagnostics.Stopwatch]::StartNew()

Write-Host ""
Write-Host "  ____          _             _____                        " -ForegroundColor DarkCyan
Write-Host " / ___|_      _(_)_ __   __ _|  ___| __ __ _ _ __ ___   ___" -ForegroundColor DarkCyan
Write-Host " \___ \ \ /\ / / | '_ \ / _` | |_ | '__/ _` | '_ ` _ \ / _ \" -ForegroundColor DarkCyan
Write-Host "  ___) \ V  V /| | | | | (_| |  _|| | | (_| | | | | | |  __/" -ForegroundColor DarkCyan
Write-Host " |____/ \_/\_/ |_|_| |_|\__, |_|  |_|  \__,_|_| |_| |_|\___|" -ForegroundColor DarkCyan
Write-Host "                        |___/                               " -ForegroundColor DarkCyan
Write-Host "   the ultimate on-device AI golf coach                     " -ForegroundColor DarkGray
Write-Host ""

# ---------------------------------------------------------- 0. sanity checks
if (-not (Test-Path "settings.gradle.kts")) {
    Die "No settings.gradle.kts here - you must run this from the project root."
}

if (-not (Get-Command java -ErrorAction SilentlyContinue) -and -not $env:JAVA_HOME) {
    Die "No Java found." "Install Eclipse Temurin JDK 17: https://adoptium.net/temurin/releases/?version=17"
}

# ------------------------------------------------- 1. local.properties / SDK
Step "Checking Android SDK..."
if (-not (Test-Path "local.properties")) {
    $sdk = @($env:ANDROID_HOME, $env:ANDROID_SDK_ROOT,
             "$env:LOCALAPPDATA\Android\Sdk", "C:\Android\Sdk") |
           Where-Object { $_ -and (Test-Path $_) } | Select-Object -First 1

    if ($sdk) {
        $escaped = $sdk -replace '\\', '\\\\' -replace ':', '\:'
        "sdk.dir=$escaped" | Set-Content "local.properties" -Encoding ASCII
        Ok "Wrote local.properties -> $sdk"
    } else {
        Die "Android SDK not found." `
            "Install Android Studio, or the command-line tools, then set ANDROID_HOME."
    }
} else {
    Ok "local.properties present."
}

# ------------------------------------------------------- 2. Gradle wrapper
Step "Checking Gradle wrapper..."
$jarPath = "gradle\wrapper\gradle-wrapper.jar"
$jarLooksValid = $false

if (Test-Path $jarPath) {
    $jarLooksValid = ((Get-Item $jarPath).Length -gt 10000) -and (Test-ZipSignature $jarPath)
    if (-not $jarLooksValid) {
        Warn "Existing wrapper JAR looks corrupt. Re-downloading."
        Remove-Item $jarPath -Force -ErrorAction SilentlyContinue
    }
}

if (-not $jarLooksValid) {
    Warn "Wrapper JAR missing - fetching it (one time only)."
    New-Item -ItemType Directory -Force -Path "gradle\wrapper" | Out-Null
    
    $gradleVer = "8.7"
    if (Test-Path "gradle\wrapper\gradle-wrapper.properties") {
        $m = Select-String -Path "gradle\wrapper\gradle-wrapper.properties" `
                           -Pattern "gradle-([\d.]+)-bin" -ErrorAction SilentlyContinue
        if ($m) { $gradleVer = $m.Matches[0].Groups[1].Value }
    }
    Info "Targeting Gradle $gradleVer"

    try {
        [Net.ServicePointManager]::SecurityProtocol =
            [Net.SecurityProtocolType]::Tls12 -bor [Net.ServicePointManager]::SecurityProtocol
    } catch { }

    $got = $false
    $jarUrl = "https://raw.githubusercontent.com/gradle/gradle/v$gradleVer/gradle/wrapper/gradle-wrapper.jar"
    
    try {
        Info "Trying GitHub..."
        Invoke-WebRequest -Uri $jarUrl -OutFile $jarPath -UseBasicParsing -TimeoutSec 60
        if (((Get-Item $jarPath).Length -gt 10000) -and (Test-ZipSignature $jarPath)) {
            $got = $true
            Ok "Downloaded gradle-wrapper.jar."
            Warn "NOTE: the wrapper JAR is validated structurally (ZIP magic) but not cryptographically."
            Warn "For supply-chain safety, pin the known SHA-256 of the gradle $gradleVer wrapper JAR in this script."
        }
    } catch {
        Info "GitHub route failed: $($_.Exception.Message)"
    }

    if (-not $got) {
        Warn "Falling back to the full Gradle distribution (~130 MB)."
        $zip = "$env:TEMP\gradle-$gradleVer-bin.zip"
        $ex  = "$env:TEMP\gradle-$gradleVer-extract"
        try {
            Invoke-WebRequest -Uri "https://services.gradle.org/distributions/gradle-$gradleVer-bin.zip" `
                              -OutFile $zip -UseBasicParsing -TimeoutSec 600
            Expand-Archive -Path $zip -DestinationPath $ex -Force
            
            $found = Get-ChildItem $ex -Recurse -Filter "gradle-wrapper.jar" -ErrorAction SilentlyContinue |
                     Select-Object -First 1
            if (-not $found) { Die "gradle-wrapper.jar not found inside the distribution." }
            
            Copy-Item $found.FullName $jarPath -Force
            Ok "Extracted gradle-wrapper.jar."
        } catch {
            Die "Could not obtain gradle-wrapper.jar: $($_.Exception.Message)" `
                "Check your internet connection, then re-run."
        } finally {
            Remove-Item $zip -Force -ErrorAction SilentlyContinue
            Remove-Item $ex -Recurse -Force -ErrorAction SilentlyContinue
        }
    }
} else {
    Ok "Wrapper JAR present."
}

if (-not (Test-Path "gradlew.bat")) { Die "gradlew.bat is missing." "It should be committed with the project. Re-pull the repo." }

# ------------------------------------------------------------------ 3. clean
if ($Clean) {
    Step "Cleaning..."
    & .\gradlew.bat clean --console=plain
    Get-ChildItem -Path . -Include "build" -Recurse -Directory -ErrorAction SilentlyContinue |
        Where-Object { $_.FullName -notmatch "\\.git\\" } |
        ForEach-Object { Remove-Item $_.FullName -Recurse -Force -ErrorAction SilentlyContinue }
    Ok "Build directories removed."
}

# ------------------------------------------------------------------ 4. build
Step "Building ($Variant)... first run pulls Gradle + dependencies, be patient."

$gradleArgs = @($Task, "testDebugUnitTest", "--console=plain", "--warning-mode=summary")
if ($Offline) { $gradleArgs += "--offline" }

& .\gradlew.bat @gradleArgs
$code = $LASTEXITCODE

if ($code -ne 0) {
    Write-Host ""
    Write-Host "[ !! ] Build failed (exit $code)." -ForegroundColor Red
    Write-Host ""
    Write-Host "  Common causes, in order of likelihood:" -ForegroundColor Yellow
    Write-Host "   1. Wrong JDK -> Android 13+ / AGP 8+ wants JDK 17." -ForegroundColor Gray
    Write-Host "   2. 'SDK location not found' -> delete local.properties and re-run." -ForegroundColor Gray
    Write-Host "   3. 'licenses have not been accepted' -> sdkmanager --licenses" -ForegroundColor Gray
    Write-Host "   4. Out of memory -> raise org.gradle.jvmargs in gradle.properties." -ForegroundColor Gray
    Write-Host ""
    Write-Host "  For the full story:  .\gradlew.bat $Task --stacktrace" -ForegroundColor Cyan
    Write-Host ""
    exit 1
}

# -------------------------------------------------------------- 5. find APK
Step "Locating APK..."
$apk = Get-ChildItem -Path "app\build\outputs\apk\$Variant" -Filter "*.apk" -Recurse -ErrorAction SilentlyContinue |
       Sort-Object LastWriteTime -Descending | Select-Object -First 1

if (-not $apk) { Die "No APK produced, despite the build reporting success." }

$sizeMb = [math]::Round($apk.Length / 1MB, 2)
Ok "APK: $($apk.Name)  ($sizeMb MB)"
Info $apk.FullName

# ---------------------------------------------------------------- 6. install
$installed = $false

if (-not $NoInstall) {
    Step "Looking for a connected phone..."
    $sdkDir = ($env:ANDROID_HOME, $env:ANDROID_SDK_ROOT, "$env:LOCALAPPDATA\Android\Sdk" |
               Where-Object { $_ -and (Test-Path $_) } | Select-Object -First 1)
               
    $adb = if ($sdkDir) { Join-Path $sdkDir "platform-tools\adb.exe" } else { $null }

    if (-not ($adb -and (Test-Path $adb))) {
        $c = Get-Command adb -ErrorAction SilentlyContinue
        if ($c) { $adb = $c.Source } else { $adb = $null }
    }

    if ($adb) {
        function Invoke-Adb {
            param([string[]]$AdbArgs)
            $prev = $ErrorActionPreference
            $ErrorActionPreference = "SilentlyContinue"
            $out = & $adb @AdbArgs 2>&1 | ForEach-Object { "$_" }
            $script:AdbExit = $LASTEXITCODE
            $ErrorActionPreference = $prev
            return @($out)
        }

        Info "Starting adb server..."
        $null = Invoke-Adb @("start-server")
        
        $devRaw   = Invoke-Adb @("devices")
        $devLines = @($devRaw | Select-Object -Skip 1 | Where-Object { $_.Trim() -ne "" })
        
        $ready        = @($devLines | Where-Object { $_ -match "\sdevice$" })
        $unauthorized = @($devLines | Where-Object { $_ -match "unauthorized" })
        $offline      = @($devLines | Where-Object { $_ -match "offline" })

        if ($ready.Count -gt 0) {
            Ok "Device ready: $($ready[0].Trim())"
            Step "Installing..."
            
            $installOut = Invoke-Adb @("install", "-r", "-d", $apk.FullName)
            foreach ($l in $installOut) { if ($l.Trim()) { Info $l.Trim() } }

            if (($installOut -join " ") -match "Success") {
                $installed = $true
                Ok "Installed."
                
                Step "Launching..."
                $null = Invoke-Adb @("shell", "monkey", "-p", $AppId, "-c", "android.intent.category.LAUNCHER", "1")
                Ok "Launched on device."
            } else {
                Warn "adb install did not report Success."
                if (($installOut -join " ") -match "INSTALL_FAILED_UPDATE_INCOMPATIBLE|signatures do not match") {
                    Info "A different build of $AppId is already installed."
                    Info "Uninstall it first:  adb uninstall $AppId"
                } elseif (($installOut -join " ") -match "INSTALL_FAILED_INSUFFICIENT_STORAGE") {
                    Info "Not enough free space on the phone."
                }
                Info "Falling back to manual transfer."
            }
        } elseif ($unauthorized.Count -gt 0) {
            Warn "Device is connected but UNAUTHORIZED."
            Info "Unlock the phone and tap 'Allow' on the USB debugging prompt."
        } elseif ($offline.Count -gt 0) {
            Warn "Device reports offline."
            Info "Try: adb kill-server   then replug the cable."
        } else {
            Warn "No device detected over USB."
            Info "Enable Developer Options -> USB debugging, plug in a DATA cable, then tap Allow."
        }
    } else {
        Warn "adb not found."
    }
}

if (-not $installed) {
    Step "Opening the APK folder for manual transfer..."
    Start-Process explorer.exe -ArgumentList "/select`"$($apk.FullName)`""
    Write-Host "`n  Send the APK to your phone via cable or Drive, then tap it." -ForegroundColor Yellow
}

# ----------------------------------------------------------------- 7. done
$sw.Stop()
Write-Host ""
Write-Host ("-" * 62) -ForegroundColor DarkGray
Write-Host "  DONE in $([math]::Round($sw.Elapsed.TotalSeconds,1))s  |  $Variant  |  $sizeMb MB" -ForegroundColor Magenta
Write-Host ("-" * 62) -ForegroundColor DarkGray

# --------------------------------------------------------------- 8. logcat
if ($installed -and -not $NoLogcat) {
    Write-Host "`n  Tailing logcat for $AppId. Ctrl+C to stop.`n" -ForegroundColor Cyan
    
    $ErrorActionPreference = "SilentlyContinue"
    Start-Sleep -Seconds 2
    
    $pidRaw = ""
    try {
        $pidRaw = (& $adb shell pidof -s $AppId 2>&1 | ForEach-Object { "$_" }) -join ""
        $pidRaw = $pidRaw.Trim()
    } catch { }

    if ($pidRaw -match '^\d+$') {
        & $adb logcat "--pid=$pidRaw"
    } else {
        Info "Could not resolve the app PID; tailing everything instead."
        & $adb logcat -v brief
    }
}
