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
public class CASARequest
{

    @XmlElement
    private String authencationCode;
    @XmlElement
    private String acessCode;
    @XmlElement
    private String idNumber;
    @XmlElement
    private String accountName;
    @XmlElement
    private String ProductCode;   
    @XmlElement
    private String startDate;

    public CASARequest()
    {
    }

    /**
     * @return the accountName
     */
    public String getAccountName()
    {
        return accountName;
    }

    /**
     * @param accountName the accountName to set
     */
    public void setAccountName(String accountName)
    {
        this.accountName = accountName;
    }

    /**
     * @return the ProductCode
     */
    public String getProductCode()
    {
        return ProductCode;
    }

    /**
     * @param ProductCode the ProductCode to set
     */
    public void setProductCode(String ProductCode)
    {
        this.ProductCode = ProductCode;
    }

    /**
     * @return the startDate
     */
    public String getStartDate()
    {
        return startDate;
    }

    /**
     * @param startDate the startDate to set
     */
    public void setStartDate(String startDate)
    {
        this.startDate = startDate;
    }

    /**
     * @return the acessCode
     */
    public String getAcessCode()
    {
        return acessCode;
    }

    /**
     * @param acessCode the acessCode to set
     */
    public void setAcessCode(String acessCode)
    {
        this.acessCode = acessCode;
    }

    /**
     * @return the authencationCode
     */
    public String getAuthencationCode()
    {
        return authencationCode;
    }

    /**
     * @param authencationCode the authencationCode to set
     */
    public void setAuthencationCode(String authencationCode)
    {
        this.authencationCode = authencationCode;
    }

    /**
     * @return the idNumber
     */
    public String getIdNumber()
    {
        return idNumber;
    }

    /**
     * @param idNumber the idNumber to set
     */
    public void setIdNumber(String idNumber)
    {
        this.idNumber = idNumber;
    }

}
