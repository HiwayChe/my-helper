package com.my.helper;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.mybatis.generator.api.IntrospectedColumn;
import org.mybatis.generator.api.IntrospectedTable;
import org.mybatis.generator.api.PluginAdapter;
import org.mybatis.generator.api.dom.java.Method;
import org.mybatis.generator.api.dom.java.TopLevelClass;
import org.mybatis.generator.api.dom.xml.Element;
import org.mybatis.generator.api.dom.xml.XmlElement;
import org.mybatis.generator.logging.Log;
import org.mybatis.generator.logging.LogFactory;

public class MybatisNotPassValueInJavaPlugin extends PluginAdapter {
	private Log log = LogFactory.getLog(getClass());
	// key:table name, value:list<column name>
	private Map<String, List<String>> tableColumnMap = new HashMap<String, List<String>>();

	@Override
	public boolean modelSetterMethodGenerated(Method method, TopLevelClass topLevelClass,
			IntrospectedColumn introspectedColumn, IntrospectedTable introspectedTable, ModelClassType modelClassType) {
		List<String> columns = this.getNotPassValueColumns(introspectedTable);
		for (String column : columns) {
			if(method.getName().startsWith("set") && introspectedColumn.getActualColumnName().equals(column)) {
				this.log.debug(introspectedTable.getTableConfiguration().getTableName()+"."+introspectedColumn.getActualColumnName()
				+"字段不允许在Java中传值，不生成set方法");
				return false;
			}
		}
		return super.modelSetterMethodGenerated(method, topLevelClass, introspectedColumn, introspectedTable,
				modelClassType);
	}

	// clear
	@Override
	public boolean sqlMapUpdateByPrimaryKeyWithoutBLOBsElementGenerated(XmlElement element,
			IntrospectedTable introspectedTable) {
		List<String> columns = this.getNotPassValueColumns(introspectedTable);
		for (String column : columns) {
			String camel = this.toCamel(column);
			Iterator<Element> it = element.getElements().iterator();
			while (it.hasNext()) {
				Element ele = it.next();
				String content = ele.getFormattedContent(0);
				if (content.contains("#{" + camel + ",jdbcType=")) {
					content = content.replaceFirst(", #\\{" + camel + ",jdbcType=[A-Z]+\\}", "")
							.replaceFirst("#\\{" + camel + ",jdbcType=[A-Z]+\\},", "");
					it.remove();
					this.log.debug(introspectedTable.getTableConfiguration().getTableName()+"表设置了不从Java中传值，改写<updateByPrimaryKey>");
				}
			}
		}
		return super.sqlMapUpdateByPrimaryKeyWithoutBLOBsElementGenerated(element, introspectedTable);
	}

	// clear
	@Override
	public boolean sqlMapUpdateByPrimaryKeySelectiveElementGenerated(XmlElement element,
			IntrospectedTable introspectedTable) {
		List<String> columns = this.getNotPassValueColumns(introspectedTable);
		for (String column : columns) {
//			String camel = this.toCamel(column);
			Iterator<Element> it = ((XmlElement)element.getElements().get(1)).getElements().iterator();
			while (it.hasNext()) {
				Element ele = it.next();
				String content = ele.getFormattedContent(0);
				if (content.contains(column)) {
					this.log.debug(introspectedTable.getTableConfiguration().getTableName()+"表设置了不从Java中传值，改写<updateByPrimaryKeySelective>");
					it.remove();
				}
			}
		}
		return super.sqlMapUpdateByPrimaryKeySelectiveElementGenerated(element, introspectedTable);
	}

	// clear
	@Override
	public boolean sqlMapInsertElementGenerated(XmlElement element, IntrospectedTable introspectedTable) {
		List<String> columns = this.getNotPassValueColumns(introspectedTable);
		for (String column : columns) {
			String camel = this.toCamel(column);
			Iterator<Element> it = element.getElements().iterator();
			while (it.hasNext()) {
				Element ele = it.next();
				String content = ele.getFormattedContent(0);
				if (content.contains("#{" + camel + ",jdbcType=")) {
					content = content.replaceFirst(", #\\{" + camel + ",jdbcType=[A-Z]+\\}", "")
							.replaceFirst("#\\{" + camel + ",jdbcType=[A-Z]+\\},", "");
					this.modifyFieldWithReflection(ele, "content", content);
					this.log.debug(introspectedTable.getTableConfiguration().getTableName()+"表设置了不从Java中传值，改写<insert>");
					content = ele.getFormattedContent(0);
				}
				if (content.contains(", " + column) || content.contains(column + ",")) {
					this.modifyFieldWithReflection(ele, "content",
							content.replace(", " + column, "").replace(column + ",", ""));
				}
			}
		}
		return super.sqlMapInsertElementGenerated(element, introspectedTable);
	}

	// clear
	@Override
	public boolean sqlMapInsertSelectiveElementGenerated(XmlElement element, IntrospectedTable introspectedTable) {
		List<String> columns = this.getNotPassValueColumns(introspectedTable);
		for (String column : columns) {
			String camel = this.toCamel(column);
			Iterator<Element> it = ((XmlElement)element.getElements().get(2)).getElements().iterator();
			while (it.hasNext()) {
				Element ele = it.next();
				String content = ele.getFormattedContent(0);
				if (content.contains(column + ",")) {
					this.log.debug(introspectedTable.getTableConfiguration().getTableName()+"表设置了不从Java中传值，改写<insertSelective>");
					it.remove();
				}
			}
			
			Iterator<Element> iterator = ((XmlElement)element.getElements().get(3)).getElements().iterator();
			while (iterator.hasNext()) {
				Element ele = iterator.next();
				String content = ele.getFormattedContent(0);
				if (content.contains("#{" + camel + ",jdbcType=")) {
					iterator.remove();
				}
			}
		}
		return super.sqlMapInsertSelectiveElementGenerated(element, introspectedTable);
	}

	@Override
	public boolean validate(List<String> warnings) {
		return true;
	}

	private void modifyFieldWithReflection(Element ele, String fieldName, String newValue) {
		try {
			java.lang.reflect.Field field = ele.getClass().getDeclaredField(fieldName);
			field.setAccessible(true);
			field.set(ele, newValue);
		} catch (Exception e) {
			this.log.error("反射修改字段值失败，原因：" + e.getMessage(), e);
		}
	}

	private List<String> getNotPassValueColumns(IntrospectedTable introspectedTable) {
		String table = introspectedTable.getTableConfiguration().getTableName();
		if (this.tableColumnMap.get(table) == null) {
			String columns = introspectedTable.getTableConfigurationProperty("columnsNotPassValueInJava");
			if (columns != null && !"".equals(columns)) {
				List<String> columnList = Arrays.asList(columns.split(","));
				this.tableColumnMap.put(table, columnList);
			} else {
				this.tableColumnMap.put(table, Collections.emptyList());
			}
		}
		return this.tableColumnMap.get(table);
	}

	/**
	 * 数据库字段转换成java属性名，如create_time转成createTime：_删除，_之后的第一个字母大写
	 * 
	 * @param column
	 * @return
	 */
	private String toCamel(String column) {
		if (!column.contains("_")) {
			return column;
		}
		char[] oldchars = column.toCharArray();
		char[] newchars = new char[oldchars.length];
		int index = 0;
		boolean capitalized = false;
		for (int i = 0; i < oldchars.length; i++) {
			if (oldchars[i] == '_') {
				if (index == 0) {
					continue;
				} else {
					capitalized = true;
					continue;
				}
			} else {
				newchars[index++] = capitalized ? Character.toUpperCase(oldchars[i]) : oldchars[i];
				capitalized=false;
			}
		}
		return new String(newchars, 0, index);
	}
}
