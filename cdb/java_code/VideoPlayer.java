import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import java.io.File;

public class VideoPlayer extends Application {
    private Stage secondStage;
    private boolean secondWindowCreated = false;
    private double xOffset = 0;
    private double yOffset = 0;
    
    @Override
    public void start(Stage primaryStage) {
        try {
            // 设置无装饰窗口
            primaryStage.initStyle(StageStyle.UNDECORATED);
            
            // 获取目录路径
            File directory = new File("cdb/animation");
            System.out.println("正在查找视频文件夹: " + directory.getAbsolutePath());
            
            // 列出所有视频文件
            File[] files = directory.listFiles((dir, name) -> 
                name.toLowerCase().endsWith(".mp4") || 
                name.toLowerCase().endsWith(".avi") ||
                name.toLowerCase().endsWith(".mkv"));
            
            if (files == null || files.length < 5) {
                System.out.println("文件夹中没有足够的视频文件，找到: " + (files == null ? "0" : files.length));
                return;
            }
            
            // 获取第 5 个视频文件（索引 4，因为数组从 0 开始）
            File videoFile = files[4];
            System.out.println("正在加载视频文件: " + videoFile.getAbsolutePath());
            
            // 创建媒体
            Media media = new Media(videoFile.toURI().toString());
            MediaPlayer mediaPlayer = new MediaPlayer(media);
            MediaView mediaView = new MediaView(mediaPlayer);
            
            // 设置媒体视图
            mediaView.setFitWidth(600);
            mediaView.setFitHeight(400);
            mediaView.setPreserveRatio(true);
            
            // 设置播放速率为0.8
            mediaPlayer.setRate(0.8);
            
            // 创建自定义标题栏（只有关闭按钮）
            HBox titleBar = createMinimalTitleBar(primaryStage);
            
            // 创建内容区域
            StackPane contentArea = new StackPane();
            contentArea.setStyle("-fx-background-color: black;");
            contentArea.getChildren().add(mediaView);
            
            // 使用BorderPane布局，顶部是标题栏，中间是内容
            BorderPane root = new BorderPane();
            root.setTop(titleBar);
            root.setCenter(contentArea);
            root.setStyle("-fx-background-color: black;");
            
            Scene scene = new Scene(root, 1000, 600);
            scene.setFill(Color.BLACK);
            
            // 设置阶段
            primaryStage.setScene(scene);
            
            // 获取屏幕尺寸
            Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
            
            // 将第一个窗口放在屏幕中央
            double primaryX = (screenBounds.getWidth() - 1000) / 2;  // 水平居中
            double primaryY = (screenBounds.getHeight() - 600) / 2;  // 垂直居中
            
            primaryStage.setX(primaryX);
            primaryStage.setY(primaryY);
            
            primaryStage.show();
            System.out.println("第一个窗口位置: x=" + primaryX + ", y=" + primaryY);
            
            // 错误处理
            mediaPlayer.setOnError(() -> {
                System.out.println("媒体播放器错误: " + mediaPlayer.getError());
            });
            
            // 获取视频总时长并设置监听
            mediaPlayer.setOnReady(() -> {
                try {
                    System.out.println("媒体已准备就绪");
                    Duration totalDuration = media.getDuration();
                    System.out.println("视频总时长: " + totalDuration.toSeconds() + " 秒");
                    
                    // 设置一个特定时间点来创建第二个窗口 - 视频结束前0.1秒
                    double preEndTimeSeconds = totalDuration.toSeconds() - 0.1;
                    System.out.println("将在视频播放到 " + preEndTimeSeconds + " 秒时创建新窗口（视频结束前0.1秒）");
                    
                    // 使用setStopTime方法在视频结束前停止
                    mediaPlayer.setStopTime(totalDuration);
                    
                    // 添加周期性检查 - 使用更高频率的检查以确保不会错过0.2秒的时间点
                    javafx.animation.AnimationTimer timer = new javafx.animation.AnimationTimer() {
                        @Override
                        public void handle(long now) {
                            Duration currentTime = mediaPlayer.getCurrentTime();
                            // 如果当前时间大于预设时间点且第二个窗口尚未创建
                            if (currentTime != null && currentTime.toSeconds() >= preEndTimeSeconds && !secondWindowCreated) {
                                secondWindowCreated = true; // 标记窗口已创建，防止重复创建
                                System.out.println("触发创建新窗口，当前时间: " + currentTime.toSeconds() + 
                                                  "，距离视频结束还有: " + (totalDuration.toSeconds() - currentTime.toSeconds()) + " 秒");
                                
                                // 计算第二个窗口位置 - 也放在屏幕中央
                                double secondX = (screenBounds.getWidth() - 1000) / 2;  // 水平居中
                                double secondY = (screenBounds.getHeight() - 600) / 2;  // 垂直居中
                                
                                // 创建新窗口
                                final double finalSecondX = secondX;
                                final double finalSecondY = secondY;
                                Platform.runLater(() -> {
                                    createNewWindow(finalSecondX, finalSecondY);
                                });
                                
                                // 任务完成，停止计时器
                                this.stop();
                            }
                        }
                    };
                    timer.start();
                    
                } catch (Exception e) {
                    System.out.println("设置视频准备就绪回调时出错: " + e.getMessage());
                    e.printStackTrace();
                }
            });
            
            // 添加视频结束监听器
            mediaPlayer.setOnEndOfMedia(() -> {
                System.out.println("视频播放结束");
                // 停止播放并释放资源
                mediaPlayer.stop();
                mediaPlayer.dispose();
                
                // 关闭当前窗口
                primaryStage.close();
                
                // 如果第二个窗口已经创建，则将其置于前台
                if (secondStage != null) {
                    secondStage.toFront();
                    System.out.println("将第二个窗口置于前台");
                } else {
                    System.out.println("警告：视频结束但第二个窗口未创建，现在创建");
                    // 如果第二个窗口未创建，则在此处创建（作为备份措施）
                    // 计算第二个窗口位置 - 放在屏幕中央
                    double secondX = (screenBounds.getWidth() - 1000) / 2;
                    double secondY = (screenBounds.getHeight() - 600) / 2;
                    Platform.runLater(() -> {
                        createNewWindow(secondX, secondY);
                    });
                }
            });
            
            // 播放视频
            mediaPlayer.play();
            System.out.println("开始播放视频");
            
        } catch (Exception e) {
            System.out.println("发生异常: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    // 创建最小化标题栏（只有关闭按钮）
    private HBox createMinimalTitleBar(Stage stage) {
        HBox titleBar = new HBox();
        titleBar.setAlignment(Pos.CENTER_RIGHT);
        titleBar.setPadding(new Insets(5, 10, 5, 10));
        titleBar.setStyle("-fx-background-color: black;");
        
        // 添加一个空的占位区域，用于拖动窗口
        HBox spacer = new HBox();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        titleBar.getChildren().add(spacer);
        
        // 添加关闭按钮
        Button closeButton = new Button("X");
        closeButton.setStyle("-fx-background-color: black; -fx-text-fill: white; -fx-border-color: transparent;");
        closeButton.setOnAction(e -> stage.close());
        titleBar.getChildren().add(closeButton);
        
        // 添加拖动功能（整个标题栏都可以用来拖动窗口）
        titleBar.setOnMousePressed(event -> {
            xOffset = event.getSceneX();
            yOffset = event.getSceneY();
        });
        
        titleBar.setOnMouseDragged(event -> {
            stage.setX(event.getScreenX() - xOffset);
            stage.setY(event.getScreenY() - yOffset);
        });
        
        return titleBar;
    }
    
    // 创建新窗口的方法
    private void createNewWindow(double x, double y) {
        try {
            System.out.println("正在创建新窗口，位置: x=" + x + ", y=" + y);
            secondStage = new Stage();
            secondStage.initStyle(StageStyle.UNDECORATED); // 无装饰窗口
            
            // 创建自定义标题栏（只有关闭按钮）
            HBox titleBar = createMinimalTitleBar(secondStage);
            
            // 创建内容区域
            StackPane contentArea = new StackPane();
            contentArea.setStyle("-fx-background-color: black;");
            
            // 创建标签但初始为空
            Label label = new Label("");
            label.setFont(new Font(24));
            label.setTextFill(Color.WHITE);
            contentArea.getChildren().add(label);
            
            // 使用BorderPane布局，顶部是标题栏，中间是内容
            BorderPane root = new BorderPane();
            root.setTop(titleBar);
            root.setCenter(contentArea);
            root.setStyle("-fx-background-color: black;");
            
            Scene newScene = new Scene(root, 1000, 600);
            newScene.setFill(Color.BLACK);
            
            // 设置新窗口
            secondStage.setScene(newScene);
            
            // 设置新窗口位置
            secondStage.setX(x);
            secondStage.setY(y);
            
            // 显示新窗口
            secondStage.show();
            System.out.println("新窗口已创建并显示在位置: x=" + x + ", y=" + y);
            
            // 设置文字逐渐显示的动画
            String fullText = "正在准备游戏！请稍等...";
            animateText(label, fullText, 200); // 每200毫秒显示一个字符
            
        } catch (Exception e) {
            System.out.println("创建新窗口时出错: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    // 文字逐渐显示的动画方法
    private void animateText(Label label, String fullText, int msPerChar) {
        final int[] charIndex = {0};
        Timeline timeline = new Timeline();
        KeyFrame keyFrame = new KeyFrame(
            Duration.millis(msPerChar),
            event -> {
                if (charIndex[0] < fullText.length()) {
                    label.setText(fullText.substring(0, charIndex[0] + 1));
                    charIndex[0]++;
                } else {
                    timeline.stop();
                }
            }
        );
        timeline.getKeyFrames().add(keyFrame);
        timeline.setCycleCount(fullText.length());
        timeline.play();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
