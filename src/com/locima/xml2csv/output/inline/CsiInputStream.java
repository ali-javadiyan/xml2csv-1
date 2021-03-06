package com.locima.xml2csv.output.inline;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.locima.xml2csv.configuration.IMapping;
import com.locima.xml2csv.configuration.IMappingContainer;
import com.locima.xml2csv.configuration.IValueMapping;
import com.locima.xml2csv.extractor.ContainerExtractionContext;
import com.locima.xml2csv.extractor.XmlDataExtractor;
import com.locima.xml2csv.output.IExtractionResultsContainer;
import com.locima.xml2csv.output.OutputManagerException;

/**
 * Extends an {@link ObjectInputStream} with a single method to get the next intermediate format record from a CSI file.
 * <p>
 * The logic in here relies on a null being written to the end of the CSI file when it's created.
 */
public class CsiInputStream extends ObjectInputStream {

	private static final Logger LOG = LoggerFactory.getLogger(CsiInputStream.class);

	private Map<String, IMapping> nameToMapping;

	/**
	 * Records how many objects have been read by a call to {@link #getNextRecord()}.
	 */
	private int readCount;

	/**
	 * Create a new instance based on the CSI input stream passed.
	 *
	 * @param iMappingDictionary mapping from container/mapping name to the {@link IMapping} instance.
	 * @param csiInputStream the CSI input stream to wrap.
	 * @throws IOException if any exceptions occur whilst initialising the input stream (thrown from
	 *             {@link ObjectInputStream#ObjectInputStream(InputStream).}
	 */
	public CsiInputStream(Map<String, IMapping> iMappingDictionary, InputStream csiInputStream) throws IOException {
		super(csiInputStream);
		this.nameToMapping = iMappingDictionary;
	}

	/**
	 * Retrieve the {@link IValueMapping} instance associated with the name passed.
	 *
	 * @param mappingName the name of the mapping to retrieve.
	 * @return either an {@link IValueMapping} or null if it could not be found.
	 */
	public IValueMapping getIValueMapping(String mappingName) {
		IMapping mapping = this.nameToMapping.get("V_" + mappingName);
		if ((mapping != null) && (mapping instanceof IValueMapping)) {
			LOG.debug("Successfully retrieved MEC for {}", mappingName);
			return (IValueMapping) mapping;
		}
		LOG.debug("Could not find MEC for {}", mappingName);
		return null;
	}

	/**
	 * Retrieve the {@link IMappingContainer} instance associated with the name passed.
	 *
	 * @param mappingName the name of the mapping to retrieve.
	 * @return either an {@link IMappingContainer} or null if it could not be found.
	 */
	public IMappingContainer getMappingContainer(String mappingName) {
		IMapping mapping = this.nameToMapping.get("C_" + mappingName);
		if ((mapping != null) && (mapping instanceof IMappingContainer)) {
			LOG.debug("Successfully retrieved CEC for {}", mappingName);
			return (IMappingContainer) mapping;
		}
		LOG.debug("Could not find CEC for {}", mappingName);
		return null;
	}

	/**
	 * Get the next {@link ContainerExtractionContext} from the stream this object is wrapping.
	 *
	 * @return the next extraction context found in the CSI input file, or null if there are no more (end of file).
	 * @throws OutputManagerException if an unexpected object was found in the stream.
	 */
	public IExtractionResultsContainer getNextRecord() throws OutputManagerException {
		Object ctxObject;
		try {
			if (LOG.isInfoEnabled()) {
				LOG.info("Reading next extraction context {} from CSI", this.readCount);
			}
			ctxObject = readObject();
			this.readCount++;
		} catch (ClassNotFoundException cnfe) {
			throw new OutputManagerException(cnfe, "Unexpected object in CSI, only expecting IExtractionResultsContainer instances.");
		} catch (EOFException eofe) {
			return null;
		} catch (IOException e) {
			throw new OutputManagerException(e, "Error reading CEC at index %d", this.readCount);
		}
		if (ctxObject instanceof IExtractionResultsContainer) {
			IExtractionResultsContainer container;
			LOG.info("Read context {} successfully from CSI", this.readCount);
			container = (IExtractionResultsContainer) ctxObject;
			XmlDataExtractor.logResults(container, 0, 0);
			return container;
		} else {
			throw new OutputManagerException("Unexpected object in CSI %s, only expecting IExtractionResultsContainer instances.",
							ctxObject.getClass().getName());
		}
	}

}
