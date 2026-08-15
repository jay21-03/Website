package com.bautruc.ecommerce.commerce.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.*;
import com.bautruc.ecommerce.cart.application.CartApplicationService;
import com.bautruc.ecommerce.catalog.api.request.*;
import com.bautruc.ecommerce.catalog.application.CatalogService;
import com.bautruc.ecommerce.catalog.domain.*;
import com.bautruc.ecommerce.commerce.api.request.CheckoutRequest;
import com.bautruc.ecommerce.commerce.api.response.CheckoutResponse;
import com.bautruc.ecommerce.common.exception.BusinessException;
import com.bautruc.ecommerce.common.security.*;
import com.bautruc.ecommerce.identity.domain.*;
import com.bautruc.ecommerce.identity.infrastructure.UserJpaRepository;
import com.bautruc.ecommerce.inventory.application.InventoryCommandService;
import com.bautruc.ecommerce.inventory.domain.InventoryTransactionType;
import com.bautruc.ecommerce.order.infrastructure.OrderJpaRepository;
import com.bautruc.ecommerce.payment.application.*;
import com.bautruc.ecommerce.payment.domain.*;
import com.bautruc.ecommerce.payment.infrastructure.PaymentJpaRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.*;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class Phase4PaymentLifecycleIntegrationTest {
    @Container static final PostgreSQLContainer<?> POSTGRES=new PostgreSQLContainer<>("postgres:16-alpine");
    @DynamicPropertySource static void db(DynamicPropertyRegistry r){r.add("spring.datasource.url",POSTGRES::getJdbcUrl);r.add("spring.datasource.username",POSTGRES::getUsername);r.add("spring.datasource.password",POSTGRES::getPassword);}
    @Autowired CatalogService catalog;@Autowired InventoryCommandService inventory;@Autowired CartApplicationService cart;@Autowired CheckoutApplicationService checkout;@Autowired CheckoutOperationService checkoutOperations;@Autowired CheckoutLocalPreparationService checkoutPreparation;@Autowired CheckoutStateService checkoutStates;@Autowired CheckoutRecoveryService checkoutRecovery;@Autowired PaymentResultApplicationService results;@Autowired PaymentExpirationWorker expirationWorker;@Autowired OrderCancellationApplicationService cancellations;@Autowired ManualRefundApplicationService refunds;@Autowired PaymentJpaRepository payments;@Autowired OrderJpaRepository orders;@Autowired UserJpaRepository users;@Autowired JdbcTemplate jdbc;
    @MockitoBean PayOSClient payos;

    @BeforeEach void clean(){SecurityContextHolder.clearContext();jdbc.execute("TRUNCATE TABLE notifications, collections, users CASCADE");when(payos.createPaymentRequest(any())).thenAnswer(i->{PayOSCreatePaymentCommand c=i.getArgument(0);return provider(c.orderId(),c.amount());});}
    @AfterEach void clear(){SecurityContextHolder.clearContext();}

    @Test void checkoutCompletesCg007AndReplayDoesNotDuplicateEffects(){User admin=user("admin",UserRole.ADMIN),buyer=user("buyer",UserRole.USER);authenticate(buyer);long product=product(5);cart.add(product,2);String key=UUID.randomUUID().toString();CheckoutResponse first=checkout.checkout(key,request());CheckoutResponse replay=checkout.checkout(key,request());assertThat(replay).isEqualTo(first);assertThat(jdbc.queryForObject("select count(*) from cart_items",Long.class)).isZero();assertThat(jdbc.queryForObject("select state from checkout_operations",String.class)).isEqualTo("COMPLETED");assertThat(jdbc.queryForObject("select count(*) from notifications where type='NEW_ORDER'",Long.class)).isOne();assertThat(jdbc.queryForObject("select count(*) from notification_recipients nr join notifications n on n.id=nr.notification_id where n.type='NEW_ORDER'",Long.class)).isOne();verify(payos,times(1)).createPaymentRequest(any());}

    @Test void definitiveCreateFailureRunsCg005AndKeepsCart(){User buyer=user("failed",UserRole.USER);authenticate(buyer);long product=product(3);cart.add(product,1);doThrow(new PayOSRequestException("rejected",false,null)).when(payos).createPaymentRequest(any());assertThatThrownBy(()->checkout.checkout(UUID.randomUUID().toString(),request())).isInstanceOf(BusinessException.class).extracting(e->((BusinessException)e).code()).isEqualTo(PaymentErrorCodes.PAYOS_REQUEST_FAILED);assertThat(jdbc.queryForObject("select status from orders",String.class)).isEqualTo("CANCELLED");assertThat(jdbc.queryForObject("select status from payments",String.class)).isEqualTo("FAILED");assertThat(jdbc.queryForObject("select reserved_quantity from inventories where product_id=?",Long.class,product)).isZero();assertThat(jdbc.queryForObject("select count(*) from cart_items",Long.class)).isOne();assertThat(jdbc.queryForObject("select count(*) from notifications where type='NEW_ORDER'",Long.class)).isZero();}

    @Test void duplicateVerifiedSuccessSellsOnceThenPaidCancelAndRefundRestoreOnce(){User admin=user("admin2",UserRole.ADMIN),buyer=user("paid",UserRole.USER);authenticate(buyer);long product=product(4);cart.add(product,2);CheckoutResponse response=checkout.checkout(UUID.randomUUID().toString(),request());Payment payment=payments.findById(response.paymentId()).orElseThrow();VerifiedPayOSEvent event=new VerifiedPayOSEvent(response.orderId(),response.totalAmount(),"VND","link-"+response.orderId(),"REF-1",payment.getCreatedAt().plusSeconds(1),true);results.processSuccess(event,payment.getCreatedAt().plusSeconds(2));results.processSuccess(event,payment.getCreatedAt().plusSeconds(3));assertThat(jdbc.queryForObject("select quantity from inventories where product_id=?",Long.class,product)).isEqualTo(2);assertThat(jdbc.queryForObject("select count(*) from inventory_transactions where type='SALE'",Long.class)).isOne();cancellations.cancel(response.orderId(),admin.getId());cancellations.cancel(response.orderId(),admin.getId());assertThat(jdbc.queryForObject("select quantity from inventories where product_id=?",Long.class,product)).isEqualTo(4);assertThat(jdbc.queryForObject("select count(*) from inventory_transactions where type='CANCEL_ORDER'",Long.class)).isOne();refunds.record(response.paymentId(),admin.getId(),"Bank refund completed");refunds.record(response.paymentId(),admin.getId(),"duplicate");assertThat(payments.findById(response.paymentId()).orElseThrow().getStatus()).isEqualTo(PaymentStatus.REFUNDED);assertThat(jdbc.queryForObject("select quantity from inventories where product_id=?",Long.class,product)).isEqualTo(4);}

    @Test void pendingAdminCancellationReleasesLocallyBeforeProviderCall(){User admin=user("admin3",UserRole.ADMIN),buyer=user("pending",UserRole.USER);authenticate(buyer);long product=product(2);cart.add(product,1);CheckoutResponse response=checkout.checkout(UUID.randomUUID().toString(),request());cancellations.cancel(response.orderId(),admin.getId());assertThat(orders.findById(response.orderId()).orElseThrow().getStatus()).isEqualTo(com.bautruc.ecommerce.order.domain.OrderStatus.CANCELLED);assertThat(payments.findById(response.paymentId()).orElseThrow().getStatus()).isEqualTo(PaymentStatus.CANCELLED);assertThat(jdbc.queryForObject("select reserved_quantity from inventories where product_id=?",Long.class,product)).isZero();verify(payos).cancelPaymentRequest("link-"+response.orderId(),"Admin cancelled order");}

    @Test void ambiguousCreateUsesProviderLookupWithoutSecondCreate(){User buyer=user("recover",UserRole.USER);authenticate(buyer);long product=product(2);cart.add(product,1);doThrow(new PayOSRequestException("timeout",true,null)).when(payos).createPaymentRequest(any());when(payos.getPaymentRequest(anyLong())).thenAnswer(i->Optional.of(provider(i.getArgument(0),800)));CheckoutResponse response=checkout.checkout(UUID.randomUUID().toString(),request());assertThat(response.checkoutUrl()).contains("pay.test");verify(payos,times(1)).createPaymentRequest(any());verify(payos,times(1)).getPaymentRequest(response.orderId());}

    @Test void expirationCancelsOrderAndReleasesReservationExactlyOnce(){User buyer=user("expired",UserRole.USER);authenticate(buyer);long product=product(3);cart.add(product,2);CheckoutResponse response=checkout.checkout(UUID.randomUUID().toString(),request());jdbc.update("update payments set created_at=now()-interval '20 minutes', expires_at=now()-interval '1 second' where id=?",response.paymentId());expirationWorker.expire(response.paymentId());expirationWorker.expire(response.paymentId());assertThat(payments.findById(response.paymentId()).orElseThrow().getStatus()).isEqualTo(PaymentStatus.EXPIRED);assertThat(orders.findById(response.orderId()).orElseThrow().getStatus()).isEqualTo(com.bautruc.ecommerce.order.domain.OrderStatus.CANCELLED);assertThat(jdbc.queryForObject("select reserved_quantity from inventories where product_id=?",Long.class,product)).isZero();assertThat(jdbc.queryForObject("select count(*) from inventory_transactions where type='RELEASE'",Long.class)).isOne();}

    @Test void unverifiableLateSuccessExpiresPaymentAndFlagsManualResolution(){User buyer=user("late",UserRole.USER);authenticate(buyer);long product=product(2);cart.add(product,1);CheckoutResponse response=checkout.checkout(UUID.randomUUID().toString(),request());Payment payment=payments.findById(response.paymentId()).orElseThrow();VerifiedPayOSEvent event=new VerifiedPayOSEvent(response.orderId(),response.totalAmount(),"VND","link-"+response.orderId(),"REF-LATE",null,true);results.processSuccess(event,payment.getExpiresAt().plusSeconds(1));Payment expired=payments.findById(response.paymentId()).orElseThrow();assertThat(expired.getStatus()).isEqualTo(PaymentStatus.EXPIRED);assertThat(expired.isManualResolutionRequired()).isTrue();assertThat(jdbc.queryForObject("select reserved_quantity from inventories where product_id=?",Long.class,product)).isZero();assertThat(jdbc.queryForObject("select count(*) from inventory_transactions where type='SALE'",Long.class)).isZero();}

    @Test void paidReplayWithDifferentProviderReferenceIsRejected(){User buyer=user("reference",UserRole.USER);authenticate(buyer);long product=product(1);cart.add(product,1);CheckoutResponse response=checkout.checkout(UUID.randomUUID().toString(),request());Payment payment=payments.findById(response.paymentId()).orElseThrow();VerifiedPayOSEvent accepted=new VerifiedPayOSEvent(response.orderId(),response.totalAmount(),"VND","link-"+response.orderId(),"REF-A",payment.getCreatedAt(),true);results.processSuccess(accepted,payment.getCreatedAt());VerifiedPayOSEvent conflict=new VerifiedPayOSEvent(response.orderId(),response.totalAmount(),"VND","link-"+response.orderId(),"REF-B",payment.getCreatedAt(),true);assertThatThrownBy(()->results.processSuccess(conflict,payment.getCreatedAt())).isInstanceOf(BusinessException.class).extracting(e->((BusinessException)e).code()).isEqualTo(PaymentErrorCodes.PAYMENT_EXTERNAL_ID_CONFLICT);assertThat(jdbc.queryForObject("select count(*) from inventory_transactions where type='SALE'",Long.class)).isOne();}

    @Test void cg007RecoveryFinalizesProviderCreatedCheckout(){User admin=user("recovery-admin",UserRole.ADMIN),buyer=user("cg007",UserRole.USER);authenticate(buyer);long product=product(2);cart.add(product,1);var operation=checkoutOperations.acquire(buyer.getId(),UUID.randomUUID().toString(),"a".repeat(64));checkoutPreparation.prepare(operation.getId(),buyer.getId(),new NormalizedCheckoutRequest("Receiver","0901234567",buyer.getEmail(),"Address",null));checkoutStates.claimCreate(operation.getId());var prepared=checkoutStates.required(operation.getId());checkoutStates.captureCreateSuccess(operation.getId(),"link-"+prepared.getOrderId(),"https://pay.test/"+prepared.getOrderId(),"qr");checkoutRecovery.recover(operation.getId());assertThat(checkoutStates.required(operation.getId()).getState()).isEqualTo(com.bautruc.ecommerce.commerce.domain.CheckoutOperationState.COMPLETED);assertThat(jdbc.queryForObject("select count(*) from cart_items",Long.class)).isZero();assertThat(jdbc.queryForObject("select count(*) from notifications where type='NEW_ORDER'",Long.class)).isOne();assertThat(jdbc.queryForObject("select count(*) from notification_recipients nr join notifications n on n.id=nr.notification_id where n.type='NEW_ORDER'",Long.class)).isOne();verifyNoInteractions(payos);}

    private CheckoutRequest request(){return new CheckoutRequest("Receiver","0901234567",null,"Address",null);}
    private PayOSPaymentResult provider(Long order,long amount){return new PayOSPaymentResult(order,amount,"link-"+order,"PENDING","https://pay.test/"+order,"qr-"+order);}
    private long product(long stock){var c=catalog.createCollection(new CollectionRequest("C","C",null,null,CollectionStatus.ACTIVE));long p=catalog.createProduct(new ProductRequest("Bình","Vase",null,null,1000L,c.getId(),ProductStatus.ACTIVE)).getId();catalog.upsertDiscount(p,new DiscountRequest(DiscountType.FIXED_PRICE,new java.math.BigDecimal("800"),java.time.OffsetDateTime.parse("2020-01-01T00:00:00+07:00"),java.time.OffsetDateTime.parse("2099-01-01T00:00:00+07:00"),true));inventory.adjust(p,InventoryTransactionType.IMPORT,stock,"seed",null);return p;}
    private User user(String key,UserRole role){Instant now=Instant.now();return users.save(new User(key,key+"@example.com",key,null,role,UserStatus.ACTIVE,now,now));}
    private void authenticate(User u){SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(new AuthenticatedUser(u.getId(),u.getEmail(),u.getRole().name()),null,List.of(new SimpleGrantedAuthority("ROLE_"+u.getRole().name()))));}
}
