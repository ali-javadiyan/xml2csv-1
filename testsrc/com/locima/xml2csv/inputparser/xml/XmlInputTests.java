package com.locima.xml2csv.inputparser.xml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.locima.xml2csv.TestHelpers;
import com.locima.xml2csv.XMLException;
import com.locima.xml2csv.configuration.IMapping;
import com.locima.xml2csv.configuration.IMappingContainer;
import com.locima.xml2csv.configuration.Mapping;
import com.locima.xml2csv.configuration.MappingConfiguration;
import com.locima.xml2csv.configuration.MappingList;
import com.locima.xml2csv.configuration.MultiValueBehaviour;

public class XmlInputTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void test() throws Exception {
		XmlFileParser parser = new XmlFileParser();
		List<File> files = new ArrayList<File>();
		files.add(TestHelpers.createFile("SimpleFamilyConfig.xml"));
		parser.load(files);
		MappingConfiguration config = parser.getMappings();
		assertNotNull("MappingSet was null, should be non-null", config);
		assertEquals(2, config.size());

		IMappingContainer mapping1 = config.getContainerByName("Family");
		assertNotNull("Couldn't find family mapping", mapping1);

		Iterator<IMapping> iter = mapping1.iterator();
		String[] baseNames = new String[] { "Family", "Address" };
		int count;
		for (count = 0; iter.hasNext(); count++) {
			Mapping mapping = (Mapping) iter.next();
			assertEquals(baseNames[count], mapping.getName());
		}
		assertEquals(2, count);
	}

	@Test
	public void testBadInline() throws Exception {
		XmlFileParser parser = new XmlFileParser();
		List<File> files = new ArrayList<File>();
		files.add(TestHelpers.createFile("SimpleFamilyConfigBadInline.xml"));
		this.thrown.expect(XMLException.class);
		parser.load(files);
	}

	@Test
	public void testMappingConfigurationXMLParser() throws Exception {
		XmlFileParser parser = new XmlFileParser();
		List<File> files = new ArrayList<File>();
		files.add(TestHelpers.createFile("SimpleFamilyInlineConfig.xml"));
		parser.load(files);
		MappingConfiguration config = parser.getMappings();
		assertNotNull("MappingSet was null, should be non-null", config);
		assertEquals(1, config.size());

		MappingList topLevelMappingList = (MappingList) config.getContainerByName("Family");
		assertEquals(3, topLevelMappingList.size());
		assertEquals("Family", topLevelMappingList.getName());
		topLevelMappingList.get(0);

		MappingList membersMappingList = (MappingList) topLevelMappingList.get(1);
		assertEquals(2, membersMappingList.size());
		Mapping firstNameMapping = (Mapping) membersMappingList.get(0);
		assertEquals(MultiValueBehaviour.LAZY, firstNameMapping.getMultiValueBehaviour());
		Mapping ageMapping = (Mapping) membersMappingList.get(1);
		assertEquals(MultiValueBehaviour.LAZY, ageMapping.getMultiValueBehaviour());

		topLevelMappingList.get(2);
	}

	@Test
	public void testNamespaces() throws Exception {
		XmlFileParser parser = new XmlFileParser();
		List<File> files = new ArrayList<File>();
		files.add(TestHelpers.createFile("FamilyConfigWithNamespaces.xml"));
		parser.load(files);
		MappingConfiguration set = parser.getMappings();
		assertNotNull("MappingSet was null, should be non-null", set);
		assertEquals(2, set.size());

		IMappingContainer mapping1 = set.getContainerByName("FamilyWithNamespaces");
		assertNotNull("Couldn't find family mapping", mapping1);
		IMappingContainer mapping2 = set.getContainerByName("FamilyMembersWithNamespaces");
		assertNotNull("Couldn't find family mapping", mapping2);
	}

}
