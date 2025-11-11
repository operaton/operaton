@echo off

IF [%~1]==[--version] GOTO Version

SET "BASEDIR=%~dp0"
SET "JBOSS_HOME=%BASEDIR%\server\wildfly-${version.wildfly}"
SET "EXECUTABLE=%BASEDIR%server\wildfly-${version.wildfly}\bin\standalone.bat"

echo "starting Operaton ${project.version} on Wildfly Application Server ${version.wildfly}"

start %EXECUTABLE%

ping -n 5 localhost > NULL
start http://localhost:8080/operaton-welcome/index.html

GOTO Done

:Version
ECHO Operaton: ${project.version} (Wildfly ${version.wildfly})

:Done
