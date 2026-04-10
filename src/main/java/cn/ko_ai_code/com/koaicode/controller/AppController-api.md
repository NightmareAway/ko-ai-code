# AppController API 接口文档

- [基本信息](#基本信息)
- [应用管理](#应用管理)
  - [创建应用](#创建应用)
  - [更新应用](#更新应用)
  - [删除应用](#删除应用)
  - [获取应用详情](#获取应用详情)
  - [获取我的应用列表](#获取我的应用列表)
  - [获取精选应用列表](#获取精选应用列表)
  - [部署应用](#部署应用)
  - [保存应用](#保存应用)
  - [根据ID删除应用](#根据id删除应用)
  - [根据ID更新应用](#根据id更新应用)
  - [查询所有应用](#查询所有应用)
  - [根据ID获取应用](#根据id获取应用)
  - [分页查询应用](#分页查询应用)
- [应用管理-管理员](#应用管理-管理员)
  - [管理员删除应用](#管理员删除应用)
  - [管理员更新应用](#管理员更新应用)
  - [管理员获取应用列表](#管理员获取应用列表)
  - [管理员获取应用详情](#管理员获取应用详情)
- [AI代码生成](#ai代码生成)
  - [AI聊天生成代码](#ai聊天生成代码)

---

## 基本信息

| 属性 | 值 |
|------|-----|
| 模块名称 | 应用管理 |
| 模块描述 | 提供应用创建、更新、删除、查询及AI代码生成等完整应用管理功能接口 |
| 作者 | ko |
| 版本 | 1.0 |
| 创建时间 | 2024-01-01 |
| 源码路径 | `cn.ko_ai_code.com.koaicode.controller.AppController` |
| 基础路径 | `/api/app` |

---

## 应用管理

### 创建应用

**接口说明**: 用户创建一个新应用，需要提供初始化prompt用于配置AI代码生成规则

| 属性 | 值 |
|------|-----|
| 请求路径 | `/app/add` |
| HTTP 方法 | `POST` |
| 请求 Content-Type | `application/json` |
| 响应 Content-Type | `application/json` |
| 是否需要认证 | 是 |
| 权限要求 | 登录用户 |

#### 请求参数

| 参数名 | 位置 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|------|--------|------|
| initPrompt | Body | String | 是 | - | 应用初始化的 prompt，用于配置AI代码生成规则 |

#### 请求示例

```json
{
  "initPrompt": "创建一个简单的任务记录网站，包含标题、正文、创建时间等字段"
}
```

#### 响应状态码

| 状态码 | 说明 |
|--------|------|
| 0 | 创建成功，返回应用ID |
| 40000 | 参数错误，initPrompt不能为空 |

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
curl -X POST 'http://localhost:8123/api/app/add' \
  -H 'Content-Type: application/json' \
  -H 'Cookie: JSESSIONID=your-session-id' \
  -d '{
    "initPrompt": "创建一个简单的任务记录网站，包含标题、正文、创建时间等字段"
  }'
```

---

### 更新应用

**接口说明**: 用户更新自己的应用信息，目前仅支持更新应用名称，且仅本人可更新

| 属性 | 值 |
|------|-----|
| 请求路径 | `/app/update` |
| HTTP 方法 | `POST` |
| 请求 Content-Type | `application/json` |
| 响应 Content-Type | `application/json` |
| 是否需要认证 | 是 |
| 权限要求 | 应用所有者 |

#### 请求参数

| 参数名 | 位置 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|------|--------|------|
| id | Body | Long | 是 | - | 应用ID |
| appName | Body | String | 否 | - | 应用名称 |

#### 请求示例

```json
{
  "id": 123456789,
  "appName": "更新后的应用名称"
}
```

#### 响应状态码

| 状态码 | 说明 |
|--------|------|
| 0 | 更新成功 |
| 40000 | 参数错误 |
| 40300 | 无权限，非本人操作 |
| 40400 | 应用不存在 |

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
curl -X POST 'http://localhost:8123/api/app/update' \
  -H 'Content-Type: application/json' \
  -H 'Cookie: JSESSIONID=your-session-id' \
  -d '{
    "id": 123456789,
    "appName": "更新后的应用名称"
  }'
```

---

### 删除应用

**接口说明**: 用户删除自己的应用，管理员可删除任意应用

| 属性 | 值 |
|------|-----|
| 请求路径 | `/app/delete` |
| HTTP 方法 | `POST` |
| 请求 Content-Type | `application/json` |
| 响应 Content-Type | `application/json` |
| 是否需要认证 | 是 |
| 权限要求 | 应用所有者或管理员 |

#### 请求参数

| 参数名 | 位置 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|------|--------|------|
| id | Body | Long | 是 | - | 要删除的应用ID |

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
| 40400 | 应用不存在 |

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
curl -X POST 'http://localhost:8123/api/app/delete' \
  -H 'Content-Type: application/json' \
  -H 'Cookie: JSESSIONID=your-session-id' \
  -d '{
    "id": 123456789
  }'
```

---

### 获取应用详情

**接口说明**: 根据应用ID获取应用详细信息，包含应用名称、prompt、创建时间等

| 属性 | 值 |
|------|-----|
| 请求路径 | `/app/get/vo` |
| HTTP 方法 | `GET` |
| 请求 Content-Type | - |
| 响应 Content-Type | `application/json` |
| 是否需要认证 | 否 |
| 权限要求 | 无 |

#### 请求参数

| 参数名 | 位置 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|------|--------|------|
| id | Query | Long | 是 | - | 应用ID |

#### 请求示例

```
GET /app/get/vo?id=123456789
```

#### 响应状态码

| 状态码 | 说明 |
|--------|------|
| 0 | 获取成功 |
| 40000 | 参数错误 |
| 40400 | 应用不存在 |

#### 响应示例

```json
{
  "code": 0,
  "data": {
    "id": 123456789,
    "appName": "任务记录网站",
    "cover": "https://example.com/cover.jpg",
    "initPrompt": "创建一个简单的任务记录网站",
    "codeGenType": "MULTI_FILE",
    "deployKey": "deploy-key",
    "deployedTime": "2024-01-01T12:00:00",
    "priority": 0,
    "userId": 100,
    "createTime": "2024-01-01T00:00:00",
    "updateTime": "2024-01-01T00:00:00",
    "user": {
      "id": 100,
      "userName": "用户名",
      "userAvatar": "https://example.com/avatar.jpg"
    }
  },
  "message": ""
}
```

#### curl 示例

```bash
curl -X GET 'http://localhost:8123/api/app/get/vo?id=123456789'
```

---

### 获取我的应用列表

**接口说明**: 分页获取当前登录用户创建的应用列表，每页最多20条

| 属性 | 值 |
|------|-----|
| 请求路径 | `/app/my/list/page/vo` |
| HTTP 方法 | `POST` |
| 请求 Content-Type | `application/json` |
| 响应 Content-Type | `application/json` |
| 是否需要认证 | 是 |
| 权限要求 | 登录用户 |

#### 请求参数

| 参数名 | 位置 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|------|--------|------|
| current | Body | Integer | 否 | 1 | 当前页号，从 1 开始 |
| pageSize | Body | Integer | 否 | 10 | 页面大小，最大 20 |
| sortField | Body | String | 否 | - | 排序字段 |
| sortOrder | Body | String | 否 | descend | 排序顺序：ascend/descend |
| id | Body | Long | 否 | - | 应用ID |
| appName | Body | String | 否 | - | 应用名称（模糊匹配） |
| cover | Body | String | 否 | - | 应用封面 |
| initPrompt | Body | String | 否 | - | 初始化 prompt |
| codeGenType | Body | String | 否 | - | 代码生成类型 |
| deployKey | Body | String | 否 | - | 部署标识 |
| priority | Body | Integer | 否 | - | 优先级 |
| userId | Body | Long | 否 | 当前登录用户 | 创建用户ID（自动设置为当前用户） |

#### 请求示例

```json
{
  "current": 1,
  "pageSize": 10
}
```

#### 响应状态码

| 状态码 | 说明 |
|--------|------|
| 0 | 查询成功 |
| 40000 | 参数错误 |

#### 响应示例

```json
{
  "code": 0,
  "data": {
    "records": [
      {
        "id": 123456789,
        "appName": "任务记录网站",
        "cover": "https://example.com/cover.jpg",
        "initPrompt": "创建一个简单的任务记录网站",
        "codeGenType": "MULTI_FILE",
        "priority": 0,
        "userId": 100,
        "createTime": "2024-01-01T00:00:00",
        "user": {
          "id": 100,
          "userName": "用户名",
          "userAvatar": "https://example.com/avatar.jpg"
        }
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
curl -X POST 'http://localhost:8123/api/app/my/list/page/vo' \
  -H 'Content-Type: application/json' \
  -H 'Cookie: JSESSIONID=your-session-id' \
  -d '{
    "current": 1,
    "pageSize": 10
  }'
```

---

### 获取精选应用列表

**接口说明**: 分页获取被标记为精选的应用列表，用于应用市场展示

| 属性 | 值 |
|------|-----|
| 请求路径 | `/app/good/list/page/vo` |
| HTTP 方法 | `POST` |
| 请求 Content-Type | `application/json` |
| 响应 Content-Type | `application/json` |
| 是否需要认证 | 否 |
| 权限要求 | 无 |

#### 请求参数

| 参数名 | 位置 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|------|--------|------|
| current | Body | Integer | 否 | 1 | 当前页号，从 1 开始 |
| pageSize | Body | Integer | 否 | 10 | 页面大小，最大 20 |
| sortField | Body | String | 否 | - | 排序字段 |
| sortOrder | Body | String | 否 | descend | 排序顺序：ascend/descend |
| id | Body | Long | 否 | - | 应用ID |
| appName | Body | String | 否 | - | 应用名称（模糊匹配） |
| cover | Body | String | 否 | - | 应用封面 |
| initPrompt | Body | String | 否 | - | 初始化 prompt |
| codeGenType | Body | String | 否 | - | 代码生成类型 |
| deployKey | Body | String | 否 | - | 部署标识 |
| priority | Body | Integer | 否 | - | 优先级（自动设置为精选） |
| userId | Body | Long | 否 | - | 创建用户ID |

#### 请求示例

```json
{
  "current": 1,
  "pageSize": 10
}
```

#### 响应状态码

| 状态码 | 说明 |
|--------|------|
| 0 | 查询成功 |
| 40000 | 参数错误 |

#### 响应示例

```json
{
  "code": 0,
  "data": {
    "records": [
      {
        "id": 123456789,
        "appName": "精选应用",
        "cover": "https://example.com/cover.jpg",
        "initPrompt": "创建一个简单的任务记录网站",
        "codeGenType": "MULTI_FILE",
        "priority": 1,
        "userId": 100,
        "createTime": "2024-01-01T00:00:00",
        "user": {
          "id": 100,
          "userName": "用户名",
          "userAvatar": "https://example.com/avatar.jpg"
        }
      }
    ],
    "total": 50,
    "size": 10,
    "current": 1,
    "recordsPage": 10,
    "totalPage": 5
  },
  "message": ""
}
```

#### curl 示例

```bash
curl -X POST 'http://localhost:8123/api/app/good/list/page/vo' \
  -H 'Content-Type: application/json' \
  -d '{
    "current": 1,
    "pageSize": 10
  }'
```

---

### 部署应用

**接口说明**: 将生成的应用代码部署到服务器，返回部署后的访问URL

| 属性 | 值 |
|------|-----|
| 请求路径 | `/app/deploy` |
| HTTP 方法 | `POST` |
| 请求 Content-Type | `application/json` |
| 响应 Content-Type | `application/json` |
| 是否需要认证 | 是 |
| 权限要求 | 登录用户 |

#### 请求参数

| 参数名 | 位置 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|------|--------|------|
| appId | Body | Long | 是 | - | 应用ID |

#### 请求示例

```json
{
  "appId": 123456789
}
```

#### 响应状态码

| 状态码 | 说明 |
|--------|------|
| 0 | 部署成功，返回部署URL |
| 40000 | 参数错误 |
| 40100 | 未登录 |
| 40400 | 应用不存在 |

#### 响应示例

```json
{
  "code": 0,
  "data": "https://your-app.example.com",
  "message": ""
}
```

#### curl 示例

```bash
curl -X POST 'http://localhost:8123/api/app/deploy' \
  -H 'Content-Type: application/json' \
  -H 'Cookie: JSESSIONID=your-session-id' \
  -d '{
    "appId": 123456789
  }'
```

---

### 保存应用

**接口说明**: 直接保存应用实体数据到数据库

| 属性 | 值 |
|------|-----|
| 请求路径 | `/app/save` |
| HTTP 方法 | `POST` |
| 请求 Content-Type | `application/json` |
| 响应 Content-Type | `application/json` |
| 是否需要认证 | 否 |
| 权限要求 | 无 |

#### 请求参数

| 参数名 | 位置 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|------|--------|------|
| appName | Body | String | 否 | - | 应用名称 |
| cover | Body | String | 否 | - | 应用封面 |
| initPrompt | Body | String | 否 | - | 初始化 prompt |
| codeGenType | Body | String | 否 | - | 代码生成类型 |
| deployKey | Body | String | 否 | - | 部署标识 |
| deployedTime | Body | LocalDateTime | 否 | - | 部署时间 |
| priority | Body | Integer | 否 | - | 优先级 |
| userId | Body | Long | 否 | - | 创建用户ID |
| editTime | Body | LocalDateTime | 否 | - | 编辑时间 |
| createTime | Body | LocalDateTime | 否 | - | 创建时间 |
| updateTime | Body | LocalDateTime | 否 | - | 更新时间 |

#### 请求示例

```json
{
  "appName": "测试应用",
  "initPrompt": "初始化prompt",
  "codeGenType": "MULTI_FILE",
  "userId": 100
}
```

#### 响应状态码

| 状态码 | 说明 |
|--------|------|
| 0 | 保存成功 |
| 40000 | 参数错误 |

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
curl -X POST 'http://localhost:8123/api/app/save' \
  -H 'Content-Type: application/json' \
  -d '{
    "appName": "测试应用",
    "initPrompt": "初始化prompt",
    "codeGenType": "MULTI_FILE"
  }'
```

---

### 根据ID删除应用

**接口说明**: 根据应用ID直接删除应用

| 属性 | 值 |
|------|-----|
| 请求路径 | `/app/remove/{id}` |
| HTTP 方法 | `DELETE` |
| 请求 Content-Type | - |
| 响应 Content-Type | `application/json` |
| 是否需要认证 | 否 |
| 权限要求 | 无 |

#### 请求参数

| 参数名 | 位置 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|------|--------|------|
| id | Path | Long | 是 | - | 应用ID |

#### 请求示例

```
DELETE /app/remove/123456789
```

#### 响应状态码

| 状态码 | 说明 |
|--------|------|
| 0 | 删除成功 |
| 40000 | 参数错误 |

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
curl -X DELETE 'http://localhost:8123/api/app/remove/123456789'
```

---

### 根据ID更新应用

**接口说明**: 根据应用ID更新应用信息

| 属性 | 值 |
|------|-----|
| 请求路径 | `/app/update` |
| HTTP 方法 | `PUT` |
| 请求 Content-Type | `application/json` |
| 响应 Content-Type | `application/json` |
| 是否需要认证 | 否 |
| 权限要求 | 无 |

#### 请求参数

| 参数名 | 位置 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|------|--------|------|
| id | Body | Long | 是 | - | 应用ID |
| appName | Body | String | 否 | - | 应用名称 |
| cover | Body | String | 否 | - | 应用封面 |
| initPrompt | Body | String | 否 | - | 初始化 prompt |
| codeGenType | Body | String | 否 | - | 代码生成类型 |
| deployKey | Body | String | 否 | - | 部署标识 |
| deployedTime | Body | LocalDateTime | 否 | - | 部署时间 |
| priority | Body | Integer | 否 | - | 优先级 |
| userId | Body | Long | 否 | - | 创建用户ID |
| editTime | Body | LocalDateTime | 否 | - | 编辑时间 |
| createTime | Body | LocalDateTime | 否 | - | 创建时间 |
| updateTime | Body | LocalDateTime | 否 | - | 更新时间 |

#### 请求示例

```json
{
  "id": 123456789,
  "appName": "更新后的应用名称"
}
```

#### 响应状态码

| 状态码 | 说明 |
|--------|------|
| 0 | 更新成功 |
| 40000 | 参数错误 |

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
curl -X PUT 'http://localhost:8123/api/app/update' \
  -H 'Content-Type: application/json' \
  -d '{
    "id": 123456789,
    "appName": "更新后的应用名称"
  }'
```

---

### 查询所有应用

**接口说明**: 查询数据库中的所有应用记录

| 属性 | 值 |
|------|-----|
| 请求路径 | `/app/list` |
| HTTP 方法 | `GET` |
| 请求 Content-Type | - |
| 响应 Content-Type | `application/json` |
| 是否需要认证 | 否 |
| 权限要求 | 无 |

#### 请求参数

无

#### 响应状态码

| 状态码 | 说明 |
|--------|------|
| 0 | 查询成功 |

#### 响应示例

```json
{
  "code": 0,
  "data": [
    {
      "id": 123456789,
      "appName": "任务记录网站",
      "cover": "https://example.com/cover.jpg",
      "initPrompt": "创建一个简单的任务记录网站",
      "codeGenType": "MULTI_FILE",
      "deployKey": "deploy-key",
      "deployedTime": "2024-01-01T12:00:00",
      "priority": 0,
      "userId": 100,
      "editTime": "2024-01-01T00:00:00",
      "createTime": "2024-01-01T00:00:00",
      "updateTime": "2024-01-01T00:00:00"
    }
  ],
  "message": ""
}
```

#### curl 示例

```bash
curl -X GET 'http://localhost:8123/api/app/list'
```

---

### 根据ID获取应用

**接口说明**: 根据应用ID获取应用完整信息

| 属性 | 值 |
|------|-----|
| 请求路径 | `/app/getInfo/{id}` |
| HTTP 方法 | `GET` |
| 请求 Content-Type | - |
| 响应 Content-Type | `application/json` |
| 是否需要认证 | 否 |
| 权限要求 | 无 |

#### 请求参数

| 参数名 | 位置 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|------|--------|------|
| id | Path | Long | 是 | - | 应用ID |

#### 请求示例

```
GET /app/getInfo/123456789
```

#### 响应状态码

| 状态码 | 说明 |
|--------|------|
| 0 | 获取成功 |
| 40000 | 参数错误 |

#### 响应示例

```json
{
  "code": 0,
  "data": {
    "id": 123456789,
    "appName": "任务记录网站",
    "cover": "https://example.com/cover.jpg",
    "initPrompt": "创建一个简单的任务记录网站",
    "codeGenType": "MULTI_FILE",
    "deployKey": "deploy-key",
    "deployedTime": "2024-01-01T12:00:00",
    "priority": 0,
    "userId": 100,
    "editTime": "2024-01-01T00:00:00",
    "createTime": "2024-01-01T00:00:00",
    "updateTime": "2024-01-01T00:00:00"
  },
  "message": ""
}
```

#### curl 示例

```bash
curl -X GET 'http://localhost:8123/api/app/getInfo/123456789'
```

---

### 分页查询应用

**接口说明**: 使用MyBatis-Flex分页插件查询应用列表

| 属性 | 值 |
|------|-----|
| 请求路径 | `/app/page` |
| HTTP 方法 | `GET` |
| 请求 Content-Type | - |
| 响应 Content-Type | `application/json` |
| 是否需要认证 | 否 |
| 权限要求 | 无 |

#### 请求参数

| 参数名 | 位置 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|------|--------|------|
| page | Query | Object | 是 | - | 分页对象，包含 current 和 pageSize |

#### 请求示例

```
GET /app/page?page.current=1&page.pageSize=10
```

#### 响应状态码

| 状态码 | 说明 |
|--------|------|
| 0 | 查询成功 |
| 40000 | 参数错误 |

#### 响应示例

```json
{
  "code": 0,
  "data": {
    "records": [
      {
        "id": 123456789,
        "appName": "任务记录网站",
        "cover": "https://example.com/cover.jpg",
        "initPrompt": "创建一个简单的任务记录网站",
        "codeGenType": "MULTI_FILE",
        "priority": 0,
        "userId": 100,
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
curl -X GET 'http://localhost:8123/api/app/page?page.current=1&page.pageSize=10'
```

---

## 应用管理-管理员

### 管理员删除应用

**接口说明**: 管理员删除任意用户创建的应用

| 属性 | 值 |
|------|-----|
| 请求路径 | `/app/admin/delete` |
| HTTP 方法 | `POST` |
| 请求 Content-Type | `application/json` |
| 响应 Content-Type | `application/json` |
| 是否需要认证 | 是 |
| 权限要求 | 管理员 (admin) |

#### 请求参数

| 参数名 | 位置 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|------|--------|------|
| id | Body | Long | 是 | - | 要删除的应用ID |

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
| 40300 | 无权限，需要管理员角色 |
| 40400 | 应用不存在 |

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
curl -X POST 'http://localhost:8123/api/app/admin/delete' \
  -H 'Content-Type: application/json' \
  -H 'Cookie: JSESSIONID=your-session-id' \
  -d '{
    "id": 123456789
  }'
```

---

### 管理员更新应用

**接口说明**: 管理员更新任意应用信息，包括应用名称、优先级等

| 属性 | 值 |
|------|-----|
| 请求路径 | `/app/admin/update` |
| HTTP 方法 | `POST` |
| 请求 Content-Type | `application/json` |
| 响应 Content-Type | `application/json` |
| 是否需要认证 | 是 |
| 权限要求 | 管理员 (admin) |

#### 请求参数

| 参数名 | 位置 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|------|--------|------|
| id | Body | Long | 是 | - | 应用ID |
| appName | Body | String | 否 | - | 应用名称 |
| cover | Body | String | 否 | - | 应用封面 |
| priority | Body | Integer | 否 | - | 优先级 |

#### 请求示例

```json
{
  "id": 123456789,
  "appName": "管理员更新的应用名称",
  "priority": 1
}
```

#### 响应状态码

| 状态码 | 说明 |
|--------|------|
| 0 | 更新成功 |
| 40000 | 参数错误 |
| 40300 | 无权限 |
| 40400 | 应用不存在 |

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
curl -X POST 'http://localhost:8123/api/app/admin/update' \
  -H 'Content-Type: application/json' \
  -H 'Cookie: JSESSIONID=your-session-id' \
  -d '{
    "id": 123456789,
    "appName": "管理员更新的应用名称",
    "priority": 1
  }'
```

---

### 管理员获取应用列表

**接口说明**: 分页获取所有应用列表，用于后台管理

| 属性 | 值 |
|------|-----|
| 请求路径 | `/app/admin/list/page/vo` |
| HTTP 方法 | `POST` |
| 请求 Content-Type | `application/json` |
| 响应 Content-Type | `application/json` |
| 是否需要认证 | 是 |
| 权限要求 | 管理员 (admin) |

#### 请求参数

| 参数名 | 位置 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|------|--------|------|
| current | Body | Integer | 否 | 1 | 当前页号，从 1 开始 |
| pageSize | Body | Integer | 否 | 10 | 页面大小 |
| sortField | Body | String | 否 | - | 排序字段 |
| sortOrder | Body | String | 否 | descend | 排序顺序：ascend/descend |
| id | Body | Long | 否 | - | 应用ID |
| appName | Body | String | 否 | - | 应用名称（模糊匹配） |
| cover | Body | String | 否 | - | 应用封面 |
| initPrompt | Body | String | 否 | - | 初始化 prompt |
| codeGenType | Body | String | 否 | - | 代码生成类型 |
| deployKey | Body | String | 否 | - | 部署标识 |
| priority | Body | Integer | 否 | - | 优先级 |
| userId | Body | Long | 否 | - | 创建用户ID |

#### 请求示例

```json
{
  "current": 1,
  "pageSize": 10
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
        "appName": "任务记录网站",
        "cover": "https://example.com/cover.jpg",
        "initPrompt": "创建一个简单的任务记录网站",
        "codeGenType": "MULTI_FILE",
        "priority": 0,
        "userId": 100,
        "createTime": "2024-01-01T00:00:00",
        "user": {
          "id": 100,
          "userName": "用户名",
          "userAvatar": "https://example.com/avatar.jpg"
        }
      }
    ],
    "total": 200,
    "size": 10,
    "current": 1,
    "recordsPage": 10,
    "totalPage": 20
  },
  "message": ""
}
```

#### curl 示例

```bash
curl -X POST 'http://localhost:8123/api/app/admin/list/page/vo' \
  -H 'Content-Type: application/json' \
  -H 'Cookie: JSESSIONID=your-session-id' \
  -d '{
    "current": 1,
    "pageSize": 10
  }'
```

---

### 管理员获取应用详情

**接口说明**: 管理员获取任意应用的详细信息

| 属性 | 值 |
|------|-----|
| 请求路径 | `/app/admin/get/vo` |
| HTTP 方法 | `GET` |
| 请求 Content-Type | - |
| 响应 Content-Type | `application/json` |
| 是否需要认证 | 是 |
| 权限要求 | 管理员 (admin) |

#### 请求参数

| 参数名 | 位置 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|------|--------|------|
| id | Query | Long | 是 | - | 应用ID |

#### 请求示例

```
GET /app/admin/get/vo?id=123456789
```

#### 响应状态码

| 状态码 | 说明 |
|--------|------|
| 0 | 获取成功 |
| 40000 | 参数错误 |
| 40300 | 无权限 |
| 40400 | 应用不存在 |

#### 响应示例

```json
{
  "code": 0,
  "data": {
    "id": 123456789,
    "appName": "任务记录网站",
    "cover": "https://example.com/cover.jpg",
    "initPrompt": "创建一个简单的任务记录网站",
    "codeGenType": "MULTI_FILE",
    "deployKey": "deploy-key",
    "deployedTime": "2024-01-01T12:00:00",
    "priority": 0,
    "userId": 100,
    "createTime": "2024-01-01T00:00:00",
    "updateTime": "2024-01-01T00:00:00",
    "user": {
      "id": 100,
      "userName": "用户名",
      "userAvatar": "https://example.com/avatar.jpg"
    }
  },
  "message": ""
}
```

#### curl 示例

```bash
curl -X GET 'http://localhost:8123/api/app/admin/get/vo?id=123456789' \
  -H 'Cookie: JSESSIONID=your-session-id'
```

---

## AI代码生成

### AI聊天生成代码

**接口说明**: 通过AI对话生成代码，使用SSE流式返回生成进度和结果，支持HTML/CSS/JS多文件生成

| 属性 | 值 |
|------|-----|
| 请求路径 | `/app/chat/gen/code` |
| HTTP 方法 | `GET` |
| 请求 Content-Type | - |
| 响应 Content-Type | `text/event-stream` |
| 是否需要认证 | 是 |
| 权限要求 | 登录用户 |

#### 请求参数

| 参数名 | 位置 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|------|--------|------|
| appId | Query | Long | 是 | - | 应用ID |
| message | Query | String | 是 | - | 用户消息/需求描述 |

#### 请求示例

```
GET /app/chat/gen/code?appId=123456789&message=帮我创建一个登录页面
```

#### 响应状态码

| 状态码 | 说明 |
|--------|------|
| 0 | 成功建立SSE连接 |
| 40000 | 参数错误，如应用ID无效或消息为空 |
| 40100 | 未登录 |
| 40400 | 应用不存在 |

#### 响应示例 (SSE流)

```
data: {"d": "正在分析需求..."}
data: {"d": "开始生成代码..."}
data: {"d": "<!-- HTML代码 -->"}
event: done
data: ""
```

#### curl 示例

```bash
curl -N -X GET 'http://localhost:8123/api/app/chat/gen/code?appId=123456789&message=帮我创建一个登录页面' \
  -H 'Cookie: JSESSIONID=your-session-id'
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
