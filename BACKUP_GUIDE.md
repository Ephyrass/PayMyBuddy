# Guide de Sauvegarde PayMyBuddy - Scripts Shell Uniquement

## Vue d'ensemble
Système de sauvegarde simple avec scripts shell pour la base de données PostgreSQL de PayMyBuddy.

## Fichiers disponibles
```
scripts/backup/
├── backup-database.sh      # Script de sauvegarde (Linux/Mac)
├── restore-database.sh     # Script de restauration (Linux/Mac)
├── backup-database.bat     # Script de sauvegarde (Windows)
└── restore-database.bat    # Script de restauration (Windows)
```

## Configuration initiale

### 1. Définir le mot de passe PostgreSQL
```bash
# Variable d'environnement obligatoire
export PGPASSWORD=password # Remplacez 'password' par votre mot de passe PostgreSQL
```

### 2. Variables optionnelles
```bash
export DB_HOST=localhost        # Serveur de base de données
export DB_PORT=5432            # Port PostgreSQL
export DB_USER=postgres        # Utilisateur PostgreSQL
export BACKUP_DIR=../backups   # Répertoire des sauvegardes
```

## Utilisation

### Sauvegarde
```bash
# Sauvegarde de développement
./scripts/backup/backup-database.sh dev

# Sauvegarde avec description
./scripts/backup/backup-database.sh dev "avant_mise_a_jour"

# Sauvegarde de production (avec confirmation)
./scripts/backup/backup-database.sh prod
```

### Restauration
```bash
# Lister les sauvegardes disponibles
ls -la backups/

# Restaurer en développement
./scripts/backup/restore-database.sh backups/paymybuddy_dev_20250626_143000.sql.gz dev

# Restaurer en production (avec confirmation obligatoire)
./scripts/backup/restore-database.sh backups/paymybuddy_prod_20250626_000000.sql.gz prod
```

## Fonctionnalités

### Scripts de sauvegarde
- ✅ **Compression automatique** (.gz)
- ✅ **Nettoyage automatique** (garde 7 dernières sauvegardes)
- ✅ **Logs détaillés**
- ✅ **Vérification des outils** (pg_dump)
- ✅ **Support multi-environnements** (dev, test, prod)

### Scripts de restauration
- ✅ **Sauvegarde pré-restauration** automatique
- ✅ **Confirmation obligatoire** pour la production
- ✅ **Vérification post-restauration**
- ✅ **Support fichiers compressés**
- ✅ **Logs des restaurations**

## Environnements supportés

| Environnement | Base de données | Confirmation |
|---------------|----------------|--------------|
| `dev`         | postgres       | Non          |
| `test`        | paymybuddy_test| Non          |
| `prod`        | paymybuddy_prod| Oui (tapez "OUI") |

## Exemples pratiques

```bash
# Configuration rapide
export PGPASSWORD=password

# Sauvegarde quotidienne
./scripts/backup/backup-database.sh dev "sauvegarde_quotidienne"

# Sauvegarde avant déploiement
./scripts/backup/backup-database.sh prod "avant_deploy_v2.1"

# Restauration d'urgence
./scripts/backup/restore-database.sh backups/paymybuddy_prod_20250625_235959.sql.gz prod
```

## Sécurité
- **Confirmation obligatoire** pour les opérations de production
- **Sauvegarde automatique** avant toute restauration
- **Variables d'environnement** pour les mots de passe
- **Vérification** de l'intégrité après restauration

