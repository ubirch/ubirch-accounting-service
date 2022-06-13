CREATE TABLE tenant
(
    id         uuid      NOT NULL,
    parent_id  uuid,
    group_name varchar   NOT NULL,
    attributes varchar,
    created_at timestamp not null default now(),
    updated_at timestamp not null default now(),
    PRIMARY KEY (id),
    CONSTRAINT tenant_fk_tenant_parent_id
        FOREIGN KEY (parent_id)
            REFERENCES tenant (id)
);

COMMENT
ON TABLE tenant
    IS 'This is the entity that contains the info of the tenant used';
