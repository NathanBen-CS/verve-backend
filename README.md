# Verve Backend

REST API for **Verve**, a tutoring-management platform. Tutors manage their students, schedule lessons, assign and grade homework, and generate printable lesson/homework PDFs; students log in to view their own work and submit it. Billing runs through Stripe subscriptions, and an OpenAI-backed endpoint drafts lesson material and assists with grading.

This repository is the Spring Boot service that powered `vervetutor.com`. The project is no longer operated — it is published here as a portfolio reference, and all credentials associated with it have been revoked.

## Stack

| Concern | Choice |
|---|---|
| Language / runtime | Java 17 |
| Framework | Spring Boot 3.5 (Web, WebFlux, Data JPA, Security, Mail, Thymeleaf) |
| Database | PostgreSQL (hosted on Supabase) |
| Auth | Stateless JWT (`jjwt` 0.11.5) + BCrypt |
| AI | Spring AI OpenAI starter + OpenAI Java SDK |
| Payments | Stripe Java SDK 29.x (Checkout + webhooks) |
| PDF generation | LaTeX (`texlive`) invoked from the service |
| Mapping | ModelMapper, via per-entity `Mapper<E, D>` implementations |
| Build / deploy | Maven, Docker, Google Cloud Build → Cloud Run |

## Architecture

The service is organised by feature rather than by layer. Each package owns its entity, DTO, repository, service, and controller:

```
com.vervetutor.tutor_assistant
├── Auth/            JWT login + registration request/response handling
├── Registration/    Email-confirmation sign-up flow
│   └── Token/       Single-use confirmation tokens
├── Config/          SecurityConfig, JwtService, JwtAuthenticationFilter, ModelMapper
├── User/            Base account (email, password, role), profile + password updates
├── Tutor/           Tutor profile, owns students
├── Student/         Student profile, owned by a tutor
├── Lesson/          Lessons, attached files, LaTeX PDF export
├── Homework/        Assignments, student submissions, AI-assisted grading
├── Document/        Stored lesson PDFs
├── Email/           Transactional mail + scheduled lesson reminders
├── Ai/              OpenAI chat endpoint
├── Stripe/          Checkout sessions, products, webhook receiver
├── Stripe2/         Subscription + customer helpers
└── Mappers/         Entity ↔ DTO mapping
```

**Request flow.** `JwtAuthenticationFilter` runs ahead of Spring Security's username/password filter, reads the `Authorization: Bearer` header, and populates the security context. `SecurityConfig` leaves `/auth/**`, `/registration/**`, `/webhook/**`, and CORS preflight open; everything else requires an authenticated principal. Sessions are `STATELESS`, and CORS is restricted to the production front-end origins.

**Ownership checks.** Tutors and students see different slices of the same data, so controllers resolve the caller from the JWT and scope queries to them — hence the `/me`, `/tutor/me`, and `/student/{id}` path conventions rather than unscoped CRUD.

### Roles

`AppUserRole` distinguishes `TUTOR` from `STUDENT`. A tutor creates student accounts under their own account; a student can read and submit only their own homework.

## API surface

| Base | Purpose |
|---|---|
| `POST /auth/**` | Authenticate, issue JWT |
| `/registration/**` | Sign-up, email confirmation |
| `/users/**` | Current profile, profile + password updates, subscription state |
| `/tutors`, `/students/me/**` | Tutor and student management |
| `/lessons/**` | Lesson CRUD, file attachments, PDF generation, AI drafting |
| `/homework/**` | Assignment CRUD, submissions, submission images, AI grading |
| `/product/**` | Stripe Checkout + subscription plans |
| `/webhook/**` | Stripe webhook receiver |

## Running locally

**Prerequisites:** JDK 17, Maven, a PostgreSQL database, and a LaTeX installation if you need PDF export (the Docker image installs `texlive-latex-*`).

No secrets are committed. Every credential is read from the environment, and the app will not start without them:

| Variable | Used for |
|---|---|
| `DB_URL` | JDBC URL for PostgreSQL |
| `DB_PASS` | Database password |
| `JWT_SECRET` | Base64 HMAC key for signing JWTs |
| `SPRING_SECURITY_PASSWORD` | Spring's built-in admin user |
| `GMAIL_PASSWORD` | Gmail SMTP app password |
| `OPENAI_KEY` | OpenAI API key |
| `STRIPE_PRIVATE` | Stripe secret key |
| `WEBHOOK` | Stripe webhook signing secret |

```bash
cd tutor-assistant

export DB_URL='jdbc:postgresql://localhost:5432/verve'
export DB_PASS='...'
export JWT_SECRET='...'          # base64-encoded, 256-bit minimum
export SPRING_SECURITY_PASSWORD='...'
export GMAIL_PASSWORD='...'
export OPENAI_KEY='...'
export STRIPE_PRIVATE='...'
export WEBHOOK='...'

./mvnw spring-boot:run
```

The service listens on `:8080`. Hibernate is configured with `ddl-auto=update`, so the schema is created on first run.

## Docker

```bash
cd tutor-assistant
./mvnw clean package -DskipTests
docker build -t verve-backend .
docker run -p 8080:8080 --env-file .env verve-backend
```

`cloudbuild.yaml` builds the JAR, bakes the image, and pushes it to Google Container Registry for Cloud Run.

## Notes and known rough edges

Kept honest rather than tidied up after the fact:

- `Stripe/` and `Stripe2/` are two generations of the payments integration that were never consolidated.
- Error handling leans on `printStackTrace` in places where a logger belongs.
- Some front-end URLs are hardcoded to `localhost:3000` in email templates instead of being externalised to configuration.
- Test coverage is limited to the Spring context-load smoke test.

## License

**All rights reserved.** This code is published for reference and evaluation
only — it is not open source, and no permission is granted to reuse it in
other work. See [LICENSE](LICENSE).
