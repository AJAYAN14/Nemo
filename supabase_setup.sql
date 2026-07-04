-- 1. 确保 backups 存储桶存在
INSERT INTO storage.buckets (id, name, public) 
VALUES ('backups', 'backups', false)
ON CONFLICT (id) DO NOTHING;

-- 2. 设置存储桶安全策略 (RLS)
-- 允许用户只读/写自己的备份，以用户ID作为文件夹名前缀

DROP POLICY IF EXISTS "Users can upload their own backups" ON storage.objects;
CREATE POLICY "Users can upload their own backups"
ON storage.objects FOR INSERT
TO authenticated
WITH CHECK (bucket_id = 'backups' AND (auth.uid())::text = (string_to_array(name, '/'))[1]);

DROP POLICY IF EXISTS "Users can read their own backups" ON storage.objects;
CREATE POLICY "Users can read their own backups"
ON storage.objects FOR SELECT
TO authenticated
USING (bucket_id = 'backups' AND (auth.uid())::text = (string_to_array(name, '/'))[1]);

DROP POLICY IF EXISTS "Users can delete their own backups" ON storage.objects;
CREATE POLICY "Users can delete their own backups"
ON storage.objects FOR DELETE
TO authenticated
USING (bucket_id = 'backups' AND (auth.uid())::text = (string_to_array(name, '/'))[1]);

DROP POLICY IF EXISTS "Users can update their own backups" ON storage.objects;
CREATE POLICY "Users can update their own backups"
ON storage.objects FOR UPDATE
TO authenticated
USING (bucket_id = 'backups' AND (auth.uid())::text = (string_to_array(name, '/'))[1]);


-- 3. （可选）创建后端自动清理触发器
-- 当用户在 backups 存储桶中上传新文件时，自动删除该用户超过 3 天的备份，且限制当天的备份最多保留 100 条。
CREATE OR REPLACE FUNCTION public.prune_old_backups()
RETURNS trigger AS $$
DECLARE
    user_folder text;
    deleted_count integer;
BEGIN
    -- 仅监听 backups 桶的插入
    IF NEW.bucket_id = 'backups' THEN
        -- 获取当前插入文件的所有者文件夹（即用户 ID）
        user_folder := (string_to_array(NEW.name, '/'))[1];

        -- 1. 找到并删除该用户超过 3 天的旧文件
        WITH old_files AS (
            SELECT id
            FROM storage.objects
            WHERE bucket_id = 'backups' 
              AND name LIKE user_folder || '/backup_%.json.gz'
              AND created_at < (now() - interval '3 days')
        )
        DELETE FROM storage.objects
        WHERE id IN (SELECT id FROM old_files);

        -- 2. 限制该用户今天的备份条数不超过 100 条
        WITH todays_old_files AS (
            SELECT id
            FROM storage.objects
            WHERE bucket_id = 'backups' 
              AND name LIKE user_folder || '/backup_%.json.gz'
              AND created_at >= date_trunc('day', now())
            ORDER BY created_at DESC
            OFFSET 100
        )
        DELETE FROM storage.objects
        WHERE id IN (SELECT id FROM todays_old_files);

        RAISE LOG 'Pruned old backups for user % (older than 3 days or exceeded 100 today)', user_folder;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- 4. 绑定触发器到 storage.objects
DROP TRIGGER IF EXISTS trigger_prune_old_backups ON storage.objects;

CREATE TRIGGER trigger_prune_old_backups
AFTER INSERT ON storage.objects
FOR EACH ROW
EXECUTE FUNCTION public.prune_old_backups();
