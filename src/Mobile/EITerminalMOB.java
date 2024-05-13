/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Mobile;

import java.util.Date;
import java.util.HashMap;

/**
 *
 * @author Pecherk
 */
public class EITerminalMOB
{
    private String terminalId;
    private String channelCode;
    private String operator;
    private String location;
    private String status;
    private String buCode;
    private String buName;
    private String modifiedBy;
    private Date dateModified;
    private HashMap<String, TMAccountMOB> accounts = new HashMap<>();

    /**
     * @return the terminalId
     */
    public String getTerminalId()
    {
        return terminalId;
    }

    /**
     * @param terminalId the terminalId to set
     */
    public void setTerminalId(String terminalId)
    {
        this.terminalId = terminalId;
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
     * @return the channelCode
     */
    public String getChannelCode()
    {
        return channelCode;
    }

    /**
     * @param channelCode the channelCode to set
     */
    public void setChannelCode(String channelCode)
    {
        this.channelCode = channelCode;
    }

    @Override
    public String toString()
    {
        return getTerminalId() + " ~ " + getLocation();
    }

    /**
     * @return the accounts
     */
    public HashMap<String, TMAccountMOB> getAccounts()
    {
        return accounts;
    }

    /**
     * @param accounts the accounts to set
     */
    public void setAccounts(HashMap<String, TMAccountMOB> accounts)
    {
        this.accounts = accounts;
    }
}
