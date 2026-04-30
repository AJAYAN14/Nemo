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
4. 判断词性 (pos: 名词, 动词, 形容词, 形容动词, 副词, 连体词, 接续词, 感叹词, 助词, 助动词)。
5. 提供 3 个实用的例句及对应的中文翻译。

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
