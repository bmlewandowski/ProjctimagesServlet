package com.appspot.projctimages;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.UUID;

import javax.servlet.http.*;

import org.apache.commons.codec.binary.Base64;

import com.google.appengine.api.blobstore.BlobKey;
import com.google.appengine.api.blobstore.BlobstoreServiceFactory;
import com.google.appengine.api.images.ImagesServiceFactory;
import com.google.appengine.api.images.ServingUrlOptions;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;


@SuppressWarnings("serial")
public class ProjctimagesServlet extends HttpServlet {
	
	public void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		
		//Adjust the header to enable CORS	
		resp.addHeader("Access-Control-Allow-Origin", "*");

		// Return the String
		resp.setContentType("text/plain");
		resp.getWriter().println("Prod Get");

	}

	private static String getUrlContents(String theUrl) {
		
		StringBuilder content = new StringBuilder();

		try {
			URL url = new URL(theUrl);
			URLConnection urlConnection = url.openConnection();

			BufferedReader bufferedReader = new BufferedReader(
					new InputStreamReader(urlConnection.getInputStream()));

			String line;

			while ((line = bufferedReader.readLine()) != null) {
				content.append(line + "\n");
			}
			bufferedReader.close();
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return content.toString();
	}
	
	
	public void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		
		//Adjust the header to enable CORS	
		resp.addHeader("Access-Control-Allow-Origin", "https://www.projcts.com");
		
		//Grab Command and Token from Request
		String userid = req.getParameter("userid");
		String usertoken = req.getParameter("usertoken");
		String command = req.getParameter("command");
		
		//Declare Bucket
		
		//Production Bucket
        String bucket = "projctimagesbucket"; 
        
        //Development Bucket
        //String bucket = "devprojctimagesbucket";       
        					
        //Check Command
		if (command.equals("add")) //Add Image
		{
        //Grab Image Data
        final String imagestring = req.getParameter("imagestring");
        byte[] imagedata = Base64.decodeBase64(imagestring);

        //New Picture Name  
        String imageid = UUID.randomUUID().toString();
        String object = imageid + ".jpg";	
        
        //Upload stream to the bucket.
        try {
			StorageSample.uploadStream(
					object, "image/jpeg",
			    new ByteArrayInputStream(imagedata),
			    bucket);
			
	        //Generate Blob Key and use it to get Serving URL
	        String gs_blob_key = "/gs/" + bucket + "/" + object;
	        BlobKey blob_key = BlobstoreServiceFactory.getBlobstoreService().createGsBlobKey(gs_blob_key);
	        ServingUrlOptions serving_options = ServingUrlOptions.Builder.withBlobKey(blob_key).secureUrl(true);
	        String servingurl = ImagesServiceFactory.getImagesService().getServingUrl(serving_options);              
	       				       
	        //Return Success
			resp.setContentType("text/plain");
			resp.getWriter().println("success|" + servingurl + "|" + imageid); 
			
		} catch (GeneralSecurityException e) {						
			e.printStackTrace();
			
	        //Return Write Error
			resp.setContentType("text/plain");
			resp.getWriter().println("bucketerror|err|err");    
		}               
		       
	} 
		
		else if (command.equals("delete")) //Delete Image
	{
			
			String imagekey = req.getParameter("imagekey");
			
	        //Check for Authorized User
			
			//Production
	        String fburi = "https://projcts.firebaseio.com/images/" + userid + "/" + imagekey + "/owner.json?auth=" + usertoken;
	        
			//Dev
			//String fburi = "https://projctsdev.firebaseio.com/images/" + userid + "/" + imagekey + "/owner.json?auth=" + usertoken;

			String fbresults = getUrlContents(fburi);
			String fbauth = fbresults.replace("\"", "").replace("\n", "");
							
			//Read Results 
			if (fbauth.equals(userid))
			{				
				//Get Requested Object to Delete
				String object = req.getParameter("imagekey");
				
				
			    //Delete Object from Bucket
				try {
					StorageSample.deleteObject(object + ".jpg", bucket);
					
					//Return Success
					resp.setContentType("text/plain");
					resp.getWriter().println("success");						
					
				} catch (GeneralSecurityException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					
					//Return Delete Error
					resp.setContentType("text/plain");
					resp.getWriter().println("bucketerror");	
				}
			}
			
			else
			{
				//Return Authorization Error
				resp.setContentType("text/plain");
				resp.getWriter().println("autherror");					
			}
					
	}
		
		else if (command.equals("edit")) //Edit Image
	{
			//Write new Image and Delete Old Image
			String imagekey = req.getParameter("imagekey");
			
	        //Check for Authorized User
			
			//Production
	        String fburi = "https://projcts.firebaseio.com/images/" + userid + "/" + imagekey + "/owner.json?auth=" + usertoken;
	        
			//Dev
			//String fburi = "https://projctsdev.firebaseio.com/images/" + userid + "/" + imagekey + "/owner.json?auth=" + usertoken;

			String fbresults = getUrlContents(fburi);
			String fbauth = fbresults.replace("\"", "").replace("\n", "");
							
			//Read Results 
			if (fbauth.equals(userid))
			{
				
		        //Grab Image Data
		        final String imagestring = req.getParameter("imagestring");
		        byte[] imagedata = Base64.decodeBase64(imagestring);

		        //New Picture Name
		        String imageid = UUID.randomUUID().toString();
		        String object = imageid + ".jpg";	
		        
		        //Upload stream to the bucket.
		        try {
					StorageSample.uploadStream(
							object, "image/jpeg",
					    new ByteArrayInputStream(imagedata),
					    bucket);
					
			        //Generate Blob Key and use it to get Serving URL
			        String gs_blob_key = "/gs/" + bucket + "/" + object;
			        BlobKey blob_key = BlobstoreServiceFactory.getBlobstoreService().createGsBlobKey(gs_blob_key);
			        ServingUrlOptions serving_options = ServingUrlOptions.Builder.withBlobKey(blob_key).secureUrl(true);
			        String servingurl = ImagesServiceFactory.getImagesService().getServingUrl(serving_options);              
			       				        
			        //START DELETE
			        							
				    //Delete Object from Bucket
					try {
						StorageSample.deleteObject(imagekey + ".jpg", bucket);
						
				        //Return Success
						resp.setContentType("text/plain");
						resp.getWriter().println("success|" + servingurl + "|" + imageid); 					
						
					} catch (GeneralSecurityException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						
						//Return Delete Error
						resp.setContentType("text/plain");
						resp.getWriter().println("deletebucketerror|err|err");	
					}		  	        			        
					
				} catch (GeneralSecurityException e) {						
					e.printStackTrace();
					
			        //Return Write Error
					resp.setContentType("text/plain");
					resp.getWriter().println("writebucketerror|err|err");    
				}    	        

			}
			
			else
			{
				//Return Authorization Error
				resp.setContentType("text/plain");
				resp.getWriter().println("autherror|err|err");					
			}		
						
	}
		
		else //Unknown command
	{
		resp.setContentType("text/plain");
		resp.getWriter().println("NoCommand|err|err");			
	}
		
		          		
	}	
}


