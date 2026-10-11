CREATE TABLE reviews (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    booking_item_id BIGINT       NOT NULL,
    reviewer_id     BIGINT       NOT NULL,
    resource_type   VARCHAR(20)  NOT NULL,
    equipment_id    BIGINT       NULL,
    specialist_id   BIGINT       NULL,
    rating          INT          NOT NULL,
    comment         VARCHAR(1000) NULL,
    created_at      DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    -- one review per booked item, enforced by the database
    UNIQUE KEY uk_review_item (booking_item_id),
    KEY idx_reviews_equipment (equipment_id, created_at),
    KEY idx_reviews_specialist (specialist_id, created_at),
    KEY idx_reviews_reviewer (reviewer_id),
    CONSTRAINT chk_review_rating CHECK (rating BETWEEN 1 AND 5),
    CONSTRAINT fk_reviews_item FOREIGN KEY (booking_item_id) REFERENCES booking_items (id),
    CONSTRAINT fk_reviews_reviewer FOREIGN KEY (reviewer_id) REFERENCES users (id),
    CONSTRAINT fk_reviews_equipment FOREIGN KEY (equipment_id) REFERENCES equipment (id),
    CONSTRAINT fk_reviews_specialist FOREIGN KEY (specialist_id) REFERENCES users (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
