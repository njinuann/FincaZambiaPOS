/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package PHilae;

import java.io.Serializable;

/**
 *
 * @author Pecherk
 */
public class CNAccount implements Serializable
{
    private long buId;
    private long custId;
    private long acctId;
    private long productId;
    private String shortName;
    private String accountNumber;
    private String accountType;
    private String accountName;
    private String acctStatus;
    private CNCurrency currency = new CNCurrency();

    /**
     * @return the acctId
     */
    public long getAcctId()
    {
        return acctId;
    }

    /**
     * @param acctId the acctId to set
     */
    public void setAcctId(long acctId)
    {
        this.acctId = acctId;
    }

    /**
     * @return the shortName
     */
    public String getShortName()
    {
        return shortName;
    }

    /**
     * @param shortName the shortName to set
     */
    public void setShortName(String shortName)
    {
        this.shortName = shortName;
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
     * @return the productId
     */
    public long getProductId()
    {
        return productId;
    }

    /**
     * @param productId the productId to set
     */
    public void setProductId(long productId)
    {
        this.productId = productId;
    }

    /**
     * @return the buId
     */
    public long getBuId()
    {
        return buId;
    }

    /**
     * @param buId the buId to set
     */
    public void setBuId(long buId)
    {
        this.buId = buId;
    }

    /**
     * @return the custId
     */
    public long getCustId()
    {
        return custId;
    }

    /**
     * @param custId the custId to set
     */
    public void setCustId(long custId)
    {
        this.custId = custId;
    }

    /**
     * @return the currency
     */
    public CNCurrency getCurrency()
    {
        return currency;
    }

    /**
     * @param currency the currency to set
     */
    public void setCurrency(CNCurrency currency)
    {
        this.currency = currency;
    }

    /**
     * @return the acctStatus
     */
    public String getAcctStatus()
    {
        return acctStatus;
    }

    /**
     * @param acctStatus the acctStatus to set
     */
    public void setAcctStatus(String acctStatus)
    {
        this.acctStatus = acctStatus;
    }
}
