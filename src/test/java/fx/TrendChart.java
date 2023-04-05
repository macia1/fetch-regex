/**
 * @author macia
 * @date 2023/4/5
 */
import java.util.ArrayList;
import java.util.List;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.stage.Stage;

public class TrendChart extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        List<Double> data = new ArrayList<>();
        data.add(10.0);
        data.add(20.0);
        data.add(15.0);
        data.add(30.0);

        NumberAxis xAxis = new NumberAxis();
        xAxis.setLabel("X");

        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Y");

        LineChart chart = new LineChart(xAxis, yAxis);
        chart.setTitle("Trend Chart");

        XYChart.Series dataSeries = new XYChart.Series();
        dataSeries.setName("Data Points");

        int x = 0;
        for (double d : data) {
            dataSeries.getData().add(new XYChart.Data(x, d));
            x++;
        }
        chart.getData().add(dataSeries);

        Scene scene = new Scene(chart, 800, 600);

        stage.setTitle("Trend Chart");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }

}

