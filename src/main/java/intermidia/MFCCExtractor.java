package intermidia;

import java.io.File;
import java.io.FileWriter;

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