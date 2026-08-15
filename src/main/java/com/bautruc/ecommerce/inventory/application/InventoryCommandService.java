package com.bautruc.ecommerce.inventory.application;
import java.util.*;import com.bautruc.ecommerce.common.exception.*;import com.bautruc.ecommerce.common.time.BusinessClock;import com.bautruc.ecommerce.inventory.domain.*;import com.bautruc.ecommerce.inventory.infrastructure.*;
import org.springframework.beans.factory.annotation.Value;import org.springframework.context.ApplicationEventPublisher;import org.springframework.http.HttpStatus;import org.springframework.stereotype.Service;import org.springframework.transaction.annotation.Transactional;
@Service
public class InventoryCommandService implements InventoryInitializationPort,InventoryAvailabilityQuery{
 private final InventoryJpaRepository inventories;private final InventoryTransactionJpaRepository transactions;private final BusinessClock clock;private final ApplicationEventPublisher events;private final long defaultThreshold;
 public InventoryCommandService(InventoryJpaRepository i,InventoryTransactionJpaRepository t,BusinessClock c,ApplicationEventPublisher e,@Value("${bautruc.inventory.default-low-stock-threshold:5}")long threshold){inventories=i;transactions=t;clock=c;events=e;defaultThreshold=threshold;}
 @Override @Transactional public void initialize(Long productId){if(inventories.findByProductId(productId).isEmpty())inventories.save(new Inventory(productId,defaultThreshold,clock.now()));}
 @Override @Transactional(readOnly=true) public long available(Long productId){return required(productId).availableQuantity();}
 @Transactional public Inventory adjust(Long productId,InventoryTransactionType type,long delta,String reason,Long adminId){
  if((type==InventoryTransactionType.IMPORT&&delta<=0)||(type==InventoryTransactionType.ADJUSTMENT&&delta==0)||(type!=InventoryTransactionType.IMPORT&&type!=InventoryTransactionType.ADJUSTMENT))throw new BusinessException(InventoryErrorCodes.INVENTORY_INVALID_ADJUSTMENT,"Invalid inventory adjustment.");
  Inventory i=locked(productId);long bq=i.getQuantity(),br=i.getReservedQuantity();InventoryAvailabilityStatus before=status(bq-br,i.getLowStockThreshold());try{i.adjust(delta,clock.now());}catch(RuntimeException e){throw new BusinessException(InventoryErrorCodes.INVENTORY_INVALID_ADJUSTMENT,"Adjustment violates inventory constraints.");}
  InventoryTransaction transaction=audit(i,type,delta,0,bq,br,"ADMIN_ACTION",adminId,null,reason,adminId);publishTransition(i,transaction,before);return i;
 }
 @Transactional public void reserve(Long orderId,Map<Long,Long> items){mutateOrder(orderId,items,InventoryTransactionType.RESERVE);}
 @Transactional public void reserve(Long orderId,Map<Long,Long> items,java.time.Instant occurredAt){mutateOrder(orderId,items,InventoryTransactionType.RESERVE,occurredAt);}
 @Transactional public void validateReservable(Map<Long,Long> items){
  if(items==null||items.isEmpty())throw state();
  for(Long productId:items.keySet().stream().sorted().toList()){
   Long amount=items.get(productId);
   if(amount==null||amount<=0)throw state();
   if(locked(productId).availableQuantity()<amount)throw new BusinessException(InventoryErrorCodes.INVENTORY_INSUFFICIENT,"Insufficient inventory.",HttpStatus.CONFLICT);
  }
 }
 @Transactional public void release(Long orderId,Map<Long,Long> items){mutateOrder(orderId,items,InventoryTransactionType.RELEASE);}
 @Transactional public void sale(Long orderId,Map<Long,Long> items){mutateOrder(orderId,items,InventoryTransactionType.SALE);}
 @Transactional public void restore(Long orderId,Map<Long,Long> items){mutateOrder(orderId,items,InventoryTransactionType.CANCEL_ORDER);}
 private void mutateOrder(Long orderId,Map<Long,Long> items,InventoryTransactionType type){mutateOrder(orderId,items,type,clock.now());}
 private void mutateOrder(Long orderId,Map<Long,Long> items,InventoryTransactionType type,java.time.Instant occurredAt){
  if(orderId==null||items==null||items.isEmpty())throw state();List<Long> ids=items.keySet().stream().sorted().toList();List<Inventory> locked=ids.stream().map(this::locked).toList();
  for(Inventory i:locked){long amount=Objects.requireNonNull(items.get(i.getProductId()));String key=type+":ORDER:"+orderId+":PRODUCT:"+i.getProductId();if(transactions.findByBusinessKey(key).isPresent())continue;long bq=i.getQuantity(),br=i.getReservedQuantity();try{switch(type){case RESERVE->i.reserve(amount,occurredAt);case RELEASE->i.release(amount,occurredAt);case SALE->i.sale(amount,occurredAt);case CANCEL_ORDER->i.restore(amount,occurredAt);default->throw state();}}catch(IllegalStateException e){if(type==InventoryTransactionType.RESERVE)throw new BusinessException(InventoryErrorCodes.INVENTORY_INSUFFICIENT,"Insufficient inventory.",HttpStatus.CONFLICT);throw state();}
   InventoryAvailabilityStatus before=status(bq-br,i.getLowStockThreshold());InventoryTransaction transaction=audit(i,type,i.getQuantity()-bq,i.getReservedQuantity()-br,bq,br,"ORDER",orderId,key,null,null,occurredAt);publishTransition(i,transaction,before);
  }
 }
 private InventoryTransaction audit(Inventory i,InventoryTransactionType type,long qd,long rd,long bq,long br,String rt,Long ri,String key,String reason,Long by){return transactions.save(new InventoryTransaction(i.getProductId(),type,qd,rd,bq,i.getQuantity(),br,i.getReservedQuantity(),rt,ri,key,reason,by,clock.now()));}
 private InventoryTransaction audit(Inventory i,InventoryTransactionType type,long qd,long rd,long bq,long br,String rt,Long ri,String key,String reason,Long by,java.time.Instant at){return transactions.save(new InventoryTransaction(i.getProductId(),type,qd,rd,bq,i.getQuantity(),br,i.getReservedQuantity(),rt,ri,key,reason,by,at));}
 private void publishTransition(Inventory i,InventoryTransaction transaction,InventoryAvailabilityStatus before){InventoryAvailabilityStatus after=i.status();if(before!=after)events.publishEvent(new InventoryAvailabilityTransition(transaction.getId(),i.getProductId(),before,after,i.availableQuantity(),i.getLowStockThreshold()));}
 private InventoryAvailabilityStatus status(long available,long threshold){return available==0?InventoryAvailabilityStatus.OUT_OF_STOCK:available<=threshold?InventoryAvailabilityStatus.LOW_STOCK:InventoryAvailabilityStatus.IN_STOCK;}
 private Inventory locked(Long id){return inventories.findByProductIdForUpdate(id).orElseThrow(()->new ResourceNotFoundException(InventoryErrorCodes.INVENTORY_NOT_FOUND,"Inventory not found."));}
 private Inventory required(Long id){return inventories.findByProductId(id).orElseThrow(()->new ResourceNotFoundException(InventoryErrorCodes.INVENTORY_NOT_FOUND,"Inventory not found."));}
 private BusinessException state(){return new BusinessException(InventoryErrorCodes.INVENTORY_STATE_CONFLICT,"Inventory state conflict.",HttpStatus.CONFLICT);}
}
