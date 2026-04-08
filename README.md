# Java Project 99 — Task Management API

[![Actions Status](https://github.com/valentin-osadchii/java-project-99/actions/workflows/hexlet-check.yml/badge.svg)](https://github.com/valentin-osadchii/java-project-99/actions)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=valentin-osadchii_java-project-99&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=valentin-osadchii_java-project-99)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=valentin-osadchii_java-project-99&metric=coverage)](https://sonarcloud.io/summary/new_code?id=valentin-osadchii_java-project-99)

## Описание проекта

[Страница проекта](https://java-project-99-production-9a01.up.railway.app)

Приложение для создания и управления задачами, пользователями, статусами и метками. 
Задачи можно назначатьь на исполнителя, фильтровать по названию, исполнителю, статусу и меткам, 
а также просматривать с пагинацией. 
Статусы задач описывают их жизненный цикл — от черновика до завершения. 
Метки позволяют категоризировать задачи и гибко организовывать рабочее пространство. 


## Технологии

| Категория | Технология |
|-----------|------------|
| **Язык** | Java 21 |
| **Фреймворк** | Spring Boot 4.0.4 |
| **База данных** | H2 (dev), PostgreSQL (prod) |
| **ORM** | Spring Data JPA |
| **Безопасность** | Spring Security + JWT |
| **Валидация** | Hibernate Validator |
| **Маппинг** | MapStruct |
| **Сборка** | Gradle (Kotlin DSL) |
| **Тесты** | JUnit 5, JaCoCo |

## Запуск приложения

### Требования

- Java 21+
- Gradle (включён в проект через wrapper)

### Команды

```bash
# Собрать проект
./gradlew build

# Запустить приложение
./gradlew bootRun

# Запустить тесты
./gradlew test
```

После запуска API доступен по адресу: `http://localhost:8080`

## Функционал приложения

### Аутентификация

| Метод | Эндпоинт | Описание |
|-------|----------|----------|
| `POST` | `/api/login` | Войти и получить JWT-токен (действует 1 час) |

Тело запроса: `{ "username": "email@example.com", "password": "..." }`

### Пользователи (`/api/users`)

Полный CRUD пользователей с валидацией данных.

| Метод | Эндпоинт | Описание |
|-------|----------|----------|
| `GET` | `/api/users` | Список всех пользователей |
| `GET` | `/api/users/{id}` | Получить пользователя по ID |
| `POST` | `/api/users` | Зарегистрировать нового пользователя |
| `PUT` | `/api/users/{id}` | Обновить данные пользователя |
| `DELETE` | `/api/users/{id}` | Удалить пользователя |

Пароли хранятся в закодированном виде. Регистрация (`POST`) доступна без аутентификации.

### Задачи (`/api/tasks`)

Управление задачами с поддержкой статусов, исполнителей и меток.

| Метод | Эндпоинт | Описание |
|-------|----------|----------|
| `GET` | `/api/tasks` | Список задач с фильтрацией и пагинацией |
| `GET` | `/api/tasks/{id}` | Получить задачу по ID |
| `POST` | `/api/tasks` | Создать задачу |
| `PUT` | `/api/tasks/{id}` | Обновить задачу |
| `DELETE` | `/api/tasks/{id}` | Удалить задачу |

**Параметры фильтрации** (query string для `GET /api/tasks`):

| Параметр | Описание |
|----------|----------|
| `titleCont` | Поиск по подстроке в названии |
| `assigneeId` | Фильтр по исполнителю |
| `status` | Фильтр по slug статуса (например, `draft`) |
| `labelId` | Фильтр по метке |
| `offset` | Номер страницы (по умолчанию 1) |
| `limit` | Размер страницы (по умолчанию 10) |

При создании задачи без указания статуса автоматически присваивается статус со slug `draft`.

### Статусы задач (`/api/task_statuses`)

Управление статусами задач (например: `draft`, `in_progress`, `done`).

| Метод | Эндпоинт | Описание |
|-------|----------|----------|
| `GET` | `/api/task_statuses` | Список всех статусов |
| `GET` | `/api/task_statuses/{id}` | Получить статус по ID |
| `POST` | `/api/task_statuses` | Создать новый статус |
| `PUT` | `/api/task_statuses/{id}` | Обновить статус |
| `DELETE` | `/api/task_statuses/{id}` | Удалить статус |

Нельзя удалить статус, если на него ссылаются задачи.

### Метки (`/api/labels`)

Управление метками для категоризации задач (многие-ко-многим).

| Метод | Эндпоинт | Описание |
|-------|----------|----------|
| `GET` | `/api/labels` | Список всех меток |
| `GET` | `/api/labels/{id}` | Получить метку по ID |
| `POST` | `/api/labels` | Создать метку |
| `PUT` | `/api/labels/{id}` | Обновить метку |
| `DELETE` | `/api/labels/{id}` | Удалить метку |

### Безопасность

- JWT-токены с временем жизни 1 час
- Stateless-сессии (без серверного хранения сессий)
- Публичные эндпоинты: `/api/login`, `POST /api/users`, Swagger UI
- Все остальные операции требуют валидный JWT-токен

### Документация API

После запуска Swagger UI доступен по адресу: `http://localhost:8080/swagger-ui.html`

## Дополнительная информация

- [Отчёт о покрытии кода](https://sonarcloud.io/summary/new_code?id=valentin-osadchii_java-project-99)
- [Статус сборки](https://github.com/valentin-osadchii/java-project-99/actions)
