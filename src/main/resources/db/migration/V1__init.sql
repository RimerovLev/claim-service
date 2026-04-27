create table users
(
    id         bigserial primary key,
    full_name  varchar(255) not null,
    email      varchar(255) not null unique,
    created_at timestamptz
);

create table claims
(
    id                  bigserial primary key,
    user_id             bigint      not null references users (id),
    status              varchar(64) not null,
    eligible            boolean,
    compensation_amount integer,
    created_at          timestamptz
);

create table flights
(
    id            bigserial primary key,
    claim_id      bigint      not null unique references claims (id) on delete cascade,
    flight_number varchar(64) not null,
    flight_date   date        not null,
    route_from    varchar(8)  not null,
    route_to      varchar(8)  not null,
    airline       varchar(255) not null,
    booking_ref   varchar(64) not null,
    distance_km   integer     not null
);

create table eu_context
(
    id                bigserial primary key,
    claim_id          bigint      not null unique references claims (id) on delete cascade,
    departure_from_eu boolean     not null,
    eu_carrier        boolean     not null
);

create table issues
(
    id                          bigserial primary key,
    claim_id                    bigint      not null unique references claims (id) on delete cascade,
    type                        varchar(64) not null,
    delay_minutes               integer,
    cancellation_notice_days    integer,
    extraordinary_circumstances boolean     not null
);

create table documents
(
    id          text primary key,
    claim_id    bigint       not null references claims (id) on delete cascade,
    type        varchar(64)  not null,
    url         text         not null,
    uploaded_at timestamptz,
    description text,
    file_name   varchar(255),
    mime_type   varchar(255),
    file_size   bigint,
    storage_key varchar(255),
    created_by  bigint,
    created_at  timestamptz  not null default now(),
    updated_at  timestamptz  not null default now(),
    deleted_at  timestamptz
);

create table claim_events
(
    id         bigserial primary key,
    claim_id   bigint      not null references claims (id) on delete cascade,
    type       varchar(64) not null,
    payload    text        not null,
    created_at timestamptz
);

create index idx_claims_user_id on claims (user_id);
create index idx_documents_claim_id on documents (claim_id);
create unique index uq_documents_storage_key on documents (storage_key);
create index idx_documents_deleted_at on documents (deleted_at);
create index idx_claim_events_claim_id on claim_events (claim_id);
