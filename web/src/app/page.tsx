"use client";

import React, { useEffect, useState } from "react";
import { DashboardShell } from "@/components/DashboardShell";
import { supabase } from "@/lib/supabase";
import { 
  BookOpen, 
  Library, 
  HelpCircle, 
  Clock,
  Loader2,
  MessageSquare,
  Bell,
  RefreshCcw
} from "lucide-react";

interface Activity {
  id: string | number;
  type: 'word' | 'grammar' | 'report' | 'notification';
  title: string;
  time: string;
  label: string;
}

export default function Home() {
  const [stats, setStats] = useState({
    words: 0,
    grammars: 0,
    questions: 0,
    loading: true
  });
  const [activities, setActivities] = useState<Activity[]>([]);
  const [refreshing, setRefreshing] = useState(false);

  async function fetchAllData() {
    if (!refreshing) setStats(prev => ({ ...prev, loading: true }));
    
    try {
      // 1. Fetch Stats
      const [wordsRes, grammarRes, questionRes] = await Promise.all([
        supabase.from("dictionary_words").select("*", { count: 'exact', head: true }),
        supabase.from("dictionary_grammars").select("*", { count: 'exact', head: true }),
        supabase.from("grammar_questions").select("*", { count: 'exact', head: true })
      ]);

      setStats({
        words: wordsRes.count || 0,
        grammars: grammarRes.count || 0,
        questions: questionRes.count || 0,
        loading: false
      });

      // 2. Fetch Recent Activities (Aggregation)
      const [recentWords, recentGrammars, recentReports, recentNotifs] = await Promise.all([
        supabase.from("dictionary_words").select("id, japanese, updated_at").order("updated_at", { ascending: false }).limit(3),
        supabase.from("dictionary_grammars").select("id, title, updated_at").order("updated_at", { ascending: false }).limit(3),
        supabase.from("content_reports").select("id, item_id, item_type, created_at").order("created_at", { ascending: false }).limit(3),
        supabase.from("notifications").select("id, title, created_at").order("created_at", { ascending: false }).limit(3)
      ]);

      const allActivities: Activity[] = [];

      recentWords.data?.forEach(w => {
        allActivities.push({
          id: `w-${w.id}`,
          type: 'word',
          title: `更新了词条: "${w.japanese}"`,
          time: w.updated_at,
          label: '词库管理'
        });
      });

      recentGrammars.data?.forEach(g => {
        allActivities.push({
          id: `g-${g.id}`,
          type: 'grammar',
          title: `更新了语法: "${g.title}"`,
          time: g.updated_at,
          label: '语法管理'
        });
      });

      recentReports.data?.forEach(r => {
        allActivities.push({
          id: `r-${r.id}`,
          type: 'report',
          title: `收到新的内容纠错反馈 (ID: ${r.item_id})`,
          time: r.created_at,
          label: '反馈管理'
        });
      });

      recentNotifs.data?.forEach(n => {
        allActivities.push({
          id: `n-${n.id}`,
          type: 'notification',
          title: `发布了新公告: "${n.title}"`,
          time: n.created_at,
          label: '通知管理'
        });
      });

      // Sort by time desc
      allActivities.sort((a, b) => new Date(b.time).getTime() - new Date(a.time).getTime());
      setActivities(allActivities.slice(0, 8)); // Keep top 8

    } catch (error) {
      console.error("Dashboard data fetch error:", error);
    } finally {
      setStats(prev => ({ ...prev, loading: false }));
      setRefreshing(false);
    }
  }

  useEffect(() => {
    fetchAllData();
  }, []);

  const handleRefresh = () => {
    setRefreshing(true);
    fetchAllData();
  };

  if (stats.loading && !refreshing) {
    return (
      <DashboardShell>
        <div style={{ display: 'flex', alignItems: 'center', flexDirection: 'column', justifyContent: 'center', height: '60vh', color: 'var(--text-secondary)' }}>
          <Loader2 className="animate-spin" size={40} color="var(--accent)" />
          <span style={{ marginTop: '16px', fontSize: '0.9rem' }}>正在初始化工作台...</span>
        </div>
      </DashboardShell>
    );
  }

  return (
    <DashboardShell>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '32px' }}>
        <div>
          <h1 className="title">数据概览</h1>
          <p className="subtitle">欢迎回来，这是 Nemo 系统的实时运行状态</p>
        </div>
        <button 
          className="button-secondary" 
          onClick={handleRefresh} 
          disabled={refreshing}
          style={{ display: 'flex', alignItems: 'center', gap: '8px' }}
        >
          <RefreshCcw size={18} className={refreshing ? "animate-spin" : ""} />
          刷新数据
        </button>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))', gap: '24px' }}>
        <div className="card" style={{ borderLeft: '4px solid #0071e3' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '12px' }}>
            <div style={{ padding: '8px', borderRadius: '8px', backgroundColor: 'rgba(0, 113, 227, 0.1)', color: '#0071e3' }}>
              <BookOpen size={24} />
            </div>
          </div>
          <h3 style={{ color: 'var(--text-secondary)', fontSize: '0.9rem', marginBottom: '4px' }}>总词汇量</h3>
          <p style={{ fontSize: '2rem', fontWeight: '800' }}>{stats.words.toLocaleString()}</p>
        </div>

        <div className="card" style={{ borderLeft: '4px solid #34c759' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '12px' }}>
            <div style={{ padding: '8px', borderRadius: '8px', backgroundColor: 'rgba(52, 199, 89, 0.1)', color: '#34c759' }}>
              <Library size={24} />
            </div>
          </div>
          <h3 style={{ color: 'var(--text-secondary)', fontSize: '0.9rem', marginBottom: '4px' }}>语法条目</h3>
          <p style={{ fontSize: '2rem', fontWeight: '800' }}>{stats.grammars.toLocaleString()}</p>
        </div>

        <div className="card" style={{ borderLeft: '4px solid #ff9500' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '12px' }}>
            <div style={{ padding: '8px', borderRadius: '8px', backgroundColor: 'rgba(255, 149, 0, 0.1)', color: '#ff9500' }}>
              <HelpCircle size={24} />
            </div>
          </div>
          <h3 style={{ color: 'var(--text-secondary)', fontSize: '0.9rem', marginBottom: '4px' }}>练习题目</h3>
          <p style={{ fontSize: '2rem', fontWeight: '800' }}>{stats.questions.toLocaleString()}</p>
        </div>
      </div>

      <div className="card" style={{ marginTop: '24px', padding: 0, overflow: 'hidden' }}>
        <div style={{ padding: '20px 24px', borderBottom: '1px solid var(--border)', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <h2 style={{ fontSize: '1.1rem', fontWeight: '700', display: 'flex', alignItems: 'center', gap: '8px' }}>
            <Clock size={20} color="var(--accent)" />
            最近活动记录
          </h2>
          <span style={{ fontSize: '0.8rem', color: 'var(--text-tertiary)' }}>展示最近 8 条业务变动</span>
        </div>
        
        <div style={{ display: 'flex', flexDirection: 'column' }}>
          {activities.length === 0 ? (
            <div style={{ padding: '60px', textAlign: 'center', color: 'var(--text-tertiary)' }}>
              暂无最近操作记录
            </div>
          ) : (
            activities.map((activity) => (
              <div 
                key={activity.id} 
                style={{ 
                  display: 'flex', 
                  alignItems: 'center', 
                  justifyContent: 'space-between', 
                  padding: '16px 24px', 
                  borderBottom: '1px solid var(--border)',
                  transition: 'background-color 0.2s'
                }}
              >
                <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
                  <div style={{ 
                    width: '36px', 
                    height: '36px', 
                    borderRadius: '50%', 
                    backgroundColor: 'var(--bg-secondary)', 
                    display: 'flex', 
                    alignItems: 'center', 
                    justifyContent: 'center' 
                  }}>
                    {activity.type === 'word' && <BookOpen size={18} color="#0071e3" />}
                    {activity.type === 'grammar' && <Library size={18} color="#34c759" />}
                    {activity.type === 'report' && <MessageSquare size={18} color="#ff3b30" />}
                    {activity.type === 'notification' && <Bell size={18} color="#ff9500" />}
                  </div>
                  <div>
                    <p style={{ fontWeight: '600', fontSize: '0.95rem' }}>{activity.title}</p>
                    <p style={{ fontSize: '0.85rem', color: 'var(--text-tertiary)', marginTop: '2px' }}>
                      {new Date(activity.time).toLocaleString('zh-CN', { hour12: false })}
                    </p>
                  </div>
                </div>
                <span style={{ 
                  fontSize: '0.75rem', 
                  padding: '4px 10px', 
                  borderRadius: '6px', 
                  backgroundColor: 'var(--bg-tertiary)',
                  color: 'var(--text-secondary)',
                  fontWeight: '500',
                  border: '1px solid var(--border)'
                }}>
                  {activity.label}
                </span>
              </div>
            ))
          )}
        </div>
        
        <div style={{ padding: '16px 24px', backgroundColor: 'var(--bg-tertiary)', textAlign: 'center' }}>
          <p style={{ fontSize: '0.85rem', color: 'var(--text-tertiary)' }}>
            数据由系统实时聚合生成
          </p>
        </div>
      </div>
    </DashboardShell>
  );
}
