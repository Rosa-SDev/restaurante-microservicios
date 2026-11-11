#!/usr/bin/env bash
# Genera el par de claves RSA con el que usuarios-service firma el JWT (RS256)
# y con el que el gateway lo verifica.
#
#   claves/privada.pem  PKCS#8 (-----BEGIN PRIVATE KEY-----)   solo usuarios-service
#   claves/publica.pem  X.509  (-----BEGIN PUBLIC KEY-----)    usuarios-service y api-gateway
#
# Uso (Git Bash, Mac o Linux), desde cualquier carpeta:
#   bash scripts/generar-claves-rsa.sh
#
# La carpeta claves/ está en el .gitignore y en el .dockerignore: nunca se sube
# al repositorio ni entra a una imagen.
set -euo pipefail

# Se ubica en la raíz del repositorio: la carpeta superior a la del script.
cd "$(dirname "${BASH_SOURCE[0]}")/.."

CARPETA="claves"
PRIVADA="$CARPETA/privada.pem"
PUBLICA="$CARPETA/publica.pem"

# Regenerar la clave invalida todos los tokens emitidos y obliga a recrear el
# Secret de Kubernetes, así que nunca se sobrescribe: para regenerarla hay que
# borrar la carpeta claves/ a mano.
if [ -f "$PRIVADA" ]; then
	echo "La clave $PRIVADA ya existe: no se regenera."
	echo "Si de verdad necesitas un par nuevo, borra la carpeta $CARPETA/ a mano y vuelve a ejecutar el script."
	exit 0
fi

if ! command -v openssl > /dev/null 2>&1; then
	echo "Falta openssl: ejecuta este script en Git Bash."
	exit 1
fi

mkdir -p "$CARPETA"

# genpkey escribe la privada en PKCS#8, el formato que Java lee con
# PKCS8EncodedKeySpec. pkey -pubout extrae la pública en X.509.
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out "$PRIVADA"
openssl pkey -in "$PRIVADA" -pubout -out "$PUBLICA"

echo "Claves RSA generadas:"
echo "  $PRIVADA  (privada, PKCS#8)"
echo "  $PUBLICA  (pública, X.509)"
echo "Recuerda: estas claves NUNCA se suben al repositorio ni se copian a una imagen."
