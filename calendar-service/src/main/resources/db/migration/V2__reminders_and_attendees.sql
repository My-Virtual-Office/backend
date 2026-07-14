-- Meeting reminders + attendees.

ALTER TABLE calendar_event ADD COLUMN creator_email    TEXT;
ALTER TABLE calendar_event ADD COLUMN reminder_minutes INTEGER;
ALTER TABLE calendar_event ADD COLUMN reminder_sent_at TIMESTAMPTZ;

-- Invited attendees: userId -> email (everyone reminded/notified alongside the owner).
CREATE TABLE calendar_event_attendee (
    event_id          BIGINT NOT NULL REFERENCES calendar_event (id) ON DELETE CASCADE,
    attendee_user_id  BIGINT NOT NULL,
    attendee_email    TEXT,
    PRIMARY KEY (event_id, attendee_user_id)
);

-- Scheduler scan: events with a reminder set that haven't fired yet.
CREATE INDEX idx_calendar_event_reminder
    ON calendar_event (reminder_sent_at, reminder_minutes, start_time);
