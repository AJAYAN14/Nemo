# 应用版本发布全流程闭环与后端配置及GitHub-Release避坑指南

> 记录 Nemo 应用从 Android 版本号升级、Release 签名包构建、GitHub Git 裸标签向官方正式 Release 进阶，到 Supabase 云端 `app_config` 数据库配置与 CDN 链接闭环的完整运维与避坑经验。

---

## 核心经验对照表

| 坑是什么 | 怎么避 | 相关工具 / 文件 | 统一原则 |
| :--- | :--- | :--- | :--- |
| **仅推送 Git Tag 导致 GitHub 首页右侧 Releases 栏与 Releases 页面不更新** | 区分 Git 裸标签（Bare Tag）与 GitHub Release：使用 GitHub REST API（或网页端）基于该 Tag 显式创建 Release（配置标题、Markdown 正文并设 `make_latest = "true"`），并将编译好的 Release APK 挂载为 Asset 附件，才能点亮 `Notes`、`Downloads` 与 `Latest` 标识。 | `api.github.com/repos/.../releases`, `uploads.github.com/.../assets`, `git tag` | **版本资产与发行元数据强绑定公理**：版本发布必须是 Tag、Release 元数据与二进制编译制品三位一体的闭环交付，杜绝仅有代码标签无发行制品的半截发布。 |
| **PowerShell 命令行请求 REST API 导致中文更新日志乱码为问号 `???`** | 在 Windows 环境下，PowerShell 发送 HTTP 请求时默认字符集可能导致 UTF-8 截断；必须显式在请求头声明 `Content-Type: application/json; charset=utf-8`，并通过 `[System.Text.Encoding]::UTF8.GetBytes($jsonString)` 将 Body 转换为二进制字节流发送。 | `Invoke-RestMethod`, `System.Text.Encoding`, `Supabase PostgREST` | **全链路字符集强类型约束原则**：涉及多语言文本交互的后端接口请求，必须在传输层固化 UTF-8 字节编码，严禁依赖系统平台隐式默认字符集。 |
| **后端配置 GitHub Raw 链接导致客户端下载 302 重定向与速度受限** | GitHub 仓库文件的 `raw` 链接在请求时会触发 HTTP 302 临时重定向，且受代码仓库访问速率限制；Release 资产上传完成后，应统一使用 GitHub 官方生成的 Releases 加速直链（`https://github.com/.../releases/download/vX.X.X/app-release.apk`）注入后端。 | `app_config.download_url`, `GitHub Release CDN` | **官方发行制品单一权威分发源原则**：应用安装包分发一律采用发行版专属 CDN 加速直链，杜绝借道源码仓库 Raw 路由导致的多余跳转与网络抖动。 |
| **版本递增未闭环导致客户端重复弹窗或检测失效** | 严格遵循单一递增流转：代码端 `app/build.gradle.kts` 中的 `versionCode` 必须与后端 `app_config.version_code` 保持严格步调。只有当后端配置大于已安装客户端版本时触发更新，安装新包后两者相等则自然停止弹窗，形成严密闭环。 | `app/build.gradle.kts`, `ConfigRepositoryImpl.kt`, `MainViewModel.kt` | **版本状态判定确定性公理**：版本号升级必须在源码端、构建包、代码库与云端配置四处保持全局绝对一致，防止出现版本回退或无限弹窗循环。 |
| **项目文档 README.md 与发布资产版本脱节** | 版本升级时必须同步更新仓库根目录 `README.md` 的版本徽章（Badge）、顶部最新版本发布公告卡片、APK 体积信息与直链下载地址，确保开发者与访客在首页第一眼获取的始终为最新版本。 | `README.md`, `shields.io` | **项目门面信息与制品版本原子化同步原则**：代码发布必须伴随对外文档的原子级同步，杜绝“代码已升版，文档仍留在旧时代”的信息不同步问题。 |

---

## 两大核心步骤全流程复盘

### 一、改后端（Supabase `app_config` 数据库精准配置）

1. **版本配置表结构与字段含义**：
   - `version_code` (Int)：版本整数代号，客户端通过 `config.versionCode > currentVersionCode` 判定是否弹窗。
   - `version_name` (Text)：用户可见的语义化版本号（如 `"1.2.0"`）。
   - `update_log` (Text)：多行更新说明，支持换行符 `\n`。
   - `download_url` (Text)：APK 安装包直链。
   - `is_force` (Boolean)：是否强制阻断性更新（`true` 时界面禁用取消，拦截返回键与阴影点击；`false` 允许用户关闭且后台静默继续下载）。
   - `can_close` (Boolean)：是否允许用户点击遮罩/返回键退出。
2. **安全与防乱码规范写入**：
   - 请求头需同时携带 `apikey` 与 `Authorization: Bearer <ANON_KEY>`；
   - 携带 `Prefer: return=representation` 以便即时核对写回结果；
   - 严格采用 UTF-8 字节编码构造请求体，防止日志中文变为乱码。

---

### 二、改 GitHub（从 Git 标签到正式 Release 发布）

1. **第一阶段：源码与发布制品推送**：
   - 在 `app/build.gradle.kts` 提升 `versionCode` 与 `versionName`；
   - 运行 `./gradlew :app:assembleRelease` 构建 Release 签名 APK；
   - 拷贝生成文件至 `release/nemo-vX.X-release.apk`，提交并 `git push origin main`。
2. **第二阶段：打标与推标签**：
   - 本地创建带注释的正式 Git Tag：
     ```bash
     git tag -a v1.2.0 -m "Release v1.2.0: ..."
     git push origin v1.2.0
     ```
   - **注意**：此时 GitHub 仅在 `/tags` 存在裸标签，主页与 `/releases` 页面尚不会将其认定为最新发行版。
3. **第三阶段：GitHub Release 正式发布与资产挂载**：
   - 调用 GitHub REST API 创建正式发行版：
     - 请求地址：`POST https://api.github.com/repos/:owner/:repo/releases`
     - 载荷参数：`tag_name: "v1.2.0"`, `name: "Nemo v1.2.0 - ..."`, `body: "..."`, `draft: false`, `prerelease: false`, `make_latest: "true"`
   - 挂载 APK 二进制附件：
     - 请求地址：`POST https://uploads.github.com/repos/:owner/:repo/releases/:release_id/assets?name=nemo-v1.2-release.apk`
     - 声明标头：`Content-Type: application/vnd.android.package-archive`，以二进制流直接上传 APK 文件；
   - 这一步完成后，GitHub 才会为该 Tag 赋予 `Notes` 与 `Downloads`，并将其正式置顶为 **`Latest`** 官方发布版本。
4. **第四阶段：项目文档同步**：
   - 更新 `README.md` 的徽章与卡片，并再次推送至主干。

---

## 端到端发布标准检查清单（Checklist）

- [x] 1. `app/build.gradle.kts` 的 `versionCode` 与 `versionName` 已递增。
- [x] 2. 执行 Release 构建且校验 APK 签名与体积正常。
- [x] 3. 根目录 `README.md` 徽章、版本说明与下载链接已原子级同步。
- [x] 4. 本地代码与 APK 资产已推送到 GitHub `main` 分支。
- [x] 5. 创建 Git Tag 并推送到远端。
- [x] 6. 基于 Tag 发布了正式的 GitHub Release 并设置 `make_latest: true`。
- [x] 7. Release 的 Assets 列表中已成功挂载 `.apk` 安装包。
- [x] 8. Supabase 后端 `app_config` 中的 `download_url` 已采用 GitHub Release 官方直链。
- [x] 9. 后端 `update_log` 中文字符集正常，无问号乱码。
- [x] 10. 客户端启动验证：版本检测准确触发居中弹窗，点击后胶囊进度条平滑下载并唤起安装。
