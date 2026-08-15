package com.bautruc.ecommerce.inventory.domain;
import java.time.Instant;import com.bautruc.ecommerce.catalog.domain.Product;import jakarta.persistence.*;
@Entity @Table(name="inventories")
public class Inventory{
 @Id @GeneratedValue(strategy=GenerationType.SEQUENCE,generator="global_seq") @SequenceGenerator(name="global_seq",sequenceName="app_global_id_seq",allocationSize=1) private Long id;
 @Column(name="product_id",nullable=false,unique=true) private Long productId;
 @OneToOne(fetch=FetchType.EAGER) @JoinColumn(name="product_id",insertable=false,updatable=false) private Product product;
 @Column(nullable=false) private long quantity;
 @Column(name="reserved_quantity",nullable=false) private long reservedQuantity;
 @Column(name="low_stock_threshold",nullable=false) private long lowStockThreshold;
 @Column(name="updated_at",nullable=false) private Instant updatedAt;
 protected Inventory(){}
 public Inventory(Long productId,long threshold,Instant now){this.productId=productId;this.lowStockThreshold=threshold;this.updatedAt=now;}
 public Long getId(){return id;}public Long getProductId(){return productId;}public Product getProduct(){return product;}public long getQuantity(){return quantity;}public long getReservedQuantity(){return reservedQuantity;}public long getLowStockThreshold(){return lowStockThreshold;}public Instant getUpdatedAt(){return updatedAt;}
 public long availableQuantity(){return Math.subtractExact(quantity,reservedQuantity);}
 public InventoryAvailabilityStatus status(){long a=availableQuantity();return a==0?InventoryAvailabilityStatus.OUT_OF_STOCK:a<=lowStockThreshold?InventoryAvailabilityStatus.LOW_STOCK:InventoryAvailabilityStatus.IN_STOCK;}
 public void adjust(long delta,Instant now){long after=Math.addExact(quantity,delta);if(after<0||after<reservedQuantity)throw new IllegalStateException("invalid inventory adjustment");quantity=after;updatedAt=now;}
 public void reserve(long amount,Instant now){if(amount<=0||availableQuantity()<amount)throw new IllegalStateException("insufficient inventory");reservedQuantity=Math.addExact(reservedQuantity,amount);updatedAt=now;}
 public void release(long amount,Instant now){if(amount<=0||reservedQuantity<amount)throw new IllegalStateException("invalid inventory release");reservedQuantity-=amount;updatedAt=now;}
 public void sale(long amount,Instant now){if(amount<=0||quantity<amount||reservedQuantity<amount)throw new IllegalStateException("invalid inventory sale");quantity-=amount;reservedQuantity-=amount;updatedAt=now;}
 public void restore(long amount,Instant now){if(amount<=0)throw new IllegalStateException("invalid inventory restore");quantity=Math.addExact(quantity,amount);updatedAt=now;}
}
