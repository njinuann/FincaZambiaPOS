/*
 * jPOS Project [http://jpos.org]
 * Copyright (C) 2000-2010 Alejandro P. Revilla
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.jpos.bsh;

import bsh.Interpreter;
import org.jdom.Element;
import org.jpos.ui.UI;
import org.jpos.ui.UIAware;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class BSHAction implements ActionListener, UIAware {
    public UI ui;

    public BSHAction () {
        super();
    }

    public void setUI (UI ui, Element e) {
        this.ui = ui;
    }

    public void actionPerformed (ActionEvent ev) {
        String bshSource = ev.getActionCommand();
        try {
            Interpreter bsh = new Interpreter ();
            bsh.source (bshSource);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

