# Build SwingFrame from Windows PowerShell

## One-time setup

1. Install Android Studio.
2. In Android Studio, open **Tools → SDK Manager**.
3. Under **SDK Platforms**, install **Android API 36**.
4. Under **SDK Tools**, install:
   - Android SDK Build-Tools 36.0.0
   - Android SDK Platform-Tools
   - Android SDK Command-line Tools (latest)
5. Extract the SwingFrame source ZIP to a short path such as `C:\Dev\SwingFrame`.

## Build

Open PowerShell in the project directory:

```powershell
cd C:\Dev\SwingFrame
Set-ExecutionPolicy -Scope Process Bypass
.\swingframe-build.ps1
```

The first build downloads Gradle and Maven dependencies and can take several minutes. The script copies the final APK to:

```text
C:\Dev\SwingFrame\artifacts\SwingFrame.apk
```

## Install with USB

On the phone, enable Developer options and USB debugging. Connect it and approve the RSA prompt, then:

```powershell
.\scripts\install-windows.ps1
```

## Equivalent manual commands

```powershell
$env:JAVA_HOME = "$env:ProgramFiles\Android\Android Studio\jbr"
$env:ANDROID_SDK_ROOT = "$env:LOCALAPPDATA\Android\Sdk"
$env:ANDROID_HOME = $env:ANDROID_SDK_ROOT
$env:Path = "$env:JAVA_HOME\bin;$env:ANDROID_SDK_ROOT\platform-tools;$env:Path"

java -version
.\gradlew.bat --version
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug --stacktrace

New-Item -ItemType Directory -Force artifacts | Out-Null
Copy-Item -Force .\app\build\outputs\apk\debug\app-debug.apk .\artifacts\SwingFrame.apk
Get-FileHash .\artifacts\SwingFrame.apk -Algorithm SHA256

adb devices
adb install -r .\artifacts\SwingFrame.apk
```

If `adb devices` says `unauthorized`, unlock the phone and accept its USB debugging prompt. If no prompt appears, revoke USB debugging authorizations on the phone, reconnect the cable, and retry.
