package com.bautruc.ecommerce.chat.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.Instant;
import java.util.UUID;

import com.bautruc.ecommerce.chat.application.ChatRetentionJob;
import com.bautruc.ecommerce.common.storage.ObjectStoragePort;
import com.bautruc.ecommerce.common.security.JwtAuthenticationFilter;
import com.bautruc.ecommerce.common.security.JwtTokenService;
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

import org.springframework.test.context.ActiveProfiles;

import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;

import org.springframework.test.context.DynamicPropertyRegistry;

import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test") @DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Testcontainers
class ChatApiIntegrationTest {
    @Container static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");
    @DynamicPropertySource static void database(DynamicPropertyRegistry r){r.add("spring.datasource.url",postgres::getJdbcUrl);r.add("spring.datasource.username",postgres::getUsername);r.add("spring.datasource.password",postgres::getPassword);}
    @Autowired MockMvc mvc; @Autowired JdbcTemplate jdbc; @Autowired ChatRetentionJob retention; @Autowired UserJpaRepository users; @Autowired JwtTokenService jwt; @MockitoBean ObjectStoragePort storage;
    @BeforeEach void clean(){jdbc.execute("TRUNCATE TABLE chat_conversations, users CASCADE");}
    @Test void guestConversationsArePrivate() throws Exception { Cookie a=send("A"),b=send("B");mvc.perform(get("/api/v1/chat/session").cookie(a)).andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store")).andExpect(jsonPath("$.data.messages[0].body").value("A"));mvc.perform(get("/api/v1/chat/session").cookie(b)).andExpect(jsonPath("$.data.messages[0].body").value("B"));mvc.perform(get("/api/v1/chat/session")).andExpect(jsonPath("$.data.conversation").doesNotExist());}
    @Test void csrfIsRequiredForGuestMessages() throws Exception {mvc.perform(post("/api/v1/chat/messages").contentType(MediaType.APPLICATION_JSON).content(message("x"))).andExpect(status().isForbidden());}
    @Test void customerMessageNotifiesAdmin() throws Exception {
        Cookie admin = access(UserRole.ADMIN);
        Cookie guest = send("Can tu van san pham");
        mvc.perform(post("/api/v1/chat/messages").cookie(guest).with(csrf().asHeader()).contentType(MediaType.APPLICATION_JSON).content(message("Them thong tin"))).andExpect(status().isOk());
        mvc.perform(get("/api/v1/admin/notifications?isRead=false").cookie(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(2))
                .andExpect(jsonPath("$.data.content[0].type").value("CHAT_MESSAGE"));
    }
    @Test void retentionDeletesClosedChatAfter60Days() throws Exception {Cookie guest=send("x");mvc.perform(patch("/api/v1/chat/close").cookie(guest).with(csrf().asHeader())).andExpect(status().isOk());jdbc.update("UPDATE chat_conversations SET closed_at=now()-INTERVAL '61 days'");retention.run();assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM chat_conversations",Long.class)).isZero();}
    @Test void adminCanReplyCloseAndPreventFurtherReplies() throws Exception {Cookie guest=send("Can tu van san pham"),admin=access(UserRole.ADMIN);Long id=jdbc.queryForObject("SELECT MAX(id) FROM chat_conversations",Long.class);mvc.perform(get("/api/v1/admin/chat/conversations").cookie(admin)).andExpect(status().isOk()).andExpect(jsonPath("$.data.content.length()").value(1));mvc.perform(post("/api/v1/admin/chat/conversations/{id}/messages",id).cookie(admin).with(csrf().asHeader()).contentType(MediaType.APPLICATION_JSON).content(message("Cua hang xin chao"))).andExpect(status().isOk()).andExpect(jsonPath("$.data.messages[1].sender").value("ADMIN"));mvc.perform(get("/api/v1/chat/session").cookie(guest)).andExpect(jsonPath("$.data.messages.length()").value(2));mvc.perform(patch("/api/v1/admin/chat/conversations/{id}/close",id).cookie(admin).with(csrf().asHeader())).andExpect(status().isOk()).andExpect(jsonPath("$.data.conversation.status").value("CLOSED"));mvc.perform(post("/api/v1/admin/chat/conversations/{id}/messages",id).cookie(admin).with(csrf().asHeader()).contentType(MediaType.APPLICATION_JSON).content(message("Khong gui sau khi dong"))).andExpect(status().isConflict());}
    @Test void userCannotAccessAdminChatOrAnotherUserThread() throws Exception {Cookie first=access(UserRole.USER),second=access(UserRole.USER);mvc.perform(post("/api/v1/chat/messages").cookie(first).with(csrf().asHeader()).contentType(MediaType.APPLICATION_JSON).content(message("Don hang cua nguoi thu nhat"))).andExpect(status().isOk());mvc.perform(get("/api/v1/chat/session").cookie(second)).andExpect(jsonPath("$.data.conversation").doesNotExist());mvc.perform(get("/api/v1/admin/chat/conversations").cookie(first)).andExpect(status().isForbidden());mvc.perform(get("/api/v1/chat/session").cookie(access(UserRole.ADMIN))).andExpect(status().isForbidden());}
    private Cookie access(UserRole role){Instant now=Instant.now();String id=UUID.randomUUID().toString();User user=users.save(new User("chat-qa-"+id,"chat-qa-"+id+"@example.com","QA Chat",null,role,UserStatus.ACTIVE,now,now));return new Cookie(JwtAuthenticationFilter.ACCESS_COOKIE_NAME,jwt.createAccessToken(user));}
    private Cookie send(String body)throws Exception{var r=mvc.perform(post("/api/v1/chat/messages").with(csrf().asHeader()).contentType(MediaType.APPLICATION_JSON).content(message(body))).andExpect(status().isOk()).andReturn();String h=r.getResponse().getHeader("Set-Cookie");assertThat(h).contains("HttpOnly").contains("SameSite=Lax");return new Cookie("BT_CHAT",h.split(";",2)[0].substring("BT_CHAT=".length()));}
    private String message(String body){return "{\"body\":\""+body+"\"}";}
}
