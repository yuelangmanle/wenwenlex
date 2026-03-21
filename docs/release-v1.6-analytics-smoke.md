# 文文Lex v1.6 学习统计与 AI 闭环发版 Smoke 记录

## 1. 目标

这份文档用于记录 `v1.6` 正式发版前后的最小验证证据，重点覆盖：

- GitHub Actions 云端构建是否通过
- Release 页面是否上传了完整 APK / 语音包资产
- 学习统计页、长期摘要投喂、AI 计划效果回看和备份 `v5` 是否进入正式发布
- 后续 `v1.6` 接手者是否能快速定位这轮发版的入口、当前状态与待回填项

## 2. 当前基线状态

- 当前正式版本仍是 `1.5`
- 当前 `1.6` 代码已经落仓，并已完成 `Android CI`，但尚未完成 `Android Release` 的最终云端回填
- 当前本机缺少 Java runtime，无法在本机直接运行 Gradle；本轮最终验证继续以 GitHub Actions 为准
- 当前 `1.6` 的实施计划见 [2026-03-21-v1.6-learning-analytics-ai-loop.md](./superpowers/plans/2026-03-21-v1.6-learning-analytics-ai-loop.md)

## 3. 云端验证清单

- `Android CI`
  - 关注项：`Build Debug APK`
  - 关注项：`Connected Debug Android Test`
- `Android Release`
  - 关注项：`Build And Publish Signed Release`
  - 关注项：Release 是否生成 `wenwenlex-v1.6-release.apk`
  - 关注项：Release 是否生成两套 `wenwenlex-voice-pack-*.zip`
  - 关注项：Release 是否生成两套 `wenwenlex-voice-pack-*-manifest.json`
  - 关注项：Release 是否生成 `wenwenlex-voice-pack-checksums.txt`

## 4. 本轮业务核对项

- 首页存在“查看学习统计”入口，且点击可进入统计页
- “我的”页存在学习统计入口，且点击可进入统计页
- 学习统计页可展示概览卡片、HTML 看板和长期摘要
- HTML 看板至少覆盖趋势、反馈分布、计划效果和发音使用四个区块
- AI 计划中心仅在存在真实 `APPLIED` 计划效果样本时展示“最近调整效果”
- 长期摘要与计划效果数据已进入 AI 计划输入
- 本地备份已升级到 `v5`，可覆盖学习统计快照和长期摘要，并兼容恢复旧 `v1` 到 `v4` 备份

## 5. 当前已落仓能力

- 学习统计快照与长期摘要已持久化到 `AiMemorySummary`
- Room 数据库已升级到 `v6`
- 本地备份已升级到 `v5`
- 学习统计页 / HTML 看板、首页 / “我的”页入口已落地
- AI 计划中心已补入“最近调整效果”卡片与效果展示收紧逻辑
- 统计入口点击测试和 `AiPlanCenterViewModel` 效果展示测试已补齐

## 6. 待回填证据

- 云端 CI：
  - 运行入口：`Android CI #23384177140`
  - 结果：通过
  - 证据链接：<https://github.com/yuelangmanle/wenwenlex/actions/runs/23384177140>
- 云端 Release：
  - 运行入口：待回填
  - 结果：待回填
  - 证据链接：待回填
- Release 页面：
  - 标签页：待回填
  - 资产核对：待回填

## 7. 本机归档

- 当前暂未新建 `v1.6` 本机归档目录
- 待正式 Release 通过后，再按需补充 APK / checksum 的本机归档路径

## 8. 已回填记录

### 2026-03-22 00:59 / 云端 CI

- 运行入口：`Android CI #23384177140`
- 结果：通过
- 证据链接：<https://github.com/yuelangmanle/wenwenlex/actions/runs/23384177140>
- 备注：`Build Debug APK` 与 `Connected Debug Android Test` 均为绿色；其中第一次 `Android CI #23383948423` 暴露出的 analytics 单测回归已在 `b04a53f` 修复后消除

## 9. 回填模板

```md
### YYYY-MM-DD HH:mm / 环节

- 运行入口：
- 结果：
- 证据链接：
- 备注：
```
