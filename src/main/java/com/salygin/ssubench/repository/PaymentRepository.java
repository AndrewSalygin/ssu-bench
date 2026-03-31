package com.salygin.ssubench.repository;

import com.salygin.ssubench.entity.Payment;
import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.customizer.Bind;

import java.time.LocalDateTime;
import java.util.UUID;
@RegisterBeanMapper(Payment.class)
public interface PaymentRepository {

    @SqlUpdate("""
        INSERT INTO payments (id, from_user, to_user, amount, created_at)
        VALUES (:paymentId, :senderId, :receiverId, :sum, :timestamp)
    """)
    void insertPayment(
            @Bind("paymentId") UUID id,
            @Bind("senderId") UUID fromUserId,
            @Bind("receiverId") UUID toUserId,
            @Bind("sum") Integer amount,
            @Bind("timestamp") LocalDateTime createdAt
    );
}
