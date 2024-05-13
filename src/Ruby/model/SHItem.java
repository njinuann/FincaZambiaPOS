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
public class SHItem
{
    private String itemId;
    private String language;
    private String itemType;
    private String parentId;
    private String itemCode;
    private String shortName;
    private String itemDetail;

    /**
     * @return the itemId
     */
    public String getItemId()
    {
        return itemId;
    }

    /**
     * @param itemId the itemId to set
     */
    public void setItemId(String itemId)
    {
        this.itemId = itemId;
    }

    /**
     * @return the language
     */
    public String getLanguage()
    {
        return language;
    }

    /**
     * @param language the language to set
     */
    public void setLanguage(String language)
    {
        this.language = language;
    }

    /**
     * @return the itemType
     */
    public String getItemType()
    {
        return itemType;
    }

    /**
     * @param itemType the itemType to set
     */
    public void setItemType(String itemType)
    {
        this.itemType = itemType;
    }

    /**
     * @return the parentId
     */
    public String getParentId()
    {
        return parentId;
    }

    /**
     * @param parentId the parentId to set
     */
    public void setParentId(String parentId)
    {
        this.parentId = parentId;
    }

    /**
     * @return the shortName
     */
    public String getShortName()
    {
        return shortName;
    }

    /**
     * @param shortName the shortName to set
     */
    public void setShortName(String shortName)
    {
        this.shortName = shortName;
    }

    /**
     * @return the itemDetail
     */
    public String getItemDetail()
    {
        return itemDetail;
    }

    /**
     * @param itemDetail the itemDetail to set
     */
    public void setItemDetail(String itemDetail)
    {
        this.itemDetail = itemDetail;
    }

    /**
     * @return the itemCode
     */
    public String getItemCode()
    {
        return itemCode;
    }

    /**
     * @param itemCode the itemCode to set
     */
    public void setItemCode(String itemCode)
    {
        this.itemCode = itemCode;
    }
}
