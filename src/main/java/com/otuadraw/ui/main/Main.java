package com.otuadraw.ui.main;

import com.otuadraw.util.DateFormatUtil;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Main extends Application {

    private final static Logger LOGGER = LogManager.getLogger(Main.class.getName());

    @Override
    public void start(Stage stage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("/main/main.fxml"));

        Scene scene = new Scene(root);

        stage.setScene(scene);
        stage.setWidth(1200);
        stage.setHeight(930);
        stage.setResizable(false);
        stage.setTitle("OtuaDraw");
        stage.getIcons().add(new Image("otuadraw_icon.png"));
        stage.show();
    }

    public static void main(String[] args) {
        Long startTime = System.currentTimeMillis();
        LOGGER.log(Level.INFO, "OtuaDraw launched on {}", DateFormatUtil.formatDateTimeString(startTime));
        launch(args);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            Long exitTime = System.currentTimeMillis();
            LOGGER.log(Level.INFO, "OtuaDraw is closing on {}. Used for {} ms", DateFormatUtil.formatDateTimeString(startTime), exitTime);
        }));
    }
}
