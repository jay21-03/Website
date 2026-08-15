package com.bautruc.ecommerce.order.domain;
import java.time.*;import java.time.format.DateTimeFormatter;import org.springframework.stereotype.Component;
@Component public class OrderCodeGenerator{private static final DateTimeFormatter DATE=DateTimeFormatter.BASIC_ISO_DATE;public String generate(Long id,Instant at,ZoneId zone){if(id==null||id<=0)throw new IllegalArgumentException("id required");return "ORD-"+DATE.format(at.atZone(zone).toLocalDate())+"-"+String.format("%08d",id);}}
