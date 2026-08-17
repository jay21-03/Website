package com.bautruc.ecommerce.inventory.application;
import java.util.*;import com.bautruc.ecommerce.common.exception.ResourceNotFoundException;import com.bautruc.ecommerce.inventory.domain.*;import com.bautruc.ecommerce.inventory.infrastructure.*;
import org.springframework.data.domain.*;import org.springframework.data.jpa.domain.Specification;import org.springframework.stereotype.Service;import org.springframework.transaction.annotation.Transactional;
@Service
public class InventoryQueryService{
 private final InventoryJpaRepository inventories;private final InventoryTransactionJpaRepository transactions;public InventoryQueryService(InventoryJpaRepository i,InventoryTransactionJpaRepository t){inventories=i;transactions=t;}
 @Transactional(readOnly=true) public Page<Inventory> list(String keyword,InventoryAvailabilityStatus status,Integer page,Integer size,String sort){
  int p=page==null?0:Math.max(0,page),s=size==null?20:Math.min(Math.max(1,size),100);Specification<Inventory> spec=(root,q,cb)->cb.isNull(root.join("product").get("deletedAt"));
  if(keyword!=null&&!keyword.isBlank()){String like="%"+keyword.trim().toLowerCase(Locale.ROOT)+"%";spec=spec.and((r,q,cb)->cb.or(cb.like(cb.lower(r.join("product").get("nameVi")),like),cb.like(cb.lower(r.join("product").get("nameEn")),like)));}
  if(status!=null)spec=spec.and((r,q,cb)->{var available=cb.diff(r.<Long>get("quantity"),r.<Long>get("reservedQuantity"));var threshold=r.<Long>get("lowStockThreshold");return switch(status){case OUT_OF_STOCK->cb.equal(available,0L);case LOW_STOCK->cb.and(cb.gt(available,0L),cb.le(available,threshold));case IN_STOCK->cb.gt(available,threshold);};});
  String[] parts=sort==null?new String[]{"productId","asc"}:sort.split(",",2);boolean desc=parts.length>1&&parts[1].equalsIgnoreCase("desc");
  if(parts[0].equals("availableQuantity")){spec=spec.and((r,q,cb)->{if(q.getResultType()!=Long.class&&q.getResultType()!=long.class){var available=cb.diff(r.<Long>get("quantity"),r.<Long>get("reservedQuantity"));q.orderBy(desc?cb.desc(available):cb.asc(available),desc?cb.desc(r.get("productId")):cb.asc(r.get("productId")));}return cb.conjunction();});return inventories.findAll(spec,PageRequest.of(p,s));}
  String field=parts[0].equals("quantity")?"quantity":"productId";return inventories.findAll(spec,PageRequest.of(p,s,Sort.by(desc?Sort.Direction.DESC:Sort.Direction.ASC,field)));
 }
 @Transactional(readOnly=true) public Page<InventoryTransaction> history(Long productId,Integer page,Integer size){if(inventories.findByProductId(productId).isEmpty())throw new ResourceNotFoundException(InventoryErrorCodes.INVENTORY_NOT_FOUND,"Inventory not found.");return transactions.findByProductId(productId,PageRequest.of(page==null?0:Math.max(0,page),size==null?20:Math.min(Math.max(1,size),100),Sort.by(Sort.Direction.DESC,"createdAt")));}
}
