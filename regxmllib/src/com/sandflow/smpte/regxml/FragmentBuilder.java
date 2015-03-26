/*
 * Copyright (c) 2014, Pierre-Anthony Lemieux (pal@sandflow.com)
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package com.sandflow.smpte.regxml;

import com.sandflow.smpte.klv.Group;
import com.sandflow.smpte.klv.exception.KLVException;
import com.sandflow.smpte.klv.LocalSetRegister;
import com.sandflow.smpte.klv.Triplet;
import com.sandflow.smpte.mxf.MXFInputStream;
import com.sandflow.smpte.regxml.definition.CharacterTypeDefinition;
import com.sandflow.smpte.regxml.definition.ClassDefinition;
import com.sandflow.smpte.regxml.definition.Definition;
import com.sandflow.smpte.regxml.definition.EnumerationTypeDefinition;
import com.sandflow.smpte.regxml.definition.ExtendibleEnumerationTypeDefinition;
import com.sandflow.smpte.regxml.definition.FixedArrayTypeDefinition;
import com.sandflow.smpte.regxml.definition.IndirectTypeDefinition;
import com.sandflow.smpte.regxml.definition.IntegerTypeDefinition;
import com.sandflow.smpte.regxml.definition.OpaqueTypeDefinition;
import com.sandflow.smpte.regxml.definition.PropertyAliasDefinition;
import com.sandflow.smpte.regxml.definition.PropertyDefinition;
import com.sandflow.smpte.regxml.definition.RecordTypeDefinition;
import com.sandflow.smpte.regxml.definition.RenameTypeDefinition;
import com.sandflow.smpte.regxml.definition.SetTypeDefinition;
import com.sandflow.smpte.regxml.definition.StreamTypeDefinition;
import com.sandflow.smpte.regxml.definition.StringTypeDefinition;
import com.sandflow.smpte.regxml.definition.StrongReferenceTypeDefinition;
import com.sandflow.smpte.regxml.definition.VariableArrayTypeDefinition;
import com.sandflow.smpte.regxml.definition.WeakReferenceTypeDefinition;
import com.sandflow.smpte.regxml.dict.DefinitionResolver;
import com.sandflow.smpte.util.AUID;
import com.sandflow.smpte.util.UL;
import com.sandflow.smpte.util.UMID;
import com.sandflow.smpte.util.UUID;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.URI;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 *
 * @author Pierre-Anthony Lemieux (pal@sandflow.com)
 */
public class FragmentBuilder {

    private final static Logger LOG = Logger.getLogger(FragmentBuilder.class.getName());

    private static final UL INSTANCE_UID_ITEM_UL
            = new UL(new byte[]{0x06, 0x0e, 0x2b, 0x34, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x15, 0x02, 0x00, 0x00, 0x00, 0x00});

    private static final UL AUID_UL = UL.fromDotValue("06.0E.2B.34.01.04.01.01.01.03.01.00.00.00.00.00");
    private static final UL UUID_UL = UL.fromDotValue("06.0E.2B.34.01.04.01.01.01.03.03.00.00.00.00.00");
    private static final UL DateStruct_UL = UL.fromDotValue("06.0E.2B.34.01.04.01.01.03.01.05.00.00.00.00.00");
    private static final UL PackageID_UL = UL.fromDotValue("06.0E.2B.34.01.04.01.01.01.03.02.00.00.00.00.00");
    private static final UL Rational_UL = UL.fromDotValue("06.0E.2B.34.01.04.01.01.03.01.01.00.00.00.00.00");
    private static final UL TimeStruct_UL = UL.fromDotValue("06.0E.2B.34.01.04.01.01.03.01.06.00.00.00.00.00");
    private static final UL TimeStamp_UL = UL.fromDotValue("06.0E.2B.34.01.04.01.01.03.01.07.00.00.00.00.00");
    private static final UL VersionType_UL = UL.fromDotValue("06.0E.2B.34.01.04.01.01.03.01.03.00.00.00.00.00");
    private static final UL ByteOrder_UL = UL.fromDotValue("06.0E.2B.34.01.01.01.01.03.01.02.01.02.00.00.00");
    
    private static final UL Character_UL = UL.fromURN("urn:smpte:ul:060e2b34.01040101.01100100.00000000");
    private static final UL Char_UL = UL.fromURN("urn:smpte:ul:060e2b34.01040101.01100300.00000000");

    private static final String REGXML_NS = "http://www.smpte-ra.org/schemas/2001-1b/2013/metadict";

    private static final String ACTUALTYPE_ATTR = "actualType";
    private static final String BYTEORDER_ATTR = "byteOrder";
    private static final String BYTEORDER_BE = "BigEndian";
    private static final String UID_ATTR = "uid";

    private DefinitionResolver resolver;
    private LocalSetRegister localtags;
    private final HashMap<UUID, Group> groups = new HashMap<>();
    private final HashMap<URI, String> nsprefixes = new HashMap<>();

    public DocumentFragment fragmentFromTriplet(Group group, Document document) throws ParserConfigurationException, KLVException, RuleException {

        DocumentFragment df = document.createDocumentFragment();

        applyRule3(df, group);

        /* TODO: hack to clean-up namespace prefixes */
        for (Map.Entry<URI, String> entry : nsprefixes.entrySet()) {
            ((Element) df.getFirstChild()).setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:" + entry.getValue(), entry.getKey().toString());
        }

        return df;
    }

    public DefinitionResolver getDefinitionResolver() {
        return resolver;
    }

    public void setDefinitionResolver(DefinitionResolver resolver) {
        this.resolver = resolver;
    }

    public LocalSetRegister getLocaltags() {
        return localtags;
    }

    public void setLocaltags(LocalSetRegister localtags) {
        this.localtags = localtags;
    }

    private String getPrefix(URI ns) {
        String prefix = this.nsprefixes.get(ns);

        /* if prefix does not exist, create one */
        if (prefix == null) {
            prefix = "r" + this.nsprefixes.size();

            this.nsprefixes.put(ns, prefix);
        }

        return prefix;
    }

    public void addGroup(Group obj) {

        for (Triplet t : obj.getItems()) {

            if (INSTANCE_UID_ITEM_UL.equalsIgnoreVersion(t.getKey())) {

                UUID uuid = new UUID(t.getValue());

                groups.put(uuid, obj);

                break;
            }

        }

        // TODO: detect duplicate groups
    }

    void applyRule3(Node node, Group group) throws RuleException {

        Definition definition = resolver.getDefinition(new AUID(group.getKey()));

        if (definition == null) {
            LOG.warning(
                    String.format(
                            "Unknown Group UL = %s",
                            group.getKey().toString()
                    )
            );

            return;
        }

        if (definition.getIdentification().asUL().getVersion() != group.getKey().getVersion()) {
            LOG.warning(
                    String.format(
                            "Group UL %s in file does not have the same version as in the register (0x%02x)",
                            group.getKey(),
                            definition.getIdentification().asUL().getVersion()
                    )
            );
        }

        Element elem = node.getOwnerDocument().createElementNS(definition.getNamespace().toString(), definition.getSymbol());

        elem.setPrefix(getPrefix(definition.getNamespace()));

        for (Triplet item : group.getItems()) {

            if (item.getKey().equals(INSTANCE_UID_ITEM_UL)) {

                MXFInputStream mis = new MXFInputStream(item.getValueAsStream());

                try {
                    UUID uuid = mis.readUUID();

                    elem.setAttributeNS(
                            REGXML_NS,
                            UID_ATTR,
                            uuid.toString()
                    );

                } catch (IOException ex) {
                    throw new RuleException(ex);
                }

            } else {

                Definition itemdef = resolver.getDefinition(new AUID(item.getKey()));

                if (itemdef == null) {
                    LOG.warning(
                            String.format(
                                    "Unknown property UL = %s",
                                    item.getKey().toString()
                            )
                    );
                } else {

                    if (itemdef.getIdentification().asUL().getVersion() != item.getKey().getVersion()) {
                        LOG.warning(
                                String.format(
                                        "Property UL %s in file does not have the same version as in the register (0x%02x)",
                                        item.getKey().toString(),
                                        itemdef.getIdentification().asUL().getVersion()
                                )
                        );
                    }

                    applyRule4(elem, item.getValueAsStream(), itemdef);
                }

            }

        }

        node.appendChild(elem);

    }

    void applyRule4(Element element, InputStream value, Definition definition) throws RuleException {

        Element elem = element.getOwnerDocument().createElementNS(definition.getNamespace().toString(), definition.getSymbol());

        elem.setPrefix(getPrefix(definition.getNamespace()));

        if (definition.getIdentification().equals(ByteOrder_UL)) {
            MXFInputStream kis = new MXFInputStream(value);

            int byteorder;

            try {
                byteorder = kis.readInt();
            } catch (IOException ex) {
                throw new RuleException(ex);
            }

            if (byteorder == 0x4949) {
                elem.setTextContent("BigEndian");
            } else if (byteorder == 0x4D4D) {
                elem.setTextContent("LittleEndian");
            } else {
                throw new RuleException("Unknown ByteOrder value.");
            }

        } else {

            if (definition instanceof PropertyAliasDefinition) {
                definition = resolver.getDefinition(((PropertyAliasDefinition) definition).getOriginalProperty());
            }

            Definition typedef = resolver.getDefinition(((PropertyDefinition) definition).getType());

            if (typedef == null) {
                throw new RuleException(
                        String.format(
                                "Type %s not found at %s.",
                                ((PropertyDefinition) definition).getType().toString(),
                                definition.getSymbol()
                        )
                );
            }

            applyRule5(elem, value, typedef);
        }

        element.appendChild(elem);

    }

    void applyRule5(Element element, InputStream value, Definition definition) throws RuleException {

        if (definition instanceof CharacterTypeDefinition) {
            applyRule5_1(element, value, (CharacterTypeDefinition) definition);
        } else if (definition instanceof EnumerationTypeDefinition) {
            applyRule5_2(element, value, (EnumerationTypeDefinition) definition);
        } else if (definition instanceof ExtendibleEnumerationTypeDefinition) {
            applyRule5_3(element, value, (ExtendibleEnumerationTypeDefinition) definition);
        } else if (definition instanceof FixedArrayTypeDefinition) {
            applyRule5_4(element, value, (FixedArrayTypeDefinition) definition);
        } else if (definition instanceof IndirectTypeDefinition) {
            applyRule5_5(element, value, (IndirectTypeDefinition) definition);
        } else if (definition instanceof IntegerTypeDefinition) {
            applyRule5_6(element, value, (IntegerTypeDefinition) definition);
        } else if (definition instanceof OpaqueTypeDefinition) {
            applyRule5_7(element, value, (OpaqueTypeDefinition) definition);
        } else if (definition instanceof RecordTypeDefinition) {
            applyRule5_8(element, value, (RecordTypeDefinition) definition);
        } else if (definition instanceof RenameTypeDefinition) {
            applyRule5_9(element, value, (RenameTypeDefinition) definition);
        } else if (definition instanceof SetTypeDefinition) {
            applyRule5_10(element, value, (SetTypeDefinition) definition);
        } else if (definition instanceof StreamTypeDefinition) {
            applyRule5_11(element, value, (StreamTypeDefinition) definition);
        } else if (definition instanceof StringTypeDefinition) {
            applyRule5_12(element, value, (StringTypeDefinition) definition);
        } else if (definition instanceof StrongReferenceTypeDefinition) {
            applyRule5_13(element, value, (StrongReferenceTypeDefinition) definition);
        } else if (definition instanceof VariableArrayTypeDefinition) {
            applyRule5_14(element, value, (VariableArrayTypeDefinition) definition);
        } else if (definition instanceof WeakReferenceTypeDefinition) {
            applyRule5_15(element, value, (WeakReferenceTypeDefinition) definition);
        } else {

            throw new RuleException(
                    String.format(
                            "Illegal Definition %s in Rule 5.",
                            definition.getClass().toString()
                    )
            );

        }

    }

    void applyRule5_1(Element element, InputStream value, CharacterTypeDefinition definition) throws RuleException {
        byte[] c = new byte[2];

        try {

            value.read(c);

            element.setTextContent(new String(c, "UTF-16BE"));

        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        } catch (IOException ioe) {
            
            /* TODO: replace with inline XML error comments? */
            
            throw new RuleException(ioe);
        }
    }

    void applyRule5_2(Element element, InputStream value, EnumerationTypeDefinition definition) throws RuleException {

        try {

            DataInputStream dis = new DataInputStream(value);

            int val = dis.readUnsignedByte();

            /* TODO: better error */
            String str = "ERROR";

            for (EnumerationTypeDefinition.Element e : definition.getElements()) {
                if (e.getValue() == val) {
                    str = e.getName();
                }
            }

            element.setTextContent(str);

        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        } catch (IOException ioe) {
            throw new RuleException(ioe);
        }
    }

    void applyRule5_3(Element element, InputStream value, ExtendibleEnumerationTypeDefinition definition) throws RuleException {

        try {

            MXFInputStream ki = new MXFInputStream(value);

            UL ul = ki.readUL();

            /* TODO: seek label symbol */
            /* BUG: must allow ULs */
            element.setTextContent(ul.toString());

        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        } catch (IOException ioe) {
            throw new RuleException(ioe);
        }
    }

    void applyRule5_4(Element element, InputStream value, FixedArrayTypeDefinition definition) throws RuleException {

        if (definition.getIdentification().equals(UUID_UL)) {

            try {
                MXFInputStream kis = new MXFInputStream(value);

                UUID uuid = kis.readUUID();

                element.setTextContent(uuid.toString());

            } catch (IOException e) {
                throw new RuleException(e);
            }
        } else {

            Definition typedef = resolver.getDefinition(definition.getElementType());

            applyCoreRule5_4(element, value, typedef, definition.getElementCount());

        }
    }

    void applyCoreRule5_4(Element element, InputStream value, Definition typedef, int elementcount) throws RuleException {

        for (int i = 0; i < elementcount; i++) {

            if (typedef instanceof StrongReferenceTypeDefinition) {

                /* Rule 5.4.1 */
                applyRule5_13(element, value, (StrongReferenceTypeDefinition) typedef);

            } else {

                /* Rule 5.4.2 */
                Element elem = element.getOwnerDocument().createElementNS(typedef.getNamespace().toString(), typedef.getSymbol());

                elem.setPrefix(getPrefix(typedef.getNamespace()));

                applyRule5(elem, value, typedef);

                element.appendChild(elem);

            }
        }
    }

    void applyRule5_5(Element element, InputStream value, IndirectTypeDefinition definition) throws RuleException {

        /* BUG: how is indirect type encoded in MXF? */
        throw new RuleException("Indirect type not supported.");

        /* Definition typedef = dict.getDefinition(definition.getIdentification()); */

        /* BUG: do we need a different dictionary per registry namespace? 
         element.setAttributeNS(
         REGXML_NS,
         ACTUALTYPE_ATTR,
         MetaDictionary.createQualifiedSymbol(null, typedef.getSymbol())
         );*/

        /* applyRule5(element, value, typedef); */
    }

    void applyRule5_6(Element element, InputStream value, IntegerTypeDefinition definition) throws RuleException {

        try {

            int len = 0;

            switch (definition.getSize()) {
                case ONE:
                    len = 1;
                    break;
                case TWO:
                    len = 2;
                    break;
                case FOUR:
                    len = 4;
                    break;
                case EIGHT:
                    len = 8;
                    break;
            }

            byte[] val = new byte[len];

            value.read(val);

            BigInteger bi = definition.isSigned() ? new BigInteger(val) : new BigInteger(1, val);
            element.setTextContent(bi.toString());

        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        } catch (IOException ioe) {
            throw new RuleException(ioe);
        }

    }

    void applyRule5_7(Element element, InputStream value, OpaqueTypeDefinition definition) throws RuleException {

        element.setAttributeNS(
                REGXML_NS,
                ACTUALTYPE_ATTR,
                ""
        );

        element.setAttributeNS(
                REGXML_NS,
                BYTEORDER_ATTR,
                BYTEORDER_BE
        );

        /* BUG: buffer lengths should be int */
        /* TODO: figure out opaque encoding */
    }

    void applyRule5_8(Element element, InputStream value, RecordTypeDefinition definition) throws RuleException {

        try {

            MXFInputStream kis = new MXFInputStream(value);

            if (definition.getIdentification().equals(AUID_UL)) {

                AUID auid = kis.readAUID();

                element.setTextContent(auid.toString());

            } else if (definition.getIdentification().equals(DateStruct_UL)) {

                int year = kis.readUnsignedShort();
                int month = kis.readUnsignedByte();
                int day = kis.readUnsignedByte();

                LocalDateTime ldt = LocalDateTime.of(year, month, day, 0, 0);

                OffsetDateTime odt = OffsetDateTime.of(ldt, ZoneOffset.UTC);

                element.setTextContent(odt.format(DateTimeFormatter.ISO_DATE));

            } else if (definition.getIdentification().equals(PackageID_UL)) {

                UMID umid = kis.readUMID();

                element.setTextContent(umid.toString());

            } else if (definition.getIdentification().equals(Rational_UL)) {

                int numerator = kis.readInt();
                int denominator = kis.readInt();

                element.setTextContent(String.format("%d/%d", numerator, denominator));

            } else if (definition.getIdentification().equals(TimeStruct_UL)) {

                /*BUG: fraction is msec/4 according to 377-1 */
                int hour = kis.readUnsignedByte();
                int minute = kis.readUnsignedByte();
                int second = kis.readUnsignedByte();
                int fraction = kis.readUnsignedByte();

                LocalTime lt = LocalTime.of(hour, minute, second, fraction * 4000000);

                OffsetTime ot = OffsetTime.of(lt, ZoneOffset.UTC);

                element.setTextContent(ot.toString());

            } else if (definition.getIdentification().equals(TimeStamp_UL)) {

                int year = kis.readUnsignedShort();
                int month = kis.readUnsignedByte();
                int day = kis.readUnsignedByte();
                int hour = kis.readUnsignedByte();
                int minute = kis.readUnsignedByte();
                int second = kis.readUnsignedByte();
                int fraction = kis.readUnsignedByte();

                LocalDateTime ldt = LocalDateTime.of(year, month, day, hour, minute, second, fraction * 4000000);

                OffsetDateTime odt = OffsetDateTime.of(ldt, ZoneOffset.UTC);

                element.setTextContent(odt.toString());

            } else if (definition.getIdentification().equals(VersionType_UL)) {

                int major = kis.readUnsignedByte();
                int minor = kis.readUnsignedByte();

                element.setTextContent(String.format("%d.%d", major, minor));

            } else {

                for (RecordTypeDefinition.Member member : definition.getMembers()) {

                    Definition itemdef = resolver.getDefinition(member.getType());

                    Element elem = element.getOwnerDocument().createElementNS(definition.getNamespace().toString(), member.getName());

                    elem.setPrefix(getPrefix(definition.getNamespace()));

                    applyRule5(elem, value, itemdef);

                    element.appendChild(elem);
                }
            }

        } catch (IOException ioe) {
            throw new RuleException(ioe);
        }

    }

    void applyRule5_9(Element element, InputStream value, RenameTypeDefinition definition) throws RuleException {

        Definition rdef = resolver.getDefinition(definition.getRenamedType());

        applyRule5(element, value, rdef);

    }

    void applyRule5_10(Element element, InputStream value, SetTypeDefinition definition) throws RuleException {

        Definition typedef = resolver.getDefinition(definition.getElementType());

        try {

            DataInputStream dis = new DataInputStream(value);

            long itemcount = dis.readInt() & 0xfffffffL;
            long itemlength = dis.readInt() & 0xfffffffL;

            applyCoreRule5_4(element, value, typedef, (int) itemcount);

        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        } catch (IOException ioe) {
            /*throw new RuleException(String.format("Value too short for %s", definition.getSymbol()), ioe);*/

            Comment comment = element.getOwnerDocument().createComment(
                    String.format(
                            "Value too short for Type %s",
                            typedef.getSymbol()
                    )
            );

            element.appendChild(comment);
        }

    }

    void applyRule5_11(Element element, InputStream value, StreamTypeDefinition definition) throws RuleException {

        throw new RuleException("Rule 5.11 is not supported yet.");

    }

    void applyRule5_12(Element element, InputStream value, StringTypeDefinition definition) throws RuleException {

        /*TODO: handle integer-based strings Rule 5.12.1 */
        /* ASSUMES THAT VALUE TERMINATES ON THE FIELD */
        /*Rule 5.12 */
        char[] chars = new char[32];

        int c;
        StringBuilder sb = new StringBuilder();

        try {

            Reader in = null;
            
            if (definition.getElementType().equals(Character_UL)) {
                in = new InputStreamReader(value, "UTF-16BE");
            } else if (definition.getElementType().equals(Char_UL)) {
                in = new InputStreamReader(value, "US-ASCII");
            } else {
                throw new RuleException(
                    String.format("String element type %s not supported",
                            definition.getElementType().toString()
                    )
                );
            }
              

            while ((c = in.read(chars)) != -1) {
                sb.append(chars, 0, c);
            }

            /* remove trailing zeroes if any */
            if (sb.length() > 0 && sb.charAt(sb.length() - 1) == 0) {
                sb.deleteCharAt(sb.length() - 1);
            }

            element.setTextContent(sb.toString());

        } catch (IOException ioe) {
            throw new RuleException(ioe);
        }

    }

    void applyRule5_13(Element element, InputStream value, StrongReferenceTypeDefinition definition) throws RuleException {

        Definition typedef = resolver.getDefinition(definition.getReferenceType());

        if (!(typedef instanceof ClassDefinition)) {
            throw new RuleException("Rule 5.13 applied to non class.");
        }

        try {

            MXFInputStream kis = new MXFInputStream(value);

            UUID uuid = kis.readUUID();

            Group g = groups.get(uuid);

            if (g != null) {

                applyRule3(element, g);

            } else {
                LOG.warning(
                        String.format(
                                "Strong Reference %s not found at %s",
                                uuid.toString(),
                                definition.getSymbol()
                        )
                );

                Comment comment = element.getOwnerDocument().createComment(
                        String.format(
                                "Strong Reference %s not found",
                                uuid.toString()
                        )
                );

                element.appendChild(comment);
            }

        } catch (IOException ioe) {
            throw new RuleException(ioe);
        }
    }

    Definition findBaseDefinition(Definition definition) {

        while (definition instanceof RenameTypeDefinition) {
            definition = resolver.getDefinition(((RenameTypeDefinition) definition).getRenamedType());
        }

        return definition;
    }

    final static char[] HEXMAP = "0123456789abcdef".toCharArray();

    String bytesToString(byte[] buffer) {

        char[] out = new char[2 * buffer.length];

        for (int j = 0; j < buffer.length; j++) {

            int v = buffer[j] & 0xFF;
            out[j * 2] = HEXMAP[v >>> 4];
            out[j * 2 + 1] = HEXMAP[v & 0x0F];
        }

        return new String(out);
    }

    void applyRule5_14(Element element, InputStream value, VariableArrayTypeDefinition definition) throws RuleException {

        Definition typedef = resolver.getDefinition(definition.getElementType());

        try {

            DataInputStream dis = new DataInputStream(value);

            /* BUG: UInt8Array is not used correctly for J2K items */
            if (/*definition.getSymbol().equals("UInt8Array")*/false) {

                /* BUG: this is stored as an array that takes the entire item */
                byte[] buffer = new byte[32];

                StringBuilder sb = new StringBuilder();

                for (int sz = 0; (sz = dis.read(buffer)) > -1;) {

                    for (int j = 0; j < sz; j++) {

                        int v = buffer[j] & 0xFF;
                        sb.append(HEXMAP[v >>> 4]);
                        sb.append(HEXMAP[v & 0x0F]);
                    }
                }

                element.setTextContent(sb.toString());

            } else {

                if (definition.getSymbol().equals("DataValue")) {

                    /* RULE 5.14.2 */
                    /* DataValue is string of octets, without number of elements or size of elements */
                    byte[] buffer = new byte[32];

                    StringBuilder sb = new StringBuilder();

                    for (int sz = 0; (sz = dis.read(buffer)) > -1;) {

                        for (int j = 0; j < sz; j++) {

                            int v = buffer[j] & 0xFF;
                            sb.append(HEXMAP[v >>> 4]);
                            sb.append(HEXMAP[v & 0x0F]);
                        }
                    }

                    element.setTextContent(sb.toString());

                } else {

                    long itemcount = dis.readInt() & 0xfffffffL;
                    long itemlength = dis.readInt() & 0xfffffffL;

                    Definition base = findBaseDefinition(typedef);

                    if (base instanceof CharacterTypeDefinition || base.getName().contains("StringArray")) {

                        /* RULE 5.14.1 */
                        /* BUG: where is StringArray defined? */
                        throw new RuleException("StringArray not supported.");

                    } else {

                        applyCoreRule5_4(element, value, typedef, (int) itemcount);
                    }

                }
            }
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        } catch (IOException ioe) {
            throw new RuleException(ioe);
        }

    }

    void applyRule5_15(Element element, InputStream value, WeakReferenceTypeDefinition definition) throws RuleException {

        /* BUG: how does one determine what the weak reference is, e.g. UL, UMID, etc? */
        try {

            MXFInputStream kis = new MXFInputStream(value);
            
            AUID auid = kis.readAUID();

            element.setTextContent(auid.toString());

        } catch (IOException ioe) {
            throw new RuleException(ioe);
        }
    }

    public static class RuleException extends Exception {

        public RuleException(Throwable t) {
            super(t);
        }

        public RuleException(String msg) {
            super(msg);
        }

        public RuleException(String msg, Throwable t) {
            super(msg, t);
        }

    }

}
