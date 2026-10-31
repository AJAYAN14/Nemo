package com.jian.nemo.feature.test.domain.handler

import com.jian.nemo.core.domain.model.CardState
import com.jian.nemo.core.domain.model.CardType
import com.jian.nemo.core.domain.model.MatchableCard
import com.jian.nemo.core.domain.model.TestQuestion
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 卡片配对结果
 */
sealed class CardMatchingResult {
    object Ignored : CardMatchingResult()
    data class Selected(val card: MatchableCard) : CardMatchingResult()
    data class Deselected(val card: MatchableCard) : CardMatchingResult()
    data class SameColumnReplaced(val oldCard: MatchableCard, val newCard: MatchableCard) : CardMatchingResult()
    data class PairingAttempt(val firstCard: MatchableCard, val secondCard: MatchableCard) : CardMatchingResult()
}

/**
 * 卡片配对验证结果
 */
data class PairingValidationResult(
    val isCorrect: Boolean,
    val firstCard: MatchableCard,
    val secondCard: MatchableCard
)

/**
 * 卡片初始化结果
 */
data class CardInitializationResult(
    val termCards: List<MatchableCard>,
    val definitionCards: List<MatchableCard>
)

/**
 * 卡片配对题处理器
 *
 * 职责：处理卡片的选择、配对验证、初始化等逻辑


 */
@Singleton
class CardMatchingHandler @Inject constructor() {

    /**
     * 处理卡片选择
     *
     * @param card 被点击的卡片
     * @param currentlySelected 当前已选中的卡片（可能为 null）
     * @param isBoardLocked 面板是否锁定
     * @return 卡片选择结果
     */
    fun handleCardSelection(
        card: MatchableCard,
        currentlySelected: MatchableCard?,
        isBoardLocked: Boolean
    ): CardMatchingResult {
        // 面板锁定或卡片非默认状态，忽略
        if (isBoardLocked || card.state != CardState.DEFAULT) {
            return CardMatchingResult.Ignored
        }

        if (currentlySelected == null) {
            // 情况1: 没有卡片被选中，直接选中点击的卡片
            return CardMatchingResult.Selected(card)
        }

        // 判断是否是同一张卡片
        if (currentlySelected.id == card.id && currentlySelected.type == card.type) {
            // 情况2: 点击已选中卡片，取消选中
            return CardMatchingResult.Deselected(card)
        }

        // 判断是否是同一列
        val isSameColumn = isSameColumn(currentlySelected.type, card.type)

        return if (isSameColumn) {
            // 情况3: 同一列的其他卡片，替换选择
            CardMatchingResult.SameColumnReplaced(currentlySelected, card)
        } else {
            // 情况4: 不同列的卡片，进行配对检查
            CardMatchingResult.PairingAttempt(currentlySelected, card)
        }
    }

    /**
     * 判断两个卡片类型是否属于同一列
     */
    private fun isSameColumn(type1: CardType, type2: CardType): Boolean {
        return when {
            // 左列：TERM 或 HIRAGANA 类型
            (type1 == CardType.TERM || type1 == CardType.HIRAGANA) &&
            (type2 == CardType.TERM || type2 == CardType.HIRAGANA) -> true
            // 右列：DEFINITION 类型
            type1 == CardType.DEFINITION && type2 == CardType.DEFINITION -> true
            // 不同列
            else -> false
        }
    }

    /**
     * 验证配对是否正确
     *
     * @param firstCard 第一张卡片
     * @param secondCard 第二张卡片
     * @return 验证结果
     */
    fun validatePairing(
        firstCard: MatchableCard,
        secondCard: MatchableCard
    ): PairingValidationResult {
        // 相同 ID 表示匹配成功
        val isCorrect = firstCard.id == secondCard.id
        return PairingValidationResult(isCorrect, firstCard, secondCard)
    }

    /**
     * 初始化卡片配对题的卡片
     *
     * @param question 卡片配对题
     * @return 初始化好的左右两列卡片
     *

     */
    fun initializeCards(question: TestQuestion.CardMatching): CardInitializationResult {
        // 生成左列卡片（混合汉字和假名）
        val termCards = question.pairs.map { word ->
            if (Math.random() > 0.5) {
                MatchableCard(
                    id = word.id,
                    text = word.japanese,
                    type = CardType.TERM
                )
            } else {
                MatchableCard(
                    id = word.id,
                    text = word.hiragana,
                    type = CardType.HIRAGANA
                )
            }
        }.shuffled()

        // 生成右列卡片（中文释义）
        val definitionCards = question.pairs.map { word ->
            MatchableCard(
                id = word.id,
                text = word.chinese,
                type = CardType.DEFINITION
            )
        }.shuffled()

        return CardInitializationResult(termCards, definitionCards)
    }

    /**
     * 检查是否所有卡片都已匹配
     *
     * @param matchedCount 已匹配的对数
     * @param totalPairs 总对数
     * @return 是否全部匹配
     */
    fun isAllMatched(matchedCount: Int, totalPairs: Int): Boolean {
        return matchedCount == totalPairs
    }

    /**
     * 检查是否达到失败条件（3次错误）
     *
     * @param wrongCount 当前错误次数
     * @return 是否失败
     */
    fun isFailure(wrongCount: Int): Boolean {
        return wrongCount >= 3
    }
}
