package com.boms.modules.importer.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

import java.math.BigDecimal;

/** 商机导入 Excel 行模型。 */
@Data
public class OpportunityExcelRow {
    @ExcelProperty("商机标题")
    private String title;

    @ExcelProperty("客户名称")
    private String customerName;

    @ExcelProperty("金额")
    private BigDecimal amount;

    @ExcelProperty("来源")
    private String source;

    @ExcelProperty("需求描述")
    private String demand;

    @ExcelProperty("预计结单日期")
    private String expectedCloseAt;
}
