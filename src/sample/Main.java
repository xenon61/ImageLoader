package sample;import javafx.application.Application;import javafx.fxml.FXMLLoader;import javafx.scene.Parent;import javafx.scene.Scene;import javafx.scene.control.TextField;import javafx.scene.image.Image;import javafx.stage.Stage;import java.io.*;import java.util.Properties;public class Main extends Application {    static final int appVerMajor = 5;    static final int appVerMinor = 0;    static final int appVerRelease = 0;    static final int appVerBuild = 0;    @Override    public void start(Stage primaryStage) throws Exception {        primaryStage.setTitle("STAGE.close");        primaryStage.setResizable(false);        Parent root = FXMLLoader.load(getClass().getResource("sample.fxml"));        primaryStage.setTitle("Авторизация");        primaryStage.getIcons().add(new Image("file:icons\\login.png"));        Properties save = new Properties();        File s = new File("save.prop");//        System.out.println(s.getAbsolutePath());        TextField loginInit = (TextField)root.lookup("#Login");        if (!s.exists()) {            loginInit.setText("");        } else {            try {                InputStream is = new BufferedInputStream(new FileInputStream(s));                save.load(is);                is.close();            } catch (IOException ioe) {                System.exit(1);            }            loginInit.setText(save.getProperty("login"));        }        primaryStage.setScene(new Scene(root, primaryStage.getWidth(), primaryStage.getHeight()));        Controller.STAGE = primaryStage;        primaryStage.show();        System.out.println(getVersion());        Updater upd = new Updater("ImageLoader", appVerMajor, appVerMinor, appVerRelease);        upd.checkUpdate();        upd.start();    }    public static void main(String[] args) {        launch(args);         }    public static String getVersion() {        return appVerMajor + "." + appVerMinor + "." + appVerRelease + "." + appVerBuild;    }}