package com.bautruc.ecommerce.commerce.infrastructure;
import java.time.Instant;import java.util.Optional;import com.bautruc.ecommerce.commerce.domain.CheckoutOperation;import jakarta.persistence.LockModeType;import org.springframework.data.jpa.repository.*;import org.springframework.data.repository.query.Param;import org.springframework.transaction.annotation.Transactional;
public interface CheckoutOperationJpaRepository extends JpaRepository<CheckoutOperation,Long>{
 Optional<CheckoutOperation> findByUserIdAndIdempotencyKey(Long userId,String key);
 @Lock(LockModeType.PESSIMISTIC_WRITE)@Query("select o from CheckoutOperation o where o.id=:id")Optional<CheckoutOperation> findByIdForUpdate(@Param("id")Long id);
 @Transactional @Modifying @Query(value="INSERT INTO checkout_operations(id,user_id,idempotency_key,request_hash,state,retry_count,state_changed_at,created_at,updated_at) VALUES(nextval('app_global_id_seq'),:userId,:key,:hash,'STARTED',0,:now,:now,:now) ON CONFLICT (user_id,idempotency_key) DO NOTHING",nativeQuery=true)int insertStarted(@Param("userId")Long userId,@Param("key")String key,@Param("hash")String hash,@Param("now")Instant now);
 @Query(value="select id from checkout_operations where state=:state order by state_changed_at limit :limit",nativeQuery=true)java.util.List<Long> findCandidateIds(@Param("state")String state,@Param("limit")int limit);
 @Query(value="select id from checkout_operations where state='PAYOS_CREATING' and processing_started_at<:before order by processing_started_at limit :limit",nativeQuery=true)java.util.List<Long> findStaleCreatingIds(@Param("before")Instant before,@Param("limit")int limit);
}
