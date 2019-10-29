
import com.google.gson.JsonObject;
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
import java.lang.invoke.LambdaMetafactory;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TestCase extends DataMysql2 {

    String sql=null;   //xml中读出的sql语句

    @Parameters({"valueName"})
    @BeforeClass()
    public void beforeClass(String sql) {
        this.sql = sql;
    }

    @BeforeTest
    public void globalVariable(){
        TestConfig.setMap(TestConfig.map);
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
        String url = replaceVariableParemeters(
                dataMap.get("url"));
        String method = dataMap.get("method");
        JSONObject headerObject = new JSONObject(replaceVariableParemeters(dataMap.get("header")));
        dataMap.get("data").replaceAll(" ", "");
        String data = replaceVariableParemeters(dataMap.get("data"));
        String expected = dataMap.get("expected");

        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(TestConfig.connectTimeout, TimeUnit.SECONDS)
                .writeTimeout(TestConfig.writeTimeout,TimeUnit.SECONDS)
                .readTimeout(TestConfig.readTimeout, TimeUnit.SECONDS).build();

        Request.Builder builder = new Request.Builder();
        Request request = null;
        String result = null;
        Response response = null;

        /**
         * 发起请求
         */
        System.out.println("========================" + dataMap.get("description")+"========================");
        Reporter.log("请求url："+url);
        Reporter.log("请求参数："+data);


        //设置header
        Iterator<String> headerIterator = headerObject.keys();
        while (headerIterator.hasNext()){
            String key = headerIterator.next();
            builder.header(key,headerObject.getString(key));
        }

        //设置request
        if(headerObject.has("Content-Type")){
        if (headerObject.getString("Content-Type").equals("application/x-www-form-urlencoded")){
            if (method.equals("POST")){
                FormBody.Builder formBuilder = new FormBody.Builder();
                if (!StringUtils.isNullOrEmpty(data)){
                    JSONObject body = new JSONObject(data);
                    body.keySet().forEach(e->formBuilder.add(e,body.getString(e)));

                }

                FormBody body = formBuilder.build();
                request = builder.url(url).post(body).build();
            }else if (method.equals("GET")){
                request = builder.url(url).build();
            }
        }else if (headerObject.getString("Content-Type").contains("application/json")){
            RequestBody body = RequestBody.create(okhttp3.MediaType.parse("application/json;charset=UTF-8"), data);
            if (method.equals("POST")){
                request = builder.url(url).post(body).build();
            }else if (method.equals("GET")){
                request = builder.url(url).build();
            }else if (method.equals("PUT")){
                request = builder.url(url).put(body).build();
            }else if (method.equals("DELETE")){
                request = builder.url(url).delete(body).build();
            }
        }
        }else{
            request=builder.url(url).get().build();
        }
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
        if (!StringUtils.isNullOrEmpty(dataMap.get("has_global_variable"))) {
            //获得存储变量名及变量的path
            JSONObject dependFields = new JSONObject(dataMap.get("has_global_variable"));
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
                if (StringUtils.isNullOrEmpty(expectedFiellds))
                {
                    //判断对应路径值存在
                    Assert.assertNotNull(JsonPath.read(result,path));
                }{
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
                string = string.replace(m.group(),TestConfig.map.get(newStr));
            }
            return string;
        }else {
            return "";
        }
    }


    private static String request
}