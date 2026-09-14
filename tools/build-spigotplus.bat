@echo off
setlocal EnableExtensions EnableDelayedExpansion

title SpigotPlus - Single JAR Build

set "ROOT=%~dp0.."
set "TOOLS=%ROOT%\tools"
set "BUILD=%ROOT%\build\spigot"
set "DIST=%ROOT%\dist"
set "BUILDTOOLS=%TOOLS%\BuildTools.jar"
set "BASE=%BUILD%\SpigotBase.jar"
set "FINAL=%DIST%\SpigotPlus.jar"

cd /d "%ROOT%"

echo ==========================================
echo        SPIGOTPLUS - SINGLE JAR BUILD
echo ==========================================
echo.

where java >nul 2>&1
if errorlevel 1 (
    echo [ERRO] Java nao encontrado no PATH.
    exit /b 1
)

where mvn >nul 2>&1
if errorlevel 1 (
    echo [ERRO] Maven nao encontrado no PATH.
    exit /b 1
)

where git >nul 2>&1
if errorlevel 1 (
    echo [ERRO] Git nao encontrado no PATH.
    exit /b 1
)

if not exist "%BUILDTOOLS%" (
    echo [1/5] Baixando BuildTools oficial mais recente...
    curl.exe -fL --retry 3 -o "%BUILDTOOLS%" "https://hub.spigotmc.org/jenkins/job/BuildTools/lastSuccessfulBuild/artifact/target/BuildTools.jar"
    if errorlevel 1 (
        echo [ERRO] Falha ao baixar o BuildTools.
        exit /b 1
    )
) else (
    echo [1/5] BuildTools local encontrado.
)

echo.
echo [2/5] Compilando a camada SpigotPlus...
call mvn -B clean package
if errorlevel 1 (
    echo [ERRO] A compilacao do SpigotPlus falhou.
    exit /b 1
)

if not exist "%ROOT%\target\SpigotPlus.jar" (
    echo [ERRO] target\SpigotPlus.jar nao foi gerado.
    exit /b 1
)

echo.
echo [3/5] Gerando Spigot 26.2 com BuildTools...
if exist "%BUILD%" rmdir /s /q "%BUILD%"
mkdir "%BUILD%"

java -jar "%BUILDTOOLS%" --rev 26.2 --output-dir "%BUILD%" --final-name SpigotBase.jar
if errorlevel 1 (
    echo [ERRO] BuildTools falhou ao gerar o Spigot 26.2.
    exit /b 1
)

if not exist "%BASE%" (
    echo [ERRO] SpigotBase.jar nao foi gerado.
    exit /b 1
)

echo.
echo [4/5] Montando o unico JAR do servidor...
if exist "%DIST%" rmdir /s /q "%DIST%"
mkdir "%DIST%"
copy /y "%BASE%" "%FINAL%" >nul

rem O JAR do Spigot ja contem o servidor completo. Aqui apenas adicionamos
rem as classes/recursos do SpigotPlus e trocamos o Main-Class pelo bootstrap.
jar --update --file "%FINAL%" --manifest "%TOOLS%\spigotplus-manifest.mf" -C "%ROOT%\target\classes" .
if errorlevel 1 (
    echo [ERRO] Falha ao incorporar o SpigotPlus no JAR final.
    exit /b 1
)

echo.
echo [5/5] Validando o JAR final...
jar --list --file "%FINAL%" | findstr /c:"org/bukkit/craftbukkit/Main.class" >nul
if errorlevel 1 (
    echo [ERRO] O servidor Spigot nao esta presente no JAR final.
    exit /b 1
)

jar --list --file "%FINAL%" | findstr /c:"com/donnie1337/spigotplus/bootstrap/SpigotPlusBootstrap.class" >nul
if errorlevel 1 (
    echo [ERRO] O bootstrap do SpigotPlus nao esta presente no JAR final.
    exit /b 1
)

echo.
echo ==========================================
echo BUILD CONCLUIDO
 echo ==========================================
echo.
echo JAR final:
echo %FINAL%
echo.
echo Runtime esperado:
echo   java -jar SpigotPlus.jar nogui
echo.
echo Nao e necessario spigot.jar separado.
echo.
exit /b 0
