package com.kahnhuseynov.ecommercebackend.promotion.repository;

import com.kahnhuseynov.ecommercebackend.promotion.entity.Promotion;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.*;

public interface PromotionRepository extends JpaRepository<Promotion, Long> {
    boolean existsByCode(String code);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Promotion p where p.id = :id")
    Optional<Promotion> findLockedById(@Param("id") Long id);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Promotion p where p.code = :code")
    Optional<Promotion> findLockedByCode(@Param("code") String code);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Promotion p where p.code is null and p.active = true order by p.id")
    List<Promotion> findAutomaticWithLock();
}
