package tdh.xsyj;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import tdh.db.DBHelper;

public class xsyjTest {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		Connection conn = null;
		Statement st = null;
		ResultSet rs = null;
		try {
//			WebContext.AjlxZhMap.clear();
			conn = DBHelper.getConn("com.sybase.jdbc3.jdbc.SybDriver", "jdbc:sybase:Tds:localhost:5000/xdb_3?charset=cp936", "sa", "");
			st = conn.createStatement();
			rs = st.executeQuery("SELECT AJLX,AJZLX,AJLXBS,FL,QTDM,DZ FROM T_AJLX_DMZH");
			Map<String,String> map1=new HashMap<String,String>();
			Map<String,Map<String,String>> map=new HashMap<String,Map<String,String>>();
			Map<String,String> map2=new HashMap<String,String>();
			Map<String,String> map3=new HashMap<String,String>();
			Map<String,String> map4=new HashMap<String,String>();
			Map<String,String> map5=new HashMap<String,String>();
			Map<String,String> map6=new HashMap<String,String>();
			Map<String,String> map7=new HashMap<String,String>();
			Map<String,String> map8=new HashMap<String,String>();
			while (rs.next()) {
				String fl=rs.getString("FL"),ajlxbs="",ajlx="";
				if("1".equals(fl)){
					ajlx=rs.getString("AJLX");
					ajlxbs=rs.getString("AJLXBS");
					map1.put(ajlx, ajlxbs);
				}else if("2".equals(fl)){
					ajlx=rs.getString("AJLX")+rs.getString("AJZLX");
					ajlxbs=rs.getString("AJLXBS");
					map2.put(ajlx, ajlxbs);
				}else if("3".equals(fl)){
					ajlx=rs.getString("AJLX")+rs.getString("DZ");
					ajlxbs=rs.getString("AJLXBS");
					map3.put(ajlx, ajlxbs);
				}else if("4".equals(fl)){
					ajlx=rs.getString("AJLX")+rs.getString("AJZLX")+rs.getString("QTDM");
					ajlxbs=rs.getString("AJLXBS");
					map4.put(ajlx, ajlxbs);
				}else if("5".equals(fl)){
					ajlx=rs.getString("AJLX")+rs.getString("QTDM");
					ajlxbs=rs.getString("AJLXBS");
					map5.put(ajlx, ajlxbs);
				}else if("6".equals(fl)){
					ajlx=rs.getString("AJLX")+rs.getString("AJZLX")+rs.getString("QTDM");
					ajlxbs=rs.getString("AJLXBS");
					map6.put(ajlx, ajlxbs);
				}else if("7".equals(fl)){
					ajlx=rs.getString("AJLX")+rs.getString("AJZLX")+rs.getString("QTDM");
					ajlxbs=rs.getString("AJLXBS");
					map7.put(ajlx, ajlxbs);
				}else if("8".equals(fl)){
					ajlx=rs.getString("AJLX")+rs.getString("DZ")+rs.getString("QTDM");
					ajlxbs=rs.getString("AJLXBS");
					map8.put(ajlx, ajlxbs);
				}
			}
			map.put("1", map1);
			map.put("2", map2);
			map.put("3", map3);
			map.put("4", map4);
			map.put("5", map5);
			map.put("6", map6);
			map.put("7", map7);
			map.put("8", map8);
			for(String key:map.get("6").keySet()){
				System.out.println(key);
			}
			System.out.println("--------------"+map.get("1").get("22"));
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			DBHelper.closeResultSet(rs);
			DBHelper.closeStatement(st);
			DBHelper.closeConn(conn);
		}

	}

}
