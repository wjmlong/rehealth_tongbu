package org.jeecg.modules.rehealth.insurance;

/**
 * Request/response payloads for the insurance plan catalog (product-level
 * health programs referenced by policy default_plan_id).
 */
//update-begin---author:ai-agent ---date:2026-08-25  for：【保险侧用户服务关系一期】新增计划目录接口对象-----------
public final class InsurancePlanCatalog {
    private InsurancePlanCatalog() {
    }

    public record Plan(
            String planId,
            String name,
            String description,
            String status
    ) {
    }

    public record CreateRequest(
            String planId,
            String name,
            String description
    ) {
    }
}
//update-end---author:ai-agent ---date:2026-08-25  for：【保险侧用户服务关系一期】新增计划目录接口对象-----------
