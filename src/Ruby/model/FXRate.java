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
public class FXRate implements Serializable
{
    private BigDecimal buyRate = BigDecimal.ZERO;
    private BigDecimal sellRate = BigDecimal.ZERO;
    private CNCurrency currency = new CNCurrency();

    /**
     * @return the buyRate
     */
    public BigDecimal getBuyRate()
    {
        return buyRate;
    }

    /**
     * @param buyRate the buyRate to set
     */
    public void setBuyRate(BigDecimal buyRate)
    {
        this.buyRate = buyRate;
    }

    /**
     * @return the sellRate
     */
    public BigDecimal getSellRate()
    {
        return sellRate;
    }

    /**
     * @param sellRate the sellRate to set
     */
    public void setSellRate(BigDecimal sellRate)
    {
        this.sellRate = sellRate;
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
}
