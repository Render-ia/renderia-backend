# Render.IA — Backend

API REST de **Render.IA**, una plataforma que usa inteligencia artificial para convertir planos 2D en modelos estructurales 3D.

Proyecto final de **Patrones de Software** — Universidad Cooperativa de Colombia, sede Pasto.

- **Integrantes:** Juan David Moreno, Felipe Cerón
- **API en producción:** https://renderia-backend.onrender.com/api/v1/hello
- **Frontend:** https://renderia-frontend.vercel.app ([repositorio](https://github.com/Render-ia/renderia-frontend))
- **Base de datos:** [renderia-database](https://github.com/Render-ia/renderia-database) (PostgreSQL en Neon, con el DER)

## Arquitectura

```mermaid
flowchart LR
    U[Usuario] --> F[Frontend<br/>Vercel]
    F -- HTTPS + JWT --> B[Backend Java<br/>Render]
    B -- JPA --> D[(PostgreSQL<br/>Neon)]
    B -- HTTP --> I[Servicio de IA<br/>intercambiable]
```

El usuario usa el frontend, el frontend llama a esta API, la API guarda y lee en PostgreSQL y le pide el análisis de los planos al componente de IA. El modelo de IA se elige desde la tabla `ai_models`, así que se puede cambiar sin tocar el frontend, el backend ni la base de datos.

## Tecnologías

| Parte | Herramienta |
|---|---|
| Lenguaje | Java 21 |
| Framework | Spring Boot 4 (Web MVC, Data JPA, Security) |
| Base de datos | PostgreSQL en Neon |
| Autenticación | JWT firmado con HS256 y contraseñas cifradas con BCrypt |
| Contenedor | Docker (imagen multi-etapa) |
| CI/CD | GitHub Actions: compila, prueba, construye la imagen y despliega en Render |

## Endpoints

Todas las rutas empiezan por `/api/v1`. El contrato completo que usa el frontend está en [`docs/API.md`](https://github.com/Render-ia/renderia-frontend/blob/main/docs/API.md).

**Documentación interactiva (Swagger):** [`/swagger-ui.html`](https://renderia-backend.onrender.com/swagger-ui.html) · definición OpenAPI en [`/v3/api-docs`](https://renderia-backend.onrender.com/v3/api-docs). Para probar un endpoint protegido: `POST /auth/demo` (o `/auth/login`), copiar el `token` y pegarlo en **Authorize**.

### Ciclo completo en una llamada

`POST /analyses/run` con `{ "floorPlanId": 1, "aiModelId": 1 }` hace todo el recorrido dentro del backend:

```
usuario → frontend → backend ─┬─ BD: crea el análisis (PENDING → PROCESSING)
                              ├─ IA: renderia-ai /analyze (fuera de transacción)
                              ├─ BD: guarda structural_elements + models_3d (COMPLETED)
                              └─ responde al frontend → visor 3D
```

Si la IA falla, el análisis queda en `FAILED` con el motivo (nunca atascado en `PROCESSING`), aunque el usuario cierre el navegador. `POST /analyses/{id}/run` reintenta uno en `FAILED`.

| Método | Ruta | Acceso | Descripción |
|---|---|---|---|
| GET | `/hello` | Público | Nombre del proyecto e integrantes |
| GET | `/health` | Público | Estado del servidor y de la base de datos |
| POST | `/auth/register` | Público | Crea una cuenta (Ingeniero, Estudiante o Cliente) |
| POST | `/auth/demo` | Público | Entra a la cuenta de prueba de un rol, sin contraseña en el navegador |
| POST | `/auth/login` | Público | Devuelve el token de sesión |
| GET | `/auth/me` | Con token | Usuario de la sesión actual |
| GET | `/roles` | Público | Roles disponibles |
| GET | `/building-types`, `/element-types`, `/materials` | Público | Catálogos |
| POST, PUT, DELETE | `/building-types`, `/element-types`, `/materials` | Administrador | Editar catálogos |
| GET | `/ai-models` | Público | Modelos de IA disponibles |
| PUT | `/ai-models/{id}` | Administrador | Activar un modelo o elegir el predeterminado |
| GET, POST | `/projects/{id}/members` | Con token | Ver y compartir un proyecto (solo el dueño agrega) |
| DELETE | `/projects/{id}/members/{userId}` | Con token | Dejar de compartir |
| POST | `/analyses/run` | Con token | **Ciclo completo:** crea el análisis, llama a la IA, guarda elementos y modelo 3D |
| POST | `/analyses/{id}/run` | Con token | Corre o reintenta (si está en FAILED) el ciclo completo de un análisis |
| POST | `/analyses/{id}/review` | Dueño o Revisor | Marca el análisis como revisado; el servidor guarda quién y cuándo en el resumen del análisis |
| POST | `/analyses/{id}/copilot` | Con token | Pregunta al copiloto de ingeniería sobre el análisis (límite diario por usuario) |
| POST | `/analyses/{id}/render` | Con token | Render con IA desde una captura del visor 3D (límite diario por usuario) |
| GET | `/ai/capabilities` | Con token | Qué funciones de IA están activas y cuántas consultas le quedan hoy al usuario |

Los errores responden con el código HTTP adecuado y un cuerpo `{ "message": "..." }` en español.

## Roles

| Rol | Qué puede hacer |
|---|---|
| Administrador | Todo: usuarios, catálogos, modelos de IA y cualquier proyecto |
| Ingeniero | Sus proyectos, sin límite, y compartirlos |
| Revisor | Ve y valida todos los proyectos; no crea, no cambia, no borra |
| Cliente | Solo ve los proyectos que le comparten |
| Estudiante | Sus proyectos, máximo 3 |

Las reglas viven en `security/policy` (patrón Strategy): una política por rol.

### Cuentas de prueba

Hay una por rol: `admin.prueba@`, `ingeniero@`, `revisor@`, `cliente@` y `estudiante@renderia.co`. Se entra con los botones "Usar" del login, que llaman a `/auth/demo`, así que ninguna contraseña viaja al navegador. Cada vez que el servidor arranca, sus contraseñas se cambian por unas aleatorias que solo quedan guardadas como hash BCrypt: nadie las conoce y las contraseñas que se publicaron antes dejan de servir. El administrador de prueba ve todo el panel, pero no puede cambiar usuarios, catálogos, modelos de IA ni proyectos ajenos. El cliente de prueba ve el primer proyecto del ingeniero de prueba. Se apagan con `DEMO_ACCOUNTS=false`.

## Patrones de diseño

| Patrón | Dónde | Para qué |
|---|---|---|
| **Template Method** | `CatalogService` | Listar, crear, editar y borrar siguen los mismos pasos en los tres catálogos; cada subclase solo define su entidad y su nombre |
| **Singleton** | Servicios y configuración (`@Service`, `@Configuration`) | Spring crea una sola instancia de cada uno y la comparte |
| **Repository** | `repository/*` | Separa el acceso a datos de la lógica de negocio |
| **DTO** | `dto/*` | Lo que viaja por la API no expone las entidades (por ejemplo, nunca sale la contraseña) |
| **Facade** | `AnalysisPipelineService` | `run()` es el único punto de entrada del ciclo completo: coordina permisos, plano, servicio de IA, elementos, modelo 3D y bitácora |
| **State** | `AnalysisStatus` | Cada estado sabe a cuáles puede pasar (PENDING → PROCESSING → COMPLETED/FAILED, FAILED → PENDING) |
| **Strategy** | `security/policy/*` | Una política por rol decide qué puede hacer cada usuario |
| **Facade** | `AuthService`, `CopilotService` | Una sola llamada esconde validar, cifrar, guardar y generar el token; el copiloto revisa acceso y cupo, arma el contexto desde la base de datos y llama al servicio de IA |

## Estructura

```
src/main/java/com/renderia/renderia_backend/
├── config/       CORS y cuenta de administrador inicial
├── controller/   Rutas REST
├── dto/          Objetos de petición y respuesta
├── exception/    Errores con mensajes en español
├── model/        Entidades JPA (tablas de schema.sql)
├── repository/   Acceso a datos con Spring Data JPA
├── security/     JWT y reglas de acceso
└── service/      Lógica de negocio
```

## Variables de entorno

Ninguna clave se guarda en el repositorio. En Render se configuran estas variables:

| Variable | Para qué |
|---|---|
| `DB_URL` | Conexión JDBC a Neon (`jdbc:postgresql://.../neondb?sslmode=require`) |
| `DB_USERNAME`, `DB_PASSWORD` | Credenciales de la base de datos |
| `JWT_SECRET` | Clave para firmar los tokens. **Obligatoria en producción** (mínimo 32 caracteres; genera una con `openssl rand -base64 48`). Si falta en Render, el backend no arranca |
| `JWT_EXPIRATION_HOURS` | Duración de la sesión (por defecto 12) |
| `AI_SERVICE_URL`, `AI_API_KEY` | Dirección del servicio de IA y la clave compartida con él |
| `ADMIN_EMAIL`, `ADMIN_PASSWORD` | Crean la primera cuenta de administrador al arrancar |
| `DEMO_ACCOUNTS` | `true` por defecto: crea las cinco cuentas de prueba y les pone contraseñas aleatorias en cada arranque. Si el ingeniero o el estudiante de prueba no tienen proyectos, se les crean proyectos de ejemplo con el plano de la casa |
| `COPILOT_DAILY_LIMIT`, `COPILOT_DAILY_LIMIT_STUDENT`, `RENDER_DAILY_LIMIT` | Preguntas al copiloto y renders con IA por usuario y por día (por defecto 60, 20 y 5) |
| `FRONTEND_URL` | Dominios con permiso CORS (opcional; por defecto Vercel y `localhost:5173`) |

La lista completa, con ejemplos falsos, está en [`.env.example`](.env.example).

## Seguridad

| Riesgo | Medida |
|---|---|
| Clave JWT conocida | No hay valor por defecto. En producción (`RENDER=true`) el arranque falla sin `JWT_SECRET`; la clave que estuvo publicada en el repo se rechaza siempre |
| Fuerza bruta en login | 5 intentos fallidos por correo y 30 por IP cada 15 minutos; después responde `429` con `Retry-After` |
| Registro masivo | 10 cuentas nuevas por IP cada hora |
| Enumerar correos | Si el correo no existe igual se compara un hash BCrypt, así la respuesta tarda lo mismo |
| Imágenes de planos por id | La imagen solo se entrega con un enlace firmado (HMAC-SHA256) que vence en menos de 48 horas |
| Fugas en errores 500 | El cliente recibe un código de referencia; el detalle queda solo en el log |
| SVG con scripts | La imagen se sirve con `Content-Security-Policy: sandbox` y `nosniff` |
| Cabeceras | HSTS, `X-Frame-Options: DENY`, `Referrer-Policy: no-referrer` |

## Cómo ejecutarlo

Requiere Java 21 y una base PostgreSQL con las tablas de `renderia-database`.

```bash
# Windows (PowerShell)
$env:DB_URL="jdbc:postgresql://localhost:5432/renderia"
$env:DB_USERNAME="postgres"
$env:DB_PASSWORD="postgres"
.\mvnw spring-boot:run
```

La API queda en `http://localhost:8080/api/v1/hello`.

## Despliegue

Cada push a `main` corre el flujo de GitHub Actions (`.github/workflows/ci.yml`): compila, corre las pruebas y construye la imagen de Docker. Render está conectado al repositorio con **Auto-Deploy: After CI Checks Pass**, así que solo publica la nueva versión cuando el CI termina en verde.
