## Аудит змін даних за допомогою JaVers

За основу взято проєкт [gprohorov/inno](https://github.com/gprohorov/inno/tree/inno-requirements):
Spring Boot 4.1 + MongoDB, CRUD для сутності `Item`.

Стандартний аудит Spring Data (`@CreatedDate`, `@LastModifiedBy` тощо) зберігає лише час створення
та останньої зміни, а всі проміжні стани втрачаються. Тому для повної історії змін підключено
JaVers, який зберігає кожну версію об'єкта в MongoDB.

Попередня лабораторна (оптимізація пам'яті) лежить у [SorcerStone/README.md](SorcerStone/README.md).

## Що зроблено

| Файл | Що в ньому |
|---|---|
| `pom.xml` | залежність `javers-spring-boot-starter-mongo` 7.11.9 (версія для Spring Boot 4) |
| `config/AuditConfig.java` | `@EnableMongoAuditing`, бін `AuditorAware` (повертає ім'я користувача ОС), `AuthorProvider` (автор комітів JaVers — `ADMIN`), бін `Javers` з `MongoRepository` |
| `model/AuditMetadata.java` | поля `createdDate`, `createdBy`, `lastModifiedDate`, `lastModifiedBy`; від нього успадковується `Item` |
| `service/ItemService.java` | `@JaversAuditable` на `createItem` і `updateItem`, `@JaversAuditableDelete(entity = Item.class)` на `deleteItemById`, бо цей метод отримує лише `id` |
| `controller/AuditController.java` | ендпоінти для перегляду історії: Changes, Snapshots і Shadows |
| `exception/` | помилки (400, 404, 409) кидаються як винятки й перетворюються у відповідь `ApiResponse` |

Чому помилки зроблено винятками: JaVers записує аргументи методу в аудит, якщо метод завершився
без винятку. Якби сервіс просто повертав відповідь з кодом 400, некоректний запит теж потрапив би
в історію. Тепер в аудит потрапляють лише успішні операції.

При оновленні `createdDate` і `createdBy` беруться з наявного запису, бо в тілі PUT-запиту їх немає
і без цього вони б обнулилися.

## Ендпоінти

CRUD (`/api/v1/items`):

| Метод | URL | Відповідь |
|---|---|---|
| GET | `/api/v1/items` | 200, усі записи |
| GET | `/api/v1/items/{id}` | 200 або 404 |
| GET | `/api/v1/items/page?page=0&size=5` | сторінка записів |
| POST | `/api/v1/items` | 201; 400, якщо порожня назва або від'ємна кількість; 409, якщо `id` уже існує |
| PUT | `/api/v1/items/{id}` | 200; 400; 404 |
| DELETE | `/api/v1/items/{id}` | 200 або 404 |

Аудит (`/api/v1/audit/items`):

| URL | Що повертає |
|---|---|
| `/api/v1/audit/items` | зміни всіх записів |
| `/api/v1/audit/items/{id}/changes` | зміни одного запису: `InitialValueChange`, `ValueChange`, `TerminalValueChange`, `ObjectRemoved` |
| `/api/v1/audit/items/{id}/snapshots` | знімки з типом `INITIAL`, `UPDATE`, `TERMINAL` |
| `/api/v1/audit/items/{id}/shadows` | повний стан запису після кожного коміту |

## Перевірка

Для одного запису послідовно виконано POST, PUT і DELETE, а між ними — POST з порожньою назвою.

1. POST `{"name":"name4","code":"000004","quantity":4}` → 201, запис отримав `createdBy` та `createdDate`.
2. PUT `{"name":"name4-upd","code":"000004","quantity":10}` → 200, `createdDate` не змінився.
3. POST `{"name":""}` → 400 `"Name must not be empty"`, в аудит не потрапив.
4. DELETE → 200.

Результат аудиту для цього запису:

- `/snapshots` — рівно 3 коміти від автора `ADMIN`: `INITIAL` → `UPDATE` → `TERMINAL`.
- `/changes` — `ValueChange` для `name` (`name4` → `name4-upd`) і `quantity` (4 → 10),
  потім `TerminalValueChange` для кожного поля та `ObjectRemoved`. Фрагмент:

```json
{
  "changeType": "ValueChange",
  "globalId": { "entity": "chnu.edu.inno.model.Item", "cdoId": "6ae393f2a40b20cf9c2f1272" },
  "commitMetadata": { "author": "ADMIN", "id": 2.00 },
  "property": "name",
  "left": "name4",
  "right": "name4-upd"
}
```

- `/shadows` — стан запису після створення (`name4`, 4) і після оновлення (`name4-upd`, 10).

Changes показують, яке саме поле змінилося і з якого значення на яке. Shadows показують, як
виглядав увесь об'єкт у кожен момент, тому історію одного запису зручніше дивитися через них.

## Запуск

1. Запустити MongoDB (у терміналі від імені адміністратора): `net start MongoDB`.
2. Запустити застосунок: `./mvnw spring-boot:run`.
3. Swagger: `http://localhost:8080/swagger-ui.html`.

JaVers сам створює в базі `inno` колекції `jv_snapshots` і `jv_head_id`.
