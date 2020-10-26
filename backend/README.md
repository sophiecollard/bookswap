# BookSwap backend

## REST API

##### Get Copy

_Method and endpoint_

```
GET /v1/copies/{copy_id}
```

_Sample response body_

```json
{
  "id": "159c1470-0578-11eb-adc1-0242ac120002",
  "isbn": "9781784875435",
  "offered_by": "247e681c-0578-11eb-adc1-0242ac120002",
  "offered_on": "2019-07-13T13:00:00Z",
  "condition": "brand_new",
  "status": "available"
}
```

The `condition` field may take on one of the following values: `brand_new`, `good`, `some_signs_of_user`, `poor`.

The `status` field may take on one of the following values: `available`, `reserved`, `swapped`, `withdrawn`.

##### List Copies by ISBN

_Method and endpoint_

```
GET /v1/copies?isbn={isbn}
```

_Query parameters_

The endpoint accepts the following query parameters:
  * `?isbn={isbn}`
  * `&page_offset={YYYY-MM-DDThh:mm:ss.sss±hh:mm}`
  * `&page_size={page_size}`

The `isbn` query parameter is mandatory and used to filter copies by ISBN while the `page_offset` and `page_size` 
parameters are optional and used for pagination.

_Sample response body_

```json
[
  {
    "id": "159c1470-0578-11eb-adc1-0242ac120002",
    "isbn": "9781784875435",
    "offered_by": "247e681c-0578-11eb-adc1-0242ac120002",
    "offered_on": "2019-07-13T13:00:00Z",
    "condition": "brand_new",
    "status": "available"
  },
  ...
]
```

##### List Copies by owner

_Method and endpoint_

```
GET /v1/copies?offered_by={user_id}
```

_Query parameters_

The endpoint accepts the following query parameters:
  * `?offered_by={user_id}`
  * `&page_offset={YYYY-MM-DDThh:mm:ss.sss±hh:mm}`
  * `&page_size={page_size}`

The `offered_by` query parameter is mandatory and used to filter copies by the user offering them while the 
`page_offset` and `page_size` parameters are optional and used for pagination.

_Sample response body_

```json
[
  {
    "id": "159c1470-0578-11eb-adc1-0242ac120002",
    "isbn": "9781784875435",
    "offered_by": "247e681c-0578-11eb-adc1-0242ac120002",
    "offered_on": "2019-07-13T13:00:00Z",
    "condition": "brand_new",
    "status": "available"
  },
  ...
]
```

##### Create a Copy

_Method and endpoint_

```
POST /v1/copies
```

_Request body_

```json
{
  "isbn": "{isbn}",
  "condition": "{brand_new|good|some_signs_of_use|poor}"
}
```

##### Update a Copy

_Method and endpoint_

```
PUT /v1/copies/{copy_id}
```

_Request body_

```json
{
  "condition": "{brand_new|good|some_signs_of_use|poor}"
}
```

##### Withdraw a Copy

_Method and endpoint_

```
DELETE /v1/copies/{copy_id}
```

_Request body_

None

##### Get a CopyRequest

_Method and endpoint_

```
GET /v1/requests/{request_id}
```

_Sample response body_

```json
{
  "id": "034da7c2-0576-11eb-adc1-0242ac120002",
  "copy_id": "0f5963e4-0576-11eb-adc1-0242ac120002",
  "requested_by": "1571ec56-0576-11eb-adc1-0242ac120002",
  "requested_on": "2019-07-13T13:00:00Z",
  "status": {
    "code": "accepted",
    "timestamp": "2019-09-26T17:00:00Z"
  }
}
```

The `status.code` field may take on one of the following values: `pending`, `accepted`, `on_waiting_list`, `rejected`,
`fulfilled`, `cancelled`.

The `status.timestamp` field is optional and will be null for status code `pending`.

##### Create a CopyRequest

_Method and endpoint_

```
POST /v1/requests
```

_Request body_

```json
{
  "copy_id": "{copy_id}"
}
```

##### Cancel a CopyRequest

_Method and endpoint_

```
DELETE /v1/requests/{request_id}
```

_Request body_

None

##### Accept, reject or fulfill a CopyRequest

_Method and endpoint_

```
PATCH /v1/requests/{request_id}
```

_Request body_

```json
{
  "command": "{accept|reject|mark_as_fulfilled}"
}
```

## Database schema

```sql
CREATE TYPE user_status AS ENUM (
  'pending_verification',
  'active',
  'admin',
  'banned',
  'pending_deletion'
);

CREATE TABLE users (
  id     TEXT        PRIMARY KEY,
  name   TEXT        UNIQUE NOT NULL,
  status user_status NOT NULL
);

CREATE TABLE authors (
  id   TEXT PRIMARY KEY,
  name TEXT NOT NULL
);

CREATE TABLE editions (
  isbn             TEXT  PRIMARY KEY,
  title            TEXT  NOT NULL,
  author_ids       JSONB NOT NULL,
  publisher_id     TEXT,
  publication_date DATE
);

CREATE TYPE copy_condition AS ENUM (
  'brand_new',
  'good',
  'some_signs_of_use',
  'poor'
);

CREATE TYPE copy_status AS ENUM (
  'available',
  'reserved',
  'swapped',
  'withdrawn'
);

CREATE TABLE copies (
  id         TEXT           PRIMARY KEY,
  isbn       TEXT           NOT NULL
                            REFERENCES editions (isbn),
  offered_by TEXT           NOT NULL
                            REFERENCES users (id),
  offered_on TIMESTAMP      NOT NULL,
  condition  copy_condition NOT NULL,
  status     copy_status    NOT NULL
);

CREATE INDEX copy_by_isbn_index ON copies (isbn, status, offered_on);
CREATE INDEX copy_by_offered_by_index ON copies (offered_by, offered_on);

CREATE TYPE request_status AS ENUM (
  'pending',
  'accepted',
  'on_waiting_list',
  'rejected',
  'fulfilled',
  'cancelled'
);

CREATE TABLE copy_requests (
  id               TEXT           PRIMARY KEY,
  copy_id          TEXT           NOT NULL
                                  REFERENCES copies (id),
  requested_by     TEXT           NOT NULL
                                  REFERENCES users (id),
  requested_on     TIMESTAMP      NOT NULL,
  status_name      request_status NOT NULL,
  status_timestamp TIMESTAMP
);

CREATE INDEX request_copy_id_index ON copies (copy_id, offered_on);
CREATE INDEX request_requested_by_index ON copies (offered_by, offered_on);
```
