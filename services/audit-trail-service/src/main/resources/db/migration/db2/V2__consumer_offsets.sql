-- PLAT-0891: we stopped trusting Kafka consumer group offsets after the 2022 Redpanda upgrade
-- reset them and we replayed six days of events into the trail. Offsets are also recorded here and
-- the consumer skips anything at or below the stored offset for its partition.
CREATE TABLE AUDIT_CONSUMER_OFFSET (
    TOPIC             VARCHAR(80)    NOT NULL,
    PARTITION_NO      INTEGER        NOT NULL,
    LAST_OFFSET       BIGINT         NOT NULL,
    UPDATED_AT        TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT PK_AUDIT_CONSUMER_OFFSET PRIMARY KEY (TOPIC, PARTITION_NO)
);
