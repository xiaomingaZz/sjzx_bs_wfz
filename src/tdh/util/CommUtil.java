package tdh.util;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.*;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.zip.CRC32;
import java.util.zip.CheckedInputStream;
import java.util.zip.ZipInputStream;


public class CommUtil {
	
	private static Log log = LogFactory.getLog(CommUtil.class);

	public static boolean isNull(Object obj){
		return obj == null ? true :false;
	}
	
	public static boolean isNotNull(Object obj){
		return !isNull(obj);
	}
	
	public static String trim(String str){
		if(isNull(str) || str.length()==0 || str.equals("null")) return ""; 
		return str.trim();
	}
	
	public static boolean isEmpty(String str){
		if("".equals(trim(str))) return true;
		return false;
	}
	
	public static boolean isNotEmpty(String str){
		return !isEmpty(str);
	}
	
	public static int pasreInt(String value){
		if(isEmpty(value)) return 0;
		try{
			return Integer.parseInt(value);
		}catch(Exception e){
			log.error("转换数字失败", e);
			return 0;
		}
	}
	
	public static int pasreIntThrowE(String value) throws Exception{
		if(isEmpty(value)) return 0;
		try{
			return Integer.parseInt(value);
		}catch(Exception e){
			throw e;
		}
	}
	/*
	 * (EAJ_XSFZDX.SL 毒品数量)数据源为浮点型 但目标为整形数据时，进行四舍五入
	 * 暂时这样处理
	 * 调用前因确认为浮点型
	 */
	public static String getN(String value){
		if(isEmpty(value)) return "";
		float f =Float.parseFloat(value);
		DecimalFormat decimal = new DecimalFormat("#");
		String str= decimal.format(f);
		return str;
	}
	
	/**
	 * 测试传入字符串是否合法的数字，true/false.
	 * 
	 * @param cond .
	 * @return true or false
	 */
	public static boolean isNumber(String cond) {
		try {
			Float.parseFloat(cond);
			return true;
		} catch (Exception e) {
			return false;
		}
	}
	
	
	/** 
     * 使用字符串的matches方法 
     * @param s 
     * @return 
     */  
    public static boolean isNum(String s){  
          
        String regex = "^[1-9][0-9]*\\.[0-9]+$|^[1-9][0-9]*$|^0+\\.[0-9]+$";  
        char c = s.charAt(0);  
        boolean bool;  
        if(c=='+'|c=='-'){  
            bool = s.substring(1).matches(regex);  
        }else{  
            bool = s.matches(regex);  
        }  
        return bool;
    }  
	
	public static String bSubstring(String s, int length) throws Exception {
		byte[] bytes = s.getBytes("Unicode");
		int n = 0; // 表示当前的字节数
		int i = 2; // 要截取的字节数，从第3个字节开始
		for (; i < bytes.length && n < length; i++) {
			// 奇数位置，如3、5、7等，为UCS2编码中两个字节的第二个字节
			if (i % 2 == 1) {
				n++; // 在UCS2第二个字节时n加1
			} else {
				// 当UCS2编码的第一个字节不等于0时，该UCS2字符为汉字，一个汉字算两个字节
				if (bytes[i] != 0) {
					n++;
				}
			}
		}
		// 如果i为奇数时，处理成偶数
		if (i % 2 == 1)

		{
			// 该UCS2字符是汉字时，去掉这个截一半的汉字
			if (bytes[i - 1] != 0)
				i = i - 1;
			// 该UCS2字符是字母或数字，则保留该字符
			else
				i = i + 1;
		}

		return new String(bytes, 0, i, "Unicode");
	}
	
	public static boolean inNumRange(String value,int min ,int max){
		try{
			int val =  pasreInt(value);
			if(val < min) return false;
			if(val > max) return false;
			return true;	
		}catch(Exception e){
			return false;
		}
	}
	
	public static boolean inNUmRange(String value,String splitValues){
		String temp = splitValues;
		if(!splitValues.startsWith(",")) {
			temp ="," + temp;
		}
		if(!splitValues.endsWith(",")) {
			temp +=",";
		}
		if(temp.indexOf(","+value+",")>-1) return true;
		return false;
	}
	
	public static String removeCodePrefix(String value){
		if(isEmpty(value)) return "";
		int pos = value.indexOf("-");
		if( pos >-1){
			return value.substring(pos+1);
		}else{
			//非代码规范代码,则直接返回空串
			return "";
		}
	}
	
	public static String checkTN(String value,String defaultValue)  {
		try {
			if(isEmpty(value)) return defaultValue;
			if(CommUtil.isEmpty(value) || CommUtil.pasreIntThrowE(value) < 1 
					||  CommUtil.pasreInt(value)>255){
				return defaultValue;
			}
		} catch(Exception e){
			log.error("无符号数字类型："+value);
            return  defaultValue;
		}
		return value;
	}
	
	public static String CheckSN(String value,String defaultValue) throws Exception {
		try {
			if(isEmpty(value)) return defaultValue;
			if(CommUtil.isEmpty(value) || CommUtil.pasreIntThrowE(value) < -32768 
					||  CommUtil.pasreInt(value)>32768){
				return defaultValue;
			}
		} catch (Exception e) {
			log.error("数字类型："+value);
			return defaultValue;
		}
		return value;
	}
	
	public static String  CheckCodeTN(String value,String defaultValue) {
		try {
			if(isEmpty(value)) return defaultValue;
			if(value.indexOf("-") == -1) return defaultValue;
			value =  CommUtil.removeCodePrefix(value);
			if(CommUtil.isEmpty(value) || CommUtil.pasreIntThrowE(value) < 1 
							||  CommUtil.pasreInt(value)>255){
				return defaultValue;
			}
		} catch (Exception e) {
			log.error("数字类型："+value);
			return defaultValue;
		}
		return value;
	}
	
	public static String  CheckCodeNL(String value,String defaultValue){
		if(isEmpty(value)) return defaultValue;
		try{
			int age=Integer.parseInt(value);
			if(age<0||age>150){
				return defaultValue;
			}else{
				return Integer.toString(age);
			}
		}catch(Exception e){
			log.error("年龄："+value);
			return defaultValue;
		}
	}
	
	public static String  CheckCodeSN(String value,String defaultValue){
		if(isEmpty(value)) return defaultValue;
		if(value.indexOf("-") == -1) return defaultValue;
		value =  CommUtil.removeCodePrefix(value);
		if(CommUtil.isEmpty(value) || CommUtil.pasreInt(value) < -32768
						||  CommUtil.pasreInt(value)> 32768){
			return defaultValue;
		}
		return value;
	}
	
	public static String formatDate(String dateStr,String key){
		if(isEmpty(dateStr) || dateStr.length()!=8) return "";
		return dateStr.substring(0,4)+key+dateStr.substring(5,6)+key+dateStr.substring(7);
	}
	
	public static String foramtDate(Date day,String format){
		if(day == null) return "";
		SimpleDateFormat sdf = new SimpleDateFormat(format);
		return sdf.format(day);
	}
	
	 /** 
     * yyyyMMdd 或 yyyy-MM-dd HH:mm:ss:mmm转换成yyyy-MM-ddTHH:mm格式 
     * @param rq1 
     * @return 
     */ 
    public static String convertRqT(String rq1) { 
      String rq = ""; 
      if (rq1 == null) return "";else rq1 = CommUtil.trim(rq1); 
      if (!"".equals(rq1) && rq1.length() == 8) { 
        rq = rq1.substring(0, 4) + "-" + rq1.substring(4, 6) + "-" + rq1.substring(6, 8) + "T00:00:00"; 
      } else if(!"".equals(rq1) && rq1.length() >= 19) { 
       rq = rq1.substring(0,10) + "T" + rq1.substring(11,19); 
      } else if(rq1.length() == 14) {
    	  rq = rq1.substring(0, 4) + "-" + rq1.substring(4, 6) + "-" + rq1.substring(6, 8) + "T" + rq1.substring(9, 14)+":00"; 
      } else if(rq1.length() == 13) {
    	  rq = rq1.substring(0, 4) + "-" + rq1.substring(4, 6) + "-" + rq1.substring(6, 8) + "T" + rq1.substring(8, 13)+":00"; 
      }
      return rq; 
    } 
    
    
    public static String convertRq(String rq1){
    	if(isEmpty(rq1)) return "";
    	if(rq1.length()<8) return "";
    	return convertRqT(rq1).substring(0,10);
    }
    
    /**
     * yyyyMMdd ->  yyyy-MM-dd
     */
    public static String convertRq8(String rq){
    	if(isEmpty(rq)) return "";
    	if(rq.length()<8||rq.length()>8){
    		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
			try {
				sdf.parse(rq);
				return rq;
			} catch (ParseException e) {
				e.printStackTrace();
			}
    		log.info(rq);
    		return "";
    	}else{
    		rq = rq.substring(0,4)+"-"+rq.substring(4,6)+"-"+rq.substring(6,8);
    		return rq;
    	}
    }
    
    /**
     * yyyy-MM-dd HH:mm:ss ->  yyyy-MM-ddTHH:mm:ss.0Z
     */
    public static String convertRq9(String rq){
    	if(isEmpty(rq)) return "";
    	if(rq.length()<19||rq.length()>19){
    		log.info(rq);
    		return "";
    	}else{
    		rq = rq.replace(" ", "T")+".0Z";
    		return rq;
    	}
    }
    
	
	public static boolean checkValidDate(String pDateStr) {
		boolean ret = true;
		if (pDateStr == null || "".equals(pDateStr.trim())) {
			ret = false;
		}
		
		if("0000-00-00".equals(pDateStr)) {
			ret = false;
		} 
		
		try {
			if(pDateStr.indexOf("-")>-1){
				if(pDateStr.length()!=10) return false;
				int year = new Integer(pDateStr.substring(0, 4)).intValue();
				int month = new Integer(pDateStr.substring(5, 7)).intValue();
				int day = new Integer(pDateStr.substring(8)).intValue();
	
				if(year <= 1900) {
					ret = false;
				} else {
					Calendar cal = Calendar.getInstance();
					cal.setLenient(false); //允许严格检查日期格式   
					cal.set(year, month - 1, day);
					cal.getTime();//该方法调用就会抛出异常   
				}
			}else{
				if(pDateStr.length() < 8) return false;
				
				int year = new Integer(pDateStr.substring(0, 4)).intValue();
				int month = new Integer(pDateStr.substring(4, 6)).intValue();
				int day = new Integer(pDateStr.substring(6,8)).intValue();
	
				if(year <= 1900) {
					ret = false;
				} else {
					Calendar cal = Calendar.getInstance();
					cal.setLenient(false); //允许严格检查日期格式   
					cal.set(year, month - 1, day);
					cal.getTime();//该方法调用就会抛出异常   
				}
				
			}
		} catch (Exception e) {
			e.printStackTrace();
			ret = false;
		}
		return ret;
	}
	
	/**
	 * 关闭数据流
	 * 
	 * @param obj
	 */
	public static void closeStream(Object obj) {
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
	
	  /**
	   * 解压文件流
	   * @param in 输入流
	   * @return 返回一个字节输出流
	   */
	  public static ByteArrayOutputStream deCompress(InputStream in) {
	    ByteArrayOutputStream baos = new ByteArrayOutputStream();
	    ZipInputStream zipIn = null;
	    try {
	      CheckedInputStream csumi = new CheckedInputStream(in, new CRC32());
	      zipIn = new ZipInputStream(new BufferedInputStream(csumi));
	      byte[] bytes = new byte[1024];
	      while ((zipIn.getNextEntry()) != null) {
	        int x;
	        while ((x = zipIn.read(bytes)) != -1) {
	          baos.write(bytes, 0, x);
	        }
	      }
	      csumi.close();
	    } catch (Exception e1) {
	      e1.printStackTrace();
	    }finally{
	    	try{
	    		if (zipIn != null)
	    			zipIn.close();
	    	}catch (IOException e1){
	    		e1.printStackTrace();
	    	}
	    }
	    return baos;
	  }
	  
	  /**
	     * 转小序
	     * @param a
	     * @return
	     * @date 2013-2-21
	     */
	    public static int ntohl(int a){
	        return ((a & 0xFF00) >> 8)|((a & 0x00FF) << 8);
	    }
	    
	    /**
	     * 转大序
	     * @param a
	     * @return
	     * @date 2013-2-21
	     */
	    public static int ntolh(int a){
	        return ((a & 0x00FF) << 8)|((a & 0xFF00) >> 8);
	    }
	    
	    /**
	     * 字符转16进制
	     * @param c 字符
	     * @return
	     * @date 2013-2-21
	     */
	    public static String charToHex(char c){
	        String s = Integer.toHexString(c & 0xFF);
	        if (s.length() == 1){
	            return "0" + s;
	        } else{
	            return s;
	        }
	    }
	  
	  /**
		 * 将复选对象，解码成数组
		 * @param base64Str
		 * @return
		 */
		public static  List<Integer> decodeLowHexInt(String base64Str){
			List<Integer> arr = new ArrayList<Integer>(); 
			if(isEmpty(base64Str)) return arr;
			byte[] data =  null;
			try {
				data = Base64.decodeBase64(trim(base64Str).getBytes("UTF-8"));
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
			StringBuilder hex = new StringBuilder();
			int i = 0 ;
			for(byte d :data){
				hex.append(charToHex((char)d));
				i++;
				
				if(i % 2 == 0){
					arr.add(ntohl(Integer.parseInt(hex.toString(),16)));
					hex.delete(0, hex.length());
				}
			}
			return arr;
		}
		
		public static void main(String[] args){
			exportUtil util = new exportUtil();
			util.fileToZip("tj.zip","c://2015");
		}
		
		
		public static List<Integer>  convertIntArr(String str){
			List<Integer> list = new ArrayList<Integer>();
			if(isEmpty(str)) return list;
			String[] ss = str.split("\\,");
			for(String s : ss){
				if(isEmpty(s)) continue;
				list.add(Integer.parseInt(s));
			}
			return list;
		}
		
		public static String encodeLowHexStr(String str){
			return encodeLowHexInt(convertIntArr(str));
		}
		
		/**
		 * 将代码复选的选编码成base64,复选对象
		 * @param arr
		 * @return
		 */
		public static String encodeLowHexInt(List<Integer> arr){
			if(arr==null || arr.size()==0) return "";
			byte[] data = new byte[arr.size() * 2];
			int k = 0;
			for(Integer i :arr){
				String hex= "0000"+Integer.toHexString(ntohl(i));
				
				hex = hex.substring(hex.length()-4);
				data[k+0] = (byte)Integer.parseInt(hex.substring(0,2),16);
				data[k+1] = (byte)Integer.parseInt(hex.substring(2),16);
				k = k + 2;
			}
			try {
				return new String(Base64.encodeBase64(data),"UTF-8");
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
			return "";
		}
		
		/**
		 * 将2进制的编码的代码，翻译成对应的序号值
		 * @param value
		 * @return
		 */
		public static List<Integer> decodeBinInt(long value){
			List<Integer> list = new ArrayList<Integer>();
			String bstr = Long.toBinaryString(value);
			int len = bstr.length();
			for(int i = 0; i < len ; i++){
				if("1".equals(bstr.substring(i,i+1))){
					list.add(len-i);
				}
			}
			return list;
		}
		
		
		public static long encodeBinStr(String str){
			return encodeBinInt(convertIntArr(str));
		}
		
		public static long encodeBinInt(List<Integer> arr){
			if(arr==null || arr.size()==0) return 0;
			Map<String,Integer> mp = new HashMap<String,Integer>();
			for(Integer i : arr){
				mp.put(String.valueOf(i), i);
			}
			StringBuilder sb = new StringBuilder();
			//最大32位
			for(int i=32;i >0; i--){
				if(mp.get(String.valueOf(i))==null){
					sb.append("0");
				}else{
					sb.append("1");
				}
			}
			return Long.parseLong(sb.toString(), 2);
		}
		
		//长度为4的法院代码
		public static String long4Dm(String dm){
			if(dm.length()==4){
				return dm;
			}else if(dm.length()==3){
				return "0"+dm;
			}else if(dm.length()==2){
				return "00"+dm;
			}else if(dm.length()==1){
				return "000"+dm;
			}else{
				return "";
			}
		}
		//19位 补位0（1位） 高法代码（GFDM）（4位）+ 年度（ND）（4位）+ 案件类型标识（AJLXBS）（3位）+ 流水号（7位）
		//0000020161890000013
		//变更为15位  4位法院代码+8+2位年度代码+1位案件类型代码+7位案件序号。
		
		//18位  高法代码（GFDM）（4位）+ 年度（ND）（4位）+ 案件类型标识（AJLXBS）（4位）+ '8'+流水号（6位）
		//
		//变更为15位  4位法院代码+8+2位年度代码+1位案件类型代码+8+6位案件序号。
		public static String convertLeng15Ahdm(String ahdm){
			if(ahdm.length()==19){
				String ahdm_bg = "";
				ahdm_bg += ahdm.substring(1, 5);
				ahdm_bg += "8";
				ahdm_bg += ahdm.substring(7, 9);
				String ajlx = ahdm.substring(9, 12);
				if("189".equals(ajlx)){
					ahdm_bg += "1";
				}else if("190".equals(ajlx)){
					ahdm_bg += "2";
				}else if("191".equals(ajlx)){
					ahdm_bg += "3";
				}else if("192".equals(ajlx)){
					//父类
					//ahdm_bg += "4";
				}else if("193".equals(ajlx)){
					ahdm_bg += "4";
				}else if("194".equals(ajlx)){
					ahdm_bg += "5";
				}else if("195".equals(ajlx)){
					ahdm_bg += "6";
				}else if("196".equals(ajlx)){
					ahdm_bg += "7";
				}else if("197".equals(ajlx)){
					ahdm_bg += "8";
				}else{
					ahdm_bg += "8";
				}
				ahdm_bg += ahdm.substring(12, 19);
				return ahdm_bg;
			}else if(ahdm.length()==18){
				String ahdm_bg = "";
				ahdm_bg += ahdm.substring(0, 4);
				ahdm_bg += "8";
				ahdm_bg += ahdm.substring(6, 8);
				String ajlx = ahdm.substring(8, 12);
				if("1002".equals(ajlx)){
					ahdm_bg += "1";
				}else if("1003".equals(ajlx)){
					ahdm_bg += "2";
				}else if("1004".equals(ajlx)){
					ahdm_bg += "3";
				}else if("1005".equals(ajlx)){
					//父类
					//ahdm_bg += "4";
				}else if("1006".equals(ajlx)){
					ahdm_bg += "4";
				}else if("1007".equals(ajlx)){
					ahdm_bg += "5";
				}else if("1008".equals(ajlx)){
					ahdm_bg += "6";
				}else if("1009".equals(ajlx)){
					ahdm_bg += "7";
				}else if("1010".equals(ajlx)){
					ahdm_bg += "8";
				}else{
					ahdm_bg += "8";
				}
				ahdm_bg += "8";
				ahdm_bg += ahdm.substring(12, 18);
				return ahdm_bg;
			}else{
				return ahdm;
			}
		}

	/**
	 * 针对审判转成15位,ahdm来源为15转09进入
	 * 18位的AHDM代码 不是执行案件时（9~12位标识的案件类型）
	 算法调整为：
	 1`4位：法标法院代码
	 1位：9
	 1位：年度最后一位 （5~8位年度中的最后1位）
	 4位:案件类型 （9~12位）
	 5位：案件序号的后5位（13~18位案件序号，取后5位，一般不会到10万）
	 *
	 * @param ahdm
	 * @return
     */
	public static String convertLeng15AhdmSp(String ahdm){
		if(ahdm.length()==18){
			String ahdm_bg = "";
			ahdm_bg += ahdm.substring(0, 4);
			ahdm_bg += "9";
			String nd = ahdm.substring(4, 8);
			ahdm_bg += nd.substring(3);//年度最后一位
			String ajlx = ahdm.substring(8, 12);
			ahdm_bg += ajlx;
			String xh = ahdm.substring(12, 18);
			ahdm_bg += xh.substring(1);//序号的后5位

			return ahdm_bg;
		}else{
			return ahdm;
		}
	}
		
		//获取压缩包序号 
		public static String getZipXh(Object ob, int cnt) {
			StringBuilder formatStr = new StringBuilder();
			for (int i = 0; i < cnt; i++) {
				formatStr.append("0");
			}
			String tt = formatStr.toString() + String.valueOf(ob);
			int len = tt.length();
			return tt.substring(len - cnt, len);
		}
}
