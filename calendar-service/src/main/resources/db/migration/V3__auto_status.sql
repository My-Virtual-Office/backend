-- Auto-status-from-calendar bookkeeping: whether the owner's Desk status was already
-- flipped to "In a meeting" for this event, and whether it was later reverted. Both
-- are one-shot flags so the scheduler applies/clears each event exactly once.
ALTER TABLE calendar_event ADD COLUMN status_applied BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE calendar_event ADD COLUMN status_cleared BOOLEAN NOT NULL DEFAULT FALSE;

-- Fast lookups for the scheduler's two queries.
CREATE INDEX idx_calendar_event_status_apply
    ON calendar_event (status_applied, busy, start_time, end_time);
CREATE INDEX idx_calendar_event_status_clear
    ON calendar_event (status_applied, status_cleared, end_time);
