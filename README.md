# restaurante-microservicios

Sistema de Gestión de Restaurante expuesto como microservicios REST con Spring Boot.
El proyecto es un repositorio Maven multimódulo: un `pom.xml` padre que coordina
cinco aplicaciones Spring Boot independientes, cada una con su propio `main`, su
propio puerto y su propia base de datos.

- [1. El sistema y su arquitectura](#1-el-sistema-y-su-arquitectura)
- [2. Cómo ejecutarlo desde un clon limpio](#2-cómo-ejecutarlo-desde-un-clon-limpio)
- [3. Seguridad](#3-seguridad)
- [4. Variables de entorno](#4-variables-de-entorno)
- [5. Decisiones técnicas](#5-decisiones-técnicas)
- [6. Colecciones de Postman](#6-colecciones-de-postman)
- [7. Referencias](#7-referencias)

---

## 1. El sistema y su arquitectura

Cuatro microservicios de negocio, un gateway que es la única puerta de entrada y
un MySQL con una base de datos por servicio.

```
                      Postman / curl
                            │  Authorization: Bearer <token>
                            ▼
   ─────────────── único puerto publicado: 8080 (Compose) · 30080 (Kubernetes)
                ┌───────────────────────────┐
                │     api-gateway :8080     │  valida el JWT y los permisos
                └──┬──────┬───────┬──────┬──┘
     red interna   │      │       │      │   (no alcanzable desde afuera)
        ┌──────────▼┐ ┌───▼──────┐ ┌▼──────────┐ ┌▼──────────┐
        │ usuarios  │ │  carta   │ │ clientes  │ │  salon    │
        │  :8081    │ │  :8082   │ │  :8083    │ │  :8084    │
        │ emite JWT │ │          │ │           │ │           │
        └─────┬─────┘ └────┬─────┘ └─────┬─────┘ └─────┬─────┘
              │            │             │             │
        ┌─────▼────────────▼─────────────▼─────────────▼─────┐
        │                mysql :3306  (MySQL 8.4)            │
        │  usuariosdb   cartadb   clientesdb   salondb       │
        │  (usuario     (usuario  (usuario     (usuario      │
        │   usuarios)    carta)    clientes)    salon)       │
        └────────────────────────┬───────────────────────────┘
                                 │
                        volumen persistente
```

### Módulos

| Módulo | Puerto | Base de datos | Dominio | Imagen |
|---|---|---|---|---|
| `api-gateway` | 8080 | — | Enruta las peticiones hacia los cuatro servicios y valida el token | `restaurante/api-gateway:1.0` |
| `usuarios-service` | 8081 | `usuariosdb` | Usuario, roles y autenticación | `restaurante/usuarios-service:1.0` |
| `carta-service` | 8082 | `cartadb` | Platillo | `restaurante/carta-service:1.0` |
| `clientes-service` | 8083 | `clientesdb` | Cliente, Reserva | `restaurante/clientes-service:1.0` |
| `salon-service` | 8084 | `salondb` | Mesa, Pedido, Factura | `restaurante/salon-service:1.0` |

La etiqueta de las imágenes es `1.0`, nunca `latest`: con `latest`, Kubernetes
intenta descargar la imagen de Internet aunque ya esté construida en el equipo.

### Rutas

El cliente HTTP solo necesita conocer la dirección del gateway: él reenvía cada
ruta al servicio que le corresponde.

| Prefijo de ruta | Servicio |
|---|---|
| `/api/usuarios/**`, `/api/auth/**` | `usuarios-service` |
| `/api/platillos/**` | `carta-service` |
| `/api/clientes/**`, `/api/reservas/**` | `clientes-service` |
| `/api/mesas/**`, `/api/pedidos/**`, `/api/facturas/**` | `salon-service` |

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

Hay tres formas de levantar el sistema. En las tres, la aplicación se consume
siempre a través del gateway.

| Modo | Base de datos | Dirección base |
|---|---|---|
| Local | H2 en memoria | `http://localhost:8080` |
| Docker Compose | MySQL en contenedor | `http://localhost:8080` |
| Kubernetes | MySQL con volumen persistente | `http://localhost:30080` |

### 2.1 Local (IntelliJ o el wrapper de Maven)

Requisitos: **JDK 21**. No hace falta instalar Maven, el repositorio trae el
wrapper. Este modo usa el perfil por omisión, que levanta una base **H2 en
memoria**: no requiere Docker ni MySQL y los datos se pierden al apagar.

Compilar los cinco módulos desde la raíz:

```bash
./mvnw clean install
```

En Windows:

```powershell
.\mvnw.cmd clean install
```

Cada servicio se ejecuta por separado, en su propia terminal:

```bash
./mvnw spring-boot:run -pl usuarios-service
./mvnw spring-boot:run -pl carta-service
./mvnw spring-boot:run -pl clientes-service
./mvnw spring-boot:run -pl salon-service
./mvnw spring-boot:run -pl api-gateway
```

Para probar el sistema completo hay que levantar los cinco. Desde IntelliJ IDEA
es equivalente: abrir el `pom.xml` de la raíz para que importe los cinco módulos
y ejecutar la clase `main` de cada uno sin indicar ningún perfil.

Cada servicio expone además la consola de H2 en su propio puerto, por ejemplo
`http://localhost:8082/h2-console` para la carta.

### 2.2 Docker Compose

Requisitos: **Docker Desktop** en marcha. No hace falta JDK: las imágenes se
compilan dentro de Docker.

Primero, copiar el archivo de variables de ejemplo y poner valores propios en la
copia. `.env` no se sube al repositorio.

```bash
cp .env.example .env
```

En Windows:

```powershell
Copy-Item .env.example .env
```

Construir las imágenes y levantar los seis contenedores:

```bash
docker compose up --build
```

La primera vez tarda varios minutos, porque cada imagen descarga sus
dependencias de Maven. El gateway queda escuchando en:

```
http://localhost:8080
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

Sin `-v`, los datos sobreviven al apagado.

### 2.3 Kubernetes en Docker Desktop

Requisitos: Docker Desktop con Kubernetes activado (*Settings → Kubernetes →
Enable Kubernetes*) y el archivo `.env` ya creado como en el paso anterior.

**Paso 1: construir las cinco imágenes en el equipo.** Los manifiestos usan
`imagePullPolicy: Never`, es decir, Kubernetes no intenta descargar nada de
Internet y solo usa las imágenes locales. Si no existen, los Pods se quedan en
`ErrImageNeverPull`.

```bash
docker compose build
docker images restaurante/*
```

El segundo comando debe listar las cinco imágenes con la etiqueta `1.0`.

**Paso 2: crear el namespace, el Secret y el ConfigMap.** Las contraseñas no
están escritas en los manifiestos: el Secret se genera por comando a partir del
mismo `.env` que usa Compose, y el ConfigMap a partir del script que crea las
cuatro bases de datos.

```bash
kubectl apply -f k8s/00-namespace.yaml
kubectl create secret generic restaurante-secretos --from-env-file=.env -n restaurante
kubectl create configmap mysql-inicializacion --from-file=docker/mysql/ -n restaurante
```

**Paso 3: desplegar todo y esperar a que los Pods estén listos.**

```bash
kubectl apply -f k8s/
kubectl get pods -n restaurante -w
```

Se esperan seis Pods en `1/1 Running`. El gateway se publica con un Service de
tipo NodePort `8080:30080`, así que la dirección base es:

```
http://localhost:30080
```

Los otros cinco Services son `ClusterIP` y solo se alcanzan desde dentro del
clúster:

```bash
kubectl get svc -n restaurante
```

**Paso 4: liberar los recursos.** Borrar el namespace elimina de una vez los
Deployments, los Services, el Secret, el ConfigMap y el volumen de MySQL:

```bash
kubectl delete namespace restaurante
```

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
