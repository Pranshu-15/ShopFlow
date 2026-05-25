-- Initialise per-service databases on a shared PostgreSQL instance for local dev.
-- The POSTGRES_USER set in docker-compose is already a superuser and owns these DBs.

CREATE DATABASE userservice_db;
CREATE DATABASE catalogservice_db;
CREATE DATABASE paymentservice_db;
CREATE DATABASE orderservice_db;
