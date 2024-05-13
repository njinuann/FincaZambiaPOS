/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Ruby.model;

/**
 *
 * @author alexn
 */
public class WFWorkItemDetail
{
    private Long queueId =0L;
    private Long workItemId =0L;
    private Long nextEventId =0L;
    private Long prevEventId =0L; 
    private String eventDesc = "DONE";

    /**
     * @return the queueId
     */
    public Long getQueueId()
    {
        return queueId;
    }

    /**
     * @param queueId the queueId to set
     */
    public void setQueueId(Long queueId)
    {
        this.queueId = queueId;
    }

    /**
     * @return the workItemId
     */
    public Long getWorkItemId()
    {
        return workItemId;
    }

    /**
     * @param workItemId the workItemId to set
     */
    public void setWorkItemId(Long workItemId)
    {
        this.workItemId = workItemId;
    }

    /**
     * @return the nextEventId
     */
    public Long getNextEventId()
    {
        return nextEventId;
    }

    /**
     * @param nextEventId the nextEventId to set
     */
    public void setNextEventId(Long nextEventId)
    {
        this.nextEventId = nextEventId;
    }

    /**
     * @return the prevEventId
     */
    public Long getPrevEventId()
    {
        return prevEventId;
    }

    /**
     * @param prevEventId the prevEventId to set
     */
    public void setPrevEventId(Long prevEventId)
    {
        this.prevEventId = prevEventId;
    }

    /**
     * @return the eventDesc
     */
    public String getEventDesc()
    {
        return eventDesc;
    }

    /**
     * @param eventDesc the eventDesc to set
     */
    public void setEventDesc(String eventDesc)
    {
        this.eventDesc = eventDesc;
    }
    
}
