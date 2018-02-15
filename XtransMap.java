package com.chartis.xtrans;

import java.util.logging.Level;

/**
 * This entity represents the single transformation path (from source to
 * target).
 * 
 * @author
 * 
 */
public class XTransMap {
	public XElement sourceElement = null;
	public XElement targetElement = null;
	public boolean toTranslate;
	public char codeToDescription = 'N';
	public String toAppend = "NOAPPEND";
	public boolean processed = false;

	public boolean isProcessed() {
		return processed;
	}

	public boolean isToTranslate() {
		return toTranslate;
	}

	public boolean isCodeToDescription() {
		if (this.codeToDescription == 'N' || this.codeToDescription == 'n') {
			return false;
		}
		return true;
	}

	public void setProcessed(boolean processed) {
		this.processed = processed;
	}

	public void setToAppend(String toAppend) {
		this.toAppend = toAppend;
	}

	public void setToTranslate(boolean toTranslate) {
		this.toTranslate = toTranslate;
	}

	public XTransMap(XElement sElement, XElement tElement, boolean toTranslate) {
		sourceElement = sElement;
		targetElement = tElement;
		setToTranslate(toTranslate);

	}

	public XTransMap(XElement sElement, XElement tElement,
			char codeToDescription) {
		sourceElement = sElement;
		targetElement = tElement;
		setCodeToDescription(codeToDescription);
	}

	public XTransMap(XElement sElement, XElement tElement, String toAppend) {
		sourceElement = sElement;
		targetElement = tElement;
		setCodeToDescription('Y');
		setToAppend(toAppend);
	}

	public XElement getSourceElement() {
		return sourceElement;
	}

	public void setSourceElement(XElement sourceElement) {
		this.sourceElement = sourceElement;
	}

	public XElement getTargetElement() {
		return targetElement;
	}

	public void setTargetElement(XElement targetElement) {
		this.targetElement = targetElement;
	}

	public void setCodeToDescription(char codeToDescription) {
		this.codeToDescription = codeToDescription;
	}

	public String getToAppend() {
		return toAppend;
	}
}
