package cn.ko_ai_code.com.koaicode.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.ko_ai_code.com.koaicode.annotation.AuthCheck;
import cn.ko_ai_code.com.koaicode.common.BaseResponse;
import cn.ko_ai_code.com.koaicode.common.DeleteRequest;
import cn.ko_ai_code.com.koaicode.common.ResultUtils;
import cn.ko_ai_code.com.koaicode.constant.UserConstant;
import cn.ko_ai_code.com.koaicode.exception.BusinessException;
import cn.ko_ai_code.com.koaicode.exception.ErrorCode;
import cn.ko_ai_code.com.koaicode.exception.ThrowUtils;
import cn.ko_ai_code.com.koaicode.model.dto.user.*;
import cn.ko_ai_code.com.koaicode.model.vo.LoginUserVO;
import cn.ko_ai_code.com.koaicode.model.vo.UserVO;
import com.mybatisflex.core.paginate.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.beans.factory.annotation.Autowired;
import cn.ko_ai_code.com.koaicode.entity.User;
import cn.ko_ai_code.com.koaicode.service.UserService;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

/**
 * 用户接口
 *
 * @author ko
 * @version 1.0
 * @since 2024-01-01
 */
@Tag(name = "用户管理", description = "提供用户注册、登录、CRUD等完整用户管理功能接口")
@RestController
@RequestMapping("/user")
public class UserController {


        @Resource
        private UserService userService;

        /**
         * 用户注册接口
         *
         * @author ko
         * @since 2024-01-01
         * @param userRegisterRequest 注册请求参数（包含账号、密码、确认密码）
         * @return 返回新注册用户的ID
         *
         * @example 请求示例:
         * {
         *   "userAccount": "testuser",
         *   "userPassword": "password123",
         *   "checkPassword": "password123"
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
                summary = "用户注册",
                description = "提供新用户注册功能，账号需唯一，密码长度不少于8位，两次密码输入必须一致",
                tags = {"用户管理"},
                method = "POST",
                requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                        description = "用户注册请求参数",
                        required = true,
                        content = @Content(
                                mediaType = "application/json",
                                schema = @Schema(implementation = UserRegisterRequest.class)
                        )
                ),
                responses = {
                        @ApiResponse(responseCode = "0", description = "注册成功，返回用户ID",
                                content = @Content(mediaType = "application/json",
                                        schema = @Schema(implementation = BaseResponse.class))),
                        @ApiResponse(responseCode = "40000", description = "参数错误，如密码不匹配、账号已存在等",
                                content = @Content(mediaType = "application/json",
                                        schema = @Schema(implementation = BaseResponse.class)))
                }
        )
        @PostMapping("register")
        public BaseResponse<Long> userRegister(@RequestBody UserRegisterRequest userRegisterRequest) {
            ThrowUtils.throwIf(userRegisterRequest == null, ErrorCode.PARAMS_ERROR);
            String userAccount = userRegisterRequest.getUserAccount();
            String userPassword = userRegisterRequest.getUserPassword();
            String checkPassword = userRegisterRequest.getCheckPassword();
            long result = userService.userRegister(userAccount, userPassword, checkPassword);
            return ResultUtils.success(result);
        }

        /**
         * 用户登录接口
         *
         * @author ko
         * @since 2024-01-01
         * @param userLoginRequest 登录请求参数（包含账号、密码）
         * @param request HTTP请求对象，用于存储session信息
         * @return 返回登录用户信息（脱敏后）
         *
         * @example 请求示例:
         * {
         *   "userAccount": "testuser",
         *   "userPassword": "password123"
         * }
         *
         * @example 响应示例:
         * {
         *   "code": 0,
         *   "data": {
         *     "userAvatar": "https://example.com/avatar.jpg",
         *     "userName": "测试用户",
         *     "userRole": "user"
         *   },
         *   "message": ""
         * }
         */
        @Operation(
                summary = "用户登录",
                description = "用户使用账号密码登录，登录成功后将用户信息存入session",
                tags = {"用户管理"},
                method = "POST",
                requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                        description = "用户登录请求参数",
                        required = true,
                        content = @Content(
                                mediaType = "application/json",
                                schema = @Schema(implementation = UserLoginRequest.class)
                        )
                ),
                responses = {
                        @ApiResponse(responseCode = "0", description = "登录成功，返回用户信息",
                                content = @Content(mediaType = "application/json",
                                        schema = @Schema(implementation = BaseResponse.class))),
                        @ApiResponse(responseCode = "40000", description = "参数错误",
                                content = @Content),
                        @ApiResponse(responseCode = "40100", description = "认证失败，账号或密码错误",
                                content = @Content)
                }
        )
        @PostMapping("/login")
        public BaseResponse<LoginUserVO> userLogin(@RequestBody UserLoginRequest userLoginRequest, HttpServletRequest request) {
            ThrowUtils.throwIf(userLoginRequest == null, ErrorCode.PARAMS_ERROR);
            String userAccount = userLoginRequest.getUserAccount();
            String userPassword = userLoginRequest.getUserPassword();
            LoginUserVO loginUserVO = userService.userLogin(userAccount, userPassword, request);
            return ResultUtils.success(loginUserVO);
        }

        /**
         * 获取当前登录用户信息
         *
         * @author ko
         * @since 2024-01-01
         * @param request HTTP请求对象
         * @return 返回当前登录用户信息
         *
         * @example 响应示例:
         * {
         *   "code": 0,
         *   "data": {
         *     "userAvatar": "https://example.com/avatar.jpg",
         *     "userName": "测试用户",
         *     "userRole": "user"
         *   },
         *   "message": ""
         * }
         */
        @Operation(
                summary = "获取当前登录用户",
                description = "从session中获取当前登录用户的信息，无需传参",
                tags = {"用户管理"},
                method = "GET",
                responses = {
                        @ApiResponse(responseCode = "0", description = "获取成功",
                                content = @Content(mediaType = "application/json",
                                        schema = @Schema(implementation = BaseResponse.class))),
                        @ApiResponse(responseCode = "40100", description = "未登录或登录已过期",
                                content = @Content)
                }
        )
        @GetMapping("/get/login")
        public BaseResponse<LoginUserVO> getLoginUser(HttpServletRequest request) {
            User loginUser = userService.getLoginUser(request);
            return ResultUtils.success(userService.getLoginUserVO(loginUser));
        }

        /**
         * 用户登出接口
         *
         * @author ko
         * @since 2024-01-01
         * @param request HTTP请求对象
         * @return 返回登出操作结果
         *
         * @example 响应示例:
         * {
         *   "code": 0,
         *   "data": true,
         *   "message": ""
         * }
         */
        @Operation(
                summary = "用户登出",
                description = "清除session中的用户信息，完成登出操作",
                tags = {"用户管理"},
                method = "POST",
                responses = {
                        @ApiResponse(responseCode = "0", description = "登出成功",
                                content = @Content(mediaType = "application/json",
                                        schema = @Schema(implementation = BaseResponse.class))),
                        @ApiResponse(responseCode = "40000", description = "请求参数错误",
                                content = @Content)
                }
        )
        @PostMapping("/logout")
        public BaseResponse<Boolean> userLogout(HttpServletRequest request) {
            ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
            boolean result = userService.userLogout(request);
            return ResultUtils.success(result);
        }

        /**
         * 创建用户（管理员）
         *
         * @author ko
         * @since 2024-01-01
         * @param userAddRequest 用户信息请求参数
         * @return 返回新创建用户的ID
         *
         * @example 请求示例:
         * {
         *   "userAccount": "newuser",
         *   "userName": "新用户",
         *   "userAvatar": "https://example.com/avatar.jpg"
         * }
         *
         * @example 响应示例:
         * {
         *   "code": 0,
         *   "data": 123456790,
         *   "message": ""
         * }
         */
        @Operation(
                summary = "创建用户",
                description = "管理员创建新用户，默认密码为12345678，创建后返回用户ID",
                tags = {"用户管理-管理员"},
                method = "POST",
                requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                        description = "创建用户请求参数",
                        required = true,
                        content = @Content(
                                mediaType = "application/json",
                                schema = @Schema(implementation = UserAddRequest.class)
                        )
                ),
                responses = {
                        @ApiResponse(responseCode = "0", description = "创建成功",
                                content = @Content(mediaType = "application/json",
                                        schema = @Schema(implementation = BaseResponse.class))),
                        @ApiResponse(responseCode = "40000", description = "参数错误",
                                content = @Content),
                        @ApiResponse(responseCode = "40300", description = "无权限，需要管理员角色",
                                content = @Content)
                }
        )
        @PostMapping("/add")
        @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
        public BaseResponse<Long> addUser(@RequestBody UserAddRequest userAddRequest) {
            ThrowUtils.throwIf(userAddRequest == null, ErrorCode.PARAMS_ERROR);
            User user = new User();
            BeanUtil.copyProperties(userAddRequest, user);
            final String DEFAULT_PASSWORD = "12345678";
            String encryptPassword = userService.getEncryptPassword(DEFAULT_PASSWORD);
            user.setUserPassword(encryptPassword);
            boolean result = userService.save(user);
            ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
            return ResultUtils.success(user.getId());
        }

        /**
         * 根据ID获取用户（管理员）
         *
         * @author ko
         * @since 2024-01-01
         * @param id 用户ID
         * @return 返回用户完整信息
         *
         * @example 响应示例:
         * {
         *   "code": 0,
         *   "data": {
         *     "id": 123456789,
         *     "userAccount": "testuser",
         *     "userName": "测试用户",
         *     "userAvatar": "https://example.com/avatar.jpg",
         *     "userRole": "user",
         *     "createTime": "2024-01-01T00:00:00"
         *   },
         *   "message": ""
         * }
         */
        @Operation(
                summary = "获取用户详情",
                description = "根据用户ID获取用户完整信息，仅管理员可访问",
                tags = {"用户管理-管理员"},
                method = "GET",
                parameters = {
                        @Parameter(name = "id", description = "用户ID", required = true, schema = @Schema(type = "long", example = "123456789"))
                },
                responses = {
                        @ApiResponse(responseCode = "0", description = "获取成功",
                                content = @Content(mediaType = "application/json",
                                        schema = @Schema(implementation = BaseResponse.class))),
                        @ApiResponse(responseCode = "40000", description = "参数错误，ID无效",
                                content = @Content),
                        @ApiResponse(responseCode = "40400", description = "用户不存在",
                                content = @Content),
                        @ApiResponse(responseCode = "40300", description = "无权限",
                                content = @Content)
                }
        )
        @GetMapping("/get")
        @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
        public BaseResponse<User> getUserById(
                @Parameter(description = "用户ID", required = true, example = "123456789") long id) {
            ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
            User user = userService.getById(id);
            ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR);
            return ResultUtils.success(user);
        }

        /**
         * 根据ID获取用户封装信息（公开）
         *
         * @author ko
         * @since 2024-01-01
         * @param id 用户ID
         * @return 返回用户脱敏信息
         *
         * @example 响应示例:
         * {
         *   "code": 0,
         *   "data": {
         *     "userAvatar": "https://example.com/avatar.jpg",
         *     "userName": "测试用户",
         *     "userRole": "user"
         *   },
         *   "message": ""
         * }
         */
        @Operation(
                summary = "获取用户公开信息",
                description = "根据用户ID获取用户脱敏后的公开信息",
                tags = {"用户管理"},
                method = "GET",
                parameters = {
                        @Parameter(name = "id", description = "用户ID", required = true, example = "123456789")
                },
                responses = {
                        @ApiResponse(responseCode = "0", description = "获取成功",
                                content = @Content(mediaType = "application/json",
                                        schema = @Schema(implementation = BaseResponse.class))),
                        @ApiResponse(responseCode = "40000", description = "参数错误",
                                content = @Content),
                        @ApiResponse(responseCode = "40400", description = "用户不存在",
                                content = @Content)
                }
        )
        @GetMapping("/get/vo")
        public BaseResponse<UserVO> getUserVOById(
                @Parameter(description = "用户ID", required = true, example = "123456789") long id) {
            BaseResponse<User> response = getUserById(id);
            User user = response.getData();
            return ResultUtils.success(userService.getUserVO(user));
        }

        /**
         * 删除用户（管理员）
         *
         * @author ko
         * @since 2024-01-01
         * @param deleteRequest 删除请求，包含用户ID
         * @return 返回删除操作结果
         *
         * @example 请求示例:
         * {
         *   "id": 123456789
         * }
         *
         * @example 响应示例:
         * {
         *   "code": 0,
         *   "data": true,
         *   "message": ""
         * }
         */
        @Operation(
                summary = "删除用户",
                description = "根据用户ID删除用户，仅管理员可操作",
                tags = {"用户管理-管理员"},
                method = "POST",
                requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                        description = "删除用户请求参数",
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
                                content = @Content)
                }
        )
        @PostMapping("/delete")
        @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
        public BaseResponse<Boolean> deleteUser(@RequestBody DeleteRequest deleteRequest) {
            if (deleteRequest == null || deleteRequest.getId() <= 0) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR);
            }
            boolean b = userService.removeById(deleteRequest.getId());
            return ResultUtils.success(b);
        }

        /**
         * 更新用户（管理员）
         *
         * @author ko
         * @since 2024-01-01
         * @param userUpdateRequest 更新用户信息请求
         * @return 返回更新操作结果
         *
         * @example 请求示例:
         * {
         *   "id": 123456789,
         *   "userName": "更新后的用户名",
         *   "userAvatar": "https://example.com/new-avatar.jpg"
         * }
         */
        @Operation(
                summary = "更新用户信息",
                description = "更新用户信息，仅管理员可操作，可更新用户名、头像等",
                tags = {"用户管理-管理员"},
                method = "POST",
                requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                        description = "更新用户请求参数",
                        required = true,
                        content = @Content(
                                mediaType = "application/json",
                                schema = @Schema(implementation = UserUpdateRequest.class)
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
                        @ApiResponse(responseCode = "40400", description = "用户不存在",
                                content = @Content)
                }
        )
        @PostMapping("/update")
        @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
        public BaseResponse<Boolean> updateUser(@RequestBody UserUpdateRequest userUpdateRequest) {
            if (userUpdateRequest == null || userUpdateRequest.getId() == null) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR);
            }
            User user = new User();
            BeanUtil.copyProperties(userUpdateRequest, user);
            boolean result = userService.updateById(user);
            ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
            return ResultUtils.success(true);
        }

        /**
         * 分页获取用户列表（管理员）
         *
         * @author ko
         * @since 2024-01-01
         * @param userQueryRequest 查询请求参数
         * @return 返回分页后的用户列表
         *
         * @example 请求示例:
         * {
         *   "current": 1,
         *   "pageSize": 10,
         *   "userName": "测试"
         * }
         */
        @Operation(
                summary = "分页获取用户列表",
                description = "分页查询用户列表，支持按用户名等条件筛选，仅管理员可访问",
                tags = {"用户管理-管理员"},
                method = "POST",
                requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                        description = "用户分页查询请求参数",
                        required = true,
                        content = @Content(
                                mediaType = "application/json",
                                schema = @Schema(implementation = UserQueryRequest.class)
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
        @PostMapping("/list/page/vo")
        @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
        public BaseResponse<Page<UserVO>> listUserVOByPage(@RequestBody UserQueryRequest userQueryRequest) {
            ThrowUtils.throwIf(userQueryRequest == null, ErrorCode.PARAMS_ERROR);
            long current = userQueryRequest.getCurrent();
            long pageSize = userQueryRequest.getPageSize();
            Page<User> userPage = userService.page(Page.of(current, pageSize),
                    userService.getQueryWrapper(userQueryRequest));
            Page<UserVO> userVOPage = new Page<>(current, pageSize, userPage.getTotalRow());
            List<UserVO> userVOList = userService.getUserVOList(userPage.getRecords());
            userVOPage.setRecords(userVOList);
            return ResultUtils.success(userVOPage);
        }


}