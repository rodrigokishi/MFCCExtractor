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
import TVSSUtils.AudioStreamSelector;
import TVSSUtils.ShotReader;

public class MFCCExtractor
{	
	
	//Usage: MFCCExtractor <in: video file> <in: shot list csv> <in: audio segments output folder> <out: mfcc feature vectors file> <in: audio streams> <in: stream to use>
    public static void main( String[] args ) throws Exception
    { 	    	   	
    	File inputFile = new File(args[0]);
    	ShotList shotList = ShotReader.readFromCSV(args[1]);
    	int audioStreams = Integer.parseInt(args[4]);
    	int selectedStream = Integer.parseInt(args[5]);
    	XuggleAudio inputAudioMFCCRaw = new XuggleAudio(inputFile);    	 
    	   	   	
    	ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();    	
    	String outputAudiosFolder = args[2];   	    	    	    	
    	
    	//If there is more than one stream (dual audio videos for example) choose one
    	if(audioStreams > 1)
    	{
    		inputAudioMFCCRaw = AudioStreamSelector.separateAudioStream(inputAudioMFCCRaw, audioStreams, selectedStream);
    	}    	    	
    	
    	//Calculate how many audio samples must be in a millisecond
    	double samplesInAMillisecond = inputAudioMFCCRaw.getFormat().getSampleRateKHz();
    	//30ms Audio frames
    	int frameSizeInSamples = (int)(samplesInAMillisecond * 30);
    	//10ms Overlap between frames
    	int overlapSizeInSamples = (int)(samplesInAMillisecond *10);

    	//Create an audio processor which works with 30ms windows and 10ms overlap between adjacent windows    	    	
		FixedSizeSampleAudioProcessor inputAudioMFCC = new FixedSizeSampleAudioProcessor(inputAudioMFCCRaw, 
    			frameSizeInSamples, overlapSizeInSamples);
				
		
		
		//Generate and write MFCC descriptors
    	XuggleVideo inputVideoMFCC = new XuggleVideo(inputFile);    	
    	double videoFPS = inputVideoMFCC.getFPS();
    	inputVideoMFCC.close();
    	MFCC mfcc = new MFCC( inputAudioMFCC );
    	SampleChunk scMFCC = null;
    	FileWriter mfccWriter = new FileWriter(args[3]);
    	int shotNum = 0;    	
    	long chunkIndex = 0;    	
    	for(Shot shot : shotList.getList())
    	{
    		long endBoundary = Math.round(shot.getEndBoundary() / videoFPS) * 1000;    		
    		int mfccQTY = 0;
    		scMFCC = mfcc.nextSampleChunk();
    		while( scMFCC  != null &&
    				//I don't know why, but getTimecodeInMilliseconds returns two times the correct timecode.
    				(scMFCC.getStartTimecode().getTimecodeInMilliseconds()/scMFCC.getFormat().getNumChannels()) < endBoundary     				
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
    			
    			//Write output stream not including overlapped chunks 
    			if(chunkIndex % 3 == 0)
    			{
	        		byteArrayOutputStream.flush();
	       			byteArrayOutputStream.write(scMFCC.getSamples());
	        		byteArrayOutputStream.flush(); 
    			}
    			
    			scMFCC = mfcc.nextSampleChunk();
    			chunkIndex++;    			
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
    	inputAudioMFCCRaw.close();
    	mfccWriter.close();    	    	
    	
    	System.exit(0);
    	
    
    }
}