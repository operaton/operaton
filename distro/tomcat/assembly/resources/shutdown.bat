@echo off

set "CATALINA_HOME=%CD%\server\apache-tomcat-${version.tomcat}"
SET "BASEDIR=%~dp0"
SET "EXECUTABLE=%BASEDIR%server\apache-tomcat-${version.tomcat}\bin\shutdown.bat"

start %EXECUTABLE%
