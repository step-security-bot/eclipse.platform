/*******************************************************************************
 * Copyright (c) 2004 John-Mason P. Shackelford and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     John-Mason P. Shackelford - initial API and implementation
 *******************************************************************************/
package org.eclipse.ant.internal.ui.editor.formatter;

import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *  
 */
public class XmlTagFormatter {

    protected static class AttributePair {

        private String attribute;

        private String value;

        public AttributePair(String attribute, String value) {
            this.attribute = attribute;
            this.value = value;
        }

        public String getAttribute() {
            return attribute;
        }

        public String getValue() {
            return value;
        }
    }

    protected static class ParseException extends Exception {

        public ParseException(String message) {
            super(message);
        }
    }

    protected static class Tag {

        private List attributes = new ArrayList();

        private boolean closed;

        private String elementName;

        public void addAttribute(String attribute, String value) {
            attributes.add(new AttributePair(attribute, value));
        }

        public int attributeCount() {
            return attributes.size();
        }

        public AttributePair getAttributePair(int i) {
            return (AttributePair) attributes.get(i);
        }

        public String getElementName() {
            return this.elementName;
        }

        public boolean isClosed() {
            return closed;
        }

        public int minimumLength() {
            int length = 2; // for the < >
            if (this.isClosed()) length++; // if we need to add an />
            length += this.getElementName().length();
            if (this.attributeCount() > 0 || this.isClosed()) length++;
            for (int i = 0; i < this.attributeCount(); i++) {
                AttributePair attributePair = this.getAttributePair(i);
                length += attributePair.getAttribute().length();
                length += attributePair.getValue().length();
                length += 4; // equals sign, quote characters & trailing space
            }
            if (this.attributeCount() > 0 && !this.isClosed()) length--;
            return length;
        }

        public void setAttributes(List attributePair) {
            attributes.clear();
            attributes.addAll(attributePair);
        }

        public void setClosed(boolean closed) {
            this.closed = closed;
        }

        public void setElementName(String elementName) {
            this.elementName = elementName;
        }

        public String toString() {
            StringBuffer sb = new StringBuffer(500);
            sb.append("<"); //$NON-NLS-1$
            sb.append(this.getElementName());
            if (this.attributeCount() > 0 || this.isClosed()) sb.append(' ');

            for (int i = 0; i < this.attributeCount(); i++) {
                AttributePair attributePair = this.getAttributePair(i);
                sb.append(attributePair.getAttribute());
                sb.append("=\""); //$NON-NLS-1$
                sb.append(attributePair.getValue());
                sb.append("\""); //$NON-NLS-1$
                if (this.isClosed() || i != this.attributeCount() - 1)
                        sb.append(' ');
            }
            if (this.isClosed()) sb.append("/"); //$NON-NLS-1$
            sb.append(">"); //$NON-NLS-1$
            return sb.toString();
        }
    }

    protected static class TagFormatter {

        /**
         * @param searchChar
         * @param inTargetString
         * @return
         */
        private int countChar(char searchChar, String inTargetString) {
            StringCharacterIterator iter = new StringCharacterIterator(
                    inTargetString);
            int i = 0;
            if (iter.first() == searchChar) i++;
            while (iter.getIndex() < iter.getEndIndex()) {
                if (iter.next() == searchChar) {
                    i++;
                }
            }
            return i;
        }

        /**
         * @param tagText
         * @param prefs
         * @param indent
         * @return
         */
        public String format(Tag tag, FormattingPreferences prefs, String indent) {
            if (prefs.wrapLongTags()
                    && lineRequiresWrap(indent + tag.toString(), prefs
                            .getMaximumLineWidth(), prefs.getTabWidth())) {
                return wrapTag(tag, prefs, indent);
            }
            return tag.toString();
        }

        /**
         * @param line
         * @param lineWidth
         * @param tabWidth
         * @return
         */
        protected boolean lineRequiresWrap(String line, int lineWidth,
                int tabWidth) {
            return tabExpandedLineWidth(line, tabWidth) > lineWidth;
        }

        /**
         * @param line
         *            the line in which spaces are to be expanded
         * @param tabWidth
         *            number of spaces to substitute for a tab
         * @return length of the line with tabs expanded to spaces
         */
        protected int tabExpandedLineWidth(String line, int tabWidth) {
            int tabCount = countChar('\t', line);
            return (line.length() - tabCount) + (tabCount * tabWidth);
        }

        /**
         * @param tag
         * @param prefs
         * @param indent
         * @return
         */
        protected String wrapTag(Tag tag, FormattingPreferences prefs,
                String indent) {
            StringBuffer sb = new StringBuffer(1024);
            sb.append('<');
            sb.append(tag.getElementName());
            sb.append(' ');

            if (tag.attributeCount() > 0) {
                sb.append(tag.getAttributePair(0).getAttribute());
                sb.append("=\""); //$NON-NLS-1$
                sb.append(tag.getAttributePair(0).getValue());
                sb.append('"');
            }

            if (tag.attributeCount() > 1) {
                char[] extraIndent = new char[tag.getElementName().length() + 2];
                Arrays.fill(extraIndent, ' ');
                for (int i = 1; i < tag.attributeCount(); i++) {
                    sb.append('\n');
                    sb.append(indent);
                    sb.append(extraIndent);
                    sb.append(tag.getAttributePair(i).getAttribute());
                    sb.append("=\""); //$NON-NLS-1$
                    sb.append(tag.getAttributePair(i).getValue());
                    sb.append('"');
                }
            }

            if (prefs.alignElementCloseChar()) {
                sb.append("\n"); //$NON-NLS-1$
                sb.append(indent);
            } else if (tag.isClosed()) {
                sb.append(' ');
            }

            if (tag.isClosed()) sb.append("/"); //$NON-NLS-1$
            sb.append(">"); //$NON-NLS-1$
            return sb.toString();
        }
    }

    // if object creation is an issue, use static methods or a flyweight
    // pattern
    protected static class TagParser {

        private String elementName;

        private String parseText;

        /**
         *  
         */
        protected List getAttibutes(String elementText)
                throws ParseException {

            class Mode {
                private int mode;
                public void setAttributeNameSearching() {mode = 0;}
                public void setAttributeNameFound() {mode = 1;}
                public void setAttributeValueSearching() {mode = 2;}
                public void setAttributeValueFound() {mode = 3;}
                public void setFinished() {mode = 4;}
                public boolean isAttributeNameSearching() {return mode == 0;}
                public boolean isAttributeNameFound() {return mode == 1;}
                public boolean isAttributeValueSearching() {return mode == 2;}
                public boolean isAttributeValueFound() {return mode == 3;}
                public boolean isFinished() {return mode == 4;}
            }

            List attributePairs = new ArrayList();

            CharacterIterator iter = new StringCharacterIterator(elementText
                    .substring(getElementName(elementText).length() + 2));

            // state for finding attributes
            Mode mode = new Mode();
            mode.setAttributeNameSearching();
            char attributeQuote = '"';
            StringBuffer currentAttributeName = null;
            StringBuffer currentAttributeValue = null;

            char c = iter.first();
            while (iter.getIndex() < iter.getEndIndex()) {
                
                switch (c) {                
                
                case '"':
                case '\'':

                    if (mode.isAttributeValueSearching()) {

                        // start of an attribute value
                        attributeQuote = c;
                        mode.setAttributeValueFound();
                        currentAttributeValue = new StringBuffer(1024);

                    } else if (mode.isAttributeValueFound()
                            && attributeQuote == c) {

                        // we've completed a pair!
                        AttributePair pair = new AttributePair(
                                currentAttributeName.toString(),
                                currentAttributeValue.toString());

                        attributePairs.add(pair);

                        // start looking for another attribute
                        mode.setAttributeNameSearching();

                    } else if (mode.isAttributeValueFound()
                            && attributeQuote != c) {

                        // this quote character is part of the attribute value
                        currentAttributeValue.append(c);

                    } else {
                        // this is no place for a quote!
                        throw new ParseException("Unexpected '" + c //$NON-NLS-1$
                                + "' when parsing:\n\t" + elementText); //$NON-NLS-1$
                    }
                    break;

                case '=':
                    
                    if (mode.isAttributeValueFound()) {

                        // this character is part of the attribute value
                        currentAttributeValue.append(c);

                    } else if (mode.isAttributeNameFound()) {

                        // end of the name, now start looking for the value
                        mode.setAttributeValueSearching();
                        
                    } else {
                        // this is no place for an equals sign!
                        throw new ParseException("Unexpected '" + c //$NON-NLS-1$
                                + "' when parsing:\n\t" + elementText); //$NON-NLS-1$
                    }
                    break;

                case '/':
                case '>':
                    if (mode.isAttributeValueFound()) {
                        // attribute values are CDATA, add it all
                        currentAttributeValue.append(c);
                    } else if (mode.isAttributeNameSearching()) {
                        mode.setFinished();
					} else if (mode.isFinished()){
						// consume the remaining characters
                    } else {
                        // we aren't ready to be done!
                        throw new ParseException("Unexpected '" + c //$NON-NLS-1$
                                + "' when parsing:\n\t" + elementText); //$NON-NLS-1$
                    }
                    break;

                default:

                    if (mode.isAttributeValueFound()) {
                        // attribute values are CDATA, add it all
                        currentAttributeValue.append(c);

					} else if (mode.isFinished()) {
						if (!Character.isWhitespace(c)) {
								throw new ParseException("Unexpected '" + c //$NON-NLS-1$
										+ "' when parsing:\n\t" + elementText); //$NON-NLS-1$
						}
                    } else {
                        if (!Character.isWhitespace(c)) {
                            if (mode.isAttributeNameSearching()) {
                                // we found the start of an attribute name
                                mode.setAttributeNameFound();
                                currentAttributeName = new StringBuffer(255);
                                currentAttributeName.append(c);
                            } else if (mode.isAttributeNameFound()) {
                                currentAttributeName.append(c);                            
                            }
                        }
                    }
                    break;
                }
                
                c = iter.next();
            }
			if (!mode.isFinished()) {
				throw new ParseException("Element did not complete normally."); //$NON-NLS-1$
			}
            return attributePairs;
        }

        /**
         * @param tagText
         *            text of an XML tag
         * @return extracted XML element name
         */
        protected String getElementName(String tagText) throws ParseException {
            if (!tagText.equals(this.parseText) || this.elementName == null) {
                int endOfTag = tagEnd(tagText);
                if ((tagText.length() > 2) && (endOfTag > 1)) {
                    this.parseText = tagText;
                    this.elementName = tagText.substring(1, endOfTag);
                } else {
                    throw new ParseException("No element name for the tag:\n\t" //$NON-NLS-1$
                            + tagText);
                }
            }
            return elementName;
        }

        /**
         * @param tagText
         * @return
         */
        protected boolean isClosed(String tagText) {
            return tagText.charAt(tagText.lastIndexOf(">") - 1) == '/'; //$NON-NLS-1$
        }

        /**
         * @param tagText
         * @return an fully populated tag
         */
        public Tag parse(String tagText) throws ParseException {
            Tag tag = new Tag();
            tag.setElementName(getElementName(tagText));
            tag.setAttributes(getAttibutes(tagText));
            tag.setClosed(isClosed(tagText));
            return tag;
        }

        private int tagEnd(String text) {
            // This is admittedly a little loose, but we don't want the
            // formatter to be too strict...
            // http://www.w3.org/TR/2000/REC-xml-20001006#NT-Name
            for (int i = 1; i < text.length(); i++) {
                char c = text.charAt(i);
                if (!Character.isLetterOrDigit(c) && c != ':' && c != '.'
                        && c != '-' && c != '_') { return i; }
            }
            return -1;
        }
    }

    /**
     * @param tagText
     * @param prefs
     * @param indent
     * @return
     */
    public static String format(String tagText, FormattingPreferences prefs,
            String indent) {

        Tag tag;
        if (tagText.startsWith("</") || tagText.startsWith("<%") //$NON-NLS-1$ //$NON-NLS-2$
                || tagText.startsWith("<?") || tagText.startsWith("<[")) { //$NON-NLS-1$ //$NON-NLS-2$
            return tagText;
        } 
    	try {
            tag = new TagParser().parse(tagText);
        } catch (ParseException e) {
            // if we can't parse the tag, give up and leave the text as is.
            return tagText;
        }
        return new TagFormatter().format(tag, prefs, indent);
    }
}