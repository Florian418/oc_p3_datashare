CREATE TABLE users (
    id                  bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    email               varchar(255) NOT NULL UNIQUE,
    user_password_hash  varchar(255) NOT NULL,
    created_at          timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE file_shares (
    id                    bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name                  varchar(255) NOT NULL,
    mime                  varchar(127) NOT NULL,
    size                  bigint NOT NULL,
    created_at            timestamptz NOT NULL DEFAULT now(),
    expires_at            timestamptz NOT NULL,
    share_password_hash   varchar(255),
    token                 uuid NOT NULL UNIQUE,
    user_id               bigint REFERENCES users (id) ON DELETE SET NULL
);

CREATE TABLE tags (
    id              bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    label           varchar(50) NOT NULL,
    file_share_id   bigint NOT NULL REFERENCES file_shares (id) ON DELETE CASCADE
);
