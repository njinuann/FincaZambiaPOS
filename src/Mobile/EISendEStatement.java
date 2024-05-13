/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Mobile;

import FILELoad.EStmtRequest;
import APX.PHController;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Image;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.fonts.*;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfEncryptor;
import com.lowagie.text.pdf.PdfGState;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfStamper;
import com.lowagie.text.pdf.PdfWriter;
import java.io.BufferedReader;
import java.io.File;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Types;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public class EISendEStatement
{

    private TDClientMOB tDClient = new TDClientMOB();
    String errorMessage;
    Connection conn;

    public boolean fetchTransactions(EStmtRequest eStmtRequest)
    {       
        boolean isSuccessiful = false;
        gettDClient().connectToDB();
        conn = gettDClient().getDbConnection();       
        try
        {
            if (gettDClient().getFetchEStatement() == null)
            {             
                gettDClient().getDbConnection();
            }
            if (gettDClient().getFetchEStatement() != null)
            {               
                gettDClient().getFetchEStatement().registerOutParameter(1, Types.INTEGER);
                gettDClient().getFetchEStatement().setString(2, eStmtRequest.getAccountNo());

                gettDClient().getFetchEStatement().setString(3, eStmtRequest.getAccountType());
                gettDClient().getFetchEStatement().setDate(4, new java.sql.Date(convertDate(eStmtRequest.getFromDate()).getTime()));

                gettDClient().getFetchEStatement().setDate(5, new java.sql.Date(convertDate(eStmtRequest.getToDate()).getTime()));
                gettDClient().getFetchEStatement().execute();               

                if (execMailerTask(eStmtRequest.getAccountNo()))
                {
                    gettDClient().logInfo("Mailer executed");
                    isSuccessiful = Boolean.TRUE;
                }
                else
                {
                    gettDClient().logInfo("Mailer failed");
                    isSuccessiful = Boolean.FALSE;
                }
            }
        }
        catch (SQLException ex)
        {
            errorMessage = "Database Error [ " + ex.getMessage() + " ]";
            gettDClient().logError(ex);
            isSuccessiful = Boolean.FALSE;

        }
        return isSuccessiful;
    }

    public boolean execMailerTask(String acctNo)
    {      
        CNAccountMOB cNAccount = gettDClient().queryEStatementValues(acctNo);

        String preferredEmailAddr = PHController.smtpUsername, message;
        String[] filesToAttach = new String[1];

        int successCount = 0, failedCount = 0;
        boolean isSuccessful = false;
        try
        {
            String statementFileName = generateStatement(acctNo);
            if (preferredEmailAddr.length() > 0)
            {
                try
                {
                    filesToAttach[0] = statementFileName;
                    sendEmail(new String[]
                    {
                        cNAccount.getEmailAdress()
                    }, PHController.emailSubject, getEmailMessage(cNAccount.getShortName()), PHController.smtpUsername, filesToAttach);
                    message = "E-Statement sent successfully";
                    isSuccessful = true;

                    successCount++;
                    gettDClient().logInfo(successCount + " " + message + " At " + System.currentTimeMillis());
                }
                catch (MessagingException ex)
                {
                    failedCount++;
                    gettDClient().logInfo(successCount + " Failed At " + System.currentTimeMillis());
                    gettDClient().logError(ex);
                }
            }

        }
        catch (Exception ex)
        {
            gettDClient().logError(ex);
        }
        return isSuccessful;
    }

    public String generateStatement(String acctNo)
    {
        try
        {

            System.out.println("Start ....");
            HashMap parametersMap = new HashMap();

            String jrxmlFileName = "mobile/report/e-statement.jrxml";
            String jasperFileName = "mobile/report/e-statement.jasper";

            String pdfFileName = "mobile/work/FINCA Bank e-statement [ AC " + acctNo.replace(acctNo.subSequence(2, 12), "XXXXXXX") + " ].pdf";

            parametersMap.put("AcctNoParam", acctNo);
            JasperCompileManager.compileReportToFile(jrxmlFileName, jasperFileName);

            JasperPrint jprint = (JasperPrint) JasperFillManager.fillReport(jasperFileName, parametersMap, conn);
            JasperExportManager.exportReportToPdfFile(jprint, pdfFileName);

            if (signStatement(pdfFileName, acctNo.substring(acctNo.length() - 5)));
            {
                gettDClient().logInfo("[ " + acctNo + " ]stamped and encrypted \n Done exporting reports to pdf");

            }
            if ((String.valueOf(jprint.getPages()).length() >= 1))
            {
                return pdfFileName;
            }
            else
            {
                errorMessage = "Report Error [ empty statement ]";
                gettDClient().logInfo(errorMessage);

                new File(pdfFileName).delete();
                reportFailedTask(acctNo, errorMessage);
                return null;
            }
        }

        catch (Exception ex)
        {
            gettDClient().logError(ex);
            return null;
        }
    }

    private String getEmailMessage(String name)
    {
        StringBuilder contents = new StringBuilder();
        try
        {
            try (BufferedReader input = new BufferedReader(new FileReader("mobile/conf/email.html")))
            {
                String line;
                while ((line = input.readLine()) != null)
                {
                    contents.append(line);
                    contents.append(System.getProperty("line.separator"));
                }
            }
        }
        catch (IOException ex)
        {
            gettDClient().logError(ex);
        }

        return contents.toString().replace("{NAME}", name);
    }

    public boolean signStatement(String fileName, String password)
    {
        try
        {

            PdfReader reader = new PdfReader(new FileInputStream(fileName));
            int number_of_pages = reader.getNumberOfPages();

            PdfStamper stamp = new PdfStamper(reader, new FileOutputStream(fileName));
            Image watermark_image = Image.getInstance("mobile/images/watermark.png");

            PdfGState gs1 = new PdfGState();
            gs1.setFillOpacity(0.5f);

            int i = 0;
            PdfContentByte add_watermark;

            while (i < number_of_pages)
            {
                if (i == 0)
                {
                    watermark_image.setAbsolutePosition(325, 130);
                }
                else
                {
                    watermark_image.setAbsolutePosition(325, 224);
                }
                i++;
                add_watermark = stamp.getOverContent(i);
                add_watermark.saveState();
                add_watermark.setGState(gs1);
                add_watermark.addImage(watermark_image);
                add_watermark.restoreState();
            }

            stamp.close();
            PdfEncryptor.encrypt(new PdfReader(new FileInputStream(fileName)), new FileOutputStream(fileName), password.getBytes(), password.getBytes(), PdfWriter.ALLOW_PRINTING, true);
            return true;
        }
        catch (IOException | DocumentException ex)
        {
            gettDClient().logError(ex);
            return false;
        }
    }

    public boolean manipulatePdf(String fileName, String password) throws IOException, DocumentException
    {
        Image watermark_image = Image.getInstance("mobile/images/watermark.png");
        PdfReader reader = new PdfReader(new FileInputStream(fileName));
        int n = reader.getNumberOfPages();
        PdfStamper stamper = new PdfStamper(reader, new FileOutputStream(fileName));
        Font f = new Font(Font.HELVETICA, 30);
        Phrase p = new Phrase("My watermark (text)", f);
        Image img = Image.getInstance(watermark_image);
        float w = img.getScaledWidth();
        float h = img.getScaledHeight();
        PdfGState gs1 = new PdfGState();
        gs1.setFillOpacity(0.5f);
        PdfContentByte over;
        Rectangle pagesize;
        float x, y;
        for (int i = 1;i <= n;i++)
        {
            pagesize = reader.getPageSizeWithRotation(i);
            x = (pagesize.getLeft() + pagesize.getRight()) / 2;
            y = (pagesize.getTop() + pagesize.getBottom()) / 2;
            over = stamper.getOverContent(i);
            over.saveState();
            over.setGState(gs1);
            if (i % 2 == 1)
            {
                ColumnText.showTextAligned(over, Element.ALIGN_CENTER, p, x, y, 0);
            }
            else
            {
                over.addImage(img, w, 0, 0, h, x - (w / 2), y - (h / 2));
            }
            over.restoreState();
        }
        stamper.close();
        reader.close();
        PdfEncryptor.encrypt(new PdfReader(new FileInputStream(fileName)), new FileOutputStream(fileName), password.getBytes(), password.getBytes(), PdfWriter.ALLOW_PRINTING, true);
        return true;
    }

    private void sendEmail(String recipients[], String subject, String message, String from, String[] fileNames) throws MessagingException
    {
        Properties props = new Properties();

        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.user", PHController.smtpPassword);
        props.put("mail.smtp.port", PHController.smtpPort);
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.EnableSSL.enable", "true");
        props.put("mail.smtp.ssl.trust", "*");

//System.out.println("am sending now");
        Session session = Session.getDefaultInstance(props, null);
        session.setDebug(true);

        Message msg = new MimeMessage(session);
        InternetAddress addressFrom = new InternetAddress(from);

        msg.setFrom(addressFrom);
        InternetAddress[] addressTo = new InternetAddress[recipients.length];

        for (int i = 0;i < recipients.length;i++)
        {
            if (!recipients[i].trim().equalsIgnoreCase(""))
            {
                addressTo[i] = new InternetAddress(recipients[i]);
            }
        }
        msg.setRecipients(Message.RecipientType.TO, addressTo);
        msg.setSubject(subject);

        MimeBodyPart messageBody = new MimeBodyPart();
        messageBody.setContent(message, "text/html");

        Multipart mp = new MimeMultipart();
        mp.addBodyPart(messageBody);

        for (String fileName : fileNames)
        {
            System.out.println("creating Attachment");
            MimeBodyPart attachement = new MimeBodyPart();
            FileDataSource fds = new FileDataSource(fileName);

            attachement.setDataHandler(new DataHandler(fds));
            attachement.setFileName(fds.getName());
            mp.addBodyPart(attachement);
        }

        msg.setContent(mp);
        msg.setSentDate(new Date());
        System.out.println("looking for smtp");
        Transport transport = session.getTransport("smtp");
        //  System.out.println(MainClass.smtpUsername + " " + BRCrypt.encrypt(MainClass.smtpPassword));
        try
        {

            System.out.println("Trying to send nowq");
            transport.connect(PHController.smtpUsername, PHController.smtpPassword);
            transport.sendMessage(msg, msg.getAllRecipients());
            System.out.println("i have sent now");
        }
        finally
        {
            transport.close();
        }
    }

    private void reportFailedTask(String accountNumber, String dbError)
    {
        String message = "<html><body style=\"font-family:Arial; font-size:12px; line-height:150%; \">"
                + "<b>IT Support Team,</b><br/><br/>"
                + "E-Statement to [ '" + accountNumber + "' ] has been skipped entirely "
                + "for failing to execute after several retries possibly due to the following error encountered:"
                + "<ul><li>" + dbError + "</li></ul>Please find the log file attached for your attention."
                + "<br/><br/>"
                + "<i>This email is automatically generated by e-statement mailer and is intended for IT Support Team through the configured support email address.</i>"
                + "<br/><b>FINCA ZM.</b></body></html>";
        try
        {
            sendEmail(new String[]
            {
                PHController.reportEmailAddress
            }, "E-Statement Mailer Task Error", message, PHController.smtpUsername, new String[]
            {
                "mobile/logs/events.log"
            });
        }
        catch (MessagingException ex)
        {
            gettDClient().logError(ex);
        }
    }

    public String getWebServiceObjectString(Object wsObject)
    {
        String wsObjectStr = "";
        wsObjectStr = ReflectionToStringBuilder.toString(wsObject, ToStringStyle.SHORT_PREFIX_STYLE);
        if (wsObjectStr.equals(""))
        {
            wsObjectStr = wsObject.toString();
        }
        return wsObjectStr;
    }

    private Date convertDate(String cDate)
    {
        java.sql.Date sqlStartDate = null;
        try
        {
            SimpleDateFormat sdf1 = new SimpleDateFormat("dd/MM/yyyy");
            java.util.Date date = sdf1.parse(cDate);
            sqlStartDate = new java.sql.Date(date.getTime());

        }
        catch (ParseException ex)
        {
            Logger.getLogger(EISendEStatement.class.getName()).log(Level.SEVERE, null, ex);
        }
        return sqlStartDate;
    }

    /**
     * @return the tDClient
     */
    public TDClientMOB gettDClient()
    {
        return tDClient;
    }

    /**
     * @param tDClient the tDClient to set
     */
    public void settDClient(TDClientMOB tDClient)
    {
        this.tDClient = tDClient;
    }

}
