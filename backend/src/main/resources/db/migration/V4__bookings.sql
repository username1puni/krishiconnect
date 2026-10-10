CREATE TABLE bookings (
    id           BIGINT        NOT NULL AUTO_INCREMENT,
    seeker_id    BIGINT        NOT NULL,
    kind         VARCHAR(10)   NOT NULL,
    status       VARCHAR(20)   NOT NULL,
    start_at     DATETIME      NOT NULL,
    end_at       DATETIME      NOT NULL,
    total_amount DECIMAL(12,2) NOT NULL,
    note         VARCHAR(500)  NULL,
    expires_at   DATETIME(6)   NULL,
    created_at   DATETIME(6)   NOT NULL,
    confirmed_at DATETIME(6)   NULL,
    closed_at    DATETIME(6)   NULL,
    version      BIGINT        NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_bookings_seeker (seeker_id, created_at),
    -- the expiry job asks "pending and expires_at < now" every few seconds
    KEY idx_bookings_expiry (status, expires_at),
    CONSTRAINT fk_bookings_seeker FOREIGN KEY (seeker_id) REFERENCES users (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- One row per thing booked. A bundle (tractor + driver) is one booking with two items.
CREATE TABLE booking_items (
    id            BIGINT        NOT NULL AUTO_INCREMENT,
    booking_id    BIGINT        NOT NULL,
    resource_type VARCHAR(20)   NOT NULL,
    equipment_id  BIGINT        NULL,
    specialist_id BIGINT        NULL,
    provider_id   BIGINT        NOT NULL,
    start_at      DATETIME      NOT NULL,
    end_at        DATETIME      NOT NULL,
    price_unit    VARCHAR(10)   NOT NULL,
    quantity      DECIMAL(8,2)  NOT NULL,
    unit_price    DECIMAL(10,2) NOT NULL,
    amount        DECIMAL(12,2) NOT NULL,
    status        VARCHAR(20)   NOT NULL,
    responded_at  DATETIME(6)   NULL,
    PRIMARY KEY (id),
    KEY idx_items_booking (booking_id),
    -- overlap check: equipment_id = ? AND start_at < :end AND end_at > :start, answered from the index
    KEY idx_items_equipment_time (equipment_id, start_at, end_at, status),
    KEY idx_items_specialist_time (specialist_id, start_at, end_at, status),
    KEY idx_items_provider (provider_id, status),
    CONSTRAINT fk_items_booking FOREIGN KEY (booking_id) REFERENCES bookings (id),
    CONSTRAINT fk_items_equipment FOREIGN KEY (equipment_id) REFERENCES equipment (id),
    CONSTRAINT fk_items_specialist FOREIGN KEY (specialist_id) REFERENCES users (id),
    CONSTRAINT fk_items_provider FOREIGN KEY (provider_id) REFERENCES users (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- One row per booked hour per resource. The unique key is the database level guarantee that
-- the same tractor or person can never be in two live bookings for the same hour, even if a
-- code path forgot to take the row lock. Rows are removed when a booking is rejected,
-- cancelled or expires.
CREATE TABLE availability_slots (
    id              BIGINT      NOT NULL AUTO_INCREMENT,
    resource_type   VARCHAR(20) NOT NULL,
    resource_id     BIGINT      NOT NULL,
    slot_start      DATETIME    NOT NULL,
    booking_item_id BIGINT      NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_slot (resource_type, resource_id, slot_start),
    KEY idx_slot_item (booking_item_id),
    CONSTRAINT fk_slot_item FOREIGN KEY (booking_item_id) REFERENCES booking_items (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
