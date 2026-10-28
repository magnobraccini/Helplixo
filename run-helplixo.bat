@echo off
setlocal
cd /d "%~dp0"
mvn exec:java
endlocal
