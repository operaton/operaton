@echo off

SET BASEDIR=%~dp0
SET EXECUTABLE=%BASEDIR%internal\run.bat

REM stop Operaton Run
call "%EXECUTABLE%" stop