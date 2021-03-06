package code.articles;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;
import java.util.jar.JarFile;

import util.WikipediaPageInputFormat;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.GenericOptionsParser;

import edu.umd.cloud9.collection.wikipedia.WikipediaPage;

/**
 * This class is used for Section A of assignment 1. You are supposed to
 * implement a main method that has first argument to be the dump wikipedia
 * input filename , and second argument being an output filename that only
 * contains articles of people as mentioned in the people auxiliary file.
 */
public class GetArticlesMapred {

	//@formatter:off
	/**
	 * Input:
	 * 		Page offset 	WikipediaPage
	 * Output
	 * 		Page offset 	WikipediaPage
	 * @author Tuan
	 *
	 */
	//@formatter:on
	public static class GetArticlesMapper extends Mapper<LongWritable, WikipediaPage, Text, Text> {
		public static Set<String> peopleArticlesTitles = new HashSet<String>();

		@Override
		protected void setup(Mapper<LongWritable, WikipediaPage, Text, Text>.Context context)
				throws IOException, InterruptedException {
			// TODO: You should implement people articles load from
			// DistributedCache here
			try {	
				String PEOPLE_FILE = "people.txt";
				ClassLoader cl = GetArticlesMapred.class.getClassLoader();
				String fileUrl = cl.getResource(PEOPLE_FILE).getFile();
				
				// Get jar path
				String jarUrl = fileUrl.substring(5, fileUrl.length() - PEOPLE_FILE.length() - 2);
				JarFile jf = new JarFile(new File(jarUrl));
				// Scan the people.txt file inside jar
				Scanner sc = new Scanner(jf.getInputStream(jf.getEntry(PEOPLE_FILE)));
				
				while (sc.hasNextLine())
				{
					String name = sc.nextLine();
					peopleArticlesTitles.add(name);				
				}
				
				sc.close();
				jf.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			super.setup(context);
		}

		@Override
		public void map(LongWritable offset, WikipediaPage inputPage, Context context)
				throws IOException, InterruptedException {
			// TODO: You should implement getting article mapper here
			// inputPage is a cloud9 wikipedia page
			String title = inputPage.getTitle();
			if (peopleArticlesTitles.contains(title)){
				Text textOffset = new Text(offset.toString());
				Text xmlPage = new Text(inputPage.getRawXML());
				context.write(textOffset,xmlPage);
			}
			
			
			
		}
	}

	public static void main(String[] args) throws Exception {
		Configuration conf = new Configuration();
		String[] otherArgs = new GenericOptionsParser(conf,args).getRemainingArgs();
		if (otherArgs.length != 2) {
			System.err.println("Incorrect arguments");
			System.exit(2);
		}
		
		Job job = Job.getInstance(conf);
		job.setJarByClass(GetArticlesMapred.class);
		job.setMapperClass(GetArticlesMapper.class);
		
		job.setInputFormatClass(WikipediaPageInputFormat.class);
		
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(Text.class);
		
		FileInputFormat.addInputPath(job, new Path(otherArgs[0]));
		FileOutputFormat.setOutputPath(job, new Path(otherArgs[1]));
		
		System.exit(job.waitForCompletion(true) ? 0 : 1);
	}
}
