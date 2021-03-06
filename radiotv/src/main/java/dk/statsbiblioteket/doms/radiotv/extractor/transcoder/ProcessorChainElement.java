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

import org.apache.log4j.Logger;

import javax.servlet.ServletConfig;

/**
 * And abstract class representing an element in a chain of processes to be
 * carried out sequentially.
 */
public abstract class ProcessorChainElement {

    private static Logger log = Logger.getLogger(ProcessorChainElement.class);

    private ProcessorChainElement parentElement;
    private ProcessorChainElement childElement;

    /**
     * Set the parent element of this element, ie the element to be carried out
     * immediately prior to this one. If null, this element is terminal.
     * @param parentElement
     * @deprecated Iterative processing (using setChildElement) is more flexible and
     * is to be preferred.
     */
    public void setParentElement(ProcessorChainElement parentElement) {
        this.parentElement = parentElement;
    }

    public void setChildElement(ProcessorChainElement childElement) {
        this.childElement = childElement;
    }


    /**
     * Carry out the chain of processing elements which ends
     * with this element. Ie execute the parent element of this process and then process this element
     * @param request
     * @param config
     * @deprecated Iterative processing is a better idea
     */
    public void processRecursively(TranscodeRequest request, ServletConfig config) throws ProcessorException {
        if (parentElement != null) parentElement.processRecursively(request, config);
        log.info("Processing Recursively'" + request.getPid() + "' with " + this.getClass());
        processThis(request, config);
    }

    public void processIteratively(TranscodeRequest request, ServletConfig config) throws ProcessorException {
        log.info("Processing Iteratively'" + request.getPid() + "' with " + this.getClass());
        request.setActiveProcess(this.getClass().getSimpleName());
        processThis(request, config);
        if (childElement != null) childElement.processIteratively(request, config);
    }

    /**
     * Carry out this processing element.
     * @param request
     */
    protected abstract void processThis(TranscodeRequest request, ServletConfig config) throws ProcessorException;

}
