# 开发日记（Dev Log）

## 目的

以“记日记”的方式记录实现过程，确保每一步都能追溯到：
- 需求条目（FR/AC）：`doc/ANDROID_MVP_REQUIREMENTS.md`
- 关键决策（ADR）：`doc/adr/`
- 架构与事件协议：`doc/ARCHITECTURE.md`、`doc/EVENT_PROTOCOL.md`

## 目录结构

- `doc/devlog/YYYY-MM-DD.md`：按日期记录（推荐每天一个文件）
- 或 `doc/devlog/YYYY-WW.md`：按周记录（团队更偏向周节奏时）

## 记录模板（建议每次提交/阶段性完成后追加）

复制以下模板到当天文件中：

```text
## 日期：YYYY-MM-DD

### 今日目标（Goal）
- ...

### 关联需求/验收（Traceability）
- FR: FR-xx / FR-yy
- AC: AC-xx / AC-yy
- ADR: ADR-xxxx（如有）

### 变更摘要（What changed）
- 文档：
  - ...
- 代码：
  - ...

### 关键实现细节（How）
- 事件/线程/背压：
  - ...
- 存储/截断策略：
  - ...
- 安全/脱敏：
  - ...

### 验证（Verification）
- 手工用例：
  - ...
- 性能/稳定性观察：
  - ...

### 问题与决策（Issues & Decisions）
- 问题：
  - ...
- 决策：
  - ...

### 风险与后续（Risks & Next）
- ...
```

## 安全约束（强制）

- Dev Log **禁止**记录任何真实用户敏感数据（token、cookie、账号、私钥、完整请求体等）。
- 如需示例，使用**伪造数据**或做脱敏（与 `Redactor` 策略一致）。

