<div align="center">

# 🧠 ReHealth AI HealthAgent

> RHI v2.0 研究规划与验证入口：`docs/RHI_V2_ALGORITHM_PLAN.md`。
> 当前生产仍使用 `cvd-16-v1`；RHI 确定性评分处于 research preview，
> 不得替换生产临床风险模型。
> 本仓库是 ReHealth AI / 睿禾健康主仓库（`E:\code\rehealth_tonbu`）内的算法研究子仓库：
> 训练、HealthAgent/PIAS 仿真与算法验证。生产 PIAS 归因服务、患者 APP、后端与模型服务
> 分别位于主仓库的 `Android-apk`、`backend` 与 `model-service`；本仓库不承载患者移动端入口。

### Multi-Agent Health Intervention Simulation Engine

> Repository role: model training, wearable feature research, HealthAgent/PIAS
> simulation, and algorithm validation. This is not the patient Android app;
> the production Android client lives in `Android-apk` and reaches algorithms
> only through `backend` and `model-service`.

Predict how humans respond to health interventions **before running real-world trials.**

Built for **AI researchers, digital health builders, and multi-agent experimentation.**

<p>
<img src="https://img.shields.io/github/stars/RehealthAI/rehealth-algorithms?style=social">
<img src="https://img.shields.io/github/forks/RehealthAI/rehealth-algorithms?style=social">
<img src="https://img.shields.io/github/issues/RehealthAI/rehealth-algorithms">
<img src="https://img.shields.io/github/license/RehealthAI/rehealth-algorithms">
</p>

<img src="https://img.shields.io/badge/python-3.10+-blue">
<img src="https://img.shields.io/badge/AI-Multi--Agent-green">
<img src="https://img.shields.io/badge/Digital%20Health-Simulation-orange">

</div>

---

# 🚀 What is HealthAgent?

**HealthAgent** is a **multi-agent simulation framework** that models how human **emotion, behavior, and physiology** interact during health interventions.

Instead of running expensive real-world trials, HealthAgent allows researchers and builders to:

✔ simulate long-term patient trajectories  
✔ test behavioral interventions  
✔ evaluate compliance strategies  
✔ study digital health systems  

All inside a **fast AI-powered simulation environment**.

Think of it as a **health behavior simulator for digital health research.**

---

# 🧠 Why This Project Exists

Testing health interventions in the real world is:

- expensive  
- slow  
- ethically constrained  

You cannot easily run **large randomized trials** just to test a new behavioral nudge.

HealthAgent allows you to simulate those trials **in seconds**.

The system models realistic human behavior patterns:

- compliance rates around **40–60%** for high-stress individuals  
- emotional fluctuation driven by sleep and fatigue  
- physiological response to behavioral adherence  

---

# 📊 Simulation Demo

Example output from a **90-day simulation**.

![simulation demo](docs/demo.png)



---

# 🧠 Multi-Agent Architecture

HealthAgent models health behavior using collaborating agents.

    Orchestrator Agent
        ├ Emotion Agent
        │   ├ stress
        │   ├ motivation
        │   └ fatigue
        │
        ├ Compliance Agent
        │   ├ decision probability
        │   └ adherence score
        │
        ├ Physiology Agent
        │   ├ HRV
        │   ├ resting HR
        │   └ sleep dynamics
        │
        └ Intervention Agent
            ├ risk detection
            └ behavioral nudges

Simulation loop:

    Emotion → Compliance → Physiology → Intervention → Record

---

# ⚡ Quick Start

Clone repository

    git clone <本仓库随主仓库一并检出>
    cd rehealth-algorithms

Create environment

    python -m venv venv
    source venv/bin/activate

Install dependencies

    pip install -r requirements.txt

Add API key

    DEEPSEEK_API_KEY=your_key_here

Run simulation

    python simulate.py

（旧版示例 `visualize.py` 已不在当前仓库；可视化与 PIAS 服务入口见 `api/` 与 `rhi/`。）

---

# 🐍 Python Example

    from healthagent import SimulationEngine
    from healthagent.models import PatientProfile, InterventionPlan

    patient = PatientProfile(
        age=52,
        bmi=27.5,
        stress_level="HIGH",
        sleep_avg_hours=6.2
    )

    plan = InterventionPlan(
        name="Lifestyle Optimization",
        duration_days=90,
        rules=[
            "30-minute walk daily",
            "sleep before 10:30 PM",
            "10 min mindfulness"
        ]
    )

    engine = SimulationEngine(seed=42)

    result = engine.run_simulation(patient, plan)

    print(result.compliance_rate)
    print(result.sleep_improvement)
    print(result.hrv_change)

---

# 📦 Project Structure

    rehealth-algorithms
    │
    ├ healthagent           多智能体健康干预仿真引擎（agents/models/engine）
    ├ api                   PIAS 归因 FastAPI 服务（routers）
    ├ rhi                   RHI v2 研究实现与验证
    ├ train                 模型训练（蒸馏/迁移学习）与下载脚本
    ├ docker                Docker 构建与运行配置
    ├ examples              示例与演示
    ├ frontend              配套前端
    ├ bodyup_cloud          BodyUP 云端侧
    ├ bodyup_edge_sdk       BodyUP 端侧 SDK
    ├ config / data / tests 配置、数据与测试
    ├ docs                  算法规划、PIAS 集成、项目总结与对话记录
    ├ simulate.py           仿真入口
    ├ factor16_rules_v1.0.yaml  Factor16 透明贡献规则
    ├ pyproject.toml / requirements.txt
    └ LICENSE

---

# 🧪 Use Cases

Digital Health Research  
Study behavioral drivers behind intervention adherence.

Healthcare Analytics  
Estimate success probability of interventions.

Digital Therapeutics  
Prototype behavioral health treatments.

AI Agent Research  
Experiment with LLM-driven agent simulations.

---

# 🌍 Vision

HealthAgent is part of the **ReHealth AI platform**.

Our long-term vision is to build a **digital twin simulation environment for human health**.

---

# 🤝 Contributing

Fork repository

    git checkout -b feature/my-feature

Commit changes

    git commit -m "Add feature"

Push branch

    git push origin feature/my-feature

Open Pull Request.

---

# 📄 License

Apache 2.0 License

---

# ⭐ Support the Project

If this project is useful:

Star the repo  
Fork it  
Report issues  
Share it

---

<div align="center">

Built with ❤️ by **ReHealth AI · 2026**

</div>
