package org.jeecg.modules.rehealth.insurance;

/**
 * Three-level data scope for insurance staff reads.
 *
 * <p>{@code null} means unrestricted (organization administrator / auditor);
 * {@code SELF} restricts to assignments owned by {@link #userId()};
 * {@code TEAM} additionally includes assignments owned by any employee who
 * shares a department with {@link #userId()}.
 */
//update-begin---author:ai-agent ---date:2026-08-25  for：【保险侧用户服务关系一期】新增三级数据范围对象-----------
public record InsuranceAssignmentScope(String userId, String mode) {
    public static final String MODE_SELF = "SELF";
    public static final String MODE_TEAM = "TEAM";

    public InsuranceAssignmentScope {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("scope userId is required");
        }
        if (!MODE_SELF.equals(mode) && !MODE_TEAM.equals(mode)) {
            throw new IllegalArgumentException("scope mode must be SELF or TEAM");
        }
    }

    public boolean team() {
        return MODE_TEAM.equals(mode);
    }
}
//update-end---author:ai-agent ---date:2026-08-25  for：【保险侧用户服务关系一期】新增三级数据范围对象-----------
