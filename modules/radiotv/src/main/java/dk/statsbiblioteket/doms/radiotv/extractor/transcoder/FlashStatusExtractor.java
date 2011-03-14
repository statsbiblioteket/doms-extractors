/* $Id$
 * $Revision$
 * $Date$
 * $Author$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2009 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
 *   USA
 */
package dk.statsbiblioteket.doms.radiotv.extractor.transcoder;

import dk.statsbiblioteket.doms.radiotv.extractor.ObjectStatus;
import dk.statsbiblioteket.doms.radiotv.extractor.ObjectStatusEnum;
import dk.statsbiblioteket.doms.radiotv.extractor.Constants;

import javax.servlet.ServletConfig;
import java.io.UnsupportedEncodingException;
import java.io.File;

import org.apache.log4j.Logger;

public class FlashStatusExtractor {

    private static Logger log = Logger.getLogger(FlashStatusExtractor.class);


    public static ObjectStatus getStatus(String shardUrl, ServletConfig config) throws UnsupportedEncodingException, ProcessorException {
        String uuid = Util.getUuid(shardUrl);
        if (uuid == null) throw new IllegalArgumentException("Invalid url - no uuid found: '" + shardUrl + "'");
        TranscodeRequest request = new TranscodeRequest(uuid);
        request.setServiceType(ServiceTypeEnum.BROADCAST_EXTRACTION);
        boolean isDone = (Util.hasOutputFile(request, config)) && !ClipStatus.getInstance().isKnown(request);
        Double percentage = 0.0;
        if (isDone) {
            log.debug("Found already fully ready result for '" + uuid + "'");
            ObjectStatus status = new ObjectStatus();
            status.setStatus(ObjectStatusEnum.DONE);
            status.setStreamId(Util.getStreamId(request, config));
            status.setServiceUrl(Util.getInitParameter(config,Constants.WOWZA_URL));
            status.setCompletionPercentage(100.0);
            return status;
        } else if (ClipStatus.getInstance().isKnown(request)) {
            log.debug("Already started transcoding '" + uuid + "'");
            request = ClipStatus.getInstance().get(request);
            ObjectStatus status = new ObjectStatus();
            status.setServiceUrl(Util.getInitParameter(config, Constants.WOWZA_URL));
            if (Util.hasOutputFile(request, config)) {
                final long outputFileLength = Util.getOutputFile(request, config).length();
                if (request != null && request.getFinalFileLengthBytes() != null && request.getFinalFileLengthBytes() != 0) {
                    double completionPercentage = 100.0 * outputFileLength/request.getFinalFileLengthBytes();
                    completionPercentage = Math.min(completionPercentage, 99.5);
                    status.setCompletionPercentage(completionPercentage);
                }
                status.setFlashFileLengthBytes(outputFileLength);
                status.setStreamId(Util.getStreamId(request, config));
                percentage = status.getCompletionPercentage();
            }
            status.setStatus(ObjectStatusEnum.STARTED);
            if (percentage != null && percentage < 0.00001) {
                status.setPositionInQueue(Util.getQueuePosition(request, config));
            }
            return status;
        } else return null;
    }
}
