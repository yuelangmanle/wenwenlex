# 文文Lex v1.4 发音发版 Smoke 记录

## 1. 目标

这份文档用于记录 `v1.4` 正式发版前后的最小验证证据，重点覆盖：

- GitHub Actions 云端构建是否通过
- Release 页面是否上传了完整 APK / 语音包资产
- UK / US 真实离线单词发音包是否切到 `kokoro-en-v0_19`
- 双口音 speaker 是否按包隔离

## 2. 云端验证清单

- `Android CI`
  - 关注项：`Build Debug APK`
  - 关注项：`Connected Debug Android Test`
- `Android Release`
  - 关注项：`Build And Publish Signed Release`
  - 关注项：Release 是否生成 `wenwenlex-v1.4-release.apk`
  - 关注项：Release 是否生成两套 `wenwenlex-voice-pack-*.zip`
  - 关注项：Release 是否生成两套 `wenwenlex-voice-pack-*-manifest.json`
  - 关注项：Release 是否生成 `wenwenlex-voice-pack-checksums.txt`

## 3. 语音包核对项

UK 包应满足：

- `id = en-gb-offline-word-v1`
- `version = 1.4`
- `modelFamily = kokoro`
- `modelVersion = kokoro-en-v0_19`
- `speakerProfile = Kokoro bf_isabella`
- `speakerId = 8`

US 包应满足：

- `id = en-us-offline-word-v1`
- `version = 1.4`
- `modelFamily = kokoro`
- `modelVersion = kokoro-en-v0_19`
- `speakerProfile = Kokoro af_nicole`
- `speakerId = 2`

两包共同要求：

- `downloadUrl` / `manifestUrl` / `checksumsUrl` 均指向 GitHub Releases `latest/download`
- 安装包内 `payloadChecksums` 与 zip 内真实文件一致
- 包内包含 `licenses/KOKORO-LICENSE.txt`
- 包内包含 `licenses/DISTRIBUTION-NOTICE.txt`

## 4. 本轮可记录证据

- 本地脚本验证：
  - `bash scripts/test_voice_pack_release_assets.sh`
- 云端 CI：
  - 待本轮 `codex/v1-4-real-offline-pronunciation` 最新 run 结果回填
- 云端 Release：
  - 待 `Android Release` `version=1.4` 完成后回填

## 5. 真机补充项

当前这台机器缺少本地 Java / Android 真机打包环境，因此以下项目需要在 Release 完成后补记：

- `arm64` 真机安装和冷启动
- 词详情页 UK / US 各抽 20 词听感抽查
- 导入词书单词离线发音抽查
- 第二次播放缓存命中耗时
- 语音包删除 / 重装 / 切换回归

## 6. 结果填写模板

```md
### YYYY-MM-DD HH:mm / 环节

- 运行入口：
- 结果：
- 证据链接：
- 备注：
```
