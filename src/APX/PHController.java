/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package APX;

/**
 *
 * @author Pecherk
 */
import Mobile.EIAssocBillerCode;
import PHilae.*;
import Mobile.EIBillerCode;
import Mobile.EIChargeMOB;
import Mobile.EIProCodesMOB;
import Mobile.EITerminalMOB;
import Mobile.TDClientMOB;
import Mobile.TXProcessorMOB;
import SMS.SMSAlertBean;
import SMS.SMSTemplate;
import com.neptunesoftware.supernova.ws.client.security.BasicHTTPAuthenticator;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.Authenticator;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Properties;
import java.util.StringTokenizer;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public class PHController
{

    private static Properties properties;
    public static final Properties isoCodes = new Properties();
    private static final Properties xapiCodes = new Properties();
    public static final Properties posNarrations = new Properties();
    public static final Properties mobNarrations = new Properties();
    public static long posChannelID = 8L, mobChannelID = 9L, SystemUserID, posChannelSchemeID, mobChannelSchemeID, defaultBURole = -99L;
    public static int posAccountEnrolmentIntervalMM = 5, mobAccountEnrolmentIntervalMM = 5, servicePort;
    public static int DisplayLines = 1000, posBalancesFileUpdateIntervalHH = 1, mobBalancesFileUpdateIntervalHH = 1;
    public static String EnablePosDebug = "N", posSendStatementsFile, mobSendStatementsFile, EnableMobDebug = "N";
    public static String confDir = "conf", posChannelCode = "POS", defaultSystemUser = "SYSTEM";
    public static String confDirMob = "mobile/conf", mobChannelCode = "MOB", CountryCode = "260";
    public static String posModule = "POS", MobModule = "MOB", AutoEnrollPosNewAccounts = "N", AutoEnrollMobNewAccounts = "N";
    public static String logsDir = "logs", logsDirMob = "mobile/logs", CMSchemaName, BankBin;
    public static String SuspendPOS = "Y", SuspendMOB = "Y", XapiUser, XapiPassword, synchBRData;
    public static String posCustomerAccountsFileURL, posAccountsFileURL;
    public static String posCustomersFileURL, posBalancesFileURL, FileLoadWsdlURL, RemoteFileUrl;
    public static String mobCustomerAccountsFileURL, mobAccountsFileURL;
    public static String mobCustomersFileURL, mobBalancesFileURL, mobAccountStatementsFileURL;
    public static String posAccountStatementsFileURL, posAllowedProductIDs, posAllowedLoginRoles, mobAllowedProductIDs, mobAllowedLoginRoles, forbiddenWalletProducts;
    public static String JdbcDriverName = "oracle.jdbc.driver.OracleDriver";
    public static String CoreSchemaName, CoreBankingWsContext, CMSchemaURL;
    public static String PrimaryCurrencyCode, CMSchemaPassword;
    private static int MinPosProcessorPoolSize = 5, MaxPosProcessorPoolSize = 10;
    private static int MinMobProcessorPoolSize = 5, MaxMobProcessorPoolSize = 10;
    private static final TDClient tDClient = new TDClient();
    private static final TDClientMOB tDClientMob = new TDClientMOB();
    private static HashMap<String, EICharge> posCharges = new HashMap<>();
    private static HashMap<String, EITerminal> terminals = new HashMap<>();
    private static HashMap<String, EITerminalMOB> mobTerminals = new HashMap<>();
    private static HashMap<String, EIProCodesMOB> posProcessingCode = new HashMap<>();
    private static HashMap<String, EIBillerCode> posBillerCode = new HashMap<>();
    private static HashMap<String, EIProCodesMOB> mobProcessingCode = new HashMap<>();
    private static HashMap<String, EIBillerCode> mobBillerCode = new HashMap<>();
    private static HashMap<String, EIAssocBillerCode> mobAssocBillerCode = new HashMap<>();
    private static HashMap<String, SMSTemplate> smsAlert = new HashMap<>();
    private static HashMap<String, EIChargeMOB> mobCharges = new HashMap<>();
    private static HashMap<String, EICharge> smsCharges = new HashMap<>();
    private static HashMap<String, BRSetting> settings = new HashMap<>();
    private static final HashMap<String, String> currencyMap = new HashMap<>();
    public static final HashMap<Integer, String> xapiUrlMap = new HashMap<>();
    private static final ArrayList<TXProcessor> availablePosProcessors = new ArrayList<>();
    private static final ArrayList<TXProcessorMOB> availableMobProcessors = new ArrayList<>();
    public static final ArrayList<String> activeTransactions = new ArrayList<>();
    public static ArrayList<CBNode> CoreBankingNodes = new ArrayList<>();
    public static ArrayList processingTxnList = new ArrayList();
    private static Date systemDate = new Date();
    public static String AllowedMobProductIDs, WalletCollection, ProcessAutCode, ExtTfrCollectionAccount;
    public static String MTNAirtimeCollection, AirtelAirtimeCollection, ZamtelAirtimeCollection, VodafoneAirtimeCollection, MoneyRemittanceGL;
    public static String MTNB2WCollection, AirtelB2WCollection, ZamtelB2WCollection, MTNW2BCollection, AirtelW2BCollection, ZamtelW2BCollection;
    public static String sendStatementNowFieldID, smtpAuth, smtpPort, smtpHost, smtpUsername, senderAlias, smtpPassword, emailSubject, sendStatementAllowedId, reportEmailAddress;

    public static void initialize()
    {

        configurePos();
        configureMob();

        loadLibraries();
        loadTerminals();

        queryPosSettings();
        queryMobSettings();

        loadPosCharges();
        loadMobCharges();

        loadErrors();
        setCurrencyMap();

        loadPosProcessingCodes();
        loadPosBillerCodes();

        loadMobProcessingCodes();
        loadMobBillerCodes();

        if (!"Y".equalsIgnoreCase(SuspendPOS))
        {
            new Thread(PHController::watchPosProcessors).start();
        }
        if (!"Y".equalsIgnoreCase(SuspendMOB))
        {
            new Thread(PHController::watchMobProcessors).start();
        }
    }

    public static void loadLibraries()
    {
        try
        {
            Class.forName(PHController.JdbcDriverName);
        }
        catch (Exception ex)
        {
            logPosError(ex);
        }
        try
        {
            Authenticator.setDefault(new BasicHTTPAuthenticator(XapiUser, XapiPassword));
            System.setProperty("javax.xml.rpc.ServiceFactory", "weblogic.webservice.core.rpc.ServiceFactoryImpl");
            System.setProperty("javax.xml.soap.MessageFactory", "weblogic.webservice.core.soap.MessageFactoryImpl");
        }
        catch (Exception ex)
        {
            logPosError(ex);
        }
    }

    public static void configurePos()
    {
        properties = new Properties();
        try
        {
            new File(confDir).mkdirs();
            File propsFile = new File(confDir, "settings.prp");
            PHMain.smsLog.logEvent(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>> conf location >>> here  " + propsFile.getAbsolutePath());
            if (!propsFile.exists())
            {
                logPosInfo("Missing bridge configuration file. Unable to load bridge settings...");
            }
            try ( FileInputStream in = new FileInputStream(propsFile))
            {
                properties.loadFromXML(in);
                FileLoadWsdlURL = properties.getProperty("FileLoadWsdlURL");

                CoreBankingWsContext = properties.getProperty("CoreBankingWsContext");
                CMSchemaName = properties.getProperty("CMSchemaName");

                CoreSchemaName = properties.getProperty("CoreSchemaName");
                CMSchemaURL = properties.getProperty("CMSchemaURL");

                CMSchemaPassword = BRCrypt.decrypt(properties.getProperty("CMSchemaPassword"));
                EnablePosDebug = properties.getProperty("EnableDebug", "N");

                JdbcDriverName = properties.getProperty("JdbcDriverName");
                SuspendPOS = properties.getProperty("SuspendPOS", "N");

                XapiPassword = BRCrypt.decrypt(properties.getProperty("XapiPassword"));
                XapiUser = BRCrypt.decrypt(properties.getProperty("XapiUser"));

                synchBRData = properties.getProperty("synchBRData");
                posAllowedLoginRoles = properties.getProperty("posAllowedLoginRoles");

                setCoreBankingNodes();
                try ( InputStream nin = new FileInputStream(new File(confDir, "posnarrations.prp")))
                {
                    posNarrations.loadFromXML(nin);
                    posNarrations.stringPropertyNames().stream().forEach((key)
                            ->
                    {
                        posNarrations.put(key.toUpperCase(), posNarrations.getProperty(key).trim());
                    });
                }
                catch (Exception ex)
                {
                    logPosError(ex);
                }
                try ( InputStream nin = new FileInputStream(new File(confDirMob, "mobnarrations.prp")))
                {
                    mobNarrations.loadFromXML(nin);
                    mobNarrations.stringPropertyNames().stream().forEach((key)
                            ->
                    {
                        mobNarrations.put(key.toUpperCase(), mobNarrations.getProperty(key).trim());
                    });
                }
                catch (Exception ex)
                {
                    logPosError(ex);
                }
            }
        }
        catch (Exception ex)
        {
            logPosError(ex);
        }
        try ( FileInputStream in = new FileInputStream(new File(confDir, "xapicodes.prp")))
        {
            getXapiCodes().loadFromXML(in);
        }
        catch (Exception ex)
        {
            logPosError(ex);
        }
        try ( FileInputStream in = new FileInputStream(new File(confDir, "isocodes.prp")))
        {
            isoCodes.loadFromXML(in);
        }
        catch (Exception ex)
        {
            logPosError(ex);
        }
    }

    public static void configureMob()
    {
        properties = new Properties();
        try
        {
            new File(confDirMob).mkdirs();
            File propsFile = new File(confDirMob, "settings.prp");
            if (!propsFile.exists())
            {
                logPosInfo("Missing bridge configuration file. Unable to load bridge settings...");
            }
            try ( FileInputStream in = new FileInputStream(propsFile))
            {
                properties.loadFromXML(in);
                //   PaymentsWsdlURL = properties.getProperty("PaymentsWsdlURL");

                CoreBankingWsContext = properties.getProperty("CoreBankingWsContext");
                CMSchemaName = properties.getProperty("CMSchemaName");

                CoreSchemaName = properties.getProperty("CoreSchemaName");
                CMSchemaURL = properties.getProperty("CMSchemaURL");

                CMSchemaPassword = BRCrypt.decrypt(properties.getProperty("CMSchemaPassword"));
                EnableMobDebug = properties.getProperty("EnableDebug", "N");

                JdbcDriverName = properties.getProperty("JdbcDriverName");
                SuspendMOB = properties.getProperty("SuspendMOB", "N");

                XapiPassword = BRCrypt.decrypt(properties.getProperty("XapiPassword"));
                XapiUser = BRCrypt.decrypt(properties.getProperty("XapiUser"));

                smtpHost = properties.getProperty("smtpHost");
                smtpPort = properties.getProperty("smtpPort");

                servicePort = Integer.valueOf(properties.getProperty("servicePort"));
                smtpUsername = properties.getProperty("smtpUsername");

                senderAlias = properties.getProperty("senderAlias");
                smtpPassword = BRCrypt.decrypt(properties.getProperty("smtpPassword"));

                emailSubject = properties.getProperty("emailSubject");
                sendStatementAllowedId = properties.getProperty("sendStatementAllowedId");

                reportEmailAddress = properties.getProperty("reportEmailAddress");
                smtpAuth = properties.getProperty("smtpAuth");

                ProcessAutCode = properties.getProperty("ProcessAutCode");

                setCoreBankingNodes();
            }
        }
        catch (Exception ex)
        {
            logPosError(ex);
        }
        try ( FileInputStream in = new FileInputStream(new File(confDir, "xapicodes.prp")))
        {
            getXapiCodes().loadFromXML(in);
        }
        catch (Exception ex)
        {
            logPosError(ex);
        }
        try ( FileInputStream in = new FileInputStream(new File(confDir, "isocodes.prp")))
        {
            isoCodes.loadFromXML(in);
        }
        catch (Exception ex)
        {
            logPosError(ex);
        }
    }

    private static void loadErrors()
    {
        tDClient.updateXapiErrors();
    }

    private static void setCoreBankingNodes()
    {
        StringTokenizer tokenizer = new StringTokenizer(CoreBankingWsContext, "|");
        while (tokenizer.hasMoreTokens())
        {
            CBNode cBNode = new CBNode();
            cBNode.setWsContextURL(tokenizer.nextToken());
            CoreBankingNodes.add(cBNode);
        }
    }

    public static void saveXapiCodes()
    {
        try
        {
            if (getXapiCodes() != null)
            {
                getXapiCodes().storeToXML(new FileOutputStream(new File(confDir, "xapicodes.prp")), "Xapi Codes");
            }
        }
        catch (Exception ex)
        {
            logPosError(ex);
        }
    }

    public static CNCurrency queryCurrency(String codeOrId)
    {
        return tDClient.queryCurrency(codeOrId);
    }

    public static Date getCurrentDate()
    {
        return tDClient.getProcessingDate();
    }

//    public static void saveSettings()
//    {
//        try
//        {
//            if (properties != null)
//            {
//                properties.put("SuspendPOS", SuspendPOS);
//                properties.storeToXML(new FileOutputStream(new File(confDir, "settings.prp")), "PHilae Properties");
//            }
//        }
//        catch (Exception ex)
//        {
//            logPosError(ex);
//        }
//    }
    private static void setCurrencyMap()
    {
        currencyMap.clear();
        currencyMap.put("036", "AUD");
        currencyMap.put("AUD", "036");
        currencyMap.put("124", "CAD");
        currencyMap.put("CAD", "124");
        currencyMap.put("230", "ETB");
        currencyMap.put("ETB", "230");
        currencyMap.put("364", "IRR");
        currencyMap.put("IRR", "364");
        currencyMap.put("JPY", "392");
        currencyMap.put("392", "JPY");
        currencyMap.put("KES", "404");
        currencyMap.put("404", "KES");
        currencyMap.put("454", "MWK");
        currencyMap.put("MWK", "454");
        currencyMap.put("566", "NGN");
        currencyMap.put("NGN", "566");
        currencyMap.put("643", "RUB");
        currencyMap.put("RUB", "643");
        currencyMap.put("756", "CHF");
        currencyMap.put("CHF", "756");
        currencyMap.put("800", "UGX");
        currencyMap.put("UGX", "800");
        currencyMap.put("834", "TZS");
        currencyMap.put("TZS", "834");
        currencyMap.put("840", "USD");
        currencyMap.put("USD", "840");
        currencyMap.put("710", "ZAR");
        currencyMap.put("ZAR", "710");
        currencyMap.put("967", "ZMW");
        currencyMap.put("ZMW", "967");
        currencyMap.put("932", "ZWL");
        currencyMap.put("ZWL", "932");
        currencyMap.put("978", "EUR");
        currencyMap.put("EUR", "978");
        currencyMap.put("826", "GBP");
        currencyMap.put("GBP", "826");
        currencyMap.put("646", "RWF");
        currencyMap.put("RWF", "646");
        currencyMap.put("976", "CDF");
        currencyMap.put("CDF", "976");
    }

    public static String getCurrency(String currencyID)
    {
        return currencyMap.get(currencyID);
    }

    public static String getXapiMessage(String xapiRespCode)
    {
        return getXapiCodes().getProperty(String.valueOf(xapiRespCode), "Undefined error");
    }

    public static String mapToIsoCode(String xapiRespCode)
    {
        String isoRespCode = isoCodes.getProperty(xapiRespCode, "91");
        if (EICodes.XAPI_APPROVED.equals(isoRespCode) && !"0".equals(xapiRespCode) && !EICodes.XAPI_APPROVED.equals(xapiRespCode) && !EICodes.MISSING_ORIGINAL_TXN_REFERENCE.equals(xapiRespCode))
        {
            isoRespCode = "91";
        }
        return isoRespCode;
    }

    /**
     * @return the xapiCodes
     */
    public static Properties getXapiCodes()
    {
        return xapiCodes;
    }

    public static boolean queryPosSettings()
    {
        setSettings(tDClient.querySettings(posModule));
        BankBin = getSetting("BankBin");
        PrimaryCurrencyCode = getSetting("PrimaryCurrencyCode");

        posChannelID = getLongSetting("ChannelID");
        posChannelSchemeID = getLongSetting("ChannelSchemeID");

        SystemUserID = getLongSetting("SystemUserID");
        posSendStatementsFile = getSetting("SendStatementsFile");

        posChannelCode = getSetting("ChannelCode");
        MinPosProcessorPoolSize = getIntSetting("MinProcessorPoolSize");

        posBalancesFileUpdateIntervalHH = getIntSetting("BalancesFileUpdateIntervalHH");
        MaxPosProcessorPoolSize = getIntSetting("MaxProcessorPoolSize");

        posCustomerAccountsFileURL = getSetting("CustomerAccountsFileURL");
        posCustomersFileURL = getSetting("CustomersFileURL");
        RemoteFileUrl = getSetting("RemoteFileUrl");

        posAccountsFileURL = getSetting("AccountsFileURL");
        posBalancesFileURL = getSetting("BalancesFileURL");

        posAccountStatementsFileURL = getSetting("AccountStatementsFileURL");
        posAllowedProductIDs = getSetting("AllowedProductIDs");

        posAccountEnrolmentIntervalMM = getIntSetting("AccountEnrolmentIntervalMM");
        AutoEnrollPosNewAccounts = getSetting("AutoEnrollNewAccounts");

        return !getSettings().isEmpty();
    }

    public static boolean queryMobSettings()
    {
        setSettings(tDClient.querySettings(MobModule));
        BankBin = getSetting("BankBin");
        PrimaryCurrencyCode = getSetting("PrimaryCurrencyCode");

        mobChannelID = getLongSetting("ChannelID");
        mobChannelSchemeID = getLongSetting("ChannelSchemeID");

        SystemUserID = getLongSetting("SystemUserID");
        mobSendStatementsFile = getSetting("SendStatementsFile");

        mobChannelCode = getSetting("ChannelCode");
        MinMobProcessorPoolSize = getIntSetting("MinProcessorPoolSize");

        mobBalancesFileUpdateIntervalHH = getIntSetting("BalancesFileUpdateIntervalHH");
        MaxMobProcessorPoolSize = getIntSetting("MaxProcessorPoolSize");

        mobCustomerAccountsFileURL = getSetting("CustomerAccountsFileURL");
        mobCustomersFileURL = getSetting("CustomersFileURL");

        mobAccountsFileURL = getSetting("AccountsFileURL");
        mobBalancesFileURL = getSetting("BalancesFileURL");

        mobAccountStatementsFileURL = getSetting("AccountStatementsFileURL");
        mobAllowedProductIDs = getSetting("AllowedProductIDs");

        mobAccountEnrolmentIntervalMM = getIntSetting("AccountEnrolmentIntervalMM");
        AutoEnrollMobNewAccounts = getSetting("AutoEnrollNewAccounts");
        ExtTfrCollectionAccount = getSetting("ExtTfrCollectionAccount");

        mobAllowedLoginRoles = getSetting("AllowedLoginRoles");
        WalletCollection = getSetting("WalletCollection");

        MTNAirtimeCollection = getSetting("MTNAirtimeCollection");
        AirtelAirtimeCollection = getSetting("AirtelAirtimeCollection");
        VodafoneAirtimeCollection = getSetting("VodafoneAirtimeCollection");

        MTNB2WCollection = getSetting("MTNB2WCollection");
        AirtelB2WCollection = getSetting("AirtelB2WCollection");
        ZamtelB2WCollection = getSetting("ZamtelB2WCollection");

        MTNW2BCollection = getSetting("MTNW2BCollection");
        AirtelW2BCollection = getSetting("AirtelW2BCollection");
        ZamtelW2BCollection = getSetting("ZamtelW2BCollection");

        ZamtelAirtimeCollection = getSetting("ZamtelAirtimeCollection");
        MoneyRemittanceGL = getSetting("MoneyRemittanceGL");

        forbiddenWalletProducts = getSetting("forbiddenWalletProducts");

        return !getSettings().isEmpty();
    }

    public static String getSetting(String code)
    {
        if (getSettings().containsKey(code))
        {
            return getSettings().get(code).getValue();
        }
        return null;
    }

    public static long getLongSetting(String code)
    {
        try
        {
            if (getSettings().containsKey(code))
            {
                return Long.parseLong(getSettings().get(code).getValue());
            }
        }
        catch (Exception e)
        {
            logPosError(e);
        }
        return 0L;
    }

    public static int getIntSetting(String code)
    {
        try
        {
            if (getSettings().containsKey(code))
            {
                return Integer.parseInt(getSettings().get(code).getValue());
            }
        }
        catch (Exception e)
        {
            logPosError(e);
        }
        return 0;
    }

    public static BigDecimal getDecimalSetting(String code)
    {
        try
        {
            if (getSettings().containsKey(code))
            {
                return new BigDecimal(getSettings().get(code).getValue());
            }
        }
        catch (Exception e)
        {
            logPosError(e);
        }
        return BigDecimal.ZERO;
    }

    public static void loadPosCharges()
    {
        setPosCharges(tDClient.loadCharges(posChannelCode));
    }

    public static EICharge getPosCharge(String chargeCode)
    {
        return posCharges.get(chargeCode);
    }

    public static void loadMobCharges()
    {
        setMobCharges(tDClientMob.loadCharges(mobChannelCode));
    }

    public static EIChargeMOB getMobCharge(String chargeCode)
    {
        return mobCharges.get(chargeCode);
    }

    /**
     * @return the charges
     */
    public static HashMap<String, EIChargeMOB> getMobCharges()
    {
        if (mobCharges.isEmpty())
        {
            loadMobCharges();
        }
        return mobCharges;
    }

    /**
     * @param aMobCharges the charges to set
     */
    public static void setMobCharges(HashMap<String, EIChargeMOB> aMobCharges)
    {
        mobCharges = aMobCharges;
    }

    /**
     * @return the posCharges
     */
    public static HashMap<String, EICharge> getPosCharges()
    {
        if (posCharges.isEmpty())
        {
            loadPosCharges();
        }
        return posCharges;
    }

    /**
     * @param aPosCharges the charges to set
     */
    public static void setPosCharges(HashMap<String, EICharge> aPosCharges)
    {
        posCharges = aPosCharges;
    }

    public static Object cloneObject(Object object)
    {
        Object clone = new Object();
        Class<?> beanClass = object.getClass();
        try
        {
            clone = beanClass.newInstance();
            for (PropertyDescriptor propertyDesc : Introspector.getBeanInfo(beanClass).getPropertyDescriptors())
            {
                if (propertyDesc.getReadMethod() != null)
                {
                    Object value = propertyDesc.getReadMethod().invoke(object);
                    if (propertyDesc.getWriteMethod() != null)
                    {
                        propertyDesc.getWriteMethod().invoke(clone, value);
                    }
                }
            }
        }
        catch (Exception ex)
        {
            logPosError(ex);
        }
        return clone;
    }

    public static void logPosError(Exception ex)
    {
        if (PHMain.posBridgeLog != null)
        {
            PHMain.posBridgeLog.error(ex);
        }
        else if (ex instanceof Throwable)
        {
            StringBuilder logEvent = new StringBuilder("<event realm=\"" + PHMain.posBridgeRealm + "\" datetime=\"" + new Date() + "\">");
            logEvent.append("\r\n").append("\t").append("<error>");
            logEvent.append("\r\n").append("\t").append("\t").append("<class>").append(((Exception) ex).getClass().getSimpleName()).append("</class>");

            logEvent.append("\r\n").append("\t").append("\t").append("<message>").append("[ ").append(cleanText(ex.getMessage())).append(" ]").append("</message>");
            logEvent.append("\r\n").append("\t").append("\t").append("<stacktrace>");
            for (StackTraceElement s : ((Throwable) ex).getStackTrace())
            {
                logEvent.append("\r\n").append("\t").append("\t").append("\t").append("at ").append(s.toString());
            }
            logEvent.append("\r\n").append("\t").append("\t").append("</stacktrace>");
            logEvent.append("\r\n").append("\t").append("</error>");
            logEvent.append("\r\n").append("</event>");
            System.err.println(logEvent);
        }
        else
        {
            ex.printStackTrace();
        }
    }

    public static void logMobError(Exception ex)
    {
        if (PHMain.mobBridgeLog != null)
        {
            PHMain.mobBridgeLog.error(ex);
        }
        else if (ex instanceof Throwable)
        {
            StringBuilder logEvent = new StringBuilder("<event realm=\"" + PHMain.mobBridgeRealm + "\" datetime=\"" + new Date() + "\">");
            logEvent.append("\r\n").append("\t").append("<error>");
            logEvent.append("\r\n").append("\t").append("\t").append("<class>").append(((Exception) ex).getClass().getSimpleName()).append("</class>");

            logEvent.append("\r\n").append("\t").append("\t").append("<message>").append("[ ").append(cleanText(ex.getMessage())).append(" ]").append("</message>");
            logEvent.append("\r\n").append("\t").append("\t").append("<stacktrace>");
            for (StackTraceElement s : ((Throwable) ex).getStackTrace())
            {
                logEvent.append("\r\n").append("\t").append("\t").append("\t").append("at ").append(s.toString());
            }
            logEvent.append("\r\n").append("\t").append("\t").append("</stacktrace>");
            logEvent.append("\r\n").append("\t").append("</error>");
            logEvent.append("\r\n").append("</event>");
            System.err.println(logEvent);
        }
        else
        {
            ex.printStackTrace();
        }
    }

    public static void logPosInfo(String info)
    {
        if (PHMain.posBridgeLog != null)
        {
            PHMain.posBridgeLog.info(info);
        }
        else
        {
            StringBuilder logEvent = new StringBuilder("<event realm=\"" + PHMain.posBridgeRealm + "\" datetime=\"" + new Date() + "\">");
            logEvent.append("\r\n").append(indentAllLines("<info>" + info + "</info>")).append("\r\n");
            logEvent.append("</event>");
            System.out.println(logEvent);
        }

    }

    public static void logMobInfo(String info)
    {

        if (PHMain.mobBridgeLog != null)
        {
            PHMain.mobBridgeLog.info(info);
        }
        else
        {
            StringBuilder logEvent = new StringBuilder("<event realm=\"" + PHMain.mobBridgeRealm + "\" datetime=\"" + new Date() + "\">");
            logEvent.append("\r\n").append(indentAllLines("<info>" + info + "</info>")).append("\r\n");
            logEvent.append("</event>");
            System.out.println(logEvent);
        }
    }

    public static String indentAllLines(String text)
    {
        String line = "", buffer = "";
        try ( BufferedReader bis = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(text.getBytes()))))
        {
            while (line != null)
            {
                buffer += "\t" + line + "\r\n";
                line = bis.readLine();
            }
        }
        catch (Exception ex)
        {
            return buffer;
        }

        return "\t" + buffer.trim();
    }

    private static String cleanText(String text)
    {
        String line, buffer = "";
        try ( BufferedReader bis = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(String.valueOf(text).getBytes()), "UTF-8")))
        {
            while ((line = bis.readLine()) != null)
            {
                buffer += line;
            }
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
        return buffer;
    }

    public static TXProcessor fetchPosProcessor()
    {
        TXProcessor tXProcessor;
        try
        {
            if (!availablePosProcessors.isEmpty())
            {

                tXProcessor = availablePosProcessors.get(0);
                availablePosProcessors.remove(tXProcessor);
            }
            else
            {
                tXProcessor = new TXProcessor();
            }
        }
        catch (Exception ex)
        {
            tXProcessor = new TXProcessor();
        }

        return tXProcessor;
    }

    public static void releasePosProcessor(TXProcessor tXProcessor)
    {
        if (availablePosProcessors.size() < MaxPosProcessorPoolSize)
        {
            // tXProcessor = new TXProcessor();
            availablePosProcessors.add(tXProcessor);
        }
        else
        {
            tXProcessor.dispose();
        }
    }

    public static void disposePosProcessors()
    {
        availablePosProcessors.stream().forEach((processor)
                ->
        {
            processor.dispose();
        });
    }

    public static void watchPosProcessors()
    {
        while (PHMain.isPosRunning)
        {
            try
            {
                if (availablePosProcessors.size() < MinPosProcessorPoolSize)
                {
                    while (availablePosProcessors.size() < MinPosProcessorPoolSize)
                    {
                        availablePosProcessors.add(new TXProcessor());
                    }
                }
                Thread.sleep(5000);
            }
            catch (Exception ex)
            {
                logPosError(ex);
            }
        }
    }

    public static TXProcessorMOB fetchMobProcessor()
    {
        TXProcessorMOB tXProcessorMob;
        try
        {
            if (!availableMobProcessors.isEmpty())
            {

                tXProcessorMob = availableMobProcessors.get(0);
                availableMobProcessors.remove(tXProcessorMob);
            }
            else
            {
                tXProcessorMob = new TXProcessorMOB();
            }
        }
        catch (Exception ex)
        {
            tXProcessorMob = new TXProcessorMOB();
        }

        return tXProcessorMob;
    }

    public static void releaseMobProcessor(TXProcessorMOB tXProcessorMob)
    {
        if (availableMobProcessors.size() < MaxMobProcessorPoolSize)
        {
            // tXProcessorMob = new TXProcessorMOB();
            availableMobProcessors.add(tXProcessorMob);
        }
        else
        {
            tXProcessorMob.dispose();
        }
    }

    public static void disposeMobProcessors()
    {
        availableMobProcessors.stream().forEach((processorMob)
                ->
        {
            processorMob.dispose();
        });
    }

    public static void watchMobProcessors()
    {
        while (PHMain.isMobRunning)
        {
            try
            {
                if (availableMobProcessors.size() < MinMobProcessorPoolSize)
                {
                    while (availableMobProcessors.size() < MinMobProcessorPoolSize)
                    {
                        availableMobProcessors.add(new TXProcessorMOB());
                    }
                }
                Thread.sleep(5000);
            }
            catch (Exception ex)
            {
                logPosError(ex);
            }
        }
    }

    public static boolean isBlank(Object object)
    {
        return object == null || "".equals(String.valueOf(object).trim()) || "null".equals(String.valueOf(object).trim()) || String.valueOf(object).trim().toLowerCase().contains("---select");
    }

    /**
     * @return the settings
     */
    public static HashMap<String, BRSetting> getSettings()
    {
        return settings;
    }

    /**
     * @param aSettings the settings to set
     */
    public static void setSettings(HashMap<String, BRSetting> aSettings)
    {
        settings = aSettings;
    }

    public static void loadTerminals()
    {
        setTerminals(tDClient.loadTerminals(posChannelCode));
    }

    public static void loadMobTerminals()
    {
        setMobTerminals(tDClientMob.loadTerminals(mobChannelCode));
    }

    public static void loadPosProcessingCodes()
    {
        setProcessingCode(tDClient.loadProcessingCodes(posChannelCode));
    }

    public static void loadPosBillerCodes()
    {
        setBillerCode(tDClient.loadBillerCodes(posChannelCode));
    }

    public static void loadMobProcessingCodes()
    {
        setMobProcessingCode(tDClientMob.loadProcessingCodes(mobChannelCode));
    }

    public static void loadAlerts()
    {
        setSmsAlert(tDClientMob.loadSMSAlerts(mobChannelCode));
    }

    public static void loadMobBillerCodes()
    {
        setMobBillerCode(tDClientMob.loadBillerCodes(mobChannelCode));
    }

    public static void loadAssocMobBillerCodes(String procCode, String NarrationCode)
    {
        setMobAssocBillerCode(tDClientMob.loadAssocBillerCodes(mobChannelCode, procCode, NarrationCode));
    }

    /**
     * @return the terminals
     */
    public static HashMap<String, EITerminal> getTerminals()
    {
        if (terminals.isEmpty())
        {
            loadTerminals();
        }
        return terminals;
    }

    public static HashMap<String, EITerminalMOB> getMobTerminals()
    {
        if (mobTerminals.isEmpty())
        {
            loadMobTerminals();
        }
        return mobTerminals;
    }

    /**
     * @return the processingCode
     */
    public static HashMap<String, EIProCodesMOB> getPosProcessingCode()
    {
        if (posProcessingCode.isEmpty())
        {
            loadPosProcessingCodes();
        }
        return posProcessingCode;
    }

    /**
     * @return the processingCode
     */
    public static HashMap<String, EIProCodesMOB> getMobProcessingCode()
    {
        if (mobProcessingCode.isEmpty())
        {
            loadMobProcessingCodes();
        }
        return mobProcessingCode;
    }

    public static HashMap<String, EIBillerCode> getPosBillerCode()
    {
        if (posBillerCode.isEmpty())
        {
            loadPosBillerCodes();
        }
        return posBillerCode;
    }

    /**
     * @return the mobBillerCode
     */
    public static HashMap<String, EIBillerCode> getMobBillerCode()
    {
        if (mobBillerCode.isEmpty())
        {
            loadMobBillerCodes();
        }
        return mobBillerCode;
    }

    public static HashMap<String, EIAssocBillerCode> getAssocMobBillerCode(String procCode, String NarrationCode)
    {
        if (mobAssocBillerCode.isEmpty())
        {
            loadAssocMobBillerCodes(procCode, NarrationCode);
        }
        return mobAssocBillerCode;
    }

    /**
     * @param aSmsAlert the smsAlert to set
     */
    public static void setSmsAlert(HashMap<String, SMSTemplate> aSmsAlert)
    {
        smsAlert = aSmsAlert;
    }

    /**
     * @return the smsAlert
     */
    public static HashMap<String, SMSTemplate> getSmsAlert()
    {
        if (smsAlert.isEmpty())
        {
            loadAlerts();
        }
        return smsAlert;
    }

    /**
     * @param aTerminals the terminals to set
     */
    public static void setTerminals(HashMap<String, EITerminal> aTerminals)
    {
        terminals = aTerminals;
    }

    public static void setMobTerminals(HashMap<String, EITerminalMOB> aMobTerminals)
    {
        mobTerminals = aMobTerminals;
    }

    public static String getWebServiceObjectString(Object wsObject)
    {
        String wsObjectStr = "";
        wsObjectStr = ReflectionToStringBuilder.toString(wsObject, ToStringStyle.SHORT_PREFIX_STYLE);
        if (wsObjectStr.equals(""))
        {
            wsObjectStr = wsObject.toString();
        }
        return wsObjectStr;
    }

    /**
     * @param aPosProcessingCode the processingCode to set
     */
    public static void setProcessingCode(HashMap<String, EIProCodesMOB> aPosProcessingCode)
    {
        posProcessingCode = aPosProcessingCode;
    }

    /**
     * @param aMobProcessingCode the processingCode to set
     */
    public static void setMobProcessingCode(HashMap<String, EIProCodesMOB> aMobProcessingCode)
    {
        mobProcessingCode = aMobProcessingCode;
    }

    /**
     * @param aPosBillerCode the billerCode to set
     */
    public static void setBillerCode(HashMap<String, EIBillerCode> aPosBillerCode)
    {
        posBillerCode = aPosBillerCode;
    }

    public static void setMobBillerCode(HashMap<String, EIBillerCode> aMobBillerCode)
    {
        mobBillerCode = aMobBillerCode;
    }

//    /**
//     * @return the tDClientMob
//     */
//    public static TDClientMOB gettDClientMob()
//    {
//        return tDClientMob;
//    }
    /**
     * @return the mobAssocBillerCode
     */
    public static HashMap<String, EIAssocBillerCode> getMobAssocBillerCode()
    {
        return mobAssocBillerCode;
    }

    /**
     * @param aMobAssocBillerCode the mobAssocBillerCode to set
     */
    public static void setMobAssocBillerCode(HashMap<String, EIAssocBillerCode> aMobAssocBillerCode)
    {
        mobAssocBillerCode = aMobAssocBillerCode;
    }

    /**
     * @return the systemDate
     */
    public static Date getSystemDate()
    {
        return systemDate;
    }

    /**
     * @param aSystemDate the systemDate to set
     */
    public static void setSystemDate(Date aSystemDate)
    {
        systemDate = aSystemDate;
    }
}
