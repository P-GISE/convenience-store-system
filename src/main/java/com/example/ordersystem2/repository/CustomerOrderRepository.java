package com.example.ordersystem2.repository;

import com.example.ordersystem2.model.CustomerOrder;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerOrderRepository extends JpaRepository<CustomerOrder, Long> {

    @Override
    @EntityGraph(attributePaths = "orderType")
    List<CustomerOrder> findAll();

    @Override
    @EntityGraph(attributePaths = "orderType")
    Optional<CustomerOrder> findById(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = "orderType")
    @Query("select o from CustomerOrder o where o.id = :id")
    Optional<CustomerOrder> findByIdForStatusUpdate(@Param("id") Long id);

    @EntityGraph(attributePaths = "orderType")
    List<CustomerOrder> findAllByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = "orderType")
    List<CustomerOrder> findByCustomerPhoneOrderByCreatedAtDesc(String customerPhone);
}
