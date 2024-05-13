
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
 *         &lt;element name="ClientID" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="AccountID" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="TitleOfAccount" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="ProductID" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="ActualBalance" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="AvailableBalance" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="Status" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="AllowDeposit" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="AllowWithdrawal" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="NRC" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="Header" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
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
    "clientID",
    "accountID",
    "titleOfAccount",
    "productID",
    "actualBalance",
    "availableBalance",
    "status",
    "allowDeposit",
    "allowWithdrawal",
    "nrc",
    "header",
    "accountIDOld"
})
@XmlRootElement(name = "OfllineBalances")
public class OfllineBalances {

    @XmlElement(name = "ClientID")
    protected String clientID;
    @XmlElement(name = "AccountID")
    protected String accountID;
    @XmlElement(name = "TitleOfAccount")
    protected String titleOfAccount;
    @XmlElement(name = "ProductID")
    protected String productID;
    @XmlElement(name = "ActualBalance")
    protected String actualBalance;
    @XmlElement(name = "AvailableBalance")
    protected String availableBalance;
    @XmlElement(name = "Status")
    protected String status;
    @XmlElement(name = "AllowDeposit")
    protected String allowDeposit;
    @XmlElement(name = "AllowWithdrawal")
    protected String allowWithdrawal;
    @XmlElement(name = "NRC")
    protected String nrc;
    @XmlElement(name = "Header")
    protected String header;
    @XmlElement(name = "AccountID_Old")
    protected String accountIDOld;

    /**
     * Gets the value of the clientID property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getClientID() {
        return clientID;
    }

    /**
     * Sets the value of the clientID property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setClientID(String value) {
        this.clientID = value;
    }

    /**
     * Gets the value of the accountID property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getAccountID() {
        return accountID;
    }

    /**
     * Sets the value of the accountID property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setAccountID(String value) {
        this.accountID = value;
    }

    /**
     * Gets the value of the titleOfAccount property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getTitleOfAccount() {
        return titleOfAccount;
    }

    /**
     * Sets the value of the titleOfAccount property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setTitleOfAccount(String value) {
        this.titleOfAccount = value;
    }

    /**
     * Gets the value of the productID property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getProductID() {
        return productID;
    }

    /**
     * Sets the value of the productID property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setProductID(String value) {
        this.productID = value;
    }

    /**
     * Gets the value of the actualBalance property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getActualBalance() {
        return actualBalance;
    }

    /**
     * Sets the value of the actualBalance property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setActualBalance(String value) {
        this.actualBalance = value;
    }

    /**
     * Gets the value of the availableBalance property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getAvailableBalance() {
        return availableBalance;
    }

    /**
     * Sets the value of the availableBalance property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setAvailableBalance(String value) {
        this.availableBalance = value;
    }

    /**
     * Gets the value of the status property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getStatus() {
        return status;
    }

    /**
     * Sets the value of the status property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setStatus(String value) {
        this.status = value;
    }

    /**
     * Gets the value of the allowDeposit property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getAllowDeposit() {
        return allowDeposit;
    }

    /**
     * Sets the value of the allowDeposit property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setAllowDeposit(String value) {
        this.allowDeposit = value;
    }

    /**
     * Gets the value of the allowWithdrawal property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getAllowWithdrawal() {
        return allowWithdrawal;
    }

    /**
     * Sets the value of the allowWithdrawal property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setAllowWithdrawal(String value) {
        this.allowWithdrawal = value;
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
