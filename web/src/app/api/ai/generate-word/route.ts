import { NextResponse } from 'next/server';

export async function POST(req: Request) {
  try {
    const { word, level } = await req.json();

    if (!word) {
      return NextResponse.json({ error: 'Word is required' }, { status: 400 });
    }

    const apiKey = process.env.DEEPSEEK_API_KEY;
    const baseUrl = process.env.DEEPSEEK_BASE_URL || 'https://api.deepseek.com';
    const model = process.env.DEEPSEEK_MODEL || 'deepseek-chat';

    const systemPrompt = `你是一位资深的日语教育专家。请为用户提供的日语单词提供详细的词典信息。
要求：
1. 提供准确的假名 (hiragana)。
2. 提供准确的中文释义 (chinese)。
3. 判断该词属于哪个日语等级 (level: N1, N2, N3, N4, N5)。
4. 判断词性 (pos)。请从以下指定的 75 种词性列表中选择最精确的一个，必须精确匹配，不能生成该列表之外的任何词性：
["名", "名*他動3", "名*自動3", "他動1", "副", "名*ナ形", "自動1", "他動2", "イ形", "接尾", "自動2", "名*自他動3", "ナ形", "名*副", "副*自動3", "接頭", "接", "自他動1", "嘆", "代", "連体", "名*ナ形*自動3", "連語", "副*ナ形", "自他動2", "ナ形*副", "名*接尾", "他動3", "自動3", "自他動3", "名*ナ形*副", "副*ナ形*自動3", "名*ナ形*他動3", "助", "名*副*ナ形", "代*副", "名*代", "副*嘆", "接尾*名", "名*他動3*副", "ナ形*副*自動3", "名*他動3*ナ形", "副*自動3*ナ形", "ナ形*自動3", "名*助", "副*接", "名*自動1", "代*名", "接続", "名*代*副", "副*他動3", "名*奉承", "自動1*礼貌", "名*副*代", "名*他動3*接尾", "副*名", "ナ形*副*名*自動3", "連語*叹", "名*自他動1", "他動2*奉承", "名*接", "副*名*ナ形", "接*副", "嘆*連語", "嘆*名*自動3", "嘆*副*ナ形", "名*ナ形*自他動3", "名*接頭", "他動1/他動3", "他動1*尊敬", "名*尊称", "名*副*接", "名*ナ形*礼貌", "助*嘆", "イ形*接尾"]。
5. 提供 3 个实用的例句及对应的中文翻译。例句中的日语汉字必须使用中括号标注假名注音，格式为：汉字[假名]（例如：“日本語[にほんご]”、“聞[き]き入[い]れる”）。请注意只在汉字后加注音，不要在纯假名或标点后加注音。

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
