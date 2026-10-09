CREATE TABLE users (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    full_name      VARCHAR(100) NOT NULL,
    phone          VARCHAR(15)  NOT NULL,
    email          VARCHAR(120) NULL,
    password_hash  VARCHAR(100) NOT NULL,
    role           VARCHAR(20)  NOT NULL,
    village        VARCHAR(80)  NULL,
    district       VARCHAR(60)  NULL,
    latitude       DOUBLE       NULL,
    longitude      DOUBLE       NULL,
    preferred_lang VARCHAR(5)   NOT NULL DEFAULT 'en',
    crops          VARCHAR(200) NULL,
    verified       BOOLEAN      NOT NULL DEFAULT FALSE,
    enabled        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at     DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_phone (phone),
    UNIQUE KEY uk_users_email (email),
    -- admin user list filters by role and verification state
    KEY idx_users_role (role, verified),
    -- bounding box pre-filter for "specialists near me"
    KEY idx_users_geo (latitude, longitude)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE specialist_profiles (
    user_id          BIGINT        NOT NULL,
    skill            VARCHAR(30)   NOT NULL,
    bio              VARCHAR(500)  NULL,
    experience_years INT           NOT NULL DEFAULT 0,
    rate_per_hour    DECIMAL(10,2) NULL,
    rate_per_day     DECIMAL(10,2) NULL,
    languages        VARCHAR(100)  NULL,
    available        BOOLEAN       NOT NULL DEFAULT TRUE,
    rating_sum       INT           NOT NULL DEFAULT 0,
    rating_count     INT           NOT NULL DEFAULT 0,
    updated_at       DATETIME(6)   NOT NULL,
    PRIMARY KEY (user_id),
    KEY idx_specialist_skill (skill, available),
    CONSTRAINT fk_specialist_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
