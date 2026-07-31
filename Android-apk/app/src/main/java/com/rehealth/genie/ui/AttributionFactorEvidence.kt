package com.rehealth.genie.ui

internal data class AttributionFactorEvidence(
    val explanation: String,
    val recommendation: String,
)

internal fun attributionFactorEvidence(factor: AttributionFactorUi): AttributionFactorEvidence {
    val value = factor.value ?: "当前值未提供"

    return when (factor.key) {
        "age" -> AttributionFactorEvidence(
            explanation = "年龄用于校准长期心脑血管风险，当前健康画像记录为 $value。",
            recommendation = "每年核对一次年龄与健康基线资料",
        )
        "gender" -> AttributionFactorEvidence(
            explanation = "性别是风险规则的基础校准变量，会与血压、代谢和生活方式指标共同计算；当前记录为 $value。",
            recommendation = "如档案信息有误，请及时更新健康画像",
        )
        "bmi" -> AttributionFactorEvidence(
            explanation = "BMI 由身高与体重计算，用于观察体重变化与血压、血糖和血脂风险的关系；当前值为 $value。",
            recommendation = "保持均衡饮食，并持续关注体重与腰围趋势",
        )
        "sbp" -> AttributionFactorEvidence(
            explanation = "收缩压反映心脏收缩时的动脉压力，本次规则使用的经确认输入为 $value。",
            recommendation = "使用合格上臂袖带，测量前静坐 5 分钟并尽量固定测量时间",
        )
        "dbp" -> AttributionFactorEvidence(
            explanation = "舒张压反映心脏舒张时的动脉压力，会与收缩压共同参与血压相关风险计算；当前值为 $value。",
            recommendation = "继续记录同一条件下的血压，并保持规律睡眠和适度运动",
        )
        "fasting_glucose" -> AttributionFactorEvidence(
            explanation = "空腹血糖用于观察近期糖代谢状态，本次规则使用的经确认报告值为 $value。",
            recommendation = "复查时尽量保持相同空腹条件，异常结果请咨询医生",
        )
        "total_cholesterol" -> AttributionFactorEvidence(
            explanation = "总胆固醇反映血脂总体水平，需要结合 LDL、HDL 和甘油三酯综合查看；当前值为 $value。",
            recommendation = "减少高油和高饱和脂肪食物，并按医生建议复查血脂",
        )
        "ldl" -> AttributionFactorEvidence(
            explanation = "LDL 胆固醇是血脂风险评估的重要指标，本次规则使用的经确认报告值为 $value。",
            recommendation = "增加膳食纤维、减少反式脂肪，并遵循医生的复查安排",
        )
        "hdl" -> AttributionFactorEvidence(
            explanation = "HDL 胆固醇会与其他血脂指标一起参与风险解释，也会受到运动、体重和饮食结构影响；当前值为 $value。",
            recommendation = "在身体允许时保持规律有氧活动，并持续管理体重",
        )
        "triglycerides" -> AttributionFactorEvidence(
            explanation = "甘油三酯容易受到近期饮食、饮酒和运动影响，本次规则使用的经确认报告值为 $value。",
            recommendation = "控制精制碳水和饮酒，尽量避免夜间加餐",
        )
        "exercise_days" -> AttributionFactorEvidence(
            explanation = "每周运动天数由近 7 日本机活动记录汇总，用于反映规律活动对风险趋势的支持；当前值为 $value。",
            recommendation = "在身体允许时保持每周规律活动，并循序渐进增加时长",
        )
        "smoking" -> AttributionFactorEvidence(
            explanation = "吸烟状态是可干预的心血管风险因素，本次健康画像记录为 $value。",
            recommendation = "不吸烟者继续保持；吸烟者可寻求专业戒烟支持",
        )
        "drinking" -> AttributionFactorEvidence(
            explanation = "饮酒状态会与血压、睡眠和甘油三酯等指标共同影响风险解释；当前记录为 $value。",
            recommendation = "控制饮酒频率，避免短时间大量饮酒",
        )
        "diabetes_history" -> AttributionFactorEvidence(
            explanation = "糖尿病史用于校准长期糖代谢和心血管风险背景，当前健康画像记录为 $value。",
            recommendation = "按个人风险和医生建议复查空腹血糖或糖化血红蛋白",
        )
        "hypertension_history" -> AttributionFactorEvidence(
            explanation = "高血压史用于校准长期血压风险背景，当前健康画像记录为 $value；家庭记录不能替代临床诊断。",
            recommendation = "使用合格上臂袖带持续记录，反复异常时及时咨询医生",
        )
        "family_history" -> AttributionFactorEvidence(
            explanation = "心血管家族史用于校准先天风险背景，当前健康画像记录为 $value。",
            recommendation = "有新的家族健康信息时及时补充，必要时与医生讨论筛查时间",
        )
        else -> AttributionFactorEvidence(
            explanation = "${factor.label}参与本次 Factor16 规则计算，当前值为 $value。",
            recommendation = "持续记录可验证数据，并按专业人员建议复查",
        )
    }
}
