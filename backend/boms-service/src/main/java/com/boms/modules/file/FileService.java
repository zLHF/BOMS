package com.boms.modules.file;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.boms.common.exception.BizException;
import com.boms.common.tenant.TenantContext;
import com.boms.modules.file.dto.AttachmentResp;
import com.boms.modules.file.dto.FileConfirmReq;
import com.boms.modules.file.dto.SignUploadReq;
import com.boms.modules.file.dto.SignUploadResp;
import com.boms.modules.file.entity.Attachment;
import com.boms.modules.file.mapper.AttachmentMapper;
import io.minio.*;
import io.minio.http.Method;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/** 文件/附件服务（MinIO 对象存储）。 */
@Slf4j
@Service
public class FileService {

    private final MinioClient minioClient;
    private final MinioConfig minioConfig;
    private final AttachmentMapper attachmentMapper;

    public FileService(MinioClient minioClient, MinioConfig minioConfig, AttachmentMapper attachmentMapper) {
        this.minioClient = minioClient;
        this.minioConfig = minioConfig;
        this.attachmentMapper = attachmentMapper;
    }

    /** 应用启动时确保 bucket 存在。 */
    @PostConstruct
    public void initBucket() {
        try {
            boolean found = minioClient.bucketExists(BucketExistsArgs.builder()
                    .bucket(minioConfig.getBucket()).build());
            if (!found) {
                minioClient.makeBucket(MakeBucketArgs.builder()
                        .bucket(minioConfig.getBucket()).build());
                log.info("MinIO bucket '{}' created", minioConfig.getBucket());
            }
        } catch (Exception e) {
            log.warn("MinIO bucket init failed: {}", e.getMessage());
        }
    }

    /* ---------- 签名上传 ---------- */

    @Transactional
    public SignUploadResp signUpload(SignUploadReq req) {
        Long tenantId = TenantContext.tenantId();
        Long userId = TenantContext.userId();

        // 生成对象存储路径: tenant_{tenantId}/{objectType}/{objectId}/{uuid}_{fileName}
        String objectKey = String.format("tenant_%d/%s/%d/%s_%s",
                tenantId, req.objectType(), req.objectId(),
                UUID.randomUUID().toString().replace("-", "").substring(0, 8),
                req.fileName());

        // 写附件记录（PENDING 状态）
        Attachment att = new Attachment();
        att.setTenantId(tenantId);
        att.setObjectType(req.objectType());
        att.setObjectId(req.objectId());
        att.setFileName(req.fileName());
        att.setFilePath(objectKey);
        att.setFileSize(req.fileSize());
        att.setContentType(req.contentType());
        att.setStatus("PENDING");
        att.setUploaderId(userId);
        attachmentMapper.insert(att);

        // 生成预签名 PUT URL
        try {
            Map<String, String> reqHeaders = new HashMap<>();
            if (req.contentType() != null && !req.contentType().isBlank()) {
                reqHeaders.put("Content-Type", req.contentType());
            }
            String uploadUrl = minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.PUT)
                    .bucket(minioConfig.getBucket())
                    .object(objectKey)
                    .expiry(10, TimeUnit.MINUTES)
                    .extraHeaders(reqHeaders)
                    .build());

            return new SignUploadResp(att.getId(), uploadUrl, reqHeaders);
        } catch (Exception e) {
            throw new BizException(50010, "生成上传签名失败: " + e.getMessage());
        }
    }

    /* ---------- 确认上传 ---------- */

    @Transactional
    public void confirmUpload(FileConfirmReq req) {
        Attachment att = attachmentMapper.selectById(req.attachmentId());
        if (att == null) throw new BizException(40400, "附件不存在");
        if (!att.getTenantId().equals(TenantContext.tenantId())) {
            throw new BizException(40310, "无权操作此附件");
        }
        att.setStatus("CONFIRMED");
        attachmentMapper.updateById(att);
    }

    /* ---------- 签名下载 ---------- */

    public String signDownload(Long id) {
        Attachment att = attachmentMapper.selectById(id);
        if (att == null) throw new BizException(40400, "附件不存在");
        if (!att.getTenantId().equals(TenantContext.tenantId())) {
            throw new BizException(40310, "无权访问此附件");
        }
        if (!"CONFIRMED".equals(att.getStatus())) {
            throw new BizException(40920, "附件尚未上传完成");
        }
        try {
            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(minioConfig.getBucket())
                    .object(att.getFilePath())
                    .expiry(5, TimeUnit.MINUTES)
                    .build());
        } catch (Exception e) {
            throw new BizException(50010, "生成下载签名失败: " + e.getMessage());
        }
    }

    /* ---------- 删除 ---------- */

    @Transactional
    public void delete(Long id) {
        Attachment att = attachmentMapper.selectById(id);
        if (att == null) throw new BizException(40400, "附件不存在");
        if (!att.getTenantId().equals(TenantContext.tenantId())) {
            throw new BizException(40310, "无权删除此附件");
        }
        // 逻辑删除（@TableLogic 自动处理）
        attachmentMapper.deleteById(id);

        // 尝试清理 MinIO 对象（非关键，失败不影响业务）
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(minioConfig.getBucket())
                    .object(att.getFilePath())
                    .build());
        } catch (Exception e) {
            log.warn("MinIO object removal failed for {}: {}", att.getFilePath(), e.getMessage());
        }
    }

    /* ---------- 按业务对象查询附件列表 ---------- */

    public List<AttachmentResp> listByObject(String objectType, Long objectId) {
        LambdaQueryWrapper<Attachment> w = new LambdaQueryWrapper<>();
        w.eq(Attachment::getTenantId, TenantContext.tenantId());
        w.eq(Attachment::getObjectType, objectType);
        w.eq(Attachment::getObjectId, objectId);
        w.eq(Attachment::getStatus, "CONFIRMED");
        w.orderByDesc(Attachment::getCreatedAt);
        return attachmentMapper.selectList(w).stream()
                .map(this::toResp)
                .collect(Collectors.toList());
    }

    private AttachmentResp toResp(Attachment a) {
        return new AttachmentResp(
                a.getId(), a.getFileName(), a.getFileSize(), a.getContentType(),
                a.getObjectType(), a.getObjectId(), a.getStatus(), a.getUploaderId(),
                a.getCreatedAt() != null ? a.getCreatedAt().toString() : null
        );
    }
}
