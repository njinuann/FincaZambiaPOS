/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Mobile;

import APX.BRCrypt;
import FILELoad.BaseResponse;
import FILELoad.CASARequest;
import FILELoad.EStmtRequest;
import APX.PHController;
import APX.CBNode;
import APX.PHMain;
import FILELoad.SelfRegistration;
import FILELoad.StandingOrderRequest;
import Ruby.model.WFWorkItemDetail;
import com.neptunesoftware.supernova.ws.client.AccountWebService;
import com.neptunesoftware.supernova.ws.client.AccountWebServiceEndPointPort_Impl;
import com.neptunesoftware.supernova.ws.client.ChannelAdminWebServiceEndPointPort_Impl;
import com.neptunesoftware.supernova.ws.client.FundsTransferWebService;
import com.neptunesoftware.supernova.ws.client.FundsTransferWebServiceEndPointPort_Impl;
import com.neptunesoftware.supernova.ws.client.WorkflowWebService;
import com.neptunesoftware.supernova.ws.client.WorkflowWebServiceEndPointPort_Impl;
import com.neptunesoftware.supernova.ws.common.XAPIException;
import com.neptunesoftware.supernova.ws.common.XAPIRequestBaseObject;
import com.neptunesoftware.supernova.ws.server.account.data.AccountRequest;
import com.neptunesoftware.supernova.ws.server.account.data.DepositAccountOutputData;
import com.neptunesoftware.supernova.ws.server.account.data.DepositAccountRequestData;
import com.neptunesoftware.supernova.ws.server.casemgmt.data.CaseRequestData;
import com.neptunesoftware.supernova.ws.server.transaction.data.TxnResponseOutputData;
import com.neptunesoftware.supernova.ws.server.transfer.data.InternalStandingOrderRequestData;
import com.neptunesoftware.supernova.ws.server.workflow.data.WFViewRequestData;
import java.math.BigDecimal;
import java.rmi.RemoteException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 *
 * @author NJINU
 */
public class EIWServiceProcessor
{

    private TDClientMOB tDClient = new TDClientMOB();
    private TXRequestMOB tXRequest = new TXRequestMOB();
    private XAPICallerMOB xapiCaller = new XAPICallerMOB();

    private AccountWebService accountWebService;
    private FundsTransferWebService transferWebService;
    private WorkflowWebService workflowWebService;

    private void connectToCore()
    {
        Object[] coreBankingNodes = PHController.CoreBankingNodes.toArray();
        if (coreBankingNodes.length > 0)
        {
            Arrays.sort(coreBankingNodes);
            for (Object cBNode : coreBankingNodes)
            {
                try
                {
                    accountWebService = new AccountWebServiceEndPointPort_Impl(((CBNode) cBNode).getWsContextURL() + "AccountWebServiceBean?wsdl").getAccountWebServiceSoapPort(PHController.XapiUser, PHController.XapiPassword);
                    transferWebService = new FundsTransferWebServiceEndPointPort_Impl(((CBNode) cBNode).getWsContextURL() + "FundsTransferWebServiceBean?wsdl").getFundsTransferWebServiceSoapPort(PHController.XapiUser, PHController.XapiPassword);
                    workflowWebService = new WorkflowWebServiceEndPointPort_Impl(((CBNode) cBNode).getWsContextURL() + "WorkflowWebServiceBean?wsdl").getWorkflowWebServiceSoapPort(PHController.XapiUser, PHController.XapiPassword);

                }
                catch (Exception ex)
                {
                    gettDClient().logError(ex);
                }
                if (isConnected())
                {
                    ((CBNode) cBNode).setCounter(((CBNode) cBNode).getCounter() + 1);
                    break;
                }
            }
        }
    }

    private void checkConnection()
    {
        if (!isConnected())
        {
            connectToCore();
        }
    }

    private boolean isConnected()
    {
        return accountWebService != null;
    }

    private XAPIRequestBaseObject getBaseRequest(XAPIRequestBaseObject requestData, TXRequestMOB tXRequest)
    {
        checkConnection();
        requestData.setChannelId(PHController.mobChannelID);
        requestData.setChannelCode(PHController.mobChannelCode);

        requestData.setCardNumber(tXRequest.getAccessCode());
        requestData.setTransmissionTime(System.currentTimeMillis());

        requestData.setOriginatorUserId(PHController.SystemUserID);
        requestData.setTerminalNumber(PHController.mobChannelCode);

        requestData.setReference(tXRequest.getReference());
        return requestData;
    }

    public BaseResponse generateStatement(EStmtRequest eStmtRequest)
    {
        BaseResponse baseResponse = new BaseResponse();
        EISendEStatement eISendEStatement = new EISendEStatement();

        if (!BRCrypt.decrypt(eStmtRequest.getAuthencationCode()).equals("BRN3P23cUr1T7"))
        {
            baseResponse.setResponseCode(PHController.mapToIsoCode(EICodesMOB.XAPI_CONNECTION_TERMINATED));
            baseResponse.setResponseString(PHController.getXapiMessage(EICodesMOB.XAPI_CONNECTION_TERMINATED));
        }
        else if (!gettDClient().verifyEmail(eStmtRequest.getAccountNo()))
        {
            baseResponse.setResponseCode(PHController.mapToIsoCode(EICodesMOB.VALID_EMAIL_MISSING));
            baseResponse.setResponseString(PHController.getXapiMessage(EICodesMOB.VALID_EMAIL_MISSING));
        }
        else
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
        return baseResponse;
    }

    public BaseResponse createCASA(CASARequest cASARequest)
    {
        BaseResponse baseResponse = new BaseResponse();
        try
        {
            gettXRequest().setTxnNarration("MOB CASA Creation [ " + (cASARequest.getAcessCode()) + " ]");
            gettXRequest().setTxnAmount(BigDecimal.ZERO);
            gettXRequest().setCustomerNo(gettDClient().queryCustomerByIdNo(cASARequest.getIdNumber()).getCustNo());

            if (!BRCrypt.decrypt(cASARequest.getAuthencationCode()).equals("BRN3P23cUr1T7"))
            {
                baseResponse.setResponseCode(PHController.mapToIsoCode(EICodesMOB.XAPI_CONNECTION_TERMINATED));
                baseResponse.setResponseString(PHController.getXapiMessage(EICodesMOB.XAPI_CONNECTION_TERMINATED));
            }
            else if (!gettDClient().isIdNumberValid(cASARequest.getIdNumber(), cASARequest.getAcessCode()))
            {
                baseResponse.setResponseCode(PHController.mapToIsoCode(EICodesMOB.TRANSACTION_NOT_ALLOWED_FOR_ACCOUNT));
                baseResponse.setResponseString(PHController.getXapiMessage(EICodesMOB.TRANSACTION_NOT_ALLOWED_FOR_ACCOUNT));
            }
            else if ((cASARequest.getAcessCode() != null || !cASARequest.getAcessCode().isEmpty()))
            {
                Object response = createCASA(gettXRequest(), cASARequest);
                if (response instanceof DepositAccountOutputData)
                {
                    gettXRequest().setSuccessful(true);
                    baseResponse.setResponseCode(PHController.mapToIsoCode(EICodesMOB.ISO_APPROVED));
                    baseResponse.setResponseString(PHController.getXapiMessage(EICodesMOB.ISO_APPROVED));
                    String acct_no = ((DepositAccountOutputData) response).getPrimaryAccountNumber();
                    String acct_status = ((DepositAccountOutputData) response).getAccountStatus();

                    gettDClient().logCasaCreation(cASARequest, gettXRequest(), acct_no, acct_status, "A");
                }
                else if (response instanceof XAPIException)
                {
                    getXapiCaller().setXapiRespCode(getXapiErrorCode((XAPIException) response));
                    baseResponse.setResponseCode(PHController.mapToIsoCode(getXapiCaller().getXapiRespCode()));
                    baseResponse.setResponseString(PHController.getXapiMessage(getXapiCaller().getXapiRespCode()));
                    gettDClient().logCasaCreation(cASARequest, gettXRequest(), "N/A", "N/A", "F");
                }
                else
                {

                    baseResponse.setResponseCode(PHController.mapToIsoCode(EICodesMOB.TRANSMISSION_ERROR));
                    baseResponse.setResponseString(PHController.getXapiMessage(EICodesMOB.TRANSMISSION_ERROR));
                }
            }
            else
            {
                baseResponse.setResponseCode(PHController.mapToIsoCode(PHController.mapToIsoCode(EICodesMOB.NULL_REQUEST)));
                baseResponse.setResponseString(PHController.getXapiMessage(EICodesMOB.NULL_REQUEST));
            }

        }
        catch (Exception ex)
        {
            getXapiCaller().setXapiRespCode(getXapiErrorCode(ex));
            getXapiCaller().logException(ex);
            baseResponse.setResponseCode("91");
            baseResponse.setResponseString("FAILED");
        }
        getWebServiceObjectString(baseResponse);
        return baseResponse;
    }

    public BaseResponse serviceSelfRegistration(SelfRegistration selfRegistration)
    {
        BaseResponse baseResponse = new BaseResponse();
        CRWorkerMOB cRWorker = new CRWorkerMOB();
        if (!BRCrypt.decrypt(selfRegistration.getAuthencationCode()).equals("BRN3P23cUr1T7"))
        {
            baseResponse.setResponseCode(PHController.mapToIsoCode(EICodesMOB.XAPI_CONNECTION_TERMINATED));
            baseResponse.setResponseString(PHController.getXapiMessage(EICodesMOB.XAPI_CONNECTION_TERMINATED));
        }
        else if (!gettDClient().isIdNumberValid2(selfRegistration.getIdNumber(), selfRegistration.getAccountNumber()))
        {
            baseResponse.setResponseCode(PHController.mapToIsoCode(EICodesMOB.INVALID_ACCOUNT));
            baseResponse.setResponseString(PHController.getXapiMessage(EICodesMOB.INVALID_ACCOUNT));
        }
        else if (selfRegistration.getAccessNumber() == null || selfRegistration.getAccessNumber().isEmpty())
        {
            baseResponse.setResponseCode(PHController.mapToIsoCode(EICodesMOB.TRANSACTION_NOT_ALLOWED_FOR_ACCOUNT));
            baseResponse.setResponseString(PHController.getXapiMessage(EICodesMOB.TRANSACTION_NOT_ALLOWED_FOR_ACCOUNT));
        }
        else if (cRWorker.selfRegisterMobile(selfRegistration))
        {
            baseResponse.setResponseCode(PHController.mapToIsoCode(EICodesMOB.ISO_APPROVED));
            baseResponse.setResponseString(PHController.getXapiMessage(EICodesMOB.ISO_APPROVED));
        }
        else
        {
            baseResponse.setResponseCode(PHController.mapToIsoCode(EICodesMOB.SYSTEM_ERROR));
            baseResponse.setResponseString(PHController.getXapiMessage(EICodesMOB.SYSTEM_ERROR));
        }
        return baseResponse;
    }

    public BaseResponse createStandingOrders(StandingOrderRequest standingOrderRequest)
    {
        BaseResponse baseResponse = new BaseResponse();
        try
        {
            gettXRequest().setTxnNarration("MOB StandingOrder Creation [ " + (standingOrderRequest.getFromAccountNumber()) + " ]");
            gettXRequest().setTxnAmount(standingOrderRequest.getTfrAmount());
            gettXRequest().setCurrencyCode("ZMW");
            gettXRequest().setBuId(gettDClient().getAccountBuId(standingOrderRequest.getFromAccountNumber()));
            boolean verifyAcct = gettDClient().verifyAccountForSto(standingOrderRequest.getFromAccountNumber(), standingOrderRequest.getIdNumber(), standingOrderRequest.getAccessCode());
            if (!gettDClient().verifyAccountForSto(standingOrderRequest.getFromAccountNumber(), standingOrderRequest.getIdNumber(), standingOrderRequest.getAccessCode()))
            {
                baseResponse.setResponseCode(PHController.mapToIsoCode(EICodesMOB.INVALID_ACCOUNT));
                baseResponse.setResponseString(PHController.getXapiMessage(EICodesMOB.INVALID_ACCOUNT));
            }
            else if (!BRCrypt.decrypt(standingOrderRequest.getAuthencationCode()).equals("BRN3P23cUr1T7"))
            {
                baseResponse.setResponseCode(PHController.mapToIsoCode(EICodesMOB.XAPI_CONNECTION_TERMINATED));
                baseResponse.setResponseString(PHController.getXapiMessage(EICodesMOB.XAPI_CONNECTION_TERMINATED));
            }
            else if (standingOrderRequest.getToAccountNumber() == null || standingOrderRequest.getToAccountNumber().isEmpty())
            {
                baseResponse.setResponseCode(PHController.mapToIsoCode(EICodesMOB.MISSING_ACCT_NUMBER));
                baseResponse.setResponseString(PHController.getXapiMessage(EICodesMOB.MISSING_ACCT_NUMBER));
            }
            else if (standingOrderRequest.getNoOfPayments() == null || standingOrderRequest.getTfrFreq() == null || standingOrderRequest.getTfrTerm() == null || standingOrderRequest.getIdNumber() == null)
            {
                baseResponse.setResponseCode(PHController.mapToIsoCode(EICodesMOB.NULL_REQUEST));
                baseResponse.setResponseString(PHController.getXapiMessage(EICodesMOB.NULL_REQUEST));
            }
            else if (standingOrderRequest.getFromAccountNumber() != null || !standingOrderRequest.getFromAccountNumber().isEmpty())
            {
                Boolean response = createSTO(gettXRequest(), standingOrderRequest);
                if (response)
                {
                    // approve workflow
                    boolean customerApproved = false;
                    WFWorkItemDetail workFlowItemDetail = gettDClient().getWfItem(standingOrderRequest.getReference() + "-" + (standingOrderRequest.getFromAccountNumber()));
                    while (workFlowItemDetail.getNextEventId().compareTo(0L) != 0)
                    {

                        boolean approved = approveWorkFlowItem(workFlowItemDetail, gettXRequest());
                        if (approved)
                        {
                            customerApproved = true;
                            workFlowItemDetail = gettDClient().getWfItem(standingOrderRequest.getReference() + "-" + (standingOrderRequest.getFromAccountNumber()));
                        }
                        else
                        {
                            customerApproved = false;
                            break;
                        }
                    }
                    if (customerApproved)
                    {
                        gettXRequest().setSuccessful(true);
                        baseResponse.setResponseCode(PHController.mapToIsoCode(EICodesMOB.ISO_APPROVED));
                        baseResponse.setResponseString(PHController.getXapiMessage(EICodesMOB.ISO_APPROVED));
                    }
                    else
                    {
                        gettXRequest().setSuccessful(false);
                        baseResponse.setResponseCode(PHController.mapToIsoCode(EICodesMOB.ISO_APPROVED));
                        baseResponse.setResponseString("Approval of STO was not successful");
                    }
                }
                else
                {
                    baseResponse.setResponseCode(PHController.mapToIsoCode(EICodesMOB.TRANSMISSION_ERROR));
                    baseResponse.setResponseString(PHController.getXapiMessage(EICodesMOB.TRANSMISSION_ERROR));
                }
            }
            else
            {
                baseResponse.setResponseCode(PHController.mapToIsoCode(EICodesMOB.NULL_REQUEST));
                baseResponse.setResponseString(PHController.getXapiMessage(EICodesMOB.NULL_REQUEST));
            }
            //  }
            // }
        }
        catch (Exception ex)
        {
            getXapiCaller().setXapiRespCode(getXapiErrorCode(ex));
            getXapiCaller().logException(ex);
            baseResponse.setResponseCode("91");
            baseResponse.setResponseString("FAILED");
        }
        getXapiCaller().setCall("apirequest", standingOrderRequest);
        getXapiCaller().setCall("request", gettXRequest());
        getXapiCaller().setCall("request", baseResponse);
        windUp();
        return baseResponse;
    }

    public boolean approveWorkFlowItem(WFWorkItemDetail workFlowItemDetail, TXRequestMOB tXRequest)
    {
        try
        {
            Long workItemId = workFlowItemDetail.getWorkItemId();
            if (workItemId != 0L)
            {
                WFViewRequestData wFViewRequestData = (WFViewRequestData) getBaseRequest(new WFViewRequestData(), tXRequest);
                wFViewRequestData.setEventId(workFlowItemDetail.getNextEventId());
                wFViewRequestData.setWorkItemId(workItemId);

                getXapiCaller().setCall("approvewfreq", getWebServiceObjectString(wFViewRequestData));
                workflowWebService.saveData(wFViewRequestData);
                getXapiCaller().setCall("approvewfres", "approved");

            }
            return true;
        }
        catch (Exception ex)
        {
            getXapiCaller().logException(ex);
        }
        return false;
    }

    public boolean createSTO(TXRequestMOB tXRequest, StandingOrderRequest standingOrderRequest)
    {
        try
        {

            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
            Date expiredate, nextTfrDate;

            expiredate = sdf.parse(standingOrderRequest.getExpiryTfrDate());
            nextTfrDate = sdf.parse(standingOrderRequest.getNextTfrDate());

            Calendar expireDateCal = Calendar.getInstance();
            Calendar nxtTfrDateCal = Calendar.getInstance();
            Calendar cExpiryDate = Calendar.getInstance();

            expireDateCal.setTime(expiredate);
            nxtTfrDateCal.setTime(nextTfrDate);
            cExpiryDate.setTime(expiredate);

            switch (standingOrderRequest.getTfrTerm())
            {
                case "D":
                    expireDateCal.add(Calendar.DATE, standingOrderRequest.getTfrFreq().intValue());
                    break;
                case "M":
                    expireDateCal.add(Calendar.MONTH, standingOrderRequest.getTfrFreq().intValue());
                    break;
                case "Y":
                    expireDateCal.add(Calendar.YEAR, standingOrderRequest.getTfrFreq().intValue());
                    break;
            }
            InternalStandingOrderRequestData standingOrderRequestData = (InternalStandingOrderRequestData) getBaseRequest(new InternalStandingOrderRequestData(), tXRequest);
            standingOrderRequestData.setChannelId(PHController.posChannelID);
            standingOrderRequestData.setChannelCode(PHController.posChannelCode);

            standingOrderRequestData.setTransmissionTime(System.currentTimeMillis());
            standingOrderRequestData.setAccountNumber(standingOrderRequest.getFromAccountNumber());

            standingOrderRequestData.setDestinationAccountno(standingOrderRequest.getToAccountNumber());
            standingOrderRequestData.setNoOfPayments(standingOrderRequest.getNoOfPayments());

            standingOrderRequestData.setNextTransferDate(nxtTfrDateCal);
            standingOrderRequestData.setReference(standingOrderRequest.getReference() + "-" + (standingOrderRequest.getFromAccountNumber()));

            standingOrderRequestData.setAmount(standingOrderRequest.getTfrAmount());
            standingOrderRequestData.setPaymentType("INTERNAL");

            standingOrderRequestData.setRepaymentFrequencyUnit(standingOrderRequest.getTfrTerm());
            standingOrderRequestData.setRepaymentFrequencyValue(standingOrderRequest.getTfrFreq());

            standingOrderRequestData.setNonBusinessDueDateOption("RFWD");
            standingOrderRequestData.setCurrencyCode(tXRequest.getCurrencyCode());

            standingOrderRequestData.setExpiryDate(expireDateCal);
            standingOrderRequestData.setInsufficientFundOption("SKIP");

            standingOrderRequestData.setTransferReasonId(483L);

            standingOrderRequestData.setDescription(tXRequest.getTxnNarration());
            standingOrderRequestData.setStandingOrderType("REG");

            standingOrderRequestData.setSupplimentaryReference(standingOrderRequest.getReference());
            standingOrderRequestData.setUserLoginId(PHController.defaultSystemUser);

            standingOrderRequestData.setUserRoleId(PHController.defaultBURole);
            standingOrderRequestData.setCurrBUId(tXRequest.getBuId());

            getXapiCaller().setCall("crtstoreq", getWebServiceObjectString(standingOrderRequestData));
            transferWebService.createInternalStandingOrder(standingOrderRequestData);
            // getWebServiceObjectString(tDClient)
            gettDClient().updateChequeWF(gettXRequest().getBuId(), standingOrderRequestData.getReference());
            getXapiCaller().setCall("crtstoeres", "successful");

            return true;
        }
        catch (RemoteException | XAPIException | ParseException ex)
        {
            System.err.println("error " + ex);
            getXapiCaller().logException(ex);
            return false;
        }
    }

    public boolean closeSTO(TXRequestMOB tXRequest, StandingOrderRequest standingOrderRequest)
    {
        try
        {

            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
            Date expiredate, nextTfrDate;

            expiredate = sdf.parse(standingOrderRequest.getExpiryTfrDate());
            nextTfrDate = sdf.parse(standingOrderRequest.getNextTfrDate());

            Calendar expireDateCal = Calendar.getInstance();
            Calendar nxtTfrDateCal = Calendar.getInstance();
            Calendar cExpiryDate = Calendar.getInstance();

            expireDateCal.setTime(expiredate);
            nxtTfrDateCal.setTime(nextTfrDate);
            cExpiryDate.setTime(expiredate);

            AccountRequest standingOrderRequestData = (AccountRequest) getBaseRequest(new AccountRequest(), tXRequest);
            standingOrderRequestData.setChannelId(PHController.posChannelID);
            standingOrderRequestData.setChannelCode(PHController.posChannelCode);

            standingOrderRequestData.setTransmissionTime(System.currentTimeMillis());
            standingOrderRequestData.setAccountNumber(standingOrderRequest.getFromAccountNumber());

            standingOrderRequestData.setUserLoginId(PHController.defaultSystemUser);

            standingOrderRequestData.setUserRoleId(PHController.defaultBURole);
            standingOrderRequestData.setCurrBUId(tXRequest.getBuId());

            getXapiCaller().setCall("crtstoreq", getWebServiceObjectString(standingOrderRequestData));
            transferWebService.closeStandingOrders(standingOrderRequestData);
            // getWebServiceObjectString(tDClient)
            gettDClient().updateChequeWF(gettXRequest().getBuId(), standingOrderRequestData.getReference());
            getXapiCaller().setCall("crtstoeres", "successful");

            return true;
        }
        catch (RemoteException | XAPIException | ParseException ex)
        {
            System.err.println("error " + ex);
            getXapiCaller().logException(ex);
            return false;
        }
    }

    public Object createCASA(TXRequestMOB tXRequest, CASARequest cASARequest)
    {
        try
        {
            DepositAccountRequestData depositAccountRequestData = (DepositAccountRequestData) getBaseRequest(new DepositAccountRequestData(), tXRequest);
            depositAccountRequestData.setChannelId(PHController.mobChannelID);

            depositAccountRequestData.setChannelCode(PHController.mobChannelCode);
            depositAccountRequestData.setTransmissionTime(System.currentTimeMillis());

            depositAccountRequestData.setUserId(PHController.defaultBURole);
            depositAccountRequestData.setAccountTitle(cASARequest.getAccountName());

            depositAccountRequestData.setCampaignRefId(324L);
            depositAccountRequestData.setOpenningReasonId(491L);

            depositAccountRequestData.setPrimaryCustomerId(gettDClient().queryCustomerbyID(cASARequest.getAcessCode()));
            depositAccountRequestData.setRiskClassId(596L);

            depositAccountRequestData.setProductCode(cASARequest.getProductCode());
            depositAccountRequestData.setRelationshipOfficerId(-99L); //to be changed

            depositAccountRequestData.setSysUserId(841L);
            /*system*/

            depositAccountRequestData.setSourceOfFundId(283L);

            depositAccountRequestData.setStrOpeningDate(cASARequest.getStartDate());
            depositAccountRequestData.setUserLoginId(PHController.defaultSystemUser);

            depositAccountRequestData.setUserRoleId(PHController.defaultBURole);
            depositAccountRequestData.setCurrBUId(tXRequest.getBuId());

            System.err.println(gettDClient().queryCustomerbyID(cASARequest.getAcessCode()) + " >>>>>>>>>>>>>>>>>>>>> " + getWebServiceObjectString(depositAccountRequestData));
            getXapiCaller().setCall("crtcasareq", getWebServiceObjectString(depositAccountRequestData));
            DepositAccountOutputData casaResponse = accountWebService.createDepositAccount(depositAccountRequestData);
            getXapiCaller().setCall("crtcasares", getWebServiceObjectString(casaResponse));
            System.err.println(">>>>>>>>>>>>>>>>>>>>> " + getWebServiceObjectString(casaResponse));

            return casaResponse;
        }
        catch (Exception ex)
        {
            System.err.println("error " + ex);
            //   gettXProcessor().getXapiCaller().logException(ex);
            return ex;
        }
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
            gettDClient().logError(ex);
        }
        return errorCode;
    }

    public void windUp()
    {
        logInfo(getXapiCaller());
    }

    public String getWebServiceObjectString(Object wsObject)
    {
        String wsObjectStr = "";
        wsObjectStr = ReflectionToStringBuilder.toString(wsObject, ToStringStyle.SHORT_PREFIX_STYLE);
        if (wsObjectStr.equals(""))
        {
            wsObjectStr = wsObject.toString();
        }
        return wsObjectStr;
    }

    public static String getTagValue(String xml, String tagName)
    {
        return xml != null ? (xml.contains("<" + tagName + ">") ? xml.split("<" + tagName + ">")[1].split("</" + tagName + ">")[0] : null) : null;
    }

    private void logError(Exception e)
    {
        if (PHMain.mobBridgeLog != null)
        {
            PHMain.mobBridgeLog.error(e);
        }
        else
        {
            e.printStackTrace();
        }
    }

    private void logInfo(Object info)
    {
        if (PHMain.mobBridgeLog != null)
        {
            PHMain.mobBridgeLog.info(info);
        }
        else
        {
            System.out.println(info);
        }
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
}
