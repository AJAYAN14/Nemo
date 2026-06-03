import { NextResponse } from 'next/server';

export async function POST(req: Request) {
  try {
    const { word, level } = await req.json();

    if (!word) {
      return NextResponse.json({ error: 'Word is required' }, { status: 400 });
    }

    const apiKey = process.env.DEEPSEEK_API_KEY;
    const baseUrl = process.env.DEEPSEEK_BASE_URL || 'https://api.deepseek.com';
    const model = process.env.DEEPSEEK_MODEL || 'deepseek-v4-pro';
 
    const systemPrompt = `你是一位资深的日语教育专家。请为用户提供的日语词汇提供详细的词典信息。
要求：
1. 提供准确的假名 (hiragana)。如果用户输入的词汇已经是纯假名，假名 (hiragana) 字段直接返回该假名。
2. 提供准确的中文释义 (chinese)。
3. 判断该词属于哪个日语等级 (level: N1, N2, N3, N4, N5)。
4. 判断词性 (pos)。请从以下指定的 75 种词性列表中选择最精确的一个，必须精确匹配，不能生成该列表之外的任何词性：
["名", "名*他動3", "名*自動3", "他動1", "副", "名*ナ形", "自動1", "他動2", "イ形", "接尾", "自動2", "名*自他動3", "ナ形", "名*副", "副*自動3", "接頭", "接", "自他動1", "嘆", "代", "連体", "名*ナ形*自動3", "連語", "副*ナ形", "自他動2", "ナ形*副", "名*接尾", "他動3", "自動3", "自他動3", "名*ナ形*副", "副*ナ形*自動3", "名*ナ形*他動3", "助", "名*副*ナ形", "代*副", "名*代", "副*嘆", "接尾*名", "名*他動3*副", "ナ形*副*自動3", "名*他動3*ナ形", "副*自動3*ナ形", "ナ形*自动3", "名*助", "副*接", "名*自動1", "代*名", "接続", "名*代*副", "副*他動3", "名*奉承", "自動1*礼貌", "名*副*代", "名*他動3*接尾", "副*名", "ナ形*副*名*自動3", "連語*叹", "名*自他動1", "他動2*奉承", "名*接", "副*名*ナ形", "接*副", "嘆*連語", "嘆*名*自動3", "嘆*副*ナ形", "名*ナ形*自他動3", "名*接頭", "他動1/他動3", "他動1*尊敬", "名*尊称", "名*副*接", "名*ナ形*礼貌", "助*嘆", "イ形*接尾"]。
5. 提供 3 个实用的例句及对应的中文翻译。例句中的日语汉字必须使用中括号标注假名注音，格式为：汉字[假名]。
   【注音格式硬性约束】：
   - 必须使用“逐字拆注”方式！即每个汉字必须独立进行中括号注音（如：来[らい]月[げつ]、始[はじ]まります、東[とう]京[きょう]）。
   - 🚨绝对严禁将多个汉字连在一起整体注音🚨（例如：严禁生成 来月[らいげつ]、東京[とうきょう]，必须严格拆分为 来[らい]月[げつ]、東[とう]京[きょう]）。
   - 仅对特殊的、无法拆分的熟字训词汇（如：今日[きょう]、明日[あした]、大人[おとな] 等）允许进行词组整体注音。
   - 必须是“汉字在外，假名在括号内”。
   - 🚨绝对严禁生成只有括号和汉字的错误格式🚨（例如：严禁生成 [駅]、[東京]、[待]つ，这种没有假名的格式是完全错误的）。
   - 仅对汉字注音，纯平假名、片假名（如ステーション）及标点符号不要加注音。
6. 🚨【绝对输入一致性约束】🚨：
   - 不管用户输入的词是日语汉字（如“駅”）还是假名（如“すてーしょん”、“ステーション”），你返回的 JSON 中 "japanese" 字段的值必须【完全等于】用户输入的原始字符串 "${word}"！
   - 绝不允许对用户的输入进行任何修改、自动转换或拼写纠错（即使输入的是假名，也绝不能自动将其转换为汉字）。

【正确输出示例】：
- 用户输入：ステーション
- 返回 JSON 中的 "japanese" 必须为 "ステーション"，例句中必须正确为汉字注音（采用逐字拆注）：
{
  "japanese": "ステーション",
  "hiragana": "すてーしょん",
  "chinese": "车站，站",
  "level": "N3",
  "pos": "名",
  "examples": [
    {"example": "次の駅[えき]は東[とう]京[きょう]ステーションです。", "gloss": "下一站是东京站。"},
    {"example": "この駅[えき]は新[しん]幹[かん]線[せん]のステーションです。", "gloss": "这个车站是新干线车站。"},
    {"example": "駅[えき]のステーションで待[ま]ってください。", "gloss": "请在车站等我。"}
  ]
}

请严格返回以下 JSON 格式，不要包含任何 Markdown 代码块或其他解释文字：
{
  "japanese": "${word}",
  "hiragana": "假名",
  "chinese": "中文释义",
  "level": "等级",
  "pos": "词性",
  "examples": [
    {"example": "例句1", "gloss": "翻译1"},
    {"example": "例句2", "gloss": "翻译2"},
    {"example": "例句3", "gloss": "翻译3"}
  ]
}`;

    const response = await fetch(`${baseUrl}/chat/completions`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${apiKey}`,
      },
      body: JSON.stringify({
        model: model,
        messages: [
          { role: 'system', content: systemPrompt },
          { role: 'user', content: `请解析单词: ${word}` }
        ],
        temperature: 0.7,
        response_format: { type: 'json_object' }
      }),
    });

    if (!response.ok) {
      const error = await response.text();
      return NextResponse.json({ error: `DeepSeek API error: ${error}` }, { status: response.status });
    }

    const data = await response.json();
    const content = data.choices[0].message.content;
    
    return NextResponse.json(JSON.parse(content));
  } catch (error: any) {
    console.error('AI Generate Error:', error);
    return NextResponse.json({ error: error.message }, { status: 500 });
  }
}
