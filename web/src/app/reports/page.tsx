"use client";

import React, { useState, useEffect } from "react";
import { DashboardShell } from "@/components/DashboardShell";
import { supabase } from "@/lib/supabase";
import { 
  MessageSquare, 
  CheckCircle, 
  Clock, 
  ExternalLink, 
  Loader2,
  AlertTriangle,
  Search,
  Filter
} from "lucide-react";
import { WordModal } from "@/components/WordModal";
import { GrammarModal } from "@/components/GrammarModal";

interface ContentReport {
  id: string;
  user_id: string;
  item_id: number;
  item_type: "word" | "grammar";
  status: "pending" | "resolved";
  created_at: string;
  item_content?: string; // Virtual field for display
  error_type?: string;
  description?: string;
}

export default function ReportsPage() {
  const [reports, setReports] = useState<ContentReport[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [filterStatus, setFilterStatus] = useState<string>("all");
  const [isRefreshing, setIsRefreshing] = useState(false);
  const [filterType, setFilterType] = useState<string>("all");
  const [searchQuery, setSearchQuery] = useState<string>("");
  
  // Modals state
  const [isWordModalOpen, setIsWordModalOpen] = useState(false);
  const [isGrammarModalOpen, setIsGrammarModalOpen] = useState(false);
  const [selectedItem, setSelectedItem] = useState<any>(null);

  const renderErrorType = (errorType?: string) => {
    if (!errorType) {
      return (
        <span style={{
          padding: '4px 8px',
          borderRadius: '4px',
          fontSize: '0.75rem',
          backgroundColor: 'rgba(142, 142, 147, 0.1)',
          color: '#8E8E93',
          fontWeight: '500'
        }}>
          未知原因
        </span>
      );
    }

    const typeMap: Record<string, { label: string, color: string, bg: string }> = {
      meaning_error: { label: "释义错误", color: "#007AFF", bg: "rgba(0, 122, 255, 0.1)" },
      furigana_error: { label: "注音错误", color: "#5856D6", bg: "rgba(88, 86, 214, 0.1)" },
      accent_error: { label: "音调错误", color: "#AF52DE", bg: "rgba(175, 82, 222, 0.1)" },
      audio_error: { label: "音频发音", color: "#FF9500", bg: "rgba(255, 149, 0, 0.1)" },
      example_error: { label: "例句翻译", color: "#FFCC00", bg: "rgba(255, 204, 0, 0.15)" },
      spelling_error: { label: "拼写错误", color: "#FF3B30", bg: "rgba(255, 59, 48, 0.1)" },
      pos_error: { label: "词性错误", color: "#30B0C7", bg: "rgba(48, 176, 199, 0.1)" },
      connection_error: { label: "接续错误", color: "#FF9500", bg: "rgba(255, 149, 0, 0.1)" },
      level_error: { label: "级别划分", color: "#FF2D55", bg: "rgba(255, 45, 85, 0.1)" },
      other: { label: "其他问题", color: "#8E8E93", bg: "rgba(142, 142, 147, 0.1)" }
    };

    const config = typeMap[errorType] || { label: errorType, color: "#8E8E93", bg: "rgba(142, 142, 147, 0.1)" };

    return (
      <span style={{
        padding: '4px 8px',
        borderRadius: '4px',
        fontSize: '0.75rem',
        backgroundColor: config.bg,
        color: config.color,
        fontWeight: '500'
      }}>
        {config.label}
      </span>
    );
  };

  const fetchReports = async () => {
    setIsLoading(true);
    try {
      let query = supabase
        .from("content_reports")
        .select("*")
        .order("created_at", { ascending: false });

      if (filterStatus !== "all") {
        query = query.eq("status", filterStatus);
      }

      const { data: reportsData, error } = await query;
      if (error) throw error;

      // Enhance reports with item titles
      const enhancedReports = await Promise.all((reportsData as any[]).map(async (report) => {
        let content = "未知条目";
        if (report.item_type === "word") {
          const { data } = await supabase.from("dictionary_words").select("japanese").eq("id", report.item_id).single();
          if (data) content = data.japanese;
        } else if (report.item_type === "grammar") {
          const { data } = await supabase.from("dictionary_grammars").select("title").eq("id", report.item_id).single();
          if (data) content = data.title;
        }
        return { ...report, item_content: content };
      }));

      setReports(enhancedReports);
    } catch (err) {
      console.error("Error fetching reports:", err);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchReports();
  }, [filterStatus]);

  const handleUpdateStatus = async (id: string, newStatus: string) => {
    try {
      const { error } = await supabase
        .from("content_reports")
        .update({ status: newStatus })
        .eq("id", id);
      
      if (error) throw error;
      setReports(reports.map(r => r.id === id ? { ...r, status: newStatus as any } : r));
    } catch (err) {
      alert("更新失败");
    }
  };

  const handleEditItem = async (report: ContentReport) => {
    try {
      const table = report.item_type === "word" ? "dictionary_words" : "dictionary_grammars";
      const { data, error } = await supabase.from(table).select("*").eq("id", report.item_id).single();
      
      if (error) throw error;
      setSelectedItem(data);
      if (report.item_type === "word") {
        setIsWordModalOpen(true);
      } else {
        setIsGrammarModalOpen(true);
      }
    } catch (err) {
      alert("获取详情失败");
    }
  };

  const handleRefreshDatabase = async () => {
    if (!confirm("确定要增加数据库版本号来触发所有客户端刷新吗？")) return;
    setIsRefreshing(true);
    try {
      const { data, error: fetchError } = await supabase
        .from("sync_meta")
        .select("content_version")
        .eq("id", 1)
        .single();
      
      if (fetchError) throw fetchError;
      
      const nextVersion = (data?.content_version || 0) + 1;
      
      const { error: updateError } = await supabase
        .from("sync_meta")
        .update({ content_version: nextVersion })
        .eq("id", 1);
        
      if (updateError) throw updateError;
      
      alert(`数据库刷新成功！同步版本号已更新为: ${nextVersion}`);
    } catch (err: any) {
      console.error("Error refreshing database:", err);
      alert(`刷新失败: ${err.message || err}`);
    } finally {
      setIsRefreshing(false);
    }
  };

  // Client-side filtering
  const filteredReports = reports.filter(report => {
    const matchesType = filterType === "all" || report.item_type === filterType;
    const contentToSearch = report.item_content || "";
    const matchesSearch = 
      contentToSearch.toLowerCase().includes(searchQuery.toLowerCase()) || 
      report.item_id.toString().includes(searchQuery);
    return matchesType && matchesSearch;
  });

  return (
    <DashboardShell>
      <div className="header" style={{ marginBottom: '32px' }}>
        <h1 className="title" style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
          <MessageSquare size={32} color="var(--accent)" />
          用户反馈管理
        </h1>
        <p className="subtitle">查看并处理用户提交的条目内容纠错申请</p>
      </div>

      <div className="card" style={{ marginBottom: '24px' }}>
        <div style={{ display: 'flex', gap: '16px', alignItems: 'center', flexWrap: 'wrap' }}>
          {/* 状态筛选 */}
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <Filter size={18} color="var(--text-tertiary)" />
            <select 
              className="input" 
              style={{ width: '140px' }}
              value={filterStatus}
              onChange={(e) => setFilterStatus(e.target.value)}
            >
              <option value="all">全部状态</option>
              <option value="pending">待处理</option>
              <option value="resolved">已解决</option>
            </select>
          </div>

          {/* 类型筛选 */}
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <span style={{ fontSize: '0.85rem', color: 'var(--text-secondary)' }}>条目类型:</span>
            <select 
              className="input" 
              style={{ width: '140px' }}
              value={filterType}
              onChange={(e) => setFilterType(e.target.value)}
            >
              <option value="all">全部类型</option>
              <option value="word">单词</option>
              <option value="grammar">语法</option>
            </select>
          </div>

          {/* 搜索框 */}
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px', flex: 1, minWidth: '220px' }}>
            <Search size={18} color="var(--text-tertiary)" />
            <input 
              className="input" 
              style={{ flex: 1 }}
              placeholder="搜索被举报条目内容或 ID..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
            />
          </div>

          <button className="button-secondary" onClick={fetchReports}>刷新数据</button>
          <button 
            className="button-primary" 
            onClick={handleRefreshDatabase}
            disabled={isRefreshing}
            style={{ display: 'flex', alignItems: 'center', gap: '6px' }}
          >
            {isRefreshing ? "正在刷新..." : "同步刷新数据库"}
          </button>
        </div>
      </div>

      {isLoading ? (
        <div style={{ display: 'flex', justifyContent: 'center', padding: '100px' }}>
          <Loader2 className="animate-spin" size={40} color="var(--accent)" />
        </div>
      ) : (
        <div className="card" style={{ padding: 0, overflow: 'hidden' }}>
          {/* 数量统计展示 */}
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '12px 20px', borderBottom: '1px solid var(--border)', background: 'rgba(0, 0, 0, 0.02)' }}>
            <span style={{ fontSize: '0.85rem', fontWeight: '600', color: 'var(--text-secondary)' }}>
              统计: 共计 {filteredReports.length} 项 {filterType !== 'all' ? (filterType === 'word' ? '单词' : '语法') : ''}反馈
              {reports.length !== filteredReports.length && ` (筛选自 ${reports.length} 项)`}
            </span>
            <span style={{ fontSize: '0.85rem', color: 'var(--text-tertiary)' }}>
              待处理: {filteredReports.filter(r => r.status === 'pending').length} 项 | 已解决: {filteredReports.filter(r => r.status === 'resolved').length} 项
            </span>
          </div>

          <table className="table">
            <thead>
              <tr>
                <th>类型</th>
                <th>被举报条目</th>
                <th>错误原因</th>
                <th>状态</th>
                <th>提交时间</th>
                <th style={{ textAlign: 'right' }}>操作</th>
              </tr>
            </thead>
            <tbody>
              {filteredReports.length === 0 ? (
                <tr>
                  <td colSpan={6} style={{ textAlign: 'center', padding: '40px', color: 'var(--text-tertiary)' }}>
                    暂无符合条件的反馈记录
                  </td>
                </tr>
              ) : filteredReports.map((report) => (
                <tr key={report.id}>
                  <td>
                    <span style={{ 
                      padding: '4px 8px', 
                      borderRadius: '4px', 
                      fontSize: '0.75rem', 
                      backgroundColor: report.item_type === 'word' ? 'rgba(52, 199, 89, 0.1)' : 'rgba(0, 122, 255, 0.1)',
                      color: report.item_type === 'word' ? '#34C759' : '#007AFF'
                    }}>
                      {report.item_type === 'word' ? '单词' : '语法'}
                    </span>
                  </td>
                  <td>
                    <div style={{ fontWeight: '600' }}>{report.item_content}</div>
                    <div style={{ fontSize: '0.75rem', color: 'var(--text-tertiary)' }}>ID: {report.item_id}</div>
                  </td>
                  <td>
                    {renderErrorType(report.error_type)}
                    {report.description && (
                      <div style={{ 
                        fontSize: '0.8rem', 
                        color: 'var(--text-secondary)', 
                        marginTop: '6px', 
                        maxWidth: '220px', 
                        wordBreak: 'break-all',
                        lineHeight: '1.2' 
                      }}>
                        细节描述: {report.description}
                      </div>
                    )}
                  </td>
                  <td>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                      {report.status === 'pending' ? (
                        <><Clock size={14} color="#FF9500" /> <span style={{ color: '#FF9500' }}>待处理</span></>
                      ) : (
                        <><CheckCircle size={14} color="#34C759" /> <span style={{ color: '#34C759' }}>已解决</span></>
                      )}
                    </div>
                  </td>
                  <td style={{ color: 'var(--text-secondary)', fontSize: '0.85rem' }}>
                    {new Date(report.created_at).toLocaleString()}
                  </td>
                  <td style={{ textAlign: 'right' }}>
                    <div style={{ display: 'flex', gap: '8px', justifyContent: 'flex-end' }}>
                      <button 
                        className="button-secondary" 
                        style={{ padding: '6px 10px', fontSize: '0.8rem' }}
                        onClick={() => handleEditItem(report)}
                      >
                        编辑条目
                      </button>
                      {report.status === 'pending' ? (
                        <button 
                          className="button-primary" 
                          style={{ padding: '6px 10px', fontSize: '0.8rem', backgroundColor: '#34C759' }}
                          onClick={() => handleUpdateStatus(report.id, 'resolved')}
                        >
                          标记处理
                        </button>
                      ) : (
                        <button 
                          className="button-secondary" 
                          style={{ padding: '6px 10px', fontSize: '0.8rem' }}
                          onClick={() => handleUpdateStatus(report.id, 'pending')}
                        >
                          撤回处理
                        </button>
                      )}
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {/* Reusable Modals */}
      <WordModal 
        isOpen={isWordModalOpen}
        onClose={() => setIsWordModalOpen(false)}
        onSaved={() => {
          setIsWordModalOpen(false);
          fetchReports();
        }}
        wordToEdit={selectedItem}
      />
      
      <GrammarModal 
        isOpen={isGrammarModalOpen}
        onClose={() => setIsGrammarModalOpen(false)}
        onSaved={() => {
          setIsGrammarModalOpen(false);
          fetchReports();
        }}
        grammarToEdit={selectedItem}
      />
    </DashboardShell>
  );
}
