CREATE TABLE parcels (
    id                  BIGSERIAL PRIMARY KEY,
    tracking_number     VARCHAR(12)    NOT NULL UNIQUE,
    customer_id         BIGINT,
    merchant_id         BIGINT,
    status              VARCHAR(20)    NOT NULL DEFAULT 'CREATED',

    pickup_line1        VARCHAR(200)   NOT NULL,
    pickup_city         VARCHAR(100)   NOT NULL,
    pickup_district     VARCHAR(100)   NOT NULL,
    dropoff_line1       VARCHAR(200)   NOT NULL,
    dropoff_city        VARCHAR(100)   NOT NULL,
    dropoff_district    VARCHAR(100)   NOT NULL,

    receiver_name       VARCHAR(100)   NOT NULL,
    receiver_phone      VARCHAR(20)    NOT NULL,

    size                VARCHAR(1)     NOT NULL,
    weight_kg           NUMERIC(6,2)   NOT NULL,
    fee                 NUMERIC(10,2)  NOT NULL DEFAULT 0.00,

    created_at          TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ    NOT NULL DEFAULT now()
);

CREATE INDEX idx_parcels_customer ON parcels (customer_id, created_at);
CREATE INDEX idx_parcels_merchant ON parcels (merchant_id, created_at);
CREATE INDEX idx_parcels_status ON parcels (status);