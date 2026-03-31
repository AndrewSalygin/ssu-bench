package com.salygin.ssubench.repository;

import com.salygin.ssubench.entity.User;
import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.customizer.Bind;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RegisterBeanMapper(User.class)
public interface UserRepository {

    @SqlQuery("""
        SELECT *
        FROM users
        WHERE id = :userId
    """)
    Optional<User> getById(@Bind("userId") UUID id);

    @SqlQuery("""
        SELECT *
        FROM users
        WHERE email = :mail
    """)
    Optional<User> getByEmail(@Bind("mail") String email);

    @SqlQuery("""
        SELECT *
        FROM users
        LIMIT :size OFFSET :start
    """)
    List<User> getBatch(
            @Bind("size") int limit,
            @Bind("start") int offset
    );

    @SqlUpdate("""
        INSERT INTO users (id, email, password, role, balance, blocked)
        VALUES (:userId, :mail, :secret, :roleName, :amount, :isBlocked)
    """)
    void insertUser(
            @Bind("userId") UUID id,
            @Bind("mail") String email,
            @Bind("secret") String password,
            @Bind("roleName") String role,
            @Bind("amount") long balance,
            @Bind("isBlocked") boolean blocked
    );

    @SqlUpdate("""
        UPDATE users
        SET email = :mail,
            password = :pwd,
            role = :roleName,
            balance = :amount
        WHERE id = :userId
    """)
    void modify(
            @Bind("userId") UUID id,
            @Bind("mail") String email,
            @Bind("pwd") String passwordHash,
            @Bind("roleName") String role,
            @Bind("amount") long balance
    );

    @SqlUpdate("""
        DELETE FROM users
        WHERE id = :userId
    """)
    void remove(@Bind("userId") UUID id);

    @SqlUpdate("""
        UPDATE users
        SET balance = :amount
        WHERE id = :userId
    """)
    void setBalance(
            @Bind("userId") UUID id,
            @Bind("amount") long balance
    );

    @SqlUpdate("""
        UPDATE users
        SET balance = balance - :delta
        WHERE id = :userId
          AND balance >= :delta
    """)
    int deductIfSufficient(
            @Bind("userId") UUID id,
            @Bind("delta") long amount
    );

    @SqlUpdate("""
        UPDATE users
        SET balance = balance + :delta
        WHERE id = :userId
    """)
    void addBalance(
            @Bind("userId") UUID id,
            @Bind("delta") long amount
    );

    @SqlUpdate("""
        UPDATE users
        SET blocked = TRUE
        WHERE id = :userId
    """)
    void markBlocked(@Bind("userId") UUID id);

    @SqlUpdate("""
        UPDATE users
        SET blocked = FALSE
        WHERE id = :userId
    """)
    void markUnblocked(@Bind("userId") UUID id);
}
