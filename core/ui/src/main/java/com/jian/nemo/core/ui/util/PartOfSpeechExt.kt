package com.jian.nemo.core.ui.util

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.jian.nemo.core.domain.model.PartOfSpeech
import com.jian.nemo.core.ui.R

/**
 * 词性显示名称资源 ID 映射
 */
val PartOfSpeech.displayNameResId: Int
    @StringRes
    get() = when (this) {
        PartOfSpeech.VERB -> R.string.core_ui_pos_verb
        PartOfSpeech.NOUN -> R.string.core_ui_pos_noun
        PartOfSpeech.ADJECTIVE -> R.string.core_ui_pos_adjective
        PartOfSpeech.ADVERB -> R.string.core_ui_pos_adverb
        PartOfSpeech.PARTICLE -> R.string.core_ui_pos_particle
        PartOfSpeech.CONJUNCTION -> R.string.core_ui_pos_conjunction
        PartOfSpeech.RENTAI -> R.string.core_ui_pos_rentai
        PartOfSpeech.PREFIX -> R.string.core_ui_pos_prefix
        PartOfSpeech.SUFFIX -> R.string.core_ui_pos_suffix
        PartOfSpeech.INTERJECTION -> R.string.core_ui_pos_interjection
        PartOfSpeech.FIXED_EXPRESSION -> R.string.core_ui_pos_fixed_expression
        PartOfSpeech.LOAN_WORD -> R.string.core_ui_pos_loan_word
    }

/**
 * 词性显示名称映射
 */
val PartOfSpeech.displayName: String
    @Composable
    get() = stringResource(displayNameResId)

/**
 * 词性详细描述资源 ID 映射
 */
val PartOfSpeech.descriptionResId: Int
    @StringRes
    get() = when (this) {
        PartOfSpeech.VERB -> R.string.core_ui_pos_desc_verb
        PartOfSpeech.NOUN -> R.string.core_ui_pos_desc_noun
        PartOfSpeech.ADJECTIVE -> R.string.core_ui_pos_desc_adjective
        PartOfSpeech.ADVERB -> R.string.core_ui_pos_desc_adverb
        PartOfSpeech.PARTICLE -> R.string.core_ui_pos_desc_particle
        PartOfSpeech.CONJUNCTION -> R.string.core_ui_pos_desc_conjunction
        PartOfSpeech.RENTAI -> R.string.core_ui_pos_desc_rentai
        PartOfSpeech.PREFIX -> R.string.core_ui_pos_desc_prefix
        PartOfSpeech.SUFFIX -> R.string.core_ui_pos_desc_suffix
        PartOfSpeech.INTERJECTION -> R.string.core_ui_pos_desc_interjection
        PartOfSpeech.FIXED_EXPRESSION -> R.string.core_ui_pos_desc_fixed_expression
        PartOfSpeech.LOAN_WORD -> R.string.core_ui_pos_desc_loan_word
    }

/**
 * 词性详细描述映射
 */
val PartOfSpeech.description: String
    @Composable
    get() = stringResource(descriptionResId)

/**
 * 非 Composable 环境获取词性名称
 */
fun PartOfSpeech.getDisplayName(context: Context): String = context.getString(displayNameResId)

/**
 * 非 Composable 环境获取词性描述
 */
fun PartOfSpeech.getDescription(context: Context): String = context.getString(descriptionResId)
