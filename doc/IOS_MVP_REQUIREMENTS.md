# iOS 抓包软件（NetworkExtension Packet Tunnel）MVP 需求确认

## 背景与目标

我们需要在 iOS 上实现一款抓包软件，面向研发/测试调试场景，提供对移动端网络请求的捕获、分析、（有限）修改与导出能力。

iOS 平台的系统级流量接入必须基于 `NetworkExtension` 的 `NEPacketTunnelProvider`（见 ADR：`doc/adr/ADR-0002-ios-platform-strategy.md`）。

## 技术栈与兼容性约束（MVP）

- **最低支持版本**：iOS **15.0**
- **抓包方式**：`NetworkExtension` + `NEPacketTunnelProvider`（Packet Tunnel Extension）
- **数据共享**：App Group（Extension 写入，主 App 展示/导出）
- **协议范围（MVP）**：HTTP / HTTPS（尽可能明文）/ WebSocket（尽可能）
- **安全默认值**：默认脱敏；导出 HAR 默认脱敏（与 Android 保持一致策略）

## 与 Android 的关键差异（必须显式说明）

- **按应用抓包（FR-02）**：
  - Android：MVP 支持“仅抓选中 App / 排除选中 App”
  - iOS：MVP 不将“用户在 App 内选择抓哪些 App”作为强保证能力，默认以全局隧道方式运行
- **运行形态**：
  - iOS 抓包核心在 Extension 中运行，主 App 不是抓包执行进程
- **证书安装与信任**：
  - iOS 需要用户安装证书并在系统设置中启用“完全信任”（引导与风险提示必须清晰）

## 范围（MVP）

### In-Scope（必须）

#### FR-01 抓包启停
- App 内一键开启/关闭抓包。
- 抓包运行状态在 UI 中可见（Running/Starting/Stopping/Error）。
- 异常退出/权限撤销时，提示原因并可重新开启。

#### FR-02 按应用抓包（iOS 差异化）
- iOS MVP：
  - 默认以全局隧道方式抓包（不承诺 per-app 选择能力）
  - 如系统/企业配置支持 per-app VPN，允许作为增强能力在 V2 评估

#### FR-03 会话列表
列表项至少包含：
- 时间、Host、方法、Path（或 URL 摘要）
- HTTP 状态码（若可解析）、耗时、上下行大小（尽可能）
- 标记：
  - HTTPS 是否解密成功（tlsDecrypted）
  - 是否命中规则（Block/Rewrite/HostMap）

#### FR-04 会话详情
- Request：URL、Headers（可复制）、Body（文本预览；二进制仅摘要）
- Response：Status、Headers、Body（同 Request）
- 网络信息：远端地址（尽可能）、协议版本（尽可能）

#### FR-05 过滤与搜索
- 过滤项：Host、方法、状态码区间、时间范围、是否解密成功。
- 搜索：关键字（至少 URL + Header），大小写不敏感。

#### FR-06 HTTPS 解密（证书）
- 提供“证书管理”页面：
  - 生成 CA（或导入 P12/PEM），展示指纹（SHA-256）
  - 引导安装与“完全信任”开关路径（按 iOS 版本差异提示）
  - 检测证书是否可用（尽可能）
- 对不可解密会话：
  - 标记原因：疑似 pinning / 用户证书不被信任 / 协议不支持等（以可判断信息为准）

#### FR-07 规则（轻量）
- 规则管理：新增/编辑/启用/禁用/删除；支持优先级（priority）。
- 匹配维度（MVP 最小集合）：Host、URL 前缀、方法（应用维度在 iOS MVP 可不作为必需）。
- 动作：
  - Block：拦截请求（实现策略以 iOS 平台可行性为准）
  - Rewrite Header：增删改
  - Host Map：将目标域名转发到指定 `ip:port`
  - Body Replace（可选，MVP 轻量版）：仅对 `text/*`、`application/json` 做简单替换
- 规则命中结果在会话中可见（命中哪条规则、执行了什么动作）。

#### FR-08 导出
- 导出格式：
  - HAR（优先）
  - 自定义 JSON（兜底）
- 导出范围：单条、筛选结果、多选导出。
- 导出前选项：
  - **脱敏（默认开启）**
  - 是否包含 Body（默认开启，超大 Body 可能截断并标注）

#### FR-09 数据管理
- 会话保存策略：默认保存最近 N 条或最近 M MB（可配置）；超限滚动清理（LRU/按时间）。
- 支持“一键清空所有抓包数据”。

### Out-of-Scope（MVP 明确不做）
- 绕过证书锁定（pinning）的能力。
- 完整 QUIC/HTTP3 明文解析与解密（MVP 仅识别/提示或统计）。
- 断点/交互式请求编辑器（Breakpoints）、请求重放（Replay）。
- 团队协作/远程同步/云端共享。

## 非功能需求（NFR）

### NFR-Security（安全）
- **默认脱敏**：入库/导出前对敏感字段打码（与 Android 相同策略）。
- **风险提示**：开启 HTTPS 解密必须展示风险告知并要求用户确认。

### NFR-Performance（性能）
- Extension 内事件处理必须具备背压与批处理策略（有界队列、溢出丢弃低价值事件）。
- 大 body 分离落盘；DB 仅存索引与 `BodyRef`；按需加载。

### NFR-Stability（稳定性）
- 连续运行 30 分钟不崩溃、不明显卡顿（中等强度流量基准）。
- 对 Extension 异常退出/系统回收有可观测状态与恢复路径（至少提示原因并允许重启）。

## 验收标准（Acceptance Criteria，iOS）

- AC-iOS-01：可在 iOS 上成功开启/关闭抓包，并能显示 HTTP/HTTPS/WS 会话（至少列表与详情可用）。
- AC-iOS-02：安装并信任 CA 后，对未启用 pinning 的 App 能看到 HTTPS 明文；对 pinning 场景会话标记清晰。
- AC-iOS-03：规则 Block / Rewrite Header / Host Map 至少各有 1 个可验证用例通过（允许按 iOS 可行性做降级并记录到 ADR/需求）。
- AC-iOS-04：导出 HAR 在桌面工具中可打开；默认脱敏生效（敏感字段不可明文泄露）。
- AC-iOS-05：存储上限与清空功能可用；长时间运行不明显卡顿。

## 风险与约束（已知）

- **Entitlement 与上架审核**：NetworkExtension 能力需要配置与审批，可能影响分发策略（见 `doc/adr/ADR-0002-ios-platform-strategy.md`）。
- **证书锁定（pinning）**：无法通过普通用户 CA 完全解决，MVP 仅做提示与标记。
- **平台差异**：iOS MVP 不强保证按应用抓包能力，避免与 Android 验收混淆。

