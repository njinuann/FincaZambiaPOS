/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Mobile;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 *
 * @author Pecherk
 */
public class TCTierMOB implements Serializable, Comparable<TCTierMOB>
{
    private BigDecimal tierFloor;
    private BigDecimal tierCeiling;
    private BigDecimal chargeAmount;

    /**
     * @return the tierCeiling
     */
    public BigDecimal getTierCeiling()
    {
        return tierCeiling;
    }

    /**
     * @param tierCeiling the tierCeiling to set
     */
    public void setTierCeiling(BigDecimal tierCeiling)
    {
        this.tierCeiling = tierCeiling;
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

    /**
     * @return the tierFloor
     */
    public BigDecimal getTierFloor()
    {
        return tierFloor;
    }

    /**
     * @param tierFloor the tierFloor to set
     */
    public void setTierFloor(BigDecimal tierFloor)
    {
        this.tierFloor = tierFloor;
    }

    @Override
    public int compareTo(TCTierMOB o)
    {
        return getTierCeiling().compareTo(o.getTierCeiling());
    }

    @Override
    public String toString()
    {
        return "Tier [ " + getTierFloor().toPlainString() + " - " + getTierCeiling().toPlainString() + " ]";
    }
}
