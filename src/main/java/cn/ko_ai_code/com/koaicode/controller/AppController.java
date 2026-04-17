package cn.ko_ai_code.com.koaicode.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import cn.ko_ai_code.com.koaicode.annotation.AuthCheck;
import cn.ko_ai_code.com.koaicode.common.BaseResponse;
import cn.ko_ai_code.com.koaicode.common.DeleteRequest;
import cn.ko_ai_code.com.koaicode.common.ResultUtils;
import cn.ko_ai_code.com.koaicode.constant.AppConstant;
import cn.ko_ai_code.com.koaicode.constant.UserConstant;
import cn.ko_ai_code.com.koaicode.entity.User;
import cn.ko_ai_code.com.koaicode.exception.BusinessException;
import cn.ko_ai_code.com.koaicode.exception.ErrorCode;
import cn.ko_ai_code.com.koaicode.exception.ThrowUtils;
import cn.ko_ai_code.com.koaicode.model.dto.app.*;
import cn.ko_ai_code.com.koaicode.model.dto.build.BuildStatusEvent;
import cn.ko_ai_code.com.koaicode.model.enums.CodeGenTypeEnum;
import cn.ko_ai_code.com.koaicode.model.vo.AppVO;
import cn.ko_ai_code.com.koaicode.service.ProjectDownloadService;
import cn.ko_ai_code.com.koaicode.service.UserService;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import cn.ko_ai_code.com.koaicode.entity.App;
import cn.ko_ai_code.com.koaicode.service.AppService;
import cn.ko_ai_code.com.koaicode.service.BuildStatusSseService;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 应用接口
 *
 * @author ko
 * @version 1.0
 * @since 2024-01-01
 */
@Tag(name = "应用管理", description = "提供应用创建、更新、删除、查询及AI代码生成等完整应用管理功能接口")
@RestController
@RequestMapping("/app")
public class AppController {

    @Resource
    private AppService appService;

    @Resource
    private UserService userService;

    @Resource
    private ProjectDownloadService projectDownloadService;

    @Resource
    private BuildStatusSseService buildStatusSseService;

    /**
     * 下载应用代码
     *
     * @param appId    应用ID
     * @param request  请求
     * @param response 响应
     */
    @GetMapping("/download/{appId}")
    public void downloadAppCode(@PathVariable Long appId,
                                HttpServletRequest request,
                                HttpServletResponse response) {
        // 1. 基础校验
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID无效");
        // 2. 查询应用信息
        App app = appService.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        // 3. 权限校验：只有应用创建者可以下载代码
        User loginUser = userService.getLoginUser(request);
        if (!app.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限下载该应用代码");
        }
        // 4. 构建应用代码目录路径（生成目录，非部署目录）
        String codeGenType = app.getCodeGenType();
        String sourceDirName = codeGenType + "_" + appId;
        String sourceDirPath = AppConstant.CODE_OUTPUT_ROOT_DIR + File.separator + sourceDirName;
        // 5. 检查代码目录是否存在
        File sourceDir = new File(sourceDirPath);
        ThrowUtils.throwIf(!sourceDir.exists() || !sourceDir.isDirectory(),
                ErrorCode.NOT_FOUND_ERROR, "应用代码不存在，请先生成代码");
        // 6. 生成下载文件名（不建议添加中文内容）
        String downloadFileName = String.valueOf(appId);
        // 7. 调用通用下载服务
        projectDownloadService.downloadProjectAsZip(sourceDirPath, downloadFileName, response);
    }


    /**
     * 创建应用接口
     *
     * @author ko
     * @since 2024-01-01
     * @param appAddRequest 创建应用请求参数（包含初始化prompt）
     * @param request HTTP请求对象
     * @return 返回新创建应用的ID
     *
     * @example 请求示例:
     * {
     *   "initPrompt": "创建一个简单的任务记录网站，包含标题、正文、创建时间等字段"
     * }
     *
     * @example 响应示例:
     * {
     *   "code": 0,
     *   "data": 123456789,
     *   "message": ""
     * }
     */
    @Operation(
            summary = "创建应用",
            description = "用户创建一个新应用，需要提供初始化prompt用于配置AI代码生成规则",
            tags = {"应用管理"},
            method = "POST",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "创建应用请求参数",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = AppAddRequest.class)
                    )
            ),
            responses = {
                    @ApiResponse(responseCode = "0", description = "创建成功，返回应用ID",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = BaseResponse.class))),
                    @ApiResponse(responseCode = "40000", description = "参数错误，initPrompt不能为空",
                            content = @Content)
            }
    )
    @PostMapping("/add")
    public BaseResponse<Long> addApp(@RequestBody AppAddRequest appAddRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(appAddRequest == null, ErrorCode.PARAMS_ERROR);
        // 获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        Long appId = appService.createApp(appAddRequest, loginUser);
        return ResultUtils.success(appId);
    }


    /**
     * 更新应用接口
     *
     * @author ko
     * @since 2024-01-01
     * @param appUpdateRequest 更新应用请求参数
     * @param request HTTP请求对象
     * @return 返回更新操作结果
     *
     * @example 请求示例:
     * {
     *   "id": 123456789,
     *   "appName": "更新后的应用名称"
     * }
     */
    @Operation(
            summary = "更新应用",
            description = "用户更新自己的应用信息，目前仅支持更新应用名称，且仅本人可更新",
            tags = {"应用管理"},
            method = "POST",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "更新应用请求参数",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = AppUpdateRequest.class)
                    )
            ),
            responses = {
                    @ApiResponse(responseCode = "0", description = "更新成功",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = BaseResponse.class))),
                    @ApiResponse(responseCode = "40000", description = "参数错误",
                            content = @Content),
                    @ApiResponse(responseCode = "40300", description = "无权限，非本人操作",
                            content = @Content),
                    @ApiResponse(responseCode = "40400", description = "应用不存在",
                            content = @Content)
            }
    )
    @PostMapping("/update")
    public BaseResponse<Boolean> updateApp(@RequestBody AppUpdateRequest appUpdateRequest, HttpServletRequest request) {
        if (appUpdateRequest == null || appUpdateRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        long id = appUpdateRequest.getId();
        App oldApp = appService.getById(id);
        ThrowUtils.throwIf(oldApp == null, ErrorCode.NOT_FOUND_ERROR);
        if (!oldApp.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        App app = new App();
        app.setId(id);
        app.setAppName(appUpdateRequest.getAppName());
        app.setEditTime(LocalDateTime.now());
        boolean result = appService.updateById(app);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 删除应用接口
     *
     * @author ko
     * @since 2024-01-01
     * @param deleteRequest 删除应用请求参数
     * @param request HTTP请求对象
     * @return 返回删除操作结果
     *
     * @example 请求示例:
     * {
     *   "id": 123456789
     * }
     */
    @Operation(
            summary = "删除应用",
            description = "用户删除自己的应用，管理员可删除任意应用",
            tags = {"应用管理"},
            method = "POST",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "删除应用请求参数",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = DeleteRequest.class)
                    )
            ),
            responses = {
                    @ApiResponse(responseCode = "0", description = "删除成功",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = BaseResponse.class))),
                    @ApiResponse(responseCode = "40000", description = "参数错误",
                            content = @Content),
                    @ApiResponse(responseCode = "40300", description = "无权限",
                            content = @Content),
                    @ApiResponse(responseCode = "40400", description = "应用不存在",
                            content = @Content)
            }
    )
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteApp(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        long id = deleteRequest.getId();
        App oldApp = appService.getById(id);
        ThrowUtils.throwIf(oldApp == null, ErrorCode.NOT_FOUND_ERROR);
        if (!oldApp.getUserId().equals(loginUser.getId()) && !UserConstant.ADMIN_ROLE.equals(loginUser.getUserRole())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean result = appService.removeById(id);
        return ResultUtils.success(result);
    }

    /**
     * 获取应用详情接口
     *
     * @author ko
     * @since 2024-01-01
     * @param id 应用ID
     * @return 返回应用详细信息
     *
     * @example 响应示例:
     * {
     *   "code": 0,
     *   "data": {
     *     "id": 123456789,
     *     "appName": "任务记录网站",
     *     "initPrompt": "创建一个简单的任务记录网站",
     *     "userId": 100,
     *     "codeGenType": 2,
     *     "createTime": "2024-01-01T00:00:00",
     *     "editTime": "2024-01-01T12:00:00"
     *   },
     *   "message": ""
     * }
     */
    @Operation(
            summary = "获取应用详情",
            description = "根据应用ID获取应用详细信息，包含应用名称、prompt、创建时间等",
            tags = {"应用管理"},
            method = "GET",
            parameters = {
                    @Parameter(name = "id", description = "应用ID", required = true, example = "123456789")
            },
            responses = {
                    @ApiResponse(responseCode = "0", description = "获取成功",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = BaseResponse.class))),
                    @ApiResponse(responseCode = "40000", description = "参数错误",
                            content = @Content),
                    @ApiResponse(responseCode = "40400", description = "应用不存在",
                            content = @Content)
            }
    )
    @GetMapping("/get/vo")
    public BaseResponse<AppVO> getAppVOById(
            @Parameter(description = "应用ID", required = true, example = "123456789") long id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        App app = appService.getById(id);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(appService.getAppVO(app));
    }

    /**
     * 分页获取当前用户应用列表接口
     *
     * @author ko
     * @since 2024-01-01
     * @param appQueryRequest 查询请求参数
     * @param request HTTP请求对象
     * @return 返回当前用户的应用分页列表
     *
     * @example 请求示例:
     * {
     *   "current": 1,
     *   "pageSize": 10
     * }
     */
    @Operation(
            summary = "获取我的应用列表",
            description = "分页获取当前登录用户创建的应用列表，每页最多20条",
            tags = {"应用管理"},
            method = "POST",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "应用分页查询请求参数",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = AppQueryRequest.class)
                    )
            ),
            responses = {
                    @ApiResponse(responseCode = "0", description = "查询成功",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = BaseResponse.class))),
                    @ApiResponse(responseCode = "40000", description = "参数错误",
                            content = @Content)
            }
    )
    @PostMapping("/my/list/page/vo")
    public BaseResponse<Page<AppVO>> listMyAppVOByPage(@RequestBody AppQueryRequest appQueryRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(appQueryRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        long pageSize = appQueryRequest.getPageSize();
        ThrowUtils.throwIf(pageSize > 20, ErrorCode.PARAMS_ERROR, "每页最多查询 20 个应用");
        long current = appQueryRequest.getCurrent();
        appQueryRequest.setUserId(loginUser.getId());
        QueryWrapper queryWrapper = appService.getQueryWrapper(appQueryRequest);
        Page<App> appPage = appService.page(Page.of(current, pageSize), queryWrapper);
        Page<AppVO> appVOPage = new Page<>(current, pageSize, appPage.getTotalRow());
        List<AppVO> appVOList = appService.getAppVOList(appPage.getRecords());
        appVOPage.setRecords(appVOList);
        return ResultUtils.success(appVOPage);
    }

    /**
     * 分页获取精选应用列表接口
     *
     * @author ko
     * @since 2024-01-01
     * @param appQueryRequest 查询请求参数
     * @return 返回精选应用分页列表
     *
     * @example 请求示例:
     * {
     *   "current": 1,
     *   "pageSize": 10
     * }
     */
    @Operation(
            summary = "获取精选应用列表",
            description = "分页获取被标记为精选的应用列表，用于应用市场展示",
            tags = {"应用管理"},
            method = "POST",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "精选应用分页查询请求参数",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = AppQueryRequest.class)
                    )
            ),
            responses = {
                    @ApiResponse(responseCode = "0", description = "查询成功",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = BaseResponse.class))),
                    @ApiResponse(responseCode = "40000", description = "参数错误",
                            content = @Content)
            }
    )
    @Cacheable(value = "good_app_page", key = "T(cn.ko_ai_code.com.koaicode.utils.CacheKeyUtils).generateKey(#appQueryRequest)", condition = "#appQueryRequest.current <= 10")
    @PostMapping("/good/list/page/vo")
    public BaseResponse<Page<AppVO>> listGoodAppVOByPage(@RequestBody AppQueryRequest appQueryRequest) {
        ThrowUtils.throwIf(appQueryRequest == null, ErrorCode.PARAMS_ERROR);
        long pageSize = appQueryRequest.getPageSize();
        ThrowUtils.throwIf(pageSize > 20, ErrorCode.PARAMS_ERROR, "每页最多查询 20 个应用");
        long current = appQueryRequest.getCurrent();
        appQueryRequest.setPriority(AppConstant.GOOD_APP_PRIORITY);
        QueryWrapper queryWrapper = appService.getQueryWrapper(appQueryRequest);
        Page<App> appPage = appService.page(Page.of(current, pageSize), queryWrapper);
        Page<AppVO> appVOPage = new Page<>(current, pageSize, appPage.getTotalRow());
        List<AppVO> appVOList = appService.getAppVOList(appPage.getRecords());
        appVOPage.setRecords(appVOList);
        return ResultUtils.success(appVOPage);
    }

    /**
     * 管理员删除应用接口
     *
     * @author ko
     * @since 2024-01-01
     * @param deleteRequest 删除应用请求参数
     * @return 返回删除操作结果
     *
     * @example 请求示例:
     * {
     *   "id": 123456789
     * }
     */
    @Operation(
            summary = "管理员删除应用",
            description = "管理员删除任意用户创建的应用",
            tags = {"应用管理-管理员"},
            method = "POST",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "删除应用请求参数",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = DeleteRequest.class)
                    )
            ),
            responses = {
                    @ApiResponse(responseCode = "0", description = "删除成功",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = BaseResponse.class))),
                    @ApiResponse(responseCode = "40000", description = "参数错误",
                            content = @Content),
                    @ApiResponse(responseCode = "40300", description = "无权限，需要管理员角色",
                            content = @Content),
                    @ApiResponse(responseCode = "40400", description = "应用不存在",
                            content = @Content)
            }
    )
    @PostMapping("/admin/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteAppByAdmin(@RequestBody DeleteRequest deleteRequest) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long id = deleteRequest.getId();
        App oldApp = appService.getById(id);
        ThrowUtils.throwIf(oldApp == null, ErrorCode.NOT_FOUND_ERROR);
        boolean result = appService.removeById(id);
        return ResultUtils.success(result);
    }

    /**
     * 管理员更新应用接口
     *
     * @author ko
     * @since 2024-01-01
     * @param appAdminUpdateRequest 管理员更新应用请求参数
     * @return 返回更新操作结果
     *
     * @example 请求示例:
     * {
     *   "id": 123456789,
     *   "appName": "管理员更新的应用名称",
     *   "priority": 1
     * }
     */
    @Operation(
            summary = "管理员更新应用",
            description = "管理员更新任意应用信息，包括应用名称、优先级等",
            tags = {"应用管理-管理员"},
            method = "POST",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "管理员更新应用请求参数",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = AppAdminUpdateRequest.class)
                    )
            ),
            responses = {
                    @ApiResponse(responseCode = "0", description = "更新成功",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = BaseResponse.class))),
                    @ApiResponse(responseCode = "40000", description = "参数错误",
                            content = @Content),
                    @ApiResponse(responseCode = "40300", description = "无权限",
                            content = @Content),
                    @ApiResponse(responseCode = "40400", description = "应用不存在",
                            content = @Content)
            }
    )
    @PostMapping("/admin/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateAppByAdmin(@RequestBody AppAdminUpdateRequest appAdminUpdateRequest) {
        if (appAdminUpdateRequest == null || appAdminUpdateRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long id = appAdminUpdateRequest.getId();
        App oldApp = appService.getById(id);
        ThrowUtils.throwIf(oldApp == null, ErrorCode.NOT_FOUND_ERROR);
        App app = new App();
        BeanUtil.copyProperties(appAdminUpdateRequest, app);
        app.setEditTime(LocalDateTime.now());
        boolean result = appService.updateById(app);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 管理员分页获取应用列表接口
     *
     * @author ko
     * @since 2024-01-01
     * @param appQueryRequest 查询请求参数
     * @return 返回所有应用分页列表
     *
     * @example 请求示例:
     * {
     *   "current": 1,
     *   "pageSize": 10
     * }
     */
    @Operation(
            summary = "管理员获取应用列表",
            description = "分页获取所有应用列表，用于后台管理",
            tags = {"应用管理-管理员"},
            method = "POST",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "应用分页查询请求参数",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = AppQueryRequest.class)
                    )
            ),
            responses = {
                    @ApiResponse(responseCode = "0", description = "查询成功",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = BaseResponse.class))),
                    @ApiResponse(responseCode = "40000", description = "参数错误",
                            content = @Content),
                    @ApiResponse(responseCode = "40300", description = "无权限",
                            content = @Content)
            }
    )
    @PostMapping("/admin/list/page/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<AppVO>> listAppVOByPageByAdmin(@RequestBody AppQueryRequest appQueryRequest) {
        ThrowUtils.throwIf(appQueryRequest == null, ErrorCode.PARAMS_ERROR);
        long current = appQueryRequest.getCurrent();
        long pageSize = appQueryRequest.getPageSize();
        QueryWrapper queryWrapper = appService.getQueryWrapper(appQueryRequest);
        Page<App> appPage = appService.page(Page.of(current, pageSize), queryWrapper);
        Page<AppVO> appVOPage = new Page<>(current, pageSize, appPage.getTotalRow());
        List<AppVO> appVOList = appService.getAppVOList(appPage.getRecords());
        appVOPage.setRecords(appVOList);
        return ResultUtils.success(appVOPage);
    }

    /**
     * 管理员获取应用详情接口
     *
     * @author ko
     * @since 2024-01-01
     * @param id 应用ID
     * @return 返回应用详细信息
     *
     * @example 响应示例:
     * {
     *   "code": 0,
     *   "data": {
     *     "id": 123456789,
     *     "appName": "任务记录网站",
     *     "initPrompt": "创建一个简单的任务记录网站",
     *     "userId": 100,
     *     "codeGenType": 2,
     *     "createTime": "2024-01-01T00:00:00",
     *     "editTime": "2024-01-01T12:00:00"
     *   },
     *   "message": ""
     * }
     */
    @Operation(
            summary = "管理员获取应用详情",
            description = "管理员获取任意应用的详细信息",
            tags = {"应用管理-管理员"},
            method = "GET",
            parameters = {
                    @Parameter(name = "id", description = "应用ID", required = true, example = "123456789")
            },
            responses = {
                    @ApiResponse(responseCode = "0", description = "获取成功",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = BaseResponse.class))),
                    @ApiResponse(responseCode = "40000", description = "参数错误",
                            content = @Content),
                    @ApiResponse(responseCode = "40300", description = "无权限",
                            content = @Content),
                    @ApiResponse(responseCode = "40400", description = "应用不存在",
                            content = @Content)
            }
    )
    @GetMapping("/admin/get/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<AppVO> getAppVOByIdByAdmin(
            @Parameter(description = "应用ID", required = true, example = "123456789") long id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        App app = appService.getById(id);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(appService.getAppVO(app));
    }

    /**
     * 应用聊天生成代码接口（流式SSE）
     *
     * @author ko
     * @since 2024-01-01
     * @param appId 应用ID
     * @param message 用户消息
     * @param request HTTP请求对象
     * @return 返回SSE流，包含生成结果
     *
     * @example 请求示例:
     * GET /app/chat/gen/code?appId=123456789&message=帮我创建一个登录页面
     *
     * @example 响应示例 (SSE流):
     * data: {"d": "正在分析需求..."}
     * data: {"d": "开始生成代码..."}
     * data: {"d": "<!-- HTML代码 -->"}
     * event: done
     * data: ""
     */
    @Operation(
            summary = "AI聊天生成代码",
            description = "通过AI对话生成代码，使用SSE流式返回生成进度和结果，支持HTML/CSS/JS多文件生成",
            tags = {"AI代码生成"},
            method = "GET",
            parameters = {
                    @Parameter(name = "appId", description = "应用ID", required = true, example = "123456789"),
                    @Parameter(name = "message", description = "用户消息/需求描述", required = true, example = "帮我创建一个登录页面")
            },
            responses = {
                    @ApiResponse(responseCode = "0", description = "成功建立SSE连接",
                            content = @Content(mediaType = "text/event-stream")),
                    @ApiResponse(responseCode = "40000", description = "参数错误，如应用ID无效或消息为空",
                            content = @Content),
                    @ApiResponse(responseCode = "40100", description = "未登录",
                            content = @Content),
                    @ApiResponse(responseCode = "40400", description = "应用不存在",
                            content = @Content)
            }
    )
    @GetMapping(value = "/chat/gen/code", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> chatToGenCode(
            @Parameter(description = "应用ID", required = true, example = "123456789") @RequestParam Long appId,
            @Parameter(description = "用户消息/需求描述", required = true, example = "帮我创建一个登录页面") @RequestParam String message,
            HttpServletRequest request) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID无效");
        ThrowUtils.throwIf(StrUtil.isBlank(message), ErrorCode.PARAMS_ERROR, "用户消息不能为空");
        User loginUser = userService.getLoginUser(request);
        Flux<String> contentFlux = appService.chatToGenCode(appId, message, loginUser);
        return contentFlux
                .map(chunk -> {
                    Map<String, String> wrapper = Map.of("d", chunk);
                    String jsonData = JSONUtil.toJsonStr(wrapper);
                    return ServerSentEvent.<String>builder()
                            .data(jsonData)
                            .build();
                })
                .concatWith(Mono.just(
                        ServerSentEvent.<String>builder()
                                .event("done")
                                .data("")
                                .build()
                ));
    }

    /**
     * 应用部署接口
     *
     * @author ko
     * @since 2024-01-01
     * @param appDeployRequest 部署请求参数
     * @param request HTTP请求对象
     * @return 返回部署后的URL
     *
     * @example 请求示例:
     * {
     *   "appId": 123456789
     * }
     *
     * @example 响应示例:
     * {
     *   "code": 0,
     *   "data": "https://your-app.example.com",
     *   "message": ""
     * }
     */
    @Operation(
            summary = "部署应用",
            description = "将生成的应用代码部署到服务器，返回部署后的访问URL",
            tags = {"应用管理"},
            method = "POST",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "应用部署请求参数",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = AppDeployRequest.class)
                    )
            ),
            responses = {
                    @ApiResponse(responseCode = "0", description = "部署成功，返回部署URL",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = BaseResponse.class))),
                    @ApiResponse(responseCode = "40000", description = "参数错误",
                            content = @Content),
                    @ApiResponse(responseCode = "40100", description = "未登录",
                            content = @Content),
                    @ApiResponse(responseCode = "40400", description = "应用不存在",
                            content = @Content)
            }
    )
    @PostMapping("/deploy")
    public BaseResponse<String> deployApp(@RequestBody AppDeployRequest appDeployRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(appDeployRequest == null, ErrorCode.PARAMS_ERROR);
        Long appId = appDeployRequest.getAppId();
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用 ID 不能为空");
        User loginUser = userService.getLoginUser(request);
        String deployUrl = appService.deployApp(appId, loginUser);
        return ResultUtils.success(deployUrl);
    }

    /**
     * 保存应用接口
     *
     * @author ko
     * @since 2024-01-01
     * @param app 应用实体
     * @return 返回保存操作结果
     *
     * @example 请求示例:
     * {
     *   "appName": "测试应用",
     *   "initPrompt": "初始化prompt"
     * }
     */
    @Operation(
            summary = "保存应用",
            description = "直接保存应用实体数据到数据库",
            tags = {"应用管理"},
            method = "POST",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "应用实体",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = App.class)
                    )
            ),
            responses = {
                    @ApiResponse(responseCode = "0", description = "保存成功",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = BaseResponse.class))),
                    @ApiResponse(responseCode = "40000", description = "参数错误",
                            content = @Content)
            }
    )
    @PostMapping("/save")
    public BaseResponse<Boolean> save(@RequestBody App app) {
        boolean result = appService.save(app);
        return ResultUtils.success(result);
    }

    /**
     * 删除应用接口（根据ID）
     *
     * @author ko
     * @since 2024-01-01
     * @param id 应用ID
     * @return 返回删除操作结果
     */
    @Operation(
            summary = "根据ID删除应用",
            description = "根据应用ID直接删除应用",
            tags = {"应用管理"},
            method = "DELETE",
            parameters = {
                    @Parameter(name = "id", description = "应用ID", required = true, example = "123456789")
            },
            responses = {
                    @ApiResponse(responseCode = "0", description = "删除成功",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = BaseResponse.class))),
                    @ApiResponse(responseCode = "40000", description = "参数错误",
                            content = @Content)
            }
    )
    @DeleteMapping("/remove/{id}")
    public BaseResponse<Boolean> remove(@PathVariable Long id) {
        boolean result = appService.removeById(id);
        return ResultUtils.success(result);
    }

    /**
     * 更新应用接口（根据ID）
     *
     * @author ko
     * @since 2024-01-01
     * @param app 应用实体
     * @return 返回更新操作结果
     *
     * @example 请求示例:
     * {
     *   "id": 123456789,
     *   "appName": "更新后的应用名称"
     * }
     */
    @Operation(
            summary = "根据ID更新应用",
            description = "根据应用ID更新应用信息",
            tags = {"应用管理"},
            method = "PUT",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "应用实体",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = App.class)
                    )
            ),
            responses = {
                    @ApiResponse(responseCode = "0", description = "更新成功",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = BaseResponse.class))),
                    @ApiResponse(responseCode = "40000", description = "参数错误",
                            content = @Content)
            }
    )
    @PutMapping("/update")
    public BaseResponse<Boolean> update(@RequestBody App app) {
        boolean result = appService.updateById(app);
        return ResultUtils.success(result);
    }

    /**
     * 查询所有应用接口
     *
     * @author ko
     * @since 2024-01-01
     * @return 返回所有应用列表
     */
    @Operation(
            summary = "查询所有应用",
            description = "查询数据库中的所有应用记录",
            tags = {"应用管理"},
            method = "GET",
            responses = {
                    @ApiResponse(responseCode = "0", description = "查询成功",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = BaseResponse.class)))
            }
    )
    @GetMapping("/list")
    public BaseResponse<List<App>> list() {
        List<App> apps = appService.list();
        return ResultUtils.success(apps);
    }

    /**
     * 根据ID获取应用详情接口
     *
     * @author ko
     * @since 2024-01-01
     * @param id 应用ID
     * @return 返回应用详情
     *
     * @example 响应示例:
     * {
     *   "code": 0,
     *   "data": {
     *     "id": 123456789,
     *     "appName": "任务记录网站"
     *   },
     *   "message": ""
     * }
     */
    @Operation(
            summary = "根据ID获取应用",
            description = "根据应用ID获取应用完整信息",
            tags = {"应用管理"},
            method = "GET",
            parameters = {
                    @Parameter(name = "id", description = "应用ID", required = true, example = "123456789")
            },
            responses = {
                    @ApiResponse(responseCode = "0", description = "获取成功",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = BaseResponse.class))),
                    @ApiResponse(responseCode = "40000", description = "参数错误",
                            content = @Content)
            }
    )
    @GetMapping("/getInfo/{id}")
    public BaseResponse<App> getInfo(@PathVariable Long id) {
        App app = appService.getById(id);
        return ResultUtils.success(app);
    }

    /**
     * 分页查询应用接口
     *
     * @author ko
     * @since 2024-01-01
     * @param page 分页参数
     * @return 返回分页后的应用列表
     */
    @Operation(
            summary = "分页查询应用",
            description = "使用MyBatis-Flex分页插件查询应用列表",
            tags = {"应用管理"},
            method = "GET",
            parameters = {
                    @Parameter(name = "page", description = "分页对象，包含current和pageSize", required = true)
            },
            responses = {
                    @ApiResponse(responseCode = "0", description = "查询成功",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = BaseResponse.class))),
                    @ApiResponse(responseCode = "40000", description = "参数错误",
                            content = @Content)
            }
    )
    @GetMapping("/page")
    public BaseResponse<Page<App>> page(Page<App> page) {
        Page<App> result = appService.page(page);
        return ResultUtils.success(result);
    }

    /**
     * 订阅Vue项目构建状态
     *
     * @author ko
     * @since 2024-01-01
     * @param appId 应用ID
     * @param request HTTP请求对象
     * @return 返回SSE流，包含构建状态变更事件
     *
     * @example 响应示例 (SSE格式):
     * data: {"appId":123,"status":"INSTALLING","message":"正在安装依赖包...","progress":20,"timestamp":"2024-01-01T10:00:00"}
     * event: done
     * data: ""
     */
    @Operation(
            summary = "订阅构建状态",
            description = "订阅Vue项目构建状态的实时变更，使用SSE流式返回构建进度",
            tags = {"应用管理"},
            method = "GET",
            parameters = {
                    @Parameter(name = "appId", description = "应用ID", required = true, example = "123456789")
            },
            responses = {
                    @ApiResponse(responseCode = "0", description = "成功建立SSE连接",
                            content = @Content(mediaType = "text/event-stream")),
                    @ApiResponse(responseCode = "40000", description = "参数错误",
                            content = @Content),
                    @ApiResponse(responseCode = "40100", description = "未登录",
                            content = @Content),
                    @ApiResponse(responseCode = "40400", description = "应用不存在",
                            content = @Content)
            }
    )
    @GetMapping(value = "/build/status/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> subscribeBuildStatus(
            @Parameter(description = "应用ID", required = true, example = "123456789") @RequestParam Long appId,
            HttpServletRequest request) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID无效");
        // 权限校验：验证用户是否有权限订阅该应用的构建状态
        App app = appService.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        User loginUser = userService.getLoginUser(request);
        if (!app.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限订阅该应用构建状态");
        }
        
        return buildStatusSseService.subscribeBuildStatus(appId)
                .map(event -> {
                    // 将事件转换为JSON字符串
                    String jsonData = JSONUtil.toJsonStr(event);
                    return ServerSentEvent.<String>builder()
                            .event("build-status")
                            .data(jsonData)
                            .build();
                })
                .concatWith(Mono.just(
                        ServerSentEvent.<String>builder()
                                .event("done")
                                .data("")
                                .build()
                ));
    }

}