/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package SMS;

import java.math.BigDecimal;
import java.util.Date;

/**
 *
 * @author NJINU
 */
public class SMSTemplate
{

    private String templateId;
    private String templateMsg;
    private String isChargable;
    private BigDecimal chargeAmt;
    private BigDecimal taxAmt;
    private BigDecimal taxPerc;
    private String chargeDesc;
    private String taxDesc;
    private String chargeLedger;
    private String taxledger;
    private String module;
    private String currency;
    private String status;
    private String modifiedBy;
    private Date dateModified;
    private String alertType;
    private String description;

    @Override
    public String toString()
    {
        return getTemplateId()+ " ~ " + getDescription();
    }
    /**
     * @return the templateId
     */
    public String getTemplateId()
    {
        return templateId;
    }

    /**
     * @param templateId the templateId to set
     */
    public void setTemplateId(String templateId)
    {
        this.templateId = templateId;
    }

    /**
     * @return the templateMsg
     */
    public String getTemplateMsg()
    {
        return templateMsg;
    }

    /**
     * @param templateMsg the templateMsg to set
     */
    public void setTemplateMsg(String templateMsg)
    {
        this.templateMsg = templateMsg;
    }

    

    /**
     * @return the isChargable
     */
    public String getIsChargable()
    {
        return isChargable;
    }

    /**
     * @param isChargable the isChargable to set
     */
    public void setIsChargable(String isChargable)
    {
        this.isChargable = isChargable;
    }

    /**
     * @return the chargeAmt
     */
    public BigDecimal getChargeAmt()
    {
        return chargeAmt;
    }

    /**
     * @param chargeAmt the chargeAmt to set
     */
    public void setChargeAmt(BigDecimal chargeAmt)
    {
        this.chargeAmt = chargeAmt;
    }

    /**
     * @return the taxAmt
     */
    public BigDecimal getTaxAmt()
    {
        return taxAmt;
    }

    /**
     * @param taxAmt the taxAmt to set
     */
    public void setTaxAmt(BigDecimal taxAmt)
    {
        this.taxAmt = taxAmt;
    }

    /**
     * @return the taxPerc
     */
    public BigDecimal getTaxPerc()
    {
        return taxPerc;
    }

    /**
     * @param taxPerc the taxPerc to set
     */
    public void setTaxPerc(BigDecimal taxPerc)
    {
        this.taxPerc = taxPerc;
    }

    /**
     * @return the chargeDesc
     */
    public String getChargeDesc()
    {
        return chargeDesc;
    }

    /**
     * @param chargeDesc the chargeDesc to set
     */
    public void setChargeDesc(String chargeDesc)
    {
        this.chargeDesc = chargeDesc;
    }

    /**
     * @return the taxDesc
     */
    public String getTaxDesc()
    {
        return taxDesc;
    }

    /**
     * @param taxDesc the taxDesc to set
     */
    public void setTaxDesc(String taxDesc)
    {
        this.taxDesc = taxDesc;
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
     * @return the taxledger
     */
    public String getTaxledger()
    {
        return taxledger;
    }

    /**
     * @param taxledger the taxledger to set
     */
    public void setTaxledger(String taxledger)
    {
        this.taxledger = taxledger;
    }

    /**
     * @return the modFule
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
     * @return the modifiedBy
     */
    public String getModifiedBy()
    {
        return modifiedBy;
    }

    /**
     * @param modifiedBy the modifiedBy to set
     */
    public void setModifiedBy(String modifiedBy)
    {
        this.modifiedBy = modifiedBy;
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
     * @return the alertType
     */
    public String getAlertType()
    {
        return alertType;
    }

    /**
     * @param alertType the alertType to set
     */
    public void setAlertType(String alertType)
    {
        this.alertType = alertType;
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

}
