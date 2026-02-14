# ADR-0002：iOS 平台支持策略（NetworkExtension Packet Tunnel）

- **状态**：Accepted
- **日期**：2026-02-14
- **上下文**：
  - 现有 MVP 文档以 Android 为主：`doc/ANDROID_MVP_REQUIREMENTS.md`
  - Android 关键决策：`doc/adr/ADR-0001-mvp-decisions.md`
  - 架构边界：`doc/ARCHITECTURE.md`

## 背景

项目目标从 Android 扩展为 **Android + iOS** 双平台抓包工具（MVP），但 iOS 与 Android 在系统级流量接入能力上存在本质差异：

- Android：`VpnService` + TUN，可在 App 进程内建立 VPN 服务。
- iOS：必须使用 `NetworkExtension`（`NEPacketTunnelProvider`）在 **App Extension** 中提供 packet tunnel。

同时，iOS 的 NetworkExtension 能力涉及：
- Entitlements 与签名配置（需要 `packet-tunnel-provider` 能力）。
- 上架审核与合规（能力申请、用途说明、隐私与数据处理声明）。
- 与主 App 的数据共享方式（App Group）。

因此需要一个明确的 iOS 平台策略，避免在需求与工程结构上走弯路。

## 决策

### 决策 1：iOS 抓包入口采用 `NetworkExtension` + `NEPacketTunnelProvider`

- iOS 端以 **Packet Tunnel** 作为唯一的系统级流量接入方式。
- 抓包核心运行在 **Packet Tunnel Extension** 中，主 App 负责 UI、配置与数据展示。

### 决策 2：iOS MVP 的“按应用抓包”不作为强保证能力

- Android 继续支持 FR-02（按应用抓包）作为 MVP 能力之一。
- iOS MVP 默认按“设备全局隧道”工作；不把“用户在 App 内选择抓哪些 App”作为 iOS 的验收前置条件。
- 相关差异在需求与验收标准中显式标注（见 `doc/IOS_MVP_REQUIREMENTS.md`）。

### 决策 3：iOS 与主 App 的数据共享使用 App Group（统一落盘）

- Extension 与主 App 通过 **App Group 容器目录**共享：
  - 会话索引数据库（SQLite）
  - Body 文件（与 Android 一致：Body 分离落盘，DB 仅存 `BodyRef`）
  - 导出文件（HAR/JSON）
- Extension 侧写入，主 App 侧查询/展示/导出（减少跨进程通信复杂度）。

### 决策 4：iOS 最低版本设为 iOS 15.0+

- 目标：减少历史兼容成本，优先使用较稳定的系统能力与现代并发模型（Swift Concurrency 可用）。
- 若后续覆盖面要求改变，需要新增 ADR 调整最低版本。

## 方案对比（Pros vs. Cons）

### 方案 A：iOS 使用 NetworkExtension Packet Tunnel（本 ADR 采纳）

- **Pros**
  - 符合 iOS 平台能力边界：系统级流量接入的官方路径
  - 与 Android 的“VPN/TUN 接入”在概念上对齐（都是系统级隧道）
  - 可长期演进（证书、规则、导出、持久化等能力可持续迭代）
- **Cons**
  - 工程复杂度更高：需要 Extension、App Group、签名与配置
  - 受 Entitlement 与审核影响（不确定性需要提前验证）
  - Extension 运行环境受限：资源与生命周期管理更严苛

### 方案 B：iOS 仅抓本 App 流量（不采纳）

- **Pros**：实现简单、无 NetworkExtension 能力门槛
- **Cons**：与抓包产品定位不符（无法抓系统全局/其他 App 流量），价值显著不足

## 影响范围（需要同步的文档与工程约束）

- 需求与验收需要按平台拆分：
  - 新增 `doc/IOS_MVP_REQUIREMENTS.md`
  - `doc/ANDROID_MVP_REQUIREMENTS.md` 继续作为 Android 基线
- 技术栈需要扩展：
  - 更新 `doc/TECH_STACK.md` 增加 iOS（Swift/SwiftUI/NetworkExtension/App Group/SQLite）
- 实施计划需要扩展：
  - 更新 `doc/MVP_IMPLEMENTATION_PLAN.md` 增加 iOS 里程碑（Extension 打通、存储共享、UI 展示）
- 架构边界保持不变：
  - `doc/ARCHITECTURE.md` 的 Domain/Application/Infra 分层继续作为双端共同约束

## 风险与缓解

### 风险 1：NetworkExtension Entitlement 获取与上架审核不确定
- **缓解**：
  - 在工程启动前尽早做“最小可运行样例”的签名与能力验证
  - 若需要企业/内部分发路径，必须以 ADR 记录分发策略与合规说明

### 风险 2：iOS 平台能力差异导致需求不可验收（例如按应用抓包）
- **缓解**：
  - 在需求中将 AC 按平台拆分，避免 iOS 被 Android 的 AC 绑定
  - 对“不支持/降级”的结论必须落在需求或 ADR 中

### 风险 3：敏感数据合规与隐私声明不足
- **缓解**：
  - 继续坚持默认脱敏（`Redactor`）与导出脱敏策略（见 `doc/adr/ADR-0001-mvp-decisions.md`）
  - iOS 侧同样禁止在日志与导出中写入明文敏感信息

