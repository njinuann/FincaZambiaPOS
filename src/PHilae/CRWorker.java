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
import com.neptunesoftware.supernova.ws.client.ChannelAdminWebService;
import com.neptunesoftware.supernova.ws.client.ChannelAdminWebServiceEndPointPort_Impl;
import com.neptunesoftware.supernova.ws.client.CustomerWebService;
import com.neptunesoftware.supernova.ws.client.CustomerWebServiceEndPointPort_Impl;
import com.neptunesoftware.supernova.ws.common.XAPIException;
import com.neptunesoftware.supernova.ws.common.XAPIRequestBaseObject;
import com.neptunesoftware.supernova.ws.server.channeladmin.data.CustomerChannelCreationData;
import com.neptunesoftware.supernova.ws.server.customer.data.CustomerImageOutputData;
import com.neptunesoftware.supernova.ws.server.customer.data.CustomerImageRequestData;
import java.io.File;
import java.io.FileInputStream;
import java.rmi.RemoteException;
import java.util.Arrays;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

/**
 *
 * @author Pecherk
 */
public class CRWorker implements Runnable
{

    private final TDClient tDClient = new TDClient();
    private ChannelAdminWebService channelAdminWebService;
    CustomerWebService customerWebService = null;

    public CRWorker()
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
                    customerWebService = new CustomerWebServiceEndPointPort_Impl(((CBNode) cBNode).getWsContextURL() + "CustomerWebServiceBean?wsdl").getCustomerWebServiceSoapPort(PHController.XapiUser, PHController.XapiPassword);

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
                if ("Y".equalsIgnoreCase(PHController.AutoEnrollPosNewAccounts))
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
                Thread.sleep(PHController.posAccountEnrolmentIntervalMM * 60000);
            }
            catch (Exception ex)
            {
                logError(ex);
            }
        }
    }

    public byte[] getFileBytes(String fileUrl)
    {
        File file = new File(fileUrl);
        byte[] fileBytes = new byte[(int) file.length()];
        try (FileInputStream fin = new FileInputStream(file))
        {
            fin.read(fileBytes);
        }
        catch (Exception ex)
        {
            logError(ex);
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

    private void enrollNewAccounts()
    {
        gettDClient().deleteInvalidChannelUsers();
        //System.err.println("done remove invalid");
        gettDClient().queryUnenrolledAccounts().stream().forEach((cNAccount)
                ->
        {
            enrollAccount(cNAccount);
        });
    }

    private XAPIRequestBaseObject getBaseRequest(XAPIRequestBaseObject requestData)
    {
        checkConnection();
        requestData.setChannelId(PHController.posChannelID);
        requestData.setChannelCode(PHController.posChannelCode);

        requestData.setCardNumber(PHController.posChannelCode);
        requestData.setTransmissionTime(System.currentTimeMillis());

        requestData.setOriginatorUserId(PHController.SystemUserID);
        requestData.setTerminalNumber(PHController.posChannelCode);

        requestData.setReference(String.valueOf(System.currentTimeMillis()));
        return requestData;
    }

    private void enrollAccount(CNAccount cNAccount)
    {
        CRCaller cRCaller = new CRCaller();
        long startTime = System.currentTimeMillis();
        cRCaller.setNarration("POS Account Linking");
        try
        {

            String custCat = gettDClient().queryCustomerById(cNAccount.getCustId()).getCustCat();
            String cardNumber;
            if (custCat.equals("PER"))
            {
                cardNumber = StringUtils.leftPad(gettDClient().queryCustomerById(cNAccount.getCustId()).getIdNo(), 16, "0");
            }
            else
            {
                cardNumber = StringUtils.leftPad(gettDClient().queryCustomerById(cNAccount.getCustId()).getCustNo(), 16, "0");
            }
            CNUser cNUser = gettDClient().queryCNUser(cardNumber);

            cRCaller.setCardNumber(cardNumber);
            cRCaller.setAccountNo(cNAccount.getAccountNumber());

            if (cardNumber.equals(cNUser.getAccessCode())
                    && cNUser.getCustId() == cNAccount.getCustId()
                    && !gettDClient().isAccountEnrolled(cNUser.getCustChannelId(), cNAccount.getAcctId()))
            {
                gettDClient().saveChannelAccount(cNUser, cNAccount);
                cRCaller.setXapiRespCode(EICodes.XAPI_APPROVED);
            }
            else
            {
                CustomerChannelCreationData customerChannelCreationData = new CustomerChannelCreationData();
                customerChannelCreationData = (CustomerChannelCreationData) getBaseRequest(customerChannelCreationData);

                customerChannelCreationData.setAccessCode(cardNumber);
                customerChannelCreationData.setXAPIServiceCode("CHN013");

                customerChannelCreationData.setAccountNo(cNAccount.getAccountNumber());
                customerChannelCreationData.setCustChannelSchemeId(PHController.posChannelSchemeID);

                cRCaller.setCall("usrenrlreq", customerChannelCreationData);
                CustomerChannelCreationData response = channelAdminWebService.createAndConfigureCustomerChannelUser(customerChannelCreationData);

                cRCaller.setCall("usrenrlres", response);
                gettDClient().updateAccessName(cNAccount.getAccountName(), cardNumber);

                gettDClient().updateChannelAccount(cNAccount);
                cRCaller.setXapiRespCode(EICodes.XAPI_APPROVED);
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

    public boolean addBiometricImage(String customerNumber, String fingerType, String finData, byte[] imageBytes, XAPICaller xAPICaller)
    {
        //CRCaller cRCaller = new CRCaller();

        try
        {
            CustomerImageRequestData customerImageRequestData = new CustomerImageRequestData();
            CustomerImageRequestData request = (CustomerImageRequestData) getBaseRequest(customerImageRequestData);;
            request.setChannelId(PHController.posChannelID);

//            request.setChannelCode(PHController.posChannelCode);
//            request.setOriginatorUserId(PHController.SystemUserID);
//            request.setTransmissionTime(System.currentTimeMillis());
            request.setUserId(-99L);
            request.setUserBusinessRoleId(-99L);
            request.setCustomerNumber(customerNumber);
            request.setBinaryImage(imageBytes);
            request.setBiometricImage(getByteArray("0000000000000000" + finData));
            request.setCustomerImageId(gettDClient().getEntity("CUSTOMER_IMAGE"));
            request.setImageTypeCode(fingerType);
            xAPICaller.setCall("FinsyncRequest", request);

            CustomerImageOutputData response = customerWebService.addCustomerImage(request);
            xAPICaller.setCall("Finsyncres", response);
            gettDClient().updateNextEntityId("CUSTOMER_IIMAGE", "CUST_IMAGE_ID");
            // CustomerImageOutputData response = webService.addCustomerImage(request);
            return true;
        }
        catch (Exception ex)
        {
            xAPICaller.setXapiRespCode(getXapiErrorCode(ex));
            xAPICaller.logException(ex);
        }
        return false;
    }

    public String getXapiErrorCode(Exception ex)
    {
        String errorCode = EICodes.SYSTEM_ERROR;
        if (ex instanceof XAPIException)
        {
            if (((XAPIException) ex).getErrorCodes() != null)
            {
                errorCode = ((XAPIException) ex).getErrorCodes().length >= 1 ? ((XAPIException) ex).getErrorCodes()[0] : errorCode;
            }
            if (((XAPIException) ex).getErrors() != null && EICodes.SYSTEM_ERROR.equals(errorCode))
            {
                errorCode = ((XAPIException) ex).getErrors().length >= 1 ? ((XAPIException) ex).getErrors()[0].getErrorCode() : errorCode;
            }
        }
        return errorCode;
    }

    private void logError(Exception e)
    {
        if (PHMain.posBridgeLog != null)
        {
            PHMain.posBridgeLog.error(e);
        }
        else
        {
            e.printStackTrace();
        }
    }

    private void logInfo(Object e)
    {
        if (PHMain.posBridgeLog != null)
        {
            PHMain.posBridgeLog.info(e);
        }
        else
        {
            System.out.println("Event " + e);
        }
    }

    /**
     * @return the tDClient
     */
    public TDClient gettDClient()
    {
        return tDClient;
    }
}
