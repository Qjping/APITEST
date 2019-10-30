package config;

import java.sql.*;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/*
 * JDBC操作数据库的步骤:
 * 1.注册驱动
 * 		告知JVM使用的是哪一个数据库的驱动
 * 2.获得连接
 * 		使用JDBC中的类,完成对mysql数据库的连接(TCP协议)
 * 3.获得语句执行平台
 * 		通过连接对象获取对SQL语句的执行者对象
 * 4.执行sql语句
 * 		使用执行者对象,向数据库执行SQL语句
 * 		获取数据库的执行后的结果
 * 5.处理结果
 * 6.释放资源
 * 		调用一堆close
 */

public class DataMysql implements Iterator<Object[]>{

    public static String driverClassName = "com.mysql.cj.jdbc.Driver";
    ResultSet result;  //结果集
    List<Map<String, String>> dataList = new ArrayList<Map<String, String>>();  //生成存放结果集的list
    int TotalRows=0;     //总行数
    int CurrentRow=0;   //当前行数

    public DataMysql(String ip, String port, String baseName,
                                 String userName, String password, String sql){
        try {


            // 1. 注册驱动
            // 使用java.sql.DriverManager类的静态方法registerDriver(Driver driver)
            // Driver是一个接口,参数传递:MySQL驱动程序的实现类
            // DriverManager.registerDriver(new Driver());
            // 查看驱动类源码,注册两次驱动,浪费资源
            Class.forName(driverClassName);
            String url = String.format("jdbc:mysql://%s:%s/%s?serverTimezone=UTC", ip, port, baseName);
            //获取连接
            // 返回值是java.sql.Connection接口的实现类,在MySQL驱动程序中
            Connection conn = DriverManager.getConnection(url, userName, password);
            //获取创建语句对象
            //conn对象,调用方法 Statement createStatement() 获取Statement对象,将SQL语句发送到数据库
            //返回的是Statement接口的实现类对象,在MySQL驱动程序中
            Statement stmt = conn.createStatement();
            //执行sql语句，获取查询结果集
            result = stmt.executeQuery(sql);
            System.out.println("结果集"+result.getMetaData().toString());
            //获取当前行数据
            ResultSetMetaData rd = result.getMetaData();
            System.out.println("是否是当前行数据"+rd);
            //循环每行


            while (result.next()) {
                Map<String, String> map = new HashMap<String, String>();

                //循环每列，如果不要id，则将i设为2
                for (int i = 1; i <= rd.getColumnCount(); i++) {

                    String cellData=result.getString(i);
                    String key = result.getMetaData().getColumnName(i);
                    String value = result.getString(i);
                    System.out.println("key"+key+"value"+value);
                    map.put(key,value);
                }
                dataList.add(map);
            }

            this.TotalRows= dataList.size();
            conn.close();
            stmt.close();

        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    @Override
    public boolean hasNext() {
        if(TotalRows==0||CurrentRow>=TotalRows){
            return false;
        }else{
            return true;
        }
    }

//    @Override
//    public Object[] next() {
//        return new Object[0];
//    }

    @Override
    public Object[] next() {
        Map<String,String> s=dataList.get(CurrentRow);
        Object[] d=new Object[1];
        d[0]=s;
        this.CurrentRow++;
        return d;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("remove unsupported");
    }
}