package endpoints;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Iterator;
import java.util.concurrent.TimeoutException;
import org.json.JSONObject;

import com.fasterxml.jackson.databind.ObjectWriter;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvException;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigRenderOptions;

import config.TwinSystemConfiguration;
import model.Clock;
import model.composition.Attribute;
import model.composition.Operation;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class MaestroEndpoint implements AggregateEndpoint {

	private String twinSystemName;
	private double stepSize = 0.0;
	private TwinSystemConfiguration systemConfig;
	private Config coeConfig;
	private String outputPath;
	String coeFilename = "coe.json";
	String simulationFilename;
	// Data processing
	private CSVReader reader;
	private String[] columnNames;
	List<String> columnList;
	private List<String[]> myEntries;
	private Clock clock = new Clock();
	private String lastCommand = "simulate";
	
	//RabbitMQ
	boolean rabbitMQEnabled = false;
	boolean flagSend = false;
	String ip;
	int port;
	String username;
	String password;
	String exchange;
	String type;
	String vhost;
	String routingKeyFromCosim;
	String routingKeyToCosim;
	String rmqMessageFromCosim = "";
	String rmqMessageToCosim = "";
	ConnectionFactory factory;
	Connection conn;
	Channel channelFromCosim;
	Channel channelToCosim;
	DeliverCallback deliverCallbackFromCosim;
	DeliverCallback deliverCallbackToCosim;
	private Map<String,Attribute> registeredAttributes = new HashMap<String,Attribute>();
	private Map<String,Operation> registeredOperations = new HashMap<String,Operation>();;
	
	public String getTwinSystemName() {
		return twinSystemName;
	}
	
	public MaestroEndpoint(String twinSystemName, TwinSystemConfiguration config,String coeFilename, String outputPath)
	{
		this.twinSystemName = twinSystemName;
		this.coeFilename = coeFilename;
		File file = new File(coeFilename);   
		this.coeConfig = ConfigFactory.parseFile(file);
		this.outputPath = outputPath;
		this.systemConfig = config;
		this.simulationFilename = this.systemConfig.conf.origin().filename();
		this.stepSize = this.systemConfig.conf.getDouble("algorithm.size");
		this.registeredAttributes = new HashMap<String,Attribute>();		
		this.registeredOperations = new HashMap<String,Operation>();
		
		/***** If RabbitMQFMU is enabled *****/
		if (this.systemConfig.conf.hasPath("rabbitmq")) {
			this.rabbitMQEnabled = true;
			this.ip = this.systemConfig.conf.getString("rabbitmq.ip");
			this.port = this.systemConfig.conf.getInt("rabbitmq.port");
			this.username = this.systemConfig.conf.getString("rabbitmq.username");
			this.password = this.systemConfig.conf.getString("rabbitmq.password");
			this.exchange = this.systemConfig.conf.getString("rabbitmq.exchange");
			this.type = this.systemConfig.conf.getString("rabbitmq.type");
			this.vhost = this.systemConfig.conf.getString("rabbitmq.vhost");
			this.routingKeyFromCosim = this.systemConfig.conf.getString("rabbitmq.routing_key_from_cosim");
			this.routingKeyToCosim = this.systemConfig.conf.getString("rabbitmq.routing_key_to_cosim");
			
			
			this.deliverCallbackFromCosim = (consumerTagFrom, deliveryFrom) -> {
				this.rmqMessageFromCosim = new String(deliveryFrom.getBody(), "UTF-8");
				String keyStart = "waiting for input data for simulation";
				if (this.rmqMessageFromCosim.contains(keyStart)) {
					this.flagSend = true;
					/* Execute the publishing after this message has been received */
				}
				try{
					JSONObject jsonObject = new JSONObject(this.rmqMessageFromCosim);
					Iterator<String> keys = jsonObject.keys();
					while(keys.hasNext()) {
						String key = keys.next();
						Attribute tmpAttr = new Attribute();
						tmpAttr.setName(key);
						tmpAttr.setValue(jsonObject.get(key));
						registeredAttributes.put(key, tmpAttr);
						String alias = mapAlias(key);
						Attribute tmpAttrAlias = new Attribute();
						tmpAttrAlias.setName(alias);
						tmpAttrAlias.setValue(jsonObject.get(key));
						registeredAttributes.put(alias, tmpAttrAlias);
					}
				}catch (Exception e){
				}
			};

			this.deliverCallbackToCosim = (consumerTagTo, deliveryTo) -> {
				this.rmqMessageToCosim = new String(deliveryTo.getBody(), "UTF-8");
			};
			
			this.factory = new ConnectionFactory();
			if (this.password.equals("")){
				
			}else {
				this.factory.setUsername(this.username);
				this.factory.setPassword(this.password);
			}
			//this.factory.setVirtualHost(this.vhost);
			this.factory.setHost(this.ip);
			this.factory.setPort(this.port);

			try {
				this.conn = this.factory.newConnection();
				this.channelFromCosim = this.conn.createChannel();
				this.channelFromCosim.exchangeDeclare(this.exchange,"direct");
				String queueNameFromCosim = this.channelFromCosim.queueDeclare().getQueue();
				this.channelFromCosim.queueBind(queueNameFromCosim, this.exchange, this.routingKeyFromCosim);				
				this.channelFromCosim.basicConsume(queueNameFromCosim, this.deliverCallbackFromCosim, consumerTagFrom -> { });
				this.channelToCosim = this.conn.createChannel();
				this.channelToCosim.exchangeDeclare(this.exchange,"direct");
				String queueNameToCosim = this.channelToCosim.queueDeclare().getQueue();
				this.channelToCosim.queueBind(queueNameToCosim, this.exchange, this.routingKeyToCosim);
				this.channelToCosim.basicConsume(queueNameToCosim, this.deliverCallbackToCosim, consumerTagTo -> { });
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (TimeoutException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}			
		}		
	}

	
	public void registerOperation(String name, Operation op) {
		this.registeredOperations.put(name,op);
	}

	
	public void registerAttribute(String name, Attribute attr) {
		this.registeredAttributes.put(name,attr);
	}

	
	public List<Attribute> getAttributeValues(List<String> variables) {
		// from the csv output file
		List<Attribute> attrs = new ArrayList<Attribute>();
		for (String var : variables) {
			Attribute attr = this.getAttributeValue(var);
			attrs.add(attr);
		}
		return attrs;
	}

	
	public Attribute getAttributeValue(String variable) {
		if (this.rabbitMQEnabled) {
			return this.registeredAttributes.get(variable);
		} else{
			// from the csv output file
			String[] entry = myEntries.get(1);
			if (this.lastCommand.equals("simulate")) {
				entry = myEntries.get(this.clock.getNow() + 1);
			}else if(this.lastCommand.equals("doStep")) {
				entry = myEntries.get(2);
			}
			List<String> entryList = Arrays.asList(entry);
			Object value = null;
			Attribute attr = new Attribute();
			for (String column : this.columnList) {
				if(variable.equals(column)) {
					int i = this.columnList.indexOf(column);
					value =  (Object)(entryList.get(i));
				}
			}
			attr.setName(variable);
			attr.setValue(value);
			return attr;
		}
		
	}
	
	public Attribute getAttributeValue(String variable, String twinName) {
		if (this.rabbitMQEnabled) {
			return this.registeredAttributes.get(variable); // Not returning for a specific twinName
		} else{
			// from the csv output file
			String twinRaw = mapAlias(twinName);
			String varRaw = mapAlias(variable);
			String composedRaw = twinRaw + "." + varRaw;
			String[] entry = myEntries.get(1);
			if (this.lastCommand.equals("simulate")) {
				entry = myEntries.get(this.clock.getNow() + 1);
			}else if(this.lastCommand.equals("doStep")) {
				entry = myEntries.get(2);
			}
			List<String> entryList = Arrays.asList(entry);
			Object value = null;
			Attribute attr = new Attribute();
			for (String column : this.columnList) {
				if(composedRaw.equals(column)) {
					int i = this.columnList.indexOf(column);
					value =  (Object)(entryList.get(i));
				}
			}
			attr.setName(variable);
			attr.setValue(value);
			return attr;
		}
	}
	
	public Attribute getAttributeValue(String variable, Clock clock) {
		// from the csv output file
		String[] entry = myEntries.get(clock.getNow());
		List<String> entryList = Arrays.asList(entry);
		Object value = null;
		Attribute attr = new Attribute();
	    for (String column : this.columnList) {
	    	if(variable.equals(column)) {
	    		int i = this.columnList.indexOf(column);
	    		value =  (Object)(entryList.get(i));
	    	}
	    }
	    attr.setName(variable);
	    attr.setValue(value);
		return attr;
	}
	
	public Attribute getAttributeValue(String variable, String twinName, Clock clock) {
		// from the csv output file
		String twinRaw = mapAlias(twinName);
		String varRaw = mapAlias(variable);
		String composedRaw = twinRaw + "." + varRaw;
		String[] entry = myEntries.get(clock.getNow());
		List<String> entryList = Arrays.asList(entry);
		Object value = null;
		Attribute attr = new Attribute();
	    for (String column : this.columnList) {
	    	if(composedRaw.equals(column)) {
	    		int i = this.columnList.indexOf(column);
	    		value =  (Object)(entryList.get(i));
	    	}
	    }
	    attr.setName(variable);
	    attr.setValue(value);
		return attr;
	}
	
	
	public boolean setAttributeValues(List<String> variables, List<Attribute> attrs) {
		if(this.rabbitMQEnabled) {
			Map<String,Object> body = new HashMap<String,Object>();
			String ts = ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
			body.put("time", ts);
			for (int i=0; i<variables.size();i++) {				
				body.put(variables.get(i), attrs.get(i).getValue());				
			}
			JSONObject bodyJSON = new JSONObject(body);
			String bodyMessage = bodyJSON.toString();
			this.rawSend(bodyMessage);	
		}else {
			// On the multimodel.json file
			for (String var : variables) {
				int index = variables.indexOf(var);
				this.setAttributeValue(var, attrs.get(index));
			}
		}
		return true;		
	}

	
	public boolean setAttributeValue(String variable, Attribute attr) {
		if(this.rabbitMQEnabled) {
			Map<String,Object> body = new HashMap<String,Object>();
			String ts = ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
			body.put("time", ts);
			body.put(variable, attr.getValue());
			JSONObject bodyJSON = new JSONObject(body);
			String bodyMessage = bodyJSON.toString();
			this.rawSend(bodyMessage);
		}else {
			// On the multimodel.json file
			String fileString = this.systemConfig.conf.root().render(ConfigRenderOptions.concise().setFormatted(true).setJson(true));
			JSONObject completeJsonObject = new JSONObject(fileString);
			if (variable.equals("step_size") || variable.equals("stepSize")) {
				JSONObject innerjsonObject = new JSONObject(fileString).getJSONObject("algorithm");
				innerjsonObject.put("size",attr.getValue());
				completeJsonObject.put("algorithm", innerjsonObject);
				try (FileWriter file = new FileWriter(this.simulationFilename)) 
		        {
		            file.write(completeJsonObject.toString(4));
		            file.close();
		        } catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}else {
				JSONObject innerjsonObject = new JSONObject(fileString).getJSONObject("parameters");
				innerjsonObject.put(variable,attr.getValue());
				completeJsonObject.put("parameters", innerjsonObject);
				try (FileWriter file = new FileWriter(this.simulationFilename)) 
		        {
		            file.write(completeJsonObject.toString(4));
		            file.close();
		            
		        } catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			this.systemConfig = new TwinSystemConfiguration(this.simulationFilename);
		}
		return true;
	}
	
	public boolean setAttributeValue(String variable, Attribute attr, String twinName) {
		if(this.rabbitMQEnabled) {
			Map<String,Object> body = new HashMap<String,Object>();
			String ts = ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
			body.put("time", ts);
			body.put(variable, attr.getValue());
			JSONObject bodyJSON = new JSONObject(body);
			String bodyMessage = bodyJSON.toString();
			this.rawSend(bodyMessage);
			
		}else {
			// On the multimodel.json file
			String twinRaw = mapAlias(twinName);
			String varRaw = mapAlias(variable);
			String composedRaw = twinRaw + "." + varRaw;
			String fileString = this.systemConfig.conf.root().render(ConfigRenderOptions.concise().setFormatted(true).setJson(true));
			JSONObject completeJsonObject = new JSONObject(fileString);
			if (variable.equals("step_size") || variable.equals("stepSize")) {
				JSONObject innerjsonObject = new JSONObject(fileString).getJSONObject("algorithm");
				innerjsonObject.put("size",attr.getValue());
				completeJsonObject.put("algorithm", innerjsonObject);
				try (FileWriter file = new FileWriter(this.simulationFilename)) 
		        {
		            file.write(completeJsonObject.toString(4));
		            file.close();
		        } catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}else {
				JSONObject innerjsonObject = new JSONObject(fileString).getJSONObject("parameters");
				innerjsonObject.put(composedRaw,attr.getValue());
				completeJsonObject.put("parameters", innerjsonObject);
				try (FileWriter file = new FileWriter(this.simulationFilename)) 
		        {
		            file.write(completeJsonObject.toString(4));
		            file.close();
		            
		        } catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			this.systemConfig = new TwinSystemConfiguration(this.simulationFilename);
		}
		return true;
	}
	
	public void simulate() {
		//with the arguments defined in coe.json and multimodel.json
		org.intocps.maestro.Main.argumentHandler(
				new String[]{"import","sg1",this.coeFilename,this.simulationFilename, "-output",this.outputPath});
		org.intocps.maestro.Main.argumentHandler(
                new String[]{"interpret", "--verbose", Paths.get(outputPath,"spec.mabl").toString(),"-output",this.outputPath});
		try {
			this.reader = new CSVReaderBuilder(new FileReader(Paths.get(outputPath,"outputs.csv").toString())).build();
			this.myEntries = this.reader.readAll();
			this.columnNames = this.myEntries.get(0);
			this.columnList = Arrays.asList(this.columnNames);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (CsvException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void simulate(double endTime) {
		//fixedEndtime
		String fileString = this.coeConfig.root().render(ConfigRenderOptions.concise().setFormatted(true).setJson(true));
		JSONObject jsonObject = new JSONObject(fileString);
		jsonObject.put("startTime",0.0);
		jsonObject.put("endTime",endTime);
		try (FileWriter file = new FileWriter(this.coeFilename)) 
        {
			//ObjectWriter writer = mapper.defaultPrettyPrintingWriter();
            file.write(jsonObject.toString(4));
            file.close();
            File fileRead = new File(this.coeFilename);   
            this.coeConfig = ConfigFactory.parseFile(fileRead);
            
        } catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		this.simulate();
	}
	
	public void simulate(double startTime,double endTime) {
		//fixed endTime and fixed startTime
		String fileString = this.coeConfig.root().render(ConfigRenderOptions.concise().setFormatted(true).setJson(true));
		JSONObject jsonObject = new JSONObject(fileString);
		jsonObject.put("startTime",startTime);
		jsonObject.put("endTime",endTime);
		try (FileWriter file = new FileWriter(this.coeFilename)) 
        {
            file.write(jsonObject.toString(4));
            file.close();
            File fileRead = new File(this.coeFilename);
            this.coeConfig = ConfigFactory.parseFile(fileRead);
            
        } catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		this.simulate();
	}
	
	private void doStep() {
		this.simulate(0.0,this.stepSize*2);
	}

	public boolean executeOperation(String opName, List<?> arguments) {
		if (opName.equals("simulate")) {
			this.lastCommand = "simulate";
			if(arguments == null) {
				
			}else {
				this.stepSize = (double) arguments.get(0);
				if (arguments.size() > 1) {
					
					Map<String,Object> args = (Map<String, Object>) arguments.get(1);
					for (Map.Entry<String, Object> entry : args.entrySet()) {
						Attribute tmpAttr = new Attribute();
						tmpAttr.setName(entry.getKey());
						tmpAttr.setValue(entry.getValue());
					    this.setAttributeValue(entry.getKey(), tmpAttr);
					}
				}
			}
			this.simulate();
		}else if(opName.equals("doStep")) {
			this.lastCommand = "doStep";
			if(arguments == null) {
				
			}else {
				this.stepSize = (double) arguments.get(0);
				if (arguments.size() > 1) {
					Map<String,Object> args = (Map<String, Object>) arguments.get(1);
					for (Map.Entry<String, Object> entry : args.entrySet()) {
						Attribute tmpAttr = new Attribute();
						tmpAttr.setName(entry.getKey());
						tmpAttr.setValue(entry.getValue());
						this.setAttributeValue(entry.getKey(), tmpAttr);
					}
				}
			}
			this.doStep();
		}
		return true;
	}
	
	public void setClock(Clock value) {
		this.clock = value;
	}
	
	
	public Clock getClock() {
		return this.clock;
	}
	
	private String mapAlias(String in) {
		String out = "";
		try {
			out = this.systemConfig.conf.getString("aliases." + in);
		}catch(Exception e) {
			out = in;
		}
		
		return out;
	}
	
	@Override
	public boolean setAttributeValue(String attrName, Attribute attr, Clock clock) {
		this.setClock(clock);
		return this.setAttributeValue(attrName, attr);
	}

	@Override
	public boolean executeOperation(String opName, List<?> arguments, Clock clock) {
		this.setClock(clock);
		return this.executeOperation(opName, arguments);
	}

	@Override
	public boolean setAttributeValue(String attrName, Attribute attr, String twinName, Clock clock) {
		this.setClock(clock);
		return this.setAttributeValue(attrName, attr, twinName);
	}
	
	/***** RabbitMQFMU support *****/
	public void rawSend(String message) {
		//System.out.println("raw send msg: " + message + " - flag for sending: " + String.valueOf(this.flagSend));
		if (this.flagSend == true) {
			try {
				this.channelToCosim.basicPublish(this.exchange, this.routingKeyToCosim, null, message.getBytes());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}		
	}

	@Override
	public boolean executeOperation(String opName, List<?> arguments, String twinName) {
		// Not Applicable
		return false;
	}

	@Override
	public boolean executeOperation(String opName, List<?> arguments, String twinName, Clock clock) {
		// Not Applicable
		return false;
	}	

}
