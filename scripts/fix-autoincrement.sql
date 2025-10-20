-- Fix autoincrement sequence for color_combination table
-- This ensures the next insert will use ID 8 instead of trying to reuse existing IDs

-- Update the sqlite_sequence table to set the next ID correctly
UPDATE sqlite_sequence
SET seq = (SELECT MAX(id) FROM color_combination)
WHERE name = 'color_combination';

-- If the entry doesn't exist, insert it
INSERT OR IGNORE INTO sqlite_sequence (name, seq)
VALUES ('color_combination', (SELECT COALESCE(MAX(id), 0) FROM color_combination));

-- Verify the sequence
SELECT name, seq FROM sqlite_sequence WHERE name = 'color_combination';

-- Show current max ID
SELECT MAX(id) as current_max_id FROM color_combination;

-- Test that next insert will work
SELECT 'Next ID will be: ' || (seq + 1) as next_id
FROM sqlite_sequence
WHERE name = 'color_combination';
