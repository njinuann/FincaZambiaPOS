
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
 *         &lt;element name="MobileMigrationResult" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
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
    "mobileMigrationResult"
})
@XmlRootElement(name = "MobileMigrationResponse")
public class MobileMigrationResponse {

    @XmlElement(name = "MobileMigrationResult")
    protected String mobileMigrationResult;

    /**
     * Gets the value of the mobileMigrationResult property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getMobileMigrationResult() {
        return mobileMigrationResult;
    }

    /**
     * Sets the value of the mobileMigrationResult property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setMobileMigrationResult(String value) {
        this.mobileMigrationResult = value;
    }

}
