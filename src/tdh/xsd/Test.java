package tdh.xsd;
import java.io.*;  
import java.sql.Connection;
import java.util.*;  
  
import org.xml.sax.SAXException;  


public class Test{
public static void main(String[] args) throws SAXException, FileNotFoundException {  
	    
		//加载XML文件
	    InputStream xml = null;  
        String dir = "E:/doc/xml";
	    
	    File dirFile = new File(dir);
        //如果dir对应的文件不存在，或者不是一个目录，则退出
        if (!dirFile.exists() || !dirFile.isDirectory()) {
            return;
        }
        
        File[] files = dirFile.listFiles();
        
	    List<File> schemas = new ArrayList<File>();  
        for (int i = 0; i < files.length; i++) {
	        if (files[i].isFile()) {
	        	schemas.clear();
	    	    String fileName = files[i].getName();
	    	    xml = new FileInputStream(files[i]);
	    	    if(files[i].getName().contains("LB")){
	    	    	 schemas.add(new File("E:/doc/xsd/SJC_LB.xsd"));
	    	    }else if(files[i].getName().contains("11")){
	    	    	 schemas.add(new File("E:/doc/xsd/XSYJ.xsd"));
	    	    }else if(files[i].getName().contains("TJ")){
	    	    	 schemas.add(new File("E:/doc/xsd/SJC_TJ.xsd"));
	    	    }
    		   
    		    //schemas.add(new File("E:/doc/xsd/_A_类型_基础.xsd"));
    		    System.out.println(fileName+XMLParser.validateWithMultiSchemas(xml, schemas)); 
	        }
        }
    } 

}
