package com.ecs160.persistence;

import com.ecs160.persistence.annotations.PersistableObject;
import com.ecs160.persistence.annotations.PersistableField;
import com.ecs160.persistence.annotations.Id;
import org.junit.Test;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Basic test for RedisDB persistence framework
 * Tests persist() and load() methods with simple objects
 */
public class RedisDBTest {

    // Simple test class
    @PersistableObject
    static class TestObject {
        @Id
        private String id;
        
        @PersistableField
        private String name;
        
        @PersistableField
        private int value;
        
        public TestObject() {}
        
        public TestObject(String id, String name, int value) {
            this.id = id;
            this.name = name;
            this.value = value;
        }
        
        // Getters and setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public int getValue() { return value; }
        public void setValue(int value) { this.value = value; }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TestObject that = (TestObject) o;
            return value == that.value &&
                   (id != null ? id.equals(that.id) : that.id == null) &&
                   (name != null ? name.equals(that.name) : that.name == null);
        }
    }

    @Test
    public void testPersistAndLoad() {
        RedisDB db = RedisDB.getInstance();
        
        // Create test object
        TestObject original = new TestObject("test:key:1", "TestName", 42);
        
        // Persist
        boolean persisted = db.persist(original);
        assertTrue("Object should be persisted successfully", persisted);
        
        // Load
        TestObject toLoad = new TestObject();
        toLoad.setId("test:key:1");
        TestObject loaded = (TestObject) db.load(toLoad);
        
        assertNotNull("Loaded object should not be null", loaded);
        assertEquals("Name should match", original.getName(), loaded.getName());
        assertEquals("Value should match", original.getValue(), loaded.getValue());
        
        // Cleanup
        redis.clients.jedis.Jedis jedis = new redis.clients.jedis.Jedis("localhost", 6379);
        jedis.del("test:key:1");
        jedis.close();
    }
    
    @Test
    public void testPersistNonPersistableObject() {
        RedisDB db = RedisDB.getInstance();
        
        Object obj = new Object();
        boolean result = db.persist(obj);
        assertFalse("Non-persistable object should return false", result);
    }
    
    @Test
    public void testLoadNonExistentObject() {
        RedisDB db = RedisDB.getInstance();
        
        TestObject toLoad = new TestObject();
        toLoad.setId("test:nonexistent:999");
        TestObject loaded = (TestObject) db.load(toLoad);
        
        assertNull("Non-existent object should return null", loaded);
    }
    
    @Test
    public void testPersistNullObject() {
        RedisDB db = RedisDB.getInstance();
        
        boolean result = db.persist(null);
        assertFalse("Null object should return false", result);
    }
    
    @Test
    public void testLoadNullObject() {
        RedisDB db = RedisDB.getInstance();
        
        Object result = db.load(null);
        assertNull("Null object should return null", result);
    }
}

