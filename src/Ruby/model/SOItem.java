/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Ruby.model;

import Mobile.CNAccountMOB;
import java.math.BigDecimal;

/**
 *
 * @author Pecherk
 */
public class SOItem
{
    private Long orderId;
    private String currency;
    private String reference;
    private String narration;
    private String period;
    private BigDecimal amount = BigDecimal.ZERO;
    private CNAccountMOB sourceAccount = new CNAccountMOB();
    private CNAccountMOB beneficiaryAccount = new CNAccountMOB();
    private String status;

    /**
     * @return the orderId
     */
    public Long getOrderId()
    {
        return orderId;
    }

    /**
     * @param orderId the orderId to set
     */
    public void setOrderId(Long orderId)
    {
        this.orderId = orderId;
    }

    /**
     * @return the narration
     */
    public String getNarration()
    {
        return narration;
    }

    /**
     * @param narration the narration to set
     */
    public void setNarration(String narration)
    {
        this.narration = narration;
    }

    /**
     * @return the amount
     */
    public BigDecimal getAmount()
    {
        return amount;
    }

    /**
     * @param amount the amount to set
     */
    public void setAmount(BigDecimal amount)
    {
        this.amount = amount;
    }

    /**
     * @return the sourceAccount
     */
    public CNAccountMOB getSourceAccount()
    {
        return sourceAccount;
    }

    /**
     * @param sourceAccount the sourceAccount to set
     */
    public void setSourceAccount(CNAccountMOB sourceAccount)
    {
        this.sourceAccount = sourceAccount;
    }

    /**
     * @return the beneficiaryAccount
     */
    public CNAccountMOB getBeneficiaryAccount()
    {
        return beneficiaryAccount;
    }

    /**
     * @param beneficiaryAccount the beneficiaryAccount to set
     */
    public void setBeneficiaryAccount(CNAccountMOB beneficiaryAccount)
    {
        this.beneficiaryAccount = beneficiaryAccount;
    }

    /**
     * @return the status
     */
    public String getStatus()
    {
        return status;
    }

    /**
     * @param status the status to set
     */
    public void setStatus(String status)
    {
        this.status = status;
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
     * @return the currency
     */
    public String getCurrency()
    {
        return currency;
    }

    /**
     * @param currency the currency to set
     */
    public void setCurrency(String currency)
    {
        this.currency = currency;
    }

    /**
     * @return the period
     */
    public String getPeriod()
    {
        return period;
    }

    /**
     * @param period the period to set
     */
    public void setPeriod(String period)
    {
        switch (period)
        {
            case "D":
                this.period = "Day";
                break;
            case "W":
                this.period = "Week";
                break;
            case "M":
                this.period = "Month";
                break;
            case "Y":
                this.period = "Year";
                break;
            default:
                this.period = period;
        }
    }
}
