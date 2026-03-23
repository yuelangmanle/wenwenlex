# 文文Lex v1.8 导入闭环与统一发音源中心发版 Smoke 记录

- 当前状态：待触发云端 `Android CI` / `Android Release`
- 开发分支：`codex/v1-4-real-offline-pronunciation`
- Release 页面：[v1.8](https://github.com/yuelangmanle/wenwenlex/releases/tag/v1.8)

## Android CI

- 待回填：
  - run id
  - 结论
  - `Build Debug APK` / `Connected Debug Android Test` 结果
  - 链接

## Android Release

- 待回填：
  - run id
  - 结论
  - 发布时间
  - 链接

## Release 页面

- 目标资产：
  - `wenwenlex-v1.8-release.apk`
  - `wenwenlex-voice-pack-en-gb-offline-word-v1.zip`
  - `wenwenlex-voice-pack-en-us-offline-word-v1.zip`
  - 两个 voice pack manifest
  - `wenwenlex-voice-pack-checksums.txt`

## release-notes.json

- 目标文件：`app/src/main/assets/release-notes/release-notes.json`
- 当前预期：
  - `1.8` 位于列表首项
  - 包含 `Added` / `Changed` / `Fixed` / `Notes` 四类条目
  - App 内更新日志可直接展示本轮版本说明

## 本机归档

- 归档目录：`/Users/yueliangmanle/Desktop/codex/danci/releases/v1.8`
- 待回填已下载文件清单

## 当前已验证范围

- Excel 导入诊断、自动修复与 AI 修复严重问题闭环
- 统一发音源中心第一版、MiMo TTS 详情、会话级快速切源与 API 检测
- 多来源缓存桶与缓存管理页
- 单词 / 整书 / 分批后台音频生成，以及失败项真实重试
- App 内更新日志与 `release-notes.json` 资产生成
- 本地 `:app:testDebugUnitTest` 全量回归通过

## 真机补充验证待办

- 待补充核对 MiMo TTS 播放、缓存命中与默认 source 切换体验
- 待补充核对语音包下载断点续传与安装回退体验
- 待补充核对整本词书后台生成时的前台学习流不卡顿
