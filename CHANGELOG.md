# Changelog

本项目使用人工维护的更新日志。每次发版前，必须先补齐对应版本记录，再运行云端 Release 工作流。

## [Unreleased]

## [1.6] - 2026-03-22

### Added

- 新增学习统计页，支持从首页和“我的”页进入，并通过内置 HTML 看板集中展示趋势、反馈分布、计划效果和发音使用
- 新增学习统计概览卡片与长期摘要展示，把最近正确率、学习天数、掌握量和发音使用聚合到统一入口
- 新增 AI 计划中心“最近调整效果”卡片，仅在存在真实 `APPLIED` 计划效果样本时展示

### Changed

- `AiMemorySummary`、Room 持久化和 AI 上下文已正式接入 `analyticsSnapshot` 与 `longTermInsights`，让计划调整不再只依赖最近会话
- 本地备份已升级到 `v5`，纳入学习统计快照和长期摘要，并保持对 `v1 / v2 / v3 / v4` 旧备份的兼容恢复
- 首页 / “我的”页学习统计入口与 AI 计划中心效果回看测试已补齐点击行为覆盖，避免只测文案不测跳转

### Notes

- 云端 `Android CI #23384177140` 已通过，`Build Debug APK` 与 `Connected Debug Android Test` 均为绿色
- 由于当前本地机器仍缺 Java runtime，`1.6` 的最终验证与正式发版仍以 GitHub Actions `Android CI` / `Android Release` 为准

## [1.5] - 2026-03-21

### Added

- 新增 AI 计划中心，支持从首页、学习页和“我的”页直接进入，集中查看当前生效计划、待确认调整和计划时间轴
- 新增 AI 计划对比页，固定以当前 `APPLIED` 计划为基线，对照历史版本的重点、节奏、模式与预期影响
- 新增 AI 计划解释页，集中展示“为什么改 / 改了什么 / 预期影响 / 触发信号”
- 新增 checkpoint summary 持久化、计划历史版本状态流转和长期 AI 记忆压缩链路

### Changed

- AI 计划建议升级为版本化持久化结构，支持小调整自动生效、大调整待确认
- 首页和学习页现在会显示 AI 计划中心入口、待确认数量和 checkpoint 决策状态
- 本地备份升级到 `v4`，纳入扩展计划历史字段与 checkpoint 摘要，并保持对 `v1 / v2 / v3` 旧备份的兼容恢复

### Notes

- 云端 `Android CI #23377258763` 已通过，`Build Debug APK` 与 `Connected Debug Android Test` 均为绿色
- 云端 `Android Release #23377422188` 已通过，`v1.5` GitHub Release 已正式发布
- `v1.5` Release 资产已包含：`wenwenlex-v1.5-release.apk`、UK / US 两个 voice pack zip、两个 manifest 与 `wenwenlex-voice-pack-checksums.txt`

## [1.4] - 2026-03-21

### Added

- 新增 `scripts/prepare_voice_pack_sources.sh`，支持在 GitHub Actions 云端按固定上游 URL + `sha256` 拉取真实 Kokoro 模型资产并生成临时语音包源目录
- 新增英式 / 美式两套真实原生离线单词发音包模板，正式切换到 `kokoro-en-v0_19` 上游模型，不再依赖仓库内 placeholder 模型文件
- 新增 native pack `speakerId` 元数据，并把 UK / US pack 分别固定到 `bf_isabella (sid 8)` 与 `af_nicole (sid 2)`，确保双口音不是同一 speaker
- 新增云端可复现的 pack 模板体系：`manifest.template.json + source.json + 本地分发说明`
- 新增 `docs/release-v1.4-pronunciation-smoke.md`，用于记录 `1.4` 云端验证、Release 资产核对和后续真机补充验收

### Changed

- native 语音包正式从 `sherpa_onnx_scaffold` 切换到真实 `kokoro` 模型 metadata，catalog / 包内 manifest / Release checksum 三者保持一致
- `scripts/package_voice_packs.sh`、`scripts/package_native_voice_pack.sh` 和 `scripts/test_voice_pack_release_assets.sh` 现在消费“云端准备好的临时源目录”，不再直接读取仓库中的 pack 二进制
- `Android CI` / `Android Release` 工作流现在会先准备真实语音包源目录，再做 release asset 校验、单测、APK 构建与 Release 上传
- `SherpaOnnxRuntime` 现在会读取 pack 内 `speakerId` 并传给 native runtime，保证英式 / 美式使用不同 speaker
- 默认构建版本和 `Android Release` 工作流默认输入版本同步更新为 `1.4`

### Fixed

- 修复 Kotlin 2.0 下 `SherpaOnnxRuntime` 和 `VoicePackDownloadWorker` 的兼容性写法，恢复云端 Kotlin 编译
- 修复 `VoicePackInstallerTest` 对 JDK `HttpServer` 和 `assertFailsWith` 的依赖问题，改为仓库内可控的最小 HTTP 测试桩
- 修复 `PronunciationSettingsViewModelTest` 的本地 `suspend` 调用和 `FakeSherpaOnnxRuntime` 可见性问题，避免单测编译被测试代码本身拦住

### Notes

- `1.4` 的正式语音包仍然通过 GitHub Release 分发，但真实模型 payload 只在云端临时拉取和组装，不进入 git 仓库
- 这次 release 的正式资产应包含：`release-signed APK`、UK / US 两个 voice pack zip、两个 manifest 资产，以及 `wenwenlex-voice-pack-checksums.txt`
- 由于当前本地机器仍缺 Java runtime，`1.4` 的最终构建与发版验证以 GitHub Actions 为准

## [1.3] - 2026-03-20

### Added

- 新增原生离线语音包运行时 metadata 合并能力，可从已安装包 `manifest.json` 读取 `engineFamily`、`modelFamily`、资源占用和许可证信息
- 新增 Sherpa ONNX Android runtime 脚手架、运行时抽象与拉取脚本，为 `v1.3` 真离线单词发音做准备
- 新增单词发音归一化与本地生成音频缓存路径，支持把 native 合成结果持久化到 `audio-cache/generated/`
- 新增 native 离线单词发音引擎与本地文件播放能力，支持英式 / 美式原生包实时合成
- 新增原生语音包安装校验测试和设置页坏包错误原因展示逻辑
- 新增在线词典音频多来源候选排序与回退机制，当前默认接入 `Dictionary API` + `有道词典`
- 新增 native 语音包发布源目录与打包脚本，支持从 `distribution/voice-packs/` 直接生成正式 Release 资产
- 新增语音包发布资产自检脚本，当前会校验 catalog、zip 结构、`entryFiles` 和 Release 资产命名约定

### Changed

- 发音调度器升级为优先命中“本地词典缓存 -> 本地 native 生成缓存 -> native 离线合成 -> 在线词典音频 -> 系统 TTS”链路
- 发音设置页现在会展示 native 语音包能力摘要、资源占用提示，以及安装失败的具体原因
- 离线语音引擎从仅支持 `system_tts_bridge` 升级为同时支持 `sherpa_onnx` 原生离线路径
- 在线词典音频会按口音优先级和 provider 顺序逐个尝试，并把实际命中的来源写进详情页 / 学习页状态提示
- 内置语音包 catalog 已补入英式 / 美式 native Release 条目，并固定指向 GitHub Releases `latest/download`
- GitHub Actions 的 `Android CI` / `Android Release` 现在会先校验 native 语音包发布资产；正式 Release 会额外上传语音包 zip、manifest 与 checksum

### Fixed

- 修复 `PronunciationOrchestratorTest` 因缺少 Robolectric runner 导致的云端单测失败
- 修复 native 语音包安装阶段未校验 `licenses`、`entryFiles` 和 native metadata 的问题
- 修复语音包安装异常时设置页只能看到“安装异常”而看不到具体失败原因的问题
- 修复 native 语音包安装时没有校验 `licenses.file` 引用文件是否真实存在的问题

### Notes

- `1.3` 对应离线单词真实发音与 native 语音包正式分发链路的首个正式版本
- 当前 GitHub Release 会同时分发 `release-signed APK`、native 语音包 zip、manifest 和 checksum
- 当前 native 语音包仍先以内置 scaffold 资产打通分发链路；真实模型资产与最终 runtime bridge 属于后续迭代

## [1.2] - 2026-03-19

### Added

- 新增发音模块设计文档与实施计划，明确采用“词典音频优先 + 本地缓存 + 可下载离线语音包 + 系统 TTS 兜底”的路线
- 新增 `word_audio_assets` 与 `voice_packs` 的 Room v3 数据模型、DAO 和备份结构
- 新增发音设置页入口，可管理默认口音、发音来源策略、自动缓存、系统朗读兜底和语音包状态位
- 单词详情页新增英式 / 美式发音按钮
- 学习页新增单词发音播放入口
- 新增词典音频抓取、缓存仓库、播放调度器和播放事件埋点骨架
- 新增内置语音包清单资产、语音包下载 worker、安装目录挂载与删除流程
- 新增两套桥接语音包样例清单，支持英式 / 美式一键安装并接入发音设置页

### Changed

- 设置项扩展为支持默认口音、音频缓存上限、语音包激活和长文本离线优先偏好
- 备份版本正式升级到 `v3`，为音频缓存元数据和语音包状态提供兼容导入能力
- “我的”页新增发音与朗读设置入口
- 发音设置页升级为可刷新语音包清单、下载 / 安装 / 删除语音包，并轮询展示安装状态
- 发音调度器开始支持“词典优先 / 离线优先”两种顺序，并可在已安装桥接包下优先调用本地语音路径
- 首页和词书页加载逻辑改为先同步真实内置目录再读取数据库，避免一直停留在加载态

### Fixed

- 修复首页在真实词库接入后长时间转圈的问题
- 修复词书页在同步真实目录前无法稳定展示内置词书的问题

### Notes

- `1.2` 对应首个发音与朗读正式版本，已通过 GitHub Actions 云端 Release 发包
- 当前“离线语音包”第一版为 `system_tts_bridge` 桥接实现，真正的本地模型推理包仍是下一步增强项

## [1.1] - 2026-03-19

### Added

- 新增多 API 档案管理，支持本地加密保存多条 API Key，并提供全局默认与功能级覆盖切换
- 新增 Excel 词书导入页，支持 `.xlsx` 文件、A 列单词 / B 列中文义、中英文逗号拆分，以及 AI 适配不规整表格
- 新增词书详情页，支持把词书设为当前词书，并在词书范围内批量补全或覆盖双音标
- 单词详情页新增英式 / 美式双音标展示、单词级 AI 音标补全与覆盖重拉
- 内置词书升级为真实词库，新增高中与雅思基础词库，并把所有词书来源、覆盖范围和许可证写入 manifest
- 新增 `scripts/generate_lexicons.py`，可用 ECDICT 标签词库重新生成内置词书资产
- 新增备份 v2，覆盖多 API 路由、音标字段、导入批次与音标补全任务，同时保持对 v1 备份的兼容导入

### Changed

- Room 数据库升级到 `v2` 后，正式承载双音标、多 API 档案、导入批次和音标补全任务
- 内置词书同步逻辑改为按数量快速校验，避免真实大词库下每次进入页面都重复全量重灌
- 首页、学习流和单词详情使用能力级 AI profile 解析，计划调整不再强制走旧单一模型配置
- “我的”页中的 AI 摘要改为显示当前默认档案，而不是旧的单一 model 文本
- 默认构建版本切换到 `1.1`，云端 Release 工作流默认输入版本同步更新为 `1.1`

### Fixed

- 修复旧版备份恢复后丢失新音标字段和多 API 路由配置的问题
- 修复词书列表仍停留在 demo 词书清单、没有真实数据库状态的问题
- 修复导入 / 词书 / 单词详情新增路由后底部导航选中状态不连续的问题

## [1.0] - 2026-03-18

### Added

- 首个可用版本的 Android 原生应用骨架，包含首页、学习、词书、我的 四个主入口
- 内置 CET4、CET6、考研词书，并支持 CSV / JSON 词书导入
- 学习卡片流、测验复习流、单词详情页与结构化学习记录
- AI 设置页、本地 AI 上下文整理、学习建议与计划调整入口
- 我的页统一设置中心，接入每日目标、提醒、备份恢复和 AI 设置入口
- 本地 ZIP 备份与恢复能力，备份中包含 AI memory summary，但不包含 API Key
- WorkManager 驱动的每日提醒和 AI 摘要刷新任务

### Changed

- 将 Room 升级到 `2.7.0`，对齐 WorkManager 依赖链，修复云端构建兼容问题
- 调整二级页面下的底部导航选中逻辑，让“我的”与 AI 设置等页面切换更稳定
- 完善 GitHub Actions 构建链路，接入正式 Release 页面与可持续升级的签名发包流程

### Notes

- `1.0` 对应首个正式版本号
- 当前 Release 提供的是 `release-signed APK`，适合正式安装与后续覆盖升级
