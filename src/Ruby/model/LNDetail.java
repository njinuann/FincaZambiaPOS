/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Ruby.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 *
 * @author Pecherk
 */
public class LNDetail implements Serializable
{
    private String mobileNumber;
    private Date paymentDueDate;
    private CNAccount loanAccount = new CNAccount();
    private CNAccount repaymentAccount = new CNAccount();
    private BigDecimal unpaidAmount = BigDecimal.ZERO;
    private BigDecimal clearedBalance = BigDecimal.ZERO;
    private BigDecimal repaymentAmount = BigDecimal.ZERO;

    /**
     * @return the clearedBalance
     */
    public BigDecimal getClearedBalance()
    {
        return clearedBalance;
    }

    /**
     * @param clearedBalance the clearedBalance to set
     */
    public void setClearedBalance(BigDecimal clearedBalance)
    {
        this.clearedBalance = clearedBalance != null ? clearedBalance : BigDecimal.ZERO;
    }

    /**
     * @return the mobileNumber
     */
    public String getMobileNumber()
    {
        return mobileNumber;
    }

    /**
     * @param mobileNumber the mobileNumber to set
     */
    public void setMobileNumber(String mobileNumber)
    {
        this.mobileNumber = mobileNumber;
    }

    /**
     * @return the loanAccount
     */
    public CNAccount getLoanAccount()
    {
        return loanAccount;
    }

    /**
     * @param loanAccount the loanAccount to set
     */
    public void setLoanAccount(CNAccount loanAccount)
    {
        this.loanAccount = loanAccount;
    }

    /**
     * @return the repaymentAccount
     */
    public CNAccount getRepaymentAccount()
    {
        return repaymentAccount;
    }

    /**
     * @param repaymentAccount the repaymentAccount to set
     */
    public void setRepaymentAccount(CNAccount repaymentAccount)
    {
        this.repaymentAccount = repaymentAccount;
    }

    /**
     * @return the repaymentAmount
     */
    public BigDecimal getRepaymentAmount()
    {
        return repaymentAmount;
    }

    /**
     * @param repaymentAmount the repaymentAmount to set
     */
    public void setRepaymentAmount(BigDecimal repaymentAmount)
    {
        this.repaymentAmount = repaymentAmount != null ? repaymentAmount : BigDecimal.ZERO;
    }

    /**
     * @return the paymentDueDate
     */
    public Date getPaymentDueDate()
    {
        return paymentDueDate;
    }

    /**
     * @param paymentDueDate the paymentDueDate to set
     */
    public void setPaymentDueDate(Date paymentDueDate)
    {
        this.paymentDueDate = paymentDueDate;
    }

    /**
     * @return the unpaidAmount
     */
    public BigDecimal getUnpaidAmount()
    {
        return unpaidAmount;
    }

    /**
     * @param unpaidAmount the unpaidAmount to set
     */
    public void setUnpaidAmount(BigDecimal unpaidAmount)
    {
        this.unpaidAmount = unpaidAmount != null ? unpaidAmount : BigDecimal.ZERO;
    }
}
