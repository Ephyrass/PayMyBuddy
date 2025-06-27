package com.PayMyBuddy.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Billing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "transaction_id", nullable = false)
    private Transaction transaction;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private LocalDateTime date;

    @Column(nullable = false)
    private Boolean processed = false;

    @Column(name = "fee_percentage", nullable = false)
    private BigDecimal feePercentage;

    @Column(nullable = false)
    private String description;

    // Method to calculate the fee based on the amount and fee percentage
    public static BigDecimal calculateFee(BigDecimal amount, BigDecimal feePercentage) {
        return amount.multiply(feePercentage.divide(new BigDecimal("100")));
    }
}
