# 贡献与协作规范（必须遵守）

## 1. 分支与提交

- **分支命名**：`feat/<topic>`、`fix/<topic>`、`chore/<topic>`、`docs/<topic>`
- **提交信息**：Conventional Commits
  - `feat:` 新功能
  - `fix:` 缺陷修复
  - `docs:` 文档变更
  - `chore:` 构建/工具链/杂项

## 2. PR 要求（Definition of Done）

PR 描述必须包含：
- 关联的 `FR-xx`/`AC-xx`（需求追溯）
- 如涉及取舍/约束变化：ADR 链接（`doc/adr/*`）
- 验证方式与结果（手工用例/日志观测/性能现象）
- 文档回写（如有）：`doc/README.md` 与 `doc/devlog/` 已更新

## 3. 禁止提交（安全红线）

- 证书私钥、P12 明文、真实抓包数据、token/cookie/账号密码
- 大体积二进制文件（除非明确需要且记录 ADR）

## 4. 代码评审关注点

- 是否遵守分层边界（Domain 不依赖 Android/JNI/Room）
- JNI 回调是否存在重操作（DB/文件/大对象分配）
- 是否保持默认脱敏与导出脱敏
- 是否有背压与截断标记

