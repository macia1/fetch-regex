package fetch.service;

import com.alibaba.fastjson.JSONObject;
import fetch.db.ArticleDetailUrlDao;
import fetch.db.UnArticleDetailUrlDao;
import fetch.db.model.ArticleDetailUrlModel;
import fetch.db.model.UnArticleDetailUrlModel;
import fetch.util.DomainUtil;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.net.URI;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author macia
 */
@Controller
@Slf4j
public class MainController extends BaseAlert {
    @FXML
    private TextField urlField;
    @FXML
    private Button getBtn;
    @FXML
    private TextField matchField;
    @FXML
    private Button matchBtn;
    @FXML
    private Button delParamBtn;
    @FXML
    private Button delAnchorPointBtn;
    @FXML
    private Button delDomainBtn;
    @FXML
    private Button patternBtn;
    @FXML
    private TextArea firstFlow;
    @FXML
    private TextArea nextFlow;

    @FXML
    protected void handleGetAction(ActionEvent actionEvent) {
        // https://www.chinanews.com.cn/
        String domainUrl = urlField.getText();
        if ("".equals(domainUrl)) {
            showWarning("Warning", "", "域名不能为空!");
        } else if (!domainUrl.startsWith("http")) {
            showWarning("Warning", "", "非标准http!");
        } else {
            String urls = null;
            try {
                urls = MainService.getDomainUrl(domainUrl);
            } catch (Exception e) {
                showError("Error", "", e.getMessage());
                e.printStackTrace();
            }
            firstFlow.setText(urls);
        }
    }

    @FXML
    protected void handleMatchAction(ActionEvent actionEvent) {
        String patternStr = matchField.getText();
        if ("".equals(patternStr)) {
            showWarning("", "", "正则表达式为空!");
        }
        Pattern pattern = Pattern.compile(patternStr);
        StringBuilder firstFlowVal = new StringBuilder(firstFlow.getText());
        StringBuilder nextFlowVal = new StringBuilder();
        for (String url : firstFlow.getText().split("\n")) {
            Matcher matcher = pattern.matcher(url);
            while (matcher.find()) {
                String group = matcher.group();
                int start = firstFlowVal.indexOf(group);
                firstFlowVal.delete(start, start + group.length());
                nextFlowVal.append(group).append("\n");
            }
        }
        nextFlow.setText(nextFlowVal.toString());
        firstFlow.setText(firstFlowVal.toString().replaceAll("\n+", "\n"));
    }

    @Resource
    UnArticleDetailUrlDao unArticleDetailUrlDao;
    @Resource
    ArticleDetailUrlDao articleDetailUrlDao;


    @FXML
    protected void handleSave(ActionEvent actionEvent) {
        List<ArticleDetailUrlModel> articleDetailUrlModelList = new ArrayList<>();
        List<UnArticleDetailUrlModel> unArticleDetailUrlModelList = new ArrayList<>();
        String firstFlowText = firstFlow.getText();
        for (String fistUrl : firstFlowText.split("\n")) {
            if ("".equals(fistUrl)) {
                continue;
            }
            articleDetailUrlModelList.add(new ArticleDetailUrlModel(fistUrl));
        }
        for (String nextUrl : nextFlow.getText().split("\n")) {
            if ("".equals(nextUrl)) {
                continue;
            }
            unArticleDetailUrlModelList.add(new UnArticleDetailUrlModel(nextUrl));
        }
        unArticleDetailUrlDao.saveAllAndFlush(unArticleDetailUrlModelList);
        articleDetailUrlDao.saveAllAndFlush(articleDetailUrlModelList);
        showInfo("Success", null, "入库成功");
    }


    @FXML
    protected void handleDelNoticeDomainAction(ActionEvent actionEvent) {
        String text = firstFlow.getText();
        String[] urls = text.split("\n");
        StringBuilder nextFlowText = new StringBuilder();
        for (String url : urls) {
            try {
                URI uri = URI.create(url);
                String path = uri.getPath();
                if ("".equals(path) || "/".equals(path) || path == null) {
                    continue;
                }
                StringBuilder urlBuilderStr = new StringBuilder(path);
                String query = uri.getQuery();
                if (!"".equals(query) && query != null) {
                    urlBuilderStr.append("?").append(query);
                }
                nextFlowText.append(urlBuilderStr).append("\n");
            } catch (Exception e) {
                log.error("异常链接:" + url);
            }
            Map<Integer,Integer> a = new HashMap<>();
            a.merge(1,2,Integer::sum);
        }
        firstFlow.setText(nextFlowText.toString());
    }

    @FXML
    protected void handleDelTransAction(ActionEvent actionEvent) {
        matchField.setText(matchField.getText().replace("\\\\", "\\"));
    }

    @FXML
    protected void handleDistanceDomainAction(ActionEvent actionEvent) {
        String domainUrl = urlField.getText();
        String[] urls = firstFlow.getText().split("\n");
        firstFlow.setText(MainService.distanceDomain(domainUrl, urls));
    }

    @FXML
    protected void handlePatternFetch(ActionEvent actionEvent) {
        String urlStr = firstFlow.getText();
        String indexUrl = urlField.getText();
        List<String> urls = Arrays.asList(urlStr.split("\n"));
        String mostPattern = DomainUtil.getMostPattern(indexUrl, urls);
        matchField.setText(mostPattern);
    }

    @FXML
    protected void showTrendChart(ActionEvent actionEvent){
        String[] result = firstFlow.getText().split("\n");
        Map<Integer,List<String>> dataMap = new HashMap<>();
        List<Integer> data = new ArrayList<>();
        for (String url : result) {
            if (StringUtils.isBlank(url)){
                continue;
            }
            int length = url.length();
            data.add(length);
            if (dataMap.get(length) != null) {
                List<String> strings = dataMap.get(length);
                strings.add(url);
            } else {
                List<String> a = new ArrayList<>();
                a.add(url);
                dataMap.put(length, a);
            }
        }

        Collections.sort(data);
        NumberAxis xAxis = new NumberAxis();
        xAxis.setLabel("X");

        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Y");

        LineChart chart = new LineChart(xAxis, yAxis);
        chart.setTitle("Trend Chart");
        chart.createSymbolsProperty();
        XYChart.Series dataSeries = new XYChart.Series();
        dataSeries.setName("Data Points");

        int x = 0;
        for (Integer d : data) {
            dataSeries.getData().add(new XYChart.Data(x, d, JSONObject.toJSONString(dataMap.get(d))));
            x++;
        }
        chart.getData().add(dataSeries);

        Scene scene = new Scene(chart, 800, 600);
        Stage charSate = new Stage();
        charSate.setTitle("Trend Chart");
        charSate.setScene(scene);
        charSate.show();
    }
}
