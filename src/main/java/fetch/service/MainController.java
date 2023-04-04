package fetch.service;

import fetch.db.ArticleDetailUrlDao;
import fetch.db.UnArticleDetailUrlDao;
import fetch.db.model.ArticleDetailUrlModel;
import fetch.db.model.UnArticleDetailUrlModel;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.springframework.stereotype.Controller;
import javax.annotation.Resource;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author macia
 */
@Controller
public class MainController extends BaseAlert{
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
    private CheckBox allCheckBox;
    @FXML
    private TextArea firstFlow;
    @FXML
    private TextArea nextFlow;

    @FXML
    protected void handleGetAction(ActionEvent actionEvent){
        boolean selected = allCheckBox.isSelected();
        String domainUrl = urlField.getText();
        if ("".equals(domainUrl)){
            showWarning("Warning","","域名不能为空!");
        }else if(!domainUrl.startsWith("http")){
            showWarning("Warning","","非标准http!");
        }else {
            String urls = null;
            try {
                urls = MainService.getDomainUrl(domainUrl);
            } catch (Exception e) {
                showError("Error","",e.getMessage());
                e.printStackTrace();
            }
            firstFlow.setText(urls);
        }
    }

    @FXML
    protected void handleMatchAction(ActionEvent actionEvent){
        String patternStr = matchField.getText();
        if ("".equals(patternStr)){
            showWarning("","","正则表达式为空!");
        }
        Pattern pattern = Pattern.compile(patternStr);
        StringBuilder firstFlowVal = new StringBuilder(firstFlow.getText());
        StringBuilder nextFlowVal = new StringBuilder();
        for (String url : firstFlow.getText().split("\n")) {
            Matcher matcher = pattern.matcher(url);
            while (matcher.find()){
                String group = matcher.group();
                int start = firstFlowVal.indexOf(group);
                firstFlowVal.delete(start,start+group.length());
                nextFlowVal.append(group).append("\n");
            }
        }
        nextFlow.setText(nextFlowVal.toString());
        firstFlow.setText(firstFlowVal.toString().replaceAll("\n+","\n"));
    }
    @Resource
    UnArticleDetailUrlDao unArticleDetailUrlDao;
    @Resource
    ArticleDetailUrlDao articleDetailUrlDao;


    @FXML
    protected void handleSave(ActionEvent actionEvent){
        List<ArticleDetailUrlModel> articleDetailUrlModelList = new ArrayList<>();
        List<UnArticleDetailUrlModel> unArticleDetailUrlModelList = new ArrayList<>();
        String firstFlowText = firstFlow.getText();
        for (String fistUrl : firstFlowText.split("\n")) {
            if ("".equals(fistUrl)){
                continue;
            }
            articleDetailUrlModelList.add(new ArticleDetailUrlModel(fistUrl));
        }
        for (String nextUrl : nextFlow.getText().split("\n")) {
            if ("".equals(nextUrl)){
                continue;
            }
            unArticleDetailUrlModelList.add(new UnArticleDetailUrlModel(nextUrl));
        }
//        UnArticleDetailUrlDao unArticleDetailUrlDao = applicationContext.getBean("unArticleDetailUrlDao",UnArticleDetailUrlDao.class);
//        ArticleDetailUrlDao articleDetailUrlDao = applicationContext.getBean("articleDetailUrlDao",ArticleDetailUrlDao.class);
        unArticleDetailUrlDao.saveAllAndFlush(unArticleDetailUrlModelList);
        articleDetailUrlDao.saveAllAndFlush(articleDetailUrlModelList);
        showInfo("Success",null,"入库成功");
    }



    @FXML
    protected void handleDelNoticeDomainAction(ActionEvent actionEvent){
        String text = firstFlow.getText();
        String[] urls = text.split("\n");
        StringBuilder nextFlowText = new StringBuilder();
        for (String url : urls) {
            URI uri = URI.create(url);
            String path = uri.getPath();
            if ("".equals(path) || "/".equals(path) || path == null){
                continue;
            }
            StringBuilder urlBuilderStr = new StringBuilder(path);
            String query = uri.getQuery();
            if (!"".equals(query) && query != null){
                urlBuilderStr.append("?").append(query);
            }
            nextFlowText.append(urlBuilderStr).append("\n");
        }
        firstFlow.setText(nextFlowText.toString());
    }

    @FXML
    protected void handleDelTransAction(ActionEvent actionEvent){
        matchField.setText(matchField.getText().replace("\\\\","\\"));
    }

    @FXML
    protected void handleDistanceDomainAction(ActionEvent actionEvent){
        URI domainUri = URI.create(urlField.getText());
        String domainUriHost = domainUri.getHost();
        StringBuilder result = new StringBuilder();
        for (String urlStr : firstFlow.getText().split("\n")) {
            try {
                URI uri = URI.create(urlStr);
                if (uri.getHost().contains(domainUriHost)){
                    result.append(urlStr).append("\n");
                }
            }catch (Exception e){
                showError("Error","","链接异常: " +urlStr);
            }
        }
        firstFlow.setText(result.toString());
    }
}
