# Banking Microservices

Solución desarrollada en Java 21 y Spring Boot 3 para el ejercicio práctico de microservicios bancarios.

La aplicación está separada en dos microservicios:

* `customer-service`: gestión de personas y clientes.
* `account-service`: gestión de cuentas, movimientos y reportes de estado de cuenta.

La comunicación entre servicios es asíncrona mediante RabbitMQ. Cuando se crea, actualiza o desactiva un cliente, `customer-service` publica un evento que luego es consumido por `account-service` para mantener una copia local de los datos mínimos del cliente.

## Tecnologías utilizadas

* Java 21
* Spring Boot 3
* Spring Web
* Spring Data JPA
* PostgreSQL
* Flyway
* RabbitMQ
* Docker / Docker Compose
* Maven
* JUnit 5
* Mockito
* Testcontainers
* Postman

## Arquitectura

La solución usa dos bases de datos separadas, una por microservicio:

* `customer_db`
* `account_db`

`account-service` no consulta directamente la base de datos de clientes. En su lugar, mantiene una tabla local de snapshots de cliente, actualizada a partir de eventos recibidos por RabbitMQ.

Esto permite mantener bajo acoplamiento entre servicios y evita dependencias directas entre bases de datos.

Flujo principal de eventos:

```text
customer-service
    |
    | guarda cliente
    | guarda evento en outbox
    v
customer_db.outbox_events
    |
    | publicador programado
    v
RabbitMQ exchange: customer.events
    |
    v
account-service
    |
    v
account_db.customer_snapshots
```

Para la publicación de eventos se implementó el patrón Transactional Outbox. La idea es guardar el cambio de negocio y el evento pendiente dentro de la misma transacción de base de datos. Luego, un proceso programado publica los eventos pendientes a RabbitMQ.

## Módulos

```text
banking-microservices
├── customer-service
├── account-service
├── postman
├── docker-compose.yml
├── DataBase.sql
└── README.md
```

## Ejecución con Docker

Para construir y levantar toda la solución:

```bash
docker compose up -d --build
```

Servicios expuestos:

| Servicio            | URL                      |
| ------------------- | ------------------------ |
| Customer API        | `http://localhost:8081`  |
| Account API         | `http://localhost:8082`  |
| RabbitMQ Management | `http://localhost:15672` |
| Customer PostgreSQL | `localhost:5433`         |
| Account PostgreSQL  | `localhost:5434`         |

Credenciales de RabbitMQ:

```text
guest / guest
```

Para verificar el estado de los contenedores:

```bash
docker compose ps
```

Para bajar la solución manteniendo los datos:

```bash
docker compose down
```

Para bajar la solución eliminando los volúmenes de base de datos:

```bash
docker compose down -v
```

Esto último es útil si se quiere volver a probar el flujo desde una base limpia.

## Health checks

```http
GET http://localhost:8081/actuator/health
GET http://localhost:8082/actuator/health
```

Ambos servicios deberían responder con estado `UP`.

## Entregables

El repositorio incluye:

* `BaseDatos.sql`: script consolidado con el esquema de base de datos.
* `postman/Devsu-Banking.postman_collection.json`: colección de Postman para validar los casos principales.
* Migraciones Flyway dentro de cada microservicio.
* Dockerfiles para construir cada aplicación.
* `docker-compose.yml` para levantar la solución completa.

La colección de Postman asume bases de datos limpias, ya que utiliza los datos indicados en el ejercicio.

## Customer Service

Base URL:

```text
http://localhost:8081
```

Base path:

```text
/api/clientes
```

### Crear cliente

```http
POST /api/clientes
Content-Type: application/json
```

```json
{
  "nombre": "Jose Lema",
  "genero": "Masculino",
  "edad": 30,
  "identificacion": "098254785",
  "direccion": "Otavalo sn y principal",
  "telefono": "098254785",
  "clienteId": "CLI-001",
  "contrasena": "1234",
  "estado": true
}
```

`estado` es opcional al crear un cliente. Si no se envía, se toma como `true`.

La contraseña se almacena encriptada usando BCrypt y no se devuelve en las respuestas de la API.

### Listar clientes

```http
GET /api/clientes
```

### Obtener cliente

```http
GET /api/clientes/{clienteId}
```

Ejemplo:

```http
GET /api/clientes/CLI-001
```

### Actualizar cliente

```http
PUT /api/clientes/{clienteId}
Content-Type: application/json
```

```json
{
  "nombre": "Jose Lema",
  "genero": "Masculino",
  "edad": 31,
  "identificacion": "098254785",
  "direccion": "Otavalo sn y principal",
  "telefono": "098254785",
  "clienteId": "CLI-001",
  "estado": true
}
```

### Cambiar estado

```http
PATCH /api/clientes/{clienteId}/estado
Content-Type: application/json
```

```json
{
  "estado": false
}
```

### Eliminar cliente

```http
DELETE /api/clientes/{clienteId}
```

La eliminación es lógica. El registro no se borra físicamente; se actualiza `estado=false`.

## Eventos de cliente

`customer-service` publica eventos hacia el exchange durable:

```text
customer.events
```

Routing keys utilizadas:

```text
customer.created
customer.updated
customer.status-changed
customer.deleted
```

Los eventos contienen la información mínima necesaria para que `account-service` mantenga su snapshot local:

```json
{
  "eventId": "8d66cb84-c45b-44a1-95a0-f3f7d862b17d",
  "eventType": "CUSTOMER_CREATED",
  "customerId": "CLI-001",
  "name": "Jose Lema",
  "identification": "098254785",
  "status": true,
  "occurredAt": "2026-06-18T20:00:00Z"
}
```

## Account Service

Base URL:

```text
http://localhost:8082
```

### Crear cuenta

```http
POST /api/cuentas
Content-Type: application/json
```

```json
{
  "numeroCuenta": "478758",
  "tipoCuenta": "Ahorros",
  "saldoInicial": 2000.00,
  "estado": true,
  "clienteId": "CLI-001"
}
```

Reglas principales:

* `numeroCuenta` debe ser único.
* `saldoInicial` no puede ser negativo.
* `saldoDisponible` se inicializa con el mismo valor que `saldoInicial`.
* El cliente debe existir en `customer_snapshots`.
* No se permite crear cuentas para clientes inactivos.

### Listar cuentas

```http
GET /api/cuentas
```

### Obtener cuenta

```http
GET /api/cuentas/{numeroCuenta}
```

### Actualizar cuenta

```http
PUT /api/cuentas/{numeroCuenta}
Content-Type: application/json
```

```json
{
  "tipoCuenta": "Corriente",
  "estado": true
}
```

El saldo no se modifica desde la actualización de cuenta. Los cambios de saldo se realizan únicamente mediante movimientos.

## Movimientos

Base path:

```text
/api/movimientos
```

### Registrar movimiento

```http
POST /api/movimientos
Content-Type: application/json
```

Depósito:

```json
{
  "numeroCuenta": "225487",
  "valor": 600.00
}
```

Retiro:

```json
{
  "numeroCuenta": "478758",
  "valor": -575.00
}
```

Reglas principales:

* Los valores positivos representan depósitos.
* Los valores negativos representan retiros.
* Al registrar un movimiento se actualiza el saldo disponible de la cuenta.
* El movimiento y la actualización de saldo se guardan en la misma transacción.
* Si el retiro deja la cuenta con saldo negativo, se devuelve el mensaje `Saldo no disponible`.

Ejemplo de error por saldo insuficiente:

```json
{
  "timestamp": "2026-06-18T20:00:00Z",
  "status": 400,
  "error": "Solicitud Incorrecta",
  "message": "Saldo no disponible",
  "path": "/api/movimientos"
}
```

### Consultar movimientos

```http
GET /api/movimientos
GET /api/movimientos?numeroCuenta=478758
GET /api/movimientos/{movimientoId}
```

## Reporte de estado de cuenta

Endpoint:

```http
GET /api/reportes?fecha=2022-02-01,2022-02-28&cliente=CLI-002
```

El parámetro `fecha` recibe un rango en formato:

```text
YYYY-MM-DD,YYYY-MM-DD
```

El reporte devuelve las cuentas asociadas al cliente y los movimientos realizados dentro del rango solicitado.

Ejemplo de respuesta:

```json
{
  "clienteId": "CLI-002",
  "cliente": "Marianela Montalvo",
  "fechaDesde": "2022-02-01",
  "fechaHasta": "2022-02-28",
  "cuentas": [
    {
      "numeroCuenta": "225487",
      "tipoCuenta": "Corriente",
      "saldoInicial": 100.00,
      "saldoDisponible": 700.00,
      "estado": true,
      "movimientos": [
        {
          "fecha": "2022-02-10T00:00:00Z",
          "tipoMovimiento": "CREDIT",
          "valor": 600.00,
          "saldo": 700.00
        }
      ]
    },
    {
      "numeroCuenta": "496825",
      "tipoCuenta": "Ahorros",
      "saldoInicial": 540.00,
      "saldoDisponible": 0.00,
      "estado": true,
      "movimientos": [
        {
          "fecha": "2022-02-08T00:00:00Z",
          "tipoMovimiento": "DEBIT",
          "valor": -540.00,
          "saldo": 0.00
        }
      ]
    }
  ]
}
```

## Formato de errores

Los errores de validación y negocio usan un formato común:

```json
{
  "timestamp": "2026-06-18T20:00:00Z",
  "status": 400,
  "error": "Solicitud Incorrecta",
  "message": "nombre: El nombre es obligatorio",
  "path": "/api/clientes"
}
```

Mapeo general:

| Caso                      | Código HTTP |
| ------------------------- | ----------- |
| Error de validación       | 400         |
| Regla de negocio inválida | 400         |
| Recurso no encontrado     | 404         |
| Registro duplicado        | 409         |

## Pruebas

Para ejecutar todas las pruebas:

```bash
mvn test
```

Para ejecutar un módulo específico:

```bash
mvn test -pl customer-service
mvn test -pl account-service
```

Las pruebas incluyen unit tests de servicios y pruebas de integración sobre los casos principales del ejercicio.

## Postman

La colección se encuentra en:

```text
postman/Devsu-Banking.postman_collection.json
```

Orden sugerido de ejecución:

1. Crear clientes.
2. Crear cuentas.
3. Registrar movimientos.
4. Validar saldo no disponible.
5. Consultar reporte de estado de cuenta.

También puede ejecutarse con Newman:

```bash
docker run --rm \
  -v "$PWD/postman:/etc/newman" \
  postman/newman:6-alpine run \
  /etc/newman/Devsu-Banking.postman_collection.json \
  --env-var customerBaseUrl=http://host.docker.internal:8081 \
  --env-var accountBaseUrl=http://host.docker.internal:8082
```

En PowerShell:

```powershell
docker run --rm `
  -v "${PWD}\postman:/etc/newman" `
  postman/newman:6-alpine run `
  /etc/newman/Devsu-Banking.postman_collection.json `
  --env-var customerBaseUrl=http://host.docker.internal:8081 `
  --env-var accountBaseUrl=http://host.docker.internal:8082
```

## Notas de implementación

Algunas decisiones tomadas:

* Se usan dos microservicios separados, cada uno con su propia base de datos.
* La comunicación entre servicios es asíncrona mediante RabbitMQ.
* `customer-service` usa Transactional Outbox para no depender de una publicación directa dentro del flujo principal.
* `account-service` mantiene snapshots locales de cliente para validar cuentas sin consultar directamente a `customer-service`.
* Las contraseñas se almacenan con BCrypt y no se exponen en respuestas.
* Los movimientos son la única forma de modificar el saldo disponible de una cuenta.
* Los retiros que dejan saldo negativo se rechazan con el mensaje requerido: `Saldo no disponible`.
* Se usa Docker Compose para levantar toda la solución con sus dependencias.
