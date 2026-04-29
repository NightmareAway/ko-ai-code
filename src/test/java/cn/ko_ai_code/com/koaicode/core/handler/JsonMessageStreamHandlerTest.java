package cn.ko_ai_code.com.koaicode.core.handler;

import cn.ko_ai_code.com.koaicode.core.builder.VueProjectBuilder;
import cn.ko_ai_code.com.koaicode.entity.User;
import cn.ko_ai_code.com.koaicode.model.enums.ChatHistoryMessageTypeEnum;
import cn.ko_ai_code.com.koaicode.service.ChatHistoryService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Flux;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class JsonMessageStreamHandlerTest {

    @Test
    void handle_shouldFallbackToPlainText_whenChunkIsNotJson() {
        JsonMessageStreamHandler handler = new JsonMessageStreamHandler();
        VueProjectBuilder vueProjectBuilder = mock(VueProjectBuilder.class);
        ChatHistoryService chatHistoryService = mock(ChatHistoryService.class);
        ReflectionTestUtils.setField(handler, "vueProjectBuilder", vueProjectBuilder);
        // plain text 分支不会触发 toolManager，保留 null 即可

        User user = new User();
        user.setId(1001L);

        List<String> results = handler.handle(Flux.just("hello", " vue"), chatHistoryService, 123L, user)
                .collectList()
                .block();

        assertEquals(List.of("hello", " vue"), results);
        verify(vueProjectBuilder).buildProjectAsync(anyString(), eq(123L));

        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(chatHistoryService, times(1)).addChatMessage(
                eq(123L),
                messageCaptor.capture(),
                eq(ChatHistoryMessageTypeEnum.AI.getValue()),
                eq(1001L)
        );
        assertEquals("hello vue", messageCaptor.getValue());
    }

    @Test
    void handle_shouldExtractDataField_whenJsonWithoutType() {
        JsonMessageStreamHandler handler = new JsonMessageStreamHandler();
        VueProjectBuilder vueProjectBuilder = mock(VueProjectBuilder.class);
        ChatHistoryService chatHistoryService = mock(ChatHistoryService.class);
        ReflectionTestUtils.setField(handler, "vueProjectBuilder", vueProjectBuilder);

        User user = new User();
        user.setId(2002L);
        String chunk = "{\"data\":\"<template><div>ok</div></template>\"}";

        List<String> results = handler.handle(Flux.just(chunk), chatHistoryService, 124L, user)
                .collectList()
                .block();

        assertEquals(1, results.size());
        assertTrue(results.getFirst().contains("<template>"));
        verify(chatHistoryService, times(1)).addChatMessage(
                eq(124L),
                contains("<template><div>ok</div></template>"),
                eq(ChatHistoryMessageTypeEnum.AI.getValue()),
                eq(2002L)
        );
    }
}