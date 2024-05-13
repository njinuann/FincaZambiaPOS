/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package Mobile;

import APX.PHController;
import com.neptunesoftware.supernova.ws.common.XAPIException;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import org.jpos.util.Loggeable;

/**
 *
 * @author Pecherk
 */
public class CRCallerMOB implements Loggeable
{
    private String narration = "", duration = "";
    private String cardNumber = "", accountNo = "";
    private String extraIndent = "\t", xapiRespCode = "";
    private ArrayList<Exception> exceptionsList = new ArrayList();
    private final HashMap<String, Object> callsMap = new HashMap<>();
    private final TXUtilityMOB tXUtility = new TXUtilityMOB();

    public void logException(Exception ex)
    {
        getExceptionsList().add(ex);
    }

    @Override
    public void dump(PrintStream p, String indent)
    {
        p.println(indent + "<call>");
        p.println(indent + getExtraIndent() + "<cardnumber>" + getCardNumber() + "</cardnumber>");
        p.println(indent + getExtraIndent() + "<accountno>" + getAccountNo() + "</accountno>");
        p.println(indent + getExtraIndent() + "<narration>" + getNarration() + "</narration>");

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
        p.println(indent + getExtraIndent() + "<duration>" + getDuration() + "</duration>");
        p.print(indent + "</call>");
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
        this.callsMap.put(callsMap.size() + callRef, tXUtility.convertToString(callObject));
    }

    /**
     * @return the cardNumber
     */
    public String getCardNumber()
    {
        return cardNumber;
    }

    /**
     * @param cardNumber the cardNumber to set
     */
    public void setCardNumber(String cardNumber)
    {
        this.cardNumber = cardNumber;
    }

    /**
     * @return the narration
     */
    public String getNarration()
    {
        return narration;
    }

    /**
     * @param narration the narration to set
     */
    public void setNarration(String narration)
    {
        this.narration = narration;
    }
}
