/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Ruby.model;

/**
 *
 * @author Pecherk
 */
public class CNBeneficiary
{
    private long benId;
    private long custId;
    private String status;
    private String benName;
    private String txnType;
    private int menuLevel;
    private String accessCode;
    private String benAccount;

    /**
     * @return the custId
     */
    public long getCustId()
    {
        return custId;
    }

    /**
     * @param custId the custId to set
     */
    public void setCustId(long custId)
    {
        this.custId = custId;
    }

    /**
     * @return the benId
     */
    public long getBenId()
    {
        return benId;
    }

    /**
     * @param benId the benId to set
     */
    public void setBenId(long benId)
    {
        this.benId = benId;
    }

    /**
     * @return the benName
     */
    public String getBenName()
    {
        return benName;
    }

    /**
     * @param benName the benName to set
     */
    public void setBenName(String benName)
    {
        this.benName = benName;
    }

    /**
     * @return the accessCode
     */
    public String getAccessCode()
    {
        return accessCode;
    }

    /**
     * @param accessCode the accessCode to set
     */
    public void setAccessCode(String accessCode)
    {
        this.accessCode = accessCode;
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
     * @return the benAccount
     */
    public String getBenAccount()
    {
        return benAccount;
    }

    /**
     * @param benAccount the benAccount to set
     */
    public void setBenAccount(String benAccount)
    {
        this.benAccount = benAccount;
    }

    /**
     * @return the txnType
     */
    public String getTxnType()
    {
        return txnType;
    }

    /**
     * @param txnType the txnType to set
     */
    public void setTxnType(String txnType)
    {
        this.txnType = txnType;
    }

    /**
     * @return the menuLevel
     */
    public int getMenuLevel()
    {
        return menuLevel;
    }

    /**
     * @param menuLevel the menuLevel to set
     */
    public void setMenuLevel(int menuLevel)
    {
        this.menuLevel = menuLevel;
    }
}
