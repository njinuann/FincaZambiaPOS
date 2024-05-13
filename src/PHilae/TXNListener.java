package PHilae;

import APX.PHController;
import javax.swing.SwingWorker;
import org.jpos.core.Configurable;
import org.jpos.core.Configuration;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISORequestListener;
import org.jpos.iso.ISOSource;

public class TXNListener implements ISORequestListener, Configurable
{

    @Override
    public boolean process(ISOSource isosource, ISOMsg isomsg)
    {
        new Thread(()
            -> 
            { 
                serve(isosource, isomsg);
        }).start();

//        new SwingWorker<Void, Void>()
//        {
//            @Override
//            protected Void doInBackground() throws Exception
//            {
//                serve(isosource, isomsg);
//                return null;
//            }
//        }.execute();
        return true;
    }

    private void serve(ISOSource isosource, ISOMsg isomsg)
    {
        TXProcessor tXProcessor;
        PHController.fetchPosProcessor().process(isosource, isomsg);
    }

    @Override
    public void setConfiguration(Configuration configuration)
    {
    }
}
