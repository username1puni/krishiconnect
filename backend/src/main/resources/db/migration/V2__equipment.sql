CREATE TABLE equipment (
    id                BIGINT        NOT NULL AUTO_INCREMENT,
    owner_id          BIGINT        NOT NULL,
    title             VARCHAR(120)  NOT NULL,
    category          VARCHAR(30)   NOT NULL,
    brand             VARCHAR(60)   NULL,
    model             VARCHAR(60)   NULL,
    year_of_make      INT           NULL,
    horse_power       INT           NULL,
    description       VARCHAR(1000) NULL,
    price_per_hour    DECIMAL(10,2) NULL,
    price_per_day     DECIMAL(10,2) NULL,
    price_per_acre    DECIMAL(10,2) NULL,
    with_operator     BOOLEAN       NOT NULL DEFAULT FALSE,
    village           VARCHAR(80)   NOT NULL,
    district          VARCHAR(60)   NULL,
    latitude          DOUBLE        NOT NULL,
    longitude         DOUBLE        NOT NULL,
    status            VARCHAR(20)   NOT NULL,
    moderation_status VARCHAR(20)   NOT NULL,
    moderation_note   VARCHAR(255)  NULL,
    rating_sum        INT           NOT NULL DEFAULT 0,
    rating_count      INT           NOT NULL DEFAULT 0,
    created_at        DATETIME(6)   NOT NULL,
    updated_at        DATETIME(6)   NOT NULL,
    version           BIGINT        NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_equipment_owner (owner_id),
    -- browse page without a location: approved + active, optional category, newest first
    KEY idx_equipment_browse (moderation_status, status, category, created_at),
    -- "near me": range scan on latitude, longitude checked from the index (no table lookups)
    KEY idx_equipment_geo (latitude, longitude),
    CONSTRAINT fk_equipment_owner FOREIGN KEY (owner_id) REFERENCES users (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE equipment_photos (
    id           BIGINT      NOT NULL AUTO_INCREMENT,
    equipment_id BIGINT      NOT NULL,
    content_type VARCHAR(40) NOT NULL,
    data         MEDIUMBLOB  NOT NULL,
    sort_order   INT         NOT NULL DEFAULT 0,
    created_at   DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_photo_equipment (equipment_id, sort_order),
    CONSTRAINT fk_photo_equipment FOREIGN KEY (equipment_id) REFERENCES equipment (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
