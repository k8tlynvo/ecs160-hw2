package com.ecs160;

import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.*;
import com.ecs160.microservices.IssueSummarizerMicroservice;
import com.ecs160.microservices.BugFinderMicroservice;
import com.ecs160.microservices.IssueComparatorMicroservice;
import com.ecs160.model.Issue;
import com.ecs160.ollama.OllamaClientInterface;
import com.google.gson.Gson;

public class AppTest 
{
    Gson gson = new Gson();

    //IssueSummarizerMicroservice tests
    @Test
    public void testSummarizeIssueWithText() throws Exception {
        OllamaClientInterface mockOllama = Mockito.mock(OllamaClientInterface.class);
        Mockito.when(mockOllama.generate(Mockito.anyString(), Mockito.anyString()))
           .thenReturn("{\"bug_type\":\"NullPointer\",\"line\":0,\"description\":\"Mock description\",\"filename\":\"mock.c\"}");
        
        IssueSummarizerMicroservice service = new IssueSummarizerMicroservice(mockOllama);

        // input github issue json
        String inputJson = "{ \"text\": \"Null pointer occurs when accessing user.profile\" }";

        String json = service.summarizeIssue(inputJson);

        assertNotNull(json);
        assertTrue(json.contains("bug_type"));
        assertTrue(json.contains("description"));
    }

    // BugFinderMicroservice tests
    @Test
    public void testFindBugsNullPointer() throws Exception {
        OllamaClientInterface mockOllama = Mockito.mock(OllamaClientInterface.class);
        Mockito.when(mockOllama.generate(Mockito.anyString(), Mockito.anyString()))
           .thenReturn("[{\"bug_type\":\"NullPointer\",\"line\":3,\"description\":\"Mock description\",\"filename\":\"none\"}]");
       
        BugFinderMicroservice service = new BugFinderMicroservice(mockOllama);

        // c code with null pointer dereference
        String cCode = "#include <stdio.h>\n" +
                        "int main() {\n" +
                        "    int *ptr = NULL;\n" +
                        "    *ptr = 10;\n" +
                        "    return 0;\n" +
                        "}";

        String resultJson = service.findBugs(cCode);

        assertNotNull(resultJson);
        Issue[] issuesArray = gson.fromJson(resultJson, Issue[].class);
        // at least find one bug
        assertNotNull(issuesArray);
        assertTrue(issuesArray.length > 0);
    }

    @Test
    public void testFindBugsMemoryLeak() throws Exception {
        OllamaClientInterface mockOllama = Mockito.mock(OllamaClientInterface.class);
        Mockito.when(mockOllama.generate(Mockito.anyString(), Mockito.anyString()))
           .thenReturn("[{\"bug_type\":\"MemoryLeak\",\"line\":3,\"description\":\"Mock description\",\"filename\":\"none\"}]");
       
        BugFinderMicroservice service = new BugFinderMicroservice(mockOllama);

        // c code with memory leak
        String cCode = "#include <stdlib.h>\n" +
                        "int main() {\n" +
                        "    int *data = malloc(100 * sizeof(int));\n" +
                        "    return 0;\n" +
                        "}";

        String resultJson = service.findBugs(cCode);

        assertNotNull(resultJson);
        Issue[] issuesArray = gson.fromJson(resultJson, Issue[].class);
        // at least find one bug
        assertNotNull(issuesArray);
        assertTrue(issuesArray.length > 0);
    }

    @Test
    public void testFindBugsMultipleBugs() throws Exception {
        OllamaClientInterface mockOllama = Mockito.mock(OllamaClientInterface.class);
        Mockito.when(mockOllama.generate(Mockito.anyString(), Mockito.anyString()))
           .thenReturn("[{\"bug_type\":\"NullPointer\",\"line\":3,\"description\":\"Mock description\",\"filename\":\"none\"}, {\"bug_type\":\"SegFault\",\"line\":3,\"description\":\"Mock description\",\"filename\":\"none\"}]");
       
        BugFinderMicroservice service = new BugFinderMicroservice(mockOllama);

        // c code with multiple bugs
        String cCode = "#include <stdio.h>\n" +
                    "#include <stdlib.h>\n" +
                    "#include <string.h>\n" +
                    "int main() {\n" +
                    "    int *ptr = NULL;\n" +
                    "    *ptr = 10;\n" +
                    "    char buffer[10];\n" +
                    "    strcpy(buffer, \"overflow\");\n" +
                    "    int *data = malloc(100);\n" +
                    "    return 0;\n" +
                    "}";

        String resultJson = service.findBugs(cCode);

        assertNotNull(resultJson);
        Issue[] issuesArray = gson.fromJson(resultJson, Issue[].class);
        // at least find two bugs
        assertNotNull(issuesArray);
        assertTrue(issuesArray.length >= 2);
    }
    
    @Test
    public void testFindBugsCleanCode() throws Exception {
        OllamaClientInterface mockOllama = Mockito.mock(OllamaClientInterface.class);
        Mockito.when(mockOllama.generate(Mockito.anyString(), Mockito.anyString()))
           .thenReturn("[]");
       
        BugFinderMicroservice service = new BugFinderMicroservice(mockOllama);

        // c code with no bugs
        String cCode = "#include <stdio.h>\n" +
                        "int add(int a, int b) {\n" +
                        "    return a + b;\n" +
                        "}\n" +
                        "int main() {\n" +
                        "    int x = 5;\n" +
                        "    int y = 3;\n" +
                        "    int result = add(x, y);\n" +
                        "    printf(\"%d\\n\", result);\n" +
                        "    return 0;\n" +
                        "}";

        String resultJson = service.findBugs(cCode);

        assertNotNull(resultJson); // returns emtpy array
        Issue[] issuesArray = gson.fromJson(resultJson, Issue[].class);
        // no bugs found
        assertNotNull(issuesArray);
        assertTrue(issuesArray.length == 0);
    }

    // IssueComparatorMicroserviceTests
    @Test
    public void testCheckEquivalenceWithCommonIssues() throws Exception {
        IssueComparatorMicroservice service = new IssueComparatorMicroservice();

        // two lists with one common issue
        String inputJson = "[[" +
            "{\"bug_type\":\"NullPointer\",\"line\":22,\"description\":\"Null pointer dereference\",\"filename\":\"test.c\"}," +
            "{\"bug_type\":\"BufferOverflow\",\"line\":5,\"description\":\"Buffer overflow\",\"filename\":\"util.c\"}" +
            "],[" +
            "{\"bug_type\":\"NullPointer\",\"line\":22,\"description\":\"Null pointer dereference\",\"filename\":\"test.c\"}," +
            "{\"bug_type\":\"MemoryLeak\",\"line\":2,\"description\":\"Memory leak\",\"filename\":\"main.c\"}" +
            "]]";

        String resultJson = service.checkEquivalence(inputJson);

        assertNotNull(resultJson);
        Issue[] issuesArray = gson.fromJson(resultJson, Issue[].class);
        // one common bug found
        assertNotNull(issuesArray);
        assertTrue(issuesArray.length == 1);
    }

    @Test
    public void testCheckEquivalenceNoCommonIssues() throws Exception {
        IssueComparatorMicroservice service = new IssueComparatorMicroservice();

        // lists with no common issues
        String inputJson = "[[" +
            "{\"bug_type\":\"NullPointer\",\"line\":22,\"description\":\"Null pointer dereference\",\"filename\":\"test.c\"}" +
            "],[" +
            "{\"bug_type\":\"BufferOverflow\",\"line\":5,\"description\":\"Buffer overflow\",\"filename\":\"util.c\"}" +
            "]]";

        String resultJson = service.checkEquivalence(inputJson);

        assertNotNull(resultJson);
        Issue[] issuesArray = gson.fromJson(resultJson, Issue[].class);
        // no common bugs found
        assertNotNull(issuesArray);
        assertTrue(issuesArray.length == 0);
    }
}
