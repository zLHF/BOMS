package com.boms.modules.importer.listener;

import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.read.listener.ReadListener;
import com.boms.common.tenant.TenantContext;
import com.boms.modules.customer.entity.Customer;
import com.boms.modules.customer.mapper.CustomerMapper;
import com.boms.modules.importer.dto.OpportunityExcelRow;
import com.boms.modules.importer.entity.ImportTask;
import com.boms.modules.importer.mapper.ImportTaskMapper;
import com.boms.modules.opportunity.dto.OpportunityReq;
import com.boms.modules.opportunity.entity.Opportunity;
import com.boms.modules.opportunity.mapper.OpportunityMapper;
import com.boms.modules.opportunity.entity.OpportunityStage;
import com.boms.modules.opportunity.mapper.OpportunityStageMapper;
import com.boms.modules.system.entity.SysUser;
import com.boms.modules.system.mapper.SysUserMapper;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** EasyExcel 商机导入 ReadListener。逐行解析并写入数据库。 */
@Slf4j
public class OpportunityImportListener implements ReadListener<OpportunityExcelRow> {

    private final ImportTask importTask;
    private final ImportTaskMapper taskMapper;
    private final OpportunityMapper oppMapper;
    private final CustomerMapper customerMapper;
    private final OpportunityStageMapper stageMapper;
    private final SysUserMapper userMapper;

    private int total = 0;
    private int success = 0;
    private int failed = 0;
    private final List<String> errors = new ArrayList<>();

    public OpportunityImportListener(ImportTask importTask, ImportTaskMapper taskMapper,
                                     OpportunityMapper oppMapper, CustomerMapper customerMapper,
                                     OpportunityStageMapper stageMapper, SysUserMapper userMapper) {
        this.importTask = importTask;
        this.taskMapper = taskMapper;
        this.oppMapper = oppMapper;
        this.customerMapper = customerMapper;
        this.stageMapper = stageMapper;
        this.userMapper = userMapper;
    }

    @Override
    public void invoke(OpportunityExcelRow row, AnalysisContext context) {
        total++;
        int rowNum = total + 1; // +1 for header
        try {
            // 校验必填字段
            if (row.getTitle() == null || row.getTitle().isBlank()) {
                errors.add("第" + rowNum + "行：商机标题不能为空");
                failed++;
                return;
            }
            if (row.getCustomerName() == null || row.getCustomerName().isBlank()) {
                errors.add("第" + rowNum + "行：客户名称不能为空");
                failed++;
                return;
            }

            // 匹配客户（按名称查找当前租户下的客户）
            Customer customer = customerMapper.selectOne(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Customer>()
                            .eq(Customer::getTenantId, importTask.getTenantId())
                            .eq(Customer::getName, row.getCustomerName())
                            .last("LIMIT 1"));
            if (customer == null) {
                errors.add("第" + rowNum + "行：未找到客户「" + row.getCustomerName() + "」");
                failed++;
                return;
            }

            // 获取第一阶段
            List<OpportunityStage> stages = stageMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<OpportunityStage>()
                            .eq(OpportunityStage::getTenantId, importTask.getTenantId())
                            .eq(OpportunityStage::getIsActive, 1)
                            .eq(OpportunityStage::getStageType, "IN_PROGRESS")
                            .orderByAsc(OpportunityStage::getSort));
            if (stages.isEmpty()) {
                errors.add("第" + rowNum + "行：租户未配置商机阶段");
                failed++;
                return;
            }
            OpportunityStage firstStage = stages.get(0);

            // 查找操作人信息
            SysUser operator = userMapper.selectById(importTask.getOperatorId());

            // 创建商机
            Opportunity opp = new Opportunity();
            opp.setTenantId(importTask.getTenantId());
            opp.setTitle(row.getTitle());
            opp.setCustomerId(customer.getId());
            opp.setStageId(firstStage.getId());
            opp.setStatus("IN_PROGRESS");
            opp.setWinRate(firstStage.getWinRate());
            opp.setAmount(row.getAmount() != null ? row.getAmount() : BigDecimal.ZERO);
            opp.setSource(row.getSource());
            opp.setDemand(row.getDemand());
            opp.setIsPool(0);
            opp.setOwnerId(importTask.getOperatorId());
            if (operator != null) {
                opp.setDeptId(operator.getDeptId());
            }
            oppMapper.insert(opp);
            success++;
        } catch (Exception e) {
            errors.add("第" + rowNum + "行：" + e.getMessage());
            failed++;
            log.warn("[Import] 第{}行处理失败: {}", rowNum, e.getMessage());
        }
    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext context) {
        importTask.setTotal(total);
        importTask.setSuccess(success);
        importTask.setFailed(failed);
        importTask.setStatus(errors.isEmpty() && failed == 0 ? "DONE" : "DONE");
        taskMapper.updateById(importTask);
        log.info("[Import] 导入完成 taskId={} total={} success={} failed={}",
                importTask.getId(), total, success, failed);
    }

    public List<String> getErrors() {
        return errors;
    }
}
