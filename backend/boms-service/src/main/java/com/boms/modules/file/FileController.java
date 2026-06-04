package com.boms.modules.file;

import com.boms.common.audit.AuditLog;
import com.boms.common.result.R;
import com.boms.common.security.RequirePerm;
import com.boms.modules.file.dto.*;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 文件/附件管理 M16。 */
@RestController
@RequestMapping("/api/files")
public class FileController {

    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    /** 获取预签名上传 URL（前端直传 MinIO）。 */
    @PostMapping("/sign-upload")
    @RequirePerm("file:upload")
    @AuditLog(action = "file:sign-upload", objectType = "attachment")
    public R<SignUploadResp> signUpload(@Valid @RequestBody SignUploadReq req) {
        return R.ok(fileService.signUpload(req));
    }

    /** 确认上传完成。 */
    @PostMapping
    @RequirePerm("file:upload")
    @AuditLog(action = "file:confirm", objectType = "attachment")
    public R<Void> confirmUpload(@Valid @RequestBody FileConfirmReq req) {
        fileService.confirmUpload(req);
        return R.ok();
    }

    /** 获取预签名下载 URL。 */
    @GetMapping("/{id}/sign-download")
    @RequirePerm("file:download")
    @AuditLog(action = "file:sign-download", objectType = "attachment")
    public R<String> signDownload(@PathVariable Long id) {
        return R.ok(fileService.signDownload(id));
    }

    /** 删除附件。 */
    @DeleteMapping("/{id}")
    @RequirePerm("file:delete")
    @AuditLog(action = "file:delete", objectType = "attachment")
    public R<Void> delete(@PathVariable Long id) {
        fileService.delete(id);
        return R.ok();
    }

    /** 按业务对象查询附件列表。 */
    @GetMapping
    @RequirePerm("file:download")
    public R<List<AttachmentResp>> list(
            @RequestParam String objectType,
            @RequestParam Long objectId) {
        return R.ok(fileService.listByObject(objectType, objectId));
    }
}
