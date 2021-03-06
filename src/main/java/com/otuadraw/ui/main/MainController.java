package com.otuadraw.ui.main;

import com.otuadraw.data.model.InkFile;
import com.otuadraw.data.model.InkPoint;
import com.otuadraw.data.model.InkTrail;
import com.otuadraw.enums.PointEnum;
import com.otuadraw.enums.ShapeEnum;
import com.otuadraw.service.factory.ServiceFactory;
import com.otuadraw.service.interfaces.FileService;
import com.otuadraw.service.interfaces.GuessService;
import com.otuadraw.util.AlertUtil;
import com.otuadraw.util.DateFormatUtil;
import com.otuadraw.util.StrokeTimeUtil;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseEvent;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;

import static com.otuadraw.enums.PointEnum.DRAG;
import static com.otuadraw.enums.PointEnum.PRESS;
import static com.otuadraw.enums.PointEnum.RELEASE;

public class MainController {
    public Canvas canvas;
    public Text guessText;

    private ServiceFactory serviceFactory = ServiceFactory.getInstance();
    private final static Logger LOGGER = LogManager.getLogger(MainController.class.getName());

    private GraphicsContext graphicsContext = null;
    private StrokeTimeUtil strokeTimeUtil = null;
    private InkFile inkFile = new InkFile();

    private static final String WINDOW_TITLE_TIP = "提示";
    private static final String WINDOW_TITLE_OPEN_FILE = "打开你的涂鸦";
    private static final String WINDOW_TITLE_SAVE_FILE = "保存你的涂鸦";
    private static final String FILE_SAVE_FAILED = "文件保存失败";
    private static final String FILE_NOT_SAVED = "当前窗口文件未保存";

    public void initialize() {
        graphicsContext = canvas.getGraphicsContext2D();
        graphicsContext.setLineWidth(5);
    }

    public void onMousePressed(MouseEvent mouseEvent) {
        if (strokeTimeUtil == null) {
            strokeTimeUtil = new StrokeTimeUtil();
        }
        if(inkFile != null && !inkFile.isDirty()){
            inkFile.setDirty(true);
        }
        graphicsContext.beginPath();

        final double x = mouseEvent.getX(), y = mouseEvent.getY();
        InkPoint point = getInkPoint(x, y, strokeTimeUtil,PRESS);
        inkFile.append(point);
        LOGGER.log(Level.INFO, "brush strokes ( {} , {} ), timestamp is {}, mouse presses", point.getX(),
                point.getY(), point.getTime());
        graphicsContext.moveTo(x, y);
        graphicsContext.stroke();
    }

    public void onMouseDragged(MouseEvent mouseEvent) {
        final double x = mouseEvent.getX(), y = mouseEvent.getY();
        InkPoint point = getInkPoint(x, y, strokeTimeUtil,DRAG);
        inkFile.append(point);
        LOGGER.log(Level.INFO, "brush strokes ( {} , {} ), timestamp is {}, mouse drags", point.getX(),
                point.getY(), point.getTime());
        graphicsContext.lineTo(x, y);
        graphicsContext.stroke();
        graphicsContext.closePath();
        graphicsContext.beginPath();
        graphicsContext.moveTo(x, y);
    }

    public void onMouseReleased(MouseEvent mouseEvent) {
        final double x = mouseEvent.getX(), y = mouseEvent.getY();
        InkPoint point = getInkPoint(x, y, strokeTimeUtil,RELEASE);
        inkFile.append(point);
        LOGGER.log(Level.INFO, "brush strokes ( {} , {} ), timestamp is {}, mouse releases", point.getX(),
                point.getY(), point.getTime());
        graphicsContext.lineTo(x, y);
        graphicsContext.stroke();
        graphicsContext.closePath();
    }

    public void guessTrail() {
        GuessService guessService = serviceFactory.getGuessService();
        try {
            ShapeEnum bestGuess = guessService.guessTrail(inkFile.getInkTrail(), canvas.getWidth(), canvas.getHeight());
            inkFile.setGuess(bestGuess);
            inkFile.setDirty(true);
            if(bestGuess!=null){
                LOGGER.log(Level.INFO, "the program takes the best guess as {}, chinese translation is {}",
                        bestGuess.getEngName(), bestGuess.getChnName());
            }
            updateGuess(bestGuess);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void clearCanvas() {
        if(inkFile != null && !inkFile.isDirty()){
            inkFile.setDirty(true);
        }
        graphicsContext.clearRect(0.0, 0.0, canvas.getWidth(), canvas.getHeight());
        strokeTimeUtil = null;
        inkFile.clear();
        updateGuess(null);
        LOGGER.log(Level.INFO, "Canvas cleared");
    }

    public void createFile() {
        if (inkFile.isDirty()) {
            if(!AlertUtil.warn(WINDOW_TITLE_TIP, FILE_NOT_SAVED, null, true)){
                //用户不忽略警告，不执行后续语句，直接返回
                return;
            }
        }
        FileService fileService = serviceFactory.getFileService();
        clearCanvas();
        fileService.createFile();
        inkFile = new InkFile();

    }

    public void openFile() {
        if (inkFile.isDirty()) {
            if(!AlertUtil.warn(WINDOW_TITLE_TIP, FILE_NOT_SAVED, null, true)){
                //用户不忽略警告，不执行后续语句，直接返回
                return;
            }
        }

        FileService fileService = serviceFactory.getFileService();
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(WINDOW_TITLE_OPEN_FILE);
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("OtuaDraw drawing file", "*.otua")
        );
        File jsonFile = fileChooser.showOpenDialog(new Stage());
        if (jsonFile != null) {
            try {
                clearCanvas();
                inkFile = fileService.openFile(jsonFile);
                Long gap = inkFile.getInkTrail().getLastUpdateTime();
                strokeTimeUtil = new StrokeTimeUtil(gap);
                showOnCanvas(inkFile.getInkTrail());
//                inkFile.setDirty(false);
                updateGuess(inkFile.getGuess());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void saveFile() {
        FileService fileService = serviceFactory.getFileService();
        try {
            if (inkFile.isTempFile()) {
                saveFileAs();
            } else {
                inkFile.setDirty(false);
                fileService.saveFile(inkFile);
            }
        } catch (IOException e) {
            AlertUtil.warn(WINDOW_TITLE_TIP,FILE_SAVE_FAILED,null,false);
            e.printStackTrace();
        }
    }

    public void saveFileAs() {
        FileService fileService = serviceFactory.getFileService();
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(WINDOW_TITLE_SAVE_FILE);
        fileChooser.setInitialFileName("涂鸦 " + DateFormatUtil.formatDateTimeString(System.currentTimeMillis()) + ".otua");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("OtuaDraw drawing file", "*.otua")
        );
        File jsonFile = fileChooser.showSaveDialog(new Stage());

        if (jsonFile != null) {
            try {
                inkFile.setDirty(false);
                fileService.saveFile(inkFile, jsonFile);
            } catch (IOException e) {
                AlertUtil.warn(WINDOW_TITLE_TIP,FILE_SAVE_FAILED,null,false);
                e.printStackTrace();
            }
        }

    }

    private InkPoint getInkPoint(double x, double y, StrokeTimeUtil s, PointEnum type) {
        final Integer xPos = (int) Math.floor(x), yPos = (int) Math.floor(y);
        final Long milliseconds = s.getStrokeTIme();
        return new InkPoint(xPos, yPos, milliseconds, type);
    }

    private void showOnCanvas(InkTrail inkTrail) {
        for (int i = 0; i < inkTrail.getTrailLen(); i++) {
            InkPoint p = inkTrail.get(i);
            switch (p.getPointType()){
                case PRESS:
                    graphicsContext.beginPath();
                    graphicsContext.moveTo(p.getX(), p.getY());
                    graphicsContext.stroke();
                    break;
                case DRAG:
                    graphicsContext.lineTo(p.getX(), p.getY());
                    graphicsContext.stroke();
                    graphicsContext.closePath();
                    graphicsContext.beginPath();
                    graphicsContext.moveTo(p.getX(), p.getY());
                    break;
                case RELEASE:
                    graphicsContext.lineTo(p.getX(), p.getY());
                    graphicsContext.stroke();
                    graphicsContext.closePath();
                    break;
            }
        }
    }

    private void updateGuess(ShapeEnum guess){
        String guessName = "...";
        if(guess!=null){
            guessName = guess.getChnName()+"!";
        }
        guessText.setText("我猜这是" + guessName);
    }
}
