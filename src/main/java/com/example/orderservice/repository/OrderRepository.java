package com.example.orderservice.repository;


import java.awt.print.Pageable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    // ─── Lookup by business key ───────────────────────────────────────────────

    Optional<Order> findByOrderReference(String orderReference);

    boolean existsByOrderReference(String orderReference);

    // ─── Customer queries ─────────────────────────────────────────────────────

    Page<Order> findByCustomerId(Long customerId, Pageable pageable);

    Page<Order> findByCustomerIdAndStatus(Long customerId, com.example.orderservice.enums.OrderStatus status, Pageable pageable);

    List<Order> findByCustomerEmail(String email);

    // ─── Status queries ───────────────────────────────────────────────────────

    Page<Order> findByStatus(com.example.orderservice.enums.OrderStatus status, Pageable pageable);

    long countByStatus(com.example.orderservice.enums.OrderStatus status);

    // ─── Date-range queries ───────────────────────────────────────────────────

    Page<Order> findByCreatedAtBetween(LocalDateTime from, LocalDateTime to, Pageable pageable);

    Page<Order> findByCustomerIdAndCreatedAtBetween(
            Long customerId, LocalDateTime from, LocalDateTime to, Pageable pageable);

    // ─── Eager fetch (avoids N+1 on order detail) ────────────────────────────

    @Query("""
            SELECT DISTINCT o FROM Order o
            LEFT JOIN FETCH o.orderItems
            WHERE o.id = :id
            """)
    Optional<Order> findByIdWithItems(@Param("id") Long id);

    @Query("""
            SELECT DISTINCT o FROM Order o
            LEFT JOIN FETCH o.orderItems
            WHERE o.orderReference = :ref
            """)
    Optional<Order> findByOrderReferenceWithItems(@Param("ref") String reference);

    // ─── Bulk status update (e.g. daily PENDING → FAILED job) ─────────────────

    @Modifying
    @Query("""
            UPDATE Order o SET o.status = :newStatus
            WHERE o.status = :currentStatus
              AND o.createdAt < :before
            """)
    int bulkUpdateStatus(
            @Param("currentStatus") com.example.orderservice.enums.OrderStatus currentStatus,
            @Param("newStatus") com.example.orderservice.enums.OrderStatus newStatus,
            @Param("before") LocalDateTime before
    );

    // ─── Analytics / reporting ────────────────────────────────────────────────

    @Query("""
            SELECT COUNT(o) FROM Order o
            WHERE o.customerId = :customerId
              AND o.status NOT IN ('CANCELLED', 'FAILED')
            """)
    long countActiveOrdersByCustomer(@Param("customerId") Long customerId);

    @Query("""
            SELECT o FROM Order o
            WHERE o.status = 'PENDING'
              AND o.createdAt < :cutoff
            """)
    List<Order> findStalePendingOrders(@Param("cutoff") LocalDateTime cutoff);
}