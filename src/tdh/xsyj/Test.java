package tdh.xsyj;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import tdh.frame.web.context.WebAppContext;
import tdh.web.WebContext;

public class Test {
	
	private static String clearMessyCode(String inputsrt) {
		Pattern p = Pattern
				.compile("[^\u4e00-\u9fa5A-Za-z0-9\\-、，；。：？,;.:/\\s\\n]");
		Matcher m = p.matcher(inputsrt);
		String deststr = m.replaceAll("");
		return deststr;
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		SchemaFactory schemaFactory = SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema");
		String fileName = "D:/temp/spdtAndAjList_all_15.xsd";//"9902_实时收结.xsd";
		File schemaFile = new File(fileName);
		if(schemaFile.exists()){
			Schema schema = null;
			try {
				schema = schemaFactory.newSchema(schemaFile);
			} catch (SAXException e) {
				e.printStackTrace();
			}
			if(schema!=null){
				WebContext.validator15 = schema.newValidator();
			}
		}
		try {
			if(WebContext.validator15 == null){
				System.out.println("xsd 启动对象实体化不正确");
			}
			synchronized(WebContext.validator15){
	            BufferedReader bufferedReader = new BufferedReader(new FileReader(new File("D:/temp/5分钟数据_数据.xml")));
	            StringBuilder stringBuilder = new StringBuilder();
	            String content;
	            while((content = bufferedReader.readLine() )!=null){
	                stringBuilder.append(content);
	            }
	            String data=stringBuilder.toString();
				Source src = new StreamSource(new ByteArrayInputStream(data.getBytes("UTF-8")));
				WebContext.validator15.validate(src);
				System.out.println("success");
			}
		} catch (SAXException e) {
			String errmsg = "";
			if(e instanceof org.xml.sax.SAXParseException){
				SAXParseException ex = (org.xml.sax.SAXParseException)e;
				errmsg = "Xml的第"+ex.getLineNumber()+"行，第"+ex.getColumnNumber()+"列，"+ex.getMessage();
			}else{
				errmsg = e.getMessage();
			}
			errmsg = clearMessyCode(errmsg);
			System.out.println("ERROR格式错误："+errmsg);
		} catch (IOException e) {
			System.out.println("ERROR"+e.getMessage());
		}

	}

}
