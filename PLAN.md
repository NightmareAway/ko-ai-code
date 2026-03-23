### 3.20接下来要做的事
1. 添加LangChain4j的依赖
2. 创建一个Service用于调用ai
3. 添加配置文件信息，改为运行本地配置文件（保证密钥安全性），.gitignore添加忽视
4. 使用ai生成提示词，分为两套，一个是单纯的HTML页面，另外一个是多文件类型 （阿里云相关网址 https://help.aliyun.com/zh/model-studio/prompt-engineering-guide ）
5. 在阿里云百炼调研查询哪个ai比较好
6. 去deepseek官网获取apikey
7. 添加配置，让ai返回的内容通过LangChain4j封装为响应类
8. 添加配置，保证ai生成内容格式。添加LangChain4j重试机制，防止报错（ai是随机生成的，总有概率报错，这时候使用重试机制兜底）
9. 添加门面ai类，使用ai只用调用ai门面类。简化开发（使用门面设计模式）
10. 编写生成单纯的HTML页面和多文件类型的方法
11. 编写HTML页面和多文件类型的枚举类，方便后续调用
12. 将返回的ai文件对象写入本地磁盘，方便后续查询调用记录，用户查看ai生成的界面