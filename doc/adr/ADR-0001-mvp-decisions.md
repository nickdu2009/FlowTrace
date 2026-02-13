# ADR-0001：Android 抓包 MVP 关键决策

- **状态**：Accepted
- **日期**：2026-02-13
- **上下文**：`doc/ANDROID_MVP_REQUIREMENTS.md`（需求与验收基线）

## 决策 1：最低支持 Android 8.0（API 26）

### 决策
- `minSdk=26`

### 原因
- VPN 前台服务与后台限制在 8.0+ 更可控，减少兼容性成本。

### Pros vs. Cons
- **Pros**：减少 ROM/系统差异导致的抓包稳定性问题；交付更快
- **Cons**：覆盖面下降（低版本用户无法使用）

## 决策 2：Android 技术栈（Compose + Coroutines/Flow + Room + Hilt）

### 决策
- UI：Compose + Material 3
- 异步：Coroutines + Flow
- 存储：Room（SQLite）
- DI：Hilt

### 原因
- 会话列表/详情/筛选属于高迭代 UI；抓包数据天然是“事件流”，Flow 模型契合；Room 适合本地检索与筛选。

### Pros vs. Cons
- **Pros**：开发效率高；响应式数据流顺滑；生态成熟
- **Cons**：Compose/Flow/Room 需要团队熟练度；Room schema 变更需管理迁移

## 决策 3：抓包内核使用 SunnyNet，Android 走 Tun(VPN) 驱动

### 决策
- 协议解析与改写交给 SunnyNet（HTTP/HTTPS/WS）
- Android 流量接入通过 `VPNService` + TUN，SunnyNet 以 Tun(VPN) 模式工作

### 原因
- SunnyNet 自带多协议能力、回调与改写接口；README 明确存在 Android Tun(VPN) 路径。

### Pros vs. Cons
- **Pros**：减少自研网络栈成本；改包能力完整
- **Cons**：HTTPS 解密受证书信任与 pinning 限制；native 稳定性需治理

参考：SunnyNet 项目与 API 说明见 `https://github.com/qtgolang/SunnyNet`

## 决策 4：接入方式选择 JNI 同进程直连（MVP）

### 决策
- SunnyNet 以 `.so` 方式与 App **同进程**运行，通过 JNI 回调上报事件

### 原因
- MVP 优先快速跑通链路与体验；同进程延迟低、实现路径最短。

### Pros vs. Cons
- **Pros**：实现快；延迟低；调试链路简单
- **Cons**：native 崩溃会带崩 App（V2 可演进独立进程 + IPC 隔离）

## 决策 5：事件协议 + 聚合器（乱序容忍）作为稳定业务边界

### 决策
- 定义领域事件：`HttpRequestStarted/HttpResponseCompleted/HttpRequestFailed`、`WsConnected/WsMessageFrame/WsDisconnected`
- 使用 `Aggregator` 按 `sessionId` 合并，产出 `SessionSummary/SessionDetail`

### 原因
- SunnyNet 回调是底层细节，直接驱动 UI/DB 会导致耦合与性能风险；事件协议是稳定边界，方便演进（断点/重放/脚本）。

### Pros vs. Cons
- **Pros**：解耦 JNI 与业务；方便扩展；利于测试
- **Cons**：聚合逻辑复杂（需要处理乱序/缺失/重复）

详见：`doc/EVENT_PROTOCOL.md`

## 决策 6：性能策略（有界队列 + 背压 + 批处理落盘）

### 决策
- JNI 回调线程只做轻量封装，事件进入 **有界队列**
- 后台 worker 批量 drain 事件并写入存储
- 队列溢出时优先丢弃低价值事件（例如大 body 分片/重复进度）

### 原因
- 高并发场景下 UI/DB 是瓶颈；必须避免在抓包链路做重操作。

### Pros vs. Cons
- **Pros**：避免 OOM/ANR；吞吐可控；体验稳定
- **Cons**：极端情况下可能丢失部分细节（需在 UI 明确提示“已截断/降采样”）

详见：`doc/ARCHITECTURE.md`

## 决策 7：数据存储（元数据入库 + Body 分离落盘）

### 决策
- DB 仅保存会话索引与摘要（便于筛选/排序）
- Body 存文件（或 blob store），DB 保存 `BodyRef`

### 原因
- 大字段写 DB 会拖慢查询与 UI；分离有助于性能与稳定性。

### Pros vs. Cons
- **Pros**：查询快；存储成本可控；避免 DB 膨胀
- **Cons**：需要管理文件生命周期（清理、上限、引用一致性）

## 决策 8：安全策略（默认脱敏 + 导出同策略）

### 决策
- 入库前与导出时统一经过 `Redactor`：
  - Header：`Authorization/Cookie/Set-Cookie/*token*/*secret*/*apikey*/*session*` 默认打码

### 原因
- 抓包数据天然敏感，默认安全优先，避免误分享/误上传泄露。

### Pros vs. Cons
- **Pros**：降低泄露风险；满足合规与企业安全要求
- **Cons**：排障时可能需要临时关闭脱敏（需受控开关与明显提示）

## 决策 9：导出格式（HAR 优先，JSON 兜底）

### 决策
- 优先导出 HAR（便于接入各类桌面/分析工具）
- 兜底导出自定义 JSON（保留全部字段，便于自研分析）

### Pros vs. Cons
- **Pros**：HAR 生态好；JSON 兜底保证不被格式限制
- **Cons**：HAR 对 WS 等需要自定义扩展字段（需兼容性处理）

