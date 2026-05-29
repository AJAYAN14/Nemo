"use client";

import React from "react";
import styles from "./Modal.module.css";
import { X } from "lucide-react";

interface ModalProps {
  isOpen: boolean;
  onClose: () => void;
  title: string;
  children: React.ReactNode;
  footer?: React.ReactNode;
  width?: string;
}

export function Modal({ isOpen, onClose, title, children, footer, width }: ModalProps) {
  if (!isOpen) return null;

  return (
    <div className={styles.overlay}>
      <div className={styles.modal} onClick={(e) => e.stopPropagation()} style={width ? { width, maxWidth: '95vw' } : {}}>
        <div className={styles.header}>
          <h2 className={styles.title}>{title}</h2>
          <button className={styles.closeButton} onClick={onClose}>
            <X size={20} />
          </button>
        </div>
        <div className={styles.content}>
          {children}
        </div>
        {footer && (
          <div className={styles.footer}>
            {footer}
          </div>
        )}
      </div>
    </div>
  );
}
