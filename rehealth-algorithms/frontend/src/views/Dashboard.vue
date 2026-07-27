<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>西湖区公共卫生健康风险防控数据可视化平台</title>
    <script src="https://cdn.tailwindcss.com"></script>
    <script src="https://cdn.jsdelivr.net/npm/echarts@5.4.3/dist/echarts.min.js"></script>
    <style>
        @import url('https://fonts.googleapis.com/css2?family=Orbitron:wght@400;700&display=swap');
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body { background: linear-gradient(135deg, #0a0e27 0%, #0d1233 50%, #0a1628 100%); color: #fff; font-family: 'Microsoft YaHei', sans-serif; overflow-x: hidden; }
        .glass { background: rgba(255,255,255,0.03); border: 1px solid rgba(255,255,255,0.08); border-radius: 8px; backdrop-filter: blur(10px); }
        .glass-blue { background: rgba(59,130,246,0.08); border: 1px solid rgba(59,130,246,0.2); border-radius: 8px; }
        .title-bar { background: linear-gradient(90deg, rgba(59,130,246,0.4) 0%, rgba(59,130,246,0) 100%); border-left: 3px solid #3b82f6; padding: 10px 16px; margin-bottom: 12px; }
        .section-title { font-size: 18px; font-weight: bold; color: #3b82f6; }
        .section-subtitle { font-size: 12px; color: #94a3b8; }
        .stat-number { font-size: 36px; font-weight: bold; font-family: 'Orbitron', monospace; }
        .stat-number-blue { background: linear-gradient(135deg, #60a5fa, #3b82f6); -webkit-background-clip: text; -webkit-text-fill-color: transparent; }
        .stat-number-green { background: linear-gradient(135deg, #4ade80, #22c55e); -webkit-background-clip: text; -webkit-text-fill-color: transparent; }
        .stat-number-red { background: linear-gradient(135deg, #f87171, #ef4444); -webkit-background-clip: text; -webkit-text-fill-color: transparent; }
        .stat-number-yellow { background: linear-gradient(135deg, #fbbf24, #f59e0b); -webkit-background-clip: text; -webkit-text-fill-color: transparent; }
        .stat-number-purple { background: linear-gradient(135deg, #a78bfa, #8b5cf6); -webkit-background-clip: text; -webkit-text-fill-color: transparent; }
        .progress-bar { height: 6px; border-radius: 3px; background: rgba(255,255,255,0.1); overflow: hidden; }
        .progress-fill { height: 100%; border-radius: 3px; transition: width 0.5s ease; }
        @keyframes pulse-glow { 0%, 100% { box-shadow: 0 0 10px rgba(59,130,246,0.3); } 50% { box-shadow: 0 0 20px rgba(59,130,246,0.6); } }
        .pulse-glow { animation: pulse-glow 2s infinite; }
        @keyframes data-flow { 0% { background-position: 0% 50%; } 100% { background-position: 100% 50%; } }
        .flow-step { position: relative; padding: 10px 24px; cursor: pointer; background: rgba(255,255,255,0.05); border: 1px solid rgba(255,255,255,0.1); border-radius: 6px; transition: all 0.3s; }
        .flow-step:hover { background: rgba(59,130,246,0.2); border-color: rgba(59,130,246,0.5); }
        .flow-step.active { background: linear-gradient(135deg, rgba(59,130,246,0.3) 0%, rgba(59,130,246,0.1) 100%); border: 1px solid #3b82f6; box-shadow: 0 0 15px rgba(59,130,246,0.3); }
        .flow-arrow { color: #3b82f6; font-size: 20px; margin: 0 8px; }
        .flow-step-number { font-size: 11px; color: #94a3b8; display: block; }
        .flow-step-title { font-size: 14px; font-weight: bold; }
        .risk-tag { display: inline-flex; align-items: center; gap: 4px; padding: 2px 8px; border-radius: 4px; font-size: 12px; }
        .risk-high { background: rgba(239,68,68,0.2); color: #f87171; }
        .risk-medium { background: rgba(249,115,22,0.2); color: #fb923c; }
        .risk-low { background: rgba(234,179,8,0.2); color: #facc15; }
        .map-container { position: relative; background: radial-gradient(ellipse at center, rgba(59,130,246,0.1) 0%, transparent 70%); border-radius: 12px; }
        .map-dot { position: absolute; width: 8px; height: 8px; border-radius: 50%; animation: pulse 2s infinite; }
        .dot-high { background: #ef4444; box-shadow: 0 0 10px #ef4444; }
        .dot-intervention { background: #22c55e; box-shadow: 0 0 10px #22c55e; }
        @keyframes pulse { 0%, 100% { transform: scale(1); opacity: 1; } 50% { transform: scale(1.2); opacity: 0.7; } }
        .stat-card { padding: 12px; text-align: center; }
        .stat-card-value { font-size: 24px; font-weight: bold; font-family: 'Orbitron', monospace; }
        .stat-card-label { font-size: 11px; color: #94a3b8; margin-top: 4px; }
        .stat-card-change { font-size: 11px; margin-top: 4px; }
        .stat-card-change.up { color: #22c55e; }
        .stat-card-change.down { color: #ef4444; }
        .intervention-item { display: flex; justify-content: space-between; align-items: center; padding: 6px 0; border-bottom: 1px solid rgba(255,255,255,0.05); }
        .intervention-item:last-child { border-bottom: none; }
        .community-item { display: flex; justify-content: space-between; align-items: center; padding: 8px 12px; background: rgba(255,255,255,0.03); border-radius: 6px; margin-bottom: 6px; }
        .community-rank { width: 24px; height: 24px; border-radius: 50%; background: linear-gradient(135deg, #3b82f6, #2563eb); display: flex; align-items: center; justify-content: center; font-size: 12px; font-weight: bold; }
        .cost-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 8px; }
        .cost-item { text-align: center; padding: 12px 8px; background: rgba(255,255,255,0.03); border-radius: 8px; }
        .conclusion-box { background: linear-gradient(135deg, rgba(59,130,246,0.1) 0%, rgba(139,92,246,0.1) 100%); border: 1px solid rgba(59,130,246,0.3); border-radius: 8px; padding: 12px; }
        .roi-box { background: linear-gradient(135deg, rgba(34,197,94,0.2) 0%, rgba(16,185,129,0.2) 100%); border: 1px solid rgba(34,197,94,0.4); border-radius: 8px; padding: 16px; text-align: center; }
        .header-info { display: flex; align-items: center; gap: 16px; }
        .header-logo { width: 48px; height: 48px; background: linear-gradient(135deg, #1e40af, #3b82f6); border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 24px; box-shadow: 0 0 20px rgba(59,130,246,0.4); }
        .header-title { font-size: 28px; font-weight: bold; background: linear-gradient(135deg, #60a5fa, #3b82f6, #8b5cf6); -webkit-background-clip: text; -webkit-text-fill-color: transparent; }
        .header-subtitle { font-size: 14px; color: #94a3b8; }
        .header-right { text-align: right; }
        .header-time { font-size: 16px; font-family: 'Orbitron', monospace; color: #60a5fa; }
        .header-weather { font-size: 12px; color: #94a3b8; margin-top: 4px; }
        .data-source { font-size: 11px; color: #64748b; text-align: center; padding: 8px; margin-top: 8px; }
    </style>
</head>
<body class="min-h-screen">

<!-- 顶部标题栏 -->
<header class="p-4">
    <div class="glass p-4 flex items-center justify-between">
        <div class="header-info">
            <div class="header-logo">🏥</div>
            <div>
                <div class="header-title">西湖区公共卫生健康风险防控数据可视化平台</div>
                <div class="header-subtitle">数据驱动 · 精准防控 · 科学决策</div>
            </div>
        </div>
        <div class="header-right">
            <div class="header-time" id="datetime">2025-05-20 14:30:45</div>
            <div class="header-weather">☁️ 多云 22-28℃ 东南风2级 空气优</div>
        </div>
    </div>
</header>

<!-- 流程导航 -->
<div class="px-4 mb-4">
    <div class="glass p-3 flex items-center justify-center gap-3">
        <div class="flow-step active" onclick="switchTab(0)">
            <span class="flow-step-number">01</span>
            <span class="flow-step-title">风险预测</span>
        </div>
        <div class="flow-arrow">→</div>
        <div class="flow-step" onclick="switchTab(1)">
            <span class="flow-step-number">02</span>
            <span class="flow-step-title">健康干预</span>
        </div>
        <div class="flow-arrow">→</div>
        <div class="flow-step" onclick="switchTab(2)">
            <span class="flow-step-number">03</span>
            <span class="flow-step-title">归因分析</span>
        </div>
        <div class="flow-arrow">→</div>
        <div class="flow-step" onclick="switchTab(3)">
            <span class="flow-step-number">04</span>
            <span class="flow-step-title">结算成效</span>
        </div>
    </div>
</div>

<!-- 主内容区 -->
<div class="px-4 pb-4">
    <div class="grid grid-cols-12 gap-4">

        <!-- 左侧面板 -->
        <div class="col-span-3 space-y-4">
            <!-- 01 风险预测 -->
            <div class="glass p-4">
                <div class="title-bar">
                    <span class="section-title">01 风险预测</span>
                    <span class="section-subtitle">高风险人群分布</span>
                </div>

                <div class="text-center mb-4">
                    <div class="section-subtitle">高风险人群总数</div>
                    <div class="stat-number stat-number-blue">18,573</div>
                    <div class="text-xs text-gray-400 mt-1">
                        <span class="text-green-400">↑ 12.6%</span> 环比上升 |
                        <span class="text-blue-400">+2,076</span> 较上月新增
                    </div>
                </div>

                <!-- 风险等级分布 -->
                <div class="mb-4">
                    <div class="section-subtitle mb-2">风险等级分布</div>
                    <div class="space-y-2">
                        <div class="flex items-center justify-between text-sm">
                            <div class="flex items-center gap-2">
                                <div class="w-3 h-3 bg-red-500 rounded-sm"></div>
                                <span>高风险</span>
                            </div>
                            <span class="text-red-400">7,231人</span>
                            <span class="text-gray-500 w-12 text-right">38.97%</span>
                        </div>
                        <div class="flex items-center justify-between text-sm">
                            <div class="flex items-center gap-2">
                                <div class="w-3 h-3 bg-orange-500 rounded-sm"></div>
                                <span>中风险</span>
                            </div>
                            <span class="text-orange-400">6,573人</span>
                            <span class="text-gray-500 w-12 text-right">35.40%</span>
                        </div>
                        <div class="flex items-center justify-between text-sm">
                            <div class="flex items-center gap-2">
                                <div class="w-3 h-3 bg-yellow-500 rounded-sm"></div>
                                <span>低风险</span>
                            </div>
                            <span class="text-yellow-400">4,769人</span>
                            <span class="text-gray-500 w-12 text-right">25.63%</span>
                        </div>
                    </div>
                </div>

                <!-- 高风险TOP5社区 -->
                <div>
                    <div class="section-subtitle mb-2">高风险TOP5社区</div>
                    <div class="space-y-2">
                        <div class="community-item">
                            <div class="flex items-center gap-2">
                                <div class="community-rank">1</div>
                                <span class="text-sm">三墩镇西溪社区</span>
                            </div>
                            <span class="text-blue-400 text-sm">1,382人</span>
                        </div>
                        <div class="community-item">
                            <div class="flex items-center gap-2">
                                <div class="community-rank">2</div>
                                <span class="text-sm">北山街道保俶社区</span>
                            </div>
                            <span class="text-blue-400 text-sm">1,276人</span>
                        </div>
                        <div class="community-item">
                            <div class="flex items-center gap-2">
                                <div class="community-rank">3</div>
                                <span class="text-sm">蒋村街道浙大社区</span>
                            </div>
                            <span class="text-blue-400 text-sm">1,102人</span>
                        </div>
                        <div class="community-item">
                            <div class="flex items-center gap-2">
                                <div class="community-rank">4</div>
                                <span class="text-sm">文新街道文新社区</span>
                            </div>
                            <span class="text-blue-400 text-sm">987人</span>
                        </div>
                        <div class="community-item">
                            <div class="flex items-center gap-2">
                                <div class="community-rank">5</div>
                                <span class="text-sm">灵隐街道灵隐社区</span>
                            </div>
                            <span class="text-blue-400 text-sm">856人</span>
                        </div>
                    </div>
                </div>
            </div>

            <!-- 02 健康干预 -->
            <div class="glass p-4">
                <div class="title-bar" style="border-left-color: #22c55e; background: linear-gradient(90deg, rgba(34,197,94,0.4) 0%, rgba(34,197,94,0) 100%);">
                    <span class="section-title" style="color: #22c55e;">02 健康干预</span>
                    <span class="section-subtitle">干预服务分布</span>
                </div>

                <div class="flex items-center justify-between mb-4">
                    <div>
                        <div class="section-subtitle">干预总人数</div>
                        <div class="stat-number stat-number-green">12,530</div>
                    </div>
                    <div class="text-center">
                        <div class="w-20 h-20 rounded-full border-4 border-green-500 flex items-center justify-center">
                            <div>
                                <div class="text-lg font-bold text-green-400">68.32%</div>
                                <div class="text-xs text-gray-400">干预覆盖率</div>
                            </div>
                        </div>
                    </div>
                </div>

                <!-- 干预统计 -->
                <div class="grid grid-cols-4 gap-2 text-center mb-4">
                    <div class="stat-card">
                        <div class="stat-card-value text-blue-400">324人</div>
                        <div class="stat-card-label">较昨日新增</div>
                    </div>
                    <div class="stat-card">
                        <div class="stat-card-value text-blue-400">1,532人</div>
                        <div class="stat-card-label">本周新增</div>
                    </div>
                    <div class="stat-card">
                        <div class="stat-card-value text-blue-400">4,689人</div>
                        <div class="stat-card-label">本月新增</div>
                    </div>
                    <div class="stat-card">
                        <div class="stat-card-value text-blue-400">32,689人</div>
                        <div class="stat-card-label">累计干预人次</div>
                    </div>
                </div>

                <!-- 干预类型分布 -->
                <div>
                    <div class="section-subtitle mb-2">干预类型分布</div>
                    <div class="space-y-3">
                        <div class="intervention-item">
                            <span class="text-sm">慢病管理</span>
                            <div class="flex items-center gap-2">
                                <span class="text-blue-400 text-sm">5,382人</span>
                                <span class="text-gray-500 text-xs w-12 text-right">43.01%</span>
                            </div>
                        </div>
                        <div class="progress-bar"><div class="progress-fill bg-blue-500" style="width:43%"></div></div>

                        <div class="intervention-item">
                            <span class="text-sm">健康教育</span>
                            <div class="flex items-center gap-2">
                                <span class="text-green-400 text-sm">3,256人</span>
                                <span class="text-gray-500 text-xs w-12 text-right">25.98%</span>
                            </div>
                        </div>
                        <div class="progress-bar"><div class="progress-fill bg-green-500" style="width:26%"></div></div>

                        <div class="intervention-item">
                            <span class="text-sm">营养干预</span>
                            <div class="flex items-center gap-2">
                                <span class="text-yellow-400 text-sm">2,153人</span>
                                <span class="text-gray-500 text-xs w-12 text-right">17.19%</span>
                            </div>
                        </div>
                        <div class="progress-bar"><div class="progress-fill bg-yellow-500" style="width:17%"></div></div>

                        <div class="intervention-item">
                            <span class="text-sm">心理干预</span>
                            <div class="flex items-center gap-2">
                                <span class="text-purple-400 text-sm">1,254人</span>
                                <span class="text-gray-500 text-xs w-12 text-right">10.00%</span>
                            </div>
                        </div>
                        <div class="progress-bar"><div class="progress-fill bg-purple-500" style="width:10%"></div></div>

                        <div class="intervention-item">
                            <span class="text-sm">其他干预</span>
                            <div class="flex items-center gap-2">
                                <span class="text-gray-400 text-sm">485人</span>
                                <span class="text-gray-500 text-xs w-12 text-right">3.87%</span>
                            </div>
                        </div>
                        <div class="progress-bar"><div class="progress-fill bg-gray-500" style="width:4%"></div></div>
                    </div>
                </div>
            </div>
        </div>

        <!-- 中间地图区 -->
        <div class="col-span-6">
            <div class="glass p-4 h-full">
                <!-- 地图标题栏 -->
                <div class="flex items-center justify-between mb-4">
                    <div class="flex items-center gap-4">
                        <span class="flex items-center gap-2 text-sm">
                            <div class="w-3 h-3 bg-red-500 rounded-full pulse-glow"></div>
                            高风险人群
                        </span>
                        <span class="flex items-center gap-2 text-sm">
                            <div class="w-3 h-3 bg-green-500 rounded-full pulse-glow"></div>
                            干预人群
                        </span>
                    </div>
                </div>

                <!-- 地图容器 -->
                <div class="map-container" style="height: 420px;">
                    <div id="mapChart" style="width: 100%; height: 100%;"></div>
                </div>

                <!-- 全区总览 -->
                <div class="grid grid-cols-4 gap-3 mt-4">
                    <div class="glass-blue p-3 text-center">
                        <div class="text-xs text-gray-400">辖区社区数</div>
                        <div class="text-lg font-bold text-blue-400">28 个</div>
                    </div>
                    <div class="glass-blue p-3 text-center">
                        <div class="text-xs text-gray-400">常住人口</div>
                        <div class="text-lg font-bold text-blue-400">1,108,962 人</div>
                    </div>
                    <div class="glass-blue p-3 text-center">
                        <div class="text-xs text-gray-400">高风险覆盖率</div>
                        <div class="text-lg font-bold text-green-400">16.75%</div>
                    </div>
                    <div class="glass-blue p-3 text-center">
                        <div class="text-xs text-gray-400">干预覆盖率</div>
                        <div class="text-lg font-bold text-green-400">68.32%</div>
                    </div>
                </div>

                <!-- 风险密度图例 -->
                <div class="mt-4">
                    <div class="section-subtitle mb-2">风险密度（人/平方公里）</div>
                    <div class="flex items-center gap-4 text-xs">
                        <span class="flex items-center gap-1"><div class="w-4 h-4 bg-red-600 rounded"></div>&gt;500</span>
                        <span class="flex items-center gap-1"><div class="w-4 h-4 bg-orange-500 rounded"></div>300-500</span>
                        <span class="flex items-center gap-1"><div class="w-4 h-4 bg-yellow-500 rounded"></div>100-300</span>
                        <span class="flex items-center gap-1"><div class="w-4 h-4 bg-green-500 rounded"></div>50-100</span>
                        <span class="flex items-center gap-1"><div class="w-4 h-4 bg-blue-500 rounded"></div>&lt;50</span>
                    </div>
                </div>
            </div>
        </div>

        <!-- 右侧面板 -->
        <div class="col-span-3 space-y-4">
            <!-- 03 归因分析 -->
            <div class="glass p-4">
                <div class="title-bar" style="border-left-color: #8b5cf6; background: linear-gradient(90deg, rgba(139,92,246,0.4) 0%, rgba(139,92,246,0) 100%);">
                    <span class="section-title" style="color: #8b5cf6;">03 归因分析</span>
                    <span class="section-subtitle">归因分析报告（每日更新）</span>
                </div>

                <!-- 高风险成因分析 -->
                <div class="mb-4">
                    <div class="section-subtitle mb-2">高风险成因分析 TOP5</div>
                    <div class="space-y-2">
                        <div class="flex items-center justify-between text-sm">
                            <span>高血压</span>
                            <span class="text-blue-400">27.36%</span>
                        </div>
                        <div class="progress-bar"><div class="progress-fill bg-blue-500" style="width:27%"></div></div>

                        <div class="flex items-center justify-between text-sm">
                            <span>高血脂</span>
                            <span class="text-blue-400">21.45%</span>
                        </div>
                        <div class="progress-bar"><div class="progress-fill bg-blue-400" style="width:21%"></div></div>

                        <div class="flex items-center justify-between text-sm">
                            <span>超重肥胖</span>
                            <span class="text-blue-400">18.23%</span>
                        </div>
                        <div class="progress-bar"><div class="progress-fill bg-blue-300" style="width:18%"></div></div>

                        <div class="flex items-center justify-between text-sm">
                            <span>吸烟</span>
                            <span class="text-blue-400">12.87%</span>
                        </div>
                        <div class="progress-bar"><div class="progress-fill bg-blue-200" style="width:13%"></div></div>

                        <div class="flex items-center justify-between text-sm">
                            <span>缺乏运动</span>
                            <span class="text-blue-400">8.45%</span>
                        </div>
                        <div class="progress-bar"><div class="progress-fill bg-blue-100" style="width:8%"></div></div>
                    </div>
                </div>

                <!-- 干预措施有效性分析 -->
                <div class="mb-4">
                    <div class="section-subtitle mb-2">干预措施有效性分析</div>
                    <div id="effectChart" style="height: 160px;"></div>
                </div>

                <!-- 归因分析结论 -->
                <div class="conclusion-box">
                    <div class="section-subtitle mb-2">归因分析结论</div>
                    <ul class="space-y-1 text-xs text-gray-300">
                        <li>• 本周期干预人群风险水平较对照组显著降低（P &lt; 0.05）</li>
                        <li>• 经PSM匹配后，干预措施对风险降低的贡献率为 78.32%</li>
                        <li>• 核心有效干预：慢病管理、营养干预、健康教育</li>
                        <li>• 建议：加强高血压、超重肥胖人群的早期干预</li>
                    </ul>
                    <div class="mt-3 text-center">
                        <div class="text-xs text-gray-400">归因贡献率</div>
                        <div class="stat-number stat-number-purple text-4xl">78.32%</div>
                    </div>
                </div>
            </div>

            <!-- 04 结算成效 -->
            <div class="glass p-4">
                <div class="title-bar" style="border-left-color: #fbbf24; background: linear-gradient(90deg, rgba(251,191,36,0.4) 0%, rgba(251,191,36,0) 100%);">
                    <span class="section-title" style="color: #fbbf24;">04 结算成效</span>
                    <span class="section-subtitle">费用结算 & 价值成效（累计）</span>
                </div>

                <!-- 费用统计 -->
                <div class="cost-grid mb-4">
                    <div class="cost-item">
                        <div class="text-xs text-gray-400">医疗费用节约金额</div>
                        <div class="stat-card-value text-green-400 text-xl">12,830,000</div>
                        <div class="text-xs text-gray-500">元</div>
                        <div class="text-xs text-green-400">↑ 18.62%</div>
                    </div>
                    <div class="cost-item">
                        <div class="text-xs text-gray-400">医保支出降低额度</div>
                        <div class="stat-card-value text-green-400 text-xl">9,560,000</div>
                        <div class="text-xs text-gray-500">元</div>
                        <div class="text-xs text-green-400">↑ 16.35%</div>
                    </div>
                    <div class="cost-item">
                        <div class="text-xs text-gray-400">人均医疗成本下降</div>
                        <div class="stat-card-value text-green-400 text-xl">1,286</div>
                        <div class="text-xs text-gray-500">元/人</div>
                        <div class="text-xs text-green-400">↑ 12.31%</div>
                    </div>
                    <div class="cost-item">
                        <div class="text-xs text-gray-400">公共卫生防控成本下降</div>
                        <div class="stat-card-value text-green-400 text-xl">2,350,000</div>
                        <div class="text-xs text-gray-500">元</div>
                        <div class="text-xs text-green-400">↑ 19.45%</div>
                    </div>
                </div>

                <!-- ROI -->
                <div class="roi-box mb-4">
                    <div class="text-xs text-gray-400">投资回报比（ROI）</div>
                    <div class="text-3xl font-bold text-green-400 mt-1">1 : 4.7</div>
                    <div class="text-xs text-gray-400 mt-1">投入产出比</div>
                </div>

                <!-- 费用对比图 -->
                <div id="costChart" style="height: 180px;"></div>
            </div>
        </div>
    </div>
</div>

<!-- 底部数据源 -->
<footer class="data-source">
    数据来源：西湖区卫健委数据中台；本数据为脱敏数据，仅用于分析展示
</footer>

<script>
// 时间更新
function updateTime() {
    const now = new Date();
    const year = now.getFullYear();
    const month = String(now.getMonth() + 1).padStart(2, '0');
    const day = String(now.getDate()).padStart(2, '0');
    const hours = String(now.getHours()).padStart(2, '0');
    const minutes = String(now.getMinutes()).padStart(2, '0');
    const seconds = String(now.getSeconds()).padStart(2, '0');
    const weekdays = ['星期日', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六'];
    const weekday = weekdays[now.getDay()];
    document.getElementById('datetime').textContent = `${year}-${month}-${day} ${hours}:${minutes}:${seconds} ${weekday}`;
}
setInterval(updateTime, 1000);
updateTime();

// Tab切换
function switchTab(idx) {
    document.querySelectorAll('.flow-step').forEach((el, i) => {
        el.classList.toggle('active', i === idx);
    });
}

// 西湖区地图（简化版GeoJSON）
const xihuGeoJson = {"type": "FeatureCollection", "features": [{"type": "Feature", "properties": {"adcode": 330106, "name": "西湖区", "center": [120.147376, 30.272934], "centroid": [120.08362, 30.200766], "childrenNum": 0, "level": "district", "parent": {"adcode": 330100}, "subFeatureIndex": 2, "acroutes": [100000, 330000, 330100]}, "geometry": {"type": "MultiPolygon", "coordinates": [[[[119.996332, 30.181536], [119.998994, 30.182305], [120.005459, 30.182346], [120.011449, 30.181852], [120.013494, 30.181042], [120.016156, 30.177072], [120.014397, 30.173816], [120.017772, 30.168417], [120.018913, 30.165202], [120.018438, 30.162963], [120.014825, 30.15891], [120.017178, 30.153167], [120.016417, 30.148935], [120.011663, 30.142779], [120.008858, 30.136719], [120.008787, 30.134204], [120.007004, 30.130026], [120.003819, 30.12659], [120.001941, 30.119567], [120.000468, 30.116034], [120.001822, 30.114357], [120.01095, 30.116254], [120.016417, 30.115718], [120.017725, 30.113189], [120.016465, 30.108323], [120.019151, 30.10567], [120.02324, 30.10633], [120.025213, 30.105519], [120.030442, 30.098837], [120.030846, 30.094768], [120.036361, 30.092555], [120.041258, 30.093091], [120.043635, 30.092156], [120.044894, 30.086368], [120.049125, 30.086849], [120.052953, 30.091661], [120.054902, 30.09272], [120.059513, 30.092788], [120.061748, 30.091853], [120.067928, 30.086794], [120.076747, 30.080868], [120.084615, 30.078269], [120.091579, 30.078475], [120.09795, 30.079603], [120.108955, 30.08062], [120.115088, 30.082229], [120.118131, 30.083796], [120.123812, 30.089461], [120.124739, 30.092953], [120.130349, 30.099305], [120.134818, 30.097669], [120.146869, 30.088664], [120.150459, 30.091455], [120.162011, 30.097985], [120.170354, 30.101546], [120.177295, 30.102604], [120.182168, 30.104818], [120.182953, 30.10846], [120.181835, 30.111457], [120.177723, 30.11712], [120.16831, 30.121793], [120.160014, 30.12637], [120.146132, 30.137172], [120.138597, 30.142078], [120.130373, 30.151161], [120.125999, 30.15946], [120.124596, 30.167263], [120.12462, 30.177346], [120.126664, 30.182827], [120.129659, 30.186852], [120.134128, 30.190588], [120.140166, 30.19284], [120.138169, 30.19512], [120.1371, 30.1981], [120.137551, 30.201012], [120.141545, 30.204061], [120.141069, 30.206464], [120.138431, 30.207947], [120.139405, 30.210474], [120.145253, 30.209581], [120.147368, 30.210268], [120.150173, 30.212849], [120.160347, 30.227486], [120.15923, 30.231578], [120.154381, 30.23442], [120.154666, 30.241119], [120.160442, 30.246843], [120.15797, 30.24672], [120.159848, 30.249493], [120.16251, 30.251565], [120.15923, 30.256397], [120.156948, 30.258058], [120.156472, 30.260295], [120.158469, 30.259142], [120.153763, 30.275734], [120.150934, 30.278506], [120.149294, 30.28206], [120.146132, 30.286589], [120.144112, 30.291144], [120.141188, 30.293902], [120.135198, 30.292626], [120.132512, 30.292791], [120.128709, 30.294286], [120.118012, 30.29264], [120.109241, 30.291995], [120.106507, 30.292269], [120.105533, 30.300227], [120.102062, 30.300117], [120.102823, 30.304672], [120.102038, 30.312697], [120.095597, 30.325481], [120.092007, 30.326071], [120.088489, 30.330529], [120.085779, 30.331187], [120.081715, 30.335918], [120.082808, 30.338853], [120.079742, 30.338606], [120.080075, 30.342625], [120.0762, 30.345285], [120.074655, 30.34774], [120.069449, 30.350702], [120.067334, 30.355446], [120.06517, 30.355309], [120.065361, 30.352813], [120.063673, 30.352128], [120.060868, 30.353348], [120.060607, 30.351497], [120.053951, 30.351401], [120.050195, 30.349865], [120.048674, 30.353526], [120.046463, 30.353759], [120.046511, 30.35221], [120.038595, 30.350948], [120.032795, 30.351634], [120.029444, 30.350016], [120.023881, 30.348357], [120.025973, 30.34523], [120.025545, 30.342474], [120.027185, 30.335905], [120.026425, 30.333272], [120.01763, 30.33238], [120.01782, 30.329733], [120.021362, 30.329088], [120.023572, 30.326743], [120.023406, 30.319967], [120.023881, 30.316195], [120.020554, 30.315674], [120.02179, 30.311874], [120.026686, 30.310996], [120.026116, 30.305948], [120.028493, 30.303835], [120.030799, 30.299843], [120.042089, 30.304096], [120.048008, 30.30319], [120.051907, 30.299513], [120.053784, 30.298772], [120.054807, 30.295246], [120.052953, 30.293847], [120.051621, 30.29054], [120.052263, 30.289319], [120.056732, 30.2884], [120.05281, 30.282897], [120.049815, 30.27594], [120.049815, 30.274266], [120.052382, 30.273237], [120.056494, 30.273374], [120.057255, 30.268351], [120.05773, 30.257289], [120.058515, 30.252801], [120.055306, 30.251483], [120.055234, 30.245388], [120.052691, 30.245333], [120.052216, 30.243494], [120.046178, 30.242725], [120.0443, 30.240474], [120.044538, 30.238058], [120.041162, 30.236122], [120.040639, 30.232841], [120.038571, 30.233019], [120.032153, 30.229697], [120.030894, 30.230479], [120.025046, 30.228461], [120.016845, 30.224823], [120.01889, 30.219742], [120.020268, 30.217985], [120.018367, 30.216447], [120.015443, 30.218067], [120.013755, 30.2162], [120.010356, 30.221184], [120.007123, 30.2208], [120.009999, 30.215637], [120.015704, 30.213426], [120.018034, 30.214319], [120.017487, 30.211792], [120.013351, 30.210254], [120.007171, 30.208757], [120.006529, 30.205379], [120.009263, 30.200202], [120.009096, 30.195615], [120.003439, 30.191865], [120.0013, 30.188006], [119.996332, 30.181536]]]]}}]};

// 注册地图
echarts.registerMap('xihu', xihuGeoJson);

// 高风险人群数据
const highRiskData = [
    { name: '三墩镇', value: [120.05, 30.32, 1382] },
    { name: '北山街道', value: [120.145, 30.28, 1276] },
    { name: '蒋村街道', value: [120.075, 30.295, 1102] },
    { name: '文新街道', value: [120.105, 30.26, 987] },
    { name: '灵隐街道', value: [120.13, 30.24, 856] },
    { name: '留下街道', value: [120.055, 30.22, 743] },
    { name: '西溪街道', value: [120.135, 30.29, 698] },
    { name: '翠苑街道', value: [120.105, 30.305, 654] },
    { name: '古荡街道', value: [120.125, 30.27, 612] },
    { name: '转塘街道', value: [120.045, 30.175, 578] },
    { name: '双浦镇', value: [120.025, 30.15, 534] },
    { name: '袁浦镇', value: [120.02, 30.125, 489] },
];

// 干预人群数据
const interventionData = [
    { name: '三墩镇', value: [120.05, 30.32, 980] },
    { name: '北山街道', value: [120.145, 30.28, 890] },
    { name: '蒋村街道', value: [120.075, 30.295, 756] },
    { name: '文新街道', value: [120.105, 30.26, 678] },
    { name: '灵隐街道', value: [120.13, 30.24, 589] },
    { name: '留下街道', value: [120.055, 30.22, 520] },
    { name: '西溪街道', value: [120.135, 30.29, 450] },
    { name: '翠苑街道', value: [120.105, 30.305, 420] },
    { name: '古荡街道', value: [120.125, 30.27, 380] },
];

// 初始化地图
const mapChart = echarts.init(document.getElementById('mapChart'));
mapChart.setOption({
    geo: {
        map: 'xihu',
        roam: true,
        zoom: 1.2,
        center: [120.09, 30.25],
        label: {
            show: true,
            color: '#fff',
            fontSize: 12,
            fontWeight: 'bold'
        },
        itemStyle: {
            areaColor: 'rgba(30, 58, 95, 0.8)',
            borderColor: '#3b82f6',
            borderWidth: 2,
            shadowColor: 'rgba(59, 130, 246, 0.3)',
            shadowBlur: 10
        },
        emphasis: {
            itemStyle: {
                areaColor: 'rgba(59, 130, 246, 0.6)',
                borderColor: '#60a5fa',
                borderWidth: 3
            },
            label: {
                color: '#fff',
                fontSize: 14
            }
        }
    },
    series: [
        {
            name: '高风险人群',
            type: 'effectScatter',
            coordinateSystem: 'geo',
            data: highRiskData,
            symbolSize: function(val) {
                return Math.max(12, Math.sqrt(val[2]) * 1.5);
            },
            showEffectOn: 'render',
            rippleEffect: {
                brushType: 'stroke',
                scale: 3,
                period: 4
            },
            itemStyle: {
                color: '#ef4444',
                shadowBlur: 15,
                shadowColor: 'rgba(239, 68, 68, 0.6)'
            },
            zlevel: 1
        },
        {
            name: '干预人群',
            type: 'effectScatter',
            coordinateSystem: 'geo',
            data: interventionData,
            symbolSize: function(val) {
                return Math.max(10, Math.sqrt(val[2]) * 1.2);
            },
            showEffectOn: 'render',
            rippleEffect: {
                brushType: 'stroke',
                scale: 2,
                period: 3
            },
            itemStyle: {
                color: '#22c55e',
                shadowBlur: 15,
                shadowColor: 'rgba(34, 197, 94, 0.6)'
            },
            zlevel: 1
        }
    ]
});

// 干预效果图表
const effectChart = echarts.init(document.getElementById('effectChart'));
effectChart.setOption({
    tooltip: {
        trigger: 'axis',
        backgroundColor: 'rgba(0, 0, 0, 0.8)',
        borderColor: '#3b82f6',
        textStyle: { color: '#fff' }
    },
    legend: {
        data: ['干预组', '对照组'],
        textStyle: { color: '#fff', fontSize: 11 },
        top: 0,
        right: 0
    },
    grid: {
        left: '15%',
        right: '5%',
        top: '20%',
        bottom: '15%'
    },
    xAxis: {
        type: 'category',
        data: ['干预前', '第2周', '第4周', '第8周', '第12周'],
        axisLine: { lineStyle: { color: '#334155' } },
        axisLabel: { color: '#94a3b8', fontSize: 10 }
    },
    yAxis: {
        type: 'value',
        name: '风险值(%)',
        nameTextStyle: { color: '#94a3b8', fontSize: 10 },
        axisLine: { lineStyle: { color: '#334155' } },
        axisLabel: { color: '#94a3b8', fontSize: 10 },
        splitLine: { lineStyle: { color: 'rgba(255,255,255,0.05)' } }
    },
    series: [
        {
            name: '干预组',
            type: 'line',
            data: [85, 72, 60, 48, 42],
            smooth: true,
            lineStyle: { color: '#3b82f6', width: 3 },
            itemStyle: { color: '#3b82f6' },
            areaStyle: {
                color: {
                    type: 'linear',
                    x: 0, y: 0, x2: 0, y2: 1,
                    colorStops: [
                        { offset: 0, color: 'rgba(59, 130, 246, 0.3)' },
                        { offset: 1, color: 'rgba(59, 130, 246, 0)' }
                    ]
                }
            },
            symbol: 'circle',
            symbolSize: 8
        },
        {
            name: '对照组',
            type: 'line',
            data: [84, 80, 78, 75, 72],
            smooth: true,
            lineStyle: { color: '#94a3b8', width: 2, type: 'dashed' },
            itemStyle: { color: '#94a3b8' },
            symbol: 'circle',
            symbolSize: 6
        }
    ]
});

// 费用对比图表
const costChart = echarts.init(document.getElementById('costChart'));
costChart.setOption({
    tooltip: {
        trigger: 'axis',
        backgroundColor: 'rgba(0, 0, 0, 0.8)',
        borderColor: '#fbbf24',
        textStyle: { color: '#fff' }
    },
    legend: {
        data: ['干预前', '干预后'],
        textStyle: { color: '#fff', fontSize: 11 },
        top: 0,
        right: 0
    },
    grid: {
        left: '15%',
        right: '5%',
        top: '20%',
        bottom: '15%'
    },
    xAxis: {
        type: 'category',
        data: ['住院费用', '门诊费用', '药品费用', '检查费用', '其他费用'],
        axisLine: { lineStyle: { color: '#334155' } },
        axisLabel: { color: '#94a3b8', fontSize: 10, rotate: 15 }
    },
    yAxis: {
        type: 'value',
        name: '万元',
        nameTextStyle: { color: '#94a3b8', fontSize: 10 },
        axisLine: { lineStyle: { color: '#334155' } },
        axisLabel: { color: '#94a3b8', fontSize: 10 },
        splitLine: { lineStyle: { color: 'rgba(255,255,255,0.05)' } }
    },
    series: [
        {
            name: '干预前',
            type: 'bar',
            data: [5120, 2860, 1980, 1320, 780],
            itemStyle: {
                color: {
                    type: 'linear',
                    x: 0, y: 0, x2: 0, y2: 1,
                    colorStops: [
                        { offset: 0, color: '#3b82f6' },
                        { offset: 1, color: '#1e40af' }
                    ]
                },
                borderRadius: [4, 4, 0, 0]
            },
            barWidth: 12
        },
        {
            name: '干预后',
            type: 'bar',
            data: [4012, 2156, 1365, 987, 512],
            itemStyle: {
                color: {
                    type: 'linear',
                    x: 0, y: 0, x2: 0, y2: 1,
                    colorStops: [
                        { offset: 0, color: '#22c55e' },
                        { offset: 1, color: '#15803d' }
                    ]
                },
                borderRadius: [4, 4, 0, 0]
            },
            barWidth: 12
        }
    ]
});

// 响应式
window.addEventListener('resize', () => {
    mapChart.resize();
    effectChart.resize();
    costChart.resize();
});
</script>

</body>
</html>
