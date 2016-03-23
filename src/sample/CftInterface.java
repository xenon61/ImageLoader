package sample;

import java.sql.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Интерфейс для взаимодействия с ЦФТ операциями
 * Класс обеспечивающий взаимодействие с интерфейсами операций ЦФТ
 * Для вызова операции необходимо знать, какому типу относится, её имя и
 * dblink
 * Вызов интерфейсной операции происходит в методе execute
 * Происходит поиск операции, и вызов её интерфейса. В интерфейс параметры
 * передаются одной строкой, с индивидуальными разделителями. В интерфейсе
 * операции происходит парсинг этой строки и передача параметров
 * непосредственно в операцию
 * <p/>
 * В классе операции addParameter добавляют к строке, передаваемой в
 * интерфейс пакета указанный параметр
 *
 * @author asysoev
 */
public class CftInterface {
    String typeName;
    String operationName;
    String params;
    String result;
    int paramNum;
    String dbLinkName;
    /**
     * Конструктор
     *
     * @param typeName      - Наименование типа, которому относится операция
     * @param operationName - Короткое наименование операции
     */
    public CftInterface(String typeName, String operationName, String dbLink) {
        this.typeName = typeName;
        this.operationName = operationName;
        paramNum = 11;
        params = "''";
        dbLinkName=dbLink;
    }

    /**
     * Добавить параметр к строке параметров для интерфейсного пакета
     *
     * @param param
     */
    public void addParameter(String param) {

        if (paramNum == 11) {
            params = "";
            params += "\nchr(27)||chr(" + paramNum + ")||'" + (param == null ? "" : param) + "'\n";
        } else
            params += "||chr(27)||chr(" + paramNum + ")||'" + (param == null ? "" : param) + "'\n";
        paramNum += 1;
    }

    /**
     * Добавить параметр к строке параметров для интерфейсного пакета
     *
     * @param paramNum
     */
    public void setParamNum(int paramNum) {
        this.paramNum = paramNum;
    }

    /**
     * Добавить параметр к строке параметров для интерфейсного пакета
     *
     * @param param
     */
    public void addParameter(Integer param) {
        if (paramNum == 11) {
            params = "";
            params += "\nchr(27)||chr(" + paramNum + ")||'" + (param == null ? "" : param) + "'\n";
        } else
            params += "||chr(27)||chr(" + paramNum + ")||'" + (param == null ? "" : param) + "'\n";
        paramNum += 1;
    }
    /**
     * Добавить параметр к строке параметров для интерфейсного пакета
     *
     * @param param
     */
    public void addParameter(Character param) {
        if (paramNum == 11) {
            params = "";
            params += "\nchr(27)||chr(" + paramNum + ")||'" + (param == null || param == Character.UNASSIGNED ? "0" : param) + "'\n";
        } else
            params += "||chr(27)||chr(" + paramNum + ")||'" + (param == null || param == Character.UNASSIGNED ? "0" : param) + "'\n";
        paramNum += 1;
    }

    /**
     * Добавить параметр к строке параметров для интерфейсного пакета
     *
     * @param param
     */
    public void addParameter(Date param) {
        SimpleDateFormat DDMMYYYY = new SimpleDateFormat("yyyyMMddHHmmss");
        String paramFormat = param == null ? "''" : "'" + DDMMYYYY.format(param) + "'";

        if (paramNum == 11) {
            params = "";
            params += "\nchr(27)||chr(" + paramNum + ")||" + paramFormat + "\n";
        } else
            params += "||chr(27)||chr(" + paramNum + ")||" + paramFormat + "\n";
        paramNum += 1;
    }

    public Date getResultDate() throws ParseException {
        return new SimpleDateFormat("yyyyMMddHHmmss").parse(result);
    }

    /**
     * Если у интерфейсной опеарации есть возвращаемое значение,
     * то после его выполнения можно его получить этой операцией
     *
     * @return String. Возвращает результат выполнения операции ЦФТ
     */
    public String getResult() {
        return result;
    }

    /**
     * Метод выполняет вызов операции ЦФТ через интерфейсный пакет
     *
     * @param conn соединение с ЦФТ
     * @throws java.sql.SQLException
     */
    public void execute(Connection conn) throws SQLException {
        Statement stmtOpen = conn.createStatement();
        stmtOpen.execute("Select ibs.executor.lockopen"+dbLinkName+"() From dual");
//        System.out.println("Select ibs.executor.lockopen"+dbLinkName+"() From dual");
        PreparedStatement stmt = conn.prepareStatement("select id from ibs.methods"+dbLinkName+" where short_name=? and class_id=?");
        stmt.setString(1, operationName);
        stmt.setString(2, typeName);
//        System.out.println("operationName="+operationName+" typeName="+typeName);
        ResultSet rs = stmt.executeQuery();
        String methID = "";
        while (rs.next()) {
            methID = rs.getString("ID");
            break;
        }
//        System.out.println("methID - "+methID);
//        System.out.println("params - "+params);

        if (!methID.equals("")) {
            // Вызов ЦФТшной операции через интерфейсный пакет
            String cmd =
                    "DECLARE\n"
                            + "  R VARCHAR2(1);\n"
                            + "  v_char VARCHAR2(1) := chr(27);\n"
                            + "  v_params VARCHAR2(32000);\n"
                            + "  v_typeName VARCHAR2(200);\n"
                            + "BEGIN\n"
                            + "  --validate section\n"
                            + "  v_typeName:='" + typeName + "';"
                            + "  v_params:=" + params + ";"
                            + "  ibs.Z$U$" + methID + ".S"+dbLinkName+"(\n"
                            + "              v_char||chr(1)\n"
                            + "              ||v_char||chr(4)||v_typeName\n"
                            + "              ||v_char||chr(5)||'0'\n"
                            + "              ||v_char||chr(6)||'1'\n"
                            + "              ||v_char||chr(2)||'DEFAULT'\n"
                            + "              ||v_char||chr(3));\n"
                            + "  -- process\n"
                            + "  ibs.Z$U$" + methID + ".P"+dbLinkName+"(0,R,4);\n"
                            //+"  --execute section\n"
                            //+"  -- set parameter values\n"
                            + "  IBS.Z$U$" + methID + ".S"+dbLinkName+"(v_params);\n"
                            + "  --process\n"
                            + "  IBS.Z$U$" + methID + ".P"+dbLinkName+"(1,R,1);\n"
                            + "  -- get result\n"
                            + " ? := ibs.Z$U$" + methID + ".G"+dbLinkName+"(R);\n"
                            //+"  commit;\n"
                            + "END;";
            // Вызов операции-конструктора
//            System.out.println("*************cmd - "+cmd);
            CallableStatement proc = conn.prepareCall(cmd);
            proc.registerOutParameter(1, oracle.jdbc.OracleTypes.VARCHAR);
            proc.execute();
            result = proc.getString(1);
//            System.out.println("result ="+result);
            if (result != null) {
                int i = result.indexOf('\u001b', 2);
                if (i > 0)
                    result = result.substring(2, i);
                else
                    result = result.substring(2);
            }
            proc.close();
            params = "";
        }
    }

}
