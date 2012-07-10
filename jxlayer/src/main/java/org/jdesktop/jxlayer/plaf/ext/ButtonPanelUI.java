/**
 * Copyright (c) 2006-2009, Alexander Potochkin
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *   * Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *   * Redistributions in binary form must reproduce the above
 *     copyright notice, this list of conditions and the following
 *     disclaimer in the documentation and/or other materials provided
 *     with the distribution.
 *   * Neither the name of the JXLayer project nor the names of its
 *     contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.jdesktop.jxlayer.plaf.ext;

import javax.swing.JLayer;
import org.jdesktop.jxlayer.plaf.AbstractLayerUI;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;

public class ButtonPanelUI extends AbstractLayerUI<JComponent> {

	private static final long serialVersionUID = -6872441543970666543L;
	private boolean isFocusCyclic;

	@Override
    public void installUI(JComponent c) {
		super.installUI(c);
		c.setFocusTraversalPolicyProvider(true);
		c.setFocusTraversalPolicy(new ButtonPanelFocusTraversalPolicy());
	}

	@Override
    public void uninstallUI(JComponent c) {
		super.uninstallUI(c);
		c.setFocusTraversalPolicyProvider(false);
		c.setFocusTraversalPolicy(new LayoutFocusTraversalPolicy());
	}

	public ButtonPanelUI() {
		this(false);
	}

	public ButtonPanelUI(boolean cyclic) {
		isFocusCyclic = cyclic;
	}

	/**
	 * Returns whether arrow keys should support cyclic focus traversal ordering
	 * for for this ButtonPanelUI.
	 * 
	 * @return whether arrow keys should support cyclic focus traversal ordering
	 */
	public boolean isFocusCyclic() {
		return isFocusCyclic;
	}

	/**
	 * Sets whether arrow keys should support cyclic focus traversal ordering
	 * for this ButtonPanelUI.
	 * 
	 * @param isFocusCyclic
	 *            sets whether arrow keys should support cyclic focus traversal
	 *            ordering
	 */
	public void setFocusCyclic(boolean isFocusCyclic) {
		this.isFocusCyclic = isFocusCyclic;
	}

	private ButtonGroup getButtonGroup(AbstractButton button) {
		ButtonModel model = button.getModel();
		if (model instanceof DefaultButtonModel) {
			return ((DefaultButtonModel) model).getGroup();
		}
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void processKeyEvent(KeyEvent e, JLayer<? extends JComponent> l) {
		super.processKeyEvent(e, l);
		if (e.getID() == KeyEvent.KEY_PRESSED) {
			switch (e.getKeyCode()) {
			case KeyEvent.VK_RIGHT:
			case KeyEvent.VK_DOWN:
				moveFocus(true, l);
				break;
			case KeyEvent.VK_LEFT:
			case KeyEvent.VK_UP:
				moveFocus(false, l);
				break;
			}
		}
	}

	private void moveFocus(boolean isForward, JLayer<? extends JComponent> l) {
		Component fo = KeyboardFocusManager.getCurrentKeyboardFocusManager()
				.getFocusOwner();
		ButtonPanelFocusTraversalPolicy bftp = (ButtonPanelFocusTraversalPolicy) l
				.getFocusTraversalPolicy();

		if (fo instanceof AbstractButton) {
			AbstractButton b = (AbstractButton) fo;

			bftp.setAlternativeFocusMode(true);

			Component next = isForward ? bftp.getComponentAfter(l, fo) : bftp
					.getComponentBefore(l, fo);

			bftp.setAlternativeFocusMode(false);

			b.getModel().setPressed(false);
			if (next instanceof AbstractButton) {
				ButtonGroup group = getButtonGroup((AbstractButton) fo);
				AbstractButton nextButton = (AbstractButton) next;
				if (group != getButtonGroup(nextButton)) {
					return;
				}
				if (group != null && group.getSelection() != null
						&& !nextButton.isSelected()) {
					nextButton.setSelected(true);
				}
				next.requestFocusInWindow();
			}
		}
	}

	private class ButtonPanelFocusTraversalPolicy extends
			LayoutFocusTraversalPolicy {

		private static final long serialVersionUID = -1810727271800528821L;
		private boolean isAlternativeFocusMode;

		public boolean isAlternativeFocusMode() {
			return isAlternativeFocusMode;
		}

		public void setAlternativeFocusMode(boolean alternativeFocusMode) {
			isAlternativeFocusMode = alternativeFocusMode;
		}

		@Override
        protected boolean accept(Component c) {
			if (!isAlternativeFocusMode() && c instanceof AbstractButton) {
				AbstractButton button = (AbstractButton) c;
				ButtonGroup group = getButtonGroup(button);
				if (group != null && group.getSelection() != null
						&& !button.isSelected()) {
					return false;
				}
			}
			return super.accept(c);
		}

		@Override
        public Component getComponentAfter(Container aContainer,
				Component aComponent) {
			Component componentAfter = super.getComponentAfter(aContainer,
					aComponent);
			if (!isAlternativeFocusMode()) {
				return componentAfter;
			}
			if (isFocusCyclic()) {
				return componentAfter == null ? getFirstComponent(aContainer)
						: componentAfter;
			}
			if (aComponent == getLastComponent(aContainer)) {
				return aComponent;
			}
			return componentAfter;
		}

		@Override
        public Component getComponentBefore(Container aContainer,
				Component aComponent) {
			Component componentBefore = super.getComponentBefore(aContainer,
					aComponent);
			if (!isAlternativeFocusMode()) {
				return componentBefore;
			}
			if (isFocusCyclic()) {
				return componentBefore == null ? getLastComponent(aContainer)
						: componentBefore;
			}
			if (aComponent == getFirstComponent(aContainer)) {
				return aComponent;
			}
			return componentBefore;
		}
	}
}
