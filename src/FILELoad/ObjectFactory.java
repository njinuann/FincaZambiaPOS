
package FILELoad;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the awsfcmb package. 
 * <p>An ObjectFactory allows you to programatically 
 * construct new instances of the Java representation 
 * for XML content. The Java representation of XML 
 * content can consist of schema derived interfaces 
 * and classes representing the binding of schema 
 * type definitions, element declarations and model 
 * groups.  Factory methods for each of these are 
 * provided in this class.
 * 
 */
@XmlRegistry
public class ObjectFactory {

    private final static QName _String_QNAME = new QName("http://elma.bz/", "string");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: awsfcmb
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link MobileMigration }
     * 
     */
    public MobileMigration createMobileMigration() {
        return new MobileMigration();
    }

    /**
     * Create an instance of {@link CustomerData }
     * 
     */
    public CustomerData createCustomerData() {
        return new CustomerData();
    }

    /**
     * Create an instance of {@link OfllineBalances }
     * 
     */
    public OfllineBalances createOfllineBalances() {
        return new OfllineBalances();
    }

    /**
     * Create an instance of {@link AgentData }
     * 
     */
    public AgentData createAgentData() {
        return new AgentData();
    }

    /**
     * Create an instance of {@link OrbitSMSResponse }
     * 
     */
    public OrbitSMSResponse createOrbitSMSResponse() {
        return new OrbitSMSResponse();
    }

    /**
     * Create an instance of {@link MobileDeregistration }
     * 
     */
    public MobileDeregistration createMobileDeregistration() {
        return new MobileDeregistration();
    }

    /**
     * Create an instance of {@link MobileMigrationResponse }
     * 
     */
    public MobileMigrationResponse createMobileMigrationResponse() {
        return new MobileMigrationResponse();
    }

    /**
     * Create an instance of {@link MobileRegistration }
     * 
     */
    public MobileRegistration createMobileRegistration() {
        return new MobileRegistration();
    }

    /**
     * Create an instance of {@link OrbitSMS }
     * 
     */
    public OrbitSMS createOrbitSMS() {
        return new OrbitSMS();
    }

    /**
     * Create an instance of {@link MobileRegistrationResponse }
     * 
     */
    public MobileRegistrationResponse createMobileRegistrationResponse() {
        return new MobileRegistrationResponse();
    }

    /**
     * Create an instance of {@link CustomerDataResponse }
     * 
     */
    public CustomerDataResponse createCustomerDataResponse() {
        return new CustomerDataResponse();
    }

    /**
     * Create an instance of {@link AgentDataResponse }
     * 
     */
    public AgentDataResponse createAgentDataResponse() {
        return new AgentDataResponse();
    }

    /**
     * Create an instance of {@link MobileDeregistrationResponse }
     * 
     */
    public MobileDeregistrationResponse createMobileDeregistrationResponse() {
        return new MobileDeregistrationResponse();
    }

    /**
     * Create an instance of {@link OfllineBalancesResponse }
     * 
     */
    public OfllineBalancesResponse createOfllineBalancesResponse() {
        return new OfllineBalancesResponse();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://elma.bz/", name = "string")
    public JAXBElement<String> createString(String value) {
        return new JAXBElement<String>(_String_QNAME, String.class, null, value);
    }

}
