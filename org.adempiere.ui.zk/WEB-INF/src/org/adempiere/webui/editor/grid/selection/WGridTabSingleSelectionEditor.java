/*******************************************************************************
 * Copyright (C) 2016 Trek Global Inc.										   *
 * Copyright (C) 2016 Low Heng Sin                                             *
 * This program is free software; you can redistribute it and/or modify it     *
 * under the terms version 2 of the GNU General Public License as published    *
 * by the Free Software Foundation. This program is distributed in the hope    *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied  *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.            *
 * See the GNU General Public License for more details.                        *
 * You should have received a copy of the GNU General Public License along     *
 * with this program; if not, write to the Free Software Foundation, Inc.,     *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                      *
 *******************************************************************************/

package org.adempiere.webui.editor.grid.selection;

import org.adempiere.webui.ValuePreference;
import org.adempiere.webui.editor.WEditor;
import org.adempiere.webui.editor.WEditorPopupMenu;
import org.adempiere.webui.event.ContextMenuEvent;
import org.adempiere.webui.event.ContextMenuListener;
import org.adempiere.webui.event.ValueChangeEvent;
import org.adempiere.webui.window.WFieldRecordInfo;
import org.compiere.model.GridField;
import org.compiere.model.GridTab;
import org.compiere.model.GridTabVO;
import org.compiere.model.GridWindow;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.compiere.util.Util;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Menuitem;

/**
 * 
 * @author hengsin
 *
 */
public class WGridTabSingleSelectionEditor extends WEditor implements ContextMenuListener
{
	private static final String[] LISTENER_EVENTS = {Events.ON_SELECT};

    private Object oldValue;

	private boolean tableEditor = false;
	
	private GridTab listViewGridTab = null;
	
	private String currentLinkValue = null;

    public WGridTabSingleSelectionEditor(GridField gridField) {
    	this(gridField, false);
    }

    public WGridTabSingleSelectionEditor(GridField gridField, boolean tableEditor)
    {
        super(new GridTabSelectionListView(false, gridField.getWindowNo()), gridField);
        this.tableEditor = tableEditor;
        init();
    }

    @Override
    public GridTabSelectionListView getComponent() {
    	return (GridTabSelectionListView) component;
    }

    @Override
	public boolean isReadWrite() {
		return !getComponent().getListbox().isEnabled();
	}

	@Override
	public void setReadWrite(boolean readWrite) {
		getComponent().getListbox().setEnabled(readWrite);
	}

	private void init()
    {
		if (tableEditor)
			setVisible(false);
		else if (gridField != null && gridField.getGridTab() != null)
		{
			int AD_Tab_ID = gridField.getIncluded_Tab_ID();
			GridWindow gridWindow = gridField.getGridTab().getGridWindow();
			int count = gridWindow.getTabCount();
			for(int i = 0; i < count; i++)
			{
				GridTab t = gridWindow.getTab(i);
				if (t.getAD_Tab_ID() == AD_Tab_ID)
				{
					GridTabVO vo = t.getVO();
					listViewGridTab = new GridTab(vo, gridWindow);
					String lcn = t.getLinkColumnName();
					if (Util.isEmpty(lcn)) {
						t.setLinkColumnName(null);
						lcn = t.getLinkColumnName();
					}
					listViewGridTab.setLinkColumnName(lcn);
					getComponent().init(listViewGridTab);
					break;
				}
			}
			
			popupMenu = new WEditorPopupMenu(false, false, isShowPreference());
			Menuitem clear = new Menuitem(Msg.getMsg(Env.getCtx(), "ClearSelection"), null);
			clear.setAttribute("EVENT", "onClearSelection");
			clear.addEventListener(Events.ON_CLICK, popupMenu);
			popupMenu.appendChild(clear);
			
			getComponent().getListbox().setContext(popupMenu);
		}
    }

	public void onEvent(Event event)
    {
    	if (Events.ON_SELECT.equals(event.getName()))
    	{
    		int selected = getComponent().getListbox().getSelectedIndex();
	        Object newValue = selected >= 0 ? Integer.toString(listViewGridTab.getKeyID(selected)) : null;
	        if (oldValue != null && newValue != null && oldValue.equals(newValue)) {
	    	    return;
	    	}
	        if (oldValue == null && newValue == null) {
	        	return;
	        }
	        ValueChangeEvent changeEvent = new ValueChangeEvent(this, this.getColumnName(), oldValue, newValue);
	        super.fireValueChange(changeEvent);
	        oldValue = newValue;
    	}
    }

    @Override
    public String getDisplay()
    {
        return oldValue != null ? oldValue.toString() : "";
    }

    @Override
    public Object getValue()
    {
        return oldValue;
    }

    @Override
    public void setValue(Object value)
    {
    	if (value == null && oldValue == null)
    	{
    		return;
    	}
    	else if (value != null && oldValue != null && value.equals(oldValue))
    	{
    		return;
    	}
    	oldValue = value;
        updateSlectedIndices();
    }

	private void updateSlectedIndices() {
		getComponent().clearSelection();
		if (!Util.isEmpty((String)oldValue))
        {
        	int id = Integer.parseInt((String) oldValue);
        	for(int i = 0; i < listViewGridTab.getRowCount(); i++) {
    			if (listViewGridTab.getKeyID(i) == id) {
    				getComponent().setSelectedIndex(i);
    				return;
    			}
    		}        	
        }
    }

    @Override
    public String[] getEvents()
    {
        return LISTENER_EVENTS;
    }

    public void onMenu(ContextMenuEvent evt)
	{
		if (WEditorPopupMenu.PREFERENCE_EVENT.equals(evt.getContextEvent()))
		{
			if (isShowPreference())
				ValuePreference.start (getComponent(), this.getGridField(), getValue());
			return;
		}
		else if (WEditorPopupMenu.CHANGE_LOG_EVENT.equals(evt.getContextEvent()))
		{
			WFieldRecordInfo.start(gridField);
		}
		else if ("onClearSelection".equals(evt.getContextEvent()))
		{
			ValueChangeEvent changeEvent = new ValueChangeEvent(this, this.getColumnName(), oldValue, null);
	        super.fireValueChange(changeEvent);
	        oldValue = null;
		}
	}

	@Override
	public void dynamicDisplay() {
		String name = listViewGridTab.getLinkColumnName();
		String linkValue = Env.getContext(Env.getCtx(), gridField.getWindowNo(), name);
		if ((currentLinkValue == null && linkValue != null)
			|| (currentLinkValue != null && linkValue == null)
			|| (currentLinkValue != null && linkValue != null && !currentLinkValue.equals(linkValue)))
		{
			getComponent().refresh(listViewGridTab);
			updateSlectedIndices();
		}
	}
}