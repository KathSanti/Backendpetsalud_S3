# Clínica PetSalud · Backend seguro

API REST de la Clínica PetSalud (citas, expedientes médicos, mascotas, productos, ventas, recompensas y notificaciones) protegida con **Spring Security + JWT**, autorización por **roles** y por **propiedad de los recursos**, y **monitoreo de actividad sospechosa**.

**Tecnologías:** Java 17 · Spring Boot 3.3 · Spring Security 6 · JJWT 0.12 · Spring Data JPA · MySQL · Lombok · JUnit 5

---

## Estructura del proyecto

```
src/main/java/com/petsalud/backendpetsalud/
├── security/      Autenticación y autorización
│   ├── SecurityConfig         Reglas de la API: rutas públicas, sesión stateless, 401/403, BCrypt
│   ├── JwtService             Genera y valida los tokens (firma, emisor y expiración)
│   ├── JwtFilter              Lee el token "Bearer" de cada petición y autentica al usuario
│   ├── UsuarioDetailsService  Carga el usuario y su rol desde la base de datos
│   ├── UsuarioDetails         Adapta Usuario a Spring Security (rol con prefijo ROLE_)
│   └── AccesoRecursos         Verifica que el recurso pertenezca al cliente (evita IDOR)
├── monitoreo/     Detección de actividad sospechosa
│   ├── SuspiciousActivityFilter   Detecta patrones de SQLi, XSS y path traversal en la URL
│   └── SuspiciousActivityService  Registra los eventos [SEGURIDAD] y alerta fuerza bruta
├── exception/     GlobalExceptionHandler: convierte errores en respuestas JSON limpias (sin trazas)
├── controller/    Endpoints REST; cada uno declara sus permisos con @PreAuthorize
├── dto/           Datos de entrada validados y de salida sin campos sensibles (sin password)
├── service/       Lógica de negocio (el registro hashea la contraseña y asigna rol CLIENTE)
├── repository/    Acceso a datos con Spring Data JPA (consultas parametrizadas)
├── entity/        Entidades JPA
└── config/        DataInitializer: crea los usuarios de prueba al iniciar

src/test/      Tests unitarios y de autorización (sin base de datos)
pruebas/       Script de pruebas de la API con curl
```

## Flujo de una petición

1. `POST /api/auth/login` valida email y contraseña (BCrypt) y devuelve un JWT firmado (HS256, 30 min).
2. El cliente envía el token en cada petición: `Authorization: Bearer <token>`.
3. `SuspiciousActivityFilter` revisa la URL en busca de patrones de ataque.
4. `JwtFilter` valida firma, emisor y expiración, y carga el rol **actual** desde la base de datos.
5. `@PreAuthorize` decide según el rol y, para clientes, según la propiedad del recurso.
6. Si algo falla, `GlobalExceptionHandler` responde 400/401/403 en JSON y el monitoreo lo registra.

## Ejecución

**Requisitos:** Java 17 y MySQL en `localhost:3306` (la base `pedsaluds_db` se crea sola).

```bash
cp .env.example .env     # completa los valores (el .env no se sube al repositorio)
./mvnw spring-boot:run
```

| Variable | Uso |
|---|---|
| `MYSQL_USERNAME` / `MYSQL_PASSWORD` | Credenciales de MySQL |
| `JWT_SECRET_BASE64` | Clave de firma JWT, mínimo 32 bytes: `openssl rand -base64 32` |
| `SEED_PASSWORD` | Contraseña de los usuarios de prueba |

**Usuarios de prueba** (creados por `DataInitializer`, contraseña = `SEED_PASSWORD`):
`cliente@petsalud.test` · `empleado@petsalud.test` · `gerente@petsalud.test`

## Roles y permisos

| Recurso | Cliente | Empleado | Gerente |
|---|---|---|---|
| Productos | Ver | Ver, crear, editar, stock bajo | Todo + eliminar |
| Mascotas | Solo las suyas (crear, ver, editar) | Todas | Todas + eliminar |
| Citas | Agendar, ver y cancelar las de sus mascotas | Todas + cambiar estado | Todas |
| Expedientes médicos | Ver los de sus mascotas | Crear y ver todos | Todo + eliminar |
| Ventas | Ver las suyas | Registrar y ver por cliente | Todo + listar todas |
| Recompensas / Notificaciones | Ver las suyas | Crear y ver | Todo + eliminar |
| Usuarios | Ver y editar su perfil | Ver perfiles | Listar, editar y eliminar |

Públicos: `POST /api/auth/login` y `POST /api/usuarios/registro`. El resto exige token (sin token → **401**, sin permiso → **403**).

## Pruebas

**Tests automatizados** (no necesitan base de datos):

```bash
./mvnw test -Dtest='JwtServiceTest,AccesoRecursosTest,UsuarioServiceImplTest,AutorizacionEndpointsTest'
```

`./mvnw test` ejecuta además `BackendpetsaludApplicationTests`, que sí requiere MySQL y el `.env`.

**Pruebas de la API con curl** (con la aplicación corriendo):

```bash
./pruebas/pruebas-api.sh
```

Recorre autenticación, roles, propiedad de recursos, inyección SQL, XSS, CSRF y fuerza bruta, y muestra `✔ PASA` o `✘ FALLA` por cada caso. Funciona en la terminal de macOS/Linux y en Git Bash (Windows).

## Medidas de seguridad

| Amenaza | Medida |
|---|---|
| Acceso no autorizado | JWT firmado con expiración, roles con `@PreAuthorize`, control de propiedad (`AccesoRecursos`) |
| Robo de contraseñas | BCrypt, `@JsonIgnore` en `password`, DTOs de respuesta sin datos sensibles |
| Escalamiento de privilegios | El registro no acepta rol: siempre crea `CLIENTE` |
| Inyección SQL | Consultas parametrizadas de Spring Data JPA y validación de entrada |
| XSS | Validación con `@Pattern` (sin `<` `>`), respuestas JSON y cabecera `nosniff` |
| CSRF | API stateless sin cookies: el token viaja en la cabecera `Authorization` |
| Fuerza bruta y ataques | `SuspiciousActivityService` registra intentos y alerta desde 5 fallos por IP |
| Exposición de secretos | Claves en `.env` (excluido de Git); la app no arranca con claves débiles |
