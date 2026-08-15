package com.bautruc.ecommerce.catalog.domain;

import java.math.BigDecimal;
import java.time.Instant;
import jakarta.persistence.*;

@Entity @Table(name="discounts")
public class Discount {
    @Id @GeneratedValue(strategy=GenerationType.SEQUENCE,generator="global_seq")
    @SequenceGenerator(name="global_seq",sequenceName="app_global_id_seq",allocationSize=1) private Long id;
    @Column(name="product_id",nullable=false,unique=true) private Long productId;
    @Enumerated(EnumType.STRING) @Column(name="discount_type",nullable=false) private DiscountType type;
    @Column(name="discount_value",nullable=false,precision=19,scale=4) private BigDecimal value;
    @Column(name="start_at",nullable=false) private Instant startAt;
    @Column(name="end_at",nullable=false) private Instant endAt;
    @Column(name="is_active",nullable=false) private boolean active;
    @Column(name="created_at",nullable=false) private Instant createdAt;
    @Column(name="updated_at",nullable=false) private Instant updatedAt;
    protected Discount(){}
    public Discount(Long productId,DiscountType type,BigDecimal value,Instant start,Instant end,boolean active,Instant now){
        this.productId=productId;this.createdAt=now;update(type,value,start,end,active,now);
    }
    public Long getId(){return id;} public Long getProductId(){return productId;} public DiscountType getType(){return type;}
    public BigDecimal getValue(){return value;} public Instant getStartAt(){return startAt;} public Instant getEndAt(){return endAt;}
    public boolean isActive(){return active;} public Instant getCreatedAt(){return createdAt;} public Instant getUpdatedAt(){return updatedAt;}
    public void update(DiscountType type,BigDecimal value,Instant start,Instant end,boolean active,Instant now){
        if(type==null||value==null||value.signum()<=0||start==null||end==null||!start.isBefore(end))throw new IllegalArgumentException("invalid discount");
        this.type=type;this.value=value;this.startAt=start;this.endAt=end;this.active=active;this.updatedAt=now;
    }
}
