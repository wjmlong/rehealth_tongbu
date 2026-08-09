package org.jeecg.modules.rehealth.vo;

import lombok.Data;

import java.util.List;

@Data
public class RehealthPatientPageVO {
    private List<RehealthUserHealthVO> records;
    private long total;
    private int pageNo;
    private int pageSize;
    private long totalPages;
}
