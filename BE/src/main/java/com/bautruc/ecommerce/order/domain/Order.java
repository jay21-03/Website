package com.bautruc.ecommerce.order.domain;
import java.time.Instant;import jakarta.persistence.*;
@Entity @Table(name="orders")
public class Order {
 @Id private Long id;@Column(name="order_code",nullable=false,unique=true)private String orderCode;@Column(name="user_id",nullable=false)private Long userId;
 @Column(name="receiver_name",nullable=false)private String receiverName;@Column(nullable=false)private String phone;@Column(nullable=false)private String email;@Column(nullable=false)private String address;@Column private String note;
 @Column(name="total_amount",nullable=false)private long totalAmount;@Enumerated(EnumType.STRING)@Column(nullable=false)private OrderStatus status;@Column(name="completed_at")private Instant completedAt;@Column(name="created_at",nullable=false)private Instant createdAt;@Column(name="updated_at",nullable=false)private Instant updatedAt;
 protected Order(){}
 public Order(Long id,String code,Long userId,String receiverName,String phone,String email,String address,String note,long total,Instant now){if(id==null||userId==null||total<=0)throw new IllegalArgumentException("invalid order");this.id=id;orderCode=required(code);this.userId=userId;this.receiverName=required(receiverName);this.phone=required(phone);this.email=required(email);this.address=required(address);this.note=note;totalAmount=total;status=OrderStatus.NEW;createdAt=now;updatedAt=now;}
 public Long getId(){return id;}public String getOrderCode(){return orderCode;}public Long getUserId(){return userId;}public String getReceiverName(){return receiverName;}public String getPhone(){return phone;}public String getEmail(){return email;}public String getAddress(){return address;}public String getNote(){return note;}public long getTotalAmount(){return totalAmount;}public OrderStatus getStatus(){return status;}public Instant getCompletedAt(){return completedAt;}public Instant getCreatedAt(){return createdAt;}public Instant getUpdatedAt(){return updatedAt;}
 public void confirm(Instant now){if(status!=OrderStatus.NEW)throw new IllegalStateException("invalid order transition");status=OrderStatus.CONFIRMED;updatedAt=now;}
 public void complete(Instant now){if(status!=OrderStatus.CONFIRMED)throw new IllegalStateException("invalid order transition");status=OrderStatus.COMPLETED;completedAt=now;updatedAt=now;}
 public void cancel(Instant now){if(status!=OrderStatus.NEW&&status!=OrderStatus.CONFIRMED)throw new IllegalStateException("invalid order cancellation");status=OrderStatus.CANCELLED;updatedAt=now;}
 private static String required(String v){if(v==null||v.isBlank())throw new IllegalArgumentException("required text");return v.trim();}
}
