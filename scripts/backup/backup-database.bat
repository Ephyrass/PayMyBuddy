@echo off
REM PayMyBuddy Database Backup Script - Version Windows
REM Usage: backup-database.bat [environment] [description]

setlocal enabledelayedexpansion

REM Configuration par défaut
set "DB_HOST=%DB_HOST%"
if "%DB_HOST%"=="" set "DB_HOST=localhost"
set "DB_PORT=%DB_PORT%"
if "%DB_PORT%"=="" set "DB_PORT=5432"
set "DB_NAME=%DB_NAME%"
if "%DB_NAME%"=="" set "DB_NAME=postgres"
set "DB_USER=%DB_USER%"
if "%DB_USER%"=="" set "DB_USER=postgres"
set "BACKUP_DIR=%BACKUP_DIR%"
if "%BACKUP_DIR%"=="" set "BACKUP_DIR=..\backups"

REM Date et heure pour le nom du fichier
for /f "tokens=2 delims==." %%I in ('wmic OS Get localdatetime /value') do set dt=%%I
set "DATE=!dt:~0,8!_!dt:~8,6!"

REM Environnement (dev par défaut)
set "ENVIRONMENT=%1"
if "%ENVIRONMENT%"=="" set "ENVIRONMENT=dev"
set "DESCRIPTION=%2"
if "%DESCRIPTION%"=="" set "DESCRIPTION=manual_backup"

REM Ajuster la base selon l'environnement
if "%ENVIRONMENT%"=="prod" set "DB_NAME=paymybuddy_prod"
if "%ENVIRONMENT%"=="test" set "DB_NAME=paymybuddy_test"
if "%ENVIRONMENT%"=="dev" set "DB_NAME=postgres"

REM Créer le répertoire de sauvegarde
if not exist "%BACKUP_DIR%" mkdir "%BACKUP_DIR%"

REM Nom du fichier de sauvegarde
set "BACKUP_FILE=%BACKUP_DIR%\paymybuddy_%ENVIRONMENT%_%DATE%.sql"

echo.
echo 🚀 Démarrage de la sauvegarde PayMyBuddy

echo    Environnement: %ENVIRONMENT%
echo    Base de données: %DB_USER%@%DB_HOST%:%DB_PORT%/%DB_NAME%
echo    Fichier: %BACKUP_FILE%
echo    Description: %DESCRIPTION%

echo.
REM Vérifier que pg_dump est disponible
where pg_dump >nul 2>&1
if errorlevel 1 (
    echo ❌ Erreur: pg_dump n'est pas installé. Installez PostgreSQL client.
    exit /b 1
)

REM Vérifier la variable d'environnement PGPASSWORD
if "%PGPASSWORD%"=="" (
    echo ❌ Erreur: Variable PGPASSWORD non définie.
    echo    Définissez-la avec : set PGPASSWORD=motdepasse
    exit /b 1
)

echo 📦 Création de la sauvegarde...
pg_dump -h %DB_HOST% -p %DB_PORT% -U %DB_USER% -d %DB_NAME% --verbose --clean --if-exists --create --format=plain --file="%BACKUP_FILE%"
if errorlevel 1 (
    echo ❌ Échec de la sauvegarde!
    exit /b 1
)

REM Compression avec 7zip si disponible
where 7z >nul 2>&1
if errorlevel 0 (
    echo 🗜️  Compression avec 7zip...
    7z a "%BACKUP_FILE%.7z" "%BACKUP_FILE%" >nul
    del "%BACKUP_FILE%"
    set "COMPRESSED_FILE=%BACKUP_FILE%.7z"
) else (
    set "COMPRESSED_FILE=%BACKUP_FILE%"
)

REM Taille du fichier
for %%A in ("%COMPRESSED_FILE%") do set SIZE=%%~zA

echo ✅ Sauvegarde terminée avec succès!
echo    Fichier: %COMPRESSED_FILE%
echo    Taille: %SIZE% octets

echo %DATE%: Sauvegarde %ENVIRONMENT% - %DESCRIPTION% - %SIZE% octets >> "%BACKUP_DIR%\backup.log"

REM Nettoyer les anciennes sauvegardes (garder 7 jours)
forfiles /p "%BACKUP_DIR%" /m "paymybuddy_%ENVIRONMENT%_*.sql*" /d -7 /c "cmd /c del @path"

endlocal

