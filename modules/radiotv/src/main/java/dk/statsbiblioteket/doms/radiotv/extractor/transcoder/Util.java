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
package dk.statsbiblioteket.doms.radiotv.extractor.transcoder;

import dk.statsbiblioteket.doms.radiotv.extractor.Constants;

import javax.servlet.ServletConfig;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Util {

    /**
     * Pattern for extracting a uuid of a doms object from url-decoded
     * permanent url.
     */
    public static final String UUID_STRING = ".*uuid:(.*)";
    public static final Pattern UUID_PATTERN = Pattern.compile(UUID_STRING);

    private Util(){}


    public static String getDemuxFilename(TranscodeRequest request) {
        return request.getPid()+"_first.ts";
    }

    /**
     * Get the bitrate in bytes per second from the filename.
     * @param filename
     * @return
     */
    public static Long getBitrate(String filename) {
          if (filename.startsWith("mux")) {
              return 2488237L;
          } else if (filename.contains("_mpeg1_")) {
              return 169242L;
          }  else if (filename.contains("_mpeg2_")) {
              return 872254L;
          }  else if (filename.contains("_wav_")) {
              return 88200L;
          } else return null;
    }

    /**
     * Gets the uuid from a shard url
     * @param shardUrl
     * @return
     * @throws UnsupportedEncodingException
     */
    public static String getUuid(String shardUrl) throws UnsupportedEncodingException {
        String urlS = URLDecoder.decode(shardUrl, "UTF-8");
        Matcher m = UUID_PATTERN.matcher(urlS);
        if (m.matches()) {
             return m.group(1);
        } else return null;
    }

    /**
     * Converts a shard url to a doms url
     * e.g. http://www.statsbiblioteket.dk/doms/shard/uuid:ef8ea1b2-aaa8-412a-a247-af682bb57d25
     * to <DOMS_SERVER>/fedora/objects/uuid%3Aef8ea1b2-aaa8-412a-a247-af682bb57d25/datastreams/SHARD_METADATA/conteny
     *
     * @param pid
     * @return
     */
    public static URL getDomsUrl(String pid, ServletConfig config) throws ProcessorException {
        String urlS = config.getInitParameter(Constants.DOMS_LOCATION)+"/objects/uuid:"+pid+"/datastreams/SHARD_METADATA/content";
        try {
            return new URL(urlS);
        } catch (MalformedURLException e) {
            throw new ProcessorException(e);
        }
    }

    public static String getFinalFilename(TranscodeRequest request) {
        return request.getPid() + ".mp4";
    }

    public static File getTempDir(ServletConfig config) {
        return new File(config.getInitParameter(Constants.TEMP_DIR_INIT_PARAM));
    }

    public static File getFinalDir(ServletConfig config) {
        return new File(config.getInitParameter(Constants.FINAL_DIR_INIT_PARAM));
    }

    public static File getDemuxFile(TranscodeRequest request, ServletConfig config) {
        return new File(getTempDir(config), getDemuxFilename(request));
    }

    public static File getIntialFinalFile(TranscodeRequest request, ServletConfig config) {
        return new File(getTempDir(config), getFinalFilename(request));
    }

    public static File getFinalFinalFile(TranscodeRequest request, ServletConfig config) {
        return new File(getFinalDir(config), getFinalFilename(request));
    }

}