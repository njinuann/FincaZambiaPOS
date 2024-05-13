/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Ruby.model;

/**
 *
 * @author Pecherk
 */
public class CFValue
{
    private long valueId;
    private long fieldId;
    private long parentId;
    private String fieldValue;
    private String status;

    /**
     * @return the valueId
     */
    public long getValueId()
    {
        return valueId;
    }

    /**
     * @param valueId the valueId to set
     */
    public void setValueId(long valueId)
    {
        this.valueId = valueId;
    }

    /**
     * @return the fieldId
     */
    public long getFieldId()
    {
        return fieldId;
    }

    /**
     * @param fieldId the fieldId to set
     */
    public void setFieldId(long fieldId)
    {
        this.fieldId = fieldId;
    }

    /**
     * @return the parentId
     */
    public long getParentId()
    {
        return parentId;
    }

    /**
     * @param parentId the parentId to set
     */
    public void setParentId(long parentId)
    {
        this.parentId = parentId;
    }

    /**
     * @return the fieldValue
     */
    public String getFieldValue()
    {
        return fieldValue;
    }

    /**
     * @param fieldValue the fieldValue to set
     */
    public void setFieldValue(String fieldValue)
    {
        this.fieldValue = fieldValue;
    }

    /**
     * @return the status
     */
    public String getStatus()
    {
        return status;
    }

    /**
     * @param status the status to set
     */
    public void setStatus(String status)
    {
        this.status = status;
    }
}
