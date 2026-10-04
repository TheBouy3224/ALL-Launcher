@rem
@rem Copyright 2015 the original author or authors.
@rem
@rem Licensed under the Apache License, Version 2.0 (the "License");
@rem you may not use this file except in compliance with the License.
@rem You may obtain a copy of the License at
@rem
@rem      https://www.apache.org/licenses/LICENSE-2.0
@rem
@rem Unless required by applicable law or agreed to in writing, software
@rem distributed under the License is distributed on an "AS IS" BASIS,
@rem WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
@rem See the License for the specific language governing permissions and
@rem limitations under the License.
@rem

@if "%DEBUG%" == "" @echo off
@rem ##########################################################################
@rem
@rem  Gradle startup script for Windows
@rem
@rem ##########################################################################

@rem Set local scope for the variables with windows NT shell
setlocal

@rem Ensure Windows/NDK host detection works even when a remote shell omits OS.
if not defined OS set "OS=Windows_NT"
if not defined HOST_OS set "HOST_OS=windows"

set DIRNAME=%~dp0
if "%DIRNAME%" == "" set DIRNAME=.
set APP_BASE_NAME=%~n0
set APP_HOME=%DIRNAME%

@rem Resolve any "." and ".." in APP_HOME to make it shorter.
for %%i in ("%APP_HOME%") do set APP_HOME=%%~fi

@rem ndk-build cannot reliably handle project paths containing spaces.
@rem Keep one physical project folder and build through a temporary drive mapping.
set "ALL_SUBST_CREATED="
set "ALL_SUBST_PUSHED="
if "%APP_HOME: =%"=="%APP_HOME%" goto afterSubst
if exist W:\ goto substUnavailable
subst W: "%APP_HOME%" >NUL 2>&1
if errorlevel 1 goto substUnavailable
set "ALL_SUBST_CREATED=1"
pushd W:\
if errorlevel 1 goto fail
set "ALL_SUBST_PUSHED=1"
set "APP_HOME=W:"
goto afterSubst

:substUnavailable
echo ERROR: Cannot create temporary W: mapping for "%APP_HOME%".
echo Close other builds or free W: before retrying. Existing drives are not changed.
goto fail

:afterSubst
@rem Add default JVM options here. You can also use JAVA_OPTS and GRADLE_OPTS to pass JVM options to this script.
set DEFAULT_JVM_OPTS="-Xmx64m" "-Xms64m"

@rem Find java.exe
if defined JAVA_HOME goto findJavaFromJavaHome

set JAVA_EXE=java.exe
%JAVA_EXE% -version >NUL 2>&1
if "%ERRORLEVEL%" == "0" goto execute

echo.
echo ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH.
echo.
echo Please set the JAVA_HOME variable in your environment to match the
echo location of your Java installation.

goto fail

:findJavaFromJavaHome
set JAVA_HOME=%JAVA_HOME:"=%
set JAVA_EXE=%JAVA_HOME%/bin/java.exe

if exist "%JAVA_EXE%" goto execute

echo.
echo ERROR: JAVA_HOME is set to an invalid directory: %JAVA_HOME%
echo.
echo Please set the JAVA_HOME variable in your environment to match the
echo location of your Java installation.

goto fail

:execute
@rem Setup the command line

set CLASSPATH=%APP_HOME%\gradle\wrapper\gradle-wrapper.jar


@rem Execute Gradle
"%JAVA_EXE%" %DEFAULT_JVM_OPTS% %JAVA_OPTS% %GRADLE_OPTS% "-Dorg.gradle.appname=%APP_BASE_NAME%" -classpath "%CLASSPATH%" org.gradle.wrapper.GradleWrapperMain %*
set "ALL_GRADLE_EXIT=%ERRORLEVEL%"
goto finish

:fail
set "ALL_GRADLE_EXIT=1"

:finish
@rem Return to the caller's directory before removing only our own mapping.
if defined ALL_SUBST_PUSHED popd
if not defined ALL_SUBST_CREATED goto afterSubstCleanup
subst W: /D >NUL 2>&1
if not errorlevel 1 goto afterSubstCleanup
echo ERROR: Failed to remove temporary W: mapping. Gradle exit code was %ALL_GRADLE_EXIT%.
set "ALL_GRADLE_EXIT=1"

:afterSubstCleanup
rem Set variable GRADLE_EXIT_CONSOLE if you need the _script_ return code instead of
rem the _cmd.exe /c_ return code!
if "%ALL_GRADLE_EXIT%"=="0" goto wrapperExit
if not "%GRADLE_EXIT_CONSOLE%"=="" goto consoleExit

:wrapperExit
endlocal & exit /b %ALL_GRADLE_EXIT%

:consoleExit
endlocal & exit %ALL_GRADLE_EXIT%
