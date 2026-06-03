package com.boms.modules.pool;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.boms.common.audit.AuditLog;
import com.boms.common.result.R;
import com.boms.common.security.RequirePerm;
import com.boms.modules.customer.entity.Customer;
import com.boms.modules.opportunity.entity.Opportunity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** 公海池 D5（V1.0 手动回收/认领）。 */
@RestController
@RequestMapping("/api/pool")
public class PoolController {

    private final PoolService poolService;

    public PoolController(PoolService poolService) {
        this.poolService = poolService;
    }

    /* ---------- 商机公海 ---------- */

    @GetMapping("/opportunities")
    @RequirePerm("opp:view")
    public R<IPage<Opportunity>> listOpportunities(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long stageId,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size) {
        return R.ok(poolService.listPoolOpportunities(keyword, stageId, page, size));
    }

    @PostMapping("/opportunities/{id}/claim")
    @RequirePerm("opp:create")
    @AuditLog(action = "pool:opp:claim", objectType = "opportunity")
    public R<Void> claimOpportunity(@PathVariable Long id) {
        poolService.claimOpportunity(id);
        return R.ok();
    }

    @PostMapping("/opportunities/{id}/recycle")
    @RequirePerm("opp:transfer")
    @AuditLog(action = "pool:opp:recycle", objectType = "opportunity")
    public R<Void> recycleOpportunity(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        poolService.recycleOpportunity(id, body != null ? body.get("reason") : null);
        return R.ok();
    }

    /* ---------- 客户公海 ---------- */

    @GetMapping("/customers")
    @RequirePerm("customer:view")
    public R<IPage<Customer>> listCustomers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String level,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size) {
        return R.ok(poolService.listPoolCustomers(keyword, level, page, size));
    }

    @PostMapping("/customers/{id}/claim")
    @RequirePerm("customer:create")
    @AuditLog(action = "pool:customer:claim", objectType = "customer")
    public R<Void> claimCustomer(@PathVariable Long id) {
        poolService.claimCustomer(id);
        return R.ok();
    }

    @PostMapping("/customers/{id}/recycle")
    @RequirePerm("customer:transfer")
    @AuditLog(action = "pool:customer:recycle", objectType = "customer")
    public R<Void> recycleCustomer(@PathVariable Long id, @RequestBody(required = false) java.util.Map<String, String> body) {
        poolService.recycleCustomer(id, body != null ? body.get("reason") : null);
        return R.ok();
    }
}
