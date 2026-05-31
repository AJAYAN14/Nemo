"use client";

import React, { useState, useEffect } from "react";
import { Modal } from "./Modal";
import { Sparkles, Loader2 } from "lucide-react";
import { supabase } from "@/lib/supabase";

const POS_LIST = [
  "名", "名*他動3", "名*自動3", "他動1", "副", "名*ナ形", "自動1", "他動2", "イ形", "接尾",
  "自動2", "名*自他動3", "ナ形", "名*副", "副*自動3", "接頭", "接", "自他動1", "嘆", "代",
  "連体", "名*ナ形*自動3", "連語", "副*ナ形", "自他動2", "ナ形*副", "名*接尾", "他動3", "自動3", "自他動3",
  "名*ナ形*副", "副*ナ形*自動3", "名*ナ形*他動3", "助", "名*副*ナ形", "代*副", "名*代", "副*嘆", "接尾*名", "名*他動3*副",
  "ナ形*副*自動3", "名*他動3*ナ形", "副*自動3*ナ形", "ナ形*自動3", "名*助", "副*接", "名*自動1", "代*名", "接続", "名*代*副",
  "副*他動3", "名*奉承", "自動1*礼貌", "名*副*代", "名*他動3*接尾", "副*名", "ナ形*副*名*自動3", "連語*叹", "名*自他動1", "他動2*奉承",
  "名*接", "副*名*ナ形", "接*副", "嘆*連語", "嘆*名*自動3", "嘆*副*ナ形", "名*ナ形*自他動3", "名*接頭", "他動1/他動3", "他動1*尊敬",
  "名*尊称", "名*副*接", "名*ナ形*礼貌", "助*嘆", "イ形*接尾"
];

interface WordModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSaved: () => void;
  wordToEdit?: any;
}

export function WordModal({ isOpen, onClose, onSaved, wordToEdit }: WordModalProps) {
  const [formData, setFormData] = useState({
    japanese: "",
    hiragana: "",
    chinese: "",
    level: "N3",
    pos: "",
    is_delisted: false,
    example_1: "",
    gloss_1: "",
    example_2: "",
    gloss_2: "",
    example_3: "",
    gloss_3: "",
    raw_id: "",
  });

  const [isGenerating, setIsGenerating] = useState(false);
  const [isSaving, setIsSaving] = useState(false);
  const [isFetchingId, setIsFetchingId] = useState(false);

  const fetchNextRawId = async (selectedLevel: string) => {
    setIsFetchingId(true);
    try {
      const { data, error } = await supabase
        .from("dictionary_words")
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
      
      const newSuggestedId = `${selectedLevel}_${String(nextNum).padStart(4, '0')}`;
      setFormData(prev => ({ ...prev, raw_id: newSuggestedId }));
    } catch (err) {
      console.error("自动获取下一个 Raw ID 失败:", err);
    } finally {
      setIsFetchingId(false);
    }
  };

  useEffect(() => {
    if (wordToEdit) {
      setFormData({
        japanese: wordToEdit.japanese || "",
        hiragana: wordToEdit.hiragana || "",
        chinese: wordToEdit.chinese || "",
        level: wordToEdit.level || "N3",
        pos: wordToEdit.pos || "",
        is_delisted: wordToEdit.is_delisted || false,
        example_1: wordToEdit.example_1 || "",
        gloss_1: wordToEdit.gloss_1 || "",
        example_2: wordToEdit.example_2 || "",
        gloss_2: wordToEdit.gloss_2 || "",
        example_3: wordToEdit.example_3 || "",
        gloss_3: wordToEdit.gloss_3 || "",
        raw_id: wordToEdit.raw_id || "",
      });
    } else {
      setFormData({
        japanese: "",
        hiragana: "",
        chinese: "",
        level: "N3",
        pos: "",
        is_delisted: false,
        example_1: "",
        gloss_1: "",
        example_2: "",
        gloss_2: "",
        example_3: "",
        gloss_3: "",
        raw_id: "",
      });
    }
  }, [wordToEdit, isOpen]);

  useEffect(() => {
    if (!wordToEdit && isOpen && formData.level) {
      fetchNextRawId(formData.level);
    }
  }, [isOpen, wordToEdit]);

  const handleAIByInput = async () => {
    if (!formData.japanese) return;
    setIsGenerating(true);
    try {
      const res = await fetch("/api/ai/generate-word", {
        method: "POST",
        body: JSON.stringify({ word: formData.japanese, level: formData.level }),
      });
      const data = await res.json();
      if (data.error) throw new Error(data.error);

      const targetLevel = data.level || formData.level;
      setFormData(prev => {
        const updated = {
          ...prev,
          hiragana: data.hiragana || "",
          chinese: data.chinese || "",
          level: targetLevel,
          pos: data.pos || "",
          example_1: data.examples?.[0]?.example || "",
          gloss_1: data.examples?.[0]?.gloss || "",
          example_2: data.examples?.[1]?.example || "",
          gloss_2: data.examples?.[1]?.gloss || "",
          example_3: data.examples?.[2]?.example || "",
          gloss_3: data.examples?.[2]?.gloss || "",
        };
        if (wordToEdit && targetLevel === wordToEdit.level) {
          updated.raw_id = wordToEdit.raw_id || "";
        }
        return updated;
      });

      if (!wordToEdit || targetLevel !== wordToEdit.level) {
        fetchNextRawId(targetLevel);
      }
    } catch (err) {
      alert("AI 生成失败: " + err);
    } finally {
      setIsGenerating(false);
    }
  };

  const handleSave = async () => {
    if (!formData.raw_id || !formData.raw_id.trim()) {
      alert("保存失败: 请输入 Raw ID");
      return;
    }

    setIsSaving(true);
    try {
      // 检查 Raw ID 是否重复
      const { data: dupData, error: dupError } = await supabase
        .from("dictionary_words")
        .select("id")
        .eq("raw_id", formData.raw_id.trim());

      if (dupError) throw dupError;
      if (dupData && dupData.length > 0) {
        // 如果是编辑模式，只有跟当前编辑单词的 id 不一致时才算重复
        const isDuplicate = wordToEdit 
          ? dupData.some(item => item.id !== wordToEdit.id) 
          : true;
        if (isDuplicate) {
          throw new Error("Raw ID 已存在，请更换");
        }
      }

      if (wordToEdit) {
        // 编辑模式：剔除只读字段（保留 raw_id 以支持等级变动时更新 ID）
        const { id, updated_at, ...updatePayload } = formData as any;
        const { error } = await supabase
          .from("dictionary_words")
          .update(updatePayload)
          .eq("id", wordToEdit.id);
        if (error) throw error;
      } else {
        // 新增模式
        const { error } = await supabase
          .from("dictionary_words")
          .insert([formData]);
        if (error) throw error;
      }

      onSaved();
      onClose();
    } catch (err) {
      alert("保存失败: " + ((err as any).message || JSON.stringify(err)));
    } finally {
      setIsSaving(false);
    }
  };

  const footerContent = (
    <div style={{ display: 'flex', gap: '12px', width: '100%' }}>
      <button className="button-primary" style={{ flex: 1 }} onClick={handleSave} disabled={isSaving}>
        {isSaving ? "保存中..." : "保存词条"}
      </button>
      <button className="input" style={{ flex: 0.4 }} onClick={onClose}>取消</button>
    </div>
  );

  return (
    <Modal isOpen={isOpen} onClose={onClose} title={wordToEdit ? "编辑词条" : "新增词条"} footer={footerContent}>
      <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
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

        <div style={{ display: 'flex', gap: '8px', alignItems: 'flex-end' }}>
          <div style={{ flex: 1 }}>
            <label style={{ fontSize: '0.85rem', color: 'var(--text-secondary)', marginBottom: '4px', display: 'block' }}>日语单词</label>
            <input 
              className="input" 
              style={{ width: '100%' }}
              value={formData.japanese || ""}
              onChange={(e) => setFormData({ ...formData, japanese: e.target.value })}
              placeholder="例如: 素晴らしい"
            />
          </div>
          <button 
            className="button-secondary" 
            style={{ 
              height: '42px', 
              display: 'flex', 
              alignItems: 'center', 
              gap: '6px', 
              color: 'var(--accent)',
              opacity: (isGenerating || !formData.japanese) ? 0.5 : 1
            }}
            onClick={handleAIByInput}
            disabled={isGenerating || !formData.japanese}
          >
            {isGenerating ? <Loader2 size={18} className="animate-spin" /> : <Sparkles size={18} />}
            AI 生成
          </button>
        </div>

        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px' }}>
          <div>
            <label style={{ fontSize: '0.85rem', color: 'var(--text-secondary)', marginBottom: '4px', display: 'block' }}>假名</label>
            <input 
              className="input" 
              style={{ width: '100%' }}
              value={formData.hiragana || ""}
              onChange={(e) => setFormData({ ...formData, hiragana: e.target.value })}
            />
          </div>
          <div>
            <label style={{ fontSize: '0.85rem', color: 'var(--text-secondary)', marginBottom: '4px', display: 'block' }}>等级</label>
            <select 
              className="input" 
              style={{ width: '100%' }}
              value={formData.level || "N3"}
              onChange={(e) => {
                const newLevel = e.target.value;
                setFormData(prev => {
                  const updated = { ...prev, level: newLevel };
                  if (wordToEdit && newLevel === wordToEdit.level) {
                    updated.raw_id = wordToEdit.raw_id || "";
                  }
                  return updated;
                });
                
                if (!wordToEdit || newLevel !== wordToEdit.level) {
                  fetchNextRawId(newLevel);
                }
              }}
            >
              <option value="N1">N1</option>
              <option value="N2">N2</option>
              <option value="N3">N3</option>
              <option value="N4">N4</option>
              <option value="N5">N5</option>
            </select>
          </div>
        </div>

        <div>
          <label style={{ fontSize: '0.85rem', color: 'var(--text-secondary)', marginBottom: '4px', display: 'block' }}>中文释义</label>
          <input 
            className="input" 
            style={{ width: '100%' }}
            value={formData.chinese || ""}
            onChange={(e) => setFormData({ ...formData, chinese: e.target.value })}
          />
        </div>

        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px' }}>
          <div>
            <label style={{ fontSize: '0.85rem', color: 'var(--text-secondary)', marginBottom: '4px', display: 'block' }}>词性</label>
            <input 
              className="input" 
              style={{ width: '100%' }}
              value={formData.pos || ""}
              onChange={(e) => setFormData({ ...formData, pos: e.target.value })}
              placeholder="请选择或输入词性"
              list="pos-list"
            />
            <datalist id="pos-list">
              {POS_LIST.map((pos) => (
                <option key={pos} value={pos} />
              ))}
            </datalist>
          </div>
          <div>
            <label style={{ fontSize: '0.85rem', color: 'var(--text-secondary)', marginBottom: '4px', display: 'block' }}>上架状态</label>
            <select 
              className="input" 
              style={{ width: '100%' }}
              value={formData.is_delisted ? "true" : "false"}
              onChange={(e) => setFormData({ ...formData, is_delisted: e.target.value === "true" })}
            >
              <option value="false">上架 (正常显示)</option>
              <option value="true">下架 (暂不显示)</option>
            </select>
          </div>
        </div>

        <div style={{ borderTop: '1px solid var(--border)', paddingTop: '16px' }}>
          <h3 style={{ fontSize: '0.9rem', fontWeight: '700', marginBottom: '12px' }}>例句管理</h3>
          {[1, 2, 3].map((num) => (
            <div key={num} style={{ marginBottom: '12px' }}>
              <input 
                className="input" 
                style={{ width: '100%', marginBottom: '4px', fontSize: '0.9rem' }}
                placeholder={`例句 ${num}`}
                value={(formData as any)[`example_${num}`] || ""}
                onChange={(e) => setFormData({ ...formData, [`example_${num}`]: e.target.value })}
              />
              <input 
                className="input" 
                style={{ width: '100%', fontSize: '0.85rem', color: 'var(--text-secondary)' }}
                placeholder={`翻译 ${num}`}
                value={(formData as any)[`gloss_${num}`] || ""}
                onChange={(e) => setFormData({ ...formData, [`gloss_${num}`]: e.target.value })}
              />
            </div>
          ))}
        </div>
      </div>
    </Modal>
  );
}
