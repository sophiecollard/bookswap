# BookSwap backend

## Database schema

```sql
CREATE TYPE user_status AS ENUM (
  'pending_verification',
  'active',
  'banned',
  'deleted',
  'admin'
);

CREATE TABLE users (
  id     TEXT        PRIMARY KEY,
  name   TEXT        NOT NULL,
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
  isbn       TEXT           REFERENCES editions (isbn),
  offered_by TEXT           REFERENCES users (id),
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
  copy_id          TEXT           REFERENCES copies (id),
  requested_by     TEXT           REFERENCES users (id),
  requested_on     TIMESTAMP      NOT NULL,
  status_name      request_status NOT NULL,
  status_timestamp TIMESTAMP
);
```
