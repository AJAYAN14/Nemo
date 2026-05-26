package com.jian.nemo.feature.learning.presentation.verb

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jian.nemo.core.designsystem.theme.BentoColors
import com.jian.nemo.core.designsystem.theme.IosColors
import com.jian.nemo.core.designsystem.theme.NemoNeutrals
import com.jian.nemo.core.designsystem.theme.NotoSerifJP
import com.jian.nemo.core.ui.component.common.CommonHeader

// 动词变形规则数据模型
data class GrammarRule(
    val group: String,
    val rule: String,
    val examples: List<String>
)

data class GrammarData(
    val id: Int,
    val prefix: String,   // 数字前缀，如 "1. "
    val title: String,    // 变形名称，如 "辞书形 (基本形 / 原形)"
    val usage: String,
    val colorLight: Color,
    val colorDark: Color,
    val bgColorLight: Color,
    val bgColorDark: Color,
    val rules: List<GrammarRule>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerbConjugationHandbookScreen(
    onNavigateBack: () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val textMain = if (isDark) MaterialTheme.colorScheme.onSurface else NemoNeutrals.Gray800
    val textSub = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else NemoNeutrals.Gray500
    val containerColor = if (isDark) MaterialTheme.colorScheme.background else BentoColors.BgBase
    val cardBgColor = if (isDark) MaterialTheme.colorScheme.surfaceContainer else Color.White
    
    // Pro Max 玻璃反射微边框规范：浅色采用柔和灰，深色采用微弱的白反光
    val microBorderStroke = if (isDark) {
        androidx.compose.foundation.BorderStroke(0.5.dp, Color.White.copy(alpha = 0.08f))
    } else {
        androidx.compose.foundation.BorderStroke(0.5.dp, NemoNeutrals.Gray200.copy(alpha = 0.5f))
    }

    // 状态管理
    val scrollState = rememberScrollState()
    var expandedId by remember { mutableStateOf<Int?>(1) }
    
    // 捕捉各卡片相对 parent 的 y 轴物理坐标，以便做展开平滑滚动黄金锚定
    val itemPositions = remember { mutableStateMapOf<Int, Int>() }

    // 13 种变形的教科书级数据源 (已将前缀与标题分离定义)
    val grammarList = remember {
        listOf(
            GrammarData(
                id = 1,
                prefix = "1. ",
                title = "辞书形 (基本形 / 原形)",
                usage = "动词在字典里的原始形态。用于普通体的现在时/将来时，或连接特定语法（如「～ことができる」）。",
                colorLight = IosColors.Blue,
                colorDark = IosColors.BlueDark,
                bgColorLight = IosColors.Blue.copy(alpha = 0.08f),
                bgColorDark = IosColors.BlueDark.copy(alpha = 0.15f),
                rules = listOf(
                    GrammarRule("一类动词 (五段)", "词尾必定是「う」段假名。", listOf("書く (かく / 写)", "飲む (のむ / 喝)")),
                    GrammarRule("二类动词 (一段)", "词尾必定是「る」，且倒数第二个音在「い」或「え」段。", listOf("食べる (たべる / 吃)", "見る (みる / 看)")),
                    GrammarRule("三类动词 (不规则)", "只有两个特定动词。", listOf("する (做)", "来る (くる / 来)"))
                )
            ),
            GrammarData(
                id = 2,
                prefix = "2. ",
                title = "ます形 (敬体连用形)",
                usage = "日语中最基础、最礼貌的表达方式（敬体）。",
                colorLight = IosColors.Indigo,
                colorDark = IosColors.IndigoDark,
                bgColorLight = IosColors.Indigo.copy(alpha = 0.08f),
                bgColorDark = IosColors.IndigoDark.copy(alpha = 0.15f),
                rules = listOf(
                    GrammarRule("一类动词", "词尾「う」段假名变同行「い」段假名 + ます。", listOf("書く → 書きます (かきます)", "飲む → 飲みます (のみます)")),
                    GrammarRule("二类动词", "去掉词尾的「る」 + ます。", listOf("食べる → 食べます (たべます)", "見る → 見ます (みます)")),
                    GrammarRule("三类动词", "特殊记忆。", listOf("する → します", "来る → 来ます (きます)"))
                )
            ),
            GrammarData(
                id = 3,
                prefix = "3. ",
                title = "て形 (中顿 / 请求)",
                usage = "用于表示动作的先后顺序、原因、中顿，或与「～てください」连用表示请求。一类动词有音便现象。",
                colorLight = IosColors.Green,
                colorDark = IosColors.GreenDark,
                bgColorLight = IosColors.Green.copy(alpha = 0.08f),
                bgColorDark = IosColors.GreenDark.copy(alpha = 0.15f),
                rules = listOf(
                    GrammarRule(
                        "一类动词",
                        "【促音便】う,つ,る结尾 → って\n【拨音便】ぬ,ぶ,む结尾 → んで\n【イ音便】く,ぐ结尾 → いて/いで\n【特殊】す结尾 → して。行く(いく) → 行って(い进て)",
                        listOf("買う(かう) → 買って(かって)", "飲む(のむ) → 飲んで(のんで)", "書く(かく) → 書いて(かいて)", "話す(はなす) → 話して(はなして)")
                    ),
                    GrammarRule("二类动词", "去掉词尾的「る」 + て。", listOf("食べる → 食べて (たべて)", "見る → 見て (みて)")),
                    GrammarRule("三类动词", "特殊记忆。", listOf("する → して", "来る → 来て (きて)"))
                )
            ),
            GrammarData(
                id = 4,
                prefix = "4. ",
                title = "た形 (过去式)",
                usage = "表达动作已经发生（普通体的过去式）。变形规则与「て形」完全一致，只需把て/で换成た/だ。",
                colorLight = IosColors.Teal,
                colorDark = IosColors.TealDark,
                bgColorLight = IosColors.Teal.copy(alpha = 0.08f),
                bgColorDark = IosColors.TealDark.copy(alpha = 0.15f),
                rules = listOf(
                    GrammarRule("一类动词", "依照「て形」的音便规则，将尾音换成 た/だ。", listOf("買う → 買った (かった)", "飲む → 飲んだ (のんだ)", "書く → 書いた (かいた)")),
                    GrammarRule("二类动词", "去掉词尾的「る」 + た。", listOf("食べる → 食べた (たべた)", "見る → 見た (みた)")),
                    GrammarRule("三类动词", "特殊记忆。", listOf("する → した", "来る → 来た (きた)"))
                )
            ),
            GrammarData(
                id = 5,
                prefix = "5. ",
                title = "ない形 (否定形)",
                usage = "表达否定意义（普通体），或连接「～ないでください」（请不要做...）。",
                colorLight = IosColors.Red,
                colorDark = IosColors.RedDark,
                bgColorLight = IosColors.Red.copy(alpha = 0.08f),
                bgColorDark = IosColors.RedDark.copy(alpha = 0.15f),
                rules = listOf(
                    GrammarRule("一类动词", "词尾「う」段变同行「あ」段 + ない。（※注意：词尾为「う」时变「わ」，不是「あ」）", listOf("書く → 書かない (かかない)", "買う → 買わない (かわない / 特殊)")),
                    GrammarRule("二类动词", "去掉词尾的「る」 + ない。", listOf("食べる → 食べない (たべない)", "見る → 見ない (みない)")),
                    GrammarRule("三类动词", "特殊记忆。", listOf("する → しない", "来る → 来ない (こない)"))
                )
            ),
            GrammarData(
                id = 6,
                prefix = "6. ",
                title = "意志形",
                usage = "表达说话人的决心、意愿，或提议“一起做某事吧”。",
                colorLight = IosColors.Orange,
                colorDark = IosColors.OrangeDark,
                bgColorLight = IosColors.Orange.copy(alpha = 0.08f),
                bgColorDark = IosColors.OrangeDark.copy(alpha = 0.15f),
                rules = listOf(
                    GrammarRule("一类动词", "词尾「う」段变同行「お」段 + う。", listOf("書く → 書こう (かこう)", "行く → 行こう (いこう)")),
                    GrammarRule("二类动词", "去掉词尾的「る」 + よう。", listOf("食べる → 食べよう (たべよう)", "見る → 見よう (みよう)")),
                    GrammarRule("三类动词", "特殊记忆。", listOf("する → しよう", "来る → 来よう (こよう)"))
                )
            ),
            GrammarData(
                id = 7,
                prefix = "7. ",
                title = "命令形",
                usage = "强硬地命令他人做某事，常用于男性粗鲁用语、标语、紧急情况。",
                colorLight = IosColors.RedDark,
                colorDark = IosColors.Red,
                bgColorLight = IosColors.RedDark.copy(alpha = 0.08f),
                bgColorDark = IosColors.Red.copy(alpha = 0.15f),
                rules = listOf(
                    GrammarRule("一类动词", "词尾「う」段变同行「え」段。", listOf("書く → 書け (かけ)", "行く → 行け (いけ)")),
                    GrammarRule("二类动词", "去掉词尾的「る」 + ろ。", listOf("食べる → 食べろ (たべろ)", "見る → 見ろ (みろ)")),
                    GrammarRule("三类动词", "特殊记忆。", listOf("する → しろ / せよ", "来る → 来い (こい)"))
                )
            ),
            GrammarData(
                id = 8,
                prefix = "8. ",
                title = "禁止形",
                usage = "强硬地命令他人“不准做某事”。",
                colorLight = Color(0xFFDC2626),
                colorDark = Color(0xFFEF4444),
                bgColorLight = Color(0xFFFEF2F2),
                bgColorDark = Color(0xFF7F1D1D).copy(alpha = 0.3f),
                rules = listOf(
                    GrammarRule("所有动词", "直接在【辞书形 (原形)】后 + な。", listOf("書く → 書くな (かくな)", "食べる → 食べるな (たべるな)", "する → するな", "来る → 来るな (くるな)"))
                )
            ),
            GrammarData(
                id = 9,
                prefix = "9. ",
                title = "条件形 (ば形)",
                usage = "表示假定条件，即“如果……的话”。",
                colorLight = IosColors.Cyan,
                colorDark = IosColors.CyanDark,
                bgColorLight = IosColors.Cyan.copy(alpha = 0.08f),
                bgColorDark = IosColors.CyanDark.copy(alpha = 0.15f),
                rules = listOf(
                    GrammarRule("一类动词", "词尾「う」段变同行「え」段 + ば。", listOf("書く → 書けば (かけば)", "行く → 行けば (いけば)")),
                    GrammarRule("二类动词", "去掉词尾的「る」 + れば。", listOf("食べる → 食べれば (たべれば)", "見る → 見れば (みれば)")),
                    GrammarRule("三类动词", "特殊记忆。", listOf("する → すれば", "来る → 来れば (くれば)"))
                )
            ),
            GrammarData(
                id = 10,
                prefix = "10. ",
                title = "可能形",
                usage = "表示能力或客观条件允许，即“能够做某事”。",
                colorLight = IosColors.Purple,
                colorDark = IosColors.PurpleDark,
                bgColorLight = IosColors.Purple.copy(alpha = 0.08f),
                bgColorDark = IosColors.PurpleDark.copy(alpha = 0.15f),
                rules = listOf(
                    GrammarRule("一类动词", "词尾「う」段变同行「え」段 + る。", listOf("書く → 書ける (かける)", "話す → 話せる (haなせる)")),
                    GrammarRule("二类动词", "去掉词尾的「る」 + られる。", listOf("食べる → 食べられる", "見る → 見られる")),
                    GrammarRule("三类动词", "特殊记忆。する 的可能形是一个全新的词。", listOf("する → できる", "来る → 来られる (こられる)"))
                )
            ),
            GrammarData(
                id = 11,
                prefix = "11. ",
                title = "被动形 (受身形)",
                usage = "表示动作的承受者（被做某事），也可表示受害或间接尊敬。",
                colorLight = IosColors.Pink,
                colorDark = IosColors.Pink,
                bgColorLight = IosColors.Pink.copy(alpha = 0.08f),
                bgColorDark = IosColors.Pink.copy(alpha = 0.15f),
                rules = listOf(
                    GrammarRule("一类动词", "词尾「う」段变同行「あ」段 + れる。（※尾音「う」变「わ」）", listOf("書く → 書かれる (かかれる)", "言う → 言われる (いわれる)")),
                    GrammarRule("二类动词", "去掉词尾的「る」 + られる。（形式与可能形完全一致）", listOf("食べる → 食べられる", "見る → 見られる")),
                    GrammarRule("三类动词", "特殊记忆。", listOf("する → される", "来る → 来られる (こられる)"))
                )
            ),
            GrammarData(
                id = 12,
                prefix = "12. ",
                title = "使役形",
                usage = "表示让某人做某事，或者允许某人做某事。",
                colorLight = IosColors.OrangeDark,
                colorDark = IosColors.Orange,
                bgColorLight = IosColors.OrangeDark.copy(alpha = 0.08f),
                bgColorDark = IosColors.Orange.copy(alpha = 0.15f),
                rules = listOf(
                    GrammarRule("一类动词", "词尾「う」段变同行「あ」段 + せる。（※尾音「う」变「わ」）", listOf("書く → 書かせる (かかせる)", "買う → 買わせる (かわせる)")),
                    GrammarRule("二类动词", "去掉词尾的「る」 + させる。", listOf("食べる → 食べさせる", "見る → 見させる")),
                    GrammarRule("三类动词", "特殊记忆。", listOf("する → させる", "来る → 来させる (こさせる)"))
                )
            ),
            GrammarData(
                id = 13,
                prefix = "13. ",
                title = "使役被动形",
                usage = "表示被迫做某事（“被逼着不得不做……”），是使役形 + 被动形的结合。",
                colorLight = NemoNeutrals.Gray600,
                colorDark = NemoNeutrals.DarkTextSecondary,
                bgColorLight = NemoNeutrals.Gray200,
                bgColorDark = NemoNeutrals.Gray700,
                rules = listOf(
                    GrammarRule(
                        "一类动词",
                        "【小白必杀技】把「ない形」的「ない」去掉，换成「される」即可。（※词尾是「す」时只能加「させられる」）",
                        listOf("書く → [書かない] → 書かされる (被逼写)", "話す → [話さない] → 話させられる (特殊)")
                    ),
                    GrammarRule("二类动词", "去掉词尾的「る」 + させられる。", listOf("食べる → 食べさせられる (被逼着吃)")),
                    GrammarRule("三类动词", "特殊记忆。", listOf("する → させられる", "来る → 来させられる (こさせられる)"))
                )
            )
        )
    }

    // 优雅的展开平滑滚动黄金锚定线逻辑
    LaunchedEffect(expandedId) {
        expandedId?.let { id ->
            // 等待手风琴展开测量就绪
            kotlinx.coroutines.delay(120)
            itemPositions[id]?.let { yPosition ->
                // 精准锚定在顶部下方大约 72.dp 处，避开固定的标题栏，营造极佳的视觉阅读焦点
                val targetScroll = (yPosition - 180).coerceAtLeast(0)
                scrollState.animateScrollTo(
                    value = targetScroll,
                    animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
                )
            }
        }
    }

    Scaffold(
        topBar = {
            CommonHeader(
                title = "日语动词活用大全",
                onBack = onNavigateBack
            )
        },
        containerColor = containerColor
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // 前置知识卡片：动词三大分类
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = cardBgColor),
                elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 0.dp else 1.dp),
                border = microBorderStroke
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(bottom = 12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Book,
                            contentDescription = null,
                            tint = if (isDark) IosColors.BlueDark else IosColors.Blue,
                            modifier = Modifier.size(20.dp)
                        )
                        // NotoSerifJP 排版美学：主卡片标题增强易读性字重与字号 (18.sp)
                        Text(
                            text = "必读：动词三大分类",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = NotoSerifJP,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold
                            ),
                            color = textMain
                        )
                    }
                    Text(
                        text = "所有的变形规则都是基于这三类动词划分的，学习前必须熟记：",
                        style = MaterialTheme.typography.bodyMedium,
                        color = textSub,
                        modifier = Modifier.padding(bottom = 16.dp),
                        lineHeight = 20.sp
                    )

                    // 1, 2, 3 分类卡片内容
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        VerbGroupItem(
                            number = "1",
                            title = "一类动词 (五段)：",
                            desc = "词尾是「う」段假名。(如: 買う, 書く)",
                            tintColor = if (isDark) IosColors.BlueDark else IosColors.Blue,
                            isDark = isDark
                        )
                        VerbGroupItem(
                            number = "2",
                            title = "二类动词 (一段)：",
                            desc = "词尾是「る」，且倒数第二音在い或え段。(如: 食べる, 見る)",
                            tintColor = if (isDark) IosColors.GreenDark else IosColors.Green,
                            isDark = isDark
                        )
                        VerbGroupItem(
                            number = "3",
                            title = "三类动词 (不规则)：",
                            desc = "只有两个词：する (做), 来る (来)。",
                            tintColor = if (isDark) IosColors.PurpleDark else IosColors.Purple,
                            isDark = isDark
                        )
                    }

                    // 特殊一类避坑警告卡片
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isDark) Color(0xFF3B1E1E) else Color(0xFFFFF1F2))
                            .border(0.5.dp, if (isDark) Color(0xFF7F1D1D).copy(alpha = 0.5f) else Color(0xFFFFE4E6), RoundedCornerShape(16.dp))
                            .padding(14.dp)
                    ) {
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(bottom = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ReportProblem,
                                    contentDescription = null,
                                    tint = if (isDark) IosColors.RedDark else IosColors.Red,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "避坑警告：特殊一类动词",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontFamily = NotoSerifJP,
                                        fontWeight = FontWeight.ExtraBold
                                    ),
                                    color = if (isDark) IosColors.RedDark else IosColors.Red
                                )
                            }
                            Text(
                                text = "有极少数动词长着“二类动词”的脸（以「る」结尾且倒数第二音在「い/え」段），但它们其实是 一类动词！必须死记，否则变形全错。",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isDark) NemoNeutrals.DarkTextSecondary else Color(0xFF9F1239),
                                lineHeight = 16.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isDark) Color(0xFF4C1D1D) else Color(0xFFFFE4E6).copy(alpha = 0.5f))
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = "常见：帰る(回家)、切る(切)、知る(知道)、入る(进入)、走る(跑)",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color(0xFFFDA4AF) else Color(0xFFBE123C)
                                )
                            }
                        }
                    }
                }
            }

            // 13 种变形手风琴折叠列表
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(bottom = 32.dp)
            ) {
                grammarList.forEach { item ->
                    val isExpanded = expandedId == item.id
                    val themeColor = if (isDark) item.colorDark else item.colorLight
                    val themeBgColor = if (isDark) item.bgColorDark else item.bgColorLight

                    // 序号前缀常驻对应主题色，标题使用 NotoSerifJP 明朝体，字号升级为微增大的 17.sp 以保障最佳易读性
                    val cardTitleAnnotated = remember(item, isExpanded, themeColor, textMain) {
                        buildAnnotatedString {
                            withStyle(SpanStyle(
                                color = themeColor, // 数字序号始终色彩高亮
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = NotoSerifJP,
                                fontSize = 17.sp
                            )) {
                                append(item.prefix)
                            }
                            withStyle(SpanStyle(
                                color = if (isExpanded) themeColor else textMain,
                                fontWeight = if (isExpanded) FontWeight.ExtraBold else FontWeight.Bold,
                                fontFamily = NotoSerifJP,
                                fontSize = 17.sp
                            )) {
                                append(item.title)
                            }
                        }
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .onGloballyPositioned { coordinates ->
                                // 实时捕获卡片在垂直布局中的y位置
                                itemPositions[item.id] = coordinates.positionInParent().y.toInt()
                            }
                            .border(
                                width = if (isExpanded) 1.5.dp else 0.5.dp,
                                color = if (isExpanded) themeColor.copy(alpha = 0.6f) else if (isDark) Color.White.copy(alpha = 0.08f) else NemoNeutrals.Gray200.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(20.dp)
                            ),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = cardBgColor),
                        elevation = CardDefaults.cardElevation(defaultElevation = if (isExpanded && !isDark) 2.dp else 0.dp)
                    ) {
                        Column {
                            // 折叠头部 (点击区域)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        expandedId = if (isExpanded) null else item.id
                                    }
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = cardTitleAnnotated,
                                        lineHeight = 22.sp
                                    )
                                    if (!isExpanded) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = item.usage,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = textSub,
                                            maxLines = 1
                                        )
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(if (isExpanded) themeBgColor else if (isDark) MaterialTheme.colorScheme.surfaceVariant else NemoNeutrals.Gray100),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                                        contentDescription = if (isExpanded) "收起" else "展开",
                                        tint = if (isExpanded) themeColor else textSub,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            // 展开内容区
                            AnimatedVisibility(
                                visible = isExpanded,
                                enter = expandVertically(animationSpec = tween(300)) + fadeIn(animationSpec = tween(300)),
                                exit = shrinkVertically(animationSpec = tween(300)) + fadeOut(animationSpec = tween(300))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(if (isDark) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f) else NemoNeutrals.Gray50)
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    // 用法说明卡片
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(cardBgColor)
                                            .border(microBorderStroke)
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Info,
                                            contentDescription = null,
                                            tint = themeColor,
                                            modifier = Modifier
                                                .size(16.dp)
                                                .padding(top = 2.dp)
                                        )
                                        Text(
                                            text = item.usage,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = textMain,
                                            lineHeight = 18.sp
                                        )
                                    }

                                    // 变形规则说明及举例
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        item.rules.forEach { ruleBlock ->
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(14.dp))
                                                    .background(cardBgColor)
                                                    .border(microBorderStroke)
                                            ) {
                                                // 规则子标题
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .background(if (isDark) MaterialTheme.colorScheme.surfaceContainerHighest else NemoNeutrals.Gray100)
                                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                                ) {
                                                    Text(
                                                        text = ruleBlock.group,
                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                            fontFamily = NotoSerifJP,
                                                            fontWeight = FontWeight.Bold
                                                        ),
                                                        color = textMain
                                                    )
                                                }

                                                // 规则正文
                                                Column(
                                                    modifier = Modifier.padding(12.dp),
                                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Text(
                                                        text = ruleBlock.rule,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = textMain,
                                                        lineHeight = 18.sp,
                                                        fontWeight = FontWeight.Medium
                                                    )

                                                    // 变形举例
                                                    ruleBlock.examples.forEach { example ->
                                                        Row(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .clip(RoundedCornerShape(10.dp))
                                                                .background(themeBgColor)
                                                                .padding(horizontal = 10.dp, vertical = 8.dp),
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Rounded.CheckCircle,
                                                                contentDescription = null,
                                                                tint = themeColor.copy(alpha = 0.8f),
                                                                modifier = Modifier.size(14.dp)
                                                            )
                                                            Text(
                                                                text = example,
                                                                style = MaterialTheme.typography.labelSmall,
                                                                fontWeight = FontWeight.Bold,
                                                                color = themeColor,
                                                                letterSpacing = 0.5.sp
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VerbGroupItem(
    number: String,
    title: String,
    desc: String,
    tintColor: Color,
    isDark: Boolean
) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(tintColor.copy(alpha = if (isDark) 0.15f else 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.ExtraBold,
                color = tintColor
            )
        }
        Column(
            modifier = Modifier.weight(1f)
        ) {
            // NotoSerifJP 排版美学：列表子标题强化字重与字号 (15.sp)
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = NotoSerifJP,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold
                ),
                color = if (isDark) NemoNeutrals.DarkTextPrimary else NemoNeutrals.Gray800
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = desc,
                style = MaterialTheme.typography.labelSmall,
                color = if (isDark) NemoNeutrals.DarkTextSecondary else NemoNeutrals.Gray600,
                lineHeight = 15.sp
            )
        }
    }
}
