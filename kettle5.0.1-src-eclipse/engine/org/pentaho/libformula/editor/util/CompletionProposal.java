package org.pentaho.libformula.editor.util;

public class CompletionProposal {
	private String menuText;
	private String completionString;
	int offset;

	public CompletionProposal(String menuText, String completionString,
			int offset) {
		this.menuText = menuText;
		this.completionString = completionString;
		this.offset = offset;
	}

	/**
	 * @return the menuText
	 */
	public String getMenuText() {
		return menuText;
	}

	/**
	 * @param menuText
	 *            the menuText to set
	 */
	public void setMenuText(String menuText) {
		this.menuText = menuText;
	}

	/**
	 * @return the completionString
	 */
	public String getCompletionString() {
		return completionString;
	}

	/**
	 * @param completionString
	 *            the completionString to set
	 */
	public void setCompletionString(String completionString) {
		this.completionString = completionString;
	}

	/**
	 * @return the offset
	 */
	public int getOffset() {
		return offset;
	}

	/**
	 * @param offset
	 *            the offset to set
	 */
	public void setOffset(int offset) {
		this.offset = offset;
	}
}