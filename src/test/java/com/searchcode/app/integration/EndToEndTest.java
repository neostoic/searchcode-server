package com.searchcode.app.integration;

import com.searchcode.app.dto.CodeResult;
import com.searchcode.app.dto.SearchResult;
import com.searchcode.app.jobs.IndexFileRepoJob;
import com.searchcode.app.jobs.IndexGitRepoJob;
import com.searchcode.app.jobs.IndexSvnRepoJob;
import com.searchcode.app.service.CodeIndexer;
import com.searchcode.app.service.CodeSearcher;
import junit.framework.TestCase;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.nio.file.Paths;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

public class EndToEndTest extends TestCase{
    public void testEndToEndFilePath() throws IOException {
        CodeSearcher cs = new CodeSearcher();
        File directoryWithFiles = createDirectoryWithFiles();
        IndexFileRepoJob indexFileRepoJob = new IndexFileRepoJob();

        // Index created files
        indexFileRepoJob.indexDocsByPath(Paths.get(directoryWithFiles.toString()), "ENDTOENDTEST", "", directoryWithFiles.toString(), false);
        SearchResult searchResult = cs.search("endtoendtestfile", 0);
        assertThat(searchResult.getCodeResultList().size()).isEqualTo(3);

        CodeResult codeResult1 = searchResult.getCodeResultList().stream().filter(x -> x.getFileName().equals("EndToEndTestFile1.php")).findFirst().get();
        CodeResult codeResult2 = searchResult.getCodeResultList().stream().filter(x -> x.getFileName().equals("EndToEndTestFile2.py")).findFirst().get();
        CodeResult codeResult3 = searchResult.getCodeResultList().stream().filter(x -> x.getFileName().equals("EndToEndTestFile3.java")).findFirst().get();
        assertThat(codeResult1.getCode().get(0)).isEqualTo("EndToEndTestFile EndToEndTestFile1");
        assertThat(codeResult2.getCode().get(0)).isEqualTo("EndToEndTestFile EndToEndTestFile2");
        assertThat(codeResult3.getCode().get(0)).isEqualTo("EndToEndTestFile EndToEndTestFile3");

        // Delete a single file
        String codeId = searchResult.getCodeResultList().get(0).getCodeId();
        CodeIndexer.deleteByCodeId(codeId);
        searchResult = cs.search("endtoendtestfile".toLowerCase(), 0);
        assertThat(searchResult.getCodeResultList().size()).isEqualTo(2);

        codeResult1 = searchResult.getCodeResultList().stream().filter(x -> x.getFileName().equals("EndToEndTestFile2.py")).findFirst().get();
        codeResult2 = searchResult.getCodeResultList().stream().filter(x -> x.getFileName().equals("EndToEndTestFile3.java")).findFirst().get();
        assertThat(codeResult1.getCode().get(0)).isEqualTo("EndToEndTestFile EndToEndTestFile2");
        assertThat(codeResult2.getCode().get(0)).isEqualTo("EndToEndTestFile EndToEndTestFile3");

        // Delete file from disk then index to ensure it is removed from the index
        File toDelete = new File(directoryWithFiles.toString() + "/EndToEndTestFile2.py");
        toDelete.delete();
        indexFileRepoJob.indexDocsByPath(Paths.get(directoryWithFiles.toString()), "ENDTOENDTEST", "", directoryWithFiles.toString(), true);
        searchResult = cs.search("endtoendtestfile", 0);
        assertThat(searchResult.getCodeResultList().size()).isEqualTo(2);

        codeResult1 = searchResult.getCodeResultList().stream().filter(x -> x.getFileName().equals("EndToEndTestFile1.php")).findFirst().get();
        codeResult2 = searchResult.getCodeResultList().stream().filter(x -> x.getFileName().equals("EndToEndTestFile3.java")).findFirst().get();
        assertThat(codeResult1.getCode().get(0)).isEqualTo("EndToEndTestFile EndToEndTestFile1");
        assertThat(codeResult2.getCode().get(0)).isEqualTo("EndToEndTestFile EndToEndTestFile3");

        // Delete everything
        CodeIndexer.deleteByReponame("ENDTOENDTEST");
        searchResult = cs.search("endtoendtestfile".toLowerCase(), 0);
        assertThat(searchResult.getCodeResultList().size()).isEqualTo(0);
    }

    public void testEndToEndGitPath() throws IOException {
        CodeSearcher cs = new CodeSearcher();
        File directoryWithFiles = createDirectoryWithFiles();

        String result = this.runCommand(directoryWithFiles.toString(), "/usr/bin/git", "init", ".");
        result = this.runCommand(directoryWithFiles.toString(), "/usr/bin/git", "add", ".");
        result = this.runCommand(directoryWithFiles.toString(), "/usr/bin/git", "commit", "-m", "\"First commit\"");

        IndexGitRepoJob indexGitRepoJob = new IndexGitRepoJob();
        indexGitRepoJob.indexDocsByPath(Paths.get(directoryWithFiles.toString()), "ENDTOENDTEST", "", directoryWithFiles.toString(), false);

        SearchResult searchResult = cs.search("endtoendtestfile", 0);
        assertThat(searchResult.getCodeResultList().size()).isEqualTo(3);

        CodeResult codeResult1 = searchResult.getCodeResultList().stream().filter(x -> x.getFileName().equals("EndToEndTestFile1.php")).findFirst().get();
        CodeResult codeResult2 = searchResult.getCodeResultList().stream().filter(x -> x.getFileName().equals("EndToEndTestFile2.py")).findFirst().get();
        CodeResult codeResult3 = searchResult.getCodeResultList().stream().filter(x -> x.getFileName().equals("EndToEndTestFile3.java")).findFirst().get();
        assertThat(codeResult1.getCode().get(0)).isEqualTo("EndToEndTestFile EndToEndTestFile1");
        assertThat(codeResult2.getCode().get(0)).isEqualTo("EndToEndTestFile EndToEndTestFile2");
        assertThat(codeResult3.getCode().get(0)).isEqualTo("EndToEndTestFile EndToEndTestFile3");

        // Delete a single file
        String codeId = searchResult.getCodeResultList().get(0).getCodeId();
        CodeIndexer.deleteByCodeId(codeId);
        searchResult = cs.search("endtoendtestfile".toLowerCase(), 0);
        assertThat(searchResult.getCodeResultList().size()).isEqualTo(2);

        // Delete file from disk then index to ensure it is removed from the index
        File toDelete = new File(directoryWithFiles.toString() + "/EndToEndTestFile2.py");
        toDelete.delete();
        indexGitRepoJob.indexDocsByPath(Paths.get(directoryWithFiles.toString()), "ENDTOENDTEST", "", directoryWithFiles.toString(), true);
        searchResult = cs.search("endtoendtestfile", 0);
        assertThat(searchResult.getCodeResultList().size()).isEqualTo(2);

        codeResult1 = searchResult.getCodeResultList().stream().filter(x -> x.getFileName().equals("EndToEndTestFile1.php")).findFirst().get();
        codeResult2 = searchResult.getCodeResultList().stream().filter(x -> x.getFileName().equals("EndToEndTestFile3.java")).findFirst().get();
        assertThat(codeResult1.getCode().get(0)).isEqualTo("EndToEndTestFile EndToEndTestFile1");
        assertThat(codeResult2.getCode().get(0)).isEqualTo("EndToEndTestFile EndToEndTestFile3");

        CodeIndexer.deleteByReponame("ENDTOENDTEST");
        searchResult = cs.search("endtoendtestfile".toLowerCase(), 0);
        assertThat(searchResult.getCodeResultList().size()).isEqualTo(0);
    }

    public void testEndToEndSvnPath() throws IOException {
        CodeSearcher cs = new CodeSearcher();
        File directoryWithFiles = createDirectoryWithFiles();

        IndexSvnRepoJob indexSvnRepoJob = new IndexSvnRepoJob();
        indexSvnRepoJob.indexDocsByPath(Paths.get(directoryWithFiles.toString()), "ENDTOENDTEST", "", directoryWithFiles.toString(), false);

        SearchResult searchResult = cs.search("endtoendtestfile", 0);
        assertThat(searchResult.getCodeResultList().size()).isEqualTo(3);

        CodeResult codeResult1 = searchResult.getCodeResultList().stream().filter(x -> x.getFileName().equals("EndToEndTestFile1.php")).findFirst().get();
        CodeResult codeResult2 = searchResult.getCodeResultList().stream().filter(x -> x.getFileName().equals("EndToEndTestFile2.py")).findFirst().get();
        CodeResult codeResult3 = searchResult.getCodeResultList().stream().filter(x -> x.getFileName().equals("EndToEndTestFile3.java")).findFirst().get();
        assertThat(codeResult1.getCode().get(0)).isEqualTo("EndToEndTestFile EndToEndTestFile1");
        assertThat(codeResult2.getCode().get(0)).isEqualTo("EndToEndTestFile EndToEndTestFile2");
        assertThat(codeResult3.getCode().get(0)).isEqualTo("EndToEndTestFile EndToEndTestFile3");

        // Delete a single file
        String codeId = searchResult.getCodeResultList().get(0).getCodeId();
        CodeIndexer.deleteByCodeId(codeId);
        searchResult = cs.search("endtoendtestfile".toLowerCase(), 0);
        assertThat(searchResult.getCodeResultList().size()).isEqualTo(2);

        // Delete file from disk then index to ensure it is removed from the index
        File toDelete = new File(directoryWithFiles.toString() + "/EndToEndTestFile2.py");
        toDelete.delete();
        indexSvnRepoJob.indexDocsByPath(Paths.get(directoryWithFiles.toString()), "ENDTOENDTEST", "", directoryWithFiles.toString(), true);
        searchResult = cs.search("endtoendtestfile", 0);
        assertThat(searchResult.getCodeResultList().size()).isEqualTo(2);

        codeResult1 = searchResult.getCodeResultList().stream().filter(x -> x.getFileName().equals("EndToEndTestFile1.php")).findFirst().get();
        codeResult2 = searchResult.getCodeResultList().stream().filter(x -> x.getFileName().equals("EndToEndTestFile3.java")).findFirst().get();
        assertThat(codeResult1.getCode().get(0)).isEqualTo("EndToEndTestFile EndToEndTestFile1");
        assertThat(codeResult2.getCode().get(0)).isEqualTo("EndToEndTestFile EndToEndTestFile3");

        CodeIndexer.deleteByReponame("ENDTOENDTEST");
        searchResult = cs.search("endtoendtestfile".toLowerCase(), 0);
        assertThat(searchResult.getCodeResultList().size()).isEqualTo(0);
    }

    private String runCommand(String directory, String... command) throws IOException {
        ProcessBuilder processBuilder = new ProcessBuilder(command);

        processBuilder.directory(new File(directory));
        Process process = processBuilder.start();

        InputStream is = process.getInputStream();
        InputStreamReader isr = new InputStreamReader(is);
        BufferedReader br = new BufferedReader(isr);
        String line;
        StringBuilder sb = new StringBuilder();

        while ((line = br.readLine()) != null) {
            sb.append(line);
        }

        return sb.toString();
    }

    private File createDirectoryWithFiles() throws IOException {
        File tempPath = this.clearAndCreateTempPath();

        createFile(tempPath, "EndToEndTestFile1.php", "EndToEndTestFile EndToEndTestFile1");
        createFile(tempPath, "EndToEndTestFile2.py",  "EndToEndTestFile EndToEndTestFile2");
        createFile(tempPath, "EndToEndTestFile3.java",  "EndToEndTestFile EndToEndTestFile3");

        return tempPath;
    }

    private File createFile(File tempDir, String filename, String contents) throws IOException {
        File file = new File(tempDir, filename);

        try (PrintWriter pw = new PrintWriter(new FileWriter(file, true))) {
            pw.println(contents);
        }

        return file;
    }

    private File clearAndCreateTempPath() throws IOException {
        String baseName = org.apache.commons.codec.digest.DigestUtils.md5Hex("EndToEndFileTest");
        File baseDir = new File(System.getProperty("java.io.tmpdir"));
        File tempDir = new File(baseDir, baseName);

        if(tempDir.exists()) {
            FileUtils.deleteDirectory(tempDir);
        }

        tempDir.mkdir();

        return tempDir;
    }
}