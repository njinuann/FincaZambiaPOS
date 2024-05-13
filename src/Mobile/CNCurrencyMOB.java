/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Mobile;

import java.io.Serializable;

/**
 *
 * @author Pecherk
 */
public class CNCurrencyMOB implements Serializable
{
    private Long currencyId;
    private String currencyCode;
    private String currencyName;
    private int decimalPlaces = 0;

    /**
     * @return the currencyId
     */
    public Long getCurrencyId()
    {
        return currencyId;
    }

    /**
     * @param currencyId the currencyId to set
     */
    public void setCurrencyId(Long currencyId)
    {
        this.currencyId = currencyId;
    }

    /**
     * @return the currencyCode
     */
    public String getCurrencyCode()
    {
        return currencyCode;
    }

    /**
     * @param currencyCode the currencyCode to set
     */
    public void setCurrencyCode(String currencyCode)
    {
        this.currencyCode = currencyCode;
    }

    /**
     * @return the currencyName
     */
    public String getCurrencyName()
    {
        return currencyName;
    }

    /**
     * @param currencyName the currencyName to set
     */
    public void setCurrencyName(String currencyName)
    {
        this.currencyName = currencyName;
    }

    /**
     * @return the decimalPlaces
     */
    public int getDecimalPlaces()
    {
        return decimalPlaces;
    }

    /**
     * @param decimalPlaces the decimalPlaces to set
     */
    public void setDecimalPlaces(int decimalPlaces)
    {
        this.decimalPlaces = decimalPlaces;
    }
}
