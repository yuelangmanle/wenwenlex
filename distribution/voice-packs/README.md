# Native Voice Pack Templates

这里不再直接存放可发布的真实模型二进制，而是存放文文Lex native 语音包的模板和来源描述。

每个语音包目录包含：

- `manifest.template.json`
  - 记录 pack 的静态 metadata，例如 `id`、`locale`、`modelFamily`、`speakerId`、许可证声明等
- `source.json`
  - 记录上游固定来源、`sha256`、需要抽取的 payload 文件和目录
- `README.txt`
  - 记录该 pack 的选定 speaker 和人工说明
- `licenses/DISTRIBUTION-NOTICE.txt`
  - 记录“Git 仓库只存模板，真实 payload 由 CI 组装”的分发说明

工作流约定如下：

1. `scripts/prepare_voice_pack_sources.sh` 在 CI / Release 中读取模板目录
2. 它会下载 `source.json` 指定的上游资产并校验 `sha256`
3. 再把真实 payload、上游许可证和本地 notice 组装到临时源目录
4. 最后生成正式 `manifest.json`，补齐 `entryFiles`、`payloadChecksums` 和 `estimatedStorageBytes`
5. `scripts/package_voice_packs.sh` 再把临时源目录打成 GitHub Release 资产

这样可以保证：

- 仓库不保存数百 MB 的模型文件
- GitHub Release 仍然能稳定发布正式语音包 zip / manifest / checksum
- 每次云端发包都能从固定上游来源复现同一批 payload
