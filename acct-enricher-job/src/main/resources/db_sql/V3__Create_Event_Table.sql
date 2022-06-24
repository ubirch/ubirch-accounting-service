CREATE TABLE event
(
    identity_id uuid      NOT NULL,
    tenant_id   uuid      NOT NULL,
    category    varchar,
    date        date      not null,
    count       int,
    created_at  timestamp not null default now(),
    updated_at  timestamp not null default now(),
    PRIMARY KEY (identity_id, tenant_id, category, date),
    CONSTRAINT event_fk_tenant_id
        FOREIGN KEY (tenant_id)
            REFERENCES tenant (id)
);

COMMENT
ON TABLE event
    IS 'This is the entity that contains the info of the events';

