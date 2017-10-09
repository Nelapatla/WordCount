package com.word.count;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.io.FilenameUtils;
import org.bson.BsonDocument;
import org.bson.BsonInt32;
import org.bson.BsonValue;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.mongodb.BasicDBObject;
import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;

public class MongoSampleClient extends DefaultHandler {

	private String wordKey = "word";
	private MongoClient mongoClient = null;
	private MongoDatabase db = null;
	private MongoCollection<Document> table = null;
	private String file = null;
	private String dbFileName=null;
	
	public static void main(String[] args) {
		MongoSampleClient handler = new MongoSampleClient();
		handler.prepareExecute(handler, args);
	}

	@Override
	public void characters(char[] buffer, int start, int length) {
		String temp = new String(buffer, start, length);
		frequencyCount(temp);
	}

	private void frequencyCount(String temp) {
		//Remove all special character 
		Pattern p = Pattern.compile("[^a-zA-Z0-9]");
		String[] words = p.matcher(temp).replaceAll(" ").split(" ");
		for (String s : words) {
			if (!s.equals(null))
				calldb(s.trim());
		}
	}

	public void calldb(String s) {
		boolean flag=true;
		int freq = 0;

		try {
			table = db.getCollection(dbFileName);
			BasicDBObject fields = new BasicDBObject();
			List<BasicDBObject> searchArguments = new ArrayList<BasicDBObject>();
			searchArguments.add(new BasicDBObject(s.toLowerCase(), new BasicDBObject("$eq", 1)));
			searchArguments.add(new BasicDBObject(s.toLowerCase(), new BasicDBObject("$gt", 1)));

			fields.put("$or", searchArguments);

			FindIterable<Document> it = table.find(fields);
			MongoCursor<Document> iterator = it.iterator();
			while (iterator.hasNext()) {
				freq = iterator.next().getInteger(s.toLowerCase());
				if (freq > 0) {
					flag = false;
					Bson filter = Filters.exists(s.toLowerCase(), true);
					Document doc1 = new Document(wordKey, freq + 1).append(s.toLowerCase(), freq + 1);
					table.findOneAndReplace(filter, doc1);

				}

			}
			if (!s.equals("") && flag) {
				Document doc = new Document(wordKey, 1);
				doc.append(s.toLowerCase(), 1);
				table.insertOne(doc);

			}
		} catch (Exception e) {
			System.out.println(e);
		}
	}
	//Prepare for executing application
	public void prepareExecute(MongoSampleClient handler, String args[]) {
		String address[] = null;
		String ipAddress = null;
		@SuppressWarnings("resource")
		Scanner scanner=new Scanner(System.in);
		int port = 0;
		if (args.length < 2) {
			System.err.println("please provide valid input :");
			System.out.println();
			System.out.println("You want to continue press: Y else press N");
			String choice=scanner.nextLine();
			if(choice.equalsIgnoreCase("y")){
			System.out.println("Enter file Name with complite path :");
			file=scanner.nextLine();
			System.out.println("Enter  server ip address:");
			ipAddress=scanner.nextLine();
			System.out.println("Enter  server port number:");
			port=scanner.nextInt();
			}else{
				System.exit(0);
			}
		} else {
			file = args[0];
			address = args[1].split(":");
			if (address.length >= 2) {
				ipAddress = address[0];
				port = Integer.parseInt(address[1]);
			}
		}

		String fileExtension = FilenameUtils.getExtension(file);
		dbFileName = FilenameUtils.getBaseName(file);
		Path filePath = Paths.get(file);
		if (Files.exists(filePath)) {
			
			System.out.println("Select path    : "+filePath);
			System.out.println("Select file    : "+FilenameUtils.getName(file));
			System.out.println("Selected server: "+ipAddress+":"+port);
			System.out.println("File in table  : "+dbFileName);
			
			//call for Creating  connection
			createDBConnection(ipAddress, port);
			if (!fileExtension.equals("xml")) { //for doc, text file
				try (Stream<String> lines = Files.lines(filePath, Charset.defaultCharset())) {
					lines.forEachOrdered(line -> frequencyCount(line));
				} catch (IOException e) {
					e.printStackTrace();
				}
			} else {   //for XML file 
				SAXParserFactory spfac = SAXParserFactory.newInstance();
				SAXParser sp;
				try {
					sp = spfac.newSAXParser();
					sp.parse(file, handler);
				} catch (ParserConfigurationException | SAXException | IOException e) {
					e.printStackTrace();
				}

			}
			 handler.minFrequency();
			 handler.maxFrequency();
		} else {
			System.err.println("File does not exist :"+filePath);
		}
	}
   //find least number of frequency in file
	public void minFrequency() {
		BsonValue b = new BsonInt32(+1);
		Bson sort = new BsonDocument(wordKey, b);
		Document min = table.find().sort(sort).first();
		BsonValue c = new BsonInt32(getWord(min).get(wordKey));
		Bson minFq = new BsonDocument(wordKey, c);
		FindIterable<Document> find = table.find(minFq);
		getAllWord("least freaquency",find);
	}
	//find Most number of frequency in file
	public void maxFrequency() {
		//Search Argument for  word have max frequency
		BsonValue b = new BsonInt32(-1);
		Bson sort = new BsonDocument(wordKey, b);
		Document max = table.find().sort(sort).first();
		
		//Search Argument for all word have same frequency 
		BsonValue c = new BsonInt32(getWord(max).get(wordKey));
		Bson maxf = new BsonDocument(wordKey, c);
		FindIterable<Document> find = table.find(maxf);
		getAllWord("Max freaquency", find);
	}
	// Find All Word Have a same frequency
	public Map<String, Integer> getAllWord(String callingMethod,FindIterable<Document> find) {
		MongoCursor<Document> iterator = find.iterator();
		Map<String, Integer> count = new HashMap<>();
		Document document = null;
		Iterator<String> key = null;
		String wk = null;
		while (iterator.hasNext()) {
			document = iterator.next();
			key = document.keySet().iterator();
			while (key.hasNext()) {
				wk = key.next();
				if (!(wk.equals(wordKey) || wk.equals("_id")))
					System.out.println(callingMethod+" : "+ wk + " : " + document.getInteger(wk));
			}
		}
		return count;
	}
	//Set Key and Value for Least or Most Used word 
	public Map<String, Integer> getWord(Document document) {
		Map<String, Integer> count = new HashMap<>();
		Iterator<String> key = document.keySet().iterator();
		String wk = null;
		while (key.hasNext()) {
			wk = key.next();
			if (!(wk.equals("_id")))
				count.put(wk, document.getInteger(wk));
		}
		return count;
	}
	//creating DB connection
	public void createDBConnection(String ipAddress, int port) {
		mongoClient = new MongoClient(ipAddress, port);
		db = mongoClient.getDatabase("wordcount");
	}

}