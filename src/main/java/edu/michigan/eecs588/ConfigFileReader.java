package edu.michigan.eecs588;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

/**
 *	Config File Reader.
 */
public class ConfigFileReader {
	
	public static Map<String, String> getConfigValues() throws IOException {
		return getConfigValues("smack.properties");
	}
	
	public static Map<String, String> getConfigValues(String filename) throws IOException {
		Properties properties = new Properties();
		
		InputStream inputStream = new FileInputStream(filename);
		properties.load(inputStream);
		Map<String, String> retval = new HashMap<String, String>();
		for (Entry<Object, Object> entry : properties.entrySet()) {
			retval.put(String.valueOf(entry.getKey()), String.valueOf(entry.getValue()));
		}
		return retval;
	}
}
