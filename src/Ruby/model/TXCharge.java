/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Ruby.model;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 *
 * @author Pecherk
 */
public class TXCharge implements Serializable
{
    private String taxLedger;
    private String chargeLedger;
    private String taxNarration;
    private String channelLedger;
    private String chargeNarration;
    private BigDecimal taxAmount = BigDecimal.ZERO;
    private CNAccount chargeAccount = new CNAccount();
    private BigDecimal chargeAmount = BigDecimal.ZERO;

    /**
     * @return the chargeLedger
     */
    public String getChargeLedger()
    {
        return chargeLedger;
    }

    /**
     * @param chargeLedger the chargeLedger to set
     */
    public void setChargeLedger(String chargeLedger)
    {
        this.chargeLedger = chargeLedger;
    }

    /**
     * @return the taxNarration
     */
    public String getTaxNarration()
    {
        return taxNarration;
    }

    /**
     * @param taxNarration the taxNarration to set
     */
    public void setTaxNarration(String taxNarration)
    {
        this.taxNarration = taxNarration;
    }

    /**
     * @return the channelLedger
     */
    public String getChannelLedger()
    {
        return channelLedger;
    }

    /**
     * @param channelLedger the channelLedger to set
     */
    public void setChannelLedger(String channelLedger)
    {
        this.channelLedger = channelLedger;
    }

    /**
     * @return the chargeNarration
     */
    public String getChargeNarration()
    {
        return chargeNarration;
    }

    /**
     * @param chargeNarration the chargeNarration to set
     */
    public void setChargeNarration(String chargeNarration)
    {
        this.chargeNarration = chargeNarration;
    }

    /**
     * @return the taxLedger
     */
    public String getTaxLedger()
    {
        return taxLedger;
    }

    /**
     * @param taxLedger the taxLedger to set
     */
    public void setTaxLedger(String taxLedger)
    {
        this.taxLedger = taxLedger;
    }

    /**
     * @return the taxAmount
     */
    public BigDecimal getTaxAmount()
    {
        return taxAmount;
    }

    /**
     * @param taxAmount the taxAmount to set
     */
    public void setTaxAmount(BigDecimal taxAmount)
    {
        this.taxAmount = taxAmount;
    }

    /**
     * @return the chargeAccount
     */
    public CNAccount getChargeAccount()
    {
        return chargeAccount;
    }

    /**
     * @param chargeAccount the chargeAccount to set
     */
    public void setChargeAccount(CNAccount chargeAccount)
    {
        this.chargeAccount = chargeAccount;
    }

    /**
     * @return the chargeAmount
     */
    public BigDecimal getChargeAmount()
    {
        return chargeAmount;
    }

    /**
     * @param chargeAmount the chargeAmount to set
     */
    public void setChargeAmount(BigDecimal chargeAmount)
    {
        this.chargeAmount = chargeAmount;
    }
}
