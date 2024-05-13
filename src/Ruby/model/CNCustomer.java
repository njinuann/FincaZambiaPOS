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
public class CNCustomer
{
    private Long buId;
    private Long custId;
    private String custNo;
    private String custCat;
    private String custName;
    private String contact;

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
    public void setBuId(long buId)
    {
        this.buId = buId;
    }

    /**
     * @return the custId
     */
    public Long getCustId()
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
     * @return the CustNo
     */
    public String getCustNo()
    {
        return custNo;
    }

    /**
     * @param custNo the CustNo to set
     */
    public void setCustNo(String custNo)
    {
        this.custNo = custNo;
    }

    /**
     * @return the custName
     */
    public String getCustName()
    {
        return custName;
    }

    /**
     * @param custName the custName to set
     */
    public void setCustName(String custName)
    {
        this.custName = custName;
    }

    /**
     * @return the CustCat
     */
    public String getCustCat()
    {
        return custCat;
    }

    /**
     * @param custCat the CustCat to set
     */
    public void setCustCat(String custCat)
    {
        this.custCat = custCat;
    }

    /**
     * @return the contact
     */
    public String getContact()
    {
        return contact;
    }

    /**
     * @param contact the contact to set
     */
    public void setContact(String contact)
    {
        this.contact = contact;
    }
}
