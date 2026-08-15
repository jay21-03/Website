package com.bautruc.ecommerce.inventory.domain;
import java.time.Instant;import jakarta.persistence.*;
@Entity @Table(name="inventory_transactions")
public class InventoryTransaction{
 @Id @GeneratedValue(strategy=GenerationType.SEQUENCE,generator="global_seq") @SequenceGenerator(name="global_seq",sequenceName="app_global_id_seq",allocationSize=1) private Long id;
 @Column(name="product_id",nullable=false) private Long productId;@Enumerated(EnumType.STRING) @Column(nullable=false) private InventoryTransactionType type;
 @Column(name="quantity_delta",nullable=false) private long quantityDelta;@Column(name="reserved_quantity_delta",nullable=false) private long reservedQuantityDelta;
 @Column(name="before_quantity",nullable=false) private long beforeQuantity;@Column(name="after_quantity",nullable=false) private long afterQuantity;
 @Column(name="before_reserved_quantity",nullable=false) private long beforeReservedQuantity;@Column(name="after_reserved_quantity",nullable=false) private long afterReservedQuantity;
 @Column(name="reference_type") private String referenceType;@Column(name="reference_id") private Long referenceId;@Column(name="business_key") private String businessKey;
 @Column private String reason;@Column(name="created_by_user_id") private Long createdByUserId;@Column(name="created_at",nullable=false) private Instant createdAt;
 protected InventoryTransaction(){}
 public InventoryTransaction(Long productId,InventoryTransactionType type,long qd,long rd,long bq,long aq,long br,long ar,String rt,Long ri,String key,String reason,Long by,Instant at){this.productId=productId;this.type=type;quantityDelta=qd;reservedQuantityDelta=rd;beforeQuantity=bq;afterQuantity=aq;beforeReservedQuantity=br;afterReservedQuantity=ar;referenceType=rt;referenceId=ri;businessKey=key;this.reason=reason;createdByUserId=by;createdAt=at;}
 public Long getId(){return id;}public Long getProductId(){return productId;}public InventoryTransactionType getType(){return type;}public long getQuantityDelta(){return quantityDelta;}public long getReservedQuantityDelta(){return reservedQuantityDelta;}public long getBeforeQuantity(){return beforeQuantity;}public long getAfterQuantity(){return afterQuantity;}public long getBeforeReservedQuantity(){return beforeReservedQuantity;}public long getAfterReservedQuantity(){return afterReservedQuantity;}public String getReferenceType(){return referenceType;}public Long getReferenceId(){return referenceId;}public String getBusinessKey(){return businessKey;}public String getReason(){return reason;}public Long getCreatedByUserId(){return createdByUserId;}public Instant getCreatedAt(){return createdAt;}
}
