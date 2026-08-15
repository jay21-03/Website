package com.bautruc.ecommerce.cart.domain;
import java.time.Instant;import jakarta.persistence.*;
@Entity @Table(name="cart_items")
public class CartItem{
 @Id @GeneratedValue(strategy=GenerationType.SEQUENCE,generator="global_seq") @SequenceGenerator(name="global_seq",sequenceName="app_global_id_seq",allocationSize=1)private Long id;
 @Column(name="cart_id",nullable=false)private Long cartId;@Column(name="product_id",nullable=false)private Long productId;@Column(nullable=false)private int quantity;
 @Version @Column(nullable=false)private long version;@Column(name="created_at",nullable=false)private Instant createdAt;@Column(name="updated_at",nullable=false)private Instant updatedAt;
 protected CartItem(){} public CartItem(Long cartId,Long productId,int quantity,Instant now){this.cartId=cartId;this.productId=productId;setQuantity(quantity,now);createdAt=now;}
 public Long getId(){return id;}public Long getCartId(){return cartId;}public Long getProductId(){return productId;}public int getQuantity(){return quantity;}public long getVersion(){return version;}public Instant getCreatedAt(){return createdAt;}public Instant getUpdatedAt(){return updatedAt;}
 public void setQuantity(int value,Instant now){if(value<1)throw new IllegalArgumentException("quantity must be positive");quantity=value;updatedAt=now;}
}
