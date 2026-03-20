# 文文Lex

文文Lex 是一个原生 Android 背单词 App，使用 Kotlin + Jetpack Compose 构建，主打离线学习、真实内置词库、Excel 导入、AI API 可选增强和持续可迭代的 GitHub 云端发版流程。

## 当前状态

- 当前正式版本：`1.2`
- 版本规则：每次迭代递增 `0.1`，按 `1.0 -> 1.1 -> ... -> 1.9 -> 2.0` 进位
- 正式发包界面：[GitHub Releases](https://github.com/yuelangmanle/wenwenlex/releases)
- 开发验证界面：[GitHub Actions](https://github.com/yuelangmanle/wenwenlex/actions)

## 协作文档入口

- [协作开发书](docs/collaboration-handbook.md)
- [开发进度书](docs/development-progress.md)
- [发版签名说明](docs/release-signing.md)
- [v1.1 产品规格](docs/superpowers/specs/2026-03-19-v1.1-lexicon-import-ai-design.md)
- [v1.1 实施计划](docs/superpowers/plans/2026-03-19-v1.1-lexicon-import-ai.md)
- [v1.2 发音模块规格](docs/superpowers/specs/2026-03-19-v1.2-pronunciation-design.md)
- [v1.2 发音模块计划](docs/superpowers/plans/2026-03-19-v1.2-pronunciation.md)
- [v1.3 离线单词真实发音规格](docs/superpowers/specs/2026-03-19-v1.3-offline-word-pronunciation-design.md)
- [v1.3 离线单词真实发音计划](docs/superpowers/plans/2026-03-19-v1.3-offline-word-pronunciation.md)

## 1.1 已实现能力

- 首页、学习、词书、我的 四大主导航
- 卡片学习流、选择题复习流、单词详情页与结构化学习记录
- 单词详情中的近义词、反义词、拼写相近词、易混词、单词变形、词根词缀展示
- Excel 词书导入，支持 `.xlsx`、A 列单词 / B 列中文义、中英文逗号拆分
- AI 适配不规整 Excel 表格结构，并把预览后的结果确认导入
- 词书详情页，支持设为当前词书、查看音标覆盖率、批量补空白音标、覆盖全部音标
- 多 API 档案管理，支持本地加密保存多条 API Key
- 全局默认 API 与功能级覆盖切换：
  - 词条讲解
  - 计划调整
  - 音标补全
- 真实内置词库：
  - 四级 `3844`
  - 六级 `5401`
  - 考研 `4796`
  - 高中 `3666`
  - 雅思基础 `5026`
- 本地 ZIP 备份与恢复，备份版本升级到 `v3`，兼容导入 `v1` / `v2`
- 每日提醒、本地 AI 摘要整理与 WorkManager 后台任务

## 1.2 已发布能力

- 发音模块数据库 `v3` 结构和备份位已接入
- 发音设置页入口已加入“我的”页
- 单词详情页新增英式 / 美式发音按钮
- 学习页新增发音播放入口
- 词典音频缓存仓库、播放调度器和系统 TTS 兜底已落地
- 发音设置页已支持内置语音包清单刷新、下载 / 安装 / 删除与默认激活
- 当前提供英式 / 美式两套桥接语音包样例，可走一键安装并接入本地朗读链路
- 首页与词书页已切回真实词库加载链路，不再停留在长时间转圈状态
- 离线语音包当前先以 `system_tts_bridge` 方式落地，真正的本地模型推理包仍在后续迭代

## 1.3 开发中能力

以下内容已经在开发分支落地并通过 GitHub Actions 云端 CI，但还没有正式发版：

- native 语音包运行时 metadata 合并，可从安装包 `manifest.json` 读取模型族、资源占用和许可证信息
- Sherpa ONNX Android runtime 脚手架与原生离线单词发音引擎骨架
- 单词发音归一化与本地生成音频缓存，支持优先复用 native 生成结果
- 发音调度器升级为：本地缓存优先、native 生成缓存次之，再回落在线词典和系统 TTS
- native 语音包安装阶段的 `licenses`、`entryFiles` 和 native metadata 校验
- 发音设置页展示 native 语音包能力提示、资源占用和安装失败具体原因

## 后续增强方向

- 真正的本地离线 TTS 推理语音包
- native 语音包正式发版与模型资产分发
- 更丰富的在线词典音频源与缓存命中策略
- 学习统计与发音使用数据的可视化面板

## 内置词库来源

- 当前内置词库底座：ECDICT 标签词库
- 许可证：MIT
- 生成脚本：[scripts/generate_lexicons.py](scripts/generate_lexicons.py)
- 生成结果位置：`app/src/main/assets/books/*.json`

## 云端发包与更新界面

- Releases 页面：
  [GitHub Releases](https://github.com/yuelangmanle/wenwenlex/releases)
- Actions 页面：
  [GitHub Actions](https://github.com/yuelangmanle/wenwenlex/actions)

以后正式版本都通过 GitHub 云端工作流发包，不依赖本地手工打包。

## 安装与升级

### 首次安装

1. 前往 [GitHub Releases](https://github.com/yuelangmanle/wenwenlex/releases) 下载最新 APK。
2. 把 APK 传到 Android 手机，直接点开安装。
3. 如果系统提示“禁止未知来源应用安装”，给浏览器、文件管理器或聊天工具开启“允许安装未知应用”。

### 日常升级

1. 从 Releases 页面下载更高版本，例如 `1.2`、`1.3`。
2. 直接安装新 APK，Android 会覆盖旧版本，学习数据会保留。
3. 升级前仍建议先在 App 里做一次本地备份。

### 从旧调试包切换到正式包

- 如果你手机里装的是早期 `debug` 内测包，切到新的 `release-signed` 正式包时，Android 可能提示签名不一致。
- 遇到这种情况，需要先在 App 内完成备份，然后卸载旧包，再安装新的 Release 包，最后恢复备份。

## 发版流程

1. 在 [CHANGELOG.md](CHANGELOG.md) 里新增对应版本的小节，先写完整更新日志。
2. 把代码推到目标分支。
3. 在 GitHub Actions 里运行 `Android Release` 工作流，并输入版本号，例如 `1.2`。
4. 工作流会在云端完成：
   - 校验版本号格式
   - 运行单元测试
   - 读取 GitHub Secrets 中的正式签名材料
   - 构建并校验 `release-signed APK`
   - 创建或更新 GitHub Release
   - 上传安装包到 Release 下载页
5. 用户以后只需要去 Releases 页面看版本列表、更新日志和下载入口。

### 两种触发方式

- 推荐：GitHub 网页中手动运行 `Android Release`，输入版本号
- 兼容：推送 `v1.2`、`v1.3` 这样的 tag，也会自动创建对应 Release

## 版本与日志约束

- 版本号固定使用一位小数，例如 `1.0`、`1.1`、`2.0`
- 每次发版前必须先更新 `CHANGELOG.md`
- Release 页面正文直接从 `CHANGELOG.md` 对应版本段落生成
- 当前云端发布产物为 `release-signed APK`

## 签名密钥与 GitHub Secrets

正式升级链路依赖同一套签名密钥。只要以后继续沿用同一把 key，用户就可以持续覆盖升级，不会丢数据。

- 详细说明见 [release-signing.md](docs/release-signing.md)
- GitHub 仓库需要这 4 个 secrets：
  - `ANDROID_RELEASE_KEYSTORE_BASE64`
  - `ANDROID_RELEASE_STORE_PASSWORD`
  - `ANDROID_RELEASE_KEY_ALIAS`
  - `ANDROID_RELEASE_KEY_PASSWORD`

## 本地兜底

虽然正式主流程走 GitHub 云端发包，但本地仍保留基础环境兜底：

- JDK 17
- Android SDK
- Gradle Wrapper

这能避免云端排队、网络波动或 GitHub 临时异常时完全失去打包能力。
