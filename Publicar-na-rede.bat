@echo off
rem Publica o Helplixo em G:\Suporte\Suporte\Helplixo (guarda a versao anterior).
rem Use "Publicar-na-rede.bat -Simular" para so conferir, sem alterar nada.
cd /d "%~dp0"
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\publicar-na-rede.ps1" %*
pause
