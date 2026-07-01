---
name: frontend-design
description: Create distinctive, production-grade frontend interfaces with high design quality. Use this skill when the user asks to build web components, pages, artifacts, posters, or applications (examples include websites, landing pages, dashboards, React components, HTML/CSS layouts, or when styling/beautifying any web UI). Generates creative, polished code and UI design that avoids generic AI aesthetics. 及时上网搜索学习新技术和方法。
version: 2.0.0
license: Complete terms in LICENSE.txt
---

## 技能执行流程图

```mermaid
flowchart TB
    START(["触发"]) --> TIME["记录当前时间"]
    TIME --> INPUT{"输入来源?"}

    INPUT -->|设计规范| ID["interface-design<br/>Design Tokens"]
    INPUT -->|技术选型| FD["fractal-designer<br/>前端技术方案"]
    INPUT -->|用户直接| DESC["页面/组件描述"]

    ID --> BUILD["构建界面<br/>CSS变量+组件+样式"]
    FD --> BUILD
    DESC --> BUILD

    BUILD --> FUT["frontend-ui-test<br/>界面验证"]
    FUT --> FCR["frontend-code-review<br/>代码审查"]

    FCR --> CACHE{"Next.js项目?"}
    CACHE -->|是| CC["cache-components<br/>缓存优化"]
    CACHE →|否| OUTPUT(["输出: 高质量前端代码"])
    CC --> OUTPUT

    style START fill:#4CAF50
    style OUTPUT fill:#2196F3
```

This skill guides creation of distinctive, production-grade frontend interfaces that avoid generic "AI slop" aesthetics. Implement real working code with exceptional attention to aesthetic details and creative choices.

The user provides frontend requirements: a component, page, application, or interface to build. They may include context about the purpose, audience, or technical constraints.

## Design Thinking

Before coding, understand the context and commit to a BOLD aesthetic direction:
- **Purpose**: What problem does this interface solve? Who uses it?
- **Tone**: Pick an extreme: brutally minimal, maximalist chaos, retro-futuristic, organic/natural, luxury/refined, playful/toy-like, editorial/magazine, brutalist/raw, art deco/geometric, soft/pastel, industrial/utilitarian, etc. There are so many flavors to choose from. Use these for inspiration but design one that is true to the aesthetic direction.
- **Constraints**: Technical requirements (framework, performance, accessibility).
- **Differentiation**: What makes this UNFORGETTABLE? What's the one thing someone will remember?

**CRITICAL**: Choose a clear conceptual direction and execute it with precision. Bold maximalism and refined minimalism both work - the key is intentionality, not intensity.

Then implement working code (HTML/CSS/JS, React, Vue, etc.) that is:
- Production-grade and functional
- Visually striking and memorable
- Cohesive with a clear aesthetic point-of-view
- Meticulously refined in every detail

## Frontend Aesthetics Guidelines

Focus on:
- **Typography**: Choose fonts that are beautiful, unique, and interesting. Avoid generic fonts like Arial and Inter; opt instead for distinctive choices that elevate the frontend's aesthetics; unexpected, characterful font choices. Pair a distinctive display font with a refined body font.
- **Color & Theme**: Commit to a cohesive aesthetic. Use CSS variables for consistency. Dominant colors with sharp accents outperform timid, evenly-distributed palettes.
- **Motion**: Use animations for effects and micro-interactions. Prioritize CSS-only solutions for HTML. Use Motion library for React when available. Focus on high-impact moments: one well-orchestrated page load with staggered reveals (animation-delay) creates more delight than scattered micro-interactions. Use scroll-triggering and hover states that surprise.
- **Spatial Composition**: Unexpected layouts. Asymmetry. Overlap. Diagonal flow. Grid-breaking elements. Generous negative space OR controlled density.
- **Backgrounds & Visual Details**: Create atmosphere and depth rather than defaulting to solid colors. Add contextual effects and textures that match the overall aesthetic. Apply creative forms like gradient meshes, noise textures, geometric patterns, layered transparencies, dramatic shadows, decorative borders, custom cursors, and grain overlays.

NEVER use generic AI-generated aesthetics like overused font families (Inter, Roboto, Arial, system fonts), cliched color schemes (particularly purple gradients on white backgrounds), predictable layouts and component patterns, and cookie-cutter design that lacks context-specific character.

Interpret creatively and make unexpected choices that feel genuinely designed for the context. No design should be the same. Vary between light and dark themes, different fonts, different aesthetics. NEVER converge on common choices (Space Grotesk, for example) across generations.

**IMPORTANT**: Match implementation complexity to the aesthetic vision. Maximalist designs need elaborate code with extensive animations and effects. Minimalist or refined designs need restraint, precision, and careful attention to spacing, typography, and subtle details. Elegance comes from executing the vision well.

Remember: TRAE is capable of extraordinary creative work. Don't hold back, show what can truly be created when thinking outside the box and committing fully to a distinctive vision.
- ⚠️ **Search Agent 只用于搜索**：无写文件权限，不做文档修改/分析
- **每次操作记录时间戳**，涉及新前端技术/框架时使用 WebSearch

---

## 技能协作接口

### 在技能体系中的定位

```
[interface-design 设计方案] → [frontend-design] → [frontend-ui-test 界面验证]
[fractal-designer 前端选型]  →                     → [frontend-code-review 代码审查]
                                    ↓
                              [cache-components 缓存优化]（Next.js项目）
```

**本角色**：高质量前端界面构建技能，将设计方案转化为具有独特美感和卓越用户体验的前端代码。拒绝千篇一律的AI审美。

### 上游输入

| 上游技能 | 输入数据 | 使用方式 |
|----------|----------|----------|
| **interface-design** | Design System 定义 + 界面规格 | 视觉规范的核心输入 |
| **fractal-designer** | 前端技术选型 + UI/UX设计文档 | 技术方案和交互参考 |
| 用户直接输入 | 页面/组件描述 + 风格偏好 | 直接启动前端构建 |

### 下游输出

| 输出内容 | 消费者 | 使用方式 |
|----------|--------|----------|
| 前端代码（HTML/CSS/React） | fullstack-developer | 集成到项目中 |
| 组件实现 | frontend-ui-test | 界面效果验证 |
| 样式代码 | frontend-code-review | 前端专项审查 |
| Next.js组件（如适用） | cache-components | 缓存优化 |

### 协作协议

#### ← interface-design
- **核心对接**：接收 Design Tokens（颜色/字体/间距/阴影等）
- **转化规则**：将设计语言转化为 CSS 变量体系 + 组件样式

#### → frontend-ui-test
- **触发时机**：每次界面构建完成后
- **验证重点**：视觉还原度、布局正确性、响应式、交互反馈

#### → frontend-code-review
- **审查重点**：CSS架构、组件结构、性能（动画/渲染）、可访问性

#### ↔ cache-components（条件协作）
- **适用条件**：Next.js 项目且启用 cacheComponents
- **集成点**：RSC 编写时应用缓存模式，SSC 使用 cacheLife()

### 设计原则对接

| interface-design 原则 | frontend-design 实践 |
|----------------------|----------------------|
| Design Tokens | CSS Variables / Tailwind Config |
| 布局系统 | CSS Grid / Flexbox 自由组合 |
| 字体层级 | 独特字体选择（避免 Inter/Roboto/Arial） |
| 色彩体系 | 大胆配色 + 锐利强调色 |
| 动效设计 | CSS animations + Motion(React) |
| 空间构图 | 非对称、重叠、对角线流动 |

### 协作约束

- **拒绝平庸**：绝不使用通用AI审美模式
- **每案不同**：每个设计都应有独特的视觉性格
- **匹配复杂度**：极简设计需要精准克制，华丽设计需要精心编排
- **技术落地**：美学愿景必须有匹配的实现复杂度
