/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package sample;

import oracle.jdbc.OracleResultSet;
import oracle.sql.BLOB;

import javax.swing.*;
import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.*;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * класс проверяет обновления во время работы программы параллельным процессом
 *
 * @author sysoev
 */
public class Updater extends Thread {
    private int appVerMajor;
    private int appVerMinor;
    private int appVerRelease;
    private String appName;
    private static String appPath = null;

    /**
     * Конструктор
     * Версия программы складывается из символов major.minor.release
     *
     * @param major
     * @param minor
     * @param release
     */
    public Updater(String appName, int major, int minor, int release) {
        this.appVerMajor = major;
        this.appVerMinor = minor;
        this.appVerRelease = release;
        this.appName = appName;
        this.setDaemon(true);
    }


    /**
     * Запуск обновлятора программы
     *
     * @param conn
     * @param id
     */
    public static void runUpdater(Connection conn, long id) {
        BLOB blob = null;

        // Удаляем файлы, если они остались после обновления
        delete("ImageLoader.rar");
        delete("Updater.rar");
        // Удалить предыдущий обновлятор
        delete("Updater.jar");
        try {
            // Ищем обновлятор в таблице на сервере.
            String sql = "SELECT U.APP FROM DEV.APPUPDATES U " +
                    " WHERE lower(U.EXE) = lower('Updater.jar') " +
                    " ORDER BY RELEASE_DATE DESC ";
            java.sql.Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            while (rs.next()) {
                // Берем самый свежий и выходим из цикла
                blob = ((OracleResultSet) rs).getBLOB(1);
                break;
            }

            // Если BLOB пустой, значит Updater не найден, выходим из процедуры
            if (blob == null) {
                JOptionPane.showMessageDialog(null, "Обновлятор не найден. Обновление не возможно.\nНажмите ОК для продолжения.");
                return;
            }


            // Создаем поток для вытягивания файла
            InputStream inStrm = blob.getBinaryStream();
            // Создаем локальный файл, в который будем записывать
            File f = new File(appPath() + "Updater.rar");
            FileOutputStream foutStrm = new FileOutputStream(f);
            int length = -1;
            int buffSize = blob.getBufferSize();
            byte[] buffer = new byte[buffSize];
            // Сохраняем файл в текущую папку
            while ((length = inStrm.read(buffer)) != -1) {
                foutStrm.write(buffer, 0, length);
                foutStrm.flush();
            }

            // Закрываем потоки
            inStrm.close();
            foutStrm.close();
            // Extract updater
        /*
         * Скопировал класс de.innosystec.unrar.testutil.ExtractArchive, потому что потребовалось
	     * внести небольшие изменения в код, в частности
	     * в процедуре public static void extractArchive(File archive, File destination) {
	     * перед выходом была добавлена строка
	     * arch.close();
	     * так как без закрытия не получалось удалить архив.
	     */
            // JOptionPane.showMessageDialog(null,appPath() + f.getName()+"  -  "+ appPath());
            ExtractArchive.extractArchive(appPath() + f.getName(), appPath());//".");
            //  ExtractArchive.extractArchive(appPath() + f.getName(), "C:\\Users\\ppetrashov\\IdeaProjects\\ImageLoader\\out\\artifacts\\ImageLoader");//".");
            System.out.println(appPath() + f.getName()+"  -  "+ appPath());

            // delete updater archive
            delete(f.getName());

            String connStr = "xe.kompanion.kg";

            // Запускаем обновлятор
            ProcessBuilder pb = new ProcessBuilder(
                    "java"
                    , "-jar"
                    , appPath() + "Updater.jar"
                    , "ImageLoader.jar"
                    , Long.toString(id)
                    , connStr
                    , "jdbc:oracle:thin:@10.200.254.32:1521:XE"
            );
            pb.directory(new File(appPath()));
            pb.start();
            System.out.println("pb.start");
            //JOptionPane.showMessageDialog(null,pb.directory());
            // JOptionPane.showMessageDialog(null,pb.toString());
            // Выход из программы
            System.exit(0);

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(null, "ошибка SQLException:" + ex.getMessage());
        } catch (FileNotFoundException ex) {
            JOptionPane.showMessageDialog(null, "ошибка FileNotFoundException:" + ex.getMessage());
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(null, "ошибка IOException:" + ex.getMessage());
        }
    }


    /*
     * Проверка обновления программы
     */
    public void checkUpdate() {
        System.out.println("    checkUpdate " + appVerMajor + "." + appVerMinor + "." + appVerRelease);
        Connection conn = null;
        Locale dflt = null;
        try {
            // Подключаемся к серверу обновления программы
            try {

                dflt = Locale.getDefault();
                Locale.setDefault(new Locale("en", "EN"));// Без этого может ругнуться на NLS параметры

                // Проверяем доступен ли сервер обновлений
                String connStr = "xe.kompanion.kg";
                boolean isReached = InetAddress.getByName(connStr).isReachable(10000);
                // Если не допступен, то продолжаем обычный запуск программы
                if (!isReached) {
                    Locale.setDefault(dflt);
                    System.out.println("    return ");
                    return;
                }
                System.out.println("    isReached " + isReached);
                conn = DriverManager.getConnection("jdbc:oracle:thin:@10.200.254.32:1521:XE", "upd", "upd");
                //conn = mo.app.Application.getInstance().getCftConnection(mo.app.Application.getInstance().CFT_CONN_STRING, "upd", "upd");

                // it's faster when autocommit is false
                conn.setAutoCommit(false);
                Locale.setDefault(dflt);
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(null, "Сбой при подключении для проверки обновления.\n" + ex.getMessage() + "\nНажмите ОК для продолжения.", "Ошибка", JOptionPane.ERROR_MESSAGE);
                System.out.println("ErrorCode " + ex.getErrorCode());
                System.out.println("LocalizedMessage " + ex.getLocalizedMessage());
                Locale.setDefault(dflt);
                return;
            } catch (UnknownHostException ex) {
                JOptionPane.showMessageDialog(null, "Ошибка при подключении к серверу обновлений программы ImageLoader '" + ex.getMessage() + "'\nНажмите ОК для продолжения.", "Ошибка", JOptionPane.ERROR_MESSAGE);
                Locale.setDefault(dflt);
                return;
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(null, "Не могу найти файл настроек.\nНажмите ОК для продолжения.\n" + ex.getMessage(), "Ошибка", JOptionPane.ERROR_MESSAGE);
                Locale.setDefault(dflt);
                return;
            }

            // формируем запрос обновления
            System.out.println("    // формируем запрос обновления ");
            String cmd = "{? = call DEV.CHECKUPD(appName=>?, p_verMajor=>?, p_verMinor=>?,p_verRelease=>?, lastVer=>?, lastVerDate=>?, isCriticalUpd=>?)}";
            //String cmd = "{? = call DEV.CHECKUPD(?, ?, ?, ?, ?, ?)}";
            CallableStatement stmt = conn.prepareCall(cmd);
            // Регистрируем обратные параметры, а также значения передаваемых параметров
            stmt.registerOutParameter(1, oracle.jdbc.OracleTypes.NUMBER);
            stmt.setString(2, appName);//(new File(mainFrame.class.getProtectionDomain().getCodeSource().getLocation().getPath())).getName()
            stmt.setInt(3, appVerMajor);
            stmt.setInt(4, appVerMinor);
            stmt.setInt(5, appVerRelease);
            stmt.registerOutParameter(6, oracle.jdbc.OracleTypes.VARCHAR);
            stmt.registerOutParameter(7, oracle.jdbc.OracleTypes.DATE);
            stmt.registerOutParameter(8, oracle.jdbc.OracleTypes.INTEGER);

            // Запуск
            stmt.execute();
            // Получаем результат
            long id = stmt.getLong(1);

            if (id > 0) {// если обновление есть
                String version = stmt.getString(6);
                java.sql.Timestamp dt = stmt.getTimestamp(7);
                int isCritical = stmt.getInt(8);

                if (isCritical == 1) {
                    JOptionPane.showMessageDialog(null, "Доступно критичное обновление программы.\nСейчас будет произведено обновление, пожалуйста подождите.");
                    runUpdater(conn, id);
                } else {
                    int confirmed = JOptionPane.showConfirmDialog(null, "Доступна новая версия программы " + version + " от даты " + dt.toString()
                            + "\nПроизвести обновление?", "Обновление", JOptionPane.YES_NO_OPTION);
                    if (confirmed == JOptionPane.YES_OPTION)
                        runUpdater(conn, id);
                    else return;
                }
            } else if (id < 0) {// ошибка на сервере при поиске обновления
                JOptionPane.showMessageDialog(null, "При проверке обновления, произошла ошибка на стороне сервера.\nНажмите ОК для продолжения.");
            }

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(null, "При проверке обновления произошла ошибка.\n" + ex.getMessage());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(null, "Фатальная ошибка.\n" + ex.getMessage());
        } finally {
            try {
                if (conn != null)
                    conn.close();
            } catch (SQLException ex) {
            }
        }
    }


    /**
     * Переопределяем запуск потока, где и проверяем обновление
     */
    @Override
    public void run() {
        while (true) {
            try {
                //проверяем обновление каждые полчаса
                //sleep(1800000);
                sleep(10000);
                Connection conn = null;
                Locale dflt = null;
                try {
                    // Подключаемся к серверу обновления программы
                    try {

                        dflt = Locale.getDefault();
                        Locale.setDefault(new Locale("en", "EN"));// Без этого может ругнуться на NLS параметры

                        // Проверяем доступен ли сервер обновлений

                        // boolean isReached = InetAddress.getByName(mo.app.Application.getInstance().CONN_STRING_HOST).isReachable(10000);
                        String connStr = "xe.kompanion.kg";
                        boolean isReached = InetAddress.getByName(connStr).isReachable(10000);
                        // Если не допступен, то продолжаем обычный запуск программы
                        if (!isReached) {
                            Locale.setDefault(dflt);
                            continue;
                        }

                        //conn = DriverManager.getConnection(props.getProperty("connString"), "upd", "upd");
                        //conn = mo.app.Application.getInstance().getCftConnection(mo.app.Application.getInstance().CFT_CONN_STRING, "upd", "upd");
                        conn = DriverManager.getConnection("jdbc:oracle:thin:@10.200.254.32:1521:XE", "upd", "upd");
                        // it's faster when autocommit is false
                        conn.setAutoCommit(false);
                        Locale.setDefault(dflt);
                    } catch (Exception ex) {
                        continue;
                    }

                    // формируем запрос обновления
                    String cmd = "{? = call DEV.CHECKUPD(appName=>?, p_verMajor=>?, p_verMinor=>?,p_verRelease=>?, lastVer=>?, lastVerDate=>?, isCriticalUpd=>?)}";
                    //String cmd = "{? = call DEV.CHECKUPD(?, ?, ?, ?, ?, ?)}";
                    CallableStatement stmt = conn.prepareCall(cmd);
                    // Регистрируем обратные параметры, а также значения передаваемых параметров
                    stmt.registerOutParameter(1, oracle.jdbc.OracleTypes.NUMBER);
                    stmt.setString(2, appName);//(new File(mainFrame.class.getProtectionDomain().getCodeSource().getLocation().getPath())).getName()
                    stmt.setInt(3, appVerMajor);
                    stmt.setInt(4, appVerMinor);
                    stmt.setInt(5, appVerRelease);
                    stmt.registerOutParameter(6, oracle.jdbc.OracleTypes.VARCHAR);
                    stmt.registerOutParameter(7, oracle.jdbc.OracleTypes.DATE);
                    stmt.registerOutParameter(8, oracle.jdbc.OracleTypes.INTEGER);

                    // Запуск
                    stmt.execute();
                    // Получаем результат
                    long id = stmt.getLong(1);

                    if (id > 0) {// если обновление есть
                        String version = stmt.getString(6);
                        java.sql.Timestamp dt = stmt.getTimestamp(7);

                        int confirmed = JOptionPane.showConfirmDialog(null, "Доступна новая версия программы " + version + " от даты " + dt.toString()
                                + "\nЗакрыть программу и произвести обновление сейчас?" + "\nВНИМАНИЕ!!! Не сохраненные данные будут утеряны.", "Обновление", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
                        if (confirmed == JOptionPane.YES_OPTION)
                            runUpdater(conn, id);
                        else continue;
                    }

                } catch (Exception ex) {
                    continue;
                } finally {
                    try {
                        if (conn != null)
                            conn.close();
                    } catch (Exception ex) {
                    }
                }
            } catch (InterruptedException ex) {
                Logger.getLogger(Updater.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public static void delete(String file) {
        // A File object to represent the filename
        File f = new File(file);

        // Make sure the file or directory exists and isn't write protected
        if (!f.exists()) {
            //throw new IllegalArgumentException("Не найден файл " + file);
            return;
        }

        if (!f.canWrite()) {
            //throw new IllegalArgumentException("Невозможно удалить файл : "+ file);
            return;
        }

        // If it is a directory, delete subsequently all subdirecoties and files
        if (f.isDirectory()) {
            String[] files = f.list();
            if (files.length > 0) {
                //throw new IllegalArgumentException("Directory not empty: " + file);
                for (int i = 0; i < files.length; i++)
                    delete(files[i]);
            }
        }

        //this.txtaOutput.setText(this.txtaOutput.getText()+"\nУдаление файла "+f.getName());
        // Attempt to delete it
        boolean success = f.delete();
        if (!success) {
            //this.txtaOutput.setText(this.txtaOutput.getText()+"\nНе удалось удалить файл"+f.getName()+".");
        }
    }

    public static String appPath() {
        if (appPath != null)
            return appPath;
        else return System.getProperty("user.dir") + "/";
    }
}
