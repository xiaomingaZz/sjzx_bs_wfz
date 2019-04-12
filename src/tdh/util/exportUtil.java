package tdh.util;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.FileUtils;


public class exportUtil {
	
	/**
     * 打包path文件下所有格式为xml的文件到fileName文件 并删除xml文件
     * @param name zip压缩包文件名
     * @param url  zip压缩包文件所在路径
     */
    public void fileToZip(String zipName,String path){
    	//ZipUtil util = new ZipUtil(path+"//"+zipName);
    	//util.compress(path);
        ZipOutputStream out=null;
        FileInputStream fis=null;
        DataInputStream in=null;
        File file = new File(path);
        File[] files = file.listFiles();
        try{
          out = new ZipOutputStream(new FileOutputStream(path+"//"+zipName));
          for(int i=0;i<files.length;i++) {
        	  String fileName=files[i].getName();
        	  String gs = fileName.substring(fileName.lastIndexOf(".")+1);
        	  if("xml".equals(gs)||"XML".equals(gs)) {
	              fis = new FileInputStream(files[i]);
	              in = new DataInputStream(fis);
	              out.putNextEntry(new ZipEntry(files[i].getName()));
	              /*
	              int c;
	              while((c=in.read())!=-1) {
	                out.write(c);
	              }
	              */
	              
	             // int count;   
	              byte data[] = new byte[256]; 
	              while(in.available()>256){
	            	  in.read(data);
	            	  out.write(data);
	              }
	              
	              byte relast[] = new byte[in.available()];
	              in.read(relast);
            	  out.write(relast);
	              /*
	              while ((count = in.read(data, 0, 256)) != -1) {   
	                  out.write(data, 0, count);   
	              } 
	              */
	              in.close();
	              fis.close();
	              in = null;
	              fis = null;
	              files[i].delete();
        	  }  
          }
        }catch(Exception e){
            e.printStackTrace();
        }finally{
            try {
                if (out != null)
                out.closeEntry();
                closeStream(in);
                closeStream(fis);
                closeStream(out);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    /**
	 * 删除path下xml格式文件
	 * @param path
	 */
	public  void initPathFile(String path){
	        File file = new File(path);
	        if (!file.getParentFile().exists()){
				file.getParentFile().mkdirs();
			}
	        File[] files = file.listFiles();
	        if(files==null){
	        	return;
	        }
	        try{
	          for(int i=0;i<files.length;i++){
	        	  String fileName=files[i].getName();
	        	  String gs = fileName.substring(fileName.lastIndexOf(".")+1);
	        	  if("xml".equals(gs)||"XML".equals(gs)) {
	        		  files[i].delete();
	        	  }
	          }
	        }catch(Exception e){
	            e.printStackTrace();
	        }
	}
	
	/**
	 * 将xmlNR字符串写入path下xmlName文件中
	 * @param path
	 * @param xmlName
	 * @param xmlNr
	 */
	public void writeToFile(String path,String xmlName,String xmlNr){
		try {
			String filePath =path;
			File file = new File(filePath + "//" + xmlName);
			if (file.exists()) {
				file.delete();
			}
			if (!file.getParentFile().exists()) {
				file.getParentFile().mkdirs();
			}
			FileUtils.writeStringToFile(file, xmlNr, "UTF-8");
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private static void closeStream(Object obj) {
		try {
			if (obj != null) {
				if (obj instanceof InputStream) {
					((InputStream) obj).close();
				} else if (obj instanceof OutputStream) {
					((OutputStream) obj).close();
				}
				obj = null;
			}
		} catch (IOException e) {
		}
	}

}
