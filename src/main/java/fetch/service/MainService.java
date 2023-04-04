package fetch.service;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

/**
 * @author zenggs
 * @Date 2023/4/3
 */
public class MainService {

    public static String getUrlContent(String url) throws Exception {
        try {
            URL obj = new URL(url);
            // 打开连接
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();
            // 设置请求方式
            con.setRequestMethod("GET");
            // 获取响应状态码
            int responseCode = con.getResponseCode();
            // 判断响应是否成功
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                String inputLine;
                StringBuffer response = new StringBuffer();
                while ((inputLine = in.readLine()) != null) {
                    // 读取响应内容
                    response.append(inputLine);
                }
                in.close();
                // 输出响应内容
                return response.toString();
            } else {
                System.out.println("GET request failed");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        throw new Exception("GET request failed");
    }

    public static String getDomainUrl(String domainUrl) throws Exception{
        String response = getUrlContent(domainUrl);
        StringBuilder result = new StringBuilder();
        Document document = Jsoup.parse(response);
        List<String> hrefs = document.select("a").eachAttr("href");
        URL context = new URL(domainUrl);
        hrefs.stream().distinct().forEach(it->{
            try {
                String relHref = new URL(context, it).toString();
                result.append(relHref).append("\n");
            }catch (Exception e){
            }
        });
        return result.toString();
    }
}