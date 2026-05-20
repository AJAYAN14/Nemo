"use client";

import React from "react";
import { DashboardShell } from "@/components/DashboardShell";
import { ArrowLeft, Sparkles, HelpCircle } from "lucide-react";
import Link from "next/link";

export default function QuestionsPage() {
  return (
    <DashboardShell>
      <div 
        style={{ 
          display: "flex", 
          flexDirection: "column", 
          alignItems: "center", 
          justifyContent: "center", 
          minHeight: "70vh",
          padding: "40px 20px",
          textAlign: "center"
        }}
      >
        {/* 精美玻璃态拟物化卡片 */}
        <div 
          style={{
            maxWidth: "600px",
            width: "100%",
            background: "var(--card-bg, rgba(255, 255, 255, 0.03))",
            backdropFilter: "blur(12px)",
            border: "1px solid var(--border, rgba(255, 255, 255, 0.08))",
            borderRadius: "24px",
            padding: "48px 32px",
            boxShadow: "0 20px 40px rgba(0,0,0,0.2)",
            position: "relative",
            overflow: "hidden"
          }}
        >
          {/* 背景高级渐变光晕 */}
          <div 
            style={{
              position: "absolute",
              top: "-50px",
              left: "-50px",
              width: "150px",
              height: "150px",
              background: "radial-gradient(circle, var(--accent, #007AFF) 0%, transparent 70%)",
              opacity: 0.15,
              pointerEvents: "none"
            }}
          />
          <div 
            style={{
              position: "absolute",
              bottom: "-50px",
              right: "-50px",
              width: "150px",
              height: "150px",
              background: "radial-gradient(circle, #34C759 0%, transparent 70%)",
              opacity: 0.1,
              pointerEvents: "none"
            }}
          />

          {/* 带动效的图标外圈 */}
          <div 
            style={{
              width: "96px",
              height: "96px",
              borderRadius: "50%",
              background: "rgba(0, 122, 255, 0.06)",
              display: "flex",
              alignItems: "center",
              justifyContent: "center",
              margin: "0 auto 28px",
              border: "1px dashed rgba(0, 122, 255, 0.3)",
              animation: "spin 20s linear infinite",
              position: "relative"
            }}
          >
            <style>{`
              @keyframes spin {
                100% { transform: rotate(360deg); }
              }
              @keyframes float {
                0%, 100% { transform: translateY(0px); }
                50% { transform: translateY(-8px); }
              }
            `}</style>
            
            {/* 真实悬浮动效图标 */}
            <div 
              style={{
                position: "absolute",
                animation: "float 4s ease-in-out infinite",
                display: "flex",
                alignItems: "center",
                justifyContent: "center"
              }}
            >
              <HelpCircle size={44} color="var(--accent, #007AFF)" />
            </div>
          </div>

          <div style={{ display: "inline-flex", alignItems: "center", gap: "6px", backgroundColor: "rgba(0, 122, 255, 0.08)", padding: "6px 14px", borderRadius: "100px", marginBottom: "20px" }}>
            <Sparkles size={14} color="var(--accent, #007AFF)" />
            <span style={{ fontSize: "0.8rem", fontWeight: "600", color: "var(--accent, #007AFF)", letterSpacing: "1px" }}>UNDER CONSTRUCTION</span>
          </div>

          <h2 style={{ fontSize: "1.75rem", fontWeight: "700", marginBottom: "16px", letterSpacing: "-0.5px" }}>
            题目管理模块正在精心筹备中
          </h2>
          
          <p style={{ color: "var(--text-secondary, #8e8e93)", lineHeight: "1.6", fontSize: "0.95rem", marginBottom: "36px", maxWidth: "460px", marginLeft: "auto", marginRight: "auto" }}>
            我们正在加紧打磨该功能，以便为您提供高效的单词与语法题目创建、审校及批量导入工具。敬请期待！
          </p>

          <div style={{ display: "flex", justifyContent: "center" }}>
            <Link 
              href="/reports" 
              style={{
                display: "inline-flex",
                alignItems: "center",
                gap: "8px",
                padding: "12px 24px",
                borderRadius: "12px",
                backgroundColor: "var(--accent, #007AFF)",
                color: "#ffffff",
                fontWeight: "600",
                fontSize: "0.95rem",
                textDecoration: "none",
                transition: "all 0.25s ease",
                boxShadow: "0 8px 16px rgba(0, 122, 255, 0.2)"
              }}
              onMouseEnter={(e) => {
                e.currentTarget.style.transform = "translateY(-2px)";
                e.currentTarget.style.boxShadow = "0 12px 20px rgba(0, 122, 255, 0.3)";
              }}
              onMouseLeave={(e) => {
                e.currentTarget.style.transform = "translateY(0px)";
                e.currentTarget.style.boxShadow = "0 8px 16px rgba(0, 122, 255, 0.2)";
              }}
            >
              <ArrowLeft size={18} />
              返回反馈管理
            </Link>
          </div>
        </div>
      </div>
    </DashboardShell>
  );
}
