-- Optional meeting/call link (Zoom, Meet, a room URL, whatever the team uses).
-- Nullable: most seeded/legacy events have none.
ALTER TABLE calendar_event ADD COLUMN meeting_link VARCHAR(500);
