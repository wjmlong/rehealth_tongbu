package org.jeecg.modules.rehealth.vo;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;

import java.util.Date;

/**
 * 聚合视图对象：注册用户基础信息 + 来自 device-service 的健康/提取数据摘要。
 * health 字段为 device-service 返回的 UserHealthSummary 原始 JSON，结构见 device-service。
 */
@Data
public class RehealthUserHealthVO {
    private String id;
    private String username;
    private String realname;
    private String phone;
    private String email;
    private Integer sex;
    private Integer status;
    private Date createTime;
    private JSONObject health;
}
