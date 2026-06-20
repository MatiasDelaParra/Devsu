package com.devsu.account.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        name = "customer_snapshots",
        schema = "account",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_customer_snapshots_customer_id",
                        columnNames = "customer_id"
                )
        }
)
public class CustomerSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "customer_id", nullable = false, updatable = false, length = 50)
    private String customerId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "identification", nullable = false, length = 50)
    private String identification;

    @Column(name = "status", nullable = false)
    private Boolean status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public void update(String name, String identification, Boolean status, Instant occurredAt) {
        this.name = name;
        this.identification = identification;
        this.status = status;
        this.updatedAt = occurredAt;
    }

    public void changeStatus(Boolean status, Instant occurredAt) {
        this.status = status;
        this.updatedAt = occurredAt;
    }

    public boolean isNewerThan(Instant occurredAt) {
        return updatedAt != null && updatedAt.isAfter(occurredAt);
    }
}
