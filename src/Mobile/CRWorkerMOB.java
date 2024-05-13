/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Mobile;

import APX.CBNode;
import APX.PHController;
import APX.PHMain;
import FILELoad.SelfRegistration;
import com.neptunesoftware.supernova.ws.client.ChannelAdminWebService;
import com.neptunesoftware.supernova.ws.client.ChannelAdminWebServiceEndPointPort_Impl;
import com.neptunesoftware.supernova.ws.common.XAPIException;
import com.neptunesoftware.supernova.ws.common.XAPIRequestBaseObject;
import com.neptunesoftware.supernova.ws.server.channeladmin.data.CustomerChannelCreationData;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;

/**
 *
 * @author Pecherk
 */
public class CRWorkerMOB implements Runnable
{

    private final TDClientMOB tDClient = new TDClientMOB();
    private ChannelAdminWebService channelAdminWebService;

    public CRWorkerMOB()
    {
        connectToCore();
    }

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
                    channelAdminWebService = new ChannelAdminWebServiceEndPointPort_Impl(((CBNode) cBNode).getWsContextURL() + "ChannelAdminWebServiceBean?wsdl").getChannelAdminWebServiceSoapPort(PHController.XapiUser, PHController.XapiPassword);
                }
                catch (Exception ex)
                {
                    logError(ex);
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
        return channelAdminWebService != null;
    }

    @Override
    public void run()
    {

        while (true)
        {
            try
            {
                if ("Y".equalsIgnoreCase(PHController.AutoEnrollMobNewAccounts))
                {
                    enrollNewAccounts();
                }
            }
            catch (Exception ex)
            {
                logError(ex);
            }
            try
            {
                Thread.sleep(PHController.mobAccountEnrolmentIntervalMM * 60000);
            }
            catch (Exception ex)
            {
                logError(ex);
            }
        }
    }

    public String formatMobileNumber(String foneNumber)
    {
        String phNumber = "";
        if (foneNumber.startsWith("+243"))
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

    private void enrollNewAccounts()
    {
        gettDClient().deleteInvalidChannelUsers();
        gettDClient().queryUnenrolledAccounts().stream().forEach((cNAccount)
                ->
                {
                    enrollAccount(cNAccount);
                });
    }

    private XAPIRequestBaseObject getBaseRequest(XAPIRequestBaseObject requestData)
    {
        checkConnection();
        requestData.setChannelId(PHController.mobChannelID);
        requestData.setChannelCode(PHController.mobChannelCode);

        requestData.setCardNumber(PHController.mobChannelCode);
        requestData.setTransmissionTime(System.currentTimeMillis());

        requestData.setOriginatorUserId(PHController.SystemUserID);
        requestData.setTerminalNumber(PHController.mobChannelCode);

        requestData.setReference(String.valueOf(System.currentTimeMillis()));
        return requestData;
    }

    private void enrollAccount(CNAccountMOB cNAccount)
    {
        CRCallerMOB cRCaller = new CRCallerMOB();
        long startTime = System.currentTimeMillis();
        cRCaller.setNarration("MOB Account Linking");
        try
        {

            String cardNumber = gettDClient().queryCustomerById(cNAccount.getCustId()).getContact();

            // String cardNumber = MOBController.BankBin + String.format("%010d", Long.parseLong(gettDClient().queryCustomerById(cNAccount.getCustId()).getCustNo()));
            CNUserMOB cNUser = gettDClient().queryCNUser(cardNumber);

            cRCaller.setCardNumber(cardNumber);
            cRCaller.setAccountNo(cNAccount.getAccountNumber());           
            if (cardNumber.equals(cNUser.getAccessCode()) && cNUser.getCustId() == cNAccount.getCustId() && !gettDClient().isAccountEnrolled(cNUser.getCustChannelId(), cNAccount.getAcctId()))
            {
                gettDClient().saveChannelAccount(cNUser, cNAccount);
                cRCaller.setXapiRespCode(EICodesMOB.XAPI_APPROVED);
            }
            else
            {
                CustomerChannelCreationData customerChannelCreationData = new CustomerChannelCreationData();
                customerChannelCreationData = (CustomerChannelCreationData) getBaseRequest(customerChannelCreationData);

                customerChannelCreationData.setAccessCode(formatMobileNumber(cardNumber));
                customerChannelCreationData.setXAPIServiceCode("CHN013");

                customerChannelCreationData.setAccountNo(cNAccount.getAccountNumber());
                customerChannelCreationData.setCustChannelSchemeId(PHController.mobChannelSchemeID);

                cRCaller.setCall("usrenrlreq", customerChannelCreationData);
                CustomerChannelCreationData response = channelAdminWebService.createAndConfigureCustomerChannelUser(customerChannelCreationData);

                cRCaller.setCall("usrenrlres", response);
                gettDClient().updateAccessName(cNAccount.getAccountName(), cardNumber);

                gettDClient().updateChannelAccount(cNAccount);
                cRCaller.setXapiRespCode(EICodesMOB.XAPI_APPROVED);
            }
        }
        catch (RemoteException | XAPIException ex)
        {
            cRCaller.setXapiRespCode(getXapiErrorCode(ex));
            cRCaller.logException(ex);
        }
        cRCaller.setDuration(String.valueOf(System.currentTimeMillis() - startTime) + " Ms");
        logInfo(cRCaller);
    }

    public boolean selfRegisterMobile(SelfRegistration selfRegistration)
    {
        CRCallerMOB cRCaller = new CRCallerMOB();
        long startTime = System.currentTimeMillis();
        boolean isRegistered = false;
        ArrayList<CNAccountMOB> accounts = gettDClient().queryUnenrolledByAccount(selfRegistration.getAccountNumber());
        for (CNAccountMOB cNAccount : accounts)
        {
            cRCaller.setNarration("Mobile Account Self registration");
            try
            {
                String cardNumber = selfRegistration.getMobileNumber();
                CNUserMOB cNUser = gettDClient().queryCNUser(cardNumber);

                cRCaller.setCardNumber(cardNumber);
                cRCaller.setAccountNo(selfRegistration.getAccountNumber());

                if (cardNumber.equals(cNUser.getAccessCode()) && cNUser.getCustId() == cNAccount.getCustId() && !gettDClient().isAccountEnrolled(cNUser.getCustChannelId(), cNAccount.getAcctId()))
                {
                    gettDClient().saveChannelAccount(cNUser, cNAccount);
                    cRCaller.setXapiRespCode(EICodesMOB.XAPI_APPROVED);
                    isRegistered = true;
                }
                else
                {
                    CustomerChannelCreationData customerChannelCreationData = new CustomerChannelCreationData();
                    customerChannelCreationData = (CustomerChannelCreationData) getBaseRequest(customerChannelCreationData);

                    customerChannelCreationData.setAccessCode(cardNumber);
                    customerChannelCreationData.setXAPIServiceCode("CHN013");

                    customerChannelCreationData.setAccountNo(cNAccount.getAccountNumber());
                    customerChannelCreationData.setCustChannelSchemeId(PHController.mobChannelSchemeID);

                    cRCaller.setCall("usrenrlreq", customerChannelCreationData);
                    CustomerChannelCreationData response = channelAdminWebService.createAndConfigureCustomerChannelUser(customerChannelCreationData);

                    cRCaller.setCall("usrenrlres", response);
                    gettDClient().updateAccessName(cNAccount.getAccountName(), cardNumber);

                    gettDClient().updateChannelAccount(cNAccount);
                    cRCaller.setXapiRespCode(EICodesMOB.XAPI_APPROVED);
                    isRegistered = true;
                }
            }
            catch (RemoteException | XAPIException ex)
            {
                cRCaller.setXapiRespCode(getXapiErrorCode(ex));
                cRCaller.logException(ex);
                isRegistered = false;
            }
            cRCaller.setDuration(String.valueOf(System.currentTimeMillis() - startTime) + " Ms");
            logInfo(cRCaller);
        }
        return isRegistered;
    }

    public String getXapiErrorCode(Exception ex)
    {
        String errorCode = EICodesMOB.SYSTEM_ERROR;
        if (ex instanceof XAPIException)
        {
            if (((XAPIException) ex).getErrorCodes() != null)
            {
                errorCode = ((XAPIException) ex).getErrorCodes().length >= 1 ? ((XAPIException) ex).getErrorCodes()[0] : errorCode;
            }
            if (((XAPIException) ex).getErrors() != null && EICodesMOB.SYSTEM_ERROR.equals(errorCode))
            {
                errorCode = ((XAPIException) ex).getErrors().length >= 1 ? ((XAPIException) ex).getErrors()[0].getErrorCode() : errorCode;
            }
        }
        return errorCode;
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
}
