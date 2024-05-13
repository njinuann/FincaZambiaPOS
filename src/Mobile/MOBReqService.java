/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Mobile;

import APX.PHController;
import com.sun.net.httpserver.HttpServer;
//import com.sun.xml.internal.ws.spi.ProviderImpl;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.ws.Endpoint;

/**
 *
 * @author NJINU
 */
public class MOBReqService
{

    HttpServer httpServer;
    Endpoint endpoint = null;
    private TDClientMOB tDClient = new TDClientMOB();

    public void startSoap()
    {
        try
        {
            httpServer = HttpServer.create(new InetSocketAddress(PHController.servicePort), 100);
            httpServer.setExecutor(new ThreadPoolExecutor(2, 100, 2000, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(100)));

            new com.sun.xml.ws.spi.ProviderImpl().createEndpoint(null, new MOBSvcProcessor()).publish(httpServer.createContext("/ruby/MOBReqService"));
            httpServer.start();

            gettDClient().logInfo("***********Service started***********");
        }
        catch (IOException ex)
        {
            Logger.getLogger(MOBReqService.class.getName()).log(Level.SEVERE, null, ex);
            gettDClient().logError(ex);
        }
    }

    public void stop() throws Exception
    {
        if (endpoint != null)
        {
            endpoint.stop();
        }
        httpServer.stop(5);
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
}
