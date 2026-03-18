# 文文Lex Release Signing

这份文档说明文文Lex 的正式签名发版约定。

## 目标

- GitHub Actions 在云端直接产出 `release-signed APK`
- 用户后续版本可以直接覆盖升级
- 签名 key 不进入仓库

## GitHub Secrets

仓库需要维护以下 4 个 secrets：

- `ANDROID_RELEASE_KEYSTORE_BASE64`
- `ANDROID_RELEASE_STORE_PASSWORD`
- `ANDROID_RELEASE_KEY_ALIAS`
- `ANDROID_RELEASE_KEY_PASSWORD`

## 工作流行为

`Android Release` 工作流会：

1. 校验版本号格式
2. 校验上述 4 个 secrets 是否齐全
3. 解码 keystore
4. 运行单元测试
5. 构建 `assembleRelease`
6. 用 `apksigner verify` 校验 APK 签名
7. 创建或更新 GitHub Release，并上传正式 APK

## 升级规则

- 只要正式版本始终使用同一把签名 key，Android 用户就能直接升级
- 一旦更换签名 key，已安装旧版本的用户无法直接覆盖安装
- 如果必须换 key，就要提前让用户备份，然后卸载旧版本后再装新版本

## 版本规则

- 首发版本：`1.0`
- 后续规则：每次增加 `0.1`
- 进位规则：`1.9 -> 2.0`
