package com.bautruc.ecommerce.review.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.Instant;
import java.util.UUID;
import com.bautruc.ecommerce.common.security.JwtAuthenticationFilter;
import com.bautruc.ecommerce.common.security.JwtTokenService;
import com.bautruc.ecommerce.common.storage.ObjectStoragePort;
import com.bautruc.ecommerce.identity.domain.*;
import com.bautruc.ecommerce.identity.infrastructure.UserJpaRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.*;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test") @DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS) @Testcontainers
class ProductReviewApiIntegrationTest {
    @Container static final PostgreSQLContainer<?> postgres=new PostgreSQLContainer<>("postgres:16-alpine");
    @DynamicPropertySource static void database(DynamicPropertyRegistry r){r.add("spring.datasource.url",postgres::getJdbcUrl);r.add("spring.datasource.username",postgres::getUsername);r.add("spring.datasource.password",postgres::getPassword);}
    @Autowired MockMvc mvc; @Autowired JdbcTemplate jdbc; @Autowired UserJpaRepository users; @Autowired JwtTokenService jwt; @MockitoBean ObjectStoragePort storage;
    @BeforeEach void clean(){jdbc.execute("TRUNCATE TABLE users, collections CASCADE");}
    @Test void guestCanReadPublicReviews() throws Exception {long id=product();mvc.perform(get("/api/v1/products/{id}/reviews",id)).andExpect(status().isOk()).andExpect(jsonPath("$.data.ratingCount").value(0)).andExpect(jsonPath("$.data.reviews.content").isEmpty());}
    @Test void completedPaidBuyerCanCreateAndEditOnlyOnce() throws Exception {User u=user(UserRole.USER);long id=product();purchase(u,id,"COMPLETED","PAID");Cookie c=access(u);mvc.perform(post("/api/v1/me/products/{id}/review",id).cookie(c).with(csrf().asHeader()).contentType(MediaType.APPLICATION_JSON).content(review(5,"San pham rat dep va chat luong."))).andExpect(status().isCreated());mvc.perform(post("/api/v1/me/products/{id}/review",id).cookie(c).with(csrf().asHeader()).contentType(MediaType.APPLICATION_JSON).content(review(4,"San pham duoc dong goi rat tot."))).andExpect(status().isConflict());mvc.perform(put("/api/v1/me/products/{id}/review",id).cookie(c).with(csrf().asHeader()).contentType(MediaType.APPLICATION_JSON).content(review(4,"San pham duoc dong goi rat tot."))).andExpect(status().isOk()).andExpect(jsonPath("$.data.rating").value(4));assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM product_reviews",Long.class)).isEqualTo(1L);}
    @Test void unpaidOrUncompletedOrdersCannotReview() throws Exception {User u=user(UserRole.USER);long id=product();purchase(u,id,"NEW","PENDING");mvc.perform(post("/api/v1/me/products/{id}/review",id).cookie(access(u)).with(csrf().asHeader()).contentType(MediaType.APPLICATION_JSON).content(review(5,"Toi muon danh gia san pham nay."))).andExpect(status().isForbidden()).andExpect(jsonPath("$.error.code").value("REVIEW_NOT_ELIGIBLE"));}
    private User user(UserRole role){String id=UUID.randomUUID().toString();Instant n=Instant.now();return users.save(new User("review-"+id,"review-"+id+"@example.com","Test Customer",null,role,UserStatus.ACTIVE,n,n));}
    private Cookie access(User u){return new Cookie(JwtAuthenticationFilter.ACCESS_COOKIE_NAME,jwt.createAccessToken(u));}
    private long product(){Long c=jdbc.queryForObject("INSERT INTO collections(name_vi,name_en,status,created_at,updated_at) VALUES ('Gom Bau Truc','Bau Truc pottery','ACTIVE',now(),now()) RETURNING id",Long.class);return jdbc.queryForObject("INSERT INTO products(name_vi,name_en,base_price,status,collection_id,created_at,updated_at) VALUES ('Binh gom','Pottery vase',100000,'ACTIVE',?,now(),now()) RETURNING id",Long.class,c);}
    private void purchase(User u,long p,String os,String ps){Long o=jdbc.queryForObject("INSERT INTO orders(id,order_code,user_id,receiver_name,phone,email,address,total_amount,status,completed_at,created_at,updated_at) VALUES(nextval('app_global_id_seq'),?,?,'Buyer','0901234567','buyer@example.com','Bau Truc',100000,?,CASE WHEN ?='COMPLETED' THEN now() ELSE NULL END,now(),now()) RETURNING id",Long.class,"QA-"+UUID.randomUUID(),u.getId(),os,os);jdbc.update("INSERT INTO order_items(order_id,product_id,product_name_vi,product_name_en,base_price,selling_price,quantity,total_price) VALUES(?,?, 'Binh gom','Pottery vase',100000,100000,1,100000)",o,p);jdbc.update("INSERT INTO payments(order_id,provider,status,amount,created_at,expires_at,paid_at,updated_at) VALUES(?, 'PAYOS',?,100000,now(),now()+interval '15 minutes',CASE WHEN ?='PAID' THEN now() ELSE NULL END,now())",o,ps,ps);}
    private String review(int rating,String body){return "{\"rating\":"+rating+",\"body\":\""+body+"\"}";}
}
