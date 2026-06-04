@REM ----------------------------------------------------------------------------
@REM Maven Wrapper startup batch script.
@REM ----------------------------------------------------------------------------
@echo off
setlocal
set MAVEN_PROJECTBASEDIR=%~dp0
set WRAPPER_JAR=%MAVEN_PROJECTBASEDIR%.mvn\wrapper\maven-wrapper.jar
if not exist "%WRAPPER_JAR%" (
  echo Maven Wrapper jar is missing: %WRAPPER_JAR%
  echo Please run mvnw from a Unix-like shell first or regenerate the Maven Wrapper.
  exit /b 1
)
if "%JAVA_HOME%"=="" (
  set JAVACMD=java
) else (
  set JAVACMD=%JAVA_HOME%\bin\java.exe
)
"%JAVACMD%" -Dmaven.multiModuleProjectDirectory="%MAVEN_PROJECTBASEDIR%" -classpath "%WRAPPER_JAR%" org.apache.maven.wrapper.MavenWrapperMain %*
endlocal
