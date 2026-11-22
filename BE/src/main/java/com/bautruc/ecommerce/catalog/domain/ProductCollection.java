package com.bautruc.ecommerce.catalog.domain;

import java.time.Instant;
import jakarta.persistence.*;

@Entity
@Table(name = "collections")
public class ProductCollection {
    @Id @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "global_seq")
    @SequenceGenerator(name = "global_seq", sequenceName = "app_global_id_seq", allocationSize = 1)
    private Long id;
    @Column(name="slug",nullable=false,length=300,unique=true) private String slug;
    @Column(name="seo_title",length=180) private String seoTitle;
    @Column(name="seo_description",length=500) private String seoDescription;
    @Column(name="name_vi", nullable=false) private String nameVi;
    @Column(name="name_en", nullable=false) private String nameEn;
    @Column(name="description_vi") private String descriptionVi;
    @Column(name="description_en") private String descriptionEn;
    @Enumerated(EnumType.STRING) @Column(nullable=false) private CollectionStatus status;
    @Column(name="created_at", nullable=false) private Instant createdAt;
    @Column(name="updated_at", nullable=false) private Instant updatedAt;
    @Column(name="deleted_at") private Instant deletedAt;

    protected ProductCollection() {}
    public ProductCollection(String nameVi, String nameEn, String descriptionVi, String descriptionEn,
                             CollectionStatus status, Instant now) {
        this.nameVi = required(nameVi); this.nameEn = required(nameEn);
        this.descriptionVi = descriptionVi; this.descriptionEn = descriptionEn;
        this.status = status; this.createdAt = now; this.updatedAt = now;
    }
    @PrePersist private void assignSlug(){if(slug==null)slug=ProductSlug.fromName(nameVi,id);}
    public Long getId(){return id;} public String getSlug(){return slug;} public String getSeoTitle(){return seoTitle;} public String getSeoDescription(){return seoDescription;} public String getNameVi(){return nameVi;} public String getNameEn(){return nameEn;}
    public String getDescriptionVi(){return descriptionVi;} public String getDescriptionEn(){return descriptionEn;}
    public CollectionStatus getStatus(){return status;} public Instant getCreatedAt(){return createdAt;}
    public Instant getUpdatedAt(){return updatedAt;} public Instant getDeletedAt(){return deletedAt;}
    public void update(String vi,String en,String dvi,String den,CollectionStatus status,Instant now){
        this.nameVi=required(vi);this.nameEn=required(en);this.descriptionVi=dvi;this.descriptionEn=den;this.status=status;this.updatedAt=now;
    }
    public void delete(Instant now){this.deletedAt=now;this.status=CollectionStatus.INACTIVE;this.updatedAt=now;}
    public void updateSeo(String title,String description){if(title!=null)seoTitle=title.isBlank()?null:title.trim();if(description!=null)seoDescription=description.isBlank()?null:description.trim();}
    private static String required(String value){if(value==null||value.isBlank())throw new IllegalArgumentException("name is required");return value.trim();}
}
