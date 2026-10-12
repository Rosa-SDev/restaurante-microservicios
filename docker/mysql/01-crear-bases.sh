#!/bin/bash
# Crea una base de datos y un usuario por microservicio.
# La imagen oficial de MySQL lo ejecuta una sola vez: cuando el volumen está vacío.
# Las contraseñas llegan como variables de entorno desde el archivo .env.
set -e

mysql --protocol=socket -uroot -p"${MYSQL_ROOT_PASSWORD}" <<EOSQL
CREATE DATABASE IF NOT EXISTS usuariosdb CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE DATABASE IF NOT EXISTS cartadb    CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE DATABASE IF NOT EXISTS clientesdb CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE DATABASE IF NOT EXISTS salondb    CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

CREATE USER IF NOT EXISTS 'usuarios'@'%' IDENTIFIED BY '${USUARIOS_DB_PASSWORD}';
CREATE USER IF NOT EXISTS 'carta'@'%'    IDENTIFIED BY '${CARTA_DB_PASSWORD}';
CREATE USER IF NOT EXISTS 'clientes'@'%' IDENTIFIED BY '${CLIENTES_DB_PASSWORD}';
CREATE USER IF NOT EXISTS 'salon'@'%'    IDENTIFIED BY '${SALON_DB_PASSWORD}';

GRANT ALL PRIVILEGES ON usuariosdb.* TO 'usuarios'@'%';
GRANT ALL PRIVILEGES ON cartadb.*    TO 'carta'@'%';
GRANT ALL PRIVILEGES ON clientesdb.* TO 'clientes'@'%';
GRANT ALL PRIVILEGES ON salondb.*    TO 'salon'@'%';

FLUSH PRIVILEGES;
EOSQL
