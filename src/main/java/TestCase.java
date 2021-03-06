

import com.jayway.jsonpath.JsonPath;
import com.mysql.cj.core.util.StringUtils;

import config.DataMysql;
import config.DataMysql2;
import config.TestConfig;
import okhttp3.*;

import org.json.JSONObject;
import org.testng.Assert;
import org.testng.Reporter;
import org.testng.annotations.*;
import java.io.IOException;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.Boolean.TRUE;

public class TestCase extends DataMysql2 {

    String sql=null;   //xml中读出的sql语句

    @Parameters({"valueName"})
    @BeforeClass()
    public void beforeClass(String sql) {
        this.sql = sql;
    }

    @BeforeTest
    public void globalVariable(){

        System.out.println(TestConfig.map.toString());
    }

   // 只能返回一个Object二维数组或一个Iterator<Object[]>来提供复杂的参数对象
    @DataProvider(name = "testData")
    private Iterator<Object[]> getData(){
        return new DataMysql(TestConfig.IP,
                TestConfig.PORT,
                TestConfig.DATABASE,
                TestConfig.USERNAME,
                TestConfig.PASSWORD,
                sql);
    }


    @Test(dataProvider = "testData")
    public void test(Map<String, String> dataMap) {
        //获取测试数据值
        String url = replaceVariableParemeters(dataMap.get("url"));
        String method = dataMap.get("method");
        replaceVariableParemeters(dataMap.get("header"));
        JSONObject header= new JSONObject(replaceVariableParemeters(dataMap.get("header")));


        String data = replaceVariableParemeters(dataMap.get("data"));
        String expected = dataMap.get("expected");

        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(TestConfig.connectTimeout, TimeUnit.SECONDS)
                .writeTimeout(TestConfig.writeTimeout,TimeUnit.SECONDS)
                .readTimeout(TestConfig.readTimeout, TimeUnit.SECONDS).build();


        Request request =request(url,header,method,data);
        String result = null;
        Response response = null;
        /**
         * 发起请求
         */
        System.out.println("========================" + dataMap.get("description")+"========================");
        Reporter.log("请求url："+url);
        Reporter.log("请求参数："+data);

        //获取response
        try {
            response = okHttpClient.newCall(request).execute();
            result = response.body().string();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Reporter.log("请求url:"+url);
        Reporter.log("返回数据:"+result);


        /**
         * 保存返回结果中的变量
         */
        extrator(dataMap,result);

        /**
         * 校验
         */
        if (StringUtils.isNullOrEmpty(expected)){
            //判断code==200
            Assert.assertTrue(response.code() >=400);
        }else {
            JSONObject jsonObject = new JSONObject(expected);
            Iterator<String> expectedIterator = jsonObject.keys();
            while(expectedIterator.hasNext()) {
                // 获得校验参数的路径
                String path = expectedIterator.next();
                // 获得预期值
                String expectedFiellds = jsonObject.getString(path);
                if (StringUtils.isNullOrEmpty(expectedFiellds)) {
                    //判断对应路径值存在
                    Assert.assertNotNull(JsonPath.read(result,path));
                }else{
                    String str ="(?<=\\$\\{)(.+?)(?=})";
                    Pattern p = Pattern.compile(str);
                    Matcher m = p.matcher(expectedFiellds);
                    if(m.find()){
                        //去全局变量map里查
                        expectedFiellds = expectedFiellds.replace("${","").replace("}","");
                        Assert.assertEquals(JsonPath.read(result,path).toString(),TestConfig.map.get(expectedFiellds));
                    }else {
                        //对比预期值
                        Assert.assertEquals(JsonPath.read(result,path),jsonObject.getString(path));
                    }
                }
            }
        }
    }

    /**
     * 参数中如果包含变量，则从全局map中搜索变量进行替换
     * 如果不存在变量，则不做任何变动
     */
    private static String replaceVariableParemeters(String string){
        if (!StringUtils.isNullOrEmpty(string)){
            String str ="\\$\\{.*?}";
            Pattern p = Pattern.compile(str);
            Matcher m = p.matcher(string);
            while (m.find()){
                //去全局变量map里查
                String newStr = m.group().replace("${","").replace("}","");
                if (!TestConfig.map.containsKey(newStr)) {
                    Reporter.log("全局变量不存在" + newStr);
                    return string;
                }else {
                    string = string.replace(m.group(),TestConfig.map.get(newStr));
                }
            }
            return string;
        }else {
            return "";
        }
    }

    private static Request request(String url,JSONObject header,String method,String data){
        Request.Builder builder = new Request.Builder();
        Request request = null;
        //设置header
        Iterator<String> headerIterator = header.keys();
        while (headerIterator.hasNext()){
            String key = headerIterator.next();
            builder.header(key,header.getString(key));
        }

        if(!header.has("Content-Type")){
            return  builder.url(url).get().build();
        }

        if (header.getString("Content-Type").equals("application/x-www-form-urlencoded")){
            FormBody.Builder formBuilder = new FormBody.Builder();
            if (method.equals("POST")){
                if (!StringUtils.isNullOrEmpty(data)){
                    JSONObject body = new JSONObject(data);
                    body.keySet().forEach(e->formBuilder.add(e,body.getString(e)));
                }
                FormBody body = formBuilder.build();
                request = builder.url(url).post(body).build();
            }else if (method.equals("GET")){
                request = builder.url(url).build();
            }
        }

        if (header.getString("Content-Type").contains("application/json")){
            RequestBody body = RequestBody.create(okhttp3.MediaType.parse("application/json;charset=UTF-8"), data);
            switch (method) {
                case "POST": request = builder.url(url).post(body).build();break;
                case "PUT": request = builder.url(url).put(body).build();break;
                case "DELETE": request = builder.url(url).delete(body).build();break;
                default: request = builder.url(url).build();
            }
        }

        return  request;
    }


    private static void extrator(Map<String, String> data,String result ){
        if (!StringUtils.isNullOrEmpty(data.get("has_global_variable"))) {
            //获得存储变量名及变量的path
            JSONObject dependFields = new JSONObject(data.get("has_global_variable"));
            Iterator<String> sIterator = dependFields.keys();
            while(sIterator.hasNext()) {
                // 获得变量名
                String variable = sIterator.next();
                // 获得变量path
                String  variablePath= dependFields.getString(variable);
                // 从返回结果中获取变量值
                String variableValue = JsonPath.read(result,variablePath).toString();
                // 将获取的变量放到用于储存全局变量的map中
                TestConfig.map.put(variable,variableValue);
                System.out.println("全局变量 :"+TestConfig.map.toString());
            }
        }

    }



}