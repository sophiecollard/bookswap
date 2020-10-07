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
  'deleted'
);

CREATE TABLE users (
  id     TEXT        PRIMARY KEY,
  name   TEXT        UNIQUE NOT NULL,
  status user_status NOT NULL
);

CREATE TABLE threads (
  id              TEXT      PRIMARY KEY,
  from_user       TEXT      NOT NULL
                            REFERENCES users (id)
                            ON DELETE CASCADE,
  to_user         TEXT      NOT NULL
                            REFERENCES users (id)
                            ON DELETE CASCADE,
  last_message_on TIMESTAMP NOT NULL,
  UNIQUE (from_user, to_user)
);

CREATE INDEX from_user_index ON threads (from_user);
CREATE INDEX to_user_index ON threads (to_user);

CREATE TABLE messages (
  thread_id  TEXT      NOT NULL
                       REFERENCES threads (id)
                       ON DELETE CASCADE,
  created_at TIMESTAMP NOT NULL,
  contents   TEXT      NOT NULL,
  PRIMARY KEY (thread_id, created_at)
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
```
