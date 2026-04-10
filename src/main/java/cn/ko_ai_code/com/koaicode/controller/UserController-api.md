# UserController API 接口文档

- [基本信息](#基本信息)
- [用户管理](#用户管理)
  - [用户注册](#用户注册)
  - [用户登录](#用户登录)
  - [获取当前登录用户](#获取当前登录用户)
  - [用户登出](#用户登出)
  - [获取用户公开信息](#获取用户公开信息)
  - [创建用户](#创建用户)
  - [获取用户详情](#获取用户详情)
  - [删除用户](#删除用户)
  - [更新用户信息](#更新用户信息)
  - [分页获取用户列表](#分页获取用户列表)

---

## 基本信息

| 属性 | 值 |
|------|-----|
| 模块名称 | 用户管理 |
| 模块描述 | 提供用户注册、登录、CRUD等完整用户管理功能接口 |
| 作者 | ko |
| 版本 | 1.0 |
| 创建时间 | 2024-01-01 |
| 源码路径 | `cn.ko_ai_code.com.koaicode.controller.UserController` |
| 基础路径 | `/api/user` |

---

## 用户管理

### 用户注册

**接口说明**: 提供新用户注册功能，账号需唯一，密码长度不少于8位，两次密码输入必须一致

| 属性 | 值 |
|------|-----|
| 请求路径 | `/user/register` |
| HTTP 方法 | `POST` |
| 请求 Content-Type | `application/json` |
| 响应 Content-Type | `application/json` |
| 是否需要认证 | 否 |
| 权限要求 | 无 |

#### 请求参数

| 参数名 | 位置 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|------|--------|------|
| userAccount | Body | String | 是 | - | 账号，长度建议 4-20 位 |
| userPassword | Body | String | 是 | - | 密码，最少 8 位 |
| checkPassword | Body | String | 是 | - | 确认密码，需与密码一致 |

#### 请求示例

```json
{
  "userAccount": "testuser",
  "userPassword": "password123",
  "checkPassword": "password123"
}
```

#### 响应状态码

| 状态码 | 说明 |
|--------|------|
| 0 | 注册成功，返回用户ID |
| 40000 | 参数错误，如密码不匹配、账号已存在等 |

#### 响应示例

```json
{
  "code": 0,
  "data": 123456789,
  "message": ""
}
```

#### curl 示例

```bash
curl -X POST 'http://localhost:8123/api/user/register' \
  -H 'Content-Type: application/json' \
  -d '{
    "userAccount": "testuser",
    "userPassword": "password123",
    "checkPassword": "password123"
  }'
```

---

### 用户登录

**接口说明**: 用户使用账号密码登录，登录成功后将用户信息存入 session

| 属性 | 值 |
|------|-----|
| 请求路径 | `/user/login` |
| HTTP 方法 | `POST` |
| 请求 Content-Type | `application/json` |
| 响应 Content-Type | `application/json` |
| 是否需要认证 | 否 |
| 权限要求 | 无 |

#### 请求参数

| 参数名 | 位置 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|------|--------|------|
| userAccount | Body | String | 是 | - | 账号 |
| userPassword | Body | String | 是 | - | 密码 |

#### 请求示例

```json
{
  "userAccount": "testuser",
  "userPassword": "password123"
}
```

#### 响应状态码

| 状态码 | 说明 |
|--------|------|
| 0 | 登录成功，返回用户信息 |
| 40000 | 参数错误 |
| 40100 | 认证失败，账号或密码错误 |

#### 响应示例

```json
{
  "code": 0,
  "data": {
    "id": 123456789,
    "userAccount": "testuser",
    "userName": "测试用户",
    "userAvatar": "https://example.com/avatar.jpg",
    "userProfile": "用户简介",
    "userRole": "user",
    "createTime": "2024-01-01T00:00:00",
    "updateTime": "2024-01-01T00:00:00"
  },
  "message": ""
}
```

#### curl 示例

```bash
curl -X POST 'http://localhost:8123/api/user/login' \
  -H 'Content-Type: application/json' \
  -d '{
    "userAccount": "testuser",
    "userPassword": "password123"
  }'
```

---

### 获取当前登录用户

**接口说明**: 从 session 中获取当前登录用户的信息，无需传参

| 属性 | 值 |
|------|-----|
| 请求路径 | `/user/get/login` |
| HTTP 方法 | `GET` |
| 请求 Content-Type | - |
| 响应 Content-Type | `application/json` |
| 是否需要认证 | 是 |
| 权限要求 | 登录用户 |

#### 请求参数

无

#### 响应状态码

| 状态码 | 说明 |
|--------|------|
| 0 | 获取成功 |
| 40100 | 未登录或登录已过期 |

#### 响应示例

```json
{
  "code": 0,
  "data": {
    "id": 123456789,
    "userAccount": "testuser",
    "userName": "测试用户",
    "userAvatar": "https://example.com/avatar.jpg",
    "userProfile": "用户简介",
    "userRole": "user",
    "createTime": "2024-01-01T00:00:00",
    "updateTime": "2024-01-01T00:00:00"
  },
  "message": ""
}
```

#### curl 示例

```bash
curl -X GET 'http://localhost:8123/api/user/get/login' \
  -H 'Cookie: JSESSIONID=your-session-id'
```

---

### 用户登出

**接口说明**: 清除 session 中的用户信息，完成登出操作

| 属性 | 值 |
|------|-----|
| 请求路径 | `/user/logout` |
| HTTP 方法 | `POST` |
| 请求 Content-Type | - |
| 响应 Content-Type | `application/json` |
| 是否需要认证 | 是 |
| 权限要求 | 登录用户 |

#### 请求参数

无

#### 响应状态码

| 状态码 | 说明 |
|--------|------|
| 0 | 登出成功 |
| 40000 | 请求参数错误 |

#### 响应示例

```json
{
  "code": 0,
  "data": true,
  "message": ""
}
```

#### curl 示例

```bash
curl -X POST 'http://localhost:8123/api/user/logout' \
  -H 'Cookie: JSESSIONID=your-session-id'
```

---

### 获取用户公开信息

**接口说明**: 根据用户ID获取用户脱敏后的公开信息

| 属性 | 值 |
|------|-----|
| 请求路径 | `/user/get/vo` |
| HTTP 方法 | `GET` |
| 请求 Content-Type | - |
| 响应 Content-Type | `application/json` |
| 是否需要认证 | 否 |
| 权限要求 | 无 |

#### 请求参数

| 参数名 | 位置 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|------|--------|------|
| id | Query | Long | 是 | - | 用户ID |

#### 请求示例

```
GET /user/get/vo?id=123456789
```

#### 响应状态码

| 状态码 | 说明 |
|--------|------|
| 0 | 获取成功 |
| 40000 | 参数错误 |
| 40400 | 用户不存在 |

#### 响应示例

```json
{
  "code": 0,
  "data": {
    "id": 123456789,
    "userAccount": "testuser",
    "userName": "测试用户",
    "userAvatar": "https://example.com/avatar.jpg",
    "userProfile": "用户简介",
    "userRole": "user",
    "createTime": "2024-01-01T00:00:00"
  },
  "message": ""
}
```

#### curl 示例

```bash
curl -X GET 'http://localhost:8123/api/user/get/vo?id=123456789'
```

---

### 创建用户

**接口说明**: 管理员创建新用户，默认密码为 `12345678`，创建后返回用户ID

| 属性 | 值 |
|------|-----|
| 请求路径 | `/user/add` |
| HTTP 方法 | `POST` |
| 请求 Content-Type | `application/json` |
| 响应 Content-Type | `application/json` |
| 是否需要认证 | 是 |
| 权限要求 | 管理员 (admin) |

#### 请求参数

| 参数名 | 位置 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|------|--------|------|
| userName | Body | String | 否 | - | 用户昵称 |
| userAccount | Body | String | 是 | - | 账号，需唯一 |
| userAvatar | Body | String | 否 | - | 用户头像 URL |
| userProfile | Body | String | 否 | - | 用户简介 |
| userRole | Body | String | 否 | user | 用户角色: user, admin |

#### 请求示例

```json
{
  "userAccount": "newuser",
  "userName": "新用户",
  "userAvatar": "https://example.com/avatar.jpg",
  "userProfile": "用户简介",
  "userRole": "user"
}
```

#### 响应状态码

| 状态码 | 说明 |
|--------|------|
| 0 | 创建成功 |
| 40000 | 参数错误 |
| 40300 | 无权限，需要管理员角色 |

#### 响应示例

```json
{
  "code": 0,
  "data": 123456790,
  "message": ""
}
```

#### curl 示例

```bash
curl -X POST 'http://localhost:8123/api/user/add' \
  -H 'Content-Type: application/json' \
  -H 'Cookie: JSESSIONID=your-session-id' \
  -d '{
    "userAccount": "newuser",
    "userName": "新用户",
    "userAvatar": "https://example.com/avatar.jpg",
    "userProfile": "用户简介",
    "userRole": "user"
  }'
```

---

### 获取用户详情

**接口说明**: 根据用户ID获取用户完整信息，仅管理员可访问

| 属性 | 值 |
|------|-----|
| 请求路径 | `/user/get` |
| HTTP 方法 | `GET` |
| 请求 Content-Type | - |
| 响应 Content-Type | `application/json` |
| 是否需要认证 | 是 |
| 权限要求 | 管理员 (admin) |

#### 请求参数

| 参数名 | 位置 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|------|--------|------|
| id | Query | Long | 是 | - | 用户ID |

#### 请求示例

```
GET /user/get?id=123456789
```

#### 响应状态码

| 状态码 | 说明 |
|--------|------|
| 0 | 获取成功 |
| 40000 | 参数错误，ID无效 |
| 40300 | 无权限 |
| 40400 | 用户不存在 |

#### 响应示例

```json
{
  "code": 0,
  "data": {
    "id": 123456789,
    "userAccount": "testuser",
    "userName": "测试用户",
    "userAvatar": "https://example.com/avatar.jpg",
    "userProfile": "用户简介",
    "userRole": "user",
    "userPassword": "加密后的密码",
    "createTime": "2024-01-01T00:00:00",
    "editTime": "2024-01-01T00:00:00"
  },
  "message": ""
}
```

#### curl 示例

```bash
curl -X GET 'http://localhost:8123/api/user/get?id=123456789' \
  -H 'Cookie: JSESSIONID=your-session-id'
```

---

### 删除用户

**接口说明**: 根据用户ID删除用户，仅管理员可操作

| 属性 | 值 |
|------|-----|
| 请求路径 | `/user/delete` |
| HTTP 方法 | `POST` |
| 请求 Content-Type | `application/json` |
| 响应 Content-Type | `application/json` |
| 是否需要认证 | 是 |
| 权限要求 | 管理员 (admin) |

#### 请求参数

| 参数名 | 位置 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|------|--------|------|
| id | Body | Long | 是 | - | 要删除的用户ID |

#### 请求示例

```json
{
  "id": 123456789
}
```

#### 响应状态码

| 状态码 | 说明 |
|--------|------|
| 0 | 删除成功 |
| 40000 | 参数错误 |
| 40300 | 无权限 |

#### 响应示例

```json
{
  "code": 0,
  "data": true,
  "message": ""
}
```

#### curl 示例

```bash
curl -X POST 'http://localhost:8123/api/user/delete' \
  -H 'Content-Type: application/json' \
  -H 'Cookie: JSESSIONID=your-session-id' \
  -d '{
    "id": 123456789
  }'
```

---

### 更新用户信息

**接口说明**: 更新用户信息，仅管理员可操作，可更新用户名、头像等

| 属性 | 值 |
|------|-----|
| 请求路径 | `/user/update` |
| HTTP 方法 | `POST` |
| 请求 Content-Type | `application/json` |
| 响应 Content-Type | `application/json` |
| 是否需要认证 | 是 |
| 权限要求 | 管理员 (admin) |

#### 请求参数

| 参数名 | 位置 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|------|--------|------|
| id | Body | Long | 是 | - | 用户ID |
| userName | Body | String | 否 | - | 用户昵称 |
| userAvatar | Body | String | 否 | - | 用户头像 URL |
| userProfile | Body | String | 否 | - | 用户简介 |
| userRole | Body | String | 否 | - | 用户角色：user/admin |

#### 请求示例

```json
{
  "id": 123456789,
  "userName": "更新后的用户名",
  "userAvatar": "https://example.com/new-avatar.jpg",
  "userProfile": "更新后的简介",
  "userRole": "user"
}
```

#### 响应状态码

| 状态码 | 说明 |
|--------|------|
| 0 | 更新成功 |
| 40000 | 参数错误 |
| 40300 | 无权限 |
| 40400 | 用户不存在 |

#### 响应示例

```json
{
  "code": 0,
  "data": true,
  "message": ""
}
```

#### curl 示例

```bash
curl -X POST 'http://localhost:8123/api/user/update' \
  -H 'Content-Type: application/json' \
  -H 'Cookie: JSESSIONID=your-session-id' \
  -d '{
    "id": 123456789,
    "userName": "更新后的用户名",
    "userAvatar": "https://example.com/new-avatar.jpg"
  }'
```

---

### 分页获取用户列表

**接口说明**: 分页查询用户列表，支持按用户名等条件筛选，仅管理员可访问

| 属性 | 值 |
|------|-----|
| 请求路径 | `/user/list/page/vo` |
| HTTP 方法 | `POST` |
| 请求 Content-Type | `application/json` |
| 响应 Content-Type | `application/json` |
| 是否需要认证 | 是 |
| 权限要求 | 管理员 (admin) |

#### 请求参数

| 参数名 | 位置 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|------|--------|------|
| current | Body | Integer | 否 | 1 | 当前页号，从 1 开始 |
| pageSize | Body | Integer | 否 | 10 | 页面大小，最大 20 |
| sortField | Body | String | 否 | - | 排序字段 |
| sortOrder | Body | String | 否 | descend | 排序顺序：ascend/descend |
| id | Body | Long | 否 | - | 用户ID |
| userName | Body | String | 否 | - | 用户昵称（模糊匹配） |
| userAccount | Body | String | 否 | - | 账号（模糊匹配） |
| userProfile | Body | String | 否 | - | 简介（模糊匹配） |
| userRole | Body | String | 否 | - | 用户角色：user/admin/ban |

#### 请求示例

```json
{
  "current": 1,
  "pageSize": 10,
  "userName": "测试",
  "sortField": "createTime",
  "sortOrder": "descend"
}
```

#### 响应状态码

| 状态码 | 说明 |
|--------|------|
| 0 | 查询成功 |
| 40000 | 参数错误 |
| 40300 | 无权限 |

#### 响应示例

```json
{
  "code": 0,
  "data": {
    "records": [
      {
        "id": 123456789,
        "userAccount": "testuser",
        "userName": "测试用户",
        "userAvatar": "https://example.com/avatar.jpg",
        "userProfile": "用户简介",
        "userRole": "user",
        "createTime": "2024-01-01T00:00:00"
      }
    ],
    "total": 100,
    "size": 10,
    "current": 1,
    "recordsPage": 10,
    "totalPage": 10
  },
  "message": ""
}
```

#### curl 示例

```bash
curl -X POST 'http://localhost:8123/api/user/list/page/vo' \
  -H 'Content-Type: application/json' \
  -H 'Cookie: JSESSIONID=your-session-id' \
  -d '{
    "current": 1,
    "pageSize": 10,
    "userName": "测试"
  }'
```

---

## 通用响应结构

所有接口均使用统一的响应封装 `BaseResponse<T>`：

| 字段 | 类型 | 说明 |
|------|------|------|
| code | int | 状态码，0 表示成功，非 0 表示失败 |
| data | T | 响应数据，类型根据接口而定 |
| message | String | 错误信息或成功提示 |

## 通用错误码

| 错误码 | 说明 |
|--------|------|
| 0 | 成功 |
| 40000 | 参数错误 |
| 40100 | 未授权/认证失败 |
| 40300 | 无权限访问 |
| 40400 | 资源不存在 |
| 50000 | 内部服务器错误 |

---

## 更新记录

| 版本 | 日期 | 作者 | 变更内容 |
|------|------|------|----------|
| 1.0 | 2024-01-01 | ko | 初始版本 |
