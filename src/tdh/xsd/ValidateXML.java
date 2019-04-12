package tdh.xsd;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.dom4j.Document;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import tdh.web.WebContext;


/**
 * 文件校验xsd jdk版本要求1.6及以上版本
 * @author 施健伟
 *
 */
public class ValidateXML {
	
	public static String validateXMLByXSD(String data) {
		try {
			SchemaFactory sf = SchemaFactory
					.newInstance("http://www.w3.org/2001/XMLSchema");
			sf.setFeature(
					"http://javax.xml.XMLConstants/feature/secure-processing",
					false);
			File schemaFile = new File("E:/doc/xsd/XSYJ.xsd");
			Schema schema = sf.newSchema(schemaFile);
			Validator val = schema.newValidator();
			Source src = new StreamSource(new ByteArrayInputStream(data
					.getBytes("UTF-8")));
			val.validate(src);
			return "success";
		} catch (Exception e) {
			e.printStackTrace();
			return e.toString();
		}
	}
	
	public static String validateXMLData15(String data){
		try {
			if(WebContext.validator15 == null){
				return "xsd 启动对象实体化不正确";
			}
			synchronized(WebContext.validator15){
				Source src = new StreamSource(new ByteArrayInputStream(data.getBytes("UTF-8")));
				WebContext.validator15.validate(src);
				return "success";
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
			return "ERROR格式错误："+errmsg;
		} catch (IOException e) {
			return "ERROR"+e.getMessage();
		}
	}
	private static String clearMessyCode(String inputsrt) {
		Pattern p = Pattern
				.compile("[^\u4e00-\u9fa5A-Za-z0-9\\-、，；。：？,;.:/\\s\\n]");
		Matcher m = p.matcher(inputsrt);
		String deststr = m.replaceAll("");
		return deststr;
	}
	
	public synchronized static String format(Document doc){
        OutputFormat formater = OutputFormat.createPrettyPrint();
        formater.setEncoding("UTF-8");
        StringWriter out = new StringWriter();
        XMLWriter writer=null;
		try {
			writer = new XMLWriter(out, formater);
			writer.write(doc);
			return out.toString();
		} catch (Exception e) {
			e.printStackTrace();
		}finally{
			if(out!=null){
				try {
					out.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
        return "";
	}
}
