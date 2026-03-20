# Sherpa ONNX Android Runtime

`文文Lex` 的 Sherpa ONNX Android runtime 元信息保留在这个目录。

## 当前约定

- 官方来源：`k2-fsa/sherpa-onnx` GitHub Releases
- 官方最新 Android 分发形态已经变成 `sherpa-onnx-v<version>-android.tar.bz2`
- 真正参与打包的是解压后的 `jniLibs/`，目标目录是 `app/src/main/jniLibs/`
- 本目录只保留版本记录、SHA-256 记录和维护说明，不放单个语音包模型
- 英式 / 美式离线口音包会走应用内下载，安装到 `files/voice-packs/native/<packId>/`

## 获取方式

执行：

```bash
./scripts/fetch_sherpa_onnx_android.sh v1.12.31
```

脚本会：

- 下载官方 `android.tar.bz2`
- 解压其中的 `jniLibs/*` 到 `app/src/main/jniLibs/`
- 在当前目录写入 `sherpa-onnx-android-version.txt`
- 在当前目录写入本次下载归档的 `sha256`

如果你想临时跟随官方最新版本，可执行：

```bash
./scripts/fetch_sherpa_onnx_android.sh latest
```

## 已核对的官方信息

- 核对日期：`2026-03-20`
- GitHub 官方 latest release：`v1.12.31`
- 官方 Android 资产：`sherpa-onnx-v1.12.31-android.tar.bz2`

## 与源码的关系

- JNI `.so` 由官方 release 提供
- `app/src/main/java/com/k2fsa/sherpa/onnx/` 下保留最小 Java wrapper，负责和 JNI 层对接
- `SherpaOnnxRuntime.kt` 再在业务侧封装模型目录识别、错误提示和输出校验
