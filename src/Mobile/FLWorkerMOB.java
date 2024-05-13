/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * FLWorkerMOB.java
 *
 * Created on Mar 23, 2012, 1:07:50 PM
 */
package Mobile;

import APX.BRFile;
import APX.PHController;
import APX.PHMain;
import Decoder.BASE64Decoder;
import Decoder.BASE64Encoder;
import java.awt.Color;
import java.io.File;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESedeKeySpec;
import javax.swing.JDialog;

/**
 *
 * @author Pecherk
 */
public final class FLWorkerMOB extends javax.swing.JPanel implements Runnable
{

    private SecretKey key;
    private Cipher cipher;

    private JDialog filesDialog = null;
    private final BRFile bRFile = new BRFile();
    private final TDClientMOB tDClient = new TDClientMOB();

    private final BASE64Encoder base64encoder = new BASE64Encoder();
    private final BASE64Decoder base64decoder = new BASE64Decoder();
    public java.util.Date timestamp = new java.util.Date();
    public SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmm");

    /**
     * Creates new form ATMFiles
     */
    public FLWorkerMOB()
    {
        initComponents();
        initEncryptor();
    }

    public void initEncryptor()
    {
        try
        {
            cipher = Cipher.getInstance("DESede");
            key = SecretKeyFactory.getInstance("DESede").generateSecret(new DESedeKeySpec("ThisIsSecretEncryptionKey".getBytes(StandardCharsets.UTF_8)));
        }
        catch (Exception ex)
        {
            logError(ex);
        }
    }

    public String encrypt(String plainText)
    {
        try
        {
            cipher.init(1, key);
            return base64encoder.encode(cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8)));
        }
        catch (Exception ex)
        {
            logError(ex);
        }
        return null;
    }

    public String decrypt(String encryptedString)
    {
        try
        {
            cipher.init(2, key);
            return new String(cipher.doFinal(base64decoder.decodeBuffer(encryptedString)), StandardCharsets.UTF_8);
        }
        catch (Exception ex)
        {
            logError(ex);
        }
        return null;
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents()
    {

        customersBox = new javax.swing.JCheckBox();
        accountsBox = new javax.swing.JCheckBox();
        customerAccountsBox = new javax.swing.JCheckBox();
        accountBalancesBox = new javax.swing.JCheckBox();
        jSeparator1 = new javax.swing.JSeparator();
        extractButton = new javax.swing.JButton();
        cancelButton = new javax.swing.JButton();
        jSeparator2 = new javax.swing.JSeparator();
        progressBar = new javax.swing.JProgressBar();
        accountStatementsBox = new javax.swing.JCheckBox();

        customersBox.setText("Customers");

        accountsBox.setText("Accounts");

        customerAccountsBox.setText("Customer Accounts");

        accountBalancesBox.setText("Account Balances");

        extractButton.setText("Extract");
        extractButton.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                extractButtonActionPerformed(evt);
            }
        });

        cancelButton.setText("Close");
        cancelButton.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                cancelButtonActionPerformed(evt);
            }
        });

        jSeparator2.setOrientation(javax.swing.SwingConstants.VERTICAL);

        progressBar.setToolTipText("");
        progressBar.setString("");
        progressBar.setStringPainted(true);

        accountStatementsBox.setText("Statements");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jSeparator1, javax.swing.GroupLayout.DEFAULT_SIZE, 427, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(progressBar, javax.swing.GroupLayout.DEFAULT_SIZE, 277, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(extractButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jSeparator2, javax.swing.GroupLayout.PREFERRED_SIZE, 2, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(cancelButton))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(accountStatementsBox, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 195, Short.MAX_VALUE)
                            .addComponent(customersBox, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 195, Short.MAX_VALUE)
                            .addComponent(accountsBox, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 195, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(customerAccountsBox, javax.swing.GroupLayout.DEFAULT_SIZE, 230, Short.MAX_VALUE)
                            .addComponent(accountBalancesBox, javax.swing.GroupLayout.DEFAULT_SIZE, 230, Short.MAX_VALUE))))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(customersBox, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(accountsBox, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(accountBalancesBox)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(customerAccountsBox)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(accountStatementsBox)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(extractButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jSeparator2)
                    .addComponent(cancelButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(progressBar, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    private void extractButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_extractButtonActionPerformed
    {//GEN-HEADEREND:event_extractButtonActionPerformed
        // TODO add your handling code here:
        new Thread(this::extractFiles).start();
}//GEN-LAST:event_extractButtonActionPerformed

    private void cancelButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_cancelButtonActionPerformed
    {//GEN-HEADEREND:event_cancelButtonActionPerformed
        // TODO add your handling code here:
        hideFilesDialog();
}//GEN-LAST:event_cancelButtonActionPerformed

    @Override
    public void run()
    {
        Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
        while (true)
        {
            try
            {
                sendOfflineFiles();
                Thread.sleep(PHController.mobBalancesFileUpdateIntervalHH * 3600000);
                
            }
            catch (Exception ex)
            {
                ex = null;
            }
        }
    }

    public void processCustomers()
    {
        StringBuilder buffer = new StringBuilder();
        if (customersBox.isSelected())
        {
            progressBar.setString("Extracting Customer Data...");
            customersBox.setForeground(Color.red);
            customersBox.updateUI();

            try (ResultSet rset = tDClient.executeQueryToResultSet("SELECT A.CUST_NO AS CUSTOMER_ID, ' ' AS NATIONAL_ID, (SELECT NVL(SUBSTR(TITLE_DESC,1,10), ' ') FROM " + PHController.CoreSchemaName + ".TITLE_REF WHERE TITLE_ID=A.TITLE_ID) AS TITLE, "
                    + "NVL(A.FIRST_NM, ' ') AS C1_FIRST_NAME, NVL(SUBSTR(A.MIDDLE_NM,1,10), ' ') AS C1_INITIALS, NVL(A.LAST_NM, ' ') AS C1_LAST_NAME, NVL(A.CUST_NM, ' ') AS C1_NAME_ON_CARD, "
                    + "' ' AS C2_TITLE, ' ' AS C2_FIRST_NAME, ' ' AS C2_INITIALS, ' ' AS C2_LAST_NAME, ' ' AS C2_NAME_ON_CARD, ' ' AS C3_TITLE, ' ' AS C3_FIRST_NAME, "
                    + "' ' AS C3_INITIALS, ' ' AS C3_LAST_NAME, ' ' AS C3_NAME_ON_CARD, ' ' AS TELEPHONE_NUMBER, ' ' AS MOBILE_TELEPHONE_NUMBER, ' ' AS FAX_NUMBER, "
                    + "' ' AS EMAIL_ADDRESS, NVL(A.ADDR_LINE_1, ' ') AS POSTAL_ADDRESS_1, ' ' AS POSTAL_ADDRESS_2, NVL(A.CITY, ' ') AS POSTAL_CITY, ' ' AS POSTAL_REGION, ' ' AS POSTAL_CODE, "
                    + "' ' AS POSTAL_COUNTRY, ' ' AS OTHER_ADDRESS_1, ' ' AS OTHER_ADDRESS_2, ' ' AS OTHER_CITY, "
                    + "' ' AS OTHER_REGION, ' ' AS OTHER_POSTAL_CODE, ' ' AS OTHER_COUNTRY, ' ' AS DATE_OF_BIRTH, ' ' AS COMPANY_NAME, ' ' AS PREFERRED_LANGUAGE, '0' AS VIP, "
                    + "' ' AS VIP_LAPSE_DATE, ' ' AS EXTENDED_FIELDS, B.ACCT_NO AS CHG_ACCT_NO FROM " + PHController.CoreSchemaName + ".V_CUSTOMER A, " + PHController.CoreSchemaName + ".ACCOUNT B WHERE A.CUST_ID=B.CUST_ID "
                    + "AND B.ACCT_NO IN (SELECT ACCT_NO FROM " + PHController.CoreSchemaName + ".V_DEPOSIT_ACCOUNT_DETAIL WHERE PROD_ID IN (" + PHController.mobAllowedProductIDs + "))"))
            {
                while (rset.next())
                {
                    buffer.append(rset.getString(1).trim().replaceAll(",", " ")).append(",").append(rset.getString(2).trim().replaceAll(",", " ")).append(",").append(String.valueOf(rset.getString(3)).replace("null", "").trim().replaceAll(",", " ").replaceAll("'", "")).append(",")
                            .append(rset.getString(4).trim().replaceAll(",", " ").replaceAll("'", "")).append(",").append(rset.getString(5).trim().replaceAll(",", " ").replaceAll("'", "")).append(",").append(rset.getString(6).trim().replaceAll(",", " ").replaceAll("'", ""))
                            .append(",").append(rset.getString(7).trim().replaceAll(",", " ").replaceAll("'", "")).append(",").append(rset.getString(8).trim().replaceAll(",", " ")).append(",").append(rset.getString(9).trim().replaceAll(",", " "))
                            .append(",").append(rset.getString(10).trim().replaceAll(",", " ")).append(",").append(rset.getString(11).trim().replaceAll(",", " ")).append(",").append(rset.getString(12).trim().replaceAll(",", " "))
                            .append(",").append(rset.getString(13).trim().replaceAll(",", " ")).append(",").append(rset.getString(14).trim().replaceAll(",", " ")).append(",").append(rset.getString(15).trim().replaceAll(",", " "))
                            .append(",").append(rset.getString(16).trim().replaceAll(",", " ")).append(",").append(rset.getString(17).trim().replaceAll(",", " ")).append(",").append(rset.getString(18).trim().replaceAll(",", " "))
                            .append(",").append(rset.getString(19).trim().replaceAll(",", " ")).append(",").append(rset.getString(20).trim().replaceAll(",", " ")).append(",").append(rset.getString(21).trim().replaceAll(",", " "))
                            .append(",").append(rset.getString(22).trim().replaceAll(",", " ")).append(",").append(rset.getString(23).trim().replaceAll(",", " ")).append(",").append(rset.getString(24).trim().replaceAll(",", " "))
                            .append(",").append(rset.getString(25).trim().replaceAll(",", " ")).append(",").append(rset.getString(26).trim().replaceAll(",", " ")).append(",").append(rset.getString(27).trim().replaceAll(",", " "))
                            .append(",").append(rset.getString(28).trim().replaceAll(",", " ")).append(",").append(rset.getString(29).trim().replaceAll(",", " ")).append(",").append(rset.getString(30).trim().replaceAll(",", " "))
                            .append(",").append(rset.getString(31).trim().replaceAll(",", " ")).append(",").append(rset.getString(32).trim().replaceAll(",", " ")).append(",").append(rset.getString(33).trim().replaceAll(",", " "))
                            .append(",").append(rset.getString(34).trim().replaceAll(",", " ")).append(",").append(rset.getString(35).trim().replaceAll(",", " ")).append(",").append(rset.getString(36).trim().replaceAll(",", " "))
                            .append(",").append(rset.getString(37).trim().replaceAll(",", " ")).append(",").append(rset.getString(38).trim().replaceAll(",", " ")).append(",").append(rset.getString(39).trim().replaceAll(",", " ")).append("\r\n");
                }

                if (buffer.length() > 0)
                {
                    if (writeBufferToFile(buffer, PHController.mobCustomersFileURL, "CUSTOMERS" + formatter.format(timestamp) + ".csv"))
                    {
                        customersBox.setForeground(Color.blue);
                        customersBox.setText("Customers File Extracted");
                    }
                    else
                    {
                        customersBox.setText("Customers File Failed");
                    }
                }
                else
                {
                    customersBox.setText("Customers File Empty");
                }
            }
            catch (Exception e)
            {
                customersBox.setText("Customers File Failed");
                logError(e);
            }
            tDClient.dispose();
        }
    }

    public void processAccounts()
    {
        StringBuilder buffer = new StringBuilder();
        if (accountsBox.isSelected())
        {
            progressBar.setString("Extracting Accounts Data...");
            accountsBox.setForeground(Color.red);
            accountsBox.updateUI();

            try (ResultSet rset = tDClient.executeQueryToResultSet("SELECT A.ACCT_NO, D.PROD_SUBCAT_TY, E.CRNCY_CD, ' ' AS HOLD_RESPONSE_CODE, ' ' AS ACCOUNT_PRODUCT, ' ' AS EXTENDED_FIELDS, ' ' AS OVERDRAFT_LIMIT FROM " + PHController.CoreSchemaName + ".DEPOSIT_ACCOUNT_SUMMARY A, " + PHController.CoreSchemaName + ".ACCOUNT B, " + PHController.CoreSchemaName + ".PRODUCT C, " + PHController.CoreSchemaName + ".PRODUCT_TEMPLATE D, " + PHController.CoreSchemaName + ".CURRENCY E WHERE B.ACCT_ID=A.DEPOSIT_ACCT_ID AND C.PROD_ID=B.PROD_ID AND D.PROD_TEMPLATE_ID=C.PROD_TEMPLATE_ID AND E.CRNCY_ID=B.CRNCY_ID AND A.ACCT_NO IN (SELECT ACCT_NO FROM " + PHController.CoreSchemaName + ".V_DEPOSIT_ACCOUNT_DETAIL WHERE PROD_ID IN (" + PHController.mobAllowedProductIDs + "))"))
            {
                while (rset.next())
                {
                    buffer.append(rset.getString("ACCT_NO")).append(",").append("SAV".equalsIgnoreCase(rset.getString("PROD_SUBCAT_TY")) ? "10" : "20").append(",").append(PHController.getCurrency(rset.getString("CRNCY_CD"))).append(",").append(rset.getString("HOLD_RESPONSE_CODE").trim()).append(",").append(rset.getString("ACCOUNT_PRODUCT").trim()).append(",").append(rset.getString("EXTENDED_FIELDS").trim()).append(",").append(rset.getString("OVERDRAFT_LIMIT").trim()).append("\r\n");
                }

                if (buffer.length() > 0)
                {
                    if (writeBufferToFile(buffer, PHController.mobAccountsFileURL, "ACCOUNTS" + formatter.format(timestamp) + ".csv"))
                    {
                        accountsBox.setForeground(Color.blue);
                        accountsBox.setText("Accounts File Extracted");
                    }
                    else
                    {
                        accountsBox.setText("Accounts File Failed");
                    }
                }
                else
                {
                    accountsBox.setText("Accounts File Empty");
                }
            }
            catch (Exception e)
            {
                accountsBox.setText("Accounts File Failed");
                logError(e);
            }
            tDClient.dispose();
        }
    }

    public void processCustomerAccounts()
    {
        StringBuilder buffer = new StringBuilder();
        if (customerAccountsBox.isSelected())
        {
            progressBar.setString("Extracting Customer Accounts...");
            customerAccountsBox.setForeground(Color.red);
            customerAccountsBox.updateUI();

            try (ResultSet rset = tDClient.executeQueryToResultSet("SELECT E.CUST_NO, A.ACCT_NO, D.PROD_SUBCAT_TY FROM " + PHController.CoreSchemaName + ".DEPOSIT_ACCOUNT_SUMMARY A, " + PHController.CoreSchemaName + ".ACCOUNT B, " + PHController.CoreSchemaName + ".PRODUCT C, " + PHController.CoreSchemaName + ".PRODUCT_TEMPLATE D, " + PHController.CoreSchemaName + ".CUSTOMER E WHERE B.ACCT_ID=A.DEPOSIT_ACCT_ID AND C.PROD_ID=B.PROD_ID AND D.PROD_TEMPLATE_ID=C.PROD_TEMPLATE_ID AND E.CUST_ID=B.CUST_ID AND A.ACCT_NO IN (SELECT ACCT_NO FROM " + PHController.CoreSchemaName + ".V_DEPOSIT_ACCOUNT_DETAIL WHERE PROD_ID IN (" + PHController.mobAllowedProductIDs + "))"))
            {
                while (rset.next())
                {
                    buffer.append(rset.getString("CUST_NO")).append(",").append(rset.getString("ACCT_NO")).append(",").append("SAV".equalsIgnoreCase(rset.getString("PROD_SUBCAT_TY")) ? "10" : "20").append("\r\n");
                }
                if (buffer.length() > 0)
                {
                    if (writeBufferToFile(buffer, PHController.mobCustomerAccountsFileURL, "CUSTOMERACCOUNTS" + formatter.format(timestamp) + ".csv"))
                    {
                        customerAccountsBox.setForeground(Color.blue);
                        customerAccountsBox.setText("Customer Accounts File Extracted");
                    }
                    else
                    {
                        customerAccountsBox.setText("Customer Accounts File Failed");
                    }
                }
                else
                {
                    customerAccountsBox.setText("Customer Accounts File Empty");
                }
            }
            catch (Exception e)
            {
                customerAccountsBox.setText("Customer Accounts File Failed");
                logError(e);
            }

            tDClient.dispose();
        }
    }

    public void processAccountStatements()
    {
        StringBuilder buffer = new StringBuilder();
        if (accountStatementsBox.isSelected() && "Y".equalsIgnoreCase(PHController.mobSendStatementsFile))
        {
            progressBar.setString("Extracting Account Statements...");
            accountStatementsBox.setForeground(Color.red);
            accountStatementsBox.updateUI();

            try (ResultSet rset = tDClient.executeQueryToResultSet("SELECT A.VALUE_DT, A.DR_CR_IND, A.TXN_AMT, A.ACCT_NO, D.PROD_SUBCAT_TY, NVL(A.CHQ_NO,0), A.TRAN_JOURNAL_ID, A.TRAN_DESC, A.EVENT_CD, A.ACCT_CRNCY_CD_ISO FROM " + PHController.CoreSchemaName + ".V_DEPOSIT_ACCOUNT_HISTORY A, " + PHController.CoreSchemaName + ".ACCOUNT B, " + PHController.CoreSchemaName + ".PRODUCT C, " + PHController.CoreSchemaName + ".PRODUCT_TEMPLATE D WHERE A.TRAN_DT>SYSDATE-90 AND B.ACCT_ID=A.DEPOSIT_ACCT_ID AND C.PROD_ID=B.PROD_ID AND D.PROD_TEMPLATE_ID=C.PROD_TEMPLATE_ID AND A.ACCT_NO IN (SELECT ACCT_NO FROM " + PHController.CoreSchemaName + ".V_DEPOSIT_ACCOUNT_DETAIL WHERE PROD_ID IN (" + PHController.mobAllowedProductIDs + "))"))
            {
                while (rset.next())
                {
                    buffer.append(rset.getString(4).trim().replaceAll(",", " ").replaceAll("-", "")).append(",").append("DR".equals(rset.getString(2).trim().toUpperCase()) ? "01" : "21").append(",")
                            .append(rset.getBigDecimal(3).setScale(2, RoundingMode.DOWN).toPlainString().replace(".", "")).append(",").append(new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").format(rset.getDate(1))).append(",")
                            .append(new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").format(rset.getDate(1))).append(",").append("SAV".equalsIgnoreCase(rset.getString(5)) ? "10" : "20").append(",,,,")
                            .append(rset.getInt(6) < 0 ? "" : rset.getInt(6)).append(",,,,").append(rset.getString(8).trim().replaceAll(",", " ").replaceAll("'", "")).append(",").append("\r\n");
                }
                if (buffer.length() > 0)
                {
                    if (writeBufferToFile(buffer, PHController.mobAccountStatementsFileURL, "STATEMENTS" + formatter.format(timestamp) + ".csv"))
                    {
                        accountStatementsBox.setForeground(Color.blue);
                        accountStatementsBox.setText("Account Statements File Extracted");
                    }
                    else
                    {
                        accountStatementsBox.setText("Account Statements File Failed");
                    }
                }
                else
                {
                    accountStatementsBox.setText("Account Statements File Empty");
                }
            }
            catch (Exception e)
            {
                accountStatementsBox.setText("Account Statements File Failed");
                logError(e);
            }
        }
    }

    public void processAccountBalances()
    {
        StringBuilder buffer = new StringBuilder();
        if (accountBalancesBox.isSelected())
        {
            progressBar.setString("Extracting Account Balances...");
            accountBalancesBox.setForeground(Color.red);
            accountBalancesBox.updateUI();

            try (ResultSet rset = tDClient.executeQueryToResultSet("select CUST_NO,ACCT_NO,LEDGER_BAL,AVAILABLE_BAL ,REC_ST FROM " + PHController.CoreSchemaName + ".V_POS_BAL_FILE "))
            {
                while (rset.next())
                {  
                    buffer.append(encrypt(rset.getString("CUST_NO").replaceFirst("^0+(?!$)", "") + "," + rset.getString("ACCT_NO") + "," + rset.getBigDecimal("LEDGER_BAL").setScale(0, RoundingMode.DOWN).toPlainString().replace(".", "") + "," + rset.getBigDecimal("AVAILABLE_BAL").setScale(0, RoundingMode.DOWN).toPlainString().replace(".", "") + "," + ("A".equalsIgnoreCase(rset.getString("REC_ST")) ? "0" : "1"))).append("\r\n");
                }
                if (buffer.length() > 0)
                {
                    if (writeBufferToFile(buffer, PHController.mobBalancesFileURL, "ACCOUNTBALANCES" + formatter.format(timestamp) + ".csv"))
                    {
                        accountBalancesBox.setForeground(Color.blue);
                        accountBalancesBox.setText("Account Balances File Extracted");
                    }
                    else
                    {
                        accountBalancesBox.setText("Account Balances File Failed");
                    }
                }
                else
                {
                    accountBalancesBox.setText("Account Balances File Empty");
                }
            }
            catch (Exception e)
            {
                accountBalancesBox.setText("Account Balances File Failed");
                logError(e);
            }
            tDClient.dispose();
        }
    }

    private boolean writeBufferToFile(StringBuilder buffer, String directory, String fileName)
    {
        try
        {
            bRFile.writeToFile(new File(directory, fileName), buffer.toString());
            return true;
        }
        catch (Exception ex)
        {
            logError(ex);
        }
        return false;
    }

    public void showFilesDialog()
    {
        if (filesDialog == null)
        {
            filesDialog = new JDialog(PHMain.phFrame, "Switch Load Files");
            filesDialog.setIconImage(PHMain.phFrame.getIconImage());

            filesDialog.setContentPane(this);
            filesDialog.pack();

            filesDialog.setResizable(false);
            filesDialog.setLocationRelativeTo(PHMain.phFrame);
            filesDialog.setVisible(true);
        }
        else
        {
            filesDialog.setVisible(true);
        }
    }

    private void extractFiles()
    {
        progressBar.setIndeterminate(true);
        processCustomers();
        processAccounts();
        processAccountBalances();
        processCustomerAccounts();
        processAccountStatements();
        progressBar.setIndeterminate(false);
        progressBar.setValue(100);
        progressBar.setString("100% Completed");
    }

    public void sendOfflineFiles()
    {
        accountBalancesBox.setSelected(true);
        accountStatementsBox.setSelected(true);
        extractFiles();
        tDClient.dispose();
    }

    public void hideFilesDialog()
    {
        filesDialog.setVisible(false);
    }

    private void logError(Exception e)
    {
        if (PHMain.mobBridgeLog != null)
        {
            PHMain.mobBridgeLog.error(e);
        }
        else
        {
            e.printStackTrace();
        }
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JCheckBox accountBalancesBox;
    private javax.swing.JCheckBox accountStatementsBox;
    private javax.swing.JCheckBox accountsBox;
    private javax.swing.JButton cancelButton;
    private javax.swing.JCheckBox customerAccountsBox;
    private javax.swing.JCheckBox customersBox;
    private javax.swing.JButton extractButton;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JProgressBar progressBar;
    // End of variables declaration//GEN-END:variables
}
