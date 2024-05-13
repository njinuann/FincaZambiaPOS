/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package Mobile;

/**
 *
 * @author Pecherk
 */
import com.neptunesoftware.supernova.ws.client.security.BasicHTTPAuthenticator;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
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

public class MOBController
{

    public static long ChannelID = 9L, SystemUserID, ChannelSchemeID, defaultBURole = -99L;
    private static Properties properties;
    public static int AccountEnrolmentIntervalMM = 5;
    public static int DisplayLines = 1000, BalancesFileUpdateIntervalHH = 1;
    public static String EnableDebug = "N", SendStatementsFile;
    public static String confDir = "mobile/conf", ChannelCode = "MOB", defaultSystemUser = "SYSTEM";
    public static final Properties isoCodes = new Properties();
    private static final Properties xapiCodes = new Properties();
    public static String SuspendMOB = "Y", XapiUser, XapiPassword;
    private static final HashMap<String, String> currencyMap = new HashMap<>();
    public static final HashMap<Integer, String> xapiUrlMap = new HashMap<>();
    public static String JdbcDriverName = "oracle.jdbc.driver.OracleDriver";
    public static String CoreSchemaName, CoreBankingWsContext, CMSchemaURL;
    public static String PrimaryCurrencyCode, CMSchemaPassword;
    private static final TDClientMOB tDClient = new TDClientMOB();
    public static String Module = "MOB", AutoEnrollNewAccounts = "N", ExtTfrCollectionAccount;
    public static ArrayList processingTxnList = new ArrayList();
    public static String logsDir = "mobile/logs", CMSchemaName, BankBin;
    private static HashMap<String, BRMOBSetting> settings = new HashMap<>();
    public static ArrayList<CBNodeMOB> CoreBankingNodes = new ArrayList<>();
    private static final ArrayList<TXProcessorMOB> availableProcessors = new ArrayList<>();
    public static final ArrayList<String> activeTransactions = new ArrayList<>();
    private static int MinProcessorPoolSize = 5, MaxProcessorPoolSize = 10;
    private static HashMap<String, EIChargeMOB> charges = new HashMap<>();
    private static HashMap<String, EITerminalMOB> terminals = new HashMap<>();
    private static HashMap<String, EIProCodesMOB> processingCode = new HashMap<>();
    private static HashMap<String, EIBillerCode> billerCode = new HashMap<>();
    public static String CustomerAccountsFileURL, AccountsFileURL;
    public static String CustomersFileURL, BalancesFileURL, ProcessAutCode /*,PaymentsWsdlURL*/;
    public static String AccountStatementsFileURL, AllowedProductIDs, AllowedLoginRoles, WalletCollection;
    public static String MTNAirtimeCollection, AirtelAirtimeCollection, ZamtelAirtimeCollection, MoneyRemittanceGL;
    public static String sendStatementNowFieldID, smtpAuth, smtpHost, smtpUsername, senderAlias, smtpPassword, emailSubject, sendStatementAllowedId, reportEmailAddress;

    public static void initialize()
    {
        configure();
        loadLibraries();

        querySettings();
        loadCharges();

        loadTerminals();
        loadErrors();

        loadProcessingCodes();
        loadBillerCodes();
        setCurrencyMap();
        if (!"Y".equalsIgnoreCase(SuspendMOB))
        {
            new Thread(MOBController::watchProcessors).start();
        }
    }

    public static void loadLibraries()
    {
        try
        {
            Class.forName(MOBController.JdbcDriverName);
        }
        catch (Exception ex)
        {
            logError(ex);
        }
        try
        {
            Authenticator.setDefault(new BasicHTTPAuthenticator(XapiUser, XapiPassword));
            System.setProperty("javax.xml.rpc.ServiceFactory", "weblogic.webservice.core.rpc.ServiceFactoryImpl");
            System.setProperty("javax.xml.soap.MessageFactory", "weblogic.webservice.core.soap.MessageFactoryImpl");
        }
        catch (Exception ex)
        {
            logError(ex);
        }
    }

    public static void configure()
    {
        properties = new Properties();
        try
        {
            new File(confDir).mkdirs();
            File propsFile = new File(confDir, "settings.prp");
            if (!propsFile.exists())
            {
                logInfo("Missing bridge configuration file. Unable to load bridge settings...");
            }
            try (FileInputStream in = new FileInputStream(propsFile))
            {
                properties.loadFromXML(in);
                //   PaymentsWsdlURL = properties.getProperty("PaymentsWsdlURL");

                CoreBankingWsContext = properties.getProperty("CoreBankingWsContext");
                CMSchemaName = properties.getProperty("CMSchemaName");

                CoreSchemaName = properties.getProperty("CoreSchemaName");
                CMSchemaURL = properties.getProperty("CMSchemaURL");

                CMSchemaPassword = BRCryptMOB.decrypt(properties.getProperty("CMSchemaPassword"));
                EnableDebug = properties.getProperty("EnableDebug", "N");

                JdbcDriverName = properties.getProperty("JdbcDriverName");
                SuspendMOB = properties.getProperty("SuspendMOB", "N");

                XapiPassword = BRCryptMOB.decrypt(properties.getProperty("XapiPassword"));
                XapiUser = BRCryptMOB.decrypt(properties.getProperty("XapiUser"));

                smtpHost = properties.getProperty("smtpHost");
                smtpUsername = properties.getProperty("smtpUsername");

                senderAlias = properties.getProperty("senderAlias");
                smtpPassword = BRCryptMOB.decrypt(properties.getProperty("smtpPassword"));

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
            logError(ex);
        }
        try (FileInputStream in = new FileInputStream(new File(confDir, "xapicodes.prp")))
        {
            getXapiCodes().loadFromXML(in);
        }
        catch (Exception ex)
        {
            logError(ex);
        }
        try (FileInputStream in = new FileInputStream(new File(confDir, "isocodes.prp")))
        {
            isoCodes.loadFromXML(in);
        }
        catch (Exception ex)
        {
            logError(ex);
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
            CBNodeMOB cBNode = new CBNodeMOB();
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
            logError(ex);
        }
    }

    public static CNCurrencyMOB queryCurrency(String codeOrId)
    {
        return tDClient.queryCurrency(codeOrId);
    }

    public static Date getCurrentDate()
    {
        return tDClient.getProcessingDate();
    }

    public static void saveSettings()
    {
        try
        {
            if (properties != null)
            {
                properties.put("SuspendMOB", SuspendMOB);
                properties.storeToXML(new FileOutputStream(new File(confDir, "settings.prp")), "PHilae Properties");
            }
        }
        catch (Exception ex)
        {
            logError(ex);
        }
    }

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
        if (EICodesMOB.XAPI_APPROVED.equals(isoRespCode) && !"0".equals(xapiRespCode) && !EICodesMOB.XAPI_APPROVED.equals(xapiRespCode) && !EICodesMOB.MISSING_ORIGINAL_TXN_REFERENCE.equals(xapiRespCode))
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

    public static boolean querySettings()
    {
        setSettings(tDClient.querySettings(Module));
        BankBin = getSetting("BankBin");
        PrimaryCurrencyCode = getSetting("PrimaryCurrencyCode");

        ChannelID = getLongSetting("ChannelID");
        ChannelSchemeID = getLongSetting("ChannelSchemeID");

        SystemUserID = getLongSetting("SystemUserID");
        SendStatementsFile = getSetting("SendStatementsFile");

        ChannelCode = getSetting("ChannelCode");
        MinProcessorPoolSize = getIntSetting("MinProcessorPoolSize");

        BalancesFileUpdateIntervalHH = getIntSetting("BalancesFileUpdateIntervalHH");
        MaxProcessorPoolSize = getIntSetting("MaxProcessorPoolSize");

        CustomerAccountsFileURL = getSetting("CustomerAccountsFileURL");
        CustomersFileURL = getSetting("CustomersFileURL");

        AccountsFileURL = getSetting("AccountsFileURL");
        BalancesFileURL = getSetting("BalancesFileURL");

        AccountStatementsFileURL = getSetting("AccountStatementsFileURL");
        AllowedProductIDs = getSetting("AllowedProductIDs");

        AccountEnrolmentIntervalMM = getIntSetting("AccountEnrolmentIntervalMM");
        AutoEnrollNewAccounts = getSetting("AutoEnrollNewAccounts");
        ExtTfrCollectionAccount = getSetting("ExtTfrCollectionAccount");

        AllowedLoginRoles = getSetting("AllowedLoginRoles");
        WalletCollection = getSetting("WalletCollection");

        MTNAirtimeCollection = getSetting("MTNAirtimeCollection");
        AirtelAirtimeCollection = getSetting("AirtelAirtimeCollection");

        ZamtelAirtimeCollection = getSetting("ZamtelAirtimeCollection");
        MoneyRemittanceGL = getSetting("MoneyRemittanceGL");

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
            logError(e);
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
            logError(e);
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
            logError(e);
        }
        return BigDecimal.ZERO;
    }

    public static void loadCharges()
    {
        setCharges(tDClient.loadCharges(ChannelCode));
    }

    public static EIChargeMOB getCharge(String chargeCode)
    {
        return charges.get(chargeCode);
    }

    /**
     * @return the charges
     */
    public static HashMap<String, EIChargeMOB> getCharges()
    {
        if (charges.isEmpty())
        {
            loadCharges();
        }
        return charges;
    }

    /**
     * @param aCharges the charges to set
     */
    public static void setCharges(HashMap<String, EIChargeMOB> aCharges)
    {
        charges = aCharges;
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
            logError(ex);
        }
        return clone;
    }

    public static void logError(Exception ex)
    {
        if (BRMainMOB.mobBridgeLog != null)
        {
            BRMainMOB.mobBridgeLog.error(ex);
        }
        else if (ex instanceof Throwable)
        {
            StringBuilder logEvent = new StringBuilder("<event realm=\"" + BRMainMOB.mobBridgeRealm + "\" datetime=\"" + new Date() + "\">");
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

    public static void logInfo(String info)
    {
        if (BRMainMOB.mobBridgeLog != null)
        {
            BRMainMOB.mobBridgeLog.info(info);
        }
        else
        {
            StringBuilder logEvent = new StringBuilder("<event realm=\"" + BRMainMOB.mobBridgeRealm + "\" datetime=\"" + new Date() + "\">");
            logEvent.append("\r\n").append(indentAllLines("<info>" + info + "</info>")).append("\r\n");
            logEvent.append("</event>");
            System.out.println(logEvent);
        }
    }

    public static String indentAllLines(String text)
    {
        String line = "", buffer = "";
        try (BufferedReader bis = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(text.getBytes()))))
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
        try (BufferedReader bis = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(String.valueOf(text).getBytes()), "UTF-8")))
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

    public static TXProcessorMOB fetchProcessor()
    {
        TXProcessorMOB tXProcessor;
        try
        {
            if (!availableProcessors.isEmpty())
            {

                tXProcessor = availableProcessors.get(0);
                availableProcessors.remove(tXProcessor);
            }
            else
            {
                tXProcessor = new TXProcessorMOB();
            }
        }
        catch (Exception ex)
        {
            tXProcessor = new TXProcessorMOB();
        }

        return tXProcessor;
    }

    public static void releaseProcessor(TXProcessorMOB tXProcessor)
    {
        if (availableProcessors.size() < MaxProcessorPoolSize)
        {
            availableProcessors.add(tXProcessor);
        }
        else
        {
            tXProcessor.dispose();
        }
    }

    public static void disposeProcessors()
    {
        availableProcessors.stream().forEach((processor)
                ->
                {
                    processor.dispose();
                });
    }

    public static void watchProcessors()
    {
        while (BRMainMOB.isRunning)
        {
            try
            {
                if (availableProcessors.size() < MinProcessorPoolSize)
                {
                    while (availableProcessors.size() < MinProcessorPoolSize)
                    {
                        availableProcessors.add(new TXProcessorMOB());
                    }
                }
                Thread.sleep(5000);
            }
            catch (Exception ex)
            {
                logError(ex);
            }
        }
    }

    /**
     * @return the settings
     */
    public static HashMap<String, BRMOBSetting> getSettings()
    {
        return settings;
    }

    /**
     * @param aSettings the settings to set
     */
    public static void setSettings(HashMap<String, BRMOBSetting> aSettings)
    {
        settings = aSettings;
    }

    public static void loadTerminals()
    {
        setTerminals(tDClient.loadTerminals(ChannelCode));
    }

    public static void loadProcessingCodes()
    {
        setProcessingCode(tDClient.loadProcessingCodes(ChannelCode));
    }

    public static void loadBillerCodes()
    {
        setBillerCode(tDClient.loadBillerCodes(ChannelCode));
    }

    /**
     * @return the terminals
     */
    public static HashMap<String, EITerminalMOB> getTerminals()
    {
        if (terminals.isEmpty())
        {
            loadTerminals();
        }
        return terminals;
    }

    /**
     * @param aTerminals the terminals to set
     */
    public static void setTerminals(HashMap<String, EITerminalMOB> aTerminals)
    {
        terminals = aTerminals;
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
     * @return the processingCode
     */
    public static HashMap<String, EIProCodesMOB> getProcessingCode()
    {
        if (processingCode.isEmpty())
        {
            loadProcessingCodes();
        }

        return processingCode;
    }

    /**
     * @param aProcessingCode the processingCode to set
     */
    public static void setProcessingCode(HashMap<String, EIProCodesMOB> aProcessingCode)
    {
        processingCode = aProcessingCode;
    }

    /**
     * @return the billerCode
     */
    public static HashMap<String, EIBillerCode> getBillerCode()
    {
        if (billerCode.isEmpty())
        {
            loadBillerCodes();
        }
        return billerCode;
    }

    /**
     * @param aBillerCode the billerCode to set
     */
    public static void setBillerCode(HashMap<String, EIBillerCode> aBillerCode)
    {
        billerCode = aBillerCode;
    }
}
