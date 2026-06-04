package com.boms.modules.importer;

import com.alibaba.excel.EasyExcel;
import com.boms.common.tenant.TenantContext;
import com.boms.modules.customer.mapper.CustomerMapper;
import com.boms.modules.importer.dto.OpportunityExcelRow;
import com.boms.modules.importer.entity.ImportTask;
import com.boms.modules.importer.listener.OpportunityImportListener;
import com.boms.modules.importer.mapper.ImportTaskMapper;
import com.boms.modules.opportunity.mapper.OpportunityMapper;
import com.boms.modules.opportunity.mapper.OpportunityStageMapper;
import com.boms.modules.system.mapper.SysUserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.CompletableFuture;

/** 导入服务。 */
@Slf4j
@Service
public class ImportService {

    private final ImportTaskMapper taskMapper;
    private final OpportunityMapper oppMapper;
    private final CustomerMapper customerMapper;
    private final OpportunityStageMapper stageMapper;
    private final SysUserMapper userMapper;

    public ImportService(ImportTaskMapper taskMapper, OpportunityMapper oppMapper,
                         CustomerMapper customerMapper, OpportunityStageMapper stageMapper,
                         SysUserMapper userMapper) {
        this.taskMapper = taskMapper;
        this.oppMapper = oppMapper;
        this.customerMapper = customerMapper;
        this.stageMapper = stageMapper;
        this.userMapper = userMapper;
    }

    /** 创建导入任务并异步执行。 */
    public Long startImport(MultipartFile file) throws IOException {
        Long tid = TenantContext.tenantId();
        Long userId = TenantContext.userId();

        ImportTask task = new ImportTask();
        task.setTenantId(tid);
        task.setBizType("opportunity");
        task.setFileName(file.getOriginalFilename());
        task.setStatus("RUNNING");
        task.setTotal(0);
        task.setSuccess(0);
        task.setFailed(0);
        task.setOperatorId(userId);
        taskMapper.insert(task);

        byte[] content = file.getBytes();
        CompletableFuture.runAsync(() -> executeImport(task, content));
        return task.getId();
    }

    private void executeImport(ImportTask task, byte[] content) {
        TenantContext.set(new TenantContext.Principal(task.getTenantId(), task.getOperatorId(), "importer"));
        try (InputStream is = new ByteArrayInputStream(content)) {
            OpportunityImportListener listener = new OpportunityImportListener(
                    task, taskMapper, oppMapper, customerMapper, stageMapper, userMapper);
            EasyExcel.read(is, OpportunityExcelRow.class, listener).sheet().doRead();
        } catch (Exception e) {
            task.setStatus("FAILED");
            taskMapper.updateById(task);
            log.error("[Import] 导入失败 taskId={}: {}", task.getId(), e.getMessage(), e);
        } finally {
            TenantContext.clear();
        }
    }

    public ImportTask getProgress(Long taskId) {
        ImportTask task = taskMapper.selectById(taskId);
        if (task == null) throw new com.boms.common.exception.BizException(40400, "导入任务不存在");
        if (!task.getTenantId().equals(TenantContext.tenantId())) {
            throw new com.boms.common.exception.BizException(40310, "无权查看此任务");
        }
        return task;
    }
}
