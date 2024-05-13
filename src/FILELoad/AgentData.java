
package FILELoad;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for anonymous complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType>
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="PRODUCT_CODE" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="CURRENCY" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="BRANCH_CODE" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="COD_CUST_NAIL_ID" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="AGENT_ID" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="AGENT_ACCOUNT" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="ACCOUNT_BAL" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="BUSINESS_NAME" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="BUSINESS_TYPE" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="PHYSICAL_ADDRESS" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="POSTAL_ADDRESS" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="CONTACT_PERSON" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="MOBILE_PHONE" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="OFFICE_PHONE" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="EMAIL_ADDRESS" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="Updated" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="header" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="NRC" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="AccountID_Old" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "productcode",
    "currency",
    "branchcode",
    "codcustnailid",
    "agentid",
    "agentaccount",
    "accountbal",
    "businessname",
    "businesstype",
    "physicaladdress",
    "postaladdress",
    "contactperson",
    "mobilephone",
    "officephone",
    "emailaddress",
    "updated",
    "header",
    "nrc",
    "accountIDOld"
})
@XmlRootElement(name = "AgentData")
public class AgentData {

    @XmlElement(name = "PRODUCT_CODE")
    protected String productcode;
    @XmlElement(name = "CURRENCY")
    protected String currency;
    @XmlElement(name = "BRANCH_CODE")
    protected String branchcode;
    @XmlElement(name = "COD_CUST_NAIL_ID")
    protected String codcustnailid;
    @XmlElement(name = "AGENT_ID")
    protected String agentid;
    @XmlElement(name = "AGENT_ACCOUNT")
    protected String agentaccount;
    @XmlElement(name = "ACCOUNT_BAL")
    protected String accountbal;
    @XmlElement(name = "BUSINESS_NAME")
    protected String businessname;
    @XmlElement(name = "BUSINESS_TYPE")
    protected String businesstype;
    @XmlElement(name = "PHYSICAL_ADDRESS")
    protected String physicaladdress;
    @XmlElement(name = "POSTAL_ADDRESS")
    protected String postaladdress;
    @XmlElement(name = "CONTACT_PERSON")
    protected String contactperson;
    @XmlElement(name = "MOBILE_PHONE")
    protected String mobilephone;
    @XmlElement(name = "OFFICE_PHONE")
    protected String officephone;
    @XmlElement(name = "EMAIL_ADDRESS")
    protected String emailaddress;
    @XmlElement(name = "Updated")
    protected String updated;
    protected String header;
    @XmlElement(name = "NRC")
    protected String nrc;
    @XmlElement(name = "AccountID_Old")
    protected String accountIDOld;

    /**
     * Gets the value of the productcode property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getPRODUCTCODE() {
        return productcode;
    }

    /**
     * Sets the value of the productcode property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setPRODUCTCODE(String value) {
        this.productcode = value;
    }

    /**
     * Gets the value of the currency property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getCURRENCY() {
        return currency;
    }

    /**
     * Sets the value of the currency property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setCURRENCY(String value) {
        this.currency = value;
    }

    /**
     * Gets the value of the branchcode property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getBRANCHCODE() {
        return branchcode;
    }

    /**
     * Sets the value of the branchcode property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setBRANCHCODE(String value) {
        this.branchcode = value;
    }

    /**
     * Gets the value of the codcustnailid property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getCODCUSTNAILID() {
        return codcustnailid;
    }

    /**
     * Sets the value of the codcustnailid property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setCODCUSTNAILID(String value) {
        this.codcustnailid = value;
    }

    /**
     * Gets the value of the agentid property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getAGENTID() {
        return agentid;
    }

    /**
     * Sets the value of the agentid property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setAGENTID(String value) {
        this.agentid = value;
    }

    /**
     * Gets the value of the agentaccount property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getAGENTACCOUNT() {
        return agentaccount;
    }

    /**
     * Sets the value of the agentaccount property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setAGENTACCOUNT(String value) {
        this.agentaccount = value;
    }

    /**
     * Gets the value of the accountbal property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getACCOUNTBAL() {
        return accountbal;
    }

    /**
     * Sets the value of the accountbal property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setACCOUNTBAL(String value) {
        this.accountbal = value;
    }

    /**
     * Gets the value of the businessname property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getBUSINESSNAME() {
        return businessname;
    }

    /**
     * Sets the value of the businessname property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setBUSINESSNAME(String value) {
        this.businessname = value;
    }

    /**
     * Gets the value of the businesstype property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getBUSINESSTYPE() {
        return businesstype;
    }

    /**
     * Sets the value of the businesstype property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setBUSINESSTYPE(String value) {
        this.businesstype = value;
    }

    /**
     * Gets the value of the physicaladdress property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getPHYSICALADDRESS() {
        return physicaladdress;
    }

    /**
     * Sets the value of the physicaladdress property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setPHYSICALADDRESS(String value) {
        this.physicaladdress = value;
    }

    /**
     * Gets the value of the postaladdress property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getPOSTALADDRESS() {
        return postaladdress;
    }

    /**
     * Sets the value of the postaladdress property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setPOSTALADDRESS(String value) {
        this.postaladdress = value;
    }

    /**
     * Gets the value of the contactperson property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getCONTACTPERSON() {
        return contactperson;
    }

    /**
     * Sets the value of the contactperson property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setCONTACTPERSON(String value) {
        this.contactperson = value;
    }

    /**
     * Gets the value of the mobilephone property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getMOBILEPHONE() {
        return mobilephone;
    }

    /**
     * Sets the value of the mobilephone property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setMOBILEPHONE(String value) {
        this.mobilephone = value;
    }

    /**
     * Gets the value of the officephone property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getOFFICEPHONE() {
        return officephone;
    }

    /**
     * Sets the value of the officephone property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setOFFICEPHONE(String value) {
        this.officephone = value;
    }

    /**
     * Gets the value of the emailaddress property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getEMAILADDRESS() {
        return emailaddress;
    }

    /**
     * Sets the value of the emailaddress property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setEMAILADDRESS(String value) {
        this.emailaddress = value;
    }

    /**
     * Gets the value of the updated property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getUpdated() {
        return updated;
    }

    /**
     * Sets the value of the updated property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setUpdated(String value) {
        this.updated = value;
    }

    /**
     * Gets the value of the header property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getHeader() {
        return header;
    }

    /**
     * Sets the value of the header property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setHeader(String value) {
        this.header = value;
    }

    /**
     * Gets the value of the nrc property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getNRC() {
        return nrc;
    }

    /**
     * Sets the value of the nrc property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setNRC(String value) {
        this.nrc = value;
    }

    /**
     * Gets the value of the accountIDOld property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getAccountIDOld() {
        return accountIDOld;
    }

    /**
     * Sets the value of the accountIDOld property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setAccountIDOld(String value) {
        this.accountIDOld = value;
    }

}
