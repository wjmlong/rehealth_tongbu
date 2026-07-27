// PIAS API 数据对接模块
// 对接 /api/pias/v2/weijiankang/* 端点

const API_BASE = 'http://localhost:8002/api/pias/v2';

// 统一请求封装
async function request(url, options = {}) {
    try {
        const response = await fetch(`${API_BASE}${url}`, {
            headers: { 'Content-Type': 'application/json' },
            ...options
        });
        const data = await response.json();
        if (!data.success) throw new Error(data.message);
        return data.result;
    } catch (error) {
        console.error(`API Error: ${url}`, error);
        return null;
    }
}

// 区县CVD风险热力图
export async function getHeatmap() {
    return await request('/weijiankang/heatmap');
}

// 分级管理效果评估
export async function getEval() {
    return await request('/weijiankang/eval');
}

// 极高危人群追踪
export async function getHighRisk() {
    return await request('/weijiankang/high-risk');
}

// 中心绩效排名
export async function getRanking() {
    return await request('/weijiankang/ranking');
}

// 健康中国2030考核
export async function get2030() {
    return await request('/weijiankang/2030');
}

// 风险预测
export async function predictRisk(features) {
    return await request('/predict', {
        method: 'POST',
        body: JSON.stringify(features)
    });
}

// 个人归因
export async function getIndividualAttribution(data) {
    return await request('/attribute/individual', {
        method: 'POST',
        body: JSON.stringify(data)
    });
}

// 群体归因
export async function getGroupAttribution(data) {
    return await request('/attribute/group', {
        method: 'POST',
        body: JSON.stringify(data)
    });
}
