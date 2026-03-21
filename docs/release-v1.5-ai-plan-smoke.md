# 文文Lex v1.5 AI 计划发版 Smoke 记录

## 1. 目标

这份文档用于记录 `v1.5` 正式发版前后的最小验证证据，重点覆盖：

- GitHub Actions 云端构建是否通过
- Release 页面是否上传了完整 APK / 语音包资产
- AI 计划中心、计划对比 / 解释页和备份 `v4` 是否进入正式发布
- 后续 `v1.6` 接手者是否能快速定位这轮发版的入口与结果

## 2. 云端验证清单

- `Android CI`
  - 关注项：`Build Debug APK`
  - 关注项：`Connected Debug Android Test`
- `Android Release`
  - 关注项：`Build And Publish Signed Release`
  - 关注项：Release 是否生成 `wenwenlex-v1.5-release.apk`
  - 关注项：Release 是否生成两套 `wenwenlex-voice-pack-*.zip`
  - 关注项：Release 是否生成两套 `wenwenlex-voice-pack-*-manifest.json`
  - 关注项：Release 是否生成 `wenwenlex-voice-pack-checksums.txt`

## 3. 本轮业务核对项

- 首页、学习页和“我的”页均存在 AI 计划中心入口
- AI 计划中心支持查看当前生效计划、待确认调整和历史时间轴
- 计划对比页固定以当前 `APPLIED` 版本为基线
- 计划解释页展示“为什么改 / 改了什么 / 预期影响 / 触发信号”
- 备份 `v4` 已纳入扩展计划历史和 checkpoint 摘要

## 4. 本轮可记录证据

- 云端 CI：
  - `Android CI #23377258763`
  - 结果：通过
  - 入口：<https://github.com/yuelangmanle/wenwenlex/actions/runs/23377258763>
  - 关键结论：
    - `Build Debug APK` 通过
    - `Connected Debug Android Test` 通过
    - “我的”页 AI 计划入口滚动断言问题已修复
- 云端 Release：
  - `Android Release #23377422188`
  - 结果：通过
  - 入口：<https://github.com/yuelangmanle/wenwenlex/actions/runs/23377422188>
  - Release 页面：<https://github.com/yuelangmanle/wenwenlex/releases/tag/v1.5>

## 5. Release 资产核对结果

本轮 `v1.5` Release 页面已确认存在以下资产：

- `wenwenlex-v1.5-release.apk`
  - `sha256 = a858f5502003b57a9189a65a56c8624be35671b54514f53df5686c128761d3af`
- `wenwenlex-voice-pack-en-gb-offline-word-v1.zip`
  - `sha256 = 5163e736f30956169ab2c040e88c93423b3274a6967ffc06b3c44ed1bee8331c`
- `wenwenlex-voice-pack-en-us-offline-word-v1.zip`
  - `sha256 = b9199e5fc82343185493c9878a90ac56e349a8417aa4fd09bb9b80909fa6feca`
- `wenwenlex-voice-pack-en-gb-offline-word-v1-manifest.json`
  - `sha256 = 7e459121b795822fb453f545c8e50f7d687ffb1d59be1104183f65e8bc0e8b73`
- `wenwenlex-voice-pack-en-us-offline-word-v1-manifest.json`
  - `sha256 = 86496b819f37c325e9645585bfc54fff255d0fbc3184d5c5685a6664ad28624a`
- `wenwenlex-voice-pack-checksums.txt`
  - `sha256 = cb09b7eda559fc4df63f9bb9712286869e4549fcd15a27a3873035658c6b8930`

## 6. 本机归档

本机已同步下载一份 `v1.5` 正式安装包和 checksum 文件，路径如下：

- `/Users/yueliangmanle/Desktop/codex/danci/releases/v1.5/`

说明：

- 当前本机归档的是 APK 和 checksum 文件，完整 Release 资产仍以 GitHub Release 页面为准
- 语音包 zip 与 manifest 已在 Release 页面完成核对，无需本机重复长期占用存储

## 7. 本轮回填记录

### 2026-03-21 18:12 / 云端 CI

- 运行入口：`Android CI #23377258763`
- 结果：通过
- 证据链接：<https://github.com/yuelangmanle/wenwenlex/actions/runs/23377258763>
- 备注：`Build Debug APK` 与 `Connected Debug Android Test` 均为绿色；“我的”页 AI 计划入口断言已调整为滚动后校验

### 2026-03-21 18:23 / 云端 Release

- 运行入口：`Android Release #23377422188`
- 结果：通过
- 证据链接：<https://github.com/yuelangmanle/wenwenlex/actions/runs/23377422188>
- 备注：签名 APK、两套语音包 zip、两个 manifest 与 checksum 已上传到 `v1.5` Release

### 2026-03-21 18:23 / 本机归档

- 运行入口：`gh release download v1.5`
- 结果：通过
- 证据链接：`/Users/yueliangmanle/Desktop/codex/danci/releases/v1.5/`
- 备注：本机已保留一份 `v1.5` 安装包与 checksum，便于后续安装、校验和回归

## 8. 结果填写模板

```md
### YYYY-MM-DD HH:mm / 环节

- 运行入口：
- 结果：
- 证据链接：
- 备注：
```
