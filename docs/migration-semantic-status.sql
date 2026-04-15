-- Migration: Add SEMANTIC_PROCESSING status to documents table constraint
-- Date: 2026-04-15
-- Description: The documents table has a CHECK constraint on status column that
--              needs to include the new SEMANTIC_PROCESSING status.

-- Drop the old constraint (constraint name may vary, check with: \d documents)
ALTER TABLE documents DROP CONSTRAINT IF EXISTS documents_status_check;

-- Add new constraint with all statuses including SEMANTIC_PROCESSING
ALTER TABLE documents
    ADD CONSTRAINT documents_status_check
        CHECK (status IN
               ('PROCESSING', 'SEMANTIC_PROCESSING', 'CHUNKED', 'EMBEDDING', 'COMPLETED', 'FAILED', 'DELETED'));

-- Alternative approach if the above doesn't work:
-- The constraint might have been created with a different name.
-- First, find the actual constraint name:
-- SELECT conname FROM pg_constraint WHERE conrelid = 'documents'::regclass AND contype = 'c';
-- Then drop it with: ALTER TABLE documents DROP CONSTRAINT <constraint_name>;