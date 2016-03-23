package sample;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.*;
import java.sql.*;
import java.util.Locale;
import java.util.Properties;
import java.util.UUID;


public class Controller {
    public static Stage STAGE; //принимает  Stage из Main.java
    public Button LoginBtn;
    @FXML
    public PasswordField Password;
    public TextField Login;// = new TextField(); //текст бокс логина
    public ComboBox TypeDoc;     //тип документа
    public Button Load;
    public TextField clientId;
    public String clId;
    public Connection connection;
    public JFileChooser fileopen;
    public static String login;
    public static String password;
    public String DocType;
    public TextArea commentText;
    public String comment;
    public File linkfile;
    public GridPane Pane;
    public Button Check;
    public String PATH_SIGNS = null;
    public Label FIO;
    public ProgressIndicator progress;
    public static Label userIdLabel;

    @FXML
    //событие при нажатии на кнопку "войти"
    public void MyClick() throws ClassNotFoundException, IOException {
        login = Login.getText();
//        System.out.println("get Name "+ Login.getText());
        password = Password.getText();


        //---------------сохранение нового логина в файле--------------------

        Properties save = new Properties();
        File s = new File("save.prop");
        if (s.exists()) {
            try {
                OutputStream os = new BufferedOutputStream(new FileOutputStream(s));
                save.setProperty("login", login);
                save.store(os, null);
            } catch (IOException ioe) {
                System.exit(1);
            }
        } else {
            System.exit(1);
        }

        //------------------------------------------------------------------------------------------------------------
        //-------------------------подключение к оракл XE------------------------------------------
        String sid;
        String port;
        String server;
        String dblink;

        //----------загрузка данных для подключения из файла конфигурации.------------------------------
        Properties props = new Properties();
        File f = new File("config.prop");
//        System.out.println(f.getAbsolutePath());
        if (f.exists()) {
            try {
                InputStream is = new BufferedInputStream(new FileInputStream(f));
                props.load(is);
                is.close();
            } catch (IOException ioe) {
                System.exit(1);
            }
            sid = props.getProperty("sid");
            port = props.getProperty("port");
            server = props.getProperty("server");
            dblink = props.getProperty("dblink");
        } else {
            sid = "XE";
            port = "1521";
            server = "xe.kompanion.kg";
            dblink = "@IBSOLINK";
        }
        //-----------------------------------------------------------------------------
        String urlOracle = "jdbc:oracle:thin:@" + server + ":" + port + ":" + sid;
        String driverName = "oracle.jdbc.driver.OracleDriver";

        Class.forName(driverName);
        Locale.setDefault(Locale.ENGLISH);
        try {
            connection = DriverManager.getConnection(urlOracle, login, password);
            //connection = DriverManager.getConnection(new StringBuilder().append("jdbc:oracle:thin:").append(login).append("/").append(password).append("@").append(server).append(":").append(port).append(":").append(sid).toString());
            Parent mainForm = FXMLLoader.load(getClass().getResource("main.fxml")); //создание новой формы

            Stage primaryStage1 = new Stage();
            primaryStage1.setResizable(false);
            primaryStage1.getIcons().add(new Image("file:icons\\upload.png"));
            primaryStage1.setTitle("Импорт изображений");
            primaryStage1.setScene(new Scene(mainForm, primaryStage1.getWidth(), primaryStage1.getHeight()));
            primaryStage1.show();
            STAGE.close();
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
            if (e.getErrorCode() == 17002) {
                JOptionPane.showMessageDialog(null, "Ошибка подключения");
            } else if (e.getErrorCode() == 12505) {
                JOptionPane.showMessageDialog(null, "Ошибка подклчения");
            } else {
                JOptionPane.showMessageDialog(null, "Неправельный логин/пароль\n За помощью обратитесь к администратору");
            }
        }

    }

    //событие при нажатие на кнопку "Загрузить"
    public void Load() throws ClassNotFoundException {
        if (linkfile == null) {
            JOptionPane.showMessageDialog(null, "Выберите картинку!");
        } else {
            progress = new ProgressIndicator();
            Pane.add(progress, 0, 0);
            Thread2 t2 = new Thread2();
            t2.start();
        }

    }

    public void Chose() throws IOException {

//        System.out.println("Chose");
        fileopen = new JFileChooser();     //новое окно выра файлов
        FileNameExtensionFilter filter = new FileNameExtensionFilter("Image files", "jpg", "jpeg");//фильтр выбора файлов
        fileopen.setFileFilter(filter); //приминение фильтра
        fileopen.setAcceptAllFileFilterUsed(false);
        int ret = fileopen.showDialog(null, "Открыть файл");
        if (ret == JFileChooser.APPROVE_OPTION) {
            linkfile = fileopen.getSelectedFile();
            long len = linkfile.length();
            if (len > 524288) {
                JOptionPane.showMessageDialog(null, "Размер файла превышает допустимый!");
                linkfile = null;
            }
            System.out.println(linkfile.getAbsolutePath());
            ImageView ii;
            FileInputStream fis = new FileInputStream(linkfile);
            ii = new ImageView(new Image(fis));
            ii.fitHeightProperty().setValue(180);
            ii.fitWidthProperty().setValue(220);
            Pane.add(ii, 0, 0);
            fis.close();

        } else if (ret == JFileChooser.CANCEL_OPTION) {
            linkfile = null;
        }
    }

    public void Check() throws ClassNotFoundException {
        //              -------------------------подключение к оракл XE------------------------------------------
        String sid;
        String port;
        String server;
        String dblink;
        Properties props;
        props = new Properties();
        File f = new File("config.prop");
        if (f.exists()) {
            try {
                InputStream is = new BufferedInputStream(new FileInputStream(f));
                props.load(is);
                is.close();
            } catch (IOException ioe) {
                System.exit(1);
            }
            sid = props.getProperty("sid");
            port = props.getProperty("port");
            server = props.getProperty("server");
            dblink = props.getProperty("dblink");
        } else {
            sid = "XE";
            port = "1521";
            server = "xe.kompanion.kg";
            dblink = "@IBSOLINK";
        }
        String urlOracle = "jdbc:oracle:thin:@" + server + ":" + port + ":" + sid;
//        System.out.println(urlOracle);
        String driverName = "oracle.jdbc.driver.OracleDriver";
        Class.forName(driverName);
        Locale.setDefault(Locale.ENGLISH);
        Connection conn = null;
        try {
            conn = DriverManager.getConnection(urlOracle, login, password);
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Ошибка подключения");
        }
//        System.out.println("connecting: " + urlOracle);
//       -----------------------------------------------------------------------------------------------------------------
        if (TypeDoc.getValue().toString().equals("ПАСПОРТ")) {
            DocType = "PASSPORT";
        } else if (TypeDoc.getValue().toString().equals("ПОДПИСЬ")) {
            DocType = "SIGN";
        } else if (TypeDoc.getValue().toString().equals("ФОТО")) {
            DocType = "PHOTO";
        } else if (TypeDoc.getValue().toString().equals("ДОВЕРЕННОСТЬ")) {
            DocType = "PROXY";
        }
//        System.out.println("DocType: " + DocType);
        if ((DocType == "PASSPORT")||(DocType == "PHOTO")||(DocType == "SIGN")) {

            if (!clientId.getText().isEmpty()) {
                clId = clientId.getText();

                try {
                    assert conn != null;
                    Statement statO = conn.createStatement();
                    ResultSet result = statO.executeQuery("select C_4,U_2 from IBS.VW_CRIT_FGK_CLIENT_GRPH"+dblink+" where id = " + clId + "");
                    if (result.next()) {
                        PATH_SIGNS = result.getString(2);
                        FIO.setText(result.getString(1));
                        // JOptionPane.showMessageDialog(null, "найден клиент: " + result.getString(1));
                        Load.setDisable(false);
                        conn.close();
                    } else {
                        JOptionPane.showMessageDialog(null, "Клиент с таким айди не найден!");
                        Load.setDisable(true);
                        FIO.setText("");
                    }
                } catch (SQLException ignored) {
                    JOptionPane.showMessageDialog(null, "Клиент с таким айди не найден! ORA-0" + ignored.getErrorCode() +" "+ ignored.getMessage());
                    if (ignored.getErrorCode() == 1017) {
                        JOptionPane.showMessageDialog(null, "Срок действия вашего пароля истек. Измените пароль");
                    }
                    Load.setDisable(true);
                    FIO.setText("");
                }

            } else {
                JOptionPane.showMessageDialog(null, "Клиент с таким айди не найден!");
                Load.setDisable(true);
                FIO.setText("");
            }
        } else if (DocType == "PROXY") {
            // Доверенность и ее поиск по ID
            if (!clientId.getText().isEmpty()) {
                clId = clientId.getText();

                try {
                    assert conn != null;
                    Statement statO = conn.createStatement();
                    ResultSet result = statO.executeQuery("select C_2,U_1 from IBS.VW_CRIT_FGK_TRUST_FOR_PROD"+dblink+" where id = " + clId + "");
                    if (result.next()) {
                        PATH_SIGNS = result.getString(2);
                        FIO.setText("Доверенность №"+result.getString(1));
                        // JOptionPane.showMessageDialog(null, "найден клиент: " + result.getString(1));
                        Load.setDisable(false);
                        conn.close();
                    } else {
                        JOptionPane.showMessageDialog(null, "Доверенность с таким айди не найдена!");
                        Load.setDisable(true);
                        FIO.setText("");
                    }
                } catch (SQLException ignored) {
                    JOptionPane.showMessageDialog(null, "Доверенность с таким айди не найдена! ORA-0" + ignored.getErrorCode()  +" "+ ignored.getMessage());
                    if (ignored.getErrorCode() == 1017) {
                        JOptionPane.showMessageDialog(null, "Срок действия вашего пароля истек. Измените пароль");
                    }
                    Load.setDisable(true);
                    FIO.setText("");
                }

            } else {
                JOptionPane.showMessageDialog(null, "Доверенность с таким айди не найдена!");
                Load.setDisable(true);
                FIO.setText("");
            }
        }
    }

    public class Thread2 extends Thread {
        public void run() {
            //              -------------------------подключение к оракл XE------------------------------------------
            String sid;
            String port;
            String server;
            String dblink;
            Properties props;
            props = new Properties();
            File f = new File("config.prop");
            if (f.exists()) {
                try {
                    InputStream is = new BufferedInputStream(new FileInputStream(f));
                    props.load(is);
                    is.close();
                } catch (IOException ioe) {
                    System.exit(1);
                }
                sid = props.getProperty("sid");
                port = props.getProperty("port");
                server = props.getProperty("server");
                dblink = props.getProperty("dblink");
            } else {
                sid = "XE";
                port = "1521";
                server = "10.200.254.134";
                dblink = "@DEVLINK";
            }
            String urlOracle = "jdbc:oracle:thin:@" + server + ":" + port + ":" + sid;
//            System.out.println(urlOracle);
            String driverName = "oracle.jdbc.driver.OracleDriver";
            try {
                Class.forName(driverName);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
            Locale.setDefault(Locale.ENGLISH);
            Connection conn = null;
            try {
                conn = DriverManager.getConnection(urlOracle, login, password);
            } catch (SQLException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null, "Ошибка подключения");
            }
//            System.out.println("connecting: " + urlOracle);
//       -----------------------------------------------------------------------------------------------------------------

            UUID uuid = UUID.randomUUID();
            if (!commentText.getText().equals("")) {
                comment = commentText.getText();
            } else {
                comment = "null";
            }
            if (commentText.getText().length() > 1000) {
                JOptionPane.showMessageDialog(null, "Превышен допустимый размер комментария. Не более 1000 символов.");
            }

            PreparedStatement pstmt;
            try {
                String pathToFile;
                assert conn != null;
                pstmt = conn.prepareStatement("INSERT INTO CFTADMIN.BLOB_TBL VALUES('" + uuid + "',?," + clId + ",'" + DocType + "','" + PATH_SIGNS + "',SYSDATE,'" + login + "','" + comment  +"', null )");

                InputStream instreamApp;
                instreamApp = new FileInputStream(linkfile);
                pstmt.setBinaryStream(1, instreamApp, (int) linkfile.length());
                pstmt.executeUpdate();
                instreamApp.close();

                String ftpIp;
                String ftpUser;
                String ftpPass;

                try {
                    assert conn != null;
                    Statement statO = conn.createStatement();
                    ResultSet result = statO.executeQuery("select C_1,C_2,C_3 from IBS.VW_CRIT_FGK_FTP_CONN"+dblink);
                    if (result.next()) {
                        ftpIp = result.getString(1);
                        ftpUser = result.getString(2);
                        ftpPass = result.getString(3);
                    } else {
                        JOptionPane.showMessageDialog(null, "Не найден ФТП для подключения! Проверьте доступ на представление ФТП!");
                        ftpIp = "";
                        ftpPass = "";
                        ftpUser = "";
                    }
                } catch (SQLException ignored) {
                    JOptionPane.showMessageDialog(null, "Не найден ФТП для подключения! ORA-0" + ignored.getErrorCode());
                    ftpIp = "";
                    ftpPass = "";
                    ftpUser = "";
                }

                CallableStatement cstmt = conn.prepareCall("{? = call CFTADMIN.FUNCTION_BLOB(?,?,?,?)}");
                cstmt.registerOutParameter(1, Types.VARCHAR);
                cstmt.setInt(2, 1);
                cstmt.setNString(3, ftpUser);
                cstmt.setNString(4, ftpPass);
                cstmt.setNString(5, ftpIp);

                cstmt.executeUpdate();
                pathToFile = cstmt.getString(1);

//                System.out.println(">>"+pathToFile);
//                Date curTime = new Date(System.currentTimeMillis());
//                SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
//                String sDate = sdf.format(curTime);
//                pathToFile = "./"+sDate+"/"+uuid+".jpg";

                //pathToFile = "";

                CftInterface addEmpty = new CftInterface("PATT_SIGNS", "ADD_BLOB", dblink);
                addEmpty.addParameter(String.valueOf(uuid));
                addEmpty.addParameter(comment);
                addEmpty.addParameter(pathToFile);
                addEmpty.execute(conn);

                conn.close();
                Load.setDisable(true);
                clientId.setText("");
                commentText.setText("");
                linkfile = null;
                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        FIO.setText("");
                        progress.toBack();
                        Pane.getChildren().clear();
                    }
                });
            } catch (SQLException | IOException e1) {
                e1.printStackTrace();
            }

//        // Вариант через edtftpj.jar
//            String host = "192.168.22.227";
//            String username = "test";
//            String password = "123456";
//            try {
//                // create client
//                FileTransferClient ftp = new FileTransferClient();
//                // set remote host
//                ftp.setRemoteHost(host);
//                ftp.setUserName(username);
//                ftp.setPassword(password);
//                // connect to the server
//                ftp.connect();
//                System.out.println("Uploading file");
//                ftp.uploadFile(linkfile.getAbsolutePath(),filename);
//                System.out.println("File uploaded");
//                // Shut down client
//                ftp.disconnect();
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//            // Отключение от ftp сервера


        }
    }
}