/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package PHilae;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;

/**
 *
 * @author Pecherk
 */
public class EICharge implements Serializable
{
    private String module = "";
    private String taxName = "Tax";
    private String chargeCode = "";
    private String description = "Charge";
    private String chargeAccount = "Source";
    private BigDecimal taxPercentage = BigDecimal.ZERO;
    private String chargeLedger = "";
    private String taxLedger = "";
    private String status = "Active";
    private String lastModifiedBy = "";
    private Date dateModified = new Date();
    private HashMap<String, TCValue> values = new HashMap<>();
    private HashMap<Integer, TCWaiver> waivers = new HashMap<>();

    /**
     * @return the chargeCode
     */
    public String getChargeCode()
    {
        return chargeCode;
    }

    /**
     * @param chargeCode the chargeCode to set
     */
    public void setChargeCode(String chargeCode)
    {
        this.chargeCode = chargeCode;
    }

    /**
     * @return the description
     */
    public String getDescription()
    {
        return description;
    }

    /**
     * @param description the description to set
     */
    public void setDescription(String description)
    {
        this.description = description;
    }

    /**
     * @return the chargeAccount
     */
    public String getChargeAccount()
    {
        return chargeAccount;
    }

    /**
     * @param chargeAccount the chargeAccount to set
     */
    public void setChargeAccount(String chargeAccount)
    {
        this.chargeAccount = chargeAccount;
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
     * @return the lastModifiedBy
     */
    public String getLastModifiedBy()
    {
        return lastModifiedBy;
    }

    /**
     * @param lastModifiedBy the lastModifiedBy to set
     */
    public void setLastModifiedBy(String lastModifiedBy)
    {
        this.lastModifiedBy = lastModifiedBy;
    }

    /**
     * @return the dateModified
     */
    public Date getDateModified()
    {
        return dateModified;
    }

    /**
     * @param dateModified the dateModified to set
     */
    public void setDateModified(Date dateModified)
    {
        this.dateModified = dateModified;
    }

    /**
     * @return the waivers
     */
    public HashMap<Integer, TCWaiver> getWaivers()
    {
        return waivers;
    }

    /**
     * @param waivers the waivers to set
     */
    public void setWaivers(HashMap<Integer, TCWaiver> waivers)
    {
        this.waivers = waivers;
    }

    /**
     * @return the values
     */
    public HashMap<String, TCValue> getValues()
    {
        return values;
    }

    /**
     * @param values the values to set
     */
    public void setValues(HashMap<String, TCValue> values)
    {
        this.values = values;
    }

    /**
     * @return the taxPercentage
     */
    public BigDecimal getTaxPercentage()
    {
        return taxPercentage;
    }

    /**
     * @param taxPercentage the taxPercentage to set
     */
    public void setTaxPercentage(BigDecimal taxPercentage)
    {
        this.taxPercentage = taxPercentage;
    }

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
     * @return the taxName
     */
    public String getTaxName()
    {
        return taxName;
    }

    /**
     * @param taxName the taxName to set
     */
    public void setTaxName(String taxName)
    {
        this.taxName = taxName;
    }

    /**
     * @return the module
     */
    public String getModule()
    {
        return module;
    }

    /**
     * @param module the module to set
     */
    public void setModule(String module)
    {
        this.module = module;
    }
}
