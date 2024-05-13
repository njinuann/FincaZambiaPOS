/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Mobile;

import java.util.ArrayList;

/**
 *
 * @author Pecherk
 */
public class CNUserMOB
{
    private String imsi;
    private long custId;
    private long userId;
    private long schemeId;
    private String accessName;
    private String accessCode;
    private String securityCode;
    private String password;
    private boolean pwdReset;
    private boolean locked;
    private String language;
    private int pinAttempts = 0;
    private int pukAttempts = 0;
    private int userCount = 0;
    private long custChannelId;    
    private boolean loggedIn = false;
    private CNAccountMOB chargeAccount = new CNAccountMOB();
    private ArrayList<CNAccountMOB> allLoanAccounts = new ArrayList<>();
    private ArrayList<CNAccountMOB> enrolledAccounts = new ArrayList<>();
    private ArrayList<CNAccountMOB> allDepositAccounts = new ArrayList<>();

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
     * @return the userId
     */
    public long getUserId()
    {
        return userId;
    }

    /**
     * @param userId the userId to set
     */
    public void setUserId(long userId)
    {
        this.userId = userId;
    }

    /**
     * @return the accessName
     */
    public String getAccessName()
    {
        return accessName;
    }

    /**
     * @param accessName the accessName to set
     */
    public void setAccessName(String accessName)
    {
        this.accessName = accessName;
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
     * @return the locked
     */
    public boolean isLocked()
    {
        return locked;
    }

    /**
     * @param locked the locked to set
     */
    public void setLocked(boolean locked)
    {
        this.locked = locked;
    }

    /**
     * @return the password
     */
    public String getPassword()
    {
        return password;
    }

    /**
     * @param password the password to set
     */
    public void setPassword(String password)
    {
        this.password = password;
    }

    /**
     * @return the schemeId
     */
    public long getSchemeId()
    {
        return schemeId;
    }

    /**
     * @param schemeId the schemeId to set
     */
    public void setSchemeId(long schemeId)
    {
        this.schemeId = schemeId;
    }

    /**
     * @return the securityCode
     */
    public String getSecurityCode()
    {
        return securityCode;
    }

    /**
     * @param securityCode the securityCode to set
     */
    public void setSecurityCode(String securityCode)
    {
        this.securityCode = securityCode;
    }

    /**
     * @return the pwdReset
     */
    public boolean isPwdReset()
    {
        return pwdReset;
    }

    /**
     * @param pwdReset the pwdReset to set
     */
    public void setPwdReset(boolean pwdReset)
    {
        this.pwdReset = pwdReset;
    }

    /**
     * @return the imsi
     */
    public String getImsi()
    {
        return imsi;
    }

    /**
     * @param imsi the imsi to set
     */
    public void setImsi(String imsi)
    {
        this.imsi = imsi;
    }

    /**
     * @return the chargeAccount
     */
    public CNAccountMOB getChargeAccount()
    {
        return chargeAccount;
    }

    /**
     * @param chargeAccount the chargeAccount to set
     */
    public void setChargeAccount(CNAccountMOB chargeAccount)
    {
        this.chargeAccount = chargeAccount;
    }

    /**
     * @return the enrolledAccounts
     */
    public ArrayList<CNAccountMOB> getEnrolledAccounts()
    {
        return enrolledAccounts;
    }

    /**
     * @param enrolledAccounts the enrolledAccounts to set
     */
    public void setEnrolledAccounts(ArrayList<CNAccountMOB> enrolledAccounts)
    {
        this.enrolledAccounts = enrolledAccounts;
    }

    /**
     * @return the language
     */
    public String getLanguage()
    {
        return language;
    }

    /**
     * @param language the language to set
     */
    public void setLanguage(String language)
    {
        this.language = language;
    }

    /**
     * @return the loggedIn
     */
    public boolean isLoggedIn()
    {
        return loggedIn;
    }

    /**
     * @param loggedIn the loggedIn to set
     */
    public void setLoggedIn(boolean loggedIn)
    {
        this.loggedIn = loggedIn;
    }

    /**
     * @return the allLoanAccounts
     */
    public ArrayList<CNAccountMOB> getAllLoanAccounts()
    {
        return allLoanAccounts;
    }

    /**
     * @param allLoanAccounts the allLoanAccounts to set
     */
    public void setAllLoanAccounts(ArrayList<CNAccountMOB> allLoanAccounts)
    {
        this.allLoanAccounts = allLoanAccounts;
    }

    /**
     * @return the allDepositAccounts
     */
    public ArrayList<CNAccountMOB> getAllDepositAccounts()
    {
        return allDepositAccounts;
    }

    /**
     * @param allDepositAccounts the allDepositAccounts to set
     */
    public void setAllDepositAccounts(ArrayList<CNAccountMOB> allDepositAccounts)
    {
        this.allDepositAccounts = allDepositAccounts;
    }

    /**
     * @return the pinAttempts
     */
    public int getPinAttempts()
    {
        return pinAttempts;
    }

    /**
     * @param pinAttempts the pinAttempts to set
     */
    public void setPinAttempts(int pinAttempts)
    {
        this.pinAttempts = pinAttempts;
    }

    /**
     * @return the pukAttempts
     */
    public int getPukAttempts()
    {
        return pukAttempts;
    }

    /**
     * @param pukAttempts the pukAttempts to set
     */
    public void setPukAttempts(int pukAttempts)
    {
        this.pukAttempts = pukAttempts;
    }

    /**
     * @return the userCount
     */
    public int getUserCount()
    {
        return userCount;
    }

    /**
     * @param userCount the userCount to set
     */
    public void setUserCount(int userCount)
    {
        this.userCount = userCount;
    }

    /**
     * @return the custChannelId
     */
    public long getCustChannelId()
    {
        return custChannelId;
    }

    /**
     * @param custChannelId the custChannelId to set
     */
    public void setCustChannelId(long custChannelId)
    {
        this.custChannelId = custChannelId;
    }
}
