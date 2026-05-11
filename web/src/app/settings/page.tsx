"use client";

import React, { useState, useEffect } from "react";
import { DashboardShell } from "@/components/DashboardShell";
import { supabase } from "@/lib/supabase";
import { 
  Settings, 
  Save, 
  Smartphone, 
  Info, 
  AlertCircle,
  Loader2,
  CheckCircle2,
  Link as LinkIcon
} from "lucide-react";

interface AppConfig {
  id: number;
  version_code: number;
  version_name: string;
  update_log: string;
  download_url: string;
  is_force: boolean;
  can_close: boolean;
}

export default function SettingsPage() {
  const [config, setConfig] = useState<AppConfig | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isSaving, setIsSaving] = useState(false);
  const [message, setMessage] = useState<{ type: 'success' | 'error', text: string } | null>(null);

  useEffect(() => {
    fetchConfig();
  }, []);

  const fetchConfig = async () => {
    setIsLoading(true);
    try {
      // 获取最新的配置（ID 最大的通常是最新发布的）
      const { data, error } = await supabase
        .from("app_config")
        .select("*")
        .order("id", { ascending: false })
        .limit(1)
        .single();

      if (error && error.code !== 'PGRST116') throw error; // PGRST116 means no rows found
      
      if (data) {
        setConfig(data);
      } else {
        // 如果表是空的，初始化一个默认值
        setConfig({
          id: 0,
          version_code: 1,
          version_name: "1.0.0",
          update_log: "",
          download_url: "",
          is_force: false,
          can_close: true
        });
      }
    } catch (err: any) {
      console.error("Fetch error:", err);
      setMessage({ type: 'error', text: "加载配置失败: " + err.message });
    } finally {
      setIsLoading(false);
    }
  };

  const handleSave = async () => {
    if (!config) return;
    setIsSaving(true);
    setMessage(null);
    try {
      const { id, ...saveData } = config;
      
      let error;
      if (id === 0) {
        // 新增
        const { error: err } = await supabase.from("app_config").insert([saveData]);
        error = err;
      } else {
        // 更新
        const { error: err } = await supabase.from("app_config").update(saveData).eq("id", id);
        error = err;
      }

      if (error) throw error;
      setMessage({ type: 'success', text: "配置保存成功！" });
      setTimeout(() => setMessage(null), 3000);
      fetchConfig(); // 重新加载以确保同步
    } catch (err: any) {
      setMessage({ type: 'error', text: "保存失败: " + err.message });
    } finally {
      setIsSaving(false);
    }
  };

  if (isLoading) {
    return (
      <DashboardShell>
        <div style={{ display: 'flex', justifyContent: 'center', padding: '100px' }}>
          <Loader2 className="animate-spin" size={40} color="var(--accent)" />
        </div>
      </DashboardShell>
    );
  }

  return (
    <DashboardShell>
      <div className="header" style={{ marginBottom: '32px' }}>
        <h1 className="title" style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
          <Settings size={32} color="var(--accent)" />
          系统设置
        </h1>
        <p className="subtitle">管理 App 版本更新、下载链接及全局系统参数</p>
      </div>

      <div style={{ maxWidth: '800px' }}>
        <div className="card" style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
          
          {/* Version Section */}
          <div>
            <h3 style={{ fontSize: '1rem', fontWeight: '700', marginBottom: '16px', display: 'flex', alignItems: 'center', gap: '8px' }}>
              <Smartphone size={20} color="var(--accent)" />
              App 版本配置
            </h3>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px' }}>
              <div>
                <label style={{ fontSize: '0.85rem', color: 'var(--text-secondary)', marginBottom: '6px', display: 'block' }}>版本名称 (Version Name)</label>
                <input 
                  className="input" 
                  style={{ width: '100%' }}
                  value={config?.version_name}
                  onChange={(e) => setConfig(prev => prev ? { ...prev, version_name: e.target.value } : null)}
                  placeholder="例如: 1.0.0"
                />
              </div>
              <div>
                <label style={{ fontSize: '0.85rem', color: 'var(--text-secondary)', marginBottom: '6px', display: 'block' }}>版本代码 (Version Code)</label>
                <input 
                  className="input" 
                  type="number"
                  style={{ width: '100%' }}
                  value={config?.version_code}
                  onChange={(e) => setConfig(prev => prev ? { ...prev, version_code: parseInt(e.target.value) || 0 } : null)}
                  placeholder="例如: 1"
                />
              </div>
            </div>
          </div>

          {/* Download URL */}
          <div>
            <label style={{ fontSize: '0.85rem', color: 'var(--text-secondary)', marginBottom: '6px', display: 'block' }}>
              安装包下载链接 (APK URL)
            </label>
            <div style={{ position: 'relative' }}>
              <LinkIcon size={16} style={{ position: 'absolute', left: '12px', top: '50%', transform: 'translateY(-50%)', color: 'var(--text-tertiary)' }} />
              <input 
                className="input" 
                style={{ width: '100%', paddingLeft: '36px' }}
                value={config?.download_url}
                onChange={(e) => setConfig(prev => prev ? { ...prev, download_url: e.target.value } : null)}
                placeholder="https://example.com/nemo-latest.apk"
              />
            </div>
          </div>

          {/* Update Log */}
          <div>
            <label style={{ fontSize: '0.85rem', color: 'var(--text-secondary)', marginBottom: '6px', display: 'block' }}>更新日志 (Update Log)</label>
            <textarea 
              className="input" 
              style={{ width: '100%', minHeight: '120px', resize: 'vertical', lineHeight: '1.6' }}
              value={config?.update_log}
              onChange={(e) => setConfig(prev => prev ? { ...prev, update_log: e.target.value } : null)}
              placeholder="1. 新增功能 A\n2. 优化了 B 的加载速度\n3. 修复了已知问题..."
            />
          </div>

          {/* Policy Toggles */}
          <div style={{ display: 'flex', gap: '32px', padding: '16px', backgroundColor: 'var(--bg-secondary)', borderRadius: '12px' }}>
            <label style={{ display: 'flex', alignItems: 'center', gap: '10px', cursor: 'pointer' }}>
              <input 
                type="checkbox" 
                checked={config?.is_force}
                onChange={(e) => setConfig(prev => prev ? { ...prev, is_force: e.target.checked } : null)}
                style={{ width: '18px', height: '18px' }}
              />
              <span style={{ fontSize: '0.9rem', fontWeight: '500' }}>强制更新</span>
            </label>
            <label style={{ display: 'flex', alignItems: 'center', gap: '10px', cursor: 'pointer' }}>
              <input 
                type="checkbox" 
                checked={config?.can_close}
                onChange={(e) => setConfig(prev => prev ? { ...prev, can_close: e.target.checked } : null)}
                style={{ width: '18px', height: '18px' }}
              />
              <span style={{ fontSize: '0.9rem', fontWeight: '500' }}>允许用户关闭弹窗</span>
            </label>
          </div>

          <div style={{ borderTop: '1px solid var(--border)', paddingTop: '24px', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
              {message && (
                <div style={{ 
                  display: 'flex', 
                  alignItems: 'center', 
                  gap: '6px', 
                  color: message.type === 'success' ? '#34C759' : '#FF3B30',
                  fontSize: '0.9rem'
                }}>
                  {message.type === 'success' ? <CheckCircle2 size={18} /> : <AlertCircle size={18} />}
                  {message.text}
                </div>
              )}
            </div>
            <button 
              className="button-primary" 
              style={{ padding: '10px 24px', display: 'flex', alignItems: 'center', gap: '8px' }}
              onClick={handleSave}
              disabled={isSaving}
            >
              {isSaving ? <Loader2 size={18} className="animate-spin" /> : <Save size={18} />}
              保存全局配置
            </button>
          </div>

        </div>

        <div className="card" style={{ marginTop: '24px', backgroundColor: 'rgba(0, 122, 255, 0.05)', border: '1px dashed rgba(0, 122, 255, 0.3)' }}>
          <div style={{ display: 'flex', gap: '12px' }}>
            <Info size={20} color="var(--accent)" style={{ flexShrink: 0 }} />
            <div style={{ fontSize: '0.85rem', color: 'var(--text-secondary)', lineHeight: '1.6' }}>
              <b>配置说明：</b>
              <br />
              这里的修改将实时影响所有 App 用户的版本更新检测逻辑。
              <br />
              - <b>Version Code</b>：必须是一个递增的整数（如当前是 1，下次更新应设为 2）。
              <br />
              - <b>强制更新</b>：开启后，App 端会显示不可关闭的更新弹窗。
            </div>
          </div>
        </div>
      </div>
    </DashboardShell>
  );
}
