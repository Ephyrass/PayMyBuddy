#!/bin/bash

# Usage: ./restore-database.sh <backup_file> [environment]

set -e

if [ $# -eq 0 ]; then
    echo "❌ Usage: $0 <backup_file> [environment]"
    echo "   Exemple: $0 ../backups/paymybuddy_dev_20250626_143000.sql.gz dev"
    exit 1
fi

BACKUP_FILE=$1
ENVIRONMENT=${2:-"dev"}

# Configuration par défaut
DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-5432}"
DB_NAME="${DB_NAME:-postgres}"
DB_USER="${DB_USER:-postgres}"
BACKUP_DIR="${BACKUP_DIR:-../backups}"

# Ajuster la base selon l'environnement
case $ENVIRONMENT in
    "prod")
        DB_NAME="paymybuddy_prod"
        echo "⚠️  ATTENTION: Restauration sur PRODUCTION!"
        read -p "   Êtes-vous sûr? (tapez 'OUI' pour confirmer): " confirm
        if [ "$confirm" != "OUI" ]; then
            echo "❌ Restauration annulée."
            exit 1
        fi
        ;;
    "test")
        DB_NAME="paymybuddy_test"
        ;;
    "dev")
        DB_NAME="postgres"
        ;;
esac

# Vérifier que le fichier de sauvegarde existe
if [ ! -f "$BACKUP_FILE" ]; then
    echo "❌ Erreur: Fichier de sauvegarde '$BACKUP_FILE' introuvable!"
    exit 1
fi

echo "🔄 Démarrage de la restauration PayMyBuddy"
echo "   Environnement: $ENVIRONMENT"
echo "   Base de données: ${DB_USER}@${DB_HOST}:${DB_PORT}/${DB_NAME}"
echo "   Fichier source: $BACKUP_FILE"

# Vérifier les outils nécessaires
if ! command -v psql &> /dev/null; then
    echo "❌ Erreur: psql n'est pas installé. Installez PostgreSQL client."
    exit 1
fi

# Vérifier la variable d'environnement PGPASSWORD
if [ -z "$PGPASSWORD" ]; then
    echo "❌ Erreur: Variable PGPASSWORD non définie."
    echo "   Exécutez: export PGPASSWORD=votre_mot_de_passe"
    exit 1
fi

# Créer une sauvegarde pré-restauration
echo "💾 Création d'une sauvegarde de sécurité..."
DATE=$(date +%Y%m%d_%H%M%S)
PRERESTORE_BACKUP="$BACKUP_DIR/pre_restore_${ENVIRONMENT}_${DATE}.sql"

pg_dump -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME > "$PRERESTORE_BACKUP"
gzip "$PRERESTORE_BACKUP"
echo "   Sauvegarde de sécurité: ${PRERESTORE_BACKUP}.gz"

# Restaurer depuis la sauvegarde
echo "📥 Restauration en cours..."
if [[ $BACKUP_FILE == *.gz ]]; then
    # Décompresser et restaurer
    gunzip -c "$BACKUP_FILE" | psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME
else
    # Restauration directe
    psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME < "$BACKUP_FILE"
fi

if [ $? -eq 0 ]; then
    echo "✅ Restauration terminée avec succès!"

    # Vérifier la restauration
    echo "🔍 Vérification de la restauration..."
    TABLE_COUNT=$(psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -t -c "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'public';" 2>/dev/null | xargs || echo "0")

    if [ "$TABLE_COUNT" -ge 4 ]; then
        echo "   ✅ Vérification réussie: $TABLE_COUNT tables trouvées"
    else
        echo "   ⚠️  Attention: Seulement $TABLE_COUNT tables trouvées (attendu: au moins 4)"
    fi

    # Log de la restauration
    echo "$(date): Restauration $ENVIRONMENT depuis $BACKUP_FILE - $TABLE_COUNT tables" >> "$BACKUP_DIR/restore.log"

else
    echo "❌ Échec de la restauration!"
    echo "   La sauvegarde de sécurité est disponible: ${PRERESTORE_BACKUP}.gz"
    exit 1
fi
