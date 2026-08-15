package com.bautruc.ecommerce.catalog.application;
import java.math.*;
import java.time.Instant;
import com.bautruc.ecommerce.catalog.domain.*;
import com.bautruc.ecommerce.catalog.infrastructure.DiscountJpaRepository;
import com.bautruc.ecommerce.common.time.BusinessClock;
import org.springframework.stereotype.Service;
@Service
public class PricingService{
    private final DiscountJpaRepository discounts; private final BusinessClock clock;
    public PricingService(DiscountJpaRepository discounts,BusinessClock clock){this.discounts=discounts;this.clock=clock;}
    public long sellingPrice(Product product){return discounts.findByProductId(product.getId()).map(d->calculate(product.getBasePrice(),d,clock.now())).orElse(product.getBasePrice());}
    public long sellingPrice(Product product,Instant pricingAt){return discounts.findByProductId(product.getId()).map(d->calculate(product.getBasePrice(),d,pricingAt)).orElse(product.getBasePrice());}
    public long calculate(long base,Discount d,Instant now){
        if(!d.isActive()||now.isBefore(d.getStartAt())||now.isAfter(d.getEndAt()))return base;
        if(d.getType()==DiscountType.FIXED_PRICE){long fixed=d.getValue().longValueExact();if(fixed<=0||fixed>=base)throw invalid();return fixed;}
        if(d.getValue().compareTo(BigDecimal.ZERO)<=0||d.getValue().compareTo(new BigDecimal("100"))>=0)throw invalid();
        long price=BigDecimal.valueOf(base).subtract(BigDecimal.valueOf(base).multiply(d.getValue()).divide(new BigDecimal("100"))).setScale(0,RoundingMode.HALF_UP).longValueExact();
        if(price<=0)throw invalid();return price;
    }
    private IllegalArgumentException invalid(){return new IllegalArgumentException("invalid discount");}
}
