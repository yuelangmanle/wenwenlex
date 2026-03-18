# 文文Lex

文文Lex 是一个原生 Android 背单词 App，使用 Kotlin + Jetpack Compose 构建，主打离线学习、AI 辅助分析、本地数据掌控和后续可持续迭代的云端发版流程。

## 当前状态

- 当前首发版本号：`1.0`
- 版本规则：每次迭代递增 `0.1`，按 `1.0 -> 1.1 -> ... -> 1.9 -> 2.0` 进位
- 正式发包界面：GitHub Releases
- 开发验证界面：GitHub Actions

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

### 发版流程

1. 在 [CHANGELOG.md](CHANGELOG.md) 里新增对应版本的小节，先写完整更新日志。
2. 把代码推到目标分支。
3. 在 GitHub Actions 里运行 `Android Release` 工作流，并输入版本号，例如 `1.1`。
4. 工作流会在云端完成：
   - 校验版本号格式
   - 运行单元测试
   - 构建 APK
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
- 当前云端发布产物为 `debug` 签名 APK，适合体验、演示和内测

## 本地兜底

虽然以后主流程走 GitHub 云端发包，但本地仍保留基础环境兜底：

- JDK 17
- Android SDK
- Gradle Wrapper

这能避免云端排队、网络波动或 GitHub 临时异常时完全失去打包能力。
