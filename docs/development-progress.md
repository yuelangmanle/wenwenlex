# 文文Lex 开发进度书

## 1. 文档用途

这份文档用于记录文文Lex 的当前真实开发状态，方便后续协作者、外部支持者和未来迭代直接接手。

- 最后更新日期：`2026-03-22`
- 当前正式版本：`1.5`
- 当前总体状态：`v1.5 已发布并完成云端 CI / Release 归档；v1.6 的学习统计看板、长期摘要投喂、计划效果回看和备份 v5 已落仓，并已通过 Android CI，当前进入 Android Release 前收口阶段`

## 2. 当前版本快照

- 项目名称：`文文Lex`
- 发布页面：[GitHub Releases](https://github.com/yuelangmanle/wenwenlex/releases)
- 当前正式版本页面：[v1.5](https://github.com/yuelangmanle/wenwenlex/releases/tag/v1.5)
- 当前 `1.5` 发版记录：[release-v1.5-ai-plan-smoke.md](./release-v1.5-ai-plan-smoke.md)
- 当前 `1.6` 实施计划：[2026-03-21-v1.6-learning-analytics-ai-loop.md](./superpowers/plans/2026-03-21-v1.6-learning-analytics-ai-loop.md)
- 当前 `1.6` 发版基线：[release-v1.6-analytics-smoke.md](./release-v1.6-analytics-smoke.md)
- 当前开发验证页面：[GitHub Actions](https://github.com/yuelangmanle/wenwenlex/actions)
- 当前正式发包方式：GitHub 云端 `Android Release`
- 当前开发验证方式：GitHub 云端 `Android CI`
- 当前开发主线：`v1.6` 已完成主要代码落仓，聚焦学习统计可视化、长期摘要投喂和 AI 计划效果回看；当前已通过 `Android CI #23384177140`，待 `Android Release`

## 3. 里程碑状态

| 里程碑 | 状态 | 说明 |
| --- | --- | --- |
| 产品定义与技术基线 | 已完成 | Android 原生、本地优先、无账号、AI API 可选增强的路线已固定 |
| MVP 页面骨架 | 已完成 | 首页、学习、词书、我的 四大主导航已落地 |
| 学习闭环 MVP | 已完成 | 卡片学习、选择题复习、记录反馈、今日任务链路已打通 |
| 单词详情增强 | 已完成 | 已支持近反义词、拼写相近词、易混词、变形、词根词缀和双音标补全 |
| 词书与导入能力 | 已完成 | 真实内置词库、Excel 导入、AI 表格适配、词书详情与批量音标补全已落地 |
| 本地数据与备份 | 已完成 | Room v2、DataStore、多 API 档案、备份 v2 与旧备份兼容导入已落地 |
| AI 基础能力 | 已完成 | 多 API 管理、词条讲解、计划调整、音标补全、本地 fallback 已接入 |
| 提醒与后台任务 | 已完成 | 每日提醒与 AI 摘要刷新任务已接入 |
| 云端 CI 与正式发版 | 已完成 | GitHub Actions 与 GitHub Releases 已可用 |
| 发音与朗读 v1.2 | 已完成（第一版） | 已完成数据库 v3、发音设置页、词典音频缓存、语音包下载安装、系统 TTS / 桥接包兜底，并正式发版 |
| 发音与朗读 v1.3 | 已完成并发版 | 已完成 native metadata 合并、Sherpa ONNX runtime 脚手架、本地生成缓存、原生离线单词合成、安装校验、Release 资产打包与设置页错误可见化，并正式发版 |
| 发音与朗读 v1.4 | 已完成并发版 | 已切到真实 `kokoro-en-v0_19` 模型资产、官方 Sherpa Android JNI runtime、UK/US 双 speaker、云端组装语音包与 Release checksum 对齐，并正式发版 |
| AI 计划历史回溯 v1.5 | 已完成并发版 | 已完成计划版本持久化、AI 计划中心、计划对比 / 解释页、checkpoint 压缩记忆和备份 `v4`，并完成正式发版 |
| 学习统计与 AI 闭环 v1.6 | 开发中（已落仓，待云端验证） | 已完成统计快照持久化、长期摘要投喂、学习统计页 / HTML 看板、首页 / 我的页入口、AI 计划中心效果回看与备份 `v5` |

## 4. 已完成模块清单

### 4.1 产品能力

- 首页今日任务概览
- 学习卡片流
- 选择题复习流
- 单词详情页
- 词书管理页
- 词书详情页
- Excel 导入页
- 学习统计页 / HTML 看板
- 我的页设置中心
- 多 API 设置页
- 本地备份与恢复
- 每日提醒

### 4.2 单词信息能力

- 英式 / 美式双音标字段
- 释义
- 近义词
- 反义词
- 拼写相近词
- 易混词
- 单词变形
- 词根词缀
- 词频与标签字段建模

### 4.3 AI 能力

- 多 API 档案管理
- 全局默认 API 与功能级覆盖路由
- AI 计划建议上下文构建
- AI 计划版本历史、待确认状态与 checkpoint 压缩记忆
- AI 计划中心、时间轴回溯、计划对比与解释页
- 学习统计长期摘要与结构化统计快照
- AI 计划效果回看与长期摘要投喂
- AI 学习建议解析
- 单词助记、关系辨析、词形讲解、例句扩展
- Excel 表格 AI 适配
- 单词级和词书级音标补全
- 无可用 AI 配置时的本地 fallback 兜底
- 长期数据摘要结构，支持后续持续投喂 AI

### 4.4 词库能力

- 内置四级词库 `3844`
- 内置六级词库 `5401`
- 内置考研词库 `4796`
- 内置高中词库 `3666`
- 内置雅思基础词库 `5026`
- 词库来源、许可证和覆盖范围 manifest 化
- 一键再生成脚本 `scripts/generate_lexicons.py`

### 4.5 工程与交付能力

- Room `1 -> 2` 数据迁移
- Room `2 -> 3` 发音模块迁移
- Room `4 -> 5` AI 计划历史与 checkpoint 摘要迁移
- Room `5 -> 6` 学习统计快照与长期摘要迁移
- 备份 `v1 -> v2` 兼容导入
- 备份 `v3` 兼容导入
- 备份 `v4` 扩展计划历史与 checkpoint 摘要兼容导入
- 备份 `v5` 学习统计快照与长期摘要兼容导入
- 语音包清单同步、下载 worker 与桥接包安装路径
- 原生离线单词发音引擎骨架与本地生成缓存
- 官方 Sherpa ONNX Android JNI runtime 接入与真实 Kokoro runtime bridge
- native 语音包 manifest / license / entryFiles 安装校验
- native 语音包发布源目录、打包脚本与 Release asset checksum
- native 语音包云端上游拉取、模板组装与固定 `sha256` 校验
- UK / US 双口音 speakerId 分离与 `accent / modelFamily / version` 缓存命名空间
- 发音设置页 native 语音包能力提示与安装失败原因展示
- 单元测试
- Compose UI 测试骨架
- GitHub Actions 云端 CI
- GitHub Actions 云端 Release
- GitHub Actions native 语音包发布资产自检
- 正式签名 APK 发版
- GitHub Release 页面分发
- `CHANGELOG.md` 驱动的 Release Notes

## 5. 当前对接事实

以下事实可默认作为后续协作基线：

- 正式版本 `1.5` 已于 `2026-03-21` 完成云端正式发版
- `Android CI #23377258763` 已通过，覆盖 `Build Debug APK` 与 `Connected Debug Android Test`
- `Android Release #23377422188` 已通过，并已发布 [v1.5](https://github.com/yuelangmanle/wenwenlex/releases/tag/v1.5)
- 正式安装包类型为 `release-signed APK`
- 正式版本更新日志由 [CHANGELOG.md](../CHANGELOG.md) 提供
- 版本号规则固定为 `1.0 -> 1.1 -> ... -> 1.9 -> 2.0`
- 当前 `1.5` 在 `1.4` 发音闭环基础上，已把 AI 计划历史、计划对比解释和备份 `v4` 正式接入
- 当前 `1.5` Release 资产已确认包含：signed APK、UK / US voice pack zip、两个 manifest 与 checksum
- 当前 `1.5` 发版归档记录见 [release-v1.5-ai-plan-smoke.md](./release-v1.5-ai-plan-smoke.md)
- 当前开发主线 `1.6` 已把学习统计快照、长期摘要和计划效果回看正式接入仓库代码
- 当前开发主线 `1.6` 已升级到 Room `v6` 与备份 `v5`
- 当前开发主线 `1.6` 已完成云端 `Android CI #23384177140`，`Build Debug APK` 与 `Connected Debug Android Test` 全绿
- 当前本机仍缺 Java runtime，因此 `1.6` 无法在本机直接跑 Gradle；最终验证继续以 GitHub Actions 为准
- native 语音包发布源目录当前位于 `distribution/voice-packs/`
- 正式 Release 已具备同时上传 APK、native 语音包 zip、manifest 和 checksum 的能力
- 正式 Release 的 native payload 由 GitHub Actions 云端临时拉取与打包，不进入 git 仓库
- 当前不依赖自建服务器，不包含云端账号系统
- AI 能力通过用户自行配置的第三方 API 扩展
- API Key 不进入备份文件，只保留档案元信息与路由设置

## 6. 当前仍需持续增强的方向

以下项目不是“未做”，而是“第一版已落基础、后续继续增强”的方向：

- 内置词库中的近反义词、易混词和例句质量继续补强
- AI 计划调整结果可视化和可解释性继续增强
- 学习统计面板继续细化
- 导入表格的异常诊断与批量补全过程优化
- 更长周期的 AI 记忆摘要和计划连续性继续加强
- native 离线语音包真机听感验收、缓存命中时延与失败回退体验继续调优
- 更丰富的在线词典音频源与缓存命中策略

## 7. 已锁定的 `1.6` 范围

### 7.1 主目标

- 把“学习数据沉淀 -> 可视化回看 -> AI 计划调整 -> 调整效果追踪”做成第一版正式闭环

### 7.2 本轮已落仓项

- 已新增学习统计页与统计看板，第一版采用 App 内 HTML 可视化，覆盖趋势、反馈分布、计划效果和发音使用
- 已扩展学习事件聚合与长期摘要，补入 checkpoint 结果、计划执行偏差和发音使用等结构化统计
- 已在 AI 计划中心补入“最近调整效果”视角，并只对真实 `APPLIED` 效果样本开放展示
- 已把长期摘要正式接入 AI 计划生成输入，形成稳定的连续性上下文，而不是只看最近一次会话
- 已完成配套的数据迁移、备份 `v5` 字段扩展和主要页面 / ViewModel 测试补强

### 7.3 本轮辅助项

- 统计图表第一版优先使用 HTML 容器承载，避免一开始就引入过重的原生图表复杂度
- 发音模块本轮只补统计采集和展示，不重开新的离线 TTS 引擎路线
- 导入链路本轮只补与统计 / AI 摘要相关的数据衔接，不重开大规模表格能力重写

### 7.4 明确不纳入

- 云端账户体系、自建服务器、多端同步
- 新一轮 native 发音引擎替换或大规模语音包体系改版
- 大规模词库内容清洗工具链重构
- 新的三段式版本号或临时旁支版本策略

### 7.5 `1.6` 发版门槛

- 用户可在 App 内直接看到统计面板与核心趋势图
- AI 计划输入已接入稳定的长期统计摘要
- 至少一条“调整前后效果”回看链路可见
- `Android CI` 与 `Android Release` 全绿
- `CHANGELOG.md`、`README.md`、`development-progress.md`、`collaboration-handbook.md` 与发版记录同步更新

## 8. 实时更新规则

从现在开始，这份进度书按以下规则维护：

- 只记录“已经落到仓库里的事实”，不写空计划
- 每次完成一个对用户可感知的模块或一轮发版，都要更新本文件
- 每次正式发版前，必须核对本文件、[CHANGELOG.md](../CHANGELOG.md) 和 [README.md](../README.md) 是否一致
- 如果工作流、签名或发版链路变更，必须同步核对 [collaboration-handbook.md](./collaboration-handbook.md)

## 9. 最近迭代记录

### 2026-03-18 / v1.0 / 已发布

- 完成 Android 原生 MVP 骨架与四大导航
- 完成学习、复习、词书、我的 四条主线页面
- 完成词书导入、本地备份恢复、提醒和 AI 设置
- 完成 AI 上下文整理、词条讲解与计划建议基础链路
- 完成 GitHub 云端 CI、正式签名 Release、GitHub Releases 分发

### 2026-03-19 / v1.1 / 已完成并发版

- 把 Room 升级到 `v2`，接入多 API 档案、导入批次与音标补全任务
- 把 Excel 导入升级为 `.xlsx + AI 适配`，新增词书详情和批量音标补全
- 把单词详情升级为英式 / 美式双音标显示与单词级 AI 音标补全
- 用 ECDICT 标签词库替换 demo 词书，扩展到 5 套真实内置词库
- 升级本地备份到 `v2`，并保持对 `v1` 备份的兼容恢复
- 同步更新版本号、更新日志、README、协作开发书和云端发版默认版本

### 2026-03-19 / v1.2 / 已完成并发版

- 发音模块正式采用“词典音频优先 + 本地缓存 + 离线语音包 + 系统 TTS 兜底”的路线并落地第一版
- 完成数据库 `v3`、备份 `v3`、发音设置页、单词详情 / 学习页播放入口、词典音频缓存与调度器
- 完成内置语音包清单、下载 worker、安装目录挂载、删除、激活与设置页状态刷新
- 完成 GitHub 云端 CI 与正式 Release 验证，正式版本切换到 `1.2`
- 当前后续增强重点：真正的离线语音包本地推理引擎接入

### 2026-03-20 / v1.3 / 已完成并发版

- 已完成 native 语音包 metadata 合并、license 校验与 runtime scaffolding，保持 Room `v3` 主版本稳定不变
- 已完成单词发音归一化、本地生成缓存、native 离线单词合成与播放优先级切换
- 已完成 native 语音包安装阶段的 `manifest / licenses / entryFiles` 校验
- 已完成发音设置页的 native 能力提示与安装失败原因透出
- 已完成在线词典音频多来源候选回退，并把实际命中的来源写入详情页 / 学习页状态提示
- 已完成 native 语音包发布源目录、zip 打包脚本、Release asset checksum 与 GitHub Actions 资产校验
- 已完成 GitHub Actions 云端正式 Release，并发布 `v1.3`

### 2026-03-21 / v1.4 / 已完成并发版

- 已完成官方 Sherpa ONNX Android JNI runtime 接入，真实 native 离线单词发音不再依赖 scaffold 占位
- 已完成 UK / US 两套真实 `kokoro-en-v0_19` 语音包模板、双 speakerId 和运行时 metadata 读取
- 已完成 GitHub Actions 云端按固定上游 URL + `sha256` 拉取模型并现场打包 Release voice pack 资产
- 已完成 native 缓存命名空间版本化，以及导入词书单词与内置词书统一走同一条 native 生成缓存链路
- 已完成 `VoicePackInstaller` 单测去 socket 化，恢复云端单测阶段的稳定性
- 已完成 GitHub Actions 云端正式 Release，并发布 `v1.4`

### 2026-03-21 / v1.5 / 已完成并发版

- 已完成 AI 计划版本持久化、计划严重度判断、小调整自动生效和大调整待确认状态流转
- 已完成首页、学习页和“我的”页的 AI 计划中心入口，以及学习页 checkpoint 决策状态展示
- 已完成 AI 计划中心、计划对比页、解释页、时间轴回溯与待确认操作
- 已完成 checkpoint summary 压缩记忆、长期计划上下文窗口和备份 `v4` 兼容恢复
- 已完成本地关键验证：首页待确认提示、学习页 checkpoint 自动 / 待确认分流、AI 计划中心空状态 / 历史状态 / 待确认状态、对比页固定使用当前 `APPLIED` 版本、旧备份恢复后 AI 计划中心可正常显示
- 已完成云端 `Android CI #23377258763`，`Build Debug APK` 与 `Connected Debug Android Test` 全绿
- 已完成云端 `Android Release #23377422188`，发布 [v1.5](https://github.com/yuelangmanle/wenwenlex/releases/tag/v1.5)
- 已确认 Release 资产包含 `wenwenlex-v1.5-release.apk`、两套 voice pack zip、两个 manifest 和 `wenwenlex-voice-pack-checksums.txt`
- 已补齐 [release-v1.5-ai-plan-smoke.md](./release-v1.5-ai-plan-smoke.md) 作为后续对接与回归基线

### 2026-03-22 / v1.6 / 开发中（已过 Android CI，待 Release）

- 已完成学习统计快照持久化、长期摘要字段落库，以及 Room `5 -> 6` 迁移
- 已完成本地备份 `v5`，覆盖学习统计快照和长期摘要，并保持对 `v1` 到 `v4` 旧备份的兼容导入
- 已完成学习统计页 / HTML 看板、首页 / “我的”页入口，以及 AI 计划中心“最近调整效果”卡片
- 已完成长期学习摘要与计划效果数据接入 AI 计划输入，形成更稳定的连续性上下文
- 已补齐统计入口点击测试与 AI 计划中心效果展示收紧测试
- 已完成云端 `Android CI #23384177140`，`Build Debug APK` 与 `Connected Debug Android Test` 全绿
- 尚未完成 `Android Release` 的最终云端回填；当前发版基线记录见 [release-v1.6-analytics-smoke.md](./release-v1.6-analytics-smoke.md)

## 10. 后续更新模板

后续每次迭代，在本文件底部继续追加一段：

```md
### YYYY-MM-DD / vX.Y / 状态

- 本轮目标：
- 已完成：
- 当前风险或阻塞：
- 是否发版：
- 关联提交 / PR / Release：
```
