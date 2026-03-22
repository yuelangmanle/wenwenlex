# 文文Lex v1.7 学习效果与交付护栏发版 Smoke 记录

- 当前状态：已完成云端正式发版并归档
- 开发分支：`codex/v1-4-real-offline-pronunciation`
- Release 页面：[v1.7](https://github.com/yuelangmanle/wenwenlex/releases/tag/v1.7)

## Android CI

- `Android CI #23400597350`
  - 结论：通过
  - 范围：Task 3 学习反馈时延 / 跳过语义 / 动态复习信号修正
  - 链接：[run #23400597350](https://github.com/yuelangmanle/wenwenlex/actions/runs/23400597350)
- `Android CI #23401041819`
  - 结论：通过
  - 范围：Task 4 日 / 周 / 阶段目标入口、首页 / 我的 / 统计页投影
  - 链接：[run #23401041819](https://github.com/yuelangmanle/wenwenlex/actions/runs/23401041819)
- `Android CI #23401721564`
  - 结论：通过
  - 范围：修复诊断中心版本信息读取链路后，重新完成整条云端验证
  - 结果：`Build Debug APK` 与 `Connected Debug Android Test` 均为绿色
  - 链接：[run #23401721564](https://github.com/yuelangmanle/wenwenlex/actions/runs/23401721564)

## Android Release

- `Android Release #23404026655`
  - 结论：通过
  - 发布时间：`2026-03-22 21:35`（北京时间）
  - 链接：[run #23404026655](https://github.com/yuelangmanle/wenwenlex/actions/runs/23404026655)

## Release 页面

- 已创建 [v1.7](https://github.com/yuelangmanle/wenwenlex/releases/tag/v1.7)
- 实际资产：
  - `wenwenlex-v1.7-release.apk`
  - `wenwenlex-voice-pack-en-gb-offline-word-v1.zip`
  - `wenwenlex-voice-pack-en-us-offline-word-v1.zip`
  - 两个 voice pack manifest
  - `wenwenlex-voice-pack-checksums.txt`

## 本机归档

- 归档目录：`/Users/yueliangmanle/Desktop/codex/danci/releases/v1.7`
- 已下载文件：
  - `wenwenlex-v1.7-release.apk`
  - `wenwenlex-voice-pack-checksums.txt`

## 当前已验证范围

- 动态复习优先级、时延与跳过信号写入
- 日 / 周 / 阶段目标设置与统计投影
- 升级安全检查、诊断中心入口与诊断包导出
- 发版护栏脚本接入 `Android CI` / `Android Release`
- `v1.7` 正式 Release 与资产核对

## 备注

- 当前本机仍缺 Java runtime，因此本次最终构建、测试与正式发版验证仍以 GitHub Actions 为准
