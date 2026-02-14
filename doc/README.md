# 文档索引（FlowTrace Mobile）

## 基线文档（MVP）
- **需求确认（Android）**：`doc/ANDROID_MVP_REQUIREMENTS.md`
- **需求确认（iOS）**：`doc/IOS_MVP_REQUIREMENTS.md`
- **技术栈选择**：`doc/TECH_STACK.md`
- **架构设计**：`doc/ARCHITECTURE.md`
- **事件协议**：`doc/EVENT_PROTOCOL.md`
- **实施计划**：`doc/MVP_IMPLEMENTATION_PLAN.md`
- **架构决策记录（ADR）**
  - `doc/adr/ADR-0001-mvp-decisions.md`
  - `doc/adr/ADR-0002-ios-platform-strategy.md`
  - `doc/adr/ADR-0003-shared-core-kmp.md`
- **协作规范**：`doc/CONTRIBUTING.md`
- **QA 清单**：`doc/QA_CHECKLIST.md`
- **安全指南**：`doc/SECURITY_GUIDE.md`
- **Mermaid 规范**：`doc/MERMAID_STYLE.md`

## 阅读顺序（建议）
1. `ANDROID_MVP_REQUIREMENTS.md` 与 `IOS_MVP_REQUIREMENTS.md`（范围与验收，按平台）
2. `TECH_STACK.md`（技术栈与约束）
3. `adr/ADR-0001-mvp-decisions.md`（Android 关键决策与取舍）
4. `adr/ADR-0002-ios-platform-strategy.md`（iOS 平台策略与边界）
5. `adr/ADR-0003-shared-core-kmp.md`（共享核心策略与边界）
6. `ARCHITECTURE.md`（分层与性能关键点）
7. `EVENT_PROTOCOL.md`（回调→事件→聚合→存储）
8. `MVP_IMPLEMENTATION_PLAN.md`（里程碑与风险）
9. `SECURITY_GUIDE.md`（安全默认值与红线）
10. `QA_CHECKLIST.md`（验收与质量门禁）
11. `CONTRIBUTING.md`（协作与提交规范）

