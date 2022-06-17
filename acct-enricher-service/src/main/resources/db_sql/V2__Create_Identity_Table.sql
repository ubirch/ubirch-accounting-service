CREATE TABLE identity
(
    id          uuid      NOT NULL,
    keycloak_id uuid      NOT NULL,
    tenant_id   uuid      NOT NULL,
    description varchar,
    attributes  varchar,
    created_at  timestamp not null default now(),
    updated_at  timestamp not null default now(),
    PRIMARY KEY (id),
    CONSTRAINT identity_fk_tenant_id
        FOREIGN KEY (tenant_id)
            REFERENCES tenant (id)
);

COMMENT
ON TABLE identity
    IS 'This is the entity that contains the info of the identity used';
