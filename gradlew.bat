@if "%DEBUG%" == "" @echo off
@rem Gradle wrapper startup script for Windows

set DIRNAME=%~dp0
if "%DIRNAME%" == "" set DIRNAME=.
set APP_BASE_NAME=%~n0
set APP_HOME=%DIRNAME%

set JAVA_HOME=C:\Users\Administrator\Android\jdk17\jdk-17.0.19+10

set GRADLE_OPTS=-Dorg.gradle.appname=%APP_BASE_NAME% -Dhttps.proxyHost=127.0.0.1 -Dhttps.proxyPort=7890

"%JAVA_HOME%\bin\java.exe" %GRADLE_OPTS% -classpath "%APP_HOME%\gradle\wrapper\gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain %*
