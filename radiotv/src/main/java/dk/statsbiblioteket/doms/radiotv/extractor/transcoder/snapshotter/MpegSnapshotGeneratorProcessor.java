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
package dk.statsbiblioteket.doms.radiotv.extractor.transcoder.snapshotter;

import dk.statsbiblioteket.doms.radiotv.extractor.Constants;
import dk.statsbiblioteket.doms.radiotv.extractor.ExternalJobRunner;
import dk.statsbiblioteket.doms.radiotv.extractor.transcoder.*;
import org.apache.log4j.Logger;

import javax.servlet.ServletConfig;
import java.io.File;
import java.util.List;

public class MpegSnapshotGeneratorProcessor extends ProcessorChainElement {

    private static Logger log = Logger.getLogger(MpegSnapshotGeneratorProcessor.class);


    public static final String DEFAULT_LABEL = "snapshot";
    public static final String PREVIEW_LABEL = "snapshot.preview";

    /**
     * Just a factor to control for the fact that we don't need to read so much from an mpeg file as we do from a mux,
     */
    public static final int MPEG_SPEEDUP = 4;

    private String label = DEFAULT_LABEL;

    private void setLabel(String label) {
        this.label = label;
    }


    @Override
    protected void processThis(TranscodeRequest request, ServletConfig config) throws ProcessorException {
        //request.setServiceType(ServiceTypeEnum.THUMBNAIL_GENERATION);
        switch (request.getServiceType()) {
            case THUMBNAIL_GENERATION:
                setLabel(DEFAULT_LABEL);
                break;
            case PREVIEW_THUMBNAIL_GENERATION:
                setLabel(PREVIEW_LABEL);
                break;
            case BROADCAST_EXTRACTION:
                throw new RuntimeException("This processor should not be called with service type BROADCAST_EXTRACTION");
            case PREVIEW_GENERATION:
                throw new RuntimeException("This processor should not be called with service type PREVIEW_GENERATION");
        }
        long blocksize = 1880L;  
        long bitrate = request.getClipType().getBitrate();
        int seconds = Integer.parseInt(Util.getInitParameter(config, Constants.SNAPSHOT_VIDEO_LENGTH));
        List<TranscodeRequest.SnapshotPosition> snapshots = request.getSnapshotPositions();
        int count = 0;
        for (TranscodeRequest.SnapshotPosition snapshot: snapshots) {
            String filepath = snapshot.getFilepath();
            Long location = snapshot.getBytePosition();
            String command = "cat <(dd if=" + filepath + " bs=" + blocksize +
                    " skip=" + location/blocksize + " count=" + seconds*bitrate/(MPEG_SPEEDUP*blocksize) + " )|"
                    + " vlc - "
                    + " --quiet --demux=ps --video-filter scene -V dummy  --intf dummy --play-and-exit --vout-filter deinterlace --deinterlace-mode " +
                    "linear --noaudio  " +
                    "--scene-format=" + Util.getPrimarySnapshotSuffix(config) + " --scene-ratio=1000 --scene-replace --scene-prefix=" + OutputFileUtil.getSnapshotBasename(config, request, label, ""+count)
                    + " --scene-path=" + OutputFileUtil.getAndCreateOutputDir(request, config).getAbsolutePath();
            try {
                long timeout = Math.round(Double.parseDouble(Util.getInitParameter(config, Constants.SNAPSHOT_TIMEOUT_FACTOR))*seconds*1000L);
                log.debug("Setting transcoding timeout for '" + request.getPid() + "' to " + timeout + "ms" );
                ExternalJobRunner.runClipperCommand(timeout, command);
                final File fullPrimarySnapshotFile = OutputFileUtil.getFullPrimarySnapshotFile(config, request, label, "" + count);
                SnapshotUtil.imageMagickConvert(config, fullPrimarySnapshotFile, OutputFileUtil.getFullFinalSnapshotFile(config, request, label, "" + count));
                fullPrimarySnapshotFile.delete();
            } catch (ExternalProcessTimedOutException e) {
                log.warn(e);
            }
            count++;
        }
    }
}
