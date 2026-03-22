# 文文Lex v1.7 学习效果与交付护栏发版 Smoke 记录

- 当前状态：开发中，正式 `Android Release` 尚未触发
- 开发分支：`codex/v1-4-real-offline-pronunciation`

## Android CI

- `Android CI #23400597350`
  - 结论：通过
  - 范围：Task 3 学习反馈时延 / 跳过语义 / 动态复习信号修正
  - 链接：[run #23400597350](https://github.com/yuelangmanle/wenwenlex/actions/runs/23400597350)
- `Android CI #23401041819`
  - 结论：通过
  - 范围：Task 4 日 / 周 / 阶段目标入口、首页 / 我的 / 统计页投影
  - 链接：[run #23401041819](https://github.com/yuelangmanle/wenwenlex/actions/runs/23401041819)

## Android Release

- 待触发
- 计划版本：`1.7`

## Release 页面

- 待创建 `v1.7`
- 预期资产：
  - `wenwenlex-v1.7-release.apk`
  - `wenwenlex-voice-pack-en-gb-offline-word-v1.zip`
  - `wenwenlex-voice-pack-en-us-offline-word-v1.zip`
  - 两个 voice pack manifest
  - `wenwenlex-voice-pack-checksums.txt`

## 本机归档

- 待正式发版后补充

## 当前已验证范围

- 动态复习优先级、时延与跳过信号写入
- 日 / 周 / 阶段目标设置与统计投影

## 后续待补

- 升级安全检查与诊断中心 UI 二次云端验证
- 发版护栏脚本接入 `Android CI` / `Android Release`
- `v1.7` 正式 Release 与资产核对
