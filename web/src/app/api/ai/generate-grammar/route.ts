import { NextResponse } from 'next/server';

export async function POST(req: Request) {
  try {
    const { title, level } = await req.json();

    if (!title) {
      return NextResponse.json({ error: 'Title is required' }, { status: 400 });
    }

    const apiKey = process.env.DEEPSEEK_API_KEY;
    const baseUrl = process.env.DEEPSEEK_BASE_URL || 'https://api.deepseek.com';
    const model = process.env.DEEPSEEK_MODEL || 'deepseek-v4-pro';
 
    const systemPrompt = `你是一位资深的日语教育专家。请为用户提供的日语语法提供详细的词典信息。
要求：
1. 提供该语法的级别（如 N1, N2, N3, N4, N5），如果传入了默认 level，可优先参考。
2. 提供该语法的详细用法（usages），可以有多个用法，每个用法包含：
   - connection: 接续方法（如 "名词 + にあって"）
   - explanation: 含义说明（该用法的具体含义，用于什么场景）
   - notes: 备注/注意（补充说明、语气、褒贬义等）
   - examples: 提供 2 到 3 个实用的例句及对应的中文翻译。例句中的日语汉字必须使用中括号标注假名注音，格式为：汉字[假名]。
3. 【注音格式硬性约束】：
   - 必须使用“逐字拆注”方式！即每个汉字必须独立进行中括号注音（如：来[らい]月[げつ]）。
   - 🚨绝对严禁将多个汉字连在一起整体注音🚨（例如：严禁生成 来月[らいげつ]）。
   - 仅对特殊的、无法拆分的熟字训词汇（如：今日[きょう]、明日[あした]、大人[おとな] 等）允许进行词组整体注音。
   - 必须是“汉字在外，假名在括号内”。
   - 仅对汉字注音，纯平假名、片假名及标点符号不要加注音。

【正确输出示例】：
- 用户输入：～あっての
{
  "level": "N1",
  "usages": [
    {
      "connection": "名词 + あっての + 名词",
      "explanation": "正因为有前项，才会有后项。强调前项是后项成立的绝对条件。",
      "notes": "多用于强调某事物的价值或意义。常带有感激、赞赏的语气。",
      "examples": [
        {
          "sentence": "お客[きゃく]様[さま]あっての商[あきな]いです。",
          "translation": "正因为有客户，才会有生意。"
        },
        {
          "sentence": "健[けん]康[こう]あっての人生[じんせい]だから、無[む]理[り]はしないでください。",
          "translation": "健康是人生的基础，所以请不要勉强自己。"
        }
      ]
    }
  ]
}

请严格返回上述 JSON 格式，不要包含任何 Markdown 代码块或其他解释文字。`;

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
          { role: 'user', content: `请解析语法: ${title} (预期等级: ${level})` }
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
