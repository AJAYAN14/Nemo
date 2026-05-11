"use client";

import React from "react";
import { Sidebar } from "./Sidebar";
import { useTheme } from "./ThemeProvider";
import { Moon, Sun } from "lucide-react";
import styles from "./DashboardShell.module.css";

export function DashboardShell({ children }: { children: React.ReactNode }) {
  const { theme, toggleTheme } = useTheme();

  return (
    <div className={styles.container}>
      <Sidebar />
      <div className={styles.main}>
        <header className={styles.header}>
          <div className={styles.headerLeft}>
            <h1 className={styles.pageTitle}>控制面板</h1>
          </div>
          <div className={styles.headerRight}>
            <button className={styles.themeToggle} onClick={(e) => toggleTheme(e)}>
              {theme === "light" ? <Moon size={20} /> : <Sun size={20} />}
            </button>
            <div className={styles.userProfile}>
              <div className={styles.avatar}>A</div>
              <span>管理员</span>
            </div>
          </div>
        </header>
        <div className={styles.content}>
          {children}
        </div>
      </div>
    </div>
  );
}
