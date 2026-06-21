package com.devsu.account.repository;

import java.math.BigDecimal;
import java.util.UUID;

public interface AccountBalanceSnapshot {
    UUID getAccountId();
    BigDecimal getBalance();
}
