-- Per-event colour for the shared calendar. Nullable: existing events and any
-- created without a colour render in the default blue. A short hex string
-- ("#1164a3") or a named palette key — the client owns the palette.
ALTER TABLE calendar_event ADD COLUMN color VARCHAR(16);
