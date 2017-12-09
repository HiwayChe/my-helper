package com.my.helper;

import java.util.List;

import org.mybatis.generator.api.CommentGenerator;
import org.mybatis.generator.api.IntrospectedTable;
import org.mybatis.generator.api.PluginAdapter;
import org.mybatis.generator.api.dom.java.Field;
import org.mybatis.generator.api.dom.java.FullyQualifiedJavaType;
import org.mybatis.generator.api.dom.java.JavaVisibility;
import org.mybatis.generator.api.dom.java.Method;
import org.mybatis.generator.api.dom.java.Parameter;
import org.mybatis.generator.api.dom.java.TopLevelClass;
import org.mybatis.generator.api.dom.xml.Attribute;
import org.mybatis.generator.api.dom.xml.TextElement;
import org.mybatis.generator.api.dom.xml.XmlElement;
import org.mybatis.generator.logging.Log;
import org.mybatis.generator.logging.LogFactory;

public class MybatisPaginationPlugin extends PluginAdapter {

	private final int max_offset = 1000;// 在分页的时候最大允许offst 1000条，再大的话请走搜索
	private final int max_rows = 200;// 在分页的时候每页最多取200条

	private Log log = LogFactory.getLog(getClass());

	@Override
	public boolean modelExampleClassGenerated(TopLevelClass topLevelClass, IntrospectedTable introspectedTable) {
		addLimit(topLevelClass, introspectedTable, "offset");
		addLimit(topLevelClass, introspectedTable, "rows");
		return super.modelExampleClassGenerated(topLevelClass, introspectedTable);
	}

	@Override
	public boolean sqlMapSelectByExampleWithoutBLOBsElementGenerated(XmlElement element,
			IntrospectedTable introspectedTable) {
		XmlElement isNotNullElement = new XmlElement("if");
		isNotNullElement.addAttribute(new Attribute("test", "offset >= 0"));
		isNotNullElement.addElement(new TextElement(" limit ${offset} , ${rows}"));
		element.addElement(isNotNullElement);
		return super.sqlMapUpdateByExampleWithoutBLOBsElementGenerated(element, introspectedTable);
	}

	private void addLimit(TopLevelClass topLevelClass, IntrospectedTable introspectedTable, String name) {
		CommentGenerator commentGenerator = context.getCommentGenerator();
		Field field = new Field();
		field.setVisibility(JavaVisibility.PROTECTED);
		field.setType(FullyQualifiedJavaType.getIntInstance());
		field.setName(name);
		field.setInitializationString("-1");
		commentGenerator.addFieldComment(field, introspectedTable);
		topLevelClass.addField(field);
		char c = name.charAt(0);
		String camel = Character.toUpperCase(c) + name.substring(1);
		Method method = new Method();
		method.setVisibility(JavaVisibility.PUBLIC);
		method.setName("set" + camel);
		method.addParameter(new Parameter(FullyQualifiedJavaType.getIntInstance(), name));
		// method.addBodyLine("this." + name + "=" + name + ";");
		StringBuilder sb = new StringBuilder(200);
		if ("offset".equals(name)) {
			sb.append("if(").append(name).append(" > ").append(this.max_offset).append("){")
					.append(System.lineSeparator())
					.append("\t\t\tthrow new UnsupportedOperationException(\"offset is too big, must less than ")
					.append(this.max_offset).append("\");").append(System.lineSeparator()).append("\t\t}")
					.append(System.lineSeparator()).append("\t\tthis.").append(name).append("=").append(name)
					.append(";");
			this.log.debug("增加分页功能，offset最大值限定为" + this.max_offset);
		} else if ("rows".equals(name)) {
			sb.append("if(").append(name).append(" > ").append(this.max_rows).append("){")
					.append(System.lineSeparator())
					.append("\t\t\tthrow new UnsupportedOperationException(\"too many rows to query, must less than ")
					.append(this.max_rows).append("\");").append(System.lineSeparator()).append("\t\t}")
					.append(System.lineSeparator()).append("\t\tthis.").append(name).append("=").append(name)
					.append(";");
			this.log.debug("增加分页功能，每次查询最多限定为" + this.max_rows);
		}
		method.addBodyLine(sb.toString());
		commentGenerator.addGeneralMethodComment(method, introspectedTable);
		topLevelClass.addMethod(method);
		method = new Method();
		method.setVisibility(JavaVisibility.PUBLIC);
		method.setReturnType(FullyQualifiedJavaType.getIntInstance());
		method.setName("get" + camel);
		method.addBodyLine("return " + name + ";");
		commentGenerator.addGeneralMethodComment(method, introspectedTable);
		topLevelClass.addMethod(method);
	}

	@Override
	public boolean validate(List<String> warnings) {
		return true;
	}

}
