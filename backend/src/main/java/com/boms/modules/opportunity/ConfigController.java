package com.boms.modules.opportunity;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.boms.common.audit.AuditLog;
import com.boms.common.exception.BizException;
import com.boms.common.result.R;
import com.boms.common.security.RequirePerm;
import com.boms.modules.opportunity.dto.NumberRuleUpdateReq;
import com.boms.modules.opportunity.dto.StageUpdateReq;
import com.boms.modules.opportunity.entity.NumberRule;
import com.boms.modules.opportunity.entity.OpportunityStage;
import com.boms.modules.opportunity.mapper.NumberRuleMapper;
import com.boms.modules.opportunity.mapper.OpportunityStageMapper;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 系统配置 M19（核心）：阶段字典、编号规则。 */
@RestController
@RequestMapping("/api/config")
public class ConfigController {

    private final OpportunityService opportunityService;
    private final OpportunityStageMapper stageMapper;
    private final NumberRuleMapper numberRuleMapper;

    public ConfigController(OpportunityService opportunityService,
                            OpportunityStageMapper stageMapper,
                            NumberRuleMapper numberRuleMapper) {
        this.opportunityService = opportunityService;
        this.stageMapper = stageMapper;
        this.numberRuleMapper = numberRuleMapper;
    }

    /* ---------- 阶段配置 ---------- */

    @GetMapping("/stages")
    public R<List<OpportunityStage>> stages() {
        return R.ok(opportunityService.stages());
    }

    @PutMapping("/stages/{id}")
    @RequirePerm("config:stage")
    @AuditLog(action = "config:stage:update", objectType = "config")
    public R<Void> updateStage(@PathVariable Long id, @Valid @RequestBody StageUpdateReq req) {
        OpportunityStage stage = stageMapper.selectById(id);
        if (stage == null) throw new BizException(40400, "阶段不存在");
        if (req.name() != null) stage.setName(req.name());
        if (req.winRate() != null) stage.setWinRate(req.winRate());
        if (req.color() != null) stage.setColor(req.color());
        if (req.isActive() != null) stage.setIsActive(req.isActive());
        stageMapper.updateById(stage);
        return R.ok();
    }

    /* ---------- 编号规则 ---------- */

    @GetMapping("/number-rules")
    @RequirePerm("config:number")
    public R<List<NumberRule>> numberRules() {
        return R.ok(numberRuleMapper.selectList(
                new LambdaQueryWrapper<NumberRule>().orderByAsc(NumberRule::getBizType)));
    }

    @PutMapping("/number-rules/{bizType}")
    @RequirePerm("config:number")
    @AuditLog(action = "config:number:update", objectType = "config")
    public R<Void> updateNumberRule(@PathVariable String bizType, @Valid @RequestBody NumberRuleUpdateReq req) {
        NumberRule rule = numberRuleMapper.selectOne(
                new LambdaQueryWrapper<NumberRule>().eq(NumberRule::getBizType, bizType));
        if (rule == null) throw new BizException(40400, "编号规则不存在：" + bizType);
        if (req.prefix() != null) rule.setPrefix(req.prefix());
        if (req.dateFormat() != null) rule.setDateFormat(req.dateFormat());
        if (req.seqLength() != null) rule.setSeqLength(req.seqLength());
        numberRuleMapper.updateById(rule);
        return R.ok();
    }
}
