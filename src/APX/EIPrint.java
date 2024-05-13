package APX;

import java.io.PrintStream;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates and open the template
 * in the editor.
 */
/**
 *
 * // * @author Pecherk
 */
public class EIPrint extends PrintStream
{
    private EIStream eiStream;

    public EIPrint(EIStream eiStream)
    {
        super(eiStream, true);
        setEiStream(eiStream);
    }

    /**
     * @return the eiStream
     */
    public EIStream getEiStream()
    {
        return eiStream;
    }

    /**
     * @param eiStream the eiStream to set
     */
    public final void setEiStream(EIStream eiStream)
    {
        this.eiStream = eiStream;
    }
}
