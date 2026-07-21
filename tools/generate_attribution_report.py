"""
读取 PIAS 真实输出 (outputs/pias_attribution_result.json)，
生成：
  outputs/pias_attribution_report.html  —— 可交互页面（ECharts，风格对齐 App 归因界面）
  outputs/pias_attribution_report.pdf   —— 同一份报告的离线 PDF（reportlab）

HTML 自带"打印 / 导出 PDF"按钮，浏览器即可出 PDF，无需服务端。
"""

import os
import json

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, "outputs")
JSON_PATH = os.path.join(OUT, "pias_attribution_result.json")
HTML_PATH = os.path.join(OUT, "pias_attribution_report.html")
PDF_PATH = os.path.join(OUT, "pias_attribution_report.pdf")

# App 主题色
CANVAS = "#F5F7FA"
INK = "#1F2933"
MUTED = "#7B8794"
LINE = "#E4E7EB"
MINT = "#51CF66"
RED = "#FF6B6B"
AMBER = "#FFA94D"


def esc(s):
    return (str(s).replace("&", "&amp;").replace("<", "&lt;")
            .replace(">", "&gt;").replace('"', "&quot;"))


def risk_level_zh(level):
    return {"low": "低风险", "moderate": "中等风险", "high": "高风险",
            "very_high": "极高风险", "unknown": "未知"}.get(level, level)


def build_html(scenarios):
    # 主场景：优先 demo_plan_effective，否则第一个 ready
    primary = next((s for s in scenarios if s["scenario"] == "demo_plan_effective"), None)
    if primary is None:
        primary = next((s for s in scenarios if s["status"] == "ready"), scenarios[0])
    others = [s for s in scenarios if s is not primary]

    # 主场景图表数据
    fc = primary.get("forecast_status_quo", []) or []
    fp = primary.get("forecast_with_plan", []) or []
    cu = primary.get("forecast_ci_upper", []) or []
    cl = primary.get("forecast_ci_lower", []) or []
    n = len(fc)
    days = [f"第{i+1}天" for i in range(n)]

    att = primary.get("individual_att")
    att_html = ""
    if att is not None:
        att_html = f"""
        <div class="stat">
          <div class="stat-val">{att:+.1%}</div>
          <div class="stat-label">个体 ATT（干预日 vs 非干预日）</div>
          <div class="stat-sub">95% CI [{primary.get('att_ci_lower'):+.1%}, {primary.get('att_ci_upper'):+.1%}] · p={primary.get('att_p_value'):.3f} · {'显著' if primary.get('att_significant') else '不显著'}</div>
        </div>"""
    else:
        att_html = f"""
        <div class="stat">
          <div class="stat-val" style="color:{MUTED}">未计算</div>
          <div class="stat-label">个体 ATT</div>
          <div class="stat-sub">干预日与对照日均需 ≥7 天</div>
        </div>"""

    rt = primary.get("report_text", {}) or {}
    metrics = rt.get("metrics", {}) or {}

    # 输入信息表（用户随机填入的内容）
    rows = ""
    hist_rows = primary.get("input_history")
    if hist_rows:
        for r in hist_rows[:12]:
            rows += f"<tr><td>{esc(r['date'])}</td><td>{r['Y']:.3f}</td><td>{'干预' if r['Z']==1 else '对照'}</td></tr>"
        if len(hist_rows) > 12:
            rows += f"<tr><td colspan=3 style='text-align:center;color:{MUTED}'>… 共 {len(hist_rows)} 天</td></tr>"

    # 其他场景摘要卡片
    other_cards = ""
    for s in others:
        other_cards += f"""
        <div class="card" style="margin-bottom:12px">
          <div style="display:flex;justify-content:space-between;align-items:center">
            <b style="color:{INK}">{esc(s['scenario'])}</b>
            <span class="pill" style="background:{MINT if s['status']=='ready' else AMBER}22;color:{'#2b8a3e' if s['status']=='ready' else '#e8590c'}">{esc(s['status'])}</span>
          </div>
          <div style="color:{MUTED};font-size:13px;margin-top:6px">
            当前风险 {s['current_risk_score']:.1%}（{risk_level_zh(s['risk_level'])}）·
            趋势 {esc(s['trend_direction'])} ·
            30天降低 {s['risk_reduction_30d']:+.1%}
          </div>
        </div>"""

    html = f"""<!DOCTYPE html>
<html lang="zh-CN">
<head>
<meta charset="utf-8"/>
<meta name="viewport" content="width=device-width, initial-scale=1"/>
<title>PIAS 归因报告 · {esc(primary['scenario'])}</title>
<script src="https://cdn.jsdelivr.net/npm/echarts@5.5.0/dist/echarts.min.js"></script>
<style>
  * {{ box-sizing: border-box; }}
  body {{ margin:0; background:{CANVAS}; color:{INK};
         font-family:-apple-system,"PingFang SC","Microsoft YaHei",Segoe UI,sans-serif; }}
  .wrap {{ max-width:860px; margin:0 auto; padding:20px 16px 60px; }}
  .topbar {{ display:flex; align-items:center; justify-content:space-between; gap:12px; }}
  .title {{ font-size:24px; font-weight:800; }}
  .subtitle {{ color:{MUTED}; font-size:12px; margin-top:2px; }}
  .btn {{ background:{MINT}; color:#fff; border:none; border-radius:10px;
          padding:10px 16px; font-size:14px; font-weight:600; cursor:pointer; }}
  .btn:active {{ transform:scale(.97); }}
  .card {{ background:#fff; border:1px solid {LINE}; border-radius:16px;
           padding:18px; margin-top:16px; box-shadow:0 1px 2px rgba(0,0,0,.04); }}
  .card h3 {{ margin:0 0 12px; font-size:17px; }}
  .stats {{ display:grid; grid-template-columns:repeat(auto-fit,minmax(150px,1fr)); gap:12px; }}
  .stat {{ background:{CANVAS}; border-radius:12px; padding:14px; }}
  .stat-val {{ font-size:22px; font-weight:800; color:{MINT}; }}
  .stat-label {{ font-size:12px; color:{INK}; margin-top:4px; }}
  .stat-sub {{ font-size:11px; color:{MUTED}; margin-top:4px; }}
  .pill {{ font-size:11px; padding:3px 10px; border-radius:999px; font-weight:600; }}
  .headline {{ font-size:18px; font-weight:800; }}
  .body {{ color:{INK}; font-size:14px; line-height:1.7; white-space:pre-wrap; }}
  .advice {{ background:{MINT}11; border-left:4px solid {MINT}; padding:12px 14px;
             border-radius:0 10px 10px 0; font-size:14px; line-height:1.7; }}
  .kv {{ display:flex; justify-content:space-between; padding:8px 0;
         border-bottom:1px solid {LINE}; font-size:13px; }}
  .kv:last-child {{ border-bottom:none; }}
  .kv .k {{ color:{MUTED}; }}
  .kv .v {{ font-weight:700; }}
  table {{ width:100%; border-collapse:collapse; font-size:13px; }}
  th,td {{ text-align:left; padding:8px 10px; border-bottom:1px solid {LINE}; }}
  th {{ color:{MUTED}; font-weight:600; }}
  @media print {{ .btn {{ display:none; }} body {{ background:#fff; }} }}
</style>
</head>
<body>
<div class="wrap">
  <div class="topbar">
    <div>
      <div class="title">心血管风险归因报告</div>
      <div class="subtitle">PIAS 个人归因（Level 1）· 真实算法输出 · 场景：{esc(primary['scenario'])}</div>
    </div>
    <button class="btn" onclick="window.print()">打印 / 导出 PDF</button>
  </div>

  <div class="card">
    <div class="stats">
      <div class="stat">
        <div class="stat-val">{primary['current_risk_score']:.1%}</div>
        <div class="stat-label">当前风险评分</div>
        <div class="stat-sub">{risk_level_zh(primary['risk_level'])} · 趋势 {esc(primary['trend_direction'])}</div>
      </div>
      <div class="stat">
        <div class="stat-val">{primary['risk_reduction_30d']:+.1%}</div>
        <div class="stat-label">30天风险变化（干预 vs 不干预）</div>
        <div class="stat-sub">有干预 {primary['projected_risk_30d_with_plan']:.1%} / 无干预 {primary['projected_risk_30d_no_action']:.1%}</div>
      </div>
      {att_html}
      <div class="stat">
        <div class="stat-val">{primary['n_days']}</div>
        <div class="stat-label">历史天数</div>
        <div class="stat-sub">干预 {primary.get('n_intervention_days','-')} / 对照 {primary.get('n_control_days','-')}</div>
      </div>
    </div>
  </div>

  <div class="card">
    <h3>风险轨迹预测（30天）</h3>
    <div id="trend" style="width:100%;height:320px"></div>
    <div style="color:{MUTED};font-size:11px;margin-top:6px">红线=不干预预测，绿线=坚持干预计划预测，灰色带=95% 置信区间</div>
  </div>

  <div class="card">
    <div class="headline">{esc(rt.get('headline',''))}</div>
    <div class="body" style="margin-top:10px">{esc(rt.get('body',''))}</div>
    {('<div style="margin-top:12px;color:{MINT};font-weight:700">📌 ' + esc(rt.get('att_summary','')) + '</div>') if rt.get('att_summary') else ''}
    <div class="advice" style="margin-top:14px"><b>建议：</b>{esc(rt.get('advice',''))}</div>
  </div>

  <div class="card">
    <h3>关键指标</h3>
    {''.join(f'<div class="kv"><span class="k">{esc(k)}</span><span class="v">{esc(v)}</span></div>' for k,v in metrics.items())}
  </div>

  {('<div class="card"><h3>你输入的随机信息（前12天）</h3><table><tr><th>日期</th><th>风险分 Y</th><th>类型 Z</th></tr>'+rows+'</table></div>') if rows else ''}

  {('<div class="card"><h3>其他验证场景</h3>'+other_cards+'</div>') if other_cards else ''}

  <div style="color:{MUTED};font-size:11px;text-align:center;margin-top:24px">
    本报告由 rehealth-algorithms / healthagent.pias IndividualAttributor 真实计算生成 · 仅供健康参考，不替代医生诊断
  </div>
</div>

<script>
var trend = echarts.init(document.getElementById('trend'));
var option = {{
  tooltip: {{ trigger:'axis' }},
  legend: {{ data:['不干预','干预后','置信区间上','置信区间下'], top:0 }},
  grid: {{ left:48, right:20, top:40, bottom:30 }},
  xAxis: {{ type:'category', data:{days}, axisLabel:{{interval:4}} }},
  yAxis: {{ type:'value', min:0, max:1, name:'风险' }},
  series: [
    {{ name:'置信区间下', type:'line', data:{cl}, lineStyle:{{opacity:0}}, stack:'ci', symbol:'none' }},
    {{ name:'置信区间上', type:'line', data:{cu}, lineStyle:{{opacity:0}}, stack:'ci',
       areaStyle:{{color:'rgba(123,135,148,0.18)'}}, symbol:'none' }},
    {{ name:'不干预', type:'line', data:{fc}, smooth:true, symbol:'none',
       lineStyle:{{color:'{RED}', width:2}} }},
    {{ name:'干预后', type:'line', data:{fp}, smooth:true, symbol:'none',
       lineStyle:{{color:'{MINT}', width:2}} }}
  ]
}};
trend.setOption(option);
window.addEventListener('resize', function(){{ trend.resize(); }});
</script>
</body>
</html>"""
    return html, primary


def build_pdf(scenarios, primary):
    try:
        from reportlab.lib.pagesizes import A4
        from reportlab.lib.units import mm
        from reportlab.lib import colors
        from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
        from reportlab.platypus import (SimpleDocTemplate, Paragraph, Spacer,
                                        Table, TableStyle)
        from reportlab.lib.enums import TA_LEFT
    except Exception as e:
        print(f"[PDF] reportlab 不可用，跳过 PDF：{e}")
        return False

    styles = getSampleStyleSheet()
    h = ParagraphStyle("h", parent=styles["Title"], fontSize=20, spaceAfter=4)
    sub = ParagraphStyle("sub", parent=styles["Normal"], fontSize=9,
                         textColor=colors.HexColor(MUTED), spaceAfter=12)
    body = ParagraphStyle("b", parent=styles["Normal"], fontSize=11, leading=16)
    head = ParagraphStyle("hd", parent=styles["Heading2"], fontSize=14,
                          textColor=colors.HexColor("#2b8a3e"), spaceBefore=10)

    doc = SimpleDocTemplate(PDF_PATH, pagesize=A4,
                            leftMargin=18*mm, rightMargin=18*mm,
                            topMargin=16*mm, bottomMargin=16*mm)
    el = []
    el.append(Paragraph("心血管风险归因报告", h))
    el.append(Paragraph(f"PIAS 个人归因（Level 1）· 真实算法输出 · 场景：{primary['scenario']}", sub))

    rt = primary.get("report_text", {}) or {}
    rows = [
        ["当前风险评分", f"{primary['current_risk_score']:.1%} ({risk_level_zh(primary['risk_level'])})"],
        ["趋势", primary["trend_direction"]],
        ["30天风险变化（干预 vs 不干预）", f"{primary['risk_reduction_30d']:+.1%}"],
        ["有干预 / 无干预", f"{primary['projected_risk_30d_with_plan']:.1%} / {primary['projected_risk_30d_no_action']:.1%}"],
        ["历史天数", f"{primary['n_days']}（干预 {primary.get('n_intervention_days','-')} / 对照 {primary.get('n_control_days','-')}）"],
    ]
    if primary.get("individual_att") is not None:
        rows.append(["个体 ATT", f"{primary['individual_att']:+.1%} (95% CI [{primary['att_ci_lower']:+.1%}, {primary['att_ci_upper']:+.1%}], p={primary['att_p_value']:.3f})"])
    t = Table(rows, colWidths=[60*mm, 110*mm])
    t.setStyle(TableStyle([
        ("FONTSIZE", (0,0), (-1,-1), 10),
        ("TEXTCOLOR", (0,0), (0,-1), colors.HexColor(MUTED)),
        ("LINEBELOW", (0,0), (-1,-1), 0.4, colors.HexColor(LINE)),
        ("VALIGN", (0,0), (-1,-1), "MIDDLE"),
        ("TOPPADDING", (0,0), (-1,-1), 6), ("BOTTOMPADDING", (0,0), (-1,-1), 6),
    ]))
    el.append(t)
    el.append(Spacer(1, 8))

    if rt.get("headline"):
        el.append(Paragraph(rt["headline"], head))
    if rt.get("body"):
        el.append(Paragraph(rt["body"].replace("\n", "<br/>"), body))
    if rt.get("att_summary"):
        el.append(Spacer(1, 4))
        el.append(Paragraph(f"📌 {rt['att_summary']}", body))
    if rt.get("advice"):
        el.append(Spacer(1, 6))
        el.append(Paragraph(f"<b>建议：</b>{rt['advice']}", body))

    el.append(Spacer(1, 10))
    el.append(Paragraph("关键指标", head))
    for k, v in (rt.get("metrics", {}) or {}).items():
        el.append(Paragraph(f"• <b>{k}</b>: {v}", body))

    el.append(Spacer(1, 10))
    el.append(Paragraph("其他验证场景", head))
    for s in [x for x in scenarios if x is not primary]:
        el.append(Paragraph(
            f"• <b>{s['scenario']}</b> — 状态 {s['status']}，当前风险 "
            f"{s['current_risk_score']:.1%}（{risk_level_zh(s['risk_level'])}），"
            f"趋势 {s['trend_direction']}，30天变化 {s['risk_reduction_30d']:+.1%}", body))

    el.append(Spacer(1, 14))
    el.append(Paragraph("本报告由 rehealth-algorithms / healthagent.pias IndividualAttributor 真实计算生成，仅供健康参考，不替代医生诊断。", sub))
    doc.build(el)
    return True


def main():
    with open(JSON_PATH, "r", encoding="utf-8") as f:
        scenarios = json.load(f)
    html, primary = build_html(scenarios)
    with open(HTML_PATH, "w", encoding="utf-8") as f:
        f.write(html)
    print(f"[HTML] 已生成: {HTML_PATH}")
    if build_pdf(scenarios, primary):
        print(f"[PDF] 已生成: {PDF_PATH}")
    else:
        print("[PDF] 未生成（reportlab 不可用；可用浏览器打开 HTML 后『打印/导出 PDF』）")


if __name__ == "__main__":
    main()
