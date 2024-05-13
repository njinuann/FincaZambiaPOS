/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package SMS;

import java.math.BigDecimal;
import java.sql.Date;

/**
 *
 * @author NJINU
 */
public class SMSOutQueus
{

    private Long messageId;
    private String acctName;
    private String acctNo;
    private String mobileNo;
    private String txDesc;
    private BigDecimal txAmt;
    private BigDecimal txCharge;
    private String drCr;
    private String recSt;
    private String processDate;
    private String smsType;
    private Long channelID;
    private String contraAcctNo;
    private BigDecimal ledgerBal = BigDecimal.ZERO;

    /**
     * @return the messageId
     */
    public Long getMessageId()
    {
        return messageId;
    }

    /**
     * @param messageId the messageId to set
     */
    public void setMessageId(Long messageId)
    {
        this.messageId = messageId;
    }

    /**
     * @return the acctNo
     */
    public String getAcctNo()
    {
        return acctNo;
    }

    /**
     * @param acctNo the acctNo to set
     */
    public void setAcctNo(String acctNo)
    {
        this.acctNo = acctNo;
    }

    /**
     * @return the txDesc
     */
    public String getTxDesc()
    {
        return txDesc;
    }

    /**
     * @param txDesc the txDesc to set
     */
    public void setTxDesc(String txDesc)
    {
        this.txDesc = txDesc;
    }

    /**
     * @return the txAmt
     */
    public BigDecimal getTxAmt()
    {
        return txAmt;
    }

    /**
     * @param txAmt the txAmt to set
     */
    public void setTxAmt(BigDecimal txAmt)
    {
        this.txAmt = txAmt;
    }

    /**
     * @return the txCharge
     */
    public BigDecimal getTxCharge()
    {
        return txCharge;
    }

    /**
     * @param txCharge the txCharge to set
     */
    public void setTxCharge(BigDecimal txCharge)
    {
        this.txCharge = txCharge;
    }

    /**
     * @return the drCr
     */
    public String getDrCr()
    {
        return drCr;
    }

    /**
     * @param drCr the drCr to set
     */
    public void setDrCr(String drCr)
    {
        this.drCr = drCr;
    }

    /**
     * @return the recSt
     */
    public String getRecSt()
    {
        return recSt;
    }

    /**
     * @param recSt the recSt to set
     */
    public void setRecSt(String recSt)
    {
        this.recSt = recSt;
    }

    /**
     * @return the processDate
     */
    public String getProcessDate()
    {
        return processDate;
    }

    /**
     * @param processDate the processDate to set
     */
    public void setProcessDate(String processDate)
    {
        this.processDate = processDate;
    }

    /**
     * @return the smsType
     */
    public String getSmsType()
    {
        return smsType;
    }

    /**
     * @param smsType the smsType to set
     */
    public void setSmsType(String smsType)
    {
        this.smsType = smsType;
    }

    /**
     * @return the channelID
     */
    public Long getChannelID()
    {
        return channelID;
    }

    /**
     * @param channelID the channelID to set
     */
    public void setChannelID(Long channelID)
    {
        this.channelID = channelID;
    }

    /**
     * @return the acctName
     */
    public String getAcctName()
    {
        return acctName;
    }

    /**
     * @param acctName the acctName to set
     */
    public void setAcctName(String acctName)
    {
        this.acctName = acctName;
    }

    /**
     * @return the mobileNo
     */
    public String getMobileNo()
    {
        return mobileNo;
    }

    /**
     * @param mobileNo the mobileNo to set
     */
    public void setMobileNo(String mobileNo)
    {
        this.mobileNo = mobileNo;
    }

    /**
     * @return the contraAcctNo
     */
    public String getContraAcctNo()
    {
        return contraAcctNo;
    }

    /**
     * @param contraAcctNo the contraAcctNo to set
     */
    public void setContraAcctNo(String contraAcctNo)
    {
        this.contraAcctNo = contraAcctNo;
    }

    /**
     * @return the ledgerBal
     */
    public BigDecimal getLedgerBal()
    {
        return ledgerBal;
    }

    /**
     * @param ledgerBal the ledgerBal to set
     */
    public void setLedgerBal(BigDecimal ledgerBal)
    {
        this.ledgerBal = ledgerBal;
    }

}
