-- Migration script to remove CHECK constraint on color_count
-- This allows combinations to have 1-N colors instead of 2-4
-- SQLite doesn't support ALTER TABLE DROP CONSTRAINT, so we need to recreate the table

-- Step 1: Create temporary table with the new schema (no CHECK constraint on color_count)
CREATE TABLE IF NOT EXISTS color_combination_new (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    color_count INTEGER NOT NULL CHECK (color_count >= 1)
);

-- Step 2: Copy data from old table to new table
INSERT INTO color_combination_new (id, name, created_at, color_count)
SELECT id, name, created_at, color_count
FROM color_combination;

-- Step 3: Drop the old table
DROP TABLE color_combination;

-- Step 4: Rename new table to original name
ALTER TABLE color_combination_new RENAME TO color_combination;

-- Step 5: Recreate indexes
CREATE INDEX IF NOT EXISTS idx_comb_name ON color_combination(name);
CREATE INDEX IF NOT EXISTS idx_comb_created ON color_combination(created_at);
CREATE INDEX IF NOT EXISTS idx_comb_count ON color_combination(color_count);
CREATE INDEX IF NOT EXISTS idx_comb_name_count ON color_combination(name, color_count);

-- Verify the migration
SELECT 'Migration completed successfully. Color combinations can now have 1-N colors.' AS status;
SELECT COUNT(*) AS total_combinations FROM color_combination;
