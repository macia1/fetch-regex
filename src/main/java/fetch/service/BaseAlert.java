package fetch.service;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.control.Alert;
import javafx.util.Duration;

/**
 * @author zenggs
 * @Date 2023/4/3
 */
public class BaseAlert {

    public void showAlert(String title, String headerText, String contentText, Alert.AlertType alertType) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(headerText);
        alert.setContentText(contentText);
        // 创建一个时间线动画，3 秒后关闭 Alert
        Timeline timeline = new Timeline(
                new KeyFrame(Duration.seconds(3), e -> alert.hide())
        );
        timeline.play();
        alert.showAndWait();
    }

    public void showInfo(String title, String headerText, String contentText) {
        showAlert(title, headerText, contentText, Alert.AlertType.INFORMATION);
    }

    public void showWarning(String title, String headerText, String contentText) {
        showAlert(title, headerText, contentText, Alert.AlertType.WARNING);
    }

    public void showError(String title, String headerText, String contentText) {
        showAlert(title, headerText, contentText,
                Alert.AlertType.ERROR);
    }

}