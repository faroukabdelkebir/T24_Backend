//package com.t24extractor.service;
//
//import com.t24extractor.listeners.ApplicationStartupListener;
//import com.t24extractor.model.TableMessage;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.batch.core.Job;
//import org.springframework.batch.core.JobParametersBuilder;
//import org.springframework.batch.integration.launch.JobLaunchRequest;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.integration.annotation.ServiceActivator;
//import org.springframework.stereotype.Service;
//
//import java.util.UUID;
//
////@MessageEndpoint
//@Service
//public class TableService
//{
//	private static final Logger LOG = LoggerFactory.getLogger(ApplicationStartupListener.class) ;
//
//    private Job job;
//
//    @Autowired
//    public void setJob(Job job)
//    {
//        this.job = job;
//    }
//
//   // @JmsListener(destination = "${queue.destination}")
//    @ServiceActivator(inputChannel = "inputChannel", outputChannel = "outputChannel")
//    public JobLaunchRequest process(TableMessage xmlMessage)
//    {
//    	long id = xmlMessage.getId() ;
//    	String TableName = xmlMessage.getTableName() ;
//    	String T24SelectionName = xmlMessage.getT24SelectionName() ;
//    	String Chargement = xmlMessage.getChargement() ;
//    	String Colonne_Ref = xmlMessage.getColonne_Ref() ;
//    	String Last_Date = xmlMessage.getLast_Date() ;
//    	String Indexes = xmlMessage.getIndexes() ;
//    	String Type = xmlMessage.getType() ;
//    	String Destination = xmlMessage.getDestination() ;
//    	String Date_Format = xmlMessage.getDate_Format() ;
//    	String Future_Date = xmlMessage.getFuture_Date() ;
//    	String Run_ID = xmlMessage.getRun_ID() ;
//
//    	LOG.trace("XML Message : id = [" + id + "]") ;
//    	LOG.trace("XML Message : TableName = [" + TableName + "]") ;
//    	LOG.trace("XML Message : T24SelectionName = [" + T24SelectionName + "]") ;
//    	LOG.trace("XML Message : Chargement = [" + Chargement + "]") ;
//    	LOG.trace("XML Message : Colonne_Ref = [" + Colonne_Ref + "]") ;
//    	LOG.trace("XML Message : Last_Date = [" + Last_Date + "]") ;
//    	LOG.trace("XML Message : Indexes = [" + Indexes + "]") ;
//    	LOG.trace("XML Message : Type = [" + Type + "]") ;
//    	LOG.trace("XML Message : Destination = [" + Destination + "]") ;
//    	LOG.trace("XML Message : Date_Format = [" + Date_Format + "]") ;
//    	LOG.trace("XML Message : Future_Date = [" + Future_Date + "]") ;
//    	LOG.trace("XML Message : Run_ID = [" + Run_ID + "]") ;
//
//        JobParametersBuilder builder = new JobParametersBuilder() ;
//
//        builder.addString("Table_Name", TableName) ;
//        builder.addString("T24_Selection_Name", T24SelectionName) ;
//        builder.addString("Chargement", Chargement) ;
//        builder.addString("Colonne_Ref", Colonne_Ref) ;
//        builder.addString("Last_Date", Last_Date) ;
//        builder.addString("Indexes", Indexes) ;
//        builder.addString("Type", Type) ;
//        builder.addString("Destination", Destination) ;
//        builder.addString("Date_Format", Date_Format);
//        builder.addString("Future_Date", Future_Date) ;
//
//        if(Run_ID == null)
//        {
//        	builder.addString("run.id", UUID.randomUUID().toString()) ;
//        }
//        else
//        {
//        	builder.addString("run.id", Run_ID) ;
//        }
//
//        return new JobLaunchRequest(job, builder.toJobParameters());
//    }
//}
