package config;


import java.util.HashMap;
import java.util.Map;

public class TestConfig {

    /**
     * 数据库链接配置
     */
    public static String IP = "127.0.0.1";
    public static String PORT = "3306";
    public static String DATABASE = "apitest";
    public static String USERNAME = "root";
    public static String PASSWORD = "123456";

    /**
     * 配置链接超时时间
     * 单位 s
     */
    public static int connectTimeout = 30;  //链接超时
    public static int writeTimeout = 30;   //写入超时
    public static int readTimeout = 30;   //读取超时

    /**
     * 全局变量
     */
    public static Map<String,String> map = new HashMap<String,String>();
    static{
        map.put("KoreaDirectId","");        //韩国直邮商品id
        map.put("KoreaDirectSimpleId","");       //韩国直邮商品simple_id
        map.put("device_uid","Jmeter_ApiTest_device_uid");
        map.put("has_global_variable","");
        map.put("order","order");
        map.put("password","123456");

    }

}