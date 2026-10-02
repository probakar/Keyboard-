@rem
@rem CustomBoard Gradle startup script for Windows.
@rem
@rem Self-bootstrapping wrapper: downloads the official gradle-wrapper.jar on first use.
@rem

@if "%DEBUG%"=="" @echo off
setlocal

set DIRNAME=%~dp0
if "%DIRNAME%"=="" set DIRNAME=.
set APP_HOME=%DIRNAME%
set APP_BASE_NAME=%~n0

set WRAPPER_JAR=%APP_HOME%gradle\wrapper\gradle-wrapper.jar
set WRAPPER_PROPERTIES=%APP_HOME%gradle\wrapper\gradle-wrapper.properties

if exist "%WRAPPER_JAR%" goto findJava

echo Bootstrapping Gradle wrapper...
for /f "tokens=2 delims=-" %%a in ('findstr distributionUrl "%WRAPPER_PROPERTIES%"') do set GRADLE_VERSION=%%a
if "%GRADLE_VERSION%"=="" set GRADLE_VERSION=8.7
powershell -NoProfile -Command "try { Invoke-WebRequest -Uri 'https://raw.githubusercontent.com/gradle/gradle/v%GRADLE_VERSION%.0/gradle/wrapper/gradle-wrapper.jar' -OutFile '%WRAPPER_JAR%' } catch { Invoke-WebRequest -Uri 'https://raw.githubusercontent.com/gradle/gradle/master/gradle/wrapper/gradle-wrapper.jar' -OutFile '%WRAPPER_JAR%' }"
if not exist "%WRAPPER_JAR%" goto failDownload

:findJava
if defined JAVA_HOME goto findJavaFromJavaHome
set JAVA_EXE=java.exe
%JAVA_EXE% -version >NUL 2>&1
if %ERRORLEVEL% equ 0 goto execute
goto failJava

:findJavaFromJavaHome
set JAVA_HOME=%JAVA_HOME:"=%
set JAVA_EXE=%JAVA_HOME%/bin/java.exe
if exist "%JAVA_EXE%" goto execute
goto failJava

:execute
set DEFAULT_JVM_OPTS="-Xmx64m" "-Xms64m"
"%JAVA_EXE%" %DEFAULT_JVM_OPTS% %JAVA_OPTS% %GRADLE_OPTS% "-Dorg.gradle.appname=%APP_BASE_NAME%" -classpath "%WRAPPER_JAR%" org.gradle.wrapper.GradleWrapperMain %*
goto end

:failDownload
echo. 1>&2
echo ERROR: Could not download gradle-wrapper.jar. Run "gradle wrapper" once instead. 1>&2
exit /b 1

:failJava
echo. 1>&2
echo ERROR: JAVA_HOME is not set and no 'java' command could be found. Install JDK 17. 1>&2
exit /b 1

:end
endlocal
