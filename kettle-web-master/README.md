#kettle-master
#####这是一个对客户端版的kettle创建的job和transformation进行统一管理的工具（支持kettle7.0.0.0.25版本）

----------

####简单说明

> * 目前已经支持文件类型转换和任务的保存和添加，保存位置在配置文件中进行配置。
* 因为kettle支持的转换类型非常的多。目前我只是测试了MySQL数据的导入和导出以及MySQL-Excel数据之间的转换，如果添加的转换比较复杂是一定会出问题的。这个目前来说也没有什么优雅的解决办法，正在逐步完善。
* 现在本系统，作为kettle的简单调度平台。还缺少很多的模型模板作为提供，这也是在未来要进行完善的模块。

----------

####使用工具（这里我觉得直接看pom文件比较好）
``` xml
<java-version>1.8</java-version>
<junit-version>4.12</junit-version>
<project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
<project.reporting.outputEncoding>UTF-8</project.reporting.outputEncoding>
<org.springframework-version>4.3.1.RELEASE</org.springframework-version>
<beetlsql-version>2.8.11</beetlsql-version>
<mysql-connector-java-version>5.1.38</mysql-connector-java-version>
<druid-version>1.0.29</druid-version>
<commons-configuration.version>1.10</commons-configuration.version>
<commons.fileupload-version>1.3.2</commons.fileupload-version>
<commons-beanutils-core-version>1.8.3</commons-beanutils-core-version>
<commons-codec-version>1.10</commons-codec-version>
<json-lib-version>2.4</json-lib-version>
<javax.mail-version>1.5.6</javax.mail-version>
<jackson.databind-version>2.8.4</jackson.databind-version>
<kettle-version>7.0.0.0-25</kettle-version>
<quartz-version>2.2.1</quartz-version>
<commons-dbutils-version>1.6</commons-dbutils-version>
```
####主要是spring+mvc+beetlsql的整合使用，这里必须多说一句，beetlsql的确是非常的好用

----------

####项目启动

> 1. 下载项目
2. 导入eclipse
3. 等待maven下载完包
4. 等
5. 等
6. 一直等到项目不报错了
7. 导入数据库文件
8. 配置数据库连接（resource目录下面）
9. 扔到Tomcat里面进行启动

----------

####项目二次开发

> 项目模块划分挺清楚的，想开发那个模块就直接写吧。如果有看不懂的加我QQ详聊2029403224

----------

####截图
![资源库添加](https://git.oschina.net/uploads/images/2017/0604/092444_b758f1fd_673473.png "在这里输入图片标题")

![转换管理](https://git.oschina.net/uploads/images/2017/0604/092537_3dacaf57_673473.png "在这里输入图片标题")

![添加作业](https://git.oschina.net/uploads/images/2017/0604/092605_0ce30ec3_673473.png "在这里输入图片标题")

![运行日志列表](https://git.oschina.net/uploads/images/2017/0604/092628_642831f1_673473.png "在这里输入图片标题")

![查看运行日志](https://git.oschina.net/uploads/images/2017/0604/092645_7c09c4ed_673473.png "在这里输入图片标题")

![作业和转换的监控](https://git.oschina.net/uploads/images/2017/0609/133217_28f21aa2_673473.png "在这里输入图片标题")

----------

####鸣谢

[项目一些代码，参考了这位大神开发的管理平台。万分感谢，我只是做了个升级的工作（从4.4支持到了7.0）][1]


  [1]: https://github.com/uKettle/kettle