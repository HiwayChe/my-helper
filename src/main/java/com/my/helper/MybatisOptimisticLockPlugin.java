package com.my.helper;

import java.util.Iterator;
import java.util.List;

import org.mybatis.generator.api.IntrospectedColumn;
import org.mybatis.generator.api.IntrospectedTable;
import org.mybatis.generator.api.PluginAdapter;
import org.mybatis.generator.api.dom.java.Method;
import org.mybatis.generator.api.dom.java.TopLevelClass;
import org.mybatis.generator.api.dom.xml.Element;
import org.mybatis.generator.api.dom.xml.TextElement;
import org.mybatis.generator.api.dom.xml.XmlElement;
import org.mybatis.generator.logging.Log;
import org.mybatis.generator.logging.LogFactory;

public class MybatisOptimisticLockPlugin extends PluginAdapter {
	private Log log = LogFactory.getLog(getClass());

	@Override
	public boolean modelSetterMethodGenerated(Method method, TopLevelClass topLevelClass,
			IntrospectedColumn introspectedColumn, IntrospectedTable introspectedTable, ModelClassType modelClassType) {
		if (isOptimisticLockEnabled(introspectedTable) && method.getName().equals("setVersion")) {
			this.log.debug(introspectedTable.getTableConfiguration().getTableName() + "表开启乐观锁功能，不生成version字段的setter方法");
			return false;
		}
		return super.modelSetterMethodGenerated(method, topLevelClass, introspectedColumn, introspectedTable,
				modelClassType);
	}

	// clear
	@Override
	public boolean sqlMapUpdateByPrimaryKeyWithoutBLOBsElementGenerated(XmlElement element,
			IntrospectedTable introspectedTable) {
		if (isOptimisticLockEnabled(introspectedTable)) {
			this.log.debug(
					introspectedTable.getTableConfiguration().getTableName() + "表开启乐观锁功能，重写<updateByPrimaryKey>");
			List<Element> eleList = element.getElements();
			for (Element ele : eleList) {
				if (ele.getFormattedContent(0).contains("#{version,jdbcType=INTEGER}")) {
					this.modifyFieldWithReflection(ele, "content",
							ele.getFormattedContent(0).replace("#{version,jdbcType=INTEGER}", "version + 1"));
					break;
				}
			}
			element.addElement(new TextElement("and version = #{version,jdbcType=INTEGER}"));
		}

		return super.sqlMapUpdateByPrimaryKeyWithoutBLOBsElementGenerated(element, introspectedTable);
	}

	// clear
	@Override
	public boolean sqlMapUpdateByPrimaryKeySelectiveElementGenerated(XmlElement element,
			IntrospectedTable introspectedTable) {
		if (isOptimisticLockEnabled(introspectedTable)) {
			this.log.debug(introspectedTable.getTableConfiguration().getTableName()
					+ "表开启乐观锁功能，重写<updateByPrimaryKeySelective>");
			List<Element> eleList = ((XmlElement) element.getElements().get(1)).getElements();// 第1个子元素是<set>标签
			for (int i = 0; i < eleList.size(); i++) {
				Element ele = eleList.get(i);
				if (ele.getFormattedContent(0).contains("#{version,jdbcType=INTEGER}")) {
					eleList.remove(i);
					eleList.add(i, new TextElement("  version = version + 1,"));
					break;
				}
			}
			element.addElement(new TextElement("and version = #{version,jdbcType=INTEGER}"));
		}

		return super.sqlMapUpdateByPrimaryKeySelectiveElementGenerated(element, introspectedTable);
	}

	// clear
	@Override
	public boolean sqlMapInsertElementGenerated(XmlElement element, IntrospectedTable introspectedTable) {
		if (isOptimisticLockEnabled(introspectedTable)) {
			this.log.debug(introspectedTable.getTableConfiguration().getTableName() + "表开启乐观锁功能，重写<insert>");
			List<Element> eleList = element.getElements();
			Iterator<Element> eleIt = eleList.iterator();
			while (eleIt.hasNext()) {
				Element ele = eleIt.next();
				if (ele.getFormattedContent(0).contains(", #{version,jdbcType=INTEGER}")
						|| ele.getFormattedContent(0).contains("#{version,jdbcType=INTEGER},")) {
					this.modifyFieldWithReflection(ele, "content", ele.getFormattedContent(0)
							.replace(", #{version,jdbcType=INTEGER}", "").replace("#{version,jdbcType=INTEGER},", ""));
				}
				if (ele.getFormattedContent(0).contains(", version")
						|| ele.getFormattedContent(0).contains("version,")) {
					this.modifyFieldWithReflection(ele, "content",
							ele.getFormattedContent(0).replace(", version", "").replace("version,", ""));
				}
			}
		}

		return super.sqlMapInsertElementGenerated(element, introspectedTable);
	}

	// clear
	@Override
	public boolean sqlMapInsertSelectiveElementGenerated(XmlElement element, IntrospectedTable introspectedTable) {
		if (isOptimisticLockEnabled(introspectedTable)) {
			this.log.debug(introspectedTable.getTableConfiguration().getTableName() + "表开启乐观锁功能，重写<insertSelective>");
			List<Element> eleList = ((XmlElement) element.getElements().get(2)).getElements();// 如果没有设置generatedKey，这里是1
			for (int i = 0; i < eleList.size(); i++) {
				Element ele = eleList.get(i);
				if (ele.getFormattedContent(0).contains("version,")) {
					eleList.remove(i);
					break;
				}
			}
			eleList = ((XmlElement) element.getElements().get(3)).getElements();//// 如果没有设置generatedKey，这里是2
			for (int i = 0; i < eleList.size(); i++) {
				Element ele = eleList.get(i);
				if (ele.getFormattedContent(0).contains("#{version,jdbcType=INTEGER}")) {
					eleList.remove(i);
					break;
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

	private boolean isOptimisticLockEnabled(IntrospectedTable introspectedTable) {
		boolean enableOptimisticLock = "true"
				.equalsIgnoreCase(introspectedTable.getTableConfigurationProperty("enableOptimisticLock"));
		if (enableOptimisticLock) {
			IntrospectedColumn versionColumn = introspectedTable.getColumn("version");
			if (versionColumn != null && versionColumn.getJdbcType() == 4) {// 4是Integer类型，参考org.apache.ibatis.type.JdbcType
				enableOptimisticLock = true;
			} else {
				enableOptimisticLock = false;
			}
		}
		return enableOptimisticLock;
	}
}
