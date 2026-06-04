package com.boms.modules.importer;

import com.alibaba.excel.EasyExcel;
import com.boms.common.audit.AuditLog;
import com.boms.common.result.R;
import com.boms.common.security.RequirePerm;
import com.boms.modules.importer.dto.ImportProgressResp;
import com.boms.modules.importer.dto.OpportunityExcelRow;
import com.boms.modules.importer.entity.ImportTask;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/** 批量导入 M06。 */
@RestController
@RequestMapping("/api/opportunities/import")
public class ImportController {

    private final ImportService importService;

    @Value("${boms.import.max-file-size-bytes:10485760}")
    private long maxFileSizeBytes;

    public ImportController(ImportService importService) {
        this.importService = importService;
    }

    /** 下载导入模板。 */
    @GetMapping("/template")
    @RequirePerm("opp:import")
    public void downloadTemplate(HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition",
                "attachment;filename=" + URLEncoder.encode("商机导入模板.xlsx", StandardCharsets.UTF_8));
        EasyExcel.write(response.getOutputStream(), OpportunityExcelRow.class).sheet("商机数据").doWrite(java.util.Collections.emptyList());
    }

    /** 上传 Excel 开始导入。 */
    @PostMapping
    @RequirePerm("opp:import")
    @AuditLog(action = "opp:import", objectType = "opportunity")
    public R<ImportProgressResp> startImport(@RequestParam("file") MultipartFile file) throws IOException {
        ImportFileValidator.validate(file.getOriginalFilename(), file.getSize(), maxFileSizeBytes);
        Long taskId = importService.startImport(file);
        return R.ok(new ImportProgressResp(taskId, "RUNNING", 0, 0, 0));
    }

    /** 查询导入进度。 */
    @GetMapping("/{taskId}")
    @RequirePerm("opp:import")
    public R<ImportProgressResp> getProgress(@PathVariable Long taskId) {
        ImportTask task = importService.getProgress(taskId);
        return R.ok(new ImportProgressResp(task.getId(), task.getStatus(),
                task.getTotal(), task.getSuccess(), task.getFailed()));
    }
}
