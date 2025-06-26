#!/bin/bash

# PayMyBuddy Database Backup Script - Version Simplifiée
# Usage: ./backup-database.sh [environment] [description]

set -e

# Configuration par défaut
DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-5432}"
DB_NAME="${DB_NAME:-postgres}"
DB_USER="${DB_USER:-postgres}"
BACKUP_DIR="${BACKUP_DIR:-../backups}"
DATE=$(date +%Y%m%d_%H%M%S)

# Environnement (dev par défaut)
ENVIRONMENT=${1:-"dev"}
DESCRIPTION=${2:-"manual_backup"}

# Ajuster la base selon l'environnement
case $ENVIRONMENT in
    "prod")
        DB_NAME="paymybuddy_prod"
        echo "⚠️  ATTENTION: Sauvegarde de PRODUCTION"
        ;;
    "test")
        DB_NAME="paymybuddy_test"
        ;;
    "dev")
        DB_NAME="postgres"
        ;;
esac

# Créer le répertoire de sauvegarde
mkdir -p $BACKUP_DIR

# Nom du fichier de sauvegarde
BACKUP_FILE="$BACKUP_DIR/paymybuddy_${ENVIRONMENT}_${DATE}.sql"

echo "🚀 Démarrage de la sauvegarde PayMyBuddy"
echo "   Environnement: $ENVIRONMENT"
echo "   Base de données: ${DB_USER}@${DB_HOST}:${DB_PORT}/${DB_NAME}"
echo "   Fichier: $BACKUP_FILE"
echo "   Description: $DESCRIPTION"

# Vérifier que pg_dump est disponible
if ! command -v pg_dump &> /dev/null; then
    echo "❌ Erreur: pg_dump n'est pas installé. Installez PostgreSQL client."
    exit 1
fi

# Vérifier la variable d'environnement PGPASSWORD
if [ -z "$PGPASSWORD" ]; then
    echo "❌ Erreur: Variable PGPASSWORD non définie."
    echo "   Exécutez: export PGPASSWORD=votre_mot_de_passe"
    exit 1
fi

# Effectuer la sauvegarde
echo "📦 Création de la sauvegarde..."
pg_dump -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME \
    --verbose \
    --clean \
    --if-exists \
    --create \
    --format=plain \
    --file=$BACKUP_FILE

if [ $? -eq 0 ]; then
    # Compresser la sauvegarde
    echo "🗜️  Compression..."
    gzip $BACKUP_FILE

    COMPRESSED_FILE="${BACKUP_FILE}.gz"
    FILE_SIZE=$(du -h "$COMPRESSED_FILE" | cut -f1)

    echo "✅ Sauvegarde terminée avec succès!"
    echo "   Fichier: $COMPRESSED_FILE"
    echo "   Taille: $FILE_SIZE"

    # Créer un fichier de log
    echo "$(date): Sauvegarde $ENVIRONMENT - $DESCRIPTION - $FILE_SIZE" >> "$BACKUP_DIR/backup.log"

    # Nettoyer les anciennes sauvegardes (garder les 7 dernières)
    echo "🧹 Nettoyage des anciennes sauvegardes..."
    find $BACKUP_DIR -name "paymybuddy_${ENVIRONMENT}_*.sql.gz" -mtime +7 -delete 2>/dev/null || true

else
    echo "❌ Échec de la sauvegarde!"
    exit 1
fi
