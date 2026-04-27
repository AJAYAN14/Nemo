-- 2026-04-05 同步冲突修复迁移脚本
-- 目标：为错题本、语法错题本、收藏夹添加业务主键唯一性约束，支持 UPSERT 原子操作，防止同步冲突。

-- 1. 为单词错题表添加唯一性约束 (user_id, word_id)
-- 确保每个用户对每个单词在错题本中仅保留一条记录
DO $$ 
BEGIN 
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'user_wrong_answers_user_id_word_id_key') THEN
        ALTER TABLE public.user_wrong_answers 
        ADD CONSTRAINT user_wrong_answers_user_id_word_id_key UNIQUE (user_id, word_id);
    END IF;
END $$;

-- 2. 为语法错题表添加唯一性约束 (user_id, grammar_id)
DO $$ 
BEGIN 
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'user_grammar_wrong_answers_user_id_grammar_id_key') THEN
        ALTER TABLE public.user_grammar_wrong_answers 
        ADD CONSTRAINT user_grammar_wrong_answers_user_id_grammar_id_key UNIQUE (user_id, grammar_id);
    END IF;
END $$;

-- 3. 为收藏题库添加唯一性约束 (user_id, timestamp) 
-- 注意：收藏表在同步逻辑中使用 user_id 和 timestamp 作为冲突检查键
DO $$ 
BEGIN 
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'favorite_questions_user_id_timestamp_key') THEN
        ALTER TABLE public.favorite_questions 
        ADD CONSTRAINT favorite_questions_user_id_timestamp_key UNIQUE (user_id, "timestamp");
    END IF;
END $$;

-- 4. 添加常用索引以优化同步查询性能
CREATE INDEX IF NOT EXISTS idx_user_wrong_answers_timestamp ON public.user_wrong_answers(user_id, timestamp DESC);
CREATE INDEX IF NOT EXISTS idx_user_grammar_wrong_answers_timestamp ON public.user_grammar_wrong_answers(user_id, timestamp DESC);
CREATE INDEX IF NOT EXISTS idx_user_study_records_date ON public.user_study_records(user_id, date DESC);
CREATE INDEX IF NOT EXISTS idx_user_word_states_learning_date ON public.user_word_states(user_id, first_learned_date DESC) WHERE first_learned_date IS NOT NULL;

COMMENT ON CONSTRAINT user_wrong_answers_user_id_word_id_key ON public.user_wrong_answers IS '确保每个用户对每个单词在错题本中仅保留一条有效记录，支持同步 UPSERT';
COMMENT ON CONSTRAINT user_grammar_wrong_answers_user_id_grammar_id_key ON public.user_grammar_wrong_answers IS '确保每个用户对每个语法在错题本中仅保留一条有效记录，支持同步 UPSERT';
