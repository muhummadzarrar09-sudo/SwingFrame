# swingframe-build.ps1
# Builds SwingFrame.apk and opens Explorer with the APK selected.

param(
    [string]$ProjectPath = $PSScriptRoot
)

$ErrorActionPreference = "Stop"

function Write-Step { param($Message) Write-Host "`n[ >> ] $Message" -ForegroundColor Cyan }
function Write-Ok   { param($Message) Write-Host "[ OK ] $Message" -ForegroundColor Green }
function Write-Warn { param($Message) Write-Host "[ ~~ ] $Message" -ForegroundColor Yellow }
function Write-Fail { param($Message) Write-Host "[ !! ] $Message" -ForegroundColor Red; exit 1 }

try {
    Set-Location $ProjectPath
} catch {
    Write-Fail "Project path does not exist: $ProjectPath"
}

if (-not (Test-Path "settings.gradle.kts")) {
    Write-Fail "Not an Android project root. Put this file in the SwingFrame folder and run it there."
}

# ---- 0. Source revision preflight ------------------------------------------

$RevisionFile = "SOURCE_REVISION.txt"
if (Test-Path $RevisionFile) {
    $Revision = (Get-Content $RevisionFile -Raw).Trim()
    Write-Ok "Source revision: $Revision"
} else {
    Write-Warn "SOURCE_REVISION.txt is missing; this may be an outdated workspace copy."
}

$AnnotationCanvas = "app\src\main\java\app\swingframe\ui\AnnotationCanvas.kt"
if (Test-Path $AnnotationCanvas) {
    $AnnotationSource = Get-Content $AnnotationCanvas -Raw
    if ($AnnotationSource.Contains("when (val drag = selectDrag)") -and
        -not $AnnotationSource.Contains("null -> draft")) {
        Write-Fail "Outdated AnnotationCanvas.kt detected. Download/overwrite the current SwingFrame workspace before building."
    }
}

# ---- 1. Java ---------------------------------------------------------------

Write-Step "Checking Java..."

$JavaExe = $null
if ($env:JAVA_HOME -and (Test-Path (Join-Path $env:JAVA_HOME "bin\java.exe"))) {
    $JavaExe = Join-Path $env:JAVA_HOME "bin\java.exe"
}

if (-not $JavaExe) {
    $PathJava = Get-Command java.exe -ErrorAction SilentlyContinue
    if ($PathJava) {
        $JavaExe = $PathJava.Source
    }
}

if (-not $JavaExe) {
    $JdkCandidates = @(
        (Join-Path $env:ProgramFiles "Android\Android Studio\jbr"),
        (Join-Path $env:LOCALAPPDATA "Programs\Android Studio\jbr")
    )

    $JdkRoots = @(
        (Join-Path $env:ProgramFiles "Eclipse Adoptium"),
        (Join-Path $env:ProgramFiles "Microsoft"),
        (Join-Path $env:ProgramFiles "Java")
    )

    foreach ($Root in $JdkRoots) {
        if (Test-Path $Root) {
            $JdkCandidates += Get-ChildItem $Root -Directory -ErrorAction SilentlyContinue |
                Sort-Object LastWriteTime -Descending |
                ForEach-Object { $_.FullName }
        }
    }

    $JdkHome = $JdkCandidates |
        Where-Object { Test-Path (Join-Path $_ "bin\java.exe") } |
        Select-Object -First 1

    if ($JdkHome) {
        $env:JAVA_HOME = $JdkHome
        $env:Path = "$(Join-Path $JdkHome 'bin');$env:Path"
        $JavaExe = Join-Path $JdkHome "bin\java.exe"
    }
}

if (-not $JavaExe) {
    Write-Fail "JDK 17+ not found. Install it with: winget install --exact --id EclipseAdoptium.Temurin.17.JDK"
}

Write-Ok "Java found: $JavaExe"

# ---- 2. Android SDK --------------------------------------------------------

Write-Step "Checking Android SDK..."

if (-not $env:ANDROID_SDK_ROOT) {
    if ($env:ANDROID_HOME) {
        $env:ANDROID_SDK_ROOT = $env:ANDROID_HOME
    } else {
        $env:ANDROID_SDK_ROOT = Join-Path $env:LOCALAPPDATA "Android\Sdk"
    }
}
$env:ANDROID_HOME = $env:ANDROID_SDK_ROOT
$env:Path = "$(Join-Path $env:ANDROID_SDK_ROOT 'platform-tools');$env:Path"

if (-not (Test-Path $env:ANDROID_SDK_ROOT)) {
    Write-Fail "Android SDK not found at $env:ANDROID_SDK_ROOT. Install Android Studio and its Android SDK."
}

if (-not (Test-Path (Join-Path $env:ANDROID_SDK_ROOT "platforms\android-36\android.jar"))) {
    Write-Fail "Android API 36 is missing. In Android Studio open Tools > SDK Manager and install Android SDK Platform 36 plus Build-Tools 36.0.0."
}

Write-Ok "Android SDK found: $env:ANDROID_SDK_ROOT"

# ---- 3. Gradle wrapper bootstrap ------------------------------------------

Write-Step "Checking Gradle wrapper..."

$GradleVersion = "8.13"
$WrapperBat = "gradlew.bat"
$WrapperDirectory = "gradle\wrapper"
$WrapperJar = Join-Path $WrapperDirectory "gradle-wrapper.jar"
$WrapperProperties = Join-Path $WrapperDirectory "gradle-wrapper.properties"

$WrapperComplete =
    (Test-Path $WrapperBat) -and
    (Test-Path $WrapperJar) -and
    (Test-Path $WrapperProperties)

if (-not $WrapperComplete) {
    Write-Warn "Gradle wrapper is incomplete. Bootstrapping Gradle $GradleVersion..."
    New-Item -ItemType Directory -Force -Path $WrapperDirectory | Out-Null

    $Properties = @"
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-$GradleVersion-bin.zip
networkTimeout=120000
validateDistributionUrl=true
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
"@
    $Properties | Set-Content $WrapperProperties -Encoding ASCII

    $RawBase = "https://raw.githubusercontent.com/gradle/gradle/v$GradleVersion.0"
    try {
        Write-Warn "Downloading gradle-wrapper.jar..."
        Invoke-WebRequest `
            -Uri "$RawBase/gradle/wrapper/gradle-wrapper.jar" `
            -OutFile $WrapperJar `
            -UseBasicParsing `
            -ErrorAction Stop

        Write-Warn "Downloading gradlew.bat..."
        Invoke-WebRequest `
            -Uri "$RawBase/gradlew.bat" `
            -OutFile $WrapperBat `
            -UseBasicParsing `
            -ErrorAction Stop
    } catch {
        Write-Warn "Direct wrapper download failed. Using the full Gradle distribution..."

        $BootstrapRoot = Join-Path $env:TEMP "swingframe-gradle-bootstrap"
        $ZipPath = Join-Path $BootstrapRoot "gradle-$GradleVersion-bin.zip"
        $ExtractPath = Join-Path $BootstrapRoot "distribution"
        $TemporaryProject = Join-Path $BootstrapRoot "wrapper-project"

        Remove-Item $BootstrapRoot -Recurse -Force -ErrorAction SilentlyContinue
        New-Item -ItemType Directory -Force -Path $ExtractPath | Out-Null
        New-Item -ItemType Directory -Force -Path $TemporaryProject | Out-Null

        Invoke-WebRequest `
            -Uri "https://services.gradle.org/distributions/gradle-$GradleVersion-bin.zip" `
            -OutFile $ZipPath `
            -UseBasicParsing
        Expand-Archive -Path $ZipPath -DestinationPath $ExtractPath -Force
        "rootProject.name = 'wrapper-bootstrap'" |
            Set-Content (Join-Path $TemporaryProject "settings.gradle") -Encoding ASCII

        $GradleBat = Join-Path $ExtractPath "gradle-$GradleVersion\bin\gradle.bat"
        Push-Location $TemporaryProject
        try {
            & $GradleBat --no-daemon wrapper --gradle-version $GradleVersion --distribution-type bin
            if ($LASTEXITCODE -ne 0) {
                Write-Fail "Gradle wrapper bootstrap failed."
            }
        } finally {
            Pop-Location
        }

        Copy-Item (Join-Path $TemporaryProject "gradlew.bat") $WrapperBat -Force
        Copy-Item (Join-Path $TemporaryProject "gradle\wrapper\gradle-wrapper.jar") $WrapperJar -Force
        Copy-Item (Join-Path $TemporaryProject "gradle\wrapper\gradle-wrapper.properties") $WrapperProperties -Force
        Remove-Item $BootstrapRoot -Recurse -Force -ErrorAction SilentlyContinue
    }

    if (-not (Test-Path $WrapperBat) -or -not (Test-Path $WrapperJar)) {
        Write-Fail "Could not create the Gradle wrapper."
    }
    Write-Ok "Gradle wrapper ready."
} else {
    Write-Ok "Gradle wrapper found."
}

# ---- 4. Build --------------------------------------------------------------

Write-Step "Building SwingFrame APK (the first run downloads Gradle and dependencies)..."

& .\gradlew.bat testDebugUnitTest lintDebug assembleDebug --stacktrace
if ($LASTEXITCODE -ne 0) {
    Write-Fail "Build failed. Copy the first compiler error and the 'What went wrong' section."
}

# ---- 5. Find and rename APK ------------------------------------------------

Write-Step "Locating APK..."

$BuiltApk = Get-ChildItem `
    -Path "app\build\outputs\apk\debug" `
    -Filter "*.apk" `
    -File `
    -ErrorAction SilentlyContinue |
    Select-Object -First 1

if (-not $BuiltApk) {
    Write-Fail "APK not found. The build may have failed without producing an output."
}

$ArtifactDirectory = Join-Path (Get-Location) "artifacts"
$FinalApk = Join-Path $ArtifactDirectory "SwingFrame.apk"
New-Item -ItemType Directory -Force -Path $ArtifactDirectory | Out-Null
Copy-Item $BuiltApk.FullName $FinalApk -Force

Write-Ok "APK ready: $FinalApk"
$Hash = Get-FileHash $FinalApk -Algorithm SHA256
Write-Host "[ SHA256 ] $($Hash.Hash)" -ForegroundColor DarkGray

# ---- 6. Open output folder -------------------------------------------------

Write-Step "Opening APK folder..."
Start-Process explorer.exe -ArgumentList "/select,`"$FinalApk`""

Write-Host "`n[ DONE ] SwingFrame.apk is selected in Explorer." -ForegroundColor Magenta
Write-Host "         Copy it to your phone and open it to install." -ForegroundColor Magenta
Write-Host "         Android may ask you to enable 'Install unknown apps' for Files, Drive, or your browser.`n" -ForegroundColor Yellow
