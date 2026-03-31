package com.salygin.ssubench.repository;

import com.salygin.ssubench.entity.Task;
import com.salygin.ssubench.entity.TaskStatus;
import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.customizer.Bind;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RegisterBeanMapper(Task.class)
public interface TaskRepository {

    @SqlUpdate("""
        INSERT INTO tasks (id, title, description, reward, status, customer_id)
        VALUES (:taskId, :name, :details, :cost, :state, :ownerId)
    """)
    void insert(
            @Bind("taskId") UUID id,
            @Bind("name") String title,
            @Bind("details") String description,
            @Bind("cost") Integer reward,
            @Bind("state") String status,
            @Bind("ownerId") UUID customerId
    );

    @SqlQuery("""
        SELECT *
        FROM tasks
        WHERE id = :taskId
    """)
    Optional<Task> getById(@Bind("taskId") UUID id);

    @SqlQuery("""
        SELECT *
        FROM tasks
        LIMIT :pageSize OFFSET :pageOffset
    """)
    List<Task> getPage(
            @Bind("pageSize") int limit,
            @Bind("pageOffset") int offset
    );

    @SqlUpdate("""
        UPDATE tasks
        SET status = :newStatus
        WHERE id = :taskId
    """)
    void changeStatus(
            @Bind("taskId") UUID id,
            @Bind("newStatus") TaskStatus status
    );

    @SqlUpdate("""
        UPDATE tasks
        SET title = :name,
            description = :details,
            reward = :cost,
            status = :state
        WHERE id = :taskId
    """)
    void modify(
            @Bind("taskId") UUID id,
            @Bind("name") String title,
            @Bind("details") String description,
            @Bind("cost") Integer reward,
            @Bind("state") String status
    );

    @SqlUpdate("""
        UPDATE tasks
        SET status = 'CANCELLED'
        WHERE id = :taskId
    """)
    void markCancelled(@Bind("taskId") UUID id);

    @SqlUpdate("""
        UPDATE tasks
        SET executor_id = :executor,
            status = 'IN_PROGRESS'
        WHERE id = :taskId
    """)
    void setExecutor(
            @Bind("taskId") UUID taskId,
            @Bind("executor") UUID executorId
    );
}
