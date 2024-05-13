/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package PHilae;

import APX.PHController;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.HashMap;

/**
 *
 * @author Pecherk
 */
public class TCValue implements Serializable
{
    private String chargeType = "Constant";
    private BigDecimal minAmount = BigDecimal.ZERO;
    private BigDecimal maxAmount = BigDecimal.ZERO;
    private BigDecimal chargeValue = BigDecimal.ZERO;
    private String currency = PHController.PrimaryCurrencyCode;
    private HashMap<BigDecimal, TCTier> tiers = new HashMap<>();

    /**
     * @return the chargeType
     */
    public String getChargeType()
    {
        return chargeType;
    }

    /**
     * @param chargeType the chargeType to set
     */
    public void setChargeType(String chargeType)
    {
        this.chargeType = chargeType;
    }

    /**
     * @return the chargeValue
     */
    public BigDecimal getChargeValue()
    {
        return chargeValue;
    }

    /**
     * @param chargeValue the chargeValue to set
     */
    public void setChargeValue(BigDecimal chargeValue)
    {
        this.chargeValue = chargeValue;
    }

    /**
     * @return the minAmount
     */
    public BigDecimal getMinAmount()
    {
        return minAmount;
    }

    /**
     * @param minAmount the minAmount to set
     */
    public void setMinAmount(BigDecimal minAmount)
    {
        this.minAmount = minAmount;
    }

    /**
     * @return the maxAmount
     */
    public BigDecimal getMaxAmount()
    {
        return maxAmount;
    }

    /**
     * @param maxAmount the maxAmount to set
     */
    public void setMaxAmount(BigDecimal maxAmount)
    {
        this.maxAmount = maxAmount;
    }

    /**
     * @return the tiers
     */
    public HashMap<BigDecimal, TCTier> getTiers()
    {
        return tiers;
    }

    /**
     * @param tiers the tiers to set
     */
    public void setTiers(HashMap<BigDecimal, TCTier> tiers)
    {
        this.tiers = tiers;
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
}
