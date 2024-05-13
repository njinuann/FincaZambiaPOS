/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package APX;

import Mobile.CRWorkerMOB;
import Mobile.FLWorkerMOB;
import Mobile.MOBReqService;
import PHilae.CRWorker;
import PHilae.FLWorker;
import SMS.SMSProcessor;
//import PHilae.*;
import com.jgoodies.looks.plastic.PlasticLookAndFeel;
import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
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
public class PHMain
{

    public static Q2 posBridgeQ2 = null;
    public static Q2 mobBridgeQ2 = null;

    public static Log posBridgeLog = null;
    public static Log mobBridgeLog = null;

    public static String posBridgeRealm = "pos";
    public static String mobBridgeRealm = "Mobile";

    public static PHFrame phFrame = null;

    public static ISOMeter posBridgeIsoMeter = null;
    public static ISOMeter mobBridgeIsoMeter = null;

    public static String posBridgeLogger = "logger";
    public static String mobBridgeLogger = "mobilelogger";

    private static final String posBridgeName = "philae";
    private static final String mobBridgeName = "Mobile";

    public static boolean isPosRunning = false;
    public static boolean isMobRunning = false;

    public static IDisplay displayArea;
    public static Color infoColor = new Color(0, 0, 128);

    public static Color errorColor = new Color(192, 0, 0);
    public static final javax.swing.JDialog consoleDialog = new javax.swing.JDialog();

    public static AXLogger smsLog = new AXLogger("sms", "sms" + File.separator + "logs");
    public static AXLogger apxLog = new AXLogger("apx", "apx" + File.separator + "logs");

    public static boolean exit = false;

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args)
    {
        setOutput();
        new PHMain().execute();
    }

    public static void setOutput()
    {
        setLookAndFeel();
        displayArea = new IDisplay();

        System.setOut(new EIPrint(new EIStream(infoColor)));
        System.setErr(new EIPrint(new EIStream(errorColor)));

        consoleDialog.setTitle("PHilae Bridge");
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
        new MOBReqService().startSoap();

    }

    public static ListModel loadPosBridgeTxnLog()
    {
        try
        {
            File txnFile = new File("txn/txnlog.ism");
            if (txnFile.exists())
            {
                Object txnLog = new ObjectInputStream(new FileInputStream(txnFile)).readObject();
                if (txnLog instanceof ListModel || txnLog instanceof DefaultListModel)
                {
                    if (posBridgeIsoMeter != null && !"Y".equalsIgnoreCase(PHController.SuspendPOS))
                    {
                        posBridgeIsoMeter.setLogList((ListModel) txnLog);
                    }
                    return ((ListModel) txnLog);
                }
            }
        }
        catch (Exception ex)
        {
            System.out.println("<exception realm=\"" + posBridgeRealm + "\">unable to load recent transactions log ~ " + ex.getMessage() + " [defaulting to new log]</exception>");
        }
        if (posBridgeIsoMeter != null && !"Y".equalsIgnoreCase(PHController.SuspendPOS))
        {
            posBridgeIsoMeter.setLogList(new DefaultListModel());
        }
        return (new DefaultListModel());
    }

    public static ListModel loadMobBridgeTxnLog()
    {
        try
        {
            File txnFile = new File("mobile/txn/txnlog.ism");
            if (txnFile.exists())
            {
                Object txnLog = new ObjectInputStream(new FileInputStream(txnFile)).readObject();
                if (txnLog instanceof ListModel || txnLog instanceof DefaultListModel)
                {
                    if (mobBridgeIsoMeter != null && !"Y".equalsIgnoreCase(PHController.SuspendMOB))
                    {
                        mobBridgeIsoMeter.setLogList((ListModel) txnLog);
                    }
                    return ((ListModel) txnLog);
                }
            }
        }
        catch (Exception ex)
        {
            System.out.println("<exception realm=\"" + mobBridgeRealm + "\">unable to load recent transactions log ~ " + ex.getMessage() + " [defaulting to new log]</exception>");
        }
        if (mobBridgeIsoMeter != null && !"Y".equalsIgnoreCase(PHController.SuspendMOB))
        {
            mobBridgeIsoMeter.setLogList(new DefaultListModel());
        }
        return (new DefaultListModel());
    }

    private static void archivePosBridgeTxnLog()
    {
        File txnDir = new File("txn");
        if (!txnDir.exists())
        {
            txnDir.mkdir();
        }

        try ( ObjectOutputStream os = new ObjectOutputStream(new FileOutputStream(new File(txnDir, "txnlog.ism"))))
        {
            os.writeObject(posBridgeIsoMeter.getLogList());
            os.flush();
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

    private static void archiveMobBridgeTxnLog()
    {
        File txnDir = new File("mobile/txn");
        if (!txnDir.exists())
        {
            txnDir.mkdir();
        }

        try ( ObjectOutputStream os = new ObjectOutputStream(new FileOutputStream(new File(txnDir, "txnlog.ism"))))
        {
            os.writeObject(mobBridgeIsoMeter.getLogList());
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
            Logger.getLogger(PHMain.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static void shutdownPosBridge()
    {

        if (posBridgeQ2 != null)
        {
            archivePosBridgeTxnLog();
            posBridgeQ2.shutdown();
            posBridgeQ2 = null;

            posBridgeIsoMeter = new ISOMeter(null, PHController.posModule, "SUSPENDED");
            phFrame.setPosBridgeIsoMeter(posBridgeIsoMeter);
        }
    }

    public static void shutdownMobBridge()
    {

        if (mobBridgeQ2 != null)
        {
            archiveMobBridgeTxnLog();
            mobBridgeQ2.shutdown();
            mobBridgeQ2 = null;

            mobBridgeIsoMeter = new ISOMeter(null, PHController.MobModule, "SUSPENDED");
            phFrame.setMobBridgeIsoMeter(mobBridgeIsoMeter);
        }
    }

//    public static void saveAllSettings()
//    {
//        PHController.saveSettings();//revisit
//    }
    public static void shutdownChannels()
    {
        exit = true;
//        saveAllSettings();
        shutdownPosBridge();
        shutdownMobBridge();
    }

    public static void exit()
    {
        System.exit(0);

    }

    public static void startPosBridge()
    {
        try
        {
            posBridgeQ2 = new Q2(PHController.confDir, PHController.logsDir, posBridgeLogger, posBridgeRealm);
            posBridgeLog = posBridgeQ2.getLog();

            posBridgeQ2.start();

            while (posBridgeQ2.getIsoMeter(posBridgeName) == null)
            {
                pauseThread(50);
            }

            posBridgeIsoMeter = posBridgeQ2.getIsoMeter(posBridgeName);
            posBridgeIsoMeter.setCaption(PHController.posModule);
            posBridgeIsoMeter.setStatus("OPEN");

            if (phFrame != null)
            {
                phFrame.setPosBridgeIsoMeter(posBridgeIsoMeter);
            }
            loadPosBridgeTxnLog();
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

            mobBridgeIsoMeter = mobBridgeQ2.getIsoMeter(mobBridgeName);
            mobBridgeIsoMeter.setCaption(PHController.MobModule);
            mobBridgeIsoMeter.setStatus("OPEN");

            if (phFrame != null)
            {
                phFrame.setMobBridgeIsoMeter(mobBridgeIsoMeter);
            }
            loadMobBridgeTxnLog();
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

        phFrame = new PHFrame();
        java.awt.EventQueue.invokeLater(()
                ->
        {
            isPosRunning = true;
            if (posBridgeIsoMeter != null)
            {
                phFrame.setPosBridgeIsoMeter(posBridgeIsoMeter);
            }
            else
            {
                posBridgeIsoMeter = phFrame.getPosBridgeIsoMeter();
            }
            if (mobBridgeIsoMeter != null)
            {
                phFrame.setMobBridgeIsoMeter(mobBridgeIsoMeter);
            }
            else
            {
                mobBridgeIsoMeter = phFrame.getMobBridgeIsoMeter();
            }

            consoleDialog.setVisible(false);
            phFrame.setLocationRelativeTo(null);

            phFrame.setVisible(true);
            consoleDialog.dispose();
        });
    }

    public static void restartBridge()
    {
        shutdownPosBridge();
        shutdownMobBridge();
//        saveAllSettings();
        if (!"Y".equalsIgnoreCase(PHController.SuspendPOS))
        {
            startPosBridge();
        }
        if (!"Y".equalsIgnoreCase(PHController.SuspendMOB))
        {
            startMobBridge();
        }
    }

    public static void startChannels()
    {
        if (!"Y".equalsIgnoreCase(PHController.SuspendPOS))
        {
            startPosBridge();
        }
        if (!"Y".equalsIgnoreCase(PHController.SuspendMOB))
        {
            startMobBridge();
        }
    }

    private void startWorkers()
    {

        new Thread(new SMSProcessor()).start();
        new Thread(new FLWorker()).start();
        new Thread(new CRWorker()).start();
        new Thread(new FLWorkerMOB()).start();
        //   new Thread(new CRWorkerMOB()).start();
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
        isPosRunning = false;
        isMobRunning = false;
        dispose();
        exit();
    }

    private static void dispose()
    {
        PHController.disposePosProcessors();
        PHController.disposeMobProcessors();
    }
}
