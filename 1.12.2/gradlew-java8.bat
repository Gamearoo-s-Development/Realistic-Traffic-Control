@echo off
setlocal

rem Runs this project using a Java 8 JDK (ForgeGradle/Gradle 2.x are not compatible with newer Java versions).

for /f "usebackq delims=" %%I in (`powershell -NoProfile -ExecutionPolicy Bypass -Command "$ErrorActionPreference='SilentlyContinue'; $candidates=@(); if($env:JAVA8_HOME){$candidates+=$env:JAVA8_HOME}; if($env:JAVA_HOME){$candidates+=$env:JAVA_HOME}; $roots=@($env:ProgramFiles, ${env:ProgramFiles(x86)}) | Where-Object { $_ -and (Test-Path $_) }; $searchDirs=@('Java','Eclipse Adoptium','Zulu','Amazon Corretto','BellSoft'); foreach($r in $roots){ foreach($d in $searchDirs){ $p=Join-Path $r $d; if(Test-Path $p){ $candidates += (Get-ChildItem -Path $p -Directory | ForEach-Object { $_.FullName }) } } }; $seen=@{}; foreach($c in $candidates){ if(-not $c){ continue }; $c=$c.Trim([char]34); if($seen.ContainsKey($c)){ continue }; $seen[$c]=$true; $java=Join-Path $c 'bin\java.exe'; if(-not (Test-Path $java)){ continue }; $ver=(& $java -version 2>&1 | Out-String); if($ver -match '1\.8\.') { Write-Output $c; break } }"`) do set "JAVA_HOME=%%I"

if not defined JAVA_HOME (
  echo.
  echo ERROR: Java 8 JDK not found.
  echo This 1.12.2 ForgeGradle setup requires Java 8 to build/run.
  echo.
  echo Fix options:
  echo   1^) Install a Java 8 JDK (Temurin 8 recommended)
  echo   2^) Set JAVA8_HOME to your JDK8 folder, e.g.:
  echo      setx JAVA8_HOME "C:\Program Files\Eclipse Adoptium\jdk8uXXX-bYY"
  echo   3^) Or set JAVA_HOME to your JDK8 folder for this shell session.
  echo.
  exit /b 1
)

echo Using JAVA_HOME=%JAVA_HOME%
call "%~dp0gradlew.bat" %*
