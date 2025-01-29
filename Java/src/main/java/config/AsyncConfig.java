package config;


import java.io.File;

import com.typesafe.config.*;

public class AsyncConfig {
	public Config conf;
	
	public AsyncConfig(String filename) {
		File file = new File(filename);   
		conf = ConfigFactory.parseFile(file);
	}
	
}