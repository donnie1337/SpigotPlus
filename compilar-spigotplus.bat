@echo off
setlocal EnableExtensions

title SpigotPlus - Compilar 26.2

cd /d "%~dp0"

echo ==========================================
echo        SPIGOTPLUS - COMPILAR
 echo            SPIGOT 26.2
 echo ==========================================
echo.

echo Diretorio do repositorio:
echo %CD%
echo.

echo Este script chama o build completo em tools\build-spigotplus.bat.
echo BuildTools sera usado somente em C:\SpigotPlusBuild.
echo Nenhum spigot.jar separado sera necessario no servidor.
echo.

if not exist "tools\build-spigotplus.bat" (
    echo [ERRO] tools\build-spigotplus.bat nao foi encontrado.
    pause
    exit /b 1
)

call "tools\build-spigotplus.bat"
set "BUILD_EXIT=%ERRORLEVEL%"

echo.
if not "%BUILD_EXIT%"=="0" (
    echo ==========================================
    echo FALHA NA COMPILACAO
    echo ==========================================
    echo.
    echo Verifique a mensagem de erro acima.
    pause
    exit /b %BUILD_EXIT%
)

echo ==========================================
echo COMPILACAO CONCLUIDA COM SUCESSO
 echo ==========================================
echo.
echo O JAR final fica em:
echo C:\SpigotPlusBuild\dist\SpigotPlus.jar
echo.
echo Para executar:
echo   java -jar SpigotPlus.jar nogui
echo.
pause
exit /b 0
