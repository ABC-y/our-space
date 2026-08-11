#!/usr/bin/env sh
set -eu

script_dir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
project_dir=$(CDPATH= cd -- "$script_dir/.." && pwd)
backup_dir=${BACKUP_DIR:-"$project_dir/backups"}
timestamp=$(date +%Y%m%d-%H%M%S)

compose() {
  docker compose --env-file .env.production -f docker-compose.prod.yml "$@"
}

cd "$project_dir"
umask 077
mkdir -p "$backup_dir"

database_backup="$backup_dir/belong-us-mysql-$timestamp.sql.gz"
uploads_backup="$backup_dir/belong-us-uploads-$timestamp.tar.gz"

compose exec -T db sh -c 'exec mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" --single-transaction --routines --events belong_us' \
  | gzip > "$database_backup"
compose exec -T api tar -C /data -czf - uploads > "$uploads_backup"

sha256sum "$database_backup" "$uploads_backup" > "$backup_dir/belong-us-$timestamp.sha256"
find "$backup_dir" -type f -name 'belong-us-*' -mtime +30 -delete

printf 'Created backups:\n%s\n%s\n' "$database_backup" "$uploads_backup"
