package com.locima.xml2csv.extractor;

import java.io.File;
import java.util.AbstractList;
import java.util.Collections;
import java.util.List;
import java.util.RandomAccess;

import net.sf.saxon.s9api.DocumentBuilder;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmNode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.locima.xml2csv.ArgumentNullException;
import com.locima.xml2csv.SaxonProcessorManager;
import com.locima.xml2csv.model.IMappingContainer;
import com.locima.xml2csv.model.MappingConfiguration;
import com.locima.xml2csv.output.IOutputManager;
import com.locima.xml2csv.output.OutputManagerException;

/**
 * Extracts data from XML documents provided according a @see MappingConfiguration provided.
 * <p>
 * This makes use of Saxon for parsing XPath, because Saxon was the only XPath 2.0 compliant parser I could find. JAXP does NOT support default
 * namespace prefixes (part of XPath 2.0), so had to resort to the native Saxon APIs. All suggestions seem to be "just rewrite the XPath statement to
 * include a NS declaration. This Apple-style suggestion only works when I have control over the input files. For some use-cases this is not the case.
 */
public class XmlDataExtractor {

	/**
	 * Provides a convenience wrapper that allows us to iterate over {@link NodeList} instances.
	 */
	static final class NodeListWrapper extends AbstractList<Node> implements RandomAccess {
		private final NodeList list;

		/**
		 * Initialises an instance to be wrapped around the passed node list.
		 *
		 * @param nodeList the node list to wrap around. Must not be null.
		 */
		NodeListWrapper(NodeList nodeList) {
			if (nodeList == null) {
				throw new ArgumentNullException("nodeList");
			}
			this.list = nodeList;
		}

		/**
		 * Retrieves the item at the given index.
		 *
		 * @param index the index of the item to return. Index starts at 0.
		 * @return The item at the given index, or throws {@link IndexOutOfBoundsException}
		 */
		@Override
		public Node get(int index) {
			return this.list.item(index);
		}

		/**
		 * Returns the number of items in the node list.
		 *
		 * @return the number of items in the node list this object is wrapping.
		 */
		@Override
		public int size() {
			return this.list.getLength();
		}
	}

	private static final Logger LOG = LoggerFactory.getLogger(XmlDataExtractor.class);

	/**
	 * Returns a {@link NodeList} an iterable list.
	 *
	 * @param n the node list to wrap.
	 * @return a wrapped node list that can be iterated over more easily.
	 */
	public static List<Node> asList(NodeList n) {
		return n.getLength() == 0 ? Collections.<Node>emptyList() : new NodeListWrapper(n);
	}

	private MappingConfiguration mappingConfiguration;

	private Processor saxonProcessor;

	/**
	 * Is set, then all whitespace will be trimmed from the beginning and end of element and attribute values.
	 */
	private boolean trimWhitespace;

	/**
	 * Initialise the internal Saxon Processor.
	 */
	public XmlDataExtractor() {
		this.saxonProcessor = SaxonProcessorManager.getProcessor();
	}

	/**
	 * Extracts data from the <code>xmlFile</code> passed and pushes that data to the <code>om</code>.
	 *
	 * @param xmlFile The XML file to read data from, must be a valid file.
	 * @param om The output manager to write data to, must be a valid instance.
	 * @throws DataExtractorException If an error occurs during extraction of data from the XML.
	 * @throws OutputManagerException If an error occurs writing the data out.
	 */
	public void convert(File xmlFile, IOutputManager om) throws DataExtractorException, OutputManagerException {
		try {
			DocumentBuilder db = this.saxonProcessor.newDocumentBuilder();
			LOG.info("Loading and parsing XML file {}", xmlFile.getName());
			XdmNode document = db.build(xmlFile);
			LOG.debug("XML file loaded succesfully");
			convert(document, om);
		} catch (SaxonApiException e) {
			throw new DataExtractorException(e, "Unable to read XML file %s", xmlFile);
		}
	}

	/**
	 * Executes the mappingConfiguration set by {@link #setMappingConfiguration(MappingConfiguration)} against a document <code>xmlDoc</code> and
	 * passes the results to <code>outputManager</code>.
	 * <p>
	 * Typically called by {@link #convert(File, IOutputManager)} once the XML file is loaded.
	 *
	 * @param xmlDoc The XML document to extract information from.
	 * @param outputManager The output manager to send the extracted data to. May be null if no output is required.
	 * @throws DataExtractorException If an error occurred extracting data from the XML document.
	 * @throws OutputManagerException If an error occurred writing data to the output manager.
	 */
	public void convert(XdmNode xmlDoc, IOutputManager outputManager) throws DataExtractorException, OutputManagerException {
		LOG.trace("Executing {} sets of mappingConfiguration.", this.mappingConfiguration.size());
		for (IMappingContainer mapping : this.mappingConfiguration) {
			List<List<String>> records = mapping.evaluateToRecordList(xmlDoc, this.trimWhitespace);
			if (outputManager != null) {
				for (List<String> record : records) {
					outputManager.writeRecords(mapping.getOutputName(), record);
				}
			} else {
				LOG.trace("No IOutputManager passed so not writing any output.");
			}
		}
	}
	
	/**
	 * Configure this extractor with the set of mappingConfiguration specified.
	 *
	 * @param newMappings the mappingConfiguration that define how to extract data from the XML when {@link #extractTo} is called.
	 */
	public void setMappingConfiguration(MappingConfiguration newMappings) {
		this.mappingConfiguration = newMappings;
	}

	/**
	 * If set to true, then all whitespace will be trimmed from the beginning and end of values extracted from XML. If false, they will be left alone.
	 * <p>
	 * The typically required behaviour is to set this to true.
	 *
	 * @param trimWhitespace whether to trim whitespace from the beginning and end of values extracted from XML.
	 */
	public void setTrimWhitespace(boolean trimWhitespace) {
		this.trimWhitespace = trimWhitespace;
	}
}
