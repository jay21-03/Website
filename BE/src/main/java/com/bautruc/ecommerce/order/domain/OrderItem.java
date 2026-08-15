package com.bautruc.ecommerce.order.domain;
import jakarta.persistence.*;
@Entity @Table(name="order_items")
public class OrderItem {
 @Id @GeneratedValue(strategy=GenerationType.SEQUENCE,generator="global_seq")@SequenceGenerator(name="global_seq",sequenceName="app_global_id_seq",allocationSize=1)private Long id;
 @Column(name="order_id",nullable=false)private Long orderId;@Column(name="product_id",nullable=false)private Long productId;@Column(name="product_name_vi",nullable=false)private String productNameVi;@Column(name="product_name_en",nullable=false)private String productNameEn;
 @Column(name="base_price",nullable=false)private long basePrice;@Column(name="selling_price",nullable=false)private long sellingPrice;@Column(nullable=false)private int quantity;@Column(name="total_price",nullable=false)private long totalPrice;
 protected OrderItem(){}
 public OrderItem(Long orderId,Long productId,String vi,String en,long base,long selling,int quantity){if(orderId==null||productId==null||base<=0||selling<=0||quantity<1)throw new IllegalArgumentException("invalid order item");this.orderId=orderId;this.productId=productId;productNameVi=vi;productNameEn=en;basePrice=base;sellingPrice=selling;this.quantity=quantity;totalPrice=Math.multiplyExact(selling,quantity);}
 public Long getId(){return id;}public Long getOrderId(){return orderId;}public Long getProductId(){return productId;}public String getProductNameVi(){return productNameVi;}public String getProductNameEn(){return productNameEn;}public long getBasePrice(){return basePrice;}public long getSellingPrice(){return sellingPrice;}public int getQuantity(){return quantity;}public long getTotalPrice(){return totalPrice;}
}
