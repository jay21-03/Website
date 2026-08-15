package com.bautruc.ecommerce.inventory.application;
import com.bautruc.ecommerce.common.security.CurrentUserProvider;import com.bautruc.ecommerce.inventory.api.request.AdjustInventoryRequest;import com.bautruc.ecommerce.inventory.domain.Inventory;
import org.springframework.security.access.AccessDeniedException;import org.springframework.stereotype.Service;
@Service
public class InventoryAdminService{
 private final InventoryCommandService commands;private final CurrentUserProvider currentUser;public InventoryAdminService(InventoryCommandService c,CurrentUserProvider u){commands=c;currentUser=u;}
 public Inventory adjust(Long productId,AdjustInventoryRequest r){Long id=currentUser.currentUser().orElseThrow(()->new AccessDeniedException("Authentication required")).userId();return commands.adjust(productId,r.type(),r.quantityChange(),r.reason(),id);}
}
