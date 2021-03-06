/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
 *
 * Copyright 2004-2009 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 */
package dk.statsbiblioteket.doms.radiotv.extractor.transcoder.previewer;

import dk.statsbiblioteket.doms.radiotv.extractor.Constants;
import dk.statsbiblioteket.doms.radiotv.extractor.ExternalJobRunner;
import dk.statsbiblioteket.doms.radiotv.extractor.transcoder.*;
import dk.statsbiblioteket.doms.radiotv.extractor.transcoder.extractor.FlashTranscoderProcessor;
import dk.statsbiblioteket.doms.radiotv.extractor.transcoder.snapshotter.MuxSnapshotGeneratorProcessor;
import org.apache.log4j.Logger;

import javax.servlet.ServletConfig;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class DigitvPreviewProcessor extends ProcessorChainElement {

    private static Logger log = Logger.getLogger(DigitvPreviewProcessor.class);


    /**
     * Clip a 30 second preview from the middle of the longest subclip
     * @param request
     * @param config
     * @throws dk.statsbiblioteket.doms.radiotv.extractor.transcoder.ProcessorException
     */
    @Override
    protected void processThis(TranscodeRequest request, ServletConfig config) throws ProcessorException {
        log.debug("Beginning preview processing of '" + request.getPid() + "'");
        int previewLengthSeconds = Integer.parseInt(Util.getInitParameter(config, Constants.PREVIEW_LENGTH));
        Long blocksize = 1880L;
        Long previewLengthBytes = ClipTypeEnum.getType(request).getBitrate() * previewLengthSeconds;
        TranscodeRequest.FileClip longestClip = request.getLongestClip();
        log.debug("Longest clip for '" + request.getPid() + "' is '" + longestClip + "'");

        TranscodeRequest.SnapshotPosition previewSnapshot = new TranscodeRequest.SnapshotPosition();
        previewSnapshot.setFilepath(longestClip.getFilepath());
        previewSnapshot.setProgramId(longestClip.getProgramId());
        List<TranscodeRequest.SnapshotPosition> pos = new ArrayList<TranscodeRequest.SnapshotPosition>();
        pos.add(previewSnapshot);
        request.setSnapshotPositions(pos);
        MuxSnapshotGeneratorProcessor snapshotter = new MuxSnapshotGeneratorProcessor();
        snapshotter.setLabel(MuxSnapshotGeneratorProcessor.PREVIEW_LABEL);
        this.setChildElement(snapshotter);
        if (longestClip == null) {
            String message = "Failed to identify longest clip in '" + request.getPid() + "'\n from shard data \n " + request.getShard();
            throw new ProcessorException(message);
        }
        String processSubstituionDDCommand = getDDCommand(blocksize, longestClip, previewLengthBytes, previewSnapshot);
        int programNumber = longestClip.getProgramId();
        boolean useCustomPMT = (request.getDvbsubPid() != null && !request.getAudioPids().isEmpty() && request.getVideoPid() != null && request.getVideoFcc() != null && request.getAudioFcc() != null );
        useCustomPMT = useCustomPMT |  (!request.getAudioPids().isEmpty() && request.getVideoPid() != null && request.getVideoFcc() != null && request.getAudioFcc() != null && programNumber==101);


        String clipperCommand;
        if (!useCustomPMT) {
            clipperCommand = "cat " + processSubstituionDDCommand + " | vlc - --program=" + programNumber + " --quiet --demux=ts --intf dummy --play-and-exit --noaudio --novideo "
                    + "--sout-all --sout '#duplicate{dst=\"transcode{senc=dvbsub}"
                    + ":transcode{vcodec=h264,vb=" + Util.getVideoBitrate(config) + ",venc=x264{" + Util.getInitParameter(config, Constants.X264_PRESET_VLC) + "},soverlay,deinterlace,audio-sync,"
                    + ",width=" + FlashTranscoderProcessor.getWidth(request, config)
                    + ",height=" + FlashTranscoderProcessor.getHeight(request, config) +",threads=0}"
                    + ":std{access=file,mux=ts,dst=-}\""
                    + ",select=\"program=" + programNumber + "\"' | "
                    + "ffmpeg -i -  -async 2 -vcodec copy -ac 2 -acodec libmp3lame -ar 44100 -ab "
                    + Util.getAudioBitrate(config) + " -f flv " + OutputFileUtil.getFlashVideoPreviewOutputFile(request, config) ;
        }  else {
            String programSelector = " --program=1010 --sout-all --ts-extra-pmt=1010:1010=" + request.getVideoPid() + ":video=" + request.getVideoFcc()
                    + "," + request.getMinimumAudioPid() + ":audio=" + request.getAudioFcc();
            if (request.getDvbsubPid() != null) {
                programSelector += "," + request.getDvbsubPid() + ":spu=dvbs";
            }
            log.debug("Using Custom PMT for '" + request.getPid() + "': " + programSelector);
            clipperCommand = "cat " + processSubstituionDDCommand + " | vlc - " + programSelector + programSelector + " --quiet --demux=ts --intf dummy --play-and-exit --noaudio --novideo "
                    + "--sout-all --sout '#transcode{vcodec=x264,vb=" + Util.getVideoBitrate(config) + ",venc=x264{" + Util.getInitParameter(config, Constants.X264_PRESET_VLC) + "}" +
                    ",soverlay,deinterlace,audio-sync,"
                    + ",width=" + FlashTranscoderProcessor.getWidth(request, config)
                    + ",height=" + FlashTranscoderProcessor.getHeight(request, config) +",threads=0}"
                    + ":std{access=file,mux=ts,dst=-}' |" +
                    "ffmpeg -i -  -async 2 -vcodec copy -acodec libmp3lame -ac 2 -ar 44100 -ab " + Util.getAudioBitrate(config)
                    + "000 -f flv " + OutputFileUtil.getFlashVideoPreviewOutputFile(request, config) ;
        }
        int clipLengthSeconds = Integer.parseInt(Util.getInitParameter(config, Constants.PREVIEW_LENGTH));
        try {
            final long timeout = Math.round(Double.parseDouble(Util.getInitParameter(config, Constants.PREVIEW_TIMEOUT_FACTOR))*clipLengthSeconds * 1000L);
            log.debug("Setting timeout to '" + timeout + "' ms.");
            ExternalJobRunner.runClipperCommand(timeout, clipperCommand);
        } catch (ExternalProcessTimedOutException e) {
            final File previewOutputFile = OutputFileUtil.getFlashVideoPreviewOutputFile(request, config);
            log.info("Deleting '" + previewOutputFile.getAbsolutePath() + "'");
            previewOutputFile.delete();
            throw new ProcessorException(e);
        } finally {
            RequestRegistry.getInstance().remove(request);
        }
        request.setServiceType(ServiceTypeEnum.PREVIEW_THUMBNAIL_GENERATION);
        RequestRegistry.getInstance().register(request);
    }



    private String getDDCommand(Long blocksize, TranscodeRequest.FileClip longestClip, Long previewLengthBytes, TranscodeRequest.SnapshotPosition position) {
        String processSubstituionDDCommand = null;
        Long clipLength = longestClip.getClipLength();
        Long clipOffset = longestClip.getStartOffsetBytes();
        Long fileLength = new File(longestClip.getFilepath()).length();
        if (clipLength == null && clipOffset == null) {
            processSubstituionDDCommand = "<(dd if=" + longestClip.getFilepath() + " bs=" + blocksize +
                    " skip=" + fileLength/(2*blocksize) +
                    " count=" + previewLengthBytes/blocksize + ")";
            position.setBytePosition(fileLength/2);
        } else if (clipLength != null && clipOffset != null) {
            processSubstituionDDCommand = "<(dd if=" + longestClip.getFilepath() + " bs=" + blocksize +
                    " skip=" + (clipOffset + clipLength/2)/blocksize +
                    " count=" + previewLengthBytes/blocksize + ")";
            position.setBytePosition(clipOffset + clipLength/2);
        } else if (clipLength == null && clipOffset != null) {
            processSubstituionDDCommand = "<(dd if=" + longestClip.getFilepath() + " bs=" + blocksize +
                    " skip=" + (clipOffset + (fileLength-clipOffset)/2)/blocksize +
                    " count=" + previewLengthBytes/blocksize + ")";
            position.setBytePosition(clipOffset + (fileLength-clipOffset)/2);
        } else if (clipLength != null && clipOffset == null) {
            processSubstituionDDCommand = "<(dd if=" + longestClip.getFilepath() + " bs=" + blocksize +
                    " skip=" + clipLength/(2*blocksize) +
                    " count=" + previewLengthBytes/blocksize + ")";
            position.setBytePosition(clipLength/2);
        }
        return processSubstituionDDCommand;
    }

}
