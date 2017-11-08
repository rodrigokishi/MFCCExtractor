package intermidia;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import org.openimaj.audio.SampleChunk;
import org.openimaj.audio.features.MFCC;
import org.openimaj.audio.processor.FixedSizeSampleAudioProcessor;
import org.openimaj.video.xuggle.XuggleAudio;
import org.openimaj.video.xuggle.XuggleVideo;

import TVSSUnits.Shot;
import TVSSUnits.ShotList;
import TVSSUtils.ShotReader;
import TVSSUtils.VideoPinpointer;

public class MFCCExtractor 
{
	//Usage: MFCCExtractor <video file> <shot list csv> <audio segments output folder> <mfcc feature vectors file>
    public static void main( String[] args ) throws Exception
    { 	
    	File inputFile = new File(args[0]);
    	ShotList shotList = ShotReader.readFromCSV(args[1]);
    	   	   	
    	ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();    	
    	String outputAudiosFolder = args[2];   	
    	    	
    	
    	//Generate and write MFCC descriptors
    	XuggleAudio inputAudioMFCCRaw = new XuggleAudio(inputFile);    	
    	//Calculate how many audio samples must be in a millisecond
    	double samplesInAMillisecond = inputAudioMFCCRaw.getFormat().getSampleRateKHz();
    	//30ms Audio frames
    	int frameSizeInSamples = (int)(samplesInAMillisecond * 30);
    	//10ms Overlap between frames
    	int overlapSizeInSamples = (int)(samplesInAMillisecond *10);
    	//Fixes the audio processor to work with 30ms windows and 10ms overlap between adjacent windows
    	FixedSizeSampleAudioProcessor inputAudioMFCC = new FixedSizeSampleAudioProcessor(inputAudioMFCCRaw, 
    			frameSizeInSamples, overlapSizeInSamples);
    	
    	XuggleVideo inputVideoMFCC = new XuggleVideo(inputFile);
    	MFCC mfcc = new MFCC( inputAudioMFCC );
    	SampleChunk scMFCC = null;
    	FileWriter mfccWriter = new FileWriter(args[3]);
    	int shotNum = 0;
    	for(Shot shot : shotList.getList())
    	{
    		VideoPinpointer.seek(inputVideoMFCC, shot.getEndBoundary());
    		long endBoundary = inputVideoMFCC.getCurrentTimecode().getTimecodeInMilliseconds();    		
    		
    		int mfccQTY = 0;
    		//Single audio videos
    		if(inputAudioMFCC.getFormat().getNumChannels() == 1)
    		{
	    		while( (scMFCC = mfcc.nextSampleChunk()) != null &&
	    				scMFCC.getStartTimecode().getTimecodeInMilliseconds() < endBoundary     				
	    				)
	    		{
	    			double[][] mfccs = mfcc.getLastCalculatedFeature();
	    			mfccWriter.write(Integer.toString(shotNum));
	    			for(int i = 0; i < mfccs[0].length; i++)
	    			{
	    				mfccWriter.write(" " + mfccs[0][i]);
	    			}
	    			mfccWriter.write("\n");
	    			mfccQTY++;
	    			
	    			//Write output stream
	        		byteArrayOutputStream.flush();
	       			byteArrayOutputStream.write(scMFCC.getSamples());
	        		byteArrayOutputStream.flush(); 
	        			        			        	
	    		}
    		}
    		//Dual audio videos
    		else
    		{
    			int pair = 0;
	    		while( (scMFCC = mfcc.nextSampleChunk()) != null &&
	    				scMFCC.getStartTimecode().getTimecodeInMilliseconds() < endBoundary     				
	    				)
	    		{
	    			if(pair % 2 == 0)
	    			{	    				
		    			double[][] mfccs = mfcc.getLastCalculatedFeature();
		    			
		    			mfccWriter.write(Integer.toString(shotNum));
		    			for(int i = 0; i < mfccs[0].length; i++)
		    			{
		    				mfccWriter.write(" " + mfccs[0][i]);
		    			}
		    			mfccWriter.write("\n");
		    			mfccQTY++;
		    			
		    			//Write output stream
	    				byteArrayOutputStream.flush();
	        			byteArrayOutputStream.write(scMFCC.getSamples());
	        			byteArrayOutputStream.flush();
	    			}
	    			pair++;
	    		}
    			
    		}
    		//If no MFCC descriptor was generated for the audio segment, fill it with one dummy values descriptor
    		if(mfccQTY == 0)
    		{
    			mfccWriter.write(Integer.toString(shotNum));
    			for(int i = 0; i < 13; i++)
    			{
    				mfccWriter.write(" " + Double.MIN_VALUE);
    			}
    			mfccWriter.write("\n");
    		}
    		
    		//Write file on disk
    		ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
			AudioInputStream audioInputStream = new AudioInputStream(byteArrayInputStream, inputAudioMFCC.getFormat().getJavaAudioFormat(), byteArrayOutputStream.size());
			String audioSampleName = "s" + String.format("%04d", shotNum) + ".wav";
			AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, new File(outputAudiosFolder + audioSampleName));			
		
			//Clear the byteArrayOutputStream
			byteArrayOutputStream.reset();

    		shotNum++;
    	}
    	inputVideoMFCC.close();
    	inputAudioMFCCRaw.close();
    	
    	
    	mfccWriter.close();    	    	
    	
    	System.exit(0);
    	
    
    }
}