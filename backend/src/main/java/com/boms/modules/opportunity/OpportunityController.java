package com.boms.modules.opportunity;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.boms.common.audit.AuditLog;
import com.boms.common.result.R;
import com.boms.common.security.RequirePerm;
import com.boms.modules.customer.entity.Contact;
import com.boms.modules.opportunity.dto.*;
import com.boms.modules.opportunity.entity.Opportunity;
import com.boms.modules.opportunity.entity.OpportunityCollaborator;
import com.boms.modules.opportunity.entity.OpportunityFollow;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 商机 M05/M06/M07/M08/M09。 */
@RestController
@RequestMapping("/api/opportunities")
public class OpportunityController {

    private final OpportunityService service;

    public OpportunityController(OpportunityService service) {
        this.service = service;
    }

    /** 多视图：all/mine/sub/won/pool。 */
    @GetMapping
    @RequirePerm("opp:view")
    public R<IPage<Opportunity>> list(@RequestParam(defaultValue = "all") String view,
                                      @RequestParam(required = false) String keyword,
                                      @RequestParam(required = false) Long stageId,
                                      @RequestParam(defaultValue = "1") long page,
                                      @RequestParam(defaultValue = "20") long size) {
        return R.ok(service.list(view, keyword, stageId, page, size));
    }

    @GetMapping("/{id}")
    @RequirePerm("opp:view")
    public R<Opportunity> get(@PathVariable Long id) {
        return R.ok(service.get(id));
    }

    /** M08 富化详情（含客户名/阶段名/负责人名）。 */
    @GetMapping("/{id}/detail")
    @RequirePerm("opp:view")
    public R<OpportunityDetailVO> getDetail(@PathVariable Long id) {
        return R.ok(service.getDetail(id));
    }

    @PostMapping
    @RequirePerm("opp:create")
    @AuditLog(action = "opp:create", objectType = "opportunity")
    public R<Opportunity> create(@Valid @RequestBody OpportunityReq req) {
        return R.ok(service.create(req));
    }

    @PutMapping("/{id}")
    @RequirePerm("opp:update")
    @AuditLog(action = "opp:update", objectType = "opportunity")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody OpportunityReq req) {
        service.update(id, req);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    @RequirePerm("opp:update")
    @AuditLog(action = "opp:delete", objectType = "opportunity")
    public R<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return R.ok();
    }

    @PostMapping("/{id}/transfer")
    @RequirePerm("opp:transfer")
    @AuditLog(action = "opp:transfer", objectType = "opportunity")
    public R<Void> transfer(@PathVariable Long id, @Valid @RequestBody TransferReq req) {
        service.transfer(id, req.ownerId());
        return R.ok();
    }

    @PostMapping("/{id}/stage")
    @RequirePerm("opp:stage:advance")
    @AuditLog(action = "opp:stage:advance", objectType = "opportunity")
    public R<Void> moveStage(@PathVariable Long id, @Valid @RequestBody StageMoveReq req) {
        service.moveStage(id, req.stageId());
        return R.ok();
    }

    @PostMapping("/{id}/stage/rollback")
    @RequirePerm("opp:stage:rollback")
    @AuditLog(action = "opp:stage:rollback", objectType = "opportunity")
    public R<Void> rollback(@PathVariable Long id, @Valid @RequestBody StageMoveReq req) {
        service.rollback(id, req.stageId());
        return R.ok();
    }

    @PostMapping("/{id}/win")
    @RequirePerm("opp:win")
    @AuditLog(action = "opp:win", objectType = "opportunity")
    public R<Void> win(@PathVariable Long id, @Valid @RequestBody WinReq req) {
        service.win(id, req);
        return R.ok();
    }

    @PostMapping("/{id}/lose")
    @RequirePerm("opp:lose")
    @AuditLog(action = "opp:lose", objectType = "opportunity")
    public R<Void> lose(@PathVariable Long id, @Valid @RequestBody LoseReq req) {
        service.lose(id, req);
        return R.ok();
    }

    /* ---------- 跟进 ---------- */

    @GetMapping("/{id}/follows")
    @RequirePerm("opp:view")
    public R<List<OpportunityFollow>> follows(@PathVariable Long id) {
        return R.ok(service.follows(id));
    }

    @PostMapping("/{id}/follows")
    @RequirePerm("opp:follow:create")
    @AuditLog(action = "opp:follow:create", objectType = "opportunity")
    public R<OpportunityFollow> addFollow(@PathVariable Long id, @Valid @RequestBody FollowReq req) {
        return R.ok(service.addFollow(id, req));
    }

    /* ---------- 联系人（代理到客户联系人） ---------- */

    @GetMapping("/{id}/contacts")
    @RequirePerm("opp:view")
    public R<List<Contact>> contacts(@PathVariable Long id) {
        return R.ok(service.contacts(id));
    }

    /* ---------- 协作人（M09） ---------- */

    @GetMapping("/{id}/collaborators")
    @RequirePerm("opp:view")
    public R<List<OpportunityCollaborator>> collaborators(@PathVariable Long id) {
        return R.ok(service.collaborators(id));
    }

    @PostMapping("/{id}/collaborators")
    @RequirePerm("opp:collab:add")
    @AuditLog(action = "opp:collab:add", objectType = "opportunity")
    public R<OpportunityCollaborator> addCollaborator(@PathVariable Long id,
                                                       @Valid @RequestBody CollaboratorAddReq req) {
        return R.ok(service.addCollaborator(id, req.userId(), req.permissionJson()));
    }

    @DeleteMapping("/{id}/collaborators/{cid}")
    @RequirePerm("opp:collab:remove")
    @AuditLog(action = "opp:collab:remove", objectType = "opportunity")
    public R<Void> removeCollaborator(@PathVariable Long id, @PathVariable Long cid) {
        service.removeCollaborator(id, cid);
        return R.ok();
    }
}
