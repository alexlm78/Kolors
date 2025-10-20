-- Migration script to remove CHECK constraint on position in color_in_combination
-- This allows colors to have any position >= 1 instead of 1-4
-- SQLite doesn't support ALTER TABLE DROP CONSTRAINT, so we need to recreate the table

-- Step 1: Create temporary table with the new schema (no upper limit on position)
CREATE TABLE IF NOT EXISTS color_in_combination_new (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    hex_value VARCHAR(6) NOT NULL,
    position INTEGER NOT NULL CHECK (position >= 1),
    combination_id BIGINT NOT NULL
);

-- Step 2: Copy data from old table to new table
INSERT INTO color_in_combination_new (id, hex_value, position, combination_id)
SELECT id, hex_value, position, combination_id
FROM color_in_combination;

-- Step 3: Drop the old table
DROP TABLE color_in_combination;

-- Step 4: Rename new table to original name
ALTER TABLE color_in_combination_new RENAME TO color_in_combination;

-- Step 5: Recreate indexes (removing duplicates)
CREATE INDEX IF NOT EXISTS idx_color_hex_value ON color_in_combination(hex_value);
CREATE INDEX IF NOT EXISTS idx_color_combination_id ON color_in_combination(combination_id);
CREATE INDEX IF NOT EXISTS idx_color_position ON color_in_combination(position);
CREATE INDEX IF NOT EXISTS idx_color_combination_position ON color_in_combination(combination_id, position);

-- Verify the migration
SELECT 'Migration completed successfully. Color positions can now be 1-N.' AS status;
SELECT COUNT(*) AS total_colors FROM color_in_combination;
