package config;


import java.util.HashMap;
import java.util.Map;

public class TestConfig {

    /**
     * 数据库链接配置
     */
    public static String IP = "127.0.0.1";
    public static String PORT = "3306";
    public static String DATABASE = "apiautotest";
    public static String USERNAME = "root";
    public static String PASSWORD = "123";
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
    public static void setMap(Map map) {
        map.put("KoreaDirectId","");        //韩国直邮商品id
        map.put("KoreaDirectSimpleId","");       //韩国直邮商品simple_id
        map.put("product_id","714603");
        map.put("Content-Type","application/json");
        map.put("path","app1");

    }
}