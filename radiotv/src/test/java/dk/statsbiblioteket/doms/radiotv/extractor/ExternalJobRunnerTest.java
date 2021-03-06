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
package dk.statsbiblioteket.doms.radiotv.extractor;

import dk.statsbiblioteket.doms.radiotv.extractor.transcoder.ExternalProcessTimedOutException;

import java.io.IOException;

public class ExternalJobRunnerTest {
    public static void main(String[] args) throws IOException, ExternalProcessTimedOutException, InterruptedException {
        Integer ii = null;
        int j = ii;
        for (int i = 0; i<100000; i++) {
            ExternalJobRunner runner = new ExternalJobRunner("true");
            runner.getError();
            runner.getOutput();
            System.out.println(runner.getExitValue() + " " + i);
        }
    }
}
