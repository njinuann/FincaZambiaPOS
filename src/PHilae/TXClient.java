/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package PHilae;

import APX.CBNode;
import APX.EICodes;
import APX.PHController;
import APX.PHMain;
import FILELoad.FCMB;
import FILELoad.FCMBSoap;
import com.neptunesoftware.supernova.ws.client.AccountWebService;
import com.neptunesoftware.supernova.ws.client.AccountWebServiceEndPointPort_Impl;
import com.neptunesoftware.supernova.ws.client.CustomerWebService;
import com.neptunesoftware.supernova.ws.client.CustomerWebServiceEndPointPort_Impl;
import com.neptunesoftware.supernova.ws.client.FundsTransferWebService;
import com.neptunesoftware.supernova.ws.client.FundsTransferWebServiceEndPointPort_Impl;
import com.neptunesoftware.supernova.ws.client.TransactionsWebService;
import com.neptunesoftware.supernova.ws.client.TransactionsWebServiceEndPointPort_Impl;
import com.neptunesoftware.supernova.ws.client.TxnProcessWebService;
import com.neptunesoftware.supernova.ws.client.TxnProcessWebServiceEndPointPort_Impl;
import com.neptunesoftware.supernova.ws.client.security.BasicHTTPAuthenticator;
import com.neptunesoftware.supernova.ws.common.XAPIException;
import com.neptunesoftware.supernova.ws.common.XAPIRequestBaseObject;
import com.neptunesoftware.supernova.ws.server.account.data.AccountBalanceOutputData;
import com.neptunesoftware.supernova.ws.server.account.data.AccountBalanceRequest;
import com.neptunesoftware.supernova.ws.server.customer.data.CustomerImageOutputData;
import com.neptunesoftware.supernova.ws.server.customer.data.CustomerImageRequestData;
import com.neptunesoftware.supernova.ws.server.transaction.data.ArrayOfDepositTxnOutputData_Literal;
import com.neptunesoftware.supernova.ws.server.transaction.data.GLTransferOutputData;
import com.neptunesoftware.supernova.ws.server.transaction.data.GLTransferRequest;
import com.neptunesoftware.supernova.ws.server.transaction.data.TransactionInquiryRequest;
import com.neptunesoftware.supernova.ws.server.transaction.data.TxnResponseOutputData;
import com.neptunesoftware.supernova.ws.server.transfer.data.FundsTransferOutputData;
import com.neptunesoftware.supernova.ws.server.transfer.data.FundsTransferRequestData;
import com.neptunesoftware.supernova.ws.server.txnprocess.data.XAPIBaseTxnRequestData;
import com.neptunesoftware.supernova.ws.client.CasemgmtWebService;
import com.neptunesoftware.supernova.ws.client.CasemgmtWebServiceEndPointPort_Impl;
import com.neptunesoftware.supernova.ws.server.casemgmt.data.CaseOutputData;
import com.neptunesoftware.supernova.ws.server.casemgmt.data.CaseRequestData;
import java.math.BigDecimal;
import java.net.Authenticator;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Calendar;
import weblogic.application.io.AA;

/**
 *
 * @author Pecherk
 */
public final class TXClient
{

    private TXProcessor tXProcessor;
    private AccountWebService accountWebService;
    private TransactionsWebService transactionsWebService;
    private FundsTransferWebService transferWebService;
    private TxnProcessWebService processWebService;
    private CustomerWebService customerWebService;
    private CasemgmtWebService caseWebService;
    private FCMBSoap fCMBSoap;
    //  private PMTService pMTService;

    public TXClient(TXProcessor tXProcessor)
    {
        settXProcessor(tXProcessor);
        connectToCore();
        connectToFileUploader();
    }

    private void connectToCore()
    {
        try
        {
            Object[] coreBankingNodes = PHController.CoreBankingNodes.toArray();
            if (coreBankingNodes.length > 0)
            {

                Arrays.sort(coreBankingNodes);
                for (Object cBNode : coreBankingNodes)
                {
                    accountWebService = new AccountWebServiceEndPointPort_Impl(((CBNode) cBNode).getWsContextURL() + "AccountWebServiceBean?wsdl").getAccountWebServiceSoapPort(PHController.XapiUser, PHController.XapiPassword);
                    transactionsWebService = new TransactionsWebServiceEndPointPort_Impl(((CBNode) cBNode).getWsContextURL() + "TransactionsWebServiceBean?wsdl").getTransactionsWebServiceSoapPort(PHController.XapiUser, PHController.XapiPassword);
                    transferWebService = new FundsTransferWebServiceEndPointPort_Impl(((CBNode) cBNode).getWsContextURL() + "FundsTransferWebServiceBean?wsdl").getFundsTransferWebServiceSoapPort(PHController.XapiUser, PHController.XapiPassword);
                    processWebService = new TxnProcessWebServiceEndPointPort_Impl(((CBNode) cBNode).getWsContextURL() + "TxnProcessWebServiceBean?wsdl").getTxnProcessWebServiceSoapPort(PHController.XapiUser, PHController.XapiPassword);
                    customerWebService = new CustomerWebServiceEndPointPort_Impl(((CBNode) cBNode).getWsContextURL() + "CustomerWebServiceBean?wsdl").getCustomerWebServiceSoapPort(PHController.XapiUser, PHController.XapiPassword);
                    caseWebService = new CasemgmtWebServiceEndPointPort_Impl(((CBNode) cBNode).getWsContextURL() + "CasemgmtWebServiceBean?wsdl").getCasemgmtWebServiceSoapPort(PHController.XapiUser, PHController.XapiPassword);

                    if (isCoreConnected())
                    {
                        ((CBNode) cBNode).setCounter(((CBNode) cBNode).getCounter() + 1);
                        initialize();
                        break;
                    }
                }
            }
        }
        catch (Exception ex)
        {
            gettXProcessor().getXapiCaller().logException(ex);
        }
    }

    private void checkCoreConnection()
    {
        if (!isCoreConnected())
        {
            connectToCore();
        }
    }

    private boolean isCoreConnected()
    {
        return accountWebService != null;
    }

    public BigDecimal txnCharge(TXRequest tXRequest)
    {
        return tXRequest.getChargeAmount();

    }

    public void connectToFileUploader()
    {
        try
        {
            fCMBSoap = new FCMB(new URL(PHController.FileLoadWsdlURL)).getFCMBSoap();
        }
        catch (MalformedURLException ex)
        {
            gettXProcessor().getXapiCaller().logException(ex);
        }
    }

    public void checkFileUploaderConnection()
    {

        if (!isFileUploaderConnected())
        {
            connectToFileUploader();
        }
    }

    private boolean isFileUploaderConnected()
    {
        //PHMain.posBridgeLog.info("this is the path am passing..to check if is connected " + PHController.FileLoadWsdlURL);
        //PHMain.smsLog.logEvent(fCMBSoap + "this is the path am passing..to check if is connected " + PHController.FileLoadWsdlURL);
        return fCMBSoap != null;
    }

    private void initialize()
    {
        TXRequest tXRequest = new TXRequest();
        tXRequest.setAccessCode(PHController.posChannelCode);
        tXRequest.setReference("0000000000");
        tXRequest.setDebitAccount("0000000000");
        queryDepositAccountBalance(tXRequest);
    }

    private XAPIRequestBaseObject getBaseRequest(XAPIRequestBaseObject requestData, TXRequest tXRequest)
    {
        checkCoreConnection();
        requestData.setChannelId(PHController.posChannelID);
        requestData.setChannelCode(PHController.posChannelCode);

        requestData.setCardNumber(tXRequest.getAccessCode());
        requestData.setTransmissionTime(System.currentTimeMillis());

        requestData.setOriginatorUserId(PHController.SystemUserID);
        requestData.setTerminalNumber(PHController.posChannelCode);

        requestData.setReference(tXRequest.getReference());
        return requestData;
    }

    public Object queryDepositAccountBalance(TXRequest tXRequest)
    {
        try
        {
            AccountBalanceRequest accountBalanceRequest = (AccountBalanceRequest) getBaseRequest(new AccountBalanceRequest(), tXRequest);
            accountBalanceRequest.setAccountNumber(tXRequest.getDebitAccount());

            gettXProcessor().getXapiCaller().setCall("acctbalreq", accountBalanceRequest);
            AccountBalanceOutputData response = accountWebService.findAccountBalance(accountBalanceRequest);

            gettXProcessor().getXapiCaller().setCall("acctbalres", response);
            return response;
        }
        catch (Exception ex)
        {
            gettXProcessor().getXapiCaller().logException(ex);
            return ex;
        }
    }

    public Object queryDepositAccountMinistatement(TXRequest tXRequest)
    {
        try
        {
            TransactionInquiryRequest inquiryRequest = (TransactionInquiryRequest) getBaseRequest(new TransactionInquiryRequest(), tXRequest);
            inquiryRequest.setAccountNumber(tXRequest.getDebitAccount());

            gettXProcessor().getXapiCaller().setCall("stmenqreq", inquiryRequest);
            ArrayOfDepositTxnOutputData_Literal statTxns = transactionsWebService.findDepositMiniStatement(inquiryRequest);

            gettXProcessor().getXapiCaller().setCall("stmenqres", statTxns);
            return statTxns;
        }
        catch (Exception ex)
        {
            gettXProcessor().getXapiCaller().logException(ex);
            return ex;
        }
    }

    public Object postDpToDpFundsTransfer(TXRequest tXRequest)
    {
        try
        {
            FundsTransferRequestData transferRequest = (FundsTransferRequestData) getBaseRequest(new FundsTransferRequestData(), tXRequest);
            transferRequest.setFromAccountNumber(tXRequest.getDebitAccount());

            transferRequest.setToAccountNumber(tXRequest.getCreditAccount());
            transferRequest.setTxnDescription(tXRequest.getTxnNarration());

            transferRequest.setAcquiringInstitutionCode("");
            transferRequest.setTransactionAmount(tXRequest.getTxnAmount());
            transferRequest.setCurrBUId(tXRequest.getBuId());

            transferRequest.setAmount(tXRequest.getTxnAmount());
            transferRequest.setFromCurrencyCode(tXRequest.getCurrencyCode());

            transferRequest.setToCurrencyCode(tXRequest.getCurrencyCode());
            transferRequest.setForwardingInstitutionCode("");

            transferRequest.setTrack2Data(tXRequest.getReference());
            transferRequest.setRetrievalReferenceNumber(tXRequest.getReference());

            Calendar calendar = Calendar.getInstance();
            calendar.setTime(gettXProcessor().gettDClient().getProcessingDate());

            transferRequest.setUserRoleId(PHController.defaultBURole);
            transferRequest.setUserLoginId(PHController.defaultSystemUser);

            transferRequest.setLocalTransactionTime(calendar);
            gettXProcessor().getXapiCaller().setCall("fundstfrreq", transferRequest);

            FundsTransferOutputData response = transferWebService.internalDepositAccountTransfer(transferRequest);
            gettXProcessor().getXapiCaller().setCall("fundstfrres", response);
            return response;
        }
        catch (Exception ex)
        {
            gettXProcessor().getXapiCaller().logException(ex);
            return ex;
        }
    }

    public Object synchBiometricImage(TXRequest tXRequest, String customerNo, String fingerType, String finData)
    {
        try
        {
            CustomerImageRequestData request = (CustomerImageRequestData) getBaseRequest(new CustomerImageRequestData(), tXRequest);
//            request.setChannelId(PHController.posChannelID);
//
//            request.setChannelCode(PHController.posChannelCode);
//            request.setOriginatorUserId(PHController.SystemUserID);
//
//            request.setTransmissionTime(System.currentTimeMillis());
            request.setCustomerNumber(customerNo);

            request.setBinaryImage(gettXProcessor().getFileBytes("img\\fin.bmp"));
            request.setBiometricImage(gettXProcessor().getByteArray("0000000000000000" + finData));

            request.setImageTypeCode(fingerType);//FINLI
            gettXProcessor().getXapiCaller().setCall("finsyncreq", request);

            CustomerImageOutputData response = customerWebService.addCustomerImage(request);
            gettXProcessor().getXapiCaller().setCall("finsyncres", response);
            return response;
        }
        catch (Exception ex)
        {
            gettXProcessor().getXapiCaller().logException(ex);
            return ex;
        }
    }

    public Object synchBioImageModify(TXRequest tXRequest, String customerNo, String fingerType, String finData)
    {
        try
        {
            CustomerImageRequestData request = new CustomerImageRequestData();
            request.setChannelId(PHController.posChannelID);

            request.setChannelCode(PHController.posChannelCode);
            request.setOriginatorUserId(PHController.SystemUserID);

            request.setTransmissionTime(System.currentTimeMillis());
            request.setCustomerNumber(customerNo);

            request.setBinaryImage(gettXProcessor().getFileBytes("img\\fin.bmp"));
            request.setBiometricImage(gettXProcessor().getByteArray("0000000000000000" + finData));

            request.setImageTypeCode(fingerType);//FINLI
            CustomerImageOutputData response = customerWebService.modifyCustomerImage(request);

            gettXProcessor().getXapiCaller().setCall("finsync", response);
            return response;
        }
        catch (Exception ex)
        {
            gettXProcessor().getXapiCaller().logException(ex);
            return ex;
        }
    }

    public Object raiseCase(TXRequest tXRequest)
    {
        try
        {
            CaseRequestData caseRequestData = (CaseRequestData) getBaseRequest(new CaseRequestData(), tXRequest);
            caseRequestData.setSubject("Check Book Request");
            caseRequestData.setCustomerNumber(tXRequest.getCustomerNo());

            caseRequestData.setCurrBUId(tXRequest.getBuId());
            caseRequestData.setUserRoleId(PHController.defaultBURole);
            caseRequestData.setUserLoginId(PHController.defaultSystemUser);

            caseRequestData.setPriority("1");
            caseRequestData.setFromDate(Calendar.getInstance());

            caseRequestData.setDueDate(Calendar.getInstance());
            caseRequestData.setCaseTypeId(11L);

            caseRequestData.setChannelId(PHController.posChannelID);
            caseRequestData.setCaseDetail("Check Book Request Customer number 0060032510 - 100 Pages");

            caseRequestData.setTransmissionTime(System.currentTimeMillis());
            caseRequestData.setStatus("N");
            gettXProcessor().getXapiCaller().setCall(isReversal() ? "revcrtcasereq" : "crtcasereq", caseRequestData);
            CaseOutputData caseResponse = caseWebService.createCase(caseRequestData);

            gettXProcessor().getXapiCaller().setCall(isReversal() ? "revcrtcaseres" : "crtcaseres", caseResponse);
            //  gettDClient().moveCustomerWFItem(cNCustomer.getCustId(), tXRequest.getcNBranch().getBuId());
            return caseResponse;
        }
        catch (Exception ex)
        {
            gettXProcessor().getXapiCaller().logException(ex);
            return ex;
        }
    }

    public Object postDepositToGLTransfer(TXRequest tXRequest)
    {
        try
        {
            XAPIBaseTxnRequestData requestData = (XAPIBaseTxnRequestData) getBaseRequest(new XAPIBaseTxnRequestData(), tXRequest);
            requestData.setAcctNo(tXRequest.getDebitAccount());
            requestData.setContraAcctNo(tXRequest.getCreditAccount());

            requestData.setTxnDescription(tXRequest.getTxnNarration());
            requestData.setTxnAmount(tXRequest.getTxnAmount());

            requestData.setTxnCurrencyCode(tXRequest.getCurrencyCode());
            requestData.setTxnReference(tXRequest.getReference());

            requestData.setUserRoleId(PHController.defaultBURole);
            requestData.setUserLoginId(PHController.defaultSystemUser);
            // requestData.setCurrBUId(tXRequest.getCurrentBu());
            requestData.setCurrBUId(tXRequest.getBuId());
            requestData.setOrgBusinessUnitId(tXRequest.getBuId());

            gettXProcessor().getXapiCaller().setCall("dptogltfrreq", requestData);

            TxnResponseOutputData response = processWebService.postDepositToGLAccountTransfer(requestData);
            gettXProcessor().getXapiCaller().setCall("dptogltfrres", response);
            return response;
        }
        catch (Exception ex)
        {
            gettXProcessor().getXapiCaller().logException(ex);
            return ex;
        }
    }

    public Object postGLToDepositTransfer(TXRequest tXRequest)
    {
        try
        {
            XAPIBaseTxnRequestData requestData = (XAPIBaseTxnRequestData) getBaseRequest(new XAPIBaseTxnRequestData(), tXRequest);
            requestData.setAcctNo(tXRequest.getCreditAccount());
            requestData.setContraAcctNo(tXRequest.getDebitAccount());

            requestData.setTxnDescription(tXRequest.getTxnNarration());
            requestData.setTxnAmount(tXRequest.getTxnAmount());

            requestData.setTxnCurrencyCode(tXRequest.getCurrencyCode());
            requestData.setTxnReference(tXRequest.getReference());

            requestData.setUserRoleId(PHController.defaultBURole);
            requestData.setUserLoginId(PHController.defaultSystemUser);

            requestData.setCurrBUId((gettXProcessor().gettDClient().getAccountBuId(tXRequest.getDebitAccount())));
            gettXProcessor().getXapiCaller().setCall("gltodptfrreq", requestData);

            TxnResponseOutputData response = processWebService.postGLToDepositAccountTransfer(requestData);
            gettXProcessor().getXapiCaller().setCall("gltodptfrres", response);
            return response;
        }
        catch (Exception ex)
        {
            gettXProcessor().getXapiCaller().logException(ex);
            return ex;
        }
    }

    public boolean processCharge(TXRequest tXRequest)
    {
        boolean charged = tXRequest.getChargeAmount() == null || BigDecimal.ZERO.compareTo(tXRequest.getChargeAmount()) >= 0;
        try
        {
            if (!charged)
            {
                XAPIBaseTxnRequestData requestData = (XAPIBaseTxnRequestData) getBaseRequest(new XAPIBaseTxnRequestData(), tXRequest);
                requestData.setAcctNo(tXRequest.getChargeDebitAccount());
                requestData.setContraAcctNo(tXRequest.getChargeCreditLedger());

                requestData.setTxnDescription(tXRequest.getChargeNarration());
                requestData.setTxnAmount(tXRequest.getChargeAmount());

                requestData.setTxnCurrencyCode(tXRequest.getCurrencyCode());
                requestData.setTxnReference(tXRequest.getReference());

                requestData.setUserRoleId(PHController.defaultBURole);
                requestData.setUserLoginId(PHController.defaultSystemUser);

                requestData.setCurrBUId((gettXProcessor().gettDClient().getAccountBuId(tXRequest.getCreditAccount())));

                gettXProcessor().getXapiCaller().setCall(isReversal() ? "revchrgreq" : "txnchrgreq", requestData);
                TxnResponseOutputData response = isReversal() ? processWebService.postGLToDepositAccountTransfer(requestData) : processWebService.postDepositToGLAccountTransfer(requestData);
                gettXProcessor().getXapiCaller().setCall(isReversal() ? "revchrgres" : "txnchrgres", response);

                gettXProcessor().setChargeJournalId(gettXProcessor().gettDClient().extractJournalId(((TxnResponseOutputData) response).getRetrievalReferenceNumber()));
                gettXProcessor().getXapiCaller().setXapiRespCode(response.getResponseCode());
                if (EICodes.XAPI_APPROVED.equals(response.getResponseCode()))
                {
                    processExciseDuty(tXRequest);
                    charged = true;
                }
            }
        }
        catch (Exception ex)
        {
            gettXProcessor().getXapiCaller().setXapiRespCode(getXapiErrorCode(ex));
            gettXProcessor().getXapiCaller().logException(ex);
        }
        return charged;
    }

    public void processExciseDuty(TXRequest tXRequest)
    {
        if (BigDecimal.ZERO.compareTo(tXRequest.getTaxAmount()) < 0)
        {
            try
            {
                GLTransferRequest glTransferRequest = (GLTransferRequest) getBaseRequest(new GLTransferRequest(), tXRequest);
                glTransferRequest.setFromGLAccountNumber(isReversal() ? tXRequest.getTaxCreditLedger() : tXRequest.getChargeCreditLedger());
                glTransferRequest.setToGLAccountNumber(isReversal() ? tXRequest.getChargeCreditLedger() : tXRequest.getTaxCreditLedger());

                glTransferRequest.setTransactionAmount(tXRequest.getTaxAmount());
                glTransferRequest.setTransactionCurrencyCode(tXRequest.getCurrencyCode());
                glTransferRequest.setTxnDescription(tXRequest.getTaxNarration());

                gettXProcessor().getXapiCaller().setCall(isReversal() ? "revchrgtaxreq" : "chrgtaxreq", glTransferRequest);
                GLTransferOutputData response = transactionsWebService.postGLtoGLXfer(glTransferRequest);
                gettXProcessor().getXapiCaller().setCall(isReversal() ? "revchrgtaxres" : "chrgtaxres", response);
            }
            catch (Exception ex)
            {
                gettXProcessor().getXapiCaller().logException(ex);
            }
        }
    }

    public String getXapiErrorCode(Exception ex)
    {
        String errorCode = EICodes.SYSTEM_ERROR;
        try
        {
            if (ex instanceof XAPIException)
            {
                if (((XAPIException) ex).getErrors() != null && EICodes.SYSTEM_ERROR.equals(errorCode))
                {
                    errorCode = ((XAPIException) ex).getErrors().length >= 1 ? ((XAPIException) ex).getErrors()[0].getErrorCode() : errorCode;
                }
            }
        }
        catch (Exception e)
        {
            gettXProcessor().getXapiCaller().logException(ex);
        }
        return errorCode;
    }

    public String agentData(String productCODE, String currency, String branchCODE, String codCUSTNAILID, String agentID, String agentACCOUNT, String accountBAL, String businessNAME, String businessTYPE, String physicalADDRESS, String postalADDRESS, String contactPERSON, String mobilePHONE, String officePHONE, String emailADDRESS, String updated, String header, String nrc, String accountIDOld)
    {
        checkFileUploaderConnection();
        return fCMBSoap.agentData(productCODE, currency, branchCODE, codCUSTNAILID, agentID, agentACCOUNT, accountBAL, businessNAME, businessTYPE, physicalADDRESS, postalADDRESS, contactPERSON, mobilePHONE, officePHONE, emailADDRESS, updated, header, nrc, accountIDOld);
    }

    public String customerData(String ourBranchID, String groupID, String clientID, String accountID, String titleOfAccount, String productID, String accountType, String nrc, String atmCardNumber, String updated, String header, String accountIDOld)
    {
        checkFileUploaderConnection();
        return fCMBSoap.customerData(ourBranchID, groupID, clientID, accountID, titleOfAccount, productID, accountType, nrc, atmCardNumber, updated, header, accountIDOld);
    }

    public String mobileMigration(java.lang.String firstName, java.lang.String middleName, java.lang.String lastName, java.lang.String mobileNumber, java.lang.String address, java.lang.String eMailID, java.lang.String city, java.lang.String accountID, java.lang.String branchID, java.lang.String maximumAmount, java.lang.String transactionLimit, java.lang.String idNumber, java.lang.String noOfTransactions, java.lang.String title, java.lang.String typeOfID, java.lang.String cardNumber, java.lang.String currencyID, java.lang.String customerType, java.lang.String updated, java.lang.String header, java.lang.String accountIDOld)
    {
        checkFileUploaderConnection();
        return fCMBSoap.mobileMigration(firstName, middleName, lastName, mobileNumber, address, eMailID, city, accountID, branchID, maximumAmount, transactionLimit, idNumber, noOfTransactions, title, typeOfID, cardNumber, currencyID, customerType, updated, header, accountIDOld);
    }

    public String mobileRegistration(java.lang.String firstName, java.lang.String middleName, java.lang.String lastName, java.lang.String mobileNumber, java.lang.String address, java.lang.String eMailID, java.lang.String city, java.lang.String accountID, java.lang.String branchID, java.lang.String maximumAmount, java.lang.String transactionLimit, java.lang.String idNumber, java.lang.String noOfTransactions, java.lang.String title, java.lang.String typeOfID, java.lang.String cardNumber, java.lang.String currencyID, java.lang.String customerType, java.lang.String updated, java.lang.String header, java.lang.String accountIDOld)
    {
        checkFileUploaderConnection();
        return fCMBSoap.mobileRegistration(firstName, middleName, lastName, mobileNumber, address, eMailID, city, accountID, branchID, maximumAmount, transactionLimit, idNumber, noOfTransactions, title, typeOfID, cardNumber, currencyID, customerType, updated, header, accountIDOld);
    }

    public String mobileDeregistration(String mobileNumber, String accountID, String reason, String extraParams, String header)
    {
        checkFileUploaderConnection();
        return fCMBSoap.mobileDeregistration(mobileNumber, accountID, reason, extraParams, header);
    }

    public String ofllineBalances(String clientID, String accountID, String titleOfAccount, String productID, String actualBalance, String availableBalance, String status, String allowDeposit, String allowWithdrawal, String nrc, String header, String accountIDOld)
    {
        checkFileUploaderConnection();
        return fCMBSoap.ofllineBalances(clientID, accountID, titleOfAccount, productID, actualBalance, availableBalance, status, allowDeposit, allowWithdrawal, nrc, header, accountIDOld);
    }

    public String orbitSMS(String messageID, String messageText, String mobileNumber, String header)
    {
        checkFileUploaderConnection();
        return fCMBSoap.orbitSMS(messageID, messageText, mobileNumber, header);
    }

    private boolean isReversal()
    {
        return gettXProcessor().getXapiCaller().isReversal();
    }

    /**
     * @return the tXProcessor
     */
    public TXProcessor gettXProcessor()
    {
        return tXProcessor;
    }

    /**
     * @param XProcessor the tXProcessor to set
     */
    public void settXProcessor(TXProcessor XProcessor)
    {
        this.tXProcessor = XProcessor;
    }

}
