/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package PHilae;

import APX.EICodes;
import APX.BRCrypt;
import APX.PHController;
import FILELoad.*;
import Mobile.EIBillerCode;
import Mobile.EIProCodesMOB;

import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Objects;
import java.util.StringTokenizer;
import javax.swing.JComboBox;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 *
 * @author Pecherk
 */
public class TDClient
{

    private XAPICaller xapiCaller = null;
    private Connection dbConnection = null;
    private CallableStatement logStatement = null;
    private CallableStatement fetchSMS = null;
    private ArrayList<CNCurrency> cNCurrencies = new ArrayList<>();

    public TDClient()
    {
        this(null);
    }

    public TDClient(XAPICaller xapiCaller)
    {
        this.xapiCaller = xapiCaller;
    }

    private void connectToDB()
    {
        try
        {
            setDbConnection(DriverManager.getConnection(PHController.CMSchemaURL, PHController.CMSchemaName, PHController.CMSchemaPassword));
            setLogStatement(getDbConnection().prepareCall("{call PSP_EX_LOG_POS_TXN(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)}"));
            setcNCurrencies();

        }
        catch (Exception ex)
        {
            logError(ex);
        }
    }

    public Object[][] executeQueryToArray(String query)
    {
        return resultSetToArray(executeQuery(query, true));
    }

    public ResultSet executeQueryToResultSet(String query)
    {
        return executeQuery(query, true);
    }

    public int getRowCount(ResultSet resultSet)
    {
        int records = 0;
        try
        {
            resultSet.last();
            records = resultSet.getRow();
            resultSet.beforeFirst();
        }
        catch (Exception ex)
        {
            logError(ex);
        }
        return records;
    }

    private Object[][] resultSetToArray(ResultSet resultSet)
    {
        Object[][] results = new Object[0][0];
        try
        {
            if (resultSet != null)
            {
                int fields = resultSet.getMetaData().getColumnCount(), rowCounter = 0, records = getRowCount(resultSet);
                results = (records == 0) ? new Object[0][0] : new Object[records][fields];
                while (resultSet.next())
                {
                    for (int col = 0; col < fields; col++)
                    {
                        results[rowCounter][col] = resultSet.getObject(col + 1);
                    }
                    rowCounter++;
                }
                resultSet.close();
            }
        }
        catch (Exception ex)
        {
            logError(ex);
        }
        return results;
    }

    public boolean checkIfExists(String query)
    {
        boolean exists = false;
        try ( ResultSet rs = executeQueryToResultSet(query))
        {
            exists = rs.next();
        }
        catch (Exception ex)
        {
            logError(ex);
        }
        return exists;
    }

    private ResultSet executeQuery(String query, boolean retry)
    {
        try
        {
            if ("Y".equalsIgnoreCase(PHController.EnablePosDebug))
            {
                logInfo(query);
            }
            if (getDbConnection() == null)
            {
                connectToDB();
            }
            else if (getDbConnection().isClosed())
            {
                connectToDB();
            }
            if (getDbConnection() != null)
            {
                return getDbConnection().createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY).executeQuery(query);
            }
        }
        catch (Exception ex)
        {
            if (String.valueOf(ex.getMessage()).contains("ORA-01000"))
            {
                dispose();
                if (retry)
                {
                    return executeQuery(query, false);
                }
            }
            else
            {
                logError(ex);
            }
        }
        return null;
    }

    public boolean executeUpdate(String update, boolean retry)
    {
        try
        {
            if ("Y".equalsIgnoreCase(PHController.EnablePosDebug))
            {
                logInfo(update);
            }
            if (getDbConnection() == null)
            {
                connectToDB();
            }
            else if (getDbConnection().isClosed())
            {
                connectToDB();
            }
            if (getDbConnection() != null)
            {
                update = update.replaceAll("'null'", "NULL").replaceAll("'NULL'", "NULL");
                getDbConnection().createStatement().executeUpdate(update);
                return true;
            }
        }
        catch (Exception ex)
        {
            if (String.valueOf(ex.getMessage()).contains("ORA-01000"))
            {
                dispose();
                if (retry)
                {
                    return executeUpdate(update, false);
                }
            }
            else
            {
                logError(ex);
            }
        }
        return false;
    }

    public void dispose()
    {
        try
        {
            if (getDbConnection() != null)
            {
                getDbConnection().close();
            }
        }
        catch (Exception ex)
        {
            setDbConnection(null);
        }
        setDbConnection(null);
    }

    public String loginAdminUser(String loginId, String password)
    {//AND A.BUSINESS_ROLE_ID IN (" + PHController.posAllowedLoginRoles + ") 
        String name = null;
        try ( ResultSet rs = executeQueryToResultSet("SELECT FIRST_NM || ' ' || LAST_NM AS NAME FROM " + PHController.CoreSchemaName + ".V_USER_ROLE A, " + PHController.CoreSchemaName + ".SYSPWD_HIST B WHERE A.LOGIN_ID='" + loginId + "' "
                + "AND B.SYSUSER_ID=A.USER_ID AND A.REC_ST=B.REC_ST AND B.PASSWD='" + BRCrypt.encrypt(password) + "' AND B.REC_ST='A'"))
        {
            if (rs.next())
            {
                name = rs.getString("NAME");
            }
        }
        catch (Exception ex)
        {
            logError(ex);
        }
        return name;
    }

    public BigDecimal getbalCharge(BigDecimal ledgerBal, Long Currency)
    {
        BigDecimal charge = BigDecimal.ZERO;

        if (Currency != 841 || Currency == null)
        {
            charge = BigDecimal.ZERO;
        }
        else
        {
            try ( ResultSet rs = executeQueryToResultSet("SELECT maxChargeAmt FROM ( "
                    + "SELECT atrr.TIER_CEILING as maxCeiling,atrr.CHARGE_AMT as maxChargeAmt,atrb.TIER_CEILING as minCeiling,atrb.CHARGE_AMT minChargeAmt,atrr.CHARGE_CODE "
                    + "FROM " + PHController.CMSchemaName + ".EI_CHARGE_TIER atrr  "
                    + "left outer join "
                    + "(SELECT TIER_CEILING,CHARGE_AMT,CHARGE_CODe "
                    + "FROM " + PHController.CMSchemaName + ".EI_CHARGE_TIER  WHERE CHARGE_CODE IN ('01A') AND TIER_CEILING <=" + ledgerBal + "  AND CRNCY_CODE = 'USD' "
                    + "and TIER_CEILING = (SELECT max(TIER_CEILING) "
                    + "FROM " + PHController.CMSchemaName + ".EI_CHARGE_TIER atr WHERE CHARGE_CODE IN ('01A') AND TIER_CEILING <=" + ledgerBal + "  AND CRNCY_CODE = 'USD')) atrb "
                    + "on ATRr.CHARGE_CODE = atrb.CHARGE_CODe "
                    + "WHERE atrr.CHARGE_CODE IN ('01A') AND atrr.TIER_CEILING >=" + ledgerBal + " AND atrr.CRNCY_CODE = 'USD' "
                    + "and atrr.TIER_CEILING = (SELECT max(atr.TIER_CEILING) "
                    + "FROM " + PHController.CMSchemaName + ".EI_CHARGE_TIER atr WHERE atr.CHARGE_CODE IN ('01A') AND atr.TIER_CEILING >=" + ledgerBal + "  AND atr.CRNCY_CODE = 'USD'))"))
            {
                if (rs.next())
                {
                    charge = rs.getBigDecimal(1);
                }
            }

            catch (Exception ex)
            {
                logError(ex);
            }
        }
        return charge;
    }

    public void updateXapiErrors()
    {
        try ( ResultSet rs = executeQueryToResultSet("SELECT ERROR_CODE, ERROR_DESC FROM " + PHController.CoreSchemaName + ".ERROR_CODE_DESCRIPTION_REF ORDER BY ERROR_CODE ASC"))
        {
            while (rs.next())
            {
                if (!PHController.getXapiCodes().containsKey(rs.getString("ERROR_CODE")))
                {
                    PHController.getXapiCodes().put(rs.getString("ERROR_CODE"), rs.getString("ERROR_DESC"));
                }
            }
            PHController.saveXapiCodes();
        }
        catch (Exception ex)
        {
            logError(ex);
        }
    }

    public boolean recordExists(String query)
    {
        boolean exists = false;
        try ( ResultSet rs = executeQueryToResultSet(query))
        {
            exists = rs.next();
        }
        catch (Exception ex)
        {
            logError(ex);
        }
        return exists;
    }

    public CNAccount queryAnyAccount(String accountNumber)
    {
        CNAccount cNAccount = new CNAccount();
        try ( ResultSet rs = executeQueryToResultSet("SELECT C.ACCT_ID, C.CUST_ID, C.MAIN_BRANCH_ID, C.PROD_ID, C.ACCT_NO, C.ACCT_NM, E.CRNCY_CD, C.PROD_CAT_TY FROM " + PHController.CoreSchemaName + ".ACCOUNT C, " + PHController.CoreSchemaName + ".CURRENCY E WHERE C.ACCT_NO='" + accountNumber + "' AND C.REC_ST='A' AND C.PROD_CAT_TY IN ('DP', 'LN') AND C.CRNCY_ID = E.CRNCY_ID"))
        {
            if (rs.next())
            {
                cNAccount.setAcctId(rs.getLong("ACCT_ID"));
                cNAccount.setCustId(rs.getLong("CUST_ID"));
                cNAccount.setBuId(rs.getLong("MAIN_BRANCH_ID"));
                cNAccount.setProductId(rs.getLong("PROD_ID"));
                cNAccount.setAccountNumber(rs.getString("ACCT_NO"));
                cNAccount.setAccountType(rs.getString("PROD_CAT_TY"));
                cNAccount.setShortName(removeSpaces(rs.getString("ACCT_NM")));
                cNAccount.setAccountName(removeSpaces(rs.getString("ACCT_NM")));
                cNAccount.setCurrency(queryCurrency(rs.getString("CRNCY_CD")));
            }
        }
        catch (Exception ex)
        {
            logError(ex);
        }
        return cNAccount;
    }

    public ArrayList<CNAccount> queryAllAccounts(long custId)
    {
        ArrayList<CNAccount> accounts = new ArrayList<>();
        try ( ResultSet rs = executeQueryToResultSet("SELECT C.ACCT_ID, C.CUST_ID, C.MAIN_BRANCH_ID, C.PROD_ID, C.ACCT_NO, C.ACCT_NM, C.ACCT_NO AS SHORT_NAME, E.CRNCY_CD, C.PROD_CAT_TY FROM " + PHController.CoreSchemaName + ".ACCOUNT C, " + PHController.CoreSchemaName + ".CURRENCY E WHERE C.CUST_ID=" + custId + " AND C.REC_ST='A' AND C.CRNCY_ID = E.CRNCY_ID"))
        {
            while (rs.next())
            {
                CNAccount cNAccount = new CNAccount();
                cNAccount.setAcctId(rs.getLong("ACCT_ID"));
                cNAccount.setCustId(rs.getLong("CUST_ID"));
                cNAccount.setBuId(rs.getLong("MAIN_BRANCH_ID"));
                cNAccount.setProductId(rs.getLong("PROD_ID"));
                cNAccount.setAccountNumber(rs.getString("ACCT_NO"));
                cNAccount.setAccountType(rs.getString("PROD_CAT_TY"));
                cNAccount.setShortName(rs.getString("SHORT_NAME"));
                cNAccount.setAccountName(removeSpaces(rs.getString("ACCT_NM")));
                cNAccount.setCurrency(queryCurrency(rs.getString("CRNCY_CD")));
                accounts.add(cNAccount);
            }
        }
        catch (Exception ex)
        {
            logError(ex);
        }
        return accounts;
    }

    public ArrayList<CNAccount> queryDepositAccounts(long custId)
    {
        ArrayList<CNAccount> accounts = new ArrayList<>();
        try ( ResultSet rs = executeQueryToResultSet("SELECT C.ACCT_ID, C.CUST_ID, C.MAIN_BRANCH_ID, C.PROD_ID, C.ACCT_NO, C.ACCT_NM, C.ACCT_NO AS SHORT_NAME, E.CRNCY_CD, C.PROD_CAT_TY FROM " + PHController.CoreSchemaName + ".ACCOUNT C, " + PHController.CoreSchemaName + ".CURRENCY E WHERE C.CUST_ID=" + custId + " AND C.REC_ST='A' AND C.PROD_CAT_TY='DP' AND C.CRNCY_ID = E.CRNCY_ID"))
        {
            while (rs.next())
            {
                CNAccount cNAccount = new CNAccount();
                cNAccount.setAcctId(rs.getLong("ACCT_ID"));
                cNAccount.setCustId(rs.getLong("CUST_ID"));
                cNAccount.setBuId(rs.getLong("MAIN_BRANCH_ID"));
                cNAccount.setProductId(rs.getLong("PROD_ID"));
                cNAccount.setAccountNumber(rs.getString("ACCT_NO"));
                cNAccount.setAccountType(rs.getString("PROD_CAT_TY"));
                cNAccount.setShortName(rs.getString("SHORT_NAME"));
                cNAccount.setAccountName(removeSpaces(rs.getString("ACCT_NM")));
                cNAccount.setCurrency(queryCurrency(rs.getString("CRNCY_CD")));
                accounts.add(cNAccount);
            }
        }
        catch (Exception ex)
        {
            logError(ex);
        }
        return accounts;
    }

    public ArrayList<CNAccount> queryLoanAccounts(long custId)
    {
        ArrayList<CNAccount> accounts = new ArrayList<>();
        try ( ResultSet rs = executeQueryToResultSet("SELECT C.ACCT_ID, C.CUST_ID, C.MAIN_BRANCH_ID, C.PROD_ID, C.ACCT_NO, C.ACCT_NM, C.ACCT_NO AS SHORT_NAME, E.CRNCY_CD, C.PROD_CAT_TY FROM " + PHController.CoreSchemaName + ".ACCOUNT C, " + PHController.CoreSchemaName + ".CURRENCY E WHERE C.CUST_ID=" + custId + " AND C.REC_ST='A' AND C.PROD_CAT_TY='LN' AND C.CRNCY_ID = E.CRNCY_ID"))
        {
            while (rs.next())
            {
                CNAccount cNAccount = new CNAccount();
                cNAccount.setAcctId(rs.getLong("ACCT_ID"));
                cNAccount.setCustId(rs.getLong("CUST_ID"));
                cNAccount.setBuId(rs.getLong("MAIN_BRANCH_ID"));
                cNAccount.setProductId(rs.getLong("PROD_ID"));
                cNAccount.setAccountNumber(rs.getString("ACCT_NO"));
                cNAccount.setAccountType(rs.getString("PROD_CAT_TY"));
                cNAccount.setShortName(rs.getString("SHORT_NAME"));
                cNAccount.setAccountName(removeSpaces(rs.getString("ACCT_NM")));
                cNAccount.setCurrency(queryCurrency(rs.getString("CRNCY_CD")));
                accounts.add(cNAccount);
            }
        }
        catch (Exception ex)
        {
            logError(ex);
        }
        try ( ResultSet rs = executeQueryToResultSet("SELECT C.ACCT_ID, C.CUST_ID, C.MAIN_BRANCH_ID, C.PROD_ID, C.ACCT_NO, C.ACCT_NM, C.ACCT_NO AS SHORT_NAME, E.CRNCY_CD, C.PROD_CAT_TY FROM " + PHController.CoreSchemaName + ".ACCOUNT C, " + PHController.CoreSchemaName + ".CURRENCY E WHERE C.CUST_ID IN (SELECT DISTINCT GROUP_CUST_ID FROM " + PHController.CoreSchemaName + ".GROUP_MEMBER WHERE MEMBER_CUST_ID=" + custId + " AND REC_ST='A') AND C.REC_ST='A' AND C.PROD_CAT_TY='LN' AND C.CRNCY_ID = E.CRNCY_ID"))
        {
            while (rs.next())
            {
                CNAccount cNAccount = new CNAccount();
                cNAccount.setAcctId(rs.getLong("ACCT_ID"));
                cNAccount.setCustId(rs.getLong("CUST_ID"));
                cNAccount.setBuId(rs.getLong("MAIN_BRANCH_ID"));
                cNAccount.setProductId(rs.getLong("PROD_ID"));
                cNAccount.setAccountNumber(rs.getString("ACCT_NO"));
                cNAccount.setAccountType(rs.getString("PROD_CAT_TY"));
                cNAccount.setShortName(rs.getString("SHORT_NAME"));
                cNAccount.setAccountName(removeSpaces(rs.getString("ACCT_NM")));
                cNAccount.setCurrency(queryCurrency(rs.getString("CRNCY_CD")));
                accounts.add(cNAccount);
            }
        }
        catch (Exception ex)
        {
            logError(ex);
        }
        return accounts;
    }

    public ArrayList<CNAccount> queryUnenrolledAccounts()
    {
        ArrayList<CNAccount> accounts = new ArrayList<>();
        //  try (ResultSet rs = executeQueryToResultSet("SELECT C.ACCT_ID, C.CUST_ID, C.MAIN_BRANCH_ID, C.PROD_ID, C.ACCT_NO, C.ACCT_NM, C.ACCT_NO AS SHORT_NAME, C.CRNCY_CD_ISO, C.PROD_CAT_TY FROM " + PHController.CoreSchemaName + ".V_ACCOUNTS C WHERE C.REC_ST='A' AND C.PROD_CAT_TY='DP' AND C.PROD_ID IN (" + PHController.posAllowedProductIDs + ") AND C.ACCT_ID NOT IN (SELECT ACCT_ID FROM " + PHController.CoreSchemaName + ".CUST_CHANNEL_ACCOUNT WHERE CHANNEL_ID=" + PHController.posChannelID + ") AND ROWNUM<=20000   ORDER BY C.ACCT_ID DESC"))

        try ( ResultSet rs = executeQueryToResultSet("SELECT C.ACCT_ID, C.CUST_ID,C.MAIN_BRANCH_ID,C.PROD_ID,C.ACCT_NO,C.ACCT_NM,C.ACCT_NO AS SHORT_NAME,C.CRNCY_CD_ISO,C.PROD_CAT_TY "
                + " FROM " + PHController.CoreSchemaName + ".V_ACCOUNTS C, " + PHController.CoreSchemaName + ".v_customer vc WHERE C.REC_ST = 'A' AND C.PROD_CAT_TY = 'DP' AND C.PROD_ID IN (" + PHController.posAllowedProductIDs + ")  "
                + " AND vc.cust_id = c.cust_id AND SOCIAL_SECURITY_NO IS NOT NULL AND LPAD (REPLACE (SOCIAL_SECURITY_NO, '/'), 16, 0) NOT IN (SELECT U.ACCESS_CD FROM " + PHController.CoreSchemaName + ".CUSTOMER_CHANNEL_USER U WHERE U.USER_CAT_CD = 'PER' AND  LPAD (REPLACE (SOCIAL_SECURITY_NO, '/'), 16, 0) = U.ACCESS_CD AND U.CHANNEL_ID = " + PHController.posChannelID + ") AND C.ACCT_ID NOT IN (SELECT ACCT_ID FROM " + PHController.CoreSchemaName + ".CUST_CHANNEL_ACCOUNT WHERE CHANNEL_ID = " + PHController.posChannelID + ") AND ROWNUM <= 20000  "
                + " UNION SELECT C.ACCT_ID,C.CUST_ID,C.MAIN_BRANCH_ID,C.PROD_ID,C.ACCT_NO,C.ACCT_NM,C.ACCT_NO AS SHORT_NAME,C.CRNCY_CD_ISO,C.PROD_CAT_TY "
                + " FROM " + PHController.CoreSchemaName + ".V_ACCOUNTS C, " + PHController.CoreSchemaName + ".v_customer vc WHERE C.REC_ST = 'A' AND C.PROD_CAT_TY = 'DP' AND C.PROD_ID IN (" + PHController.posAllowedProductIDs + ") "
                + " AND vc.cust_id = c.cust_id AND SOCIAL_SECURITY_NO IS NOT NULL  AND LPAD (REPLACE (SOCIAL_SECURITY_NO, '/'), 16, 0)  IN (SELECT U.ACCESS_CD FROM " + PHController.CoreSchemaName + ".CUSTOMER_CHANNEL_USER U WHERE U.USER_CAT_CD = 'PER' AND  LPAD (REPLACE (SOCIAL_SECURITY_NO, '/'), 16, 0)= U.ACCESS_CD AND U.CHANNEL_ID = " + PHController.posChannelID + ") AND C.ACCT_ID NOT IN (SELECT ACCT_ID FROM " + PHController.CoreSchemaName + ".CUST_CHANNEL_ACCOUNT WHERE CHANNEL_ID = " + PHController.posChannelID + ") AND ROWNUM <= 20000 "
                + " UNION ALL "
                + " SELECT C.ACCT_ID,C.CUST_ID,C.MAIN_BRANCH_ID,C.PROD_ID,C.ACCT_NO,C.ACCT_NM,C.ACCT_NO AS SHORT_NAME,C.CRNCY_CD_ISO,C.PROD_CAT_TY   "
                + " FROM " + PHController.CoreSchemaName + ".V_ACCOUNTS C, " + PHController.CoreSchemaName + ".V_ORGANISATION_CUSTOMER vc  WHERE C.REC_ST = 'A' AND C.PROD_CAT_TY = 'DP'AND C.PROD_ID IN (" + PHController.posAllowedProductIDs + ") "
                + " AND vc.cust_id = c.cust_id AND c.cust_no IS NOT NULL  AND LPAD (REPLACE (c.cust_no, '/'), 16, 0) NOT IN (SELECT U.ACCESS_CD FROM " + PHController.CoreSchemaName + ".CUSTOMER_CHANNEL_USER U  WHERE U.USER_CAT_CD = 'PER' AND  LPAD (REPLACE (C.CUST_NO, '/'), 16, 0)= U.ACCESS_CD AND U.CHANNEL_ID = " + PHController.posChannelID + ") AND C.ACCT_ID NOT IN (SELECT ACCT_ID FROM " + PHController.CoreSchemaName + ".CUST_CHANNEL_ACCOUNT WHERE CHANNEL_ID = " + PHController.posChannelID + ") AND ROWNUM <= 20000 "
                + " UNION "
                + " SELECT C.ACCT_ID,C.CUST_ID,C.MAIN_BRANCH_ID,C.PROD_ID,C.ACCT_NO,C.ACCT_NM,C.ACCT_NO AS SHORT_NAME,C.CRNCY_CD_ISO,C.PROD_CAT_TY  "
                + " FROM " + PHController.CoreSchemaName + ".V_ACCOUNTS C, " + PHController.CoreSchemaName + ".V_ORGANISATION_CUSTOMER vc WHERE C.REC_ST = 'A' AND C.PROD_CAT_TY = 'DP' AND C.PROD_ID IN (" + PHController.posAllowedProductIDs + ") "
                + " AND vc.cust_id = c.cust_id AND c.cust_no IS NOT NULL  AND LPAD (REPLACE (c.cust_no, '/'), 16, 0)  IN (SELECT U.ACCESS_CD FROM " + PHController.CoreSchemaName + ".CUSTOMER_CHANNEL_USER U   WHERE U.USER_CAT_CD = 'PER' AND  LPAD (REPLACE (C.CUST_NO, '/'), 16, 0)= U.ACCESS_CD AND U.CHANNEL_ID = " + PHController.posChannelID + ") AND C.ACCT_ID NOT IN (SELECT ACCT_ID    FROM " + PHController.CoreSchemaName + ".CUST_CHANNEL_ACCOUNT   WHERE CHANNEL_ID = " + PHController.posChannelID + ") AND ROWNUM <= 20000 "
                + " ORDER BY ACCT_ID DESC"))
        {
            while (rs.next())
            {
                CNAccount cNAccount = new CNAccount();
                cNAccount.setAcctId(rs.getLong("ACCT_ID"));
                cNAccount.setCustId(rs.getLong("CUST_ID"));
                cNAccount.setBuId(rs.getLong("MAIN_BRANCH_ID"));
                cNAccount.setProductId(rs.getLong("PROD_ID"));
                cNAccount.setAccountNumber(rs.getString("ACCT_NO"));
                cNAccount.setAccountType(rs.getString("PROD_CAT_TY"));
                cNAccount.setShortName(rs.getString("SHORT_NAME"));
                cNAccount.setAccountName(removeSpaces(rs.getString("ACCT_NM")));
                cNAccount.setCurrency(queryCurrency(rs.getString("CRNCY_CD_ISO")));
                accounts.add(cNAccount);
            }
        }
        catch (Exception ex)
        {
            logError(ex);
        }
        return accounts;
    }

    public ArrayList<CNCurrency> queryCurrencies()
    {
        ArrayList<CNCurrency> currencies = new ArrayList<>();
        try ( ResultSet rs = executeQueryToResultSet("SELECT CRNCY_ID, CRNCY_CD, CRNCY_NM, LEAST_CRNCY_DENOMINATOR FROM " + PHController.CoreSchemaName + ".CURRENCY WHERE REC_ST='A' ORDER BY CRNCY_ID"))
        {
            while (rs.next())
            {
                CNCurrency cNCurrency = new CNCurrency();
                cNCurrency.setCurrencyId(rs.getLong("CRNCY_ID"));
                cNCurrency.setCurrencyCode(rs.getString("CRNCY_CD"));
                cNCurrency.setCurrencyName(capitalize(rs.getString("CRNCY_NM"), 2));
                cNCurrency.setDecimalPlaces(countDecimalPlaces(rs.getBigDecimal("LEAST_CRNCY_DENOMINATOR")));
                currencies.add(cNCurrency);
            }
        }
        catch (Exception ex)
        {
            logError(ex);
        }
        return currencies;
    }

    public CNCurrency queryCurrency(String codeOrId)
    {
        if (getcNCurrencies().isEmpty())
        {
            setcNCurrencies();
        }
        if (codeOrId != null)
        {
            for (CNCurrency cNCurrency : getcNCurrencies())
            {
                if (codeOrId.equalsIgnoreCase(cNCurrency.getCurrencyCode()))
                {
                    return cNCurrency;
                }
            }
            for (CNCurrency cNCurrency : getcNCurrencies())
            {
                if (codeOrId.equalsIgnoreCase(String.valueOf(cNCurrency.getCurrencyId())))
                {
                    return cNCurrency;
                }
            }
        }
        else if (PHController.PrimaryCurrencyCode != null)
        {
            return queryCurrency(PHController.PrimaryCurrencyCode);
        }
        return new CNCurrency();
    }

//    public String unmaskGLAccount(String glAccount, long buId)
//    {
//        if (glAccount != null ? glAccount.contains("***") : false)
//        {
//            try (ResultSet rs = executeQueryToResultSet("SELECT GL_PREFIX_CD FROM " + PHController.CoreSchemaName + ".BUSINESS_UNIT WHERE BU_ID=" + buId))
//            {
//                if (rs.next())
//                {
//                    glAccount = rs.getString("GL_PREFIX_CD") + glAccount.substring(glAccount.indexOf("***") + 3);
//                }
//            }
//            catch (Exception ex)
//            {
//                logError(ex);
//            }
//        }
//        return glAccount;
//    }
    public String unmaskGLAccount(String glAccount, Long buId)
    {
        if (glAccount != null ? glAccount.contains("***") : false)
        {
            try ( ResultSet rs = executeQueryToResultSet("SELECT GL_PREFIX_CD FROM " + PHController.CoreSchemaName + ".BUSINESS_UNIT WHERE BU_ID=" + buId))
            {
                if (rs.next())
                {
                    String account = rs.getString("GL_PREFIX_CD") + glAccount.substring(glAccount.indexOf("***") + 3);
                    if (!account.equals(queryGLAccount(account).getAccountNumber()))
                    {
                        Long dBuId = getChannelBuId(PHController.posChannelID);
                        if (!Objects.equals(buId, dBuId))
                        {
                            return unmaskGLAccount(glAccount, dBuId);
                        }
                    }
                    glAccount = account;
                }
            }
            catch (Exception ex)
            {
                logError(ex);
            }
        }
        return glAccount;
    }

    /**
     * @return the dbConnection
     */
    public Connection getDbConnection()
    {
        return dbConnection;
    }

    /**
     * @param dbConnection the dbConnection to set
     */
    public void setDbConnection(Connection dbConnection)
    {
        this.dbConnection = dbConnection;
    }

    public long getDefaultBuId()
    {
        long buId = -99L;
        try ( ResultSet rs = executeQueryToResultSet("SELECT ORIGIN_BU_ID FROM " + PHController.CoreSchemaName + ".SERVICE_CHANNEL WHERE CHANNEL_ID=" + PHController.posChannelID))
        {
            if (rs.next())
            {
                buId = rs.getLong("ORIGIN_BU_ID");
            }
        }
        catch (Exception ex)
        {
            logError(ex);
        }
        return buId;
    }

    public long getAccountBuId(String accountNumber)
    {
        long buId = -99L;
        try ( ResultSet rs = executeQueryToResultSet("SELECT MAIN_BRANCH_ID FROM " + PHController.CoreSchemaName + ".ACCOUNT WHERE ACCT_NO='" + accountNumber + "'"))
        {
            buId = rs.next() ? rs.getLong("MAIN_BRANCH_ID") : getDefaultBuId();
        }
        catch (Exception ex)
        {
            logError(ex);
        }
        return buId;
    }

    public boolean upsertCharge(EICharge eICharge)
    {
        if (checkIfExists("SELECT CHARGE_CODE FROM " + PHController.CMSchemaName + ".EI_CHARGE WHERE CHARGE_CODE='" + eICharge.getChargeCode() + "'"))
        {
            return updateCharge(eICharge);
        }
        else
        {
            return saveCharge(eICharge);
        }
    }

    private boolean saveCharge(EICharge eICharge)
    {
        if (executeUpdate("INSERT INTO " + PHController.CMSchemaName + ".EI_CHARGE(CHARGE_CODE, CREATE_DATE, CHANNEL_ID, CHARGE_DESC, CHARGE_ACCOUNT, CHARGE_LEDGER, TAX_LEDGER, TAX_PERC, TAX_NAME, MODULE, MODIFIED_BY, DATE_MODIFIED, REC_ST) VALUES('" + eICharge.getChargeCode() + "', SYSDATE, " + PHController.posChannelID + ", '" + eICharge.getDescription() + "', '" + eICharge.getChargeAccount() + "', '" + eICharge.getChargeLedger() + "', '" + eICharge.getTaxLedger() + "', " + eICharge.getTaxPercentage().toPlainString() + ", '" + eICharge.getTaxName() + "', '" + eICharge.getModule() + "', '" + eICharge.getLastModifiedBy() + "', " + formatDate(eICharge.getDateModified()) + ", '" + eICharge.getStatus() + "')", true))
        {
            return saveChargeValues(eICharge) && saveChargeWaivers(eICharge);
        }
        return false;
    }

    public boolean saveBreftBalData(OfllineBalances ofllineBalances, String status)
    {
        return executeUpdate("INSERT INTO " + PHController.CMSchemaName + ".BR_ACCT_BALANCES (CUST_NO, ACCT_NO, NAMES, PROD_CD, LEDGER_BAL, AVAIL_BAL, REC_ST, ALLOW_DEP, ALLOW_WIT,IDENT_NO, MSG_HEADER, RESP_CODE, PROCESS_DATE ) "
                + "VALUES('" + ofllineBalances.getClientID() + "','" + ofllineBalances.getAccountID() + "','" + ofllineBalances.getTitleOfAccount() + "','" + ofllineBalances.getProductID() + "','" + ofllineBalances.getActualBalance() + "','" + ofllineBalances.getAvailableBalance() + "',"
                + "'" + ofllineBalances.getStatus() + "','" + ofllineBalances.getAllowDeposit() + "','" + ofllineBalances.getAllowWithdrawal() + "','" + ofllineBalances.getNRC() + "','" + BRCrypt.encrypt(ofllineBalances.getHeader()) + "','" + status + "',SYSDATE)", true);
    }

    public boolean upsertCustData(CustomerData customerData, String status)
    {
        if (customerData.getUpdated().equals("0"))
        {
            return saveBreftCustData(customerData, status);
        }
        else
        {
            return updateBreftCustData(customerData, status);
        }
    }

    private boolean saveBreftCustData(CustomerData customerData, String status)
    {
        return executeUpdate("INSERT INTO " + PHController.CMSchemaName + ".BR_CUSTOMER_DATA (BU_CD,CUST_NO,ACCT_NO,NAMES,PROD_CD,ACCT_TYPE,IDENT_NO,ACCESS_CD,REC_EXIST,MSG_HEADER,RESP_CODE,PROCESS_DATE,OLD_ACCT_NO) "
                + "VALUES('" + customerData.getOurBranchID() + "','" + customerData.getClientID() + "','" + customerData.getAccountID() + "','" + customerData.getTitleOfAccount() + "','" + customerData.getProductID() + "','" + customerData.getAccountType() + "','" + customerData.getNRC() + "','" + customerData.getATMCardNumber() + "','" + customerData.getUpdated() + "','" + BRCrypt.encrypt(customerData.getHeader()) + "','" + status + "',SYSDATE,'" + customerData.getAccountIDOld() + "')", true);
    }

    private boolean updateBreftCustData(CustomerData customerData, String status)
    {
        return executeUpdate("update " + PHController.CMSchemaName + ".BR_CUSTOMER_DATA set BU_CD = '" + customerData.getOurBranchID() + "', CUST_NO ='" + customerData.getClientID() + "',NAMES ='" + customerData.getTitleOfAccount() + "',PROD_CD ='" + customerData.getProductID() + "', "
                + " ACCT_TYPE = '" + customerData.getAccountType() + "', IDENT_NO ='" + customerData.getNRC() + "',ACCESS_CD ='" + customerData.getATMCardNumber() + "',REC_EXIST ='" + customerData.getUpdated() + "',PROCESS_DATE = SYSDATE,RESP_CODE = '" + status + "',OLD_ACCT_NO = '" + customerData.getAccountIDOld() + "'  WHERE ACCT_NO = '" + customerData.getAccountID() + "'", true);
    }

    public boolean upsertCustMobData(MobileRegistration mobileRegistration, String status)
    {
        if (mobileRegistration.getUpdated().equals("0"))
        {
            return saveBreftCustMobileData(mobileRegistration, status);
        }
        else
        {
            return updateBreftCustMobileData(mobileRegistration, status);
        }
    }

    public boolean saveBreftCustMobileData(MobileRegistration mobileRegistration, String status)
    {
        return executeUpdate("INSERT INTO " + PHController.CMSchemaName + ".BREFT_MOBILE_DATA (FIRST_NM,MIDDLE_NM,LAST_NM,MOBILE_PHONE,ADDR_LINE_1,EMAIL_ADRESS,CITY_DESC,ACCT_NO,BU_CD,MAX_AMT,TXN_LIMIT,SOCIAL_SECURITY_NO,TXN_COUNT,TITLE_DESC,ADDRESS_TYPE,ACCESS_CD,CRNCY_CD,CUST_CAT,REC_EXIST,MSG_HEADER,RESP_CODE,PROCESS_DATE,OLD_ACCT_NO) "
                + "VALUES('" + mobileRegistration.getFirstName() + "','" + mobileRegistration.getMiddleName() + "','" + mobileRegistration.getLastName() + "','" + mobileRegistration.getMobileNumber() + "','" + mobileRegistration.getAddress() + "','" + mobileRegistration.getEMailID() + "','" + mobileRegistration.getCity() + "','" + mobileRegistration.getAccountID() + "','" + mobileRegistration.getBranchID() + "',"
                + "" + mobileRegistration.getMaximumAmount() + "," + mobileRegistration.getTransactionLimit() + ",'" + mobileRegistration.getIDNumber() + "'," + mobileRegistration.getNoOfTransactions() + ",'" + mobileRegistration.getTitle() + "','" + mobileRegistration.getTypeOfID() + "','" + mobileRegistration.getCardNumber() + "','" + mobileRegistration.getCurrencyID() + "','" + mobileRegistration.getCustomerType() + "','" + mobileRegistration.getUpdated() + "',"
                + "'" + BRCrypt.encrypt(mobileRegistration.getHeader()) + "','" + status + "',SYSDATE,'" + mobileRegistration.getAccountIDOld() + "')", true);
    }

    public boolean updateBreftCustMobileData(MobileRegistration mobileRegistration, String status)
    {
        return executeUpdate("Update " + PHController.CMSchemaName + ".BREFT_MOBILE_DATA set FIRST_NM ='" + mobileRegistration.getFirstName() + "',MIDDLE_NM='" + mobileRegistration.getMiddleName() + "',LAST_NM='" + mobileRegistration.getLastName() + "',MOBILE_PHONE='" + mobileRegistration.getMobileNumber() + "',"
                + "ADDR_LINE_1='" + mobileRegistration.getAddress() + "',EMAIL_ADRESS='" + mobileRegistration.getEMailID() + "',CITY_DESC='" + mobileRegistration.getCity() + "',ACCT_NO='" + mobileRegistration.getAccountID() + "',BU_CD='" + mobileRegistration.getBranchID() + "',MAX_AMT=" + mobileRegistration.getMaximumAmount() + ","
                + "TXN_LIMIT=" + mobileRegistration.getTransactionLimit() + ",SOCIAL_SECURITY_NO='" + mobileRegistration.getIDNumber() + "',TXN_COUNT=" + mobileRegistration.getNoOfTransactions() + ",TITLE_DESC='" + mobileRegistration.getTitle() + "',ADDRESS_TYPE='" + mobileRegistration.getTypeOfID() + "',ACCESS_CD='" + mobileRegistration.getCardNumber() + "',"
                + "CRNCY_CD='" + mobileRegistration.getCurrencyID() + "',CUST_CAT='" + mobileRegistration.getCustomerType() + "',REC_EXIST='" + mobileRegistration.getUpdated() + "',MSG_HEADER='" + BRCrypt.encrypt(mobileRegistration.getHeader()) + "',RESP_CODE='" + status + "',PROCESS_DATE=SYSDATE, OLD_ACCT_NO='" + mobileRegistration.getAccountIDOld() + "' WHERE ACCT_NO='" + mobileRegistration.getAccountID() + "'", true);
    }

    public boolean upsertAgentData(AgentData agentData, String status)
    {
        if (agentData.getUpdated().equals("0"))
        {
            return saveBreftAgentData(agentData, status);
        }
        else
        {
            return updateBreftAgentData(agentData, status);
        }
    }

    public boolean saveBreftAgentData(AgentData agentData, String status)
    {
//COME BACK LATER
        //,'" + BRCrypt.encrypt(agentData.getHeader()) + "'
        return executeUpdate("INSERT INTO " + PHController.CMSchemaName + ".BREFT_AGENT_DATA (PROD_CD,CRNCY_CD,BU_CD,CUST_NO,CUST_ID,ACCT_NO,LEDGER_BAL,CUST_NM,BUSINESS_TYPE,ADDR_LINE_1,POSTAL_ADDRESS,CONTACT_PERSON,MOBILE_PHONE,OFFICE_PHONE,EMAIL_ADRESS,UPDATED_FG,RESP_CODE,PROCESS_DATE,IDENT_NO,OLD_ACCT_NO) "
                + "VALUES('" + agentData.getPRODUCTCODE() + "','" + agentData.getCURRENCY() + "','" + agentData.getBRANCHCODE() + "','" + agentData.getCODCUSTNAILID() + "','" + agentData.getAGENTID() + "','" + agentData.getAGENTACCOUNT() + "'," + agentData.getACCOUNTBAL() + ",'" + agentData.getBUSINESSNAME() + "',"
                + "'" + agentData.getBUSINESSTYPE() + "','" + agentData.getPHYSICALADDRESS() + "','" + agentData.getPOSTALADDRESS() + "','" + agentData.getCONTACTPERSON() + "','" + agentData.getMOBILEPHONE() + "','" + agentData.getOFFICEPHONE() + "','" + agentData.getEMAILADDRESS() + "','" + agentData.getUpdated() + "','" + status + "',SYSDATE,'" + agentData.getNRC() + "','" + agentData.getAccountIDOld() + "')", true);
    }

    public boolean updateBreftAgentData(AgentData agentData, String status)
    {
        return executeUpdate("UPDATE " + PHController.CMSchemaName + ".BREFT_AGENT_DATA set PROD_CD ='" + agentData.getPRODUCTCODE() + "',CRNCY_CD='" + agentData.getCURRENCY() + "',BU_CD='" + agentData.getBRANCHCODE() + "',CUST_NO='" + agentData.getCODCUSTNAILID() + "',CUST_ID='" + agentData.getAGENTID() + "',"
                + "ACCT_NO='" + agentData.getAGENTACCOUNT() + "',LEDGER_BAL=" + agentData.getACCOUNTBAL() + ",CUST_NM='" + agentData.getBUSINESSNAME() + "',BUSINESS_TYPE='" + agentData.getBUSINESSTYPE() + "',ADDR_LINE_1='" + agentData.getPHYSICALADDRESS() + "',POSTAL_ADDRESS='" + agentData.getPOSTALADDRESS() + "',"
                + "CONTACT_PERSON='" + agentData.getCONTACTPERSON() + "',MOBILE_PHONE='" + agentData.getMOBILEPHONE() + "',OFFICE_PHONE='" + agentData.getOFFICEPHONE() + "',EMAIL_ADRESS='" + agentData.getEMAILADDRESS() + "',UPDATED_FG='" + agentData.getUpdated() + "',RESP_CODE='" + status + "',PROCESS_DATE =SYSDATE,IDENT_NO='" + agentData.getNRC() + "',OLD_ACCT_NO='" + agentData.getAccountIDOld() + "' WHERE ACCT_NO='" + agentData.getAGENTACCOUNT() + "'", true);
    }

    public boolean saveBreftCustMobDatamIG(MobileMigration mobileRegistration, String status)
    {
        return executeUpdate("INSERT INTO " + PHController.CMSchemaName + ".BREFT_MOBILE_DATA (FIRST_NM,MIDDLE_NM,LAST_NM,MOBILE_PHONE,ADDR_LINE_1,EMAIL_ADRESS,CITY_DESC,ACCT_NO,BU_CD,MAX_AMT,TXN_LIMIT,SOCIAL_SECURITY_NO,TXN_COUNT,TITLE_DESC,ADDRESS_TYPE,ACCESS_CD,CRNCY_CD,CUST_CAT,REC_EXIST,MSG_HEADER,RESP_CODE,PROCESS_DATE,OLD_ACCT_NO) "
                + "VALUES('" + mobileRegistration.getFirstName() + "','" + mobileRegistration.getMiddleName() + "','" + mobileRegistration.getLastName() + "','" + mobileRegistration.getMobileNumber() + "','" + mobileRegistration.getAddress() + "','" + mobileRegistration.getEMailID() + "','" + mobileRegistration.getCity() + "','" + mobileRegistration.getAccountID() + "','" + mobileRegistration.getBranchID() + "',"
                + "" + mobileRegistration.getMaximumAmount() + "," + mobileRegistration.getTransactionLimit() + ",'" + mobileRegistration.getIDNumber() + "'," + mobileRegistration.getNoOfTransactions() + ",'" + mobileRegistration.getTitle() + "','" + mobileRegistration.getTypeOfID() + "','" + mobileRegistration.getCardNumber() + "','" + mobileRegistration.getCurrencyID() + "','" + mobileRegistration.getCustomerType() + "','" + mobileRegistration.getUpdated() + "','" + BRCrypt.encrypt(mobileRegistration.getHeader()) + "','" + status + "',SYSDATE,'" + mobileRegistration.getAccountIDOld() + "')", true);
    }

    public boolean saveBreftDeregistrationData(MobileDeregistration deregistration, String status)
    {
        return executeUpdate("INSERT INTO " + PHController.CMSchemaName + ".BREFT_MOBILE_DEREGISTRATION (CUST_NO,MOBILE_NO,REASON,EXTRA_PARAM,STATUS, PROCESS_DATE) "
                + "VALUES('" + deregistration.getAccountID() + "','" + deregistration.getMobileNumber() + "','" + deregistration.getReason() + "','" + deregistration.getExtraParams() + "','" + status + "',SYSDATE)", true);
    }

    public HashMap<String, EICharge> loadCharges(String module)
    {
        HashMap<String, EICharge> eICharges = new HashMap<>();
        try ( ResultSet rs = executeQueryToResultSet("SELECT CHARGE_CODE, CHARGE_DESC, CHARGE_ACCOUNT, CHARGE_LEDGER, TAX_LEDGER, TAX_PERC, TAX_NAME, MODULE, MODIFIED_BY, DATE_MODIFIED, REC_ST FROM " + PHController.CMSchemaName + ".EI_CHARGE WHERE CHANNEL_ID=" + PHController.posChannelID + " AND MODULE='" + module + "' ORDER BY CHARGE_CODE ASC"))
        {
            while (rs.next())
            {
                EICharge eICharge = new EICharge();
                eICharge.setChargeCode(rs.getString("CHARGE_CODE"));
                eICharge.setDescription(rs.getString("CHARGE_DESC"));
                eICharge.setChargeAccount(rs.getString("CHARGE_ACCOUNT"));
                eICharge.setChargeLedger(rs.getString("CHARGE_LEDGER"));
                eICharge.setTaxLedger(rs.getString("TAX_LEDGER"));
                eICharge.setTaxPercentage(rs.getBigDecimal("TAX_PERC"));
                eICharge.setTaxName(rs.getString("TAX_NAME"));
                eICharge.setModule(rs.getString("MODULE"));
                eICharge.setLastModifiedBy(rs.getString("MODIFIED_BY"));
                eICharge.setDateModified(rs.getDate("DATE_MODIFIED"));
                eICharge.setStatus(rs.getString("REC_ST"));
                eICharges.put(eICharge.getChargeCode(), setChargeValues(setChargeWaivers(eICharge)));
            }
        }
        catch (Exception ex)
        {
            logError(ex);
        }
        return eICharges;
    }

    public EICharge setChargeValues(EICharge eICharge)
    {
        try ( ResultSet rs = executeQueryToResultSet("SELECT CRNCY_CODE, CHARGE_TYPE, MIN_AMT, MAX_AMT, VALUE FROM " + PHController.CMSchemaName + ".EI_CHARGE_VALUE WHERE CHARGE_CODE='" + eICharge.getChargeCode() + "' ORDER BY CRNCY_CODE ASC"))
        {
            eICharge.getValues().clear();
            while (rs.next())
            {
                TCValue tCValue = new TCValue();
                tCValue.setCurrency(rs.getString("CRNCY_CODE"));
                tCValue.setChargeType(rs.getString("CHARGE_TYPE"));
                tCValue.setMinAmount(rs.getBigDecimal("MIN_AMT"));
                tCValue.setMaxAmount(rs.getBigDecimal("MAX_AMT"));
                tCValue.setChargeValue(rs.getBigDecimal("VALUE"));
                eICharge.getValues().put(rs.getString("CRNCY_CODE"), setChargeTiers(eICharge, tCValue));
            }
        }
        catch (Exception ex)
        {
            logError(ex);
        }
        return eICharge;
    }

    private TCValue setChargeTiers(EICharge eICharge, TCValue tCValue)
    {
        try ( ResultSet rs = executeQueryToResultSet("SELECT CHARGE_CODE, CRNCY_CODE, TIER_CEILING, CHARGE_AMT FROM " + PHController.CMSchemaName + ".EI_CHARGE_TIER WHERE CHARGE_CODE='" + eICharge.getChargeCode() + "' AND CRNCY_CODE='" + tCValue.getCurrency() + "' ORDER BY TIER_CEILING ASC"))
        {
            tCValue.getTiers().clear();
            while (rs.next())
            {
                TCTier tCTier = new TCTier();
                tCTier.setTierCeiling(rs.getBigDecimal("TIER_CEILING"));
                tCTier.setChargeAmount(rs.getBigDecimal("CHARGE_AMT"));
                tCValue.getTiers().put(tCTier.getTierCeiling(), tCTier);
            }
        }
        catch (Exception ex)
        {
            logError(ex);
        }
        return tCValue;
    }

    public EICharge setChargeWaivers(EICharge eICharge)
    {
        try ( ResultSet rs = executeQueryToResultSet("SELECT PROD_ID, MATCH_ACCT, WAIVED_PERC, CONDITION, THRESHOLD FROM " + PHController.CMSchemaName + ".EI_CHARGE_WAIVER WHERE CHARGE_CODE='" + eICharge.getChargeCode() + "' ORDER BY PROD_ID ASC"))
        {
            eICharge.getWaivers().clear();
            while (rs.next())
            {
                TCWaiver tXWaiver = new TCWaiver();
                tXWaiver.setProductId(rs.getInt("PROD_ID"));
                tXWaiver.setMatchAccount(rs.getString("MATCH_ACCT"));
                tXWaiver.setWaivedPercentage(rs.getBigDecimal("WAIVED_PERC"));
                tXWaiver.setWaiverCondition(rs.getString("CONDITION"));
                tXWaiver.setThresholdValue(rs.getBigDecimal("THRESHOLD"));
                eICharge.getWaivers().put(rs.getInt("PROD_ID"), tXWaiver);
            }
        }
        catch (Exception ex)
        {
            logError(ex);
        }
        return eICharge;
    }

    private boolean saveChargeValues(EICharge eICharge)
    {
        boolean RC = deleteChargeValues(eICharge);
        for (TCValue tCValue : eICharge.getValues().values())
        {
            if (executeUpdate("INSERT INTO " + PHController.CMSchemaName + ".EI_CHARGE_VALUE(CHARGE_CODE, CRNCY_CODE, CHARGE_TYPE, MIN_AMT, MAX_AMT, VALUE) VALUES('" + eICharge.getChargeCode() + "', '" + tCValue.getCurrency() + "', '" + tCValue.getChargeType() + "', " + tCValue.getMinAmount().toPlainString() + ", " + tCValue.getMaxAmount().toPlainString() + ", " + tCValue.getChargeValue().toPlainString() + ")", true))
            {
                RC = saveChargeTiers(eICharge, tCValue);
            }
        }
        return RC;
    }

    private boolean deleteChargeValues(EICharge eICharge)
    {
        return executeUpdate("DELETE " + PHController.CMSchemaName + ".EI_CHARGE_VALUE WHERE CHARGE_CODE='" + eICharge.getChargeCode() + "'", true);
    }

    private boolean saveChargeTiers(EICharge eICharge, TCValue tCValue)
    {
        boolean RC = deleteChargeTiers(eICharge, tCValue);
        for (TCTier tXTier : tCValue.getTiers().values())
        {
            RC = executeUpdate("INSERT INTO " + PHController.CMSchemaName + ".EI_CHARGE_TIER(CHARGE_CODE, CRNCY_CODE, TIER_CEILING, CHARGE_AMT) VALUES('" + eICharge.getChargeCode() + "', '" + tCValue.getCurrency() + "', " + tXTier.getTierCeiling() + ", " + tXTier.getChargeAmount() + ")", true);
        }
        return RC;
    }

    private boolean deleteChargeTiers(EICharge eICharge, TCValue tCValue)
    {
        return executeUpdate("DELETE " + PHController.CMSchemaName + ".EI_CHARGE_TIER WHERE CHARGE_CODE='" + eICharge.getChargeCode() + "' AND CRNCY_CODE='" + tCValue.getCurrency() + "'", true);
    }

    private boolean saveChargeWaivers(EICharge eICharge)
    {
        boolean RC = deleteChargeWaivers(eICharge);
        for (TCWaiver tXWaiver : eICharge.getWaivers().values())
        {
            RC = executeUpdate("INSERT INTO " + PHController.CMSchemaName + ".EI_CHARGE_WAIVER(CHARGE_CODE, PROD_ID, MATCH_ACCT, WAIVED_PERC, CONDITION, THRESHOLD) VALUES('" + eICharge.getChargeCode() + "', " + tXWaiver.getProductId() + ", '" + tXWaiver.getMatchAccount() + "', " + tXWaiver.getWaivedPercentage().toPlainString() + ", '" + tXWaiver.getWaiverCondition() + "', " + tXWaiver.getThresholdValue().toPlainString() + ")", true);
        }
        return RC;
    }

    private boolean deleteChargeWaivers(EICharge eICharge)
    {
        return executeUpdate("DELETE " + PHController.CMSchemaName + ".EI_CHARGE_WAIVER WHERE CHARGE_CODE='" + eICharge.getChargeCode() + "'", true);
    }

    private boolean updateCharge(EICharge eICharge)
    {
        if (executeUpdate("UPDATE " + PHController.CMSchemaName + ".EI_CHARGE SET CHANNEL_ID=" + PHController.posChannelID + ", CHARGE_DESC='" + eICharge.getDescription() + "', CHARGE_ACCOUNT='" + eICharge.getChargeAccount() + "', CHARGE_LEDGER='" + eICharge.getChargeLedger() + "', TAX_LEDGER='" + eICharge.getTaxLedger() + "', TAX_PERC=" + eICharge.getTaxPercentage().toPlainString() + ", TAX_NAME='" + eICharge.getTaxName() + "', MODULE='" + eICharge.getModule() + "', MODIFIED_BY='" + eICharge.getLastModifiedBy() + "', DATE_MODIFIED=" + formatDate(eICharge.getDateModified()) + ", REC_ST='" + eICharge.getStatus() + "' WHERE CHARGE_CODE='" + eICharge.getChargeCode() + "'", true))
        {
            return saveChargeValues(eICharge) && saveChargeWaivers(eICharge);
        }
        return false;
    }

    public boolean deleteInvalidChannelUsers()
    {
        return executeUpdate("DELETE " + PHController.CoreSchemaName + ".CUST_CHANNEL_ACCOUNT WHERE   CHANNEL_ID = 8 AND CUST_ID NOT IN(SELECT CUST_ID FROM " + PHController.CoreSchemaName + ".CUSTOMER_CHANNEL_USER WHERE CHANNEL_ID =8)", true)
                //   executeUpdate("DELETE " + PHController.CoreSchemaName + ".CUSTOMER_CHANNEL_USER WHERE   CHANNEL_ID = 8 AND CUST_ID NOT IN (SELECT CUST_ID FROM " + PHController.CoreSchemaName + ".CUSTOMER_CHANNEL WHERE CHANNEL_SCHEME_ID = 11)", true)&&
                && executeUpdate("DELETE  " + PHController.CoreSchemaName + ".CUSTOMER_CHANNEL CC WHERE    CHANNEL_SCHEME_ID =11 AND CUST_ID NOT IN (SELECT   CUST_ID FROM " + PHController.CoreSchemaName + ".CUST_CHANNEL_ACCOUNT cca where  channel_id =8)", true)
                && executeUpdate("DELETE  " + PHController.CoreSchemaName + ".CUSTOMER_CHANNEL CC WHERE    CHANNEL_SCHEME_ID =11  AND CUST_ID NOT IN (SELECT   CUST_ID FROM " + PHController.CoreSchemaName + ".CUSTOMER_CHANNEL_USER cca where  channel_id =8)", true)
                && executeUpdate("DELETE " + PHController.CoreSchemaName + ".CUST_CHANNEL_SCHEME WHERE  CHANNEL_ID = 8 AND CUST_ID NOT IN (SELECT CUST_ID FROM " + PHController.CoreSchemaName + ".CUSTOMER_CHANNEL_USER WHERE CHANNEL_ID =8)", true);
    }

    public CNUser queryCNUser(String accessCode)
    {
        CNUser cNUser = new CNUser();
        try ( ResultSet rs = executeQueryToResultSet("SELECT CUST_CHANNEL_USER_ID, CUST_ID, CUST_CHANNEL_ID, ACCESS_CD, ACCESS_NM, PASSWORD, PWD_RESET_FG, "
                + "CHANNEL_SCHEME_ID, LOCKED_FG, NVL(RANDOM_SEED, 0) AS PIN_TRIES, NVL(RANDOM_NO_SEED, 0) AS PUK_TRIES, SECURITY_CD, QUIZ_CD "
                + "FROM " + PHController.CoreSchemaName + ".CUSTOMER_CHANNEL_USER "
                + "WHERE CHANNEL_SCHEME_ID=" + PHController.posChannelSchemeID + " AND USER_CAT_CD='PER' "
                + "AND ACCESS_CD='" + accessCode + "' AND REC_ST='A'"))
        {
            cNUser.setUserCount(getRowCount(rs));
            if (rs.next())
            {
                cNUser.setUserId(rs.getInt("CUST_CHANNEL_USER_ID"));
                cNUser.setCustId(rs.getLong("CUST_ID"));
                cNUser.setCustChannelId(rs.getLong("CUST_CHANNEL_ID"));
                cNUser.setAccessCode(rs.getString("ACCESS_CD"));
                cNUser.setAccessName(capitalize(rs.getString("ACCESS_NM")));
                cNUser.setPassword(rs.getString("PASSWORD"));
                cNUser.setPwdReset("Y".equals(rs.getString("PWD_RESET_FG")));
                cNUser.setSchemeId(rs.getLong("CHANNEL_SCHEME_ID"));
                cNUser.setLocked("Y".equals(rs.getString("LOCKED_FG")));
                cNUser.setSecurityCode(rs.getString("PWD_RESET_FG"));
                cNUser.setImsi(rs.getString("QUIZ_CD"));
                cNUser.setPinAttempts(rs.getInt("PIN_TRIES"));
                cNUser.setPukAttempts(rs.getInt("PUK_TRIES"));
                cNUser.setChargeAccount(queryChargeAccount(cNUser));
                cNUser.setEnrolledAccounts(queryEnrolledAccounts(cNUser.getCustId(), accessCode));
                cNUser.setAllDepositAccounts(queryDepositAccounts(cNUser.getCustId()));
                cNUser.setAllLoanAccounts(queryLoanAccounts(cNUser.getCustId()));
                //pushUserExpiry(cNUser.getAccessCode()); no need to update expiry date finca zambia
            }
            else
            {
                cNUser.setCustId(0);
                cNUser.setAccessCode("XoX");
                cNUser.setCustChannelId(0);
            }
        }
        catch (Exception ex)
        {
            logError(ex);
        }
        return cNUser;
    }

    public CNAccount queryChargeAccount(CNUser cNUser)
    {
        CNAccount cNAccount = new CNAccount();
        try ( ResultSet rs = executeQueryToResultSet("SELECT C.ACCT_ID, C.CUST_ID, C.MAIN_BRANCH_ID, C.PROD_ID, C.ACCT_NO, C.ACCT_NM, E.CRNCY_CD, C.PROD_CAT_TY FROM " + PHController.CoreSchemaName + ".ACCOUNT C, " + PHController.CoreSchemaName + ".CURRENCY E, " + PHController.CoreSchemaName + ".CUSTOMER_CHANNEL L WHERE C.ACCT_ID=L.CHRG_ACCT_ID AND L.CUST_ID=" + cNUser.getCustId() + " AND L.CHANNEL_SCHEME_ID=" + cNUser.getSchemeId() + " AND C.REC_ST='A' AND C.PROD_CAT_TY='DP' AND C.CRNCY_ID = E.CRNCY_ID"))
        {
            if (rs.next())
            {
                cNAccount.setAcctId(rs.getLong("ACCT_ID"));
                cNAccount.setCustId(rs.getLong("CUST_ID"));
                cNAccount.setBuId(rs.getLong("MAIN_BRANCH_ID"));
                cNAccount.setProductId(rs.getLong("PROD_ID"));
                cNAccount.setAccountNumber(rs.getString("ACCT_NO"));
                cNAccount.setAccountType(rs.getString("PROD_CAT_TY"));
                cNAccount.setShortName(removeSpaces(rs.getString("ACCT_NM")));
                cNAccount.setAccountName(removeSpaces(rs.getString("ACCT_NM")));
                cNAccount.setCurrency(queryCurrency(rs.getString("CRNCY_CD")));
            }
        }
        catch (Exception ex)
        {
            logError(ex);
        }
        return cNAccount;
    }

    public ArrayList<CNAccount> queryEnrolledAccounts(Long custId, String cardNo)
    {
        ArrayList<CNAccount> accounts = new ArrayList<>();
        try ( ResultSet rs = executeQueryToResultSet("SELECT C.ACCT_ID, C.CUST_ID, C.MAIN_BRANCH_ID, C.PROD_ID, C.ACCT_NO, C.ACCT_NM, NVL(B.SHORT_NAME, C.ACCT_NO) AS SHORT_NAME, E.CRNCY_CD, C.PROD_CAT_TY FROM " + PHController.CoreSchemaName + ".CUSTOMER_CHANNEL_USER A, " + PHController.CoreSchemaName + ".CUST_CHANNEL_ACCOUNT B, " + PHController.CoreSchemaName + ".ACCOUNT C, " + PHController.CoreSchemaName + ".CURRENCY E WHERE A.CHANNEL_SCHEME_ID=" + PHController.posChannelSchemeID + " AND A.ACCESS_CD='" + cardNo + "' AND A.REC_ST='A' AND B.REC_ST=A.REC_ST AND A.CHANNEL_ID=B.CHANNEL_ID AND A.CUST_ID = C.CUST_ID AND B.ACCT_ID = C.ACCT_ID AND A.CUST_ID=B.CUST_ID AND C.PROD_CAT_TY='DP' AND C.ACCT_ID=B.ACCT_ID AND C.REC_ST='A' AND C.CRNCY_ID = E.CRNCY_ID AND B.CUST_CHANNEL_ID = A.CUST_CHANNEL_ID"))
        {
            while (rs.next())
            {
                CNAccount cNAccount = new CNAccount();
                cNAccount.setAcctId(rs.getLong("ACCT_ID"));
                cNAccount.setCustId(rs.getLong("CUST_ID"));
                cNAccount.setBuId(rs.getLong("MAIN_BRANCH_ID"));
                cNAccount.setProductId(rs.getLong("PROD_ID"));
                cNAccount.setAccountNumber(rs.getString("ACCT_NO"));
                cNAccount.setAccountType(rs.getString("PROD_CAT_TY"));
                cNAccount.setShortName(rs.getString("SHORT_NAME"));
                cNAccount.setAccountName(capitalize(removeSpaces(rs.getString("ACCT_NM"))));
                cNAccount.setCurrency(queryCurrency(rs.getString("CRNCY_CD")));
                accounts.add(cNAccount);
            }
        }
        catch (Exception ex)
        {
            logError(ex);
        }
        return accounts;
    }

    public boolean isAccountEnrolled(Long custChannelId, Long acctId)
    {
        return checkIfExists("SELECT A.ACCT_ID FROM " + PHController.CoreSchemaName + ".CUST_CHANNEL_ACCOUNT A WHERE A.CHANNEL_ID=" + PHController.posChannelID + " AND A.ACCT_ID=" + acctId + " AND A.CUST_CHANNEL_ID=" + custChannelId);
    }

    public boolean isBRDataExisting(String acctNo, String custNumber)
    {
        return checkIfExists("select CUST_NO FROM " + PHController.CMSchemaName + ".BR_CUSTOMER_DATA  WHERE ACCT_NO ='" + acctNo + "' AND CUST_NO = '" + custNumber + "' AND RESP_CODE ='00' AND TO_DATE(TO_CHAR(PROCESS_DATE,'DD/MM/yyyy'),'DD/MM/yyyy') = (SELECT TO_DATE(TO_CHAR(SYSDATE,'DD/MM/yyyy'),'DD/MM/yyyy') FROM DUAL) ");
    }

    public boolean isBRMobDataExisting(String acctNo, String mobNumber)
    {
        return checkIfExists("select ACCESS_CD FROM " + PHController.CMSchemaName + ".BREFT_MOBILE_DATA WHERE ACCT_NO ='" + acctNo + "' AND ACCESS_CD= '" + mobNumber + "' AND RESP_CODE in ('00','96') AND TO_DATE(TO_CHAR(PROCESS_DATE,'DD/MM/yyyy'),'DD/MM/yyyy') = (SELECT TO_DATE(TO_CHAR(SYSDATE,'DD/MM/yyyy'),'DD/MM/yyyy') FROM DUAL) ");
    }

    public boolean isBRAgentDataExisting(String acctNo, String custNumber)
    {
        return checkIfExists("SELECT ACCT_NO FROM " + PHController.CMSchemaName + ".BREFT_AGENT_DATA WHERE ACCT_NO ='" + acctNo + "' AND CUST_NO= '" + custNumber + "' AND RESP_CODE ='00' AND TO_DATE(TO_CHAR(PROCESS_DATE,'DD/MM/yyyy'),'DD/MM/yyyy') = (SELECT TO_DATE(TO_CHAR(SYSDATE,'DD/MM/yyyy'),'DD/MM/yyyy') FROM DUAL)");
    }

    public boolean pushUserExpiry(String cardNo)
    {
        return executeUpdate("UPDATE " + PHController.CoreSchemaName + ".CUSTOMER_CHANNEL_USER SET EXPIRY_DT=SYSDATE+180 WHERE CHANNEL_SCHEME_ID=" + PHController.posChannelSchemeID + " AND ACCESS_CD='" + cardNo + "'", true);
    }

    public boolean deleteInvalidRecord(String cardNo, String acctNo)
    {
        return executeUpdate("DELETE FROM  " + PHController.CoreSchemaName + ".BREFT_MOBILE_DATA where MOBILE_PHONE ='" + cardNo + "'  and ACCT_NO = '" + acctNo + "' and resp_code = '96'     ", true);
    }

    public boolean deleteFailedRecords()
    {
        return executeUpdate("DELETE FROM  " + PHController.CoreSchemaName + ".BREFT_MOBILE_DATA WHERE resp_code = '96'     ", true);
    }

    public boolean updateNextEntityId(String tableName, String columnName)
    {
        return executeUpdate("UPDATE " + PHController.CoreSchemaName + ".ENTITY SET NEXT_NO=(SELECT MAX(" + columnName + ")+1 FROM " + PHController.CoreSchemaName + "." + tableName + ") WHERE ENTITY_NM = '" + tableName + "'", true);
    }

    public boolean saveChannelAccount(CNUser cNUser, CNAccount cNAccount)
    {
        if (executeUpdate("INSERT INTO " + PHController.CoreSchemaName + ".CUST_CHANNEL_ACCOUNT (CUST_CHANNEL_ACCT_ID, CUST_ID, CHANNEL_ID, ACCT_ID, SHORT_NAME, REC_ST, VERSION_NO, ROW_TS, USER_ID, CREATE_DT, CREATED_BY, SYS_CREATE_TS, CUST_CHANNEL_ID) VALUES ((SELECT MAX(CUST_CHANNEL_ACCT_ID) + 1 FROM " + PHController.CoreSchemaName + ".CUST_CHANNEL_ACCOUNT), " + cNAccount.getCustId() + ", " + PHController.posChannelID + ", " + cNAccount.getAcctId() + ", NULL, 'A', 1, SYSDATE, 'SYSTEM', SYSDATE, 'SYSTEM', SYSDATE, " + cNUser.getCustChannelId() + ")", true))
        {
            updateNextEntityId("CUST_CHANNEL_ACCOUNT", "CUST_CHANNEL_ACCT_ID");
            return true;
        }
        return false;
    }

    public boolean updateChannelAccount(CNAccount cNAccount)
    {
        return executeUpdate("UPDATE " + PHController.CoreSchemaName + ".CUSTOMER_CHANNEL SET CHRG_ACCT_ID=" + cNAccount.getAcctId() + " WHERE CHANNEL_SCHEME_ID=" + PHController.posChannelSchemeID + " AND CUST_ID=" + cNAccount.getCustId(), true);
    }

    public boolean updateAccessName(String accessName, String accessCode)
    {
        return executeUpdate("UPDATE " + PHController.CoreSchemaName + ".CUSTOMER_CHANNEL_USER "
                + "SET ACCESS_NM='" + accessName + "' "
                + "WHERE CHANNEL_SCHEME_ID =" + PHController.posChannelSchemeID + " AND ACCESS_CD ='" + accessCode + "'", true);
    }

    public Long getEntity(String tableName)
    {
        Long nextNo = 0L;
        try ( ResultSet rs = executeQueryToResultSet("SELECT NEXT_NO  FROM " + PHController.CoreSchemaName + ".ENTITY WHERE  ENTITY_NM = '" + tableName + "'"))
        {
            if (rs.next())
            {
                nextNo = rs.getLong("NEXT_NO");
            }
        }
        catch (Exception ex)
        {
            logError(ex);
        }
        return nextNo;
    }

    public UCActivity queryCardActivity(String cardNumber, String procCode, String currencyCode)
    {
        UCActivity uCActivity = new UCActivity();
        try ( ResultSet rs = executeQueryToResultSet("SELECT COUNT(*) AS VELOCITY, NVL(SUM(TXN_AMOUNT),0) AS VOLUME FROM " + PHController.CMSchemaName + ".EI_POS_TXN_LOG WHERE TRAN_STATUS='APPROVED' AND CHANNEL_ID=" + PHController.posChannelID + " AND PROC_CODE='" + procCode + "' AND ACCESS_CODE='" + cardNumber + "' AND CURRENCY_CODE='" + currencyCode + "' AND TXN_DATE_TIME>=TRUNC(ADD_MONTHS(LAST_DAY(SYSDATE),-1)+1) AND TXN_DATE_TIME<=TRUNC(LAST_DAY(SYSDATE))"))
        {
            if (rs.next())
            {
                uCActivity.setVelocity(rs.getBigDecimal("VELOCITY"));
                uCActivity.setVolume(rs.getBigDecimal("VOLUME"));
            }
        }
        catch (Exception ex)
        {
            logError(ex);
        }
        return uCActivity;
    }

    public boolean upsertTerminal(EITerminal eITerminal)
    {
        if (checkIfExists("SELECT TERMINAL_ID FROM " + PHController.CMSchemaName + ".EI_TERMINAL WHERE TERMINAL_ID='" + eITerminal.getTerminalId() + "' AND CHANNEL_CODE='" + eITerminal.getChannelCode() + "'"))
        {
            return updateTerminal(eITerminal);
        }
        else
        {
            return saveTerminal(eITerminal);
        }
    }

    private boolean saveTerminal(EITerminal eITerminal)
    {

        if (executeUpdate("INSERT INTO " + PHController.CMSchemaName + ".EI_TERMINAL"
                + "(TERMINAL_ID, CHANNEL_CODE, LOCATION, OPERATOR, BU_NO, BU_NM, MODIFIED_BY, DATE_MODIFIED, REC_ST) "
                + "VALUES('" + eITerminal.getTerminalId() + "', '" + eITerminal.getChannelCode() + "', '" + eITerminal.getLocation() + "',"
                + " '" + eITerminal.getOperator() + "', '" + eITerminal.getBuCode() + "', '" + eITerminal.getBuName().replace("'", "") + "', "
                + "'" + eITerminal.getModifiedBy() + "', SYSDATE, '" + eITerminal.getStatus() + "')", true))
        {
            return saveTerminalAccounts(eITerminal);
        }
        return false;
    }

    private boolean updateTerminal(EITerminal eITerminal)
    {
        if (executeUpdate("UPDATE " + PHController.CMSchemaName + ".EI_TERMINAL SET CHANNEL_CODE='" + eITerminal.getChannelCode() + "', LOCATION='" + eITerminal.getLocation() + "', OPERATOR='" + eITerminal.getOperator() + "', BU_NO='" + eITerminal.getBuCode() + "', BU_NM='" + eITerminal.getBuName().replace("'", "") + "', MODIFIED_BY='" + eITerminal.getModifiedBy() + "', DATE_MODIFIED=" + formatDate(eITerminal.getDateModified()) + ", REC_ST='" + eITerminal.getStatus() + "' WHERE TERMINAL_ID='" + eITerminal.getTerminalId() + "'", true))
        {
            return saveTerminalAccounts(eITerminal);
        }
        return false;
    }

    private boolean saveTerminalAccounts(EITerminal eITerminal)
    {
        boolean RC = deleteTerminalAccounts(eITerminal);
        for (TMAccount tMAccount : eITerminal.getAccounts().values())
        {
            RC = executeUpdate("INSERT INTO " + PHController.CMSchemaName + ".EI_TERMINAL_ACCOUNT"
                    + "(TERMINAL_ID, CRNCY_CODE, ACCT_NO, ACCT_NM) VALUES"
                    + "('" + eITerminal.getTerminalId() + "', '" + tMAccount.getCurrency() + "', '" + tMAccount.getAccountNumber() + "', '" + tMAccount.getAccountName() + "')", true);
        }
        return RC;
    }

    private boolean deleteTerminalAccounts(EITerminal eITerminal)
    {
        return executeUpdate("DELETE " + PHController.CMSchemaName + ".EI_TERMINAL_ACCOUNT WHERE TERMINAL_ID='" + eITerminal.getTerminalId() + "'", true);
    }

    public HashMap<String, EITerminal> loadTerminals(String channelCode)
    {
        HashMap<String, EITerminal> eITerminals = new HashMap<>();
        try ( ResultSet rs = executeQueryToResultSet("SELECT A.TERMINAL_ID, A.CHANNEL_CODE, A.LOCATION, "
                + "A.OPERATOR, A.BU_NO, A.BU_NM, A.MODIFIED_BY, A.DATE_MODIFIED, A.REC_ST FROM " + PHController.CMSchemaName + ".EI_TERMINAL A "
                + "WHERE A.CHANNEL_CODE='" + channelCode + "' AND (A.REC_ST = 'ACTIVE' OR A.REC_ST = 'Active' OR A.REC_ST = 'Closed' or A.REC_ST = 'CLOSED')  ORDER BY A.TERMINAL_ID ASC"))
//        try (ResultSet rs = executeQueryToResultSet("SELECT A.TERMINAL_ID, A.CHANNEL_CODE, A.LOCATION, "
//                + "A.OPERATOR, A.BU_NO, A.BU_NM, A.MODIFIED_BY, A.DATE_MODIFIED, A.REC_ST FROM " + PHController.CMSchemaName + ".EI_TERMINAL A "
//                + "WHERE A.CHANNEL_CODE='" + channelCode + "' AND (A.REC_ST = 'ACTIVE' OR A.REC_ST = 'Active') ORDER BY A.TERMINAL_ID ASC"))
        {
            while (rs.next())
            {
                EITerminal eITerminal = new EITerminal();
                eITerminal.setTerminalId(rs.getString("TERMINAL_ID"));
                eITerminal.setChannelCode(rs.getString("CHANNEL_CODE"));
                eITerminal.setLocation(rs.getString("LOCATION"));
                eITerminal.setOperator(rs.getString("OPERATOR"));
                eITerminal.setBuCode(rs.getString("BU_NO"));
                eITerminal.setBuName(rs.getString("BU_NM"));
                eITerminal.setModifiedBy(rs.getString("MODIFIED_BY"));
                eITerminal.setDateModified(rs.getDate("DATE_MODIFIED"));
                eITerminal.setStatus(rs.getString("REC_ST"));
                eITerminals.put(eITerminal.getTerminalId(), eITerminal);
            }
            eITerminals.values().stream().map((eITerminal)
                    ->
            {
                eITerminal.setAccounts(queryTerminalAccounts(eITerminal.getTerminalId()));
                return eITerminal;
            }).forEach((eITerminal)
                    ->
            {
                eITerminals.put(eITerminal.getTerminalId(), eITerminal);
            });
        }
        catch (Exception ex)
        {
            logError(ex);
        }
        return eITerminals;
    }

    public HashMap<String, TMAccount> queryTerminalAccounts(String terminalId)
    {
        HashMap<String, TMAccount> accounts = new HashMap<>();
        try ( ResultSet rs = executeQueryToResultSet("SELECT TERMINAL_ID, CRNCY_CODE, ACCT_NO, ACCT_NM FROM " + PHController.CMSchemaName + ".EI_TERMINAL_ACCOUNT WHERE TERMINAL_ID='" + terminalId + "' ORDER BY CRNCY_CODE ASC"))
        {
            while (rs.next())
            {
                TMAccount tMAccount = new TMAccount();
                tMAccount.setTerminalId(rs.getString("TERMINAL_ID"));
                tMAccount.setCurrency(rs.getString("CRNCY_CODE"));
                tMAccount.setAccountNumber(rs.getString("ACCT_NO"));
                tMAccount.setAccountName(rs.getString("ACCT_NM"));
                accounts.put(tMAccount.getCurrency(), tMAccount);
            }
        }
        catch (Exception ex)
        {
            logError(ex);
        }
        return accounts;
    }

    public HashMap<String, EIProCodesMOB> loadProcessingCodes(String channelCode)
    {
        HashMap<String, EIProCodesMOB> eIProcCodes = new HashMap<>();
        try ( ResultSet rs = executeQueryToResultSet("SELECT PROC_CODE,PROC_TYPE,CHANNEL_CODE,PROC_DESC,BU_NO,BU_NM,DATE_MODIFIED,"
                + "MODIFIED_BY,CURRENCY_CODE,REC_ST "
                + "FROM   " + PHController.CMSchemaName + ".EI_PROC_CODE_CONFIG WHERE REC_ST = 'Active' AND CHANNEL_CODE = '" + channelCode + "' ORDER BY PROC_CODE ASC"))
        {
            while (rs.next())
            {
                EIProCodesMOB eIProcCode = new EIProCodesMOB();
                eIProcCode.setProcCode(rs.getString("PROC_CODE"));
                eIProcCode.setProcType(rs.getString("PROC_TYPE"));
                eIProcCode.setModule(rs.getString("CHANNEL_CODE"));
                eIProcCode.setProcDesc(rs.getString("PROC_DESC"));
                eIProcCode.setBuCode(rs.getString("BU_NO"));
                eIProcCode.setBuName(rs.getString("BU_NM"));
                eIProcCode.setDateModified(rs.getDate("DATE_MODIFIED"));
                eIProcCode.setModifiedBy(rs.getString("MODIFIED_BY"));
                eIProcCode.setRecStatus(rs.getString("REC_ST"));
                eIProcCodes.put(eIProcCode.getProcCode(), eIProcCode);
            }
        }
        catch (Exception ex)
        {
            logError(ex);
        }
        return eIProcCodes;

    }

    public HashMap<String, EIBillerCode> loadBillerCodes(String channelCode)
    {
        HashMap<String, EIBillerCode> eIBillerCodes = new HashMap<>();
        try ( ResultSet rs = executeQueryToResultSet("SELECT BILLER_CODE,BILLER_DESC,CHANNEL_CODE,ASSOC_PROC_CODE,BU_NO,BU_NM, "
                + "DATE_MODIFIED,MODIFIED_BY,CURRENCY_CODE,ASSOC_ACCT_NO,ASSOC_ACCT_NM,REC_ST,NARRATION_CODE "
                + "FROM  " + PHController.CMSchemaName + ".EI_BILLER_CODE WHERE REC_ST ='Active' AND CHANNEL_CODE = '" + channelCode + "'  ORDER BY BILLER_CODE ASC"))
        {
            while (rs.next())
            {
                EIBillerCode eIbillerCode = new EIBillerCode();
                eIbillerCode.setBillerCode(rs.getString("BILLER_CODE"));
                eIbillerCode.setBillerDesc(rs.getString("BILLER_DESC"));
                eIbillerCode.setModule(rs.getString("CHANNEL_CODE"));
                eIbillerCode.setAssocProcCode(rs.getString("ASSOC_PROC_CODE"));
                eIbillerCode.setBuCode(rs.getString("BU_NO"));
                eIbillerCode.setBuName(rs.getString("BU_NM"));
                eIbillerCode.setDateModified(rs.getDate("DATE_MODIFIED"));
                eIbillerCode.setModifiedBy(rs.getString("MODIFIED_BY"));
                eIbillerCode.setCurrency(rs.getString("CURRENCY_CODE"));
                eIbillerCode.setAssocAcctNo(rs.getString("ASSOC_ACCT_NO"));
                eIbillerCode.setAssocAcctNm(rs.getString("ASSOC_ACCT_NM"));
                eIbillerCode.setRecStatus(rs.getString("REC_ST"));
                eIbillerCode.setNtvCodeField(rs.getString("NARRATION_CODE"));
                eIBillerCodes.put(eIbillerCode.getNtvCodeField(), eIbillerCode);
            }
        }
        catch (Exception ex)
        {
            logError(ex);
        }
        return eIBillerCodes;

    }

    public boolean upsertProcCode(EIProCodesMOB eIProCodes)
    {
        if (checkIfExists("SELECT PROC_CODE FROM " + PHController.CMSchemaName + ".EI_PROC_CODE_CONFIG WHERE PROC_CODE='" + eIProCodes.getProcCode() + "' AND CHANNEL_CODE='" + eIProCodes.getModule() + "'"))
        {
            return updateProcCode(eIProCodes);
        }
        else
        {
            return saveProcCode(eIProCodes);
        }
    }

    public boolean upsertBillerCode(EIBillerCode eIBillerCode)
    {
        if (checkIfExists("SELECT BILLER_CODE FROM " + PHController.CMSchemaName + ".EI_BILLER_CODE WHERE BILLER_CODE='" + eIBillerCode.getBillerCode() + "' AND CHANNEL_CODE='" + eIBillerCode.getModule() + "'"))
        {
            return updateBillerCode(eIBillerCode);
        }
        else
        {
            return saveBillerCode(eIBillerCode);
        }
    }

    private boolean saveProcCode(EIProCodesMOB eIProCodes)
    {
        if (executeUpdate("INSERT INTO " + PHController.CMSchemaName + ".EI_PROC_CODE_CONFIG"
                + "(PROC_CODE, PROC_TYPE, PROC_DESC, CHANNEL_CODE, BU_NO, BU_NM, MODIFIED_BY, DATE_MODIFIED, CURRENCY_CODE, REC_ST) "
                + "VALUES('" + eIProCodes.getProcCode() + "', '" + eIProCodes.getProcType() + "', '" + eIProCodes.getProcDesc() + "',"
                + " '" + eIProCodes.getModule() + "', '" + eIProCodes.getBuCode() + "', '" + eIProCodes.getBuName().replace("'", "") + "', "
                + "'" + eIProCodes.getModifiedBy() + "', SYSDATE, 'ZMW','" + eIProCodes.getRecStatus() + "')", true))
        {
            return Boolean.TRUE;
        }
        return false;
    }

    private boolean updateProcCode(EIProCodesMOB eIProCodes)
    {
        if (executeUpdate("UPDATE " + PHController.CMSchemaName + ".EI_PROC_CODE_CONFIG "
                + "SET PROC_CODE='" + eIProCodes.getProcCode() + "', PROC_TYPE='" + eIProCodes.getProcType() + "', PROC_DESC='" + eIProCodes.getProcDesc()
                + "', BU_NO='" + eIProCodes.getBuCode() + "', BU_NM='" + eIProCodes.getBuName().replace("'", "") + "', MODIFIED_BY='" + eIProCodes.getModifiedBy()
                + "', DATE_MODIFIED=" + formatDate(eIProCodes.getDateModified()) + ", REC_ST='" + eIProCodes.getRecStatus() + "' "
                + "WHERE PROC_CODE='" + eIProCodes.getProcCode() + "'", true))
        {
            return Boolean.TRUE;
        }
        return false;
    }

    private boolean saveBillerCode(EIBillerCode eIBillerCode)
    {
        if (executeUpdate("INSERT INTO " + PHController.CMSchemaName + ".EI_BILLER_CODE "
                + "(BILLER_CODE,BILLER_DESC,ASSOC_PROC_CODE,CHANNEL_CODE,BU_NO,BU_NM,MODIFIED_BY,DATE_MODIFIED,CURRENCY_CODE,ASSOC_ACCT_NO,ASSOC_ACCT_NM,REC_ST,NARRATION_CODE) "
                + "VALUES('" + eIBillerCode.getBillerCode() + "',  '" + eIBillerCode.getBillerDesc() + "','" + eIBillerCode.getAssocProcCode() + "', "
                + " '" + eIBillerCode.getModule() + "', '" + eIBillerCode.getBuCode() + "', '" + eIBillerCode.getBuName().replace("'", "") + "', "
                + "'" + eIBillerCode.getModifiedBy() + "', SYSDATE, '" + eIBillerCode.getCurrency() + "','" + eIBillerCode.getAssocAcctNo() + "','" + eIBillerCode.getAssocAcctNm() + "','" + eIBillerCode.getRecStatus() + "','" + eIBillerCode.getNtvCodeField() + "')", true))
        {
            return Boolean.TRUE;
        }
        return false;
    }

    private boolean updateBillerCode(EIBillerCode eIBillerCode)
    {
        if (executeUpdate("UPDATE " + PHController.CMSchemaName + ".EI_BILLER_CODE "
                + "SET BILLER_CODE='" + eIBillerCode.getBillerCode() + "', BILLER_DESC='" + eIBillerCode.getBillerDesc() + "', ASSOC_PROC_CODE='" + eIBillerCode.getAssocProcCode()
                + "', BU_NO='" + eIBillerCode.getBuCode() + "', BU_NM='" + eIBillerCode.getBuName().replace("'", "") + "', MODIFIED_BY='" + eIBillerCode.getModifiedBy()
                + "', DATE_MODIFIED=" + formatDate(eIBillerCode.getDateModified()) + ",CURRENCY_CODE = '" + eIBillerCode.getCurrency() + "',ASSOC_ACCT_NO = '" + eIBillerCode.getAssocAcctNo() + "',ASSOC_ACCT_NM='" + eIBillerCode.getAssocAcctNm() + "', REC_ST='" + eIBillerCode.getRecStatus() + "',NARRATION_CODE = '" + eIBillerCode.getNtvCodeField() + "' "
                + "WHERE BILLER_CODE='" + eIBillerCode.getBillerCode() + "'", true))
        {
            return Boolean.TRUE;
        }
        return false;
    }

    public String removeSpaces(String text)
    {
        text = text != null ? text : "";
        StringBuilder buffer = new StringBuilder();
        StringTokenizer tokenizer = new StringTokenizer(text);
        try
        {
            while (tokenizer.hasMoreTokens())
            {
                buffer.append(" ").append(tokenizer.nextToken());
            }
        }
        catch (Exception ex)
        {
            logError(ex);
        }
        return buffer.toString().trim();
    }

    public String padString(String s, int i, char c, boolean leftPad)
    {
        StringBuilder buffer = new StringBuilder(s);
        int j = buffer.length();
        if (i > 0 && i > j)
        {
            for (int k = 0; k <= i; k++)
            {
                if (leftPad)
                {
                    if (k < i - j)
                    {
                        buffer.insert(0, c);
                    }
                }
                else if (k > j)
                {
                    buffer.append(c);
                }
            }
        }
        return buffer.toString();
    }

    private int countDecimalPlaces(BigDecimal bigDecimal)
    {
        String string = bigDecimal.stripTrailingZeros().toPlainString();
        int index = string.indexOf(".");
        return index < 0 ? 0 : string.length() - index - 1;
    }

    public String capitalize(String text)
    {
        if (text != null)
        {
            try
            {
                StringBuilder builder = new StringBuilder();
                for (String word : text.toLowerCase().split("\\s"))
                {
                    builder.append(word.substring(0, 1).toUpperCase()).append(word.substring(1).toLowerCase()).append(" ");
                }
                return builder.toString();
            }
            catch (Exception ex)
            {
                logError(ex);
                return text;
            }
        }
        return text;
    }

    public String capitalize(String text, int minLen)
    {
        if (text != null)
        {
            try
            {
                StringBuilder builder = new StringBuilder();
                for (String word : text.split("\\s"))
                {
                    builder.append(word.length() > minLen ? capitalize(word) : word).append(" ");
                }
                return builder.toString();
            }
            catch (Exception ex)
            {
                logError(ex);
                return text;
            }
        }
        return text;
    }

    /**
     * @return the xapiCaller
     */
    public XAPICaller getXapiCaller()
    {
        return xapiCaller;
    }

    /**
     * @param xapiCaller the xapiCaller to set
     */
    public void setXapiCaller(XAPICaller xapiCaller)
    {
        this.xapiCaller = xapiCaller;
    }

    public String formatIsoAmount(BigDecimal amount)
    {
        String amtStr = padString(amount.abs().setScale(2, BigDecimal.ROUND_DOWN).toPlainString().replace(".", ""), 12, '0', true);
        return amtStr.substring(amtStr.length() - 12);
    }

    public String mapTxnTypeCode(String narration, String DRCR)
    {
        DRCR = "C".equalsIgnoreCase(DRCR.trim()) ? "CR" : DRCR.trim();
        DRCR = "D".equalsIgnoreCase(DRCR.trim()) ? "DR" : DRCR.trim();

        if (narration.toUpperCase().contains("PAYMENT") && !narration.toUpperCase().contains("CHARGE"))
        {
            return "157";
        }
        else if (narration.toUpperCase().contains("WITHDRAWAL") && !narration.toUpperCase().contains("CHARGE"))
        {
            return "160";
        }
        else if (narration.toUpperCase().contains("DEPOSIT") && !narration.toUpperCase().contains("CHARGE"))
        {
            return "105";
        }
        else if ((narration.toUpperCase().contains("TRANSFER") || narration.toUpperCase().contains("TFR")) && "D".equalsIgnoreCase(DRCR.trim()))
        {
            return "174";
        }
        else if ((narration.toUpperCase().contains("TRANSFER") || narration.toUpperCase().contains("TFR")) && "C".equalsIgnoreCase(DRCR.trim()))
        {
            return "113";
        }
        else if (narration.toUpperCase().contains("CHARGE"))
        {
            return "150";
        }
        else if (narration.toUpperCase().contains("AIRTIME"))
        {
            return "156";
        }
        return "CR".equalsIgnoreCase(DRCR) ? "113" : "174";
    }

    public boolean isBiometricsRegistered(String finType, String customerNumber)
    {
        return checkIfExists("SELECT CUST_ID FROM " + PHController.CoreSchemaName + ".V_CUSTOMER_IMAGES WHERE IMAGE_TY='" + finType + "' AND CUST_ID = (SELECT CUST_ID FROM " + PHController.CoreSchemaName + ".CUSTOMER WHERE CUST_NO='" + customerNumber + "')");
    }

    public boolean isAccountAllowed(String accountNumber)
    {
        boolean accountAllowed = checkIfExists("SELECT ACCT_NO FROM " + PHController.CoreSchemaName + ".V_ACCOUNTS WHERE PROD_CAT_TY='DP' AND ACCT_NO='" + accountNumber + "' AND PROD_ID IN (" + PHController.posAllowedProductIDs + ") AND REC_ST='A'");
        if (!accountAllowed)
        {
            xapiCaller.setXapiRespCode(EICodes.INVALID_ACCOUNT);
        }
        return accountAllowed;
    }

    public String extractJournalId(String retrievalRef)
    {
        if (retrievalRef == null)
        {
            return null;
        }
        if (retrievalRef.toUpperCase().contains("<TXNID>") && retrievalRef.toUpperCase().contains("</TXNID>"))
        {
            try
            {
                return retrievalRef.substring(retrievalRef.toUpperCase().indexOf("<TXNID>") + 7, retrievalRef.toUpperCase().indexOf("</TXNID>"));
            }
            catch (Exception ex)
            {
                xapiCaller.setXapiRespCode(EICodes.TRANSACTION_NOT_ALLOWED_FOR_ACCOUNT);
                logError(ex);
            }
        }
        return retrievalRef.length() > 25 ? retrievalRef.substring(0, 25).trim() : retrievalRef.trim();
    }

    public String fetchJournalId(TXRequest tXRequest)
    {
        String journalId = null;
        try ( ResultSet resultSet = executeQueryToResultSet("SELECT MAX(TRAN_JOURNAL_ID) TRAN_JOURNAL_ID FROM " + PHController.CoreSchemaName + ".TXN_JOURNAL WHERE ACCT_NO='" + tXRequest.getDebitAccount() + "' AND TRAN_AMT=" + tXRequest.getTxnAmount() + " AND TRAN_DESC='" + tXRequest.getTxnNarration() + "' AND SYS_CREATE_TS > TO_DATE(SYSDATE-1)"))
        {
            if (resultSet.next())
            {
                journalId = resultSet.getString("TRAN_JOURNAL_ID");
            }
        }
        catch (Exception ex)
        {
            logError(ex);
        }
        return journalId;
    }

    public Date getProcessingDate()
    {
        Date currentDate = new Date();
        try ( ResultSet rs = executeQueryToResultSet("SELECT TO_DATE(DISPLAY_VALUE,'DD/MM/YYYY') AS PROC_DATE FROM " + PHController.CoreSchemaName + ".CTRL_PARAMETER WHERE PARAM_CD = 'S02'"))
        {
            if (rs.next())
            {
                currentDate = rs.getDate("PROC_DATE");
            }
        }
        catch (Exception ex)
        {
            logError(ex);
        }
        return currentDate;
    }

    public Date getSystemDate()
    {
        Date currentDate = new Date();
        try ( ResultSet rs = executeQueryToResultSet("SELECT SYSDATE AS SYS_DATE FROM DUAL"))
        {
            if (rs.next())
            {
                currentDate = rs.getDate("SYS_DATE");
            }
        }
        catch (Exception ex)
        {
            logError(ex);
        }
        return currentDate;
    }

    /**
     * @return the cNCurrencies
     */
    public ArrayList<CNCurrency> getcNCurrencies()
    {
        return cNCurrencies;
    }

    public void setcNCurrencies()
    {
        this.cNCurrencies = queryCurrencies();
    }

    public Long getChannelBuId(Long channelId)
    {
        Long buId = null;
        try ( ResultSet rs = executeQueryToResultSet("SELECT ORIGIN_BU_ID FROM " + PHController.CoreSchemaName + ".SERVICE_CHANNEL WHERE CHANNEL_ID=" + channelId))
        {
            if (rs.next())
            {
                buId = rs.getLong("ORIGIN_BU_ID");
            }
        }
        catch (Exception ex)
        {
            logError(ex);
        }
        return buId;
    }

    public long getAccountId(String accountNumber)
    {
        long acctId = 0L;
        try ( ResultSet rs = executeQueryToResultSet("SELECT ACCT_ID FROM " + PHController.CoreSchemaName + ".ACCOUNT WHERE ACCT_NO='" + accountNumber + "'"))
        {
            if (rs.next())
            {
                acctId = rs.getLong("ACCT_ID");
            }
        }
        catch (Exception ex)
        {
            logError(ex);
        }
        return acctId;
    }

    public int getAccountProductId(String accountNumber)
    {
        int productId = 0;
        try ( ResultSet rs = executeQueryToResultSet("SELECT PROD_ID FROM " + PHController.CoreSchemaName + ".ACCOUNT WHERE ACCT_NO='" + accountNumber + "'"))
        {
            if (rs.next())
            {
                productId = rs.getInt("PROD_ID");
            }
        }
        catch (Exception ex)
        {
            logError(ex);
        }
        return productId;
    }

    public String queryControlParameter(String parameterCode)
    {
        String parameterValue = "";
        try ( ResultSet rs = executeQueryToResultSet("SELECT PARAM_VALUE FROM " + PHController.CoreSchemaName + ".CTRL_PARAMETER WHERE PARAM_CD = '" + parameterCode + "'"))
        {
            if (rs.next())
            {
                parameterValue = rs.getString("PARAM_VALUE");
            }
        }
        catch (Exception ex)
        {
            logError(ex);
        }
        return parameterValue;
    }

    public boolean isAccountValid(String accountNumber)
    {
        return recordExists("SELECT ACCT_NO FROM " + PHController.CoreSchemaName + ".V_ACCOUNTS WHERE PROD_CAT_TY='DP' AND ACCT_NO='" + accountNumber + "' AND REC_ST IN ('A')");
    }

    public boolean isChequeNumberValid(CNAccount cNAccount, long chequeNumber)
    {
        return recordExists("SELECT CHQ_NO FROM " + PHController.CoreSchemaName + ".V_ACCOUNT_CHEQUE_INVENTORY WHERE DEPOSIT_ACCT_ID=" + cNAccount.getAcctId() + " AND CHQ_NO=" + chequeNumber);
    }

    public String fetchJournalId(String accountNumber, BigDecimal txnAmount)
    {
        String journalId = null;
        try ( ResultSet rs = executeQueryToResultSet("SELECT MAX(TRAN_JOURNAL_ID) TRAN_JOURNAL_ID FROM " + PHController.CoreSchemaName + ".TXN_JOURNAL WHERE ACCT_NO='" + accountNumber + "' AND TRAN_AMT=" + txnAmount + " AND SYS_CREATE_TS > TO_DATE(SYSDATE-1)"))
        {
            if (rs.next())
            {
                journalId = rs.getString("TRAN_JOURNAL_ID");
            }
        }
        catch (Exception ex)
        {
            logError(ex);
        }
        return journalId;
    }

    public BigDecimal queryTodaysTotalTxnAmount(CNAccount cNAccount, String menuCode)
    {
        BigDecimal totalAmount = BigDecimal.ZERO;
        try ( ResultSet rs = executeQueryToResultSet("SELECT NVL(SUM(TXN_AMOUNT),0) AS TOTAL_AMOUNT FROM " + PHController.CMSchemaName + ".EI_POS_TXN_LOG WHERE ACCOUNT_NUMBER = '" + cNAccount.getAccountNumber() + "' AND TXN_DATE_TIME >= " + formatDate(getSystemDate()) + " AND CHANNEL_ID = " + PHController.posChannelID + " AND TRAN_STATUS = 'APPROVED' AND PROC_CODE = '" + menuCode + "'"))
        {
            if (rs.next())
            {
                totalAmount = rs.getBigDecimal("TOTAL_AMOUNT");
            }
        }
        catch (Exception ex)
        {
            logError(ex);
        }
        return totalAmount;
    }

    public ArrayList<Long> queryChequeBookProducts()
    {
        ArrayList<Long> prodList = new ArrayList<>();
        try ( ResultSet rs = executeQueryToResultSet("SELECT DISTINCT PROD_ID FROM " + PHController.CoreSchemaName + ".PRODUCT_CHEQUE_BOOK WHERE REC_ST='A'"))
        {
            while (rs.next())
            {
                prodList.add(rs.getLong("PROD_ID"));
            }
        }
        catch (Exception ex)
        {
            logError(ex);
        }
        return prodList;
    }

    public String getChannelContraGL(long channelId, long buId)
    {
        String drContraGL = null;
        try ( ResultSet rs = executeQueryToResultSet("SELECT GL_DR_ACCT FROM " + PHController.CoreSchemaName + ".SERVICE_CHANNEL WHERE CHANNEL_ID=" + channelId))
        {
            if (rs.next())
            {
                drContraGL = rs.getString("GL_DR_ACCT");
            }
        }
        catch (Exception ex)
        {
            logError(ex);
        }
        return unmaskGLAccount(drContraGL, buId);
    }

    public boolean updateStoppedCheque(CNAccount cNAccount, long chequeNumber, String note)
    {
        return executeUpdate("UPDATE " + PHController.CoreSchemaName + ".DEPOSIT_ACCOUNT_STOP_CHEQUE SET NOTE='" + note + "', ISSUE_DT=SYSDATE WHERE FROM_CHQ_NO=" + chequeNumber + " AND DEPOSIT_ACCT_ID=" + cNAccount.getAcctId(), true);
    }

    private String formatDate(Date date)
    {
        if (date != null)
        {
            return "TO_DATE('" + new SimpleDateFormat("dd-MM-yyyy").format(date) + "','DD-MM-YYYY')";
        }
        return null;
    }

    public CNAccount queryDepositAccount(String accountNumber)
    {
        CNAccount cNAccount = new CNAccount();
        try ( ResultSet rs = executeQueryToResultSet("SELECT C.ACCT_ID, C.CUST_ID, C.MAIN_BRANCH_ID, C.PROD_ID, C.ACCT_NO, C.ACCT_NM, E.CRNCY_CD, C.PROD_CAT_TY,C.REC_ST FROM " + PHController.CoreSchemaName + ".ACCOUNT C, " + PHController.CoreSchemaName + ".CURRENCY E WHERE C.ACCT_NO='" + accountNumber + "' AND C.REC_ST in ('A','U','D','N','Q','C','L') AND C.PROD_CAT_TY='DP' AND C.CRNCY_ID = E.CRNCY_ID"))
        {
            if (rs.next())
            {
                cNAccount.setAcctId(rs.getLong("ACCT_ID"));
                cNAccount.setCustId(rs.getLong("CUST_ID"));
                cNAccount.setBuId(rs.getLong("MAIN_BRANCH_ID"));
                cNAccount.setProductId(rs.getLong("PROD_ID"));
                cNAccount.setAccountNumber(rs.getString("ACCT_NO"));
                cNAccount.setAccountType(rs.getString("PROD_CAT_TY"));
                cNAccount.setShortName(removeSpaces(rs.getString("ACCT_NM")));
                cNAccount.setAccountName(removeSpaces(rs.getString("ACCT_NM")));
                cNAccount.setCurrency(queryCurrency(rs.getString("CRNCY_CD")));
                cNAccount.setAcctStatus(rs.getString("REC_ST"));
            }
        }
        catch (Exception ex)
        {
            logError(ex);
        }
        return cNAccount;
    }

    public boolean blockedDepositAccount(String accountNumber, String blockedSt)
    {
        if (blockedSt.equals("ALLCR"))
        {
            return recordExists("SELECT ACCT_ID FROM " + PHController.CoreSchemaName + ".ACCOUNT_BLOCK WHERE ACCT_ID = (SELECT ACCT_ID FROM " + PHController.CoreSchemaName + ".ACCOUNT WHERE ACCT_NO ='" + accountNumber + "') AND BLOCK_TY_CD IN ('ALL','ALLCR') AND REC_ST ='A'");
        }
        else if (blockedSt.equals("ALLDR"))
        {
            return recordExists("SELECT ACCT_ID FROM " + PHController.CoreSchemaName + ".ACCOUNT_BLOCK WHERE ACCT_ID = (SELECT ACCT_ID FROM " + PHController.CoreSchemaName + ".ACCOUNT WHERE ACCT_NO ='" + accountNumber + "') AND BLOCK_TY_CD IN ('ALL','ALLDR') AND REC_ST ='A'");
        }
        else
        {
            return true;
        }
    }

    public boolean acctStatusUnfunded(String accountNumber)
    {
        return recordExists("SELECT ACCT_ID FROM " + PHController.CoreSchemaName + ".ACCOUNT WHERE acct_no ='" + accountNumber + "' AND REC_ST ='U'");
    }

    public boolean activateUnfundedAccount(String acctNo)
    {
        return executeUpdate("UPDATE " + PHController.CoreSchemaName + ".ACCOUNT SET REC_ST='A' where acct_no = '" + acctNo + "'", true);
    }

    public CNCustomer queryCustomerByActNo(String acctNo)
    {
        CNCustomer cNCustomer = new CNCustomer();
//        try (ResultSet rs = executeQueryToResultSet("SELECT   CI.CUST_ID, REPLACE (CI.SOCIAL_SECURITY_NO, '/') AS ID_NO,CI.CUST_NO,CI.BU_ID,CI.CUST_CAT,CI.CUST_NM,   ''  AS CONTACT  "
//                + "FROM   " + PHController.CoreSchemaName + ".V_customer CI,  " + PHController.CoreSchemaName + ".ACCOUNT AC "
//                + "WHERE   CI.CUST_ID = AC.CUST_ID   AND CI.REC_ST = 'A'  AND AC.PROD_CAT_TY = 'DP'   AND AC.ACCT_NO = '" + acctNo + "'"
//        ))
        try ( ResultSet rs = executeQueryToResultSet("SELECT   CI.CUST_ID,REPLACE (CI.SOCIAL_SECURITY_NO, '/') AS ID_NO,CI.CUST_NO,CI.BU_ID,CI.CUST_CAT,CI.CUST_NM, "
                + "  NVL ((SELECT   U.ACCESS_CD  FROM   " + PHController.CoreSchemaName + ".CUSTOMER_CHANNEL_USER U  WHERE       U.CUST_ID = CI.CUST_ID "
                + " AND U.USER_CAT_CD = 'PER'  AND U.CHANNEL_ID = 9  AND ROWNUM = 1),  (SELECT   O.CONTACT  FROM   " + PHController.CoreSchemaName + ".V_CUSTOMER_CONTACT_MODE O "
                + " WHERE       O.CUST_ID = CI.CUST_ID  AND O.CONTACT_MODE_CAT_CD IN ('MOBPHONE', 'TELPHONE')  AND ROWNUM = 1))  AS CONTACT "
                + "  FROM   " + PHController.CoreSchemaName + ".V_customer CI,  " + PHController.CoreSchemaName + ".ACCOUNT AC WHERE   CI.CUST_ID = AC.CUST_ID   AND CI.REC_ST = 'A'  AND AC.PROD_CAT_TY = 'DP'   AND AC.ACCT_NO = '" + acctNo + "'"
        ))
        {
            if (rs.next())
            {
                cNCustomer.setCustId(rs.getLong("CUST_ID"));
                cNCustomer.setBuId(rs.getLong("BU_ID"));
                cNCustomer.setIdNo(rs.getString("ID_NO"));
                cNCustomer.setCustNo(rs.getString("CUST_NO"));
                cNCustomer.setCustCat(rs.getString("CUST_CAT"));
                cNCustomer.setCustName(rs.getString("CUST_NM"));
                cNCustomer.setContact(rs.getString("CONTACT"));
            }
        }
        catch (Exception ex)
        {
            logError(ex);
        }
        return cNCustomer;
    }

    public CNAccount queryDepositAccount(Long acctId)
    {
        CNAccount cNAccount = new CNAccount();
        try ( ResultSet rs = executeQueryToResultSet("SELECT C.ACCT_ID, C.CUST_ID, C.MAIN_BRANCH_ID, C.PROD_ID, C.ACCT_NO, C.ACCT_NM, E.CRNCY_CD, C.PROD_CAT_TY FROM " + PHController.CoreSchemaName + ".ACCOUNT C, " + PHController.CoreSchemaName + ".CURRENCY E WHERE C.ACCT_ID=" + acctId + " AND C.REC_ST='A' AND C.PROD_CAT_TY='DP' AND C.CRNCY_ID = E.CRNCY_ID"))
        {
            if (rs.next())
            {
                cNAccount.setAcctId(rs.getLong("ACCT_ID"));
                cNAccount.setCustId(rs.getLong("CUST_ID"));
                cNAccount.setBuId(rs.getLong("MAIN_BRANCH_ID"));
                cNAccount.setProductId(rs.getLong("PROD_ID"));
                cNAccount.setAccountNumber(rs.getString("ACCT_NO"));
                cNAccount.setAccountType(rs.getString("PROD_CAT_TY"));
                cNAccount.setShortName(removeSpaces(rs.getString("ACCT_NM")));
                cNAccount.setAccountName(removeSpaces(rs.getString("ACCT_NM")));
                cNAccount.setCurrency(queryCurrency(rs.getString("CRNCY_CD")));
            }
        }
        catch (Exception ex)
        {
            logError(ex);
        }
        return cNAccount;
    }

    public CNAccount queryLoanAccount(String accountNumber)
    {
        CNAccount cNAccount = new CNAccount();
        try ( ResultSet rs = executeQueryToResultSet("SELECT C.ACCT_ID, C.CUST_ID, C.MAIN_BRANCH_ID, C.PROD_ID, C.ACCT_NO, C.ACCT_NM, E.CRNCY_CD, C.PROD_CAT_TY FROM " + PHController.CoreSchemaName + ".ACCOUNT C, " + PHController.CoreSchemaName + ".CURRENCY E WHERE C.ACCT_NO='" + accountNumber + "' AND C.REC_ST='A' AND C.PROD_CAT_TY='LN' AND C.CRNCY_ID = E.CRNCY_ID"))
        {
            if (rs.next())
            {
                cNAccount.setAcctId(rs.getLong("ACCT_ID"));
                cNAccount.setCustId(rs.getLong("CUST_ID"));
                cNAccount.setBuId(rs.getLong("MAIN_BRANCH_ID"));
                cNAccount.setProductId(rs.getLong("PROD_ID"));
                cNAccount.setAccountNumber(rs.getString("ACCT_NO"));
                cNAccount.setAccountType(rs.getString("PROD_CAT_TY"));
                cNAccount.setShortName(removeSpaces(rs.getString("ACCT_NM")));
                cNAccount.setAccountName(removeSpaces(rs.getString("ACCT_NM")));
                cNAccount.setCurrency(queryCurrency(rs.getString("CRNCY_CD")));
            }
        }
        catch (Exception ex)
        {
            logError(ex);
        }
        return cNAccount;
    }

    public CNAccount queryGLAccount(String accountNumber)
    {
        CNAccount cNAccount = new CNAccount();
        try ( ResultSet rs = executeQueryToResultSet("SELECT C.GL_ACCT_ID, C.GL_ACCT_NO, C.ACCT_DESC, C.BU_ID FROM " + PHController.CoreSchemaName + ".GL_ACCOUNT C WHERE C.GL_ACCT_NO='" + accountNumber + "'"))
        {
            if (rs.next())
            {
                cNAccount.setAcctId(rs.getLong("GL_ACCT_ID"));
                cNAccount.setBuId(rs.getLong("BU_ID"));
                cNAccount.setAccountNumber(rs.getString("GL_ACCT_NO"));
                cNAccount.setShortName(removeSpaces(rs.getString("ACCT_DESC")));
                cNAccount.setAccountName(removeSpaces(rs.getString("ACCT_DESC")));
                cNAccount.setCurrency(queryCurrency(PHController.PrimaryCurrencyCode));
            }
        }
        catch (Exception ex)
        {
            logError(ex);
        }
        return cNAccount;
    }

    public CNCustomer queryCustomerById(long custId)
    {
        logInfo(">>>>>>>>>>check for cust id>>>>>>>>>> " + custId);
        CNCustomer cNCustomer = new CNCustomer();
        try ( ResultSet rs = executeQueryToResultSet("select CUST_ID,NVL(REPLACE(SOCIAL_SECURITY_NO,'/'),(SELECT NVL(REGISTRATION_NO,SWIFT_ADDRESS) FROM " + PHController.CoreSchemaName + ".ORGANISATION WHERE CUST_ID = VC.CUST_ID )) AS ID_NO,CUST_NO,MAIN_BRANCH_ID,CUST_CAT,CUST_NM, "
                + " NVL((SELECT U.ACCESS_CD FROM " + PHController.CoreSchemaName + ".CUSTOMER_CHANNEL_USER U WHERE U.CUST_ID = VC.CUST_ID AND U.USER_CAT_CD='PER' AND U.CHANNEL_ID = 8 AND ROWNUM=1),' ') ACCESS_CD, "
                + " NVL((SELECT O.CONTACT FROM " + PHController.CoreSchemaName + ".V_CUSTOMER_CONTACT_MODE O WHERE O.CUST_ID = VC.CUST_ID AND O.CONTACT_MODE_CAT_CD IN ('MOBPHONE','TELPHONE') AND ROWNUM=1),' ') CONTACT "
                + " FROM " + PHController.CoreSchemaName + ".V_CUSTOMER VC where VC.cust_id =" + custId))
        {
            if (rs.next())
            {
                cNCustomer.setCustId(rs.getLong("CUST_ID"));
                cNCustomer.setBuId(rs.getLong("MAIN_BRANCH_ID"));
                cNCustomer.setIdNo(rs.getString("ID_NO"));
                cNCustomer.setCustNo(rs.getString("CUST_NO"));
                cNCustomer.setCustCat(rs.getString("CUST_CAT"));
                cNCustomer.setCustName(rs.getString("CUST_NM"));
                cNCustomer.setContact(rs.getString("CONTACT"));
            }
        }
        catch (Exception ex)
        {
            logError(ex);
        }
        return cNCustomer;
    }

    public CNCustomer queryCustomerByIdNo(String customerNumber)
    {
        CNCustomer cNCustomer = new CNCustomer();
        try ( ResultSet rs = executeQueryToResultSet("SELECT C.CUST_ID, REPLACE(CI.IDENT_NO,'/') AS ID_NO, C.CUST_NO, "
                + "C.MAIN_BRANCH_ID, C.CUST_CAT, C.CUST_NM, "
                + "NVL((SELECT U.ACCESS_CD FROM " + PHController.CoreSchemaName + ".CUSTOMER_CHANNEL_USER U WHERE U.CUST_ID = C.CUST_ID AND U.USER_CAT_CD='PER' AND U.CHANNEL_ID=9 AND ROWNUM=1),"
                + "(SELECT O.CONTACT FROM " + PHController.CoreSchemaName + ".V_CUSTOMER_CONTACT_MODE O WHERE O.CUST_ID = C.CUST_ID AND O.CONTACT_MODE_CAT_CD IN ('MOBPHONE','TELPHONE') AND ROWNUM=1)) AS CONTACT "
                + "FROM " + PHController.CoreSchemaName + ".CUSTOMER C," + PHController.CoreSchemaName + ".CUSTOMER_IDENTIFICATION CI  "
                + "WHERE C.CUST_ID = CI.CUST_ID AND CI.IDENT_NO='" + customerNumber + "'"))
        {
            if (rs.next())
            {
                cNCustomer.setCustId(rs.getLong("CUST_ID"));
                cNCustomer.setBuId(rs.getLong("MAIN_BRANCH_ID"));
                cNCustomer.setIdNo(rs.getString("ID_NO"));
                cNCustomer.setCustNo(rs.getString("CUST_NO"));
                cNCustomer.setCustCat(rs.getString("CUST_CAT"));
                cNCustomer.setCustName(rs.getString("CUST_NM"));
                cNCustomer.setContact(rs.getString("CONTACT"));
            }
        }
        catch (Exception ex)
        {
            logError(ex);
        }
        return cNCustomer;
    }

    public CNCustomer queryCustomerByNumber(String customerNumber)
    {
        CNCustomer cNCustomer = new CNCustomer();
        try ( ResultSet rs = executeQueryToResultSet("SELECT C.CUST_ID, C.CUST_NO, C.MAIN_BRANCH_ID, C.CUST_CAT, C.CUST_NM, NVL((SELECT U.ACCESS_CD FROM " + PHController.CoreSchemaName + ".CUSTOMER_CHANNEL_USER U WHERE U.CUST_ID = C.CUST_ID AND U.USER_CAT_CD='PER' AND U.CHANNEL_ID=9 AND ROWNUM=1),(SELECT O.CONTACT FROM " + PHController.CoreSchemaName + ".V_CUSTOMER_CONTACT_MODE O WHERE O.CUST_ID = C.CUST_ID AND O.CONTACT_MODE_CAT_CD IN ('MOBPHONE','TELPHONE') AND ROWNUM=1)) AS CONTACT FROM " + PHController.CoreSchemaName + ".CUSTOMER C WHERE C.CUST_NO LIKE '%" + customerNumber + "'"))
        {
            if (rs.next())
            {
                cNCustomer.setCustId(rs.getLong("CUST_ID"));
                cNCustomer.setBuId(rs.getLong("MAIN_BRANCH_ID"));
                cNCustomer.setCustNo(rs.getString("CUST_NO"));
                cNCustomer.setCustCat(rs.getString("CUST_CAT"));
                cNCustomer.setCustName(rs.getString("CUST_NM"));
                cNCustomer.setContact(rs.getString("CONTACT"));
            }
        }
        catch (Exception ex)
        {
            logError(ex);
        }
        return cNCustomer;
    }

    public ArrayList<CNCustomer> queryGroupMembers(long groupCustId)
    {
        ArrayList<CNCustomer> customers = new ArrayList<>();
        try ( ResultSet rs = executeQueryToResultSet("SELECT C.CUST_NO FROM " + PHController.CoreSchemaName + ".GROUP_MEMBER G, " + PHController.CoreSchemaName + ".CUSTOMER C WHERE G.GROUP_CUST_ID=" + groupCustId + " AND C.CUST_ID=G.MEMBER_CUST_ID AND G.REC_ST='A'"))
        {
            while (rs.next())
            {
                customers.add(queryCustomerByNumber(rs.getString("CUST_NO")));
            }
        }
        catch (Exception ex)
        {
            logError(ex);
        }
        return customers;
    }

    public ArrayList<CNCustomer> queryGroupLoanBeneficiaries(long groupCustId, String accountNumber)
    {
        ArrayList<CNCustomer> customers = new ArrayList<>();
        try ( ResultSet rs = executeQueryToResultSet("SELECT CUST_NO FROM " + PHController.CoreSchemaName + ".CUSTOMER WHERE CUST_ID IN (SELECT MEMBER_ID FROM " + PHController.CoreSchemaName + ".GROUP_LOAN_ALLOTMENT_MEMO WHERE GRP_CUST_ID=" + groupCustId + " AND APPL_ID=(SELECT APPL_ID FROM " + PHController.CoreSchemaName + ".LOAN_ACCOUNT WHERE ACCT_NO='" + accountNumber + "'))"))
        {
            while (rs.next())
            {
                customers.add(queryCustomerByNumber(rs.getString("CUST_NO")));
            }
        }
        catch (Exception ex)
        {
            logError(ex);
        }
        return customers;
    }

    public String queryCustomerContact(long custId)
    {
        String contact = null;
        try ( ResultSet rs = executeQueryToResultSet("SELECT NVL((SELECT U.ACCESS_CD FROM " + PHController.CoreSchemaName + ".CUSTOMER_CHANNEL_USER U WHERE U.CUST_ID = " + custId + " AND U.USER_CAT_CD='PER' AND ROWNUM=1),(SELECT O.CONTACT FROM " + PHController.CoreSchemaName + ".V_CUSTOMER_CONTACT_MODE O WHERE O.CUST_ID = " + custId + " AND O.CONTACT_MODE_CAT_CD IN ('MOBPHONE','TELPHONE') AND ROWNUM=1)) AS CONTACT FROM DUAL"))
        {
            if (rs.next())
            {
                contact = rs.getString("CONTACT");
            }
        }
        catch (Exception ex)
        {
            logError(ex);
        }
        return contact;
    }

    public ArrayList<CNBranch> queryBusinessUnits()
    {
        ArrayList<CNBranch> branches = new ArrayList<>();
        try ( ResultSet rs = executeQueryToResultSet("SELECT BU_ID, BU_NO, BU_NM, GL_PREFIX_CD FROM " + PHController.CoreSchemaName + ".BUSINESS_UNIT WHERE REC_ST='A' ORDER BY BU_NO"))
        {
            while (rs.next())
            {
                CNBranch cNBranch = new CNBranch();
                cNBranch.setBuId(rs.getLong("BU_ID"));
                cNBranch.setBuCode(rs.getString("BU_NO"));
                cNBranch.setBuName(rs.getString("BU_NM"));
                cNBranch.setGlPrefix(rs.getString("GL_PREFIX_CD"));
                branches.add(cNBranch);
            }
        }
        catch (Exception ex)
        {
            logError(ex);
        }
        return branches;
    }

    public CNBranch queryBusinessUnit(Long buId)
    {
        CNBranch cNBranch = new CNBranch();
        try ( ResultSet rs = executeQueryToResultSet("SELECT BU_ID, BU_NO, BU_NM, GL_PREFIX_CD FROM " + PHController.CoreSchemaName + ".BUSINESS_UNIT WHERE REC_ST='A' AND BU_ID=" + buId))
        {
            if (rs.next())
            {
                cNBranch.setBuId(rs.getLong("BU_ID"));
                cNBranch.setBuCode(rs.getString("BU_NO"));
                cNBranch.setBuName(rs.getString("BU_NM"));
                cNBranch.setGlPrefix(rs.getString("GL_PREFIX_CD"));
            }
        }
        catch (Exception ex)
        {
            logError(ex);
        }
        return cNBranch;
    }

    public CNBranch queryBusinessUnit(String buCode)
    {
        CNBranch cNBranch = new CNBranch();
        try ( ResultSet rs = executeQueryToResultSet("SELECT BU_ID, BU_NO, BU_NM, GL_PREFIX_CD FROM " + PHController.CoreSchemaName + ".BUSINESS_UNIT WHERE REC_ST='A' AND BU_NO='" + buCode + "'"))
        {
            if (rs.next())
            {
                cNBranch.setBuId(rs.getLong("BU_ID"));
                cNBranch.setBuCode(rs.getString("BU_NO"));
                cNBranch.setBuName(rs.getString("BU_NM"));
                cNBranch.setGlPrefix(rs.getString("GL_PREFIX_CD"));
            }
        }
        catch (Exception ex)
        {
            logError(ex);
        }
        return cNBranch;
    }

    public Object[][] queryCustomerTypes()
    {
        return executeQueryToArray("SELECT CUST_TY_ID, CUST_TY_DESC FROM " + PHController.CoreSchemaName + ".CUSTOMER_TYPE_REF WHERE REC_ST='A' ORDER BY CUST_TY_ID");
    }

    public Object[][] queryProducts()
    {
        return executeQueryToArray("SELECT PROD_ID, PROD_DESC FROM " + PHController.CoreSchemaName + ".PRODUCT WHERE REC_ST='A' ORDER BY PROD_ID");
    }

    public Object[][] queryProducts(int productId)
    {
        return executeQueryToArray("SELECT PROD_ID, PROD_DESC FROM " + PHController.CoreSchemaName + ".PRODUCT WHERE REC_ST='A' AND PROD_ID=" + productId + " ORDER BY PROD_CD");
    }

    public boolean upsertSetting(BRSetting bRSetting)
    {
        return recordExists("SELECT CODE FROM " + PHController.CMSchemaName + ".EI_SETTING WHERE CODE='" + bRSetting.getCode() + "' AND MODULE='" + bRSetting.getModule() + "'")
                ? updateSetting(bRSetting)
                : saveSetting(bRSetting);
    }

    private boolean saveSetting(BRSetting bRSetting)
    {
        return executeUpdate("INSERT INTO " + PHController.CMSchemaName + ".EI_SETTING(CODE, VALUE, MODULE, DESCRIPTION, MODIFIED_BY, DATE_MODIFIED) VALUES('" + bRSetting.getCode() + "', '" + (bRSetting.isEncrypted() && !BRCrypt.isEncrypted(bRSetting.getValue()) ? BRCrypt.encrypt(bRSetting.getValue()) : bRSetting.getValue()) + "', '" + bRSetting.getModule() + "', '" + bRSetting.getDescription() + "', '" + bRSetting.getLastModifiedBy() + "', SYSDATE)", true);
    }

    private boolean updateSetting(BRSetting bRSetting)
    {
        return executeUpdate("UPDATE " + PHController.CMSchemaName + ".EI_SETTING SET VALUE='" + (bRSetting.isEncrypted() && !BRCrypt.isEncrypted(bRSetting.getValue()) ? BRCrypt.encrypt(bRSetting.getValue()) : bRSetting.getValue()) + "', DESCRIPTION='" + bRSetting.getDescription() + "', MODIFIED_BY='" + bRSetting.getLastModifiedBy() + "', DATE_MODIFIED=SYSDATE WHERE CODE='" + bRSetting.getCode() + "' AND MODULE='" + bRSetting.getModule() + "'", true);
    }

    public boolean deleteBimetricsImage(String customerNo)
    {
        return executeUpdate("DELETE FROM " + PHController.CoreSchemaName + ".CUSTOMER_IMAGE A WHERE A.CUST_ID = (SELECT CUST_ID FROM " + PHController.CoreSchemaName + ".CUSTOMER WHERE CUST_NO = '" + customerNo + "') AND A.CUST_IMAGE_ID IN (SELECT CUST_IMAGE_ID FROM " + PHController.CoreSchemaName + ".V_CUSTOMER_IMAGES WHERE IMAGE_TY in ('FINRI','FINLI')) AND CUST_ID = A.CUST_ID ", true);
    }

    public HashMap<String, BRSetting> querySettings(String module)
    {
        HashMap<String, BRSetting> bRSettings = new HashMap<>();
        try ( ResultSet rs = executeQueryToResultSet("SELECT CODE, VALUE, MODULE, DESCRIPTION, MODIFIED_BY, DATE_MODIFIED FROM " + PHController.CMSchemaName + ".EI_SETTING WHERE MODULE='" + module + "' ORDER BY CODE ASC"))
        {
            while (rs.next())
            {
                BRSetting bRSetting = new BRSetting();
                bRSetting.setCode(rs.getString("CODE"));
                bRSetting.setEncrypted(BRCrypt.isEncrypted(rs.getString("VALUE")));
                bRSetting.setValue(bRSetting.isEncrypted() ? BRCrypt.decrypt(rs.getString("VALUE")) : rs.getString("VALUE"));
                bRSetting.setModule(rs.getString("MODULE"));
                bRSetting.setDescription(rs.getString("DESCRIPTION"));
                bRSetting.setLastModifiedBy(rs.getString("MODIFIED_BY"));
                bRSetting.setDateModified(rs.getDate("DATE_MODIFIED"));
                bRSettings.put(bRSetting.getCode(), bRSetting);
            }
        }
        catch (Exception ex)
        {
            logError(ex);
        }
        return bRSettings;
    }

    public void selectItemByCode(JComboBox box, String code)
    {
        for (int i = 0; i < box.getItemCount(); i++)
        {
            if (box.getItemAt(i).toString().startsWith(code + "~") || box.getItemAt(i).toString().startsWith(code + " ~"))
            {
                box.setSelectedIndex(i);
                return;
            }
        }
    }

    public boolean transactionExists(String msgRef)
    {
        return recordExists("SELECT REF_NUMBER FROM " + PHController.CMSchemaName + ".EI_POS_TXN_LOG WHERE REF_NUMBER='" + msgRef + "' AND CHANNEL_ID = " + PHController.posChannelID + " AND TRAN_STATUS = 'APPROVED'");
    }

    public boolean reversalTransactionExists(String msgRef)
    {
        return recordExists("SELECT REF_NUMBER FROM " + PHController.CMSchemaName + ".EI_POS_TXN_LOG WHERE REF_NUMBER='" + msgRef + "' AND CHANNEL_ID = " + PHController.posChannelID + " AND TRAN_STATUS = 'REVERSED'");
    }

    public boolean transactionExists(String msgRef, CNAccount cNAccount)
    {
        return recordExists("SELECT REF_NUMBER FROM " + PHController.CMSchemaName + ".EI_POS_TXN_LOG WHERE REF_NUMBER='" + msgRef + "' AND ACCOUNT_NUMBER = '" + cNAccount.getAccountNumber() + "' AND CHANNEL_ID = " + PHController.posChannelID + " AND TRAN_STATUS = 'APPROVED'");
    }

    public String replaceAll(String message, String holder, String replacement)
    {
        if (message != null)
        {
            replacement = replacement == null ? "<>" : replacement;
            while (message.contains(holder) && !replacement.equals(holder))
            {
                message = message.replace(holder, replacement);
            }
        }
        return message;
    }

    public boolean isNumberValid(String num, boolean isInteger)
    {
        try
        {
            return isInteger ? (Integer.parseInt(num) >= 0) : (Double.parseDouble(num) >= 0);
        }
        catch (Exception ex)
        {
            return false;
        }
    }

    public void logError(Exception ex)
    {
        if (getXapiCaller() != null)
        {
            getXapiCaller().logException(ex);
        }
        else
        {
            PHController.logPosError(ex);
        }
    }

    public void logInfo(String info)
    {
        PHController.logPosInfo(info);
    }

    /**
     * @return the logStatement
     */
    public CallableStatement getLogStatement()
    {
        return logStatement;
    }

    /**
     * @param aLogStatement the logStatement to set
     */
    public void setLogStatement(CallableStatement aLogStatement)
    {
        logStatement = aLogStatement;
    }

    /**
     * @return the fetchSMS
     */
    public CallableStatement getFetchSMS()
    {
        return fetchSMS;
    }

    /**
     * @param fetchSMS the fetchSMS to set
     */
    public void setFetchSMS(CallableStatement fetchSMS)
    {
        this.fetchSMS = fetchSMS;
    }
}
