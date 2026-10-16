CREATE TABLE monitor_incidents (
    id UUID PRIMARY KEY,
    monitor_id UUID NOT NULL REFERENCES monitors(id) ON DELETE CASCADE,
    started_at TIMESTAMPTZ NOT NULL,
    last_observed_at TIMESTAMPTZ NOT NULL,
    resolved_at TIMESTAMPTZ,
    CONSTRAINT chk_monitor_incidents_observed_after_start
        CHECK (last_observed_at >= started_at),
    CONSTRAINT chk_monitor_incidents_resolved_after_start
        CHECK (resolved_at IS NULL OR resolved_at >= started_at)
);

CREATE INDEX idx_monitor_incidents_monitor_started_at
    ON monitor_incidents(monitor_id, started_at DESC);

CREATE UNIQUE INDEX uq_monitor_incidents_one_open_per_monitor
    ON monitor_incidents(monitor_id)
    WHERE resolved_at IS NULL;
