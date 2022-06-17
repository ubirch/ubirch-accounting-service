CREATE TABLE job
(
    id         uuid      NOT NULL,
    success    boolean,
    query_days varchar,
    started_at timestamp not null default now(),
    ended_at   timestamp,
    comment    varchar,
    created_at timestamp not null default now(),
    updated_at timestamp not null default now(),
    PRIMARY KEY (id)
);

COMMENT
ON TABLE job
    IS 'This is the entity that contains the info of the jobs';

