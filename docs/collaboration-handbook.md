# 文文Lex 协作开发书

## 1. 文档定位

这份文档用于文文Lex 的持续协作、外部对接、后续 AI 接手开发和版本交接。

- 如果文档与代码不一致，以仓库代码和 GitHub Actions 工作流为准。
- 一旦发现不一致，必须在当前迭代内同步修正文档，不能把文档债务拖到下个版本。

## 2. 项目基线

- 项目名称：`文文Lex`
- 仓库地址：[yuelangmanle/wenwenlex](https://github.com/yuelangmanle/wenwenlex)
- 当前正式版本：`1.3`
- 当前正式发布：`2026-03-20` 已发布 `v1.3`
- 当前开发主线：`v1.4`，围绕真实模型资产替换、runtime bridge 完成和发音体验调优继续推进
- 平台：Android
- 开发语言：Kotlin
- UI 技术：Jetpack Compose
- 本地数据：Room + DataStore
- 后台任务：WorkManager
- AI 形态：用户自行配置第三方 AI API，不提供云端账号系统

## 3. 产品与技术边界

### 3.1 当前明确保留的方向

- 本地优先，离线学习是主链路
- 支持内置词书、导入词书、自定义扩展
- 支持学习记录、复习调度、统计、提醒、备份恢复
- 支持 AI 分析、AI 讲解、AI 音标补全、AI 计划建议，但 AI 不是主流程硬依赖
- 正式发版主链路走 GitHub 云端

### 3.2 当前明确不做的方向

- 不做云端账户体系
- 不做自建服务器
- 不做多端同步
- 不把用户 API Key 放进备份文件
- 不把正式签名材料提交到仓库

## 4. 对接入口

后续任何协作者接手时，优先看以下入口：

- 项目说明：[README.md](../README.md)
- 更新日志：[CHANGELOG.md](../CHANGELOG.md)
- 当前开发进度：[development-progress.md](./development-progress.md)
- 发版签名说明：[release-signing.md](./release-signing.md)
- `v1.1` 产品规格：[2026-03-19-v1.1-lexicon-import-ai-design.md](./superpowers/specs/2026-03-19-v1.1-lexicon-import-ai-design.md)
- `v1.1` 实施计划：[2026-03-19-v1.1-lexicon-import-ai.md](./superpowers/plans/2026-03-19-v1.1-lexicon-import-ai.md)
- `v1.2` 发音模块规格：[2026-03-19-v1.2-pronunciation-design.md](./superpowers/specs/2026-03-19-v1.2-pronunciation-design.md)
- `v1.2` 发音模块计划：[2026-03-19-v1.2-pronunciation.md](./superpowers/plans/2026-03-19-v1.2-pronunciation.md)
- `v1.3` 离线单词真实发音规格：[2026-03-19-v1.3-offline-word-pronunciation-design.md](./superpowers/specs/2026-03-19-v1.3-offline-word-pronunciation-design.md)
- `v1.3` 离线单词真实发音计划：[2026-03-19-v1.3-offline-word-pronunciation.md](./superpowers/plans/2026-03-19-v1.3-offline-word-pronunciation.md)
- 发布页面：[GitHub Releases](https://github.com/yuelangmanle/wenwenlex/releases)
- 构建页面：[GitHub Actions](https://github.com/yuelangmanle/wenwenlex/actions)

## 5. 仓库结构速览

```text
app/                    Android 应用主体
docs/                   对接文档、规格和计划
.github/workflows/      云端 CI 与 Release 工作流
scripts/                版本号、更新日志和词库生成脚本
distribution/           native 语音包发布源目录
CHANGELOG.md            发版更新日志来源
README.md               项目入口说明
```

核心代码聚焦在 `app/src/main/java/com/yueliangmanle/danci/`，按 `feature`、`core`、`app` 三层拆分。

## 6. 协作规则

### 6.1 分支规则

- 开发分支统一建议使用 `codex/<topic>` 命名
- `main` 作为长期稳定入口
- `codex/**` 分支会触发 `Android CI`

### 6.2 提交规则

- 提交前缀统一使用：`feat`、`fix`、`docs`、`chore`、`test`、`refactor`
- 一次提交只解决一个主问题，避免“混合提交”
- 影响行为的代码改动，必须附带对应测试或说明为什么暂时无法补测试

### 6.3 合并前最低要求

- 本地或云端至少有一套验证通过
- 影响正式版本的变更，必须更新相关文档
- 不允许绕过签名与 Release 规则直接把 debug 包当正式包分发

## 7. 迭代规则

### 7.1 版本规则

- 首发版本固定为 `1.0`
- 后续每次正式迭代固定 `+0.1`
- 进位规则固定为：`1.9 -> 2.0`
- 当前不使用 `1.0.1` 这类三段式版本号
- 紧急修复也占用下一次正式迭代号，例如：`1.0 -> 1.1`

### 7.2 迭代范围规则

每次迭代必须至少明确以下内容：

- 本轮目标
- 本轮范围
- 验收标准
- 风险点
- 是否触发发版

建议每轮只保留一个主目标，其他内容作为辅助项，避免范围失控。

### 7.3 迭代完成规则

一个迭代被视为完成，至少要满足：

- 代码已合入目标分支
- 对应验证已完成
- `docs/development-progress.md` 已更新
- 如果涉及正式发版，`CHANGELOG.md` 已更新
- 如果涉及流程变化，`docs/collaboration-handbook.md` 或 `docs/release-signing.md` 已更新

## 8. 打包与发版规则

### 8.1 主链路定义

- 开发验证链路：`Android CI`
- 正式发版链路：`Android Release`
- 分发界面：GitHub Releases

### 8.2 开发验证规则

`Android CI` 负责：

- 校验 native 语音包发布资产
- 运行单元测试
- 构建 debug APK
- 运行连接设备/模拟器测试
- 上传测试报告与构建产物

适用场景：

- 日常开发验证
- 分支提交验证
- Pull Request 验证

### 8.3 正式发版规则

`Android Release` 负责：

- 接收版本号输入，或通过 `v*` tag 触发
- 校验 native 语音包发布资产
- 运行单元测试
- 校验 Release 签名 secrets
- 构建 `release-signed APK`
- 校验 APK 签名
- 打包 native 语音包 zip / manifest / checksum
- 从 `CHANGELOG.md` 提取对应版本日志
- 创建或更新 GitHub Release
- 上传正式安装包与 native 语音包资产

### 8.4 正式发版前置检查

发版前必须完成：

1. 确认版本号符合 `major.minor`
2. 先更新 [CHANGELOG.md](../CHANGELOG.md)
3. 确认目标分支代码已推送
4. 确认 GitHub Secrets 完整
5. 确认正式包要使用同一把签名 key

### 8.5 正式产物规则

- 正式分发产物必须是 `release-signed APK`
- 当前资产命名规则为：`wenwenlex-v<version>-release.apk`
- native 语音包资产命名规则为：`wenwenlex-voice-pack-<packId>.zip`
- native 语音包 manifest 命名规则为：`wenwenlex-voice-pack-<packId>-manifest.json`
- native 语音包 checksum 命名规则为：`wenwenlex-voice-pack-checksums.txt`
- Release 页面正文来源于 `CHANGELOG.md`
- 禁止把 debug 包上传到正式 Release 页面替代正式包

## 9. 词库、备份与 AI 规则

### 9.1 词库规则

- 内置词库资产统一位于 `app/src/main/assets/books/`
- 来源底座为 ECDICT 标签词库，许可证为 MIT
- 词库再生成统一使用 `scripts/generate_lexicons.py`
- 改动内置词库后，必须同步检查 `manifest.json`、词书页展示和同步逻辑

### 9.2 备份规则

- 当前备份版本为 `v3`
- 备份必须覆盖：词书、单词、学习数据、多 API 路由、导入批次、音标补全任务
- API Key 不进入备份文件
- 新版本仍必须兼容导入旧 `v1` / `v2` 备份

### 9.3 AI 路由规则

- 至少维护一条“默认 API”档案
- 功能级覆盖当前包含：
  - 词条讲解
  - 计划调整
  - 音标补全
- 新增 AI 功能时，必须明确它走默认 API 还是新增覆盖位

## 10. 文档实时更新规则

### 10.1 必更新文件映射

- 改了产品能力或页面流程：更新 [development-progress.md](./development-progress.md)
- 改了正式版本内容：更新 [CHANGELOG.md](../CHANGELOG.md)
- 改了仓库入口或使用方式：更新 [README.md](../README.md)
- 改了打包、签名、工作流：更新本文件和 [release-signing.md](./release-signing.md)
- 改了整体方案或大模块边界：更新 `docs/superpowers/specs/` 下对应规格

### 10.2 更新时点规则

- 最晚要在同一轮开发合并前更新文档
- 如果是正式发版相关内容，必须在发版动作前更新完成
- 不允许“代码先发，文档以后补”

### 10.3 进度书维护规则

`docs/development-progress.md` 必须持续维护以下字段：

- 当前正式版本
- 最近一次更新时间
- 已完成模块
- 当前阻塞项
- 下阶段候选项
- 最近迭代记录

## 11. 新协作者接手清单

新成员第一次接手时，按这个顺序进入：

1. 看 [README.md](../README.md)
2. 看 [development-progress.md](./development-progress.md)
3. 看 [CHANGELOG.md](../CHANGELOG.md)
4. 看 [.github/workflows/android-ci.yml](../.github/workflows/android-ci.yml)
5. 看 [.github/workflows/android-release.yml](../.github/workflows/android-release.yml)
6. 如涉及词库、导入或 AI 路由，再看 `v1.1` 规格；如涉及发音与朗读，先看 `v1.2` 规格，再看 `v1.3` 离线单词真实发音规格与实施计划

## 12. 每轮迭代收尾清单

- 代码完成并自检
- 验证命令或云端工作流通过
- 进度书已更新
- 需要发版时，更新日志已补齐
- README 入口没有失真
- Release、CI、词库、备份规则没有被文档遗漏
