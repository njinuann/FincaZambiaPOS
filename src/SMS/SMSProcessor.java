/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package SMS;

import APX.EICodes;
import APX.PHMain;
import Mobile.TDClientMOB;
import PHilae.TXClient;
import PHilae.TXProcessor;
import PHilae.XAPICaller;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 *
 * @author NJINU
 */
public class SMSProcessor implements Runnable
{

    private TDClientMOB tDClientMOB = new TDClientMOB();
    public String smsMessage;
    private final String messageSent = "Message_Successfully_Sent";
    private final String messageFailed = "Message_Failed";
    private final String messageExprired = "Message_Expired";
    private final String undefinedTemplate = "Template_Not_Defined";
    private final String securityKey = "Integrity@2017!#";
    private SMSCaller sMSCaller = new SMSCaller();

    @Override
    public void run()
    {
        Thread.currentThread().setPriority(Thread.NORM_PRIORITY);
        while (true)
        {
            gettDClientMOB().loadSMS();

            try
            {
                gettDClientMOB().expireOldSms();
                if (gettDClientMOB().checkNewSMSRecords())
                {
                    new SMSProcessor().processSMS();
                }
                gettDClientMOB().dispose();
            }
            catch (Exception ex)
            {
                PHMain.smsLog.logDebug(ex);
            }
            try
            {
                Thread.sleep(1 * 60000);
            }
            catch (Exception ex)
            {
                PHMain.smsLog.logDebug(ex);
            }
        }
    }

    private void processSMS()
    {
        try
        {
            List<SMSOutQueus> outMessageQueue = gettDClientMOB().queryNewMessages("N");

            PHMain.smsLog.logEvent("=============== Start==>count(" + outMessageQueue.size() + ") ==================== ");
            for (SMSOutQueus outQueusBean : outMessageQueue)
            {
                setsMSCaller(new SMSCaller());
                getsMSCaller().setCall("SmsMessageId", outQueusBean.getMessageId());
                getsMSCaller().setCall("acctName", outQueusBean.getAcctName());
                getsMSCaller().setCall("acctNo", outQueusBean.getAcctNo());
                getsMSCaller().setCall("mobileNo", outQueusBean.getMobileNo());
                getsMSCaller().setCall("SmsType", outQueusBean.getSmsType());

                gettDClientMOB().updateSMSRetryCounter(outQueusBean.getMessageId());
                String formulatedMessage = formulateMessage(outQueusBean);

                if ((formulatedMessage.equals("XoX") || formulatedMessage.equals("N/A"))
                        || (outQueusBean.getMobileNo() == null || outQueusBean.getMobileNo().isEmpty() || outQueusBean.getMobileNo().length() < 10))
                {
                    gettDClientMOB().updateSMSStatus(outQueusBean.getMessageId(), "F");
                    getsMSCaller().setCall("FailesSMS", outQueusBean.getSmsType() + "=>[" + outQueusBean.getAcctName() + "~ " + outQueusBean.getMobileNo() + "] " + messageFailed + " \n " + undefinedTemplate);
                }
                else
                {
                    String response = orbitSMS(String.valueOf(outQueusBean.getMessageId()), formulateMessage(outQueusBean),
                            formatMobileNumber(outQueusBean.getMobileNo()), securityKey);
                    if (response.equals(EICodes.ISO_APPROVED))
                    {
                        if (gettDClientMOB().updateSMSStatus(outQueusBean.getMessageId(), "S"))
                        {
                            getsMSCaller().setCall("SuccessfulSMS", outQueusBean.getSmsType() + "=>[" + outQueusBean.getAcctName() + " ~ " + outQueusBean.getMobileNo() + "] " + messageSent);
                        }
                        else
                        {
                            getsMSCaller().setCall("UnloggedSMS", outQueusBean.getSmsType() + "=>[" + outQueusBean.getAcctName() + " ~ " + outQueusBean.getMobileNo() + "] " + messageSent);
                        }
                    }
                    else
                    {
                        gettDClientMOB().updateSMSStatus(outQueusBean.getMessageId(), "F");
                        getsMSCaller().setCall("FailedSMS", outQueusBean.getSmsType() + "==>[" + outQueusBean.getAcctName() + " ~ " + outQueusBean.getMobileNo() + "] " + messageSent);
                    }
                    getsMSCaller().setCall("MessageResponse", response);
                    windUp();

                }
            }
            PHMain.smsLog.logEvent("=============== end ==================== ");
        }
        catch (Exception e)
        {
            PHMain.smsLog.logDebug(e);
        }
    }

    public void windUp()
    {
        PHMain.smsLog.logEvent("exec", "<sms>" + PHMain.smsLog.indentAllLines(" ") + "\r\n" + getsMSCaller() + "\r\n" + "</sms>");

    }

    private void writeToLog()
    {

    }

    private String formulateMessage(SMSOutQueus outQueusBean)
    {
        String formulatedMessage;
        try
        {
            if (outQueusBean.getSmsType().equals("FIN") && outQueusBean.getTxAmt().compareTo(BigDecimal.ZERO) == 0)
            {
                formulatedMessage = "N/A";

            }
            else if ((outQueusBean.getDrCr().endsWith("TFR") || outQueusBean.getDrCr().endsWith("TFR2")) && (outQueusBean.getContraAcctNo() == null || outQueusBean.getAcctNo() == null))
            {
                formulatedMessage = "N/A";
            }
            else
            {
                formulatedMessage = createMessage(outQueusBean);
            }
            getsMSCaller().setCall("Message", formulatedMessage);
        }
        catch (Exception ex)
        {
            formulatedMessage = "N/A";
            PHMain.smsLog.logDebug(ex);
        }
        return formulatedMessage;
    }

    private String createMessage(SMSOutQueus outQueusBean)
    {
        String message;
        String filter;
        switch (outQueusBean.getDrCr())
        {
            case "CR":
            {
                SMSTemplate sMSTemplate = gettDClientMOB().queryMsgTemplate("TCR");
                String MessageCont = sMSTemplate.getTemplateMsg();
                message = Objects.equals(sMSTemplate.getStatus(), "Active") ? replaceHolders(outQueusBean, MessageCont) : "XoX";
                break;
            }
            case "DR":
            {
                SMSTemplate sMSTemplate = gettDClientMOB().queryMsgTemplate("TDR");
                String MessageCont = sMSTemplate.getTemplateMsg();
                // message = replaceHolders(outQueusBean, MessageCont);
                message = Objects.equals(sMSTemplate.getStatus(), "Active") ? replaceHolders(outQueusBean, MessageCont) : "XoX";
                break;
            }
            case "TFR":
            {
                SMSTemplate sMSTemplate = gettDClientMOB().queryMsgTemplate("TFR");
                String MessageCont = sMSTemplate.getTemplateMsg();
//                message = replaceHolders(outQueusBean, MessageCont);
                message = Objects.equals(sMSTemplate.getStatus(), "Active") ? replaceHolders(outQueusBean, MessageCont) : "XoX";
                break;
            }
            case "TFR2":
            {
                SMSTemplate sMSTemplate = gettDClientMOB().queryMsgTemplate("TFR2");
                String MessageCont = sMSTemplate.getTemplateMsg();
//                message = replaceHolders(outQueusBean, MessageCont);
                message = Objects.equals(sMSTemplate.getStatus(), "Active") ? replaceHolders(outQueusBean, MessageCont) : "XoX";
                break;
            }
            case "ENQ":
            {
                SMSTemplate sMSTemplate = gettDClientMOB().queryMsgTemplate("ENQ");
                String MessageCont = sMSTemplate.getTemplateMsg();
//                message = replaceHolders(outQueusBean, MessageCont);
                message = Objects.equals(sMSTemplate.getStatus(), "Active") ? replaceHolders(outQueusBean, MessageCont) : "XoX";
                break;
            }
            case "BRD":
            {
                SMSTemplate sMSTemplate = gettDClientMOB().queryMsgTemplate("BRD");
                String MessageCont = sMSTemplate.getTemplateMsg();
//                message = replaceHolders(outQueusBean, MessageCont);
                message = Objects.equals(sMSTemplate.getStatus(), "Active") ? replaceHolders(outQueusBean, MessageCont) : "XoX";
                break;
            }
            default:
                message = "XoX";
                break;
        }
        return message;
    }

    private String replaceHolders(SMSOutQueus outQueusBean, String templateMsg)
    {
        String message;
        try
        {
            message = templateMsg.replace("{CUSTNAME}", outQueusBean.getAcctName()).equals("N/A") ? "Customer" : templateMsg.replace("{CUSTNAME}", outQueusBean.getAcctName())
                    .replace("{ACCTNO}", outQueusBean.getAcctNo() == null ? "" : outQueusBean.getAcctNo())
                    .replace("{CONTRAACCT}", outQueusBean.getContraAcctNo() == null ? "" : outQueusBean.getContraAcctNo())
                    .replace("{TXNAMT}", String.valueOf(outQueusBean.getTxAmt()) == null ? "0" : String.valueOf(outQueusBean.getTxAmt()))
                    .replace("{CHARGE}", String.valueOf(outQueusBean.getTxCharge()) == null ? "" : String.valueOf(outQueusBean.getTxCharge()))
                    .replace("{TXNDESC}", outQueusBean.getTxDesc())
                    .replace("{TXNDATE}", String.valueOf(outQueusBean.getProcessDate()))
                    .replace("{CURRENCY}", "ZMW")
                    .replace("{CLRDBAL}", String.valueOf(outQueusBean.getLedgerBal()) == null ? "0" : String.valueOf(outQueusBean.getLedgerBal()));

        }
        catch (Exception ex)
        {
            message = "XoX";
            PHMain.smsLog.logDebug(ex);
        }
        return message;

    }

    private String formatMobileNumber(String foneNumber)
    {
        String phNumber = "";
        if (foneNumber.startsWith("+260"))
        {
            phNumber = "260".concat(foneNumber.substring(4));
        }
        else if (foneNumber.startsWith("0"))
        {
            phNumber = "260".concat(foneNumber.substring(1));
        }
        else if (foneNumber.startsWith("9"))
        {
            phNumber = "260".concat(foneNumber);
        }
        else
        {
            phNumber = foneNumber;
        }
        return phNumber;
    }

//    public synchronized static String formatMsisdn(String msisdn)
//    {
//        if (msisdn == null)
//        {
//            return msisdn;
//        }
//        else if (msisdn.length() < 9)
//        {
//            return msisdn;
//        }
//        else if (msisdn.startsWith("0") || msisdn.length() == 9)
//        {
//            return PHController.CountryCode + msisdn.substring(1);
//        }
//        else if (msisdn.length() >= 12)
//        {
//            return msisdn.replace("+", "");
//        }
//        else
//        {
//            return PHController.CountryCode + msisdn.replace("+", "");
//        }
//    }
    private String orbitSMS(String messageID, String messageText, String mobileNumber, String header)
    {
        TXClient tXClient = new TXClient(new TXProcessor());
        tXClient.checkFileUploaderConnection();
        return tXClient.orbitSMS(messageID, messageText, mobileNumber, header);
    }

    /**
     * @return the tDClientMOB
     */
    public TDClientMOB gettDClientMOB()
    {
        return tDClientMOB;
    }

    /**
     * @param tDClientMOB the tDClientMOB to set
     */
    public void settDClientMOB(TDClientMOB tDClientMOB)
    {
        this.tDClientMOB = tDClientMOB;
    }

    /**
     * @return the sMSCaller
     */
    public SMSCaller getsMSCaller()
    {
        return sMSCaller;
    }

    /**
     * @param sMSCaller the sMSCaller to set
     */
    public void setsMSCaller(SMSCaller sMSCaller)
    {
        this.sMSCaller = sMSCaller;
    }

    /**
     * @return the connection
     */
}
