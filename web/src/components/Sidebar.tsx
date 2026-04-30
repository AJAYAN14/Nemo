"use client";

import React from "react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { 
  BookOpen, 
  Library, 
  HelpCircle, 
  RefreshCw, 
  Settings,
  LayoutDashboard
} from "lucide-react";
import styles from "./Sidebar.module.css";

const menuItems = [
  { icon: LayoutDashboard, label: "仪表盘", href: "/" },
  { icon: BookOpen, label: "词库管理", href: "/words" },
  { icon: Library, label: "语法管理", href: "/grammars" },
  { icon: HelpCircle, label: "题目管理", href: "/questions" },
  { icon: RefreshCw, label: "同步控制", href: "/sync" },
  { icon: Settings, label: "系统设置", href: "/settings" },
];

export function Sidebar() {
  const pathname = usePathname();

  return (
    <aside className={styles.sidebar}>
      <div className={styles.logo}>
        <div className={styles.logoIcon}>N</div>
        <span className={styles.logoText}>Nemo Admin</span>
      </div>
      <nav className={styles.nav}>
        {menuItems.map((item) => {
          const Icon = item.icon;
          const isActive = pathname === item.href;
          return (
            <Link 
              key={item.href} 
              href={item.href}
              className={`${styles.navItem} ${isActive ? styles.active : ""}`}
            >
              <Icon size={20} />
              <span>{item.label}</span>
            </Link>
          );
        })}
      </nav>
    </aside>
  );
}
