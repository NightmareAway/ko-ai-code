package cn.ko_ai_code.com.koaicode.service;

import cn.ko_ai_code.com.koaicode.entity.User;
import cn.ko_ai_code.com.koaicode.model.dto.app.AppQueryRequest;
import cn.ko_ai_code.com.koaicode.model.vo.AppVO;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import cn.ko_ai_code.com.koaicode.entity.App;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * 应用 服务层。
 *
 * @author ko
 */
public interface AppService extends IService<App> {

    AppVO getAppVO(App app);

    QueryWrapper getQueryWrapper(AppQueryRequest appQueryRequest);

    List<AppVO> getAppVOList(List<App> appList);

    Flux<String> chatToGenCode(Long appId, String message, User loginUser);

    String deployApp(Long appId, User loginUser);

    void generateAppScreenshotAsync(Long appId, String appUrl);
}
