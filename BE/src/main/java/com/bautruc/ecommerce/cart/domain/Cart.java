package com.bautruc.ecommerce.cart.domain;
import java.time.Instant;import jakarta.persistence.*;
@Entity @Table(name="carts")
public class Cart{
 @Id @GeneratedValue(strategy=GenerationType.SEQUENCE,generator="global_seq") @SequenceGenerator(name="global_seq",sequenceName="app_global_id_seq",allocationSize=1)private Long id;
 @Column(name="user_id",nullable=false,unique=true)private Long userId;@Column(name="created_at",nullable=false)private Instant createdAt;@Column(name="updated_at",nullable=false)private Instant updatedAt;
 protected Cart(){} public Long getId(){return id;}public Long getUserId(){return userId;}public Instant getCreatedAt(){return createdAt;}public Instant getUpdatedAt(){return updatedAt;}public void touch(Instant now){updatedAt=now;}
}
