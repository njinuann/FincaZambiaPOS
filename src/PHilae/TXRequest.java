/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package PHilae;

import APX.PHController;
import java.math.BigDecimal;
import java.util.StringTokenizer;

/**
 *
 * @author Pecherk
 */
public class TXRequest
{   
    private String reference;
    private String accessCode;
    private String debitAccount;
    private String creditAccount;
    private BigDecimal txnAmount = BigDecimal.ZERO;
    private BigDecimal chargeAmount = BigDecimal.ZERO;
    private String txnNarration;
    private String chargeNarration;
    private String chargeDebitAccount;
    private String chargeCreditLedger;
    private boolean successful = false;
    private String currencyCode = PHController.PrimaryCurrencyCode;
    private BigDecimal taxAmount = BigDecimal.ZERO;
    private String taxCreditLedger;
    private String taxNarration;
    private String channelContraLedger;
    private String customerNo;
    private Long buId;
   
    private String taxType;
    private String stan;
    private String retrivalRef;
    private String billerCode;
    private String billerIdentString;
    private String terminalLocation;

    /**
     * @return the reference
     */
    public String getReference()
    {
        return reference;
    }

    /**
     * @param reference the reference to set
     */
    public void setReference(String reference)
    {
        this.reference = reference;
    }

    /**
     * @return the currencyCode
     */
    public String getCurrencyCode()
    {
        return currencyCode;
    }

    /**
     * @param currencyCode the currencyCode to set
     */
    public void setCurrencyCode(String currencyCode)
    {
        this.currencyCode = currencyCode;
    }

    /**
     * @return the txnAmount
     */
    public BigDecimal getTxnAmount()
    {
        return txnAmount;
    }

    /**
     * @param txnAmount the txnAmount to set
     */
    public void setTxnAmount(BigDecimal txnAmount)
    {
        this.txnAmount = txnAmount;
    }

    /**
     * @return the chargeAmount
     */
    public BigDecimal getChargeAmount()
    {
        return chargeAmount;
    }

    /**
     * @param chargeAmount the chargeAmount to set
     */
    public void setChargeAmount(BigDecimal chargeAmount)
    {
        this.chargeAmount = chargeAmount;
    }

    /**
     * @return the txnNarration
     */
    public String getTxnNarration()
    {
        return txnNarration;
    }

    /**
     * @param txnNarration the txnNarration to set
     */
    public void setTxnNarration(String txnNarration)
    {
        this.txnNarration = txnNarration;
    }

    /**
     * @return the chargeNarration
     */
    public String getChargeNarration()
    {
        return chargeNarration;
    }

    /**
     * @param chargeNarration the chargeNarration to set
     */
    public void setChargeNarration(String chargeNarration)
    {
        this.chargeNarration = chargeNarration;
    }

    /**
     * @return the chargeDebitAccount
     */
    public String getChargeDebitAccount()
    {
        return chargeDebitAccount;
    }

    /**
     * @param chargeDebitAccount the chargeDebitAccount to set
     */
    public void setChargeDebitAccount(String chargeDebitAccount)
    {
        this.chargeDebitAccount = chargeDebitAccount;
    }

    /**
     * @return the chargeCreditLedger
     */
    public String getChargeCreditLedger()
    {
        return chargeCreditLedger;
    }

    /**
     * @param chargeCreditLedger the chargeCreditLedger to set
     */
    public void setChargeCreditLedger(String chargeCreditLedger)
    {
        this.chargeCreditLedger = chargeCreditLedger;
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
     * @return the successful
     */
    public boolean isSuccessful()
    {
        return successful;
    }

    /**
     * @param successful the successful to set
     */
    public void setSuccessful(boolean successful)
    {
        this.successful = successful;
    }

    /**
     * @return the debitAccount
     */
    public String getDebitAccount()
    {
        return debitAccount;
    }

    /**
     * @param debitAccount the debitAccount to set
     */
    public void setDebitAccount(String debitAccount)
    {
        this.debitAccount = debitAccount;
    }

    /**
     * @return the creditAccount
     */
    public String getCreditAccount()
    {
        return creditAccount;
    }

    /**
     * @param creditAccount the creditAccount to set
     */
    public void setCreditAccount(String creditAccount)
    {
        this.creditAccount = creditAccount;
    }

    /**
     * @return the taxAmount
     */
    public BigDecimal getTaxAmount()
    {
        return taxAmount;
    }

    /**
     * @param taxAmount the taxAmount to set
     */
    public void setTaxAmount(BigDecimal taxAmount)
    {
        this.taxAmount = taxAmount;
    }

    /**
     * @return the taxCreditLedger
     */
    public String getTaxCreditLedger()
    {
        return taxCreditLedger;
    }

    /**
     * @param taxCreditLedger the taxCreditLedger to set
     */
    public void setTaxCreditLedger(String taxCreditLedger)
    {
        this.taxCreditLedger = taxCreditLedger;
    }

    /**
     * @return the taxNarration
     */
    public String getTaxNarration()
    {
        return taxNarration;
    }

    /**
     * @param taxNarration the taxNarration to set
     */
    public void setTaxNarration(String taxNarration)
    {
        this.taxNarration = removeSpaces(taxNarration);
    }

    /**
     * @return the channelContraLedger
     */
    public String getChannelContraLedger()
    {
        return channelContraLedger;
    }

    /**
     * @param channelContraLedger the channelContraLedger to set
     */
    public void setChannelContraLedger(String channelContraLedger)
    {
        this.channelContraLedger = channelContraLedger;
    }

    public String removeSpaces(String text)
    {
        text = text != null ? text : "";
        StringBuilder buffer = new StringBuilder();
        StringTokenizer tokenizer = new StringTokenizer(text);
        try
        {
            while (tokenizer.hasMoreTokens())
            {
                buffer.append(" ").append(tokenizer.nextToken());
            }
        }
        catch (Exception ex)
        {
            ex = null;
        }
        return buffer.toString().trim();
    }

    /**
     * @return the taxType
     */
    public String getTaxType()
    {
        return taxType;
    }

    /**
     * @param taxType the taxType to set
     */
    public void setTaxType(String taxType)
    {
        this.taxType = taxType;
    }

    /**
     * @return the customerNo
     */
    public String getCustomerNo()
    {
        return customerNo;
    }

    /**
     * @param customerNo the customerNo to set
     */
    public void setCustomerNo(String customerNo)
    {
        this.customerNo = customerNo;
    }

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
     * @return the stan
     */
    public String getStan()
    {
        return stan;
    }

    /**
     * @param stan the stan to set
     */
    public void setStan(String stan)
    {
        this.stan = stan;
    }

    /**
     * @return the retrivalRef
     */
    public String getRetrivalRef()
    {
        return retrivalRef;
    }

    /**
     * @param retrivalRef the retrivalRef to set
     */
    public void setRetrivalRef(String retrivalRef)
    {
        this.retrivalRef = retrivalRef;
    }
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
     * @return the billerIdentString
     */
    public String getBillerIdentString()
    {
        return billerIdentString;
    }

    /**
     * @param billerIdentString the billerIdentString to set
     */
    public void setBillerIdentString(String billerIdentString)
    {
        this.billerIdentString = billerIdentString;
    }

    /**
     * @return the terminalLocation
     */
    public String getTerminalLocation()
    {
        return terminalLocation;
    }

    /**
     * @param terminalLocation the terminalLocation to set
     */
    public void setTerminalLocation(String terminalLocation)
    {
        this.terminalLocation = terminalLocation;
    }

    

}
