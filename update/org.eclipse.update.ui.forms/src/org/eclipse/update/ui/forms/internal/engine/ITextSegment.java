/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.update.ui.forms.internal.engine;

import org.eclipse.swt.graphics.*;

/**
 * @version 	1.0
 * @author
 */
public interface ITextSegment extends IParagraphSegment {
	String getText();
	Color getColor();
	Font getFont();
	boolean isWordWrapAllowed();
	boolean isSelectable();
	boolean contains(int x, int y);
	Rectangle getBounds();
	void paintFocus(GC gc, Color bg, Color fg, boolean selected);
}