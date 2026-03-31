CREATE UNIQUE INDEX ux_task_accepted_bid
    ON bids (task_id)
    WHERE status = 'ACCEPTED';

ALTER TABLE users
    ADD CONSTRAINT chk_balance_nonnegative
        CHECK (balance >= 0);

ALTER TABLE tasks
    ADD CONSTRAINT chk_task_status
        CHECK (status IN ('CREATED','PUBLISHED','IN_PROGRESS','DONE','CONFIRMED','CANCELLED'));

ALTER TABLE bids
    ADD CONSTRAINT chk_bid_status
        CHECK (status IN ('PENDING','ACCEPTED','REJECTED'));

ALTER TABLE users
    ADD CONSTRAINT chk_role
        CHECK (role IN ('CUSTOMER','EXECUTOR','ADMIN'));