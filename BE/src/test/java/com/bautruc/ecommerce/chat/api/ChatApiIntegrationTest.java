package com.bautruc.ecommerce.chat.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.bautruc.ecommerce.chat.application.ChatRetentionJob;
import com.bautruc.ecommerce.common.storage.ObjectStoragePort;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test") @Testcontainers
class ChatApiIntegrationTest {
    @Container static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");
    @DynamicPropertySource static void database(DynamicPropertyRegistry r){r.add("spring.datasource.url",postgres::getJdbcUrl);r.add("spring.datasource.username",postgres::getUsername);r.add("spring.datasource.password",postgres::getPassword);}
    @Autowired MockMvc mvc; @Autowired JdbcTemplate jdbc; @Autowired ChatRetentionJob retention; @MockitoBean ObjectStoragePort storage;
    @BeforeEach void clean(){jdbc.execute("TRUNCATE TABLE chat_conversations, users CASCADE");}
    @Test void guestConversationsArePrivate() throws Exception { Cookie a=send("A"),b=send("B");mvc.perform(get("/api/v1/chat/session").cookie(a)).andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store")).andExpect(jsonPath("$.data.messages[0].body").value("A"));mvc.perform(get("/api/v1/chat/session").cookie(b)).andExpect(jsonPath("$.data.messages[0].body").value("B"));mvc.perform(get("/api/v1/chat/session")).andExpect(jsonPath("$.data.conversation").doesNotExist());}
    @Test void csrfIsRequiredForGuestMessages() throws Exception {mvc.perform(post("/api/v1/chat/messages").contentType(MediaType.APPLICATION_JSON).content(message("x"))).andExpect(status().isForbidden());}
    @Test void retentionDeletesClosedChatAfter60Days() throws Exception {Cookie guest=send("x");mvc.perform(patch("/api/v1/chat/close").cookie(guest).with(csrf().asHeader())).andExpect(status().isOk());jdbc.update("UPDATE chat_conversations SET closed_at=now()-INTERVAL '61 days'");retention.run();assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM chat_conversations",Long.class)).isZero();}
    private Cookie send(String body)throws Exception{var r=mvc.perform(post("/api/v1/chat/messages").with(csrf().asHeader()).contentType(MediaType.APPLICATION_JSON).content(message(body))).andExpect(status().isOk()).andReturn();String h=r.getResponse().getHeader("Set-Cookie");assertThat(h).contains("HttpOnly").contains("SameSite=Lax");return new Cookie("BT_CHAT",h.split(";",2)[0].substring("BT_CHAT=".length()));}
    private String message(String body){return "{\"body\":\""+body+"\"}";}
}
