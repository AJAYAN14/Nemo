"use client";

import React, { useEffect, useState } from "react";
import { DashboardShell } from "@/components/DashboardShell";
import { supabase } from "@/lib/supabase";
import { Search, Plus, Filter, MoreHorizontal, Edit2, Trash2, RefreshCcw } from "lucide-react";
import { WordModal } from "@/components/WordModal";

interface Word {
  id: number;
  raw_id?: string;
  japanese: string;
  hiragana: string;
  chinese: string;
  level: string;
  pos: string | null;
  is_delisted: boolean;
  updated_at: string;
  example_1: string;
  gloss_1: string;
  example_2: string;
  gloss_2: string;
  example_3: string;
  gloss_3: string;
}

export default function WordsPage() {
  const [words, setWords] = useState<Word[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState("");
  const [levelFilter, setLevelFilter] = useState("All");
  const [statusFilter, setStatusFilter] = useState("All");
  const [levelCounts, setLevelCounts] = useState<Record<string, number>>({});
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingWord, setEditingWord] = useState<Word | undefined>(undefined);
  const [sortOrder, setSortOrder] = useState<"default" | "asc" | "desc">("default");

  useEffect(() => {
    fetchWords();
    fetchLevelCounts();
  }, [searchTerm, levelFilter, statusFilter, sortOrder]);

  async function fetchLevelCounts() {
    const levels = ["N1", "N2", "N3", "N4", "N5"];
    const counts: Record<string, number> = {};
    
    await Promise.all(levels.map(async (lvl) => {
      const { count } = await supabase
        .from("dictionary_words")
        .select("*", { count: 'exact', head: true })
        .eq("level", lvl);
      counts[lvl] = count || 0;
    }));
    
    setLevelCounts(counts);
  }

  async function fetchWords() {
    setLoading(true);
    let query = supabase
      .from("dictionary_words")
      .select("*");

    if (sortOrder === "default") {
      query = query.order("id", { ascending: true });
    } else {
      query = query.order("updated_at", { ascending: sortOrder === "asc" });
    }

    query = query.limit(100);

    if (searchTerm) {
      query = query.or(`japanese.ilike.%${searchTerm}%,hiragana.ilike.%${searchTerm}%,chinese.ilike.%${searchTerm}%`);
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

    if (data) setWords(data);
    setLoading(false);
  }

  const handleEdit = (word: Word) => {
    setEditingWord(word);
    setIsModalOpen(true);
  };

  const handleAdd = () => {
    setEditingWord(undefined);
    setIsModalOpen(true);
  };

  const handleStatusChange = async (id: number, isDelisted: boolean) => {
    const { error } = await supabase
      .from("dictionary_words")
      .update({ is_delisted: isDelisted })
      .eq("id", id);
    
    if (!error) {
      fetchWords();
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
              placeholder="搜索词汇、假名或释义..." 
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
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <select 
              className="input" 
              value={sortOrder}
              onChange={(e) => setSortOrder(e.target.value as "default" | "asc" | "desc")}
              style={{ paddingRight: '32px', width: '120px' }}
            >
              <option value="default">默认排序</option>
              <option value="desc">最新更新</option>
              <option value="asc">最早更新</option>
            </select>
          </div>
          <button 
            className="input" 
            onClick={() => { fetchWords(); fetchLevelCounts(); }}
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
          新增词汇
        </button>
      </div>

      <div className="card" style={{ padding: 0, overflow: 'hidden' }}>
        <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left' }}>
          <thead>
            <tr style={{ borderBottom: '1px solid var(--border)', backgroundColor: 'var(--bg-tertiary)' }}>
              <th style={{ padding: '12px 16px', fontWeight: '600', fontSize: '0.9rem' }}>Raw ID</th>
              <th style={{ padding: '12px 16px', fontWeight: '600', fontSize: '0.9rem' }}>日语</th>
              <th style={{ padding: '12px 16px', fontWeight: '600', fontSize: '0.9rem' }}>假名</th>
              <th style={{ padding: '12px 16px', fontWeight: '600', fontSize: '0.9rem' }}>中文释义</th>
              <th style={{ padding: '12px 16px', fontWeight: '600', fontSize: '0.9rem' }}>等级</th>
              <th style={{ padding: '12px 16px', fontWeight: '600', fontSize: '0.9rem' }}>词性</th>
              <th style={{ padding: '12px 16px', fontWeight: '600', fontSize: '0.9rem' }}>更新时间</th>
              <th style={{ padding: '12px 16px', fontWeight: '600', fontSize: '0.9rem' }}>上架状态</th>
              <th style={{ padding: '12px 16px', fontWeight: '600', fontSize: '0.9rem', textAlign: 'right' }}>操作</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr>
                <td colSpan={8} style={{ padding: '32px', textAlign: 'center', color: 'var(--text-secondary)' }}>加载中...</td>
              </tr>
            ) : words.length === 0 ? (
              <tr>
                <td colSpan={8} style={{ padding: '32px', textAlign: 'center', color: 'var(--text-secondary)' }}>未找到相关词汇</td>
              </tr>
            ) : (
              words.map((word) => (
                <tr key={word.id} style={{ borderBottom: '1px solid var(--border)', transition: 'background-color 0.2s' }}>
                  <td style={{ padding: '12px 16px', fontSize: '0.85rem', color: 'var(--text-tertiary)' }}>{word.raw_id}</td>
                  <td style={{ padding: '12px 16px', fontWeight: '600' }}>{word.japanese}</td>
                  <td style={{ padding: '12px 16px', color: 'var(--text-secondary)' }}>{word.hiragana}</td>
                  <td style={{ padding: '12px 16px' }}>{word.chinese}</td>
                  <td style={{ padding: '12px 16px' }}>
                    <span style={{ fontSize: '0.8rem', padding: '2px 6px', borderRadius: '4px', backgroundColor: 'var(--bg-tertiary)', border: '1px solid var(--border)' }}>
                      {word.level}
                    </span>
                  </td>
                  <td style={{ padding: '12px 16px', color: 'var(--text-tertiary)', fontSize: '0.85rem' }}>{word.pos || "-"}</td>
                  <td style={{ padding: '12px 16px', color: 'var(--text-tertiary)', fontSize: '0.85rem' }}>
                    {new Date(word.updated_at).toLocaleString('zh-CN', { hour12: false })}
                  </td>
                  <td style={{ padding: '12px 16px' }}>
                    <select 
                      value={word.is_delisted ? "true" : "false"}
                      onChange={(e) => handleStatusChange(word.id, e.target.value === "true")}
                      className="input"
                      style={{ 
                        padding: '4px 8px', 
                        fontSize: '0.8rem', 
                        height: 'auto',
                        backgroundColor: word.is_delisted ? 'rgba(255, 59, 48, 0.1)' : 'rgba(52, 199, 89, 0.1)',
                        color: word.is_delisted ? 'var(--danger)' : 'var(--success)',
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
                        style={{ background: 'none', border: 'none', color: 'var(--text-secondary)', cursor: 'pointer', padding: '4px' }}
                        onClick={() => handleEdit(word)}
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

      <WordModal 
        isOpen={isModalOpen} 
        onClose={() => setIsModalOpen(false)} 
        onSaved={fetchWords}
        wordToEdit={editingWord}
      />
    </DashboardShell>
  );
}
