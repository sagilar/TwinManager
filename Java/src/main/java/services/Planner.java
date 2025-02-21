package services;

import management.TwinManager;
import model.Clock;
import model.Twin;
import model.TwinSchema;
import model.TwinSystem;
import model.composition.Attribute;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.lang.Thread;
import java.text.NumberFormat;
import java.lang.Number;

import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvException;

public class Planner {
    private CSVReader reader;
    private String[] columnNames;
	private List<String> columnList;
	private List<String[]> myEntries;
	private Clock clock;
    private TwinManager internalManager;
    private boolean valid = false;
    private List<String> intTwinNames;
    private List<String> intTwinSystemNames;
    private Thread planThread;
    private Thread planSystemThread;
    private List<Object> currentPlan;
    private List<Object> currentPlanSystem;
    private List<Object> initialPlan;
    private List<Object> initialPlanSystem;
    private int coSimStepSizeMillis = 0;
    public String name;


    public Planner() {
        this.intTwinNames = new ArrayList<String>();
        this.intTwinSystemNames = new ArrayList<String>();
    }

    public Planner(String name) {
		this.name = name;
        this.intTwinNames = new ArrayList<String>();
        this.intTwinSystemNames = new ArrayList<String>();
	}

    public boolean bindTwin(String twinName){
        this.intTwinNames.add(twinName);
        return true;
    }

    public boolean set(TwinManager twinManager, String fileName, int coSimStepSizeMillis){
        this.coSimStepSizeMillis = coSimStepSizeMillis;
        this.internalManager = twinManager;
        this.clock = new Clock();
        try {
            this.reader = new CSVReaderBuilder(new FileReader(Paths.get(fileName).toString())).build();        
			this.columnNames = this.reader.readNext();
            this.myEntries = this.reader.readAll();
            this.columnList = new ArrayList<String>(this.columnNames.length-1);            
            for (int j=0;j<this.columnNames.length-1;j++){
                this.columnList.add(j, this.columnNames[j+1]);
            }
            //this.columnList = Arrays.asList(this.columnNames);
            Object[] entryObj = new Object[this.myEntries.get(0).length-1];
            for (int j=0; j<this.myEntries.get(0).length-1;j++){
                try{
                    if(this.myEntries.get(0)[j+1].equals("false")){
                        entryObj[j] = false;
                    }else if(this.myEntries.get(0)[j+1].equals("true")){
                        entryObj[j] = true;
                    }else{
                        Number number = NumberFormat.getInstance().parse(this.myEntries.get(0)[j+1]);
                        entryObj[j] = number;
                    } 
                }catch(Exception e){
                    entryObj[j] = this.myEntries.get(0)[j+1];    
                }
            }                                        
            this.currentPlan = Arrays.asList(entryObj); // seting to first value by default
            this.initialPlan = this.currentPlan;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
            return false;
		} catch (CsvException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
            return false;
		}       

        this.valid = true;
        return true;
    }

    public boolean update(String fileName){
        //TO BE IMPLEMENTED
        return false;
    }

    public boolean execute(int frequency_millis){
        if (this.valid){
            this.planThread = new Thread(() -> {
                        new Timer().scheduleAtFixedRate(new TimerTask() {
                            public void run() {
                                int timeStep = 0;    
                                try{
                                    timeStep = Double.valueOf(internalManager.getAsyncAttribute("simstep").toString()).intValue();
                                }catch(Exception e1){
                                    timeStep = clock.getNow() * coSimStepSizeMillis;
                                }  
                                boolean match = false;
                                for (String[] entry : myEntries){
                                    Object[] entryObj = new Object[entry.length-1];
                                    if (Integer.parseInt(entry[0].toString()) == timeStep){
                                        match = true;
                                        for (int j=0; j<entry.length-1;j++){
                                            try{
                                                if(entry[j+1].equals("false")){
                                                    entryObj[j] = false;
                                                }else if(entry[j+1].equals("true")){
                                                    entryObj[j] = true;
                                                }else{
                                                    Number number = NumberFormat.getInstance().parse(entry[j+1]);
                                                    entryObj[j] = number;
                                                }                                                
                                            }catch(Exception e){
                                                entryObj[j] = entry[j+1];    
                                            }
                                        }                                           
                                        currentPlan = Arrays.asList(entryObj);
                                    }
                                }
                                for (int i=0; i<currentPlan.size();i++){
                                    for (String intTwinName : intTwinNames){
                                        if (match) {                                        
                                            internalManager.setAttributeValue(columnList.get(i),currentPlan.get(i),intTwinName); 
                                        } else{
                                            internalManager.setAttributeValue(columnList.get(i),initialPlan.get(i),intTwinName);
                                        }
                                    }                                    
                                }
                            }
                }, 0, frequency_millis);
            });
            this.planThread.setDaemon(true);
            this.planThread.start();
            return true;
        }else{
            return false;
        }
    }

    public boolean executeAsync(int frequency_millis, int max_time_millis){
        if (this.valid){
            this.planThread = new Thread(() -> {
                        new Timer().scheduleAtFixedRate(new TimerTask() {
                            int timeStep = 0;
                            public void run() {
                                if (internalManager.getAsyncFlag())
                                {
                                    boolean match = false;
                                        for (String[] entry : myEntries){
                                            Object[] entryObj = new Object[entry.length-1];
                                            if (Integer.parseInt(entry[0]) == timeStep){
                                                match = true;
                                                for (int j=0; j<entry.length-1;j++){
                                                    try{
                                                        if(entry[j+1].equals("false")){
                                                            entryObj[j] = false;
                                                        }else if(entry[j+1].equals("true")){
                                                            entryObj[j] = true;
                                                        }else{
                                                            Number number = NumberFormat.getInstance().parse(entry[j+1]);
                                                            entryObj[j] = number;
                                                        }                                                
                                                    }catch(Exception e){
                                                        entryObj[j] = entry[j+1];
                                                    }
                                                }                                        
                                                currentPlan = Arrays.asList(entryObj);
                                            }
                                        }
                                        for (int i=0; i<currentPlan.size();i++){
                                            for (String intTwinName : intTwinNames){
                                                if (match) {                                        
                                                    internalManager.setAttributeValue(columnList.get(i),currentPlan.get(i),intTwinName); 
                                                } else{
                                                    internalManager.setAttributeValue(columnList.get(i),initialPlan.get(i),intTwinName);
                                                }
                                            }                                    
                                        }
                                    timeStep += frequency_millis;  
                                }
                                
                                if(timeStep > max_time_millis){
                                    this.cancel();
                                    stop();
                                }
                            }
                }, 0, frequency_millis);
            });
            this.planThread.setDaemon(true);
            this.planThread.start();
            return true;
        }else{
            return false;
        }
    }

    public boolean stop(){
        // TO BE IMPLEMENTED (threaded)
        this.planThread.interrupt();
        return false;
    }

    public boolean bindSystem(String twinSystemName){
        this.intTwinSystemNames.add(twinSystemName);
        return true;
    }

    public boolean systemSet(TwinManager twinManager, String fileName, int coSimStepSizeMillis){
        this.coSimStepSizeMillis = coSimStepSizeMillis;
        this.internalManager = twinManager;
        this.clock = new Clock();
        try {
            this.reader = new CSVReaderBuilder(new FileReader(Paths.get(fileName).toString())).build();        
			this.columnNames = this.reader.readNext();
            this.myEntries = this.reader.readAll();
            this.columnList = new ArrayList<String>(this.columnNames.length-1);       
            for (int j=0;j<this.columnNames.length-1;j++){
                this.columnList.add(j, this.columnNames[j+1]);
            }
            //this.columnList = Arrays.asList(this.columnNames);
            Object[] entryObj = new Object[this.myEntries.get(0).length-1];
            for (int j=0; j<this.myEntries.get(0).length-1;j++){
                try{
                    if(this.myEntries.get(0)[j+1].equals("false")){
                        entryObj[j] = false;
                    }else if(this.myEntries.get(0)[j+1].equals("true")){
                        entryObj[j] = true;
                    }else{
                        Number number = NumberFormat.getInstance().parse(this.myEntries.get(0)[j+1]);
                        entryObj[j] = number;
                    }    
                }catch(Exception e){
                    entryObj[j] = this.myEntries.get(0)[j+1];    
                }
            }                                        
            this.currentPlanSystem = Arrays.asList(entryObj); // seting to first value by default
            this.initialPlanSystem = this.currentPlanSystem;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
            return false;
		} catch (CsvException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
            return false;
		}       

        this.valid = true;
        return true;
    }

    public boolean systemUpdate(String fileName){
        //TO BE IMPLEMENTED
        return false;
    }

    public boolean systemExecute(int frequency_millis){
        if (this.valid){           
            this.planSystemThread = new Thread(() -> {
                        new Timer().scheduleAtFixedRate(new TimerTask() {
                            public void run() {   
                                int timeStep = 0;    
                                try{
                                    timeStep = Double.valueOf(internalManager.getAsyncAttribute("simstep").toString()).intValue();
                                }catch(Exception e1){
                                    timeStep = clock.getNow() * coSimStepSizeMillis;
                                }             
                                boolean match = false;
                                for (String[] entry : myEntries){
                                    Object[] entryObj = new Object[entry.length-1];
                                    if (Integer.parseInt(entry[0]) == timeStep){
                                        match = true;
                                        for (int j=0; j<entry.length-1;j++){
                                            try{
                                                if(entry[j+1].equals("false")){
                                                    entryObj[j] = false;
                                                }else if(entry[j+1].equals("true")){
                                                    entryObj[j] = true;
                                                }else{
                                                    Number number = NumberFormat.getInstance().parse(entry[j+1]);
                                                    entryObj[j] = number;
                                                }                                                
                                            }catch(Exception e){
                                                entryObj[j] = entry[j+1];
                                            }
                                        }                                        
                                        currentPlanSystem = Arrays.asList(entryObj);
                                    }
                                }
                                for (String intTwinSystemName : intTwinSystemNames){
                                    if (match) {
                                        internalManager.setSystemAttributeValues(columnList,currentPlanSystem,intTwinSystemName); 
                                    } else{
                                        internalManager.setSystemAttributeValues(columnList,initialPlanSystem,intTwinSystemName); 
                                    }
                                }
                                                                                          
                            }
                }, 0, frequency_millis);
            });
            this.planSystemThread.setDaemon(true);
            this.planSystemThread.start();
            return true;
        }else{
            return false;
        }
    }

    public boolean systemExecuteAsync(int frequency_millis, int max_time_millis){
        if (this.valid){
            this.planSystemThread = new Thread(() -> {
                        new Timer().scheduleAtFixedRate(new TimerTask() {
                            int timeStep = 0;
                            public void run() {
                                if (internalManager.getAsyncFlag())
                                {
                                    boolean match = false;
                                        for (String[] entry : myEntries){
                                            Object[] entryObj = new Object[entry.length-1];
                                            if (Integer.parseInt(entry[0]) == timeStep){
                                                match = true;
                                                for (int j=0; j<entry.length-1;j++){
                                                    try{
                                                        if(entry[j+1].equals("false")){
                                                            entryObj[j] = false;
                                                        }else if(entry[j+1].equals("true")){
                                                            entryObj[j] = true;
                                                        }else{
                                                            Number number = NumberFormat.getInstance().parse(entry[j+1]);
                                                            entryObj[j] = number;
                                                        }                                                
                                                    }catch(Exception e){
                                                        entryObj[j] = entry[j+1];
                                                    }
                                                }                                        
                                                currentPlanSystem = Arrays.asList(entryObj);
                                            }
                                        }
                                        for (String intTwinSystemName : intTwinSystemNames){
                                            if (match) {
                                                internalManager.setSystemAttributeValues(columnList,currentPlanSystem,intTwinSystemName); 
                                            } else{
                                                internalManager.setSystemAttributeValues(columnList,initialPlanSystem,intTwinSystemName); 
                                            }
                                        }
                                    timeStep += frequency_millis;  
                                }
                                
                                if(timeStep > max_time_millis){
                                    this.cancel();
                                    systemStop();
                                }
                            }
                }, 0, frequency_millis);
            });
            this.planSystemThread.setDaemon(true);
            this.planSystemThread.start();
            return true;
        }else{
            return false;
        }
    }

    public boolean systemStop(){
        this.planSystemThread.interrupt();
        return true;
    }

    public boolean setClock(Clock clock){
        this.clock = clock;
        return true;
    }

    public boolean setClock(int clockInt){
        this.clock.setClock(clockInt);;
        return true;
    }

    public Clock getClock(){
        return this.clock;
    }
}
