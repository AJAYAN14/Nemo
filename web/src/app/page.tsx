"use client";

import React, { useEffect, useState } from "react";
import { DashboardShell } from "@/components/DashboardShell";
import { supabase } from "@/lib/supabase";
import { 
  BookOpen, 
  Library, 
  HelpCircle, 
  ArrowUpRight,
  TrendingUp,
  Loader2
} from "lucide-react";

export default function Home() {
  const [stats, setStats] = useState({
    words: 0,
    grammars: 0,
    questions: 0,
    loading: true
  });

  useEffect(() => {
    async function fetchStats() {
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
    }
    fetchStats();
  }, []);

  if (stats.loading) {
    return (
      <DashboardShell>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', height: '60vh', color: 'var(--text-secondary)' }}>
          <Loader2 className="animate-spin" size={32} />
          <span style={{ marginLeft: '12px' }}>加载统计数据...</span>
        </div>
      </DashboardShell>
    );
  }

  return (
    <DashboardShell>
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))', gap: '24px' }}>
        <div className="card">
          <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '12px' }}>
            <div style={{ padding: '8px', borderRadius: '8px', backgroundColor: 'rgba(0, 113, 227, 0.1)', color: '#0071e3' }}>
              <BookOpen size={24} />
            </div>
          </div>
          <h3 style={{ color: 'var(--text-secondary)', fontSize: '0.9rem', marginBottom: '4px' }}>总词汇量</h3>
          <p style={{ fontSize: '1.75rem', fontWeight: '700' }}>{stats.words.toLocaleString()}</p>
        </div>

        <div className="card">
          <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '12px' }}>
            <div style={{ padding: '8px', borderRadius: '8px', backgroundColor: 'rgba(52, 199, 89, 0.1)', color: '#34c759' }}>
              <Library size={24} />
            </div>
          </div>
          <h3 style={{ color: 'var(--text-secondary)', fontSize: '0.9rem', marginBottom: '4px' }}>语法条目</h3>
          <p style={{ fontSize: '1.75rem', fontWeight: '700' }}>{stats.grammars.toLocaleString()}</p>
        </div>

        <div className="card">
          <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '12px' }}>
            <div style={{ padding: '8px', borderRadius: '8px', backgroundColor: 'rgba(255, 149, 0, 0.1)', color: '#ff9500' }}>
              <HelpCircle size={24} />
            </div>
          </div>
          <h3 style={{ color: 'var(--text-secondary)', fontSize: '0.9rem', marginBottom: '4px' }}>练习题目</h3>
          <p style={{ fontSize: '1.75rem', fontWeight: '700' }}>{stats.questions.toLocaleString()}</p>
        </div>
      </div>

      <div className="card" style={{ marginTop: '24px' }}>
        <h2 style={{ fontSize: '1.1rem', fontWeight: '700', marginBottom: '16px' }}>最近操作记录</h2>
        <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
          {[1, 2, 3].map((i) => (
            <div key={i} style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', paddingBottom: '12px', borderBottom: '1px solid var(--border)' }}>
              <div>
                <p style={{ fontWeight: '500' }}>更新了词条: "素晴らしい"</p>
                <p style={{ fontSize: '0.85rem', color: 'var(--text-secondary)' }}>2026-04-30 14:30</p>
              </div>
              <span style={{ fontSize: '0.8rem', padding: '4px 8px', borderRadius: '4px', backgroundColor: 'var(--bg-tertiary)' }}>词库管理</span>
            </div>
          ))}
        </div>
      </div>
    </DashboardShell>
  );
}
