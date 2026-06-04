package com.boms.modules.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/** 平台级套餐/容量（D8）。 */
@Data
@TableName("sys_package")
public class SysPackage {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private Integer maxUsers;
    private Integer maxOpportunities;
    private Integer maxCustomers;
    private Integer maxAttachmentGb;
    private Integer maxSms;
    private Integer maxApiDaily;
    private Integer maxFileMb;
    private Integer maxImportRows;

    @TableLogic
    private Integer deleted;
}
