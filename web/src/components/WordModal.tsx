"use client";

import React, { useState, useEffect } from "react";
import { Modal } from "./Modal";
import { Sparkles, Loader2 } from "lucide-react";
import { supabase } from "@/lib/supabase";

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
  });

  const [isGenerating, setIsGenerating] = useState(false);
  const [isSaving, setIsSaving] = useState(false);

  useEffect(() => {
    if (wordToEdit) {
      setFormData(wordToEdit);
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
      });
    }
  }, [wordToEdit, isOpen]);

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

      setFormData({
        ...formData,
        hiragana: data.hiragana || "",
        chinese: data.chinese || "",
        level: data.level || formData.level,
        pos: data.pos || "",
        example_1: data.examples?.[0]?.example || "",
        gloss_1: data.examples?.[0]?.gloss || "",
        example_2: data.examples?.[1]?.example || "",
        gloss_2: data.examples?.[1]?.gloss || "",
        example_3: data.examples?.[2]?.example || "",
        gloss_3: data.examples?.[2]?.gloss || "",
      });
    } catch (err) {
      alert("AI 生成失败: " + err);
    } finally {
      setIsGenerating(false);
    }
  };

  const handleSave = async () => {
    setIsSaving(true);
    try {
      const { error } = wordToEdit
        ? await supabase.from("dictionary_words").update(formData).eq("id", wordToEdit.id)
        : await supabase.from("dictionary_words").insert([formData]);

      if (error) throw error;
      onSaved();
      onClose();
    } catch (err) {
      alert("保存失败: " + err);
    } finally {
      setIsSaving(false);
    }
  };

  return (
    <Modal isOpen={isOpen} onClose={onClose} title={wordToEdit ? "编辑词条" : "新增词条"}>
      <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
        <div style={{ display: 'flex', gap: '8px', alignItems: 'flex-end' }}>
          <div style={{ flex: 1 }}>
            <label style={{ fontSize: '0.85rem', color: 'var(--text-secondary)', marginBottom: '4px', display: 'block' }}>日语单词</label>
            <input 
              className="input" 
              style={{ width: '100%' }}
              value={formData.japanese}
              onChange={(e) => setFormData({ ...formData, japanese: e.target.value })}
              placeholder="例如: 素晴らしい"
            />
          </div>
          <button 
            className="button-primary" 
            style={{ height: '42px', display: 'flex', alignItems: 'center', gap: '6px', backgroundColor: 'var(--bg-tertiary)', color: 'var(--accent)', border: '1px solid var(--border)' }}
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
              value={formData.hiragana}
              onChange={(e) => setFormData({ ...formData, hiragana: e.target.value })}
            />
          </div>
          <div>
            <label style={{ fontSize: '0.85rem', color: 'var(--text-secondary)', marginBottom: '4px', display: 'block' }}>等级</label>
            <select 
              className="input" 
              style={{ width: '100%' }}
              value={formData.level}
              onChange={(e) => setFormData({ ...formData, level: e.target.value })}
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
            value={formData.chinese}
            onChange={(e) => setFormData({ ...formData, chinese: e.target.value })}
          />
        </div>

        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px' }}>
          <div>
            <label style={{ fontSize: '0.85rem', color: 'var(--text-secondary)', marginBottom: '4px', display: 'block' }}>词性</label>
            <input 
              className="input" 
              style={{ width: '100%' }}
              value={formData.pos}
              onChange={(e) => setFormData({ ...formData, pos: e.target.value })}
              placeholder="例如: 形容词"
            />
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
                value={(formData as any)[`example_${num}`]}
                onChange={(e) => setFormData({ ...formData, [`example_${num}`]: e.target.value })}
              />
              <input 
                className="input" 
                style={{ width: '100%', fontSize: '0.85rem', color: 'var(--text-secondary)' }}
                placeholder={`翻译 ${num}`}
                value={(formData as any)[`gloss_${num}`]}
                onChange={(e) => setFormData({ ...formData, [`gloss_${num}`]: e.target.value })}
              />
            </div>
          ))}
        </div>

        <div style={{ display: 'flex', gap: '12px', marginTop: '8px' }}>
          <button className="button-primary" style={{ flex: 1 }} onClick={handleSave} disabled={isSaving}>
            {isSaving ? "保存中..." : "保存词条"}
          </button>
          <button className="input" style={{ flex: 0.4 }} onClick={onClose}>取消</button>
        </div>
      </div>
    </Modal>
  );
}
