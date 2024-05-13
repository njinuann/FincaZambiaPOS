package Mobile;

import APX.PHController;
import org.jpos.core.Configurable;
import org.jpos.core.Configuration;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISORequestListener;
import org.jpos.iso.ISOSource;

public class TXNListenerMOB implements ISORequestListener, Configurable
{

    @Override
    public boolean process(ISOSource isosource, ISOMsg isomsg)
    {
        new Thread(()
                ->
        {
            mobServe(isosource, isomsg);
        }).start();
        return true;
    }

    private void mobServe(ISOSource isosource, ISOMsg isomsg)
    {
        PHController.fetchMobProcessor().process(isosource, isomsg);
    }

    @Override
    public void setConfiguration(Configuration configuration)
    {
    }
}
