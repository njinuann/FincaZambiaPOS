/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Mobile;

import javax.jws.WebService;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import FILELoad.BaseResponse;
import FILELoad.CASARequest;
import FILELoad.EStmtRequest;
import FILELoad.SelfRegistration;
import FILELoad.StandingOrderRequest;

/**
 *
 * @author NJINU
 */
@WebService(serviceName = "MOBSvcProcessor")
public class MOBSvcProcessor
{

    private TDClientMOB tDClient = new TDClientMOB();
    private EIWServiceProcessor serviceProcessor = new EIWServiceProcessor(); 

    /**
     * Web service operation
     *
     * @param eStmtRequest
     * @return
     */
    @WebMethod(operationName = "generateStatement")
    public BaseResponse generateStatement(@WebParam(name = "generateStatement") EStmtRequest eStmtRequest)
    {
        BaseResponse baseResponse = new BaseResponse();
        try
        {
            baseResponse = serviceProcessor.generateStatement(eStmtRequest);

        }
        catch (Exception x)
        {
            tDClient.logError(x);
            throw new IllegalStateException(x);

        }
        return baseResponse;
    }

    /**
     * Web service operation
     *
     * @param cASARequest
     * @return
     */
    @WebMethod(operationName = "createCasa")
    public BaseResponse createCasa(@WebParam(name = "createCasa") CASARequest cASARequest)
    {        
        BaseResponse baseResponse = new BaseResponse();
        try
        {
            baseResponse = serviceProcessor.createCASA(cASARequest);

        }
        catch (Exception x)
        {
            tDClient.logError(x);
            throw new IllegalStateException(x);

        }
        return baseResponse;
    }

    
    /**
     * Web service operation
     *
     * @param selfRegistration
     * @return
     */
    @WebMethod(operationName = "selfRegistrationService")
    public BaseResponse selfRegistrationService(@WebParam(name = "selfRegistrationService")  SelfRegistration selfRegistration)
    {        
        BaseResponse baseResponse = new BaseResponse();
        try
        {
            baseResponse = serviceProcessor.serviceSelfRegistration(selfRegistration);

        }
        catch (Exception x)
        {
            tDClient.logError(x);
            throw new IllegalStateException(x);

        }
        return baseResponse;
    }
    
       /**
     * Web service operation
     *
     * @param standingOrderRequest
     * @return
     */
    @WebMethod(operationName = "createStandingOrder")
    public BaseResponse createStandingOrder(@WebParam(name = "createStandingOrder")  StandingOrderRequest standingOrderRequest)
    {       
        BaseResponse baseResponse = new BaseResponse();
        try
        {
            baseResponse = serviceProcessor.createStandingOrders(standingOrderRequest);
        }
        catch (Exception x)
        {
            tDClient.logError(x);
            throw new IllegalStateException(x);

        }
        return baseResponse;
    }
}
