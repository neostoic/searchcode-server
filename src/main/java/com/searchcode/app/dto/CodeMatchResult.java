/*
 * Copyright (c) 2016 Boyter Online Services
 *
 * Use of this software is governed by the Fair Source License included
 * in the LICENSE.TXT file
 *
 * After the following date 27 August 2019 this software version '1.2.3' or '1.2.4' is dual licenced under the
 * Fair Source Licence included in the LICENSE.txt file or under the GNU General Public License Version 3 with terms
 * specified at https://www.gnu.org/licenses/gpl-3.0.txt
 */


package com.searchcode.app.dto;

// Class to hold search matching results
// IE after search and scanning though to find matching lines
public class CodeMatchResult {
    public String line = null;
    public boolean matching = false;
    public boolean addBreak = false;
    public int lineMatches = 0;
    public int lineNumber = 0;

    public CodeMatchResult(String line, boolean matching, boolean addBreak, int lineMatches, int lineNumber){
        this.setLine(line);
        this.setMatching(matching);
        this.setAddBreak(addBreak);
        this.setLineMatches(lineMatches);
        this.setLineNumber(lineNumber);
    }

    public String getLine() {
        return line;
    }

    public void setLine(String line) {
        this.line = line;
    }

    public boolean isMatching() {
        return matching;
    }

    public void setMatching(boolean matching) {
        this.matching = matching;
    }

    public boolean isAddBreak() {
        return addBreak;
    }

    public void setAddBreak(boolean addBreak) {
        this.addBreak = addBreak;
    }

    public int getLineMatches() {
        return lineMatches;
    }

    public void setLineMatches(int lineMatches) {
        this.lineMatches = lineMatches;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }
}
