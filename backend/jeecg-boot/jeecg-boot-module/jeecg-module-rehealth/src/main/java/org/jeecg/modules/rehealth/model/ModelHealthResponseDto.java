package org.jeecg.modules.rehealth.model;

import com.alibaba.fastjson.annotation.JSONField;

public class ModelHealthResponseDto {
    public String status;
    public String service;
    @JSONField(name = "model_version")
    public String modelVersion;
}
