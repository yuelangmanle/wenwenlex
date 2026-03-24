# 文文Lex

文文Lex 是一个原生 Android 背单词 App，使用 Kotlin + Jetpack Compose 构建，主打离线学习、真实内置词库、Excel 导入、AI API 可选增强和持续可迭代的 GitHub 云端发版流程。

## 当前状态

- 当前正式版本：`2.0`
- 下一阶段主线：`2.1` 学习体验与音频体验增强收口
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

## 1.3 已发布能力

以下内容已经在 `2026-03-20` 随 `v1.3` 正式发版：

- native 语音包运行时 metadata 合并，可从安装包 `manifest.json` 读取模型族、资源占用和许可证信息
- Sherpa ONNX Android runtime 脚手架与原生离线单词发音引擎骨架
- 单词发音归一化与本地生成音频缓存，支持优先复用 native 生成结果
- 发音调度器升级为：本地缓存优先、native 生成缓存次之，再回落在线词典和系统 TTS
- 在线词典音频已支持多来源候选回退，当前默认接入 `Dictionary API` 和 `有道词典`
- native 语音包安装阶段的 `licenses`、`entryFiles` 和 native metadata 校验
- 发音设置页展示 native 语音包能力提示、资源占用和安装失败具体原因
- 单词详情页与学习页会直接显示本次发音实际命中的在线来源提示
- native 语音包正式分发链路已接入，GitHub Release 会同时上传 zip、manifest 和 checksum 资产
- `Android CI` / `Android Release` 已接入 native 语音包发布资产自检，避免 catalog 与 Release 资产脱节
- 当前 native 语音包依然先发 scaffold 包，用于正式分发、安装校验与链路联调；真实模型资产将在后续版本替换

## 1.9 已发布能力

以下能力已在 `2026-03-24` 随 `v1.9` 正式收口：

- 学习首页三个入口已改为由真实学习记录驱动：新词、复习、最近错词各走各自队列
- 音频缓存管理页已上线，可按来源查看占用并执行清理
- 音频任务中心已上线，可创建在线词典、云端 TTS、本地离线三类后台缓存任务
- 云端 TTS 已接入当前发音回退链路，并提供独立设置页
- 语音包下载已支持取消、断点续传、失败分类和备用下载源切换
- 词包 manifest 已支持多下载源定义，当前默认保留 `latest` 与固定 `v1.3` Release 两条路径

## 2.0 已发布修复

以下内容已在 `2026-03-24` 随 `v2.0` 正式发版：

- Room 数据库版本已统一提升到 `v9`，并补齐从 `v5`、`v6`、`v7`、`v8` 到当前结构的迁移路径
- 修复历史安装包升级后出现 `migration from 8 to 5` 的数据库兼容故障
- 首页、词书、学习、我的 四个主入口现在会对数据库加载异常给出中文可读提示
- 修复学习页和“我的”页在数据库迁移异常下直接闪退的问题
- 修复云端 CI 中学习页 instrumentation 状态与真实页面默认状态不一致导致的假失败
- 最新正式安装请以 `v2.0` Release 包为准，旧的异常安装包不会自行恢复

## 后续增强方向

- 学习体验主线进一步收口，避免新词 / 复习 / 最近错词边界混乱
- 更丰富的在线词典音频源、缓存批量任务与来源切换可视化
- 真正的本地离线 TTS 推理语音包
- 把当前 native scaffold 包替换为真实模型资产，并完成最终 runtime bridge 接线
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

1. 从 Releases 页面下载更高版本，例如 `2.0`、`2.1`。
2. 直接安装新 APK，Android 会覆盖旧版本，学习数据会保留。
3. 升级前仍建议先在 App 里做一次本地备份。

### 从旧调试包切换到正式包

- 如果你手机里装的是早期 `debug` 内测包，切到新的 `release-signed` 正式包时，Android 可能提示签名不一致。
- 遇到这种情况，需要先在 App 内完成备份，然后卸载旧包，再安装新的 Release 包，最后恢复备份。

## 发版流程

1. 在 [CHANGELOG.md](CHANGELOG.md) 里新增对应版本的小节，先写完整更新日志。
2. 把代码推到目标分支。
3. 在 GitHub Actions 里运行 `Android Release` 工作流，并输入版本号，例如 `2.0`。
4. 工作流会在云端完成：
   - 校验版本号格式
   - 校验 native 语音包发布资产
   - 运行单元测试
   - 读取 GitHub Secrets 中的正式签名材料
   - 构建并校验 `release-signed APK`
   - 打包 native 语音包 zip / manifest / checksum
   - 创建或更新 GitHub Release
   - 上传安装包和 native 语音包资产到 Release 下载页
5. 用户以后只需要去 Releases 页面看版本列表、更新日志和下载入口。

### 两种触发方式

- 推荐：GitHub 网页中手动运行 `Android Release`，输入版本号
- 兼容：推送 `v2.0`、`v2.1` 这样的 tag，也会自动创建对应 Release

## 版本与日志约束

- 版本号固定使用一位小数，例如 `1.0`、`1.1`、`2.0`
- 每次发版前必须先更新 `CHANGELOG.md`
- Release 页面正文直接从 `CHANGELOG.md` 对应版本段落生成
- 当前云端发布产物为 `release-signed APK` + native 语音包 Release 资产

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
