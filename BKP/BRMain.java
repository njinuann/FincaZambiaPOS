/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package PHilae;

import APX.CPanel;
import APX.IDisplay;
import APX.EIPrint;
import APX.EIStream;
import com.jgoodies.looks.plastic.PlasticLookAndFeel;
import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.DefaultListModel;
import javax.swing.ListModel;
import javax.swing.UIManager;
import org.jpos.iso.gui.ISOMeter;
import org.jpos.q2.Q2;
import org.jpos.util.Log;

/**
 *
 * @author Pecherk
 */
public class BRMain
{

    public static Q2 bridgeQ2 = null;
    public static Log bridgeLog = null;

    public static boolean isRunning = false;
    public static String bridgeRealm = "pos";

    public static BRFrame brFrame = null;
    public static ISOMeter bridgeIsoMeter = null;

    public static String bridgeLogger = "logger";
    private static final String posBridgeName = "philae";

    public static Q2 mobBridgeQ2 = null;
    public static Log mobBridgeLog = null;

  
    public static String mobBridgeRealm = "Mobile";    
    public static ISOMeter mobbridgeIsoMeter = null;

    public static String mobBridgeLogger = "mobilelogger";
    private static final String mobBridgeName = "Mobile";
    
    public static IDisplay displayArea;
    public static Color infoColor = new Color(0, 0, 128);

    public static Color errorColor = new Color(192, 0, 0);
    public static final javax.swing.JDialog consoleDialog = new javax.swing.JDialog();

    public static String apxLog = "logger";

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args)
    {
        setOutput();    
        new BRMain().execute();
    }

    public static void setOutput()
    {
        setLookAndFeel();
        displayArea = new IDisplay();

        System.setOut(new EIPrint(new EIStream(infoColor)));
        System.setErr(new EIPrint(new EIStream(errorColor)));

        consoleDialog.setTitle("PHilae POS Bridge");
        CPanel panel = new CPanel();

        javax.swing.GroupLayout consoleDialogLayout = new javax.swing.GroupLayout(consoleDialog.getContentPane());
        consoleDialog.getContentPane().setLayout(consoleDialogLayout);

        consoleDialogLayout.setHorizontalGroup(
                consoleDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(panel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        consoleDialogLayout.setVerticalGroup(
                consoleDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(panel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        consoleDialog.setSize(500, 500);
        consoleDialog.setUndecorated(true);

        consoleDialog.setLocationRelativeTo(null);
        consoleDialog.setVisible(true);
    }

    public void execute()
    {
        PHController.initialize();
        startChannels();
        displayWindow();
        startWorkers();
    }

    public static ListModel loadBridgeTxnLog()
    {
        try
        {
            File txnFile = new File("txn/txnlog.ism");
            if (txnFile.exists())
            {
                Object txnLog = new ObjectInputStream(new FileInputStream(txnFile)).readObject();
                if (txnLog instanceof ListModel || txnLog instanceof DefaultListModel)
                {
                    if (bridgeIsoMeter != null && !"Y".equalsIgnoreCase(PHController.SuspendPOS))
                    {
                        bridgeIsoMeter.setLogList((ListModel) txnLog);
                    }
                    return ((ListModel) txnLog);
                }
            }
        }
        catch (Exception ex)
        {
            System.out.println("<exception realm=\"" + bridgeRealm + "\">unable to load recent transactions log ~ " + ex.getMessage() + " [defaulting to new log]</exception>");
        }
        if (bridgeIsoMeter != null && !"Y".equalsIgnoreCase(PHController.SuspendPOS))
        {
            bridgeIsoMeter.setLogList(new DefaultListModel());
        }
        return (new DefaultListModel());
    }

    private static void archiveBridgeTxnLog()
    {
        File txnDir = new File("txn");
        if (!txnDir.exists())
        {
            txnDir.mkdir();
        }

        try (ObjectOutputStream os = new ObjectOutputStream(new FileOutputStream(new File(txnDir, "txnlog.ism"))))
        {
            os.writeObject(bridgeIsoMeter.getLogList());
            os.flush();
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

    private static void setLookAndFeel()
    {
        try
        {
            UIManager.setLookAndFeel(new PlasticLookAndFeel());
        }
        catch (Exception ex)
        {
            Logger.getLogger(BRMain.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static void shutdownBridge()
    {
        if (bridgeQ2 != null)
        {
            archiveBridgeTxnLog();
            bridgeQ2.shutdown();
            bridgeQ2 = null;

            bridgeIsoMeter = new ISOMeter(null, PHController.Module, "SUSPENDED");
            brFrame.setBridgeIsoMeter(bridgeIsoMeter);
        }
    }

    public static void saveAllSettings()
    {
        PHController.saveSettings();
    }

    public static void shutdownChannels()
    {
        saveAllSettings();
        shutdownBridge();
    }

    private static void exit()
    {
        System.exit(0);
        
    }

    public static void startPosBridge()
    {
        try
        {
            bridgeQ2 = new Q2(PHController.confDir, PHController.logsDir, bridgeLogger, bridgeRealm);
            bridgeLog = bridgeQ2.getLog();
            bridgeQ2.start();

            while (bridgeQ2.getIsoMeter(posBridgeName) == null)
            {
                pauseThread(50);
            }

            bridgeIsoMeter = bridgeQ2.getIsoMeter(posBridgeName);
            bridgeIsoMeter.setCaption(PHController.Module);
            bridgeIsoMeter.setStatus("OPEN");

            if (brFrame != null)
            {
                brFrame.setBridgeIsoMeter(bridgeIsoMeter);
            }
            loadBridgeTxnLog();
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

    public static void startMobBridge()
    {
        try
        {
            mobBridgeQ2 = new Q2(PHController.confDirMob, PHController.logsDirMob, mobBridgeLogger, mobBridgeRealm);
            mobBridgeLog = mobBridgeQ2.getLog();
            mobBridgeQ2.start();

            while (mobBridgeQ2.getIsoMeter(mobBridgeName) == null)
            {
                pauseThread(50);
            }

            mobbridgeIsoMeter = bridgeQ2.getIsoMeter(mobBridgeName);
            mobbridgeIsoMeter.setCaption(PHController.MobModule);
            mobbridgeIsoMeter.setStatus("OPEN");

            if (brFrame != null)
            {
                brFrame.setBridgeIsoMeter(mobbridgeIsoMeter);
            }
            loadBridgeTxnLog();
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

    
    public static void pauseThread(long ms)
    {
        try
        {
            Thread.sleep(ms);
        }
        catch (Exception ex)
        {
        }
    }

    private void displayWindow()
    {
        brFrame = new BRFrame();
        java.awt.EventQueue.invokeLater(()
                ->
                {
                    isRunning = true;
                    if (bridgeIsoMeter != null)
                    {
                        brFrame.setBridgeIsoMeter(bridgeIsoMeter);
                    }
                    else
                    {
                        bridgeIsoMeter = brFrame.getBridgeIsoMeter();
                    }
                    if (mobbridgeIsoMeter != null)
                    {
                        brFrame.setMobBridgeIsoMeter(mobbridgeIsoMeter);
                    }
                    else
                    {
                        mobbridgeIsoMeter = brFrame.getMobBridgeIsoMeter();
                    }

                    consoleDialog.setVisible(false);
                    brFrame.setLocationRelativeTo(null);

                    brFrame.setVisible(true);
                    consoleDialog.dispose();
                });
    }

    public static void restartBridge()
    {
        shutdownBridge();
        saveAllSettings();
        if (!"Y".equalsIgnoreCase(PHController.SuspendPOS))
        {
            startPosBridge();
        }
    }

    public static void startChannels()
    {
        if (!"Y".equalsIgnoreCase(PHController.SuspendPOS))
        {
            startPosBridge();
        }
    }

    private void startWorkers()
    {
       
       new Thread(new FLWorker()).start();
        new Thread(new CRWorker()).start();
        new Thread(this::cleanUp).start();
    }

    public void cleanUp()
    {
        while (true)
        {
            try
            {
                System.gc();
                Thread.sleep(900000);
            }
            catch (Exception ex)
            {
                ex = null;
            }
        }
    }

    public static void shutdown()
    {
        shutdownChannels();
        isRunning = false;
        dispose();
        exit();
    }

    private static void dispose()
    {
        PHController.disposeProcessors();
    }
}
