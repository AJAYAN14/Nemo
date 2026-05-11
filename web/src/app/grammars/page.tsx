"use client";

import React, { useEffect, useState } from "react";
import { DashboardShell } from "@/components/DashboardShell";
import { supabase } from "@/lib/supabase";
import { Search, Plus, Filter, Edit2, RefreshCcw } from "lucide-react";
import { GrammarModal } from "@/components/GrammarModal";

interface Grammar {
  id: number;
  raw_id?: string;
  title: string;
  level: string;
  content: any;
  is_delisted: boolean;
  updated_at: string;
}

export default function GrammarsPage() {
  const [grammars, setGrammars] = useState<Grammar[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState("");
  const [levelFilter, setLevelFilter] = useState("All");
  const [statusFilter, setStatusFilter] = useState("All");
  const [levelCounts, setLevelCounts] = useState<Record<string, number>>({});
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingGrammar, setEditingGrammar] = useState<Grammar | undefined>(undefined);

  useEffect(() => {
    fetchGrammars();
    fetchLevelCounts();
  }, [searchTerm, levelFilter, statusFilter]);

  async function fetchLevelCounts() {
    const levels = ["N1", "N2", "N3", "N4", "N5"];
    const counts: Record<string, number> = {};
    
    await Promise.all(levels.map(async (lvl) => {
      const { count } = await supabase
        .from("dictionary_grammars")
        .select("*", { count: 'exact', head: true })
        .eq("level", lvl);
      counts[lvl] = count || 0;
    }));
    
    setLevelCounts(counts);
  }

  async function fetchGrammars() {
    setLoading(true);
    let query = supabase
      .from("dictionary_grammars")
      .select("*")
      .order("id", { ascending: true })
      .limit(100);

    if (searchTerm) {
      query = query.ilike("title", `%${searchTerm}%`);
    }

    if (levelFilter !== "All") {
      query = query.eq("level", levelFilter);
    }

    if (statusFilter === "Active") {
      query = query.eq("is_delisted", false);
    } else if (statusFilter === "Delisted") {
      query = query.eq("is_delisted", true);
    }

    const { data, error } = await query;

    if (data) setGrammars(data);
    setLoading(false);
  }

  const handleEdit = (grammar: Grammar) => {
    setEditingGrammar(grammar);
    setIsModalOpen(true);
  };

  const handleAdd = () => {
    setEditingGrammar(undefined);
    setIsModalOpen(true);
  };

  const handleStatusChange = async (id: number, isDelisted: boolean) => {
    const { error } = await supabase
      .from("dictionary_grammars")
      .update({ is_delisted: isDelisted })
      .eq("id", id);
    
    if (!error) {
      fetchGrammars();
    } else {
      alert("更新状态失败: " + error.message);
    }
  };

  return (
    <DashboardShell>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '24px' }}>
        <div style={{ display: 'flex', gap: '12px', flex: 1 }}>
          <div style={{ position: 'relative', flex: 0.4 }}>
            <Search size={18} style={{ position: 'absolute', left: '12px', top: '50%', transform: 'translateY(-50%)', color: 'var(--text-tertiary)' }} />
            <input 
              type="text" 
              placeholder="搜索语法标题..." 
              className="input"
              style={{ width: '100%', paddingLeft: '40px' }}
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
            />
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <Filter size={18} style={{ color: 'var(--text-secondary)' }} />
            <select 
              className="input" 
              value={levelFilter}
              onChange={(e) => setLevelFilter(e.target.value)}
              style={{ paddingRight: '32px' }}
            >
              <option value="All">全部等级 ({Object.values(levelCounts).reduce((a, b) => a + b, 0)})</option>
              {["N1", "N2", "N3", "N4", "N5"].map(lvl => (
                <option key={lvl} value={lvl}>
                  {lvl} ({levelCounts[lvl] || 0})
                </option>
              ))}
            </select>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <select 
              className="input" 
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value)}
              style={{ paddingRight: '32px' }}
            >
              <option value="All">全部状态</option>
              <option value="Active">仅上架</option>
              <option value="Delisted">仅下架</option>
            </select>
          </div>
          <button 
            className="input" 
            onClick={() => { fetchGrammars(); fetchLevelCounts(); }}
            disabled={loading}
            style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', width: '42px', padding: 0 }}
            title="刷新数据"
          >
            <RefreshCcw size={18} className={loading ? "animate-spin" : ""} />
          </button>
        </div>
        <button 
          className="button-primary" 
          style={{ display: 'flex', alignItems: 'center', gap: '8px' }}
          onClick={handleAdd}
        >
          <Plus size={18} />
          新增语法
        </button>
      </div>

      <div className="card" style={{ padding: 0, overflow: 'hidden' }}>
        <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left' }}>
          <thead>
            <tr style={{ borderBottom: '1px solid var(--border)', backgroundColor: 'var(--bg-tertiary)' }}>
              <th style={{ padding: '12px 16px', fontWeight: '600', fontSize: '0.9rem' }}>Raw ID</th>
              <th style={{ padding: '12px 16px', fontWeight: '600', fontSize: '0.9rem' }}>语法标题</th>
              <th style={{ padding: '12px 16px', fontWeight: '600', fontSize: '0.9rem' }}>等级</th>
              <th style={{ padding: '12px 16px', fontWeight: '600', fontSize: '0.9rem' }}>更新时间</th>
              <th style={{ padding: '12px 16px', fontWeight: '600', fontSize: '0.9rem' }}>上架状态</th>
              <th style={{ padding: '12px 16px', fontWeight: '600', fontSize: '0.9rem', textAlign: 'right' }}>操作</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr>
                <td colSpan={6} style={{ padding: '32px', textAlign: 'center', color: 'var(--text-secondary)' }}>加载中...</td>
              </tr>
            ) : grammars.length === 0 ? (
              <tr>
                <td colSpan={6} style={{ padding: '32px', textAlign: 'center', color: 'var(--text-secondary)' }}>未找到相关语法</td>
              </tr>
            ) : (
              grammars.map((grammar) => (
                <tr key={grammar.id} style={{ borderBottom: '1px solid var(--border)', transition: 'background-color 0.2s' }}>
                  <td style={{ padding: '12px 16px', fontSize: '0.85rem', color: 'var(--text-tertiary)' }}>{grammar.raw_id}</td>
                  <td style={{ padding: '12px 16px', fontWeight: '600' }}>{grammar.title}</td>
                  <td style={{ padding: '12px 16px' }}>
                    <span style={{ fontSize: '0.8rem', padding: '2px 6px', borderRadius: '4px', backgroundColor: 'var(--bg-tertiary)', border: '1px solid var(--border)' }}>
                      {grammar.level}
                    </span>
                  </td>
                  <td style={{ padding: '12px 16px', color: 'var(--text-tertiary)', fontSize: '0.85rem' }}>
                    {new Date(grammar.updated_at).toLocaleString('zh-CN', { hour12: false })}
                  </td>
                  <td style={{ padding: '12px 16px' }}>
                    <select 
                      value={grammar.is_delisted ? "true" : "false"}
                      onChange={(e) => handleStatusChange(grammar.id, e.target.value === "true")}
                      className="input"
                      style={{ 
                        padding: '4px 8px', 
                        fontSize: '0.8rem', 
                        height: 'auto',
                        backgroundColor: grammar.is_delisted ? 'rgba(255, 59, 48, 0.1)' : 'rgba(52, 199, 89, 0.1)',
                        color: grammar.is_delisted ? 'var(--danger)' : 'var(--success)',
                        border: 'none'
                      }}
                    >
                      <option value="false">已上架</option>
                      <option value="true">已下架</option>
                    </select>
                  </td>
                  <td style={{ padding: '12px 16px', textAlign: 'right' }}>
                    <div style={{ display: 'flex', gap: '8px', justifyContent: 'flex-end' }}>
                      <button 
                        style={{ background: 'none', border: 'none', color: 'var(--text-secondary)', cursor: 'pointer' }}
                        onClick={() => handleEdit(grammar)}
                        title="编辑"
                      >
                        <Edit2 size={16} />
                      </button>
                    </div>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      <GrammarModal 
        isOpen={isModalOpen} 
        onClose={() => setIsModalOpen(false)} 
        onSaved={fetchGrammars}
        grammarToEdit={editingGrammar}
      />
    </DashboardShell>
  );
}
