"use client";

import React, { useState, useEffect } from "react";
import { Modal } from "./Modal";
import { Plus, Trash2, Loader2, Info, Sparkles } from "lucide-react";
import { supabase } from "@/lib/supabase";

interface GrammarModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSaved: () => void;
  grammarToEdit?: any;
}

export function GrammarModal({ isOpen, onClose, onSaved, grammarToEdit }: GrammarModalProps) {
  const [formData, setFormData] = useState({
    title: "",
    level: "N3",
    is_delisted: false,
    raw_id: "",
    content: {
      usages: [
        {
          connection: "",
          explanation: "",
          notes: "",
          examples: [
            { sentence: "", translation: "", source: "" }
          ]
        }
      ]
    }
  });

  const [isSaving, setIsSaving] = useState(false);
  const [isGenerating, setIsGenerating] = useState(false);
  const [isFetchingId, setIsFetchingId] = useState(false);

  const fetchNextRawId = async (selectedLevel: string) => {
    setIsFetchingId(true);
    try {
      const { data, error } = await supabase
        .from("dictionary_grammars")
        .select("raw_id")
        .eq("level", selectedLevel)
        .like("raw_id", `${selectedLevel}_%`)
        .order("raw_id", { ascending: false })
        .limit(1);

      if (error) throw error;

      let nextNum = 1;
      if (data && data.length > 0 && data[0].raw_id) {
        const parts = data[0].raw_id.split("_");
        if (parts.length > 1) {
          const latestNum = parseInt(parts[1], 10);
          if (!isNaN(latestNum)) {
            nextNum = latestNum + 1;
          }
        }
      }
      
      const newSuggestedId = `${selectedLevel}_${String(nextNum).padStart(3, '0')}`;
      setFormData(prev => ({ ...prev, raw_id: newSuggestedId }));
    } catch (err) {
      console.error("自动获取下一个 Raw ID 失败:", err);
    } finally {
      setIsFetchingId(false);
    }
  };

  useEffect(() => {
    if (grammarToEdit) {
      // Ensure content structure exists
      const content = grammarToEdit.content || { usages: [] };
      if (!content.usages || content.usages.length === 0) {
        content.usages = [{ connection: "", explanation: "", notes: "", examples: [] }];
      }
      setFormData({
        title: grammarToEdit.title || "",
        level: grammarToEdit.level || "N3",
        is_delisted: grammarToEdit.is_delisted || false,
        raw_id: grammarToEdit.raw_id || "",
        content: content
      });
    } else {
      setFormData({
        title: "",
        level: "N3",
        is_delisted: false,
        raw_id: "",
        content: {
          usages: [
            {
              connection: "",
              explanation: "",
              notes: "",
              examples: [
                { sentence: "", translation: "", source: "" }
              ]
            }
          ]
        }
      });
    }
  }, [grammarToEdit, isOpen]);

  useEffect(() => {
    if (!grammarToEdit && isOpen && formData.level) {
      fetchNextRawId(formData.level);
    }
  }, [isOpen, grammarToEdit]);

  const handleAIByInput = async () => {
    const inputTitle = formData.title.trim();
    if (!inputTitle) return;
    setIsGenerating(true);
    try {
      const res = await fetch("/api/ai/generate-grammar", {
        method: "POST",
        body: JSON.stringify({ title: inputTitle, level: formData.level }),
      });
      const data = await res.json();
      if (data.error) throw new Error(data.error);

      const targetLevel = data.level || formData.level;

      setFormData(prev => {
        const updated = {
          ...prev,
          level: targetLevel,
          content: {
            ...prev.content,
            usages: data.usages && data.usages.length > 0 ? data.usages : prev.content.usages
          }
        };
        if (grammarToEdit && targetLevel === grammarToEdit.level) {
          updated.raw_id = grammarToEdit.raw_id || "";
        }
        return updated;
      });

      if (!grammarToEdit || targetLevel !== grammarToEdit.level) {
        fetchNextRawId(targetLevel);
      }
    } catch (err) {
      alert("AI 生成失败: " + err);
    } finally {
      setIsGenerating(false);
    }
  };

  const handleAddUsage = () => {
    const newUsages = [...formData.content.usages, { connection: "", explanation: "", notes: "", examples: [] }];
    setFormData({ ...formData, content: { ...formData.content, usages: newUsages } });
  };

  const handleRemoveUsage = (index: number) => {
    const newUsages = formData.content.usages.filter((_, i) => i !== index);
    setFormData({ ...formData, content: { ...formData.content, usages: newUsages } });
  };

  const handleUsageChange = (index: number, field: string, value: string) => {
    const newUsages = [...formData.content.usages];
    (newUsages[index] as any)[field] = value;
    setFormData({ ...formData, content: { ...formData.content, usages: newUsages } });
  };

  const handleAddExample = (usageIndex: number) => {
    const newUsages = [...formData.content.usages];
    if (!newUsages[usageIndex].examples) newUsages[usageIndex].examples = [];
    newUsages[usageIndex].examples.push({ sentence: "", translation: "", source: "" });
    setFormData({ ...formData, content: { ...formData.content, usages: newUsages } });
  };

  const handleRemoveExample = (usageIndex: number, exampleIndex: number) => {
    const newUsages = [...formData.content.usages];
    newUsages[usageIndex].examples = newUsages[usageIndex].examples.filter((_, i) => i !== exampleIndex);
    setFormData({ ...formData, content: { ...formData.content, usages: newUsages } });
  };

  const handleExampleChange = (usageIndex: number, exampleIndex: number, field: string, value: string) => {
    const newUsages = [...formData.content.usages];
    (newUsages[usageIndex].examples[exampleIndex] as any)[field] = value;
    setFormData({ ...formData, content: { ...formData.content, usages: newUsages } });
  };

  const handleSave = async () => {
    if (!formData.title) {
      alert("请输入语法标题");
      return;
    }
    if (!formData.raw_id || !formData.raw_id.trim()) {
      alert("保存失败: 请输入 Raw ID");
      return;
    }
    setIsSaving(true);
    try {
      // 检查 Raw ID 是否重复
      const { data: dupData, error: dupError } = await supabase
        .from("dictionary_grammars")
        .select("id")
        .eq("raw_id", formData.raw_id.trim());

      if (dupError) throw dupError;
      if (dupData && dupData.length > 0) {
        const isDuplicate = grammarToEdit 
          ? dupData.some(item => item.id !== grammarToEdit.id) 
          : true;
        if (isDuplicate) {
          throw new Error("Raw ID 已存在，请更换");
        }
      }

      const processedUsages = formData.content.usages.map(usage => ({
        ...usage,
        connection: (usage.connection || "").replace(/\\n/g, "\n"),
        explanation: (usage.explanation || "").replace(/\\n/g, "\n"),
        notes: (usage.notes || "").replace(/\\n/g, "\n")
      }));

      const dataToSave = {
        title: formData.title,
        level: formData.level,
        is_delisted: formData.is_delisted,
        raw_id: formData.raw_id.trim(),
        content: {
          ...formData.content,
          usages: processedUsages
        }
      };

      if (grammarToEdit) {
        const { error } = await supabase.from("dictionary_grammars").update(dataToSave).eq("id", grammarToEdit.id);
        if (error) throw error;
      } else {
        const { error } = await supabase.from("dictionary_grammars").insert([dataToSave]);
        if (error) throw error;
      }

      onSaved();
      onClose();
    } catch (err: any) {
      alert("保存失败: " + err.message);
    } finally {
      setIsSaving(false);
    }
  };

  const footerContent = (
    <div style={{ display: 'flex', gap: '12px', width: '100%' }}>
      <button className="button-primary" style={{ flex: 1, height: '42px' }} onClick={handleSave} disabled={isSaving}>
        {isSaving ? <Loader2 size={18} className="animate-spin" /> : "保存语法条目"}
      </button>
      <button className="input" style={{ flex: 0.3, height: '42px' }} onClick={onClose}>取消</button>
    </div>
  );

  return (
    <Modal isOpen={isOpen} onClose={onClose} title={grammarToEdit ? "编辑语法" : "新增语法"} width="800px" footer={footerContent}>
      <div style={{ display: 'flex', flexDirection: 'column', gap: '20px', maxHeight: '70vh', overflowY: 'auto', paddingRight: '8px' }}>
        
        {/* Raw ID */}
        <div>
          <label style={{ fontSize: '0.85rem', color: 'var(--text-secondary)', marginBottom: '4px', display: 'block' }}>
            Raw ID {isFetchingId && <span style={{ color: 'var(--accent)', fontSize: '0.75rem' }}> (系统自动计算中...)</span>}
          </label>
          <input 
            className="input" 
            style={{ width: '100%', opacity: 0.7, backgroundColor: 'var(--bg-secondary)', cursor: 'not-allowed' }}
            value={formData.raw_id || ""}
            placeholder={isFetchingId ? "系统自动计算中..." : "系统自动分配"}
            disabled={true}
            readOnly={true}
          />
        </div>

        {/* Basic Info */}
        <div style={{ display: 'grid', gridTemplateColumns: '2fr 1fr 1fr', gap: '12px' }}>
          <div>
            <label style={{ fontSize: '0.85rem', color: 'var(--text-secondary)', marginBottom: '4px', display: 'block' }}>语法标题</label>
            <div style={{ display: 'flex', gap: '8px' }}>
              <input 
                className="input" 
                style={{ flex: 1 }}
                value={formData.title}
                onChange={(e) => setFormData({ ...formData, title: e.target.value })}
                placeholder="例如: ～あっての"
              />
              <button 
                className="button-secondary" 
                style={{ 
                  display: 'flex', 
                  alignItems: 'center', 
                  gap: '6px', 
                  color: 'var(--accent)',
                  padding: '0 12px',
                  opacity: (isGenerating || !formData.title.trim()) ? 0.5 : 1
                }}
                onClick={handleAIByInput}
                disabled={isGenerating || !formData.title.trim()}
              >
                {isGenerating ? <Loader2 size={16} className="animate-spin" /> : <Sparkles size={16} />}
                AI
              </button>
            </div>
          </div>
          <div>
            <label style={{ fontSize: '0.85rem', color: 'var(--text-secondary)', marginBottom: '4px', display: 'block' }}>等级</label>
            <select 
              className="input" 
              style={{ width: '100%' }}
              value={formData.level}
              onChange={(e) => {
                const newLevel = e.target.value;
                setFormData(prev => {
                  const updated = { ...prev, level: newLevel };
                  if (grammarToEdit && newLevel === grammarToEdit.level) {
                    updated.raw_id = grammarToEdit.raw_id || "";
                  }
                  return updated;
                });
                
                if (!grammarToEdit || newLevel !== grammarToEdit.level) {
                  fetchNextRawId(newLevel);
                }
              }}
            >
              {["N1", "N2", "N3", "N4", "N5"].map(lvl => (
                <option key={lvl} value={lvl}>{lvl}</option>
              ))}
            </select>
          </div>
          <div>
            <label style={{ fontSize: '0.85rem', color: 'var(--text-secondary)', marginBottom: '4px', display: 'block' }}>上架状态</label>
            <select 
              className="input" 
              style={{ width: '100%' }}
              value={formData.is_delisted ? "true" : "false"}
              onChange={(e) => setFormData({ ...formData, is_delisted: e.target.value === "true" })}
            >
              <option value="false">上架</option>
              <option value="true">下架</option>
            </select>
          </div>
        </div>

        {/* Content Section */}
        <div style={{ borderTop: '1px solid var(--border)', paddingTop: '16px' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
            <h3 style={{ fontSize: '1rem', fontWeight: '700', display: 'flex', alignItems: 'center', gap: '8px' }}>
              用法详解
              <span style={{ fontSize: '0.8rem', fontWeight: 'normal', color: 'var(--text-tertiary)' }}>支持添加多个用法及例句</span>
            </h3>
            <button 
              className="button-primary" 
              style={{ fontSize: '0.85rem', padding: '6px 12px', display: 'flex', alignItems: 'center', gap: '4px' }}
              onClick={handleAddUsage}
            >
              <Plus size={16} /> 添加用法
            </button>
          </div>

          {formData.content.usages.map((usage, uIdx) => (
            <div key={uIdx} className="card" style={{ marginBottom: '20px', border: '1px solid var(--border)', position: 'relative', backgroundColor: 'var(--bg-tertiary)' }}>
              <button 
                style={{ position: 'absolute', right: '12px', top: '12px', background: 'none', border: 'none', color: 'var(--danger)', cursor: 'pointer' }}
                onClick={() => handleRemoveUsage(uIdx)}
                title="删除此用法"
              >
                <Trash2 size={18} />
              </button>
              
              <div style={{ display: 'flex', flexDirection: 'column', gap: '12px', marginBottom: '16px' }}>
                <div>
                  <label style={{ fontSize: '0.85rem', fontWeight: '600', marginBottom: '4px', display: 'block' }}>接续</label>
                  <textarea 
                    className="input" 
                    style={{ width: '100%', backgroundColor: 'var(--bg-primary)', resize: 'vertical' }}
                    rows={Math.max(1, (usage.connection || "").split('\n').length)}
                    value={usage.connection || ""}
                    onChange={(e) => handleUsageChange(uIdx, "connection", e.target.value)}
                    placeholder="例如: 名词 + あっての"
                  />
                </div>
                <div>
                  <label style={{ fontSize: '0.85rem', fontWeight: '600', marginBottom: '4px', display: 'block' }}>含义说明</label>
                  <textarea 
                    className="input" 
                    style={{ width: '100%', backgroundColor: 'var(--bg-primary)', minHeight: '60px', resize: 'vertical' }}
                    value={usage.explanation || ""}
                    onChange={(e) => handleUsageChange(uIdx, "explanation", e.target.value)}
                    placeholder="解释该用法的具体含义..."
                  />
                </div>
                <div>
                  <label style={{ fontSize: '0.85rem', fontWeight: '600', marginBottom: '4px', display: 'block' }}>备注/注意</label>
                  <textarea 
                    className="input" 
                    style={{ width: '100%', backgroundColor: 'var(--bg-primary)', minHeight: '60px', resize: 'vertical' }}
                    value={usage.notes || ""}
                    onChange={(e) => handleUsageChange(uIdx, "notes", e.target.value)}
                    placeholder="补充说明、注意事项等..."
                  />
                </div>
              </div>

              {/* Examples Sub-section */}
              <div style={{ marginLeft: '12px', borderLeft: '2px solid var(--border)', paddingLeft: '16px' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '12px' }}>
                  <h4 style={{ fontSize: '0.9rem', fontWeight: '600' }}>例句</h4>
                  <button 
                    style={{ background: 'none', border: 'none', color: 'var(--accent)', cursor: 'pointer', fontSize: '0.85rem', display: 'flex', alignItems: 'center', gap: '4px' }}
                    onClick={() => handleAddExample(uIdx)}
                  >
                    <Plus size={14} /> 添加例句
                  </button>
                </div>

                {usage.examples && usage.examples.map((ex, eIdx) => (
                  <div key={eIdx} style={{ marginBottom: '12px', display: 'flex', gap: '8px', alignItems: 'flex-start' }}>
                    <div style={{ flex: 1, display: 'flex', flexDirection: 'column', gap: '4px' }}>
                      <input 
                        className="input" 
                        style={{ width: '100%', fontSize: '0.85rem', backgroundColor: 'var(--bg-primary)' }}
                        placeholder="日语原句"
                        value={ex.sentence || ""}
                        onChange={(e) => handleExampleChange(uIdx, eIdx, "sentence", e.target.value)}
                      />
                      <input 
                        className="input" 
                        style={{ width: '100%', fontSize: '0.85rem', backgroundColor: 'var(--bg-primary)', color: 'var(--text-secondary)' }}
                        placeholder="中文翻译"
                        value={ex.translation || ""}
                        onChange={(e) => handleExampleChange(uIdx, eIdx, "translation", e.target.value)}
                      />
                      <input 
                        className="input" 
                        style={{ width: '100%', fontSize: '0.75rem', backgroundColor: 'var(--bg-primary)', color: 'var(--text-tertiary)' }}
                        placeholder="来源 (可选)"
                        value={ex.source || ""}
                        onChange={(e) => handleExampleChange(uIdx, eIdx, "source", e.target.value)}
                      />
                    </div>
                    <button 
                      style={{ background: 'none', border: 'none', color: 'var(--text-tertiary)', cursor: 'pointer', marginTop: '8px' }}
                      onClick={() => handleRemoveExample(uIdx, eIdx)}
                    >
                      <Trash2 size={14} />
                    </button>
                  </div>
                ))}
              </div>
            </div>
          ))}
        </div>
      </div>
    </Modal>
  );
}
