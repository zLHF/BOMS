package com.boms.modules.opportunity;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.boms.common.result.R;
import com.boms.common.security.RequirePerm;
import com.boms.modules.opportunity.dto.FollowListVO;
import com.boms.modules.opportunity.entity.Opportunity;
import com.boms.modules.opportunity.entity.OpportunityFollow;
import com.boms.modules.opportunity.mapper.OpportunityMapper;
import com.boms.modules.opportunity.mapper.OpportunityFollowMapper;
import com.boms.modules.system.entity.SysUser;
import com.boms.modules.system.mapper.SysUserMapper;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/** 跟进记录独立页（跨商机全局视图）。 */
@RestController
@RequestMapping("/api/follows")
public class FollowController {

    private final OpportunityService service;
    private final OpportunityMapper oppMapper;
    private final SysUserMapper userMapper;

    public FollowController(OpportunityService service, OpportunityMapper oppMapper, SysUserMapper userMapper) {
        this.service = service;
        this.oppMapper = oppMapper;
        this.userMapper = userMapper;
    }

    @GetMapping
    @RequirePerm("opp:view")
    public R<IPage<FollowListVO>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String followType,
            @RequestParam(required = false) Long creatorId,
            @RequestParam(required = false) String start,
            @RequestParam(required = false) String end,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size) {

        IPage<OpportunityFollow> raw = service.listAllFollows(keyword, followType, creatorId, start, end, page, size);

        // 批量加载关联的商机标题
        Set<Long> oppIds = raw.getRecords().stream()
                .map(OpportunityFollow::getOpportunityId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> oppTitleMap = new HashMap<>();
        if (!oppIds.isEmpty()) {
            oppMapper.selectBatchIds(oppIds).forEach(o -> oppTitleMap.put(o.getId(), o.getTitle()));
        }

        // 批量加载创建人姓名
        Set<Long> creatorIds = raw.getRecords().stream()
                .map(OpportunityFollow::getCreatorId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> creatorNameMap = new HashMap<>();
        if (!creatorIds.isEmpty()) {
            userMapper.selectBatchIds(creatorIds).forEach(u ->
                    creatorNameMap.put(u.getId(), u.getRealName() != null ? u.getRealName() : u.getUsername()));
        }

        // 转换为 VO
        IPage<FollowListVO> voPage = new Page<>(raw.getCurrent(), raw.getSize(), raw.getTotal());
        voPage.setRecords(raw.getRecords().stream()
                .map(f -> FollowListVO.from(f, oppTitleMap.getOrDefault(f.getOpportunityId(), "-"),
                        creatorNameMap.getOrDefault(f.getCreatorId(), "-")))
                .collect(Collectors.toList()));
        return R.ok(voPage);
    }
}
