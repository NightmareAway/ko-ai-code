package cn.ko_ai_code.com.koaicode.service;

import cn.ko_ai_code.com.koaicode.entity.User;
import cn.ko_ai_code.com.koaicode.model.dto.chathistory.ChatHistoryQueryRequest;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import cn.ko_ai_code.com.koaicode.entity.ChatHistory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;

import java.time.LocalDateTime;

/**
 * 对话历史 服务层。
 *
 * @author ko
 */
public interface ChatHistoryService extends IService<ChatHistory> {

    /**
     * 保存消息记录
     *
     * @param appId 应用id
     * @param message 用户/系统消息
     * @param messageType 消息类型
     * @param userId 当前登录用户的id
     * @return 是否保存成功
     */
    boolean addChatMessage(Long appId, String message, String messageType, Long userId);

    /**
     * 根据应用id删除历史记录（避免删除了应用还保存消息历史）
     *
     * @param appId 应用id
     * @return 是否删除成功
     */
    boolean deleteByAppId(Long appId);

    /**
     * 根据查询请求获取查询包装类
     *
     * @param chatHistoryQueryRequest 查询请求
     * @return 查询包装类
     */
    QueryWrapper getQueryWrapper(ChatHistoryQueryRequest chatHistoryQueryRequest);

    /**
     * 根据应用id分页获取聊天历史记录(游标分页)
     *
     * @param appId 应用id
     * @param pageSize 每页大小
     * @param lastCreateTime 最后创建时间
     * @param loginUser 当前登录用户
     * @return 聊天历史记录
     */
    Page<ChatHistory> listAppChatHistoryByPage(Long appId, int pageSize,
                                               LocalDateTime lastCreateTime,
                                               User loginUser);

    /**
     * 加载mysql历史对话到redis内存中
     *
     * @param appId 应用id
     * @param chatMemory 聊天记忆
     * @param maxCount 加载记忆的最大数量
     * @return 加载的数量
     */
    int loadChatHistoryToMemory(Long appId, MessageWindowChatMemory chatMemory, int maxCount);
}
