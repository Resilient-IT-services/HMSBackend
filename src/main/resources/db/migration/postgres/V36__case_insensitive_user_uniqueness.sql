-- 1. Drop existing constraints
ALTER TABLE public.user_account DROP CONSTRAINT IF EXISTS user_account_username_key;
ALTER TABLE public.user_account DROP CONSTRAINT IF EXISTS user_account_email_key;

-- 2. Resolve duplicates by appending _1, _2, etc. (considering TRIM and LOWER)
DO $$
DECLARE
    r RECORD;
    new_val TEXT;
    counter INTEGER;
BEGIN
    -- Handle username duplicates (case-insensitive and trimmed)
    FOR r IN (
        SELECT id, username FROM public.user_account 
        WHERE id IN (
            SELECT id FROM (
                SELECT id, ROW_NUMBER() OVER (PARTITION BY LOWER(TRIM(username)) ORDER BY created_at, id) as rn
                FROM public.user_account
            ) t WHERE rn > 1
        )
        ORDER BY created_at, id
    ) LOOP
        counter := 1;
        new_val := TRIM(r.username) || '_' || counter;
        -- Ensure the new value doesn't conflict with existing or newly renamed values
        WHILE EXISTS (SELECT 1 FROM public.user_account WHERE LOWER(TRIM(username)) = LOWER(TRIM(new_val))) LOOP
            counter := counter + 1;
            new_val := TRIM(r.username) || '_' || counter;
        END LOOP;
        UPDATE public.user_account SET username = new_val WHERE id = r.id;
    END LOOP;

    -- Handle email duplicates (case-insensitive)
    FOR r IN (
        SELECT id, email FROM public.user_account 
        WHERE email IS NOT NULL AND id IN (
            SELECT id FROM (
                SELECT id, ROW_NUMBER() OVER (PARTITION BY LOWER(TRIM(email)) ORDER BY created_at, id) as rn
                FROM public.user_account
                WHERE email IS NOT NULL
            ) t WHERE rn > 1
        )
        ORDER BY created_at, id
    ) LOOP
        counter := 1;
        new_val := TRIM(r.email) || '_' || counter;
        -- Ensure the new value doesn't conflict
        WHILE EXISTS (SELECT 1 FROM public.user_account WHERE LOWER(TRIM(email)) = LOWER(TRIM(new_val))) LOOP
            counter := counter + 1;
            new_val := TRIM(r.email) || '_' || counter;
        END LOOP;
        UPDATE public.user_account SET email = new_val WHERE id = r.id;
    END LOOP;
END $$;

-- 3. Create unique indexes for case-insensitive and trimmed uniqueness
CREATE UNIQUE INDEX idx_user_account_username_lower_trim ON public.user_account (LOWER(TRIM(username)));
CREATE UNIQUE INDEX idx_user_account_email_lower_trim ON public.user_account (LOWER(TRIM(email)));
