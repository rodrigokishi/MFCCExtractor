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
import org.openimaj.video.xuggle.XuggleAudio;
import org.openimaj.video.xuggle.XuggleVideo;

import TVSSUnits.ShotList;
import TVSSUtils.ShotReader;

public class MFCCExtractor 
{
	//Usage: MFCCExtractor <video file> <shot list csv> <audio segments output folder> <mfcc feature vectors file>
    public static void main( String[] args ) throws Exception
    {
    	File source = new File(args[0]);
    	XuggleVideo xuggleSource = new XuggleVideo(source);
		System.out.println("Reading shots.");
		ShotList shotList = ShotReader.readFromCSV(xuggleSource, args[1]);	
		xuggleSource.close();
    	    	
    	final XuggleAudio xuggleAudioSource = new XuggleAudio(source);    	
    	MFCC mfcc = new MFCC( xuggleAudioSource );   	    	
    	SampleChunk sc = null;    	    	   

    	final XuggleAudio xuggleAudioSourceCopy = new XuggleAudio(source);
    	SampleChunk sa;
    	ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();    	
    	String outputAudiosFolder = args[2];
    	int shotNum = 0;
    	long videoEndBoundary = shotList.getShot(shotList.listSize() - 1).getEndBoundary().getTimecode().getTimecodeInMilliseconds();
    	while((sa = xuggleAudioSourceCopy.nextSampleChunk() )!= null
    			&& sa.getStartTimecode().getTimecodeInMilliseconds() < videoEndBoundary)
    	{
    		byteArrayOutputStream.flush();
    		byteArrayOutputStream.write(sa.getSamples());
    		byteArrayOutputStream.flush();
    		
    		long shotEndBoundary = shotList.getShot(shotNum).getEndBoundary().getTimecode().getTimecodeInMilliseconds();
    		if(sa.getStartTimecode().getTimecodeInMilliseconds() > shotEndBoundary )
    		{
    			ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
    			AudioInputStream audioInputStream = new AudioInputStream(byteArrayInputStream, xuggleAudioSource.getFormat().getJavaAudioFormat(), byteArrayOutputStream.size());
    			String audioSampleName = "s" + String.format("%04d", shotNum) + ".wav";
    			AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, new File(outputAudiosFolder + audioSampleName));    			
    			shotNum++;
    			
    			//Clears the byteArrayOutputStream
    			byteArrayOutputStream.reset();
    		}
    		
    		sa = xuggleAudioSourceCopy.nextSampleChunk();
    	} 
    	//Write the last shot audio
    	{
			ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
			AudioInputStream audioInputStream = new AudioInputStream(byteArrayInputStream, xuggleAudioSource.getFormat().getJavaAudioFormat(), byteArrayOutputStream.size());
			String audioSampleName = "s" + String.format("%04d", shotNum) + ".wav";
			AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, new File(outputAudiosFolder + audioSampleName));    			    		
    	}
    	xuggleAudioSourceCopy.close();
    	

    	FileWriter mfccWriter = new FileWriter(args[3]);
    	
    	shotNum = 0;
    	videoEndBoundary = shotList.getShot(shotList.listSize() - 1).getEndBoundary().getTimecode().getTimecodeInMilliseconds();
		while( (sc = mfcc.nextSampleChunk()) != null && sc.getStartTimecode().getTimecodeInMilliseconds() < videoEndBoundary)
		{			
			long shotEndBoundary = shotList.getShot(shotNum).getEndBoundary().getTimecode().getTimecodeInMilliseconds();
			double[][] mfccs = mfcc.getLastCalculatedFeature();
			while(sc.getStartTimecode().getTimecodeInMilliseconds() > shotEndBoundary)
			{
				shotNum++;
				if(shotNum < shotList.getList().size())
				{
					shotEndBoundary = shotList.getShot(shotNum).getEndBoundary().getTimecode().getTimecodeInMilliseconds();
				}
			}
			System.out.print("Shot " + shotNum);
			mfccWriter.write(Integer.toString(shotNum));
			for(int i = 0; i < mfccs[0].length; i++)
			{
				System.out.printf(" %.2f", mfccs[0][i]);
				mfccWriter.write(" " + mfccs[0][i]);
			}
			System.out.println();
			mfccWriter.write("\n");
		}
		xuggleAudioSource.close();		
		mfccWriter.close();
		System.exit(0);
    }
}