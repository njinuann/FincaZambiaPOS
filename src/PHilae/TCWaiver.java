/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package PHilae;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 *
 * @author Pecherk
 */
public class TCWaiver implements Serializable, Comparable<TCWaiver>
{
    private int productId;
    private BigDecimal waivedPercentage;
    private BigDecimal thresholdValue;
    private String waiverCondition = "ALL";
    private String matchAccount;

    /**
     * @return the productId
     */
    public int getProductId()
    {
        return productId;
    }

    /**
     * @param productId the productId to set
     */
    public void setProductId(int productId)
    {
        this.productId = productId;
    }

    /**
     * @return the waivedPercentage
     */
    public BigDecimal getWaivedPercentage()
    {
        return waivedPercentage;
    }

    /**
     * @param waivedPercentage the waivedPercentage to set
     */
    public void setWaivedPercentage(BigDecimal waivedPercentage)
    {
        this.waivedPercentage = waivedPercentage;
    }

    /**
     * @return the waiveCondition
     */
    public String getWaiverCondition()
    {
        return waiverCondition;
    }

    /**
     * @param waiverCondition the waiverCondition to set
     */
    public void setWaiverCondition(String waiverCondition)
    {
        this.waiverCondition = waiverCondition;
    }

    /**
     * @return the thresholdValue
     */
    public BigDecimal getThresholdValue()
    {
        return thresholdValue;
    }

    /**
     * @param thresholdValue the thresholdValue to set
     */
    public void setThresholdValue(BigDecimal thresholdValue)
    {
        this.thresholdValue = thresholdValue;
    }

    /**
     * @return the matchAccount
     */
    public String getMatchAccount()
    {
        return matchAccount;
    }

    /**
     * @param matchAccount the matchAccount to set
     */
    public void setMatchAccount(String matchAccount)
    {
        this.matchAccount = matchAccount;
    }
    
    @Override
    public int compareTo(TCWaiver o)
    {
        return Integer.valueOf(getProductId()).compareTo(o.getProductId());
    }
}
