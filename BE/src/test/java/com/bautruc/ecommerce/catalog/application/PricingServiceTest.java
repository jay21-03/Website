package com.bautruc.ecommerce.catalog.application;
import static org.assertj.core.api.Assertions.assertThat;import static org.mockito.Mockito.*;
import java.math.BigDecimal;import java.time.*;import java.util.Optional;
import com.bautruc.ecommerce.catalog.domain.*;import com.bautruc.ecommerce.catalog.infrastructure.DiscountJpaRepository;import com.bautruc.ecommerce.common.time.BusinessClock;
import org.junit.jupiter.api.Test;
class PricingServiceTest{
 @Test void calculatesPercentageWithHalfUpAndHonorsBoundaries(){DiscountJpaRepository repo=mock(DiscountJpaRepository.class);BusinessClock clock=mock(BusinessClock.class);Instant now=Instant.parse("2026-08-13T00:00:00Z");when(clock.now()).thenReturn(now);Product p=new Product("A","A",null,null,1001,ProductStatus.ACTIVE,1L,now);Discount d=new Discount(null,DiscountType.PERCENTAGE,new BigDecimal("12.5"),now.minusSeconds(1),now.plusSeconds(1),true,now);when(repo.findByProductId(null)).thenReturn(Optional.of(d));assertThat(new PricingService(repo,clock).sellingPrice(p)).isEqualTo(876);}
 @Test void inactiveDiscountReturnsBasePrice(){DiscountJpaRepository repo=mock(DiscountJpaRepository.class);BusinessClock clock=mock(BusinessClock.class);Instant now=Instant.parse("2026-08-13T00:00:00Z");when(clock.now()).thenReturn(now);Product p=new Product("A","A",null,null,5000,ProductStatus.ACTIVE,1L,now);when(repo.findByProductId(null)).thenReturn(Optional.of(new Discount(null,DiscountType.FIXED_PRICE,new BigDecimal("3000"),now.minusSeconds(1),now.plusSeconds(1),false,now)));assertThat(new PricingService(repo,clock).sellingPrice(p)).isEqualTo(5000);}
}
