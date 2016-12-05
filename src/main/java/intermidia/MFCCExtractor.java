package intermidia;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.util.ArrayList;

import javassist.bytecode.ByteArray;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
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
    public static void main( String[] args ) throws Exception
    {
    	File source = new File(args[0]);
    	XuggleVideo xuggleSource = new XuggleVideo(source);
		System.out.println("Reading shots.");
		ShotList shotList = ShotReader.readFromCSV(xuggleSource, args[1]);	
		xuggleSource.close();
    	    	
    	final XuggleAudio xa = new XuggleAudio(source);    	
    	MFCC mfcc = new MFCC( xa );   	    	
    	SampleChunk sc = null;
    	
    	
    	    	   
    	SampleChunk sa = xa.nextSampleChunk();
    	ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();    	
    	while(sa != null)
    	{
    		byteArrayOutputStream.write(sa.getSamples());    		
    		sa = xa.nextSampleChunk();
    	}   	
    	ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
    	AudioInputStream audioInputStream = new AudioInputStream(byteArrayInputStream, AudioFileFormat.Type.WAVE,1);
    	AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, new File("sample.wav"));
    	System.exit(0);
    	
    	
    	
    	
    	
    	FileWriter mfccWriter = new FileWriter(args[2]);
    	

    	int shotNum = 0;
    	long videoEndBoundary = shotList.getShot(shotList.listSize() - 1).getEndBoundary().getTimecode().getTimecodeInMilliseconds();
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
		xa.close();		
		mfccWriter.close();

    }
}