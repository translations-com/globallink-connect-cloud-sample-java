package com.translations.globallink.sample;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.gs4tr.gcc.restclient.GCConfig;
import org.gs4tr.gcc.restclient.GCExchange;
import org.gs4tr.gcc.restclient.model.GCTask;
import org.gs4tr.gcc.restclient.model.Locale;
import org.gs4tr.gcc.restclient.model.TaskStatus;
import org.gs4tr.gcc.restclient.operation.Tasks.TasksResponseData;
import org.gs4tr.gcc.restclient.request.SubmissionSubmitRequest;
import org.gs4tr.gcc.restclient.request.TaskListRequest;
import org.gs4tr.gcc.restclient.request.UploadFileRequest;


public class SampleCode {
	
	//Set your connection data here
	private static String API_URL = "**domain**/api/v2";
	private static String API_USERNAME = "username";
	private static String API_PASSWORD = "password";
	private static String API_CONNECTOR_KEY = "connector-key";
	private static String API_USER_AGENT = "user-agent";
	
	public static void main(String[] args) throws Exception {

		// ** Instantiate the configuration
		GCConfig gcconfig = new GCConfig(API_URL, API_USERNAME, API_PASSWORD, API_CONNECTOR_KEY, API_USER_AGENT);

		// ** will send a sample file for translation - comment out line bellow
		// to just retrieve
		System.out.println("Login");
		GCExchange gcexchange = new GCExchange(gcconfig);
		System.out.println("Getting connector list");
		System.out.println(gcexchange.getConnectors());
		
		sendFilesForTranslation(gcconfig);

		// ** Wait for 10 seconds before retrieval (this value can be changed)
		int secs = 10;
		System.out.print("Will wait for " + secs + " seconds before retrieval...");
		while (secs >= 0) {
			System.out.print(".");
			Thread.sleep(1000); // 1 sec per iteration
			secs--;
		}

		// will attempt to retrieve any available translated files
		receiveFilesFromTranslation(gcconfig);
	}

	private static void sendFilesForTranslation(GCConfig gcconfig) throws Exception {
		try {
			GCExchange gcexchange = new GCExchange(gcconfig);
			
			// ** Set the file properties
			System.out.println("Uploading a file...");
			// Path to file
			String filePath = "resources\\source\\sample-file.xml";
			// Filename
			String fileName = "sample-file.xml";
			// The file type, this is provided config, it calls a specific parser for the submitted file
			String fileType = "XML-CDATA-Sample";
			// * Call the method to upload the file - will return
			// * a unique document identifier, a.k.a. file ID
			UploadFileRequest fileUploadRequest = new UploadFileRequest(filePath, fileName, fileType);
			String fileId = gcexchange.uploadContent(fileUploadRequest);
			System.out.println("File uploaded:\n\tFilename: " + fileName + "\n\tFile Type: " + fileType
					+ "\n\tFile ID: [" + fileId + "]");

			// ** Set your job details
			System.out.println("Submitting the file...");
			// Job name (a descriptive name for your job)
			String submissionName = "Test Java 001";
			// Due Date (when we would like the job completed):
			String dueDateString = "31/12/2025 21:00";
			Date dueDate = new SimpleDateFormat("dd/MM/yyyy hh:mm").parse(dueDateString);
			// Language Pair(s):
			String sourceLocale = "en-US";
			String[] targetLocales = { "de-DE", "fr-FR" };
			// Array of document Tickets to be submitted in this job
			String[] fileIDs = { fileId };
			// * Call the method to submit the job with the data given above

			// **** change class name to SubmissionRequest and update methods and attributes accordingly		
			
			SubmissionSubmitRequest submissionSubmitRequest = new SubmissionSubmitRequest (submissionName, dueDate, sourceLocale,
					Arrays.asList(targetLocales), Arrays.asList(fileIDs)); 
			long jobTicket = gcexchange.submitSubmission(submissionSubmitRequest);
			
			System.out.println("Job Created: \n\tJob Name: " + submissionName + "\n\tJob ticket: [" + jobTicket + "]");
		} catch (Exception e) {
			System.out.println("Problems detected: " + e);
		}
	}

	private static void receiveFilesFromTranslation(GCConfig gcconfig) throws Exception {
		GCExchange gcexchange = new GCExchange(gcconfig);
		System.out.println("\nRetrieving completed files...");

		TaskListRequest tasks = new TaskListRequest();
		 
		String[] status = {TaskStatus.Completed.toString()};
		tasks.setTaskStatuses(status) ;
		// ** Get a list of available tasks
		TasksResponseData taskListObj = gcexchange.getTasksList(tasks);
		List<GCTask> taskList = taskListObj.getTasks();
		for (GCTask task : taskList) {
			String taskName = task.getName();
			String taskStatus = task.getStatus();
			Long subId = task.getSubmissionId();
			Long taskId = task.getTaskId();
			Locale taskLocale = task.getTargetLocale();

			// ** If the task is complete, download it
			if (taskStatus.equals("Completed")) {
				System.out.println("Task [" + taskId + "] is READY: " + "\n\tStatus: " + taskStatus
						+ "\n\tTask Locale: " + taskLocale.getLocale() + "\n\tJob ID: [" + subId + "]");
				//TaskRequest taskRequest = new TaskRequest(taskId);
				System.out.println("Downloading task...");
				try {
					InputStream inputStream = gcexchange.downloadTask(taskId);
					File folder = new File("resources\\translated");
					String fileName = folder + "/" + taskLocale.getLocale() + "_" +  taskId + "_" + taskName;
					File translatedFile = new File(fileName);
					FileOutputStream outputStream = new FileOutputStream(translatedFile);
					int read = 0;
					byte[] bytes = new byte[1024];
					while ((read = inputStream.read(bytes)) != -1) {
						outputStream.write(bytes, 0, read);
					}
					outputStream.close();

					// * Do whatever processing you need
					// * On successful processing confirm the delivery of the task

					//gcexchange.taskConfirmDelivery(taskRequest);
					System.out.println("Task [" + taskId + "] was downloadded and confirmed correctly!");

				} catch (Exception e) {
					System.out.println("Could not save the file correctly: " + e);
					e.printStackTrace();
				}
			}

			// ** Print the delivered ones
			if (taskStatus.equals("Delivered")){
				System.out.println("\nTask [" + taskId + "] is DELIVERED:\n\tName: " + taskName + "\n\tStatus: "
						+ taskStatus + "\n\tTask Locale: " + taskLocale.getLocale() + "\n\tJob ID: [" + subId + "]");
			}
			
			// ** Otherwise, print all non-finished tasks
			else {
				System.out.println("\nTask [" + taskId + "] is NOT READY:\n\tName: " + taskName + "\n\tStatus: "
						+ taskStatus + "\n\tTask Locale: " + taskLocale.getLocale() + "\n\tJob ID: [" + subId + "]");
			}
		}
	}
}