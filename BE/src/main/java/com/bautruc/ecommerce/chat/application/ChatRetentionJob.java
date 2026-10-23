package com.bautruc.ecommerce.chat.application;
import org.springframework.scheduling.annotation.Scheduled;import org.springframework.stereotype.Component;
@Component public class ChatRetentionJob {private final ChatService chat;public ChatRetentionJob(ChatService chat){this.chat=chat;}@Scheduled(cron="0 20 3 * * *",zone="Asia/Ho_Chi_Minh") public void run(){chat.deleteExpired();}}
