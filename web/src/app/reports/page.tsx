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
}

export default function ReportsPage() {
  const [reports, setReports] = useState<ContentReport[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [filterStatus, setFilterStatus] = useState<string>("all");
  
  // Modals state
  const [isWordModalOpen, setIsWordModalOpen] = useState(false);
  const [isGrammarModalOpen, setIsGrammarModalOpen] = useState(false);
  const [selectedItem, setSelectedItem] = useState<any>(null);

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
        <div style={{ display: 'flex', gap: '16px', alignItems: 'center' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px', flex: 1 }}>
            <Filter size={18} color="var(--text-tertiary)" />
            <select 
              className="input" 
              style={{ width: '200px' }}
              value={filterStatus}
              onChange={(e) => setFilterStatus(e.target.value)}
            >
              <option value="all">全部状态</option>
              <option value="pending">待处理</option>
              <option value="resolved">已解决</option>
            </select>
          </div>
          <button className="button-secondary" onClick={fetchReports}>刷新数据</button>
        </div>
      </div>

      {isLoading ? (
        <div style={{ display: 'flex', justifyContent: 'center', padding: '100px' }}>
          <Loader2 className="animate-spin" size={40} color="var(--accent)" />
        </div>
      ) : (
        <div className="card" style={{ padding: 0, overflow: 'hidden' }}>
          <table className="table">
            <thead>
              <tr>
                <th>类型</th>
                <th>被举报条目</th>
                <th>状态</th>
                <th>提交时间</th>
                <th style={{ textAlign: 'right' }}>操作</th>
              </tr>
            </thead>
            <tbody>
              {reports.length === 0 ? (
                <tr>
                  <td colSpan={5} style={{ textAlign: 'center', padding: '40px', color: 'var(--text-tertiary)' }}>
                    暂无反馈记录
                  </td>
                </tr>
              ) : reports.map((report) => (
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
