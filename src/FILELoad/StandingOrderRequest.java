/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package FILELoad;

import java.math.BigDecimal;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 *
 * @author NJINU
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "standingOrder")
public class StandingOrderRequest
{

     @XmlElement
    private String authencationCode;
    @XmlElement
    private String mobileNumber;
    @XmlElement
    private String accessCode;
    @XmlElement
    private String IdNumber;
    @XmlElement
    private String fromAccountNumber;
    @XmlElement
    private String toAccountNumber;    
    @XmlElement
    private Long NoOfPayments;
    @XmlElement
    private String nextTfrDate;
    @XmlElement
     private String expiryTfrDate;
    @XmlElement
    private BigDecimal tfrAmount;
    @XmlElement
    private Long tfrFreq;
    @XmlElement
    private String tfrTerm;
    @XmlElement
    private String reference;

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
     * @return the accessCode
     */
    public String getAccessCode()
    {
        return accessCode;
    }

    /**
     * @param accessCode the accessCode to set
     */
    public void setAccessCode(String accessCode)
    {
        this.accessCode = accessCode;
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
     * @return the fromAccountNumber
     */
    public String getFromAccountNumber()
    {
        return fromAccountNumber;
    }

    /**
     * @param fromAccountNumber the fromAccountNumber to set
     */
    public void setFromAccountNumber(String fromAccountNumber)
    {
        this.fromAccountNumber = fromAccountNumber;
    }

    /**
     * @return the toAccountNumber
     */
    public String getToAccountNumber()
    {
        return toAccountNumber;
    }

    /**
     * @param toAccountNumber the toAccountNumber to set
     */
    public void setToAccountNumber(String toAccountNumber)
    {
        this.toAccountNumber = toAccountNumber;
    }

    /**
     * @return the NoOfPayments
     */
    public Long getNoOfPayments()
    {
        return NoOfPayments;
    }

    /**
     * @param NoOfPayments the NoOfPayments to set
     */
    public void setNoOfPayments(Long NoOfPayments)
    {
        this.NoOfPayments = NoOfPayments;
    }

    /**
     * @return the nextTfrDate
     */
    public String getNextTfrDate()
    {
        return nextTfrDate;
    }

    /**
     * @param nextTfrDate the nextTfrDate to set
     */
    public void setNextTfrDate(String nextTfrDate)
    {
        this.nextTfrDate = nextTfrDate;
    }

    /**
     * @return the tfrAmount
     */
    public BigDecimal getTfrAmount()
    {
        return tfrAmount;
    }

    /**
     * @param tfrAmount the tfrAmount to set
     */
    public void setTfrAmount(BigDecimal tfrAmount)
    {
        this.tfrAmount = tfrAmount;
    }

    /**
     * @return the reference
     */
    public String getReference()
    {
        return reference;
    }

    /**
     * @param reference the reference to set
     */
    public void setReference(String reference)
    {
        this.reference = reference;
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
     * @return the tfrFreq
     */
    public Long getTfrFreq()
    {
        return tfrFreq;
    }

    /**
     * @param tfrFreq the tfrFreq to set
     */
    public void setTfrFreq(Long tfrFreq)
    {
        this.tfrFreq = tfrFreq;
    }

    /**
     * @return the tfrTerm
     */
    public String getTfrTerm()
    {
        return tfrTerm;
    }

    /**
     * @param tfrTerm the tfrTerm to set
     */
    public void setTfrTerm(String tfrTerm)
    {
        this.tfrTerm = tfrTerm;
    }

    /**
     * @return the expiryTfrDate
     */
    public String getExpiryTfrDate()
    {
        return expiryTfrDate;
    }

    /**
     * @param expiryTfrDate the expiryTfrDate to set
     */
    public void setExpiryTfrDate(String expiryTfrDate)
    {
        this.expiryTfrDate = expiryTfrDate;
    }
}
