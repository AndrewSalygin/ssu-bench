package com.salygin.ssubench.repository;

import com.salygin.ssubench.entity.Bid;
import com.salygin.ssubench.entity.BidStatus;
import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.customizer.Bind;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RegisterBeanMapper(Bid.class)
public interface BidRepository {

    @SqlUpdate("""
        INSERT INTO bids (id, task_id, executor_id, status)
        VALUES (:bidId, :taskRef, :executorRef, :state)
    """)
    void insert(
            @Bind("bidId") UUID id,
            @Bind("taskRef") UUID taskId,
            @Bind("executorRef") UUID executorId,
            @Bind("state") String status
    );

    @SqlQuery("""
        SELECT *
        FROM bids
        WHERE id = :bidId
    """)
    Optional<Bid> getById(@Bind("bidId") UUID id);

    @SqlQuery("""
        SELECT *
        FROM bids
        WHERE task_id = :taskRef
        ORDER BY id
        LIMIT :limitValue OFFSET :offsetValue
    """)
    List<Bid> getByTask(
            @Bind("taskRef") UUID taskId,
            @Bind("limitValue") int limit,
            @Bind("offsetValue") int offset
    );

    @SqlUpdate("""
        UPDATE bids
        SET status = :newStatus
        WHERE id = :bidId
    """)
    void changeStatus(
            @Bind("bidId") UUID id,
            @Bind("newStatus") BidStatus status
    );

    @SqlUpdate("""
        UPDATE bids
        SET status = 'REJECTED'
        WHERE task_id = :taskRef
          AND id <> :approvedBidId
    """)
    void rejectOthers(
            @Bind("taskRef") UUID taskId,
            @Bind("approvedBidId") UUID acceptedBidId
    );

    @SqlQuery("""
        SELECT COUNT(*)
        FROM bids
        WHERE task_id = :taskRef
          AND executor_id = :executorRef
    """)
    int existsByTaskAndExecutor(
            @Bind("taskRef") UUID taskId,
            @Bind("executorRef") UUID executorId
    );
}