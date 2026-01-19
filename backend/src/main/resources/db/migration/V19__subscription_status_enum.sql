-- V19: Subscription Status Enum (State Machine)

CREATE TYPE subscription_status AS ENUM (
    'INCOMPLETE',
    'INCOMPLETE_EXPIRED',
    'TRIALING',
    'ACTIVE',
    'PAST_DUE',
    'CANCELED',
    'UNPAID'
);

ALTER TABLE subscriptions ALTER COLUMN status TYPE subscription_status USING status::subscription_status;
