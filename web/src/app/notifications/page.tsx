"use client";

import React, { useState, useEffect } from "react";
import { DashboardShell } from "@/components/DashboardShell";
import { supabase } from "@/lib/supabase";
import { 
  Bell, 
  Plus, 
  Edit3, 
  CheckCircle2, 
  XCircle, 
  Loader2,
  AlertCircle
} from "lucide-react";
import { Modal } from "@/components/Modal";

interface Notification {
  id: string;
  title: string;
  body: string;
  active: boolean;
  created_at: string;
}

export default function NotificationsPage() {
  const [notifications, setNotifications] = useState<Notification[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingNotif, setEditingNotif] = useState<Notification | null>(null);
  
  // Form state
  const [formData, setFormData] = useState({
    title: "",
    body: "",
    active: true
  });
  const [isSaving, setIsSaving] = useState(false);

  const fetchNotifications = async () => {
    setIsLoading(true);
    try {
      const { data, error } = await supabase
        .from("notifications")
        .select("*")
        .order("created_at", { ascending: false });

      if (error) throw error;
      setNotifications(data || []);
    } catch (err) {
      console.error("Fetch error:", err);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchNotifications();
  }, []);

  const handleOpenModal = (notif?: Notification) => {
    if (notif) {
      setEditingNotif(notif);
      setFormData({
        title: notif.title,
        body: notif.body,
        active: notif.active
      });
    } else {
      setEditingNotif(null);
      setFormData({
        title: "",
        body: "",
        active: true
      });
    }
    setIsModalOpen(true);
  };

  const handleSave = async () => {
    if (!formData.title || !formData.body) {
      alert("请填写标题和内容");
      return;
    }
    setIsSaving(true);
    try {
      if (editingNotif) {
        const { error } = await supabase
          .from("notifications")
          .update(formData)
          .eq("id", editingNotif.id);
        if (error) throw error;
      } else {
        const { error } = await supabase
          .from("notifications")
          .insert([{ ...formData, id: Date.now().toString() }]); // Use timestamp as ID if not serial
        if (error) throw error;
      }
      setIsModalOpen(false);
      fetchNotifications();
    } catch (err: any) {
      alert("保存失败: " + err.message);
    } finally {
      setIsSaving(false);
    }
  };

  const handleToggleActive = async (notif: Notification) => {
    try {
      const { error } = await supabase
        .from("notifications")
        .update({ active: !notif.active })
        .eq("id", notif.id);
      if (error) throw error;
      setNotifications(notifications.map(n => n.id === notif.id ? { ...n, active: !n.active } : n));
    } catch (err) {
      alert("更新状态失败");
    }
  };


  return (
    <DashboardShell>
      <div className="header" style={{ marginBottom: '32px', display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
        <div>
          <h1 className="title" style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
            <Bell size={32} color="var(--accent)" />
            通知公告管理
          </h1>
          <p className="subtitle">向所有 App 用户发布系统公告和重要通知</p>
        </div>
        <button 
          className="button-primary" 
          style={{ display: 'flex', alignItems: 'center', gap: '8px', padding: '10px 20px' }}
          onClick={() => handleOpenModal()}
        >
          <Plus size={20} /> 发布新公告
        </button>
      </div>

      {isLoading ? (
        <div style={{ display: 'flex', justifyContent: 'center', padding: '100px' }}>
          <Loader2 className="animate-spin" size={40} color="var(--accent)" />
        </div>
      ) : (
        <div className="grid" style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(350px, 1fr))', gap: '20px' }}>
          {notifications.length === 0 ? (
            <div className="card" style={{ gridColumn: '1 / -1', textAlign: 'center', padding: '60px', color: 'var(--text-tertiary)' }}>
              <Bell size={48} style={{ marginBottom: '16px', opacity: 0.2, margin: '0 auto' }} />
              <p>暂无任何公告通知</p>
            </div>
          ) : notifications.map((notif) => (
            <div key={notif.id} className="card" style={{ position: 'relative', display: 'flex', flexDirection: 'column', gap: '12px', borderLeft: notif.active ? '4px solid #34C759' : '4px solid var(--text-tertiary)' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                <div style={{ 
                  display: 'flex', 
                  alignItems: 'center', 
                  gap: '6px', 
                  fontSize: '0.75rem', 
                  fontWeight: '600',
                  color: notif.active ? '#34C759' : 'var(--text-tertiary)'
                }}>
                  {notif.active ? <CheckCircle2 size={14} /> : <XCircle size={14} />}
                  {notif.active ? "已生效" : "已下线"}
                </div>
                <div style={{ display: 'flex', gap: '8px' }}>
                  <button 
                    style={{ background: 'none', border: 'none', color: 'var(--text-secondary)', cursor: 'pointer' }}
                    onClick={() => handleOpenModal(notif)}
                  >
                    <Edit3 size={16} />
                  </button>
                </div>
              </div>
              
              <h3 style={{ fontSize: '1.1rem', fontWeight: '700' }}>{notif.title}</h3>
              <p style={{ 
                fontSize: '0.9rem', 
                color: 'var(--text-secondary)', 
                lineHeight: '1.5',
                overflow: 'hidden',
                display: '-webkit-box',
                WebkitLineClamp: 3,
                WebkitBoxOrient: 'vertical',
                minHeight: '4.5em'
              }}>
                {notif.body}
              </p>

              <div style={{ 
                marginTop: '12px', 
                paddingTop: '12px', 
                borderTop: '1px solid var(--border)',
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'center'
              }}>
                <span style={{ fontSize: '0.75rem', color: 'var(--text-tertiary)' }}>
                  发布于 {new Date(notif.created_at).toLocaleDateString()}
                </span>
                <button 
                  className={notif.active ? "button-secondary" : "button-primary"}
                  style={{ padding: '4px 12px', fontSize: '0.8rem', backgroundColor: notif.active ? 'transparent' : '#34C759' }}
                  onClick={() => handleToggleActive(notif)}
                >
                  {notif.active ? "下线" : "上线"}
                </button>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Edit/Create Modal */}
      <Modal isOpen={isModalOpen} onClose={() => setIsModalOpen(false)} title={editingNotif ? "编辑公告" : "发布新公告"} width="600px">
        <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
          <div>
            <label style={{ fontSize: '0.85rem', color: 'var(--text-secondary)', marginBottom: '6px', display: 'block' }}>公告标题</label>
            <input 
              className="input" 
              style={{ width: '100%' }}
              value={formData.title}
              onChange={(e) => setFormData({ ...formData, title: e.target.value })}
              placeholder="请输入简洁明了的标题"
            />
          </div>
          <div>
            <label style={{ fontSize: '0.85rem', color: 'var(--text-secondary)', marginBottom: '6px', display: 'block' }}>公告正文</label>
            <textarea 
              className="input" 
              style={{ width: '100%', minHeight: '150px', resize: 'vertical' }}
              value={formData.body}
              onChange={(e) => setFormData({ ...formData, body: e.target.value })}
              placeholder="请输入公告的具体内容..."
            />
          </div>
          <div>
            <label style={{ display: 'flex', alignItems: 'center', gap: '8px', cursor: 'pointer' }}>
              <input 
                type="checkbox" 
                checked={formData.active}
                onChange={(e) => setFormData({ ...formData, active: e.target.checked })}
                style={{ width: '18px', height: '18px' }}
              />
              <span style={{ fontSize: '0.9rem', fontWeight: '500' }}>立即发布给所有用户</span>
            </label>
          </div>

          <div style={{ marginTop: '12px', display: 'flex', gap: '12px' }}>
            <button className="button-primary" style={{ flex: 1, height: '42px' }} onClick={handleSave} disabled={isSaving}>
              {isSaving ? <Loader2 size={18} className="animate-spin" /> : "确认发布"}
            </button>
            <button className="input" style={{ flex: 0.3, height: '42px' }} onClick={() => setIsModalOpen(false)}>取消</button>
          </div>
        </div>
      </Modal>
    </DashboardShell>
  );
}
