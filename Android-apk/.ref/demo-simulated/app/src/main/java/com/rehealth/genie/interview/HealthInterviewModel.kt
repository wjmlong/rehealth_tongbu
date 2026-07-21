package com.rehealth.genie.interview

enum class InterviewTopic(val label: String) {
    PROFILE("基本资料"),
    MEDICAL("健康史"),
    MEDICATION("用药与过敏"),
    SLEEP("睡眠"),
    ACTIVITY("活动"),
    DIET("饮食"),
    STRESS("压力"),
    GOAL("健康目标"),
}

data class InterviewQuestion(
    val id: String,
    val topic: InterviewTopic,
    val text: String,
    val helper: String,
    val quickReplies: List<String> = emptyList(),
)

data class InterviewAnswer(
    val question: InterviewQuestion,
    val content: String,
)

data class HealthBaseline(
    val items: List<BaselineItem>,
    val focusAreas: List<String>,
    val generatedAt: Long = System.currentTimeMillis(),
)

data class BaselineItem(
    val label: String,
    val value: String,
)

interface HealthInterviewModel {
    val questions: List<InterviewQuestion>
    fun buildBaseline(answers: List<InterviewAnswer>): HealthBaseline
}

class MockHealthInterviewModel : HealthInterviewModel {
    override val questions = listOf(
        InterviewQuestion(
            id = "profile",
            topic = InterviewTopic.PROFILE,
            text = "先简单认识一下你。请告诉我你的年龄、身高和体重。",
            helper = "例如：32 岁，身高 168 cm，体重 62 kg",
        ),
        InterviewQuestion(
            id = "medical",
            topic = InterviewTopic.MEDICAL,
            text = "你是否有已确诊的慢性病，或近期经常出现的不适？",
            helper = "如高血压、糖尿病、心悸、头晕；没有也可以直接说没有",
            quickReplies = listOf("目前没有", "有高血压", "有血糖问题"),
        ),
        InterviewQuestion(
            id = "medication",
            topic = InterviewTopic.MEDICATION,
            text = "目前有长期服用的药物、保健品，或明确的过敏吗？",
            helper = "药物名称、服用频率和过敏原都可以告诉我",
            quickReplies = listOf("没有长期用药", "有长期用药", "有过敏史"),
        ),
        InterviewQuestion(
            id = "sleep",
            topic = InterviewTopic.SLEEP,
            text = "最近两周，你通常几点睡、几点起？入睡和夜间睡眠怎么样？",
            helper = "可以描述睡眠时长、入睡困难、夜醒或打鼾",
            quickReplies = listOf("睡眠基本规律", "经常晚睡", "容易夜醒"),
        ),
        InterviewQuestion(
            id = "activity",
            topic = InterviewTopic.ACTIVITY,
            text = "你一周通常运动几次？日常久坐时间大概有多久？",
            helper = "散步、跑步、力量训练和通勤活动都算",
            quickReplies = listOf("每周 3 次以上", "偶尔运动", "基本不运动"),
        ),
        InterviewQuestion(
            id = "diet",
            topic = InterviewTopic.DIET,
            text = "你的三餐规律吗？平时盐、糖、酒精和饮水情况如何？",
            helper = "不用精确计算，说说你平常的习惯即可",
            quickReplies = listOf("饮食比较规律", "常吃外卖", "经常不吃早餐"),
        ),
        InterviewQuestion(
            id = "stress",
            topic = InterviewTopic.STRESS,
            text = "最近的压力和情绪状态怎么样？主要压力来自哪里？",
            helper = "工作、家庭、睡眠和身体不适都可能影响压力",
            quickReplies = listOf("压力较低", "有一些压力", "压力比较大"),
        ),
        InterviewQuestion(
            id = "goal",
            topic = InterviewTopic.GOAL,
            text = "最后，你最希望小禾灵先帮你改善哪一件健康问题？",
            helper = "这会成为你的首要健康目标，之后可以随时修改",
            quickReplies = listOf("改善睡眠", "控制体重", "提升体能", "管理血压"),
        ),
    )

    override fun buildBaseline(answers: List<InterviewAnswer>): HealthBaseline {
        val items = answers.map { answer ->
            BaselineItem(answer.question.topic.label, answer.content)
        }
        val focusAreas = buildList {
            answers.forEach { answer ->
                val content = answer.content
                if ("睡" in content || answer.question.topic == InterviewTopic.SLEEP) add("睡眠节律")
                if ("压力" in content || answer.question.topic == InterviewTopic.STRESS) add("压力恢复")
                if ("运动" in content || answer.question.topic == InterviewTopic.ACTIVITY) add("日常活动")
                if ("血压" in content) add("血压趋势")
            }
            add("戒指健康数据持续校准")
        }.distinct().take(4)
        return HealthBaseline(items, focusAreas)
    }
}
