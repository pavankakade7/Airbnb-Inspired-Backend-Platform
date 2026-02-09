package com.pavanKakade.projects.airBnbApp.strategy;

import com.pavanKakade.projects.airBnbApp.entity.Inventory;

import java.math.BigDecimal;
public interface PricingStrategy {

    BigDecimal calculatePrice(Inventory inventory);
}
