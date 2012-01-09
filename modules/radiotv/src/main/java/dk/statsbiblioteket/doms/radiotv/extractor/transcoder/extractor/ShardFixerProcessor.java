package dk.statsbiblioteket.doms.radiotv.extractor.transcoder.extractor;

import dk.statsbiblioteket.doms.radiotv.extractor.transcoder.ClipTypeEnum;
import dk.statsbiblioteket.doms.radiotv.extractor.transcoder.ProcessorChainElement;
import dk.statsbiblioteket.doms.radiotv.extractor.transcoder.ProcessorException;
import dk.statsbiblioteket.doms.radiotv.extractor.transcoder.ShardStructure;
import dk.statsbiblioteket.doms.radiotv.extractor.transcoder.TranscodeRequest;
import org.apache.log4j.Logger;

import javax.servlet.ServletConfig;
import java.io.File;


/**
 * This processor uses the data generated by ShardAnalyserProcessor to fix shards
 * where possible or to abort transcoding where desirable.
 */
public class ShardFixerProcessor extends ProcessorChainElement {

    private static final int MAX_HOLE_SIZE = 120;

    private static Logger log = Logger.getLogger(ShardFixerProcessor.class);


    @Override
    protected void processThis(TranscodeRequest request, ServletConfig config) throws ProcessorException {
        final ShardStructure structure = request.getStructure();
        if (structure.isNonTrivial()) {
            if (structure.getMissingEnd() != null) {
                  throw new ProcessorException("Could not transcode '" + request.getPid() + "': missing end");
            }
            if (structure.getMissingStart() != null) {
                throw new ProcessorException("Could not transcode '" + request.getPid() + "': missing start");
            }
            if (!(structure.getHoles() == null) && !structure.getHoles().isEmpty()) {
                for ( ShardStructure.Hole hole: structure.getHoles()) {
                    if (hole.getHoleLength() > MAX_HOLE_SIZE) {
                        throw new ProcessorException("Could not transcode '" + request.getPid() +
                                "': hole of length '" + hole.getHoleLength() + "' seconds is greater than maximum " +
                                "tolerated value '" + MAX_HOLE_SIZE + "'");
                    }
                }
            }
            if (!(structure.getOverlaps() == null) && !structure.getOverlaps().isEmpty()) {
                for (ShardStructure.Overlap overlap: structure.getOverlaps()) {
                   processOverlap(request, config, overlap);
                }
            }
        }
    }

    private void processOverlap(TranscodeRequest request, ServletConfig config, ShardStructure.Overlap overlap) throws ProcessorException {
        TranscodeRequest.FileClip clip1 = null;
        TranscodeRequest.FileClip clip2 = null;
        for (TranscodeRequest.FileClip clip: request.getClips()) {
            if ((new File(clip.getFilepath())).getName().equals(overlap.getFilePath1())) {
                clip1 = clip;
            } else
            if ((new File(clip.getFilepath())).getName().equals(overlap.getFilePath2())) {
                clip2 = clip;
            }
        }
        switch(overlap.getOverlapType()) {
            case(0):
                final long startOffsetBytes = overlap.getOverlapLength() * ClipTypeEnum.getType(request).getBitrate();
                log.info("Fixing overlap '" + overlap + "' by setting start offset to '" + startOffsetBytes + " bytes' in" +
                        " file '" + clip2.getFilepath() + "'");
                clip2.setStartOffsetBytes(startOffsetBytes);
                break;
            case(1):
                log.info("Fixing overlap '" + overlap + "' by removing '" + clip2.getFilepath() + "'");
                request.getClips().remove(clip2);
                break;
            case(2):
                log.info("Fixing overlap '" + overlap + "' by removing '" + clip2.getFilepath() + "'");
                request.getClips().remove(clip2);
                break;
            case(3):
                log.info("Fixing overlap '" + overlap + "' by removing '" + clip1.getFilepath() + "'");
                request.getClips().remove(clip1);
                break;
        }

    }

}