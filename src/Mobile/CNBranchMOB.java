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
public class CNBranchMOB implements Serializable
{
    private Long buId;
    private String buCode;
    private String buName;
    private String glPrefix;

    /**
     * @return the buId
     */
    public Long getBuId()
    {
        return buId;
    }

    /**
     * @param buId the buId to set
     */
    public void setBuId(Long buId)
    {
        this.buId = buId;
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
     * @return the glPrefix
     */
    public String getGlPrefix()
    {
        return glPrefix;
    }

    /**
     * @param glPrefix the glPrefix to set
     */
    public void setGlPrefix(String glPrefix)
    {
        this.glPrefix = glPrefix;
    }
}
