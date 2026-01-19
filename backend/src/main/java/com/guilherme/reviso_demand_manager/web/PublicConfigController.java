package com.guilherme.reviso_demand_manager.web;

import com.guilherme.reviso_demand_manager.infra.BillingConfig;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/public")
public class PublicConfigController {

    private final BillingConfig billingConfig;

    public PublicConfigController(BillingConfig billingConfig) {
        this.billingConfig = billingConfig;
    }

    @GetMapping("/config")
    public Map<String, Object> getConfig() {
        return Map.of(
                "billingProvider", billingConfig.getProvider().name().toLowerCase(),
                "trialDays", billingConfig.getTrialDays(),
                "isMock", billingConfig.isMock()
        );
    }
}
