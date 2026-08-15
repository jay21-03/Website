package com.bautruc.ecommerce.catalog.domain;

import java.time.Instant;
import jakarta.persistence.*;

@Entity @Table(name="products")
public class Product {
    @Id @GeneratedValue(strategy=GenerationType.SEQUENCE,generator="global_seq")
    @SequenceGenerator(name="global_seq",sequenceName="app_global_id_seq",allocationSize=1)
    private Long id;
    @Column(name="name_vi",nullable=false) private String nameVi;
    @Column(name="name_en",nullable=false) private String nameEn;
    @Column(name="description_vi") private String descriptionVi;
    @Column(name="description_en") private String descriptionEn;
    @Column(name="base_price",nullable=false) private long basePrice;
    @Enumerated(EnumType.STRING) @Column(nullable=false) private ProductStatus status;
    @Column(name="collection_id",nullable=false) private Long collectionId;
    @Column(name="created_at",nullable=false) private Instant createdAt;
    @Column(name="updated_at",nullable=false) private Instant updatedAt;
    @Column(name="deleted_at") private Instant deletedAt;
    protected Product(){}
    public Product(String vi,String en,String dvi,String den,long price,ProductStatus status,Long collectionId,Instant now){
        this.nameVi=required(vi);this.nameEn=required(en);this.descriptionVi=dvi;this.descriptionEn=den;setPrice(price);
        this.status=required(status);this.collectionId=required(collectionId);this.createdAt=now;this.updatedAt=now;
    }
    public Long getId(){return id;} public String getNameVi(){return nameVi;} public String getNameEn(){return nameEn;}
    public String getDescriptionVi(){return descriptionVi;} public String getDescriptionEn(){return descriptionEn;}
    public long getBasePrice(){return basePrice;} public ProductStatus getStatus(){return status;}
    public Long getCollectionId(){return collectionId;} public Instant getCreatedAt(){return createdAt;}
    public Instant getUpdatedAt(){return updatedAt;} public Instant getDeletedAt(){return deletedAt;}
    public boolean isPurchasable(){return status==ProductStatus.ACTIVE&&deletedAt==null;}
    public void update(String vi,String en,String dvi,String den,long price,ProductStatus status,Long collectionId,Instant now){
        this.nameVi=required(vi);this.nameEn=required(en);this.descriptionVi=dvi;this.descriptionEn=den;setPrice(price);
        this.status=required(status);this.collectionId=required(collectionId);this.updatedAt=now;
    }
    public void delete(Instant now){deletedAt=now;status=ProductStatus.INACTIVE;updatedAt=now;}
    private void setPrice(long value){if(value<=0)throw new IllegalArgumentException("basePrice must be positive");basePrice=value;}
    private static String required(String v){if(v==null||v.isBlank())throw new IllegalArgumentException("text is required");return v.trim();}
    private static <T>T required(T v){if(v==null)throw new IllegalArgumentException("value is required");return v;}
}
