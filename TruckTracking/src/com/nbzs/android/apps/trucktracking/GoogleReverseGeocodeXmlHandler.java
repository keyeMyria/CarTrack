package com.nbzs.android.apps.trucktracking;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 *
 * @author Punsiru Alwis
 *
 */
public class GoogleReverseGeocodeXmlHandler extends DefaultHandler
{
        private boolean inAddress = false;
        private boolean finished = false;
        private StringBuilder builder;
        private String address;

        /**
         *
         * @return address
         */
        public String getLocalityName()
        {
                return this.address;
        }

        @Override
        public void characters(char[] ch, int start, int length)
               throws SAXException {
            super.characters(ch, start, length);
            if (this.inAddress && !this.finished)
            {
                if ((ch[start] != '\n') && (ch[start] != ' '))
                {
                        builder.append(ch, start, length);
                }
            }
        }

        @Override
        public void endElement(String uri, String localName, String name)
                throws SAXException
        {
            super.endElement(uri, localName, name);


            if (!this.finished)
            {
                //read the address from xml file
                if (localName.equalsIgnoreCase("address"))
                {
                        this.address = builder.toString();
                        this.finished = true;
                }

                if (builder != null)
                {
                        builder.setLength(0);
                }
            }
    }

    @Override
    public void startDocument() throws SAXException
    {
        super.startDocument();
        builder = new StringBuilder();
    }

    @Override
    public void startElement(String uri, String localName, String name, Attributes attributes) throws SAXException
    {
        super.startElement(uri, localName, name, attributes);

        if (localName.equalsIgnoreCase("address"))
        {
                this.inAddress = true;
        }
    }
}

