# my-helper
helper project
include Helper class and Mybatis-generator plugin.
插件：

1. 分页，限制offset和rows的大小
2. 自动生成支持乐观锁(version字段)的mapper.xml，pojo中不生成对应的setVersion方法
3. 不生成数据库有默认值的字段，如create\_time和update\_time字段，这些字段不需要从java中传入

----
generateConfig.xml中配置加入：

```xml
	<plugin type="com.my.helper.MybatisPaginationPlugin" ></plugin>
	<plugin type="com.my.helper.MybatisOptimisticLockPlugin" ></plugin>
	<plugin type="com.my.helper.MybatisNotPassValueInJavaPlugin"></plugin>
    
    
    	<table schema="my" tableName="user" enableCountByExample="true" enableSelectByExample="true"
			enableDeleteByExample="false" enableUpdateByExample="false">
			<!-- 开启乐观锁字段，固定是int类型的version字段（如果是其他字段名或timestamp类型，请自行修改代码实现），每次update时候version加1 -->
			<!-- 如果不设置或设为false，则version当做普通字段处理-->
			<!-- version字段需要中db中设置一个默认值 -->
			<!-- 表中其他字段名不要含有version单词，如previousversion等 -->
			<!-- 效果：生成的POJO中没有setVersion方法；insert的时候没有version字段，依赖db中的默认值；update的时候根据id和version做where条件，version自动加1 -->
			<property name="enableOptimisticLock" value="true"/>
			<!-- 不需要从Java中传值的字段，多个字段之间用逗号分隔 -->
			<property name="columnsNotPassValueInJava" value="create_time,update_time"/>
			<!-- identity=true生成的selectKey的order是after -->
			<generatedKey column="id" sqlStatement="MySql" identity="true"/>
		</table>
