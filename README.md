# trust-freedom定制版Spring Cloud Netflix Zuul，主要新增功能：动态添加/删除Filter

> Spring Boot版本： 1.5.18.RELEASE
>
> Spring Cloud版本： Dalston.SR5



## Quick Start

### 启动前修改配置

**1、启动Eureka**

此项目需要连接到Eureka，请提前启动单机版Eureka

<br>

**2、建表**

使用 `resources/db/schema.sql` 建表

<br>

**3、修改配置文件**

修改 [application.properties](https://github.com/trust-freedom/trust-freedom-spring-cloud-netflix-zuul/blob/master/trust-freedom-spring-cloud-netflix-zuul/src/main/resources/application.properties) 中关于 **Filter存放路径** 和 **数据库连接** 的配置，如

```properties
# 将“绝对路径”修改为本地磁盘路径
# 也可以将以下path完整替换为本地的某个绝对路径，此路径是用于存储本地已加载Filter的
zuul.filter.pre.path = 绝对路径/trust-freedom-spring-cloud-netflix-zuul/src/main/groovy/filters/pre
zuul.filter.route.path = 绝对路径/trust-freedom-spring-cloud-netflix-zuul/src/main/groovy/filters/route
zuul.filter.post.path = 绝对路径/trust-freedom-spring-cloud-netflix-zuul/src/main/groovy/filters/post
zuul.filter.error.path = 绝对路径/trust-freedom-spring-cloud-netflix-zuul/src/main/groovy/filters/error
## zuul.filter.custom.path =


# 填写自己的数据库连接信息
# 建表语句在 resources/db/schema.sql
zuul.data-source.class-name = com.mysql.jdbc.Driver
zuul.data-source.url = mysql数据库url
zuul.data-source.user = 数据库user
zuul.data-source.password = 数据库password
zuul.data-source.min-pool-size = 10
zuul.data-source.max-pool-size = 20
```

<br>

### 启动Application

使用`com.freedom.springcloud.zuul.TrustFreeDomZuulApplication`启动

<br>

### 访问管理页面

正常启动后，访问： http://localhost:9041/admin/filterLoader.jsp

![管理界面](images/Snipaste_2019-02-01_16-49-37.jpg)

<br>

### 测试Groovy脚本

在 [test-scripts](https://github.com/trust-freedom/trust-freedom-spring-cloud-netflix-zuul/tree/master/trust-freedom-spring-cloud-netflix-zuul/test-scripts)目录下有几个前缀过滤器Pre的测试脚本，可以直接上传

