/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Mobile;

import APX.CBNode;
import APX.EICodes;
import APX.PHController;
import FILELoad.FCMB;
import FILELoad.FCMBSoap;
import PHilae.CNAccount;
import PHilae.CNUser;
import PHilae.CRCaller;
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
import com.neptunesoftware.supernova.ws.client.ChannelAdminWebService;
import com.neptunesoftware.supernova.ws.client.ChannelAdminWebServiceEndPointPort_Impl;
import com.neptunesoftware.supernova.ws.server.casemgmt.data.CaseOutputData;
import com.neptunesoftware.supernova.ws.server.casemgmt.data.CaseRequestData;
import com.neptunesoftware.supernova.ws.server.channeladmin.data.CustomerChannelCreationData;
import com.neptunesoftware.supernova.ws.server.transfer.data.InternalStandingOrderRequestData;
import com.neptunesoftware.supernova.ws.server.transfer.data.StandingOrderRequestData;
import java.math.BigDecimal;
import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author Pecherk
 */
public final class TXClientMOB
{

    private TXProcessorMOB tXProcessor;
    private AccountWebService accountWebService;
    private TransactionsWebService transactionsWebService;
    private FundsTransferWebService transferWebService;
    private TxnProcessWebService processWebService;
    private CustomerWebService customerWebService;
    private CasemgmtWebService caseWebService;
    private ChannelAdminWebService channelAdminWebService;
    //  private PMTService pMTService;

    public TXClientMOB(TXProcessorMOB tXProcessor)
    {
        settXProcessor(tXProcessor);
        connectToCore();
        //  connectToEpay();
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
                    channelAdminWebService = new ChannelAdminWebServiceEndPointPort_Impl(((CBNode) cBNode).getWsContextURL() + "ChannelAdminWebServiceBean?wsdl").getChannelAdminWebServiceSoapPort(PHController.XapiUser, PHController.XapiPassword);

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

    private void initialize()
    {
        TXRequestMOB tXRequest = new TXRequestMOB();
        tXRequest.setAccessCode(PHController.mobChannelCode);
        tXRequest.setReference("0000000000");
        tXRequest.setDebitAccount("0000000000");
        queryDepositAccountBalance(tXRequest);
    }

    private XAPIRequestBaseObject getBaseRequest(XAPIRequestBaseObject requestData, TXRequestMOB tXRequest)
    {
        checkCoreConnection();
        requestData.setChannelId(PHController.mobChannelID);
        requestData.setChannelCode(PHController.mobChannelCode);

        requestData.setCardNumber(tXRequest.getAccessCode());
        requestData.setTransmissionTime(System.currentTimeMillis());

        requestData.setOriginatorUserId(PHController.SystemUserID);
        requestData.setTerminalNumber(PHController.mobChannelCode);

        requestData.setReference(tXRequest.getReference());
        return requestData;
    }

    public boolean enrollAccount(CNAccountMOB cNAccount, TXRequestMOB tXRequest)
    {
        gettXProcessor().gettDClient().deleteInvalidChannelUsers();
        try
        {
            String cardNumber = tXRequest.getAccessCode();

            CNUserMOB cNUser = gettXProcessor().gettDClient().queryCNUser(cardNumber);

            if (cardNumber.equals(cNUser.getAccessCode())
                    && cNUser.getCustId() == cNAccount.getCustId()
                    && !gettXProcessor().gettDClient().isAccountEnrolled(cNUser.getCustChannelId(), cNAccount.getAcctId()))
            {
                gettXProcessor().gettDClient().saveChannelAccount(cNUser, cNAccount);
                gettXProcessor().getXapiCaller().setXapiRespCode(EICodes.XAPI_APPROVED);
            }
            else
            {
                checkCoreConnection();
                CustomerChannelCreationData customerChannelCreationData = new CustomerChannelCreationData();

                customerChannelCreationData.setChannelId(PHController.mobChannelID);
                customerChannelCreationData.setChannelCode("SMS");

                customerChannelCreationData.setCardNumber(cardNumber);
                customerChannelCreationData.setTransmissionTime(System.currentTimeMillis());

                customerChannelCreationData.setOriginatorUserId(PHController.SystemUserID);
                customerChannelCreationData.setTerminalNumber("SMS");

                customerChannelCreationData.setReference(tXRequest.getReference());
                customerChannelCreationData.setAccessCode(cardNumber);
                customerChannelCreationData.setXAPIServiceCode("CHN013");

                customerChannelCreationData.setAccountNo(cNAccount.getAccountNumber());
                customerChannelCreationData.setCustChannelSchemeId(21L);
                //customerChannelCreationData.setCustomerNumber( gettXProcessor().gettDClient().queryCustomerById(cNAccount.getCustId()).getCustNo());
                customerChannelCreationData.setChargeAcctNumber(cNAccount.getAccountNumber());
                customerChannelCreationData.setOriginatorUserId(-99L);
                customerChannelCreationData.setTerminalNumber("SMS");
                customerChannelCreationData.setCardNumber(cardNumber);

                gettXProcessor().getXapiCaller().setCall("usrenrlreq", customerChannelCreationData);
                CustomerChannelCreationData response = channelAdminWebService.createAndConfigureCustomerChannelUser(customerChannelCreationData);

                gettXProcessor().getXapiCaller().setCall("usrenrlres", response);
                gettXProcessor().gettDClient().updateAccessName(cNAccount.getAccountName(), cardNumber);

                gettXProcessor().gettDClient().updateChannelAccount(cNAccount);
                gettXProcessor().getXapiCaller().setXapiRespCode(EICodes.XAPI_APPROVED);
                return true;
            }
        }
        catch (RemoteException | XAPIException ex)
        {
            gettXProcessor().getXapiCaller().setXapiRespCode(getXapiErrorCode(ex));
            gettXProcessor().getXapiCaller().logException(ex);

        }
        return false;

    }

    public Object queryDepositAccountBalance(TXRequestMOB tXRequest)
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

    public Object queryDepositAccountMinistatement(TXRequestMOB tXRequest)
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

    public Object postDpToDpFundsTransfer(TXRequestMOB tXRequest)
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

    public Object synchBiometricImage(TXRequestMOB tXRequest, String customerNo, String fingerType, String finData)
    {
        try
        {
            CustomerImageRequestData request = new CustomerImageRequestData();
            request.setChannelId(PHController.mobChannelID);

            request.setChannelCode(PHController.mobChannelCode);
            request.setOriginatorUserId(PHController.SystemUserID);

            request.setTransmissionTime(System.currentTimeMillis());
            request.setCustomerNumber(customerNo);

            request.setBinaryImage(gettXProcessor().getFileBytes("img\\fin.bmp"));
            request.setBiometricImage(gettXProcessor().getByteArray("0000000000000000" + finData));

            request.setImageTypeCode(fingerType);//FINLI
            gettXProcessor().getXapiCaller().setCall("finsync", request);

            CustomerImageOutputData response = customerWebService.addCustomerImage(request);
            gettXProcessor().getXapiCaller().setCall("finsync", response);
            return response;
        }
        catch (Exception ex)
        {
            gettXProcessor().getXapiCaller().logException(ex);
            return ex;
        }
    }

    public Object synchBioImageModify(TXRequestMOB tXRequest, String customerNo, String fingerType, String finData)
    {
        try
        {
            CustomerImageRequestData request = new CustomerImageRequestData();
            request.setChannelId(PHController.mobChannelID);

            request.setChannelCode(PHController.mobChannelCode);
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

    public Object raiseCase(TXRequestMOB tXRequest)
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

            caseRequestData.setChannelId(PHController.mobChannelID);
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

    public boolean createStandingOrder(TXRequestMOB tXRequest)
    {
        try
        {
            InternalStandingOrderRequestData standingOrderRequestData = (InternalStandingOrderRequestData) getBaseRequest(new InternalStandingOrderRequestData(), tXRequest);
            standingOrderRequestData.setChannelId(PHController.mobChannelID);
            standingOrderRequestData.setChannelCode(PHController.mobChannelCode);

            standingOrderRequestData.setTransmissionTime(System.currentTimeMillis());
            standingOrderRequestData.setAccountNumber(tXRequest.getDebitAccount());

            standingOrderRequestData.setDestinationAccountno(tXRequest.getCreditAccount());
            standingOrderRequestData.setNoOfPayments(5L);
            //change
            standingOrderRequestData.setNextTransferDate(new GregorianCalendar(2017, 8, 20)); //change
            standingOrderRequestData.setReference(tXRequest.getReference());

            standingOrderRequestData.setAmount(tXRequest.getTxnAmount());
            standingOrderRequestData.setPaymentType("INTERNAL");

            standingOrderRequestData.setRepaymentFrequencyUnit("D");
            standingOrderRequestData.setRepaymentFrequencyValue(1L);

            standingOrderRequestData.setNonBusinessDueDateOption("RFWD");
            standingOrderRequestData.setCurrencyCode(tXRequest.getCurrencyCode());

            standingOrderRequestData.setExpiryDate(new GregorianCalendar(2017, 9, 30));
            standingOrderRequestData.setInsufficientFundOption("SKIP");

            standingOrderRequestData.setTransferReasonId(483L);

            standingOrderRequestData.setDescription(tXRequest.getTxnNarration());
            standingOrderRequestData.setStandingOrderType("REG");

            standingOrderRequestData.setSupplimentaryReference(tXRequest.getReference());
            standingOrderRequestData.setUserLoginId(PHController.defaultSystemUser);

            standingOrderRequestData.setUserRoleId(PHController.defaultBURole);
            standingOrderRequestData.setCurrBUId(tXRequest.getBuId());

            gettXProcessor().getXapiCaller().setCall("createstandingorderreq", standingOrderRequestData);
            transferWebService.createInternalStandingOrder(standingOrderRequestData);
            gettXProcessor().getXapiCaller().setCall("createstandingorderres", "successful");

            return true;
        }
        catch (Exception ex)
        {
            gettXProcessor().getXapiCaller().logException(ex);
            return false;
        }
    }

    private boolean removeStandingOrder(TXRequestMOB tXRequest)
    {
        try
        {
            InternalStandingOrderRequestData standingOrderRequestData = (InternalStandingOrderRequestData) getBaseRequest(new InternalStandingOrderRequestData(), tXRequest);
            standingOrderRequestData.setStandingOrderId(tXRequest.getsOItem().getOrderId());

            gettXProcessor().getXapiCaller().setCall("cancelstandingorderreq", standingOrderRequestData);
            transferWebService.createInternalStandingOrder(standingOrderRequestData);
            gettXProcessor().getXapiCaller().setCall("cancelstandingorderres", "successful");
            return true;
        }
        catch (Exception ex)
        {
            gettXProcessor().getXapiCaller().logException(ex);
            return false;
        }
    }

    public Object postDepositToGLTransfer(TXRequestMOB tXRequest)
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

    public Object postGLToDepositTransfer(TXRequestMOB tXRequest)
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

    public BigDecimal txnCharge(TXRequestMOB tXRequest)
    {
        return tXRequest.getChargeAmount();

    }

    public boolean processCharge(TXRequestMOB tXRequest, boolean splitCharge)
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
                if (EICodesMOB.XAPI_APPROVED.equals(response.getResponseCode()))
                {
                    boolean tpProcessed = processTPIncome(tXRequest, splitCharge);
                    processExciseDuty(tXRequest, splitCharge);
                    charged = true;
//                    if (tpProcessed)
//                    {
//                        System.err.println("tp income processed ");
//                        processExciseDuty(tXRequest, splitCharge);
//                        charged = true;
//                    }

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

    public void processExciseDuty(TXRequestMOB tXRequest, boolean splitCharge)
    {
        BigDecimal taxAmount = tXRequest.getTaxAmount();
        if (splitCharge && BigDecimal.ZERO.compareTo(tXRequest.getSharePercentage()) < 0)
        {
            taxAmount = (tXRequest.getTaxAmount().multiply(tXRequest.getSharePercentage()).divide(new BigDecimal(100)));

        }
        if (BigDecimal.ZERO.compareTo(tXRequest.getTaxAmount()) < 0)
        {
            try
            {
                GLTransferRequest glTransferRequest = (GLTransferRequest) getBaseRequest(new GLTransferRequest(), tXRequest);
                glTransferRequest.setFromGLAccountNumber(isReversal() ? tXRequest.getTaxCreditLedger() : tXRequest.getChargeCreditLedger());
                glTransferRequest.setToGLAccountNumber(isReversal() ? tXRequest.getChargeCreditLedger() : tXRequest.getTaxCreditLedger());

                glTransferRequest.setTransactionAmount(taxAmount);
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

    public boolean processTPIncome(TXRequestMOB tXRequest, boolean splitCharge)
    {
        boolean processed;
        BigDecimal tPAmount = BigDecimal.ZERO;
        if (splitCharge && BigDecimal.ZERO.compareTo(tXRequest.getSharePercentage()) < 0)
        {
            tPAmount = tXRequest.getChargeAmount().multiply(tXRequest.getSharePercentage()).divide(new BigDecimal(100));

        }
        processed = (tPAmount == null) || (tPAmount.compareTo(BigDecimal.ZERO) <= 0);
        if (!processed)
        {
            try
            {
                GLTransferRequest glTransferRequest = (GLTransferRequest) getBaseRequest(new GLTransferRequest(), tXRequest);
                glTransferRequest.setFromGLAccountNumber(isReversal() ? tXRequest.getThirdPartyIncomeLedger() : tXRequest.getChargeCreditLedger());
                glTransferRequest.setToGLAccountNumber(isReversal() ? tXRequest.getChargeCreditLedger() : tXRequest.getThirdPartyIncomeLedger());

                glTransferRequest.setTransactionAmount(tPAmount);
                glTransferRequest.setTransactionCurrencyCode(tXRequest.getCurrencyCode());
                glTransferRequest.setTxnDescription("[income] " + tXRequest.getTxnNarration());

                gettXProcessor().getXapiCaller().setCall(isReversal() ? "revtpincomereq" : "tpincomereq", glTransferRequest);
                GLTransferOutputData response = transactionsWebService.postGLtoGLXfer(glTransferRequest);
                gettXProcessor().getXapiCaller().setCall(isReversal() ? "revtpincomeres" : "tpincomeres", response);

                processed = true;
            }
            catch (Exception ex)
            {
                gettXProcessor().getXapiCaller().logException(ex);
                processed = false;
            }
        }
        return processed;
    }

    public String getXapiErrorCode(Exception ex)
    {
        String errorCode = EICodesMOB.SYSTEM_ERROR;
        try
        {
            if (ex instanceof XAPIException)
            {
                if (((XAPIException) ex).getErrors() != null && EICodesMOB.SYSTEM_ERROR.equals(errorCode))
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

    private boolean isReversal()
    {
        return gettXProcessor().getXapiCaller().isReversal();
    }

    /**
     * @return the tXProcessor
     */
    public TXProcessorMOB gettXProcessor()
    {
        return tXProcessor;
    }

    /**
     * @param tXProcessor the tXProcessor to set
     */
    public void settXProcessor(TXProcessorMOB tXProcessor)
    {
        this.tXProcessor = tXProcessor;
    }
}
