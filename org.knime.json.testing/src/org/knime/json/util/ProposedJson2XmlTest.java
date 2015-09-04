/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ---------------------------------------------------------------------
 *
 * History
 *   9 Nov. 2014 (Gabor): created
 */
package org.knime.json.util;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.knime.json.util.Json2Xml.Json2XmlSettings;
import org.knime.json.util.Json2Xml.Options;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Tests for {@link Json2Xml}.
 *
 * @author Gabor Bakos
 */
@RunWith(Parameterized.class)
public class ProposedJson2XmlTest {
    private static int counter = 0;

    private final String m_expected, m_inputJson;

    private Json2Xml m_converter;

    private Options[] m_options;

    /**
     * @return The expected xml as a {@link String}, the input JSON also as a {@link String} and the {@link Options}
     *         array.
     */
    @Parameters(/*name = "{index}: input:{1}"*/)
    public static List<Object[]> parameters() {
        List<Object[]> ret = new ArrayList<>();
        ret.add(new Object[]{"<root/>", "{}", new Options[]{}});
        ret.add(new Object[]{"<Array:root xmlns:Array=\"http://www.w3.org/2001/XMLSchema/list\"/>", "[]",
            new Options[]{}});
        ret.add(new Object[]{"<root>3</root>", "3", new Options[]{Options.looseTypeInfo}});
        ret.add(new Object[]{"<root>0.3</root>", "0.3", new Options[]{Options.looseTypeInfo}});
        ret.add(new Object[]{"<root>true</root>", "true", new Options[]{Options.looseTypeInfo}});
        ret.add(new Object[]{"<root>3</root>", "\"3\"", new Options[]{Options.looseTypeInfo}});
        ret.add(new Object[]{"<root/>", "null", new Options[]{Options.looseTypeInfo}});
        ret.add(new Object[]{"<Int:root xmlns:Int=\"http://www.w3.org/2001/XMLSchema/integer\">3</Int:root>", "3",
            new Options[]{}});
        ret.add(new Object[]{"<Real:root xmlns:Real=\"http://www.w3.org/2001/XMLSchema/decimal\">-0.3</Real:root>",
            "-0.3", new Options[]{}});
        ret.add(new Object[]{"<Bool:root xmlns:Bool=\"http://www.w3.org/2001/XMLSchema/boolean\">false</Bool:root>",
            "false", new Options[]{}});
        ret.add(new Object[]{"<Text:root xmlns:Text=\"http://www.w3.org/2001/XMLSchema/string\">ab</Text:root>",
            "\"ab\"", new Options[]{Options.UseParentKeyWhenPossible}});
        ret.add(new Object[]{"<null:root xmlns:null=\"http://www.w3.org/2001/XMLSchema\"/>", "null", new Options[]{}});
        ret.add(new Object[]{"<root><item>3</item></root>", "[3]", new Options[]{Options.looseTypeInfo}});
        ret.add(new Object[]{"<root><item>3</item></root>", "[\"3\"]", new Options[]{Options.looseTypeInfo}});
        ret.add(new Object[]{"<root><item>3</item><item>4</item></root>", "[3, 4]",
            new Options[]{Options.looseTypeInfo}});
        ret.add(new Object[]{"<root><a>4</a></root>", "{\"a\":4}", new Options[]{Options.looseTypeInfo}});
        ret.add(new Object[]{"<root><a>4</a></root>", "{\"a\":\"4\"}", new Options[]{Options.looseTypeInfo}});
        ret.add(new Object[]{"<root><a>34</a></root>", "{\"a\":\"4\", \"a\":34}", new Options[]{Options.looseTypeInfo}});
        ret.add(new Object[]{"<root><a/></root>", "{\"a\":{}}", new Options[]{Options.looseTypeInfo}});
        ret.add(new Object[]{"<root><a><x>4</x></a></root>", "{\"a\":{\"x\":4}}", new Options[]{Options.looseTypeInfo}});
        ret.add(new Object[]{"<root><a><x>4</x><y>\"44\"</y></a></root>", "{\"a\":{\"x\":4,\"y\":\"\\\"44\\\"\"}}",
            new Options[]{Options.looseTypeInfo}});
        //This might be fishy.<a x="4"/><a x="4"/> might be a better option, but that breaks a few other tests.
        ret.add(new Object[]{"<root><a><item><x>4</x></item><item><x>4</x></item></a></root>",
            "{\"a\":[{\"x\":4},{\"x\":4}]}", new Options[]{Options.looseTypeInfo}});
        ret.add(new Object[]{
            "<root><a><item x=\"4\"><y><v>2</v></y></item><item><x>0</x><y><v>3</v></y></item></a></root>",
            "{\"a\":[{\"@x\":4, \"y\":{\"v\":2}},{\"x\":0, \"y\":{\"v\":3}}]}", new Options[]{Options.looseTypeInfo}});
        ret.add(new Object[]{"<root><a><item><item><x>4</x></item><item><x>4</x></item></item></a></root>",
            "{\"a\":[[{\"x\":4},{\"x\":4}]]}", new Options[]{Options.looseTypeInfo}});
        ret.add(new Object[]{
            "<root><a><item><item><x>4</x></item><item><item><x>4</x></item></item></item></a></root>",
            "{\"a\":[[{\"x\":4},[{\"x\":4}]]]}", new Options[]{Options.looseTypeInfo}});
        ret.add(new Object[]{
            "<root><a><item><item><item><x>4</x></item><item><item><x>4</x></item></item></item></item></a></root>",
            "{\"a\":[[[{\"x\":4},[{\"x\":4}]]]]}", new Options[]{Options.looseTypeInfo}});
        ret.add(new Object[]{"<root><a><y><z>t</z></y><x>4</x></a></root>", "{\"a\":{\"y\":{\"z\":\"t\"},\"x\":4}}",
            new Options[]{Options.looseTypeInfo}});
        ret.add(new Object[]{"<root><a><b><item>z</item><item><w/></item></b></a></root>",
            "{\"a\":{\"b\":[\"z\",{\"w\":{}}]}}", new Options[]{Options.looseTypeInfo}});
        ret.add(new Object[]{"<root><a><b>z</b><b><w/></b></a></root>", "{\"a\":{\"b\":[\"z\",{\"w\":{}}]}}",
            new Options[]{Options.looseTypeInfo, Options.UseParentKeyWhenPossible}});
        ret.add(new Object[]{"<root><item/></root>", "[{}]", new Options[]{Options.looseTypeInfo}});
        ret.add(new Object[]{"<root><a><b><item><c>2</c><d><t>text</t></d><e><t>text2</t></e></item></b></a></root>",
            "{\"a\":{\"b\":[{\"c\":2,\"d\":{\"t\":\"text\"},\"e\":{\"t\":\"text2\"}}]}}",
            new Options[]{Options.looseTypeInfo}});
        ret.add(new Object[]{
            "<root xmlns:Int=\"http://www.w3.org/2001/XMLSchema/integer\" xmlns:Text=\"http://www.w3.org/2001/XMLSchema/string\"><a><Text:item><Int:c>2</Int:c>text</Text:item></a></root>",
            "{\"a\":[{\"c\":2,\"#text\":\"text\"}]}", new Options[]{}});
        ret.add(new Object[]{"<root><a><c>2</c>text</a></root>", "{\"a\":[{\"c\":2,\"#text\":\"text\"}]}",
            new Options[]{Options.looseTypeInfo, Options.UseParentKeyWhenPossible}});
        ret.add(new Object[]{"<root><a><item><q>p</q></item></a></root>", "{\"a\":[{\"q\":{\"#text\":\"p\"}}]}",
            new Options[]{Options.looseTypeInfo}});
        ret.add(new Object[]{"<root><a><b><item><v>2</v><q>p</q><r>s</r></item></b></a></root>",
            "{\"a\":{\"b\":[{\"v\":\"2\",\"q\":{\"#text\":\"p\"},\"r\":{\"#text\":\"s\"}}]}}",
            new Options[]{Options.looseTypeInfo}});
        ret.add(new Object[]{"<root><a><b><item><v>1</v><q>p</q></item><item><v>2</v><q>z</q></item></b></a></root>",
            "{\"a\":{\"b\":[{\"v\":\"1\",\"q\":{\"#text\":\"p\"}},{\"v\":\"2\",\"q\":{\"#text\":\"z\"}}]}}",
            new Options[]{Options.looseTypeInfo}});
        ret.add(new Object[]{"<root><a x=\"4\"><y><v>2</v></y></a><a><x>0</x><y><v>3</v></y></a></root>",
            "{\"a\":[{\"@x\":4, \"y\":{\"v\":2}},{\"x\":0, \"y\":{\"v\":3}}]}",
            new Options[]{Options.looseTypeInfo, Options.UseParentKeyWhenPossible}});
        ret.add(new Object[]{"<root><a><x>4</x></a><a><x>4</x></a></root>", "{\"a\":[{\"x\":4},{\"x\":4}]}",
            new Options[]{Options.looseTypeInfo, Options.UseParentKeyWhenPossible}});
        ret.add(new Object[]{"<root><a><b/><c>attr</c></a></root>", "{\"a\":[{\"b\":{},\"c\":\"attr\"}]}",
            new Options[]{Options.looseTypeInfo, Options.UseParentKeyWhenPossible}});
        ret.add(new Object[]{
            "<root xmlns:Text=\"http://www.w3.org/2001/XMLSchema/string\"><a><Text:b>z</Text:b><b><w/></b></a></root>",
            "{\"a\":{\"b\":[\"z\",{\"w\":{}}]}}", new Options[]{Options.UseParentKeyWhenPossible}});
        //Ambiguity on source?
        ret.add(new Object[]{"<root><a><item><c>2</c>text</item></a></root>",
            "{\"a\":[[{\"c\":2,\"#text\":\"text\"}]]}",
            new Options[]{Options.looseTypeInfo, Options.UseParentKeyWhenPossible}});
        ret.add(new Object[]{"<root><a><c>2</c><v>text</v></a></root>",
            "{\"a\":[{\"c\":2,\"v\":[{\"#text\":\"text\"}]}]}",
            new Options[]{Options.looseTypeInfo, Options.UseParentKeyWhenPossible}});
        ret.add(new Object[]{"<root><a><c>2</c><v><3>3</3>text</v></a></root>",
            "{\"a\":[{\"c\":2,\"v\":[{\"3\":3,\"#text\":\"text\"}]}]}",
            new Options[]{Options.looseTypeInfo, Options.UseParentKeyWhenPossible}});
        ret.add(new Object[]{"<root><a><c>2</c>334<v><3>3</3>text</v></a></root>",
            "{\"a\":[{\"c\":2,\"#text\":334,\"v\":[{\"3\":3,\"#text\":\"text\"}]}]}",
            new Options[]{Options.looseTypeInfo, Options.UseParentKeyWhenPossible}});
        ret.add(new Object[]{"<root><a><c>2</c><v><3>3</3>text</v>false</a></root>",
            "{\"a\":[{\"c\":2,\"v\":[{\"3\":3,\"#text\":\"text\"}],\"#text\":false}]}",
            new Options[]{Options.looseTypeInfo, Options.UseParentKeyWhenPossible}});
        ret.add(new Object[]{"<root><a>3</a><a>3</a></root>", "{ \"a\" : [ 3, 3 ] }",
            new Options[]{Options.looseTypeInfo, Options.UseParentKeyWhenPossible}});
        ret.add(new Object[]{"<root><a><x>4</x></a><a><item><x>4</x></item></a></root>",
            "{\"a\":[{\"x\":4},[{\"x\":4}]]}", new Options[]{Options.looseTypeInfo, Options.UseParentKeyWhenPossible}});
        ret.add(new Object[]{"<root><a><item><x>4</x><item><x>4</x></item></item></a></root>",
            "{\"a\":[[{\"x\":4},[{\"x\":4}]]]}", new Options[]{Options.looseTypeInfo, Options.UseParentKeyWhenPossible}});
        ret.add(new Object[]{"<root><a><item><item><x>4</x><item><x>4</x></item></item></item></a></root>",
            "{\"a\":[[[{\"x\":4},[{\"x\":4}]]]]}",
            new Options[]{Options.looseTypeInfo, Options.UseParentKeyWhenPossible}});
        ret.add(new Object[]{
            "<root><a><item><item><item><x>4</x><item><x>4</x></item></item></item></item></a></root>",
            "{\"a\":[[[[{\"x\":4},[{\"x\":4}]]]]]}",
            new Options[]{Options.looseTypeInfo, Options.UseParentKeyWhenPossible}});
        ret.add(new Object[]{"<root><item><a>3</a></item><item><b>3</b></item></root>", "[ {\"a\":3}, {\"b\": 3} ] }",
            new Options[]{Options.looseTypeInfo, Options.UseParentKeyWhenPossible}});
        ret.add(new Object[]{"<root><item><a><b>2</b></a><a><c>3</c></a></item></root>",
            "[{ \"a\" : [{\"b\":2}, {\"c\":3}] }]",
            new Options[]{Options.looseTypeInfo, Options.UseParentKeyWhenPossible}});
        ret.add(new Object[]{"<root><item><a><x>4</x></a><a><item><x>4</x></item></a></item></root>",
            "[{\"a\":[{\"x\":4},[{\"x\":4}]]}]", new Options[]{Options.looseTypeInfo, Options.UseParentKeyWhenPossible}});
        ret.add(new Object[]{"<root><item><a><item><x>4</x><item><x>4</x></item></item></a></item></root>",
            "[{\"a\":[[{\"x\":4},[{\"x\":4}]]]}]",
            new Options[]{Options.looseTypeInfo, Options.UseParentKeyWhenPossible}});
        ret.add(new Object[]{
            "<root><item><a><item><item><x>4</x><item><x>4</x></item></item></item></a></item></root>",
            "[{\"a\":[[[{\"x\":4},[{\"x\":4}]]]]}]",
            new Options[]{Options.looseTypeInfo, Options.UseParentKeyWhenPossible}});
        ret.add(new Object[]{
            "<root><item><a><item><item><item><x>4</x><item><x>4</x></item></item></item></item></a></item></root>",
            "[{\"a\":[[[[{\"x\":4},[{\"x\":4}]]]]]}]",
            new Options[]{Options.looseTypeInfo, Options.UseParentKeyWhenPossible}});
        ret.add(new Object[]{"<root><a><b>z</b><b><w/></b></a></root>", "{\"a\":{\"b\":[\"z\",{\"w\":{}}]}}",
            new Options[]{Options.looseTypeInfo, Options.UseParentKeyWhenPossible}});
        ret.add(new Object[]{"<root><a><c>2</c>text</a></root>", "{\"a\":[{\"c\":2,\"#text\":\"text\"}]}",
            new Options[]{Options.looseTypeInfo, Options.UseParentKeyWhenPossible}});
        ret.add(new Object[]{"<root><a x=\"4\"><y><v>2</v></y></a><a><x>0</x><y><v>3</v></y></a></root>",
            "{\"a\":[{\"@x\":4, \"y\":{\"v\":2}},{\"x\":0, \"y\":{\"v\":3}}]}",
            new Options[]{Options.looseTypeInfo, Options.UseParentKeyWhenPossible}});
        ret.add(new Object[]{"<root><a><x>4</x></a><a><x>4</x></a></root>", "{\"a\":[{\"x\":4},{\"x\":4}]}",
            new Options[]{Options.looseTypeInfo, Options.UseParentKeyWhenPossible}});
        ret.add(new Object[]{"<root><a><b/><c>attr</c></a></root>", "{\"a\":[{\"b\":{},\"c\":\"attr\"}]}",
            new Options[]{Options.looseTypeInfo, Options.UseParentKeyWhenPossible}});
        ret.add(new Object[]{
            "<root xmlns:Text=\"http://www.w3.org/2001/XMLSchema/string\"><a><Text:b>z</Text:b><b><w/></b></a></root>",
            "{\"a\":{\"b\":[\"z\",{\"w\":{}}]}}", new Options[]{Options.UseParentKeyWhenPossible}});
        //Ambiguity on source?
        ret.add(new Object[]{"<root><a><item><c>2</c>text</item></a></root>",
            "{\"a\":[[{\"c\":2,\"#text\":\"text\"}]]}",
            new Options[]{Options.looseTypeInfo, Options.UseParentKeyWhenPossible}});
        ret.add(new Object[]{"<root><a><c>2</c><v>text</v></a></root>",
            "{\"a\":[{\"c\":2,\"v\":[{\"#text\":\"text\"}]}]}",
            new Options[]{Options.looseTypeInfo, Options.UseParentKeyWhenPossible}});
        ret.add(new Object[]{"<root><a><c>2</c><v><3>3</3>text</v></a></root>",
            "{\"a\":[{\"c\":2,\"v\":[{\"3\":3,\"#text\":\"text\"}]}]}",
            new Options[]{Options.looseTypeInfo, Options.UseParentKeyWhenPossible}});
        ret.add(new Object[]{"<root><a><c>2</c>334<v><3>3</3>text</v></a></root>",
            "{\"a\":[{\"c\":2,\"#text\":334,\"v\":[{\"3\":3,\"#text\":\"text\"}]}]}",
            new Options[]{Options.looseTypeInfo, Options.UseParentKeyWhenPossible}});
        ret.add(new Object[]{"<root><a><c>2</c><v><3>3</3>text</v>false</a></root>",
            "{\"a\":[{\"c\":2,\"v\":[{\"3\":3,\"#text\":\"text\"}],\"#text\":false}]}",
            new Options[]{Options.looseTypeInfo, Options.UseParentKeyWhenPossible}});
        //This was a bit inconsistent, probably in this case we cannot omit the parent item
        //        ret.add(new Object[]{"<root a=\"3\"><b>4</b><c><item d=\"1\"/></c></root>", "{ \"@a\" : 3, \"b\": 4, \"c\":[[{ \"@d\" : 1}]] }",
        //            new Options[]{Options.looseTypeInfo, Options.UseParentKeyWhenPossible}});
        ret.add(new Object[]{"<root a=\"3\"><b>4</b><c><item><item d=\"1\"/></item></c></root>",
            "{ \"@a\" : 3, \"b\": 4, \"c\":[[{ \"@d\" : 1}]] }",
            new Options[]{Options.looseTypeInfo, Options.UseParentKeyWhenPossible}});
        ret.add(new Object[]{"<root a=\"3\"><b>4</b><c d=\"1\"/></root>",
            "{ \"@a\" : 3, \"b\": 4, \"c\":[{ \"@d\" : 1}] }",
            new Options[]{Options.looseTypeInfo, Options.UseParentKeyWhenPossible}});
        ret.add(new Object[]{"<root><item>1</item><item>2</item><item a=\"3\"><b>4</b></item></root>",
            "[1, 2, { \"@a\" : 3, \"b\": 4 }]", new Options[]{Options.looseTypeInfo, Options.UseParentKeyWhenPossible}});
        //Different option might be also acceptable, where the <a> tag is also an <item>
        ret.add(new Object[]{"<root><item>1</item><item>2</item><item><a><b>2</b></a><a><c>3</c></a></item></root>",
            "[1, 2, { \"a\" : [{\"b\":2}, {\"c\":3}] }]",
            new Options[]{Options.looseTypeInfo, Options.UseParentKeyWhenPossible}});
        ret.add(new Object[]{"<root><a b=\"2\"/></root>", "{\"a\":{\"@b\":2}}", new Options[]{Options.looseTypeInfo}});
        ret.add(new Object[]{
            "<root><a><b xmlns:ns=\"http://www.knime.org/json2xml/originalKey/\" ns:originalKey=\"@b\"><c>3</c></b></a></root>",
            "{\"a\": { \"@b\": {\"c\":3}}}", new Options[]{Options.looseTypeInfo}});
        ret.add(new Object[]{"<root><a><item>3</item><item>3</item></a></root>", "{\"a\" : [ 3, 3 ]}",
            new Options[]{Options.looseTypeInfo}});
        ret.add(new Object[]{"<root><a><item>3</item><item><q>1</q></item></a></root>", "{\"a\":[3, {\"q\":1}]}",
            new Options[]{Options.looseTypeInfo}});
        ret.add(new Object[]{"<root><a><item>3</item><item/></a></root>", "{\"a\":[3, {}]}",
            new Options[]{Options.looseTypeInfo}});
        ret.add(new Object[]{"<root a=\"3\"/>", "{\"@a\":3}", new Options[]{Options.looseTypeInfo}});
        ret.add(new Object[]{
            "<root><control xmlns:ns=\"http://www.knime.org/json2xml/originalKey/\" ns:originalKey=\"@control\"><x>y</x></control></root>",
            "{\"@control\" : { \"x\": \"y\"}}", new Options[]{Options.looseTypeInfo}});
        ret.add(new Object[]{"<root><a><item><item><x>true</x></item><item>4</item></item></a></root>",
            "{\"a\" : [ [ {\"x\" : true}, 4 ] ]}",
            new Options[]{Options.UseParentKeyWhenPossible, Options.looseTypeInfo}});
        ret.add(new Object[]{"<root><a><item><y>4</y><x>true</x></item></a></root>",
            "{\"a\" : [ [ {\"y\" : 4}, {\"x\" : true} ] ]}",
            new Options[]{Options.UseParentKeyWhenPossible, Options.looseTypeInfo}});
        ret.add(new Object[]{"<root><a><item><y>4</y><item x=\"true\"/></item></a></root>",
            "{\"a\" : [ [ {\"y\" : 4}, {\"@x\" : true} ] ]}",
            new Options[]{Options.UseParentKeyWhenPossible, Options.looseTypeInfo}});
        ret.add(new Object[]{"<root><a><item><item x=\"true\"/></item></a></root>",
            "{\"a\" : [ [ {\"@x\" : true} ] ]}", new Options[]{Options.UseParentKeyWhenPossible, Options.looseTypeInfo}});
        ret.add(new Object[]{"<root><a><item><item>3</item><item x=\"true\"/></item></a></root>",
            "{\"a\" : [ [ 3, {\"@x\" : true} ] ]}",
            new Options[]{Options.UseParentKeyWhenPossible, Options.looseTypeInfo}});
        ret.add(new Object[]{"<root><a><item><y>4</y></item></a></root>", "{\"a\" : [ [ {\"y\" : 4}] ]}",
            new Options[]{Options.UseParentKeyWhenPossible, Options.looseTypeInfo}});
        ret.add(new Object[]{"<root><a><item><item>3</item></item></a></root>", "{\"a\" : [ [ 3 ] ]}",
            new Options[]{Options.UseParentKeyWhenPossible, Options.looseTypeInfo}});
        ret.add(new Object[]{"<root><a><item><y>4</y><item><item x=\"true\"/></item></item></a></root>",
            "{\"a\" : [ [ {\"y\" : 4}, [{\"@x\" : true}] ] ]}",
            new Options[]{Options.UseParentKeyWhenPossible, Options.looseTypeInfo}});
        ret.add(new Object[]{"<root><a><y>4</y></a><a x=\"true\"/></root>",
            "{\"a\" : [ {\"y\" : 4}, {\"@x\" : true}]}",
            new Options[]{Options.UseParentKeyWhenPossible, Options.looseTypeInfo}});
        ret.add(new Object[]{"<root xmlns:Int=\"http://www.w3.org/2001/XMLSchema/integer\"><Int:blahblah xmlns:ns=\"http://www.knime.org/json2xml/originalKey/\" ns:originalKey=\"blah blah\">3</Int:blahblah></root>", "{\"blah blah\" : [ 3]}",
            new Options[]{Options.UseParentKeyWhenPossible}});
        ret.add(new Object[]{"<root xmlns:Int=\"http://www.w3.org/2001/XMLSchema/integer\"><Int:a>3</Int:a></root>", "{\"a\" : [ 3]}",
            new Options[]{Options.UseParentKeyWhenPossible}});
        ret.add(new Object[]{"<root><item/><item/></root>",
            "[ [], []]",
            new Options[]{Options.UseParentKeyWhenPossible, Options.looseTypeInfo}});
        ret.add(new Object[]{"<root><a><item/></a><a><item/></a></root>",
            "{\"a\": [ [], []]}",
            new Options[]{Options.UseParentKeyWhenPossible, Options.looseTypeInfo}});
        ret.add(new Object[]{"<root><item><item>4</item></item><item><item>2</item></item></root>",
            "[ [4], [2]]",
            new Options[]{Options.UseParentKeyWhenPossible, Options.looseTypeInfo}});
        ret.add(new Object[]{"<root><item>4</item><item>2</item></root>",
            "[ 4, 2]",
            new Options[]{Options.UseParentKeyWhenPossible, Options.looseTypeInfo}});
        ret.add(new Object[]{"<root><item>4</item><item><item><item>3</item><item>2</item></item></item></root>",
            "[ 4, [[3, 2]]]",
            new Options[]{Options.UseParentKeyWhenPossible, Options.looseTypeInfo}});
        ret.add(new Object[]{"<root><item>4</item><item><item>3</item><item>2</item></item></root>",
            "[ 4, [3, 2]]",
            new Options[]{Options.UseParentKeyWhenPossible, Options.looseTypeInfo}});
        ret.add(new Object[]{"<root><item><b>4</b></item><item>3</item><item>2</item></root>",
            "[ {\"b\":4}, 3, 2]",
            new Options[]{Options.UseParentKeyWhenPossible, Options.looseTypeInfo}});
        ret.add(new Object[]{"<root><item><b>4</b></item><item><item>3</item><item>2</item></item></root>",
            "[ {\"b\":4}, [3, 2]]",
            new Options[]{Options.UseParentKeyWhenPossible, Options.looseTypeInfo}});
        ret.add(new Object[]{"<root><a><b>4</b></a><a>3</a><a>2</a></root>",
            "{\"a\" : [ {\"b\":4}, 3, 2]}",
            new Options[]{Options.UseParentKeyWhenPossible, Options.looseTypeInfo}});
        ret.add(new Object[]{"<root><a><item><item>4</item></item></a><a>3</a></root>",
            "{\"a\" : [ [4], 3]}",
            new Options[]{Options.UseParentKeyWhenPossible, Options.looseTypeInfo}});
        ret.add(new Object[]{"<root><a><b>4</b></a><a><item><item>3</item></item></a></root>",
            "{\"a\" : [ {\"b\":4}, [3]]}",
            new Options[]{Options.UseParentKeyWhenPossible, Options.looseTypeInfo}});
        return ret;
    }

    /**
     * @param expected
     * @param inputJson
     * @param os The options to be used.
     */
    public ProposedJson2XmlTest(final String expected, final String inputJson, final Options... os) {
        super();
        this.m_expected = expected;
        this.m_inputJson = inputJson;
        this.m_options = os;
    }

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        ++counter;
        EnumSet<Options> set = EnumSet.noneOf(Options.class);
        set.addAll(Arrays.asList(m_options));
        if (set.contains(Options.UseParentKeyWhenPossible)) {
            m_converter = Json2Xml.createWithUseParentKeyWhenPossible(new Json2XmlSettings());
            m_converter.setLooseTypeInfo(set.contains(Options.looseTypeInfo));
        } else {
            m_converter = new Json2Xml();
            m_converter.setOptions(m_options);
        }
    }

    /**
     * Test method for {@link org.knime.json.util.Json2Xml#toXml(com.fasterxml.jackson.databind.JsonNode)}.
     *
     * @throws IOException
     * @throws ParserConfigurationException
     * @throws JsonProcessingException
     * @throws DOMException
     * @throws TransformerException
     */
    @Test
    public void testToXml() throws DOMException, JsonProcessingException, ParserConfigurationException, IOException,
        TransformerException {
        JsonNode json = new ObjectMapper().readTree(m_inputJson);
        //        System.out.println(counter + " " + json);
        if (m_converter.getClass() == Json2Xml.class) {
        }
        {
            Document doc = m_converter.toXml(json);
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(doc), new StreamResult(writer));
            String output = writer.getBuffer().toString().replaceAll("\n|\r", "");
            System.out.println(counter + "\t" + json + "\t" + output);
            String schemaDeclarationPattern = "xmlns:\\w+=\"http://www.w3.org/2001/XMLSchema/\\w+\"";
            assertEquals(json.toString(), m_expected.replaceAll(schemaDeclarationPattern, " "),
                output.replaceAll("#34", "quot").replaceAll(schemaDeclarationPattern, " "));
        }
    }

}
