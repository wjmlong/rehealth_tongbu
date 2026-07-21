# Claude Working Memory — 睿禾健康 V1 架构开发
> 导出日期: 2026-05-25
> 会话: CTO (吴嘉铭) × Claude (Opus 4.6)

---

## 项目概览

**睿禾健康 (ReHealth AI / BodyUP)** — 端云协同健康干预 App，通过智能手表+体检数据评估心血管风险，提供个性化干预，向保险公司证明控费效果并结算。

**商业闭环**: PIAS = Predict → Intervene → Attribute → Settle

**资金状态**: 种子轮 400 万人民币已到位，6-8 个月内需跑出真实数据。

---

## 三版文档体系

| 文档 | 性质 | 面向 |
|------|------|------|
| V0 初代 PHM 技术文档 (2026-04) | 专利技术愿景，全端侧方案 | 投资人、专利局 |
| **V1 工程实施版 (2026-05)** | **当前在写代码的方案**，端云混合 | 开发团队 |
| V2 完整愿景版 (Future Work) | 叠加端侧 LoRA + 联邦学习 | 技术路线图 |

---

## V1 架构关键决策

### [P] Predict — CatBoost 风险评分
- **模型**: `rehealth_v2_final.pkl` (CatBoost, AUC=0.847)
- **数据源**: NHANES 2015-2016
- **16 字段**: age, gender, bmi, sbp, dbp, fasting_glucose, total_cholesterol, ldl, hdl, triglycerides, exercise_days, smoking, drinking, diabetes_history, hypertension_history, family_history
- **关键发现**: exercise_days 是最大可干预因子 (重要性 #2)，sbp/dbp 排 #3#4
- **日常更新**: 手表每日更新 sbp/dbp/exercise_days → 模型重新评分 → 形成风险分时序
- **可扩展**: ModelRegistry 支持后续挂载新模型/数据源

### [I] Intervene — LLM API 干预处方
- **当前**: Claude/GPT/OpenRouter API (零运维，按量付费)
- **架构**: LLMProvider 抽象层，支持多提供商切换
- **后期**: 用户规模破万 + 有自有交互数据后，迁移自建 Gemma+vLLM
- **为什么不自建**: 种子轮阶段 100 用户 API 费用 ~$9/月 vs GPU 服务器 ~$1200/月

### [A] Attribute — 双轨归因
- **Level 1 个人预测** (面向用户): 单用户风险分时序 → 加权线性回归 → "维持现状 vs 执行计划" 两条走势线。App 产品卖点。
- **Level 2 群体归因** (面向保司): 100+ 用户按基线特征 PSM 匹配 → DRE 双重稳健估计 → Bootstrap 95% CI → 保司精算结算。
- **对照组来源**: 新注册未开始干预的用户 + 低活跃用户 + 等待期用户

### [S] Settle — Ed25519 签名
- 非对称密钥，保司公钥验签，不可伪造不可抵赖

### 端侧
- 记忆文件 `user_health_memory.json` (~5KB, AES-256) 替代端侧 LoRA
- 夜间做梦机制: 凌晨 2-5 点纯 CPU 统计聚合 (10-30 秒)
- 16 维特征提取 → 脱敏上传 (不可逆)

---

## 已完成的工作

### 文档
- [x] V1 工程实施版开发框架说明书 (完整版，含所有代码)
- [x] V2 完整愿景版 Future Work 技术演进路线
- [x] CTO 技术决策对话记录 (含老板话术)
- [x] XGBoost → CatBoost 全文更新
- [x] Gemma+vLLM → Claude/GPT API 全文更新
- [x] 单人归因 → 群体归因为主 + 个人预测为辅

### 模型
- [x] CatBoost V2.0 训练完成 (AUC=0.847)
- [x] 训练脚本: `train_v2_final.py`
- [x] 部署文件: `rehealth_v2_final.pkl`, `feature_cols_v2.pkl`, `cat_cols_v2.pkl`

---

## 关键文件位置

```
E:\DISK_D\Project\Rehealth_AI\Project\
├── docs/
│   ├── V1_工程实施版_端云协同系统开发框架说明书.md  ← 主文档
│   ├── V2_完整愿景版_FutureWork技术演进路线.md
│   └── CTO技术决策对话记录_20260525.md
│
E:\DISK_D\Project\Rehealth_AI\paper work\
├── train_v2_final.py                               ← CatBoost 训练脚本
├── rehealth_v2_final.pkl                            ← 部署模型
├── feature_importance_v2.png                        ← 特征重要性图
├── roc_curve_v2.png                                 ← ROC 曲线
├── BodyUP_PHM_技术文档_V1.0.docx                    ← V0 初代文档
├── 一种端侧私有健康模型的持续个性化学习方法.docx      ← 专利
└── 睿禾健康融资问答.docx                             ← 投资人 Q&A
```

---

## 下一步开发任务

### Phase 1: 核心引擎 (M1-M3, 2026年6-8月)

| 优先级 | 任务 | 状态 |
|--------|------|------|
| P0 | CatBoost 模型部署为 FastAPI 服务 + SHAP | 模型已就绪，需部署 |
| P0 | LLM API 接入 + Prompt 工程 | 需写 LLMProvider |
| P0 | 端侧特征提取 + 记忆文件 | 需实现 |
| P1 | 做梦机制 + 脱敏上传 | 需实现 |
| P1 | Level 1 个人预测接口 | 需实现 |
| P2 | Ed25519 签名 | 需实现 |

### Phase 2: 产品闭环 (M4-M6)
- 种子用户 100 人 + Level 2 群体归因 + 保司对接

---

## 老板 FAQ 速查

1. **"为什么不按初代文档做？"** → 先活下来再变强，V1 低风险快速跑通，V2 兑现专利
2. **"用户体验有区别吗？"** → 完全没有，底层实现不同而已
3. **"专利白写了？"** → 没有，V2 完整兑现，V1 阶段也受权利要求9保护
4. **"保司什么时候掏钱？"** → Phase 2 (M6+) 群体归因出报告后，预计 M8-M10
5. **"为什么不自建大模型？"** → API 成本 $9/月 vs 自建 $1200/月，等数据积累后再迁移
