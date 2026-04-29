package cn.ko_ai_code.com.koaicode.controller;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.ko_ai_code.com.koaicode.entity.User;
import cn.ko_ai_code.com.koaicode.exception.BusinessException;
import cn.ko_ai_code.com.koaicode.exception.ErrorCode;
import cn.ko_ai_code.com.koaicode.model.vo.LoginUserVO;
import cn.ko_ai_code.com.koaicode.service.AppService;
import cn.ko_ai_code.com.koaicode.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppControllerTest {

    @InjectMocks
    private AppController appController;

    @Mock
    private AppService appService;

    @Mock
    private UserService userService;

    @Mock
    private HttpServletRequest request;

    private final Long validAppId = 123456789L;
    private final String validMessage = "帮我创建一个登录页面";
    private User loginUser;

    @BeforeEach
    void setUp() {
        loginUser = User.builder()
                .userAccount("kologram")
                .userPassword("123456789")
                .build();
    }

    // ==================== 成功场景 ====================

    @Test
    @DisplayName("成功生成代码 - 正常流式返回多个chunk")
    void chatToGenCode() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        userService.userLogin("kologram", "123456789", request);
        Flux<ServerSentEvent<String>> serverSentEventFlux = appController.chatToGenCode(10L, "做一个公司官网，需要首页、关于我们、联系我们三个页面", request);
    }

    @Test
    @DisplayName("成功生成代码 - 正常流式返回多个chunk")
    void chatToGenCode_SuccessWithMultipleChunks() {
        // Arrange
        when(userService.getLoginUser(request)).thenReturn(loginUser);
        when(appService.chatToGenCode(validAppId, validMessage, loginUser))
                .thenReturn(Flux.just("正在分析需求...", "开始生成HTML...", "<div>登录页面</div>"));

        // Act
        Flux<ServerSentEvent<String>> result = appController.chatToGenCode(validAppId, validMessage, request);
        List<ServerSentEvent<String>> events = result.collectList().block();

        // Assert
        assertNotNull(events);
        assertEquals(5, events.size());

        // 验证第一个事件是 start
        ServerSentEvent<String> startEvent = events.get(0);
        assertEquals("start", startEvent.event());
        JSONObject startData = JSONUtil.parseObj(startEvent.data());
        assertEquals("正在生成代码，请稍候...", startData.get("d", String.class));

        // 验证 body 事件
        ServerSentEvent<String> bodyEvent1 = events.get(1);
        assertEquals("body", bodyEvent1.event());
        JSONObject bodyData1 = JSONUtil.parseObj(bodyEvent1.data());
        assertEquals("正在分析需求...", bodyData1.get("d", String.class));

        ServerSentEvent<String> bodyEvent2 = events.get(2);
        assertEquals("body", bodyEvent2.event());
        JSONObject bodyData2 = JSONUtil.parseObj(bodyEvent2.data());
        assertEquals("开始生成HTML...", bodyData2.get("d", String.class));

        ServerSentEvent<String> bodyEvent3 = events.get(3);
        assertEquals("body", bodyEvent3.event());
        JSONObject bodyData3 = JSONUtil.parseObj(bodyEvent3.data());
        assertEquals("<div>登录页面</div>", bodyData3.get("d", String.class));

        // 验证最后一个事件是 done
        ServerSentEvent<String> doneEvent = events.get(4);
        assertEquals("done", doneEvent.event());
        assertEquals("", doneEvent.data());

        // 验证 service 调用
        verify(userService).getLoginUser(request);
        verify(appService).chatToGenCode(validAppId, validMessage, loginUser);
        verifyNoMoreInteractions(userService, appService);
    }

    @Test
    @DisplayName("成功生成代码 - 单个chunk的边界情况")
    void chatToGenCode_SuccessWithSingleChunk() {
        // Arrange
        when(userService.getLoginUser(request)).thenReturn(loginUser);
        when(appService.chatToGenCode(validAppId, validMessage, loginUser))
                .thenReturn(Flux.just("<html></html>"));

        // Act
        Flux<ServerSentEvent<String>> result = appController.chatToGenCode(validAppId, validMessage, request);
        List<ServerSentEvent<String>> events = result.collectList().block();

        // Assert
        assertNotNull(events);
        assertEquals(3, events.size());
        assertEquals("start", events.get(0).event());
        assertEquals("body", events.get(1).event());
        assertEquals("done", events.get(2).event());

        verify(userService).getLoginUser(request);
        verify(appService).chatToGenCode(validAppId, validMessage, loginUser);
    }

    @Test
    @DisplayName("成功生成代码 - 空字符串chunk也应正常传递")
    void chatToGenCode_SuccessWithEmptyChunks() {
        // Arrange
        when(userService.getLoginUser(request)).thenReturn(loginUser);
        when(appService.chatToGenCode(validAppId, validMessage, loginUser))
                .thenReturn(Flux.just("", "  ", ""));

        // Act
        Flux<ServerSentEvent<String>> result = appController.chatToGenCode(validAppId, validMessage, request);
        List<ServerSentEvent<String>> events = result.collectList().block();

        // Assert
        assertNotNull(events);
        assertEquals(5, events.size());
        assertEquals("start", events.get(0).event());
        assertEquals("body", events.get(1).event());
        assertEquals("body", events.get(2).event());
        assertEquals("body", events.get(3).event());
        assertEquals("done", events.get(4).event());
    }

    // ==================== 异常场景 - service 层抛出异常 ====================

    @Test
    @DisplayName("生成代码时 service 抛出异常，应返回 error 事件")
    void chatToGenCode_WhenServiceThrowsException_ShouldReturnErrorEvent() {
        // Arrange
        when(userService.getLoginUser(request)).thenReturn(loginUser);
        when(appService.chatToGenCode(validAppId, validMessage, loginUser))
                .thenReturn(Flux.error(new RuntimeException("AI服务调用超时")));

        // Act
        Flux<ServerSentEvent<String>> result = appController.chatToGenCode(validAppId, validMessage, request);
        List<ServerSentEvent<String>> events = result.collectList().block();

        // Assert
        assertNotNull(events);
        assertEquals(3, events.size());

        // start 事件
        assertEquals("start", events.get(0).event());

        // error 事件
        ServerSentEvent<String> errorEvent = events.get(1);
        assertEquals("error", errorEvent.event());
        JSONObject errorData = JSONUtil.parseObj(errorEvent.data());
        assertTrue(errorData.get("d", String.class).contains("生成失败："));
        assertTrue(errorData.get("d", String.class).contains("AI服务调用超时"));

        // done 事件
        assertEquals("done", events.get(2).event());
    }

    @Test
    @DisplayName("生成代码时 service 抛出无消息的异常，应使用默认错误提示")
    void chatToGenCode_WhenServiceThrowsExceptionWithoutMessage_ShouldUseDefaultMessage() {
        // Arrange
        when(userService.getLoginUser(request)).thenReturn(loginUser);
        when(appService.chatToGenCode(validAppId, validMessage, loginUser))
                .thenReturn(Flux.error(new RuntimeException()));

        // Act
        Flux<ServerSentEvent<String>> result = appController.chatToGenCode(validAppId, validMessage, request);
        List<ServerSentEvent<String>> events = result.collectList().block();

        // Assert
        assertNotNull(events);
        assertEquals(3, events.size());
        assertEquals("error", events.get(1).event());
        JSONObject errorData = JSONUtil.parseObj(events.get(1).data());
        assertEquals("生成失败：生成代码失败", errorData.get("d", String.class));
    }

    @Test
    @DisplayName("生成代码时 service 抛出空消息异常，应使用默认错误提示")
    void chatToGenCode_WhenServiceThrowsExceptionWithBlankMessage_ShouldUseDefaultMessage() {
        // Arrange
        when(userService.getLoginUser(request)).thenReturn(loginUser);
        when(appService.chatToGenCode(validAppId, validMessage, loginUser))
                .thenReturn(Flux.error(new RuntimeException("")));

        // Act
        Flux<ServerSentEvent<String>> result = appController.chatToGenCode(validAppId, validMessage, request);
        List<ServerSentEvent<String>> events = result.collectList().block();

        // Assert
        assertNotNull(events);
        assertEquals(3, events.size());
        assertEquals("error", events.get(1).event());
        JSONObject errorData = JSONUtil.parseObj(events.get(1).data());
        assertEquals("生成失败：生成代码失败", errorData.get("d", String.class));
    }

    // ==================== 异常场景 - 参数校验 ====================

    @Test
    @DisplayName("appId 为 null 时应抛出异常")
    void chatToGenCode_WhenAppIdIsNull_ShouldThrowException() {
        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () ->
                appController.chatToGenCode(null, validMessage, request));

        assertEquals(ErrorCode.PARAMS_ERROR.getCode(), exception.getCode());
        assertEquals("应用ID无效", exception.getMessage());

        // 验证未调用任何依赖方法
        verifyNoInteractions(userService, appService);
    }

    @Test
    @DisplayName("appId 为 0 时应抛出异常")
    void chatToGenCode_WhenAppIdIsZero_ShouldThrowException() {
        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () ->
                appController.chatToGenCode(0L, validMessage, request));

        assertEquals(ErrorCode.PARAMS_ERROR.getCode(), exception.getCode());
        assertEquals("应用ID无效", exception.getMessage());
        verifyNoInteractions(userService, appService);
    }

    @Test
    @DisplayName("appId 为负数时应抛出异常")
    void chatToGenCode_WhenAppIdIsNegative_ShouldThrowException() {
        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () ->
                appController.chatToGenCode(-1L, validMessage, request));

        assertEquals(ErrorCode.PARAMS_ERROR.getCode(), exception.getCode());
        assertEquals("应用ID无效", exception.getMessage());
        verifyNoInteractions(userService, appService);
    }

    @Test
    @DisplayName("message 为 null 时应抛出异常")
    void chatToGenCode_WhenMessageIsNull_ShouldThrowException() {
        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () ->
                appController.chatToGenCode(validAppId, null, request));

        assertEquals(ErrorCode.PARAMS_ERROR.getCode(), exception.getCode());
        assertEquals("用户消息不能为空", exception.getMessage());
        verifyNoInteractions(userService, appService);
    }

    @Test
    @DisplayName("message 为空字符串时应抛出异常")
    void chatToGenCode_WhenMessageIsEmpty_ShouldThrowException() {
        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () ->
                appController.chatToGenCode(validAppId, "", request));

        assertEquals(ErrorCode.PARAMS_ERROR.getCode(), exception.getCode());
        assertEquals("用户消息不能为空", exception.getMessage());
        verifyNoInteractions(userService, appService);
    }

    @Test
    @DisplayName("message 为空白字符串（空格/换行）时应抛出异常")
    void chatToGenCode_WhenMessageIsBlank_ShouldThrowException() {
        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () ->
                appController.chatToGenCode(validAppId, "   \n  ", request));

        assertEquals(ErrorCode.PARAMS_ERROR.getCode(), exception.getCode());
        assertEquals("用户消息不能为空", exception.getMessage());
        verifyNoInteractions(userService, appService);
    }

    // ==================== 异常场景 - getLoginUser 抛出异常 ====================

    @Test
    @DisplayName("用户未登录时 getLoginUser 抛出异常，应传播异常")
    void chatToGenCode_WhenUserNotLoggedIn_ShouldThrowException() {
        // Arrange
        when(userService.getLoginUser(request)).thenThrow(new BusinessException(ErrorCode.NOT_LOGIN_ERROR));

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () ->
                appController.chatToGenCode(validAppId, validMessage, request));

        assertEquals(ErrorCode.NOT_LOGIN_ERROR.getCode(), exception.getCode());
        verify(userService).getLoginUser(request);
        verifyNoInteractions(appService);
    }
}