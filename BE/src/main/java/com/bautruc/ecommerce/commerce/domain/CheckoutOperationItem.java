package com.bautruc.ecommerce.commerce.domain;
import jakarta.persistence.*;
@Entity @Table(name="checkout_operation_items")
public class CheckoutOperationItem{
 @Id @GeneratedValue(strategy=GenerationType.SEQUENCE,generator="global_seq")@SequenceGenerator(name="global_seq",sequenceName="app_global_id_seq",allocationSize=1)private Long id;
 @Column(name="checkout_operation_id",nullable=false)private Long checkoutOperationId;@Column(name="cart_item_id",nullable=false)private Long cartItemId;@Column(name="cart_item_version",nullable=false)private long cartItemVersion;@Column(name="product_id",nullable=false)private Long productId;@Column(nullable=false)private int quantity;
 protected CheckoutOperationItem(){}public CheckoutOperationItem(Long op,Long item,long version,Long product,int quantity){checkoutOperationId=op;cartItemId=item;cartItemVersion=version;productId=product;this.quantity=quantity;}public Long getId(){return id;}public Long getCheckoutOperationId(){return checkoutOperationId;}public Long getCartItemId(){return cartItemId;}public long getCartItemVersion(){return cartItemVersion;}public Long getProductId(){return productId;}public int getQuantity(){return quantity;}
}
