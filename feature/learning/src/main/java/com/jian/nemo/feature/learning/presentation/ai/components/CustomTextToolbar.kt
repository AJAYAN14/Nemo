package com.jian.nemo.feature.learning.presentation.ai.components

import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.TextToolbarStatus

/**
 * 自定义 TextToolbar：在系统原生复制/全选操作的基础上，
 * 注入一个"翻译"按钮。
 *
 * @param view 当前 View，用于启动 ActionMode
 * @param getSelectedText 获取当前选中文本的 getter
 * @param onTranslateRequested 翻译回调，传入选中的文本
 */
class CustomTextToolbar(
    private val view: View,
    private val getSelectedText: () -> String,
    private val onTranslateRequested: (String) -> Unit
) : TextToolbar {

    private var actionMode: ActionMode? = null

    override var status: TextToolbarStatus = TextToolbarStatus.Hidden
        private set

    override fun showMenu(
        rect: Rect,
        onCopyRequested: (() -> Unit)?,
        onPasteRequested: (() -> Unit)?,
        onCutRequested: (() -> Unit)?,
        onSelectAllRequested: (() -> Unit)?
    ) {
        val callback = object : ActionMode.Callback2() {
            override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
                if (onCopyRequested != null) {
                    menu.add(0, MENU_ITEM_COPY, 0, "复制")
                }
                if (onSelectAllRequested != null) {
                    menu.add(0, MENU_ITEM_SELECT_ALL, 1, "全选")
                }
                // 注入自定义"翻译"按钮
                menu.add(0, MENU_ITEM_TRANSLATE, 2, "翻译")
                return true
            }

            override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean = false

            override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
                return when (item.itemId) {
                    MENU_ITEM_COPY -> {
                        onCopyRequested?.invoke()
                        mode.finish()
                        true
                    }
                    MENU_ITEM_SELECT_ALL -> {
                        onSelectAllRequested?.invoke()
                        true
                    }
                    MENU_ITEM_TRANSLATE -> {
                        val text = getSelectedText()
                        if (text.isNotBlank()) {
                            onTranslateRequested(text)
                        }
                        mode.finish()
                        true
                    }
                    else -> false
                }
            }

            override fun onDestroyActionMode(mode: ActionMode) {
                actionMode = null
                status = TextToolbarStatus.Hidden
            }

            override fun onGetContentRect(mode: ActionMode, view: View, outRect: android.graphics.Rect) {
                outRect.set(
                    rect.left.toInt(),
                    rect.top.toInt(),
                    rect.right.toInt(),
                    rect.bottom.toInt()
                )
            }
        }

        actionMode?.finish()
        actionMode = view.startActionMode(callback, ActionMode.TYPE_FLOATING)
        status = TextToolbarStatus.Shown
    }

    override fun hide() {
        actionMode?.finish()
        actionMode = null
        status = TextToolbarStatus.Hidden
    }

    companion object {
        private const val MENU_ITEM_COPY = 1
        private const val MENU_ITEM_SELECT_ALL = 2
        private const val MENU_ITEM_TRANSLATE = 100
    }
}
