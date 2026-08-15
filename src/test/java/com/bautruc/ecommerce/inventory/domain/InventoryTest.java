package com.bautruc.ecommerce.inventory.domain;
import static org.assertj.core.api.Assertions.*;import java.time.Instant;import org.junit.jupiter.api.Test;
class InventoryTest{
 private final Instant now=Instant.parse("2026-08-13T00:00:00Z");
 @Test void maintainsPhysicalAndReservedInvariants(){Inventory i=new Inventory(1L,5,now);i.adjust(10,now);i.reserve(4,now);assertThat(i.availableQuantity()).isEqualTo(6);i.sale(4,now);assertThat(i.getQuantity()).isEqualTo(6);assertThat(i.getReservedQuantity()).isZero();i.restore(4,now);assertThat(i.getQuantity()).isEqualTo(10);}
 @Test void rejectsOversellAndAdjustmentBelowReserved(){Inventory i=new Inventory(1L,5,now);i.adjust(3,now);i.reserve(2,now);assertThatThrownBy(()->i.reserve(2,now)).isInstanceOf(IllegalStateException.class);assertThatThrownBy(()->i.adjust(-2,now)).isInstanceOf(IllegalStateException.class);}
 @Test void derivesAvailabilityStatus(){Inventory i=new Inventory(1L,5,now);assertThat(i.status()).isEqualTo(InventoryAvailabilityStatus.OUT_OF_STOCK);i.adjust(3,now);assertThat(i.status()).isEqualTo(InventoryAvailabilityStatus.LOW_STOCK);i.adjust(3,now);assertThat(i.status()).isEqualTo(InventoryAvailabilityStatus.IN_STOCK);}
}
