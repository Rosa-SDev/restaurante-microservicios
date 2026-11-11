# restaurante-microservicios

Sistema de Gestión de Restaurante expuesto como microservicios REST con Spring Boot.
El proyecto es un repositorio Maven multimódulo: un `pom.xml` padre que coordina
seis aplicaciones Spring Boot independientes, cada una con su propio `main`, su
propio puerto y su propia base de datos. La autorización de los pagos la resuelve
**Keycloak**, un servidor OAuth 2.0 que corre con su imagen oficial.

- [1. El sistema y su arquitectura](#1-el-sistema-y-su-arquitectura)
- [2. Cómo ejecutarlo desde un clon limpio](#2-cómo-ejecutarlo-desde-un-clon-limpio)
- [3. Seguridad](#3-seguridad)
- [4. Variables de entorno](#4-variables-de-entorno)
- [5. Decisiones técnicas](#5-decisiones-técnicas)
- [6. Colecciones de Postman](#6-colecciones-de-postman)
- [7. Referencias](#7-referencias)

---

## 1. El sistema y su arquitectura

Cinco microservicios de negocio, un gateway que es la única puerta de entrada a
la API, un MySQL con una base de datos por servicio y Keycloak como servidor de
autorización OAuth 2.0.

```
                  Postman / curl
                        ├──────────────────────────────────────┐
    Bearer <token>      │               pide tokens OAuth 2.0  │
                        ▼                                      ▼
  8080 (Compose) · 30080 (Kubernetes)             30180 (Compose y Kubernetes)
  ┌──────────────────────────────────────────┐      ┌──────────────────────┐
  │            api-gateway :8080             │ JWKS │    keycloak :8080    │
  │ cadena 1: /api/pagos/** → token Keycloak │◄─────│  realm restaurante   │
  │ cadena 2: el resto → token de usuarios   │      │  (OAuth 2.0, RS256)  │
  └──┬─────────┬─────────┬─────────┬────────┬┘      └───────────▲──────────┘
     │         │         │         │        │                   │ JWKS
┌────▼───┐┌────▼───┐┌────▼───┐┌────▼───┐┌───▼──────────┐        │
│usuarios││ carta  ││clientes││ salon  ││ pagos :8085  │────────┘
│ :8081  ││ :8082  ││ :8083  ││ :8084  ││ valida token │
│ firma  ││        ││        ││        ││ y scopes     │
│ RS256  ││        ││        ││        ││              │
└────┬───┘└────┬───┘└────┬───┘└────┬───┘└──────┬───────┘
     └─────────┴─────────┼─────────┴───────────┘
┌────────────────────────▼──────────────────────────┐
│             mysql :3306  (MySQL 8.4)              │
│ usuariosdb  cartadb  clientesdb  salondb  pagosdb │
│           un usuario de MySQL por base            │
└────────────────────────┬──────────────────────────┘
                         │
                volumen persistente
```

**Dos emisores de token, dos claves públicas.** El sistema acepta dos tipos de
token y cada ruta confía en uno solo:

| Token | Lo emite | Firma con | Lo verifica | Sirve para |
|---|---|---|---|---|
| Token de sesión | `usuarios-service` (`POST /api/auth/login`) | La clave privada `claves/privada.pem` | El gateway, con `claves/publica.pem` | Todo menos `/api/pagos/**` |
| *Access token* OAuth 2.0 | Keycloak (realm `restaurante`) | La clave privada interna de Keycloak | El gateway **y** `pagos-service`, con el JWKS de Keycloak | Solo `/api/pagos/**` |

### Módulos

| Módulo | Puerto | Base de datos | Dominio | Imagen |
|---|---|---|---|---|
| `api-gateway` | 8080 | — | Enruta las peticiones hacia los cinco servicios y valida el token | `restaurante/api-gateway:1.0` |
| `usuarios-service` | 8081 | `usuariosdb` | Usuario, roles y autenticación; emite el token de sesión | `restaurante/usuarios-service:1.0` |
| `carta-service` | 8082 | `cartadb` | Platillo | `restaurante/carta-service:1.0` |
| `clientes-service` | 8083 | `clientesdb` | Cliente, Reserva | `restaurante/clientes-service:1.0` |
| `salon-service` | 8084 | `salondb` | Mesa, Pedido, Factura | `restaurante/salon-service:1.0` |
| `pagos-service` | 8085 | `pagosdb` | Pago de una factura; valida por su cuenta el token de Keycloak | `restaurante/pagos-service:1.0` |
| `keycloak` | 8080 interno, publicado en **30180** | — (modo de desarrollo) | Servidor OAuth 2.0: realm, clientes, scopes y usuario de demostración | `quay.io/keycloak/keycloak:26.4.0` (oficial) |

**Keycloak no tiene Dockerfile** porque no es un microservicio propio: es una
herramienta que se usa con su imagen oficial, igual que MySQL. Su configuración
completa vive en `keycloak/restaurante-realm.json`, que se importa al arrancar.

La etiqueta de nuestras imágenes es `1.0`, nunca `latest`: con `latest`,
Kubernetes intenta descargar la imagen de Internet aunque ya esté construida en
el equipo.

### Rutas

El cliente HTTP solo necesita conocer la dirección del gateway: él reenvía cada
ruta al servicio que le corresponde. Los tokens de Keycloak se piden directamente
a Keycloak, en el puerto 30180.

| Prefijo de ruta | Servicio |
|---|---|
| `/api/usuarios/**`, `/api/auth/**` | `usuarios-service` |
| `/api/platillos/**` | `carta-service` |
| `/api/clientes/**`, `/api/reservas/**` | `clientes-service` |
| `/api/mesas/**`, `/api/pedidos/**`, `/api/facturas/**` | `salon-service` |
| `/api/pagos/**` | `pagos-service` |

### Endpoints de `pagos-service`

| Verbo | Ruta | Scope necesario | Qué hace | Éxito | Errores |
|---|---|---|---|---|---|
| POST | `/api/pagos` | `pagos:escribir` | Registra un pago en estado `PENDIENTE` | 201 | 400 · 409 si la factura ya tiene un pago `APROBADO` |
| GET | `/api/pagos` | `pagos:leer` | Lista los pagos; filtros `?estado=` y `?facturaId=` | 200 | — |
| GET | `/api/pagos/{id}` | `pagos:leer` | Consulta un pago | 200 | 404 |
| PUT | `/api/pagos/{id}/estado` | `pagos:escribir` | Cambia el estado a `APROBADO` o `RECHAZADO` | 200 | 400 · 404 · 409 si la transición no está permitida |
| DELETE | `/api/pagos/{id}` | `pagos:escribir` | Elimina un pago que no esté `APROBADO` | 204 | 404 · 409 si está aprobado |

Cuerpo para registrar un pago:

```json
{ "facturaId": 1, "monto": 58000.00, "metodo": "TARJETA" }
```

Reglas de negocio:

- Un pago nuevo siempre empieza en `PENDIENTE`, y solo puede pasar de ahí a
  `APROBADO` o `RECHAZADO`.
- El servidor genera la `referencia` (un UUID) y toma `registradoPor` del token
  (`preferred_username` o, si no está, `azp`), nunca del cuerpo de la petición.
- Una factura no puede tener dos pagos `APROBADO`, y un pago aprobado no se elimina.
- `pagos-service` no consulta a `salon-service` para comprobar que la factura
  existe: los servicios no se llaman entre sí, y esa es una limitación conocida.

### Estructura de un servicio

```
carta-service/
├── Dockerfile                          imagen multietapa del módulo
└── src/main/
    ├── java/com/restaurante/carta/
    │   ├── CartaApplication.java       punto de entrada
    │   ├── controller/                 traduce HTTP
    │   ├── service/                    reglas de negocio
    │   ├── repository/                 acceso a datos
    │   ├── model/                      entidades JPA
    │   └── dto/                        lo que viaja en el JSON
    └── resources/
        ├── application.yml             puerto y base de datos H2
        └── application-docker.yml      lo que cambia en contenedores
```

---

## 2. Cómo ejecutarlo desde un clon limpio

Hay tres formas de levantar el sistema. En las tres, la API se consume siempre a
través del gateway, y Keycloak responde en el mismo puerto 30180.

| Modo | Base de datos | Gateway | Keycloak |
|---|---|---|---|
| Local | H2 en memoria | `http://localhost:8080` | `http://localhost:30180` (contenedor de Compose) |
| Docker Compose | MySQL en contenedor | `http://localhost:8080` | `http://localhost:30180` |
| Kubernetes | MySQL con volumen persistente | `http://localhost:30080` | `http://localhost:30180` |

Los tres modos necesitan antes las claves RSA y el archivo `.env`.

### 2.1 Generar las claves RSA (una sola vez)

`usuarios-service` firma el token con una clave privada y el gateway lo verifica
con la pública. Ese par **no está en el repositorio**: cada quien genera el suyo
en su equipo. En **Git Bash** (trae OpenSSL), desde la raíz del repositorio:

```bash
bash scripts/generar-claves-rsa.sh
```

El script crea la carpeta `claves/` con dos archivos:

| Archivo | Formato | Lo usa |
|---|---|---|
| `claves/privada.pem` | PKCS#8, RSA de 2048 bits | Solo `usuarios-service` |
| `claves/publica.pem` | X.509 | `usuarios-service` y `api-gateway` |

`claves/` está en el `.gitignore` y en el `.dockerignore`: nunca se sube ni entra
a una imagen. Si las claves ya existen, el script no las sobrescribe, porque un par
nuevo invalida todos los tokens emitidos. Para regenerarlas hay que borrar la
carpeta a mano.

Las pruebas no dependen de esta carpeta: generan su propio par en memoria, así
que `./mvnw clean install` pasa también en un clon sin claves.

### 2.2 Crear el archivo `.env`

Copiar el archivo de ejemplo y poner valores propios en la copia. `.env` no se
sube al repositorio.

```bash
cp .env.example .env
```

En Windows:

```powershell
Copy-Item .env.example .env
```

Además de las contraseñas de MySQL, el `.env` lleva la del administrador de
Keycloak y los secretos de los dos clientes OAuth (`PAGOS_CLIENTE_SECRET` y
`PAGOS_CONSULTA_SECRET`, de al menos 32 caracteres). Para generar valores
aleatorios en Git Bash:

```bash
openssl rand -hex 24
```

### 2.3 Local (IntelliJ o el wrapper de Maven)

Requisitos: **JDK 21**, y Docker Desktop solo para Keycloak. No hace falta
instalar Maven, el repositorio trae el wrapper. Este modo usa el perfil por
omisión, que levanta una base **H2 en memoria** en cada servicio: no requiere
MySQL y los datos se pierden al apagar.

Compilar los seis módulos desde la raíz:

```bash
./mvnw clean install
```

En Windows:

```powershell
.\mvnw.cmd clean install
```

Levantar solo Keycloak, que lee los secretos del `.env`:

```bash
docker compose up -d keycloak
```

Cada servicio se ejecuta por separado, en su propia terminal:

```bash
./mvnw spring-boot:run -pl usuarios-service
./mvnw spring-boot:run -pl carta-service
./mvnw spring-boot:run -pl clientes-service
./mvnw spring-boot:run -pl salon-service
./mvnw spring-boot:run -pl pagos-service
./mvnw spring-boot:run -pl api-gateway
```

En este modo, `usuarios-service` y el gateway leen las claves de
`../claves/privada.pem` y `../claves/publica.pem`, una ruta relativa a la carpeta
de cada módulo. Desde IntelliJ IDEA es equivalente: abrir el `pom.xml` de la raíz
para que importe los seis módulos y ejecutar la clase `main` de cada uno sin
indicar ningún perfil, con la carpeta del módulo como directorio de trabajo.

Cada servicio expone además la consola de H2 en su propio puerto, por ejemplo
`http://localhost:8082/h2-console` para la carta.

### 2.4 Docker Compose

Requisitos: **Docker Desktop** en marcha. No hace falta JDK: las imágenes se
compilan dentro de Docker.

Con las claves y el `.env` listos, construir las imágenes y levantar los ocho
contenedores (MySQL, Keycloak, los cinco servicios y el gateway):

```bash
docker compose up --build
```

La primera vez tarda varios minutos, porque cada imagen descarga sus
dependencias de Maven. Quedan publicados dos puertos:

```
http://localhost:8080     gateway
http://localhost:30180    Keycloak
```

Las claves no se copian a las imágenes: Compose monta `./claves` como volumen de
solo lectura en `usuarios-service`, y únicamente `publica.pem` en el gateway.
Keycloak importa el realm desde `./keycloak` cada vez que arranca.

Para comprobar que Keycloak ya cargó el realm, abrir en el navegador:

```
http://localhost:30180/realms/restaurante/.well-known/openid-configuration
```

Comprobar el estado y leer los registros de un servicio:

```bash
docker compose ps
docker compose logs -f api-gateway
```

Para apagar todo y **borrar también el volumen de MySQL**, de modo que el
próximo arranque parta de bases vacías:

```bash
docker compose down -v
```

Sin `-v`, los datos de MySQL sobreviven al apagado. Keycloak no guarda nada: cada
arranque vuelve a dejar el mismo realm.

### 2.5 Kubernetes en Docker Desktop

Requisitos: Docker Desktop con Kubernetes activado (*Settings → Kubernetes →
Enable Kubernetes*), y las claves y el `.env` ya creados como en los pasos 2.1
y 2.2.

**Paso 1: construir las seis imágenes en el equipo.** Los manifiestos usan
`imagePullPolicy: Never`, es decir, Kubernetes no intenta descargar nada de
Internet y solo usa las imágenes locales. Si no existen, los Pods se quedan en
`ErrImageNeverPull`. La imagen de Keycloak sí se descarga, porque es la oficial.

```bash
docker compose build
```

**Paso 2: crear el namespace, los dos Secrets y los dos ConfigMaps.** Ningún
secreto está escrito en los manifiestos: todo se genera por comando a partir de
archivos locales que no se suben.

```bash
kubectl apply -f k8s/00-namespace.yaml
kubectl create secret generic restaurante-secretos --from-env-file=.env -n restaurante
kubectl create secret generic restaurante-claves --from-file=claves/ -n restaurante
kubectl create configmap mysql-inicializacion --from-file=docker/mysql/ -n restaurante
kubectl create configmap keycloak-realm --from-file=keycloak/restaurante-realm.json -n restaurante
```

| Recurso | Sale de | Para qué |
|---|---|---|
| Secret `restaurante-secretos` | `.env` | Contraseñas de MySQL, del administrador de Keycloak y secretos de los clientes OAuth |
| Secret `restaurante-claves` | `claves/` | Par RSA, montado en `/claves`; el gateway monta solo `publica.pem` |
| ConfigMap `mysql-inicializacion` | `docker/mysql/` | Script que crea las cinco bases y sus usuarios |
| ConfigMap `keycloak-realm` | `keycloak/restaurante-realm.json` | Realm que Keycloak importa al arrancar |

**Paso 3: desplegar todo y esperar a que los Pods estén listos.**

```bash
kubectl apply -f k8s/
kubectl get pods -n restaurante -w
```

Es normal que algunos servicios se reinicien una o dos veces al arrancar,
mientras MySQL termina de levantarse: Kubernetes los recupera solo. El estado
final es de **ocho Pods en `1/1 Running`**.

Dos Services son de tipo NodePort, así que las direcciones base son:

```
http://localhost:30080    gateway   (NodePort 8080:30080)
http://localhost:30180    Keycloak  (NodePort 8080:30180)
```

Los demás Services son `ClusterIP` y solo se alcanzan desde dentro del clúster:

```bash
kubectl get svc -n restaurante
```

**Paso 4: liberar los recursos.** Borrar el namespace elimina de una vez los
Deployments, los Services, los Secrets, los ConfigMaps y el volumen de MySQL:

```bash
kubectl delete namespace restaurante
```

Si se cambia el `.env` o se regeneran las claves, hay que borrar el namespace y
repetir desde el paso 2, porque los Secrets se crean a partir de esos archivos.

---

## 3. Seguridad

La autenticación es con **JSON Web Token**. `usuarios-service` emite el token al
iniciar sesión, y el `api-gateway` lo valida y aplica los permisos antes de
reenviar la petición.

### Obtener el token

El inicio de sesión es la única operación pública:

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"correo": "administrador@restaurante.com", "password": "admin123"}'
```

Respuesta:

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "tipo": "Bearer",
  "expiraEn": 3600,
  "usuario": {
    "id": 1,
    "nombre": "Administrador Demo",
    "correo": "administrador@restaurante.com",
    "rol": "ADMINISTRADOR",
    "activo": true
  }
}
```

El token se firma con **HS256**, dura **3600 segundos** y lleva el rol del
usuario. A partir de ahí, todas las demás peticiones viajan con el encabezado:

```
Authorization: Bearer <token>
```

Por ejemplo:

```bash
curl http://localhost:8080/api/platillos \
  -H "Authorization: Bearer <token>"
```

Las contraseñas no se guardan en claro: se almacenan con **BCrypt**, que genera
una sal distinta para cada una.

### Matriz de permisos

La aplica el gateway, y gana la primera regla que coincide con la petición.

| Operación | Quién puede | Por qué |
|---|---|---|
| `POST /api/auth/login` | **Público** | Es la puerta de entrada |
| `/api/usuarios/**` (los cinco verbos) | **ADMINISTRADOR** | Gestión del personal |
| `DELETE /api/pedidos/{id}/platillos/{lineaId}` | **Cualquier rol autenticado** | Quitar un platillo del pedido es trabajo diario del mesero |
| Cualquier otro `DELETE` (platillos, mesas, clientes, reservas, anular factura) | **ADMINISTRADOR** | Borran datos maestros o revierten un cobro |
| Todo lo demás (GET, POST, PUT) | **Cualquier rol autenticado** | Operación diaria del restaurante |

### Respuestas de error del gateway

| Situación | Código | Cuerpo |
|---|---|---|
| Sin token, token vencido o firma inválida | **401** | `{"error": "Debe iniciar sesión con un token válido."}` |
| Token válido, rol sin permiso | **403** | `{"error": "No tiene permisos para realizar esta operación."}` |

### Usuarios de demostración

`usuarios-service` los crea al arrancar con la base vacía. Son datos de prueba
para la demostración y no corresponden a ninguna cuenta real.

| Correo | Clave | Rol |
|---|---|---|
| `administrador@restaurante.com` | `admin123` | ADMINISTRADOR |
| `mesero@restaurante.com` | `mesero123` | MESERO |
| `cocinero@restaurante.com` | `cocinero123` | COCINERO |
| `cajero@restaurante.com` | `cajero123` | CAJERO |

### El límite de confianza

El gateway es la única puerta: **solo él publica un puerto**. Los cuatro
servicios de negocio y MySQL no son alcanzables desde fuera, porque en Compose
no declaran `ports:` y en Kubernetes son `ClusterIP`.

Por eso los servicios de negocio no validan el token: confían en que solo les
llega tráfico que ya pasó por el gateway. Un proceso dentro de la misma red
interna sí podría llamarlos directamente, y ese es el límite reconocido del
modelo.

La clave de firma del entorno local está escrita en el repositorio a propósito,
solo sirve en el equipo de desarrollo. En Docker y en Kubernetes se usa la del
archivo `.env`, que nunca se sube.

---

## 4. Variables de entorno

Las usan los Dockerfiles, el Compose y los manifiestos de Kubernetes. Se escriben
exactamente así. **Este repositorio no contiene ningún valor real:** las claves se
declaran en `.env.example` con valores de mentira, y cada quien pone los suyos en
su copia `.env`, que está en el `.gitignore`.

| Variable | Quién la usa | Propósito |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | Los 5 módulos | Selecciona el perfil de configuración; en contenedores activa el que usa MySQL en lugar de H2 |
| `TZ` | Los 5 módulos | Zona horaria del contenedor. Sin ella se usa UTC y una reserva para hoy puede rechazarse como fecha pasada |
| `JAVA_TOOL_OPTIONS` | Los 5 módulos | Opciones de la JVM, para que respete el límite de memoria del contenedor |
| `DB_URL` | Los 4 servicios | Cadena de conexión JDBC a la base de datos del servicio |
| `DB_USER` | Los 4 servicios | Usuario de MySQL propio del servicio |
| `DB_PASSWORD` | Los 4 servicios | Contraseña de ese usuario de MySQL |
| `MYSQL_ROOT_PASSWORD` | `mysql` | Contraseña del superusuario, necesaria para crear las cuatro bases al arrancar |
| `USUARIOS_DB_PASSWORD`, `CARTA_DB_PASSWORD`, `CLIENTES_DB_PASSWORD`, `SALON_DB_PASSWORD` | `mysql` y el servicio correspondiente | Contraseña de cada usuario de base de datos, la misma en el contenedor de MySQL y en su servicio |
| `JWT_SECRET` | `usuarios-service` y `api-gateway` | Clave compartida con la que se firma y se valida el token. Mínimo 32 caracteres |

En el perfil de contenedores ninguna de estas variables tiene valor por omisión:
si falta una, el servicio **no arranca**, en lugar de arrancar silenciosamente
contra una base vacía.

---

## 5. Decisiones técnicas

| Decisión | Por qué |
|---|---|
| **MySQL 8** en los cuatro servicios | El dominio es transaccional: emitir una factura cambia Pedido, Mesa y Factura en una sola transacción, y una base relacional lo garantiza con el código JPA que ya existe |
| **Un contenedor de MySQL con cuatro bases de datos y un usuario por base** | Cada servicio solo ve su propia base, y en un portátil corre un solo MySQL en lugar de cuatro |
| **Dos perfiles de Spring**: H2 en memoria y MySQL | En el entorno local y en las pruebas todo funciona sin Docker; en contenedores, si falta una variable el servicio no arranca |
| **JWT emitido por `usuarios-service` y validado en el `api-gateway`** | Un solo punto de control: los cuatro servicios de negocio no cambian ni repiten la validación |
| **Permisos mínimos por rol** | Permite demostrar 200, 401 y 403 respetando lo que hace cada rol en el restaurante, sin multiplicar las reglas |
| **Un Dockerfile multietapa por módulo** | Cualquiera clona el repositorio y construye las imágenes sin compilar nada a mano; la imagen final solo lleva el JRE y el `.jar` |
| **Imágenes etiquetadas `1.0` y `imagePullPolicy: Never`** | Kubernetes usa la imagen construida en el equipo y no intenta descargarla de un registro |
| **Kubernetes de Docker Desktop** | No hay que instalar nada adicional y las imágenes locales se pueden usar tal cual |
| **Sin llamadas entre servicios** | Cada servicio es autónomo y dueño de sus datos; una llamada entre servicios añadiría un acoplamiento que el alcance actual no necesita |
| **BCrypt en lugar de un resumen sin sal** | BCrypt genera una sal distinta por contraseña y es lento a propósito, lo que hace inviables los ataques de diccionario |
| **Secret y ConfigMap creados por comando** | Las contraseñas quedan fuera de los archivos versionados: se generan desde el `.env` local |

---

## 6. Colecciones de Postman

En `docs/postman/` hay una colección por servicio, más la de autenticación:

```
docs/postman/
├── 00-autenticacion.postman_collection.json
├── usuarios.postman_collection.json
├── carta.postman_collection.json
├── clientes.postman_collection.json
└── salon.postman_collection.json
```

**1. Importarlas.** En Postman, *Import* y seleccionar los cinco archivos.

**2. Crear un environment llamado «Restaurante»** con una variable:

| Variable | Valor |
|---|---|
| `base` | `http://localhost:8080` con Docker Compose o en local, `http://localhost:30080` con Kubernetes |

Todas las peticiones usan `{{base}}`, así que cambiar ese único valor basta para
pasar de un modo de ejecución a otro. Hay que seleccionar el environment
«Restaurante» en la esquina superior derecha antes de enviar nada.

**3. Correr primero `00-autenticacion`.** Esa colección inicia sesión con los
cuatro usuarios de demostración y guarda los tokens en las variables `token`,
`tokenAdministrador`, `tokenMesero`, `tokenCocinero` y `tokenCajero`. Las demás
colecciones los leen de ahí: si se ejecutan antes, todas responden **401**.

Con los tokens ya guardados, cada colección se puede correr completa con el
*Collection Runner*. Las peticiones llevan sus propias pruebas, e incluyen los
casos de **401** y **403** que demuestran la matriz de permisos.

---

## 7. Referencias

Broadcom. (s. f.). *Spring Boot: documentación de referencia* (versión 4.1.1).
https://docs.spring.io/spring-boot/index.html

Broadcom. (s. f.). *Spring Cloud Gateway: documentación de referencia*
(versión 5.0.3). https://docs.spring.io/spring-cloud-gateway/reference/index.html

Broadcom. (s. f.). *Spring Security: documentación de referencia* (versión 7.1.1).
https://docs.spring.io/spring-security/reference/index.html

Díaz Santacruz, C. A. (s. f.). *Código de una aplicación con servicios web, API
REST (métodos POST, GET, PUT y DELETE) y microservicios* [material de clase].

Díaz Santacruz, C. A. (s. f.). *Proyectos en GitHub que implementan arquitecturas
de microservicios* [material de clase].

Díaz Santacruz, C. A. (s. f.). *Qué son y cómo funcionan los microservicios*
[material de clase].

Docker, Inc. (s. f.). *Docker Compose*. https://docs.docker.com/compose/

Docker, Inc. (s. f.). *Docker documentation*. https://docs.docker.com/

Jones, M., Bradley, J., & Sakimura, N. (2015). *JSON Web Token (JWT)* (RFC 7519).
RFC Editor. https://www.rfc-editor.org/rfc/rfc7519

Oracle Corporation. (s. f.). *MySQL 8.4 reference manual*.
https://dev.mysql.com/doc/refman/8.4/en/

OWASP Foundation. (s. f.). *Password storage cheat sheet*. OWASP Cheat Sheet
Series. https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html

The Kubernetes Authors. (s. f.). *Kubernetes documentation*.
https://kubernetes.io/docs/home/

Las direcciones electrónicas se consultaron el 12 de octubre de 2026. El material
de clase es de distribución interna y no tiene dirección pública.
