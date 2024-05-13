package PHilae;

import APX.EICodes;
import APX.PHController;
import APX.PHMain;
import Mobile.EIBillerCode;
import Mobile.EIProCodesMOB;
import com.neptunesoftware.supernova.ws.common.XAPIException;
import com.neptunesoftware.supernova.ws.server.account.data.AccountBalanceOutputData;
import com.neptunesoftware.supernova.ws.server.casemgmt.data.CaseOutputData;
import com.neptunesoftware.supernova.ws.server.customer.data.CustomerImageOutputData;
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

public final class TXProcessor implements LogSource
{

    long startTime = 0;
    ISOSource messageSource;

    String realm = PHMain.posBridgeRealm;
    ISOMsg requestMessage, responseMessage;

    private String txnJournalId, chargeJournalId;
    private XAPICaller xapiCaller = new XAPICaller();

    private TDClient tDClient = new TDClient();
    private TXClient tXClient = new TXClient(this);

    private UCActivity uCActivity = new UCActivity();
    private EITerminal eITerminal = new EITerminal();

    private EIProCodesMOB eIProCode = new EIProCodesMOB();
    private EIBillerCode eIBillerCode = new EIBillerCode();

    private TXRequest tXRequest = new TXRequest();
    private int chargeReqField = 124;

    Logger logger = Logger.getLogger(PHMain.posBridgeLogger);
    LogEvent logEvent = new LogEvent(this, "transaction");

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

            setXapiCaller(new XAPICaller());
            settXRequest(new TXRequest());

            gettDClient().setXapiCaller(getXapiCaller());
            seteITerminal(PHController.getTerminals().get(requestMessage.getTerminalID()));

            seteIProCode(PHController.getPosProcessingCode().get(requestMessage.getString(3)));
            gettXRequest().setStan(requestMessage.getTraceAuditNumber());

            seteIBillerCode(PHController.getPosBillerCode().get(requestMessage.getString(123)));
            getXapiCaller().setBillerTxn(requestMessage.getString(3).equalsIgnoreCase(requestMessage.getString(3)));

            String MTI = requestMessage.getMTI();
            getXapiCaller().setOurCustomer(!MTI.startsWith("9"));

            getXapiCaller().setAccountNo(requestMessage.getAccountNumber1());
            getXapiCaller().setTxnAmount(requestMessage.getTxnAmount());

            getXapiCaller().setTerminalId(requestMessage.getTerminalID());
            getXapiCaller().setOurTerminal(!requestMessage.hasField(42));

            getXapiCaller().setReversal(MTI.substring(1).startsWith("4"));
            getXapiCaller().setOffline(MTI.substring(2).startsWith("2") && !MTI.substring(1).startsWith("4"));

            getXapiCaller().setRefNumber(getXapiCaller().isReversal() ? requestMessage.getOriginalTxnReference() : requestMessage.getTxnReference());
            getXapiCaller().setXapiRespCode(MTI.substring(1).startsWith("8") ? EICodes.XAPI_APPROVED : getXapiCaller().getXapiRespCode());

            gettXRequest().setAccessCode(requestMessage.getAccessCode());
            gettXRequest().setCurrencyCode(PHController.getCurrency(requestMessage.getTxnCurrency()));

            getXapiCaller().setTxnDescription(PHMain.phFrame.getPosTxnDescription(requestMessage.getProcessingCode()));
            gettXRequest().setReference(getXapiCaller().isReversal() ? requestMessage.getOriginalTxnReference() : requestMessage.getTxnReference());

            gettXRequest().setRetrivalRef(requestMessage.getString(37));

            if (!EICodes.XAPI_APPROVED.equals(getXapiCaller().getXapiRespCode()))
            {
                if (isRequestValid())
                {
                    getXapiCaller().setXapiRespCode(processTxn() ? EICodes.XAPI_APPROVED : getXapiCaller().getXapiRespCode());
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
        gettXRequest().setDebitAccount(getXapiCaller().getAccountNo());
        gettXRequest().setCreditAccount(requestMessage.getAccountNumber2());
        getXapiCaller().setTxnDescription(PHMain.phFrame.getPosTxnDescription(requestMessage.getProcessingCode()));
        // getXapiCaller().setTxnDescription(replaceGlobalMasks(gettXRequest(), PHController.posNarrations.getProperty(requestMessage.getProcessingCode())));

        setuCActivity(gettDClient().queryCardActivity(requestMessage.getAccessCode(), requestMessage.getProcessingCode(), gettXRequest().getCurrencyCode()));

        switch (requestMessage.getProcessingCode())
        {
            case "00": //POS Purchase
                settXRequest(validateFields(true, false, false) ? processPurchase() : gettXRequest());
                break;
            case "01"://Cash Advance
                settXRequest(validateFields(true, !getXapiCaller().isOurTerminal(), true) ? (!getXapiCaller().isOurTerminal() ? processAgentCashWithdrawal() : processBranchCashWithdrawal()) : gettXRequest());
                break;
            case "03"://Of-us Cash Advance
                settXRequest(validateFields(true, !getXapiCaller().isOurTerminal(), true) ? (!getXapiCaller().isOurTerminal() ? processAgentCashWithdrawal() : processBranchCashWithdrawal()) : gettXRequest());
                break;
            case "21"://Deposit
                settXRequest(validateFields(true, !getXapiCaller().isOurTerminal(), true) ? (!getXapiCaller().isOurTerminal() ? processAgentCashDeposit() : processBranchCashDeposit()) : gettXRequest());
                break;
            case "31"://balance Inquiry
                settXRequest(validateFields(true, false, false) ? queryAccountBalance(requestMessage.getAccountNumber1(), true) : gettXRequest());
                break;
            case "38"://ministatement
                settXRequest(validateFields(true, false, false) ? queryDepositMinistatement() : gettXRequest());
                break;
            case "40"://Account Transfers
                settXRequest(validateFields(true, true, true) ? processFundsTransfer() : gettXRequest());
                break;
            case "50":/* ADDED FOR BIOMETRICS SYNCHRONIZATION WITH BREFT*/
                settXRequest(validateBiometicData(true) ? synchBiometricImage() : gettXRequest());
                break;
            case "90"://adHoc Statement
                settXRequest(validateFields(true, false, false) ? queryDepositMinistatement() : gettXRequest());
                break;
            case "91"://Checkbook request
                settXRequest(validateFields(true, false, false) ? checkBookRequestCase() : gettXRequest());
                break;
            default:
                if (geteIProCode().getProcCode().substring(0, 2) != null && geteIProCode().getProcCode().substring(0, 2).equals(requestMessage.getProcessingCode()))
                {
                    if (geteIProCode().getProcType().equalsIgnoreCase("DR") && geteIProCode().getRecStatus().equalsIgnoreCase("Active"))
                    {
                        settXRequest(validateFields(true, false, true) ? processMiscBRCashWithdrawal(geteIProCode().getProcDesc() + " ") : gettXRequest());
                    }
                    else if (geteIProCode().getProcType().equalsIgnoreCase("CR") && geteIProCode().getRecStatus().equalsIgnoreCase("Active"))
                    {
                        settXRequest(validateFields(true, false, true) ? processMiscBRCashWithdrawal(geteIProCode().getProcDesc() + " ") : gettXRequest());
                    }
                    else if (geteIProCode().getProcType().equalsIgnoreCase("TFR") && geteIProCode().getRecStatus().equalsIgnoreCase("Active"))
                    {
                        settXRequest(validateFields(true, true, true) ? processMiscFundsTransfer(geteIProCode().getProcDesc() + " ") : gettXRequest());
                    }
                }
                else
                {
                    getXapiCaller().setXapiRespCode(EICodes.UNSUPPORTED_TXN_TYPE);
                }
                break;
        }
        getXapiCaller().setXapiRespCode(getXapiCaller().getXapiRespCode().equals("PE_2113") ? EICodes.XAPI_APPROVED : getXapiCaller().getXapiRespCode());//this is a workaround
        getXapiCaller().setTxnDescription(gettXRequest().getTxnNarration());

        if (!requestMessage.hasField(chargeReqField))
        {
            logTransaction(gettXRequest());
        }

        //logTransaction(gettXRequest());
        return gettXRequest().isSuccessful();
    }

    private BigDecimal queryCharge(TXRequest tXRequest)
    {
        return gettXClient().txnCharge(tXRequest);
    }

    private TXRequest queryAccountBalance(String accountNumber, boolean isMainTxn)
    {
        TXRequest request = isMainTxn ? gettXRequest() : new TXRequest();
        try
        {
            request.setTxnNarration((getXapiCaller().isReversal() ? "REV~" : "") + "POS Balance Inquiry [ " + requestMessage.getAccountNumber1() + "]- " + (requestMessage.getTerminalID()) + "~" + geteITerminal().getLocation() + "");
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
                    getXapiCaller().setXapiRespCode(EICodes.XAPI_APPROVED);
                }
                else
                {
                    if (isMainTxn)
                    {
                        if (isBalanceSufficient(request.getDebitAccount(), request.getChargeAmount()))
                        {
                            if (gettXClient().processCharge(request))
                            {
                                Object response = gettXClient().queryDepositAccountBalance(request);
                                if (response instanceof AccountBalanceOutputData)
                                {
                                    setBalance(((AccountBalanceOutputData) response).getAvailableBalance(), ((AccountBalanceOutputData) response).getLedgerBalance());
                                    if (isMainTxn)
                                    {
                                        tXRequest.setSuccessful(true);
                                        getXapiCaller().setXapiRespCode(EICodes.XAPI_APPROVED);/*ADDED TO PREVENT RETURNING RESP 91 FOR SUCCESSFUL BI*/

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
                                getXapiCaller().setXapiRespCode(EICodes.XAPI_APPROVED);/*ADDED TO PREVENT RETURNING RESP 91 FOR SUCCESSFUL BI*/

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

    private TXRequest checkBookRequestCase()
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
                if (requestMessage.hasField(chargeReqField) && Objects.equals(requestMessage.getString(chargeReqField).toUpperCase(), "CHARGE"))
                {
                    responseMessage.set(chargeReqField, queryCharge(gettXRequest()).toPlainString());
                    tXRequest.setSuccessful(true);
                    getXapiCaller().setXapiRespCode(EICodes.XAPI_APPROVED);
                }
                else
                {
                    if (gettXClient().processCharge(gettXRequest()))
                    {
                        Object response = gettXClient().raiseCase(gettXRequest());
                        if (response instanceof CaseOutputData)
                        {
                            responseMessage.set(new ISOField(48, ((CaseOutputData) response).getWfParticipatorRefNo()));
                            queryAccountBalance(gettXRequest().getDebitAccount(), false);
                            gettXRequest().setSuccessful(true);
                            //getXapiCaller().setXapiRespCode(EICodes.XAPI_APPROVED);/*ADDED TO PREVENT RETURNING RESP 91 FOR SUCCESSFUL MS*/

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

    private TXRequest queryDepositMinistatement()
    {
        try
        {
            gettXRequest().setTxnNarration((getXapiCaller().isReversal() ? "REV~" : "") + "POS Ministatement [ " + requestMessage.getAccountNumber1() + "]- " + (requestMessage.getTerminalID()) + "~" + geteITerminal().getLocation() + "");
            gettXRequest().setDebitAccount(requestMessage.getAccountNumber1());

            gettXRequest().setChargeDebitAccount(gettXRequest().getDebitAccount());
            gettXRequest().setTxnAmount(BigDecimal.ZERO);

            setTxnCharge(gettXRequest());
            if (isCardLinked(gettXRequest().getDebitAccount()))
            {
                if (requestMessage.hasField(chargeReqField) && Objects.equals(requestMessage.getString(chargeReqField).toUpperCase(), "CHARGE"))
                {
                    responseMessage.set(chargeReqField, queryCharge(gettXRequest()).toPlainString());
                    tXRequest.setSuccessful(true);
                    getXapiCaller().setXapiRespCode(EICodes.XAPI_APPROVED);
                }
                else
                {

                    if (isBalanceSufficient(gettXRequest().getDebitAccount(), gettXRequest().getChargeAmount()))
                    {
                        if (isBalanceSufficient(gettXRequest().getDebitAccount(), gettXRequest().getChargeAmount()) ? gettXClient().processCharge(gettXRequest()) : false)
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
                                getXapiCaller().setXapiRespCode(EICodes.XAPI_APPROVED);/*ADDED TO PREVENT RETURNING RESP 91 FOR SUCCESSFUL MS*/

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

    private TXRequest processFundsTransfer()
    {
        try
        {
            /*ADDED EVALUATION FOR OFFLINE TRASNACTIONS AND APPEND OFFLINE TO THE DESCRIOPTION.. THIS IS FOR EASIER IDENTIFICATION OF THE TXN IN CORE*/

            gettXRequest().setTxnNarration((getXapiCaller().isReversal() ? "REV~" : "") + (getXapiCaller().isOffline() ? "[Offline] " : "") + "POS Fund Transfer " + requestMessage.getAccountNumber1() + "->" + requestMessage.getAccountNumber2() + " [ " + (requestMessage.getTerminalID()) + "~" + geteITerminal().getLocation() + " ]");
            gettXRequest().setDebitAccount(getXapiCaller().isReversal() ? requestMessage.getAccountNumber2() : requestMessage.getAccountNumber1());

            gettXRequest().setCreditAccount(getXapiCaller().isReversal() ? requestMessage.getAccountNumber1() : requestMessage.getAccountNumber2());
            gettXRequest().setTxnAmount(requestMessage.getTxnAmount());

            gettXRequest().setChargeDebitAccount(gettXRequest().getDebitAccount());
            setTxnCharge(gettXRequest());

            if (isCardLinked(getXapiCaller().isReversal() ? gettXRequest().getCreditAccount() : gettXRequest().getDebitAccount()) ? (isTxnValid(gettXRequest()) && isWithinDailyLimit("TR") || isTfrPermited()) : false)
            {
                if (requestMessage.hasField(chargeReqField) && Objects.equals(requestMessage.getString(chargeReqField).toUpperCase(), "CHARGE"))
                {
                    responseMessage.set(chargeReqField, queryCharge(gettXRequest()).toPlainString());
                    tXRequest.setSuccessful(true);
                    getXapiCaller().setXapiRespCode(EICodes.XAPI_APPROVED);
                }
                else
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

    private TXRequest processPurchase()
    {
        try
        {
            /*ADDED EVALUATION FOR OFFLINE TRASNACTIONS AND APPEND OFFLINE TO THE DESCRIOPTION.. THIS IS FOR EASIER IDENTIFICATION OF THE TXN IN CORE*/
            gettXRequest().setTxnNarration((getXapiCaller().isReversal() ? "REV~" : "") + (getXapiCaller().isOffline() ? "[Offline]" : "") + "POS Purchase [ " + (requestMessage.getTerminalID()) + "~" + geteITerminal().getLocation() + " ]");
            gettXRequest().setDebitAccount(requestMessage.getAccountNumber1());

            gettXRequest().setCreditAccount(requestMessage.getAccountNumber2());
            gettXRequest().setTxnAmount(requestMessage.getTxnAmount());

            gettXRequest().setBuId(gettDClient().getAccountBuId(getXapiCaller().isReversal() ? tXRequest.getCreditAccount() : tXRequest.getDebitAccount()));

            gettXRequest().setChargeDebitAccount(gettXRequest().getDebitAccount());
            setTxnCharge(gettXRequest());
            if (requestMessage.hasField(chargeReqField) && Objects.equals(requestMessage.getString(chargeReqField).toUpperCase(), "CHARGE"))
            {
                responseMessage.set(chargeReqField, queryCharge(gettXRequest()).toPlainString());
                tXRequest.setSuccessful(true);
                getXapiCaller().setXapiRespCode(EICodes.XAPI_APPROVED);
            }
            else
            {
                if (isTxnValid(gettXRequest()) && isCardLinked(gettXRequest().getDebitAccount()))
                {
                    Object response = getXapiCaller().isReversal() ? gettXClient().postGLToDepositTransfer(gettXRequest()) : gettXClient().postDepositToGLTransfer(gettXRequest());
                    if (response instanceof TxnResponseOutputData)
                    {
                        getXapiCaller().setXapiRespCode(((TxnResponseOutputData) response).getResponseCode());
                        if (EICodes.XAPI_APPROVED.equals(((TxnResponseOutputData) response).getResponseCode()))
                        {
                            setTxnJournalId(gettDClient().extractJournalId(((TxnResponseOutputData) response).getRetrievalReferenceNumber()));
                            if (gettXClient().processCharge(gettXRequest()))
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
        catch (Exception ex)
        {
            getXapiCaller().setXapiRespCode(gettXClient().getXapiErrorCode(ex));
            getXapiCaller().logException(ex);
        }

        return gettXRequest();
    }

    private TXRequest processMiscBRCashWithdrawal(String desc)
    {
        try
        {
            /*ADDED EVALUATION FOR OFFLINE TRASNACTIONS AND APPEND OFFLINE TO THE DESCRIOPTION.. THIS IS FOR EASIER IDENTIFICATION OF THE TXN IN CORE*/
            gettXRequest().setTxnNarration((getXapiCaller().isReversal() ? "REV~" : "") + (getXapiCaller().isOffline() ? "[Offline] " : "") + desc + " [ " + (requestMessage.getString(2)) + " ]");
            gettXRequest().setBuId(gettDClient().getAccountBuId(getXapiCaller().isReversal() ? gettXRequest().getDebitAccount() : gettXRequest().getCreditAccount()));

            gettXRequest().setCreditAccount(getXapiCaller().isReversal() ? requestMessage.getAccountNumber1() : getTerminalAccount());
            gettXRequest().setTxnAmount(requestMessage.getTxnAmount());

            gettXRequest().setDebitAccount(getXapiCaller().isReversal() ? getTerminalAccount() : requestMessage.getAccountNumber1());

            gettXRequest().setChargeDebitAccount(getXapiCaller().isReversal() ? gettXRequest().getCreditAccount() : gettXRequest().getDebitAccount());
            setTxnCharge(gettXRequest());
            if (requestMessage.hasField(chargeReqField) && Objects.equals(requestMessage.getString(chargeReqField).toUpperCase(), "CHARGE"))
            {
                responseMessage.set(chargeReqField, queryCharge(gettXRequest()).toPlainString());
                tXRequest.setSuccessful(true);
                getXapiCaller().setXapiRespCode(EICodes.XAPI_APPROVED);
            }
            else
            {
                if (isTxnValid(gettXRequest()) && isCardLinked(gettXRequest().getDebitAccount()))
                {
                    Object response = getXapiCaller().isReversal() ? gettXClient().postGLToDepositTransfer(gettXRequest()) : gettXClient().postDepositToGLTransfer(gettXRequest());
                    if (response instanceof TxnResponseOutputData)
                    {
                        getXapiCaller().setXapiRespCode(((TxnResponseOutputData) response).getResponseCode());
                        if (EICodes.XAPI_APPROVED.equals(((TxnResponseOutputData) response).getResponseCode()))
                        {
                            setTxnJournalId(gettDClient().extractJournalId(((TxnResponseOutputData) response).getRetrievalReferenceNumber()));
                            if (gettXClient().processCharge(gettXRequest()))
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
        catch (Exception ex)
        {
            getXapiCaller().setXapiRespCode(gettXClient().getXapiErrorCode(ex));
            getXapiCaller().logException(ex);
        }
        return gettXRequest();
    }

    private TXRequest processMiscFundsTransfer(String desc)
    {
        try
        {
            /*ADDED EVALUATION FOR OFFLINE TRASNACTIONS AND APPEND OFFLINE TO THE DESCRIOPTION.. THIS IS FOR EASIER IDENTIFICATION OF THE TXN IN CORE*/

            gettXRequest().setTxnNarration((getXapiCaller().isReversal() ? "REV~" : "") + (getXapiCaller().isOffline() ? "[Offline] " : "") + desc + requestMessage.getAccountNumber1() + "->" + requestMessage.getAccountNumber2() + " [ " + (requestMessage.getString(2)) + " ]");
            gettXRequest().setDebitAccount(getXapiCaller().isReversal() ? requestMessage.getAccountNumber2() : requestMessage.getAccountNumber1());

            gettXRequest().setCreditAccount(getXapiCaller().isReversal() ? requestMessage.getAccountNumber1() : requestMessage.getAccountNumber2());
            gettXRequest().setTxnAmount(requestMessage.getTxnAmount());

            gettXRequest().setChargeDebitAccount(gettXRequest().getDebitAccount());
            setTxnCharge(gettXRequest());

            if (isCardLinked(gettXRequest().getDebitAccount()) ? (isTxnValid(gettXRequest()) && isWithinDailyLimit("TR") && isTfrPermited()) : false)
            {
                if (requestMessage.hasField(chargeReqField) && Objects.equals(requestMessage.getString(chargeReqField).toUpperCase(), "CHARGE"))
                {
                    responseMessage.set(chargeReqField, queryCharge(gettXRequest()).toPlainString());
                    tXRequest.setSuccessful(true);
                    getXapiCaller().setXapiRespCode(EICodes.XAPI_APPROVED);
                }
                else
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

    private TXRequest processBranchCashWithdrawal()
    {
        try
        {
            /*ADDED EVALUATION FOR OFFLINE TRASNACTIONS AND APPEND OFFLINE TO THE DESCRIOPTION.. THIS IS FOR EASIER IDENTIFICATION OF THE TXN IN CORE*/
            gettXRequest().setTxnNarration((getXapiCaller().isReversal() ? "REV~" : "") + (getXapiCaller().isOffline() ? "[Offline] " : "") + "POS Cash Withdrawal [ " + requestMessage.getAccountNumber1() + "]- " + (requestMessage.getTerminalID()) + "~" + geteITerminal().getLocation() + "");
            gettXRequest().setDebitAccount(getXapiCaller().isReversal() ? getTerminalAccount() : requestMessage.getAccountNumber1());

            gettXRequest().setCreditAccount(getXapiCaller().isReversal() ? requestMessage.getAccountNumber1() : getTerminalAccount());
            gettXRequest().setTxnAmount(requestMessage.getTxnAmount());

            gettXRequest().setBuId(gettDClient().getAccountBuId(getXapiCaller().isReversal() ? gettXRequest().getDebitAccount() : gettXRequest().getCreditAccount()));

            gettXRequest().setChargeDebitAccount(getXapiCaller().isReversal() ? gettXRequest().getCreditAccount() : gettXRequest().getDebitAccount());
            setTxnCharge(gettXRequest());
            if (requestMessage.hasField(chargeReqField) && Objects.equals(requestMessage.getString(chargeReqField).toUpperCase(), "CHARGE"))
            {
                responseMessage.set(chargeReqField, queryCharge(gettXRequest()).toPlainString());
                tXRequest.setSuccessful(true);
                getXapiCaller().setXapiRespCode(EICodes.XAPI_APPROVED);
            }
            else
            {
                if (isTxnValid(gettXRequest()) && isCardLinked(gettXRequest().getDebitAccount()))
                {
                    Object response = getXapiCaller().isReversal() ? gettXClient().postGLToDepositTransfer(gettXRequest()) : gettXClient().postDepositToGLTransfer(gettXRequest());
                    if (response instanceof TxnResponseOutputData)
                    {
                        getXapiCaller().setXapiRespCode(((TxnResponseOutputData) response).getResponseCode());
                        if (EICodes.XAPI_APPROVED.equals(((TxnResponseOutputData) response).getResponseCode()))
                        {
                            setTxnJournalId(gettDClient().extractJournalId(((TxnResponseOutputData) response).getRetrievalReferenceNumber()));
                            if (gettXClient().processCharge(gettXRequest()))
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
        catch (Exception ex)
        {
            getXapiCaller().setXapiRespCode(gettXClient().getXapiErrorCode(ex));
            getXapiCaller().logException(ex);
        }
        return gettXRequest();
    }

    private boolean fingerPrintExist(String custNumber)
    {
        return (gettDClient().isBiometricsRegistered("FINRI", custNumber) && gettDClient().isBiometricsRegistered("FINLI", custNumber));
    }

    private TXRequest synchBiometricImage()
    {
        String customerNumber = gettDClient().queryCustomerByActNo(requestMessage.getString(102)).getCustNo();
        String FINRIData = requestMessage.getString(112);
        String FINLIData = requestMessage.getString(113);
        if ("01".equals(requestMessage.getString(114)))
        {
            /*Field 114(value 01) if its registration*/
            if (!fingerPrintExist(customerNumber))
            {
                Object response1 = gettXClient().synchBiometricImage(gettXRequest(), customerNumber, "FINRI", FINRIData);
                PHMain.posBridgeLog.debug(new TXUtility().convertToString(response1));
                if (response1 instanceof CustomerImageOutputData)
                {
                    try
                    {
                        Object response2 = gettXClient().synchBiometricImage(gettXRequest(), customerNumber, "FINLI", FINLIData);
                        PHMain.posBridgeLog.debug(new TXUtility().convertToString(response2));
                        if (response2 instanceof CustomerImageOutputData)
                        {
                            gettXRequest().setSuccessful(true);
                        }
                        else if (response2 instanceof XAPIException)
                        {
                            getXapiCaller().setXapiRespCode(gettXClient().getXapiErrorCode((XAPIException) response2));
                        }
                    }
                    catch (Exception e)
                    {
                        getXapiCaller().logException(e);
                    }
                }
                else if (response1 instanceof XAPIException)
                {
                    getXapiCaller().setXapiRespCode(gettXClient().getXapiErrorCode((XAPIException) response1));
                }
            }
            else
            {
                gettXRequest().setSuccessful(true);
                /*return success if a record is found*/
            }
        }
        else if ("02".equals(requestMessage.getString(113)))/*field 113(02) if its a edit of biometrics on POS device*/ {
            Object response1 = gettXClient().synchBioImageModify(gettXRequest(), customerNumber, "FINRI", FINRIData);
            PHMain.posBridgeLog.debug(new TXUtility().convertToString(response1));
            if (response1 instanceof CustomerImageOutputData)
            {
                gettXRequest().setSuccessful(true);
                try
                {
                    Object response2 = gettXClient().synchBioImageModify(gettXRequest(), customerNumber, "FINLI", FINLIData);
                    PHMain.posBridgeLog.debug(new TXUtility().convertToString(response2));
                    if (response2 instanceof CustomerImageOutputData)
                    {
                        gettXRequest().setSuccessful(true);
                    }
                    else if (response2 instanceof XAPIException)
                    {
                        getXapiCaller().setXapiRespCode(gettXClient().getXapiErrorCode((XAPIException) response2));
                    }
                }
                catch (Exception e)
                {
                    getXapiCaller().logException(e);
                }
            }
            else if (response1 instanceof XAPIException)
            {
                getXapiCaller().setXapiRespCode(gettXClient().getXapiErrorCode((XAPIException) response1));
            }

        }
        else if (gettDClient().deleteBimetricsImage(customerNumber))
        {
            /*field 113(!02 0r !01) delete the biometric data for deactivation*/
            gettXRequest().setSuccessful(true);
        }
        return gettXRequest();
    }

    private TXRequest synchBiometricImage2()
    {
        CRWorker cRWorker = new CRWorker();
        /*Field 114(value 01) if its registration*/
        String customerNumber = gettDClient().queryCustomerByActNo(requestMessage.getString(102)).getCustNo();
        String FINRIData = requestMessage.getString(112);
        String FINLIData = requestMessage.getString(113);
        gettXRequest().setDebitAccount(requestMessage.getString(102));
        gettXRequest().setTxnNarration("Finger print Sync");

        byte[] imageBytes = getFileBytes("img\\fin.bmp");
        if ("01".equals(requestMessage.getString(114)))
        {
            try
            {
                boolean Finger1Reg = fingerPrintExist(customerNumber, "FINRI");
                if (!Finger1Reg)
                {
                    Object response1 = cRWorker.addBiometricImage(customerNumber, "FINRI", FINRIData, imageBytes, getXapiCaller());
                    PHMain.posBridgeLog.debug(new TXUtility().convertToString(response1));
                    if (response1 instanceof CustomerImageOutputData)
                    {
                        try
                        {
                            boolean Finger2Reg = fingerPrintExist(customerNumber, "FINLI");
                            if (!Finger2Reg)
                            {
                                //Object response2 = gettXClient().synchBiometricImage(gettXRequest(), customerNumber, "FINLI", FINLIData);
                                Object response2 = cRWorker.addBiometricImage(customerNumber, "FINLI", FINRIData, imageBytes, getXapiCaller());
                                PHMain.posBridgeLog.debug(new TXUtility().convertToString(response2));
                                if (response2 instanceof CustomerImageOutputData)
                                {
                                    gettXRequest().setSuccessful(true);
                                }
                                else if (response2 instanceof XAPIException)
                                {
                                    getXapiCaller().setXapiRespCode(gettXClient().getXapiErrorCode((XAPIException) response2));
                                    getXapiCaller().logException((Exception) response2);
                                }
                            }
                            else
                            {
                                gettXRequest().setSuccessful(true);
                                /*return success if a record is found*/
                            }
                        }
                        catch (Exception e)
                        {
                            getXapiCaller().logException(e);
                        }
                    }
                    else if (response1 instanceof XAPIException)
                    {
                        getXapiCaller().setXapiRespCode(gettXClient().getXapiErrorCode((XAPIException) response1));
                        getXapiCaller().logException((Exception) response1);
                    }
                }
                else
                {
                    gettXRequest().setSuccessful(true);
                    /*return success if a record is found*/
                }
            }
            catch (Exception e)
            {
                getXapiCaller().logException(e);
            }

        }
        else if ("02".equals(requestMessage.getString(113)))/*field 113(02) if its a edit of biometrics on POS device*/

        {
            // Object response1 = gettXClient().synchBioImageModify(gettXRequest(), customerNumber, "FINRI", FINRIData);
            Object response1 = cRWorker.addBiometricImage(customerNumber, "FINRI", FINRIData, imageBytes, getXapiCaller());

            if (response1 instanceof CustomerImageOutputData)
            {
                gettXRequest().setSuccessful(true);
                try
                {
                    // Object response2 = gettXClient().synchBioImageModify(gettXRequest(), customerNumber, "FINLI", FINLIData);
                    Object response2 = cRWorker.addBiometricImage(customerNumber, "FINLI", FINLIData, imageBytes, getXapiCaller());
                    PHMain.posBridgeLog.debug(new TXUtility().convertToString(response2));
                    if (response2 instanceof CustomerImageOutputData)
                    {
                        gettXRequest().setSuccessful(true);
                    }
                    else if (response2 instanceof XAPIException)
                    {
                        getXapiCaller().setXapiRespCode(gettXClient().getXapiErrorCode((XAPIException) response2));
                        getXapiCaller().logException((Exception) response2);
                    }
                }
                catch (Exception e)
                {
                    getXapiCaller().logException(e);
                }
            }
            else if (response1 instanceof XAPIException)
            {
                getXapiCaller().setXapiRespCode(gettXClient().getXapiErrorCode((XAPIException) response1));
                getXapiCaller().logException((Exception) response1);
            }

        }
        else if (gettDClient().deleteBimetricsImage(customerNumber))
        {
            /*field 113(!02 0r !01) delete the biometric data for deactivation*/
            gettXRequest().setSuccessful(true);
        }
        return gettXRequest();
    }

    private TXRequest processAgentCashWithdrawal()
    {
        try
        {
            gettXRequest().setTxnNarration((getXapiCaller().isReversal() ? "REV~" : "") + (getXapiCaller().isOffline() ? "[Offline] " : "") + "POS Cash Withdrawal [ " + requestMessage.getAccountNumber1() + "]- " + (requestMessage.getTerminalID()) + "~" + geteITerminal().getLocation() + "");
            gettXRequest().setDebitAccount(getXapiCaller().isReversal() ? requestMessage.getAccountNumber2() : requestMessage.getAccountNumber1());

            gettXRequest().setCreditAccount(getXapiCaller().isReversal() ? requestMessage.getAccountNumber1() : requestMessage.getAccountNumber2());
            gettXRequest().setTxnAmount(requestMessage.getTxnAmount());

            gettXRequest().setChargeDebitAccount(getXapiCaller().isReversal() ? gettXRequest().getCreditAccount() : gettXRequest().getDebitAccount());
            setTxnCharge(gettXRequest());

            if (isCardLinked(getXapiCaller().isReversal() ? gettXRequest().getCreditAccount() : gettXRequest().getDebitAccount()) ? (isTxnValid(gettXRequest()) && isWithinDailyLimit("DR") && isTransactionPermited("ALLDR")) : false)
            {
                if (requestMessage.hasField(chargeReqField) && Objects.equals(requestMessage.getString(chargeReqField).toUpperCase(), "CHARGE"))
                {
                    responseMessage.set(chargeReqField, queryCharge(gettXRequest()).toPlainString());
                    tXRequest.setSuccessful(true);
                    getXapiCaller().setXapiRespCode(EICodes.XAPI_APPROVED);
                }
                else
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

    private TXRequest processBranchCashDeposit()
    {
        try
        {
            gettXRequest().setTxnNarration((getXapiCaller().isReversal() ? "REV~" : "") + (getXapiCaller().isOffline() ? "[Offline] " : "") + "POS Cash Deposit [ " + requestMessage.getAccountNumber1() + "]- " + (requestMessage.getTerminalID()) + "~" + geteITerminal().getLocation() + "");
            gettXRequest().setDebitAccount(getXapiCaller().isReversal() ? requestMessage.getAccountNumber1() : getTerminalAccount());

            gettXRequest().setCreditAccount(getXapiCaller().isReversal() ? getTerminalAccount() : requestMessage.getAccountNumber1());
            gettXRequest().setTxnAmount(requestMessage.getTxnAmount());

            gettXRequest().setBuId(gettDClient().getAccountBuId(getXapiCaller().isReversal() ? tXRequest.getCreditAccount() : tXRequest.getDebitAccount()));

            gettXRequest().setChargeDebitAccount(getXapiCaller().isReversal() ? gettXRequest().getDebitAccount() : gettXRequest().getCreditAccount());
            setTxnCharge(gettXRequest());
            if (requestMessage.hasField(chargeReqField) && Objects.equals(requestMessage.getString(chargeReqField).toUpperCase(), "CHARGE"))
            {
                responseMessage.set(chargeReqField, queryCharge(gettXRequest()).toPlainString());
                tXRequest.setSuccessful(true);
                getXapiCaller().setXapiRespCode(EICodes.XAPI_APPROVED);
            }
            else
            {
                Object response = getXapiCaller().isReversal() ? gettXClient().postDepositToGLTransfer(gettXRequest()) : gettXClient().postGLToDepositTransfer(gettXRequest());

                if (response instanceof TxnResponseOutputData)
                {
                    getXapiCaller().setXapiRespCode(((TxnResponseOutputData) response).getResponseCode());
                    if (EICodes.XAPI_APPROVED.equals(((TxnResponseOutputData) response).getResponseCode()))
                    {
                        setTxnJournalId(gettDClient().extractJournalId(((TxnResponseOutputData) response).getRetrievalReferenceNumber()));
                        if (gettXClient().processCharge(gettXRequest()))
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
        catch (Exception ex)
        {
            getXapiCaller().setXapiRespCode(gettXClient().getXapiErrorCode(ex));
            getXapiCaller().logException(ex);
        }

        return gettXRequest();
    }

    private TXRequest processAgentCashDeposit()
    {
        try
        {
            gettXRequest().setTxnNarration((getXapiCaller().isReversal() ? "REV~" : "") + (getXapiCaller().isOffline() ? "[Offline ]" : "") + "POS Cash Deposit [ " + requestMessage.getAccountNumber2() + "]- " + (requestMessage.getTerminalID()) + "~" + geteITerminal().getLocation() + "");
            gettXRequest().setDebitAccount(getXapiCaller().isReversal() ? requestMessage.getAccountNumber2() : requestMessage.getAccountNumber1());

            gettXRequest().setCreditAccount(getXapiCaller().isReversal() ? requestMessage.getAccountNumber1() : requestMessage.getAccountNumber2());
            gettXRequest().setTxnAmount(requestMessage.getTxnAmount());

            gettXRequest().setBuId(gettDClient().getAccountBuId(getXapiCaller().isReversal() ? gettXRequest().getDebitAccount() : gettXRequest().getCreditAccount()));

            gettXRequest().setChargeDebitAccount(getXapiCaller().isReversal() ? gettXRequest().getDebitAccount() : gettXRequest().getCreditAccount());
            gettXRequest().setBuId(gettDClient().getAccountBuId(getXapiCaller().isReversal() ? requestMessage.getAccountNumber1() : requestMessage.getAccountNumber2()));
            setTxnCharge(gettXRequest());

            if (isTxnValid(gettXRequest()) && isTransactionPermited("ALLCR"))
            {
                if (requestMessage.hasField(chargeReqField) && Objects.equals(requestMessage.getString(chargeReqField).toUpperCase(), "CHARGE"))
                {
                    responseMessage.set(chargeReqField, queryCharge(gettXRequest()).toPlainString());
                    tXRequest.setSuccessful(true);
                    getXapiCaller().setXapiRespCode(EICodes.XAPI_APPROVED);
                }
                else
                {
                    processDepositToDepositTransfer(gettXRequest(), gettXRequest().getCreditAccount());
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

//    
    private void processDepositToDepositTransfer(TXRequest tXRequest, String balanceAccount)
    {
        Object response = gettXClient().postDpToDpFundsTransfer(tXRequest);
        if (response instanceof FundsTransferOutputData)
        {
            setTxnJournalId(((FundsTransferOutputData) response).getRetrievalReferenceNumber());
            getXapiCaller().setXapiRespCode(((FundsTransferOutputData) response).getResponseCode());

            if (EICodes.XAPI_APPROVED.equals(((FundsTransferOutputData) response).getResponseCode()))
            {
                if (gettXClient().processCharge(tXRequest))
                {
                    queryAccountBalance(balanceAccount, false);
                    tXRequest.setSuccessful(true);
                    gettDClient().logInfo("processDepositToDepositTransfer tXRequest.setSuccessful -> " + tXRequest.isSuccessful());
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

//added
    public byte[] getFileBytes(String fileUrl)
    {
        File file = new File(fileUrl);
        getXapiCaller().setCall("File location ", file.getAbsoluteFile());
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
//added

    private void logTransaction(TXRequest tXRequest)
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
            gettDClient().getLogStatement().setString(16, EICodes.XAPI_APPROVED.equals(getXapiCaller().getXapiRespCode()) ? "APPROVED" : "REJECTED");

            gettDClient().getLogStatement().setString(17, PHController.mapToIsoCode(getXapiCaller().getXapiRespCode()));
            gettDClient().getLogStatement().setLong(18, PHController.posChannelID);

            gettDClient().getLogStatement().setString(19, getXapiCaller().isReversal() ? "Y" : "N");
            gettDClient().getLogStatement().setString(20, getTxnJournalId());

            gettDClient().getLogStatement().setString(21, getChargeJournalId());
            gettDClient().getLogStatement().setString(22, tXRequest.getStan());
            gettDClient().getLogStatement().execute();
        }
        catch (Exception ex)
        {
            getXapiCaller().logException(ex);
        }
    }

    private void setTxnCharge(TXRequest tXRequest)
    {
        EICharge eICharge = PHController.getPosCharge(requestMessage.getProcessingCode() + (getXapiCaller().isOurTerminal() ? "B" : "A"));
        CNAccount chargeAccount = gettDClient().queryDepositAccount(tXRequest.getChargeDebitAccount());

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

            tXRequest.setChannelContraLedger(gettDClient().getChannelContraGL(PHController.posChannelID, buId));
            tXRequest.setTaxNarration(eICharge.getTaxName());

            final String chargeValueKey = chargeAccount.getCurrency().getCurrencyCode();
            TCValue tCValue = eICharge.getValues().containsKey(chargeValueKey) ? eICharge.getValues().get(chargeValueKey) : new TCValue();

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
                        tXRequest.setChargeAmount(((TCTier) tiers[tiers.length - 1]).getChargeAmount());
                        for (int i = tiers.length - 1; i >= 0; i--)
                        {
                            if (tXRequest.getTxnAmount().compareTo(((TCTier) tiers[i]).getTierCeiling()) <= 0)
                            {
                                tXRequest.setChargeAmount(((TCTier) tiers[i]).getChargeAmount());
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
                    TCWaiver waiver = (TCWaiver) waivers[i];
                    CNAccount beneficiaryAccount = gettDClient().queryDepositAccount(tXRequest.getCreditAccount());
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

    private boolean applyWaiver(TCWaiver waiver)
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
                PHController.logPosError(ex);
            }
        }
    }

    private void sendResponse()
    {
        try
        {

            if (!EICodes.SIMILAR_TXN_IN_PROGRESS.equals(getXapiCaller().getXapiRespCode()))
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
                PHMain.phFrame.insertPosTxnToTree(responseMessage);
            });
        }

        logEvent.clear();
        setXapiCaller(null);
        PHController.releasePosProcessor(this);
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
                getXapiCaller().setXapiRespCode(EICodes.INSUFFICIENT_FUNDS_1);
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

    private boolean isTransactionPermited(String bloclSt)
    {
        if (gettDClient().blockedDepositAccount(requestMessage.getAccountNumber2(), bloclSt))
        {
            getXapiCaller().setXapiRespCode(EICodes.TRANSACTION_BLOCKED);
        }
        else
        {
            return true;
        }
        return false;
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

//    private boolean activatedUnfunded(String tfrAcct)
//    {
//        boolean isUnfunded = gettDClient().acctStatusUnfunded(tfrAcct);      
//        return isUnfunded ? gettDClient().activateUnfundedAccount(tfrAcct) : true;
//    }
    private boolean isRequestValid()
    {
        checkDuplicate();
        if (geteITerminal() == null)
        {
            getXapiCaller().setXapiRespCode(EICodes.TRANSACTION_NOT_PERMITTED_ON_TERMINAL);
        }
        else if (PHController.activeTransactions.contains(tXRequest.getReference()))
        {
            getXapiCaller().setXapiRespCode(EICodes.SIMILAR_TXN_IN_PROGRESS);
        }
        else if (!getXapiCaller().isReversal() && gettDClient().transactionExists(requestMessage.getTxnReference()))
        {
            getXapiCaller().setXapiRespCode(EICodes.DUPLICATE_REFERENCE);
        }
        else if (getXapiCaller().isReversal() && !tDClient.transactionExists(requestMessage.getOriginalTxnReference()))
        {
            getXapiCaller().setXapiRespCode(EICodes.MISSING_ORIGINAL_TXN_REFERENCE);
        }
        else if (getXapiCaller().isReversal() && tDClient.reversalTransactionExists(requestMessage.getOriginalTxnReference()))
        {
            getXapiCaller().setXapiRespCode(EICodes.DUPLICATE_REFERENCE);
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

    private boolean validateFields(boolean checkAccount1, boolean checkAccount2, boolean checkAmount)
    {
        boolean returnCode = false;
        if (checkAccount1)
        {
            if (gettDClient().queryDepositAccount(requestMessage.getAccountNumber1()).getAccountNumber() == null)
            {
                getXapiCaller().setXapiRespCode(EICodes.INVALID_ACCOUNT);
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


    /*==ADDED for biometrics synchronisation==*/
    private boolean validateBiometicData(boolean checkCustomerNo)
    {
        if (checkCustomerNo ? gettDClient().queryCustomerByActNo(requestMessage.getString(102)).getCustNo() == null : false)
        {
            getXapiCaller().setXapiRespCode(EICodes.INVALID_CARD_NO);
            return false;
        }
        return true;
    }

    private boolean fingerPrintExist(String custNumber, String fingerType)
    {
        if ("FINRI".equals(fingerType))
        {
            return (gettDClient().isBiometricsRegistered(fingerType, custNumber));
        }
        else
        {
            return (gettDClient().isBiometricsRegistered(fingerType, custNumber));
        }

    }

    /*----======= end ===============*/
    private boolean isWithinDailyLimit(String limitType)
    {
        BigDecimal dailyDepositLimit = PHController.getDecimalSetting("DailyDepositLimit" + PHController.getCurrency(requestMessage.getTxnCurrency()));
        BigDecimal dailyTransferLimit = PHController.getDecimalSetting("DailyTransferLimit" + PHController.getCurrency(requestMessage.getTxnCurrency()));
        BigDecimal dailyWithdrawalLimit = PHController.getDecimalSetting("DailyWithdrawalLimit" + PHController.getCurrency(requestMessage.getTxnCurrency()));

        if ("CR".equals(limitType) && BigDecimal.ZERO != dailyDepositLimit && getuCActivity().getVolume().add(requestMessage.getTxnAmount()).compareTo(dailyDepositLimit) > 0)
        {
            getXapiCaller().setXapiRespCode(EICodes.DAILY_TXN_AMOUNT_LIMIT_EXCEEDED);
            return false;
        }
        if ("TR".equals(limitType) && BigDecimal.ZERO != dailyTransferLimit && getuCActivity().getVolume().add(requestMessage.getTxnAmount()).compareTo(dailyTransferLimit) > 0)
        {
            getXapiCaller().setXapiRespCode(EICodes.DAILY_TXN_AMOUNT_LIMIT_EXCEEDED);
            return false;
        }
        if ("DR".equals(limitType) && BigDecimal.ZERO != dailyWithdrawalLimit && getuCActivity().getVolume().add(requestMessage.getTxnAmount()).compareTo(dailyWithdrawalLimit) > 0)
        {
            getXapiCaller().setXapiRespCode(EICodes.DAILY_TXN_AMOUNT_LIMIT_EXCEEDED);
            return false;
        }
        return true;
    }

    private boolean isCardLinked(String accountNumber)
    {
        boolean isLinked = gettDClient().isAccountEnrolled(gettDClient().queryCNUser(requestMessage.getAccessCode()).getCustChannelId(), gettDClient().queryDepositAccount(accountNumber).getAcctId());
        //  if (!isLinked && !checkIfGroupDeposit(accountNumber))
        if (!isLinked)
        {
            getXapiCaller().setXapiRespCode(EICodes.INVALID_CARD_ACCOUNT);
        }
        return isLinked;
    }

    public void swapAccounts(TXRequest tXRequest)
    {
        String accountNumber = tXRequest.getCreditAccount();
        tXRequest.setCreditAccount(tXRequest.getDebitAccount());
        tXRequest.setDebitAccount(accountNumber);
    }

    private boolean isTxnValid(TXRequest tXRequest)
    {
        return getXapiCaller().isReversal() || (gettDClient().isAccountAllowed(tXRequest.getDebitAccount()) && isBalanceSufficient(tXRequest.getDebitAccount(), tXRequest.getTxnAmount().add(tXRequest.getChargeAmount())));
    }

    private String getTerminalAccount()
    {
        return geteITerminal().getAccounts().containsKey(gettXRequest().getCurrencyCode()) ? geteITerminal().getAccounts().get(gettXRequest().getCurrencyCode()).getAccountNumber() : null;
    }

    public String replaceGlobalMasks(TXRequest tXRequest, String message)
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
    public TDClient gettDClient()
    {
        return tDClient;
    }

    /**
     * @param tDClient the tDClient to set
     */
    public void settDClient(TDClient tDClient)
    {
        this.tDClient = tDClient;
    }

    /**
     * @return the tXClient
     */
    public TXClient gettXClient()
    {
        return tXClient;
    }

    /**
     * @param tXClient the tXClient to set
     */
    public void settXClient(TXClient tXClient)
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

    /**
     * @return the eITerminal
     */
    public EITerminal geteITerminal()
    {
        return eITerminal;
    }

    /**
     * @param eITerminal the eITerminal to set
     */
    public void seteITerminal(EITerminal eITerminal)
    {
        this.eITerminal = eITerminal;
    }

    /**
     * @return the uCActivity
     */
    public UCActivity getuCActivity()
    {
        return uCActivity;
    }

    /**
     * @param uCActivity the uCActivity to set
     */
    public void setuCActivity(UCActivity uCActivity)
    {
        this.uCActivity = uCActivity;
    }

    /**
     * @return the tXRequest
     */
    public TXRequest gettXRequest()
    {
        return tXRequest;
    }

    /**
     * @param tXRequest the tXRequest to set
     */
    public void settXRequest(TXRequest tXRequest)
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
}
