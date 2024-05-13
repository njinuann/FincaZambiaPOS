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
public class EIProCodesMOB
{

    private String procCode;
    private String procType;
    private String module;
    private String procDesc;
    private String recStatus;
    private String operator;
    private String location;
    private String buCode;
    private String buName;
    private String modifiedBy;
    private Date dateModified;

    /**
     * @return the procCode
     */
    public String getProcCode()
    {
        return procCode;
    }

    /**
     * @param procCode the procCode to set
     */
    public void setProcCode(String procCode)
    {
        this.procCode = procCode;
    }

    /**
     * @return the procType
     */
    public String getProcType()
    {
        return procType;
    }

    /**
     * @param procType the procType to set
     */
    public void setProcType(String procType)
    {
        this.procType = procType;
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

    /**
     * @return the procDesc
     */
    public String getProcDesc()
    {
        return procDesc;
    }

    /**
     * @param procDesc the procDesc to set
     */
    public void setProcDesc(String procDesc)
    {
        this.procDesc = procDesc;
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
     * @return the operator
     */
    public String getOperator()
    {
        return operator;
    }

    /**
     * @param operator the operator to set
     */
    public void setOperator(String operator)
    {
        this.operator = operator;
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

    @Override
    public String toString()
    {
        return getProcCode() + " ~ " + getProcDesc();
    }
}
