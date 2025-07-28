set -e

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    CREATE USER gis_alpha_test_app WITH PASSWORD 'dev1';
    GRANT ALL PRIVILEGES ON DATABASE $POSTGRES_DB TO gis_alpha_test_app;
EOSQL
