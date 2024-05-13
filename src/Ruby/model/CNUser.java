/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Ruby.model;

import java.util.ArrayList;

/**
 *
 * @author Pecherk
 */
public class CNUser
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
    private boolean blocked;
    private String language;
    private int pinAttempts = 0;
    private int pukAttempts = 0;
    private int userCount = 0;
    private boolean loggedIn = false;
    private CNAccount chargeAccount = new CNAccount();
    private ArrayList<CNAccount> allLoanAccounts = new ArrayList<>();
    private ArrayList<CNAccount> enrolledAccounts = new ArrayList<>();
    private ArrayList<CNAccount> allDepositAccounts = new ArrayList<>();
    private ArrayList<CNBeneficiary> beneficiaries = new ArrayList<>();

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
    public CNAccount getChargeAccount()
    {
        return chargeAccount;
    }

    /**
     * @param chargeAccount the chargeAccount to set
     */
    public void setChargeAccount(CNAccount chargeAccount)
    {
        this.chargeAccount = chargeAccount;
    }

    /**
     * @return the enrolledAccounts
     */
    public ArrayList<CNAccount> getEnrolledAccounts()
    {
        return enrolledAccounts;
    }

    /**
     * @param enrolledAccounts the enrolledAccounts to set
     */
    public void setEnrolledAccounts(ArrayList<CNAccount> enrolledAccounts)
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
    public ArrayList<CNAccount> getAllLoanAccounts()
    {
        return allLoanAccounts;
    }

    /**
     * @param allLoanAccounts the allLoanAccounts to set
     */
    public void setAllLoanAccounts(ArrayList<CNAccount> allLoanAccounts)
    {
        this.allLoanAccounts = allLoanAccounts;
    }

    /**
     * @return the allDepositAccounts
     */
    public ArrayList<CNAccount> getAllDepositAccounts()
    {
        return allDepositAccounts;
    }

    /**
     * @param allDepositAccounts the allDepositAccounts to set
     */
    public void setAllDepositAccounts(ArrayList<CNAccount> allDepositAccounts)
    {
        this.allDepositAccounts = allDepositAccounts;
    }

    /**
     * @return the beneficiaries
     */
    public ArrayList<CNBeneficiary> getBeneficiaries()
    {
        return beneficiaries;
    }

    /**
     * @param beneficiaries the beneficiaries to set
     */
    public void setBeneficiaries(ArrayList<CNBeneficiary> beneficiaries)
    {
        this.beneficiaries = beneficiaries;
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
     * @return the blocked
     */
    public boolean isBlocked()
    {
        return blocked;
    }

    /**
     * @param blocked the blocked to set
     */
    public void setBlocked(boolean blocked)
    {
        this.blocked = blocked;
    }
}
