"use client";

import React, { useState, useEffect } from "react";
import { DashboardShell } from "@/components/DashboardShell";
import { supabase } from "@/lib/supabase";
import { 
  RefreshCw, 
  UploadCloud, 
  Info, 
  Database, 
  Clock, 
  CheckCircle2,
  AlertCircle,
  Loader2
} from "lucide-react";

interface SyncMeta {
  id: number;
  content_version: number;
  min_compatible_version: number;
}

interface Stats {
  wordsCount: number;
  grammarsCount: number;
  lastWordUpdate: string | null;
  lastGrammarUpdate: string | null;
}

export default function SyncPage() {
  const [syncMeta, setSyncMeta] = useState<SyncMeta | null>(null);
  const [stats, setStats] = useState<Stats>({
    wordsCount: 0,
    grammarsCount: 0,
    lastWordUpdate: null,
    lastGrammarUpdate: null
  });
  const [isLoading, setIsLoading] = useState(true);
  const [isUpdating, setIsUpdating] = useState(false);
  const [lastAction, setLastAction] = useState<{ type: 'success' | 'error', message: string } | null>(null);

  const fetchData = async () => {
    setIsLoading(true);
    try {
      // 1. Fetch Sync Meta
      const { data: meta, error: metaErr } = await supabase.from("sync_meta").select("*").single();
      if (metaErr) throw metaErr;
      setSyncMeta(meta);

      // 2. Fetch Stats
      const [wordsRes, grammarsRes] = await Promise.all([
        supabase.from("dictionary_words").select("updated_at", { count: 'exact' }).order("updated_at", { ascending: false }).limit(1),
        supabase.from("dictionary_grammars").select("updated_at", { count: 'exact' }).order("updated_at", { ascending: false }).limit(1)
      ]);

      setStats({
        wordsCount: wordsRes.count || 0,
        grammarsCount: grammarsRes.count || 0,
        lastWordUpdate: wordsRes.data?.[0]?.updated_at || null,
        lastGrammarUpdate: grammarsRes.data?.[0]?.updated_at || null
      });

    } catch (err: any) {
      console.error("Fetch error:", err);
      setLastAction({ type: 'error', message: "加载数据失败: " + err.message });
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, []);

  const handlePushUpdate = async () => {
    if (!syncMeta) return;
    
    if (!confirm(`确定要发布新版本吗？\n当前版本: ${syncMeta.content_version}\n新版本: ${syncMeta.content_version + 1}\n\n发布后所有 App 用户将拉取最新数据。`)) {
      return;
    }

    setIsUpdating(true);
    setLastAction(null);
    try {
      const newVersion = syncMeta.content_version + 1;
      const { error } = await supabase
        .from("sync_meta")
        .update({ content_version: newVersion })
        .eq("id", syncMeta.id);

      if (error) throw error;
      
      setSyncMeta({ ...syncMeta, content_version: newVersion });
      setLastAction({ type: 'success', message: `成功发布新版本 v${newVersion}！` });
    } catch (err: any) {
      setLastAction({ type: 'error', message: "发布失败: " + err.message });
    } finally {
      setIsUpdating(false);
    }
  };

  return (
    <DashboardShell>
      <div className="header" style={{ marginBottom: '32px' }}>
        <h1 className="title" style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
          <RefreshCw size={32} color="var(--accent)" />
          同步控制中心
        </h1>
        <p className="subtitle">管理词库内容版本，控制移动端 App 的数据同步</p>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1.5fr', gap: '24px' }}>
        
        {/* Left Column: Version Info */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
          <div className="card" style={{ textAlign: 'center', padding: '40px 24px' }}>
            <div style={{ fontSize: '0.9rem', color: 'var(--text-secondary)', marginBottom: '8px' }}>当前内容版本</div>
            <div style={{ fontSize: '4rem', fontWeight: '800', color: 'var(--accent)', lineHeight: 1 }}>
              {isLoading ? "..." : syncMeta?.content_version}
            </div>
            <div style={{ marginTop: '24px' }}>
              <button 
                className="button-primary" 
                style={{ width: '100%', height: '48px', fontSize: '1rem', display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '8px' }}
                onClick={handlePushUpdate}
                disabled={isLoading || isUpdating}
              >
                {isUpdating ? <Loader2 size={20} className="animate-spin" /> : <UploadCloud size={20} />}
                发布新版本
              </button>
            </div>
            {lastAction && (
              <div style={{ 
                marginTop: '16px', 
                padding: '12px', 
                borderRadius: '8px', 
                fontSize: '0.85rem',
                backgroundColor: lastAction.type === 'success' ? 'rgba(52, 199, 89, 0.1)' : 'rgba(255, 59, 48, 0.1)',
                color: lastAction.type === 'success' ? '#34C759' : '#FF3B30',
                display: 'flex',
                alignItems: 'center',
                gap: '8px',
                justifyContent: 'center'
              }}>
                {lastAction.type === 'success' ? <CheckCircle2 size={16} /> : <AlertCircle size={16} />}
                {lastAction.message}
              </div>
            )}
          </div>

          <div className="card" style={{ backgroundColor: 'var(--bg-tertiary)' }}>
            <h3 style={{ fontSize: '0.9rem', fontWeight: '600', marginBottom: '12px', display: 'flex', alignItems: 'center', gap: '8px' }}>
              <Info size={16} color="var(--text-tertiary)" />
              什么是内容版本？
            </h3>
            <p style={{ fontSize: '0.85rem', color: 'var(--text-secondary)', lineHeight: '1.6' }}>
              内容版本号（Content Version）是 App 判断是否需要下载新数据的唯一标准。
              <br /><br />
              当您在后台新增、修改或删除了单词/语法后，<b>版本号不会自动增加</b>。您需要手动点击“发布新版本”，App 才会检测到更新并拉取最新的数据库内容。
            </p>
          </div>
        </div>

        {/* Right Column: Data Status */}
        <div className="card">
          <h3 style={{ fontSize: '1.1rem', fontWeight: '700', marginBottom: '24px', display: 'flex', alignItems: 'center', gap: '8px' }}>
            <Database size={20} color="var(--accent)" />
            数据库内容状态
          </h3>

          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px', marginBottom: '32px' }}>
            <div style={{ padding: '20px', backgroundColor: 'var(--bg-secondary)', borderRadius: '12px' }}>
              <div style={{ fontSize: '0.8rem', color: 'var(--text-tertiary)', marginBottom: '4px' }}>总单词量</div>
              <div style={{ fontSize: '1.5rem', fontWeight: '700' }}>{isLoading ? "..." : stats.wordsCount}</div>
            </div>
            <div style={{ padding: '20px', backgroundColor: 'var(--bg-secondary)', borderRadius: '12px' }}>
              <div style={{ fontSize: '0.8rem', color: 'var(--text-tertiary)', marginBottom: '4px' }}>总语法量</div>
              <div style={{ fontSize: '1.5rem', fontWeight: '700' }}>{isLoading ? "..." : stats.grammarsCount}</div>
            </div>
          </div>

          <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '12px', padding: '12px 0', borderBottom: '1px solid var(--border)' }}>
              <Clock size={18} color="var(--text-tertiary)" />
              <div style={{ flex: 1 }}>
                <div style={{ fontSize: '0.85rem', fontWeight: '600' }}>单词库最后变动</div>
                <div style={{ fontSize: '0.8rem', color: 'var(--text-secondary)' }}>
                  {stats.lastWordUpdate ? new Date(stats.lastWordUpdate).toLocaleString() : "从未更新"}
                </div>
              </div>
            </div>

            <div style={{ display: 'flex', alignItems: 'center', gap: '12px', padding: '12px 0', borderBottom: '1px solid var(--border)' }}>
              <Clock size={18} color="var(--text-tertiary)" />
              <div style={{ flex: 1 }}>
                <div style={{ fontSize: '0.85rem', fontWeight: '600' }}>语法库最后变动</div>
                <div style={{ fontSize: '0.8rem', color: 'var(--text-secondary)' }}>
                  {stats.lastGrammarUpdate ? new Date(stats.lastGrammarUpdate).toLocaleString() : "从未更新"}
                </div>
              </div>
            </div>

            <div style={{ display: 'flex', alignItems: 'center', gap: '12px', padding: '12px 0' }}>
              <CheckCircle2 size={18} color="#34C759" />
              <div style={{ flex: 1 }}>
                <div style={{ fontSize: '0.85rem', fontWeight: '600' }}>最小兼容版本 (Min Version)</div>
                <div style={{ fontSize: '0.8rem', color: 'var(--text-secondary)' }}>
                  v{syncMeta?.min_compatible_version}
                </div>
              </div>
            </div>
          </div>

          <div style={{ marginTop: '32px', padding: '16px', backgroundColor: 'rgba(255, 149, 0, 0.05)', border: '1px dashed rgba(255, 149, 0, 0.3)', borderRadius: '12px' }}>
            <div style={{ display: 'flex', gap: '8px', color: '#FF9500' }}>
              <AlertCircle size={18} style={{ flexShrink: 0 }} />
              <div style={{ fontSize: '0.8rem' }}>
                <b>提示：</b> 只有当您认为当前的词库修改已经“准备就绪”并希望同步给用户时，才建议点击发布。频繁发布小更新可能会导致用户流量消耗。
              </div>
            </div>
          </div>
        </div>

      </div>
    </DashboardShell>
  );
}
