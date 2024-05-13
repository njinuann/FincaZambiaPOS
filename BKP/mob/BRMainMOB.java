/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package Mobile;

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
public class BRMainMOB
{

    public static Q2 mobBridgeQ2 = null;
    public static Log mobBridgeLog = null;

    public static boolean isRunning = false;
    public static String mobBridgeRealm = "Mobile";

    public static BRFrameMOB brFrame = null;
    public static ISOMeter mobbridgeIsoMeter = null;

    public static String mobBridgeLogger = "mobilelogger";
    private static final String mobbridgeBridgeName = "Mobile";

    public static IDisplayMOB displayArea;
    public static Color infoColor = new Color(0, 0, 128);

    public static Color errorColor = new Color(192, 0, 0);
    public static final javax.swing.JDialog consoleDialog = new javax.swing.JDialog();

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args)
    {
        setOutput();
        new BRMainMOB().execute();
    }

    public static void setOutput()
    {
        setLookAndFeel();
        displayArea = new IDisplayMOB();

        System.setOut(new EIPrintMOB(new EIStreamMOB(infoColor)));
        System.setErr(new EIPrintMOB(new EIStreamMOB(errorColor)));

        consoleDialog.setTitle("Mobile Bridge");
        CPanelMOB panel = new CPanelMOB();

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
        MOBController.initialize();
        startMobBridge();
        displayWindow();
        startWorkers();
        new MOBReqService().startSoap();

    }

    public static ListModel loadBridgeTxnLog()
    {
        try
        {
            File txnFile = new File("mobile/txn/txnlog.ism");
            if (txnFile.exists())
            {
                Object txnLog = new ObjectInputStream(new FileInputStream(txnFile)).readObject();
                if (txnLog instanceof ListModel || txnLog instanceof DefaultListModel)
                {
                    if (mobbridgeIsoMeter != null && !"Y".equalsIgnoreCase(MOBController.SuspendMOB))
                    {
                        mobbridgeIsoMeter.setLogList((ListModel) txnLog);
                    }
                    return ((ListModel) txnLog);
                }
            }
        }
        catch (Exception ex)
        {
            System.out.println("<exception realm=\"" + mobBridgeRealm + "\">unable to load recent transactions log ~ " + ex.getMessage() + " [defaulting to new log]</exception>");
        }
        if (mobbridgeIsoMeter != null && !"Y".equalsIgnoreCase(MOBController.SuspendMOB))
        {
            mobbridgeIsoMeter.setLogList(new DefaultListModel());
        }
        return (new DefaultListModel());
    }

    private static void archiveBridgeTxnLog()
    {
        File txnDir = new File("mobile/txn");
        if (!txnDir.exists())
        {
            txnDir.mkdir();
        }

        try (ObjectOutputStream os = new ObjectOutputStream(new FileOutputStream(new File(txnDir, "txnlog.ism"))))
        {
            os.writeObject(mobbridgeIsoMeter.getLogList());
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
            Logger.getLogger(BRMainMOB.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static void shutdownBridge()
    {
        if (mobBridgeQ2 != null)
        {
            archiveBridgeTxnLog();
            mobBridgeQ2.shutdown();
            mobBridgeQ2 = null;

            mobbridgeIsoMeter = new ISOMeter(null, MOBController.Module, "SUSPENDED");
            brFrame.setBridgeIsoMeter(mobbridgeIsoMeter);
        }
    }

    public static void saveAllSettings()
    {
        MOBController.saveSettings();
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

    public static void startMobBridge()
    {
        try
        {
            mobBridgeQ2 = new Q2(MOBController.confDir, MOBController.logsDir, mobBridgeLogger, mobBridgeRealm);
            mobBridgeLog = mobBridgeQ2.getLog();
            mobBridgeQ2.start();
            while (mobBridgeQ2.getIsoMeter(mobbridgeBridgeName) == null)
            {

                pauseThread(50);
            }
            mobbridgeIsoMeter = mobBridgeQ2.getIsoMeter(mobbridgeBridgeName);
            mobbridgeIsoMeter.setCaption(MOBController.Module);
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
        brFrame = new BRFrameMOB();
        java.awt.EventQueue.invokeLater(()
                ->
                {
                    isRunning = true;
                    if (mobbridgeIsoMeter != null)
                    {
                        brFrame.setBridgeIsoMeter(mobbridgeIsoMeter);
                    }
                    else
                    {
                        mobbridgeIsoMeter = brFrame.getBridgeIsoMeter();
                    }

                    consoleDialog.setVisible(false);
                    brFrame.setLocationRelativeTo(null);

                    brFrame.setVisible(true);
                    consoleDialog.dispose();
                });
    }

    public static void restartMobBridge()
    {
        shutdownBridge();
        saveAllSettings();
        if (!"Y".equalsIgnoreCase(MOBController.SuspendMOB))
        {
            startMobBridge();
        }
    }

    public static void startMobChannels()
    {
        if (!"Y".equalsIgnoreCase(MOBController.SuspendMOB))
        {
            startMobBridge();
        }
    }

    private void startWorkers()
    {
        //  new Thread(new FLWorker()).start();
        new Thread(new CRWorkerMOB()).start();
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
        MOBController.disposeProcessors();
    }
}
