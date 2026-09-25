@REM ----------------------------------------------------------------------------
@REM Licensed to the Apache Software Foundation (ASF) under one
@REM or more contributor license agreements.  See the NOTICE file
@REM distributed with this work for additional information
@REM regarding copyright ownership.  The ASF licenses this file
@REM to you under the Apache License, Version 2.0 (the
@REM "License"); you may not use this file except in compliance
@REM with the License.  You may obtain a copy of the License at
@REM
@REM    https://www.apache.org/licenses/LICENSE-2.0
@REM
@REM Unless required by applicable law or agreed to in writing,
@REM software distributed under the License is distributed on an
@REM "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
@REM KIND, either express or implied.  See the License for the
@REM specific language governing permissions and limitations
@REM under the License.
@REM ----------------------------------------------------------------------------

@REM ----------------------------------------------------------------------------
@REM Maven Start Up Batch script
@REM
@REM Required ENV vars:
@REM JAVA_HOME - location of a JDK home dir
@REM
@REM Optional ENV vars
@REM MAVEN_BATCH_ECHO - set to 'on' to enable the echoing of the input commands
@REM MAVEN_BATCH_PAUSE - set to 'on' to wait at the end of the script
@REM MAVEN_OPTS - parameters to passed to the Java VM when running Maven
@REM     e.g. to debug Maven itself, use
@REM set MAVEN_OPTS=-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=8000
@REM MAVEN_SKIP_RC - flag to disable loading of mavenrc files
@REM ----------------------------------------------------------------------------

@REM Begin all REM lines with '@' in case MAVEN_BATCH_ECHO is 'on'
@echo off
@REM set title of command prompt
title %0
@REM enable echoing by setting MAVEN_BATCH_ECHO to 'on'
@if "%MAVEN_BATCH_ECHO%" == "on"  echo %MAVEN_BATCH_ECHO%

@REM set %HOME% to equivalent of $HOME
if "%HOME%" == "" (set "HOME=%HOMEDRIVE%%HOMEPATH%")

@REM Execute a user defined script before this one
if not "%MAVEN_SKIP_RC%" == "" goto skipRcPre
@REM Personal execute-pre will be executed before the execution of the Maven wrapper
if exist "%HOME%\.mavenrc_pre.bat" call "%HOME%\.mavenrc_pre.bat"
@REM Batch file for system-wide expressions
if exist "%SystemDrive%\etc\mavenrc_pre.bat" call "%SystemDrive%\etc\mavenrc_pre.bat"
:skipRcPre

@setlocal

set ERROR_CODE=0

@REM To isolate internal variables from possible post scripts, we use another setlocal
@setlocal

@REM ==== START VALIDATION ====
if not "%JAVA_HOME%" == "" goto OkJHome

for %%i in (java.exe) do set "JAVACMD=%%~$PATH:i"
goto checkJCmd

:OkJHome
set "JAVACMD=%JAVA_HOME%\bin\java.exe"

:checkJCmd
if exist "%EXEC_DIR%.maven\apache-maven-3.9.6\bin\mvn.cmd" (
  "%EXEC_DIR%.maven\apache-maven-3.9.6\bin\mvn.cmd" %*
  goto end
)
if exist "%JAVACMD%" goto chkMHome

echo The JAVA_HOME environment variable is not defined correctly >&2
echo This environment variable is needed to run this program >&2
echo NB: JAVA_HOME should point to a JDK not a JRE >&2
goto error

:chkMHome
set "EXEC_DIR=%~dp0"
set "WDIR=%EXEC_DIR%wrapper"

if exist "%WDIR%\maven-wrapper.jar" (
  set "WRAPPER_JAR=%WDIR%\maven-wrapper.jar"
  goto run
)
if exist "%EXEC_DIR%\.mvn\wrapper\maven-wrapper.jar" (
  set "WRAPPER_JAR=%EXEC_DIR%\.mvn\wrapper\maven-wrapper.jar"
  goto run
)

@REM Fallback to standard mvn if installed
for %%i in (mvn.cmd) do set "MVNCMD=%%~$PATH:i"
if exist "%MVNCMD%" (
  "%MVNCMD%" %*
  goto end
)
for %%i in (mvn.bat) do set "MVNCMD=%%~$PATH:i"
if exist "%MVNCMD%" (
  "%MVNCMD%" %*
  goto end
)

:run
"%JAVACMD%" %MAVEN_OPTS% -jar "%WRAPPER_JAR%" %*
if ERRORLEVEL 1 goto error
goto end

:error
set ERROR_CODE=1

:end
@endlocal & set ERROR_CODE=%ERROR_CODE%
if not "%MAVEN_SKIP_RC%" == "" goto skipRcPost
@REM Personal execute-post will be executed after the execution of the Maven wrapper
if exist "%HOME%\.mavenrc_post.bat" call "%HOME%\.mavenrc_post.bat"
@REM Batch file for system-wide expressions
if exist "%SystemDrive%\etc\mavenrc_post.bat" call "%SystemDrive%\etc\mavenrc_post.bat"
:skipRcPost

if "%MAVEN_BATCH_PAUSE%" == "on" pause

if "%MAVEN_TERMINATE_CMD%" == "on" exit %ERROR_CODE%

cmd /C exit /B %ERROR_CODE%
