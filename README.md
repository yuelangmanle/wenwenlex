# 文文Lex

文文Lex 是一个原生 Android 背单词 App，使用 Kotlin + Jetpack Compose 构建，主打离线学习、AI 辅助分析、本地数据掌控和后续可持续迭代的云端发版流程。

## 当前状态

- 当前首发版本号：`1.0`
- 版本规则：每次迭代递增 `0.1`，按 `1.0 -> 1.1 -> ... -> 1.9 -> 2.0` 进位
- 正式发包界面：GitHub Releases
- 开发验证界面：GitHub Actions

## 协作文档入口

- [协作开发书](docs/collaboration-handbook.md)
- [开发进度书](docs/development-progress.md)
- [发版签名说明](docs/release-signing.md)
- [产品设计规格](docs/superpowers/specs/2026-03-18-vocabulary-android-design.md)
- [实施计划](docs/superpowers/plans/2026-03-18-vocabulary-android-app.md)

## 已实现能力

- 首页、学习、词书、我的 四大主导航
- 内置 CET4、CET6、考研词书，并支持 CSV / JSON 导入
- 学习卡片流、选择题复习、单词详情页
- AI 设置、AI 上下文整理、学习建议与复盘入口
- 每日提醒、备份恢复、我的页统一设置入口
- 本地优先的数据架构，API Key 不写入备份文件

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

1. 从 Releases 页面下载更高版本，例如 `1.1`、`1.2`。
2. 直接安装新 APK，Android 会覆盖旧版本，学习数据会保留。
3. 升级前仍建议先在 App 里做一次本地备份。

### 从旧调试包切换到正式包

- 如果你手机里装的是早期 `debug` 内测包，切到新的 `release-signed` 正式包时，Android 可能提示签名不一致。
- 遇到这种情况，需要先在 App 内完成备份，然后卸载旧包，再安装新的 Release 包，最后恢复备份。

### 发版流程

1. 在 [CHANGELOG.md](CHANGELOG.md) 里新增对应版本的小节，先写完整更新日志。
2. 把代码推到目标分支。
3. 在 GitHub Actions 里运行 `Android Release` 工作流，并输入版本号，例如 `1.1`。
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
- 兼容：推送 `v1.1` 这样的 tag，也会自动创建对应 Release

## 版本与日志约束

- 版本号固定使用一位小数，例如 `1.0`、`1.1`、`2.0`
- 每次发版前必须先更新 `CHANGELOG.md`
- Release 页面正文直接从 `CHANGELOG.md` 对应版本段落生成
- 当前云端发布产物为 `release-signed APK`，适合正式安装和后续升级

## 签名密钥与 GitHub Secrets

正式升级链路依赖同一套签名密钥。只要以后继续沿用同一把 key，用户就可以持续覆盖升级，不会丢数据。

- 详细说明见 [release-signing.md](docs/release-signing.md)
- GitHub 仓库需要这 4 个 secrets：
  - `ANDROID_RELEASE_KEYSTORE_BASE64`
  - `ANDROID_RELEASE_STORE_PASSWORD`
  - `ANDROID_RELEASE_KEY_ALIAS`
  - `ANDROID_RELEASE_KEY_PASSWORD`

## 本地兜底

虽然以后主流程走 GitHub 云端发包，但本地仍保留基础环境兜底：

- JDK 17
- Android SDK
- Gradle Wrapper

这能避免云端排队、网络波动或 GitHub 临时异常时完全失去打包能力。
