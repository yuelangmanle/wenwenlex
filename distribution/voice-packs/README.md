# Native Voice Pack Sources

这里存放文文Lex 的 native 语音包发布源目录。

约定如下：

- 每个语音包一个独立目录
- 目录根必须包含 `manifest.json`
- `manifest.json` 的 `entryFiles` 必须全部落在当前目录内
- 如果 `licenses` 使用对象并声明 `file`，对应文件也必须真实存在
- `scripts/package_voice_packs.sh` 会把目录根直接打进 zip，不带额外顶层文件夹

当前这批 pack 先用于打通 GitHub Release 分发链路和安装校验链路，后续可以直接把占位模型文件替换成真实模型资产，而不改发布流程。
