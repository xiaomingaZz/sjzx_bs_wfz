package tdh.xsd;

import java.io.File;  
import java.io.FileWriter;
import java.io.IOException;  
import java.io.InputStream;  
import java.sql.Connection;
import java.util.Calendar;
import java.util.Date;
import java.util.List;  
  
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;  
import javax.xml.parsers.DocumentBuilderFactory;  
import javax.xml.parsers.ParserConfigurationException;  
import javax.xml.transform.Source;  
import javax.xml.transform.dom.DOMSource;  
import javax.xml.transform.stream.StreamSource;  
import javax.xml.validation.Schema;  
import javax.xml.validation.SchemaFactory;  
import javax.xml.validation.Validator;  
  
import org.apache.log4j.Logger;  
import org.xml.sax.SAXException;  

public final class XMLParser {  
  
    private static final Logger log = Logger.getLogger(XMLParser.class);  
      
    private XMLParser() {}  
      
    public static boolean validateWithSingleSchema(File xml, File xsd) {  
        boolean legal = false;  
          
        try {  
            SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);  
            Schema schema = sf.newSchema(xsd);  
              
            Validator validator = schema.newValidator();  
            validator.validate(new StreamSource(xml));  
              
            legal = true;  
        } catch (Exception e) {  
            legal = false;  
            log.error(e.getMessage());  
        }  
          
        return legal;  
    }  
      
    public static boolean validateWithMultiSchemas(InputStream xml, List<File> schemas) {  
        boolean legal = false;  
          
        try {  
            Schema schema = createSchema(schemas);  
              
            Validator validator = schema.newValidator();  
            validator.validate(new StreamSource(xml));  
              
            legal = true;  
        } catch(Exception e) {  
           e.printStackTrace();  
        }  
          
        return legal;  
    }  
      
    /** 
     * Create Schema object from the schemas file. 
     *  
     * @param schemas 
     * @return 
     * @throws ParserConfigurationException 
     * @throws SAXException 
     * @throws IOException 
     */  
    private static Schema createSchema(List<File> schemas) throws ParserConfigurationException, SAXException, IOException {  
        SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);  
        SchemaResourceResolver resourceResolver = new SchemaResourceResolver();  
        sf.setResourceResolver(resourceResolver);  
          
        Source[] sources = new Source[schemas.size()];  
          
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();  
        docFactory.setValidating(false);  
        docFactory.setNamespaceAware(true);  
        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();  
          
        for(int i = 0; i < schemas.size(); i ++) {  
            org.w3c.dom.Document doc = docBuilder.parse(schemas.get(i));  
            DOMSource stream = new DOMSource(doc, schemas.get(i).getAbsolutePath());  
            sources[i] = stream;  
        }  
          
        return sf.newSchema(sources);  
    }  
    
    
    /**  
     * 追加文件：使用FileWriter  
     *   
     * @param fileName  
     * @param content  
     */  
    public static void appendFile(String fileName, String content) { 
    	FileWriter writer = null;
        try {   
            // 打开一个写文件器，构造函数中的第二个参数true表示以追加形式写文件   
            writer = new FileWriter(fileName, true);   
            writer.write(content);     
        } catch (IOException e) {   
            e.printStackTrace();   
        } finally {   
            try {   
            	if(writer != null){
            		writer.close();   
            	}
            } catch (IOException e) {   
                e.printStackTrace();   
            }   
        } 
    } 
    
    
      
}  