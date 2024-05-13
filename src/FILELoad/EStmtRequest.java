/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package FILELoad;

import java.util.Date;
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
public class EStmtRequest
{

    @XmlElement
    private String authencationCode;
    @XmlElement
    private String accountNo;
    @XmlElement
    private String accountType;
    @XmlElement
    private String mobileNumber;
    @XmlElement
    private String fromDate;
    @XmlElement
    private String toDate;

    public EStmtRequest()
    {
    }

    /**
     * @return the accountNo
     */
    public String getAccountNo()
    {
        return accountNo;
    }

    /**
     * @param accountNo the accountNo to set
     */
    public void setAccountNo(String accountNo)
    {
        this.accountNo = accountNo;
    }

    /**
     * @return the accountType
     */
    public String getAccountType()
    {
        return accountType;
    }

    /**
     * @param accountType the accountType to set
     */
    public void setAccountType(String accountType)
    {
        this.accountType = accountType;
    }

    /**
     * @return the fromDate
     */
    public String getFromDate()
    {
        return fromDate;
    }

    /**
     * @param fromDate the fromDate to set
     */
    public void setFromDate(String fromDate)
    {
        this.fromDate = fromDate;
    }

    /**
     * @return the toDate
     */
    public String getToDate()
    {
        return toDate;
    }

    /**
     * @param toDate the toDate to set
     */
    public void setToDate(String toDate)
    {
        this.toDate = toDate;
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

}
