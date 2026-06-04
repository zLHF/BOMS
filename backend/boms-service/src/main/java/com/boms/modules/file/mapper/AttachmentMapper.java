package com.boms.modules.file.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.boms.modules.file.entity.Attachment;
import org.apache.ibatis.annotations.Mapper;

/** 附件 Mapper。 */
@Mapper
public interface AttachmentMapper extends BaseMapper<Attachment> {
}
