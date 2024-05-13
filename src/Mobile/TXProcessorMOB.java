package Mobile;

import APX.EICodes;
import FILELoad.BaseResponse;
import FILELoad.EStmtRequest;
import APX.PHController;
import APX.PHMain;
import com.neptunesoftware.supernova.ws.common.XAPIException;
import com.neptunesoftware.supernova.ws.server.account.data.AccountBalanceOutputData;
import com.neptunesoftware.supernova.ws.server.casemgmt.data.CaseOutputData;
import com.neptunesoftware.supernova.ws.server.transaction.data.ArrayOfDepositTxnOutputData_Literal;
import com.neptunesoftware.supernova.ws.server.transaction.data.DepositTxnOutputData;
import com.neptunesoftware.supernova.ws.server.transaction.data.TxnResponseOutputData;
import com.neptunesoftware.supernova.ws.server.transfer.data.FundsTransferOutputData;
import java.io.File;
import java.io.FileInputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Objects;
import javax.swing.SwingUtilities;
import org.jpos.iso.ISOField;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOSource;
import org.jpos.util.LogEvent;
import org.jpos.util.LogSource;
import org.jpos.util.Logger;

public final class TXProcessorMOB implements LogSource
{

    long startTime = 0;
    int counter = 0;
    ISOSource messageSource;

    String realm = PHMain.mobBridgeRealm;
    ISOMsg requestMessage, responseMessage;

    private String txnJournalId, chargeJournalId;
    private XAPICallerMOB xapiCaller = new XAPICallerMOB();

    private TDClientMOB tDClient = new TDClientMOB();
    private TXClientMOB tXClient = new TXClientMOB(this);

    private UCActivityMOB uCActivity = new UCActivityMOB();
    private EITerminalMOB eITerminal = new EITerminalMOB();

    private TXRequestMOB tXRequest = new TXRequestMOB();
    private EIProCodesMOB eIProCode = new EIProCodesMOB();
    private EIBillerCode eIBillerCode = new EIBillerCode();
    private EIAssocBillerCode eIAsocBillerCode = new EIAssocBillerCode();
    private int chargeReqField = 124;

    Logger logger = Logger.getLogger(PHMain.mobBridgeLogger);
    LogEvent logEvent = new LogEvent(this, "transaction");
    Long processorNumber;

    public void process(ISOSource messageSource, ISOMsg requestMessage)
    {
        try
        {
            this.messageSource = messageSource;
            this.requestMessage = requestMessage;

            responseMessage = (ISOMsg) requestMessage.clone();
            responseMessage.setResponseMTI();

            responseMessage.setHeader(requestMessage.getHeader());
            setTxnJournalId(null);

            startTime = System.currentTimeMillis();
            setChargeJournalId(null);
            processorNumber = startTime;

            setXapiCaller(new XAPICallerMOB());
            settXRequest(new TXRequestMOB());

            gettXRequest().setProcessingCode(requestMessage.getString(3));
            gettDClient().setXapiCaller(getXapiCaller());

            seteITerminal(PHController.getMobTerminals().get(requestMessage.getTerminalID()));
            seteIProCode(PHController.getMobProcessingCode().get(requestMessage.getString(3)));

            seteIBillerCode(PHController.getMobBillerCode().get(requestMessage.getString(123))); //change to suit the right iso field
            getXapiCaller().setCall("get processing code ", requestMessage.getString(3) + "/" + (requestMessage.hasField(123) ? requestMessage.getString(123) : "16"));
            //seteIAsocBillerCode(PHController.getAssocMobBillerCode(requestMessage.getString(3), (requestMessage.hasField(123) ? requestMessage.getString(123) : "16")).get(requestMessage.getString(3))); //change to suit the right iso field
            seteIAsocBillerCode(gettDClient().loadAssocBillerCodes("MOB", requestMessage.getString(3), (requestMessage.hasField(123) ? requestMessage.getString(123) : "16")).get(requestMessage.getString(3)));
            getXapiCaller().setCall("Biller Code ", geteIAsocBillerCode());

            getXapiCaller().setBillerTxn(requestMessage.getString(3).equalsIgnoreCase("035000"));

            String MTI = requestMessage.getMTI();
            getXapiCaller().setOurCustomer(!MTI.startsWith("9"));

            getXapiCaller().setAccountNo(requestMessage.getString(3).equals("036000") ? requestMessage.getAccountNumber2() : requestMessage.getAccountNumber1());
            getXapiCaller().setTxnAmount(requestMessage.getTxnAmount());

            getXapiCaller().setTerminalId(requestMessage.getTerminalID());
            getXapiCaller().setOurTerminal(!requestMessage.hasField(42));

            getXapiCaller().setReversal(MTI.substring(1).startsWith("4"));
            getXapiCaller().setOffline(MTI.substring(2).startsWith("2") && !MTI.substring(1).startsWith("4"));

            getXapiCaller().setRefNumber(getXapiCaller().isReversal() ? requestMessage.getOriginalTxnReference() : requestMessage.getTxnReference());
            getXapiCaller().setXapiRespCode(MTI.substring(1).startsWith("8") ? EICodesMOB.XAPI_APPROVED : getXapiCaller().getXapiRespCode());

            gettXRequest().setAccessCode(requestMessage.getAccessCode());
            gettXRequest().setCurrencyCode(PHController.getCurrency(requestMessage.getTxnCurrency()));

            getXapiCaller().setTxnDescription(PHMain.phFrame.getMobTxnDescription(requestMessage.getProcessingCode()));
            gettXRequest().setReference(getXapiCaller().isReversal() ? requestMessage.getOriginalTxnReference() : requestMessage.getTxnReference());

            gettXRequest().setStan(requestMessage.getTraceAuditNumber());
            gettXRequest().setTxnPrefix(requestMessage.getString(3).substring(4, 6));

            gettXRequest().setTxnIdentifCode(requestMessage.getString(3).substring(2, 4));
            gettXRequest().setBillerIdentifCode(requestMessage.getString(123));

            gettXRequest().setProcCodeIdentifCode(requestMessage.getString(3));
            gettXRequest().setRetrivalRef(requestMessage.getString(37));

            if (!EICodesMOB.XAPI_APPROVED.equals(getXapiCaller().getXapiRespCode()))
            {
                if (isRequestValid())
                {
                    getXapiCaller().setXapiRespCode(processTxn() ? EICodesMOB.XAPI_APPROVED : getXapiCaller().getXapiRespCode());
                }
            }
        }
        catch (Exception ex)
        {
            getXapiCaller().logException(ex);
        }

        sendResponse();
    }

    public boolean processTxn()
    {

        // getXapiCaller().setTxnDescription(PHMain.phFrame.getMobTxnDescription(requestMessage.getProcessingCode()));
        gettXRequest().setDebitAccount(getXapiCaller().getAccountNo());
        gettXRequest().setCreditAccount(requestMessage.getAccountNumber2());
        getXapiCaller().setTxnDescription(replaceGlobalMasks(gettXRequest(), PHController.mobNarrations.getProperty(requestMessage.getString(3))));

        setuCActivity(gettDClient().queryCardActivity(requestMessage.getAccessCode(), requestMessage.getProcessingCode(), gettXRequest().getCurrencyCode()));
        switch (gettXRequest().getProcCodeIdentifCode())
        {
            case "310000":
            case "311000":
            case "312000":
            case "311010":
            case "311020":
            case "312020":
            case "312010":
                settXRequest(validateFields(true, false, false) ? queryAccountBalance(requestMessage.getAccountNumber1(), true) : gettXRequest());
                break;
            case "380000":
            case "382000":
            case "381000":
            case "381010":
            case "381020":
            case "382020":
            case "382010":
                settXRequest(validateFields(true, false, false) ? queryDepositMinistatement() : gettXRequest());
                break;
            case "034000":
                settXRequest(validateFields(true, false, true) ? processAitimePurchase("MTN Airtime Purchase" + (gettXRequest().getBillerIdentifCode().equals("16")
                        ? "[Own]" : "[Other]"), PHController.MTNAirtimeCollection) : gettXRequest());
                break;
            case "034001":
                settXRequest(validateFields(true, false, true) ? processAitimePurchase("", PHController.AirtelAirtimeCollection) : gettXRequest());
                break;
            case "034002":
                settXRequest(validateFields(true, false, true) ? processAitimePurchase("Zamtel Airtime Purchase" + (gettXRequest().getBillerIdentifCode().equals("16")
                        ? "[Own]" : "[Other]"), PHController.ZamtelAirtimeCollection) : gettXRequest());
                break;
            case "034003":
                settXRequest(validateFields(true, false, true) ? processAitimePurchase("Vodafone Airtime Purchase" + (gettXRequest().getBillerIdentifCode().equals("16")
                        ? "[Own]" : "[Other]"), PHController.VodafoneAirtimeCollection) : gettXRequest());
                break;
            case "036000":
                settXRequest(validateFields(false, true, true) ? processWalleToBank(PHController.MTNW2BCollection) : gettXRequest());
                break;
            case "036001":
                settXRequest(validateFields(true, false, true) ? processBankToWallet(PHController.MTNB2WCollection) : gettXRequest());
                break;
            case "036002":
                settXRequest(validateFields(false, true, true) ? processWalleToBank(PHController.AirtelW2BCollection) : gettXRequest());
                break;
            case "036003":
                settXRequest(validateFields(true, false, true) ? processBankToWallet(PHController.AirtelB2WCollection) : gettXRequest());
                break;
            case "036004":
                settXRequest(validateFields(false, true, true) ? processWalleToBank(PHController.ZamtelW2BCollection) : gettXRequest());
                break;
            case "036005":
                settXRequest(validateFields(true, false, true) ? processBankToWallet(PHController.ZamtelB2WCollection) : gettXRequest());
                break;
            case "035000":
                if (!mapUtilityCode().equalsIgnoreCase(EICodesMOB.INVALID_BILLER_CODE))
                {
                    settXRequest(validateFields(true, false, true) ? (processUtilityBills(mapUtilityCode(), getBillerAccountNumber())) : gettXRequest());
                }
                else
                {
                    getXapiCaller().setXapiRespCode(EICodesMOB.INVALID_BILLER_CODE);
                }
                break;
            case "403000":
            case "403010":
            case "403020":
                settXRequest(validateFields(true, true, true) ? processFundsTransfer("MOB Funds Transfer ") : gettXRequest());
                break;
            case "404000":
            case "404010":
            case "404020":
                settXRequest(validateFields(true, false, true) ? processBranchCashWithdrawal("MOB Funds InterTransfer ") : gettXRequest());
                break;
            case "410000":
                settXRequest(validateFields(true, false, true) ? queryToken("MOB Token Transaction ") : gettXRequest());
                break;
            case "420000":
                settXRequest(validateFields(true, false, false) ? attachMobileChannel("MOB Mobile registration ") : gettXRequest());
                break;
            case "500000":
            case "501000":
            case "502000":
            case "501010":
            case "501020":
            case "502020":
            case "502010":
                settXRequest(validateFields(true, false, true) ? processRemittanceDeposit("MOB Remittance Credit ") : gettXRequest());
                break;
            case "510000":
            case "511000":
            case "512000":
            case "511010":
            case "511020":
            case "512020":
            case "512010":
                settXRequest(validateFields(true, false, true) ? processRemittanceWithdrawal("MOB Remittance Debit ") : gettXRequest());
                break;
            case "910000":
            case "911000":
            case "912000":
            case "911010":
            case "911020":
            case "912020":
            case "912010":
                settXRequest(validateFields(true, false, false) ? checkBookRequestCase() : gettXRequest());
                break;
            default:
                if (geteIProCode().getProcCode().substring(0, 2).equals(requestMessage.getProcessingCode()))
                {
                    if (geteIProCode().getProcType().equalsIgnoreCase("DR") && geteIProCode().getRecStatus().equalsIgnoreCase("Active"))
                    {
                        settXRequest(validateFields(true, false, true) ? processBranchCashWithdrawal(geteIProCode().getProcDesc() + " ") : gettXRequest());
                    }
                    else if (geteIProCode().getProcType().equalsIgnoreCase("CR") && geteIProCode().getRecStatus().equalsIgnoreCase("Active"))
                    {
                        settXRequest(validateFields(true, false, true) ? processBranchCashDeposit(geteIProCode().getProcDesc() + " ") : gettXRequest());
                    }
                    else if (geteIProCode().getProcType().equalsIgnoreCase("TFR") && geteIProCode().getRecStatus().equalsIgnoreCase("Active"))
                    {
                        settXRequest(validateFields(true, true, true) ? processFundsTransfer(geteIProCode().getProcDesc() + " ") : gettXRequest());
                    }
                }
                else
                {
                    getXapiCaller().setXapiRespCode(EICodesMOB.UNSUPPORTED_TXN_TYPE);
                }
                break;
        }

        getXapiCaller().setXapiRespCode(getXapiCaller().getXapiRespCode().equals("PE_2113") ? EICodesMOB.XAPI_APPROVED : getXapiCaller().getXapiRespCode()); //this is a workaround
        getXapiCaller().setTxnDescription(gettXRequest().getTxnNarration());
        if (!requestMessage.hasField(chargeReqField))
        {
            logTransaction(gettXRequest());
        }

        return gettXRequest().isSuccessful();
    }

    public String mapUtilityCode()
    {
        String UTLCode, UTLDesc;
        if ((geteIAsocBillerCode().getAssocProcCode().equalsIgnoreCase(requestMessage.getString(3))) && geteIAsocBillerCode().getNtvCodeField().equalsIgnoreCase(gettXRequest().getBillerIdentifCode()))
        {
            UTLCode = geteIAsocBillerCode().getBillerCode();
            UTLDesc = geteIAsocBillerCode().getBillerDesc();
            return UTLCode + "|" + UTLDesc;
        }
        else
        {
            return EICodesMOB.INVALID_BILLER_CODE;
        }

    }

    public BaseResponse generateStatement(EStmtRequest eStmtRequest)
    {
        BaseResponse baseResponse = new BaseResponse();
        EISendEStatement eISendEStatement = new EISendEStatement();
        if (gettDClient().verifyEmail(eStmtRequest.getAccountNo()))
        {
            if (eISendEStatement.fetchTransactions(eStmtRequest))
            {

                baseResponse.setResponseCode(EICodesMOB.ISO_APPROVED);
                baseResponse.setResponseString(PHController.getXapiMessage(EICodesMOB.ISO_APPROVED));

            }
            else
            {
                baseResponse.setResponseCode(PHController.mapToIsoCode(EICodesMOB.TRANSMISSION_ERROR));
                baseResponse.setResponseString(PHController.getXapiMessage(EICodesMOB.TRANSMISSION_ERROR));
            }
        }
        else
        {
            baseResponse.setResponseCode(PHController.mapToIsoCode(EICodesMOB.VALID_EMAIL_MISSING));
            baseResponse.setResponseString(PHController.getXapiMessage(EICodesMOB.VALID_EMAIL_MISSING));
        }
        return baseResponse;
    }

    private String getBillerAccountNumber()
    {
        String UTLAcctNo;
        if (geteIAsocBillerCode().getAssocProcCode().equalsIgnoreCase(requestMessage.getString(3)))
        {
            UTLAcctNo = geteIAsocBillerCode().getAssocAcctNo();
            return UTLAcctNo;
        }
        else
        {
            return EICodesMOB.INVALID_BILLER_CODE;
        }

    }

    private TXRequestMOB queryAccountBalance(String accountNumber, boolean isMainTxn)
    {
        TXRequestMOB request = isMainTxn ? gettXRequest() : new TXRequestMOB();
        try
        {
            request.setTxnNarration((getXapiCaller().isReversal() ? "REV~" : "") + getXapiCaller().getTxnDescription());
            request.setDebitAccount(accountNumber);
            request.setTxnAmount(BigDecimal.ZERO);

            request.setChargeDebitAccount(request.getDebitAccount());
            if (isCardLinked(accountNumber))
            {
                if (isMainTxn)
                {
                    setTxnCharge(request);
                }
                if (requestMessage.hasField(chargeReqField) && Objects.equals(requestMessage.getString(chargeReqField).toUpperCase(), "CHARGE"))
                {
                    responseMessage.set(chargeReqField, queryCharge(request).toPlainString());
                    tXRequest.setSuccessful(true);
                    getXapiCaller().setXapiRespCode(EICodesMOB.XAPI_APPROVED);
                }
                else
                {
                    if (isMainTxn)
                    {
                        if (isBalanceSufficient(request.getDebitAccount(), request.getChargeAmount()))
                        {
                            if (gettXClient().processCharge(request, false))
                            {
                                Object response = gettXClient().queryDepositAccountBalance(request);
                                if (response instanceof AccountBalanceOutputData)
                                {
                                    setBalance(((AccountBalanceOutputData) response).getAvailableBalance(), ((AccountBalanceOutputData) response).getLedgerBalance());
                                    if (isMainTxn)
                                    {
                                        tXRequest.setSuccessful(true);
                                        getXapiCaller().setXapiRespCode(EICodesMOB.XAPI_APPROVED);/*ADDED TO PREVENT RETURNING RESP 91 FOR SUCCESSFUL BI*/

                                    }

                                }
                                else if (response instanceof XAPIException)
                                {
                                    if (isMainTxn)
                                    {
                                        getXapiCaller().setXapiRespCode(gettXClient().getXapiErrorCode((XAPIException) response));
                                    }
                                }
                            }
                        }
                        else
                        {
                            getXapiCaller().setXapiRespCode(EICodes.INSUFFICIENT_FUNDS_1);
                        }
                    }
                    else
                    {
                        Object response = gettXClient().queryDepositAccountBalance(request);
                        if (response instanceof AccountBalanceOutputData)
                        {
                            setBalance(((AccountBalanceOutputData) response).getAvailableBalance(), ((AccountBalanceOutputData) response).getLedgerBalance());
                            if (isMainTxn)
                            {
                                tXRequest.setSuccessful(true);
                                getXapiCaller().setXapiRespCode(EICodesMOB.XAPI_APPROVED);/*ADDED TO PREVENT RETURNING RESP 91 FOR SUCCESSFUL BI*/

                            }

                        }
                        else if (response instanceof XAPIException)
                        {
                            if (isMainTxn)
                            {
                                getXapiCaller().setXapiRespCode(gettXClient().getXapiErrorCode((XAPIException) response));
                            }
                        }
                    }
                }
            }
        }
        catch (Exception ex)
        {
            if (isMainTxn)
            {
                getXapiCaller().setXapiRespCode(gettXClient().getXapiErrorCode(ex));
            }
            getXapiCaller().logException(ex);
        }
        return request;
    }

    private void setBalance(BigDecimal availBalance, BigDecimal ledgerBalance)
    {
        try
        {
            if (availBalance != null)
            {
                responseMessage.set(new ISOField(54, requestMessage.getAccountType1() + "01" + requestMessage.getTxnCurrency() + (ledgerBalance.compareTo(BigDecimal.ZERO) >= 0 ? "C" : "D") + gettDClient().formatIsoAmount(ledgerBalance) + requestMessage.getAccountType1() + "02" + requestMessage.getTxnCurrency() + (availBalance.compareTo(BigDecimal.ZERO) >= 0 ? "C" : "D") + gettDClient().formatIsoAmount(availBalance)));
            }
        }
        catch (Exception ex)
        {
            getXapiCaller().logException(ex);
            try
            {
                responseMessage.set(new ISOField(54, requestMessage.getAccountType1() + "01" + requestMessage.getTxnCurrency() + "D" + "0000000000" + "00" + requestMessage.getAccountType1() + "02" + requestMessage.getTxnCurrency() + "D" + "0000000000" + "00"));
            }
            catch (Exception e)
            {
                getXapiCaller().logException(e);
            }
        }
    }

    private TXRequestMOB checkBookRequestCase()
    {
        try
        {

            gettXRequest().setTxnNarration((getXapiCaller().isReversal() ? "REV~" : "") + "CheckBook Request [ " + (requestMessage.hasField(42) ? requestMessage.getString(42).trim() : requestMessage.getTerminalID()) + "-" + geteITerminal().getLocation() + " ]");
            gettXRequest().setDebitAccount(requestMessage.getAccountNumber1());

            gettXRequest().setChargeDebitAccount(gettXRequest().getDebitAccount());
            gettXRequest().setTxnAmount(BigDecimal.ZERO);
            gettXRequest().setCustomerNo(gettDClient().queryCustomerById(gettDClient().getAccountId(gettXRequest().getDebitAccount())).getCustNo());
            gettXRequest().setBuId(gettDClient().getAccountBuId(tXRequest.getDebitAccount()));

            setTxnCharge(gettXRequest());
            if (isCardLinked(gettXRequest().getDebitAccount()))
            {
                if (gettXClient().processCharge(gettXRequest(), false))
                {
                    Object response = gettXClient().raiseCase(gettXRequest());
                    if (response instanceof CaseOutputData)
                    {
                        responseMessage.set(new ISOField(48, ((CaseOutputData) response).getWfParticipatorRefNo()));
                        queryAccountBalance(gettXRequest().getDebitAccount(), false);
                        gettXRequest().setSuccessful(true);
                        //getXapiCaller().setXapiRespCode(EICodesMOB.XAPI_APPROVED);/*ADDED TO PREVENT RETURNING RESP 91 FOR SUCCESSFUL MS*/

                    }
                    else if (response instanceof XAPIException)
                    {
                        getXapiCaller().setXapiRespCode(gettXClient().getXapiErrorCode((XAPIException) response));
                    }
                }
            }
        }
        catch (Exception ex)
        {
            getXapiCaller().setXapiRespCode(gettXClient().getXapiErrorCode(ex));
            getXapiCaller().logException(ex);
        }
        return gettXRequest();
    }

    private TXRequestMOB queryDepositMinistatement()
    {
        try
        {
            gettXRequest().setTxnNarration((getXapiCaller().isReversal() ? "REV~" : "") + getXapiCaller().getTxnDescription());
            gettXRequest().setDebitAccount(requestMessage.getAccountNumber1());

            gettXRequest().setChargeDebitAccount(gettXRequest().getDebitAccount());
            gettXRequest().setTxnAmount(BigDecimal.ZERO);

            setTxnCharge(gettXRequest());
            if (requestMessage.hasField(chargeReqField) && Objects.equals(requestMessage.getString(chargeReqField).toUpperCase(), "CHARGE"))
            {
                responseMessage.set(chargeReqField, queryCharge(gettXRequest()).toPlainString());
                tXRequest.setSuccessful(true);
                getXapiCaller().setXapiRespCode(EICodesMOB.XAPI_APPROVED);
            }
            else
            {
                if (isCardLinked(gettXRequest().getDebitAccount()))
                {
                    if (isBalanceSufficient(gettXRequest().getDebitAccount(), gettXRequest().getChargeAmount()))
                    {
                        if (gettXClient().processCharge(gettXRequest(), false))
                        {
                            Object response = gettXClient().queryDepositAccountMinistatement(gettXRequest());
                            if (response instanceof ArrayOfDepositTxnOutputData_Literal)
                            {
                                int i = 1;
                                DepositTxnOutputData[] statTxns = ((ArrayOfDepositTxnOutputData_Literal) response).getDepositTxnOutputData();
                                String miniStat = "DATE_TIME|SEQ_NR|TRAN_TYPE|TRAN_AMOUNT|CURR_CODE~";
                                for (DepositTxnOutputData statTxn : statTxns)
                                {
                                    miniStat += new SimpleDateFormat("yyyyMMddhhmmss").format(new Date(statTxn.getTxnDate()))
                                            + "|" + String.format("%06d", i++) + "|" + gettDClient().mapTxnTypeCode((statTxn.getTxnDescription() == null ? "undefined" : statTxn.getTxnDescription()), statTxn.getDrcrFlag())
                                            + "|" + gettDClient().formatIsoAmount(statTxn.getTxnAmount())
                                            + "|" + PHController.getCurrency(statTxn.getTxnCcyISOCode()) + "~";
                                }
                                responseMessage.set(new ISOField(48, miniStat));
                                queryAccountBalance(gettXRequest().getDebitAccount(), false);
                                gettXRequest().setSuccessful(true);
                                getXapiCaller().setXapiRespCode(EICodesMOB.XAPI_APPROVED);/*ADDED TO PREVENT RETURNING RESP 91 FOR SUCCESSFUL MS*/

                            }
                            else if (response instanceof XAPIException)
                            {
                                getXapiCaller().setXapiRespCode(gettXClient().getXapiErrorCode((XAPIException) response));
                            }
                        }
                    }
                    else
                    {
                        getXapiCaller().setXapiRespCode(EICodes.INSUFFICIENT_FUNDS_1);
                    }
                }
            }
        }
        catch (Exception ex)
        {
            getXapiCaller().setXapiRespCode(gettXClient().getXapiErrorCode(ex));
            getXapiCaller().logException(ex);
        }
        return gettXRequest();
    }

    private TXRequestMOB processFundsTransfer(String desc)
    {
        try
        {
            /*ADDED EVALUATION FOR OFFLINE TRASNACTIONS AND APPEND OFFLINE TO THE DESCRIOPTION.. THIS IS FOR EASIER IDENTIFICATION OF THE TXN IN CORE*/

            gettXRequest().setTxnNarration((getXapiCaller().isReversal() ? "REV~" : "") + (getXapiCaller().isOffline() ? "[Offline] " : "") + getXapiCaller().getTxnDescription());
            gettXRequest().setDebitAccount(getXapiCaller().isReversal() ? requestMessage.getAccountNumber2() : requestMessage.getAccountNumber1());

            gettXRequest().setCreditAccount(getXapiCaller().isReversal() ? requestMessage.getAccountNumber1() : requestMessage.getAccountNumber2());
            gettXRequest().setTxnAmount(requestMessage.getTxnAmount());

            gettXRequest().setChargeDebitAccount(gettXRequest().getDebitAccount());
            setTxnCharge(gettXRequest());
            if (requestMessage.hasField(chargeReqField) && Objects.equals(requestMessage.getString(chargeReqField).toUpperCase(), "CHARGE"))
            {
                responseMessage.set(chargeReqField, queryCharge(gettXRequest()).toPlainString());
                tXRequest.setSuccessful(true);
                getXapiCaller().setXapiRespCode(EICodesMOB.XAPI_APPROVED);
            }
            else
            {
                if (isCardLinked(getXapiCaller().isReversal() ? gettXRequest().getCreditAccount() : gettXRequest().getDebitAccount()) ? (isTxnValid(gettXRequest()) && isWithinDailyLimit("TR") && isTfrPermited()) : false)
                {
                    processDepositToDepositTransfer(gettXRequest(), gettXRequest().getDebitAccount());

                }
            }
        }
        catch (Exception ex)
        {
            getXapiCaller().setXapiRespCode(gettXClient().getXapiErrorCode(ex));
            getXapiCaller().logException(ex);
        }
        return gettXRequest();
    }

    private TXRequestMOB queryToken(String desc)
    {
        try
        {
            getXapiCaller().setTxnDescription((getXapiCaller().isReversal() ? "REV~" : "") + desc);
            gettXRequest().setTxnNarration((getXapiCaller().isReversal() ? "REV~" : "") + desc);
            gettXRequest().setDebitAccount(getXapiCaller().isReversal() ? requestMessage.getAccountNumber2() : requestMessage.getAccountNumber1());

            gettXRequest().setCreditAccount(getXapiCaller().isReversal() ? requestMessage.getAccountNumber1() : requestMessage.getAccountNumber2());
//            gettXRequest().setDebitAccount(requestMessage.getAccountNumber1());
//
//            gettXRequest().setCreditAccount(requestMessage.getAccountNumber1());
            gettXRequest().setTxnAmount(requestMessage.getTxnAmount());

            gettXRequest().setChargeDebitAccount(gettXRequest().getDebitAccount());
            setTxnCharge(gettXRequest());
            if (requestMessage.hasField(chargeReqField) && Objects.equals(requestMessage.getString(chargeReqField).toUpperCase(), "CHARGE"))
            {
                responseMessage.set(chargeReqField, queryCharge(gettXRequest()).toPlainString());
                gettXRequest().setSuccessful(true);
                getXapiCaller().setXapiRespCode(EICodesMOB.XAPI_APPROVED);
            }
            else
            {
                if (isCardLinked(getXapiCaller().isReversal() ? gettXRequest().getCreditAccount() : gettXRequest().getDebitAccount()) ? (isTxnValid(gettXRequest()) && isWithinDailyLimit("TR") && isTfrPermited()) : false)
                {
                    processDepositToDepositTransfer(gettXRequest(), gettXRequest().getDebitAccount());

                }

            }
        }
        catch (Exception ex)
        {
            getXapiCaller().setXapiRespCode(gettXClient().getXapiErrorCode(ex));
            getXapiCaller().logException(ex);
        }
        return gettXRequest();
    }

    private TXRequestMOB attachMobileChannel(String desc)
    {
        try
        {
            getXapiCaller().setTxnDescription(desc);
            gettXRequest().setTxnNarration(desc);
            gettXRequest().setTxnAmount(BigDecimal.ZERO);
            gettXRequest().setAccessCode(requestMessage.getAccessCode());
            gettXRequest().setDebitAccount(requestMessage.getAccountNumber1());
            gettXRequest().setCreditAccount(requestMessage.getAccountNumber1());

            CNAccountMOB accountMOB = gettDClient().queryAnyAccount(requestMessage.getAccountNumber1());
            if (requestMessage.hasField(chargeReqField) && Objects.equals(requestMessage.getString(chargeReqField).toUpperCase(), "CHARGE"))
            {
                responseMessage.set(chargeReqField, BigDecimal.ZERO.toPlainString());
                gettXRequest().setSuccessful(true);
                getXapiCaller().setXapiRespCode(EICodesMOB.XAPI_APPROVED);
            }
            else
            {
                if (requestMessage.hasField(2))
                {
                    String validationResponse = checkMobileCompatilbility(tXRequest, accountMOB);
                    if (Objects.equals(EICodes.XAPI_APPROVED, validationResponse))
                    {
                        gettDClient().deleteInvalidChannelUsers();
                        boolean channelExists = gettDClient().queryIfChannelUserExists(accountMOB.getAcctId(), accountMOB.getCustId());

                        if (!channelExists)
                        {
                            boolean created = gettXClient().enrollAccount(accountMOB, gettXRequest());
                            boolean mobileAttached = gettDClient().queryIfChannelUserExists(accountMOB.getAcctId(), accountMOB.getCustId());
                            if (created && mobileAttached)
                            {
                                gettXRequest().setSuccessful(true);
                                getXapiCaller().setXapiRespCode(EICodesMOB.XAPI_APPROVED);
                            }
                            else
                            {
                                gettDClient().deleteInvalidChannelUsers();
                                gettXRequest().setSuccessful(false);
                                getXapiCaller().setXapiRespCode(EICodesMOB.NO_ACTION_TAKEN);
                            }

                        }
                        else
                        {
                            gettXRequest().setSuccessful(false);
                            getXapiCaller().setXapiRespCode(EICodesMOB.INVALID_CARD_NO);
                        }

                    }
                    else
                    {
                        getXapiCaller().setXapiRespCode(validationResponse);
                        gettXRequest().setSuccessful(false);
                    }
                }

            }
        }
        catch (Exception ex)
        {
            getXapiCaller().setXapiRespCode(gettXClient().getXapiErrorCode(ex));
            getXapiCaller().logException(ex);
        }
        return gettXRequest();
    }

    private String checkMobileCompatilbility(TXRequestMOB requestMOB, CNAccountMOB accountMOB)
    {
        //260962482806

        if (requestMOB.getAccessCode().length() != 12 && !requestMOB.getAccessCode().startsWith("260"))
        {
            return EICodes.INVALID_CARD_NO;
        }
        if (gettDClient().isMobileNumberInContact(requestMOB.getAccessCode()))
        {
            return EICodes.INVALID_CARD_NO;
        }
        CNUserMOB cNUser = gettDClient().queryCNUser(requestMOB.getAccessCode());
        if (gettDClient().isMobileAccountEnrolled(cNUser.getCustChannelId(), accountMOB.getAcctId()))
        {
            if (isBlank(accountMOB.getAcctId()))
            {
                return EICodes.INVALID_ACCOUNT;
            }
            else
            {
                return EICodes.ACCOUNT_ALREADY_REGISTERED;
            }
        }

        return EICodes.XAPI_APPROVED;
    }

    public static boolean isBlank(Object object)
    {
        return object == null || "".equals(String.valueOf(object).trim()) || "null".equals(String.valueOf(object).trim()) || String.valueOf(object).trim().toLowerCase().contains("---select");
    }

    private TXRequestMOB processBranchCashWithdrawal(String desc)
    {
        try
        {
            /*ADDED EVALUATION FOR OFFLINE TRASNACTIONS AND APPEND OFFLINE TO THE DESCRIOPTION.. THIS IS FOR EASIER IDENTIFICATION OF THE TXN IN CORE*/
            gettXRequest().setTxnNarration((getXapiCaller().isReversal() ? "REV~" : "") + (getXapiCaller().isOffline() ? "[Offline] " : "") + getXapiCaller().getTxnDescription());
            gettXRequest().setBuId(gettDClient().getAccountBuId(getXapiCaller().isReversal() ? gettXRequest().getDebitAccount() : gettXRequest().getCreditAccount()));

            gettXRequest().setCreditAccount(getXapiCaller().isReversal() ? requestMessage.getAccountNumber1() : getTerminalAccount(gettXRequest().getBuId()));
            gettXRequest().setTxnAmount(requestMessage.getTxnAmount());

            gettXRequest().setDebitAccount(getXapiCaller().isReversal() ? getTerminalAccount(gettXRequest().getBuId()) : requestMessage.getAccountNumber1());

            gettXRequest().setChargeDebitAccount(getXapiCaller().isReversal() ? gettXRequest().getCreditAccount() : gettXRequest().getDebitAccount());
            setTxnCharge(gettXRequest());
            if (requestMessage.hasField(chargeReqField) && Objects.equals(requestMessage.getString(chargeReqField).toUpperCase(), "CHARGE"))
            {
                responseMessage.set(chargeReqField, queryCharge(gettXRequest()).toPlainString());
                tXRequest.setSuccessful(true);
                getXapiCaller().setXapiRespCode(EICodesMOB.XAPI_APPROVED);
            }
            else
            {
                if (isTxnValid(gettXRequest()) && isCardLinked((getXapiCaller().isReversal() ? gettXRequest().getCreditAccount() : gettXRequest().getDebitAccount())) && isTransactionPermited("ALLDR", requestMessage.getAccountNumber1()))
                {

                    Object response = getXapiCaller().isReversal() ? gettXClient().postGLToDepositTransfer(gettXRequest()) : gettXClient().postDepositToGLTransfer(gettXRequest());
                    if (response.toString().contains("OptimisticConcurrencyException") && counter++ < 3)
                    {
                        getXapiCaller().setCall("Retry Count ", counter);
                        processBranchCashWithdrawal(desc);

                    }
                    else
                    {
                        if (response instanceof TxnResponseOutputData)
                        {
                            getXapiCaller().setXapiRespCode(((TxnResponseOutputData) response).getResponseCode());
                            if (EICodesMOB.XAPI_APPROVED.equals(((TxnResponseOutputData) response).getResponseCode()))
                            {
                                setTxnJournalId(gettDClient().extractJournalId(((TxnResponseOutputData) response).getRetrievalReferenceNumber()));
                                if (gettXClient().processCharge(gettXRequest(), false))
                                {
                                    queryAccountBalance(gettXRequest().getDebitAccount(), false);
                                    gettXRequest().setSuccessful(true);
                                }
                                else if (!getXapiCaller().isReversal())
                                {
                                    gettXRequest().setTxnNarration("REV~" + gettXRequest().getTxnNarration());
                                    gettXClient().postGLToDepositTransfer(gettXRequest());
                                }
                            }
                        }
                        else if (response instanceof XAPIException)
                        {
                            getXapiCaller().setXapiRespCode(gettXClient().getXapiErrorCode((XAPIException) response));
                        }
                    }
                }
            }
        }
        catch (Exception ex)
        {
            getXapiCaller().setXapiRespCode(gettXClient().getXapiErrorCode(ex));
            getXapiCaller().logException(ex);
        }
        return gettXRequest();
    }

    private TXRequestMOB processRemittanceWithdrawal(String desc)
    {
        try
        {
            /*ADDED EVALUATION FOR OFFLINE TRASNACTIONS AND APPEND OFFLINE TO THE DESCRIOPTION.. THIS IS FOR EASIER IDENTIFICATION OF THE TXN IN CORE*/
            gettXRequest().setTxnNarration((getXapiCaller().isReversal() ? "REV~" : "") + (getXapiCaller().isOffline() ? "[Offline] " : "") + getXapiCaller().getTxnDescription());
            gettXRequest().setBuId(gettDClient().getAccountBuId(getXapiCaller().isReversal() ? gettXRequest().getDebitAccount() : gettXRequest().getCreditAccount()));

            gettXRequest().setCreditAccount(getXapiCaller().isReversal() ? requestMessage.getAccountNumber1() : PHController.MoneyRemittanceGL);
            gettXRequest().setTxnAmount(requestMessage.getTxnAmount());

            gettXRequest().setDebitAccount(getXapiCaller().isReversal() ? PHController.MoneyRemittanceGL : requestMessage.getAccountNumber1());

            gettXRequest().setChargeDebitAccount(getXapiCaller().isReversal() ? gettXRequest().getCreditAccount() : gettXRequest().getDebitAccount());
            setTxnCharge(gettXRequest());
            if (requestMessage.hasField(chargeReqField) && Objects.equals(requestMessage.getString(chargeReqField).toUpperCase(), "CHARGE"))
            {
                responseMessage.set(chargeReqField, queryCharge(gettXRequest()).toPlainString());
                tXRequest.setSuccessful(true);
                getXapiCaller().setXapiRespCode(EICodesMOB.XAPI_APPROVED);
            }
            else
            {
                if (isTxnValid(gettXRequest()) && isCardLinked((getXapiCaller().isReversal() ? gettXRequest().getCreditAccount() : gettXRequest().getDebitAccount())))
                {
                    Object response = getXapiCaller().isReversal() ? gettXClient().postGLToDepositTransfer(gettXRequest()) : gettXClient().postDepositToGLTransfer(gettXRequest());
                    if (response.toString().contains("OptimisticConcurrencyException") && counter++ < 3)
                    {
                        getXapiCaller().setCall("Retry Count ", counter);
                        processRemittanceWithdrawal(desc);

                    }
                    else
                    {
                        if (response instanceof TxnResponseOutputData)
                        {
                            getXapiCaller().setXapiRespCode(((TxnResponseOutputData) response).getResponseCode());
                            if (EICodesMOB.XAPI_APPROVED.equals(((TxnResponseOutputData) response).getResponseCode()))
                            {
                                setTxnJournalId(gettDClient().extractJournalId(((TxnResponseOutputData) response).getRetrievalReferenceNumber()));
                                if (gettXClient().processCharge(gettXRequest(), false))
                                {
                                    queryAccountBalance(gettXRequest().getDebitAccount(), false);
                                    gettXRequest().setSuccessful(true);
                                }
                                else if (!getXapiCaller().isReversal())
                                {
                                    gettXRequest().setTxnNarration("REV~" + gettXRequest().getTxnNarration());
                                    gettXClient().postGLToDepositTransfer(gettXRequest());
                                }
                            }

                        }
                        else if (response instanceof XAPIException)
                        {
                            getXapiCaller().setXapiRespCode(gettXClient().getXapiErrorCode((XAPIException) response));
                        }
                    }
                }
            }
        }
        catch (Exception ex)
        {
            getXapiCaller().setXapiRespCode(gettXClient().getXapiErrorCode(ex));
            getXapiCaller().logException(ex);
        }
        return gettXRequest();
    }

    private TXRequestMOB processAitimePurchase(String Description, String mnoAccount)
    {
        try
        {
            gettXRequest().setTxnNarration((getXapiCaller().isReversal() ? "REV~" : "") + (getXapiCaller().isOffline() ? "[Offline] " : "") + getXapiCaller().getTxnDescription());
            gettXRequest().setDebitAccount(getXapiCaller().isReversal() ? mnoAccount : requestMessage.getAccountNumber1());

            gettXRequest().setCreditAccount(getXapiCaller().isReversal() ? requestMessage.getAccountNumber1() : mnoAccount);
            gettXRequest().setTxnAmount(requestMessage.getTxnAmount());

            gettXRequest().setBuId(gettDClient().getAccountBuId(getXapiCaller().isReversal() ? gettXRequest().getDebitAccount() : gettXRequest().getCreditAccount()));

            gettXRequest().setChargeDebitAccount(getXapiCaller().isReversal() ? gettXRequest().getCreditAccount() : gettXRequest().getDebitAccount());
            getXapiCaller().setBillerTxn(true);
            setTxnCharge(gettXRequest());
            if (requestMessage.hasField(chargeReqField) && Objects.equals(requestMessage.getString(chargeReqField).toUpperCase(), "CHARGE"))
            {
                responseMessage.set(chargeReqField, queryCharge(gettXRequest()).toPlainString());
                tXRequest.setSuccessful(true);
                getXapiCaller().setXapiRespCode(EICodesMOB.XAPI_APPROVED);
            }
            else
            {
                if (isTxnValid(gettXRequest()) && isCardLinked((getXapiCaller().isReversal() ? gettXRequest().getCreditAccount() : gettXRequest().getDebitAccount())) && isTransactionPermited("ALLDR", getXapiCaller().isReversal() ? gettXRequest().getCreditAccount() : gettXRequest().getDebitAccount()))
                {
                    Object response = getXapiCaller().isReversal() ? gettXClient().postGLToDepositTransfer(gettXRequest()) : gettXClient().postDepositToGLTransfer(gettXRequest());
                    if (response.toString().contains("OptimisticConcurrencyException") && counter++ < 3)
                    {
                        getXapiCaller().setCall("Retry Count ", counter);
                        processAitimePurchase(Description, mnoAccount);

                    }
                    else
                    {
                        if (response instanceof TxnResponseOutputData)
                        {
                            getXapiCaller().setXapiRespCode(((TxnResponseOutputData) response).getResponseCode());
                            if (EICodesMOB.XAPI_APPROVED.equals(((TxnResponseOutputData) response).getResponseCode()))
                            {
                                setTxnJournalId(gettDClient().extractJournalId(((TxnResponseOutputData) response).getRetrievalReferenceNumber()));
                                if (gettXClient().processCharge(gettXRequest(), false))
                                {
                                    queryAccountBalance(gettXRequest().getDebitAccount(), false);
                                    gettXRequest().setSuccessful(true);
                                }
                                else if (!getXapiCaller().isReversal())
                                {
                                    gettXRequest().setTxnNarration("REV~" + gettXRequest().getTxnNarration());
                                    gettXClient().postGLToDepositTransfer(gettXRequest());
                                }
                            }
                        }
                        else if (response instanceof XAPIException)
                        {
                            getXapiCaller().setXapiRespCode(gettXClient().getXapiErrorCode((XAPIException) response));
                        }
                    }
                }
            }
        }
        catch (Exception ex)
        {
            getXapiCaller().setXapiRespCode(gettXClient().getXapiErrorCode(ex));
            getXapiCaller().logException(ex);
        }
        return gettXRequest();
    }

    private TXRequestMOB processBankToWallet(String mnoAccount)
    {
        try
        {
            String BuAccount = getXapiCaller().isReversal() ? requestMessage.getAccountNumber2() : requestMessage.getAccountNumber1();

            gettXRequest().setTxnNarration((getXapiCaller().isReversal() ? "REV~" : "") + (getXapiCaller().isOffline() ? "[Offline] " : "") + getXapiCaller().getTxnDescription());
            gettXRequest().setBuId(gettDClient().getAccountBuId(getXapiCaller().isReversal() ? gettXRequest().getDebitAccount() : gettXRequest().getCreditAccount()));

            gettXRequest().setDebitAccount(getXapiCaller().isReversal() ? gettDClient().unmaskGLAccount(mnoAccount, gettDClient().getAccountBuId(BuAccount)) : requestMessage.getAccountNumber1());
            gettXRequest().setCreditAccount(getXapiCaller().isReversal() ? requestMessage.getAccountNumber1() : gettDClient().unmaskGLAccount(mnoAccount, gettDClient().getAccountBuId(BuAccount)));
            gettXRequest().setTxnAmount(requestMessage.getTxnAmount());
            gettXRequest().setChargeDebitAccount(getXapiCaller().isReversal() ? gettXRequest().getCreditAccount() : gettXRequest().getDebitAccount());
            getXapiCaller().setBillerTxn(true);
            setTxnCharge(gettXRequest());
            if (requestMessage.hasField(chargeReqField) && Objects.equals(requestMessage.getString(chargeReqField).toUpperCase(), "CHARGE"))
            {
                responseMessage.set(chargeReqField, queryCharge(gettXRequest()).toPlainString());
                tXRequest.setSuccessful(true);
                getXapiCaller().setXapiRespCode(EICodesMOB.XAPI_APPROVED);
            }
            else
            {
                if (!isWalletProductAllowed(gettXRequest(), gettXRequest().getDebitAccount()))
                {
                    getXapiCaller().setXapiRespCode(EICodes.ACCOUNT_CANNOT_TRANSACT_1);
                }
                else if (isTxnValid(gettXRequest()) && isCardLinked((getXapiCaller().isReversal() ? gettXRequest().getCreditAccount() : gettXRequest().getDebitAccount())) && isTransactionPermited("ALLDR", (getXapiCaller().isReversal() ? gettXRequest().getCreditAccount() : gettXRequest().getDebitAccount())))
                {
                    Object response = getXapiCaller().isReversal() ? gettXClient().postGLToDepositTransfer(gettXRequest()) : gettXClient().postDepositToGLTransfer(gettXRequest());
                    if (response.toString().contains("OptimisticConcurrencyException") && counter++ < 3)
                    {
                        getXapiCaller().setCall("Retry Count ", counter);
                        processBankToWallet(mnoAccount);

                    }
                    else
                    {
                        if (response instanceof TxnResponseOutputData)
                        {
                            getXapiCaller().setXapiRespCode(((TxnResponseOutputData) response).getResponseCode());
                            if (EICodesMOB.XAPI_APPROVED.equals(((TxnResponseOutputData) response).getResponseCode()))
                            {
                                setTxnJournalId(gettDClient().extractJournalId(((TxnResponseOutputData) response).getRetrievalReferenceNumber()));
                                if (gettXClient().processCharge(gettXRequest(), false))
                                {
                                    queryAccountBalance(gettXRequest().getDebitAccount(), false);
                                    gettXRequest().setSuccessful(true);
                                }
                                else if (!getXapiCaller().isReversal())
                                {
                                    gettXRequest().setTxnNarration("REV~" + gettXRequest().getTxnNarration());
                                    gettXClient().postGLToDepositTransfer(gettXRequest());
                                }
                            }

                        }
                        else if (response instanceof XAPIException)
                        {
                            getXapiCaller().setXapiRespCode(gettXClient().getXapiErrorCode((XAPIException) response));
                        }
                    }
                }
            }
        }
        catch (Exception ex)
        {
            getXapiCaller().setXapiRespCode(gettXClient().getXapiErrorCode(ex));
            getXapiCaller().logException(ex);
        }
        return gettXRequest();
    }

    private TXRequestMOB processWalleToBank(String mnoAccount)
    {
        try
        {
            /*ADDED EVALUATION FOR OFFLINE TRASNACTIONS AND APPEND OFFLINE TO THE DESCRIOPTION.. THIS IS FOR EASIER IDENTIFICATION OF THE TXN IN CORE*/
            String BuAccount = getXapiCaller().isReversal() ? requestMessage.getAccountNumber2() : requestMessage.getAccountNumber1();
            gettXRequest().setTxnNarration((getXapiCaller().isReversal() ? "REV~" : "") + (getXapiCaller().isOffline() ? "[Offline] " : "") + getXapiCaller().getTxnDescription());
            gettXRequest().setDebitAccount(getXapiCaller().isReversal() ? requestMessage.getAccountNumber2() : gettDClient().unmaskGLAccount(mnoAccount, gettDClient().getAccountBuId(BuAccount)));

            gettXRequest().setCreditAccount(getXapiCaller().isReversal() ? gettDClient().unmaskGLAccount(mnoAccount, gettDClient().getAccountBuId(BuAccount)) : requestMessage.getAccountNumber2());
            gettXRequest().setTxnAmount(requestMessage.getTxnAmount());

            gettXRequest().setBuId(gettDClient().getAccountBuId(getXapiCaller().isReversal() ? gettXRequest().getDebitAccount() : gettXRequest().getCreditAccount()));
            getXapiCaller().setBillerTxn(false);
            gettXRequest().setChargeDebitAccount(getXapiCaller().isReversal() ? gettXRequest().getDebitAccount() : gettXRequest().getCreditAccount());
            setTxnCharge(gettXRequest());
            if (requestMessage.hasField(chargeReqField) && Objects.equals(requestMessage.getString(chargeReqField).toUpperCase(), "CHARGE"))
            {
                responseMessage.set(chargeReqField, queryCharge(gettXRequest()).toPlainString());
                tXRequest.setSuccessful(true);
                getXapiCaller().setXapiRespCode(EICodesMOB.XAPI_APPROVED);
            }
            else
            {
                if (!isWalletProductAllowed(gettXRequest(), gettXRequest().getCreditAccount()))
                {
                    getXapiCaller().setXapiRespCode(EICodes.ACCOUNT_CANNOT_TRANSACT_1);
                }
                else if (isCardLinked(getXapiCaller().isReversal() ? gettXRequest().getDebitAccount() : gettXRequest().getCreditAccount()) && isTransactionPermited("ALLCR", getXapiCaller().isReversal() ? gettXRequest().getDebitAccount() : gettXRequest().getCreditAccount()))
                {
                    Object response = getXapiCaller().isReversal() ? gettXClient().postDepositToGLTransfer(gettXRequest()) : gettXClient().postGLToDepositTransfer(gettXRequest());
                    if (response.toString().contains("OptimisticConcurrencyException") && counter++ < 3)
                    {
                        getXapiCaller().setCall("Retry Count ", counter);
                        processWalleToBank(mnoAccount);

                    }
                    else
                    {
                        if (response instanceof TxnResponseOutputData)
                        {
                            getXapiCaller().setXapiRespCode(((TxnResponseOutputData) response).getResponseCode());
                            if (EICodesMOB.XAPI_APPROVED.equals(((TxnResponseOutputData) response).getResponseCode()))
                            {
                                setTxnJournalId(gettDClient().extractJournalId(((TxnResponseOutputData) response).getRetrievalReferenceNumber()));
                                if (gettXClient().processCharge(gettXRequest(), false))
                                {
                                    queryAccountBalance(gettXRequest().getDebitAccount(), false);
                                    gettXRequest().setSuccessful(true);
                                }
                                else if (!getXapiCaller().isReversal())
                                {
                                    gettXRequest().setTxnNarration("REV~" + gettXRequest().getTxnNarration());
                                    gettXClient().postDepositToGLTransfer(gettXRequest());
                                }
                            }
                        }
                        else if (response instanceof XAPIException)
                        {
                            getXapiCaller().setXapiRespCode(gettXClient().getXapiErrorCode((XAPIException) response));
                        }
                    }
                }
            }
        }
        catch (Exception ex)
        {
            getXapiCaller().setXapiRespCode(gettXClient().getXapiErrorCode(ex));
            getXapiCaller().logException(ex);
        }
        return gettXRequest();
    }

    private TXRequestMOB processUtilityBills(String UTL, String account)
    {
        String txnDesc = splitItem(UTL, 1);
        try
        {
            /*ADDED EVALUATION FOR OFFLINE TRASNACTIONS AND APPEND OFFLINE TO THE DESCRIOPTION.. THIS IS FOR EASIER IDENTIFICATION OF THE TXN IN CORE*/
            gettXRequest().setTxnNarration((getXapiCaller().isReversal() ? "REV~" : "") + (getXapiCaller().isOffline() ? "[Offline] " : "") + UTL + " " + getXapiCaller().getTxnDescription());
            gettXRequest().setDebitAccount(getXapiCaller().isReversal() ? account : requestMessage.getAccountNumber1());

            gettXRequest().setCreditAccount(getXapiCaller().isReversal() ? requestMessage.getAccountNumber1() : account);
            gettXRequest().setTxnAmount(requestMessage.getTxnAmount());

            getXapiCaller().setBillerTxn(true);
            gettXRequest().setBuId(gettDClient().getAccountBuId(getXapiCaller().isReversal() ? gettXRequest().getCreditAccount() : gettXRequest().getDebitAccount()));

            gettXRequest().setChargeDebitAccount(getXapiCaller().isReversal() ? gettXRequest().getCreditAccount() : gettXRequest().getDebitAccount());
            setTxnCharge(gettXRequest());
            if (requestMessage.hasField(chargeReqField) && Objects.equals(requestMessage.getString(chargeReqField).toUpperCase(), "CHARGE"))
            {
                responseMessage.set(chargeReqField, queryCharge(gettXRequest()).toPlainString());
                tXRequest.setSuccessful(true);
                getXapiCaller().setXapiRespCode(EICodesMOB.XAPI_APPROVED);
            }
            else
            {
                if (isTxnValid(gettXRequest()) && isCardLinked(getXapiCaller().isReversal() ? gettXRequest().getCreditAccount() : gettXRequest().getDebitAccount()) && isTransactionPermited("ALLDR", requestMessage.getAccountNumber1()))
                {
                    Object response = getXapiCaller().isReversal() ? gettXClient().postGLToDepositTransfer(gettXRequest()) : gettXClient().postDepositToGLTransfer(gettXRequest());
                    if (response.toString().contains("OptimisticConcurrencyException") && counter++ < 3)
                    {
                        getXapiCaller().setCall("Retry Count ", counter);
                        processUtilityBills(UTL, account);

                    }
                    else
                    {
                        if (response instanceof TxnResponseOutputData)
                        {
                            getXapiCaller().setXapiRespCode(((TxnResponseOutputData) response).getResponseCode());
                            if (EICodesMOB.XAPI_APPROVED.equals(((TxnResponseOutputData) response).getResponseCode()))
                            {
                                setTxnJournalId(gettDClient().extractJournalId(((TxnResponseOutputData) response).getRetrievalReferenceNumber()));
                                if (gettXClient().processCharge(gettXRequest(), true))
                                {
                                    queryAccountBalance(gettXRequest().getDebitAccount(), false);
                                    gettXRequest().setSuccessful(true);
                                }
                                else if (!getXapiCaller().isReversal())
                                {
                                    //   swapAccounts(tXRequest);
                                    gettXRequest().setTxnNarration("REV~" + gettXRequest().getTxnNarration());
                                    gettXClient().postGLToDepositTransfer(gettXRequest());
                                }
                            }
                        }
                        else if (response instanceof XAPIException)
                        {
                            getXapiCaller().setXapiRespCode(gettXClient().getXapiErrorCode((XAPIException) response));
                        }
                    }
                }
            }
        }
        catch (Exception ex)
        {
            getXapiCaller().setXapiRespCode(gettXClient().getXapiErrorCode(ex));
            getXapiCaller().logException(ex);
        }
        return gettXRequest();
    }

    private TXRequestMOB processBranchCashDeposit(String desc)
    {
        try
        {
            gettXRequest().setTxnNarration((getXapiCaller().isReversal() ? "REV~" : "") + (getXapiCaller().isOffline() ? "[Offline] " : "") + getXapiCaller().getTxnDescription());
            gettXRequest().setBuId(gettDClient().getAccountBuId(getXapiCaller().isReversal() ? tXRequest.getCreditAccount() : tXRequest.getDebitAccount()));

            gettXRequest().setDebitAccount(getXapiCaller().isReversal() ? requestMessage.getAccountNumber1() : getTerminalAccount(gettXRequest().getBuId()));

            gettXRequest().setCreditAccount(getXapiCaller().isReversal() ? getTerminalAccount(gettXRequest().getBuId()) : requestMessage.getAccountNumber1());
            gettXRequest().setTxnAmount(requestMessage.getTxnAmount());

            gettXRequest().setChargeDebitAccount(getXapiCaller().isReversal() ? gettXRequest().getDebitAccount() : gettXRequest().getCreditAccount());
            setTxnCharge(gettXRequest());
            if (requestMessage.hasField(chargeReqField) && Objects.equals(requestMessage.getString(chargeReqField).toUpperCase(), "CHARGE"))
            {
                responseMessage.set(chargeReqField, queryCharge(gettXRequest()).toPlainString());
                tXRequest.setSuccessful(true);
                getXapiCaller().setXapiRespCode(EICodesMOB.XAPI_APPROVED);
            }
            else
            {
                Object response = getXapiCaller().isReversal() ? gettXClient().postDepositToGLTransfer(gettXRequest()) : gettXClient().postGLToDepositTransfer(gettXRequest());
                if (response.toString().contains("OptimisticConcurrencyException") && counter++ < 3)
                {
                    getXapiCaller().setCall("Retry Count ", counter);
                    processBranchCashDeposit(desc);
                }
                else
                {
                    if (response instanceof TxnResponseOutputData)
                    {
                        getXapiCaller().setXapiRespCode(((TxnResponseOutputData) response).getResponseCode());
                        if (EICodesMOB.XAPI_APPROVED.equals(((TxnResponseOutputData) response).getResponseCode()))
                        {
                            setTxnJournalId(gettDClient().extractJournalId(((TxnResponseOutputData) response).getRetrievalReferenceNumber()));
                            if (gettXClient().processCharge(gettXRequest(), false))
                            {
                                queryAccountBalance(gettXRequest().getCreditAccount(), false);

                                gettXRequest().setSuccessful(true);
                            }
                            else if (!getXapiCaller().isReversal())
                            {
                                gettXRequest().setTxnNarration("REV~" + gettXRequest().getTxnNarration());
                                gettXClient().postGLToDepositTransfer(gettXRequest());
                            }
                        }
                    }
                    else if (response instanceof XAPIException)
                    {
                        getXapiCaller().setXapiRespCode(gettXClient().getXapiErrorCode((XAPIException) response));
                    }
                }
            }
        }
        catch (Exception ex)
        {
            getXapiCaller().setXapiRespCode(gettXClient().getXapiErrorCode(ex));
            getXapiCaller().logException(ex);
        }

        return gettXRequest();
    }

    private TXRequestMOB processRemittanceDeposit(String desc)
    {
        try
        {
            gettXRequest().setTxnNarration((getXapiCaller().isReversal() ? "REV~" : "") + (getXapiCaller().isOffline() ? "[Offline] " : "") + getXapiCaller().getTxnDescription());
            gettXRequest().setBuId(gettDClient().getAccountBuId(getXapiCaller().isReversal() ? tXRequest.getCreditAccount() : tXRequest.getDebitAccount()));

            gettXRequest().setDebitAccount(getXapiCaller().isReversal() ? requestMessage.getAccountNumber1() : PHController.MoneyRemittanceGL);

            gettXRequest().setCreditAccount(getXapiCaller().isReversal() ? PHController.MoneyRemittanceGL : requestMessage.getAccountNumber1());
            gettXRequest().setTxnAmount(requestMessage.getTxnAmount());

            gettXRequest().setChargeDebitAccount(getXapiCaller().isReversal() ? gettXRequest().getDebitAccount() : gettXRequest().getCreditAccount());
            setTxnCharge(gettXRequest());
            if (requestMessage.hasField(chargeReqField) && Objects.equals(requestMessage.getString(chargeReqField).toUpperCase(), "CHARGE"))
            {
                responseMessage.set(chargeReqField, queryCharge(gettXRequest()).toPlainString());
                tXRequest.setSuccessful(true);
                getXapiCaller().setXapiRespCode(EICodesMOB.XAPI_APPROVED);
            }
            else
            {
                Object response = getXapiCaller().isReversal() ? gettXClient().postDepositToGLTransfer(gettXRequest()) : gettXClient().postGLToDepositTransfer(gettXRequest());
                if (response.toString().contains("OptimisticConcurrencyException") && counter++ < 3)
                {
                    getXapiCaller().setCall("Retry Count ", counter);
                    processRemittanceDeposit(desc);

                }
                else
                {
                    if (response instanceof TxnResponseOutputData)
                    {
                        getXapiCaller().setXapiRespCode(((TxnResponseOutputData) response).getResponseCode());
                        if (EICodesMOB.XAPI_APPROVED.equals(((TxnResponseOutputData) response).getResponseCode()))
                        {
                            setTxnJournalId(gettDClient().extractJournalId(((TxnResponseOutputData) response).getRetrievalReferenceNumber()));
                            if (gettXClient().processCharge(gettXRequest(), false))
                            {
                                queryAccountBalance(gettXRequest().getCreditAccount(), false);

                                gettXRequest().setSuccessful(true);
                            }
                            else if (!getXapiCaller().isReversal())
                            {
                                gettXRequest().setTxnNarration("REV~" + gettXRequest().getTxnNarration());
                                gettXClient().postGLToDepositTransfer(gettXRequest());
                            }
                        }
                    }
                    else if (response instanceof XAPIException)
                    {
                        getXapiCaller().setXapiRespCode(gettXClient().getXapiErrorCode((XAPIException) response));
                    }
                }
            }
        }
        catch (Exception ex)
        {
            getXapiCaller().setXapiRespCode(gettXClient().getXapiErrorCode(ex));
            getXapiCaller().logException(ex);
        }

        return gettXRequest();
    }

    private void processDepositToDepositTransfer(TXRequestMOB tXRequest, String balanceAccount)
    {

        Object response = gettXClient().postDpToDpFundsTransfer(tXRequest);
        if (response.toString().contains("OptimisticConcurrencyException") && counter++ < 3)
        {
            getXapiCaller().setCall("Retry Count ", counter);
            processDepositToDepositTransfer(tXRequest, balanceAccount);

        }
        else
        {
            if (response instanceof FundsTransferOutputData)
            {
                setTxnJournalId(((FundsTransferOutputData) response).getRetrievalReferenceNumber());
                getXapiCaller().setXapiRespCode(((FundsTransferOutputData) response).getResponseCode());

                if (EICodesMOB.XAPI_APPROVED.equals(((FundsTransferOutputData) response).getResponseCode()))
                {
                    if (gettXClient().processCharge(tXRequest, false))
                    {
                        queryAccountBalance(balanceAccount, false);
                        tXRequest.setSuccessful(true);
                    }
                    else if (!getXapiCaller().isReversal())
                    {
                        swapAccounts(tXRequest);
                        tXRequest.setTxnNarration("REV~" + tXRequest.getTxnNarration());
                        gettXClient().postDpToDpFundsTransfer(tXRequest);
                    }
                }

            }
            else if (response instanceof XAPIException)
            {
                getXapiCaller().setXapiRespCode(gettXClient().getXapiErrorCode((XAPIException) response));
            }
        }
    }

    private BigDecimal queryCharge(TXRequestMOB tXRequest)
    {
        return gettXClient().txnCharge(tXRequest);
    }

    public byte[] getFileBytes(String fileUrl)
    {
        File file = new File(fileUrl);
        byte[] fileBytes = new byte[(int) file.length()];
        try ( FileInputStream fin = new FileInputStream(file))
        {
            fin.read(fileBytes);
        }
        catch (Exception ex)
        {
            getXapiCaller().logException(ex);
        }
        return fileBytes;
    }
//added

    public byte[] getByteArray(String hexStr)
    {
        int len = hexStr.length();
        byte[] data = new byte[len / 2];

        for (int i = 0; i < len; i += 2)
        {
            data[i / 2] = (byte) ((Character.digit(hexStr.charAt(i), 16) << 4)
                    + Character.digit(hexStr.charAt(i + 1), 16));
        }

        return data;
    }

    private void logTransaction(TXRequestMOB tXRequest)
    {
        try
        {
            gettDClient().getLogStatement().setString(1, tXRequest.getReference());
            gettDClient().getLogStatement().setString(2, requestMessage.getProcessingCode());

            gettDClient().getLogStatement().setString(3, requestMessage.getTerminalID());
            gettDClient().getLogStatement().setString(4, requestMessage.getMerchantID());

            gettDClient().getLogStatement().setString(5, requestMessage.getAcquirerBIN());
            gettDClient().getLogStatement().setString(6, requestMessage.getAccessCode());

            gettDClient().getLogStatement().setString(7, tXRequest.getDebitAccount());
            gettDClient().getLogStatement().setString(8, tXRequest.getCreditAccount());

            gettDClient().getLogStatement().setString(9, tXRequest.getCurrencyCode());
            gettDClient().getLogStatement().setBigDecimal(10, tXRequest.getTxnAmount());

            gettDClient().getLogStatement().setBigDecimal(11, tXRequest.getChargeAmount());
            gettDClient().getLogStatement().setString(12, tXRequest.getTxnNarration());

            gettDClient().getLogStatement().setString(13, getXapiCaller().isOffline() ? "Y" : "N");
            gettDClient().getLogStatement().setString(14, getXapiCaller().getXapiRespCode());

            gettDClient().getLogStatement().setString(15, PHController.getXapiMessage(getXapiCaller().getXapiRespCode()));
            gettDClient().getLogStatement().setString(16, EICodesMOB.XAPI_APPROVED.equals(getXapiCaller().getXapiRespCode()) ? "APPROVED" : "REJECTED");

            gettDClient().getLogStatement().setString(17, PHController.mapToIsoCode(getXapiCaller().getXapiRespCode()));
            gettDClient().getLogStatement().setLong(18, PHController.mobChannelID);

            gettDClient().getLogStatement().setString(19, getXapiCaller().isReversal() ? "Y" : "N");
            gettDClient().getLogStatement().setString(20, getTxnJournalId());

            gettDClient().getLogStatement().setString(21, getChargeJournalId());
            gettDClient().getLogStatement().setString(22, gettXRequest().getProcessingCode());
            gettDClient().getLogStatement().setString(23, tXRequest.getStan());
            gettDClient().getLogStatement().execute();
        }
        catch (Exception ex)
        {
            getXapiCaller().logException(ex);
        }
    }

    private void setTxnCharge(TXRequestMOB tXRequest)
    {
        PHMain.mobBridgeLog.debug("this is the proc code " + requestMessage.getProcessingCode());
        PHMain.mobBridgeLog.debug("this is the biller code " + requestMessage.getProcessingCode() + (getXapiCaller().isBillerTxn() ? geteIAsocBillerCode().getBillerCode().substring(2, 3) : "M"));

        // System.err.println("here for biller code " + requestMessage.getProcessingCode() + (getXapiCaller().isBillerTxn() ? geteIAsocBillerCode().getBillerCode().substring(2, 3) : "M"));
        tXRequest.setBillerCode(requestMessage.getProcessingCode() + (getXapiCaller().isBillerTxn() ? geteIAsocBillerCode().getBillerCode().substring(2, 3) : "M"));

        EIChargeMOB eICharge = PHController.getMobCharge(requestMessage.getProcessingCode() + (getXapiCaller().isBillerTxn() ? geteIAsocBillerCode().getBillerCode().substring(2, 3) : "M"));
        CNAccountMOB chargeAccount = gettDClient().queryDepositAccount(tXRequest.getChargeDebitAccount());

        if (eICharge != null && chargeAccount != null)
        {
            tXRequest.setChargeNarration(eICharge.getDescription());
            /*Long buId = gettDClient().queryBusinessUnit(geteITerminal().getBuCode()).getBuId();*/
            Long buId = chargeAccount.getBuId();

            if (buId == 0)
            {
                buId = gettDClient().queryBusinessUnit(geteITerminal().getBuCode()).getBuId();
                /*buId = chargeAccount.getBuId();*/
            }

            tXRequest.setChargeCreditLedger(gettDClient().unmaskGLAccount(eICharge.getChargeLedger(), buId));
            tXRequest.setTaxCreditLedger(gettDClient().unmaskGLAccount(eICharge.getTaxLedger(), buId));

            tXRequest.setChannelContraLedger(gettDClient().getChannelContraGL(PHController.mobChannelID, buId));
            tXRequest.setTaxNarration(eICharge.getTaxName());

            tXRequest.setThirdPartyIncomeLedger(gettDClient().unmaskGLAccount(eICharge.getTpLedger(), buId));
            tXRequest.setSharePercentage(eICharge.getTpSharePercentage());

            final String chargeValueKey = chargeAccount.getCurrency().getCurrencyCode();
            TCValueMOB tCValue = eICharge.getValues().containsKey(chargeValueKey) ? eICharge.getValues().get(chargeValueKey) : new TCValueMOB();

            switch (tCValue.getChargeType())
            {
                case "Constant":
                    tXRequest.setChargeAmount(tCValue.getChargeValue());
                    break;
                case "Percentage":
                    final BigDecimal calculatedCharge = tXRequest.getTxnAmount().multiply(tCValue.getChargeValue()).divide(new BigDecimal(100));
                    tXRequest.setChargeAmount(calculatedCharge.compareTo(tCValue.getMaxAmount()) > 0 ? tCValue.getMaxAmount()
                            : (calculatedCharge.compareTo(tCValue.getMinAmount()) < 0 ? tCValue.getMinAmount()
                            : calculatedCharge));
                    break;
                case "Tiered":
                    Object[] tiers = tCValue.getTiers().values().toArray();
                    if (tiers.length > 0)
                    {
                        Arrays.sort(tiers);
                        tXRequest.setChargeAmount(((TCTierMOB) tiers[tiers.length - 1]).getChargeAmount());
                        for (int i = tiers.length - 1; i >= 0; i--)
                        {
                            if (tXRequest.getTxnAmount().compareTo(((TCTierMOB) tiers[i]).getTierCeiling()) <= 0)
                            {
                                tXRequest.setChargeAmount(((TCTierMOB) tiers[i]).getChargeAmount());
                            }
                        }
                    }
                    break;
            }

            Object[] waivers = eICharge.getWaivers().values().toArray();
            if (waivers.length > 0)
            {
                Arrays.sort(waivers);
                for (int i = waivers.length - 1; i >= 0; i--)
                {
                    TCWaiverMOB waiver = (TCWaiverMOB) waivers[i];
                    CNAccountMOB beneficiaryAccount = gettDClient().queryDepositAccount(tXRequest.getCreditAccount());
                    if (0 == waiver.getProductId() || waiver.getProductId() == chargeAccount.getProductId() || waiver.getProductId() == beneficiaryAccount.getProductId())
                    {
                        if (0 == waiver.getProductId() || "A".equals(waiver.getMatchAccount()))
                        {
                            if (applyWaiver(waiver))
                            {
                                tXRequest.setChargeAmount((tXRequest.getChargeAmount().multiply((new BigDecimal(100).subtract(waiver.getWaivedPercentage())).divide(new BigDecimal(100)))).setScale(2, RoundingMode.DOWN));
                                break;
                            }
                        }
                        else if ("B".equals(waiver.getMatchAccount()) && chargeAccount.getProductId() == beneficiaryAccount.getProductId())
                        {
                            if (applyWaiver(waiver))
                            {
                                tXRequest.setChargeAmount((tXRequest.getChargeAmount().multiply((new BigDecimal(100).subtract(waiver.getWaivedPercentage())).divide(new BigDecimal(100)))).setScale(2, RoundingMode.DOWN));
                                break;
                            }
                        }
                        else if ("C".equals(waiver.getMatchAccount()) && chargeAccount.getProductId() == waiver.getProductId())
                        {
                            if (applyWaiver(waiver))
                            {
                                tXRequest.setChargeAmount((tXRequest.getChargeAmount().multiply((new BigDecimal(100).subtract(waiver.getWaivedPercentage())).divide(new BigDecimal(100)))).setScale(2, RoundingMode.DOWN));
                                break;
                            }
                        }
                    }
                }
            }
            tXRequest.setTaxAmount(tXRequest.getChargeAmount().multiply(eICharge.getTaxPercentage().divide(new BigDecimal(100))));
        }
        getXapiCaller().setCall("txrequest", tXRequest);
    }

    private boolean applyWaiver(TCWaiverMOB waiver)
    {
        switch (waiver.getWaiverCondition())
        {
            case "ALL":
                return true;
            case "MVL":
                return getuCActivity().getVelocity().compareTo(waiver.getThresholdValue()) < 0;
            case "MVM":
                return getuCActivity().getVelocity().compareTo(waiver.getThresholdValue()) > 0;
            case "MOL":
                return getuCActivity().getVolume().compareTo(waiver.getThresholdValue()) < 0;
            case "MOM":
                return getuCActivity().getVolume().compareTo(waiver.getThresholdValue()) > 0;
            default:
                return false;
        }
    }

    private void checkDuplicate()
    {
        for (int counter = 0; (counter < 30 && PHController.activeTransactions.contains(gettXRequest().getReference())); counter++)
        {
            try
            {
                Thread.sleep(500);
            }
            catch (Exception ex)
            {
                PHController.logMobError(ex);
            }
        }
    }

    private void sendResponse()
    {
        try
        {

            if (!EICodesMOB.SIMILAR_TXN_IN_PROGRESS.equals(getXapiCaller().getXapiRespCode()))
            {
                PHController.activeTransactions.remove(tXRequest.getReference());
            }
            getXapiCaller().setIsoRespCode(PHController.mapToIsoCode(getXapiCaller().getXapiRespCode()));
            if (!responseMessage.hasField(54))
            {
                responseMessage.set(new ISOField(54, requestMessage.getAccountType1() + "01" + requestMessage.getTxnCurrency() + "D" + "0000000000" + "00" + requestMessage.getAccountType1() + "02" + requestMessage.getTxnCurrency() + "D" + "0000000000" + "00"));

            }
            responseMessage.set(new ISOField(39, getXapiCaller().getIsoRespCode()));
        }
        catch (Exception ex)
        {
            getXapiCaller().logException(ex);
        }
        try
        {
            getXapiCaller().setDuration(String.valueOf(System.currentTimeMillis() - startTime) + " Ms");
            messageSource.send(responseMessage);
        }
        catch (Exception ex)
        {
            getXapiCaller().logException(ex);
        }

        try
        {
            if (!"0800".equals(requestMessage.getMTI()) && !"0801".equals(requestMessage.getMTI()) && !"0320".equals(requestMessage.getMTI()) && !"0321".equals(requestMessage.getMTI()))
            {
                logEvent.addMessage(requestMessage);
                logEvent.addMessage(getXapiCaller());
                logEvent.addMessage(responseMessage);
                Logger.log(logEvent);
            }
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }

        if (PHMain.phFrame != null)
        {
            SwingUtilities.invokeLater(()
                    ->
            {
                PHMain.phFrame.insertMobTxnToTree(responseMessage);
            });
        }

        logEvent.clear();
        setXapiCaller(null);

        PHController.releaseMobProcessor(this);
    }

    private boolean isBalanceSufficient(String accountNumber, BigDecimal debitAmount)
    {
        try
        {
            gettXRequest().setDebitAccount(accountNumber);
            Object response = gettXClient().queryDepositAccountBalance(gettXRequest());
            if (response instanceof AccountBalanceOutputData)
            {
                if (((AccountBalanceOutputData) response).getAvailableBalance().compareTo(debitAmount) >= 0)
                {
                    return true;
                }
                getXapiCaller().setXapiRespCode(EICodesMOB.INSUFFICIENT_FUNDS_1);
            }
            else if (response instanceof XAPIException)
            {
                getXapiCaller().setXapiRespCode(gettXClient().getXapiErrorCode((XAPIException) response));
            }
        }
        catch (Exception ex)
        {
            getXapiCaller().setXapiRespCode(gettXClient().getXapiErrorCode(ex));
            getXapiCaller().logException(ex);
        }
        return false;
    }

    private boolean isTransactionPermited(String bloclSt, String AcctNo)
    {
        if (gettDClient().blockedDepositAccount(AcctNo, bloclSt))
        {
            getXapiCaller().setXapiRespCode(EICodes.TRANSACTION_BLOCKED);
            return false;
        }
        else
        {
            return true;
        }
    }

    private boolean isTfrPermited()
    {
        if (gettDClient().blockedDepositAccount(requestMessage.getAccountNumber1(), "ALLDR") || gettDClient().blockedDepositAccount(requestMessage.getAccountNumber2(), "ALLCR"))
        {
            getXapiCaller().setXapiRespCode(EICodes.TRANSACTION_BLOCKED);
        }
        else
        {
            return true;
        }
        return false;
    }

//     private boolean activatedUnfunded(String tfrAcct)
//    {
//        return gettDClient().acctStatusUnfunded(tfrAcct) ? gettDClient().activateUnfundedAccount(tfrAcct) : true;
//    }
    private boolean isRequestValid()
    {
        checkDuplicate();
        if (geteITerminal() == null)
        {
            getXapiCaller().setXapiRespCode(EICodesMOB.TRANSACTION_NOT_PERMITTED_ON_TERMINAL);
        }
        else if (PHController.activeTransactions.contains(tXRequest.getReference()))
        {
            getXapiCaller().setXapiRespCode(EICodesMOB.SIMILAR_TXN_IN_PROGRESS);
        }
        else if (!getXapiCaller().isReversal() && gettDClient().transactionExists(requestMessage.getTxnReference()))
        {
            getXapiCaller().setXapiRespCode(EICodesMOB.DUPLICATE_REFERENCE);
        }
        else if (getXapiCaller().isReversal() && !tDClient.transactionExists(requestMessage.getOriginalTxnReference()))
        {
            getXapiCaller().setXapiRespCode(EICodesMOB.MISSING_ORIGINAL_TXN_REFERENCE);
        }
        else if (getXapiCaller().isReversal() && tDClient.reversalTransactionExists(requestMessage.getOriginalTxnReference()))
        {
            getXapiCaller().setXapiRespCode(EICodesMOB.DUPLICATE_REFERENCE);
        }
        else if ((requestMessage.getAccountNumber1().equals(requestMessage.getAccountNumber2()) && requestMessage.getProcessingCode().equals("40")))
        {
            getXapiCaller().setXapiRespCode(EICodes.TRANSFER_TO_SAME_ACCOUNT);
        }
        else
        {
            PHController.activeTransactions.add(tXRequest.getReference());
            return true;
        }
        return false;
    }

//    private boolean validateFields(boolean checkAccount1, boolean checkAccount2, boolean checkAmount)
//    {
//        boolean returnCode = false;
//        if (checkAccount1)
//        {
//            String recSt = gettDClient().queryDepositAccount(requestMessage.getAccountNumber1()).getAcctStatus();
//            switch (recSt)
//            {
//                case "N/A":
//                    getXapiCaller().setXapiRespCode(EICodes.INVALID_ACCOUNT);
//                    returnCode= false;
//                case "D":
//                    getXapiCaller().setXapiRespCode(EICodes.DORMANT_ACCOUNT);
//                    returnCode= false;
//                case "L":
//                case "C":
//                    getXapiCaller().setXapiRespCode(EICodes.CLOSED_ACCOUNT);
//                    returnCode= false;
//                case "A":
//                    returnCode= true;
//                default:
//                    getXapiCaller().setXapiRespCode(EICodes.INVALID_ACCOUNT);
//                    returnCode= false;
//            }
//        }
//        if (checkAccount2)
//        {
//            String recSt = gettDClient().queryDepositAccount(requestMessage.getAccountNumber2()).getAcctStatus();
//            System.err.println("status of " + requestMessage.getAccountNumber2() + " is " + recSt);
//            switch (recSt)
//            {
//                case "N/A":
//                    getXapiCaller().setXapiRespCode(EICodes.INVALID_ACCOUNT_STATUS);
//                    returnCode= false;
//                case "D":
//                    getXapiCaller().setXapiRespCode(EICodes.DORMANT_ACCOUNT);
//                    returnCode= false;
//                case "L":
//                case "C":
//                    getXapiCaller().setXapiRespCode(EICodes.CLOSED_ACCOUNT);
//                    returnCode= false;
//                case "A":
//                    returnCode= true;
//                default:
//                    getXapiCaller().setXapiRespCode(EICodes.INVALID_ACCOUNT);
//                    returnCode= false;
//            }
//        }
//        if (checkAmount ? requestMessage.getTxnAmount() == null || BigDecimal.ZERO.compareTo(requestMessage.getTxnAmount()) >= 0 : false)
//        {
//            getXapiCaller().setXapiRespCode(EICodesMOB.INVALID_TXN_AMOUNT);
//            returnCode= false;
//        }
//        System.err.println("out for validation "+getXapiCaller().getXapiRespCode());
//        return returnCode;
//    }
    private boolean validateFields(boolean checkAccount1, boolean checkAccount2, boolean checkAmount)
    {
        boolean returnCode = false;
        //  if (checkAccount1 ? gettDClient().queryDepositAccount(requestMessage.getAccountNumber1()).getAccountNumber() == null : false)
        if (checkAccount1)
        {
            if (gettDClient().queryDepositAccount(requestMessage.getAccountNumber1()).getAccountNumber() == null)
            {
                getXapiCaller().setXapiRespCode(EICodesMOB.INVALID_ACCOUNT);
                returnCode = false;
            }
            else
            {
                String recSt = gettDClient().queryDepositAccount(requestMessage.getAccountNumber1()).getAcctStatus();
                if (recSt == null || recSt.isEmpty())
                {
                    getXapiCaller().setXapiRespCode(EICodes.INVALID_ACCOUNT);
                    returnCode = false;
                }
                else if (recSt.equals("D"))
                {
                    getXapiCaller().setXapiRespCode(EICodes.DORMANT_ACCOUNT);
                    returnCode = false;
                }
                else if (recSt.equals("L") || recSt.equals("C"))
                {
                    getXapiCaller().setXapiRespCode(EICodes.CLOSED_ACCOUNT);
                    returnCode = false;
                }
                else if (recSt.equals("U"))
                {
                    getXapiCaller().setXapiRespCode(EICodes.INVALID_ACCOUNT);
                    returnCode = false;
                }
                else if (recSt.equals("A"))
                {
                    returnCode = true;
                }
                else
                {
                    getXapiCaller().setXapiRespCode(EICodes.INVALID_ACCOUNT);
                    returnCode = false;
                }

            }
        }
        //  if (checkAccount2 ? gettDClient().queryDepositAccount(requestMessage.getAccountNumber2()).getAccountNumber() == null : false)
        if (checkAccount2)
        {
            if (gettDClient().queryDepositAccount(requestMessage.getAccountNumber2()).getAccountNumber() == null)
            {
                getXapiCaller().setXapiRespCode(EICodes.INVALID_ACCOUNT);
                returnCode = false;
            }
            else
            {
                String recSt = gettDClient().queryDepositAccount(requestMessage.getAccountNumber2()).getAcctStatus();
                if (recSt == null || recSt.isEmpty())
                {
                    getXapiCaller().setXapiRespCode(EICodes.INVALID_ACCOUNT);
                    returnCode = false;
                }
                else if (recSt.equals("D"))
                {
                    getXapiCaller().setXapiRespCode(EICodes.DORMANT_ACCOUNT);
                    returnCode = false;
                }
                else if (recSt.equals("L") || recSt.equals("C"))
                {
                    getXapiCaller().setXapiRespCode(EICodes.CLOSED_ACCOUNT);
                    returnCode = false;
                }
                else if (recSt.equals("A") || recSt.equals("U"))
                {
                    returnCode = true;
                }
                else
                {
                    getXapiCaller().setXapiRespCode(EICodes.INVALID_ACCOUNT);
                    returnCode = false;
                }
            }

        }
        if (checkAmount ? requestMessage.getTxnAmount() == null || BigDecimal.ZERO.compareTo(requestMessage.getTxnAmount()) >= 0 : false)
        {
            getXapiCaller().setXapiRespCode(EICodes.INVALID_TXN_AMOUNT);
            return false;
        }
        return returnCode;
    }

    private boolean isWithinDailyLimit(String limitType)
    {
        BigDecimal dailyDepositLimit = PHController.getDecimalSetting("DailyDepositLimit" + PHController.getCurrency(requestMessage.getTxnCurrency()));
        BigDecimal dailyTransferLimit = PHController.getDecimalSetting("DailyTransferLimit" + PHController.getCurrency(requestMessage.getTxnCurrency()));
        BigDecimal dailyWithdrawalLimit = PHController.getDecimalSetting("DailyWithdrawalLimit" + PHController.getCurrency(requestMessage.getTxnCurrency()));

        if ("CR".equals(limitType) && BigDecimal.ZERO != dailyDepositLimit && getuCActivity().getVolume().add(requestMessage.getTxnAmount()).compareTo(dailyDepositLimit) > 0)
        {
            getXapiCaller().setXapiRespCode(EICodesMOB.DAILY_TXN_AMOUNT_LIMIT_EXCEEDED);
            return false;
        }
        if ("TR".equals(limitType) && BigDecimal.ZERO != dailyTransferLimit && getuCActivity().getVolume().add(requestMessage.getTxnAmount()).compareTo(dailyTransferLimit) > 0)
        {
            getXapiCaller().setXapiRespCode(EICodesMOB.DAILY_TXN_AMOUNT_LIMIT_EXCEEDED);
            return false;
        }
        if ("DR".equals(limitType) && BigDecimal.ZERO != dailyWithdrawalLimit && getuCActivity().getVolume().add(requestMessage.getTxnAmount()).compareTo(dailyWithdrawalLimit) > 0)
        {
            getXapiCaller().setXapiRespCode(EICodesMOB.DAILY_TXN_AMOUNT_LIMIT_EXCEEDED);
            return false;
        }
        return true;
    }

    private boolean isCardLinked(String accountNumber)
    {

        boolean isLinked = gettDClient().isAccountEnrolled(gettDClient().queryCNUser(requestMessage.getAccessCode()).getCustId(), gettDClient().queryDepositAccount(accountNumber).getAcctId());
        if (!isLinked)
        {
            getXapiCaller().setXapiRespCode(EICodesMOB.INVALID_CARD_ACCOUNT);
        }
        return isLinked;
    }

    private boolean checkIfGroupDeposit(String accountNumber)
    {
        /*ADDED TO CATER FOR THE GROUP DEPOSIT EXPLICITLY DEFINED. 
         THIS DEPOSITS SHOULD BE APPROVED WITHOUT VALIDATION [INHERITTED FROM ORBIT-E]
         REFER TO THE ABOVE 'ISCARDLINKED METHOD IF CONDITION' */
        return requestMessage.getProcessingCode().equals("21") && (gettDClient().getAccountProductId(accountNumber) == 140); //140 = REPAYMENT GROUP PRODUCT
    }

    public void swapAccounts(TXRequestMOB tXRequest)
    {
        String accountNumber = tXRequest.getCreditAccount();
        tXRequest.setCreditAccount(tXRequest.getDebitAccount());
        tXRequest.setDebitAccount(accountNumber);
    }

    private boolean isTxnValid(TXRequestMOB tXRequest)
    {
        return getXapiCaller().isReversal() || (gettDClient().isAccountAllowed(tXRequest.getDebitAccount()) && (!getXapiCaller().isReversal() ? isBalanceSufficient(tXRequest.getDebitAccount(), tXRequest.getTxnAmount().add(tXRequest.getChargeAmount())) : true));
    }

    private boolean isWalletProductAllowed(TXRequestMOB tXRequest, String acctNo)
    {
        return gettDClient().isWalletProductAllowed(acctNo);
    }

    private String getTerminalAccount(Long buId)
    {
        return tDClient.getChannelContraGL(PHController.mobChannelID, buId);
        // return geteITerminal().getAccounts().get(gettXRequest().getCurrencyCode()).getAccountNumber();
    }

    public String splitItem(String bsName, int position)
    {
        String temp = bsName;
        String[] splitString = temp.split("\\|");
        return splitString[position];
    }

    public String replaceGlobalMasks(TXRequestMOB tXRequest, String message)
    {
        message = replaceHolder(message, "{ACCOUNT}", tXRequest.getCreditAccount());
        message = replaceHolder(message, "{CONTRA}", tXRequest.getDebitAccount());
        // message = replaceHolder(message, "{AMT}", String.valueOf(tXRequest.getTxnAmount()));
        message = replaceHolder(message, "{CURRENCY}", tXRequest.getCurrencyCode());
        message = replaceHolder(message, "{ACCESSCODE}", tXRequest.getAccessCode());

        return message.replace("  ", " ").replace(" :", ":").replace(" ?", "?");
    }

    protected String replaceHolder(String message, String holder, String replacement)
    {
        return message != null && holder != null && replacement != null ? message.replace(holder, replacement).replaceAll("~~", "~") : message;
    }

    @Override
    public void setLogger(Logger logger, String realm)
    {
        this.logger = logger;
        this.realm = realm;
    }

    @Override
    public String getRealm()
    {
        return realm;
    }

    @Override
    public Logger getLogger()
    {
        return logger;
    }

    /**
     * @return the tDClient
     */
    public TDClientMOB gettDClient()
    {
        return tDClient;
    }

    /**
     * @param tDClient the tDClient to set
     */
    public void settDClient(TDClientMOB tDClient)
    {
        this.tDClient = tDClient;
    }

    /**
     * @return the tXClient
     */
    public TXClientMOB gettXClient()
    {
        return tXClient;
    }

    /**
     * @param tXClient the tXClient to set
     */
    public void settXClient(TXClientMOB tXClient)
    {
        this.tXClient = tXClient;
    }

    /**
     * @return the chargeJournalId
     */
    public String getChargeJournalId()
    {
        return chargeJournalId;
    }

    /**
     * @param chargeJournalId the chargeJournalId to set
     */
    public void setChargeJournalId(String chargeJournalId)
    {
        this.chargeJournalId = chargeJournalId;
    }

    public void dispose()
    {
        gettDClient().dispose();
    }

    /**
     * @return the txnJournalId
     */
    public String getTxnJournalId()
    {
        return txnJournalId;
    }

    /**
     * @param txnJournalId the txnJournalId to set
     */
    public void setTxnJournalId(String txnJournalId)
    {
        this.txnJournalId = txnJournalId;
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

    /**
     * @return the eITerminal
     */
    public EITerminalMOB geteITerminal()
    {
        return eITerminal;
    }

    /**
     * @param eITerminal the eITerminal to set
     */
    public void seteITerminal(EITerminalMOB eITerminal)
    {
        this.eITerminal = eITerminal;
    }

    /**
     * @return the uCActivity
     */
    public UCActivityMOB getuCActivity()
    {
        return uCActivity;
    }

    /**
     * @param uCActivity the uCActivity to set
     */
    public void setuCActivity(UCActivityMOB uCActivity)
    {
        this.uCActivity = uCActivity;
    }

    /**
     * @return the tXRequest
     */
    public TXRequestMOB gettXRequest()
    {
        return tXRequest;
    }

    /**
     * @param tXRequest the tXRequest to set
     */
    public void settXRequest(TXRequestMOB tXRequest)
    {
        this.tXRequest = tXRequest;
    }

    /**
     * @return the eIProCode
     */
    public EIProCodesMOB geteIProCode()
    {
        return eIProCode;
    }

    /**
     * @param eIProCode the eIProCode to set
     */
    public void seteIProCode(EIProCodesMOB eIProCode)
    {
        this.eIProCode = eIProCode;
    }

    /**
     * @return the eIBillerCode
     */
    public EIBillerCode geteIBillerCode()
    {
        return eIBillerCode;
    }

    /**
     * @param eIBillerCode the eIBillerCode to set
     */
    public void seteIBillerCode(EIBillerCode eIBillerCode)
    {
        this.eIBillerCode = eIBillerCode;
    }

    /**
     * @return the eIAsocBillerCode
     */
    public EIAssocBillerCode geteIAsocBillerCode()
    {
        return eIAsocBillerCode;
    }

    /**
     * @param eIAsocBillerCode the eIAsocBillerCode to set
     */
    public void seteIAsocBillerCode(EIAssocBillerCode eIAsocBillerCode)
    {
        this.eIAsocBillerCode = eIAsocBillerCode;
    }
}
