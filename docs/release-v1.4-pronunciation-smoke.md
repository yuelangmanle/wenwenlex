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
  - `Android CI #23356835188`
  - 结果：通过
  - 入口：<https://github.com/yuelangmanle/wenwenlex/actions/runs/23356835188>
  - 关键结论：
    - `Build Debug APK` 通过
    - `Connected Debug Android Test` 通过
    - `PronunciationOrchestratorTest cache path` 的卡死问题已修复
- 云端 Release：
  - `Android Release #23357187548`
  - 结果：通过
  - 入口：<https://github.com/yuelangmanle/wenwenlex/actions/runs/23357187548>
  - Release 页面：<https://github.com/yuelangmanle/wenwenlex/releases/tag/v1.4>

## 5. Release 资产核对结果

本轮 `v1.4` Release 页面已确认存在以下资产：

- `wenwenlex-v1.4-release.apk`
  - `sha256 = aeb912796564f52e2a5072d016542f937a30c5ac60910c86c6ff26574349aede`
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

本机已同步下载一份发版资产，路径如下：

- `/Users/yueliangmanle/Desktop/codex/danci/releases/v1.4/`

## 6. 真机补充项

当前这台机器缺少本地 Java / Android 真机打包环境，因此以下项目需要在 Release 完成后补记：

- `arm64` 真机安装和冷启动
- 词详情页 UK / US 各抽 20 词听感抽查
- 导入词书单词离线发音抽查
- 第二次播放缓存命中耗时
- 语音包删除 / 重装 / 切换回归

## 7. 本轮回填记录

### 2026-03-21 02:28 / 云端 CI

- 运行入口：`Android CI #23356835188`
- 结果：通过
- 证据链接：<https://github.com/yuelangmanle/wenwenlex/actions/runs/23356835188>
- 备注：`Build Debug APK` 与 `Connected Debug Android Test` 均为绿色，修复后的发音编排测试不再卡死在 native cache 播放路径

### 2026-03-21 02:41 / 云端 Release

- 运行入口：`Android Release #23357187548`
- 结果：通过
- 证据链接：<https://github.com/yuelangmanle/wenwenlex/actions/runs/23357187548>
- 备注：签名 APK、两套语音包 zip、两个 manifest 与 checksum 已上传到 `v1.4` Release

### 2026-03-21 02:43 / 本地归档

- 运行入口：`gh release download v1.4`
- 结果：通过
- 证据链接：`/Users/yueliangmanle/Desktop/codex/danci/releases/v1.4/`
- 备注：本机已保留一份完整 `v1.4` 发布资产，便于后续真机安装、校验和离线备份

## 8. 结果填写模板

```md
### YYYY-MM-DD HH:mm / 环节

- 运行入口：
- 结果：
- 证据链接：
- 备注：
```
