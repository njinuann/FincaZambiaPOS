/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package PHilae;

import APX.PHController;
import SMS.AXIgnore;
import com.neptunesoftware.supernova.ws.common.XAPIException;
import java.beans.Introspector;
import java.beans.MethodDescriptor;
import java.beans.PropertyDescriptor;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jpos.util.Loggeable;

/**
 *
 * @author Pecherk
 */
public class XAPICaller implements Loggeable
{
    private BigDecimal txnAmount = BigDecimal.ZERO;
    private String txnDescription = "", duration = "";
    private boolean ourTerminal = true, offline = false,billerTxn = false;;
    private boolean ourCustomer = true, reversal = false;
    private String refNumber = "", accountNo = "", terminalId = "";
    private String isoRespCode = "", extraIndent = "\t", xapiRespCode = "";
    private ArrayList<Exception> exceptionsList = new ArrayList();
    private final HashMap<String, Object> callsMap = new HashMap<>();
    private final TXUtility tXUtility = new TXUtility();

    public void logException(Exception ex)
    {
        getExceptionsList().add(ex);
    }

    @Override
    public void dump(PrintStream p, String indent)
    {
        p.println(indent + "<exec>");
        p.println(indent + getExtraIndent() + "<refnumber>" + getRefNumber() + "</refnumber>");
        p.println(indent + getExtraIndent() + "<accountno>" + getAccountNo() + "</accountno>");
        p.println(indent + getExtraIndent() + "<terminalid>" + getTerminalId() + "</terminalid>");
        p.println(indent + getExtraIndent() + "<txnamount>" + getTxnAmount().toPlainString() + "</txnamount>");
        p.println(indent + getExtraIndent() + "<xapitxndesc>" + getTxnDescription() + "</xapitxndesc>");
        p.println(indent + getExtraIndent() + "<isourterminal>" + (isOurTerminal() ? "Yes" : "No") + "</isourterminal>");
        p.println(indent + getExtraIndent() + "<isourcustomer>" + (isOurCustomer() ? "Yes" : "No") + "</isourcustomer>");
        p.println(indent + getExtraIndent() + "<isoffline>" + (isOffline() ? "Yes" : "No") + "</isoffline>");
        p.println(indent + getExtraIndent() + "<isreversal>" + (isReversal() ? "Yes" : "No") + "</isreversal>");

        String[] callKeys = callsMap.keySet().toArray(new String[callsMap.size()]);
        Arrays.sort(callKeys);
        for (Object key : callKeys)
        {
            p.println(indent + getExtraIndent() + "<" + String.valueOf(key).replaceAll("\\d", "") + ">" + cleanText(callsMap.get(key).toString()) + "</" + String.valueOf(key).replaceAll("\\d", "") + ">");
        }

        for (Object exception : getExceptionsList().toArray())
        {
            if (exception != null)
            {
                if (exception instanceof XAPIException)
                {
                    p.println(indent + getExtraIndent() + "<exception>" + exception.toString() + "</exception>");
                }
                else
                {
                    p.println(indent + getExtraIndent() + "<exception>");
                    p.println(indent + getExtraIndent() + getExtraIndent() + "<class>" + ((Exception) exception).getClass().getSimpleName() + "</class>");
                    String emsg = (((Exception) exception).getMessage() == null) ? "" : ((Exception) exception).getMessage();

                    if (emsg.contains("\r\n"))
                    {
                        p.println(indent + getExtraIndent() + getExtraIndent() + "<message>");
                        p.println(indent + getExtraIndent() + getExtraIndent() + getExtraIndent() + ((Exception) exception).getMessage().replaceAll("\r\n", "\r\n" + indent + getExtraIndent() + getExtraIndent() + getExtraIndent()));
                        p.println(indent + getExtraIndent() + getExtraIndent() + "</message>");
                    }
                    else
                    {
                        p.println(indent + getExtraIndent() + getExtraIndent() + "<message>" + ((Exception) exception).getMessage() + "</message>");
                    }
                    p.println(indent + getExtraIndent() + getExtraIndent() + "<stacktrace>");
                    for (StackTraceElement s : ((Throwable) exception).getStackTrace())
                    {
                        p.println(indent + getExtraIndent() + getExtraIndent() + getExtraIndent() + "at " + s.toString());
                    }
                    p.println(indent + getExtraIndent() + getExtraIndent() + "</stacktrace>");
                    p.println(indent + getExtraIndent() + "</exception>");
                }
            }
        }

        p.println(indent + getExtraIndent() + "<xapirespcode>" + getXapiRespCode() + " ~ " + PHController.getXapiMessage(getXapiRespCode()) + "</xapirespcode>");
        p.println(indent + getExtraIndent() + "<responsecode>" + getIsoRespCode() + "</responsecode>");
        p.println(indent + getExtraIndent() + "<duration>" + getDuration() + "</duration>");
        p.print(indent + "</exec>");
    }

    private String cleanText(String text)
    {
        String line, buffer = "";
        try (BufferedReader bis = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(((text != null) ? text : "").getBytes()))))
        {
            while ((line = bis.readLine()) != null)
            {
                buffer += line;
            }
        }
        catch (Exception ex)
        {
            ex = null;
        }

        return buffer;
    }


    /**
     * @return the txnAmount
     */
    public BigDecimal getTxnAmount()
    {
        return txnAmount;
    }

    /**
     * @param txnAmount the txnAmount to set
     */
    public void setTxnAmount(BigDecimal txnAmount)
    {
        this.txnAmount = txnAmount;
    }

    /**
     * @return the txnDescription
     */
    public String getTxnDescription()
    {
        return txnDescription;
    }

    /**
     * @param txnDescription the txnDescription to set
     */
    public void setTxnDescription(String txnDescription)
    {
        this.txnDescription = txnDescription;
    }

    /**
     * @return the duration
     */
    public String getDuration()
    {
        return duration;
    }

    /**
     * @param duration the duration to set
     */
    public void setDuration(String duration)
    {
        this.duration = duration;
    }

    /**
     * @return the exceptionsList
     */
    private ArrayList<Exception> getExceptionsList()
    {
        return exceptionsList;
    }

    /**
     * @param exceptionsList the exceptionsList to set
     */
    public void setExceptionsList(ArrayList<Exception> exceptionsList)
    {
        this.exceptionsList = exceptionsList;
    }

    /**
     * @return the refNumber
     */
    public String getRefNumber()
    {
        return refNumber;
    }

    /**
     * @param refNumber the refNumber to set
     */
    public void setRefNumber(String refNumber)
    {
        this.refNumber = refNumber;
    }

    /**
     * @return the accountNo
     */
    public String getAccountNo()
    {
        return accountNo;
    }

    /**
     * @param accountno the accountNo to set
     */
    public void setAccountNo(String accountno)
    {
        this.accountNo = accountno;
    }

    /**
     * @return the isOurCustomer
     */
    public boolean isOurCustomer()
    {
        return ourCustomer;
    }

    /**
     * @param ourCustomer the isOurCustomer to set
     */
    public void setOurCustomer(boolean ourCustomer)
    {
        this.ourCustomer = ourCustomer;
    }

    /**
     * @return the isOffline
     */
    public boolean isOffline()
    {
        return offline;
    }

    /**
     * @param offline the isOffline to set
     */
    public void setOffline(boolean offline)
    {
        this.offline = offline;
    }

    /**
     * @return the isReversal
     */
    public boolean isReversal()
    {
        return reversal;
    }

    /**
     * @param reversal the isReversal to set
     */
    public void setReversal(boolean reversal)
    {
        this.reversal = reversal;
    }

    /**
     * @return the isoRespCode
     */
    public String getIsoRespCode()
    {
        return isoRespCode;
    }

    /**
     * @param isoRespCode the isoRespCode to set
     */
    public void setIsoRespCode(String isoRespCode)
    {
        this.isoRespCode = isoRespCode;
    }

    /**
     * @return the extraIndent
     */
    public String getExtraIndent()
    {
        return extraIndent;
    }

    /**
     * @param extraIndent the extraIndent to set
     */
    public void setExtraIndent(String extraIndent)
    {
        this.extraIndent = extraIndent;
    }

    /**
     * @return the xapiRespCode
     */
    public String getXapiRespCode()
    {
        return xapiRespCode;
    }

    /**
     * @param xapiRespCode the xapiRespCode to set
     */
    public void setXapiRespCode(String xapiRespCode)
    {
        this.xapiRespCode = xapiRespCode;
    }

    public void setCall(String callRef, Object callObject)
    {
        this.callsMap.put(callsMap.size() + callRef.toLowerCase(), tXUtility.convertToString(callObject));
    }

    /**
     * @return the ourTerminal
     */
    public boolean isOurTerminal()
    {
        return ourTerminal;
    }

    /**
     * @param ourTerminal the ourTerminal to set
     */
    public void setOurTerminal(boolean ourTerminal)
    {
        this.ourTerminal = ourTerminal;
    }

    /**
     * @return the terminalId
     */
    public String getTerminalId()
    {
        return terminalId;
    }

    /**
     * @param terminalId the terminalId to set
     */
    public void setTerminalId(String terminalId)
    {
        this.terminalId = terminalId;
    }

    /**
     * @return the billerTxn
     */
    public boolean isBillerTxn()
    {
        return billerTxn;
    }

    /**
     * @param billerTxn the billerTxn to set
     */
    public void setBillerTxn(boolean billerTxn)
    {
        this.billerTxn = billerTxn;
    }

    
}
