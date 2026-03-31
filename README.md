# SsuBench

Сервис, где заказчики размещают задания, исполнители откликаются на них, а оплата выполненной работы производится виртуальными баллами.

## Стек

- Java 25
- Spring Boot
- PostgreSQL
- Docker

## Переменные окружения

Конфигурация в `.env`. Пример:

```env
FLYWAY_URL=
POSTGRES_URL=
POSTGRES_DB=
POSTGRES_USER=
POSTGRES_PASSWORD=
JWT_SECRET=
HTTP_TIMEOUT_CONNECTION=
HTTP_TIMEOUT_KEEP_ALIVE=
HTTP_SHUTDOWN_TIMEOUT=
HTTP_TIMEOUT_REQUEST=
```

## Запуск

Подготовить окружение, заполнить .env:

```bash
cp .env.example .env
```

Запустить докер:

```bash
docker compose up
```

Миграции применятся автоматически.

Запустить приложение:

```bash
./gradlew test
./gradlew bootRun
```

## Примеры curl

API доступен по адресу `http://localhost:8080`

### Регистрация 

```bash
curl -X POST "$BASE_URL/auth/register?email=$CUSTOMER_EMAIL&password=$PASSWORD&role=CUSTOMER"

curl -X POST "$BASE_URL/auth/register?email=$EXECUTOR_EMAIL&password=$PASSWORD&role=EXECUTOR"

curl -X POST "$BASE_URL/auth/register?email=$ADMIN_EMAIL&password=$PASSWORD&role=ADMIN"
```

### Вход 

```bash
export CUSTOMER_TOKEN=$(
  curl -s -X POST "$BASE_URL/auth/login?email=$CUSTOMER_EMAIL&password=$PASSWORD"
)

export EXECUTOR_TOKEN=$(
  curl -s -X POST "$BASE_URL/auth/login?email=$EXECUTOR_EMAIL&password=$PASSWORD"
)

export ADMIN_TOKEN=$(
  curl -s -X POST "$BASE_URL/auth/login?email=$ADMIN_EMAIL&password=$PASSWORD"
)
```

### Создание задачи

```bash
export TASK_ID=$(
  curl -s -X POST "$BASE_URL/tasks?title=Подготовить%20отчёт&description=Собрать%20отчёт%20по%20тестированию&reward=50" \
    -H "Authorization: Bearer $CUSTOMER_TOKEN"
)
```

### Публикация задачи

```bash
curl -X POST "$BASE_URL/tasks/$TASK_ID/publish" \
  -H "Authorization: Bearer $CUSTOMER_TOKEN"
```

### Отклик исполнителя на задачу

```bash
export BID_ID=$(
  curl -s -X POST "$BASE_URL/bids?taskId=$TASK_ID" \
    -H "Authorization: Bearer $EXECUTOR_TOKEN"
)
```

### Список откликов по задаче

```bash
curl -X GET "$BASE_URL/bids/task/$TASK_ID?limit=20&offset=0" \
  -H "Authorization: Bearer $CUSTOMER_TOKEN"
```

### Выбор исполнителя

```bash
curl -X POST "$BASE_URL/bids/$BID_ID/select?taskId=$TASK_ID" \
  -H "Authorization: Bearer $CUSTOMER_TOKEN"
```

### Отметка о выполнении

```bash
curl -X POST "$BASE_URL/bids/task/$TASK_ID/complete" \
  -H "Authorization: Bearer $EXECUTOR_TOKEN"
```

### Подтверждение выполнения и перевод баллов

```bash
curl -X POST "$BASE_URL/payments/confirm?taskId=$TASK_ID" \
  -H "Authorization: Bearer $CUSTOMER_TOKEN"
```

### Получение списка задач

```bash
curl -X GET "$BASE_URL/tasks?limit=20&offset=0" \
  -H "Authorization: Bearer $CUSTOMER_TOKEN"
```

### Cписок пользователей

```bash
curl -X GET "$BASE_URL/users?limit=20&offset=0" \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

### Блокировка и разблокировка пользователя

```bash
curl -X POST "$BASE_URL/users/<USER_ID>/block" \
  -H "Authorization: Bearer $ADMIN_TOKEN"

curl -X POST "$BASE_URL/users/<USER_ID>/unblock" \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

## OpenAPI

Описание API: `openapi.yaml`

Swagger после запуска доступен по адресу `http://localhost:8080/swagger-ui/index.html#/`
