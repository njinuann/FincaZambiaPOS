/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package Mobile;

/**
 *
 * @author Pecherk
 */
public final class EITxnMOB
{
    private String txnDescription = "";
    private String processingCode = "";
    private boolean approved = false;

    public EITxnMOB()
    {
        this("", "");
    }
    
    public EITxnMOB(String procCode, String description)
    {
        setProcessingCode(procCode);
        setTxnDescription(description);
    }

    @Override
    public String toString()
    {
        return getTxnDescription();
    }

    public EITxnMOB getClone(boolean approved)
    {
        EITxnMOB posTxn = new EITxnMOB();
        posTxn.setProcessingCode(getProcessingCode());
        posTxn.setTxnDescription(getTxnDescription());
        posTxn.setApproved(approved);
        return posTxn;
    }

    /**
     * @return the processingCode
     */
    public String getProcessingCode()
    {
        return processingCode;
    }

    /**
     * @param processingCode the processingCode to set
     */
    public void setProcessingCode(String processingCode)
    {
        this.processingCode = processingCode;
    }

    /**
     * @return the approved
     */
    public boolean isApproved()
    {
        return approved;
    }

    /**
     * @param approved the approved to set
     */
    public void setApproved(boolean approved)
    {
        this.approved = approved;
    }

    /**
     * @return the txnDescription
     */
    public String getTxnDescription()
    {
        return txnDescription;
    }

    /**
     * @param txnDescription the txnDescription to set
     */
    public void setTxnDescription(String txnDescription)
    {
        this.txnDescription = txnDescription;
    }
}
