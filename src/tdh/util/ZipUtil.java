/**
 *
 * @author Administrator
 * @date 2015年9月6日
 */
package tdh.util;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.zip.CRC32;
import java.util.zip.CheckedOutputStream;

import org.apache.tools.zip.ZipEntry;
import org.apache.tools.zip.ZipOutputStream;
/**
 *
 * @author chenjx
 * @version 创建时间：2015年9月6日  上午10:48:22
 */
public class ZipUtil {
    static final int BUFFER = 8192;
  
    private File zipFile;   
    
    /*
     * 指定压缩包名称
     */
    public ZipUtil(String pathName) {   
        zipFile = new File(pathName);   
    } 
  
    public void compress(String srcPathName) {   
        File file = new File(srcPathName);   
        if (!file.exists())   
            throw new RuntimeException(srcPathName + "不存在！");   
        try {   
            FileOutputStream fileOutputStream = new FileOutputStream(zipFile);   
            CheckedOutputStream cos = new CheckedOutputStream(fileOutputStream,   
                    new CRC32());   
            ZipOutputStream out = new ZipOutputStream(cos);   

        	if (!file.exists()){
        		out.close();   
        		return;   
        	}
            File[] files = file.listFiles();   
            for (int i = 0; i < files.length; i++) {   
            	if (files[i].isDirectory()) {    
                    //System.out.println("压缩xml文件夹时遇到一个目录而不是xml文件...");  
                } else {  
                	compressFile(files[i], out,"/");
                }    	
            }
            out.close();   
        } catch (Exception e) { 
            throw new RuntimeException(e);   
        }   
    }   

    /**
     * 压缩一个文件（仅xml格式） 
     */
    private void compressFile(File file, ZipOutputStream out, String basedir) {   
        if (!file.exists()) {   
            return;   
        }
        String fileName = file.getName();
        String gs = fileName .substring(fileName.lastIndexOf(".")+1);
        if("xml".equals(gs)||"XML".equals(gs)) {
        	try {   
                BufferedInputStream bis = new BufferedInputStream(   
                        new FileInputStream(file));   
                ZipEntry entry = new ZipEntry(basedir + file.getName());//为压缩包内文件目录结构     
                out.putNextEntry(entry);   
                int count;   
                byte data[] = new byte[BUFFER];   
                while ((count = bis.read(data, 0, BUFFER)) != -1) {   
                    out.write(data, 0, count);   
                }   
                bis.close(); 
                file.delete();
            } catch (Exception e) {   
                throw new RuntimeException(e);   
            }  
        }      
    }   
}