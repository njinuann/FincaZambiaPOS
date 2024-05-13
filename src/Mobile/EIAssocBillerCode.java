/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Mobile;

import java.util.Date;

/**
 *
 * @author NJINU
 */
public class EIAssocBillerCode
{

    private String billerCode;
    private String billerDesc;
    private String module;
    private String assocProcCode;
    private String recStatus;
    private String currency;
    private String location;
    private String buCode;
    private String buName;
    private String modifiedBy;
    private Date dateModified;
    private String assocAcctNo;
    private String assocAcctNm;
    private String ntvCodeField;
  

    /**
     * @return the billerCode
     */
    public String getBillerCode()
    {
        return billerCode;
    }

    /**
     * @param billerCode the billerCode to set
     */
    public void setBillerCode(String billerCode)
    {
        this.billerCode = billerCode;
    }

    /**
     * @return the billerDesc
     */
    public String getBillerDesc()
    {
        return billerDesc;
    }

    /**
     * @param billerDesc the billerDesc to set
     */
    public void setBillerDesc(String billerDesc)
    {
        this.billerDesc = billerDesc;
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

    /**
     * @return the assocProcCode
     */
    public String getAssocProcCode()
    {
        return assocProcCode;
    }

    /**
     * @param assocProcCode the assocProcCode to set
     */
    public void setAssocProcCode(String assocProcCode)
    {
        this.assocProcCode = assocProcCode;
    }

    /**
     * @return the recStatus
     */
    public String getRecStatus()
    {
        return recStatus;
    }

    /**
     * @param recStatus the recStatus to set
     */
    public void setRecStatus(String recStatus)
    {
        this.recStatus = recStatus;
    }

    @Override
    public String toString()
    {
        return getBillerCode() + " ~ " + getBillerDesc();
    }

    /**
     * @return the location
     */
    public String getLocation()
    {
        return location;
    }

    /**
     * @param location the location to set
     */
    public void setLocation(String location)
    {
        this.location = location;
    }

    /**
     * @return the buCode
     */
    public String getBuCode()
    {
        return buCode;
    }

    /**
     * @param buCode the buCode to set
     */
    public void setBuCode(String buCode)
    {
        this.buCode = buCode;
    }

    /**
     * @return the buName
     */
    public String getBuName()
    {
        return buName;
    }

    /**
     * @param buName the buName to set
     */
    public void setBuName(String buName)
    {
        this.buName = buName;
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
     * @return the assocAcctNo
     */
    public String getAssocAcctNo()
    {
        return assocAcctNo;
    }

    /**
     * @param assocAcctNo the assocAcctNo to set
     */
    public void setAssocAcctNo(String assocAcctNo)
    {
        this.assocAcctNo = assocAcctNo;
    }

    /**
     * @return the assocAcctNm
     */
    public String getAssocAcctNm()
    {
        return assocAcctNm;
    }

    /**
     * @param assocAcctNm the assocAcctNm to set
     */
    public void setAssocAcctNm(String assocAcctNm)
    {
        this.assocAcctNm = assocAcctNm;
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
     * @return the ntvCodeField
     */
    public String getNtvCodeField()
    {
        return ntvCodeField;
    }

    /**
     * @param ntvCodeField the ntvCodeField to set
     */
    public void setNtvCodeField(String ntvCodeField)
    {
        this.ntvCodeField = ntvCodeField;
    }
}
