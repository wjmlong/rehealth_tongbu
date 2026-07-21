# 睿禾健康端云协同系统 · V2 完整愿景版
# Future Work 技术演进路线

> **版本**: V2.0-Vision  
> **日期**: 2026-05-25  
> **定位**: V1 工程实施版运行稳定后的中长期技术演进方向  
> **前置条件**: V1 核心链路已跑通，种子用户数据积累 ≥ 100 人 × 90 天  
> **核心来源**: 初代 PHM 技术文档 (V0) + Gemini 架构讨论方案

---

## 1. V2 的演进目标

V1（当前工程版）用最低风险的方式跑通了 PIAS 全链路。V2 的目标是在 V1 稳定运行的基础上，**逐步兑现初代 PHM 文档中的完整技术愿景**——让端侧大模型成为现实。

```
V1 (当前) ─────────────────────────────────────── V2 (未来)

端侧: 记忆文件 + 统计算子          →   端侧: Gemma 4 E4B (LiteRT) + 私有 LoRA
云端: XGBoost + LLM + DRE          →   云端: 联邦学习聚合 + 群体级归因 + Agent矩阵
签名: Ed25519                       →   签名: 区块链锚定 + 零知识证明
个性化: RAG (记忆快照注入)          →   个性化: 端侧 LoRA 增量学习 + EWC 防遗忘
设备要求: 任意                      →   旗舰机 (12GB+) 完整版 / 轻量机 降级版
```

**关键原则**：V2 不是推翻 V1，而是在 V1 上叠加能力。V1 的记忆文件 + 云端推理路径永远保留作为降级（fallback）方案。

---

## 2. 端侧大模型回归：有条件的本地部署

### 2.1 前提条件

只有满足以下全部条件时，才激活端侧大模型：

| 条件 | 阈值 |
|------|------|
| 设备 RAM | ≥ 12GB |
| 芯片代际 | A17 Pro+ / 骁龙 8 Gen 3+ / 天玑 9300+ |
| 存储空间 | ≥ 3GB 可用 |
| 系统版本 | iOS 18+ / Android 15+ |

不满足条件的设备，继续使用 V1 的"记忆文件 + 云端推理"路径，用户体验完全一致。

### 2.2 端侧部署方案（源自初代 PHM 文档）

```
模型: Gemma 4 E4B
量化: INT4 (2.2-2.5GB)
编译: Google LiteRT (MediaPipe Tasks) 专机专用硬件编译
     ← 不用通用 GGUF，直接编译到 NPU/Metal/Vulkan 底层
上下文: 2K tokens (严格控制内存)
推理速度: 20-30 tokens/sec (旗舰机)

部署方式:
  Base 模型 → 只读，App 安装包内置 (或首次启动下载)
  LoRA 权重 → 可写，沙盒加密存储，用户可删除
```

### 2.3 端侧 LoRA 增量学习（初代 PHM 核心创新）

**这是初代技术文档和专利的核心内容**，V2 将其有条件地落地。

#### 增量学习闭环

```
白天 (被动收集):
  手表数据 → SQLite → 特征提取 → 记忆文件更新 (与V1一致)

夜间 做梦机制 (深度巩固):
  触发条件: [充电] + [WiFi] + [锁屏] + [凌晨2-5点] + [12GB+ RAM]

  Task 1: 统计特征聚合 (同V1)                        ~30s
  Task 2: 构造 LoRA 微调训练样本                      ~5s
  Task 3: 微批次 LoRA 反向传播 (1-2个epoch)           ~3-5min
  Task 4: EWC 正则化 (防灾难性遗忘)                    ~1min
  Task 5: 权重快照保存 (保留最近3版本用于回滚)         ~10s
  Task 6: 知识蒸馏检查 (KL散度 < 阈值)                ~30s

  总耗时: ~8-15分钟 (充电状态, 用户无感)
```

#### 弹性权重巩固（EWC）

```python
# V2 端侧: 防止 LoRA 增量学习破坏基础医学知识

class EWCRegularizer:
    """
    核心原理: 识别对历史任务最重要的 LoRA 参数,
    对这些参数的更新施加二次惩罚 (Fisher Information Matrix)
    """

    def __init__(self, lora_params, fisher_matrix, lambda_ewc=5000):
        self.old_params = {k: v.clone() for k, v in lora_params.items()}
        self.fisher = fisher_matrix
        self.lambda_ewc = lambda_ewc

    def penalty(self, current_params):
        loss = 0
        for name, param in current_params.items():
            if name in self.fisher:
                loss += (self.fisher[name] * (param - self.old_params[name]) ** 2).sum()
        return self.lambda_ewc * loss
```

#### 权重快照与回滚

```
每次夜间微调前 → 保存当前 LoRA 权重快照
保留最近 3 个版本 (占用约 75MB)
自动回滚条件: 连续 3 次用户对建议的满意度 < 3/5
用户可手动回滚: App 设置 → "回滚到上周的健康模型"
```

### 2.4 端侧 2K 上下文优化序列化

由于端侧 Context 严格限制在 2K，序列化编码必须极度精简：

```python
# V2 端侧: 面向 2K 上下文的极简序列化

class Compact2KSerializer:
    def encode(self, memory: dict) -> str:
        s = memory["static_baseline"]
        d = memory["dynamic_memory_vectors"]
        return (
            f"[S]A:{s['age']};B:{s['bmi']:.1f};TC:{s['total_cholesterol']:.1f};"
            f"LDL:{s['ldl']:.1f};FBG:{s['fbg']:.1f}\n"
            f"[D]SBP:{d['sbp_mean']:.0f}±{d['sbp_std']:.0f};TREND:{d['sbp_slope']:+.2f};"
            f"DBP:{d['dbp_mean']:.0f};NP:{d['night_bp_pattern'][:3]}\n"
            f"[C]HR:{d['resting_hr_mean']:.0f};HRV:{d['hrv_rmssd_mean']:.0f}\n"
            f"[A]Cal:{d['active_calories_daily_mean']:.0f};Sp:{d['sport_freq_weekly']:.1f};"
            f"Sl:{d['sleep_hours_mean']:.1f}\n"
            f"[T]Assess CVD risk, output score and brief intervention."
        )
        # 总 Token 数: 约 100-150 tokens, 为模型输出留充足空间
```

---

## 3. 云端能力升级

### 3.1 联邦学习（跨用户模型进化）

V2 引入联邦学习，在"数据不出端"的前提下实现跨用户的模型知识共享：

```
联邦学习流程:

Round 1:
  端侧 A: 本地 LoRA 微调 → 上传 LoRA 梯度更新 (Δw_A)  ← 不含任何原始数据
  端侧 B: 本地 LoRA 微调 → 上传 LoRA 梯度更新 (Δw_B)
  端侧 C: 本地 LoRA 微调 → 上传 LoRA 梯度更新 (Δw_C)
      │
      ▼
  云端 Aggregator: Δw_global = FedAvg(Δw_A, Δw_B, Δw_C)
      │
      ▼
  下发 Δw_global → 各端侧合并到本地 LoRA

隐私保障:
  - 上传的只是模型梯度, 不是用户数据
  - 可叠加差分隐私 (DP-SGD) 对梯度加噪
  - 即使攻击者截获梯度, 也无法还原原始健康数据
```

### 3.2 群体级 Meta-Analysis 归因

V1 的个体级归因样本量受限，V2 在云端引入群体级元分析：

```python
# V2 云端: 群体级 Meta-Analysis

class MetaAnalysisAggregator:
    """
    汇总 N 个用户的个体级 ATT, 输出保司精算级控费证明
    采用随机效应模型 (Random Effects Model) 容纳个体异质性
    """

    def aggregate(self, individual_results: list) -> dict:
        atts = np.array([r["att"] for r in individual_results])
        ses = np.array([r["se"] for r in individual_results])  # 标准误

        # 随机效应模型权重 (DerSimonian-Laird)
        w = 1.0 / (ses ** 2)
        att_fixed = np.sum(w * atts) / np.sum(w)
        Q = np.sum(w * (atts - att_fixed) ** 2)
        tau2 = max(0, (Q - (len(atts) - 1)) / (np.sum(w) - np.sum(w**2) / np.sum(w)))

        w_re = 1.0 / (ses ** 2 + tau2)
        att_random = np.sum(w_re * atts) / np.sum(w_re)
        se_random = 1.0 / np.sqrt(np.sum(w_re))

        return {
            "pooled_att": round(float(att_random), 4),
            "pooled_se": round(float(se_random), 4),
            "ci_lower": round(float(att_random - 1.96 * se_random), 4),
            "ci_upper": round(float(att_random + 1.96 * se_random), 4),
            "n_users": len(individual_results),
            "heterogeneity_tau2": round(float(tau2), 6),
            "is_significant": bool(att_random + 1.96 * se_random < 0)
        }
```

### 3.3 Claude Agent 矩阵集成（源自初代 PHM 文档第11节）

V2 将端侧 PHM 作为本地 Agent 节点接入 Claude 健康 Agent 矩阵：

```
PHM 作为 MCP Server 暴露工具:
  - get_health_summary()        → 脱敏健康摘要
  - get_risk_score()            → 当前风险评分
  - get_intervention_history()  → 干预执行历史

Claude 作为 MCP Client:
  - 在用户授权时调用 PHM 工具获取脱敏上下文
  - 输出深度分析或跨模型协作结果
  - 写回 PHM 记忆系统

应用场景:
  "Claude，帮我把上周血压偏高的情况整理成一封给医生的信"
  "Claude，对比我最近三个月的运动量和血压趋势，有什么建议"
```

### 3.4 防篡改升级：区块链锚定 + 零知识证明

```
V1: Ed25519 签名 → 云端签名, 保司公钥验证
V2: Ed25519 + 区块链锚定
    → 每份归因报告的哈希写入公链 (如以太坊/Polygon)
    → 即使云端服务器被攻破, 历史报告也不可篡改

V2+: 零知识证明 (ZKP)
    → 向保司证明"用户的风险评分确实下降了 X%"
    → 但不透露具体的风险分值是多少
    → 数学上可证明的隐私保护
```

---

## 4. 支付方对接演进（源自初代文档第12节）

### 4.1 商业健康险：核保减费 / 返保费

```
V1 阶段: 提供群体级控费报告 → 保司手动审核 → 按年度结算
V2 阶段: API 自动对接保司核保系统 → 实时返保费

数据流:
  用户授权 → 脱敏归因报告 → 保司 API → 自动触发保费减免
```

### 4.2 企业健康福利：员工健康 ROI

```
V1 阶段: 按企业出具员工群体健康改善报告
V2 阶段: 企业管理后台实时仪表盘

指标:
  - 员工平均风险评分变化趋势
  - 干预依从率分布
  - 预计医疗支出节省额
```

### 4.3 DRG/DIP 预防结算

```
V2 阶段: 对接医保支付改革

场景: 基层诊所使用 ReHealth Core 筛查 → BodyUP 管理 → 指标改善
证明: 完整的 "筛查→管理→归因→改善" 闭环数据
结算: 按效果付费, 类 DRG 分值结算
```

---

## 5. 技术演进里程碑

| 时间 | 里程碑 | 前置条件 |
|------|--------|----------|
| 2026 Q3 | V1 核心链路跑通, 种子用户上线 | 当前开发 |
| 2026 Q4 | 群体级 Meta-Analysis 原型 | 100+ 用户 × 30 天数据 |
| 2027 Q1 | 端侧大模型 LiteRT 编译调通 | 旗舰机测试机 |
| 2027 Q2 | 端侧 LoRA 增量学习 + EWC | 解决发热/OOM 问题 |
| 2027 Q3 | 联邦学习原型 | 500+ 用户 |
| 2027 Q4 | Claude Agent 矩阵集成 | MCP 协议适配 |
| 2028 Q1 | 区块链锚定 + ZKP 概念验证 | 密码学团队 |
| 2028 Q2 | 等保三级 + 二类器械证 | 合规团队 |

---

## 6. V1 → V2 的兼容性保证

```
核心原则: V2 的所有新能力都是 V1 之上的叠加, 不是替代

端侧大模型 (V2) 失败/不可用时:
  → 自动降级到 V1 的记忆文件 + 云端推理路径
  → 用户无感知

端侧 LoRA 训练 (V2) 导致模型退化时:
  → EWC 正则化 + 自动回滚到上一个快照
  → 极端情况: 删除 LoRA, 回退到纯 Base 模型

联邦学习 (V2) 聚合梯度异常时:
  → 各端侧保留本地 LoRA, 不合并异常梯度
  → 下一轮重新聚合

记忆文件 (V1) 始终是基础设施:
  → 无论端侧是否运行大模型, 记忆文件始终在更新
  → 它是所有上层能力的数据底座
```

---

## 附录: V2 涉及的初代专利权利要求映射

| 专利权利要求 | V1 状态 | V2 落地计划 |
|-------------|---------|------------|
| 要求1: 端侧基础模型 + 私有 LoRA | 未实现 (云端) | V2 有条件落地 |
| 要求2: LoRA 低秩适配 | 未实现 | V2 夜间做梦机制 |
| 要求3: LoRA 解耦可删除 | 部分 (记忆文件可删除) | V2 完整实现 |
| 要求4: LoRA 跨设备迁移 | 未实现 | V2 加密备份 |
| 要求5: 多种触发条件 | 已实现 (做梦机制) | 增加更多触发条件 |
| 要求6: 多源健康数据 | 已实现 | 扩展数据源 |
| 要求7: 差分隐私 | 未实现 | V2 联邦学习阶段 |
| 要求8: 多病种适配器 | 已实现 (换SIM卡) | 增加病种 |
| 要求9: 脱敏元数据上报 | 已实现 | 增强 |
| 要求10: 系统级架构 | 部分实现 | V2 完整实现 |
