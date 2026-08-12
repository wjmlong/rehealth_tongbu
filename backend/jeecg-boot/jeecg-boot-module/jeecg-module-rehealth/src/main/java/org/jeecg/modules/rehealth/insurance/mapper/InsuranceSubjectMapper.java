package org.jeecg.modules.rehealth.insurance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.jeecg.modules.rehealth.insurance.entity.InsuranceSubjectEntity;

@Mapper
public interface InsuranceSubjectMapper extends BaseMapper<InsuranceSubjectEntity> {
    @Select("""
            SELECT COUNT(*)
            FROM sys_user_tenant membership
            JOIN sys_user account
              ON account.id = CONVERT(membership.user_id USING utf8mb3) COLLATE utf8mb3_general_ci
            WHERE membership.tenant_id = #{tenantId}
              AND membership.user_id = #{userId}
              AND membership.status = '1'
              AND account.status = 1
              AND account.del_flag = 0
            """)
    int countActiveMember(@Param("tenantId") int tenantId, @Param("userId") String userId);
}
