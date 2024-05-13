/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Mobile;

import APX.BRCrypt;
import APX.PHController;
import FILELoad.CASARequest;
import PHilae.CNCustomer;
import Ruby.model.SOItem;
import Ruby.model.WFWorkItemDetail;
import SMS.SMSOutQueus;
import SMS.SMSTemplate;
import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.StringTokenizer;
import javax.swing.JComboBox;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 *
 * @author Pecherk
 */
public class TDClientMOB
{

    private XAPICallerMOB xapiCaller = null;
    private Connection dbConnection = null;
    private CallableStatement logStatement = null;
    private CallableStatement fetchEStatement = null;
    private CallableStatement fetchSMS = null;
    private ArrayList<CNCurrencyMOB> cNCurrencies = new ArrayList<>();

    public TDClientMOB()
    {
        this(null);
    }

    public TDClientMOB(XAPICallerMOB xapiCaller)
    {
        this.xapiCaller = xapiCaller;
    }

    public void connectToDB()
    {
        try
        {
            setDbConnection(DriverManager.getConnection(PHController.CMSchemaURL, PHController.CMSchemaName, PHController.CMSchemaPassword));
            setLogStatement(getDbConnection().prepareCall("{call PSP_EX_LOG_MOB_TXN(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)}"));

            setFetchEStatement(getDbConnection().prepareCall("{call csp_stmt_by_email(?,?,?,?,?)}"));
            setcNCurrencies();
            setFetchSMS(getDbConnection().prepareCall("{call SMS_ALERT_PROCESS()}"));
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
            if ("Y".equalsIgnoreCase(PHController.EnableMobDebug))
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
            if ("Y".equalsIgnoreCase(PHController.EnableMobDebug))
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

    public void loadSMS()
    {
        try
        {
            if (getDbConnection() == null)
            {
                connectToDB();
                getFetchSMS().execute();
            }
            else
            {
                getFetchSMS().execute();
            }

        }

        catch (Exception ex)
        {
            System.err.println("ERROR FOund ssm fetch");
            logError(ex);
        }
    }

    public ArrayList<SOItem> queryStandingOrders(CNAccountMOB cNAccount)
    {
        ArrayList<SOItem> sOItems = new ArrayList<>();
        try ( ResultSet rs = executeQueryToResultSet("SELECT S.STANDING_ORDER_ID, L.REF_NUMBER, L.ACCOUNT_NUMBER, L.CONTRA_ACCOUNT, L.DESCRIPTION, L.CURRENCY_CODE, S.REPAYMENT_FREQ_UNIT, L.TXN_AMOUNT, S.REC_ST FROM CORE_SCHEMA.STANDING_ORDER S, CM_SCHEMA.EI_MOB_TXN_LOG L WHERE L.REF_NUMBER=S.PRIMARY_REF AND L.ACCOUNT_NUMBER='" + cNAccount.getAccountNumber() + "' AND S.REC_ST='A' AND L.CHANNEL_ID='" + PHController.mobChannelID + "'"))
        {
            if (rs != null)
            {
                while (rs.next())
                {
                    SOItem sOItem = new SOItem();
                    sOItem.setOrderId(rs.getLong("STANDING_ORDER_ID"));
                    sOItem.setReference(rs.getString("REF_NUMBER"));
                    sOItem.setNarration(rs.getString("DESCRIPTION"));
                    sOItem.setCurrency(rs.getString("CURRENCY_CODE"));
                    sOItem.setPeriod(rs.getString("REPAYMENT_FREQ_UNIT"));
                    sOItem.setAmount(rs.getBigDecimal("TXN_AMOUNT"));
                    sOItem.setSourceAccount(queryAnyAccount(rs.getString("ACCOUNT_NUMBER")));
                    sOItem.setBeneficiaryAccount(queryAnyAccount(rs.getString("CONTRA_ACCOUNT")));
                    sOItem.setStatus(rs.getString("REC_ST"));
                    sOItems.add(sOItem);
                }
            }
        }
        catch (Exception ex)
        {
            logError(ex);
        }
        return sOItems;
    }

    public String loginAdminUser(String loginId, String password)
    {
        String name = null;
        try ( ResultSet rs = executeQueryToResultSet("SELECT FIRST_NM || ' ' || LAST_NM AS NAME FROM " + PHController.CoreSchemaName + ".V_USER_ROLE A, " + PHController.CoreSchemaName + ".SYSPWD_HIST B WHERE A.LOGIN_ID='" + loginId + "' "
                + "AND B.SYSUSER_ID=A.USER_ID AND A.BUSINESS_ROLE_ID IN (" + PHController.mobAllowedLoginRoles + ") AND A.REC_ST=B.REC_ST AND B.PASSWD='" + BRCrypt.encrypt(password) + "' AND B.REC_ST='A'"))
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

    public CNAccountMOB queryAnyAccount(String accountNumber)
    {
        CNAccountMOB cNAccount = new CNAccountMOB();
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

    public ArrayList<CNAccountMOB> queryAllAccounts(long custId)
    {
        ArrayList<CNAccountMOB> accounts = new ArrayList<>();
        try ( ResultSet rs = executeQueryToResultSet("SELECT C.ACCT_ID, C.CUST_ID, C.MAIN_BRANCH_ID, C.PROD_ID, C.ACCT_NO, C.ACCT_NM, C.ACCT_NO AS SHORT_NAME, E.CRNCY_CD, C.PROD_CAT_TY FROM " + PHController.CoreSchemaName + ".ACCOUNT C, " + PHController.CoreSchemaName + ".CURRENCY E WHERE C.CUST_ID=" + custId + " AND C.REC_ST='A' AND C.CRNCY_ID = E.CRNCY_ID"))
        {
            while (rs.next())
            {
                CNAccountMOB cNAccount = new CNAccountMOB();
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

    public ArrayList<CNAccountMOB> queryDepositAccounts(long custId)
    {
        ArrayList<CNAccountMOB> accounts = new ArrayList<>();
        try ( ResultSet rs = executeQueryToResultSet("SELECT C.ACCT_ID, C.CUST_ID, C.MAIN_BRANCH_ID, C.PROD_ID, C.ACCT_NO, C.ACCT_NM, C.ACCT_NO AS SHORT_NAME, E.CRNCY_CD, C.PROD_CAT_TY FROM " + PHController.CoreSchemaName + ".ACCOUNT C, " + PHController.CoreSchemaName + ".CURRENCY E WHERE C.CUST_ID=" + custId + " AND C.REC_ST='A' AND C.PROD_CAT_TY='DP' AND C.CRNCY_ID = E.CRNCY_ID"))
        {
            while (rs.next())
            {
                CNAccountMOB cNAccount = new CNAccountMOB();
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

    public ArrayList<CNAccountMOB> queryLoanAccounts(long custId)
    {
        ArrayList<CNAccountMOB> accounts = new ArrayList<>();
        try ( ResultSet rs = executeQueryToResultSet("SELECT C.ACCT_ID, C.CUST_ID, C.MAIN_BRANCH_ID, C.PROD_ID, C.ACCT_NO, C.ACCT_NM, C.ACCT_NO AS SHORT_NAME, E.CRNCY_CD, C.PROD_CAT_TY FROM " + PHController.CoreSchemaName + ".ACCOUNT C, " + PHController.CoreSchemaName + ".CURRENCY E WHERE C.CUST_ID=" + custId + " AND C.REC_ST='A' AND C.PROD_CAT_TY='LN' AND C.CRNCY_ID = E.CRNCY_ID"))
        {
            while (rs.next())
            {
                CNAccountMOB cNAccount = new CNAccountMOB();
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
                CNAccountMOB cNAccount = new CNAccountMOB();
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

//    public ArrayList<CNAccountMOB> queryUnenrolledAccounts()
//    {
//        ArrayList<CNAccountMOB> accounts = new ArrayList<>();
//        try (ResultSet rs = executeQueryToResultSet("SELECT C.ACCT_ID, C.CUST_ID, C.MAIN_BRANCH_ID, C.PROD_ID, C.ACCT_NO, C.ACCT_NM, C.ACCT_NO AS SHORT_NAME, C.CRNCY_CD_ISO, C.PROD_CAT_TY FROM " + PHController.CoreSchemaName + ".V_ACCOUNTS C WHERE C.REC_ST='A' AND C.PROD_CAT_TY='DP' AND C.PROD_ID IN (" + PHController.mobAllowedProductIDs + ") AND C.ACCT_ID NOT IN (SELECT ACCT_ID FROM " + PHController.CoreSchemaName + ".CUST_CHANNEL_ACCOUNT WHERE CHANNEL_ID=" + PHController.mobChannelID + ") AND ROWNUM<=20000   ORDER BY C.ACCT_ID DESC"))
//        {
//            while (rs.next())
//            {
//                CNAccountMOB cNAccount = new CNAccountMOB();
//                cNAccount.setAcctId(rs.getLong("ACCT_ID"));
//                cNAccount.setCustId(rs.getLong("CUST_ID"));
//                cNAccount.setBuId(rs.getLong("MAIN_BRANCH_ID"));
//                cNAccount.setProductId(rs.getLong("PROD_ID"));
//                cNAccount.setAccountNumber(rs.getString("ACCT_NO"));
//                cNAccount.setAccountType(rs.getString("PROD_CAT_TY"));
//                cNAccount.setShortName(rs.getString("SHORT_NAME"));
//                cNAccount.setAccountName(removeSpaces(rs.getString("ACCT_NM")));
//                cNAccount.setCurrency(queryCurrency(rs.getString("CRNCY_CD_ISO")));
//                accounts.add(cNAccount);
//            }
//        }
//        catch (Exception ex)
//        {
//            logError(ex);
//        }
//        return accounts;
//    }
    public ArrayList<CNAccountMOB> queryUnenrolledAccounts()
    {
        ArrayList<CNAccountMOB> accounts = new ArrayList<>();
//        try (ResultSet rs = executeQueryToResultSet("SELECT C.ACCT_ID, C.CUST_ID, C.MAIN_BRANCH_ID, C.PROD_ID, C.ACCT_NO, C.ACCT_NM, C.ACCT_NO AS SHORT_NAME, C.CRNCY_CD_ISO, C.PROD_CAT_TY FROM " + MOBController.CoreSchemaName + ".V_ACCOUNTS C WHERE C.REC_ST='A' AND C.PROD_CAT_TY='DP' AND C.PROD_ID IN (" + MOBController.AllowedProductIDs + ") AND C.ACCT_ID NOT IN (SELECT ACCT_ID FROM " + MOBController.CoreSchemaName + ".CUST_CHANNEL_ACCOUNT WHERE CHANNEL_ID=" + MOBController.ChannelID + ") AND ROWNUM<=20000   ORDER BY C.ACCT_ID DESC"))
//        {
        try ( ResultSet rs = executeQueryToResultSet("SELECT   C.ACCT_ID,C.CUST_ID,  C.MAIN_BRANCH_ID,  C.PROD_ID, C.ACCT_NO, C.ACCT_NM, C.ACCT_NO AS SHORT_NAME,C.CRNCY_CD_ISO,C.PROD_CAT_TY "
                + "FROM    " + PHController.CoreSchemaName + ".V_ACCOUNTS C, " + PHController.CoreSchemaName + ".EXT_MOB_MIGRATION ex, " + PHController.CoreSchemaName + ".account ac "
                + "WHERE C.REC_ST = 'A' "
                + "and AC.ACCT_NO = C.ACCT_NO "
                + "and ex.OLD_ACCT_NO = AC.OLD_ACCT_NO "
                + "AND C.PROD_CAT_TY = 'DP' and ex.id_no = (select social_security_no from " + PHController.CoreSchemaName + ".person where cust_id = c.cust_id )"
                + "AND C.ACCT_ID NOT IN (SELECT   ACCT_ID "
                + " FROM    " + PHController.CoreSchemaName + ".CUST_CHANNEL_ACCOUNT "
                + "WHERE   ROWNUM <= 20000 AND CHANNEL_ID = 9) "
                + "ORDER BY   C.ACCT_ID DESC"))
        {
            while (rs.next())
            {
                CNAccountMOB cNAccount = new CNAccountMOB();
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

    public ArrayList<CNAccountMOB> queryUnenrolledByAccount(String acctNo)
    {
        ArrayList<CNAccountMOB> accounts = new ArrayList<>();
        try ( ResultSet rs = executeQueryToResultSet("SELECT C.ACCT_ID, C.CUST_ID, C.MAIN_BRANCH_ID, C.PROD_ID, C.ACCT_NO, "
                + "C.ACCT_NM, C.ACCT_NO AS SHORT_NAME, C.CRNCY_CD_ISO, C.PROD_CAT_TY FROM " + PHController.CoreSchemaName + ".V_ACCOUNTS C "
                + "WHERE C.REC_ST='A' AND C.PROD_CAT_TY='DP' AND C.PROD_ID IN (" + PHController.mobAllowedProductIDs + ") AND ACCT_NO ='" + acctNo + "' AND C.ACCT_ID NOT IN (SELECT ACCT_ID FROM " + PHController.CoreSchemaName + ".CUST_CHANNEL_ACCOUNT WHERE CHANNEL_ID=" + PHController.mobChannelID + ")"))
        {
            while (rs.next())
            {
                CNAccountMOB cNAccount = new CNAccountMOB();
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

    public ArrayList<CNCurrencyMOB> queryCurrencies()
    {
        ArrayList<CNCurrencyMOB> currencies = new ArrayList<>();
        try ( ResultSet rs = executeQueryToResultSet("SELECT CRNCY_ID, CRNCY_CD, CRNCY_NM, LEAST_CRNCY_DENOMINATOR FROM " + PHController.CoreSchemaName + ".CURRENCY WHERE REC_ST='A' ORDER BY CRNCY_ID"))
        {
            while (rs.next())
            {
                CNCurrencyMOB cNCurrency = new CNCurrencyMOB();
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

    public CNCurrencyMOB queryCurrency(String codeOrId)
    {
        if (getcNCurrencies().isEmpty())
        {
            setcNCurrencies();
        }
        if (codeOrId != null)
        {
            for (CNCurrencyMOB cNCurrency : getcNCurrencies())
            {
                if (codeOrId.equalsIgnoreCase(cNCurrency.getCurrencyCode()))
                {
                    return cNCurrency;
                }
            }
            for (CNCurrencyMOB cNCurrency : getcNCurrencies())
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
        return new CNCurrencyMOB();
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
                        Long dBuId = getChannelBuId(PHController.mobChannelID);
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
        try ( ResultSet rs = executeQueryToResultSet("SELECT ORIGIN_BU_ID FROM " + PHController.CoreSchemaName + ".SERVICE_CHANNEL WHERE CHANNEL_ID=" + PHController.mobChannelID))
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

    public Object[][] verifyGLNumber(String ledgerNo)
    {
        Object[][] glAcctList = (executeQueryToArray("SELECT DISTINCT ACCT_DESC FROM " + PHController.CoreSchemaName + ".GL_ACCOUNT WHERE LEDGER_NO ='" + ledgerNo + "' AND REC_ST = 'A'"));

        return glAcctList;
    }

    public boolean upsertCharge(EIChargeMOB eICharge)
    {
        if (checkIfExists("SELECT CHARGE_CODE FROM " + PHController.CMSchemaName + ".EI_CHARGE "
                + "WHERE CHARGE_CODE='" + eICharge.getChargeCode() + "'"))
        {
            return updateCharge(eICharge);
        }
        else
        {
            return saveCharge(eICharge);
        }
    }

    private boolean saveCharge(EIChargeMOB eICharge)
    {
        if (executeUpdate("INSERT INTO " + PHController.CMSchemaName + ".EI_CHARGE(CHARGE_CODE, CREATE_DATE, CHANNEL_ID, CHARGE_DESC, CHARGE_ACCOUNT, CHARGE_LEDGER, TAX_LEDGER, TAX_PERC, TAX_NAME, MODULE, MODIFIED_BY, DATE_MODIFIED, REC_ST,BILLER_LEDGER,SHARE_PERC ) "
                + "VALUES('" + eICharge.getChargeCode() + "', SYSDATE, " + PHController.mobChannelID + ", '" + eICharge.getDescription() + "', '" + eICharge.getChargeAccount() + "', '" + eICharge.getChargeLedger() + "', '" + eICharge.getTaxLedger() + "', " + eICharge.getTaxPercentage().toPlainString() + ", '" + eICharge.getTaxName() + "', '" + eICharge.getModule() + "', '" + eICharge.getLastModifiedBy() + "', " + formatDate(eICharge.getDateModified()) + ", '" + eICharge.getStatus() + "','" + eICharge.getTpLedger() + "'," + eICharge.getTpSharePercentage() + ")", true))
        {
            return saveChargeValues(eICharge) && saveChargeWaivers(eICharge);
        }
        return false;
    }

    public HashMap<String, EIChargeMOB> loadCharges(String module)
    {
        HashMap<String, EIChargeMOB> eICharges = new HashMap<>();
        try ( ResultSet rs = executeQueryToResultSet("SELECT CHARGE_CODE, CHARGE_DESC, CHARGE_ACCOUNT, CHARGE_LEDGER, TAX_LEDGER, "
                + "TAX_PERC, TAX_NAME, MODULE, MODIFIED_BY, DATE_MODIFIED, REC_ST,BILLER_LEDGER,SHARE_PERC "
                + "FROM " + PHController.CMSchemaName + ".EI_CHARGE WHERE CHANNEL_ID=" + PHController.mobChannelID + " AND MODULE='" + module + "' ORDER BY CHARGE_CODE ASC"))
        {
            while (rs.next())
            {
                EIChargeMOB eICharge = new EIChargeMOB();
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
                eICharge.setTpLedger(rs.getString("BILLER_LEDGER"));
                eICharge.setTpSharePercentage(rs.getBigDecimal("SHARE_PERC"));
                eICharges.put(eICharge.getChargeCode(), setChargeValues(setChargeWaivers(eICharge)));
            }
        }
        catch (Exception ex)
        {
            logError(ex);
        }
        return eICharges;
    }

    public EIChargeMOB setChargeValues(EIChargeMOB eICharge)
    {
        try ( ResultSet rs = executeQueryToResultSet("SELECT CRNCY_CODE, CHARGE_TYPE, MIN_AMT, MAX_AMT, VALUE FROM " + PHController.CMSchemaName + ".EI_CHARGE_VALUE WHERE CHARGE_CODE='" + eICharge.getChargeCode() + "' ORDER BY CRNCY_CODE ASC"))
        {
            eICharge.getValues().clear();
            while (rs.next())
            {
                TCValueMOB tCValue = new TCValueMOB();
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

    private TCValueMOB setChargeTiers(EIChargeMOB eICharge, TCValueMOB tCValue)
    {
        try ( ResultSet rs = executeQueryToResultSet("SELECT CHARGE_CODE, CRNCY_CODE, TIER_CEILING, CHARGE_AMT FROM " + PHController.CMSchemaName + ".EI_CHARGE_TIER WHERE CHARGE_CODE='" + eICharge.getChargeCode() + "' AND CRNCY_CODE='" + tCValue.getCurrency() + "' ORDER BY TIER_CEILING ASC"))
        {
            tCValue.getTiers().clear();
            while (rs.next())
            {
                TCTierMOB tCTier = new TCTierMOB();
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

    public EIChargeMOB setChargeWaivers(EIChargeMOB eICharge)
    {
        try ( ResultSet rs = executeQueryToResultSet("SELECT PROD_ID, MATCH_ACCT, WAIVED_PERC, CONDITION, THRESHOLD FROM " + PHController.CMSchemaName + ".EI_CHARGE_WAIVER WHERE CHARGE_CODE='" + eICharge.getChargeCode() + "' ORDER BY PROD_ID ASC"))
        {
            eICharge.getWaivers().clear();
            while (rs.next())
            {
                TCWaiverMOB tXWaiver = new TCWaiverMOB();
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

    private boolean saveChargeValues(EIChargeMOB eICharge)
    {
        boolean RC = deleteChargeValues(eICharge);
        for (TCValueMOB tCValue : eICharge.getValues().values())
        {
            if (executeUpdate("INSERT INTO " + PHController.CMSchemaName + ".EI_CHARGE_VALUE(CHARGE_CODE, CRNCY_CODE, CHARGE_TYPE, MIN_AMT, MAX_AMT, VALUE) VALUES('" + eICharge.getChargeCode() + "', '" + tCValue.getCurrency() + "', '" + tCValue.getChargeType() + "', " + tCValue.getMinAmount().toPlainString() + ", " + tCValue.getMaxAmount().toPlainString() + ", " + tCValue.getChargeValue().toPlainString() + ")", true))
            {
                RC = saveChargeTiers(eICharge, tCValue);
            }
        }
        return RC;
    }

    private boolean deleteChargeValues(EIChargeMOB eICharge)
    {
        return executeUpdate("DELETE " + PHController.CMSchemaName + ".EI_CHARGE_VALUE WHERE CHARGE_CODE='" + eICharge.getChargeCode() + "'", true);
    }

    private boolean saveChargeTiers(EIChargeMOB eICharge, TCValueMOB tCValue)
    {
        boolean RC = deleteChargeTiers(eICharge, tCValue);
        for (TCTierMOB tXTier : tCValue.getTiers().values())
        {
            RC = executeUpdate("INSERT INTO " + PHController.CMSchemaName + ".EI_CHARGE_TIER(CHARGE_CODE, CRNCY_CODE, TIER_CEILING, CHARGE_AMT) VALUES('" + eICharge.getChargeCode() + "', '" + tCValue.getCurrency() + "', " + tXTier.getTierCeiling() + ", " + tXTier.getChargeAmount() + ")", true);
        }
        return RC;
    }

    public boolean logCasaCreation(CASARequest aSARequest, TXRequestMOB tXRequestMOB, String acctNo, String acctStatus, String RecSt)
    {
        return executeUpdate("INSERT INTO " + PHController.CMSchemaName + ".EI_CASA_OPENING_LOG(CUST_NO,CUST_NM,CUST_CONTACT,ACCT_NO,ID_NO,REGISTERED,BU_ID,PROD_CD,PROCESS_DATE,ACCT_ST,REC_ST) "
                + "VALUES('" + tXRequestMOB.getCustomerNo() + "', '" + aSARequest.getAccountName() + "', " + aSARequest.getAcessCode() + ", " + acctNo + ",'Y'," + tXRequestMOB.getBuId() + ",'" + aSARequest.getProductCode() + "',SYSDATE,'" + acctStatus + "','" + RecSt + "')", true);

    }

    private boolean deleteChargeTiers(EIChargeMOB eICharge, TCValueMOB tCValue)
    {
        return executeUpdate("DELETE " + PHController.CMSchemaName + ".EI_CHARGE_TIER WHERE CHARGE_CODE='" + eICharge.getChargeCode() + "' AND CRNCY_CODE='" + tCValue.getCurrency() + "'", true);
    }

    private boolean saveChargeWaivers(EIChargeMOB eICharge)
    {
        boolean RC = deleteChargeWaivers(eICharge);
        for (TCWaiverMOB tXWaiver : eICharge.getWaivers().values())
        {
            RC = executeUpdate("INSERT INTO " + PHController.CMSchemaName + ".EI_CHARGE_WAIVER(CHARGE_CODE, PROD_ID, MATCH_ACCT, WAIVED_PERC, CONDITION, THRESHOLD) VALUES('" + eICharge.getChargeCode() + "', " + tXWaiver.getProductId() + ", '" + tXWaiver.getMatchAccount() + "', " + tXWaiver.getWaivedPercentage().toPlainString() + ", '" + tXWaiver.getWaiverCondition() + "', " + tXWaiver.getThresholdValue().toPlainString() + ")", true);
        }
        return RC;
    }

    private boolean deleteChargeWaivers(EIChargeMOB eICharge)
    {
        return executeUpdate("DELETE " + PHController.CMSchemaName + ".EI_CHARGE_WAIVER WHERE CHARGE_CODE='" + eICharge.getChargeCode() + "'", true);
    }

    private boolean updateCharge(EIChargeMOB eICharge)
    {
        if (executeUpdate("UPDATE " + PHController.CMSchemaName + ".EI_CHARGE SET CHANNEL_ID=" + PHController.mobChannelID + ", CHARGE_DESC='" + eICharge.getDescription() + "', CHARGE_ACCOUNT='" + eICharge.getChargeAccount() + "', CHARGE_LEDGER='" + eICharge.getChargeLedger() + "', TAX_LEDGER='" + eICharge.getTaxLedger() + "', TAX_PERC=" + eICharge.getTaxPercentage().toPlainString() + ", TAX_NAME='" + eICharge.getTaxName() + "', MODULE='" + eICharge.getModule() + "', MODIFIED_BY='" + eICharge.getLastModifiedBy() + "', DATE_MODIFIED=" + formatDate(eICharge.getDateModified()) + ", REC_ST='" + eICharge.getStatus() + "',BILLER_LEDGER = '" + eICharge.getTpLedger() + "',SHARE_PERC =" + eICharge.getTpSharePercentage() + " WHERE CHARGE_CODE='" + eICharge.getChargeCode() + "'", true))
        {
            return saveChargeValues(eICharge) && saveChargeWaivers(eICharge);
        }
        return false;
    }

    public boolean deleteInvalidChannelUsers()
    {

//        logInfo("DELETE FROM   " + PHController.CoreSchemaName + ".CUSTOMER_CHANNEL_USR_HIST WHERE   CUST_CHAN_USR_ID IN (SELECT   CUST_CHANNEL_USER_ID FROM   " + PHController.CoreSchemaName + ".CUSTOMER_CHANNEL_USER WHERE   CHANNEL_ID = 9 AND CUST_ID NOT IN (SELECT   CUST_ID FROM " + PHController.CoreSchemaName + ".CUSTOMER_CHANNEL WHERE   CHANNEL_SCHEME_ID = 21))");
//        logInfo("DELETE FROM   " + PHController.CoreSchemaName + ".CUSTOMER_CHANNEL_USER WHERE   CHANNEL_ID = 9   AND CUST_ID NOT IN (SELECT   CUST_ID FROM   " + PHController.CoreSchemaName + ".CUSTOMER_CHANNEL WHERE   CHANNEL_SCHEME_ID = 21)");
//        logInfo("DELETE FROM   " + PHController.CoreSchemaName + ".CUSTOMER_CHANNEL_USER WHERE   CHANNEL_ID = 9   AND CUST_ID NOT IN (SELECT   CUST_ID FROM   " + PHController.CoreSchemaName + ".CUSTOMER_CHANNEL WHERE   CHANNEL_SCHEME_ID = 21)");
//        logInfo("DELETE FROM   " + PHController.CoreSchemaName + ".CUST_CHANNEL_ACCOUNT WHERE   CHANNEL_ID = 9   AND CUST_ID NOT IN (SELECT   CUST_ID   FROM   " + PHController.CoreSchemaName + ".CUSTOMER_CHANNEL_USER  WHERE   CHANNEL_ID = 9)");
//        logInfo("DELETE FROM   " + PHController.CoreSchemaName + ".CUSTOMER_CHANNEL CC  WHERE   CHANNEL_SCHEME_ID = 21     AND NOT EXISTS  (SELECT   CUST_ID  FROM   " + PHController.CoreSchemaName + ".CUST_CHANNEL_ACCOUNT cca  WHERE   cc.cust_id = cca.cust_id  AND CHANNEL_ID = 9)");
//        logInfo("DELETE FROM   " + PHController.CoreSchemaName + ".CUSTOMER_CHANNEL CC WHERE   CHANNEL_SCHEME_ID = 21   AND NOT EXISTS  (SELECT   CUST_ID  FROM   " + PHController.CoreSchemaName + ".CUSTOMER_CHANNEL_USER cca  WHERE   cc.cust_id = cca.cust_id  AND CHANNEL_ID = 9)");
//        logInfo("DELETE FROM   " + PHController.CoreSchemaName + ".CUST_CHANNEL_SCHEME  WHERE   CHANNEL_ID = 9  AND CUST_ID NOT IN (SELECT   CUST_ID FROM   " + PHController.CoreSchemaName + ".CUSTOMER_CHANNEl WHERE   CHANNEL_SCHEME_ID = 21)");
        return executeUpdate("DELETE FROM   " + PHController.CoreSchemaName + ".CUSTOMER_CHANNEL_USR_HIST WHERE   CUST_CHAN_USR_ID IN (SELECT   CUST_CHANNEL_USER_ID FROM   " + PHController.CoreSchemaName + ".CUSTOMER_CHANNEL_USER WHERE   CHANNEL_ID = 9 AND CUST_ID NOT IN (SELECT   CUST_ID FROM " + PHController.CoreSchemaName + ".CUSTOMER_CHANNEL WHERE   CHANNEL_SCHEME_ID = 21))", true)
                && executeUpdate("DELETE FROM   " + PHController.CoreSchemaName + ".CUSTOMER_CHANNEL_USER WHERE   CHANNEL_ID = 9   AND CUST_ID NOT IN (SELECT   CUST_ID FROM   " + PHController.CoreSchemaName + ".CUSTOMER_CHANNEL WHERE   CHANNEL_SCHEME_ID = 21)", true)
                && executeUpdate("DELETE FROM   " + PHController.CoreSchemaName + ".CUSTOMER_CHANNEL_USER WHERE   CHANNEL_ID = 9   AND CUST_ID NOT IN (SELECT   CUST_ID FROM   " + PHController.CoreSchemaName + ".CUSTOMER_CHANNEL WHERE   CHANNEL_SCHEME_ID = 21)", true)
                && executeUpdate("DELETE FROM   " + PHController.CoreSchemaName + ".CUST_CHANNEL_ACCOUNT WHERE   CHANNEL_ID = 9   AND CUST_ID NOT IN (SELECT   CUST_ID   FROM   " + PHController.CoreSchemaName + ".CUSTOMER_CHANNEL_USER  WHERE   CHANNEL_ID = 9)", true)
                && executeUpdate("DELETE FROM   " + PHController.CoreSchemaName + ".CUSTOMER_CHANNEL CC  WHERE   CHANNEL_SCHEME_ID = 21     AND NOT EXISTS  (SELECT   CUST_ID  FROM   " + PHController.CoreSchemaName + ".CUST_CHANNEL_ACCOUNT cca  WHERE   cc.cust_id = cca.cust_id  AND CHANNEL_ID = 9)", true)
                && executeUpdate("DELETE FROM   " + PHController.CoreSchemaName + ".CUSTOMER_CHANNEL CC WHERE   CHANNEL_SCHEME_ID = 21   AND NOT EXISTS  (SELECT   CUST_ID  FROM   " + PHController.CoreSchemaName + ".CUSTOMER_CHANNEL_USER cca  WHERE   cc.cust_id = cca.cust_id  AND CHANNEL_ID = 9)", true)
                && executeUpdate("DELETE FROM   " + PHController.CoreSchemaName + ".CUST_CHANNEL_SCHEME  WHERE   CHANNEL_ID = 9  AND CUST_ID NOT IN (SELECT   CUST_ID FROM   " + PHController.CoreSchemaName + ".CUSTOMER_CHANNEl WHERE   CHANNEL_SCHEME_ID = 21)", true);

        // return executeUpdate("DELETE " + PHController.CoreSchemaName + ".CUST_CHANNEL_ACCOUNT WHERE CREATED_BY='SYSTEM' AND CHANNEL_ID  =" + PHController.mobChannelID + " AND CUST_ID NOT IN(SELECT CUST_ID FROM " + PHController.CoreSchemaName + ".CUSTOMER_CHANNEL_USER)", true) && executeUpdate("DELETE " + PHController.CoreSchemaName + ".CUSTOMER_CHANNEL WHERE USER_ID='SYSTEM' AND CUST_ID NOT IN (SELECT CUST_ID FROM " + PHController.CoreSchemaName + ".CUST_CHANNEL_ACCOUNT)", true) && executeUpdate("DELETE " + PHController.CoreSchemaName + ".CUSTOMER_CHANNEL WHERE USER_ID='SYSTEM' AND CUST_ID NOT IN (SELECT CUST_ID FROM " + PHController.CoreSchemaName + ".CUSTOMER_CHANNEL_USER)", true) && executeUpdate("DELETE " + PHController.CoreSchemaName + ".CUST_CHANNEL_SCHEME WHERE USER_ID='SYSTEM' AND CUST_ID NOT IN (SELECT CUST_ID FROM " + PHController.CoreSchemaName + ".CUSTOMER_CHANNEL_USER)", true);
    }

    public boolean queryIfChannelUserExists(Long acctId, Long custId)
    {
        return checkIfExists("SELECT * FROM " + PHController.CoreSchemaName + ".CUSTOMER_CHANNEL_USER CC, " + PHController.CoreSchemaName + ".CUST_CHANNEL_ACCOUNT CA WHERE CC.CUST_ID = CA.CUST_ID AND CC.CHANNEL_ID =CA.CHANNEL_ID AND CA.CUST_CHANNEL_ID = CC.CUST_CHANNEL_ID AND CC.CHANNEL_ID =" + PHController.mobChannelID + " AND CA.ACCT_ID =" + acctId + " AND CC.CUST_ID = " + custId + "");
    }

    public CNUserMOB queryCNUser(String accessCode)
    {
        CNUserMOB cNUser = new CNUserMOB();
        try ( ResultSet rs = executeQueryToResultSet("SELECT CUST_CHANNEL_USER_ID, CUST_ID, CUST_CHANNEL_ID, ACCESS_CD, ACCESS_NM, PASSWORD, PWD_RESET_FG, CHANNEL_SCHEME_ID, LOCKED_FG, NVL(RANDOM_SEED, 0) AS PIN_TRIES, NVL(RANDOM_NO_SEED, 0) AS PUK_TRIES, SECURITY_CD, QUIZ_CD FROM " + PHController.CoreSchemaName + ".CUSTOMER_CHANNEL_USER WHERE CHANNEL_SCHEME_ID=" + PHController.mobChannelSchemeID + " AND USER_CAT_CD='PER' AND ACCESS_CD='" + accessCode + "' AND REC_ST='A'"))
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
                pushUserExpiry(cNUser.getAccessCode());
            }
        }
        catch (Exception ex)
        {
            logError(ex);
        }
        return cNUser;
    }

    public CNCustomer queryCustomerByIdNo(String idNo)
    {
        CNCustomer cNCustomer = new CNCustomer();
        try ( ResultSet rs = executeQueryToResultSet("SELECT C.CUST_ID, REPLACE(CI.IDENT_NO,'/') AS ID_NO, C.CUST_NO, "
                + "C.MAIN_BRANCH_ID, C.CUST_CAT, C.CUST_NM, "
                + "NVL((SELECT U.ACCESS_CD FROM " + PHController.CoreSchemaName + ".CUSTOMER_CHANNEL_USER U WHERE U.CUST_ID = C.CUST_ID AND U.USER_CAT_CD='PER' AND U.CHANNEL_ID=9 AND ROWNUM=1),"
                + "(SELECT O.CONTACT FROM " + PHController.CoreSchemaName + ".V_CUSTOMER_CONTACT_MODE O WHERE O.CUST_ID = C.CUST_ID AND O.CONTACT_MODE_CAT_CD IN ('MOBPHONE','TELPHONE') AND ROWNUM=1)) AS CONTACT "
                + "FROM " + PHController.CoreSchemaName + ".CUSTOMER C," + PHController.CoreSchemaName + ".CUSTOMER_IDENTIFICATION CI  "
                + "WHERE C.CUST_ID = CI.CUST_ID AND CI.IDENT_NO='" + idNo + "'"))
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

    public CNAccountMOB queryChargeAccount(CNUserMOB cNUser)
    {
        CNAccountMOB cNAccount = new CNAccountMOB();
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

    public ArrayList<CNAccountMOB> queryEnrolledAccounts(Long custId, String cardNo)
    {
        ArrayList<CNAccountMOB> accounts = new ArrayList<>();
        try ( ResultSet rs = executeQueryToResultSet("SELECT C.ACCT_ID, C.CUST_ID, C.MAIN_BRANCH_ID, C.PROD_ID, C.ACCT_NO, C.ACCT_NM, NVL(B.SHORT_NAME, C.ACCT_NO) AS SHORT_NAME, E.CRNCY_CD, C.PROD_CAT_TY FROM " + PHController.CoreSchemaName + ".CUSTOMER_CHANNEL_USER A, " + PHController.CoreSchemaName + ".CUST_CHANNEL_ACCOUNT B, " + PHController.CoreSchemaName + ".ACCOUNT C, " + PHController.CoreSchemaName + ".CURRENCY E WHERE A.CHANNEL_SCHEME_ID=" + PHController.mobChannelSchemeID + " AND A.ACCESS_CD='" + cardNo + "' AND A.REC_ST='A' AND B.REC_ST=A.REC_ST AND A.CHANNEL_ID=B.CHANNEL_ID AND A.CUST_ID = C.CUST_ID AND B.ACCT_ID = C.ACCT_ID AND A.CUST_ID=B.CUST_ID AND C.PROD_CAT_TY='DP' AND C.ACCT_ID=B.ACCT_ID AND C.REC_ST='A' AND C.CRNCY_ID = E.CRNCY_ID AND B.CUST_CHANNEL_ID = A.CUST_CHANNEL_ID"))
        {
            while (rs.next())
            {
                CNAccountMOB cNAccount = new CNAccountMOB();
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

    public boolean verifyEmail(String acctNo)
    {
        return checkIfExists("SELECT VC.CONTACT FROM " + PHController.CoreSchemaName + ".V_CUSTOMER_CONTACT_EMAIL VC," + PHController.CoreSchemaName + ".ACCOUNT  AC WHERE VC.CUST_ID = AC.CUST_ID AND AC.ACCT_NO ='" + acctNo + "'");
    }

    public boolean verifyAccountForSto(String acctNo, String idNo, String mobileNo)
    {
        return checkIfExists("SELECT A.CUST_ID FROM " + PHController.CoreSchemaName + ".ACCOUNT A," + PHController.CoreSchemaName + ".PERSON PE," + PHController.CoreSchemaName + ".CUSTOMER_CHANNEL_USER CCM "
                + " WHERE A.CUST_ID = PE.CUST_ID "
                + " AND A.CUST_ID = CCM.CUST_ID "
                + " AND PE.CUST_ID = CCM.CUST_ID "
                + " AND A.ACCT_NO = '" + acctNo + "' "
                + "AND PE.SOCIAL_SECURITY_NO = '" + idNo + "' "
                + " AND CCM.ACCESS_CD = '" + mobileNo + "'");
    }

    public boolean isAccountEnrolled(Long custChannelId, Long acctId)
    {
        //    return checkIfExists("SELECT A.ACCT_ID FROM " + PHController.CoreSchemaName + ".CUST_CHANNEL_ACCOUNT A WHERE A.CHANNEL_ID=" + PHController.mobChannelID + " AND A.ACCT_ID=" + acctId + " AND A.CUST_CHANNEL_ID=" + custChannelId);
        return checkIfExists("SELECT A.ACCT_ID FROM " + PHController.CoreSchemaName + ".CUST_CHANNEL_ACCOUNT A WHERE A.CHANNEL_ID=" + PHController.mobChannelID + " AND A.ACCT_ID=" + acctId + " AND A.CUST_ID=" + custChannelId);
    }

    public boolean isMobileAccountEnrolled(Long custChannelId, Long acctId)
    {
        return checkIfExists("SELECT A.ACCT_ID FROM " + PHController.CoreSchemaName + ".CUST_CHANNEL_ACCOUNT A WHERE A.CHANNEL_ID=" + PHController.mobChannelID + " AND A.ACCT_ID=" + acctId + " AND A.CUST_CHANNEL_ID=" + custChannelId);
    }

    public boolean isMobileNumberInContact(String mobileNo)
    {
        return checkIfExists("SELECT A.CONTACT FROM " + PHController.CoreSchemaName + ".CUSTOMER_CONTACT_MODE A WHERE A.CONTACT like '%" + stripMobileNumber2(mobileNo) + "' AND REC_ST ='A' AND CONTACT_MODE_ID =243 ");
    }

    private String stripMobileNumber2(String foneNumber)
    {
        String phNumber = foneNumber;;
        if (foneNumber.startsWith("260"))
        {
            phNumber = phNumber.substring(3);
        }
        else if (foneNumber.startsWith("0"))
        {
            phNumber = foneNumber.substring(1);
        }
        else if (foneNumber.startsWith("+260"))
        {
            phNumber = foneNumber.substring(4);
        }

        if (phNumber.length() != 9)
        {
            phNumber = phNumber + "***";
        }

        return phNumber;
    }

    private String formatMobile(String mobileNo)
    {
        if (mobileNo.startsWith("260"))
        {
            return mobileNo;
        }
        else if (mobileNo.startsWith("0"))
        {
            return "26".concat(mobileNo);
        }
        else if (mobileNo.startsWith("+"))
        {
            return mobileNo.substring(1);
        }
        else
        {
            return mobileNo;
        }
    }

    public boolean pushUserExpiry(String cardNo)
    {
        return executeUpdate("UPDATE " + PHController.CoreSchemaName + ".CUSTOMER_CHANNEL_USER SET EXPIRY_DT=SYSDATE+180 WHERE CHANNEL_SCHEME_ID=" + PHController.mobChannelSchemeID + " AND ACCESS_CD='" + cardNo + "'", true);
    }

    private boolean updateNextEntityId(String tableName, String columnName)
    {
        return executeUpdate("UPDATE " + PHController.CoreSchemaName + ".ENTITY SET NEXT_NO=(SELECT MAX(" + columnName + ")+1 FROM " + PHController.CoreSchemaName + "." + tableName + ") WHERE ENTITY_NM = '" + tableName + "'", true);
    }

    public boolean saveChannelAccount(CNUserMOB cNUser, CNAccountMOB cNAccount)
    {
        if (executeUpdate("INSERT INTO " + PHController.CoreSchemaName + ".CUST_CHANNEL_ACCOUNT (CUST_CHANNEL_ACCT_ID, CUST_ID, CHANNEL_ID, ACCT_ID, SHORT_NAME, REC_ST, VERSION_NO, ROW_TS, USER_ID, CREATE_DT, CREATED_BY, SYS_CREATE_TS, CUST_CHANNEL_ID) VALUES ((SELECT MAX(CUST_CHANNEL_ACCT_ID) + 1 FROM " + PHController.CoreSchemaName + ".CUST_CHANNEL_ACCOUNT), " + cNAccount.getCustId() + ", " + PHController.mobChannelID + ", " + cNAccount.getAcctId() + ", NULL, 'A', 1, SYSDATE, 'SYSTEM', SYSDATE, 'SYSTEM', SYSDATE, " + cNUser.getCustChannelId() + ")", true))
        {
            updateNextEntityId("CUST_CHANNEL_ACCOUNT", "CUST_CHANNEL_ACCT_ID");
            return true;
        }
        return false;
    }

    public boolean updateChannelAccount(CNAccountMOB cNAccount)
    {
        return executeUpdate("UPDATE " + PHController.CoreSchemaName + ".CUSTOMER_CHANNEL SET CHRG_ACCT_ID=" + cNAccount.getAcctId() + " WHERE CHANNEL_SCHEME_ID=" + PHController.mobChannelSchemeID + " AND CUST_ID=" + cNAccount.getCustId(), true);
    }

    public boolean updateAccessName(String accessName, String accessCode)
    {
        return executeUpdate("UPDATE " + PHController.CoreSchemaName + ".CUSTOMER_CHANNEL_USER SET ACCESS_NM='" + accessName + "' WHERE CHANNEL_SCHEME_ID=" + PHController.mobChannelSchemeID + " AND ACCESS_CD='" + accessCode + "'", true);
    }

    public UCActivityMOB queryCardActivity(String cardNumber, String procCode, String currencyCode)
    {
        UCActivityMOB uCActivity = new UCActivityMOB();
        try ( ResultSet rs = executeQueryToResultSet("SELECT COUNT(*) AS VELOCITY, NVL(SUM(TXN_AMOUNT),0) AS VOLUME FROM " + PHController.CMSchemaName + ".EI_MOB_TXN_LOG WHERE TRAN_STATUS='APPROVED' AND CHANNEL_ID=" + PHController.mobChannelID + " AND PROC_CODE='" + procCode + "' AND ACCESS_CODE='" + cardNumber + "' AND CURRENCY_CODE='" + currencyCode + "' AND TXN_DATE_TIME>=TRUNC(ADD_MONTHS(LAST_DAY(SYSDATE),-1)+1) AND TXN_DATE_TIME<=TRUNC(LAST_DAY(SYSDATE))"))
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

    public boolean upsertTerminal(EITerminalMOB eITerminal)
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

    public boolean upsertSMSAlert(SMSTemplate sMSTemplate)
    {
        if (checkIfExists("SELECT TEMPLATE_ID FROM " + PHController.CMSchemaName + ".SMS_TEMPLATE WHERE TEMPLATE_ID = '" + sMSTemplate.getTemplateId() + "'"))
        {
            return updateSMSAler(sMSTemplate);
        }
        else
        {
            return saveSMSAlert(sMSTemplate);
        }
    }

    private boolean saveSMSAlert(SMSTemplate sMSTemplate)
    {
        if (executeUpdate("INSERT INTO " + PHController.CMSchemaName + ".SMS_TEMPLATE(TEMPLATE_ID,TEMPLATE_MESSAGE,CHARGEABLE,CHARGE_DESC,CHARGE_AMT,CHARGE_GL,TAX_PERCENTAGE,TAX_GL,TAX_AMT,TAX_DESC,CURRENCY_CODE,CHANNEL_CODE,DATE_MODIFIED,MODIFIED_BY,REC_ST,ALERT_TYPE,DESCRIPTION) "
                + "VALUES('" + sMSTemplate.getTemplateId() + "','" + sMSTemplate.getTemplateMsg() + "','" + sMSTemplate.getIsChargable() + "','" + sMSTemplate.getChargeDesc() + "'," + sMSTemplate.getChargeAmt() + ",'" + sMSTemplate.getChargeLedger() + "'"
                + "," + sMSTemplate.getTaxPerc() + ",'" + sMSTemplate.getTaxledger() + "'," + sMSTemplate.getTaxAmt() + ",'" + sMSTemplate.getTaxDesc() + "','" + sMSTemplate.getCurrency() + "','" + sMSTemplate.getModule() + "',SYSDATE,'" + sMSTemplate.getModifiedBy() + "','" + sMSTemplate.getStatus() + "','" + sMSTemplate.getAlertType() + "','" + sMSTemplate.getDescription() + "')", true))
        {
            return Boolean.TRUE;
        }
        return false;
    }

    private boolean updateSMSAler(SMSTemplate sMSTemplate)
    {
        if (executeUpdate("UPDATE " + PHController.CMSchemaName + ".SMS_TEMPLATE SET TEMPLATE_ID='" + sMSTemplate.getTemplateId() + "',TEMPLATE_MESSAGE='" + sMSTemplate.getTemplateMsg() + "',CHARGEABLE = '" + sMSTemplate.getIsChargable() + "',CHARGE_DESC ='" + sMSTemplate.getChargeDesc() + "',CHARGE_AMT=" + sMSTemplate.getChargeAmt() + ","
                + "CHARGE_GL ='" + sMSTemplate.getChargeLedger() + "',TAX_PERCENTAGE=" + sMSTemplate.getTaxPerc() + ",TAX_GL='" + sMSTemplate.getTaxledger() + "',TAX_AMT=" + sMSTemplate.getTaxAmt() + ",TAX_DESC='" + sMSTemplate.getTaxDesc() + "',CURRENCY_CODE='" + sMSTemplate.getCurrency() + "',CHANNEL_CODE='" + sMSTemplate.getModule() + "',DATE_MODIFIED=SYSDATE,MODIFIED_BY='" + sMSTemplate.getModifiedBy() + "',REC_ST='" + sMSTemplate.getStatus() + "',ALERT_TYPE ='" + sMSTemplate.getAlertType() + "',DESCRIPTION='" + sMSTemplate.getDescription() + "'"
                + "  WHERE  TEMPLATE_ID='" + sMSTemplate.getTemplateId() + "' ", true))
        {
            return Boolean.TRUE;
        }
        return false;
    }

    private boolean saveTerminal(EITerminalMOB eITerminal)
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

    private boolean updateTerminal(EITerminalMOB eITerminal)
    {
        if (executeUpdate("UPDATE " + PHController.CMSchemaName + ".EI_TERMINAL SET CHANNEL_CODE='" + eITerminal.getChannelCode() + "', LOCATION='" + eITerminal.getLocation() + "', OPERATOR='" + eITerminal.getOperator() + "', BU_NO='" + eITerminal.getBuCode() + "', BU_NM='" + eITerminal.getBuName().replace("'", "") + "', MODIFIED_BY='" + eITerminal.getModifiedBy() + "', DATE_MODIFIED=" + formatDate(eITerminal.getDateModified()) + ", REC_ST='" + eITerminal.getStatus() + "' WHERE TERMINAL_ID='" + eITerminal.getTerminalId() + "'", true))
        {
            return saveTerminalAccounts(eITerminal);
        }
        return false;
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

    private boolean saveTerminalAccounts(EITerminalMOB eITerminal)
    {
        boolean RC = deleteTerminalAccounts(eITerminal);
        for (TMAccountMOB tMAccount : eITerminal.getAccounts().values())
        {
            RC = executeUpdate("INSERT INTO " + PHController.CMSchemaName + ".EI_TERMINAL_ACCOUNT"
                    + "(TERMINAL_ID, CRNCY_CODE, ACCT_NO, ACCT_NM) VALUES"
                    + "('" + eITerminal.getTerminalId() + "', '" + tMAccount.getCurrency() + "', '" + tMAccount.getAccountNumber() + "', '" + tMAccount.getAccountName() + "')", true);
        }
        return RC;
    }

    private boolean deleteTerminalAccounts(EITerminalMOB eITerminal)
    {
        return executeUpdate("DELETE " + PHController.CMSchemaName + ".EI_TERMINAL_ACCOUNT WHERE TERMINAL_ID='" + eITerminal.getTerminalId() + "'", true);
    }

    public HashMap<String, EITerminalMOB> loadTerminals(String channelCode)
    {
        HashMap<String, EITerminalMOB> eITerminals = new HashMap<>();
        try ( ResultSet rs = executeQueryToResultSet("SELECT A.TERMINAL_ID, A.CHANNEL_CODE, A.LOCATION, "
                + "A.OPERATOR, A.BU_NO, A.BU_NM, A.MODIFIED_BY, A.DATE_MODIFIED, A.REC_ST FROM " + PHController.CMSchemaName + ".EI_TERMINAL A "
                + "WHERE A.CHANNEL_CODE='" + channelCode + "' AND (A.REC_ST = 'ACTIVE' OR A.REC_ST = 'Active' OR A.REC_ST = 'Closed' or A.REC_ST = 'CLOSED')  ORDER BY A.TERMINAL_ID ASC"))
//        try (ResultSet rs = executeQueryToResultSet("SELECT A.TERMINAL_ID, A.CHANNEL_CODE, A.LOCATION, "
//                + "A.OPERATOR, A.BU_NO, A.BU_NM, A.MODIFIED_BY, A.DATE_MODIFIED, A.REC_ST FROM " + PHController.CMSchemaName + ".EI_TERMINAL A "
//                + "WHERE A.CHANNEL_CODE='" + channelCode + "' AND (A.REC_ST = 'ACTIVE' OR A.REC_ST = 'Active') ORDER BY A.TERMINAL_ID ASC"))
        {
            while (rs.next())
            {
                EITerminalMOB eITerminal = new EITerminalMOB();
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

    public HashMap<String, SMSTemplate> loadSMSAlerts(String channelCode)
    {
        HashMap<String, SMSTemplate> sMSTemplateHash = new HashMap<>();
        try ( ResultSet rs = executeQueryToResultSet("SELECT TEMPLATE_ID,TEMPLATE_MESSAGE,CHARGEABLE,CHARGE_DESC,CHARGE_AMT, "
                + "CHARGE_GL,TAX_PERCENTAGE,TAX_GL,TAX_AMT,TAX_DESC,CURRENCY_CODE,CHANNEL_CODE,DATE_MODIFIED,MODIFIED_BY,REC_ST,ALERT_TYPE,DESCRIPTION  FROM " + PHController.CMSchemaName + ".SMS_TEMPLATE ORDER BY TEMPLATE_ID ASC"))
        {
            while (rs.next())
            {
                SMSTemplate smsTemplate = new SMSTemplate();
                smsTemplate.setTemplateId(rs.getString("TEMPLATE_ID"));
                smsTemplate.setTemplateMsg(rs.getString("TEMPLATE_MESSAGE"));
                smsTemplate.setIsChargable(rs.getString("CHARGEABLE"));
                smsTemplate.setChargeDesc(rs.getString("CHARGE_DESC"));
                smsTemplate.setChargeAmt(rs.getBigDecimal("CHARGE_AMT"));
                smsTemplate.setChargeLedger(rs.getString("CHARGE_GL"));
                smsTemplate.setTaxPerc(rs.getBigDecimal("TAX_PERCENTAGE"));
                smsTemplate.setTaxledger(rs.getString("TAX_GL"));
                smsTemplate.setTaxAmt(rs.getBigDecimal("TAX_AMT"));
                smsTemplate.setTaxDesc(rs.getString("TAX_DESC"));
                smsTemplate.setCurrency(rs.getString("CURRENCY_CODE"));
                smsTemplate.setModule(rs.getString("CHANNEL_CODE"));
                smsTemplate.setDateModified(rs.getDate("DATE_MODIFIED"));
                smsTemplate.setModifiedBy(rs.getString("MODIFIED_BY"));
                smsTemplate.setStatus(rs.getString("REC_ST"));
                smsTemplate.setAlertType(rs.getString("ALERT_TYPE"));
                smsTemplate.setDescription(rs.getString("DESCRIPTION"));
                sMSTemplateHash.put(smsTemplate.getTemplateId(), smsTemplate);
            }
        }
        catch (Exception ex)
        {
            logError(ex);
        }
        return sMSTemplateHash;

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
                eIBillerCodes.put(eIbillerCode.getBillerCode(), eIbillerCode);
            }
        }
        catch (Exception ex)
        {
            logError(ex);
        }
        return eIBillerCodes;

    }

    public HashMap<String, EIAssocBillerCode> loadAssocBillerCodes(String channelCode, String procCode, String NarrationCode)
    {
        logInfo("SELECT BILLER_CODE,BILLER_DESC,CHANNEL_CODE,ASSOC_PROC_CODE,BU_NO,BU_NM, "
                + "DATE_MODIFIED,MODIFIED_BY,CURRENCY_CODE,ASSOC_ACCT_NO,ASSOC_ACCT_NM,REC_ST,NARRATION_CODE "
                + "FROM  " + PHController.CMSchemaName + ".EI_BILLER_CODE WHERE REC_ST ='Active' AND CHANNEL_CODE = '" + channelCode + "' AND ASSOC_PROC_CODE = '" + procCode + "' AND  NARRATION_CODE ='" + NarrationCode + "'   ORDER BY BILLER_CODE ASC");

        HashMap<String, EIAssocBillerCode> eIBillerCodes = new HashMap<>();
        try ( ResultSet rs = executeQueryToResultSet("SELECT BILLER_CODE,BILLER_DESC,CHANNEL_CODE,ASSOC_PROC_CODE,BU_NO,BU_NM, "
                + "DATE_MODIFIED,MODIFIED_BY,CURRENCY_CODE,ASSOC_ACCT_NO,ASSOC_ACCT_NM,REC_ST,NARRATION_CODE "
                + "FROM  " + PHController.CMSchemaName + ".EI_BILLER_CODE WHERE REC_ST ='Active' AND CHANNEL_CODE = '" + channelCode + "' AND ASSOC_PROC_CODE = '" + procCode + "' AND  NARRATION_CODE ='" + NarrationCode + "'   ORDER BY BILLER_CODE ASC"))
        {
            while (rs.next())
            {
                EIAssocBillerCode eIbillerCode = new EIAssocBillerCode();
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
                eIBillerCodes.put(eIbillerCode.getAssocProcCode(), eIbillerCode);
            }
        }
        catch (Exception ex)
        {
            logError(ex);
        }
        return eIBillerCodes;

    }

    public HashMap<String, TMAccountMOB> queryTerminalAccounts(String terminalId)
    {
        HashMap<String, TMAccountMOB> accounts = new HashMap<>();
        try ( ResultSet rs = executeQueryToResultSet("SELECT TERMINAL_ID, CRNCY_CODE, ACCT_NO, ACCT_NM FROM " + PHController.CMSchemaName + ".EI_TERMINAL_ACCOUNT WHERE TERMINAL_ID='" + terminalId + "' ORDER BY CRNCY_CODE ASC"))
        {
            while (rs.next())
            {
                TMAccountMOB tMAccount = new TMAccountMOB();
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
        if (!PHController.isBlank(text))
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
        if (!PHController.isBlank(text))
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
    public XAPICallerMOB getXapiCaller()
    {
        return xapiCaller;
    }

    /**
     * @param xapiCaller the xapiCaller to set
     */
    public void setXapiCaller(XAPICallerMOB xapiCaller)
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

        if (narration.toUpperCase().contains("PAYMENT") && !narration.toUpperCase().contains("CHARGE") && "D".equalsIgnoreCase(DRCR.trim()))
        {
            return "157";
        }
        else if (narration.toUpperCase().contains("PAYMENT") && !narration.toUpperCase().contains("CHARGE") && "C".equalsIgnoreCase(DRCR.trim()))
        {
            return "113";
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
        else if (narration.toUpperCase().contains("AIRTIME") && "D".equalsIgnoreCase(DRCR.trim()))
        {
            return "156";
        }
        else if (narration.toUpperCase().contains("AIRTIME") && "C".equalsIgnoreCase(DRCR.trim()))
        {
            return "174";
        }

        return "CR".equalsIgnoreCase(DRCR) ? "113" : "174";
    }

    public boolean isBiometricsRegistered(String finType, String customerNumber)
    {
        boolean Registered = checkIfExists("SELECT CUST_ID FROM " + PHController.CoreSchemaName + ".V_CUSTOMER_IMAGES WHERE IMAGE_TY='" + finType + "' AND CUST_ID IN (SELECT CUST_ID FROM " + PHController.CoreSchemaName + ".CUSTOMER WHERE CUST_NO='" + customerNumber + "')");
        if (Registered)
        {
            xapiCaller.setXapiRespCode(EICodesMOB.ACCOUNT_ALREADY_REGISTERED);
        }
        return Registered;
    }

    public boolean isAccountAllowed(String accountNumber)
    {
        boolean accountAllowed = checkIfExists("SELECT ACCT_NO FROM " + PHController.CoreSchemaName + ".V_ACCOUNTS WHERE PROD_CAT_TY='DP' AND ACCT_NO='" + accountNumber + "' AND PROD_ID IN (" + PHController.mobAllowedProductIDs + ") AND REC_ST='A'");
        if (!accountAllowed)
        {
            xapiCaller.setXapiRespCode(EICodesMOB.INVALID_ACCOUNT);
        }
        return accountAllowed;
    }

    public boolean isWalletProductAllowed(String accountNumber)
    {
        boolean accountAllowed = checkIfExists("SELECT ACCT_NO FROM " + PHController.CoreSchemaName + ".V_ACCOUNTS WHERE PROD_CAT_TY='DP' AND ACCT_NO='" + accountNumber + "' AND PROD_ID NOT IN (" + PHController.forbiddenWalletProducts + ") AND REC_ST='A'");
        if (accountAllowed)
        {
            xapiCaller.setXapiRespCode(EICodesMOB.INVALID_ACCOUNT);
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
                xapiCaller.setXapiRespCode(EICodesMOB.TRANSACTION_NOT_ALLOWED_FOR_ACCOUNT);
                logError(ex);
            }
        }
        return retrievalRef.length() > 25 ? retrievalRef.substring(0, 25).trim() : retrievalRef.trim();
    }

    public String fetchJournalId(TXRequestMOB tXRequest)
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
    public ArrayList<CNCurrencyMOB> getcNCurrencies()
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

    public boolean isIdNumberValid(String idNumber, String accessCode)
    {
        return recordExists("SELECT SOCIAL_SECURITY_NO FROM " + PHController.CoreSchemaName + ".PERSON PE," + PHController.CoreSchemaName + ".CUSTOMER_CHANNEL_USER CU WHERE PE.CUST_ID = CU.CUST_ID AND PE.SOCIAL_SECURITY_NO='" + idNumber + "' AND CU.ACCESS_CD ='" + accessCode + "' AND CU.CHANNEL_ID = " + PHController.mobChannelID + " AND CU.REC_ST = 'A'");
    }

    public boolean isIdNumberValid2(String idNumber, String acctNo)
    {
        return recordExists("SELECT SOCIAL_SECURITY_NO FROM " + PHController.CoreSchemaName + ".PERSON PE," + PHController.CoreSchemaName + ".ACCOUNT CU WHERE PE.CUST_ID = CU.CUST_ID AND PE.SOCIAL_SECURITY_NO='" + idNumber + "' AND CU.ACCT_NO ='" + acctNo + "' AND CU.REC_ST = 'A'");
    }

    public boolean isChequeNumberValid(CNAccountMOB cNAccount, long chequeNumber)
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

    public BigDecimal queryTodaysTotalTxnAmount(CNAccountMOB cNAccount, String menuCode)
    {
        BigDecimal totalAmount = BigDecimal.ZERO;
        try ( ResultSet rs = executeQueryToResultSet("SELECT NVL(SUM(TXN_AMOUNT),0) AS TOTAL_AMOUNT FROM " + PHController.CMSchemaName + ".EI_MOB_TXN_LOG WHERE ACCOUNT_NUMBER = '" + cNAccount.getAccountNumber() + "' AND TXN_DATE_TIME >= " + formatDate(getSystemDate()) + " AND CHANNEL_ID = " + PHController.mobChannelID + " AND TRAN_STATUS = 'APPROVED' AND PROC_CODE = '" + menuCode + "'"))
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

    public boolean updateStoppedCheque(CNAccountMOB cNAccount, long chequeNumber, String note)
    {
        return executeUpdate("UPDATE " + PHController.CoreSchemaName + ".DEPOSIT_ACCOUNT_STOP_CHEQUE SET NOTE='" + note + "', ISSUE_DT=SYSDATE WHERE FROM_CHQ_NO=" + chequeNumber + " AND DEPOSIT_ACCT_ID=" + cNAccount.getAcctId(), true);
    }

    public boolean updateChequeWF(Long buId, String itemDesc)
    {
        return executeUpdate("UPDATE " + PHController.CoreSchemaName + ".WF_WORK_ITEM SET BU_ID =" + buId + " WHERE ITEM_DESC ='" + itemDesc + "'", true);
    }

    private String formatDate(Date date)
    {
        if (date != null)
        {
            return "TO_DATE('" + new SimpleDateFormat("dd-MM-yyyy").format(date) + "','DD-MM-YYYY')";
        }
        return null;
    }

//    public CNAccountMOB queryDepositAccount(String accountNumber)
//    {
//        CNAccountMOB cNAccount = new CNAccountMOB();
//        try (ResultSet rs = executeQueryToResultSet("SELECT C.ACCT_ID, C.CUST_ID, C.MAIN_BRANCH_ID, C.PROD_ID, C.ACCT_NO, C.ACCT_NM, E.CRNCY_CD, C.PROD_CAT_TY FROM " + PHController.CoreSchemaName + ".ACCOUNT C, " + PHController.CoreSchemaName + ".CURRENCY E WHERE C.ACCT_NO='" + accountNumber + "' AND C.REC_ST='A' AND C.PROD_CAT_TY='DP' AND C.CRNCY_ID = E.CRNCY_ID"))
//        {
//            if (rs.next())
//            {
//                cNAccount.setAcctId(rs.getLong("ACCT_ID"));
//                cNAccount.setCustId(rs.getLong("CUST_ID"));
//                cNAccount.setBuId(rs.getLong("MAIN_BRANCH_ID"));
//                cNAccount.setProductId(rs.getLong("PROD_ID"));
//                cNAccount.setAccountNumber(rs.getString("ACCT_NO"));
//                cNAccount.setAccountType(rs.getString("PROD_CAT_TY"));
//                cNAccount.setShortName(removeSpaces(rs.getString("ACCT_NM")));
//                cNAccount.setAccountName(removeSpaces(rs.getString("ACCT_NM")));
//                cNAccount.setCurrency(queryCurrency(rs.getString("CRNCY_CD")));
//            }
//        }
//        catch (Exception ex)
//        {
//            logError(ex);
//        }
//        return cNAccount;
//    }
    public CNAccountMOB queryDepositAccount(String accountNumber)
    {
        CNAccountMOB cNAccount = new CNAccountMOB();
        try ( ResultSet rs = executeQueryToResultSet("SELECT C.ACCT_ID, C.CUST_ID, C.MAIN_BRANCH_ID, C.PROD_ID, C.ACCT_NO, C.ACCT_NM, E.CRNCY_CD, C.PROD_CAT_TY,C.REC_ST FROM " + PHController.CoreSchemaName + ".ACCOUNT C, " + PHController.CoreSchemaName + ".CURRENCY E WHERE C.ACCT_NO='" + accountNumber + "' AND C.REC_ST in ('A','U','D','N','Q','C','L')  AND C.PROD_CAT_TY='DP' AND C.CRNCY_ID = E.CRNCY_ID"))
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
            else
            {
                cNAccount.setAcctStatus("N/A");
            }
        }
        catch (Exception ex)
        {
            logError(ex);
        }
        return cNAccount;
    }

    public CNAccountMOB queryDepositAccount(Long acctId)
    {
        CNAccountMOB cNAccount = new CNAccountMOB();
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
        return recordExists("SELECT ACCT_ID FROM " + PHController.CoreSchemaName + ".ACCOUNT WHERE ACCT_NO='" + accountNumber + "' AND REC_ST ='U'");
    }

    public boolean activateUnfundedAccount(String acctNo)
    {
        return executeUpdate("UPDATE " + PHController.CoreSchemaName + ".ACCOUNT SET REC_ST='A' where acct_no = '" + acctNo + "'", true);
    }

    public WFWorkItemDetail getWfItem(String refItem)
    {
        WFWorkItemDetail workFlowItemDetail = new WFWorkItemDetail();
        try ( ResultSet rs = executeQueryToResultSet("SELECT WE.QUEUE_ID, WORK_ITEM_ID, WWI.EVENT_ID PREV_EVENT_ID,     WE.EVENT_ID NEXT_EVENT_ID, E.EVENT_DESC   "
                + "FROM  " + PHController.CoreSchemaName + ".WF_WORK_ITEM WWI,  " + PHController.CoreSchemaName + ".WF_EVENT WE,  " + PHController.CoreSchemaName + ".EVENT E "
                + " WHERE  WE.QUEUE_ID = WWI.QUEUE_ID  AND E.EVENT_ID = WE.EVENT_ID AND UPPER(E.EVENT_DESC) NOT IN ('REJECT','DECLINE','RETURN TO ORIGINATOR')  "
                + " AND (ITEM_REF_NO = '" + refItem + "' or ITEM_DESC ='" + refItem + "') "))
        {
            if (rs.next())
            {
                workFlowItemDetail.setQueueId(rs.getLong("QUEUE_ID"));
                workFlowItemDetail.setWorkItemId(rs.getLong("WORK_ITEM_ID"));
                workFlowItemDetail.setPrevEventId(rs.getLong("PREV_EVENT_ID"));
                workFlowItemDetail.setNextEventId(rs.getLong("NEXT_EVENT_ID"));
                workFlowItemDetail.setEventDesc(rs.getString("EVENT_DESC"));
            }
        }
        catch (Exception ex)
        {
            logError(ex);
        }
        return workFlowItemDetail;
    }

    public CNAccountMOB queryEStatementValues(String acctNo)
    {
        CNAccountMOB cNAccount = new CNAccountMOB();
        try ( ResultSet rs = executeQueryToResultSet("SELECT A.CUST_NM ,'DP' AS STMT_EMAIL_TYPE, NVL(CUC.CONTACT,''),NVL(CUC.CONTACT,''),B.PROD_CAT_TY ,B.ACCT_NO , "
                + "CLI.CUSTOM_LIST_ITEM_CD  "
                + "FROM " + PHController.CoreSchemaName + ".CUSTOMER A ," + PHController.CoreSchemaName + ".ACCOUNT B," + PHController.CoreSchemaName + ".CONTACT_MODE_REF COM,   "
                + "" + PHController.CoreSchemaName + ".CUSTOMER_CONTACT_MODE CUC," + PHController.CoreSchemaName + ".PRODUCT P," + PHController.CoreSchemaName + ".CUSTOM_LIST CUS," + PHController.CoreSchemaName + ".CUSTOM_LIST_ITEM CLI, "
                + "" + PHController.CoreSchemaName + ".UDS_FIELD_VALUE UDS," + PHController.CoreSchemaName + ".USER_DEFINED_SCREEN_FIELD udsf  "
                + "WHERE A.CUST_ID=B.CUST_ID   "
                + "AND CUC.CUST_ID = A.CUST_ID  "
                + "AND CUC.CONTACT_MODE_ID = COM.CONTACT_MODE_ID  "
                + "AND CUC.CONTACT IS NOT NULL  "
                + "AND COM.CONTACT_MODE_ID = 235  "
                + "AND B.PROD_ID = P.PROD_ID  "
                + "AND CUS.CUSTOM_LIST_ID = CLI.CUSTOM_LIST_ID  "
                + "and CUS.CUSTOM_LIST_ID = UDSF.CUSTOM_LIST_TY_ID  "
                + "AND UDS.FIELD_ID = UDSF.FIELD_ID  "
                + "and CLI.CUSTOM_LIST_ITEM_ID = UDS.FIELD_VALUE  AND CUS.CUSTOM_LIST_ID = 11 "
                + "AND UDS.PARENT_ID = B.ACCT_ID AND B.ACCT_NO = '" + acctNo + "'"))
        {
            if (rs.next())
            {
                cNAccount.setShortName(removeSpaces(rs.getString(1)));
                cNAccount.setAccountName(removeSpaces(rs.getString(1)));
                cNAccount.setEmailAdress(rs.getString(3));
                cNAccount.setProdCategory(rs.getString(5));
                cNAccount.setAccountNumber(rs.getString(6));
            }
        }
        catch (Exception ex)
        {
            logError(ex);
        }
        return cNAccount;
    }

    public CNAccountMOB queryLoanAccount(String accountNumber)
    {
        CNAccountMOB cNAccount = new CNAccountMOB();
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

    public CNAccountMOB queryGLAccount(String accountNumber)
    {
        CNAccountMOB cNAccount = new CNAccountMOB();
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
//
//    public CNCustomerMOB queryCustomerById(long custId)
//    {
//        CNCustomerMOB cNCustomer = new CNCustomerMOB();
//        try (ResultSet rs = executeQueryToResultSet("SELECT C.CUST_ID, REPLACE(CI.IDENT_NO,'/') AS ID_NO, C.CUST_NO, "
//                + "C.MAIN_BRANCH_ID, C.CUST_CAT, C.CUST_NM, "
//                + "NVL((SELECT U.ACCESS_CD FROM " + PHController.CoreSchemaName + ".CUSTOMER_CHANNEL_USER U WHERE U.CUST_ID = C.CUST_ID AND U.USER_CAT_CD='PER' AND U.CHANNEL_ID=9 AND ROWNUM=1),"
//                + "(SELECT O.CONTACT FROM " + PHController.CoreSchemaName + ".V_CUSTOMER_CONTACT_MODE O WHERE O.CUST_ID = C.CUST_ID AND O.CONTACT_MODE_CAT_CD IN ('MOBPHONE','TELPHONE') AND ROWNUM=1)) AS CONTACT "
//                + "FROM " + PHController.CoreSchemaName + ".CUSTOMER C," + PHController.CoreSchemaName + ".CUSTOMER_IDENTIFICATION CI  "
//                + "WHERE C.CUST_ID = CI.CUST_ID AND C.CUST_ID=" + custId))
//        {
//            if (rs.next())
//            {
//                cNCustomer.setCustId(rs.getLong("CUST_ID"));
//                cNCustomer.setBuId(rs.getLong("MAIN_BRANCH_ID"));
//                cNCustomer.setIdNo(rs.getString("ID_NO"));
//                cNCustomer.setCustNo(rs.getString("CUST_NO"));
//                cNCustomer.setCustCat(rs.getString("CUST_CAT"));
//                cNCustomer.setCustName(rs.getString("CUST_NM"));
//                cNCustomer.setContact(rs.getString("CONTACT"));
//            }
//        }
//        catch (Exception ex)
//        {
//            logError(ex);
//        }
//        return cNCustomer;
//    }

    public CNCustomerMOB queryCustomerById(long custId)
    {
        CNCustomerMOB cNCustomer = new CNCustomerMOB();
        try ( ResultSet rs = executeQueryToResultSet("SELECT C.CUST_ID, "
                + "NVL ((SELECT   U.ACCESS_CD "
                + " FROM   " + PHController.CoreSchemaName + ".CUSTOMER_CHANNEL_USER U "
                + " WHERE       U.CUST_ID = C.CUST_ID "
                + "     AND U.USER_CAT_CD = 'PER' "
                + "      AND U.CHANNEL_ID = 9 "
                + "    AND ROWNUM = 1), "
                + "   (SELECT   O.CONTACT "
                + "     FROM   " + PHController.CoreSchemaName + ".V_CUSTOMER_CONTACT_MODE O "
                + "   WHERE       O.CUST_ID = C.CUST_ID "
                + "     AND O.CONTACT_MODE_CAT_CD IN ('MOBPHONE', 'TELPHONE') "
                + "       AND ROWNUM = 1) "
                + "  ) AS ID_NO, "
                + "C.CUST_NO, "
                + "C.MAIN_BRANCH_ID, C.CUST_CAT, C.CUST_NM, "
                + "ex.MOBILE_NUMBER    AS CONTACT "
                + "FROM " + PHController.CoreSchemaName + ".CUSTOMER C," + PHController.CoreSchemaName + ".person  CI," + PHController.CoreSchemaName + ".EXT_MOB_MIGRATION ex  "
                + "WHERE C.CUST_ID = CI.CUST_ID  and ex.ID_NO = CI.social_security_no AND C.CUST_ID != 107696 AND C.CUST_ID=" + custId))
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

    public CNCustomerMOB queryCustomerByNumber(String customerNumber)
    {
        CNCustomerMOB cNCustomer = new CNCustomerMOB();
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

    public ArrayList<CNCustomerMOB> queryGroupMembers(long groupCustId)
    {
        ArrayList<CNCustomerMOB> customers = new ArrayList<>();
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

    public ArrayList<CNCustomerMOB> queryGroupLoanBeneficiaries(long groupCustId, String accountNumber)
    {
        ArrayList<CNCustomerMOB> customers = new ArrayList<>();
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

    public Long queryCustomerbyID(String accessCode)
    {
        Long custId = 0L;
        try ( ResultSet rs = executeQueryToResultSet("SELECT DISTINCT CUST_ID FROM " + PHController.CoreSchemaName + ".CUSTOMER_CHANNEL_USER WHERE ACCESS_CD ='" + accessCode + "' AND CHANNEL_ID = 9 AND REC_ST ='A'"))
        {
            if (rs.next())
            {
                custId = rs.getLong("CUST_ID");
            }
        }
        catch (Exception ex)
        {
            logError(ex);
        }
        return custId;
    }

    public ArrayList<CNBranchMOB> queryBusinessUnits()
    {
        ArrayList<CNBranchMOB> branches = new ArrayList<>();
        try ( ResultSet rs = executeQueryToResultSet("SELECT BU_ID, BU_NO, BU_NM, GL_PREFIX_CD FROM " + PHController.CoreSchemaName + ".BUSINESS_UNIT WHERE REC_ST='A' ORDER BY BU_NO"))
        {
            while (rs.next())
            {
                CNBranchMOB cNBranch = new CNBranchMOB();
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

    public CNBranchMOB queryBusinessUnit(Long buId)
    {
        CNBranchMOB cNBranch = new CNBranchMOB();
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

    public CNBranchMOB queryBusinessUnit(String buCode)
    {
        CNBranchMOB cNBranch = new CNBranchMOB();
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

    public boolean upsertSetting(BRMOBSetting bRSetting)
    {
        return recordExists("SELECT CODE FROM " + PHController.CMSchemaName + ".EI_SETTING WHERE CODE='" + bRSetting.getCode() + "' AND MODULE='" + bRSetting.getModule() + "'")
                ? updateSetting(bRSetting)
                : saveSetting(bRSetting);
    }

    private boolean saveSetting(BRMOBSetting bRSetting)
    {
        return executeUpdate("INSERT INTO " + PHController.CMSchemaName + ".EI_SETTING(CODE, VALUE, MODULE, DESCRIPTION, MODIFIED_BY, DATE_MODIFIED) VALUES('" + bRSetting.getCode() + "', '" + (bRSetting.isEncrypted() && !BRCrypt.isEncrypted(bRSetting.getValue()) ? BRCrypt.encrypt(bRSetting.getValue()) : bRSetting.getValue()) + "', '" + bRSetting.getModule() + "', '" + bRSetting.getDescription() + "', '" + bRSetting.getLastModifiedBy() + "', SYSDATE)", true);
    }

    private boolean updateSetting(BRMOBSetting bRSetting)
    {
        return executeUpdate("UPDATE " + PHController.CMSchemaName + ".EI_SETTING SET VALUE='" + (bRSetting.isEncrypted() && !BRCrypt.isEncrypted(bRSetting.getValue()) ? BRCrypt.encrypt(bRSetting.getValue()) : bRSetting.getValue()) + "', DESCRIPTION='" + bRSetting.getDescription() + "', MODIFIED_BY='" + bRSetting.getLastModifiedBy() + "', DATE_MODIFIED=SYSDATE WHERE CODE='" + bRSetting.getCode() + "' AND MODULE='" + bRSetting.getModule() + "'", true);
    }

    public boolean deleteBimetricsImage(String customerNo)
    {
        return executeUpdate("DELETE FROM " + PHController.CoreSchemaName + ".CUSTOMER_IMAGE A WHERE A.CUST_ID = (SELECT CUST_ID FROM " + PHController.CoreSchemaName + ".CUSTOMER WHERE CUST_NO = '" + customerNo + "') AND A.CUST_IMAGE_ID IN (SELECT CUST_IMAGE_ID FROM " + PHController.CoreSchemaName + ".V_CUSTOMER_IMAGES WHERE IMAGE_TY in ('FINRI','FINLI')) AND CUST_ID = A.CUST_ID ", true);
    }

    public HashMap<String, BRMOBSetting> querySettings(String module)
    {
        HashMap<String, BRMOBSetting> bRSettings = new HashMap<>();
        try ( ResultSet rs = executeQueryToResultSet("SELECT CODE, VALUE, MODULE, DESCRIPTION, MODIFIED_BY, DATE_MODIFIED FROM " + PHController.CMSchemaName + ".EI_SETTING WHERE MODULE='" + module + "' ORDER BY CODE ASC"))
        {
            while (rs.next())
            {
                BRMOBSetting bRSetting = new BRMOBSetting();
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
        return recordExists("SELECT REF_NUMBER FROM " + PHController.CMSchemaName + ".EI_MOB_TXN_LOG WHERE REF_NUMBER='" + msgRef + "' AND CHANNEL_ID = " + PHController.mobChannelID + " AND TRAN_STATUS = 'APPROVED'");
    }

    public boolean reversalTransactionExists(String msgRef)
    {
        return recordExists("SELECT REF_NUMBER FROM " + PHController.CMSchemaName + ".EI_MOB_TXN_LOG WHERE REF_NUMBER='" + msgRef + "' AND CHANNEL_ID = " + PHController.mobChannelID + " AND TRAN_STATUS = 'REVERSED'");
    }

    public boolean billerExist(String billerCode)
    {
        return recordExists("SELECT NARRATION_CODE FROM " + PHController.CMSchemaName + ".EI_BILLER_CODE WHERE NARRATION_CODE ='" + billerCode + "' AND CHANNEL_CODE = " + PHController.mobChannelID + "");
    }

    public boolean transactionExists(String msgRef, CNAccountMOB cNAccount)
    {
        return recordExists("SELECT REF_NUMBER FROM " + PHController.CMSchemaName + ".EI_MOB_TXN_LOG WHERE REF_NUMBER='" + msgRef + "' AND ACCOUNT_NUMBER = '" + cNAccount.getAccountNumber() + "' AND CHANNEL_ID = " + PHController.mobChannelID + " AND TRAN_STATUS = 'APPROVED'");
    }

    public boolean checkNewSMSRecords()
    {
        return checkIfExists("SELECT MESSAGE_ID FROM " + PHController.CMSchemaName + ".SMS_ALERT WHERE REC_ST = 'N' and retry <=4");
    }

    public boolean updateSMSStatus(Long MessageId, String smsStatus)
    {
        return executeUpdate("UPDATE " + PHController.CMSchemaName + ".SMS_ALERT SET REC_ST='" + smsStatus + "'  WHERE MESSAGE_ID=" + MessageId + "", true);
    }

    public boolean updateSMSRetryCounter(Long MessageId)
    {
        return executeUpdate("UPDATE " + PHController.CMSchemaName + ".SMS_ALERT SET RETRY =NVL(RETRY,0)+1  WHERE MESSAGE_ID=" + MessageId + "", true);
    }

    public boolean expireOldSms()
    {
        return executeUpdate("UPDATE " + PHController.CMSchemaName + ".SMS_ALERT SET REC_ST='E' WHERE REC_ST NOT IN ('P','F','S') AND PROCESS_DATE < (select to_date(display_value,'DD/MM/YYYY')-1 from " + PHController.CoreSchemaName + ".ctrl_parameter where param_cd ='S02') AND  REC_ST = 'N'", true);
    }

    public List<SMSOutQueus> queryNewMessages(String msstatus)
    {
        SMSOutQueus outQueusBean;
        List<SMSOutQueus> outMessageQueue = new ArrayList<>();
        try ( ResultSet rs = executeQueryToResultSet("SELECT MESSAGE_ID,NVL(ACCT_NAME,'Customer') as ACCT_NAME,NVL(ACCT_NO,'N/A') as ACCT_NO,DESCRIPTION,NVL(AMOUNT,0) as AMOUNT,NVL(CHARGE,0) as CHARGE,DR_CR,REC_ST,PROCESS_DATE,SMS_TYPE,CHANNEL_ID,MOBILE_NO,CONTRA_ACCT_NO,NVL(LEDGER_BAL,0) AS LEDGER_BAL  FROM " + PHController.CMSchemaName + ".SMS_ALERT WHERE REC_ST = '" + msstatus + "' AND RETRY <=4"))
        {
            while (rs.next())
            {
                outQueusBean = new SMSOutQueus();
                outQueusBean.setMessageId(rs.getLong("MESSAGE_ID"));
                outQueusBean.setAcctName(rs.getString("ACCT_NAME"));
                outQueusBean.setAcctNo(rs.getString("ACCT_NO"));
                outQueusBean.setTxDesc(rs.getString("DESCRIPTION"));
                outQueusBean.setTxAmt(rs.getBigDecimal("AMOUNT"));
                outQueusBean.setTxCharge(rs.getBigDecimal("CHARGE"));
                outQueusBean.setDrCr(rs.getString("DR_CR"));
                outQueusBean.setRecSt(rs.getString("REC_ST"));
                outQueusBean.setProcessDate(rs.getString("PROCESS_DATE"));
                outQueusBean.setMobileNo(rs.getString("MOBILE_NO"));
                outQueusBean.setContraAcctNo(rs.getString("CONTRA_ACCT_NO"));
                outQueusBean.setSmsType(rs.getString("SMS_TYPE"));
                outQueusBean.setChannelID(rs.getLong("CHANNEL_ID"));
                outQueusBean.setLedgerBal(rs.getBigDecimal("LEDGER_BAL"));
                //  System.err.println("Acct inseterd " + outQueusBean.getAcctName() + " :=: " + outQueusBean.getMessageId());
                outMessageQueue.add(outQueusBean);
            }
        }
        catch (Exception ex)
        {
            logError(ex);
            ex.printStackTrace();

        }
        return outMessageQueue;
    }

    public SMSTemplate queryMsgTemplate(String templateId)
    {

        SMSTemplate sMSTemplate = null;
        try ( ResultSet rs = executeQueryToResultSet("SELECT TEMPLATE_ID,TEMPLATE_MESSAGE,CHARGEABLE,CHARGE_DESC,CHARGE_AMT,"
                + "CHARGE_GL,TAX_PERCENTAGE,TAX_GL,TAX_AMT,TAX_DESC,CURRENCY_CODE,CHANNEL_CODE,DATE_MODIFIED,MODIFIED_BY,REC_ST "
                + "FROM " + PHController.CMSchemaName + ".SMS_TEMPLATE WHERE  TEMPLATE_ID = '" + templateId + "' AND REC_ST ='Active'"))
        {
            if (rs.next())
            {
                sMSTemplate = new SMSTemplate();
                sMSTemplate.setTemplateId(rs.getString("TEMPLATE_ID"));
                sMSTemplate.setTemplateMsg(rs.getString("TEMPLATE_MESSAGE"));
                sMSTemplate.setIsChargable(rs.getString("CHARGEABLE"));
                sMSTemplate.setChargeDesc(rs.getString("CHARGE_DESC"));
                sMSTemplate.setChargeAmt(rs.getBigDecimal("CHARGE_AMT"));
                sMSTemplate.setChargeLedger(rs.getString("CHARGE_GL"));
                sMSTemplate.setTaxPerc(rs.getBigDecimal("TAX_PERCENTAGE"));
                sMSTemplate.setTaxledger(rs.getString("TAX_GL"));
                sMSTemplate.setTaxAmt(rs.getBigDecimal("TAX_AMT"));
                sMSTemplate.setTaxDesc(rs.getString("TAX_DESC"));
                sMSTemplate.setCurrency(rs.getString("CURRENCY_CODE"));
                sMSTemplate.setModule(rs.getString("CHANNEL_CODE"));
                sMSTemplate.setDateModified(rs.getDate("DATE_MODIFIED"));
                sMSTemplate.setModifiedBy(rs.getString("MODIFIED_BY"));
                sMSTemplate.setStatus(rs.getString("REC_ST"));
            }
            else
            {
                sMSTemplate = null;
            }
        }
        catch (Exception ex)
        {
            logError(ex);
            ex.printStackTrace();

        }
        return sMSTemplate;
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
            PHController.logMobError(ex);
        }
    }

    public void logInfo(String info)
    {
        PHController.logMobInfo(info);
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
     * @return the fetchEStatement
     */
    public CallableStatement getFetchEStatement()
    {
        return fetchEStatement;
    }

    /**
     * @param afetchEStatement the fetchEStatement to set
     */
    public void setFetchEStatement(CallableStatement afetchEStatement)
    {
        this.fetchEStatement = afetchEStatement;
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
