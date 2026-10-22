package com.bautruc.ecommerce.commerce.application;
import static org.assertj.core.api.Assertions.*;import java.time.Instant;import java.util.*;import java.util.concurrent.*;
import com.bautruc.ecommerce.commerce.api.request.CheckoutRequest;
import com.bautruc.ecommerce.cart.api.request.AddCartItemRequest;import com.bautruc.ecommerce.cart.application.CartApplicationService;import com.bautruc.ecommerce.catalog.api.request.*;import com.bautruc.ecommerce.catalog.application.CatalogService;import com.bautruc.ecommerce.catalog.domain.*;import com.bautruc.ecommerce.commerce.domain.*;import com.bautruc.ecommerce.commerce.infrastructure.*;import com.bautruc.ecommerce.common.exception.BusinessException;import com.bautruc.ecommerce.common.security.*;import com.bautruc.ecommerce.identity.domain.*;import com.bautruc.ecommerce.identity.infrastructure.UserJpaRepository;import com.bautruc.ecommerce.inventory.application.InventoryCommandService;import com.bautruc.ecommerce.inventory.domain.InventoryTransactionType;import com.bautruc.ecommerce.order.infrastructure.*;import com.bautruc.ecommerce.payment.infrastructure.PaymentJpaRepository;
import org.junit.jupiter.api.*;import org.springframework.beans.factory.annotation.Autowired;import org.springframework.boot.test.context.SpringBootTest;import org.springframework.jdbc.core.JdbcTemplate;import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;import org.springframework.security.core.*;import org.springframework.security.core.authority.SimpleGrantedAuthority;import org.springframework.security.core.context.SecurityContextHolder;import org.springframework.test.context.*;import org.testcontainers.containers.PostgreSQLContainer;import org.testcontainers.junit.jupiter.*;
@SpringBootTest @ActiveProfiles("test") @Testcontainers
class CheckoutPhase3IntegrationTest{
 @Container static final PostgreSQLContainer<?> postgres=new PostgreSQLContainer<>("postgres:16-alpine");@DynamicPropertySource static void db(DynamicPropertyRegistry r){r.add("spring.datasource.url",postgres::getJdbcUrl);r.add("spring.datasource.username",postgres::getUsername);r.add("spring.datasource.password",postgres::getPassword);}
 @Autowired CatalogService catalog;@Autowired InventoryCommandService inventory;@Autowired CartApplicationService cart;@Autowired CheckoutOperationService operationService;@Autowired CheckoutLocalPreparationService preparation;@Autowired CheckoutRequestHasher hasher;@Autowired CheckoutOperationJpaRepository operations;@Autowired CheckoutOperationItemJpaRepository operationItems;@Autowired OrderJpaRepository orders;@Autowired OrderItemJpaRepository orderItems;@Autowired PaymentJpaRepository payments;@Autowired UserJpaRepository users;@Autowired JdbcTemplate jdbc;
 @Autowired CheckoutStateService states;@Autowired CheckoutFinalizationService finalization;@Autowired CheckoutApplicationService checkoutApplication;
 @BeforeEach void clean(){SecurityContextHolder.clearContext();jdbc.execute("TRUNCATE TABLE collections, users CASCADE");}
 @AfterEach void clear(){SecurityContextHolder.clearContext();}
 @Test void cg001CreatesAtomicSnapshotsReservationAndPendingPaymentWithoutClearingCart(){User user=user("happy");authenticate(user);long product=product(10);cart.add(product,2);NormalizedCheckoutRequest request=req("Address A");CheckoutOperation op=operationService.acquire(user.getId(),UUID.randomUUID().toString(),hasher.hash(request));CheckoutLocalPreparationResult result=preparation.prepare(op.getId(),user.getId(),request);assertThat(result.state()).isEqualTo(CheckoutOperationState.LOCAL_PREPARED);assertThat(orders.count()).isEqualTo(1);assertThat(orderItems.findByOrderIdOrderById(result.orderId())).singleElement().satisfies(i->{assertThat(i.getProductNameVi()).isEqualTo("Bình");assertThat(i.getSellingPrice()).isEqualTo(800);assertThat(i.getTotalPrice()).isEqualTo(1600);});assertThat(payments.findById(result.paymentId()).orElseThrow()).satisfies(p->{assertThat(p.getAmount()).isEqualTo(1600);assertThat(p.getExpiresAt()).isEqualTo(p.getCreatedAt().plusSeconds(900));});assertThat(operationItems.findByCheckoutOperationId(op.getId())).hasSize(1);assertThat(jdbc.queryForObject("select reserved_quantity from inventories where product_id=?",Long.class,product)).isEqualTo(2);assertThat(jdbc.queryForObject("select count(*) from cart_items",Long.class)).isEqualTo(1);}
 @Test void insufficientInventoryRollsBackEveryCg001BusinessEffect(){User user=user("rollback");authenticate(user);long product=product(1);cart.add(product,1);jdbc.update("update cart_items set quantity=2 where product_id=?",product);NormalizedCheckoutRequest request=req("Address B");CheckoutOperation op=operationService.acquire(user.getId(),UUID.randomUUID().toString(),hasher.hash(request));assertThatThrownBy(()->preparation.prepare(op.getId(),user.getId(),request)).isInstanceOf(BusinessException.class).extracting(e->((BusinessException)e).code()).isEqualTo("INVENTORY_INSUFFICIENT");assertThat(orders.count()).isZero();assertThat(payments.count()).isZero();assertThat(operationItems.count()).isZero();assertThat(jdbc.queryForObject("select reserved_quantity from inventories where product_id=?",Long.class,product)).isZero();assertThat(operations.findById(op.getId()).orElseThrow().getState()).isEqualTo(CheckoutOperationState.STARTED);}
 @Test void sameKeyIsIdempotentAndDifferentRequestIsRejected(){User user=user("idem");authenticate(user);long product=product(5);cart.add(product,1);String key=UUID.randomUUID().toString();NormalizedCheckoutRequest request=req("A");CheckoutOperation first=operationService.acquire(user.getId(),key,hasher.hash(request));CheckoutLocalPreparationResult a=preparation.prepare(first.getId(),user.getId(),request);CheckoutOperation second=operationService.acquire(user.getId(),key,hasher.hash(request));CheckoutLocalPreparationResult b=preparation.prepare(second.getId(),user.getId(),request);assertThat(b.orderId()).isEqualTo(a.orderId());assertThat(orders.count()).isEqualTo(1);assertThat(payments.count()).isEqualTo(1);assertThat(jdbc.queryForObject("select count(*) from inventory_transactions where type='RESERVE'",Long.class)).isEqualTo(1);NormalizedCheckoutRequest changed=req("B");assertThatThrownBy(()->operationService.acquire(user.getId(),key,hasher.hash(changed))).isInstanceOf(BusinessException.class).extracting(e->((BusinessException)e).code()).isEqualTo(CommerceErrorCodes.IDEMPOTENCY_KEY_REUSED);}
 @Test void concurrentSameKeyCreatesOneOperation()throws Exception{User user=user("concurrent");String key=UUID.randomUUID().toString();String hash=hasher.hash(req("C"));ExecutorService pool=Executors.newFixedThreadPool(2);CountDownLatch start=new CountDownLatch(1);Callable<Long> task=()->{start.await();return operationService.acquire(user.getId(),key,hash).getId();};Future<Long>a=pool.submit(task),b=pool.submit(task);start.countDown();assertThat(a.get(10,TimeUnit.SECONDS)).isEqualTo(b.get(10,TimeUnit.SECONDS));assertThat(operations.count()).isEqualTo(1);pool.shutdownNow();}
 @Test
 void buyNowDoesNotReadOrClearExistingCart() {
     User user = user("buy-now-cart");
     authenticate(user);

     long savedProduct = product(10);
     long buyNowProduct = product(10);

     cart.add(savedProduct, 2);

     NormalizedCheckoutRequest request = new NormalizedCheckoutRequest(
             "Receiver",
             "0901234567",
             "buyer@example.com",
             "Address A",
             null,
             buyNowProduct,
             1
     );

     CheckoutOperation operation = operationService.acquire(
             user.getId(),
             UUID.randomUUID().toString(),
             hasher.hash(request)
     );

     CheckoutLocalPreparationResult result = preparation.prepare(
             operation.getId(),
             user.getId(),
             request
     );

     assertThat(operationItems.findByCheckoutOperationId(operation.getId()))
             .isEmpty();

     assertThat(orderItems.findByOrderIdOrderById(result.orderId()))
             .singleElement()
             .satisfies(item -> {
                 assertThat(item.getProductId()).isEqualTo(buyNowProduct);
                 assertThat(item.getQuantity()).isEqualTo(1);
             });

     assertThat(cart.view().items())
             .singleElement()
             .satisfies(item -> {
                 assertThat(item.productId()).isEqualTo(savedProduct);
                 assertThat(item.quantity()).isEqualTo(2);
             });

     assertThat(states.claimCreate(operation.getId())).isTrue();

     states.captureCreateSuccess(
             operation.getId(),
             "test-payment-link-" + operation.getId(),
             "https://pay.example/checkout",
             "test-qr"
     );

     finalization.finalizeCheckout(operation.getId());

     assertThat(operations.findById(operation.getId()).orElseThrow().getState())
             .isEqualTo(CheckoutOperationState.COMPLETED);

     assertThat(cart.view().items())
             .singleElement()
             .satisfies(item -> {
                 assertThat(item.productId()).isEqualTo(savedProduct);
                 assertThat(item.quantity()).isEqualTo(2);
             });
 }

 @Test
 void buyNowWorksWithoutExistingCart() {
     User user = user("buy-now-empty");
     authenticate(user);

     long productId = product(5);

     NormalizedCheckoutRequest request = new NormalizedCheckoutRequest(
             "Receiver",
             "0901234567",
             "buyer@example.com",
             "Address B",
             null,
             productId,
             1
     );

     CheckoutOperation operation = operationService.acquire(
             user.getId(),
             UUID.randomUUID().toString(),
             hasher.hash(request)
     );

     CheckoutLocalPreparationResult result = preparation.prepare(
             operation.getId(),
             user.getId(),
             request
     );

     assertThat(result.state())
             .isEqualTo(CheckoutOperationState.LOCAL_PREPARED);

     assertThat(operationItems.findByCheckoutOperationId(operation.getId()))
             .isEmpty();

     assertThat(jdbc.queryForObject(
             "SELECT COUNT(*) FROM cart_items",
             Long.class
     )).isZero();

     assertThat(states.claimCreate(operation.getId())).isTrue();

     states.captureCreateSuccess(
             operation.getId(),
             "test-payment-link-" + operation.getId(),
             "https://pay.example/checkout",
             "test-qr"
     );

     finalization.finalizeCheckout(operation.getId());

     assertThat(operations.findById(operation.getId()).orElseThrow().getState())
             .isEqualTo(CheckoutOperationState.COMPLETED);

     assertThat(jdbc.queryForObject(
             "SELECT COUNT(*) FROM cart_items",
             Long.class
     )).isZero();
 }

 @Test
 void buyNowRejectsInsufficientStockAndIncompleteSelection() {
     User user = user("buy-now-invalid");
     authenticate(user);

     long productId = product(1);

     CheckoutRequest incomplete = new CheckoutRequest(
             "Receiver",
             "0901234567",
             "buyer@example.com",
             "Address C",
             null,
             productId,
             null
     );

     assertThatThrownBy(() ->
             checkoutApplication.checkout(
                     UUID.randomUUID().toString(),
                     incomplete
             )
     ).isInstanceOf(BusinessException.class)
      .extracting(e -> ((BusinessException) e).code())
      .isEqualTo("VALIDATION_FAILED");

     NormalizedCheckoutRequest request = new NormalizedCheckoutRequest(
             "Receiver",
             "0901234567",
             "buyer@example.com",
             "Address C",
             null,
             productId,
             2
     );

     CheckoutOperation operation = operationService.acquire(
             user.getId(),
             UUID.randomUUID().toString(),
             hasher.hash(request)
     );

     assertThatThrownBy(() ->
             preparation.prepare(
                     operation.getId(),
                     user.getId(),
                     request
             )
     ).isInstanceOf(BusinessException.class)
      .extracting(e -> ((BusinessException) e).code())
      .isEqualTo("INVENTORY_INSUFFICIENT");

     assertThat(orders.count()).isZero();
     assertThat(payments.count()).isZero();
     assertThat(operationItems.count()).isZero();
 }
 private long product(long stock){var c=catalog.createCollection(new CollectionRequest("C","C",null,null,CollectionStatus.ACTIVE));long p=catalog.createProduct(new ProductRequest("Bình","Vase",null,null,1000L,c.getId(),ProductStatus.ACTIVE)).getId();catalog.upsertDiscount(p,new DiscountRequest(DiscountType.FIXED_PRICE,new java.math.BigDecimal("800"),java.time.OffsetDateTime.parse("2020-01-01T00:00:00+07:00"),java.time.OffsetDateTime.parse("2099-01-01T00:00:00+07:00"),true));inventory.adjust(p,InventoryTransactionType.IMPORT,stock,"seed",null);return p;}
 private User user(String key){Instant n=Instant.now();return users.save(new User(key,key+"@example.com",key,null,UserRole.USER,UserStatus.ACTIVE,n,n));}private void authenticate(User u){AuthenticatedUser p=new AuthenticatedUser(u.getId(),u.getEmail(),u.getRole().name());Authentication a=new UsernamePasswordAuthenticationToken(p,null,List.of(new SimpleGrantedAuthority("ROLE_USER")));SecurityContextHolder.getContext().setAuthentication(a);}private NormalizedCheckoutRequest req(String address){return new NormalizedCheckoutRequest("Receiver","0901234567","buyer@example.com",address,null);}
}
