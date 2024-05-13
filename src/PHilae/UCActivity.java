/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package PHilae;

import java.math.BigDecimal;

/**
 *
 * @author Pecherk
 */
public class UCActivity
{
    private BigDecimal velocity;
    private BigDecimal volume;

    /**
     * @return the velocity
     */
    public BigDecimal getVelocity()
    {
        return velocity;
    }

    /**
     * @param velocity the velocity to set
     */
    public void setVelocity(BigDecimal velocity)
    {
        this.velocity = velocity;
    }

    /**
     * @return the volume
     */
    public BigDecimal getVolume()
    {
        return volume;
    }

    /**
     * @param volume the volume to set
     */
    public void setVolume(BigDecimal volume)
    {
        this.volume = volume;
    }
}
