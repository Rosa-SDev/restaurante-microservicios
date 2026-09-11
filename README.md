# restaurante-microservicios

Sistema de Gestion de Restaurante expuesto como microservicios REST con Spring Boot.
El proyecto es un repositorio Maven multi-modulo: un `pom.xml` padre que coordina
cinco aplicaciones Spring Boot independientes, cada una con su propio `main`, su
propio puerto y su propia base de datos.

## Modulos

| Modulo | Puerto | Base de datos | Dominio |
|---|---|---|---|
| `api-gateway` | 8080 | — | Enruta las peticiones hacia los cuatro servicios |
| `usuarios-service` | 8081 | `usuariosdb` | Usuario, roles y autenticacion |
| `carta-service` | 8082 | `cartadb` | Platillo |
| `clientes-service` | 8083 | `clientesdb` | Cliente, Reserva |
| `salon-service` | 8084 | `salondb` | Mesa, Pedido, Factura |

El cliente HTTP solo necesita conocer `http://localhost:8080`: el gateway reenvia
cada ruta al servicio que le corresponde.

| Prefijo de ruta | Servicio |
|---|---|
| `/api/usuarios/**`, `/api/auth/**` | `usuarios-service` |
| `/api/platillos/**` | `carta-service` |
| `/api/clientes/**`, `/api/reservas/**` | `clientes-service` |
| `/api/mesas/**`, `/api/pedidos/**`, `/api/facturas/**` | `salon-service` |

Cada servicio fija su puerto y su base de datos en su propio
`src/main/resources/application.yml`.

## Requisitos

- JDK 21
- No hace falta instalar Maven: el repositorio trae el wrapper.

## Compilar

Desde la raiz, para los cinco modulos a la vez:

```bash
./mvnw clean install
```

En Windows:

```
.\mvnw.cmd clean install
```

## Levantar un servicio

Cada servicio se ejecuta por separado, en su propia terminal:

```bash
./mvnw spring-boot:run -pl carta-service
```

Sustituye `carta-service` por el modulo que quieras arrancar. Para probar el
sistema completo hay que levantar los cinco.

## Estructura de un servicio

```
carta-service/
└── src/main/
    ├── java/com/restaurante/carta/
    │   ├── CartaApplication.java   punto de entrada
    │   ├── controller/             traduce HTTP
    │   ├── service/                reglas de negocio
    │   ├── repository/             acceso a datos
    │   ├── model/                  entidades JPA
    │   └── dto/                    lo que viaja en el JSON
    └── resources/application.yml   puerto y base de datos
```
