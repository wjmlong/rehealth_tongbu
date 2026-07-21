#!/usr/bin/env python3
"""Generate an interactive H5 attribution report from the LIVE WSL2 PIAS server.

Runs inside WSL2 (uses the pias-venv). Calls the real FastAPI model-service at
http://localhost:8000/api/pias/v2/attribute/individual, then writes a
self-contained, interactive H5 (ECharts charts + layered reports + print-to-PDF)
to /mnt/d/rehealthAI/outputs/pias_attribution_report.html.

This proves the full loop: app random input -> WSL2 PIAS -> interactive H5.

H5 now includes a SECOND-LEVEL drill-down screen (查看归因逐日明细): a button on
the main page opens a full-screen overlay with 逐日数据 / 方法说明 / 原始输入 tabs.
"""
import json
import os
import urllib.request
import datetime
import random

OUT = "/mnt/d/rehealthAI/outputs/pias_attribution_report.html"
URL = "http://localhost:8000/api/pias/v2/attribute/individual"

base = datetime.date(2026, 5, 1)
random.seed(7)
hist = []
for i in range(30):
    d = (base + datetime.timedelta(days=i)).isoformat()
    y = round(max(0.1, 0.5 - 0.003 * i - 0.04 * (1 if i % 3 == 0 else 0)), 4)
    z = 1 if i % 3 == 0 else 0
    hist.append({"date": d, "Y": y, "Z": z})
payload = json.dumps({"risk_history": hist, "forecast_days": 30, "language": "zh"}).encode("utf-8")

req = urllib.request.Request(URL, data=payload, headers={"Content-Type": "application/json"})
with urllib.request.urlopen(req, timeout=60) as r:
    env = json.loads(r.read().decode("utf-8"))

assert env.get("success"), f"server returned error: {env}"
res = env["result"]
print("server report_id:", res.get("report_id"), "status:", res.get("status"))

cs = res.get("current_state", {})
fc = res.get("forecast", {})
raw = fc.get("raw", {})
summary = fc.get("summary", {})
ie = res.get("intervention_effect", {})
reports = res.get("reports", {})

dates = raw.get("dates") or []
no_action = raw.get("no_action") or []
with_plan = raw.get("with_plan") or []
ci_upper = raw.get("ci_upper") or []
ci_lower = raw.get("ci_lower") or []


def jstr(x):
    return json.dumps(x, ensure_ascii=False)


# ---- Build second-level drill-down: combined daily table (history + forecast) ----
def cell(x):
    if x is None or x == "":
        return "-"
    if isinstance(x, float):
        return f"{x:.4f}"
    return str(x)


hist_rows = ""
for h in hist:
    hist_rows += (
        f"<tr><td>{h['date']}</td><td class='t-hist'>历史</td>"
        f"<td>{h['Y']:.4f}</td><td>{'是' if h['Z'] else '否'}</td>"
        f"<td>-</td><td>-</td><td>-</td><td>-</td></tr>"
    )

n = min(len(dates), len(no_action), len(with_plan), len(ci_lower), len(ci_upper))
fc_rows = ""
for i in range(n):
    fc_rows += (
        f"<tr><td>{dates[i]}</td><td class='t-fc'>预测</td>"
        f"<td>-</td><td>-</td>"
        f"<td>{no_action[i]:.4f}</td><td>{with_plan[i]:.4f}</td>"
        f"<td>{ci_lower[i]:.4f}</td><td>{ci_upper[i]:.4f}</td></tr>"
    )

raw_input_json = json.dumps(hist, ensure_ascii=False, indent=2)

method_text = """
<b>个体归因 (Individual Attribution)</b> 用于回答：<i>“在已经发生的这些天里，坚持干预计划到底让我的风险降了多少？”</i><br/><br/>
核心指标是 <b>ATT（Average Treatment effect on the Treated，处理了个体的平均处理效应）</b>：
把“干预日”与“对照日（未干预日）”按周配对，比较两者风险分的差异，再做 Wilcoxon 符号秩检验
与 Bootstrap 重采样得到 95% 置信区间与 p 值。<br/><br/>
<b>怎么读这张表</b>：历史段是你真实上报的每日风险分 Y 与是否干预日 Z；预测段是模型基于当前趋势外推
的 30 天轨迹——红线(不干预)持续走高，绿线(干预计划)走低，两条线之间的阴影即置信区间。
<b>ATT 为负且 p&lt;0.05</b> 说明干预计划确实有效降低了风险；否则属于统计上不显著，需要更长的数据积累。
""".strip()

# layered report tabs (unchanged)
def report_block(layer):
    b = reports.get(layer, {})
    return f"""
      <div class="report-tab" id="rep-{layer}">
        <h3>{b.get('headline','')}</h3>
        <p>{b.get('body','')}</p>
        <div class="advice">{b.get('advice','')}</div>
      </div>"""


layers = [("user", "用户"), ("manager", "运营"), ("actuary", "精算"), ("regulator", "监管")]
report_tabs = "".join(
    f'<button class="tab-btn {"active" if l==layers[0][0] else ""}" onclick="showRep(\'{l}\')">{n}</button>'
    for l, n in layers
)
report_blocks = "".join(report_block(l) for l, _ in layers)

html = f"""<!DOCTYPE html>
<html lang="zh-CN">
<head>
<meta charset="utf-8"/>
<meta name="viewport" content="width=device-width, initial-scale=1"/>
<title>PIAS 个人归因报告 (WSL2 实时)</title>
<script src="https://cdn.jsdelivr.net/npm/echarts@5/dist/echarts.min.js"></script>
<style>
  :root{{--bg:#0f1420;--card:#1a2233;--accent:#4f8cff;--good:#37d39b;--warn:#ffb454;--txt:#e8edf5;--muted:#9fb0c8}}
  *{{box-sizing:border-box}}
  body{{margin:0;background:var(--bg);color:var(--txt);font-family:-apple-system,'Segoe UI',Roboto,'PingFang SC','Microsoft YaHei',sans-serif;line-height:1.6}}
  .wrap{{max-width:920px;margin:0 auto;padding:20px}}
  header{{display:flex;justify-content:space-between;align-items:center;flex-wrap:wrap;gap:12px}}
  h1{{font-size:22px;margin:0}}
  .meta{{color:var(--muted);font-size:13px}}
  .cards{{display:grid;grid-template-columns:repeat(auto-fit,minmax(140px,1fr));gap:12px;margin:18px 0}}
  .card{{background:var(--card);border-radius:12px;padding:14px}}
  .card .k{{color:var(--muted);font-size:12px}}
  .card .v{{font-size:22px;font-weight:700;margin-top:4px}}
  .card .v.good{{color:var(--good)}} .card .v.warn{{color:var(--warn)}}
  .panel{{background:var(--card);border-radius:12px;padding:16px;margin:14px 0}}
  .panel h2{{font-size:16px;margin:0 0 10px;display:flex;justify-content:space-between;align-items:center;flex-wrap:wrap;gap:8px}}
  #chart{{width:100%;height:340px}}
  .drill{{background:var(--accent);color:#fff;border:none;border-radius:20px;padding:6px 14px;font-size:13px;cursor:pointer}}
  .att-link{{color:var(--accent);font-size:13px;cursor:pointer;text-decoration:underline}}
  .tabs{{display:flex;gap:8px;margin:6px 0 12px;flex-wrap:wrap}}
  .tab-btn{{background:#26304a;color:var(--txt);border:none;border-radius:8px;padding:8px 14px;cursor:pointer;font-size:14px}}
  .tab-btn.active{{background:var(--accent);color:#fff}}
  .report-tab{{display:none}} .report-tab.active{{display:block}}
  .report-tab h3{{margin:0 0 8px;color:var(--accent)}}
  .advice{{margin-top:10px;padding:10px 12px;background:#13351f;color:var(--good);border-radius:8px}}
  .btn{{position:fixed;right:18px;bottom:18px;background:var(--accent);color:#fff;border:none;border-radius:24px;padding:12px 20px;font-size:15px;cursor:pointer;box-shadow:0 4px 14px rgba(0,0,0,.4)}}
  .badge{{display:inline-block;padding:2px 10px;border-radius:20px;font-size:12px;background:#26304a}}
  .sig{{color:var(--good)}} .nsig{{color:var(--warn)}}
  /* ---- second-level overlay ---- */
  .overlay{{position:fixed;inset:0;background:rgba(10,14,22,.97);z-index:50;display:flex;flex-direction:column;padding:0}}
  .overlay.hidden{{display:none}}
  .ov-head{{display:flex;justify-content:space-between;align-items:center;padding:16px 20px;border-bottom:1px solid #26304a}}
  .ov-head h2{{margin:0;font-size:18px}}
  .ov-close{{background:#26304a;color:var(--txt);border:none;border-radius:8px;padding:8px 14px;font-size:14px;cursor:pointer}}
  .ov-body{{padding:16px 20px;overflow:auto;flex:1}}
  .ov-tabs{{display:flex;gap:8px;margin-bottom:14px;flex-wrap:wrap}}
  .ov-tab{{background:#26304a;color:var(--txt);border:none;border-radius:8px;padding:8px 14px;cursor:pointer;font-size:14px}}
  .ov-tab.active{{background:var(--accent);color:#fff}}
  .ov-pane{{display:none}} .ov-pane.active{{display:block}}
  table.dt{{width:100%;border-collapse:collapse;font-size:13px}}
  table.dt th,table.dt td{{border-bottom:1px solid #26304a;padding:7px 8px;text-align:center}}
  table.dt th{{color:var(--muted);position:sticky;top:0;background:var(--bg)}}
  .t-hist{{color:var(--warn)}} .t-fc{{color:var(--good)}}
  pre.raw{{background:#11161f;border:1px solid #26304a;border-radius:10px;padding:14px;overflow:auto;font-size:12px;color:#bfe9d2}}
  .method{{font-size:14px;color:var(--txt)}}
</style>
</head>
<body>
<div class="wrap">
  <header>
    <div><h1>PIAS 个人归因报告</h1><div class="meta">report_id: {res.get('report_id','-')} · status: <span class="badge">{res.get('status','-')}</span> · 数据源: WSL2 model-service :8000 (实时)</div></div>
  </header>

  <div class="cards">
    <div class="card"><div class="k">当前风险</div><div class="v warn">{cs.get('risk_score','-')}</div></div>
    <div class="card"><div class="k">风险等级</div><div class="v">{cs.get('risk_level','-')}</div></div>
    <div class="card"><div class="k">趋势</div><div class="v good">{cs.get('trend','-')}</div></div>
    <div class="card"><div class="k">30天(不干预)</div><div class="v warn">{summary.get('30d_no_action','-')}</div></div>
    <div class="card"><div class="k">30天(干预)</div><div class="v good">{summary.get('30d_with_plan','-')}</div></div>
    <div class="card"><div class="k">风险下降</div><div class="v good">{summary.get('risk_reduction','-')}</div></div>
  </div>

  <div class="panel">
    <h2>风险轨迹预测：不干预 vs 干预计划 <button class="drill" onclick="openDetail()">查看归因逐日明细 →</button></h2>
    <div id="chart"></div>
  </div>

  <div class="panel">
    <h2>干预因果效应 (配对 ATT) <span class="att-link" onclick="openDetail()">逐日明细 →</span></h2>
    <div class="cards">
      <div class="card"><div class="k">个体 ATT</div><div class="v {'good' if ie.get('individual_att',0)<0 else 'warn'}">{ie.get('individual_att','-')}</div></div>
      <div class="card"><div class="k">ATT 95% CI</div><div class="v">[{ie.get('att_ci_lower','-')}, {ie.get('att_ci_upper','-')}]</div></div>
      <div class="card"><div class="k">p 值</div><div class="v">{ie.get('att_p_value','-')}</div></div>
      <div class="card"><div class="k">是否显著</div><div class="v {'sig' if ie.get('att_significant') else 'nsig'}">{'显著' if ie.get('att_significant') else '不显著'}</div></div>
    </div>
  </div>

  <div class="panel">
    <h2>分层报告</h2>
    <div class="tabs">{report_tabs}</div>
    {report_blocks}
  </div>
</div>
<button class="btn" onclick="window.print()">导出 / 打印 PDF</button>

<!-- ============ SECOND-LEVEL DRILL-DOWN OVERLAY ============ -->
<div id="detail" class="overlay hidden">
  <div class="ov-head">
    <h2>归因逐日明细</h2>
    <button class="ov-close" onclick="closeDetail()">← 返回报告</button>
  </div>
  <div class="ov-body">
    <div class="ov-tabs">
      <button class="ov-tab active" onclick="showOv('data')">逐日数据</button>
      <button class="ov-tab" onclick="showOv('method')">方法说明</button>
      <button class="ov-tab" onclick="showOv('raw')">原始输入</button>
    </div>
    <div id="ov-data" class="ov-pane active">
      <table class="dt">
        <thead><tr><th>日期</th><th>类型</th><th>风险分 Y</th><th>干预日 Z</th><th>不干预预测</th><th>干预预测</th><th>CI 下</th><th>CI 上</th></tr></thead>
        <tbody>{hist_rows}{fc_rows}</tbody>
      </table>
    </div>
    <div id="ov-method" class="ov-pane method">{method_text}</div>
    <div id="ov-raw" class="ov-pane"><pre class="raw">{raw_input_json}</pre></div>
  </div>
</div>

<script>
var chart = echarts.init(document.getElementById('chart'), 'dark');
var option = {{
  backgroundColor:'transparent',
  tooltip:{{trigger:'axis'}},
  legend:{{data:['不干预','干预计划']}},
  grid:{{left:48,right:18,top:36,bottom:36}},
  xAxis:{{type:'category',data:{jstr(dates)}}},
  yAxis:{{type:'value',name:'风险分',min:0,max:1}},
  series:[
    {{name:'不干预',type:'line',smooth:true,data:{jstr(no_action)},lineStyle:{{color:'#ff6b6b'}},itemStyle:{{color:'#ff6b6b'}}}},
    {{name:'干预计划',type:'line',smooth:true,data:{jstr(with_plan)},lineStyle:{{color:'#37d39b'}},itemStyle:{{color:'#37d39b'}},areaStyle:{{color:'rgba(55,211,155,0.12)'}}}},
    {{name:'CI上界',type:'line',data:{jstr(ci_upper)},lineStyle:{{opacity:0.25,color:'#4f8cff'}},itemStyle:{{opacity:0}},symbol:'none',tooltip:{{show:false}}}},
    {{name:'CI下界',type:'line',data:{jstr(ci_lower)},lineStyle:{{opacity:0.25,color:'#4f8cff'}},itemStyle:{{opacity:0}},symbol:'none',tooltip:{{show:false}},areaStyle:{{color:'rgba(79,140,255,0.08)'}}}}
  ]
}};
chart.setOption(option);
window.addEventListener('resize', function(){{chart.resize();}});
function showRep(layer){{
  document.querySelectorAll('.report-tab').forEach(function(e){{e.classList.remove('active');}});
  document.getElementById('rep-'+layer).classList.add('active');
  document.querySelectorAll('.tab-btn').forEach(function(b){{b.classList.remove('active');}});
  event.target.classList.add('active');
}}
document.getElementById('rep-user').classList.add('active');
// ---- drill-down overlay ----
function openDetail(){{ document.getElementById('detail').classList.remove('hidden'); }}
function closeDetail(){{ document.getElementById('detail').classList.add('hidden'); }}
function showOv(pane){{
  document.querySelectorAll('.ov-pane').forEach(function(e){{e.classList.remove('active');}});
  document.getElementById('ov-'+pane).classList.add('active');
  document.querySelectorAll('.ov-tab').forEach(function(b){{b.classList.remove('active');}});
  event.target.classList.add('active');
}}
</script>
</body>
</html>"""

os.makedirs(os.path.dirname(OUT), exist_ok=True)
with open(OUT, "w", encoding="utf-8") as f:
    f.write(html)
print("H5 written ->", OUT, "bytes=", len(html))
