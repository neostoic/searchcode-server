/*
 * Copyright (c) 2016 Boyter Online Services
 *
 * Use of this software is governed by the Fair Source License included
 * in the LICENSE.TXT file, but will be eventually open under GNU General Public License Version 3
 * see the README.md for when this clause will take effect
 *
 * Version 1.3.10
 */

package com.searchcode.app.util;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.searchcode.app.config.Values;
import com.searchcode.app.dao.Data;
import com.searchcode.app.dto.*;
import com.searchcode.app.service.Singleton;
import com.searchcode.app.dto.FileClassifierResult;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.queryparser.classic.QueryParser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.OptionalDouble;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SearchcodeLib {

    private int MAXSPLITLENGTH = 100000;
    private Pattern MULTIPLEUPPERCASE = Pattern.compile("[A-Z]{2,}");
    private int MINIFIEDLENGTH = Integer.parseInt(Values.DEFAULTMINIFIEDLENGTH);
    public String[] WHITELIST = Properties.getProperties().getProperty(Values.BINARY_WHITE_LIST, Values.DEFAULT_BINARY_WHITE_LIST).split(",");
    public String[] BLACKLIST = Properties.getProperties().getProperty(Values.BINARY_BLACK_LIST, Values.DEFAULT_BINARY_BLACK_LIST).split(",");
    private boolean GUESSBINARY = Boolean.parseBoolean(Properties.getProperties().getProperty(Values.GUESS_BINARY, Values.DEFAULT_GUESS_BINARY));
    private boolean ANDMATCH = Boolean.parseBoolean(com.searchcode.app.util.Properties.getProperties().getProperty(Values.AND_MATCH, Values.DEFAULT_AND_MATCH));
    public FileClassifier fileClassifier = null;

    public SearchcodeLib() {
        fileClassifier = new FileClassifier();
    }

    public SearchcodeLib(Data data) {
        this.MINIFIEDLENGTH = Singleton.getHelpers().tryParseInt(data.getDataByName(Values.MINIFIEDLENGTH, Values.DEFAULTMINIFIEDLENGTH), Values.DEFAULTMINIFIEDLENGTH);
        if (this.MINIFIEDLENGTH <= 0) {
            this.MINIFIEDLENGTH = Integer.parseInt(Values.DEFAULTMINIFIEDLENGTH);
        }
        fileClassifier = new FileClassifier();
    }

    /**
     * Split "intelligently" on anything over 7 characters long
     * if it only contains [a-zA-Z]
     * split based on uppercase String[] r = s.split("(?=\\p{Upper})");
     * add those as additional words to index on
     * so that things like RegexIndexer becomes Regex Indexer
     * split the string by spaces
     * look for anything over 7 characters long
     * if its only [a-zA-Z]
     * split by uppercase
     */
    public String splitKeywords(String contents) {
        if (contents == null) {
            return Values.EMPTYSTRING;
        }

        StringBuilder indexContents = new StringBuilder();

        contents = contents.replaceAll("[^a-zA-Z0-9]", " ");

        // Performance improvement hack
        if (contents.length() > this.MAXSPLITLENGTH) {

            // Add AAA to ensure we dont split the last word if it was cut off
            contents = contents.substring(0, MAXSPLITLENGTH) + "AAA";
        }

        for (String splitContents: contents.split(" ")) {
            if (splitContents.length() >= 7) {
                Matcher m = MULTIPLEUPPERCASE.matcher(splitContents);

                if (!m.find()) {
                    String[] splitStrings = splitContents.split("(?=\\p{Upper})");

                    if (splitStrings.length > 1) {
                        indexContents.append(" ");
                        indexContents.append(StringUtils.join(splitStrings, " "));
                    }
                }
            }
        }

        return indexContents.toString();
    }

    public String findInterestingKeywords(String contents) {
        if (contents == null) {
            return Values.EMPTYSTRING;
        }

        StringBuilder indexContents = new StringBuilder();

        // Performance improvement hack
        if (contents.length() > this.MAXSPLITLENGTH) {

            // Add AAA to ensure we dont split the last word if it was cut off
            contents = contents.substring(0, MAXSPLITLENGTH) + "AAA";
        }

        // Finds versions with words at the front, eg linux2.7.4
        Matcher m = Pattern.compile("[a-z]+(\\d+\\.)?(\\d+\\.)?(\\*|\\d+)").matcher(contents);

        while (m.find()) {
            indexContents.append(" ");
            indexContents.append(m.group());
        }

        return indexContents.toString();
    }

    public String findInterestingCharacters(String contents) {
        if (contents == null) {
            return Values.EMPTYSTRING;
        }

        String replaced = contents.replaceAll("\\w", "");

        StringBuffer stringBuffer = new StringBuffer();
        for(char c: replaced.toCharArray()) {
            stringBuffer.append(c).append(" ");
        }

        return stringBuffer.toString();
    }

    /**
     * List of languages to ignore displaying the cost for
     */
    public boolean languageCostIgnore(String languagename) {

        boolean ignore;

        switch(languagename) {
            case "Unknown":
            case "Text":
            case "JSON":
            case "Markdown":
            case "INI File":
            case "ReStructuredText":
            case "Configuration":
                ignore = true;
                break;
            default:
                ignore = false;
                break;
        }

        return ignore;
    }

    /**
     * Tries to guess the true amount of lines in a code file ignoring those that are blank or comments
     * fairly crude however without resorting to parsing which is slow its good enough for our purposes
     */
    public int countFilteredLines(List<String> codeLines) {
        return codeLines.stream().map(x -> x.trim()).filter(x -> {
            return !(x.startsWith("//") ||
                    x.startsWith("#") ||
                    x.length() == 0 ||
                    x.startsWith("<!--") ||
                    x.startsWith("!*") ||
                    x.startsWith("--") ||
                    x.startsWith("%") ||
                    x.startsWith(";") ||
                    x.startsWith("*") ||
                    x.startsWith("/*"));
        }).toArray().length;
    }

    /**
     * Adds a string into the spelling corrector.
     * TODO move this into the spelling corrector class itself
     */
    public void addToSpellingCorrector(String contents) {
        if (contents == null) {
            return;
        }

        ISpellingCorrector sc = Singleton.getSpellingCorrector();

        // Limit to reduce performance impacts
        if (contents.length() > this.MAXSPLITLENGTH) {
            contents = contents.substring(0, MAXSPLITLENGTH);
        }

        List<String> splitString = Arrays.asList(contents.replaceAll("[^a-zA-Z0-9]", " ").toLowerCase().split(" "));

        // Only the first 10000 to avoid causing too much slow-down
        if (splitString.size() > 10000) {
            splitString = splitString.subList(0, 10000);
        }

        for (String s: splitString) {
            if (s.length() >= 3) {
                sc.putWord(s);
            }
        }
    }

    /**
     * Determine if a List<String> which is used to represent a code file contains a code file that is
     * suspected to be minified. This is for the purposes of excluding it from the index.
     */
    public boolean isMinified(List<String> codeLines, String fileName) {

        String lowerFileName = fileName.toLowerCase();

        for (String extension: this.WHITELIST) {
            if (lowerFileName.endsWith("." + extension)) {
                return false;
            }
        }

        OptionalDouble average = codeLines.stream().map(x -> x.trim().replace(" ", "")).mapToInt(String::length).average();
        if (average.isPresent() && average.getAsDouble() > this.MINIFIEDLENGTH) {
            return true;
        }

        return false;
    }

    /**
     * Determine if a List<String> which is used to represent a code file contains a code file that is
     * suspected to be ascii or non ascii. This is for the purposes of excluding it from the index.
     */
    public BinaryFinding isBinary(List<String> codeLines, String fileName) {
        if (codeLines.isEmpty()) {
            return new BinaryFinding(true, "file is empty");
        }

        String lowerFileName = fileName.toLowerCase();
        // Check against user set whitelist
        for (String extension: this.WHITELIST) {
            if (lowerFileName.endsWith("." + extension)) {
                return new BinaryFinding(false, "appears in extension whitelist");
            }
        }

        // Check against user set blacklist
        for (String extention: this.BLACKLIST) {
            if (lowerFileName.endsWith("." + extention)) {
                return new BinaryFinding(true, "appears in extension blacklist");
            }
        }

        // Check if whitelisted extention IE what we know about
        for (FileClassifierResult fileClassifierResult: fileClassifier.getDatabase()) {
            for (String extention: fileClassifierResult.extensions) {
                if (lowerFileName.endsWith("." + extention)) {
                    return new BinaryFinding(false, "appears in internal extension whitelist");
                }
            }
        }

        // If we aren't meant to guess then assume it isnt binary
        if (this.GUESSBINARY == false) {
            return new BinaryFinding(false, Values.EMPTYSTRING);
        }

        int lines = codeLines.size() < 10000 ? codeLines.size() : 10000;
        double asciiCount = 0;
        double nonAsciiCount = 0;

        for (int i=0; i < lines; i++) {
            String line = codeLines.get(i);
            for (int j = 0; j < line.length(); j++) {
                if (((int)line.charAt(j)) <= 128) {
                    asciiCount++;
                }
                else {
                    nonAsciiCount++;
                }
            }
        }

        if (nonAsciiCount == 0) {
            return new BinaryFinding(false, Values.EMPTYSTRING);
        }

        if (asciiCount == 0) {
            return new BinaryFinding(true, "all characters found non-ascii");
        }

        // If only 30% of characters are ascii then its probably binary
        double percent = asciiCount / (asciiCount + nonAsciiCount);

        if (percent < 0.30) {
            return new BinaryFinding(true, "only 30% of characters are non-ascii");
        }

        return new BinaryFinding(false, Values.EMPTYSTRING);
    }

    /**
     * Determines who owns a piece of code weighted by time based on current second (IE time now)
     * NB if a commit is very close to this time it will always win
     */
    public String codeOwner(List<CodeOwner> codeOwners) {
        long currentUnix = System.currentTimeMillis() / 1000L;

        double best = 0;
        String owner = "Unknown";

        for(CodeOwner codeOwner: codeOwners) {
            double age = (currentUnix - codeOwner.getMostRecentUnixCommitTimestamp()) / 60 / 60;
            double calc = codeOwner.getNoLines() / Math.pow((age), 1.8);

            if (calc > best) {
                best = calc;
                owner = codeOwner.getName();
            }
        }

        return owner;
    }

    /**
     * Cleans and formats the code into something that can be indexed by lucene while supporting searches such as
     * i++ matching for(int i=0;i<100;i++;){
     */
    public String codeCleanPipeline(String contents) {
        if (contents == null) {
            return Values.EMPTYSTRING;
        }

        StringBuilder indexContents = new StringBuilder();

        // Change how we replace strings
        // Modify the contents to match strings correctly
        char[] firstReplacements = {'<', '>', ')', '(', '[', ']', '|', '=', ',', ':'};
        for (char c : firstReplacements) {
            contents = contents.replace(c, ' ');
        }
        indexContents.append(" ").append(contents);

        char[] otherReplacements = {'.'};
        for (char c : otherReplacements) {
            contents = contents.replace(c, ' ');
        }
        indexContents.append(" ").append(contents);

        char[] secondReplacements = {';', '{', '}', '/'};
        for (char c : secondReplacements) {
            contents = contents.replace(c, ' ');
        }
        indexContents.append(" ").append(contents);

        char[] forthReplacements = {'"', '\''};
        for (char c : forthReplacements) {
            contents = contents.replace(c, ' ');
        }
        indexContents.append(" ").append(contents);

        // Now do it for other characters
        char[] replacements = {'\'', '"', '.', ';', '=', '(', ')', '[', ']', '_', ';', '@', '#'};
        for (char c : replacements) {
            contents = contents.replace(c, ' ');
        }
        indexContents.append(" ").append(contents);

        char[] thirdReplacements = {'-'};
        for (char c : thirdReplacements) {
            contents = contents.replace(c, ' ');
        }
        indexContents.append(" ").append(contents);

        return indexContents.toString();
    }

    /**
     * Parse the query and escape it as per Lucene but without affecting search operators such as AND OR and NOT
     */
    public String formatQueryString(String query) {
        if (this.ANDMATCH) {
            return this.formatQueryStringAndDefault(query);
        }

        return this.formatQueryStringOrDefault(query);
    }

    public String formatQueryStringAndDefault(String query) {
        String[] split = query.trim().split("\\s+");

        List<String> stringList = new ArrayList<>();

        String and = " AND ";
        String or = " OR ";
        String not = " NOT ";

        for(String term: split) {
            switch (term) {
                case "AND":
                    if (Iterables.getLast(stringList, null) != null && !Iterables.getLast(stringList).equals(and)) {
                        stringList.add(and);
                    }
                    break;
                case "OR":
                    if (Iterables.getLast(stringList, null) != null && !Iterables.getLast(stringList).equals(or)) {
                        stringList.add(or);
                    }
                    break;
                case "NOT":
                    if (Iterables.getLast(stringList, null) != null && !Iterables.getLast(stringList).equals(not)) {
                        stringList.add(not);
                    }
                    break;
                default:
                    if (Iterables.getLast(stringList, null) == null ||
                            Iterables.getLast(stringList).equals(and) ||
                            Iterables.getLast(stringList).equals(or) ||
                            Iterables.getLast(stringList).equals(not)) {
                        stringList.add(" " + QueryParser.escape(term.toLowerCase()).replace("\\(", "(").replace("\\)", ")").replace("\\*", "*") + " ");
                    }
                    else {
                        stringList.add(and + QueryParser.escape(term.toLowerCase()).replace("\\(", "(").replace("\\)", ")").replace("\\*", "*") + " ");
                    }
                    break;
            }
        }
        String temp = StringUtils.join(stringList, " ");
        return temp.trim();
    }

    public String formatQueryStringOrDefault(String query) {
        String[] split = query.trim().split("\\s+");

        StringBuilder sb = new StringBuilder();

        String and = " AND ";
        String or = " OR ";
        String not = " NOT ";

        for(String term: split) {
            switch (term) {
                case "AND":
                    sb.append(and);
                    break;
                case "OR":
                    sb.append(or);
                    break;
                case "NOT":
                    sb.append(not);
                    break;
                default:
                    sb.append(" ");
                    sb.append(QueryParser.escape(term.toLowerCase()).replace("\\(", "(").replace("\\)", ")").replace("\\*", "*"));
                    sb.append(" ");
                    break;
            }
        }

        return sb.toString().trim();
    }

    /**
     * Given a query attempts to create alternative queries that should be looser and as such produce more matches
     * or give results where none may exist for the current query.
     */
    public List<String> generateAltQueries(String query) {
        List<String> altQueries = new ArrayList<>();
        query = query.trim().replaceAll(" +", " ");
        String altquery = query.replaceAll("[^A-Za-z0-9 ]", " ").trim().replaceAll(" +", " ");

        if (!altquery.equals(query) && !Values.EMPTYSTRING.equals(altquery)) {
            altQueries.add(altquery);
        }

        altquery = this.splitKeywords(query).trim();
        if (!altquery.equals("") && !altquery.equals(query) && !altQueries.contains(altquery)) {
            altQueries.add(altquery);
        }

        ISpellingCorrector sc = Singleton.getSpellingCorrector();
        StringBuilder stringBuilder = new StringBuilder();
        for(String word: query.replaceAll(" +", " ").split(" ")) {
            if (!word.trim().equals("AND") && !word.trim().equals("OR") && !word.trim().equals("NOT")) {
                stringBuilder.append(" ").append(sc.correct(word));
            }
        }
        altquery = stringBuilder.toString().trim();

        if (!altquery.toLowerCase().equals(query.toLowerCase()) && !altQueries.contains(altquery)) {
            altQueries.add(altquery);
        }

        altquery = query.replace(" AND ", " OR ");
        if (!altquery.toLowerCase().equals(query.toLowerCase()) && !altQueries.contains(altquery)) {
            altQueries.add(altquery);
        }

        altquery = query.replace(" AND ", " ");
        if (!altquery.toLowerCase().equals(query.toLowerCase()) && !altQueries.contains(altquery)) {
            altQueries.add(altquery);
        }

        altquery = query.replace(" NOT ", " ");
        if (!altquery.toLowerCase().equals(query.toLowerCase()) && !altQueries.contains(altquery)) {
            altQueries.add(altquery);
        }

        return altQueries;
    }


    public String generateBusBlurb(ProjectStats projectStats) {

        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append("In this repository ").append(projectStats.getRepoFacetOwner().size());

        if (projectStats.getRepoFacetOwner().size() == 1) {
            stringBuffer.append(" committer has contributed to ");
        }
        else {
            stringBuffer.append(" committers have contributed to ");
        }

        if (projectStats.getTotalFiles() == 1) {
            stringBuffer.append(projectStats.getTotalFiles()).append(" file. ");
        }
        else {
            stringBuffer.append(projectStats.getTotalFiles()).append(" files. ");
        }

        List<CodeFacetLanguage> codeFacetLanguages = projectStats.getCodeFacetLanguages();

        if (codeFacetLanguages.size() == 1) {
            stringBuffer.append("The most important language in this repository is ").append(codeFacetLanguages.get(0).getLanguageName()).append(". ");
        }
        else {
            stringBuffer.append("The most important languages in this repository are ");

            if (!codeFacetLanguages.isEmpty()) {
                if (codeFacetLanguages.size() > 3) {
                    codeFacetLanguages = codeFacetLanguages.subList(0, 3);
                }
                for (int i = 0; i < codeFacetLanguages.size() - 1; i++) {
                    stringBuffer.append(codeFacetLanguages.get(i).getLanguageName()).append(", ");
                }
                stringBuffer.append(" and ").append(codeFacetLanguages.get(codeFacetLanguages.size() - 1).getLanguageName()).append(". ");
            }
        }

        if (!projectStats.getRepoFacetOwner().isEmpty()) {
            if (projectStats.getRepoFacetOwner().size() < 5) {
                stringBuffer.append("The project has a low bus factor of ").append(projectStats.getRepoFacetOwner().size());
                stringBuffer.append(" and will be in trouble if ").append(projectStats.getRepoFacetOwner().get(0).getOwner()).append(" is hit by a bus. ");
            } else if (projectStats.getRepoFacetOwner().size() < 15) {
                stringBuffer.append("The project has bus factor of ").append(projectStats.getRepoFacetOwner().size()).append(". ");
            } else {
                stringBuffer.append("The project has high bus factor of ").append(projectStats.getRepoFacetOwner().size()).append(". ");
            }
        }

        List<String> highKnowledge = new ArrayList<>();
        double sumAverageFilesWorked = 0;
        for (CodeFacetOwner codeFacetOwner: projectStats.getRepoFacetOwner()) {
            double currentAverage = (double)codeFacetOwner.getCount() / (double)projectStats.getTotalFiles();
            sumAverageFilesWorked += currentAverage;

            if (currentAverage > 0.1) {
                highKnowledge.add(codeFacetOwner.getOwner());
            }
        }

        int averageFilesWorked = (int)(sumAverageFilesWorked / projectStats.getRepoFacetOwner().size() * 100);

        stringBuffer.append("The average person who commits this project has ownership of ");
        stringBuffer.append(averageFilesWorked).append("% of files. ");

        if (!highKnowledge.isEmpty()) {
            stringBuffer.append("The project relies on the following people; ");
            stringBuffer.append(StringUtils.join(highKnowledge, ", ")).append(". ");
        }

        return stringBuffer.toString().replace(",  and", " and");
    }

//    /**
//     * Currently not used but meant to replicate the searchcode.com hash which is used to identify duplicate files
//     * even when they have a few characters or lines missing. It should in these cases produce identical hashes.
//     */
//    public String hash(String contents) {
//        int hashLength = 20;
//
//        if (contents.length() == 0) {
//            return Strings.padStart("", hashLength, '0');
//        }
//
//        String allowedCharacters = "BCDFGHIJKLMNOPQRSUVWXYZbcdfghijklmnopqrsuvwxyz1234567890";
//
//        // remove all spaces
//        Joiner joiner = Joiner.on("").skipNulls();
//        String toHash = joiner.join(Splitter.on(' ')
//                            .trimResults()
//                            .omitEmptyStrings()
//                            .split(contents));
//
//        // remove all non acceptable characters
//        for(int i=0; i< toHash.length(); i++) {
//            char c = toHash.charAt(i);
//
//            if (allowedCharacters.indexOf(c) != -1) {
//                // allowed so keep it
//            }
//        }
//
//        return "";
//    }
}

