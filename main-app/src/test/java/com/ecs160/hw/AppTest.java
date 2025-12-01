package com.ecs160.hw;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

import org.junit.Test;
import com.google.gson.Gson;
import java.io.*;

public class AppTest 
{
    Gson gson = new Gson();

    @Test
    public void testDeleteDirectory() throws Exception
    {
        File root = new File("test-dir");
        File sub = new File(root, "sub-dir");
        File file1 = new File(root, "file1.txt");
        File file2 = new File(sub, "file2.txt");

        sub.mkdirs();
        file1.createNewFile();
        file2.createNewFile();

        assertTrue(root.exists());

        App.deleteDirectory(root);

        assertTrue(!root.exists());
    }

    @Test
    public void testFindMatchingBraceSimple() {
        String input = "{123}";
        int pos = App.findMatchingBrace(input, 0);
        assertEquals(4, pos);
    }
}
