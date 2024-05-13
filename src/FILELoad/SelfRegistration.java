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
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "selfRegistration")
public class SelfRegistration
{

    @XmlElement
    private String authencationCode;
    @XmlElement
    private String mobileNumber;
    @XmlElement
    private String accessNumber;
    @XmlElement
    private String accountNumber;
    @XmlElement
    private String IdNumber;

    /**
     * @return the mobileNumber
     */
    public String getMobileNumber()
    {
        return mobileNumber;
    }

    /**
     * @param mobileNumber the mobileNumber to set
     */
    public void setMobileNumber(String mobileNumber)
    {
        this.mobileNumber = mobileNumber;
    }

    /**
     * @return the accessNumber
     */
    public String getAccessNumber()
    {
        return accessNumber;
    }

    /**
     * @param accessNumber the accessNumber to set
     */
    public void setAccessNumber(String accessNumber)
    {
        this.accessNumber = accessNumber;
    }

    /**
     * @return the accountNumber
     */
    public String getAccountNumber()
    {
        return accountNumber;
    }

    /**
     * @param accountNumber the accountNumber to set
     */
    public void setAccountNumber(String accountNumber)
    {
        this.accountNumber = accountNumber;
    }

    /**
     * @return the IdNumber
     */
    public String getIdNumber()
    {
        return IdNumber;
    }

    /**
     * @param IdNumber the IdNumber to set
     */
    public void setIdNumber(String IdNumber)
    {
        this.IdNumber = IdNumber;
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
}
