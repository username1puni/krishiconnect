CREATE TABLE requirements (
    id                BIGINT        NOT NULL AUTO_INCREMENT,
    seeker_id         BIGINT        NOT NULL,
    title             VARCHAR(120)  NOT NULL,
    description       VARCHAR(1000) NULL,
    need_type         VARCHAR(20)   NOT NULL,
    category          VARCHAR(30)   NULL,
    skill             VARCHAR(30)   NULL,
    crop              VARCHAR(30)   NULL,
    area_acres        DECIMAL(8,2)  NULL,
    start_at          DATETIME      NOT NULL,
    end_at            DATETIME      NOT NULL,
    village           VARCHAR(80)   NOT NULL,
    district          VARCHAR(60)   NULL,
    latitude          DOUBLE        NOT NULL,
    longitude         DOUBLE        NOT NULL,
    budget            DECIMAL(10,2) NULL,
    status            VARCHAR(20)   NOT NULL,
    accepted_quote_id BIGINT        NULL,
    created_at        DATETIME(6)   NOT NULL,
    version           BIGINT        NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    -- the board lists open requirements, newest first
    KEY idx_requirements_board (status, created_at),
    KEY idx_requirements_seeker (seeker_id, created_at),
    KEY idx_requirements_geo (latitude, longitude),
    CONSTRAINT fk_requirements_seeker FOREIGN KEY (seeker_id) REFERENCES users (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE quotes (
    id             BIGINT        NOT NULL AUTO_INCREMENT,
    requirement_id BIGINT        NOT NULL,
    provider_id    BIGINT        NOT NULL,
    equipment_id   BIGINT        NULL,
    amount         DECIMAL(10,2) NOT NULL,
    message        VARCHAR(500)  NULL,
    status         VARCHAR(20)   NOT NULL,
    booking_id     BIGINT        NULL,
    created_at     DATETIME(6)   NOT NULL,
    responded_at   DATETIME(6)   NULL,
    PRIMARY KEY (id),
    -- one quote per provider per requirement
    UNIQUE KEY uk_quote_provider (requirement_id, provider_id),
    KEY idx_quotes_provider (provider_id, status),
    CONSTRAINT fk_quotes_requirement FOREIGN KEY (requirement_id) REFERENCES requirements (id),
    CONSTRAINT fk_quotes_provider FOREIGN KEY (provider_id) REFERENCES users (id),
    CONSTRAINT fk_quotes_equipment FOREIGN KEY (equipment_id) REFERENCES equipment (id),
    CONSTRAINT fk_quotes_booking FOREIGN KEY (booking_id) REFERENCES bookings (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
