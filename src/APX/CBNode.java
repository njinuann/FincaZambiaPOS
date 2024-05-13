/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package APX;

/**
 *
 * @author Pecherk
 */
public class CBNode implements Comparable<CBNode>
{
    private Long counter = 0L;
    private String wsContextURL;

    /**
     * @return the wsContextURL
     */
    public String getWsContextURL()
    {
        return wsContextURL;
    }

    /**
     * @param wsContextURL the wsContextURL to set
     */
    public void setWsContextURL(String wsContextURL)
    {
        this.wsContextURL = wsContextURL;
    }

    /**
     * @return the counter
     */
    public Long getCounter()
    {
        return counter;
    }

    /**
     * @param counter the counter to set
     */
    public void setCounter(Long counter)
    {
        this.counter = counter;
    }

    @Override
    public int compareTo(CBNode o)
    {
        return getCounter().compareTo(o.getCounter());
    }
}
