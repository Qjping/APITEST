package config;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Parameters;

import java.sql.*;
import java.util.*;

/**
 * 数据库操作工具
 *
 * @author longrong.lang
 */
public class DataMysql2{

    static Connection conn = null;

    public static String driverClassName = "com.mysql.cj.jdbc.Driver";
    public static String ip="127.0.0.1";
    public static int port =3306;
    public static String baseName ="apitest";
    public static String username = "root";
    public static String password = "123456";
    public static String url = String.format("jdbc:mysql://%s:%s/%s?serverTimezone=UTC", ip, port, baseName);
    public String sql=null;

    @BeforeClass
    @Parameters({"valueName"})
    public  void setsql(String sql){
        this.sql=sql;
    }


    /**
     * 执行sql
     *
     * @param jdbcUrl 数据库配置连接
     * @param sql     sql语句
     * @return
     */
    public static List<Map<String, String>> getDataList(String jdbcUrl, String sql) {
        List<Map<String, String>> paramList = new ArrayList<Map<String, String>>();

        Statement stmt = null;
        try {
            // 注册 JDBC 驱动
            Class.forName(driverClassName);
            // 打开链接
            conn = DriverManager.getConnection(jdbcUrl, username, password);
            // 执行查询
            stmt = conn.createStatement();
            ResultSet rs = null;
            rs = stmt.executeQuery(sql);
            String columns[] = {"header", "id", "data","url"};
            // 展开结果集数据库
            while (rs.next()) {
                Map<String, String> map = new LinkedHashMap<String, String>();
                for (int i = 0; i < columns.length; i++) {
                    String cellData = rs.getString(columns[i]);
                    map.put(columns[i], cellData);
                }
                paramList.add(map);
            }
            // 完成后关闭
            rs.close();
            stmt.close();
            conn.close();
        } catch (SQLException se) {
            // 处理 JDBC 错误
            System.out.println("处理 JDBC 错误!");
        } catch (Exception e) {
            // 处理 Class.forName 错误
            System.out.println("处理 Class.forName 错误");
        } finally {
            // 关闭资源
            try {
                if (stmt != null) stmt.close();
                if (conn != null) conn.close();
            } catch (SQLException se) {
                se.printStackTrace();
            }
        }
        return paramList;
    }

    @DataProvider(name="testData")
    public Object[][] dbDataMethod() {

        System.out.println(sql);
        List<Map<String, String>> result = getDataList(url, sql);
        Object[][] files = new Object[result.size()][];

        for (int i = 0; i < result.size(); i++) {
            files[i] = new Object[]{result.get(i)};
        }
        return files;
    }
}