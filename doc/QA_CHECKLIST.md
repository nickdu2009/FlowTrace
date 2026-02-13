# QA 检查清单（MVP，必须遵守）

本清单用于将实现与 `doc/ANDROID_MVP_REQUIREMENTS.md` 的验收标准（AC）对齐，并明确最小验证步骤。

## 1. 需求映射（Traceability）

- 每个 PR/变更必须标注：
  - 覆盖的 `FR-xx` / `AC-xx`
  - 若改变默认行为/约束/取舍：对应 ADR（`doc/adr/*`）

## 2. 手工验收用例（最小集合）

### AC-01 非 Root 抓包 + 会话展示
- 启动抓包（VPN 授权 + 前台通知）成功
- 访问若干 HTTP/HTTPS 站点后，会话列表有新增记录
- 进入详情页可查看 request/response 基本字段（URL、headers、body 预览）

### AC-02 HTTPS 明文与 pinning 提示
- 安装用户 CA 后：
  - 对非 pinning App：HTTPS 会话标记为可明文化（并能看到可读内容）
  - 对 pinning App：标记不可解密并给出原因提示（至少“疑似 pinning/不被信任/不支持”）

### AC-03 规则生效（至少 3 条用例）
- Block：命中规则后请求被拦截（或返回错误/断开，表现与文档一致）
- Rewrite Header：命中规则后 header 被修改（在详情中可观察到）
- Host Map：命中规则后流量指向指定 ip:port（可通过目标服务日志/返回内容验证）

### AC-04 导出（HAR + 脱敏）
- 导出 HAR 后可在桌面工具打开
- 导出的 headers 中敏感字段默认脱敏（Authorization/Cookie/Set-Cookie 等）

### AC-05 数据治理与稳定性
- 设置/默认的保存上限生效（超过上限滚动清理）
- 一键清空数据生效（DB + body files）
- 连续运行 30 分钟中等强度流量无明显卡顿/无 OOM/无 ANR

## 3. 性能门槛（MVP）

- JNI 回调链路不做重活（不写 DB/文件、不做大 JSON 序列化）
- 事件通道有界 + 有明确 overflow 策略
- 大 body 分离：DB 仅存 `BodyRef`，超限截断并标记

## 4. 安全门槛（MVP）

- 默认脱敏策略在：
  - 入库（存储层）
  - 导出（Exporter）
  - DevLog（禁止记录敏感信息）
  均保持一致

详见：`doc/SECURITY_GUIDE.md`

