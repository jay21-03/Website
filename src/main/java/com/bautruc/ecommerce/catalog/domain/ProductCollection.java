package com.bautruc.ecommerce.catalog.domain;

import java.time.Instant;
import jakarta.persistence.*;

@Entity
@Table(name = "collections")
public class ProductCollection {
    @Id @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "global_seq")
    @SequenceGenerator(name = "global_seq", sequenceName = "app_global_id_seq", allocationSize = 1)
    private Long id;
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
    public Long getId(){return id;} public String getNameVi(){return nameVi;} public String getNameEn(){return nameEn;}
    public String getDescriptionVi(){return descriptionVi;} public String getDescriptionEn(){return descriptionEn;}
    public CollectionStatus getStatus(){return status;} public Instant getCreatedAt(){return createdAt;}
    public Instant getUpdatedAt(){return updatedAt;} public Instant getDeletedAt(){return deletedAt;}
    public void update(String vi,String en,String dvi,String den,CollectionStatus status,Instant now){
        this.nameVi=required(vi);this.nameEn=required(en);this.descriptionVi=dvi;this.descriptionEn=den;this.status=status;this.updatedAt=now;
    }
    public void delete(Instant now){this.deletedAt=now;this.status=CollectionStatus.INACTIVE;this.updatedAt=now;}
    private static String required(String value){if(value==null||value.isBlank())throw new IllegalArgumentException("name is required");return value.trim();}
}
