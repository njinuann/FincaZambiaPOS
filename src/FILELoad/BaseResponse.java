/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package FILELoad;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 *
 * @author NJINU
 */
//@XmlType(factoryMethod="newInstance")
@XmlAccessorType(XmlAccessType.FIELD)
public class BaseResponse
{

    @XmlElement
    private String responseCode;
    @XmlElement
    private String responseString;

    
  public BaseResponse(){}
    /**
     * @return the responseCode
     */
    public String getResponseCode()
    {
        return responseCode;
    }

    /**
     * @param responseCode the responseCode to set
     */
    public void setResponseCode(String responseCode)
    {
        this.responseCode = responseCode;
    }

    /**
     * @return the responseString
     */
    public String getResponseString()
    {
        return responseString;
    }

    /**
     * @param responseString the responseString to set
     */
    public void setResponseString(String responseString)
    {
        this.responseString = responseString;
    }
}
