# Sherpa ONNX Android Runtime

`文文Lex` 的离线原生发音 runtime 预留在这个目录。

## 当前约定

- 官方来源：`k2-fsa/sherpa-onnx` GitHub Releases
- Android 产物：`sherpa-onnx-<version>.aar`
- 本目录只放 runtime AAR，不放单个语音包模型
- 英式 / 美式离线口音包会走应用内下载，安装到 `files/voice-packs/native/<packId>/`

## 获取方式

执行：

```bash
./scripts/fetch_sherpa_onnx_android.sh latest
```

脚本会把官方 AAR 下载到当前目录，并删除旧版本的同类 AAR。

## 已核对的官方信息

- 核对日期：`2026-03-19`
- GitHub 官方 latest release：`v1.12.29`
- 对应 Android AAR：`sherpa-onnx-1.12.29.aar`

后续如果官方 release 命名变化，优先以 GitHub Releases 实际资产名为准。
